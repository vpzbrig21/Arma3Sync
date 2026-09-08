package fr.soe.a3s.service.administration;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

import fr.soe.a3s.controller.ObserverConnectionLost;
import fr.soe.a3s.controller.ObserverCountInt;
import fr.soe.a3s.controller.ObserverCountLong;
import fr.soe.a3s.controller.ObserverEnd;
import fr.soe.a3s.controller.ObserverError;
import fr.soe.a3s.controller.ObserverText;
import fr.soe.a3s.controller.ObserverUpload;
import fr.soe.a3s.constant.ProtocolType;
import fr.soe.a3s.dao.connection.AbstractConnexionDAO;
import fr.soe.a3s.domain.AbstractProtocole;
import fr.soe.a3s.dto.sync.SyncTreeDirectoryDTO;
import fr.soe.a3s.exception.CheckException;
import fr.soe.a3s.service.ConnectionService;
import fr.soe.a3s.service.RepositoryService;
import fr.soe.a3s.utils.DebugLogger;

public class RepositoryUploadProcessor {

	/* Data */
	private final String repositoryName;
	private int lastIndexFileUploaded;
	private long uploadedFilesSize, totalFilesSize, currentSize;
	private long startTime, deltaTimeSpeed;
	/* Services */
	private final RepositoryService repositoryService = new RepositoryService();
	private ConnectionService connexionService;
	private FilesUploadManager filesManager;
	/* Tests */
	private volatile boolean canceled = false;
	private volatile boolean completed = false;
	private final AtomicBoolean cancellationRequested = new AtomicBoolean(false);
	private final AtomicLong parallelUploadedBytes = new AtomicLong();
	private final Map<AbstractConnexionDAO, AtomicLong> parallelCurrentFileBytes =
			new ConcurrentHashMap<AbstractConnexionDAO, AtomicLong>();
	/* observers */
	private ObserverText observerText;
	private ObserverCountInt observerCountProgress;
	private ObserverCountLong observerTotalSize, observerUploadedSize,
			observerSpeed, observerRemainingTime;
	private ObserverEnd observerEndUpload;
	private ObserverError observerError;
	private ObserverConnectionLost observerConnectionLost;

	public RepositoryUploadProcessor(String repositoryName) {
		this.repositoryName = repositoryName;
	}

	public void run() {
		long operationStart = System.nanoTime();
		completed = false;
		canceled = false;
		cancellationRequested.set(false);
		parallelUploadedBytes.set(0);
		parallelCurrentFileBytes.clear();
		DebugLogger.info("Repository upload started: name=" + repositoryName);

		try {
			// 1. Check repository upload protocol
			AbstractProtocole uploadProtocol = repositoryService
					.getUploadProtocol(repositoryName);

			if (uploadProtocol == null) {
				String message = "Please use the upload options to configure a connection.";
				throw new CheckException(message);
			}
			DebugLogger.info("Upload protocol loaded: " + DebugLogger.describeProtocol(uploadProtocol));

			int configuredConnections = repositoryService.getParallelUploadConnections(repositoryName);
			boolean parallelUpload = uploadProtocol.getProtocolType() == ProtocolType.FTP
					|| uploadProtocol.getProtocolType() == ProtocolType.SFTP;
			connexionService = new ConnectionService(parallelUpload ? configuredConnections : 1, uploadProtocol);
			DebugLogger.info("Upload connection configuration: protocol="
					+ uploadProtocol.getProtocolType() + ", configured=" + configuredConnections
					+ ", active=" + connexionService.getConnexionDAOs().size()
					+ ", parallel=" + parallelUpload);

			for (AbstractConnexionDAO connectionDAO : connexionService.getConnexionDAOs()) {
				connectionDAO.addObserverText(new ObserverText() {
						@Override
						public void update(String text) {
							executeUpdateText(text);
						}
					});
			}

			connexionService.getConnexionDAOs().get(0).addObserverCount(new ObserverCountInt() {
						@Override
						public void update(int value) {
							executeUpdateSingleSizeProgress(value);
						}
					});

			ObserverUpload sequentialObserver = createSequentialUploadObserver();
			for (int i = 0; i < connexionService.getConnexionDAOs().size(); i++) {
				AbstractConnexionDAO connectionDAO = connexionService.getConnexionDAOs().get(i);
				connectionDAO.addObserverUpload(parallelUpload
						? new AggregatedUploadObserver(connectionDAO)
						: sequentialObserver);
			}

			// 2. Read local sync, autoconfig, serverInfo, changelogs
			repositoryService.readLocalyBuildedRepository(repositoryName);// IOException

			// 3. Determine files to check, upload and delete
			try {
				connexionService.getSyncWithUploadProtocole(repositoryName);// IOException
			} catch (IOException e) {
				if (!(e instanceof FileNotFoundException)) {// if remoteSync not found
					throw e;
				}
			}
			// SocketException
			SyncTreeDirectoryDTO localSync = repositoryService
					.getLocalSync(repositoryName);// not null
			SyncTreeDirectoryDTO remoteSync = repositoryService
					.getSync(repositoryName);// may be null

			filesManager = new FilesUploadManager();
			filesManager.setLocalSync(localSync);
			filesManager.setRemoteSync(remoteSync);
			filesManager.update();
			DebugLogger.info("Upload plan prepared: check=" + filesManager.getFilesToCheck().size()
					+ ", upload=" + filesManager.getFilesToUpload().size() + ", delete="
					+ filesManager.getFilesToDelete().size());

			// 4. Resume Upload
			lastIndexFileUploaded = repositoryService
					.getLastIndexFileTransfered(repositoryName);
			startTime = System.nanoTime();
			connexionService.uploadRepository(repositoryName,
					filesManager.getFilesToCheck(),
					filesManager.getFilesToUpload(),
					filesManager.getFilesToDelete(), lastIndexFileUploaded);

			if (!canceled) {
				repositoryService.setLastIndexFileTransfered(repositoryName, 0);
			}

			completed = true;
			executEnd();
			DebugLogger.info("Repository upload finished successfully in "
					+ elapsedSeconds(operationStart) + " s.");

		} catch (SocketTimeoutException | SocketException e1) {
			DebugLogger.error("Repository upload lost its connection after " + elapsedSeconds(operationStart) + " s.", e1);
			connexionService.getConnexionDAOs().get(0)
					.updateObserverUploadConnectionLost();
		} catch (Exception e2) {
			DebugLogger.error("Repository upload failed after " + elapsedSeconds(operationStart) + " s.", e2);
			// e2.printStackTrace();
			List<Exception> errors = new ArrayList<Exception>();
			errors.add(e2);
			executeError(errors);
		}
	}

	private static String elapsedSeconds(long startNanos) {
		return String.format(java.util.Locale.ROOT, "%.3f", (System.nanoTime() - startNanos) / 1_000_000_000.0);
	}

	private void executEnd() {
		observerEndUpload.end();
	}

	private void executeError(List<Exception> errors) {
		observerError.error(errors);
	}

	private void executeConnectionLost() {
		observerConnectionLost.lost();
	}

	private void executeUpdateText(String text) {
		this.observerText.update(text);
	}

	private void executeUpdateTotalSize(long value) {
		totalFilesSize = value;
		this.observerTotalSize.update(totalFilesSize);
	}

	private void executeUpdateSingleSizeProgress(int pourcentage) {
		this.observerCountProgress.update(pourcentage);
	}

	private void executeUpdateUploadedSize(long value) {
		currentSize = uploadedFilesSize + value;
		this.observerUploadedSize.update(currentSize);
	}

	private void executeUpdateTotalSizeProgress(long value) {
		uploadedFilesSize = uploadedFilesSize + value;
		currentSize = uploadedFilesSize;
	}

	private void executeUpdateRemaingTime() {

		double endTime = System.nanoTime();
		double elapsedTime = endTime - startTime;
		long remainingFilesSize = totalFilesSize - currentSize;

		if (currentSize != 0) {
			long remainingTime = (long) ((elapsedTime * Math.pow(10, -9) * remainingFilesSize) / currentSize);
			this.observerRemainingTime.update(remainingTime);
		}
	}

	private void executeUpdateLastIndexUpdated() {
		lastIndexFileUploaded++;
		repositoryService.setLastIndexFileTransfered(repositoryName,
				lastIndexFileUploaded);
	}

	private void executeUpdateSpeed() {

		long endTime = System.nanoTime();
		long delta = endTime - deltaTimeSpeed;

		if (delta > (Math.pow(10, 9)) / 2) {// 0.5s
			long speed = 0;
			for (AbstractConnexionDAO connectionDAO : connexionService.getConnexionDAOs()) {
				speed += connectionDAO.getSpeed();
			}
			deltaTimeSpeed = endTime;
			if (observerSpeed != null) {
				observerSpeed.update(speed);
			}
		}
	}

	public void cancel() {
		if (completed || !cancellationRequested.compareAndSet(false, true)) {
			return;
		}
		canceled = true;
		DebugLogger.info("Repository upload cancellation requested: name=" + repositoryName);
		if (connexionService != null) {
			connexionService.cancel();
		}
	}

	/** Performs cleanup after a terminal UI callback without reporting a user cancellation. */
	public void shutdown() {
		if (!completed && connexionService != null) {
			connexionService.cancel();
		}
	}

	public void addObserverText(ObserverText obs) {
		this.observerText = obs;
	}

	public void addObserverCountProgress(ObserverCountInt obs) {
		this.observerCountProgress = obs;
	}

	public void addObserverUploadedSize(ObserverCountLong obs) {
		this.observerUploadedSize = obs;
	}

	public void addObserverTotalSize(ObserverCountLong obs) {
		this.observerTotalSize = obs;
	}

	public void addObserverSpeed(ObserverCountLong obs) {
		this.observerSpeed = obs;
	}

	public void addObserverRemainingTime(ObserverCountLong obs) {
		this.observerRemainingTime = obs;
	}

	public void addObserverEnd(ObserverEnd obs) {
		this.observerEndUpload = obs;
	}

	public void addObserverError(ObserverError obs) {
		this.observerError = obs;
	}

	public void addObserverConnectionLost(ObserverConnectionLost obs) {
		this.observerConnectionLost = obs;
	}

	private ObserverUpload createSequentialUploadObserver() {
		return new ObserverUpload() {
			@Override
			public void updateTotalSize(long value) {
				executeUpdateTotalSize(value);
			}

			@Override
			public void updateSingleSizeProgress(int percentage, long value) {
				executeUpdateSingleSizeProgress(percentage);
				executeUpdateUploadedSize(value);
				executeUpdateRemaingTime();
			}

			@Override
			public void updateTotalSizeProgress(long value) {
				executeUpdateTotalSizeProgress(value);
			}

			@Override
			public void updateSpeed() {
				executeUpdateSpeed();
			}

			@Override
			public void updateLastIndexFileUploaded() {
				executeUpdateLastIndexUpdated();
			}

			@Override
			public void updateConnectionLost() {
				executeConnectionLost();
			}
		};
	}

	/**
	 * Converts per-connection file progress into one monotonic repository-wide
	 * progress stream. Upload workers deliberately do not persist a resume index
	 * because files may finish out of order.
	 */
	private final class AggregatedUploadObserver implements ObserverUpload {
		private final AbstractConnexionDAO source;
		private long currentFileBytes;

		private AggregatedUploadObserver(AbstractConnexionDAO source) {
			this.source = source;
		}

		@Override
		public void updateTotalSize(long value) {
			executeUpdateTotalSize(value);
		}

		@Override
		public synchronized void updateSingleSizeProgress(int percentage, long value) {
			currentFileBytes = Math.max(0, value);
			parallelCurrentFileBytes.computeIfAbsent(source, key -> new AtomicLong()).set(currentFileBytes);
			executeUpdateAggregateProgress(parallelUploadedBytes.get() + getParallelCurrentFileBytes());
		}

		@Override
		public synchronized void updateTotalSizeProgress(long value) {
			parallelUploadedBytes.addAndGet(Math.max(0, value));
			currentFileBytes = 0;
			parallelCurrentFileBytes.computeIfAbsent(source, key -> new AtomicLong()).set(0);
			executeUpdateAggregateProgress(parallelUploadedBytes.get() + getParallelCurrentFileBytes());
		}

		@Override
		public void updateSpeed() {
			executeUpdateSpeed();
		}

		@Override
		public void updateLastIndexFileUploaded() {
			/* Parallel completion order is not a valid resume index. */
		}

		@Override
		public void updateConnectionLost() {
			executeConnectionLost();
		}
	}

	private long getParallelCurrentFileBytes() {
		long current = 0;
		for (AtomicLong value : parallelCurrentFileBytes.values()) {
			current += Math.max(0, value.get());
		}
		return current;
	}

	private synchronized void executeUpdateAggregateProgress(long uploaded) {
		currentSize = Math.min(uploaded, totalFilesSize);
		int percentage = totalFilesSize <= 0 ? 100 : (int) ((currentSize * 100) / totalFilesSize);
		executeUpdateSingleSizeProgress(percentage);
		if (observerUploadedSize != null) {
			observerUploadedSize.update(currentSize);
		}
		executeUpdateRemaingTime();
	}
}

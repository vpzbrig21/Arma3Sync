
package fr.soe.a3s.service.synchronization;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import fr.soe.a3s.controller.ObserverConnectionLost;
import fr.soe.a3s.controller.ObserverCountInt;
import fr.soe.a3s.controller.ObserverCountLong;
import fr.soe.a3s.controller.ObserverDownload;
import fr.soe.a3s.controller.ObserverEnd;
import fr.soe.a3s.controller.ObserverError;
import fr.soe.a3s.controller.ObserverProceed;
import fr.soe.a3s.controller.ObserverUncompress;
import fr.soe.a3s.dao.connection.AbstractConnexionDAO;
import fr.soe.a3s.domain.AbstractProtocole;
import fr.soe.a3s.service.ConnectionService;
import fr.soe.a3s.service.RepositoryService;
import fr.soe.a3s.utils.UnitConverter;

public class FilesSynchronizationProcessor {

	/* Data */
	private final String repositoryName;
	private final String eventName;
	private long startTime, deltaTimeSpeed;
	/* Services */
	private final RepositoryService repositoryService = new RepositoryService();
	private ConnectionService connexionService;
	/* Managers */
	private final FilesSynchronizationManager filesManager;
	private final FilesSynchronizationReportManager reportManager;
	/* observers */

	private ObserverCountInt observerCountTotalProgress, observerCountSingleProgress, observerActiveConnections,
			observerConnectionWaiting;// null if no recording
	private ObserverCountLong observerTotalSize, observerDownloadedSize, observerSpeed, observerRemainigTime,
			observerDiskUsage;// null if no recording
	private ObserverEnd observerEnd;// not null
	private ObserverError observerError;// not null
	private ObserverConnectionLost observerConnectionLost; // not null
	private ObserverProceed observerProceedUncompress;// not null

	private FilesSynchronizationDiskUsageListerner diskUsageListener;
	private FileSynchronizationConnectionWaitingListener connnectionWaitingListener;

	public FilesSynchronizationProcessor(String repositoryName, String eventName,
			FilesSynchronizationManager filesManager) {
		this.repositoryName = repositoryName;
		this.eventName = eventName;
		this.filesManager = filesManager;
		this.reportManager = new FilesSynchronizationReportManager(repositoryName, filesManager);
		this.diskUsageListener = new FilesSynchronizationDiskUsageListerner();
		this.connnectionWaitingListener = new FileSynchronizationConnectionWaitingListener();
	}

	public void run() {

		try {
			int numberOfServerInfoConnections = repositoryService.getServerInfoNumberOfConnections(repositoryName);
			int numberOfClientConnections = repositoryService.getNumberOfClientConnections(repositoryName);

			if (numberOfServerInfoConnections == 0) {
				numberOfServerInfoConnections = 1;
			}
			if (numberOfClientConnections == 0) {
				numberOfClientConnections = 1;
			}

			int numberOfConnections = 1;
			if (numberOfClientConnections >= numberOfServerInfoConnections) {
				numberOfConnections = numberOfServerInfoConnections;
			} else {
				numberOfConnections = numberOfClientConnections;
			}

			AbstractProtocole protocole = repositoryService.getProtocol(repositoryName);
			connexionService = new ConnectionService(numberOfConnections, protocole);

			for (AbstractConnexionDAO connect : connexionService.getConnexionDAOs()) {
				connect.addObserverDownload(new ObserverDownload() {

					@Override
					public void updateSingleSizeProgress(long value, int pourcentage) {
						executeUpdateSingleSizeProgress(value, pourcentage);
					}

					@Override
					public void updateTotalSizeProgress(long value) {
						executeUpdateTotalSizeProgress(value);
					}

					@Override
					public void updateTotalSize() {
						filesManager.update();
						executeUpdateTotalSize(filesManager.getTotalFilesSize());
					}

					@Override
					public void updateSpeed() {
						executeUpdateSpeed();
					}

					@Override
					public void updateActiveConnections() {
						executeUpdateActiveConnections();
					}

					@Override
					public void end() {
						excuteEnd();
					}

					@Override
					public void error(List<Exception> errors) {
						String message = "Download finished with errors";
						executeError(message, errors);
					}

					@Override
					public void updateCancelTooManyErrors(int value, List<Exception> errors) {
						String message = "Download has been canceled due to too many errors (>" + value + ")";
						executeError(message, errors);
					}

					@Override
					public void updateConnectionLost() {
						executeConnectionLost();
					}
				});
			}

			connexionService.getUnZipFlowProcessor().addObserverUncompress(new ObserverUncompress() {

				@Override
				public void start() {
					executeUpdateUncompressStart();
				}

				@Override
				public void update(int value) {
					executeUpdateUncompress(value);
				}

				@Override
				public void end() {
					excuteEnd();
				}

				@Override
				public void error(List<Exception> errors) {
					String message = "Download finished with error";
					executeError(message, errors);
				}
			});

			String defaultDownloadLocation = repositoryService.getDefaultDownloadLocation(repositoryName, eventName);

			if (defaultDownloadLocation == null || "".equals(defaultDownloadLocation)) {
				throw new IOException("Default destination folder is empty.");
			}

			if (!new File(defaultDownloadLocation).exists()) {
				throw new IOException("Default destination folder does not exists: " + defaultDownloadLocation);
			}

			diskUsageListener.init(new File(defaultDownloadLocation), connexionService);
			diskUsageListener.addObserverDiskUsage(observerDiskUsage);
			diskUsageListener.addObserverCount(observerConnectionWaiting);
			diskUsageListener.start();

//			connnectionWaitingListener.init(connexionService);
//			connnectionWaitingListener.addObserverCount(observerConnectionWaiting);
//			connnectionWaitingListener.start();

			// Start by uncompressing in background already downloaded .pbo.zip
			// files
			connexionService.unZip(repositoryName, filesManager.getDownloadedFiles());

			// Delete extra local file
			connexionService.deleteExtraLocalFiles(repositoryName, filesManager.getListFilesToDelete());

			// Set total file size already downloaded
			executeUpdateTotalSize(filesManager.getTotalFilesSize());

			// Start/Resume synchronization
			startTime = System.nanoTime();
			deltaTimeSpeed = startTime;
			connexionService.synchronize(repositoryName, filesManager.getResumedFiles());

		} catch (Exception e) {
			e.printStackTrace();
			List<Exception> errors = new ArrayList<Exception>();
			errors.add(e);
			String message = "Download finished with error";
			executeError(message, errors);
		}
	}

	private synchronized void executeUpdateSingleSizeProgress(long value, int pourcentage) {

		if (observerCountSingleProgress != null) {
			observerCountSingleProgress.update(pourcentage);
		}

		double endTime = System.nanoTime();
		double elapsedTime = endTime - startTime;

		long remainingFilesSize = filesManager.getTotalFilesSize() - filesManager.getResumedFilesSize() - value;
		long downloadedFilesSize = filesManager.getResumedFilesSize() + value;

		executeUpdateTotalDownloadedSize(downloadedFilesSize);

		if (observerRemainigTime != null && downloadedFilesSize > 0) {
			long remainingTime = (long) ((remainingFilesSize * elapsedTime) / downloadedFilesSize);
			observerRemainigTime.update(remainingTime);
		}
	}

	private synchronized void executeUpdateTotalSizeProgress(long value) {

		if (observerCountTotalProgress != null && filesManager.getTotalFilesSize() > 0) {
			int pourcentage = (int) (((filesManager.getResumedFilesSize()) * 100) / filesManager.getTotalFilesSize());
			observerCountTotalProgress.update(pourcentage);
		}
	}

	private synchronized void executeUpdateTotalSize(long value) {

		if (observerTotalSize != null) {
			observerTotalSize.update(value);
		}
	}

	private synchronized void executeUpdateTotalDownloadedSize(long value) {

		if (observerDownloadedSize != null) {
			observerDownloadedSize.update(value);
		}
	}

	private synchronized void executeUpdateSpeed() {

		long endTime = System.nanoTime();
		long delta = endTime - deltaTimeSpeed;

		if (delta > (Math.pow(10, 9) / 2)) {// 0.5s
			long speed = 0;
			for (AbstractConnexionDAO connect : connexionService.getConnexionDAOs()) {
				if (connect.isActiveConnection()) {
					speed = speed + connect.getSpeed();
				}
			}
			deltaTimeSpeed = endTime;
			if (observerSpeed != null) {
				observerSpeed.update(speed);
			}
			// Report
			if (speed > 0) {
				if (filesManager.getAverageDownloadSpeed() == 0) {
					filesManager.setAverageDownloadSpeed(speed);
				} else {
					filesManager.setAverageDownloadSpeed((filesManager.getAverageDownloadSpeed() + speed) / 2);
				}
			}
		}
	}

	private synchronized void executeUpdateActiveConnections() {

		int activeConnections = 0;
		for (AbstractConnexionDAO connect : connexionService.getConnexionDAOs()) {
			if (connect.isActiveConnection()) {
				activeConnections++;
			}
		}

		double maximumClientDownloadSpeed = repositoryService.getMaximumClientDownloadSpeed(repositoryName);
		if (activeConnections == 0) {
			connexionService.setMaximumClientDownloadSpeed(maximumClientDownloadSpeed);
		} else {
			connexionService.setMaximumClientDownloadSpeed(maximumClientDownloadSpeed / activeConnections);
		}

		if (observerActiveConnections != null) {
			observerActiveConnections.update(activeConnections);
		}

		// Report
		if (activeConnections > filesManager.getMaxActiveconnections()) {
			filesManager.setMaxActiveconnections(activeConnections);
		}
	}

	private void executeUpdateUncompressStart() {

		if (observerProceedUncompress != null) {
			observerProceedUncompress.proceed();
		}
	}

	private void executeUpdateUncompress(int value) {

		if (observerCountTotalProgress != null) {
			observerCountTotalProgress.update(value);
		}
	}

	private void excuteEnd() {

		/* Generate Report */
		String report = reportManager.generateReport("Download finished successfully.");
		repositoryService.setReport(repositoryName, report);

		/* End */
		observerEnd.end();
	}

	private void executeError(String message, List<Exception> errors) {

		/* Generate Report */
		String report = reportManager.generateReport(message, errors);
		repositoryService.setReport(repositoryName, report);

		/* End */
		observerError.error(errors);
	}

	private void executeConnectionLost() {

		/* End */
		observerConnectionLost.lost();
	}

	public void cancel() {

		if (connexionService != null) {
			connexionService.cancel();
		}
		diskUsageListener.stop();
		connnectionWaitingListener.stop();
	}

	/* */

	public void addObserverCountSingleProgress(ObserverCountInt obs) {
		this.observerCountSingleProgress = obs;
	}

	public void addObserverCountTotalProgress(ObserverCountInt obs) {
		this.observerCountTotalProgress = obs;
	}

	public void addObserverTotalSize(ObserverCountLong obs) {
		this.observerTotalSize = obs;
	}

	public void addObserverDownloadedSize(ObserverCountLong obs) {
		this.observerDownloadedSize = obs;
	}

	public void addObserverEnd(ObserverEnd obs) {
		this.observerEnd = obs;
	}

	public void addObserverError(ObserverError obs) {
		this.observerError = obs;
	}

	public void addObserverConnectionLost(ObserverConnectionLost obs) {
		this.observerConnectionLost = obs;
	}

	public void addObserverSpeed(ObserverCountLong obs) {
		this.observerSpeed = obs;
	}

	public void addObserverActiveConnections(ObserverCountInt obs) {
		this.observerActiveConnections = obs;
	}

	public void addObserverRemainingTime(ObserverCountLong obs) {
		this.observerRemainigTime = obs;
	}

	public void addObserverProceedUncompress(ObserverProceed obs) {
		this.observerProceedUncompress = obs;
	}

	public void addObserverDiskUsage(ObserverCountLong obs) {
		this.observerDiskUsage = obs;
	}

	public void addObserverConnectionWaiting(ObserverCountInt observerCountInt) {
		this.observerConnectionWaiting = observerCountInt;
	}
}

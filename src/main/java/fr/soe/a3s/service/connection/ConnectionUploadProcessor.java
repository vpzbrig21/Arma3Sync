package fr.soe.a3s.service.connection;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import fr.soe.a3s.dao.DataAccessConstants;
import fr.soe.a3s.dao.connection.AbstractConnexionDAO;
import fr.soe.a3s.dao.connection.RemoteFile;
import fr.soe.a3s.domain.AbstractProtocole;
import fr.soe.a3s.domain.Http;
import fr.soe.a3s.domain.repository.Repository;
import fr.soe.a3s.dto.sync.SyncTreeNodeDTO;

public class ConnectionUploadProcessor extends AbstractConnectionProcessor {

	private final Repository repository;
	private final List<RemoteFile> missingRemoteFiles;
	private final int lastIndexFileUploaded;
	private final AbstractProtocole protocol;
	private final List<AbstractConnexionDAO> connectionDAOs;

	public ConnectionUploadProcessor(AbstractConnexionDAO abstractConnexionDAO,
			List<SyncTreeNodeDTO> filesToUpload,
			List<RemoteFile> missingRemoteFiles, int lastIndexFileUploaded,
			Repository repository) {
		this(singletonConnection(abstractConnexionDAO), filesToUpload, missingRemoteFiles,
				lastIndexFileUploaded, repository);
	}

	public ConnectionUploadProcessor(List<AbstractConnexionDAO> connectionDAOs,
			List<SyncTreeNodeDTO> filesToUpload,
			List<RemoteFile> missingRemoteFiles, int lastIndexFileUploaded,
			Repository repository) {
		super(firstConnection(connectionDAOs), filesToUpload, repository
				.isUploadCompressedPboFilesOnly(),
				(repository.getProtocol() instanceof Http));
		this.repository = repository;
		this.missingRemoteFiles = missingRemoteFiles == null ? new ArrayList<RemoteFile>() : missingRemoteFiles;
		this.lastIndexFileUploaded = lastIndexFileUploaded;
		this.protocol = repository.getUploadProtocole();
		this.connectionDAOs = new ArrayList<AbstractConnexionDAO>(connectionDAOs);
	}

	private static List<AbstractConnexionDAO> singletonConnection(AbstractConnexionDAO connectionDAO) {
		List<AbstractConnexionDAO> connections = new ArrayList<AbstractConnexionDAO>();
		connections.add(connectionDAO);
		return connections;
	}

	private static AbstractConnexionDAO firstConnection(List<AbstractConnexionDAO> connections) {
		if (connections == null || connections.isEmpty() || connections.get(0) == null) {
			throw new IllegalArgumentException("At least one upload connection is required.");
		}
		return connections.get(0);
	}

	public void run() throws IOException {
		extract();
		remoteFiles.addAll(missingRemoteFiles);

		/* Resume from the last completed prefix. Parallel workers intentionally do
		 * not update this index because completion order is not deterministic. */
		int maxIndex = remoteFiles.size() - 1;
		if (lastIndexFileUploaded < maxIndex) {
			List<RemoteFile> resumedFiles = new ArrayList<RemoteFile>();
			for (int i = Math.max(0, lastIndexFileUploaded); i <= maxIndex; i++) {
				resumedFiles.add(remoteFiles.get(i));
			}
			remoteFiles.clear();
			remoteFiles.addAll(resumedFiles);
		}

		long totalFilesSize = calculateTotalFilesSize();
		abstractConnexionDAO.updateObserverUploadTotalSize(totalFilesSize);

		if (connectionDAOs.size() > 1) {
			runParallelFileUpload();
		} else {
			runSequentialFileUpload(abstractConnexionDAO, remoteFiles);
		}

		repository.getLocalServerInfo().setCompressedPboFilesOnly(
				repository.isUploadCompressedPboFilesOnly());
		if (!abstractConnexionDAO.isCanceled()) {
			uploadMetadata(abstractConnexionDAO);
		}
	}

	private long calculateTotalFilesSize() throws FileNotFoundException {
		long totalFilesSize = 0;
		for (RemoteFile remoteFile : remoteFiles) {
			if (!remoteFile.isDirectory()) {
				File file = localFile(remoteFile);
				if (!file.exists()) {
					throw new FileNotFoundException("File not found: " + file.getAbsolutePath());
				}
				totalFilesSize += file.length();
			}
		}
		return totalFilesSize;
	}

	private File localFile(RemoteFile remoteFile) {
		return new File(repository.getPath() + remoteFile.getRelativeFilePath());
	}

	private void runSequentialFileUpload(AbstractConnexionDAO connection,
			List<RemoteFile> files) throws IOException {
		for (RemoteFile remoteFile : files) {
			if (connection.isCanceled()) {
				return;
			}
			connection.updateObserverText("Uploading file: " + remoteFile.getRelativeFilePath());
			connection.uploadFile(protocol, localFile(remoteFile), remoteFile);
		}
	}

	private void runParallelFileUpload() throws IOException {
		/* Directory creation is done before concurrent transfers. This prevents
		 * workers from racing while creating the same remote directory tree. */
		List<RemoteFile> files = new ArrayList<RemoteFile>();
		for (RemoteFile remoteFile : remoteFiles) {
			if (remoteFile.isDirectory()) {
				if (abstractConnexionDAO.isCanceled()) {
					return;
				}
				abstractConnexionDAO.updateObserverText("Creating directory: "
						+ remoteFile.getRelativeFilePath());
				abstractConnexionDAO.uploadFile(protocol, localFile(remoteFile), remoteFile);
			} else {
				files.add(remoteFile);
			}
		}

		if (files.isEmpty() || abstractConnexionDAO.isCanceled()) {
			return;
		}

		List<AbstractConnexionDAO> startedWorkerConnections = new ArrayList<AbstractConnexionDAO>();
		ExecutorService executor = null;
		try {
			/* Connection 0 owns the session opened by ConnectionService. Every other
			 * worker receives an independent FTP/SFTP session. */
			for (int i = 1; i < connectionDAOs.size(); i++) {
				AbstractConnexionDAO connection = connectionDAOs.get(i);
				connection.beginUploadSession(protocol);
				startedWorkerConnections.add(connection);
			}

			executor = Executors.newFixedThreadPool(Math.min(connectionDAOs.size(), files.size()));
			List<List<RemoteFile>> assignments = createAssignments(files, connectionDAOs.size());
			List<Future<Void>> futures = new ArrayList<Future<Void>>();
			for (int i = 0; i < assignments.size(); i++) {
				final AbstractConnexionDAO connection = connectionDAOs.get(i);
				final List<RemoteFile> assignedFiles = assignments.get(i);
				if (!assignedFiles.isEmpty()) {
					futures.add(executor.submit(() -> {
						runSequentialFileUpload(connection, assignedFiles);
						return null;
					}));
				}
			}

			for (Future<Void> future : futures) {
				try {
					future.get();
				} catch (InterruptedException e) {
					cancelAllConnections();
					Thread.currentThread().interrupt();
					throw new IOException("Parallel repository upload was interrupted.", e);
				} catch (ExecutionException e) {
					cancelAllConnections();
					Throwable cause = e.getCause();
					if (cause instanceof IOException) {
						throw (IOException) cause;
					}
					throw new IOException("Parallel repository upload failed.", cause);
				}
			}
		} finally {
			if (executor != null) {
				executor.shutdownNow();
			}
			for (AbstractConnexionDAO connection : startedWorkerConnections) {
				connection.endUploadSession();
			}
		}
	}

	private List<List<RemoteFile>> createAssignments(List<RemoteFile> files, int workerCount) {
		List<List<RemoteFile>> assignments = new ArrayList<List<RemoteFile>>(workerCount);
		for (int i = 0; i < workerCount; i++) {
			assignments.add(new ArrayList<RemoteFile>());
		}
		for (int i = 0; i < files.size(); i++) {
			assignments.get(i % workerCount).add(files.get(i));
		}
		return assignments;
	}

	private void cancelAllConnections() {
		for (AbstractConnexionDAO connection : connectionDAOs) {
			connection.cancel();
		}
	}

	private void uploadMetadata(AbstractConnexionDAO connection) throws IOException {
		connection.uploadA3SObject(repository.getLocalSync(), repository.getUploadProtocole(),
				DataAccessConstants.SYNC_FILE_NAME, repository.getName());
		connection.uploadA3SObject(repository.getLocalServerInfo(), repository.getUploadProtocole(),
				DataAccessConstants.SERVERINFO_FILE_NAME, repository.getName());
		connection.uploadA3SObject(repository.getLocalChangelogs(), repository.getUploadProtocole(),
				DataAccessConstants.CHANGELOGS_FILE_NAME, repository.getName());
		connection.uploadA3SObject(repository.getLocalAutoConfig(), repository.getUploadProtocole(),
				DataAccessConstants.AUTOCONFIG_FILE_NAME, repository.getName());
		if (repository.getLocalEvents() != null) {
			connection.uploadA3SObject(repository.getLocalEvents(), repository.getUploadProtocole(),
				DataAccessConstants.EVENTS_FILE_NAME, repository.getName());
		}
	}
}

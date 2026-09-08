package fr.soe.a3s.service.connection;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import fr.soe.a3s.dao.connection.AbstractConnexionDAO;
import fr.soe.a3s.dao.connection.RemoteFile;
import fr.soe.a3s.domain.AbstractProtocole;
import fr.soe.a3s.dto.sync.SyncTreeNodeDTO;

public class ConnectionCheckProcessor extends AbstractConnectionProcessor {

	private final List<Exception> errors;
	private final List<RemoteFile> missingRemoteFiles;
	private final List<AbstractConnexionDAO> connectionDAOs;
	private int count, totalCount;
	private int lastReportedProgress = -1;
	private final AtomicInteger nextFileIndex = new AtomicInteger();
	private volatile IOException firstError;
	private AbstractProtocole protocol = null;

	public ConnectionCheckProcessor(AbstractConnexionDAO abstractConnexionDAO,
			List<SyncTreeNodeDTO> filesToCheck,
			boolean isCompressedPboFilesOnly, boolean withzsync,
			AbstractProtocole protocol) {
			super(abstractConnexionDAO, filesToCheck, isCompressedPboFilesOnly,
				withzsync);
		this.connectionDAOs = Collections.singletonList(abstractConnexionDAO);
		this.errors = Collections.synchronizedList(new ArrayList<Exception>());
		this.missingRemoteFiles = Collections.synchronizedList(new ArrayList<RemoteFile>());
		this.protocol = protocol;
	}

	public ConnectionCheckProcessor(List<AbstractConnexionDAO> connectionDAOs,
			List<SyncTreeNodeDTO> filesToCheck,
			boolean isCompressedPboFilesOnly, boolean withzsync,
			AbstractProtocole protocol) {
		super(firstConnection(connectionDAOs), filesToCheck, isCompressedPboFilesOnly,
				withzsync);
		this.connectionDAOs = new ArrayList<AbstractConnexionDAO>(connectionDAOs);
		this.errors = Collections.synchronizedList(new ArrayList<Exception>());
		this.missingRemoteFiles = Collections.synchronizedList(new ArrayList<RemoteFile>());
		this.protocol = protocol;
	}

	private static AbstractConnexionDAO firstConnection(List<AbstractConnexionDAO> connectionDAOs) {
		if (connectionDAOs == null || connectionDAOs.isEmpty() || connectionDAOs.get(0) == null) {
			throw new IllegalArgumentException("At least one connection is required.");
		}
		return connectionDAOs.get(0);
	}

	public void run() throws IOException {

		extract();
		// HTTP repositories generally do not expose directory listings. A
		// directory is created implicitly when its files are uploaded, so only
		// real files can be verified through the file-exists operation.
		remoteFiles.removeIf(RemoteFile::isDirectory);

		this.totalCount = this.remoteFiles.size();
		this.count = 0;
		this.lastReportedProgress = -1;
		this.nextFileIndex.set(0);
		this.firstError = null;

		if (remoteFiles.isEmpty()) {
			return;
		}

		int workerCount = Math.min(connectionDAOs.size(), remoteFiles.size());
		ExecutorService executor = Executors.newFixedThreadPool(workerCount);
		List<Callable<Void>> workers = new ArrayList<Callable<Void>>(workerCount);
		for (AbstractConnexionDAO connectionDAO : connectionDAOs.subList(0, workerCount)) {
			workers.add(() -> checkFiles(connectionDAO));
		}
		try {
			executor.invokeAll(workers);
		} catch (InterruptedException e) {
			cancelConnections();
			Thread.currentThread().interrupt();
			throw new IOException("Repository check was interrupted.", e);
		} finally {
			executor.shutdownNow();
		}

		if (firstError != null) {
			throw firstError;
		}
	}

	private Void checkFiles(AbstractConnexionDAO connectionDAO) {
		while (!connectionDAO.isCanceled() && firstError == null) {
			int index = nextFileIndex.getAndIncrement();
			if (index >= remoteFiles.size()) {
				break;
			}

			RemoteFile remoteFile = remoteFiles.get(index);
			try {
				boolean found = connectionDAO.fileExists(protocol, remoteFile);
				if (!found) {
					FileNotFoundException error = new FileNotFoundException(
							"File not found on repository: " + remoteFile.getRelativeFilePath());
					missingRemoteFiles.add(remoteFile);
					synchronized (errors) {
						errors.add(error);
						connectionDAOs.get(0).updateObserverCountErrors(errors.size());
					}
				}
				increment();
			} catch (IOException e) {
				if (!connectionDAO.isCanceled()) {
					firstError = e;
					cancelConnections();
				}
				break;
			}
		}
		return null;
	}

	private void cancelConnections() {
		for (AbstractConnexionDAO connectionDAO : connectionDAOs) {
			connectionDAO.cancel();
		}
	}

	private synchronized void increment() {
		count++;
		int value = count * 100 / totalCount;
		if (value != lastReportedProgress) {
			lastReportedProgress = value;
			connectionDAOs.get(0).updateObserverCount(value);
		}
	}

	public List<RemoteFile> getMissingRemoteFiles() {
		return missingRemoteFiles;
	}

	public List<Exception> getErrors() {
		return errors;
	}
}

package fr.soe.a3s.service.connection;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import fr.soe.a3s.controller.ObserverProceed;
import fr.soe.a3s.dao.DataAccessConstants;
import fr.soe.a3s.dao.connection.AbstractConnexionDAO;
import fr.soe.a3s.domain.repository.Repository;
import fr.soe.a3s.dto.sync.SyncTreeLeafDTO;

public class ConnectionCompletionProcessor implements DataAccessConstants {

	private final List<AbstractConnexionDAO> connectionDAOs;
	private final Stack<SyncTreeLeafDTO> downloadFilesStack;
	private final List<Exception> completionErrors;
	private final Repository repository;
	private int count, totalCount;
	private boolean terminated;

	public ConnectionCompletionProcessor(List<SyncTreeLeafDTO> filesToCheck, List<AbstractConnexionDAO> httpDAOs,
			Repository repository) {
		this.connectionDAOs = httpDAOs;
		this.downloadFilesStack = new Stack<SyncTreeLeafDTO>();
		this.downloadFilesStack.addAll(filesToCheck);
		this.completionErrors = new ArrayList<Exception>();
		this.repository = repository;
		this.totalCount = downloadFilesStack.size();
		this.count = 0;
		this.terminated = false;
	}

	public void run() throws IOException {

		if (downloadFilesStack.isEmpty()) {
			terminate(connectionDAOs.get(0));
		} else {
			for (final AbstractConnexionDAO connectionDAO : connectionDAOs) {
				if (!downloadFilesStack.isEmpty()) {// nb files < nb connections
					try {
						connectionDAO.checkConnection(repository.getProtocol());
						final Thread t = new Thread(new Runnable() {
							@Override
							public void run() {
								getFileCompletion(connectionDAO);
							}
						});
						t.start();
					} catch (IOException e) {
						boolean isDowloading = false;
						for (final AbstractConnexionDAO cDAO : connectionDAOs) {
							if (cDAO.isActiveConnection()) {
								isDowloading = true;
								break;
							}
						}
						if (!isDowloading) {
							throw e;
						}
					}
				}
			}
		}
	}

	private void getFileCompletion(AbstractConnexionDAO connexionDAO) {

		while (!connexionDAO.isCanceled() && !isEmptyDownloadFilesStack()) {
			final SyncTreeLeafDTO leaf = popDownloadFilesStack();
			if (leaf != null) {
				try {
					connexionDAO.setActiveConnection(true);
					double complete = connexionDAO.getFileCompletion(repository, leaf);
					leaf.setComplete(complete);
					if (!connexionDAO.isCanceled()) {
						increment();
					}
				} catch (IOException e) {
					if (!connexionDAO.isCanceled()) {
						completionErrors.add(e);
					}
				} finally {
					connexionDAO.setActiveConnection(false);
					if (completionErrors.size() != 0) {
						break;
					}
				}
			}
		}

		// no more file to check for this DAO
		terminate(connexionDAO);
	}

	private void terminate(AbstractConnexionDAO connectionDAO) {

		if (!terminated) {
			if (this.completionErrors.size() > 0) {
				terminated = true;
				connectionDAO.updateObserverError(this.completionErrors);
			} else {
				// Check if there is no more active connections
				boolean downloadFinished = true;
				for (final AbstractConnexionDAO cDAO : connectionDAOs) {
					if (cDAO.isActiveConnection()) {
						downloadFinished = false;
						break;
					}
				}
				if (downloadFinished) {
					terminated = true;
					connectionDAO.updateObserverEnd();
				}
			}
		}
	}

	private synchronized boolean isEmptyDownloadFilesStack() {
		return downloadFilesStack.isEmpty();
	}

	private synchronized SyncTreeLeafDTO popDownloadFilesStack() {

		if (downloadFilesStack.isEmpty()) {
			return null;
		} else {
			return downloadFilesStack.pop();
		}
	}

	private synchronized void increment() {

		count++;
		int value = count * 100 / totalCount;
		connectionDAOs.get(0).updateObserverCount(value);
	}
}

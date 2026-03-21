package fr.soe.a3s.service.synchronization;

import java.util.ArrayList;
import java.util.List;

import fr.soe.a3s.controller.ObserverCountInt;
import fr.soe.a3s.controller.ObserverError;
import fr.soe.a3s.domain.AbstractProtocole;
import fr.soe.a3s.dto.sync.SyncTreeDirectoryDTO;
import fr.soe.a3s.exception.remote.RemoteEventsFileNotFoundException;
import fr.soe.a3s.exception.remote.RemoteServerInfoFileNotFoundException;
import fr.soe.a3s.exception.remote.RemoteSyncFileNotFoundException;
import fr.soe.a3s.service.ConnectionService;
import fr.soe.a3s.service.RepositoryService;

public class FilesCheckProcessor {

	/* Data */
	private final String repositoryName;
	private final String eventName;
	/* Services */
	private ConnectionService connexionService;
	private final RepositoryService repositoryService = new RepositoryService();;
	/* observers */
	private ObserverCountInt observerCount;// null for no recording
	private ObserverError observerError;// not null

	public FilesCheckProcessor(String repositoryName, String eventName) {
		this.repositoryName = repositoryName;
		this.eventName = eventName;// may be null
	}

	public SyncTreeDirectoryDTO run() {

		SyncTreeDirectoryDTO parent = null;

		try {
			AbstractProtocole protocole = repositoryService
					.getProtocol(repositoryName);
			connexionService = new ConnectionService(protocole);
			connexionService.checkRepository(repositoryName);

			if (repositoryService.getSync(repositoryName) == null) {
				throw new RemoteSyncFileNotFoundException();
			}

			if (repositoryService.getServerInfo(repositoryName) == null) {
				throw new RemoteServerInfoFileNotFoundException();
			}

			if (eventName!=null
					&& repositoryService.getEvents(repositoryName) == null) {
				throw new RemoteEventsFileNotFoundException();
			}

			repositoryService.updateRepository(repositoryName);

			repositoryService.getRepositorySHA1Processor().addObserverCount(
					new ObserverCountInt() {
						@Override
						public void update(int value) {
							executeUpdate(value);
						}
					});

			parent = repositoryService.checkForAddons(repositoryName,eventName);
			repositoryService.write(repositoryName);// save SHA1 computations

		} catch (Exception e) {
			List<Exception> errors = new ArrayList<Exception>();
			errors.add(e);
			observerError.error(errors);
		}
		return parent;
	}

	private void executeUpdate(int value) {

		if (observerCount != null) {
			observerCount.update(value);
		}
	}

	public void cancel() {

		if (connexionService != null) {
			connexionService.cancel();
		}
		repositoryService.cancel();
	}

	public void addObserverCount(ObserverCountInt obs) {
		this.observerCount = obs;
	}

	public void addObserverError(ObserverError obs) {
		this.observerError = obs;
	}
}

package fr.soe.a3s.ui.main.tasks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import fr.soe.a3s.constant.RepositoryStatus;
import fr.soe.a3s.controller.ObserverEnd;
import fr.soe.a3s.domain.AbstractProtocole;
import fr.soe.a3s.dto.RepositoryDTO;
import fr.soe.a3s.dto.ServerInfoDTO;
import fr.soe.a3s.service.ConnectionService;
import fr.soe.a3s.service.RepositoryService;
import fr.soe.a3s.ui.Facade;
import fr.soe.a3s.ui.UIConstants;
import fr.soe.a3s.ui.main.dialogs.InfoUpdatedRepositoryDialog;
import fr.soe.a3s.ui.repository.RepositoryPanel;

public class TaskCheckRepositories extends TimerTask implements UIConstants {

	private static final int MAX_CONCURRENT_REPOSITORY_CHECKS = 4;

	private final Facade facade;
	/* Services */
	private final RepositoryService repositoryService = new RepositoryService();
	/* Prevents the same remote revision from generating a notification every timer cycle. */
	private final Map<String, Integer> notifiedRepositoryRevisions = new HashMap<String, Integer>();

	public TaskCheckRepositories(Facade facade) {
		this.facade = facade;
	}

	@Override
	public void run() {

		/* Check repositories */

		System.out.println("Checking repositories...");

		List<RepositoryDTO> list = repositoryService.getRepositories();

		List<Callable<Integer>> callables = new ArrayList<Callable<Integer>>();
		for (final RepositoryDTO repositoryDTO : list) {
			Callable<Integer> c = new Callable<Integer>() {
				@Override
				public Integer call() {
					try {
						AbstractProtocole protocole = repositoryService.getProtocol(repositoryDTO.getName());
						ConnectionService connexionService = new ConnectionService(protocole);
						connexionService.checkRepository(repositoryDTO.getName());
					} catch (Exception e) {
						System.out.println("Error when checking repository " + repositoryDTO.getName() + ":" + "\n"
								+ e.getMessage());
					}
					return 0;
				}
			};
			callables.add(c);
		}

		int workerCount = Math.max(1, Math.min(MAX_CONCURRENT_REPOSITORY_CHECKS, list.size()));
		ExecutorService executor = Executors.newFixedThreadPool(workerCount);
		try {
			executor.invokeAll(callables);
		} catch (InterruptedException e) {
			System.out.println("Checking repositories has been anormaly interrupted.");
		}

		executor.shutdownNow();

		System.out.println("Checking repositories done.");

		/* Update local repositories info with remote a3s folder content changed */
		for (RepositoryDTO repositoryDTO : list) {
			repositoryService.updateRepository(repositoryDTO.getName());
		}

		facade.getMainPanel().updateTabs(OP_REPOSITORY_CHANGED);

		/* Get updated repositories */

		final List<RepositoryDTO> updatedRepositoryDTOs = new ArrayList<RepositoryDTO>();

		for (RepositoryDTO repositoryDTO : list) {
			RepositoryStatus repositoryStatus = repositoryService.getRepositorySyncStatus(repositoryDTO.getName());
			if (repositoryStatus.equals(RepositoryStatus.UPDATED)) {
				updatedRepositoryDTOs.add(repositoryDTO);
			} else {
				// A successful synchronization makes this revision eligible for a
				// future notification again when the repository changes later.
				notifiedRepositoryRevisions.remove(repositoryDTO.getName());
			}
		}

		if (!updatedRepositoryDTOs.isEmpty()) {
			// Show info on concole
			String message = "The following repositories have been updated: ";
			for (RepositoryDTO updatedRepositoryDTO : updatedRepositoryDTOs) {
				message = message + "\n" + updatedRepositoryDTO.getName();
			}
			System.out.println(message);
		}

		/* Run auto update on repositories */

		final List<RepositoryDTO> autoUpdateRepositoryDTOs = new ArrayList<RepositoryDTO>();

		for (RepositoryDTO updatedRepositoryDTO : updatedRepositoryDTOs) {
			if (updatedRepositoryDTO.isAuto()) {
				autoUpdateRepositoryDTOs.add(updatedRepositoryDTO);
			}
		}

		for (final RepositoryDTO autoUpdateRepositoryDTO : autoUpdateRepositoryDTOs) {

			System.out.println("Auto updating repository: " + autoUpdateRepositoryDTO.getName());

			RepositoryPanel repositoryPanel = facade.getMainPanel().openRepository(autoUpdateRepositoryDTO.getName(),
					null, false, false);
			if (repositoryPanel != null) {
				ObserverEnd obs = new ObserverEnd() {
					@Override
					public void end() {
						facade.getMainPanel().closeRepository(autoUpdateRepositoryDTO.getName(), null, true);
					}
				};
				repositoryPanel.autoUpdate(autoUpdateRepositoryDTO.getName(), null, obs);
			}
		}

		/* Get notified repositories */

		final List<RepositoryDTO> notifyRepositoryDTOs = new ArrayList<RepositoryDTO>();

		for (RepositoryDTO updatedRepositoryDTO : updatedRepositoryDTOs) {
			/*
			 * An automatic update is already handled by the update workflow. The
			 * old implementation notified from the pre-update snapshot immediately
			 * after starting that asynchronous workflow, which made a completed
			 * update look pending again. Manual-notify repositories are handled here.
			 */
			Integer serverRevision = null;
			try {
				ServerInfoDTO serverInfo = repositoryService.getServerInfo(updatedRepositoryDTO.getName());
				if (serverInfo != null) serverRevision = serverInfo.getRevision();
			} catch (Exception ignored) {
				// The repository status is already known as UPDATED. If the revision
				// cannot be read, retain the legacy notification behavior.
			}
			if (shouldNotify(updatedRepositoryDTO, serverRevision, notifiedRepositoryRevisions)) {
				notifyRepositoryDTOs.add(updatedRepositoryDTO);
			}
		}

		if (!notifyRepositoryDTOs.isEmpty()) {
			// Both the tray message and the dialog must honor NOTIFY and the
			// per-revision de-duplication above.
			facade.getMainPanel().displayMessageToSystemTray("Repositories updates!");
			InfoUpdatedRepositoryDialog infoUpdatedRepositoryPanel = new InfoUpdatedRepositoryDialog(facade);
			infoUpdatedRepositoryPanel.init(notifyRepositoryDTOs);
			if (!facade.getMainPanel().isToTray()) {
				facade.getMainPanel().showSyncPanel();
				infoUpdatedRepositoryPanel.setVisible(true);
			}
		}
	}

	/**
	 * Returns whether a notification should be emitted for the current remote
	 * revision. The tracker is intentionally in-memory: after restarting the
	 * application, an unresolved update is reported once again.
	 */
	static boolean shouldNotify(RepositoryDTO repositoryDTO, Integer serverRevision,
			Map<String, Integer> notifiedRevisions) {
		if (repositoryDTO == null || !repositoryDTO.isNotify() || repositoryDTO.isAuto()) return false;
		if (serverRevision == null) return true;

		String repositoryName = repositoryDTO.getName();
		Integer previousRevision = notifiedRevisions.get(repositoryName);
		if (previousRevision != null && previousRevision.intValue() == serverRevision.intValue()) {
			return false;
		}
		notifiedRevisions.put(repositoryName, serverRevision);
		return true;
	}
}

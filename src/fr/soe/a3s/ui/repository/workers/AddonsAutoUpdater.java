package fr.soe.a3s.ui.repository.workers;

import java.util.List;

import fr.soe.a3s.controller.ObserverConnectionLost;
import fr.soe.a3s.controller.ObserverEnd;
import fr.soe.a3s.controller.ObserverError;
import fr.soe.a3s.controller.ObserverStart;
import fr.soe.a3s.ui.repository.DownloadPanel;

public class AddonsAutoUpdater extends Thread {

	private final String repositoryName;
	private DownloadPanel downloadPanel;
	/* Workers */
	private AddonsChecker addonsChecker;
	private AddonsDownloader addonsDownloader;
	/* Tests */
	private boolean check1IsDone, check2IsDone;
	/* observers */
	private ObserverStart observerCheckStart,observerDownloadStart;
	private ObserverEnd observerCheckEnd,observerDownloadEnd;
	private ObserverError observerDownloadError;
	private ObserverConnectionLost observerConnectionLost;

	public AddonsAutoUpdater(String repositoryName) {
		this.repositoryName = repositoryName;
	}

	public AddonsAutoUpdater(String repositoryName, AddonsChecker addonsChecker, AddonsDownloader addonsDownloader,
			DownloadPanel downloadPanel) {
		this.repositoryName = repositoryName;
		this.addonsChecker = addonsChecker;
		this.addonsDownloader = addonsDownloader;
		this.downloadPanel = downloadPanel;
	}

	@Override
	public void run() {

		System.out.println("Auto updating with repository: " + repositoryName);

		check1IsDone = false;
		check2IsDone = false;

		addonsChecker.addObserverEnd(new ObserverEnd() {
			@Override
			public void end() {
				addonsCheckerEnd();
			}
		});
		addonsDownloader.addObserverEnd(new ObserverEnd() {
			@Override
			public void end() {
				addonsDownloaderEnd();
			}
		});
		addonsChecker.addObserverError(new ObserverError() {
			@Override
			public void error(List<Exception> errors) {
				observerDownloadError.error(errors);
			}
		});
		addonsDownloader.addObserverError(new ObserverError() {
			@Override
			public void error(List<Exception> errors) {
				observerDownloadError.error(errors);
			}
		});
		addonsDownloader.addObserverConnectionLost(new ObserverConnectionLost() {
			@Override
			public void lost() {
				observerConnectionLost.lost();
			}
		});

		observerCheckStart.start();
		addonsChecker.run();
	}

	private void addonsCheckerEnd() {

		observerCheckEnd.end();
		if (!check1IsDone) {
			check1IsDone = true;
		} else if (!check2IsDone) {
			check2IsDone = true;
		}

		if (check1IsDone && check2IsDone) {
			System.out.println("Synchronization with repository: " + repositoryName + " finished.");
			observerDownloadEnd.end();
		} else if (check1IsDone && !check2IsDone) {
			downloadPanel.updateArbre(addonsChecker.getParent());
			downloadPanel.getCheckBoxSelectAll().setSelected(true);
			downloadPanel.checkBoxSelectAllPerformed();
			observerDownloadStart.start();
			addonsDownloader.run();
		}
	}

	private void addonsDownloaderEnd() {
		observerCheckStart.start();
		addonsChecker.run();
	}
	
	public void addObserverCheckStart(ObserverStart obs) {
		this.observerCheckStart = obs;
	}
	
	public void addObserverCheckEnd(ObserverEnd obs) {
		this.observerCheckEnd = obs;
	}

	public void addObserverDownloadStart(ObserverStart obs) {
		this.observerDownloadStart = obs;
	}

	public void addObserverDownloadEnd(ObserverEnd obs) {
		this.observerDownloadEnd = obs;
	}

	public void addObserverDownloadError(ObserverError obs) {
		this.observerDownloadError = obs;
	}

	public void addObserverConnectionLost(ObserverConnectionLost obs) {
		this.observerConnectionLost = obs;
	}
}

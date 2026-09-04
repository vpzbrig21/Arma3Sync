package fr.soe.a3s.service.synchronization;

import java.util.List;

import fr.soe.a3s.controller.ObserverCountInt;
import fr.soe.a3s.dao.connection.AbstractConnexionDAO;
import fr.soe.a3s.service.ConnectionService;

public class FileSynchronizationConnectionWaitingListener {

	private boolean canceled;

	private ObserverCountInt observer;

	private ConnectionService connexionService;

	public FileSynchronizationConnectionWaitingListener() {
		canceled = false;
		observer = null;
		connexionService = null;
	}

	public void init(ConnectionService connexionService) {
		this.connexionService = connexionService;
	}

	public void start() {

		final Thread t = new Thread(new Runnable() {
			@Override
			public void run() {

				long startTime = System.nanoTime();
				int waitTime = 0;

				while (!canceled) {
					try {
						Thread.sleep(1000);
						List<AbstractConnexionDAO> list = connexionService.getConnexionDAOs();
						boolean isActive = false;
						long speed = 0;
						for (AbstractConnexionDAO cDAO : list) {
							if (cDAO.isActiveConnection()) {
								isActive = true;
							}
							speed = speed + cDAO.getSpeed();
						}
						if (!isActive) {
							waitTime = 0;
						} else {
							if (speed > 0) {
								waitTime = 0;
							} else {
								waitTime++;// +1s
							}
						}
						// if (waitTime >= 15) {// 15s
						observer.update(waitTime);
						// }
					} catch (InterruptedException e) {
						e.printStackTrace();
						canceled = true;
					}
				}
			}
		});
		t.start();
	}

	public void stop() {
		this.canceled = true;
	}

	public void addObserverCount(ObserverCountInt observer) {
		this.observer = observer;
	}
}

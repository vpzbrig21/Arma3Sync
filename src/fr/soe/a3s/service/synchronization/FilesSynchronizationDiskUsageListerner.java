package fr.soe.a3s.service.synchronization;

import java.io.File;
import java.util.List;

import fr.soe.a3s.controller.ObserverCountInt;
import fr.soe.a3s.controller.ObserverCountLong;
import fr.soe.a3s.dao.connection.AbstractConnexionDAO;
import fr.soe.a3s.service.ConnectionService;

public class FilesSynchronizationDiskUsageListerner {

	private File file;
	private boolean canceled;
	private ObserverCountLong observerDiskUsage;
	private long diskUsage;
	private ConnectionService connexionService;
	private ObserverCountInt observerConnectionWaiting;

	public FilesSynchronizationDiskUsageListerner() {
		this.file = null;
		this.canceled = false;
		diskUsage = 0;
		connexionService = null;
	}

	public void init(File file, ConnectionService connexionService) {
		this.file = file;
		this.connexionService = connexionService;
	}

	public void start() {

		if (observerDiskUsage == null || file == null) {
			return;
		}

		final Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				while (!canceled) {
					try {
						long time1 = System.nanoTime();
						long freeSpace1 = file.getFreeSpace();

						Thread.sleep(500);

						long time2 = System.nanoTime();
						long freeSpace2 = file.getFreeSpace();

						double deltaTime = (time2 - time1) * Math.pow(10, -9);// s

						double deltaBytes = freeSpace2 - freeSpace1;// Bytes

						if (deltaBytes > 0) {// deleting files
							deltaBytes = 0;
						} else {
							deltaBytes = -deltaBytes;// writing files
						}

						diskUsage = (long) (deltaBytes / deltaTime);
						observerDiskUsage.update(diskUsage);

					} catch (InterruptedException e) {
						e.printStackTrace();
						canceled = true;
					}
				}
			}
		});
		t.start();

		final Thread t2 = new Thread(new Runnable() {
			@Override
			public void run() {

				long startTime = System.nanoTime();
				int waitTime = 0;

				while (!canceled) {
					try {
						Thread.sleep(1000);
						List<AbstractConnexionDAO> list = connexionService.getConnexionDAOs();
						boolean isActive = false;
						for (AbstractConnexionDAO cDAO : list) {
							if (cDAO.isActiveConnection()) {
								isActive = true;
								break;
							}
						}
						if (!isActive) {
							waitTime = 0;
						} else {
							if (!connexionService.getUnZipFlowProcessor().uncompressionIsFinished()) {
								waitTime = 0;
							} else if (diskUsage > 0) {
								waitTime = 0;
							} else {
								waitTime++;// +1s
							}
						}
						observerConnectionWaiting.update(waitTime);
					} catch (InterruptedException e) {
						e.printStackTrace();
						canceled = true;
					}
				}
			}
		});
		t2.start();
	}

	public void stop() {
		this.canceled = true;
	}

	public void addObserverDiskUsage(ObserverCountLong obs) {
		this.observerDiskUsage = obs;
	}

	public void addObserverCount(ObserverCountInt observer) {
		this.observerConnectionWaiting = observer;
	}
}

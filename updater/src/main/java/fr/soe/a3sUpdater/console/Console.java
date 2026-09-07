package fr.soe.a3sUpdater.console;

import fr.soe.a3sUpdater.service.Service;
import fr.soe.a3sUpdater.model.UpdateSource;

public final class Console {
    private final boolean devMode;
    private final UpdateSource source;

    public Console(boolean devMode) { this(devMode, UpdateSource.CONFIGURED); }

    public Console(boolean devMode, UpdateSource source) {
        this.devMode = devMode;
        this.source = source;
    }

    public int execute() {
        Service service = new Service();
        service.setSourcePreference(source);
        try {
            String targetVersion = service.getManifest(devMode).version();
            if (!service.isUpdateAvailable(devMode)) {
                System.out.println("No new update available.");
                return 2;
            }
            System.out.println("Updating ArmA3Sync to version " + targetVersion + "...");
            long total = service.getSize(devMode);
            service.setDownload();
            service.addDownloadObserver(value -> printProgress(value, total));
            service.download(devMode);
            System.out.println();
            System.out.println("Processing update...");
            service.install();
            System.out.println("ArmA3Sync has been successfully updated to version " + targetVersion + ".");
            return 0;
        } catch (Exception exception) {
            System.err.println("An error occurred: " + exception.getMessage());
            System.err.println("Update process aborted.");
            return 1;
        } finally {
            service.clean();
        }
    }

    public int checkOnly() {
        Service service = new Service();
        service.setSourcePreference(source);
        try {
            String targetVersion = service.getManifest(devMode).version();
            if (service.isUpdateAvailable(devMode)) {
                System.out.println("Update available: " + targetVersion);
                return 0;
            }
            System.out.println("No new update available.");
            return 2;
        } catch (Exception exception) {
            System.err.println("Update check failed: " + exception.getMessage());
            return 1;
        } finally {
            service.clean();
        }
    }

    private static void printProgress(int bytes, long total) {
        if (total <= 0) return;
        int percent = (int) Math.min(100L, bytes * 100L / total);
        System.out.print("\rDownloading: " + percent + "%");
    }
}

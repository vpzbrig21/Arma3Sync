package fr.soe.a3sUpdater.console;

import fr.soe.a3sUpdater.service.Service;
import fr.soe.a3sUpdater.service.ElevationSupport;
import fr.soe.a3sUpdater.service.DiagnosticLog;
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
            DiagnosticLog.info("Console update started: source=" + source + ", dev=" + devMode);
            String targetVersion = service.getManifest(devMode).version();
            if (!service.isUpdateAvailable(devMode)) {
                System.out.println("No new update available.");
                return 2;
            }
            if (ElevationSupport.requiresElevation(service.installationPath())) {
                DiagnosticLog.info("Console preflight requires elevation; requesting UAC before download.");
                ElevationSupport.Result result = ElevationSupport.restart(
                        source, devMode, service.installationPath(), true);
                if (result.started()) {
                    System.out.println("Administrator rights requested. The elevated updater is continuing the update.");
                    DiagnosticLog.info("Console preflight UAC handoff accepted; elevated updater owns the update.");
                    return 0;
                }
                System.err.println("Administrator rights are required to update this installation.");
                System.err.println(result.message());
                DiagnosticLog.error("Console preflight UAC handoff failed: " + result.message(), null);
                return 1;
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
            DiagnosticLog.info("Console update completed successfully: targetVersion=" + targetVersion);
            return 0;
        } catch (Exception exception) {
            DiagnosticLog.error("Console updater failed.", exception);
            if (ElevationSupport.isPermissionFailure(exception)
                    && !ElevationSupport.isElevatedProcess()) {
                ElevationSupport.Result result = ElevationSupport.restart(
                        source, devMode, service.installationPath(), true);
                if (result.started()) {
                    DiagnosticLog.info("Console UAC restart accepted.");
                    System.out.println("Administrator rights requested. The elevated updater is continuing the update.");
                    return 0;
                }
                System.err.println("Administrator rights are required to update this installation.");
                System.err.println(result.message());
                DiagnosticLog.error("Console UAC restart failed: " + result.message(), null);
                return 1;
            }
            if (ElevationSupport.isElevatedProcess()
                    && ElevationSupport.isPermissionFailure(exception)) {
                DiagnosticLog.error("Elevated console updater still cannot write to the installation; not retrying UAC.", exception);
            }
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

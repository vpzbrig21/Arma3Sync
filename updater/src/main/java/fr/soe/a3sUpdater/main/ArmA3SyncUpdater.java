package fr.soe.a3sUpdater.main;

import fr.soe.a3sUpdater.console.Console;
import fr.soe.a3sUpdater.ui.Facade;
import fr.soe.a3sUpdater.ui.Updater;
import fr.soe.a3sUpdater.model.UpdateSource;
import fr.soe.a3sUpdater.service.DiagnosticLog;
import fr.soe.a3sUpdater.dao.DataAccessConstants;

import java.awt.GraphicsEnvironment;
import java.util.Arrays;

import javax.swing.SwingUtilities;

public final class ArmA3SyncUpdater {
    private ArmA3SyncUpdater() { }

    public static void main(String[] args) {
        DiagnosticLog.info("Updater started: java=" + System.getProperty("java.version")
                + ", user=" + System.getProperty("user.name")
                + ", installation=" + DataAccessConstants.installationPath()
                + ", arguments=" + String.join(" ", args));
        DiagnosticLog.markElevatedStartup();
        boolean devMode = Arrays.stream(args).anyMatch("-dev"::equalsIgnoreCase);
        boolean console = Arrays.stream(args).anyMatch("-console"::equalsIgnoreCase);
        boolean checkOnly = Arrays.stream(args).anyMatch("-check"::equalsIgnoreCase);
        boolean github = Arrays.stream(args).anyMatch("-github"::equalsIgnoreCase);
        boolean manifest = Arrays.stream(args).anyMatch("-manifest"::equalsIgnoreCase);
        if (github && manifest) {
            DiagnosticLog.error("Conflicting update source arguments.", null);
            System.err.println("Choose either -github or -manifest, not both.");
            System.exit(1);
            return;
        }
        UpdateSource source = github ? UpdateSource.GITHUB
                : manifest ? UpdateSource.MANIFEST : UpdateSource.CONFIGURED;
        if (console) {
            DiagnosticLog.info("Starting console update: source=" + source + ", dev=" + devMode);
            System.exit(new Console(devMode, source).execute());
            return;
        }
        if (checkOnly) {
            DiagnosticLog.info("Starting update check: source=" + source + ", dev=" + devMode);
            System.exit(new Console(devMode, source).checkOnly());
            return;
        }
        boolean unknownArgument = Arrays.stream(args).anyMatch(argument ->
                !argument.equalsIgnoreCase("-dev")
                        && !argument.equalsIgnoreCase("-github")
                        && !argument.equalsIgnoreCase("-manifest"));
        if (unknownArgument) {
            DiagnosticLog.error("Unknown updater argument.", null);
            System.err.println("ArmA3Sync-Updater - command not found.");
            System.err.println("Usage: java -jar ArmA3Sync-Updater.jar [-dev] [-github|-manifest] [-console|-check]");
            return;
        }
        if (GraphicsEnvironment.isHeadless()) {
            DiagnosticLog.error("Updater cannot start because the environment is headless.", null);
            System.err.println("Can't start ArmA3Sync Updater. GUI is missing.");
            System.exit(1);
            return;
        }
        Facade facade = new Facade();
        facade.setDevMode(devMode);
        facade.setSource(source);
        DiagnosticLog.info("Starting graphical updater: source=" + source + ", dev=" + devMode);
        SwingUtilities.invokeLater(() -> new Updater(facade).startUpdate());
    }
}

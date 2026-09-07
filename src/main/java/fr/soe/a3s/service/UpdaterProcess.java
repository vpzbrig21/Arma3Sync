package fr.soe.a3s.service;

import fr.soe.a3s.dao.DataAccessConstants;
import fr.soe.a3s.dao.ApplicationPaths;
import fr.soe.a3s.main.Version;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Starts the installed updater so update discovery and installation share one implementation. */
public final class UpdaterProcess implements DataAccessConstants {
    public static final int UPDATE_AVAILABLE = 0;
    public static final int NO_UPDATE = 2;

    private UpdaterProcess() { }

    public static CheckResult check(boolean devMode) throws IOException, InterruptedException {
        Process process = startProcess(devMode, true, preferGitHubUpdates());
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) output.append(line).append(System.lineSeparator());
        }
        return new CheckResult(process.waitFor(), output.toString());
    }

    public static Process startUpdate(boolean devMode) throws IOException {
        return startProcess(devMode, false, preferGitHubUpdates());
    }

    private static Process startProcess(boolean devMode, boolean checkOnly, boolean preferGitHub)
            throws IOException {
        Path installation = Path.of(INSTALLATION_PATH).toAbsolutePath().normalize();
        File updater = installation.resolve("ArmA3Sync-Updater.jar").toFile();
        if (!updater.isFile()) throw new IOException("ArmA3Sync-Updater.jar was not found in " + installation);

        List<String> command = new ArrayList<>();
        command.add(javaExecutable());
        command.add("-Djava.net.preferIPv4Stack=true");
        command.add("-Da3s.updater.installationPath=" + installation);
        command.add("-Da3s.updater.userConfigPath=" + ApplicationPaths.configurationFolderPath());
        command.add("-Da3s.updater.currentVersion=" + Version.getVersion());
        command.add("-jar");
        command.add(updater.getAbsolutePath());
        command.add(preferGitHub ? "-github" : "-manifest");
        if (devMode) command.add("-dev");
        if (checkOnly) command.add("-check");
        else command.add("-console");
        return new ProcessBuilder(command).directory(installation.toFile()).redirectErrorStream(true).start();
    }

    private static boolean preferGitHubUpdates() {
        return new PreferencesService().getPreferences().isPreferGitHubUpdates();
    }

    private static String javaExecutable() {
        String javaHome = System.getProperty("java.home");
        String executable = System.getProperty("os.name", "").toLowerCase().contains("win") ? "java.exe" : "java";
        Path candidate = Path.of(javaHome, "bin", executable);
        return candidate.toFile().isFile() ? candidate.toString() : "java";
    }

    public record CheckResult(int exitCode, String output) {
        public boolean updateAvailable() { return exitCode == UPDATE_AVAILABLE; }
        public boolean noUpdate() { return exitCode == NO_UPDATE; }
    }
}

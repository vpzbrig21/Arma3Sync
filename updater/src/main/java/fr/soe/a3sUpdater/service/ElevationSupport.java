package fr.soe.a3sUpdater.service;

import fr.soe.a3sUpdater.model.UpdateSource;

import java.io.File;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileSystemException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/** Windows-specific helpers for restarting the updater with UAC elevation. */
public final class ElevationSupport {
    private static final String ELEVATE_ARGUMENT = "-a3s-elevate-updater";
    private static final List<String> FORWARDED_PROPERTIES = List.of(
            "a3s.updater.protocol",
            "a3s.updater.host",
            "a3s.updater.port",
            "a3s.updater.repository",
            "a3s.updater.repository.dev",
            "a3s.updater.url",
            "a3s.updater.url.dev",
            "a3s.updater.manifestUrl",
            "a3s.updater.legacyXmlUrl",
            "a3s.updater.manifestUrl.dev",
            "a3s.updater.legacyXmlUrl.dev",
            "a3s.updater.allowHttp",
            "a3s.updater.github.enabled",
            "a3s.updater.github.apiUrl",
            "a3s.updater.github.apiUrl.dev");

    private ElevationSupport() { }

    public static boolean isWindows() {
        return System.getProperty("os.name", "")
                .toLowerCase(Locale.ROOT)
                .contains("win");
    }

    public static boolean isPermissionFailure(Throwable failure) {
        Throwable current = failure;
        while (current != null) {
            if (current instanceof AccessDeniedException) return true;
            if (current instanceof FileSystemException exception && containsPermissionText(exception.getReason())) return true;
            if (current instanceof IOException && containsPermissionText(current.getMessage())) return true;
            current = current.getCause();
        }
        return false;
    }

    public static Result restart(UpdateSource source, boolean devMode, Path installationPath) {
        if (!isWindows()) return Result.failure("UAC elevation is only available on Windows.");

        File launcher = installationPath.resolve("Arma3Sync.exe").toFile();
        if (!launcher.isFile()) launcher = installationPath.resolve("ArmA3Sync.exe").toFile();
        if (!launcher.isFile()) {
            return Result.failure("The native Arma3Sync launcher was not found in " + installationPath + ".");
        }

        List<String> command = new ArrayList<>();
        command.add(launcher.getAbsolutePath());
        command.add(ELEVATE_ARGUMENT);
        if (devMode) command.add("-dev");
        if (source == UpdateSource.GITHUB) command.add("-github");
        else if (source == UpdateSource.MANIFEST) command.add("-manifest");

        addSystemProperty(command, "a3s.updater.userConfigPath");
        addSystemProperty(command, "a3s.updater.currentVersion");
        FORWARDED_PROPERTIES.forEach(property -> addSystemProperty(command, property));

        try {
            Process process = new ProcessBuilder(command)
                    .directory(installationPath.toFile())
                    .redirectErrorStream(true)
                    .start();
            if (!process.waitFor(30, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return Result.failure("The UAC launcher did not finish starting the elevated updater.");
            }
            if (process.exitValue() == 0) return Result.success();
            return Result.failure("Administrator rights were not granted or the elevated updater could not be started.");
        } catch (IOException exception) {
            return Result.failure("The elevated updater could not be started: " + exception.getMessage());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return Result.failure("Starting the elevated updater was interrupted.");
        }
    }

    private static void addSystemProperty(List<String> command, String name) {
        String value = System.getProperty(name);
        if (value != null && !value.isBlank()) command.add("-D" + name + "=" + value);
    }

    private static boolean containsPermissionText(String value) {
        if (value == null) return false;
        String normalized = value.toLowerCase(Locale.ROOT);
        return normalized.contains("access denied")
                || normalized.contains("permission denied")
                || normalized.contains("zugriff verweigert");
    }

    public record Result(boolean started, String message) {
        static Result success() { return new Result(true, ""); }
        static Result failure(String message) { return new Result(false, message); }
    }
}

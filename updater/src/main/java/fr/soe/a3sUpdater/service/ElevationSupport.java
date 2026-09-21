package fr.soe.a3sUpdater.service;

import fr.soe.a3sUpdater.model.UpdateSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/** Windows-specific helpers for restarting the updater with UAC elevation. */
public final class ElevationSupport {
    private static final String ELEVATED_PROPERTY = "a3s.updater.elevated";
    private static final String PARENT_PROCESS_PROPERTY = "a3s.updater.parentPid";
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
            "a3s.updater.github.apiUrl.dev",
            "a3s.updater.debug",
            "a3s.updater.debugLogPath");

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
        return restart(source, devMode, installationPath, false);
    }

    /**
     * Starts a temporary update runner through PowerShell's UAC runas verb.
     * The runner is deliberately staged outside the installation because the
     * bundled runtime is otherwise locked by the JVM that performs the update.
     */
    public static Result restart(UpdateSource source, boolean devMode, Path installationPath,
                                 boolean consoleMode) {
        if (!isWindows()) return Result.failure("UAC elevation is only available on Windows.");

        Path updater = installationPath.resolve("ArmA3Sync-Updater.jar").toAbsolutePath().normalize();
        if (!updater.toFile().isFile()) {
            return Result.failure("ArmA3Sync-Updater.jar was not found in " + installationPath + ".");
        }
        Path java = resolveJava(installationPath, consoleMode);
        if (java == null) return Result.failure("No Java runtime was found to restart the updater.");
        DiagnosticLog.info("UAC restart requested: java=" + java + ", updater=" + updater
                + ", installation=" + installationPath + ", console=" + consoleMode);

        StagedLaunch stagedLaunch;
        try {
            stagedLaunch = stageLaunch(updater, java, installationPath, consoleMode);
            DiagnosticLog.info("UAC update runner staged outside installation: root="
                    + stagedLaunch.root() + ", java=" + stagedLaunch.java());
        } catch (IOException exception) {
            DiagnosticLog.error("Could not stage the temporary elevated update runner.", exception);
            return Result.failure("The temporary elevated update runner could not be prepared: "
                    + exception.getMessage());
        }

        List<String> arguments = new ArrayList<>();
        arguments.add("-Da3s.updater.installationPath=" + installationPath.toAbsolutePath().normalize());
        addSystemProperty(arguments, "a3s.updater.userConfigPath");
        addSystemProperty(arguments, "a3s.updater.currentVersion");
        arguments.add("-D" + PARENT_PROCESS_PROPERTY + "=" + ProcessHandle.current().pid());
        Path startupMarker;
        Path elevatedScript;
        try {
            Path markerDirectory = DiagnosticLog.logPath().toAbsolutePath().normalize().getParent();
            Files.createDirectories(markerDirectory);
            startupMarker = Files.createTempFile(markerDirectory, "arma3sync-uac-", ".startup");
            Files.deleteIfExists(startupMarker);
        } catch (IOException exception) {
            DiagnosticLog.error("Could not create the elevated updater startup marker.", exception);
            deleteTree(stagedLaunch.root());
            return Result.failure("The elevated updater startup marker could not be created: "
                    + exception.getMessage());
        }
        arguments.add("-Da3s.updater.elevationMarker=" + startupMarker);
        arguments.add("-D" + ELEVATED_PROPERTY + "=true");
        FORWARDED_PROPERTIES.forEach(property -> addSystemProperty(arguments, property));
        arguments.add("-jar");
        arguments.add(stagedLaunch.updater().toString());
        if (consoleMode) arguments.add("-console");
        if (devMode) arguments.add("-dev");
        if (source == UpdateSource.GITHUB) arguments.add("-github");
        else if (source == UpdateSource.MANIFEST) arguments.add("-manifest");

        // The helper must be written only after the complete Java command line
        // has been assembled.  Writing it earlier silently omitted -jar and
        // the elevation marker, which made UAC appear successful while no
        // updater JVM actually reached main().
        try {
            elevatedScript = Files.createTempFile(
                    DiagnosticLog.logPath().toAbsolutePath().normalize().getParent(),
                    "arma3sync-uac-", ".ps1");
            Files.writeString(elevatedScript, buildElevatedScript(
                    stagedLaunch.java(), arguments, stagedLaunch.root(), ProcessHandle.current().pid(), startupMarker),
                    StandardCharsets.UTF_8);
            DiagnosticLog.info("Elevated updater helper created: script=" + elevatedScript
                    + ", argumentCount=" + arguments.size());
        } catch (IOException exception) {
            DiagnosticLog.error("Could not create the elevated updater helper script.", exception);
            try { Files.deleteIfExists(startupMarker); } catch (IOException ignored) { }
            deleteTree(stagedLaunch.root());
            return Result.failure("The elevated updater helper could not be created: "
                    + exception.getMessage());
        }

        boolean handoffStarted = false;
        try {
            Process process = new ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive",
                    "-ExecutionPolicy", "Bypass", "-Command", buildPowerShellCommand(elevatedScript,
                    installationPath.toAbsolutePath().normalize(), consoleMode))
                    .directory(installationPath.toFile())
                    .redirectErrorStream(true)
                    .start();
            byte[] output;
            if (!process.waitFor(30, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return Result.failure("The UAC launcher did not finish starting the elevated updater.");
            }
            output = process.getInputStream().readAllBytes();
            if (process.exitValue() == 0) {
                handoffStarted = true;
                DiagnosticLog.info("UAC restart confirmed: temporary elevated update runner accepted.");
                return Result.success();
            }
            String detail = new String(output, StandardCharsets.UTF_8).trim();
            DiagnosticLog.error("UAC restart rejected by PowerShell: exit=" + process.exitValue()
                    + ", output=" + detail, null);
            return Result.failure("Administrator rights were not granted or the elevated updater could not be started."
                    + (detail.isBlank() ? "" : " " + detail));
        } catch (IOException exception) {
            DiagnosticLog.error("UAC restart process could not be started.", exception);
            return Result.failure("The elevated updater could not be started: " + exception.getMessage());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            DiagnosticLog.error("UAC restart wait was interrupted.", exception);
            return Result.failure("Starting the elevated updater was interrupted.");
        } finally {
            if (!handoffStarted) {
                try { Files.deleteIfExists(startupMarker); } catch (IOException ignored) { }
                try { Files.deleteIfExists(elevatedScript); } catch (IOException ignored) { }
                deleteTree(stagedLaunch.root());
            }
        }
    }

    /**
     * Returns true only for a process that was explicitly started by the UAC
     * handoff. This prevents an elevated child from opening a second UAC
     * request if a protected file is still locked or has a separate ACL.
     */
    public static boolean isElevatedProcess() {
        return Boolean.parseBoolean(System.getProperty(ELEVATED_PROPERTY, "false"));
    }

    /**
     * Performs a non-invasive write probe in the installation directory. The
     * probe is deliberately done before downloading an update so a protected
     * installation does not download a complete archive only to fail during
     * the final copy step.
     */
    public static boolean canWriteInstallation(Path installationPath) {
        if (installationPath == null || !Files.isDirectory(installationPath)) return false;
        Path probe = null;
        try {
            probe = Files.createTempFile(installationPath, ".a3s-write-probe-", ".tmp");
            return true;
        } catch (IOException exception) {
            DiagnosticLog.info("Installation write probe failed: path=" + installationPath
                    + ", reason=" + exception.getClass().getSimpleName());
            return false;
        } finally {
            if (probe != null) {
                try { Files.deleteIfExists(probe); }
                catch (IOException exception) {
                    DiagnosticLog.info("Installation write probe cleanup failed: " + probe);
                }
            }
        }
    }

    public static boolean requiresElevation(Path installationPath) {
        return isWindows()
                && !isElevatedProcess()
                && installationPath != null
                && Files.isDirectory(installationPath)
                && !canWriteInstallation(installationPath);
    }

    private static StagedLaunch stageLaunch(Path updater, Path java, Path installationPath,
                                            boolean consoleMode) throws IOException {
        Path root = Files.createTempDirectory("arma3sync-update-runner-");
        try {
            Path stagedUpdater = root.resolve("ArmA3Sync-Updater.jar");
            Files.copy(updater, stagedUpdater);

            Path bundledRuntime = installationPath.resolve("runtime").toAbsolutePath().normalize();
            Path normalizedJava = java.toAbsolutePath().normalize();
            Path stagedJava = normalizedJava;
            if (normalizedJava.startsWith(bundledRuntime) && Files.isDirectory(bundledRuntime)) {
                validateRuntimeLayout(bundledRuntime, normalizedJava);
                Path stagedRuntime = root.resolve("runtime");
                int copiedFiles = copyTree(bundledRuntime, stagedRuntime);
                stagedJava = stagedRuntime.resolve("bin")
                        .resolve(consoleMode ? "java.exe" : "javaw.exe");
                validateStagedRuntime(stagedRuntime, stagedJava, copiedFiles);
                DiagnosticLog.info("Temporary Java runtime copied: files=" + copiedFiles);
            } else {
                DiagnosticLog.info("Using external Java runtime for temporary update runner: " + stagedJava);
            }
            if (!Files.isRegularFile(stagedUpdater)) {
                throw new IOException("The staged updater JAR is missing: " + stagedUpdater);
            }
            return new StagedLaunch(root, stagedJava, stagedUpdater);
        } catch (IOException exception) {
            deleteTree(root);
            throw exception;
        }
    }

    private static void validateStagedRuntime(Path stagedRuntime, Path stagedJava, int copiedFiles)
            throws IOException {
        if (copiedFiles < 10) {
            throw new IOException("The staged Java runtime is incomplete; only " + copiedFiles + " files were copied.");
        }
        validateRuntimeLayout(stagedRuntime, stagedJava);
    }

    private static void validateRuntimeLayout(Path runtime, Path java) throws IOException {
        List<Path> required = List.of(
                runtime.resolve("release"),
                runtime.resolve("conf"),
                runtime.resolve("lib"),
                java,
                runtime.resolve("bin").resolve("server").resolve("jvm.dll"));
        for (Path path : required) {
            boolean directory = path.getFileName().toString().equals("conf")
                    || path.getFileName().toString().equals("lib");
            if (directory ? !Files.isDirectory(path) : !Files.isRegularFile(path)) {
                throw new IOException("The Java runtime is incomplete; missing " + path);
            }
        }
    }

    private static int copyTree(Path source, Path destination) throws IOException {
        int[] copiedFiles = {0};
        Files.walkFileTree(source, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes)
                    throws IOException {
                Path relative = source.relativize(directory);
                Files.createDirectories(destination.resolve(relative));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes)
                    throws IOException {
                Path relative = source.relativize(file);
                Path target = destination.resolve(relative);
                Files.createDirectories(target.getParent());
                Files.copy(file, target);
                copiedFiles[0]++;
                return FileVisitResult.CONTINUE;
            }
        });
        return copiedFiles[0];
    }

    private static void deleteTree(Path root) {
        if (root == null || !Files.exists(root)) return;
        try (var stream = Files.walk(root)) {
            stream.sorted(Comparator.reverseOrder()).forEach(path -> {
                try { Files.deleteIfExists(path); } catch (IOException ignored) { }
            });
        } catch (IOException ignored) { }
    }

    private record StagedLaunch(Path root, Path java, Path updater) { }

    private static Path resolveJava(Path installationPath, boolean consoleMode) {
        String executable = consoleMode ? "java.exe" : "javaw.exe";
        Path bundled = installationPath.resolve("runtime").resolve("bin").resolve(executable);
        if (bundled.toFile().isFile()) return bundled.toAbsolutePath().normalize();
        String javaHome = System.getProperty("java.home");
        if (javaHome != null && !javaHome.isBlank()) {
            Path current = Path.of(javaHome).resolve("bin").resolve(executable);
            if (current.toFile().isFile()) return current.toAbsolutePath().normalize();
            Path parent = Path.of(javaHome).getParent();
            if (parent != null) {
                current = parent.resolve("bin").resolve(executable);
                if (current.toFile().isFile()) return current.toAbsolutePath().normalize();
            }
        }
        return null;
    }

    static String buildPowerShellCommand(Path elevatedScript, Path workingDirectory, boolean consoleMode) {
        StringBuilder command = new StringBuilder("$ErrorActionPreference='Stop'; ");
        command.append("$p=Start-Process -FilePath ")
                .append(powerShellQuote("powershell.exe"))
                .append(" -ArgumentList @(");
        List<String> scriptArguments = List.of("-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass",
                "-File", elevatedScript.toString());
        for (int index = 0; index < scriptArguments.size(); index++) {
            if (index > 0) command.append(", ");
            command.append(powerShellQuote(scriptArguments.get(index)));
        }
        command.append(") -WorkingDirectory ")
                .append(powerShellQuote(workingDirectory.toString()))
                // Keep the helper invisible while retaining the normal UAC
                // consent prompt shown by Windows for the RunAs verb.
                .append(" -WindowStyle Hidden -Verb RunAs -PassThru");
        // The helper waits for the original updater to exit and then starts
        // the staged Java process. Waiting here would deadlock that handoff.
        command.append("; exit 0");
        return command.toString();
    }

    static String buildElevatedScript(Path java, List<String> arguments) {
        return buildElevatedScript(java, arguments, null, 0L, null);
    }

    static String buildElevatedScript(Path java, List<String> arguments, Path stagingRoot,
                                      long parentPid, Path startupMarker) {
        StringBuilder script = new StringBuilder()
                .append("$ErrorActionPreference='Stop'").append(System.lineSeparator())
                .append("$exitCode=1").append(System.lineSeparator());
        if (parentPid > 0) {
            script.append("try {").append(System.lineSeparator())
                    .append("  Wait-Process -Id ").append(parentPid)
                    .append(" -ErrorAction SilentlyContinue").append(System.lineSeparator())
                    .append("} catch { }").append(System.lineSeparator());
        }
        script.append("try {").append(System.lineSeparator())
                .append("  $child=Start-Process -FilePath ")
                .append(powerShellQuote(java.toString()))
                .append(" -ArgumentList @(");
        for (int index = 0; index < arguments.size(); index++) {
            if (index > 0) script.append(", ");
            // Start-Process joins ArgumentList entries into one native
            // command line. Quote every entry for the Windows command-line
            // parser so installation paths such as "Program Files (x86)"
            // remain one Java argument.
            script.append(powerShellQuote(windowsArgumentQuote(arguments.get(index))));
        }
        script.append(")");
        if (stagingRoot != null) {
            script.append(" -WorkingDirectory ")
                    .append(powerShellQuote(stagingRoot.toString()));
        }
        script.append(" -Wait -PassThru").append(System.lineSeparator())
                .append("  $exitCode=$child.ExitCode").append(System.lineSeparator())
                .append("} catch {").append(System.lineSeparator())
                .append("  $exitCode=1").append(System.lineSeparator())
                .append("}").append(System.lineSeparator())
                .append("try {").append(System.lineSeparator());
        if (startupMarker != null) {
            script.append("  Remove-Item -LiteralPath ")
                    .append(powerShellQuote(startupMarker.toString()))
                    .append(" -Force -ErrorAction SilentlyContinue")
                    .append(System.lineSeparator());
        }
        if (stagingRoot != null) {
            script.append("  Remove-Item -LiteralPath ")
                    .append(powerShellQuote(stagingRoot.toString()))
                    .append(" -Recurse -Force -ErrorAction SilentlyContinue")
                    .append(System.lineSeparator());
        }
        script.append("} catch { }").append(System.lineSeparator())
                .append("Remove-Item -LiteralPath $PSCommandPath -Force -ErrorAction SilentlyContinue").append(System.lineSeparator())
                .append("exit $exitCode").append(System.lineSeparator());
        return script.toString();
    }

    private static String powerShellQuote(String value) {
        return "'" + value.replace("'", "''") + "'";
    }

    private static String windowsArgumentQuote(String value) {
        StringBuilder quoted = new StringBuilder(value.length() + 2).append('"');
        int backslashes = 0;
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character == '\\') {
                backslashes++;
            } else if (character == '"') {
                quoted.append("\\\\".repeat(backslashes * 2 + 1)).append('"');
                backslashes = 0;
            } else {
                if (backslashes > 0) quoted.append("\\".repeat(backslashes));
                quoted.append(character);
                backslashes = 0;
            }
        }
        if (backslashes > 0) quoted.append("\\".repeat(backslashes * 2));
        return quoted.append('"').toString();
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

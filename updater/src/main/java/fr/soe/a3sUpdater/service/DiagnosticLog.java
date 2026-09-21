package fr.soe.a3sUpdater.service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

/** Optional updater diagnostics written outside protected installation folders. */
public final class DiagnosticLog {
    private static final String ENABLED_PROPERTY = "a3s.updater.debug";
    private static final String PATH_PROPERTY = "a3s.updater.debugLogPath";
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private static final Object LOCK = new Object();

    private DiagnosticLog() { }

    public static boolean enabled() {
        return Boolean.parseBoolean(System.getProperty(ENABLED_PROPERTY, "false"));
    }

    public static Path logPath() {
        String configured = System.getProperty(PATH_PROPERTY);
        if (configured != null && !configured.isBlank()) return Path.of(configured).toAbsolutePath().normalize();

        String localAppData = System.getenv("LOCALAPPDATA");
        Path root = localAppData == null || localAppData.isBlank()
                ? Path.of(System.getProperty("user.home", "."), "AppData", "Local")
                : Path.of(localAppData);
        return root.resolve("Arma3Sync").resolve("logs").resolve("arma3sync-updater-debug.log");
    }

    public static void info(String message) {
        write("INFO", message, null);
    }

    public static void error(String message, Throwable failure) {
        write("ERROR", message, failure);
    }

    /** Confirms that a UAC-started JVM reached the updater main method. */
    public static void markElevatedStartup() {
        String marker = System.getProperty("a3s.updater.elevationMarker");
        if (marker == null || marker.isBlank()) return;
        try {
            Path file = Path.of(marker).toAbsolutePath().normalize();
            Files.createDirectories(file.getParent());
            Files.writeString(file, OffsetDateTime.now().format(TIMESTAMP), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Exception failure) {
            error("Could not write the elevated updater startup marker.", failure);
        }
    }

    private static void write(String level, String message, Throwable failure) {
        if (!enabled()) return;
        StringBuilder line = new StringBuilder()
                .append(OffsetDateTime.now().format(TIMESTAMP))
                .append(" [").append(Thread.currentThread().getName()).append("] [")
                .append(level).append("] ").append(message == null ? "" : message);
        if (failure != null) {
            StringWriter stack = new StringWriter();
            failure.printStackTrace(new PrintWriter(stack));
            line.append(System.lineSeparator()).append(stack);
        }
        line.append(System.lineSeparator());
        try {
            Path file = logPath();
            synchronized (LOCK) {
                Files.createDirectories(file.getParent());
                Files.writeString(file, line.toString(), StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            }
        } catch (Exception ignored) {
            // Diagnostics must never change the updater result.
        }
    }
}

package fr.soe.a3s.utils;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import fr.soe.a3s.dao.DataAccessConstants;
import fr.soe.a3s.domain.AbstractProtocole;

/**
 * Opt-in diagnostic logging for support and troubleshooting.
 *
 * <p>The logger is intentionally disabled by default. It writes only to the
 * per-user configuration directory and never logs protocol passwords.</p>
 */
public final class DebugLogger {

    private static final Logger LOGGER = Logger.getLogger("fr.soe.a3s.debug");
    private static final Object LOCK = new Object();
    private static volatile boolean enabled;
    private static volatile Path logPath;
    private static Handler fileHandler;

    private DebugLogger() {
    }

    /** Enables diagnostic logging and returns the log file path when possible. */
    public static Path enable() {
        synchronized (LOCK) {
            if (enabled && logPath != null) {
                return logPath;
            }

            Path configurationDirectory = Paths.get(DataAccessConstants.CONFIGURATION_FOLDER_PATH);
            try {
                Files.createDirectories(configurationDirectory);
                logPath = configurationDirectory.resolve("arma3sync-debug.log").toAbsolutePath().normalize();
                fileHandler = new FileHandler(logPath.toString(), 5 * 1024 * 1024, 3, true);
                fileHandler.setEncoding(StandardCharsets.UTF_8.name());
                fileHandler.setFormatter(new DiagnosticFormatter());
                LOGGER.setUseParentHandlers(false);
                LOGGER.setLevel(Level.ALL);
                LOGGER.addHandler(fileHandler);
                enabled = true;
                info("Debug logging enabled. Log file: " + logPath);
                return logPath;
            } catch (IOException | RuntimeException e) {
                enabled = false;
                logPath = null;
                closeHandler(fileHandler);
                fileHandler = null;
                System.err.println("Arma3Sync diagnostic logging could not be initialized: "
                        + e.getClass().getSimpleName() + ": " + e.getMessage());
                return null;
            }
        }
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static Path getLogPath() {
        return logPath;
    }

    public static void info(String message) {
        if (enabled) {
            LOGGER.log(Level.INFO, message);
        }
    }

    public static void warning(String message) {
        if (enabled) {
            LOGGER.log(Level.WARNING, message);
        }
    }

    public static void error(String message, Throwable error) {
        if (enabled) {
            LOGGER.log(Level.SEVERE, message, error);
        }
    }

    /** Returns the location from which a class was actually loaded. */
    public static String describeCodeSource(Class<?> type) {
        if (type == null || type.getProtectionDomain() == null
                || type.getProtectionDomain().getCodeSource() == null
                || type.getProtectionDomain().getCodeSource().getLocation() == null) {
            return "<unknown>";
        }
        return type.getProtectionDomain().getCodeSource().getLocation().toExternalForm();
    }

    /** Returns a safe endpoint description without the protocol password. */
    public static String describeProtocol(AbstractProtocole protocol) {
        if (protocol == null) {
            return "protocol=null";
        }
        String type = protocol.getProtocolType() == null ? "unknown" : protocol.getProtocolType().name();
        String host = protocol.getHostname();
        String remotePath = protocol.getRemotePath();
        return "protocol=" + type + ", host=" + safe(host) + ", port=" + safe(protocol.getPort())
                + ", remotePath=" + safe(remotePath);
    }

    private static String safe(String value) {
        if (value == null || value.isEmpty()) {
            return "<empty>";
        }
        String sanitized = value.replace('\r', ' ').replace('\n', ' ');
        int credentialsSeparator = sanitized.lastIndexOf('@');
        int schemeSeparator = sanitized.indexOf("://");
        if (credentialsSeparator > schemeSeparator && credentialsSeparator >= 0) {
            sanitized = "<credentials-redacted>@" + sanitized.substring(credentialsSeparator + 1);
        }
        return sanitized.replaceAll("(?i)(password|passwd|pwd|secret|token)\\s*[=:]\\s*[^&\\s]+", "$1=<redacted>");
    }

    private static void closeHandler(Handler handler) {
        if (handler != null) {
            try {
                handler.close();
            } catch (RuntimeException ignored) {
                // Diagnostics must never affect the application.
            }
        }
    }

    private static final class DiagnosticFormatter extends Formatter {
        @Override
        public String format(LogRecord record) {
            StringBuilder output = new StringBuilder(String.format("%1$tF %1$tT.%1$tL [%2$s] [%3$s] %4$s%n",
                    record.getMillis(), Thread.currentThread().getName(), record.getLevel().getName(),
                    formatMessage(record)));
            if (record.getThrown() != null) {
                StringWriter stackTrace = new StringWriter();
                record.getThrown().printStackTrace(new PrintWriter(stackTrace));
                output.append(stackTrace);
            }
            return output.toString();
        }
    }
}

package fr.soe.a3s.service;

import java.io.IOException;

/** Central policy for the exceptional, certificate-validation-disabled mode. */
public final class SslValidationPolicy {

    private static final String OVERRIDE_PROPERTY = "a3s.allowInsecureSsl";

    private SslValidationPolicy() {
    }

    public static boolean isLocalTestHost(String host) {
        if (host == null) return false;
        String normalized = hostPart(host).toLowerCase(java.util.Locale.ROOT);
        if (normalized.startsWith("[") && normalized.endsWith("]")) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        return normalized.equals("localhost") || normalized.equals("localhost.")
                || normalized.equals("::1") || normalized.equals("0:0:0:0:0:0:0:1")
                || normalized.equals("127.0.0.1") || normalized.startsWith("127.");
    }

    public static boolean isAllowedFor(String host) {
        return isLocalTestHost(host) || Boolean.getBoolean(OVERRIDE_PROPERTY);
    }

    public static String hostPart(String urlOrHost) {
        if (urlOrHost == null) return "";
        String value = urlOrHost.trim();
        int slash = value.indexOf('/');
        return slash < 0 ? value : value.substring(0, slash);
    }

    public static void requireAllowed(String host) throws IOException {
        if (!isAllowedFor(host)) {
            throw new IOException("SSL certificate validation is disabled for a non-local repository. "
                    + "Enable certificate validation or explicitly start with -D" + OVERRIDE_PROPERTY + "=true "
                    + "for a controlled development test.");
        }
    }

    public static String warningText(String host) {
        return "SSL certificate validation is disabled for '" + hostPart(host) + "'.\n\n"
                + "This accepts forged certificates and hostnames. This setting is intended only for local tests.\n"
                + "For a remote development server, start ArmA3Sync with -D" + OVERRIDE_PROPERTY + "=true.\n\n"
                + "Enable certificate validation whenever possible.";
    }
}

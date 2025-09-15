package fr.soe.a3s.main;

/**
 * Centralised application version information.
 * <p>
 * This utility class exposes build metadata so it can be referenced
 * throughout the project without duplicating version strings.
 * </p>
 */
public final class Version {

        private static final String NAME = "1.8";

        private static final int MAJOR = 1;

        private static final int MINOR = 8;

        private static final int BUILD = 109;

        private static final String YEAR = "2013-2025";

        private Version() {
                // utility class
        }

        public static String getVersion() {
                return MAJOR + "." + MINOR + "." + BUILD;
        }

        public static String getName() {
                return NAME;
        }

        public static int getBuild() {
                return BUILD;
        }

        public static String getYear() {
                return YEAR;
        }
}

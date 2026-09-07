package fr.soe.a3s.dao;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

/**
 * Resolves writable application paths independently from the installation
 * directory. The installation directory contains the immutable program files;
 * user data belongs to the current user.
 */
public final class ApplicationPaths {

    public static final String APPLICATION_NAME = "Arma3Sync";
    private static final String INSTALLATION_PROPERTY = "a3s.installationPath";
    private static final String PORTABLE_PROPERTY = "a3s.portable";

    private ApplicationPaths() {
    }

    public static String installationPath() {
        String configured = System.getProperty(INSTALLATION_PROPERTY);
        Path path = configured == null || configured.isBlank()
                ? Paths.get(System.getProperty("user.dir", "."))
                : Paths.get(configured);
        return path.toAbsolutePath().normalize().toString();
    }

    /**
     * Establishes the installation directory before any path constants are
     * initialized. This is important for direct JAR launches and Gradle
     * generated scripts, where {@code user.dir} may be unrelated to the
     * application directory.
     */
    public static void initializeInstallationPath(Class<?> applicationClass) {
        String configured = System.getProperty(INSTALLATION_PROPERTY);
        if (configured != null && !configured.isBlank()) {
            Path candidate = Paths.get(configured).toAbsolutePath().normalize();
            if (looksLikeInstallation(candidate)
                    && (Boolean.parseBoolean(System.getProperty(PORTABLE_PROPERTY, "false"))
                            || isPortableInstallation(candidate))) {
                System.setProperty(PORTABLE_PROPERTY, "true");
            }
            return;
        }
        if (applicationClass == null || applicationClass.getProtectionDomain() == null
                || applicationClass.getProtectionDomain().getCodeSource() == null
                || applicationClass.getProtectionDomain().getCodeSource().getLocation() == null) {
            return;
        }
        try {
            Path location = Paths.get(applicationClass.getProtectionDomain().getCodeSource().getLocation().toURI())
                    .toAbsolutePath().normalize();
            Path candidate = Files.isDirectory(location) ? location : location.getParent();
            if (candidate != null && candidate.getFileName() != null
                    && "lib".equalsIgnoreCase(candidate.getFileName().toString())) {
                candidate = candidate.getParent();
            }
            if (candidate != null && looksLikeInstallation(candidate)) {
                System.setProperty(INSTALLATION_PROPERTY, candidate.toString());
                if (Boolean.parseBoolean(System.getProperty(PORTABLE_PROPERTY, "false"))
                        || isPortableInstallation(candidate)) {
                    System.setProperty(PORTABLE_PROPERTY, "true");
                }
            }
        } catch (URISyntaxException | RuntimeException ignored) {
            // The working directory remains a safe fallback for development
            // launches where no packaged installation can be inferred.
        }
    }

    private static boolean looksLikeInstallation(Path candidate) {
        return Files.isDirectory(candidate.resolve("lib"))
                || Files.isDirectory(candidate.resolve("resources"))
                || Files.isRegularFile(candidate.resolve("Arma3Sync.jar"))
                || Files.isRegularFile(candidate.resolve("ArmA3Sync.jar"));
    }

    public static String configRoot() {
        if (isPortableStorage()) {
            return installationPath();
        }
        return platformRoot("config").resolve(APPLICATION_NAME).toString();
    }

    public static String dataRoot() {
        if (isPortableStorage()) {
            return installationPath();
        }
        return platformRoot("data").resolve(APPLICATION_NAME).toString();
    }

    public static String cacheRoot() {
        if (isPortableStorage()) {
            return Paths.get(installationPath(), "resources", "temp").toString();
        }
        return platformRoot("cache").resolve(APPLICATION_NAME).toString();
    }

    public static String configurationFilePath() {
        if (isPortableStorage()) {
            return portableConfigurationFolder().resolve("a3s.cfg").toString();
        }
        return Paths.get(configRoot(), "configuration", "a3s.cfg").toString();
    }

    public static String preferencesFilePath() {
        if (isPortableStorage()) {
            return portableConfigurationFolder().resolve("a3s.prefs").toString();
        }
        return Paths.get(configRoot(), "configuration", "a3s.prefs").toString();
    }

    public static String profilesFolderPath() {
        if (isPortableStorage()) {
            return Paths.get(installationPath(), "profiles").toString();
        }
        return Paths.get(configRoot(), "profiles").toString();
    }

    public static String configurationFolderPath() {
        if (isPortableStorage()) {
            return portableConfigurationFolder().toString();
        }
        return Paths.get(configRoot(), "configuration").toString();
    }

    public static String repositoryFolderPath() {
        if (isPortableStorage()) {
            Path legacyRepositoryFolder = Paths.get(installationPath(), "resources", "ftp");
            return legacyRepositoryFolder.toString();
        }
        return Paths.get(dataRoot(), "repositories").toString();
    }

    public static String tempFolderPath() {
        return Paths.get(cacheRoot(), "temp").toString();
    }

    public static String binFolderPath() {
        if (isPortableStorage()) {
            return Paths.get(installationPath(), "bin").toString();
        }
        return Paths.get(dataRoot(), "bin").toString();
    }

    public static String updateMetadataFilePath() {
        if (isPortableStorage()) {
            return Paths.get(installationPath(), "resources", "temp", "a3s.xml").toString();
        }
        return Paths.get(cacheRoot(), "a3s.xml").toString();
    }

    /**
     * Copies data from the pre-modern layout without overwriting data already
     * created in the new layout. Copying instead of moving keeps rollback and
     * manual recovery possible if an installation is interrupted.
     */
    public static void migrateLegacyData(String installationPath) {
        if (isPortableStorage()) {
            return;
        }
        Path installation = Paths.get(installationPath).toAbsolutePath().normalize();
        copyMissing(installation.resolve("profiles"), Paths.get(profilesFolderPath()));
        copyMissing(installation.resolve("resources/configuration"), Paths.get(configurationFolderPath()));
        copyMissing(installation.resolve("resources/ftp"), Paths.get(repositoryFolderPath()));
        copyMissing(installation.resolve("resources/bin"), Paths.get(binFolderPath()));
    }

    private static void copyMissing(Path source, Path target) {
        if (!Files.isDirectory(source) || source.toAbsolutePath().normalize().equals(target.toAbsolutePath().normalize())) {
            return;
        }
        try (Stream<Path> paths = Files.walk(source)) {
            paths.forEach(path -> {
                try {
                    Path destination = target.resolve(source.relativize(path));
                    if (Files.isDirectory(path)) {
                        Files.createDirectories(destination);
                    } else if (Files.notExists(destination)) {
                        Files.createDirectories(destination.getParent());
                        // Do not copy DOS read-only attributes from Program Files.
                        Files.copy(path, destination);
                    }
                } catch (IOException e) {
                    System.err.println("Unable to migrate legacy data " + path + ": " + e.getMessage());
                }
            });
        } catch (IOException e) {
            System.err.println("Unable to scan legacy data " + source + ": " + e.getMessage());
        }
    }

    private static Path platformRoot(String kind) {
        String testOverride = System.getProperty("a3s." + kind + "Path");
        if (testOverride != null && !testOverride.isBlank()) {
            return Paths.get(testOverride);
        }
        String osName = System.getProperty("os.name", "").toLowerCase();
        Path home = Paths.get(System.getProperty("user.home", "."));
        if (osName.contains("win")) {
            String environmentName = "config".equals(kind) ? "APPDATA" : "LOCALAPPDATA";
            String environmentValue = System.getenv(environmentName);
            if (environmentValue != null && !environmentValue.isBlank()) {
                return Paths.get(environmentValue);
            }
            return home.resolve("AppData").resolve("config".equals(kind) ? "Roaming" : "Local");
        }

        if (osName.contains("linux") || osName.contains("unix") || osName.contains("bsd")
                || osName.contains("mac")) {
            String environmentName;
            String fallback;
            switch (kind) {
            case "config":
                environmentName = "XDG_CONFIG_HOME";
                fallback = ".config";
                break;
            case "cache":
                environmentName = "XDG_CACHE_HOME";
                fallback = ".cache";
                break;
            default:
                environmentName = "XDG_DATA_HOME";
                fallback = ".local/share";
                break;
            }
            String environmentValue = System.getenv(environmentName);
            if (environmentValue != null && !environmentValue.isBlank()) {
                return Paths.get(environmentValue);
            }
            return home.resolve(fallback);
        }

        return home.resolve("." + APPLICATION_NAME.toLowerCase());
    }

    private static boolean isPortableStorage() {
        return Boolean.parseBoolean(System.getProperty(PORTABLE_PROPERTY, "false"));
    }

    /**
     * Keep the compact distribution layout compatible: older releases store
     * a3s.cfg and a3s.prefs directly beside the JAR, while newer portable
     * layouts may place them below resources/configuration.
     */
    private static Path portableConfigurationFolder() {
        Path installation = Paths.get(installationPath());
        if (Files.isRegularFile(installation.resolve("a3s.cfg"))
                || Files.isRegularFile(installation.resolve("a3s.prefs"))) {
            return installation;
        }
        return installation.resolve("resources").resolve("configuration");
    }

    /**
     * A distribution outside a protected Windows installation directory is
     * treated as portable. This keeps extracted test/release folders isolated
     * from the installed user's AppData while Program Files installations keep
     * using per-user writable locations.
     */
    private static boolean isPortableInstallation(Path candidate) {
        if (candidate == null) {
            return false;
        }
        String osName = System.getProperty("os.name", "").toLowerCase();
        if (!osName.contains("win")) {
            return true;
        }
        Path normalized = candidate.toAbsolutePath().normalize();
        String[] protectedRoots = {
                System.getenv("ProgramFiles"),
                System.getenv("ProgramFiles(x86)"),
                System.getenv("ProgramW6432")
        };
        for (String root : protectedRoots) {
            if (root != null && !root.isBlank()) {
                Path protectedPath = Paths.get(root).toAbsolutePath().normalize();
                if (normalized.equals(protectedPath) || normalized.startsWith(protectedPath)) {
                    return false;
                }
            }
        }
        return true;
    }
}

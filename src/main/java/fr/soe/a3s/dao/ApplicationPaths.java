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
        return platformRoot("config").resolve(APPLICATION_NAME).toString();
    }

    public static String dataRoot() {
        return platformRoot("data").resolve(APPLICATION_NAME).toString();
    }

    public static String cacheRoot() {
        return platformRoot("cache").resolve(APPLICATION_NAME).toString();
    }

    public static String configurationFilePath() {
        return Paths.get(configRoot(), "configuration", "a3s.cfg").toString();
    }

    public static String preferencesFilePath() {
        return Paths.get(configRoot(), "configuration", "a3s.prefs").toString();
    }

    public static String profilesFolderPath() {
        return Paths.get(configRoot(), "profiles").toString();
    }

    public static String configurationFolderPath() {
        return Paths.get(configRoot(), "configuration").toString();
    }

    public static String repositoryFolderPath() {
        return Paths.get(dataRoot(), "repositories").toString();
    }

    public static String tempFolderPath() {
        return Paths.get(cacheRoot(), "temp").toString();
    }

    public static String binFolderPath() {
        return Paths.get(dataRoot(), "bin").toString();
    }

    public static String updateMetadataFilePath() {
        return Paths.get(cacheRoot(), "a3s.xml").toString();
    }

    /**
     * Copies data from the pre-modern layout without overwriting data already
     * created in the new layout. Copying instead of moving keeps rollback and
     * manual recovery possible if an installation is interrupted.
     */
    public static void migrateLegacyData(String installationPath) {
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
}

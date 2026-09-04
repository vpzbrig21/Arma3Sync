package fr.soe.a3s.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ApplicationPathsTest {

    @TempDir
    Path tempDir;

    @Test
    void migratesLegacyFilesWithoutOverwritingNewData() throws Exception {
        Path installation = tempDir.resolve("installation");
        Path legacyProfile = installation.resolve("profiles").resolve("Default.a3s.profile");
        Files.createDirectories(legacyProfile.getParent());
        Files.writeString(legacyProfile, "legacy");
        Path legacyRepository = installation.resolve("resources").resolve("ftp").resolve("repository.a3s.repository");
        Files.createDirectories(legacyRepository.getParent());
        Files.writeString(legacyRepository, "repository");

        Path configRoot = tempDir.resolve("config");
        Path dataRoot = tempDir.resolve("data");
        Path cacheRoot = tempDir.resolve("cache");
        Path currentProfile = configRoot.resolve("profiles").resolve(legacyProfile.getFileName());
        Files.createDirectories(currentProfile.getParent());
        Files.writeString(currentProfile, "current");

        String previousConfig = System.getProperty("a3s.configPath");
        String previousData = System.getProperty("a3s.dataPath");
        String previousCache = System.getProperty("a3s.cachePath");
        try {
            System.setProperty("a3s.configPath", configRoot.toString());
            System.setProperty("a3s.dataPath", dataRoot.toString());
            System.setProperty("a3s.cachePath", cacheRoot.toString());

            ApplicationPaths.migrateLegacyData(installation.toString());

            assertEquals("current", Files.readString(currentProfile));
            assertTrue(Files.isRegularFile(Path.of(ApplicationPaths.repositoryFolderPath()).resolve(legacyRepository.getFileName())));
        } finally {
            restoreProperty("a3s.configPath", previousConfig);
            restoreProperty("a3s.dataPath", previousData);
            restoreProperty("a3s.cachePath", previousCache);
        }
    }

    private static void restoreProperty(String name, String value) {
        if (value == null) {
            System.clearProperty(name);
        } else {
            System.setProperty(name, value);
        }
    }
}

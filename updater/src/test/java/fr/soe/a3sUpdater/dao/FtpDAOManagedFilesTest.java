package fr.soe.a3sUpdater.dao;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FtpDAOManagedFilesTest {
    @TempDir
    Path temp;

    @Test
    void removesFilesManagedByPreviousUpdateButKeepsUserData() throws IOException {
        Path destination = temp.resolve("installation");
        Path source = temp.resolve("extracted");
        Files.createDirectories(destination.resolve("bin"));
        Files.writeString(destination.resolve("bin/old-launcher.exe"), "old", StandardCharsets.UTF_8);
        Files.writeString(destination.resolve("user-file.txt"), "keep", StandardCharsets.UTF_8);
        Files.writeString(destination.resolve(".a3s-updater-files"), "bin/old-launcher.exe\n", StandardCharsets.UTF_8);
        Files.createDirectories(destination.resolve("resources/configuration"));
        Files.writeString(destination.resolve("resources/configuration/updater.toml"), "existing user config", StandardCharsets.UTF_8);

        Files.createDirectories(source.resolve("bin"));
        Files.writeString(source.resolve("bin/new-launcher.exe"), "new", StandardCharsets.UTF_8);
        Files.writeString(source.resolve("user-file.txt"), "update-must-not-overwrite", StandardCharsets.UTF_8);
        Files.createDirectories(source.resolve("resources/configuration"));
        Files.writeString(source.resolve("resources/configuration/updater.toml"), "user config", StandardCharsets.UTF_8);

        FtpDAO.copyTree(source, destination);

        assertFalse(Files.exists(destination.resolve("bin/old-launcher.exe")));
        assertTrue(Files.exists(destination.resolve("bin/new-launcher.exe")));
        assertTrue(Files.exists(destination.resolve("user-file.txt")));
        assertTrue(Files.exists(destination.resolve("resources/configuration/updater.toml")));
        assertTrue(Files.readString(destination.resolve("resources/configuration/updater.toml"), StandardCharsets.UTF_8)
                .equals("existing user config"));
        assertTrue(Files.readString(destination.resolve(".a3s-updater-files"), StandardCharsets.UTF_8)
                .contains("bin/new-launcher.exe"));
    }

    @Test
    void replacesChangedFilesAndRemovesRenamedManagedFiles() throws IOException {
        Path destination = temp.resolve("installation-renamed");
        Path source = temp.resolve("extracted-renamed");
        Files.createDirectories(destination);
        Files.writeString(destination.resolve("Arma3Sync.jar"), "old-content", StandardCharsets.UTF_8);
        Files.writeString(destination.resolve(".a3s-updater-files"),
                "ArmA3Sync.jar\n", StandardCharsets.UTF_8);

        Files.createDirectories(source);
        Files.writeString(source.resolve("Arma3Sync.jar"), "new-content", StandardCharsets.UTF_8);

        FtpDAO.copyTree(source, destination);

        assertTrue(Files.readString(destination.resolve("Arma3Sync.jar"), StandardCharsets.UTF_8)
                .equals("new-content"));
        String managedFiles = Files.readString(destination.resolve(".a3s-updater-files"), StandardCharsets.UTF_8);
        assertTrue(managedFiles.contains("Arma3Sync.jar"));
        assertFalse(managedFiles.contains("ArmA3Sync.jar"));
    }

    @Test
    void keepsLegacyRootConfigurationDuringUpdateCleanup() throws IOException {
        Path destination = temp.resolve("legacy-installation");
        Path source = temp.resolve("legacy-extracted");
        Files.createDirectories(destination.resolve("bin"));
        Files.writeString(destination.resolve("a3s.cfg"), "repository paths", StandardCharsets.UTF_8);
        Files.writeString(destination.resolve("a3s.prefs"), "preferences", StandardCharsets.UTF_8);
        Files.writeString(destination.resolve(".a3s-updater-files"),
                "a3s.cfg\na3s.prefs\nbin/old-launcher.exe\n", StandardCharsets.UTF_8);
        Files.writeString(destination.resolve("bin/old-launcher.exe"), "old", StandardCharsets.UTF_8);

        Files.createDirectories(source.resolve("bin"));
        Files.writeString(source.resolve("bin/new-launcher.exe"), "new", StandardCharsets.UTF_8);

        FtpDAO.copyTree(source, destination);

        assertTrue(Files.exists(destination.resolve("a3s.cfg")));
        assertTrue(Files.exists(destination.resolve("a3s.prefs")));
        assertFalse(Files.exists(destination.resolve("bin/old-launcher.exe")));
        assertTrue(Files.exists(destination.resolve("bin/new-launcher.exe")));
    }

    @Test
    void rejectsUnsafeManagedFileStateBeforeDeletingAnything() throws IOException {
        Path destination = temp.resolve("installation");
        Path source = temp.resolve("extracted");
        Files.createDirectories(destination);
        Files.writeString(destination.resolve(".a3s-updater-files"), "../outside.txt\n", StandardCharsets.UTF_8);
        Files.writeString(destination.resolve("keep.txt"), "keep", StandardCharsets.UTF_8);
        Files.createDirectories(source);

        org.junit.jupiter.api.Assertions.assertThrows(IOException.class, () -> FtpDAO.copyTree(source, destination));
        assertTrue(Files.exists(destination.resolve("keep.txt")));
    }
}

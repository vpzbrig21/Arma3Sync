package fr.soe.a3sUpdater.dao;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class FtpDAOSafetyTest {
    @TempDir
    Path temp;

    @Test
    void rejectsZipEntryOutsideExtractionRoot() throws IOException {
        Path archive = createArchive("../../outside.txt", "blocked");
        assertThrows(IOException.class, () -> FtpDAO.extractSecurely(archive, temp.resolve("extract")));
        org.junit.jupiter.api.Assertions.assertFalse(Files.exists(temp.resolve("outside.txt")));
    }

    @Test
    void extractsNormalZipEntry() throws IOException {
        Path archive = createArchive("bin/launcher.txt", "ok");
        Path target = temp.resolve("extract");
        FtpDAO.extractSecurely(archive, target);
        assertEquals("ok", Files.readString(target.resolve("bin/launcher.txt")));
    }

    @Test
    void validatesEveryArchiveEntryBeforeInstallation() throws IOException {
        Path archive = createArchive("bin/launcher.txt", "ok");
        FtpDAO.validateArchive(archive);
    }

    @Test
    void rejectsTruncatedArchiveBeforeExtraction() throws IOException {
        Path archive = temp.resolve("truncated.zip");
        Files.write(archive, new byte[] { 0x50, 0x4b, 0x03, 0x04, 0x01 });
        assertThrows(IOException.class, () -> FtpDAO.validateArchive(archive));
    }

    @Test
    void copiesRuntimeLegalModuleUsingTheValidatedEntryList() throws IOException {
        Path archive = temp.resolve("runtime.zip");
        try (OutputStream output = Files.newOutputStream(archive);
             ZipOutputStream zip = new ZipOutputStream(output)) {
            zip.putNextEntry(new ZipEntry("runtime/legal/java.base/LICENSE"));
            zip.write("license".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("runtime/bin/java.exe"));
            zip.write(new byte[] { 1, 2, 3 });
            zip.closeEntry();
        }

        Path extracted = temp.resolve("runtime-extracted");
        Path destination = temp.resolve("runtime-installation");
        FtpDAO.validateArchive(archive);
        var entries = FtpDAO.stageArchive(archive, extracted);
        FtpDAO.copyStagedFiles(destination, entries);

        assertEquals("license",
                Files.readString(destination.resolve("runtime/legal/java.base/LICENSE")));
        org.junit.jupiter.api.Assertions.assertArrayEquals(new byte[] { 1, 2, 3 },
                Files.readAllBytes(destination.resolve("runtime/bin/java.exe")));
    }

    @Test
    void treatsWindowsStyleZipDirectoryEntriesAsDirectories() throws IOException {
        Path archive = temp.resolve("windows-style-directories.zip");
        try (OutputStream output = Files.newOutputStream(archive);
             ZipOutputStream zip = new ZipOutputStream(output)) {
            zip.putNextEntry(new ZipEntry("runtime\\legal\\"));
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("runtime\\legal\\java.base\\LICENSE"));
            zip.write("license".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            zip.closeEntry();
        }

        Path extracted = temp.resolve("windows-style-extracted");
        Path destination = temp.resolve("windows-style-installation");
        FtpDAO.validateArchive(archive);
        var entries = FtpDAO.stageArchive(archive, extracted);
        FtpDAO.copyStagedFiles(destination, entries);

        assertEquals("license",
                Files.readString(destination.resolve("runtime/legal/java.base/LICENSE")));
    }

    @Test
    void copiesBuiltStandardReleaseArchiveWhenAvailable() throws IOException {
        Path archive = Path.of("release/output/Arma3Sync-2026.4.4.zip");
        assumeTrue(Files.isRegularFile(archive), "local beta artifact is not available");

        Path extracted = temp.resolve("built-release-extracted");
        Path destination = temp.resolve("built-release-installation");
        FtpDAO.validateArchive(archive);
        var entries = FtpDAO.stageArchive(archive, extracted);
        FtpDAO.copyStagedFiles(destination, entries);

        assertTrue(Files.isRegularFile(destination.resolve("runtime/legal/java.base/LICENSE")));
        assertTrue(Files.isRegularFile(destination.resolve("runtime/bin/java.exe")));
    }

    private Path createArchive(String name, String content) throws IOException {
        Path archive = temp.resolve("update.zip");
        try (OutputStream output = Files.newOutputStream(archive);
             ZipOutputStream zip = new ZipOutputStream(output)) {
            zip.putNextEntry(new ZipEntry(name));
            zip.write(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return archive;
    }
}

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

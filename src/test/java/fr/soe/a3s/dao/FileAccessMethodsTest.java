package fr.soe.a3s.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileAccessMethodsTest {

    @TempDir
    Path tempDir;

    @Test
    void rejectsZipEntryOutsideExtractionRoot() throws Exception {
        Path archive = createArchive("../outside.txt", "blocked");
        Path target = tempDir.resolve("extract");

        assertThrows(IOException.class, () -> FileAccessMethods.extractToFolder(archive.toFile(), target.toFile()));
        assertFalse(Files.exists(tempDir.resolve("outside.txt")));
    }

    @Test
    void extractsNormalZipEntryInsideExtractionRoot() throws Exception {
        Path archive = createArchive("nested/file.txt", "ok");
        Path target = tempDir.resolve("extract");

        FileAccessMethods.extractToFolder(archive.toFile(), target.toFile());

        assertEquals("ok", Files.readString(target.resolve("nested/file.txt"), StandardCharsets.UTF_8));
    }

    private Path createArchive(String name, String content) throws IOException {
        Path archive = tempDir.resolve("archive.zip");
        try (OutputStream output = Files.newOutputStream(archive);
                ZipOutputStream zip = new ZipOutputStream(output)) {
            zip.putNextEntry(new ZipEntry(name));
            zip.write(content.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return archive;
    }
}

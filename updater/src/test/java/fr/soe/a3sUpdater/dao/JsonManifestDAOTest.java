package fr.soe.a3sUpdater.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;

import org.junit.jupiter.api.Test;

class JsonManifestDAOTest {

    private static final String HASH = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    @Test
    void acceptsStrictManifestAndIntegerSize() throws Exception {
        var manifest = new JsonManifestDAO().read(
                "{\"version\":\"2026.2.1\",\"file\":\"Arma3Sync-2026.2.1.zip\","
                        + "\"sha256\":\"" + HASH + "\",\"size\":1234}",
                URI.create("https://updates.example/a3s.json"));

        assertEquals("2026.2.1", manifest.version());
        assertEquals(1234L, manifest.size());
    }

    @Test
    void rejectsInvalidJsonNumbersAndVersions() {
        JsonManifestDAO dao = new JsonManifestDAO();
        URI source = URI.create("https://updates.example/a3s.json");
        assertThrows(Exception.class, () -> dao.read(
                "{\"version\":\"2026.2.1\",\"file\":\"update.zip\",\"sha256\":\"" + HASH
                        + "\",\"size\":1.5}", source));
        assertThrows(Exception.class, () -> dao.read(
                "{\"version\":\"2026.2\",\"file\":\"update.zip\",\"sha256\":\"" + HASH
                        + "\"}", source));
        assertThrows(Exception.class, () -> dao.read(
                "{\"version\":\"2026.2.1\",\"file\":\"update.zip\",\"sha256\":\"" + HASH
                        + "\",\"size\":01}", source));
    }

    @Test
    void rejectsNonHttpDownloadUrls() {
        assertThrows(Exception.class, () -> new JsonManifestDAO().read(
                "{\"version\":\"2026.2.1\",\"file\":\"update.zip\","
                        + "\"downloadUrl\":\"file:///tmp/update.zip\",\"sha256\":\"" + HASH + "\"}",
                URI.create("https://updates.example/a3s.json")));
    }
}

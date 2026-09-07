package fr.soe.a3sUpdater.dao;

import com.sun.net.httpserver.HttpServer;
import fr.soe.a3sUpdater.config.UpdateConfig;
import fr.soe.a3sUpdater.model.UpdateManifest;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GitHubReleaseDAOTest {
    @TempDir
    Path tempDir;

    @Test
    void selectsConfiguredAssetAndNormalizesTagDigest() throws Exception {
        String digest = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
        String response = "{\"tag_name\":\"v2026.1.1\",\"assets\":["
                + "{\"name\":\"Arma3Sync-2026.1.1.zip\","
                + "\"browser_download_url\":\"https://downloads.example/Arma3Sync-2026.1.1.zip\","
                + "\"digest\":\"sha256:" + digest + "\",\"size\":1234}]}";
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/latest", exchange -> {
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            try (var output = exchange.getResponseBody()) { output.write(bytes); }
        });
        server.start();
        try {
            Path installation = tempDir.resolve("installation");
            Files.createDirectories(installation);
            Files.writeString(installation.resolve("updater.toml"), "[update]\nallow_http = true\n");
            UpdateConfig config = UpdateConfig.load(installation);
            UpdateManifest manifest = new GitHubReleaseDAO().read(
                    java.net.URI.create("http://localhost:" + server.getAddress().getPort() + "/latest"),
                    "Arma3Sync-{version}.zip", new HttpDAO(), config);

            assertEquals("2026.1.1", manifest.version());
            assertEquals("Arma3Sync-2026.1.1.zip", manifest.fileName());
            assertEquals(digest, manifest.sha256());
            assertEquals(1234L, manifest.size());
            assertEquals(UpdateManifest.Format.GITHUB, manifest.format());
        } finally {
            server.stop(0);
        }
    }
}

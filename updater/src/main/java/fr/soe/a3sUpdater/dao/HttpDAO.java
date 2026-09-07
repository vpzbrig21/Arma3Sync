package fr.soe.a3sUpdater.dao;

import fr.soe.a3sUpdater.config.UpdateConfig;
import fr.soe.a3sUpdater.controller.Observateur;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

/** HTTPS transport used by the updated launcher; FTP remains available for legacy servers. */
public final class HttpDAO implements DataAccessConstants {
    private DownloadCountingOutputStream dos;
    private Path folderUpdate;
    private Path zipFile;
    private volatile HttpURLConnection activeConnection;

    public long getFileSize(URI uri, UpdateConfig config) throws IOException {
        HttpURLConnection connection = open(uri, config, "HEAD");
        try {
            int response = connection.getResponseCode();
            ensureSuccess(response);
            return connection.getContentLengthLong();
        } finally {
            connection.disconnect();
        }
    }

    public void setDownload(String fileName) throws IOException {
        String safeFileName = safeFileName(fileName);
        folderUpdate = Files.createTempDirectory("arma3sync-update-");
        zipFile = folderUpdate.resolve(safeFileName);
        dos = new DownloadCountingOutputStream(Files.newOutputStream(zipFile));
    }

    public void addObserver(Observateur observer) {
        if (dos != null) dos.addObservateur(observer);
    }

    public void download(URI uri, UpdateConfig config, String expectedSha256) throws IOException {
        HttpURLConnection connection = open(uri, config, "GET");
        activeConnection = connection;
        try {
            ensureSuccess(connection.getResponseCode());
            try (InputStream input = new BufferedInputStream(connection.getInputStream()); OutputStream output = dos) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) != -1) output.write(buffer, 0, read);
            }
            dos = null;
            if (expectedSha256 != null && !expectedSha256.isBlank()) verifySha256(expectedSha256);
        } finally {
            activeConnection = null;
            connection.disconnect();
        }
    }

    /** Interrupts an active HTTP download without deleting its staging folder. */
    public void cancel() {
        HttpURLConnection connection = activeConnection;
        if (connection != null) connection.disconnect();
        DownloadCountingOutputStream stream = dos;
        if (stream != null) {
            try { stream.close(); } catch (IOException ignored) { }
        }
    }

    public String readText(URI uri, UpdateConfig config) throws IOException {
        return readText(uri, config, Map.of());
    }

    public String readText(URI uri, UpdateConfig config, Map<String, String> headers) throws IOException {
        HttpURLConnection connection = open(uri, config, "GET");
        try {
            headers.forEach(connection::setRequestProperty);
            ensureSuccess(connection.getResponseCode());
            long contentLength = connection.getContentLengthLong();
            if (contentLength > 1_048_576L) throw new IOException("Update manifest is larger than 1 MiB.");
            try (InputStream input = new BufferedInputStream(connection.getInputStream())) {
                byte[] bytes = input.readNBytes(1_048_577);
                if (bytes.length > 1_048_576) throw new IOException("Update manifest is larger than 1 MiB.");
                return new String(bytes, StandardCharsets.UTF_8);
            }
        } finally {
            connection.disconnect();
        }
    }

    public void install(Path installationPath) throws IOException {
        if (zipFile == null || !Files.isRegularFile(zipFile)) throw new IOException("Update archive not found.");
        Path extracted = folderUpdate.resolve("extracted");
        Files.createDirectories(extracted);
        FtpDAO.extractSecurely(zipFile, extracted);
        FtpDAO.copyTree(extracted, installationPath);
    }

    public void clean() {
        if (folderUpdate == null) return;
        try (var paths = Files.walk(folderUpdate)) {
            paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                try { Files.deleteIfExists(path); } catch (IOException ignored) { }
            });
        } catch (IOException ignored) { }
    }

    private HttpURLConnection open(URI uri, UpdateConfig config, String method) throws IOException {
        if (uri == null || (!"https".equalsIgnoreCase(uri.getScheme()) && !"http".equalsIgnoreCase(uri.getScheme()))) {
            throw new IOException("Update URL must use HTTP or HTTPS.");
        }
        if (!"https".equalsIgnoreCase(uri.getScheme()) && !config.allowHttp()) {
            throw new IOException("Updater requires HTTPS; set -Da3s.updater.allowHttp=true for local tests.");
        }
        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(config.connectTimeoutMs());
        connection.setReadTimeout(config.readTimeoutMs());
        connection.setRequestProperty("User-Agent", "ArmA3Sync-Updater");
        return connection;
    }

    private static void ensureSuccess(int response) throws IOException {
        if (response < 200 || response >= 300) throw new IOException("Update server returned HTTP " + response + ".");
    }

    private void verifySha256(String expectedSha256) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = new BufferedInputStream(Files.newInputStream(zipFile))) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) != -1) digest.update(buffer, 0, read);
            }
            byte[] bytes = digest.digest();
            StringBuilder actual = new StringBuilder(64);
            for (byte value : bytes) actual.append(String.format("%02x", value));
            if (!actual.toString().equalsIgnoreCase(expectedSha256.trim())) {
                throw new IOException("SHA-256 verification failed for the update archive.");
            }
        } catch (NoSuchAlgorithmException exception) {
            throw new IOException("SHA-256 is unavailable in this Java runtime.", exception);
        }
    }

    private static String safeFileName(String fileName) throws IOException {
        Path path = Path.of(fileName).normalize();
        if (path.isAbsolute() || path.getNameCount() != 1 || !fileName.toLowerCase().endsWith(".zip")) {
            throw new IOException("Invalid update archive name: " + fileName);
        }
        return path.getFileName().toString();
    }
}

package fr.soe.a3sUpdater.dao;

import fr.soe.a3sUpdater.controller.Observateur;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class FtpDAO implements DataAccessConstants {
    private static final String MANAGED_FILES_NAME = ".a3s-updater-files";

    private DownloadCountingOutputStream dos;
    private Path folderUpdate;
    private Path zipFile;

    public long getFtpFileSize(String fileName, FTPClient ftpClient, boolean devMode) throws IOException {
        String safeFileName = safeFileName(fileName);
        changeRepository(ftpClient, devMode);
        FTPFile[] files = ftpClient.listFiles(safeFileName);
        return files.length == 0 ? 0L : files[0].getSize();
    }

    public void setDownload(String fileName) throws IOException {
        String safeFileName = safeFileName(fileName);
        folderUpdate = Files.createTempDirectory("arma3sync-update-");
        zipFile = folderUpdate.resolve(safeFileName);
        dos = new DownloadCountingOutputStream(Files.newOutputStream(zipFile));
    }

    public boolean download(String fileName, FTPClient ftpClient, boolean devMode) throws IOException {
        String safeFileName = safeFileName(fileName);
        changeRepository(ftpClient, devMode);
        boolean result;
        try (OutputStream output = dos) {
            result = ftpClient.retrieveFile(safeFileName, output);
        }
        dos = null;
        return result;
    }

    public DownloadCountingOutputStream getDos() { return dos; }

    /** Closes the active download stream; the owning FTP client is cancelled by Service. */
    public void cancel() {
        if (dos != null) {
            try { dos.close(); } catch (IOException ignored) { }
        }
    }

    public void addObserver(Observateur observer) {
        if (dos != null) dos.addObservateur(observer);
    }

    public void install(Path installationPath) throws IOException {
        if (zipFile == null || !Files.isRegularFile(zipFile)) {
            throw new IOException("Update archive not found: " + zipFile);
        }

        Path extracted = folderUpdate.resolve("extracted");
        Files.createDirectories(extracted);
        extractSecurely(zipFile, extracted);
        copyTree(extracted, installationPath);
    }

    public void clean() {
        if (folderUpdate == null) return;
        try {
            Files.walk(folderUpdate)
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try { Files.deleteIfExists(path); } catch (IOException ignored) { }
                    });
        } catch (IOException ignored) { }
    }

    private void changeRepository(FTPClient client, boolean devMode) throws IOException {
        String configured = System.getProperty(devMode ? UPDATE_REPOSITORY_DEV_PROPERTY : UPDATE_REPOSITORY_PROPERTY);
        String repository = configured == null || configured.isBlank()
                ? (devMode ? UPDATE_REPOSITORY_DEV : UPDATE_REPOSITORY)
                : configured;
        if (!client.changeWorkingDirectory(repository)) {
            throw new IOException("FTP repository is not available: " + repository);
        }
    }

    private static String safeFileName(String fileName) throws IOException {
        Path path = Path.of(fileName).normalize();
        if (path.isAbsolute() || path.getNameCount() != 1 || !fileName.toLowerCase().endsWith(".zip")) {
            throw new IOException("Invalid update archive name: " + fileName);
        }
        return path.getFileName().toString();
    }

    static void extractSecurely(Path archive, Path target) throws IOException {
        Path normalizedTarget = target.toAbsolutePath().normalize();
        Files.createDirectories(normalizedTarget);
        try (ZipInputStream input = new ZipInputStream(new BufferedInputStream(Files.newInputStream(archive)))) {
            ZipEntry entry;
            byte[] buffer = new byte[8192];
            while ((entry = input.getNextEntry()) != null) {
                Path destination = normalizedTarget.resolve(entry.getName()).normalize();
                if (!destination.startsWith(normalizedTarget)) {
                    throw new IOException("Unsafe ZIP entry: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(destination);
                    continue;
                }
                Files.createDirectories(destination.getParent());
                try (OutputStream output = new BufferedOutputStream(Files.newOutputStream(destination))) {
                    int read;
                    while ((read = input.read(buffer)) != -1) output.write(buffer, 0, read);
                }
            }
        }
    }

    static void copyTree(Path source, Path destination) throws IOException {
        Path normalizedSource = source.toAbsolutePath().normalize();
        Path normalizedDestination = destination.toAbsolutePath().normalize();
        Files.createDirectories(normalizedDestination);

        Set<String> previousFiles = readManagedFiles(normalizedDestination.resolve(MANAGED_FILES_NAME), normalizedDestination);
        Set<String> currentFiles = new LinkedHashSet<>();

        try (var paths = Files.walk(source)) {
            for (Path path : paths.toList()) {
                Path relative = normalizedSource.relativize(path.toAbsolutePath().normalize());
                if (!Files.isDirectory(path) && isUserManagedFile(relative)) continue;
                if (!Files.isDirectory(path) && toPortablePath(relative).equalsIgnoreCase(MANAGED_FILES_NAME)) continue;
                Path target = normalizedDestination.resolve(relative).normalize();
                if (!target.startsWith(normalizedDestination)) {
                    throw new IOException("Unsafe update path: " + relative);
                }
                if (Files.isDirectory(path)) {
                    Files.createDirectories(target);
                } else {
                    Files.createDirectories(target.getParent());
                    Files.copy(path, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
                    currentFiles.add(toPortablePath(relative));
                }
            }
        }

        for (String file : previousFiles) {
            if (currentFiles.stream().anyMatch(current -> sameManagedPath(current, file))) continue;
            if (isUserManagedFile(Path.of(file))) continue;
            Path target = resolveManagedPath(file, normalizedDestination);
            Files.deleteIfExists(target);
        }

        writeManagedFiles(normalizedDestination.resolve(MANAGED_FILES_NAME), normalizedDestination, currentFiles);
    }

    private static Set<String> readManagedFiles(Path stateFile, Path destination) throws IOException {
        if (!Files.exists(stateFile)) return Set.of();
        if (!Files.isRegularFile(stateFile)) throw new IOException("Updater state is not a file: " + stateFile);

        Set<String> files = new LinkedHashSet<>();
        for (String line : Files.readAllLines(stateFile, StandardCharsets.UTF_8)) {
            if (line.isBlank()) continue;
            resolveManagedPath(line, destination);
            files.add(line.replace('\\', '/'));
        }
        return files;
    }

    private static Path resolveManagedPath(String relative, Path destination) throws IOException {
        Path path;
        try {
            path = Path.of(relative);
        } catch (RuntimeException exception) {
            throw new IOException("Invalid updater state path: " + relative, exception);
        }
        Path resolved = destination.resolve(path).normalize();
        if (path.isAbsolute() || resolved.equals(destination) || !resolved.startsWith(destination)) {
            throw new IOException("Unsafe updater state path: " + relative);
        }
        return resolved;
    }

    private static String toPortablePath(Path relative) {
        return relative.toString().replace('\\', '/');
    }

    private static boolean sameManagedPath(String left, String right) {
        if (isWindows()) return left.equalsIgnoreCase(right);
        return left.equals(right);
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).contains("win");
    }

    private static void writeManagedFiles(Path stateFile, Path destination, Set<String> files) throws IOException {
        Path temporary = Files.createTempFile(destination, MANAGED_FILES_NAME + ".", ".tmp");
        try {
            Files.write(temporary, new ArrayList<>(files), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, stateFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (java.nio.file.AtomicMoveNotSupportedException exception) {
                Files.move(temporary, stateFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static boolean isUserManagedFile(Path relative) {
        String value = relative.toString().replace('\\', '/');
        return value.equalsIgnoreCase("a3s.cfg")
                || value.equalsIgnoreCase("a3s.prefs")
                || value.equalsIgnoreCase("a3s.xml")
                || value.equalsIgnoreCase("updater.toml")
                || value.equalsIgnoreCase("resources/configuration/updater.toml")
                || value.equalsIgnoreCase("resources/configuration/a3s.cfg")
                || value.equalsIgnoreCase("resources/configuration/a3s.prefs")
                || value.equalsIgnoreCase("resources/configuration/a3s.xml")
                || value.equalsIgnoreCase("profiles")
                || value.startsWith("profiles/")
                || value.equalsIgnoreCase("resources/ftp")
                || value.startsWith("resources/ftp/")
                || value.equalsIgnoreCase("resources/temp")
                || value.startsWith("resources/temp/");
    }
}

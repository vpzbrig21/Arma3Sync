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
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public class FtpDAO implements DataAccessConstants {
    private static final String MANAGED_FILES_NAME = ".a3s-updater-files";

    static record StagedEntry(Path source, Path relativePath) { }

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

        try {
            validateArchive(zipFile);
        } catch (IOException exception) {
            throw stageFailure("archive validation", exception);
        }
        // Keep extraction outside the download staging tree. The extracted source
        // must remain stable for the complete copy operation and must not be
        // confused with the archive cleanup path.
        Path extracted = Files.createTempDirectory("arma3sync-update-extracted-");
        try {
            List<StagedEntry> extractedEntries;
            try {
                extractedEntries = stageArchive(zipFile, extracted);
            } catch (IOException exception) {
                throw stageFailure("archive extraction", exception);
            }
            try {
                copyStagedFiles(installationPath, extractedEntries);
            } catch (IOException exception) {
                throw stageFailure("installation copy", exception);
            }
        } finally {
            deleteTree(extracted);
        }
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

    /**
     * Reads every ZIP entry before installation. This validates the central
     * directory, entry paths and CRC checksums before any target files are
     * modified.
     */
    static void validateArchive(Path archive) throws IOException {
        int fileCount = 0;
        try (ZipFile zip = new ZipFile(archive.toFile())) {
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                validateEntryPath(entry.getName());
                if (isDirectoryEntry(entry)) continue;
                fileCount++;
                try (InputStream input = new BufferedInputStream(zip.getInputStream(entry))) {
                    byte[] buffer = new byte[64 * 1024];
                    while (input.read(buffer) != -1) {
                        // Reading the complete stream makes ZipFile verify the
                        // entry checksum before installation starts.
                    }
                }
            }
        } catch (java.util.zip.ZipException exception) {
            throw new IOException("Update archive is invalid or incomplete: " + exception.getMessage(), exception);
        }
        if (fileCount == 0) {
            throw new IOException("Update archive is empty.");
        }
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
        extractSecurelyWithEntries(archive, target);
    }

    /** Extracts the archive and returns the exact entries that were created. */
    static List<Path> extractSecurelyWithEntries(Path archive, Path target) throws IOException {
        Path normalizedTarget = target.toAbsolutePath().normalize();
        Files.createDirectories(normalizedTarget);
        List<Path> extractedEntries = new ArrayList<>();
        try (ZipInputStream input = new ZipInputStream(new BufferedInputStream(Files.newInputStream(archive)))) {
            ZipEntry entry;
            byte[] buffer = new byte[64 * 1024];
            while ((entry = input.getNextEntry()) != null) {
                Path destination = resolveEntryPath(normalizedTarget, entry.getName());
                if (isDirectoryEntry(entry)) {
                    Files.createDirectories(destination);
                    extractedEntries.add(destination);
                    continue;
                }
                try {
                    Files.createDirectories(destination.getParent());
                } catch (IOException exception) {
                    throw new IOException("Could not create extraction directory '"
                            + destination.getParent() + "' for ZIP entry '" + entry.getName() + "'.", exception);
                }
                try (OutputStream output = new BufferedOutputStream(Files.newOutputStream(destination))) {
                    int read;
                    while ((read = input.read(buffer)) != -1) output.write(buffer, 0, read);
                }
                extractedEntries.add(destination);
            }
        }
        return List.copyOf(extractedEntries);
    }

    /**
     * Stages archive files as flat temporary files. This avoids relying on the
     * Windows filesystem to create nested runtime/legal directories while an
     * update is being unpacked.
     */
    static List<StagedEntry> stageArchive(Path archive, Path target) throws IOException {
        Path normalizedTarget = target.toAbsolutePath().normalize();
        Files.createDirectories(normalizedTarget);
        List<StagedEntry> stagedEntries = new ArrayList<>();
        try (ZipInputStream input = new ZipInputStream(new BufferedInputStream(Files.newInputStream(archive)))) {
            ZipEntry entry;
            byte[] buffer = new byte[64 * 1024];
            while ((entry = input.getNextEntry()) != null) {
                Path destination = resolveEntryPath(normalizedTarget, entry.getName());
                if (isDirectoryEntry(entry)) continue;

                Path relative = normalizedTarget.relativize(destination);
                Path stagedFile = Files.createTempFile(normalizedTarget, "entry-", ".tmp");
                try (OutputStream output = new BufferedOutputStream(Files.newOutputStream(stagedFile))) {
                    int read;
                    while ((read = input.read(buffer)) != -1) output.write(buffer, 0, read);
                } catch (IOException exception) {
                    Files.deleteIfExists(stagedFile);
                    throw new IOException("Could not stage ZIP entry '" + entry.getName() + "'.", exception);
                }
                stagedEntries.add(new StagedEntry(stagedFile, relative));
            }
        }
        return List.copyOf(stagedEntries);
    }

    private static void validateEntryPath(String entryName) throws IOException {
        resolveEntryPath(Path.of("." ).toAbsolutePath().normalize(), entryName);
    }

    /**
     * ZIP requires forward slashes, but the Windows release packager can emit
     * directory entries with backslashes. Treat both forms as directories so
     * an entry such as runtime\\legal\\ is never copied over an existing
     * directory as if it were a file.
     */
    private static boolean isDirectoryEntry(ZipEntry entry) {
        String name = entry.getName();
        return entry.isDirectory() || name.endsWith("/") || name.endsWith("\\");
    }

    private static Path resolveEntryPath(Path root, String entryName) throws IOException {
        if (entryName == null || entryName.isBlank()) {
            throw new IOException("Update archive contains an empty entry name.");
        }
        Path destination;
        try {
            destination = root.resolve(entryName.replace('\\', '/')).normalize();
        } catch (RuntimeException exception) {
            throw new IOException("Invalid ZIP entry: " + entryName, exception);
        }
        if (!destination.startsWith(root)) {
            throw new IOException("Unsafe ZIP entry: " + entryName);
        }
        return destination;
    }

    static void copyTree(Path source, Path destination) throws IOException {
        Path normalizedSource = source.toAbsolutePath().normalize();
        final List<Path> sourceEntries;
        try (var paths = Files.walk(source)) {
            sourceEntries = paths.filter(path -> !Files.isDirectory(path)).toList();
        } catch (NoSuchFileException exception) {
            throw missingSource(Path.of(exception.getFile()));
        }
        copyTree(source, destination, sourceEntries);
    }

    static void copyTree(Path source, Path destination, List<Path> sourceEntries) throws IOException {
        Path normalizedSource = source.toAbsolutePath().normalize();
        List<StagedEntry> stagedEntries = new ArrayList<>();
        for (Path path : sourceEntries) {
            Path normalizedPath = path.toAbsolutePath().normalize();
            if (!normalizedPath.startsWith(normalizedSource) || !Files.isRegularFile(normalizedPath)) {
                throw missingSource(normalizedPath);
            }
            Path relative = normalizedSource.relativize(normalizedPath);
            stagedEntries.add(new StagedEntry(normalizedPath, relative));
        }
        copyStagedFiles(destination, stagedEntries);
    }

    static void copyStagedFiles(Path destination, List<StagedEntry> stagedEntries) throws IOException {
        Path normalizedDestination = destination.toAbsolutePath().normalize();
        Files.createDirectories(normalizedDestination);

        Set<String> previousFiles = readManagedFiles(normalizedDestination.resolve(MANAGED_FILES_NAME), normalizedDestination);
        Set<String> currentFiles = new LinkedHashSet<>();

        for (StagedEntry entry : stagedEntries) {
            Path source = entry.source().toAbsolutePath().normalize();
            Path relative = entry.relativePath().normalize();
            if (!Files.isRegularFile(source)) throw missingSource(source);
            if (relative.isAbsolute() || relative.getNameCount() == 0) {
                throw new IOException("Unsafe update path: " + relative);
            }
            if (isUserManagedFile(relative)) continue;
            if (toPortablePath(relative).equalsIgnoreCase(MANAGED_FILES_NAME)) continue;
            Path target = normalizedDestination.resolve(relative).normalize();
            if (!target.startsWith(normalizedDestination)) {
                throw new IOException("Unsafe update path: " + relative);
            }
            Files.createDirectories(target.getParent());
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
            currentFiles.add(toPortablePath(relative));
        }

        for (String file : previousFiles) {
            if (currentFiles.stream().anyMatch(current -> sameManagedPath(current, file))) continue;
            if (isUserManagedFile(Path.of(file))) continue;
            Path target = resolveManagedPath(file, normalizedDestination);
            Files.deleteIfExists(target);
        }

        writeManagedFiles(normalizedDestination.resolve(MANAGED_FILES_NAME), normalizedDestination, currentFiles);
    }

    private static IOException missingSource(Path path) {
        return new NoSuchFileException(path.toString(), null,
                "Update archive extraction is incomplete; source entry is missing");
    }

    static IOException stageFailure(String stage, IOException exception) {
        String location = exception instanceof NoSuchFileException missing
                ? " Missing path: " + missing.getFile() + "." : "";
        return new IOException("Update " + stage + " failed." + location + " "
                + exception.getMessage(), exception);
    }

    static void deleteTree(Path root) {
        try (var paths = Files.walk(root)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try { Files.deleteIfExists(path); } catch (IOException ignored) { }
            });
        } catch (IOException ignored) { }
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

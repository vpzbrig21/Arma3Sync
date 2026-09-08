package fr.soe.a3s.dao.connection.sftp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.future.ConnectFuture;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.future.SshFutureListener;
import org.apache.sshd.core.CoreModuleProperties;
import org.apache.sshd.sftp.client.SftpClient;
import org.apache.sshd.sftp.client.SftpClientFactory;
import org.apache.sshd.sftp.common.SftpConstants;

import fr.soe.a3s.dao.connection.AbstractConnexionDAO;
import fr.soe.a3s.dao.connection.RemoteFile;
import fr.soe.a3s.domain.AbstractProtocole;
import fr.soe.a3s.domain.repository.Repository;
import fr.soe.a3s.dto.sync.SyncTreeLeafDTO;
import fr.soe.a3s.exception.ConnectionExceptionFactory;
import fr.soe.a3s.exception.IncompleteFileTransferException;
import fr.soe.a3s.utils.DebugLogger;

/** SFTP transport using a single SSH/SFTP session for a repository upload. */
public class SftpDAO extends AbstractConnexionDAO {

    private static final int LOCAL_BUFFER_SIZE = 1024 * 1024;
    /* Keep SFTP request buffering conservative for servers with smaller
     * channel/window limits. The local file I/O buffer remains large. */
    private static final int SFTP_WRITE_BUFFER_SIZE = 64 * 1024;

    private volatile SshClient sshClient;
    private volatile ConnectFuture pendingConnection;
    private volatile ClientSession session;
    private volatile SftpClient sftpClient;
    private volatile String baseDirectory;
    private long currentDownloadOffset;
    private long checkedFiles;
    private long uploadedFiles;

    @Override
    public void beginUploadSession(AbstractProtocole protocol) throws IOException {
        if (isUploadSessionActive()) {
            DebugLogger.info("SFTP upload session already active.");
            return;
        }
        try {
            DebugLogger.info("SFTP upload session starting: " + DebugLogger.describeProtocol(protocol));
            updateObserverText("Connecting to SFTP server...");
            connect(protocol, null, 0, -1);
            if (isCanceled()) {
                setUploadSessionActive(false);
                DebugLogger.info("SFTP upload session canceled during connection setup.");
                return;
            }
            setUploadSessionActive(true);
            DebugLogger.info("SFTP upload session ready. baseDirectory=" + baseDirectory);
        } catch (IOException e) {
            DebugLogger.error("SFTP upload session could not be started.", e);
            disconnect();
            throw e;
        }
    }

    @Override
    public void endUploadSession() {
        if (isUploadSessionActive()) {
            try {
                DebugLogger.info("SFTP upload session closing. checkedFiles=" + checkedFiles
                        + ", uploadedFiles=" + uploadedFiles);
                disconnect();
            } finally {
                setUploadSessionActive(false);
            }
        }
    }

    @Override
    protected void connect(AbstractProtocole protocol, RemoteFile remoteFile, long startOffset, long endOffset)
            throws IOException {
        try {
            int timeout = timeoutMillis(protocol.getConnectionTimeOut());
            DebugLogger.info("SFTP connection setup started: " + DebugLogger.describeProtocol(protocol)
                    + ", timeoutMs=" + timeout);
            DebugLogger.info("SFTP runtime libraries: sshdClient="
                    + DebugLogger.describeCodeSource(SshClient.class) + ", sshdSftp="
                    + DebugLogger.describeCodeSource(SftpClient.class));
            sshClient = SshClient.setUpDefaultClient();
            Duration timeoutDuration = Duration.ofMillis(timeout);
            CoreModuleProperties.IO_CONNECT_TIMEOUT.set(sshClient, timeoutDuration);
            CoreModuleProperties.AUTH_TIMEOUT.set(sshClient, timeoutDuration);
            CoreModuleProperties.CHANNEL_OPEN_TIMEOUT.set(sshClient, timeoutDuration);
            CoreModuleProperties.IDLE_TIMEOUT.set(sshClient, timeoutDuration);
            sshClient.start();
            DebugLogger.info("SFTP SSH client started; opening SSH connection.");
            throwIfCanceled();

            String normalizedUrl = normalizeUrl(protocol.getUrl());
            String hostname = getHostname(normalizedUrl);
            if (hostname.isEmpty()) {
                throw new IOException("SFTP host is empty. Enter a host name before the optional remote path.");
            }
            String configuredRemotePath = getRemotePath(normalizedUrl);
            DebugLogger.info("SFTP endpoint parsed: host=" + hostname + ", configuredRemotePath="
                    + (configuredRemotePath.isEmpty() ? "<home>" : configuredRemotePath));

            int port = Integer.parseInt(protocol.getPort());
            InetAddress remoteAddress = resolveRemoteAddress(hostname);
            DebugLogger.info("SFTP endpoint resolved: host=" + hostname + ", selectedAddress="
                    + remoteAddress.getHostAddress() + ", port=" + port);

            ConnectFuture connection = sshClient.connect(protocol.getLogin(),
                    new InetSocketAddress(remoteAddress, port));
            pendingConnection = connection;
            connection.addListener((SshFutureListener<ConnectFuture>) future -> {
                if (future.isConnected()) {
                    DebugLogger.info("SFTP SSH connection future completed successfully.");
                } else {
                    try {
                        future.verify();
                    } catch (Exception e) {
                        DebugLogger.error("SFTP SSH connection future completed with an error.", e);
                    }
                }
            });
            DebugLogger.info("SFTP SSH connection request dispatched.");
            waitForConnection(connection, timeout);
            session = connection.getSession();
            pendingConnection = null;
            DebugLogger.info("SFTP SSH connection established; authenticating.");
            throwIfCanceled();
            if (protocol.getPassword() == null || protocol.getPassword().isEmpty()) {
                throw new IOException("SFTP password is empty. Key-based authentication is not configured yet.");
            }
            session.addPasswordIdentity(protocol.getPassword());
            session.auth().verify(timeout);
            DebugLogger.info("SFTP authentication succeeded; opening SFTP subsystem.");
            throwIfCanceled();

            sftpClient = SftpClientFactory.instance().createSftpClient(session);
            DebugLogger.info("SFTP subsystem ready; resolving remote working directory.");
            throwIfCanceled();
            String home = sftpClient.canonicalPath(".");
            if (home == null || home.isEmpty()) {
                throw new IOException("SFTP server did not provide a working directory.");
            }
            baseDirectory = resolveConfiguredDirectory(home, configuredRemotePath);
            currentDownloadOffset = Math.max(0, startOffset);
            checkedFiles = 0;
            uploadedFiles = 0;
            DebugLogger.info("SFTP remote working directory resolved: home=" + home + ", baseDirectory="
                    + baseDirectory);

            if (remoteFile != null && !remoteFile.isDirectory()) {
                String remotePath = remotePath(remoteFile);
                if (!exists(remotePath)) {
                    throw new FileNotFoundException("Remote file not found: " + remoteFile.getRelativeFilePath());
                }
            }
        } catch (IOException e) {
            DebugLogger.error("SFTP connection setup failed.", e);
            disconnect();
            if (!isCanceled()) {
                throw ConnectionExceptionFactory.Exception(
                        "Failed to connect to the SFTP server on url: " + protocol.getHostUrl(), e);
            }
        }
    }

    @Override
    public void cancel() {
        DebugLogger.info("SFTP cancellation requested.");
        super.cancel();
        ConnectFuture connection = pendingConnection;
        if (connection != null && !connection.isDone()) {
            DebugLogger.info("SFTP canceling the pending SSH connection future.");
            connection.cancel();
        }
        /* Do not close SSH synchronously on the Swing event thread. */
        Thread closer = new Thread(this::disconnect, "Arma3Sync-SFTP-cancel");
        closer.setDaemon(true);
        closer.start();
    }

    @Override
    protected void disconnect() {
        ConnectFuture connection = pendingConnection;
        pendingConnection = null;
        if (connection != null && !connection.isDone()) {
            connection.cancel();
        }
        closeQuietly(sftpClient);
        sftpClient = null;
        closeQuietly(session);
        session = null;
        if (sshClient != null) {
            try {
                sshClient.stop();
            } catch (Exception ignored) {
            }
            sshClient = null;
        }
        baseDirectory = null;
        currentDownloadOffset = 0;
    }

    @Override
    protected void downloadFile(File file, RemoteFile remoteFile, boolean doRecordProgress, boolean doControlSpeed)
            throws IOException, IncompleteFileTransferException {
        ensureConnected();
        long offset = currentDownloadOffset;
        try (InputStream input = sftpClient.read(remotePath(remoteFile));
                FileOutputStream output = new FileOutputStream(file, offset > 0)) {
            skipFully(input, offset);
            DownloadProgressListener progress = new DownloadProgressListener(doRecordProgress);
            progress.init(output);
            byte[] buffer = new byte[LOCAL_BUFFER_SIZE];
            int count;
            while ((count = input.read(buffer)) != -1 && !isCanceled()) {
                output.write(buffer, 0, count);
                progress.write(buffer, count);
            }
            progress.close();
        } catch (IOException e) {
            if (!isCanceled()) {
                throw ConnectionExceptionFactory.Exception("Failed to retrieve file: "
                        + remoteFile.getRelativeFilePath(), e);
            }
        }
    }

    @Override
    public void downloadPartialFile(File file, Repository repository, SyncTreeLeafDTO leaf) throws IOException {
        RemoteFile remoteFile = new RemoteFile(leaf.getName(), leaf.getParentRelativePath(), false);
        connect(repository.getProtocol(), remoteFile, 0, -1);
        try {
            if (file.exists() && !file.delete()) {
                throw new IOException("Unable to replace incomplete local file: " + file);
            }
            downloadFile(file, remoteFile, false, false);
        } catch (IncompleteFileTransferException e) {
            throw new IOException(e.getMessage(), e);
        } finally {
            disconnect();
        }
    }

    @Override
    protected boolean fileExists(RemoteFile remoteFile) throws IOException {
        ensureConnected();
        String path = remotePath(remoteFile);
        boolean found = exists(path);
        long checked = ++checkedFiles;
        if (checked == 1 || checked % 25 == 0) {
            DebugLogger.info("SFTP remote check progress: checked=" + checked + ", lastPath=" + path);
        }
        return found;
    }

    @Override
    protected void uploadFile(File file, RemoteFile remoteFile, boolean doRecordProgress) throws IOException {
        ensureConnected();
        if (remoteFile.isDirectory()) {
            DebugLogger.info("SFTP creating directory: " + remotePath(remoteFile));
            makeDirectory(remotePath(remoteFile));
            return;
        }

        long start = System.nanoTime();
        String targetPath = remotePath(remoteFile);
        DebugLogger.info("SFTP file upload started: local=" + file.getAbsolutePath() + ", size=" + file.length()
                + ", remote=" + targetPath);
        makeDirectory(remotePathParent(remoteFile));
        try (FileInputStream input = new FileInputStream(file);
                OutputStream output = sftpClient.write(targetPath, SFTP_WRITE_BUFFER_SIZE,
                        EnumSet.of(SftpClient.OpenMode.Create, SftpClient.OpenMode.Write, SftpClient.OpenMode.Truncate))) {
            UploadProgressListener progress = new UploadProgressListener();
            progress.init(input, doRecordProgress);
            byte[] buffer = new byte[LOCAL_BUFFER_SIZE];
            int count;
            while ((count = progress.read(buffer)) != -1 && !isCanceled()) {
                output.write(buffer, 0, count);
            }
            progress.close();
            long uploaded = ++uploadedFiles;
            DebugLogger.info("SFTP file upload finished: remote=" + targetPath + ", durationMs="
                    + ((System.nanoTime() - start) / 1_000_000) + ", uploadedFiles=" + uploaded);
        } catch (IOException e) {
            DebugLogger.error("SFTP file upload failed: local=" + file.getAbsolutePath() + ", remote="
                    + targetPath + ", durationMs=" + ((System.nanoTime() - start) / 1_000_000), e);
            if (!isCanceled()) {
                throw ConnectionExceptionFactory.Exception("Failed to upload file: " + file.getAbsolutePath()
                        + "\nTo repository directory: " + remoteFile.getParentDirectoryRelativePath(), e);
            }
        }
    }

    @Override
    protected void uploadObjectFile(Object object, RemoteFile remoteFile) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream objectOutput = new ObjectOutputStream(new GZIPOutputStream(bytes))) {
            objectOutput.writeObject(object);
        }
        makeDirectory(remotePathParent(remoteFile));
        String targetPath = remotePath(remoteFile);
        DebugLogger.info("SFTP metadata upload started: remote=" + targetPath + ", size=" + bytes.size());
        try (InputStream input = new ByteArrayInputStream(bytes.toByteArray());
                OutputStream output = sftpClient.write(targetPath, SFTP_WRITE_BUFFER_SIZE,
                        EnumSet.of(SftpClient.OpenMode.Create, SftpClient.OpenMode.Write, SftpClient.OpenMode.Truncate))) {
            input.transferTo(output);
            DebugLogger.info("SFTP metadata upload finished: remote=" + targetPath);
        } catch (IOException e) {
            DebugLogger.error("SFTP metadata upload failed: remote=" + targetPath, e);
            if (!isCanceled()) {
                throw ConnectionExceptionFactory.Exception("Failed to upload file: "
                        + remoteFile.getRelativeFilePath(), e);
            }
        }
    }

    @Override
    protected void deleteFile(RemoteFile remoteFile) throws IOException {
        ensureConnected();
        deletePath(remotePath(remoteFile), remoteFile.isDirectory());
    }

    @Override
    public String checkPartialFileTransfer(Repository repository) {
        return null;
    }

    @Override
    public double getFileCompletion(Repository repository, SyncTreeLeafDTO leaf) {
        return 0;
    }

    private void deletePath(String path, boolean directory) throws IOException {
        if (!exists(path)) {
            return;
        }
        if (directory) {
            for (SftpClient.DirEntry entry : sftpClient.readDir(path)) {
                String name = entry.getFilename();
                if (!".".equals(name) && !"..".equals(name)) {
                    deletePath(join(path, name), entry.getAttributes().isDirectory());
                }
            }
            sftpClient.rmdir(path);
        } else {
            sftpClient.remove(path);
        }
    }

    private void makeDirectory(String path) throws IOException {
        if (path == null || path.isEmpty() || ".".equals(path)) {
            return;
        }
        String normalized = path.replace('\\', '/');
        String[] parts = normalized.split("/");
        String current = normalized.startsWith("/") ? "/" : "";
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            current = join(current, part);
            if (!exists(current)) {
                sftpClient.mkdir(current);
            }
        }
    }

    private boolean exists(String path) throws IOException {
        try {
            sftpClient.stat(path);
            return true;
        } catch (IOException e) {
            if (isMissing(e)) {
                return false;
            }
            throw e;
        }
    }

    private String remotePath(RemoteFile remoteFile) throws IOException {
        String relativePath = remoteFile.getRelativeFilePath();
        validateRelativePath(relativePath);
        return join(baseDirectory, relativePath);
    }

    private String remotePathParent(RemoteFile remoteFile) throws IOException {
        String parent = remoteFile.getParentDirectoryRelativePath();
        validateRelativePath(parent);
        return join(baseDirectory, parent);
    }

    private static String resolveConfiguredDirectory(String home, String configuredPath) throws IOException {
        String configured = configuredPath == null ? "" : configuredPath.replace('\\', '/');
        validateRelativePath(configured);
        if (configured.isEmpty() || "/".equals(configured)) {
            return configured.isEmpty() ? home : "/";
        }
        /* SFTP paths are absolute from the server's SFTP root. This also works
         * with chrooted accounts, where '/' is the account's visible root. */
        return configured.startsWith("/") ? configured : join(home, configured);
    }

    /**
     * SFTP configuration values are stored as host[/path] without a scheme.
     * Accept the scheme and an extra leading slash as well so configurations
     * written by earlier builds remain usable.
     */
    static String normalizeUrl(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.regionMatches(true, 0, "sftp://", 0, "sftp://".length())) {
            normalized = normalized.substring("sftp://".length());
        }
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    static String getHostname(String normalizedUrl) {
        int separator = normalizedUrl.indexOf('/');
        return separator < 0 ? normalizedUrl : normalizedUrl.substring(0, separator);
    }

    static String getRemotePath(String normalizedUrl) {
        int separator = normalizedUrl.indexOf('/');
        return separator < 0 ? "" : normalizedUrl.substring(separator);
    }

    private static void validateRelativePath(String path) throws IOException {
        for (String part : path.split("/")) {
            if ("..".equals(part)) {
                throw new IOException("SFTP remote path must not contain '..'.");
            }
        }
    }

    private static String join(String first, String second) {
        String left = first == null ? "" : first.replace('\\', '/');
        String right = second == null ? "" : second.replace('\\', '/');
        while (right.startsWith("/")) {
            right = right.substring(1);
        }
        while (left.endsWith("/") && left.length() > 1) {
            left = left.substring(0, left.length() - 1);
        }
        if (right.isEmpty()) {
            return left.isEmpty() ? "." : left;
        }
        if (left.isEmpty() || ".".equals(left)) {
            return right;
        }
        return "/".equals(left) ? "/" + right : left + "/" + right;
    }

    private static void skipFully(InputStream input, long offset) throws IOException {
        long remaining = offset;
        while (remaining > 0) {
            long skipped = input.skip(remaining);
            if (skipped <= 0) {
                if (input.read() == -1) {
                    throw new IOException("Remote file is shorter than the local resume offset.");
                }
                skipped = 1;
            }
            remaining -= skipped;
        }
    }

    private void ensureConnected() throws IOException {
        throwIfCanceled();
        if (sftpClient == null) {
            throw new IOException("SFTP connection is not active.");
        }
    }

    private void throwIfCanceled() throws IOException {
        if (isCanceled()) {
            DebugLogger.info("SFTP operation observed cancellation; closing session.");
            disconnect();
            throw new IOException("SFTP operation canceled.");
        }
    }

    /**
     * Waits in short intervals so the UI cancellation flag can interrupt a
     * connection attempt. Calling ConnectFuture.verify(timeout) directly can
     * otherwise keep the worker blocked until the full socket timeout expires.
     */
    private void waitForConnection(ConnectFuture connection, int timeoutMillis) throws IOException {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
        long nextProgressLog = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (!connection.isDone()) {
            throwIfCanceled();
            long remainingMillis = TimeUnit.NANOSECONDS.toMillis(deadline - System.nanoTime());
            if (remainingMillis <= 0) {
                connection.cancel();
                throw new SocketTimeoutException("SFTP connection timed out after " + timeoutMillis + " ms.");
            }
            if (System.nanoTime() >= nextProgressLog) {
                DebugLogger.info("SFTP SSH connection still pending; remainingTimeoutMs=" + remainingMillis);
                nextProgressLog = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            }
            connection.await(Math.min(250L, remainingMillis), TimeUnit.MILLISECONDS);
        }
        throwIfCanceled();
        connection.verify();
    }

    private static int timeoutMillis(String value) {
        try {
            int timeout = Integer.parseInt(value);
            return timeout <= 0 ? 30_000 : timeout;
        } catch (NumberFormatException e) {
            return 30_000;
        }
    }

    /**
     * Resolves the endpoint before handing it to SSHD. Prefer IPv4 when both
     * address families are available; this avoids waiting on an unreachable
     * IPv6 route on machines where other clients use IPv4 fallback.
     */
    private static InetAddress resolveRemoteAddress(String hostname) throws IOException {
        InetAddress[] addresses = InetAddress.getAllByName(hostname);
        if (addresses.length == 0) {
            throw new UnknownHostException("No address found for SFTP host: " + hostname);
        }
        DebugLogger.info("SFTP DNS resolution: host=" + hostname + ", addresses="
                + Arrays.toString(Arrays.stream(addresses).map(InetAddress::getHostAddress).toArray()));
        for (InetAddress address : addresses) {
            if (address instanceof Inet4Address) {
                return address;
            }
        }
        return addresses[0];
    }

    private static boolean isMissing(IOException e) {
        return e instanceof org.apache.sshd.sftp.common.SftpException
                && ((org.apache.sshd.sftp.common.SftpException) e).getStatus() == SftpConstants.SSH_FX_NO_SUCH_FILE;
    }

    private static void closeQuietly(AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception ignored) {
            }
        }
    }
}

package fr.soe.a3s.dao.connection.ftp;

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
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import fr.soe.a3s.dao.connection.AbstractConnexionDAO;
import fr.soe.a3s.dao.connection.RemoteFile;
import fr.soe.a3s.domain.AbstractProtocole;
import fr.soe.a3s.constant.ProtocolType;
import fr.soe.a3s.domain.repository.Repository;
import fr.soe.a3s.service.SslValidationPolicy;
import fr.soe.a3s.dto.sync.SyncTreeLeafDTO;
import fr.soe.a3s.exception.ConnectionExceptionFactory;
import fr.soe.a3s.exception.IncompleteFileTransferException;
import fr.soe.a3s.utils.DebugLogger;

public class FtpDAO extends AbstractConnexionDAO {

	private FTPClient ftpClient;
	private String uploadSessionBaseDirectory;
	private String uploadSessionCurrentRelativeDirectory;
	private final Set<String> uploadSessionKnownDirectories = new HashSet<String>();
	private final Map<String, Set<String>> uploadSessionDirectoryEntries = new HashMap<String, Set<String>>();
	private long uploadSessionDirectoryListingCount;
	private long uploadSessionCachedFileCheckCount;

	@Override
	public void beginUploadSession(AbstractProtocole protocol) throws IOException {
		if (isUploadSessionActive()) {
			DebugLogger.info("FTP upload session already active.");
			return;
		}

		try {
			DebugLogger.info("FTP upload session starting: " + DebugLogger.describeProtocol(protocol));
			connect(protocol, null, 0, -1);
			if (ftpClient == null || !ftpClient.isConnected()) {
				throw new IOException("FTP server did not establish a connection.");
			}
			uploadSessionBaseDirectory = ftpClient.printWorkingDirectory();
			if (uploadSessionBaseDirectory == null || uploadSessionBaseDirectory.isEmpty()) {
				uploadSessionBaseDirectory = protocol.getRemotePath();
			}
			if (uploadSessionBaseDirectory == null || uploadSessionBaseDirectory.isEmpty()) {
				/* Without a stable base directory, reuse could target the wrong path. */
				disconnect();
				return;
			}
			uploadSessionCurrentRelativeDirectory = "";
			uploadSessionKnownDirectories.clear();
			uploadSessionKnownDirectories.add("");
			uploadSessionDirectoryEntries.clear();
			uploadSessionDirectoryListingCount = 0;
			uploadSessionCachedFileCheckCount = 0;
			setUploadSessionActive(true);
			DebugLogger.info("FTP upload session ready. baseDirectory=" + uploadSessionBaseDirectory);
		} catch (IOException e) {
			DebugLogger.error("FTP upload session could not be started.", e);
			disconnect();
			uploadSessionBaseDirectory = null;
			throw e;
		}
	}

	@Override
	public void endUploadSession() {
		if (isUploadSessionActive()) {
			try {
				DebugLogger.info("FTP upload session closing. directoryListings=" + uploadSessionDirectoryListingCount
						+ ", cachedFileChecks=" + uploadSessionCachedFileCheckCount);
				disconnect();
			} finally {
				setUploadSessionActive(false);
				uploadSessionBaseDirectory = null;
				uploadSessionCurrentRelativeDirectory = null;
				uploadSessionKnownDirectories.clear();
				uploadSessionDirectoryEntries.clear();
			}
		}
	}

	@Override
	protected void connect(AbstractProtocole protocol, RemoteFile remoteFile, long startOffset, long endOffset)
			throws IOException {

		// !remoteFile is null when upload!

		try {
			DebugLogger.info("FTP connection setup started: " + DebugLogger.describeProtocol(protocol));
			if (protocol.getProtocolType() == ProtocolType.FTPS) {
				ftpClient = createFtpsClient(protocol);
			} else {
				ftpClient = new FTPClient();
			}

			String port = protocol.getPort();
			String login = protocol.getLogin();
			String password = protocol.getPassword();
			String hostname = protocol.getHostname();

			// Set connection and read time out
			int connectionTimeOutValue = Integer.parseInt(protocol.getConnectionTimeOut());
			if (connectionTimeOutValue != 0) {
				ftpClient.setConnectTimeout(connectionTimeOutValue);
			}
			int readTimeOutValue = Integer.parseInt(protocol.getReadTimeOut());
			if (readTimeOutValue != 0) {
				ftpClient.setDataTimeout(Duration.ofMillis(readTimeOutValue));
			}

			// Set buffer size
			ftpClient.setBufferSize(1048576);// 1024*1024

			ftpClient.connect(hostname, Integer.parseInt(port));
			DebugLogger.info("FTP control connection established; logging in.");
			boolean isLoged = ftpClient.login(login, password);

			if (isLoged && protocol.getProtocolType() == ProtocolType.FTPS) {
				((org.apache.commons.net.ftp.FTPSClient) ftpClient).execPBSZ(0);
				((org.apache.commons.net.ftp.FTPSClient) ftpClient).execPROT("P");
				if (!FTPReply.isPositiveCompletion(ftpClient.getReplyCode())) {
					throw new IOException("FTPS server rejected private data protection: " + ftpClient.getReplyString());
				}
			}

			ftpClient.setFileType(FTP.BINARY_FILE_TYPE);// binary transfer
			ftpClient.enterLocalPassiveMode();// passive mode

			ftpClient.setRestartOffset(startOffset);// start offset

			int reply = ftpClient.getReplyCode();

			if (!isLoged) {
				throw new IOException(ConnectionExceptionFactory.WRONG_LOGIN_PASSWORD);
			}

			if (!FTPReply.isPositiveCompletion(reply)) {
				throw new IOException("Server returned FTP error: " + Integer.toString(reply));
			}
			DebugLogger.info("FTP authentication succeeded. reply=" + reply);

			String remoteDirectory = null;
			if (!protocol.getRemotePath().isEmpty()) {
				remoteDirectory = protocol.getRemotePath();
				if (remoteFile != null) {
					String parentDirectoryRelativePath = remoteFile.getParentDirectoryRelativePath();
					if (!parentDirectoryRelativePath.isEmpty()) {
						remoteDirectory += "/" + parentDirectoryRelativePath;
					}
				}
			} else {
				if (remoteFile != null) {
					String parentDirectoryRelativePath = remoteFile.getParentDirectoryRelativePath();
					if (!parentDirectoryRelativePath.isEmpty()) {
						remoteDirectory = parentDirectoryRelativePath;
					}
				}
			}

			if (remoteDirectory != null) {
				boolean ok = ftpClient.changeWorkingDirectory(remoteDirectory);
				if (!ok) {
					throw new FileNotFoundException("Remote directory not found: " + remoteDirectory);
				} else if (remoteFile != null) {
					if (!fileExists(remoteFile)) {
						throw new FileNotFoundException("Remote file not found: " + remoteFile.getRelativeFilePath());
					}
				}
			}

		} catch (IOException e) {
			DebugLogger.error("FTP connection setup failed.", e);
			if (!isCanceled()) {
				String coreMessage = "Failed to connect to the FTP server on url: " + protocol.getHostUrl();
				IOException ioe = ConnectionExceptionFactory.Exception(coreMessage, e);
				throw ioe;
			}
		}
	}

	@Override
	protected void disconnect() {

		if (ftpClient != null) {
			try {
				ftpClient.disconnect();
			} catch (Exception e) {
			}
		}
	}

	private FTPClient createFtpsClient(AbstractProtocole protocol) throws IOException {
		try {
			org.apache.commons.net.ftp.FTPSClient client;
			if (protocol.isValidateSSLCertificate()) {
				client = new org.apache.commons.net.ftp.FTPSClient(false);
				client.setEndpointCheckingEnabled(true);
			} else {
				SslValidationPolicy.requireAllowed(protocol.getHostname());
				TrustManager[] trustAll = new TrustManager[] { new X509TrustManager() {
					@Override
					public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
					@Override
					public void checkClientTrusted(X509Certificate[] chain, String authType) { }
					@Override
					public void checkServerTrusted(X509Certificate[] chain, String authType) { }
				} };
				SSLContext context = SSLContext.getInstance("TLS");
				context.init(null, trustAll, new SecureRandom());
				client = new org.apache.commons.net.ftp.FTPSClient(false, context);
				client.setEndpointCheckingEnabled(false);
			}
			return client;
		} catch (Exception e) {
			throw new IOException("Unable to initialize FTPS: " + e.getMessage(), e);
		}
	}

	@Override
	protected void prepareUploadSessionFile(AbstractProtocole protocol, RemoteFile remoteFile) throws IOException {
		if (remoteFile == null || remoteFile.getParentDirectoryRelativePath() == null
				|| remoteFile.getParentDirectoryRelativePath().isEmpty()) {
			changeToUploadSessionBaseDirectory();
		} else {
			makeDir(remoteFile.getParentDirectoryRelativePath());
		}
	}

	@Override
	protected void prepareUploadSessionDelete(AbstractProtocole protocol, RemoteFile remoteFile) throws IOException {
		String parentDirectory = remoteFile.getParentDirectoryRelativePath();
		if (parentDirectory != null && !parentDirectory.isEmpty()) {
			changeToRemoteDirectory(parentDirectory);
		} else {
			changeToUploadSessionBaseDirectory();
		}
	}

	@Override
	protected void prepareUploadSessionFileExists(AbstractProtocole protocol, RemoteFile remoteFile)
			throws IOException {
		prepareUploadSessionDelete(protocol, remoteFile);
	}

	private void changeToUploadSessionBaseDirectory() throws IOException {
		if (ftpClient == null || !ftpClient.isConnected()) {
			throw new IOException("FTP upload session is no longer connected.");
		}
		if ("".equals(uploadSessionCurrentRelativeDirectory)) {
			return;
		}
		if (uploadSessionBaseDirectory != null && !uploadSessionBaseDirectory.isEmpty()
				&& !ftpClient.changeWorkingDirectory(uploadSessionBaseDirectory)) {
			throw new IOException("Unable to restore FTP upload session directory: " + uploadSessionBaseDirectory);
		}
		uploadSessionCurrentRelativeDirectory = "";
	}

	private void changeToRemoteDirectory(String relativeDirectory) throws IOException {
		String normalizedDirectory = normalizeRelativeDirectory(relativeDirectory);
		if (normalizedDirectory.isEmpty()) {
			changeToUploadSessionBaseDirectory();
			return;
		}
		if (normalizedDirectory.equals(uploadSessionCurrentRelativeDirectory)) {
			return;
		}

		String targetDirectory;
		if (uploadSessionBaseDirectory == null || uploadSessionBaseDirectory.isEmpty()
				|| "/".equals(uploadSessionBaseDirectory)) {
			targetDirectory = "/".equals(uploadSessionBaseDirectory)
					? "/" + normalizedDirectory
					: normalizedDirectory;
		} else {
			targetDirectory = uploadSessionBaseDirectory + "/" + normalizedDirectory;
		}
		if (!ftpClient.changeWorkingDirectory(targetDirectory)) {
			throw new FileNotFoundException("Remote directory not found: " + targetDirectory);
		}
		uploadSessionCurrentRelativeDirectory = normalizedDirectory;
		uploadSessionKnownDirectories.add(normalizedDirectory);
	}

	private String normalizeRelativeDirectory(String directory) {
		if (directory == null || directory.isEmpty()) {
			return "";
		}
		String normalized = directory.replace('\\', '/');
		while (normalized.startsWith("/")) {
			normalized = normalized.substring(1);
		}
		while (normalized.endsWith("/")) {
			normalized = normalized.substring(0, normalized.length() - 1);
		}
		return normalized;
	}

	@Override
	protected void downloadFile(File file, RemoteFile remoteFile, boolean doRecordProgress, boolean doControlSpeed)
			throws IOException {

		boolean resume = false;
		if (ftpClient.getRestartOffset() > 0) {
			resume = true;
		} else {
			resume = false;
		}

		FileOutputStream fos = null;
		InputStream inputStream = null;
		DownloadProgressListener downloadProgressListener = null;
		SpeedControlListener speedControlListener = null;

		try {
			fos = new FileOutputStream(file, resume);
			downloadProgressListener = new DownloadProgressListener(doRecordProgress);
			downloadProgressListener.init(fos);

			speedControlListener = new SpeedControlListener(doControlSpeed);

			inputStream = ftpClient.retrieveFileStream(remoteFile.getFilename());

			if (inputStream == null) {
				int code = ftpClient.getReplyCode();
				if (code == 550) {
					throw new FileNotFoundException("Remote file not found");
				} else {
					throw new IOException("Server returned FTP error: " + Integer.toString(code));
				}
			} else {
				int bytesRead = -1;
				ReadableByteChannel inChannel = Channels.newChannel(inputStream);
				ByteBuffer buffer = ByteBuffer.allocate(4096);
				while (((bytesRead = inChannel.read(buffer)) != -1) && !isCanceled()) {
					byte[] array = buffer.array();
					downloadProgressListener.write(array, bytesRead);
					buffer.clear();
					long wait = speedControlListener.getWaitTime();
					if (wait > 0) {
						try {
							Thread.sleep(wait);
						} catch (InterruptedException e) {
						}
					}
				}

				// Must close before ftpClient.completePendingCommand()
				fos.close();
				inputStream.close();
				downloadProgressListener.close();

				// Ensure transfer is complete
				if (!isCanceled()) {
					boolean ok = ftpClient.completePendingCommand();
					if (!ok) {
						int code = ftpClient.getReplyCode();
						throw new IOException("Server returned FTP error: " + Integer.toString(code));
					} else {
						long actualSize = file.length();
						FTPFile ftpFile = ftpClient.mlistFile(remoteFile.getFilename());
						int reply = ftpClient.getReplyCode();
						if (FTPReply.isPositiveCompletion(reply)) {
							long remoteSize = ftpFile.getSize();
							if (actualSize < remoteSize) {
								throw new IncompleteFileTransferException(file.getAbsolutePath(), actualSize,
										remoteSize);
							} else if (actualSize == 0 && remoteSize == 0) {
								// Create an empty file
								file.createNewFile();
							}
						}
					}
				}
			}
		} catch (IOException e) {
			if (!isCanceled()) {
				String coreMessage = "Failed to retrieve file: " + remoteFile.getRelativeFilePath();
				IOException ioe = ConnectionExceptionFactory.Exception(coreMessage, e);
				throw ioe;
			}
		} finally {
			if (fos != null) {
				fos.close();
			}
			if (inputStream != null) {
				inputStream.close();
			}
			if (downloadProgressListener != null) {
				downloadProgressListener.close();
			}
		}
	}

	@Override
	public void downloadPartialFile(File file, Repository repository, SyncTreeLeafDTO leaf) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected boolean fileExists(RemoteFile remoteFile) throws IOException {

		try {
			if (remoteFile.isDirectory()) {
				int reply = ftpClient.getReplyCode();
				return reply != 550;
			} else if (isUploadSessionActive()) {
				String directory = normalizeRelativeDirectory(uploadSessionCurrentRelativeDirectory);
				Set<String> entries = uploadSessionDirectoryEntries.get(directory);
				if (entries == null) {
					entries = loadCurrentDirectoryEntries(directory);
				}
				if (entries != null) {
					uploadSessionCachedFileCheckCount++;
					return entries.contains(remoteFile.getFilename());
				}
			}

			return fileExistsWithMlst(remoteFile);
		} catch (IOException e) {
			if (!isCanceled()) {
				String coreMessage = "Failed to check file: " + remoteFile.getRelativeFilePath();
				IOException ioe = ConnectionExceptionFactory.Exception(coreMessage, e);
				throw ioe;
			}
		}
		return false;
	}

	private Set<String> loadCurrentDirectoryEntries(String directory) throws IOException {
		FTPFile[] entries = ftpClient.listFiles();
		int reply = ftpClient.getReplyCode();
		if (entries == null || !FTPReply.isPositiveCompletion(reply)) {
			DebugLogger.warning("FTP directory listing unavailable; falling back to MLST. directory=" + directory
					+ ", reply=" + reply);
			return null;
		}

		Set<String> filenames = new HashSet<String>();
		for (FTPFile entry : entries) {
			filenames.add(entry.getName());
		}
		uploadSessionDirectoryEntries.put(directory, filenames);
		uploadSessionDirectoryListingCount++;
		DebugLogger.info("FTP directory listing cached: directory=" + directory + ", entries=" + filenames.size());
		return filenames;
	}

	private boolean fileExistsWithMlst(RemoteFile remoteFile) throws IOException {
		ftpClient.mlistFile(remoteFile.getFilename());
		int reply = ftpClient.getReplyCode();
		if (reply == 550) {
			return false;
		}
		if (FTPReply.isPositiveCompletion(reply)) {
			return true;
		}

		DebugLogger.warning("FTP MLST unavailable; using directory listing fallback. directory="
				+ uploadSessionCurrentRelativeDirectory + ", reply=" + reply);
		Set<String> entries = loadCurrentDirectoryEntries(
				normalizeRelativeDirectory(uploadSessionCurrentRelativeDirectory));
		return entries != null && entries.contains(remoteFile.getFilename());
	}

	@Override
	public void uploadFile(File file, RemoteFile remoteFile, boolean doRecordProgress) throws IOException {

		if (remoteFile.isDirectory()) {
			DebugLogger.info("FTP creating directory: " + remoteFile.getRelativeFilePath());
			makeDir(remoteFile.getRelativeFilePath());
		} else {
			long start = System.nanoTime();
			DebugLogger.info("FTP file upload started: local=" + file.getAbsolutePath() + ", size=" + file.length()
					+ ", remote=" + remoteFile.getRelativeFilePath());
			makeDir(remoteFile.getParentDirectoryRelativePath());

			FileInputStream fis = null;
			OutputStream outputStream = null;
			UploadProgressListener uploadProgressListener = null;

			try {
				fis = new FileInputStream(file);
				uploadProgressListener = new UploadProgressListener();
				uploadProgressListener.init(fis, doRecordProgress);

				outputStream = ftpClient.storeFileStream(remoteFile.getFilename());

				if (outputStream == null) {
					int code = ftpClient.getReplyCode();
					if (code == 550) {
						throw new FileNotFoundException("Remote file not found: " + remoteFile.getRelativeFilePath());
					} else {
						throw new IOException("Server returned FTP error: " + Integer.toString(code));
					}
				} else {
					int bytesRead = -1;
					// http://stackoverflow.com/questions/14000341/why-is-ftp-upload-slow-in-java-7
					byte[] buffer = new byte[1048576];// 1024*1024
					while ((bytesRead = uploadProgressListener.read(buffer)) != -1 && !isCanceled()) {
						outputStream.write(buffer, 0, bytesRead);
					}

					// Must close before ftpClient.completePendingCommand()
					fis.close();
					outputStream.close();
					uploadProgressListener.close();

					if (!isCanceled()) {
						boolean ok = ftpClient.completePendingCommand();
						if (!ok) {
							int code = ftpClient.getReplyCode();
							throw new IOException("Server returned FTP error: " + Integer.toString(code));
						}
						DebugLogger.info("FTP file upload finished: remote=" + remoteFile.getRelativeFilePath()
								+ ", durationMs=" + ((System.nanoTime() - start) / 1_000_000));
						rememberCurrentDirectoryEntry(remoteFile.getFilename());
					}
				}
			} catch (IOException e) {
				DebugLogger.error("FTP file upload failed: local=" + file.getAbsolutePath() + ", remote="
						+ remoteFile.getRelativeFilePath() + ", durationMs="
						+ ((System.nanoTime() - start) / 1_000_000), e);
				if (!isCanceled()) {
					String coreMessage = "Failed to upload file: " + file.getAbsolutePath() + "\n"
							+ "To repository directory: " + remoteFile.getParentDirectoryRelativePath();
					IOException ioe = ConnectionExceptionFactory.Exception(coreMessage, e);
					throw ioe;
				}
			} finally {
				if (fis != null) {
					fis.close();
				}
				if (outputStream != null) {
					outputStream.close();
				}
				if (uploadProgressListener != null) {
					uploadProgressListener.close();
				}
			}
		}
	}

	@Override
	protected void uploadObjectFile(Object object, RemoteFile remoteFile) throws IOException {

		ByteArrayOutputStream baos = null;
		ObjectOutputStream oos = null;
		InputStream uis = null;

		try {
			long start = System.nanoTime();
			DebugLogger.info("FTP metadata upload started: remote=" + remoteFile.getRelativeFilePath());
			baos = new ByteArrayOutputStream();
			oos = new ObjectOutputStream(new GZIPOutputStream(baos));
			oos.writeObject(object);
			oos.flush();
			oos.close();
			uis = new ByteArrayInputStream(baos.toByteArray());
			makeDir(remoteFile.getParentDirectoryRelativePath());
			boolean ok = ftpClient.storeFile(remoteFile.getFilename(), uis);
			if (!ok) {
				int code = ftpClient.getReplyCode();
				throw new IOException("Server returned error code: " + code);
			}
			ftpClient.noop();
			rememberCurrentDirectoryEntry(remoteFile.getFilename());
			DebugLogger.info("FTP metadata upload finished: remote=" + remoteFile.getRelativeFilePath()
					+ ", durationMs=" + ((System.nanoTime() - start) / 1_000_000));
		} catch (IOException e) {
			DebugLogger.error("FTP metadata upload failed: remote=" + remoteFile.getRelativeFilePath(), e);
			if (!isCanceled()) {
				String coreMessage = "Failed to upload file: " + remoteFile.getRelativeFilePath() + "\n"
						+ "To repository directory: " + remoteFile.getParentDirectoryRelativePath();
				IOException ioe = ConnectionExceptionFactory.Exception(coreMessage, e);
				throw ioe;
			}
		} finally {
			if (baos != null) {
				baos.close();
			}
			if (oos != null) {
				oos.close();
			}
			if (uis != null) {
				uis.close();
			}
		}
	}

	private void makeDir(String dirTree) throws IOException {
		String normalizedDirectory = normalizeRelativeDirectory(dirTree);
		if (normalizedDirectory.isEmpty()) {
			changeToUploadSessionBaseDirectory();
			return;
		}
		if (normalizedDirectory.equals(uploadSessionCurrentRelativeDirectory)) {
			return;
		}

		if (uploadSessionKnownDirectories.contains(normalizedDirectory)) {
			try {
				changeToRemoteDirectory(normalizedDirectory);
				return;
			} catch (FileNotFoundException e) {
				/* The directory may have been removed externally; recreate it below. */
				uploadSessionKnownDirectories.remove(normalizedDirectory);
			}
		}

		changeToUploadSessionBaseDirectory();
		String currentDirectory = "";
		for (String directory : normalizedDirectory.split("/")) {
			if (directory.isEmpty()) {
				continue;
			}
			String nextDirectory = currentDirectory.isEmpty() ? directory : currentDirectory + "/" + directory;
			boolean entered = ftpClient.changeWorkingDirectory(directory);
			if (!entered) {
				if (!ftpClient.makeDirectory(directory) || !ftpClient.changeWorkingDirectory(directory)) {
					throw new IOException("Unable to create remote directory " + normalizedDirectory + "\n"
							+ "Server returned FTP error: " + ftpClient.getReplyString());
				}
				uploadSessionDirectoryEntries.remove(nextDirectory);
			}
			markKnownDirectory(nextDirectory);
			currentDirectory = nextDirectory;
		}
		uploadSessionCurrentRelativeDirectory = normalizedDirectory;
	}

	private void markKnownDirectory(String directory) {
		uploadSessionKnownDirectories.add(directory);
		int separator = directory.lastIndexOf('/');
		String parent = separator < 0 ? "" : directory.substring(0, separator);
		String name = separator < 0 ? directory : directory.substring(separator + 1);
		Set<String> entries = uploadSessionDirectoryEntries.get(parent);
		if (entries != null) {
			entries.add(name);
		}
	}

	private void rememberCurrentDirectoryEntry(String filename) {
		if (filename == null || uploadSessionCurrentRelativeDirectory == null) {
			return;
		}
		Set<String> entries = uploadSessionDirectoryEntries.get(uploadSessionCurrentRelativeDirectory);
		if (entries != null) {
			entries.add(filename);
		}
	}

	@Override
	protected void deleteFile(RemoteFile remoteFile) throws IOException {

		String workingDirectory = ftpClient.printWorkingDirectory();
		deleteFile(remoteFile, workingDirectory);
	}

	private void deleteFile(RemoteFile remoteFile, String workingDirectory) throws IOException {

		ftpClient.changeWorkingDirectory(workingDirectory);

		if (remoteFile.isDirectory()) {
			List<RemoteFile> remoteFiles = new ArrayList<RemoteFile>();
			FTPFile[] subFiles = ftpClient.listFiles(remoteFile.getFilename());
			if (subFiles != null) {
				for (FTPFile aFile : subFiles) {
					RemoteFile newRemoteFile = new RemoteFile(aFile.getName(), remoteFile.getRelativeFilePath(),
							!aFile.isFile());
					remoteFiles.add(newRemoteFile);
				}
			}
			for (RemoteFile rmf : remoteFiles) {
				String newWorkingDirecory = workingDirectory + "/" + remoteFile.getFilename();
				deleteFile(rmf, newWorkingDirecory);
			}
			ftpClient.changeWorkingDirectory(workingDirectory);
			if (!ftpClient.removeDirectory(remoteFile.getFilename())) {
				int reply = ftpClient.getReplyCode();
				if (reply != 550 && !isCanceled()) {
					throw new IOException("Failed to remove directory: " + remoteFile.getRelativeFilePath()
							+ "\nServer returned FTP error: " + ftpClient.getReplyString());
				}
			}
		} else {
			if (!ftpClient.deleteFile(remoteFile.getFilename())) {
				int reply = ftpClient.getReplyCode();
				if (reply != 550 && !isCanceled()) {
					throw new IOException("Failed to remove file: " + remoteFile.getRelativeFilePath()
							+ "\nServer returned FTP error: " + ftpClient.getReplyString());
				}
			}
		}
	}

	@Override
	public String checkPartialFileTransfer(Repository repository) {
		return null;
	}

	@Override
	public double getFileCompletion(Repository repository, SyncTreeLeafDTO leaf) {
		return 0;
	}
}

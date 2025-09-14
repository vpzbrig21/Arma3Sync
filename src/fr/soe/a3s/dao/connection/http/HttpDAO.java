package fr.soe.a3s.dao.connection.http;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.bind.DatatypeConverter;

import fr.soe.a3s.constant.ProtocolType;
import fr.soe.a3s.dao.DataAccessConstants;
import fr.soe.a3s.dao.connection.AbstractConnexionDAO;
import fr.soe.a3s.dao.connection.RemoteFile;
import fr.soe.a3s.domain.AbstractProtocole;
import fr.soe.a3s.domain.Http;
import fr.soe.a3s.domain.repository.Repository;
import fr.soe.a3s.dto.sync.SyncTreeLeafDTO;
import fr.soe.a3s.exception.ConnectionExceptionFactory;
import fr.soe.a3s.exception.IncompleteFileTransferException;
import fr.soe.a3s.jazsync.FileMaker;
import fr.soe.a3s.jazsync.MetaFileReader;

public class HttpDAO extends AbstractConnexionDAO {

	private URLConnection urlConnection;
	private DataRange dataRange;

	public void connect(AbstractProtocole protocol, RemoteFile remoteFile, long startOffset, long endOffset)
			throws IOException {

		final String relativeUrl = protocol.getRemotePath() + remoteFile.getRelativeFilePath();

		final String hostname = protocol.getHostname();
		final String port = protocol.getPort();
		final String connectionTimeOut = protocol.getConnectionTimeOut();
		final String readTimeOut = protocol.getReadTimeOut();
		final String login = protocol.getLogin();
		final String password = protocol.getPassword();
		final boolean doValidateSSLCertificate = protocol.isValidateSSLCertificate();

		/*
		 * URL redirect: URLConnection can manage automatically redirections for both
		 * GET and HEAD requests. However trying to connect using http protocol to an
		 * https server will still throw a 301 error message.
		 */

		try {
			// Set url
			// http://stackoverflow.com/questions/724043/http-url-address-encoding -in-java
			URL url = null;
			if (protocol.getProtocolType().equals(ProtocolType.HTTPS)) {
				URI uri = new URI("https", hostname, relativeUrl, null);
				String file = uri.toURL().getFile();
				url = new URL("https", hostname, Integer.parseInt(port), file);
			} else if (protocol.getProtocolType().equals(ProtocolType.HTTP)) {
				URI uri = new URI("http", hostname, relativeUrl, null);
				String file = uri.toURL().getFile();
				url = new URL("http", hostname, Integer.parseInt(port), file);
			} else {
				throw new RuntimeException("Unknown protocol!");
			}

			// SSL certificate validation disabled
			if (protocol.getProtocolType().equals(ProtocolType.HTTPS) && !doValidateSSLCertificate) {

				// Create a trust manager that does not validate certificate
				// chains
				// http://stackoverflow.com/questions/13022717/java-and-https-url-connection-without-downloading-certificate
				TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
					@Override
					public java.security.cert.X509Certificate[] getAcceptedIssuers() {
						return null;
					}

					@Override
					public void checkClientTrusted(X509Certificate[] chain, String authType)
							throws CertificateException {
					}

					@Override
					public void checkServerTrusted(X509Certificate[] chain, String authType)
							throws CertificateException {
					}
				} };

				// Install the all-trusting trust manager
				final SSLContext sc = SSLContext.getInstance("SSL");
				sc.init(null, trustAllCerts, new java.security.SecureRandom());
				((HttpsURLConnection) urlConnection).setDefaultSSLSocketFactory(sc.getSocketFactory());

				// Create all-trusting host name verifier
				HostnameVerifier allHostsValid = new HostnameVerifier() {
					@Override
					public boolean verify(String hostname, SSLSession session) {
						return true;
					}
				};

				// Install the all-trusting host name verifier
				((HttpsURLConnection) urlConnection).setDefaultHostnameVerifier(allHostsValid);
			}

			// Open connection
			urlConnection = url.openConnection();

			// Set connection and read timeouts
			int connectionTimeOutValue = Integer.parseInt(connectionTimeOut);
			if (connectionTimeOutValue != 0) {
				urlConnection.setConnectTimeout(connectionTimeOutValue);
			}
			int readTimeOutValue = Integer.parseInt(readTimeOut);
			if (readTimeOutValue != 0) {
				urlConnection.setReadTimeout(readTimeOutValue);
			}

			// Set User Agent
			urlConnection.setRequestProperty("User-Agent", "ArmA3Sync");

			// Login
			// http://stackoverflow.com/questions/37170850/java-illegal-characters-in-message-header-value-basic
			if (!(login.equalsIgnoreCase("anonymous"))) {
				String userCredentials = login + ":" + password;
				String basicAuth = "Basic "
						+ DatatypeConverter.printBase64Binary(userCredentials.getBytes(StandardCharsets.UTF_8));
				urlConnection.setRequestProperty("Authorization", basicAuth);
			}

			// Set offset
			// http://stackoverflow.com/questions/3414438/java-resume-download
			// -in-urlconnection
			this.dataRange = new DataRange(startOffset, endOffset);
			if (dataRange.isPartial()) {
				urlConnection.setRequestProperty("Range", "bytes=" + this.dataRange.getRange());
			}

			// Check connection state
			int reply = ((HttpURLConnection) urlConnection).getResponseCode();
			if (reply == HttpURLConnection.HTTP_UNAUTHORIZED) {
				throw new IOException(ConnectionExceptionFactory.WRONG_LOGIN_PASSWORD);
			} else if (reply == HttpURLConnection.HTTP_NOT_FOUND) {
				throw new FileNotFoundException("Remote file not found: " + remoteFile.getRelativeFilePath());
			} else if (reply == HttpURLConnection.HTTP_MOVED_PERM || reply == HttpURLConnection.HTTP_MOVED_TEMP) {
				followRedirect((HttpURLConnection) urlConnection, protocol, remoteFile, startOffset, endOffset);
			} else if (!isPositiveCompletion(reply)) {
				String message = "Server response code is " + Integer.toString(reply) + " "
						+ ((HttpURLConnection) urlConnection).getResponseMessage();
				throw new IOException(message);
			}
		} catch (NumberFormatException | NoSuchAlgorithmException | KeyManagementException e) {
			throw new RuntimeException(e.getMessage());
		} catch (MalformedURLException e) {
			String message = "Failed to connect to the HTTP server on url: " + protocol.getHostUrl()
					+ remoteFile.getRelativeFilePath();
			message += "\n" + e.getMessage();
			throw new IOException(message);
		} catch (URISyntaxException e) {
			String message = "Failed to connect to the HTTP server on url: " + e.getInput();
			message += "\n" + e.getReason();
			throw new IOException(message);
		} catch(SSLException e) {
			String message = "Failed to connect to the HTTP server on url: " + protocol.getHostUrl() + remoteFile.getRelativeFilePath();
			if (e.getMessage().contains("protocol_version")) {
				// TLS version of the server is not supported by the JVM
				// https://stackoverflow.com/questions/16541627/javax-net-ssl-sslexception-received-fatal-alert-protocol-version/47717547#47717547
				message += "\n" + "SSL/TLS protocol version between client and server mismatch.";
			}
			else{
				message += "\n" + e.getMessage();
			}
			throw new IOException(message);
		} catch (IOException e) {
			if (!isCanceled()) {
				String coreMessage = "Failed to connect to the HTTP server on url: " + protocol.getHostUrl()
						+ remoteFile.getRelativeFilePath();
				IOException ioe = ConnectionExceptionFactory.Exception(coreMessage, e);
				throw ioe;
			}
		}
	}

	private boolean isPositiveCompletion(int reply) {

		boolean ok = reply == HttpURLConnection.HTTP_OK || reply == HttpURLConnection.HTTP_ACCEPTED
				|| reply == HttpURLConnection.HTTP_CREATED || reply == HttpURLConnection.HTTP_NO_CONTENT
				|| reply == HttpURLConnection.HTTP_NOT_AUTHORITATIVE || reply == HttpURLConnection.HTTP_PARTIAL
				|| reply == HttpURLConnection.HTTP_RESET;
		return ok;
	}
	
	private void followRedirect(HttpURLConnection urlConnection, AbstractProtocole protocol,
			RemoteFile remoteFile, long startOffset, long endOffset) throws IOException {

		String[] target  = urlConnection.getHeaderField("Location").split("/", 4);
		String newPrompt = target[0] + "//";

		if (newPrompt.equals("https://") && protocol.getProtocolType().equals(ProtocolType.HTTP)) {
			String[] newUrlPortSplit = target[2].split(":");
			String newUrl  = newUrlPortSplit[0];
			String newPort = newUrlPortSplit[1];

			String newFileComplete = target[3];

			int slashIndex = newFileComplete.lastIndexOf("/");
			if (slashIndex == -1) { // No slash found
				remoteFile.setParentDirectoryRelativePath("");
				remoteFile.setFilename(newFileComplete);
			} else {
				remoteFile.setParentDirectoryRelativePath( newFileComplete.substring(0, slashIndex) );
				remoteFile.setFilename( newFileComplete.substring(slashIndex + 1) );
			}

			AbstractProtocole protocolRedirect = new Http(newUrl, newPort, protocol.getLogin(),
					protocol.getPassword(), ProtocolType.HTTPS, true);

			connect(protocolRedirect, remoteFile, startOffset, endOffset);
		} else {
			// Going from http to https is fine, but the opposite direction can be dangerous
			throw new IOException("Won't follow redirects from " + protocol.getProtocolType().getPrompt()
					+ " to " + newPrompt + " because it's too dangerous");
		}
	}

	@Override
	public void disconnect() {

		if (urlConnection != null) {
			try {
				((HttpURLConnection) urlConnection).disconnect();
			} catch (Exception e) {
			}
		}
	}

	@Override
	protected void downloadFile(File file, RemoteFile remoteFile, boolean doRecordProgress, boolean doControlSpeed)
			throws IOException {

		boolean resume = false;
		if (this.dataRange.getStart() > 0) {
			resume = true;
		} else {
			resume = false;
		}

		FileOutputStream fos = null;
		InputStream inputStream = null;
		DownloadProgressListener downloadProgressListener = null;
		SpeedControlListener speedControlListener = null;

		try {
			/*
			 * opens input stream from the HTTP httpURLConnection, throws
			 * FileNotFoundException if remote file does not exists
			 */
			inputStream = urlConnection.getInputStream();

			/*
			 * opens an output stream to save into targetFile, throws FileNotFoundException
			 * target file can't be accessed/created
			 */
			fos = new FileOutputStream(file, resume);
			downloadProgressListener = new DownloadProgressListener(doRecordProgress);
			downloadProgressListener.init(fos);
			speedControlListener = new SpeedControlListener(doControlSpeed);

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

			// Ensure transfer is complete
			if (!isCanceled()) {
				long actualSize = file.length();
				long remoteSize = urlConnection.getContentLengthLong() + this.dataRange.getStart();
				if (actualSize < remoteSize && remoteSize != -1) {
					throw new IncompleteFileTransferException(file.getAbsolutePath(), actualSize, remoteSize);
				} else if (actualSize == 0 && remoteSize == 0) {
					// Create an empty file
					file.createNewFile();
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
	protected boolean fileExists(RemoteFile remoteFile) throws IOException {

		boolean exists = false;
		try {
			int reply = ((HttpURLConnection) urlConnection).getResponseCode();
			if (reply == HttpURLConnection.HTTP_OK) {
				exists = true;
			}
		} catch (IOException e) {
			if (!isCanceled()) {
				String coreMessage = "Failed to check file: " + remoteFile.getRelativeFilePath();
				IOException ioe = ConnectionExceptionFactory.Exception(coreMessage, e);
				throw ioe;
			}
		}
		return exists;
	}

	@Override
	public String downloadXMLupdateFile(boolean devMode, AbstractProtocole protocol) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected void uploadFile(File file, RemoteFile remoteFile, boolean doRecordProgress) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	protected void uploadObjectFile(Object object, RemoteFile remoteFile) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	protected void deleteFile(RemoteFile remoteFile) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public String checkPartialFileTransfer(Repository repository) throws IOException {

		String remoteDirectoryPath = DataAccessConstants.A3S_FOlDER_NAME;
		RemoteFile remoteFile = new RemoteFile(DataAccessConstants.SYNC_FILE_NAME, remoteDirectoryPath, false);

		// Note: IIS7 does not support range request with HEAD
		connect(repository.getProtocol(), remoteFile, 0, 1);

		int reply = ((HttpURLConnection) urlConnection).getResponseCode();
		String header = null;
		if (reply != HttpURLConnection.HTTP_PARTIAL) {
			header = getResponseHeader();
		}

		return header;
	}

	@Override
	public double getFileCompletion(Repository repository, SyncTreeLeafDTO leaf) throws IOException {

		File targetFile = new File(leaf.getDestinationPath() + "/" + leaf.getName());

		double complete = 0;

		if (targetFile.exists() && targetFile.length() > 0) {

			System.out.println("Determining file completion: " + targetFile.getAbsolutePath());

			RemoteFile remoteZsyncFile = new RemoteFile(leaf.getName() + DataAccessConstants.ZSYNC_EXTENSION,
					leaf.getParentRelativePath(), false);

			byte[] bytes = downloadZyncFileMetaData(repository.getProtocol(), remoteZsyncFile);

			MetaFileReader mfr = null;
			FileMaker fm = null;
			try {
				if (bytes != null) {
					mfr = new MetaFileReader(bytes);
					fm = new FileMaker(mfr, this);
					complete = fm.mapMatcher(targetFile);
				}
			} catch (IOException e) {
				if (!isCanceled()) {
					String message = "Failed to read zsync file: " + remoteZsyncFile.getRelativeFilePath() + "\n"
							+ e.getMessage();
					throw new IOException(message);
				}
			} finally {
				bytes = null;
				mfr = null;
				fm = null;
				System.gc();
			}

			System.out.println(
					"File completion: " + ((int) (100 * complete)) / 100.0 + "% " + targetFile.getAbsolutePath());// 2
																													// d�cimals
		}

		return complete;
	}

	@Override
	public void downloadPartialFile(File file, Repository repository, SyncTreeLeafDTO leaf) throws IOException {

		File targetFile = new File(leaf.getDestinationPath() + "/" + leaf.getName());

		RemoteFile remoteZsyncFile = new RemoteFile(leaf.getName() + DataAccessConstants.ZSYNC_EXTENSION,
				leaf.getParentRelativePath(), false);

		byte[] bytes = downloadZyncFileMetaData(repository.getProtocol(), remoteZsyncFile);

		RemoteFile remoteFile = new RemoteFile(leaf.getName(), leaf.getParentRelativePath(), false);

		MetaFileReader mfr = null;
		FileMaker fm = null;
		try {
			if (bytes != null) {
				mfr = new MetaFileReader(bytes);
				fm = new FileMaker(mfr, this);
				fm.mapMatcher(targetFile);
				fm.fileMaker(targetFile, remoteFile, repository.getProtocol());
			}
		} catch (IOException e) {
			if (!isCanceled()) {
				String coreMessage = "Failed to retrieve file: " + remoteZsyncFile.getRelativeFilePath();
				IOException ioe = ConnectionExceptionFactory.Exception(coreMessage, e);
				throw ioe;
			}
		} finally {
			bytes = null;
			mfr = null;
			fm = null;
			System.gc();
		}
	}

	private byte[] downloadZyncFileMetaData(AbstractProtocole protoocol, RemoteFile remoteZsyncFile)
			throws IOException {

		connect(protoocol, remoteZsyncFile, 0, -1);

		byte[] bytes = null;
		try {
			bytes = getResponseBody(remoteZsyncFile, 0, false, false);
		} finally {
			disconnect();
		}
		return bytes;
	}

	private String getResponseHeader() throws IOException {

		String header = "";
		Map<String, List<String>> responseHeader = urlConnection.getHeaderFields();
		for (Iterator<String> iterator = responseHeader.keySet().iterator(); iterator.hasNext();) {
			String key = (String) iterator.next();
			if (key != null) {
				header += key + " = ";
			}
			List<String> values = responseHeader.get(key);
			for (int i = 0; i < values.size(); i++) {
				header += values.get(i);
			}
			header += "\n";
		}
		return header;
	}

	public byte[] getResponseBody(RemoteFile remoteFile, long cumulatedDataTransfered, boolean doRecordProgress,
			boolean doControlSpeed) throws IOException {

		InputStream inputStream = null;
		byte[] bytes = null;
		ByteArrayOutputStream byteArrayBuffer = null;
		DownloadProgressListener downloadProgressListener = null;
		SpeedControlListener speedControlListener = null;

		try {
			// opens input stream from the HTTP connection
			inputStream = urlConnection.getInputStream();

			downloadProgressListener = new DownloadProgressListener(doRecordProgress);
			byteArrayBuffer = new ByteArrayOutputStream();
			downloadProgressListener.init(byteArrayBuffer, cumulatedDataTransfered);
			speedControlListener = new SpeedControlListener(doControlSpeed);

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
			bytes = byteArrayBuffer.toByteArray();
		} catch (IOException e) {
			if (!isCanceled()) {
				String coreMessage = "Failed to retrieve file: " + remoteFile.getRelativeFilePath();
				IOException ioe = ConnectionExceptionFactory.Exception(coreMessage, e);
				throw ioe;
			}
		} finally {
			if (inputStream != null) {
				inputStream.close();
			}
			if (byteArrayBuffer != null) {
				byteArrayBuffer.close();
			}
			if (downloadProgressListener != null) {
				downloadProgressListener.close();
			}
			byteArrayBuffer = null;
		}
		return bytes;
	}
}

package fr.soe.a3s.dao.connection.webdav;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.zip.GZIPOutputStream;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.HttpHost;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.SchemePortResolver;
import org.apache.http.conn.UnsupportedSchemeException;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;

import com.github.sardine.Sardine;
import com.github.sardine.impl.SardineException;
import com.github.sardine.impl.SardineImpl;

import fr.soe.a3s.constant.ProtocolType;
import fr.soe.a3s.dao.connection.AbstractConnexionDAO;
import fr.soe.a3s.dao.connection.RemoteFile;
import fr.soe.a3s.domain.AbstractProtocole;
import fr.soe.a3s.domain.repository.Repository;
import fr.soe.a3s.dto.sync.SyncTreeLeafDTO;
import fr.soe.a3s.exception.ConnectionExceptionFactory;
import fr.soe.a3s.exception.IncompleteFileTransferException;

public class WebdavDAO extends AbstractConnexionDAO {

	private Sardine sardine;
	private AbstractProtocole protocol;

	@Override
	protected void connect(AbstractProtocole protocol, RemoteFile remoteFile, long startOffset, long endOffset)
			throws IOException {

		// !remoteFile is null for file upload!

		final String hostname = protocol.getHostname();
		final String port = protocol.getPort();
		final String connectionTimeOut = protocol.getConnectionTimeOut();
		final String readTimeOut = protocol.getReadTimeOut();
		final String login = protocol.getLogin();
		final String password = protocol.getPassword();
		final boolean doValidateSSLCertificate = protocol.isValidateSSLCertificate();

		/*
		 * URL redirect: HttpClient can manage automatically redirections requests for
		 * both GET and HEAD but not DELETE.
		 */

		try {
			// Set port number
			// https://stackoverflow.com/questions/34815886/how-to-define-a-custom-port-for-the-webdav-server-using-sardine
			HttpClientBuilder builder = new HttpClientBuilder() {
				@Override
				public CloseableHttpClient build() {
					SchemePortResolver spr = new SchemePortResolver() {
						@Override
						public int resolve(HttpHost httpHost) throws UnsupportedSchemeException {
							return Integer.parseInt(port);
						}
					};
					CloseableHttpClient httpclient = HttpClients.custom().useSystemProperties()
							.setSchemePortResolver(spr).build();
					return httpclient;
				}
			};

			// Set timeouts
			// https://stackoverflow.com/questions/6024376/apache-httpcomponents-httpclient-timeout
			RequestConfig.Builder requestBuilder = RequestConfig.custom();
			requestBuilder = requestBuilder.setConnectTimeout(Integer.parseInt(connectionTimeOut));
			requestBuilder = requestBuilder.setConnectionRequestTimeout(Integer.parseInt(readTimeOut));
			builder.setDefaultRequestConfig(requestBuilder.build());

			// Allow automatic redirect for delete method
			builder.setRedirectStrategy(new LaxRedirectStrategy());

			// Set SSL
			// https://github.com/lookfirst/sardine/wiki/UsageGuide
			final SSLContext sc = SSLContext.getInstance("SSL");

			// SSL certificate validation enabled
			if (doValidateSSLCertificate) {
				sc.init(null, null, new java.security.SecureRandom());
			}
			// SSL certificate validation disabled
			else if (!doValidateSSLCertificate) {

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
				sc.init(null, trustAllCerts, new java.security.SecureRandom());
			}

			final SSLConnectionSocketFactory sslConnectionSocketFactory = new SSLConnectionSocketFactory(sc);

			if (!(login.equalsIgnoreCase("anonymous"))) {
				this.sardine = new SardineImpl(builder, login, password) {
					@Override
					protected ConnectionSocketFactory createDefaultSecureSocketFactory() {
						return sslConnectionSocketFactory;
					}
				};
				this.sardine.enablePreemptiveAuthentication(hostname);
			} else {
				this.sardine = new SardineImpl(builder) {
					@Override
					protected ConnectionSocketFactory createDefaultSecureSocketFactory() {
						return sslConnectionSocketFactory;
					}
				};
			}

			this.protocol = protocol;

		} catch (NumberFormatException | NoSuchAlgorithmException | KeyManagementException e) {
			throw new RuntimeException(e.getMessage());
		}
	}

	@Override
	protected void disconnect() {

		try {
			this.sardine.shutdown();
		} catch (IOException e) {
		}
	}

	@Override
	protected void downloadFile(File file, RemoteFile remoteFile, boolean doRecordProgress, boolean doControlSpeed)
			throws IOException, IncompleteFileTransferException {

		FileOutputStream fos = null;
		DownloadProgressListener downloadProgressListener = null;
		InputStream inputStream = null;

		try {
			try {
				inputStream = sardine.get(getUrl(remoteFile));
				fos = new FileOutputStream(file, false);
				downloadProgressListener = new DownloadProgressListener(doRecordProgress);
				downloadProgressListener.init(fos);
				int bytesRead = -1;
				ReadableByteChannel inChannel = Channels.newChannel(inputStream);
				ByteBuffer buffer = ByteBuffer.allocate(4096);
				while (((bytesRead = inChannel.read(buffer)) != -1) && !isCanceled()) {
					byte[] array = buffer.array();
					downloadProgressListener.write(array, bytesRead);
					buffer.clear();
				}
			} catch (SardineException e) {
				processSardineException(e, remoteFile);
			}
		} catch (MalformedURLException e) {
			String message = "Failed to connect to the HTTP server on url: " + protocol.getHostUrl()
					+ remoteFile.getRelativeFilePath();
			message += "\n" + e.getMessage();
			throw new IOException(message);
		} catch (URISyntaxException e) {
			String message = "Failed to connect to the HTTP server on url: " + e.getInput();
			message += "\n" + e.getReason();
			throw new IOException(message);
		} catch (IOException e) {
			if (!isCanceled()) {
				String coreMessage = "Failed to connect to the HTTP server on url: " + protocol.getHostUrl()
						+ remoteFile.getRelativeFilePath();
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

	private void processSardineException(SardineException e, RemoteFile remoteFile) throws IOException {

		int reply = e.getStatusCode();
		if (reply == HttpStatus.SC_UNAUTHORIZED) {
			throw new IOException(ConnectionExceptionFactory.WRONG_LOGIN_PASSWORD);
		} else if (reply == HttpStatus.SC_NOT_FOUND) {
			throw new FileNotFoundException("Remote file not found: " + remoteFile.getRelativeFilePath());
		} else {
			String message = "Server response code is " + Integer.toString(reply) + " " + e.getResponsePhrase();
			throw new IOException(message);
		}
	}

	@Override
	protected void downloadPartialFile(File file, Repository repository, SyncTreeLeafDTO leaf) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	protected boolean fileExists(RemoteFile remoteFile) throws IOException {

		boolean exists = false;
		try {
			try {
				exists = sardine.exists(getUrl(remoteFile));
			} catch (SardineException e) {
				processSardineException(e, remoteFile);
			}
		} catch (MalformedURLException e) {
			String message = "Failed to connect to the HTTP server on url: " + protocol.getHostUrl()
					+ remoteFile.getRelativeFilePath();
			message += "\n" + e.getMessage();
			throw new IOException(message);
		} catch (URISyntaxException e) {
			String message = "Failed to connect to the HTTP server on url: " + e.getInput();
			message += "\n" + e.getReason();
			throw new IOException(message);
		} catch (IOException e) {
			if (!isCanceled()) {
				String coreMessage = "Failed to connect to the HTTP server on url: " + protocol.getHostUrl()
						+ remoteFile.getRelativeFilePath();
				IOException ioe = ConnectionExceptionFactory.Exception(coreMessage, e);
				throw ioe;
			}
		}
		return exists;
	}

	@Override
	protected void uploadFile(File file, RemoteFile remoteFile, boolean doRecordProgress) throws IOException {

		if (remoteFile.isDirectory()) {
			makeDir(remoteFile.getRelativeFilePath());
		} else {
			makeDir(remoteFile.getParentDirectoryRelativePath());

			FileInputStream fis = null;
			UploadProgressListener uploadProgressListener = null;

			try {
				try {
					fis = new FileInputStream(file);
					uploadProgressListener = new UploadProgressListener();
					uploadProgressListener.init(fis, doRecordProgress);
					sardine.put(getUrl(remoteFile), uploadProgressListener.getUis());
				} catch (SardineException e) {
					processSardineException(e, remoteFile);
				}
			} catch (MalformedURLException e) {
				String message = "Failed to connect to the HTTP server on url: " + protocol.getHostUrl()
						+ remoteFile.getRelativeFilePath();
				message += "\n" + e.getMessage();
				throw new IOException(message);
			} catch (URISyntaxException e) {
				String message = "Failed to connect to the HTTP server on url: " + e.getInput();
				message += "\n" + e.getReason();
				throw new IOException(message);
			} catch (IOException e) {
				if (!isCanceled()) {
					String coreMessage = "Failed to connect to the HTTP server on url: " + protocol.getHostUrl()
							+ remoteFile.getRelativeFilePath();
					IOException ioe = ConnectionExceptionFactory.Exception(coreMessage, e);
					throw ioe;
				}
			} finally {
				if (fis != null) {
					fis.close();
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
			try {
				baos = new ByteArrayOutputStream();
				oos = new ObjectOutputStream(new GZIPOutputStream(baos));
				oos.writeObject(object);
				oos.flush();
				oos.close();
				byte[] data = baos.toByteArray();
				makeDir(remoteFile.getParentDirectoryRelativePath());
				sardine.put(getUrl(remoteFile), data);
			} catch (SardineException e) {
				processSardineException(e, remoteFile);
			}
		} catch (MalformedURLException e) {
			String message = "Failed to connect to the HTTP server on url: " + protocol.getHostUrl()
					+ remoteFile.getRelativeFilePath();
			message += "\n" + e.getMessage();
			throw new IOException(message);
		} catch (URISyntaxException e) {
			String message = "Failed to connect to the HTTP server on url: " + e.getInput();
			message += "\n" + e.getReason();
			throw new IOException(message);
		} catch (IOException e) {
			if (!isCanceled()) {
				String coreMessage = "Failed to connect to the HTTP server on url: " + protocol.getHostUrl()
						+ remoteFile.getRelativeFilePath();
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

	@Override
	protected void deleteFile(RemoteFile remoteFile) throws IOException {

		if (fileExists(remoteFile)) {
			try {
				try {
					if (remoteFile.isDirectory()) {
						sardine.delete(getUrl(remoteFile));
					} else {
						sardine.delete(getUrl(remoteFile));
					}
				} catch (SardineException e) {
					processSardineException(e, remoteFile);
				}
			} catch (MalformedURLException e) {
				String message = "Failed to connect to the HTTP server on url: " + protocol.getHostUrl()
						+ remoteFile.getRelativeFilePath();
				message += "\n" + e.getMessage();
				throw new IOException(message);
			} catch (URISyntaxException e) {
				String message = "Failed to connect to the HTTP server on url: " + e.getInput();
				message += "\n" + e.getReason();
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
	}

	@Override
	public String checkPartialFileTransfer(Repository repository) throws IOException {
		return null;
	}

	@Override
	public double getFileCompletion(Repository repository, SyncTreeLeafDTO leaf) throws IOException {
		return 0;
	}

	private String getUrl(RemoteFile remoteFile) throws URISyntaxException, MalformedURLException {

		String relativeUrl = protocol.getRemotePath() + remoteFile.getRelativeFilePath();

		if (protocol.getProtocolType().equals(ProtocolType.HTTPS_WEBDAV)) {
			URI uri = new URI("https", this.protocol.getHostname(), relativeUrl, null);
			URL url = uri.toURL();
			return url.toString();
		} else if (protocol.getProtocolType().equals(ProtocolType.HTTP_WEBDAV)) {
			URI uri = new URI("http", this.protocol.getHostname(), relativeUrl, null);
			URL url = uri.toURL();
			return url.toString();
		} else {
			throw new RuntimeException("Unknown protocol!");
		}
	}

	private void makeDir(String dirTree) throws IOException {

		String[] directories = dirTree.split("/");
		String relativePath = "";
		for (String dir : directories) {
			if (!dir.isEmpty()) {
				relativePath = relativePath + "/" + dir;
				String url = this.protocol.getProtocolType().getPrompt() + this.protocol.getHostname()
						+ this.protocol.getRemotePath() + relativePath + "/";
				boolean dirExists = sardine.exists(url);
				if (!dirExists) {
					sardine.createDirectory(url);
				}
			}
		}
	}
}

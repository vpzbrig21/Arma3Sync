package fr.soe.a3s.dao.connection;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;

import fr.soe.a3s.constant.ProtocolType;
import fr.soe.a3s.dao.DataAccessConstants;
import fr.soe.a3s.dao.EncryptionProvider;
import fr.soe.a3s.domain.AbstractProtocole;
import fr.soe.a3s.domain.AbstractProtocoleFactory;
import fr.soe.a3s.exception.CheckException;

public class AutoConfigURLAccessMethods implements DataAccessConstants {

	/**
	 * Determine autoconfigUrl from repository protocol
	 * 
	 * @param protocol not null
	 * @return string autoconfig url
	 */
	public static String determineAutoConfigUrl(AbstractProtocole protocol) {

		String repositoryUrl = protocol.getUrl();
		String port = protocol.getPort();
		String login = protocol.getLogin();
		String hostname = protocol.getHostname();
		String remotePath = protocol.getRemotePath();

		if (login.equalsIgnoreCase("anonymous")) {
			login = "";
		}

		if (protocol.getPort().equals(protocol.getProtocolType().getDefaultPort())) {
			port = "";
		}

		String autoConfigURL = repositoryUrl;
		if (!port.isEmpty()) {
			autoConfigURL = hostname + ":" + port + remotePath;
		}

		autoConfigURL = autoConfigURL + "/" + A3S_FOlDER_NAME + "/" + AUTOCONFIG_FILE_NAME;

		return autoConfigURL;
	}

	/**
	 * Parse autoconfig url
	 * 
	 * @param autoConfigURL
	 * @param protocolType
	 * @return
	 * @throws CheckException
	 */
	public static AbstractProtocole parse(String autoConfigURL) throws CheckException {

		if (autoConfigURL == null) {
			throw new CheckException("Invalid url or unsupported protocol.");
		}

		String normalizedUrl = autoConfigURL.trim();
		ProtocolType protocolType = null;
		// The protocol must be the URL prefix. In particular, HTTPS must not be
		// treated as a legacy HTTP URL by a substring match.
		if (normalizedUrl.regionMatches(true, 0, ProtocolType.HTTPS.getPrompt(), 0,
				ProtocolType.HTTPS.getPrompt().length())) {
			protocolType = ProtocolType.HTTPS;
		} else if (normalizedUrl.regionMatches(true, 0, ProtocolType.HTTP.getPrompt(), 0,
				ProtocolType.HTTP.getPrompt().length())) {
			protocolType = ProtocolType.HTTP;
		} else if (normalizedUrl.regionMatches(true, 0, ProtocolType.FTP.getPrompt(), 0,
				ProtocolType.FTP.getPrompt().length())) {
			protocolType = ProtocolType.FTP;
		}
		// else if (autoConfigURL.toLowerCase().trim()
		// .contains(ProtocolType.A3S.getPrompt())) {
		// protocolType = ProtocolType.A3S;
		// }
		else {
			String message = "Invalid url or unsupported protocol." + "\n" + "Url must start with "
					+ ProtocolType.FTP.getPrompt() + " or " + ProtocolType.HTTP.getPrompt() + " or  "
					+ ProtocolType.HTTPS.getPrompt();
			throw new CheckException(message);
		}

		autoConfigURL = normalizedUrl.substring(protocolType.getPrompt().length());

		// if (protocolType.equals(ProtocolType.A3S)) {
		// return parseA3S(autoConfigURL);
		// } else {
		// return parseStandard(autoConfigURL, protocolType);
		// }

		return parseStandard(autoConfigURL, protocolType);
	}

	/**
	 * 
	 * @param autoConfigURL : repositoryUrl/encryptedLoginInfos
	 * @return AbstractProtocole or null
	 * @throws CheckException
	 */
	private static AbstractProtocole parseA3S(String autoConfigURL) throws CheckException {

		String url = "";
		String port = "";
		String hostname = "";
		String relativePath = "";
		String login = "";
		String password = "";
		ProtocolType protocolType = null;

		int index1 = autoConfigURL.indexOf("/");
		if (index1 != -1) {
			hostname = autoConfigURL.substring(0, index1);
			relativePath = autoConfigURL.substring(index1);
		} else {
			hostname = autoConfigURL;
		}

		int index2 = autoConfigURL.indexOf("#");
		if (index1 != -1) {
			try {
				Cipher cipher = EncryptionProvider.getDecryptionCipher();
			} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		} else {
			String message = "Invalid url or unsupported protocol.";
			throw new CheckException(message);
		}

		return AbstractProtocoleFactory.getProtocol(url, port, login, password, protocolType, false);
	}

	/**
	 * 
	 * @param autoConfigURL : repositoryUrl/.a3s/autoconfig
	 * @param protocolType  not null
	 * @return AbstractProtocole or null
	 */
	private static AbstractProtocole parseStandard(String autoConfigURL, ProtocolType protocolType) {

		String url = "";
		String port = "";
		String hostname = "";
		String relativePath = "";

		int index = autoConfigURL.indexOf("/");
		if (index != -1) {
			hostname = autoConfigURL.substring(0, index);
			relativePath = autoConfigURL.substring(index);
		} else {
			hostname = autoConfigURL;
		}

		int index2 = hostname.indexOf(":");
		if (index2 != -1) {
			port = hostname.substring(index2 + 1);
			hostname = hostname.substring(0, index2);
		}

		int index3 = relativePath.toLowerCase().lastIndexOf("/autoconfig");
		if (index3 != -1) {
			relativePath = relativePath.substring(0, index3);
		}

		url = hostname + relativePath;

		try {
			Integer.parseInt(port);
		} catch (NumberFormatException e) {
			port = "";
		}

		if (port.isEmpty()) {
			port = protocolType.getDefaultPort();
		}

		String login = "anonymous";
		String password = "";

		// Auto-config imports must use normal certificate validation. The previous
		// false value enabled the insecure HTTPS path for every remote repository;
		// that path is now restricted to explicit local-development use.
		return AbstractProtocoleFactory.getProtocol(url, port, login, password, protocolType, true);
	}
}

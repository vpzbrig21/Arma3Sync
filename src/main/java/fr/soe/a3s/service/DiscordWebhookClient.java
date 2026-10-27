package fr.soe.a3s.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;

/** Sends Components V2 payloads to an incoming Discord webhook. */
public final class DiscordWebhookClient {

	private static final int CONNECT_TIMEOUT_MS = 10000;
	private static final int READ_TIMEOUT_MS = 20000;

	private DiscordWebhookClient() {
	}

	public static void send(String webhookUrl, String jsonPayload) throws IOException {
		URI uri = validateWebhookUrl(webhookUrl);
		String separator = uri.getRawQuery() == null ? "?" : "&";
		URI requestUri = URI.create(uri.toASCIIString() + separator + "wait=true&with_components=true");
		HttpURLConnection connection = (HttpURLConnection) requestUri.toURL().openConnection();
		connection.setRequestMethod("POST");
		connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
		connection.setReadTimeout(READ_TIMEOUT_MS);
		connection.setInstanceFollowRedirects(false);
		connection.setDoOutput(true);
		connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
		byte[] requestBody = jsonPayload.getBytes(StandardCharsets.UTF_8);
		connection.setFixedLengthStreamingMode(requestBody.length);
		try {
			try (OutputStream output = connection.getOutputStream()) {
				output.write(requestBody);
			}
			int status = connection.getResponseCode();
			if (status < 200 || status >= 300) {
				String response = readResponse(connection.getErrorStream());
				throw new IOException("Discord rejected the webhook request (HTTP " + status + ")"
						+ (response.isBlank() ? "." : ": " + response));
			}
			try (InputStream input = connection.getInputStream()) {
				if (input != null) input.transferTo(OutputStream.nullOutputStream());
			}
		} finally {
			connection.disconnect();
		}
	}

	public static URI validateWebhookUrl(String webhookUrl) throws IOException {
		try {
			URI uri = URI.create(webhookUrl == null ? "" : webhookUrl.trim());
			String host = uri.getHost();
			boolean discordHost = "discord.com".equalsIgnoreCase(host)
					|| "discordapp.com".equalsIgnoreCase(host);
			if (!"https".equalsIgnoreCase(uri.getScheme()) || !discordHost
					|| uri.getRawUserInfo() != null || (uri.getPort() != -1 && uri.getPort() != 443)
					|| uri.getPath() == null || !uri.getPath().matches("/api/webhooks/[0-9]+/[^/]+/?")) {
				throw new IOException("Enter a valid HTTPS Discord webhook URL.");
			}
			return uri;
		} catch (IllegalArgumentException exception) {
			throw new IOException("Enter a valid HTTPS Discord webhook URL.", exception);
		}
	}

	private static String readResponse(InputStream input) throws IOException {
		if (input == null) return "";
		try (InputStream response = input) {
			byte[] data = response.readNBytes(2049);
			String text = new String(data, StandardCharsets.UTF_8).replaceAll("[\\r\\n]+", " ").trim();
			return text.length() > 2000 ? text.substring(0, 2000) : text;
		}
	}
}

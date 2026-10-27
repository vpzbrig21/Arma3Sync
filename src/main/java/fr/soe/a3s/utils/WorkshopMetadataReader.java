package fr.soe.a3s.utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reads only the Workshop fields needed by changelog rendering from a mod's meta.cpp. */
public final class WorkshopMetadataReader {

	private static final Pattern PUBLISHED_ID_PATTERN = Pattern.compile(
			"(?m)^\\s*publishedid\\s*=\\s*(\\d+)\\s*;");
	private static final Pattern NAME_PATTERN = Pattern.compile(
			"(?m)^\\s*name\\s*=\\s*\\\"((?:\\\\.|[^\\\"\\\\])*)\\\"\\s*;");

	private WorkshopMetadataReader() {
	}

	public static Metadata read(Path metadataFile) throws IOException {
		if (metadataFile == null || !Files.isRegularFile(metadataFile)) return null;
		String content = Files.readString(metadataFile, StandardCharsets.UTF_8);
		Matcher idMatcher = PUBLISHED_ID_PATTERN.matcher(content);
		if (!idMatcher.find()) return null;
		String publishedId = idMatcher.group(1);
		try {
			if (Long.parseLong(publishedId) < 1) return null;
		} catch (NumberFormatException exception) {
			return null;
		}
		Matcher nameMatcher = NAME_PATTERN.matcher(content);
		String name = nameMatcher.find() ? unescape(nameMatcher.group(1)).replaceAll("\\s+", " ").trim() : "";
		return new Metadata(publishedId, name);
	}

	private static String unescape(String value) {
		return value.replace("\\\\", "\\").replace("\\\"", "\"").replace("\\n", "\n");
	}

	public static final class Metadata {
		private final String publishedId;
		private final String name;

		private Metadata(String publishedId, String name) {
			this.publishedId = publishedId;
			this.name = name;
		}

		public String getPublishedId() { return publishedId; }
		public String getName() { return name; }
	}
}

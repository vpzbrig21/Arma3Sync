package fr.soe.a3s.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Resolves changelog folder names to Workshop names and links using local meta.cpp files. */
final class DiscordWorkshopEntryResolver {

	private static final Pattern PUBLISHED_ID_PATTERN = Pattern.compile(
			"(?m)^\\s*publishedid\\s*=\\s*(\\d+)\\s*;");
	private static final Pattern NAME_PATTERN = Pattern.compile(
			"(?m)^\\s*name\\s*=\\s*\\\"((?:\\\\.|[^\\\"\\\\])*)\\\"\\s*;");

	private DiscordWorkshopEntryResolver() {
	}

	static Map<String, WorkshopEntry> resolve(Path repositoryRoot, Set<String> changelogNames) {
		if (repositoryRoot == null || changelogNames == null || changelogNames.isEmpty()
				|| !Files.isDirectory(repositoryRoot)) {
			return java.util.Collections.emptyMap();
		}
	Map<String, WorkshopEntry> resolved = new HashMap<String, WorkshopEntry>();
	Set<String> ambiguous = new HashSet<String>();
	try {
		Files.walkFileTree(repositoryRoot, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes) {
				Path fileName = directory.getFileName();
				if (fileName == null || !changelogNames.contains(fileName.toString())) {
					return FileVisitResult.CONTINUE;
				}
				Path metadataFile = directory.resolve("meta.cpp");
				if (!Files.isRegularFile(metadataFile)) return FileVisitResult.CONTINUE;
				try {
					WorkshopEntry entry = parse(metadataFile);
					if (entry == null) return FileVisitResult.CONTINUE;
					String folderName = fileName.toString();
					if (resolved.containsKey(folderName)) {
						resolved.remove(folderName);
						ambiguous.add(folderName);
					} else if (!ambiguous.contains(folderName)) {
						resolved.put(folderName, entry);
					}
				} catch (IOException ignored) {
					// Keep the folder name in the changelog when metadata cannot be read.
				}
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFileFailed(Path file, IOException exception) {
				return FileVisitResult.CONTINUE;
			}
		});
	} catch (IOException ignored) {
		// Metadata resolution is optional; a filesystem issue must not block Discord publishing.
	}
	return resolved;
	}

	private static WorkshopEntry parse(Path metadataFile) throws IOException {
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
		String workshopName = nameMatcher.find() ? unescape(nameMatcher.group(1)).replaceAll("\\s+", " ").trim() : null;
		return new WorkshopEntry(workshopName, "https://steamcommunity.com/sharedfiles/filedetails/?id=" + publishedId);
	}

	private static String unescape(String value) {
		return value.replace("\\\\", "\\").replace("\\\"", "\"").replace("\\n", "\n");
	}

	static final class WorkshopEntry {
		private final String name;
		private final String url;

		WorkshopEntry(String name, String url) {
			this.name = name;
			this.url = url;
		}

		String getName() { return name; }
		String getUrl() { return url; }
	}
}

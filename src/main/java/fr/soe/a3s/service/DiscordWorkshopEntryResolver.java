package fr.soe.a3s.service;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import fr.soe.a3s.utils.WorkshopMetadataReader;

/** Resolves changelog folder names to Workshop names and links using local meta.cpp files. */
final class DiscordWorkshopEntryResolver {

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
		WorkshopMetadataReader.Metadata metadata = WorkshopMetadataReader.read(metadataFile);
		if (metadata == null) return null;
		return new WorkshopEntry(metadata.getName(), workshopUrl(metadata.getPublishedId()));
	}

	static WorkshopEntry fromMetadata(String workshopId, String name) {
		if (workshopId == null || workshopId.isBlank()) return null;
		return new WorkshopEntry(name, workshopUrl(workshopId));
	}

	private static String workshopUrl(String publishedId) {
		return "https://steamcommunity.com/sharedfiles/filedetails/?id=" + publishedId;
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

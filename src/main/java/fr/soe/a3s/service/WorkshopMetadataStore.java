package fr.soe.a3s.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Base64;
import java.util.Optional;
import java.util.Properties;

import fr.soe.a3s.dao.ApplicationPaths;

/**
 * User-local cache for Workshop metadata which is not available in a mod
 * folder. It is deliberately outside the repository/event format so old
 * Arma3Sync versions and existing repositories remain compatible.
 */
public final class WorkshopMetadataStore {

	private static final String FILE_NAME = "known-workshop-mods.properties";
	private static final String PREFIX = "mod.";
	private final Path file;
	private final Properties properties = new Properties();

	public WorkshopMetadataStore() {
		this(Paths.get(ApplicationPaths.configurationFolderPath(), FILE_NAME));
	}

	WorkshopMetadataStore(Path file) {
		this.file = file;
		load();
	}

	public Optional<Entry> find(String addonKey) {
		if (addonKey == null || addonKey.isBlank()) {
			return Optional.empty();
		}
		String prefix = propertyPrefix(addonKey);
		String id = properties.getProperty(prefix + ".publishedid");
		String name = properties.getProperty(prefix + ".name");
		if (id == null || name == null || id.isBlank() || name.isBlank()) {
			return Optional.empty();
		}
		return Optional.of(new Entry(addonKey, id.trim(), name.trim()));
	}

	public void save(String addonKey, String publishedId, String name) throws IOException {
		if (addonKey == null || addonKey.isBlank()) {
			throw new IllegalArgumentException("Addon key must not be empty.");
		}
		if (publishedId == null || !publishedId.matches("[1-9][0-9]*")) {
			throw new IllegalArgumentException("Workshop ID must be a positive number.");
		}
		if (name == null || name.isBlank()) {
			throw new IllegalArgumentException("Workshop name must not be empty.");
		}

		Path parent = file.getParent();
		if (parent == null) parent = Path.of(".").toAbsolutePath().normalize();
		Files.createDirectories(parent);
		Path lockFile = file.resolveSibling(file.getFileName() + ".lock");
		try (FileChannel channel = FileChannel.open(lockFile, StandardOpenOption.CREATE,
				StandardOpenOption.WRITE); FileLock fileLock = channel.lock()) {
			Properties latest = new Properties();
			if (Files.isRegularFile(file)) {
				try (InputStream input = Files.newInputStream(file)) {
					latest.load(input);
				} catch (IOException | IllegalArgumentException malformedCache) {
					latest.clear();
				}
			}
			String prefix = propertyPrefix(addonKey);
			latest.setProperty(prefix + ".key", addonKey);
			latest.setProperty(prefix + ".publishedid", publishedId.trim());
			latest.setProperty(prefix + ".name", name.trim());
			Path temporary = Files.createTempFile(parent, file.getFileName().toString(), ".tmp");
			try {
				try (OutputStream output = Files.newOutputStream(temporary)) {
					latest.store(output, "Arma3Sync known Workshop metadata");
				}
				try {
					Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
				} catch (AtomicMoveNotSupportedException exception) {
					Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
				}
			} finally {
				Files.deleteIfExists(temporary);
			}
			properties.clear();
			properties.putAll(latest);
		}
	}

	public Path getFile() {
		return file;
	}

	private void load() {
		if (!Files.isRegularFile(file)) {
			return;
		}
		try (InputStream input = Files.newInputStream(file)) {
			properties.load(input);
		} catch (IOException | IllegalArgumentException ignored) {
			// A damaged cache must never prevent the application or export from
			// starting. The next successful save replaces it.
			properties.clear();
		}
	}

	private String propertyPrefix(String addonKey) {
		String encoded = Base64.getUrlEncoder().withoutPadding()
				.encodeToString(addonKey.getBytes(StandardCharsets.UTF_8));
		return PREFIX + encoded;
	}

	public static final class Entry {
		private final String addonKey;
		private final String publishedId;
		private final String name;

		private Entry(String addonKey, String publishedId, String name) {
			this.addonKey = addonKey;
			this.publishedId = publishedId;
			this.name = name;
		}

		public String getAddonKey() {
			return addonKey;
		}

		public String getPublishedId() {
			return publishedId;
		}

		public String getName() {
			return name;
		}
	}
}

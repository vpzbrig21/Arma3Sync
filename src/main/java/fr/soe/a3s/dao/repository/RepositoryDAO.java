package fr.soe.a3s.dao.repository;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

import javax.crypto.Cipher;

import fr.soe.a3s.dao.A3SFilesAccessor;
import fr.soe.a3s.dao.DataAccessConstants;
import fr.soe.a3s.dao.EncryptionProvider;
import fr.soe.a3s.dao.FileAccessMethods;
import fr.soe.a3s.domain.repository.AutoConfig;
import fr.soe.a3s.domain.repository.Changelogs;
import fr.soe.a3s.domain.repository.Events;
import fr.soe.a3s.domain.repository.Repository;
import fr.soe.a3s.domain.repository.ServerInfo;
import fr.soe.a3s.domain.repository.SyncTreeDirectory;
import fr.soe.a3s.exception.CreateDirectoryException;
import fr.soe.a3s.exception.LoadingException;
import fr.soe.a3s.exception.WritingException;

public class RepositoryDAO implements DataAccessConstants {

	private static final Map<String, Repository> mapRepositories = new HashMap<String, Repository>();
	private static final Map<String, File> repositoryFiles = new HashMap<String, File>();
	private static final Object REPOSITORY_WRITE_LOCK = new Object();
	private static final Pattern UNSAFE_FILENAME_CHARS = Pattern.compile("[\\\\/:*?\"<>|]");
	private static final Pattern WHITESPACE = Pattern.compile("\\s+");
	private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();
	private static final int HASH_PREFIX_BYTES = 6;

	public Map<String, Repository> getMap() {
		return mapRepositories;
	}

	public void add(Repository repository) {
		mapRepositories.put(repository.getName(), repository);
		repositoryFiles.put(repository.getName(), buildPreferredRepositoryFile(repository.getName()));
	}

	public boolean remove(String repositoryName) {

		Repository repository = mapRepositories.get(repositoryName);
		if (repository == null) {
			return false;
		}
		boolean deleted = deleteRepositoryFiles(repositoryName);
		if (deleted) {
			mapRepositories.remove(repositoryName);
			repositoryFiles.remove(repositoryName);
			return true;
		} else {
			System.out.println("Warning: Unable to locate repository file for \"" + repositoryName
					+ "\" during removal.");
			return false;
		}
	}

	private boolean deleteRepositoryFiles(String repositoryName) {

		boolean deleted = false;
		File storedFile = repositoryFiles.get(repositoryName);
		if (storedFile != null) {
			deleted = FileAccessMethods.deleteFile(storedFile) || deleted;
			deleted = deleteBackupFile(storedFile) || deleted;
		}

		File preferredFile = buildPreferredRepositoryFile(repositoryName);
		if (!preferredFile.equals(storedFile)) {
			deleted = FileAccessMethods.deleteFile(preferredFile) || deleted;
			deleted = deleteBackupFile(preferredFile) || deleted;
		}

		File legacyFile = buildLegacyRepositoryFile(repositoryName);
		if (!legacyFile.equals(preferredFile) && !legacyFile.equals(storedFile)) {
			deleted = FileAccessMethods.deleteFile(legacyFile) || deleted;
			deleted = deleteBackupFile(legacyFile) || deleted;
		}
		return deleted;
	}

	private boolean deleteBackupFile(File repositoryFile) {

		File parent = repositoryFile.getParentFile();
		if (parent == null) {
			return false;
		}
		File backupFile = new File(parent, repositoryFile.getName() + ".backup");
		return FileAccessMethods.deleteFile(backupFile);
	}

	public Map<String, Exception> readAll() throws LoadingException {

		Map<String, Exception> repositoriesFailedToLoad;
		List<String> duplicateWarnings = new ArrayList<String>();
		try {
			Cipher cipher = EncryptionProvider.getDecryptionCipher();
			File directory = new File(REPOSITORY_FOLDER_PATH);
			File[] subfiles = directory.listFiles();
			repositoriesFailedToLoad = new TreeMap<String, Exception>();
			mapRepositories.clear();
			repositoryFiles.clear();
			if (subfiles != null) {
				for (File file : subfiles) {
					if (file.isFile() && file.getName().endsWith(REPOSITORY_EXTENSION)) {
						try {
							Repository repository = (Repository) A3SFilesAccessor.read(cipher, file);
							if (repository != null) {
								String repositoryName = repository.getName();
								if (repositoryName == null || repositoryName.trim().isEmpty()) {
									repositoriesFailedToLoad.put(file.getName(),
											new IllegalStateException("Repository name is missing."));
									continue;
								}
								registerRepository(repository, file, duplicateWarnings);
							}
						} catch (Exception e) {
							repositoriesFailedToLoad.put(file.getName(), e);
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			String message = "Failed to read repository files";
			System.out.println(message);
			if (e.getMessage() != null) {
				throw new LoadingException(message + "\n" + e.getMessage());
			} else {
				throw new LoadingException(message);
			}
		}

		if (!duplicateWarnings.isEmpty()) {
			System.out.println("Detected duplicate repository definitions during load:");
			for (String warning : duplicateWarnings) {
				System.out.println(" - " + warning);
			}
		}

		if (!repositoriesFailedToLoad.isEmpty()) {
			String message = "Failed to read repository files:";
			for (Iterator<String> iter = repositoriesFailedToLoad.keySet().iterator(); iter.hasNext();) {
				String repositoryName = iter.next();
				Exception e = repositoriesFailedToLoad.get(repositoryName);
				if (e.getMessage() != null) {
					message = message + "\n" + " - " + repositoryName + ": " + e.getMessage();
				} else {
					message = message + "\n" + " - " + repositoryName;
				}
			}
			throw new LoadingException(message);
		}

		return repositoriesFailedToLoad;
	}

	private void registerRepository(Repository repository, File sourceFile, List<String> duplicateWarnings) {

		String repositoryName = repository.getName();
		Repository existingRepository = mapRepositories.get(repositoryName);
		if (existingRepository == null) {
			mapRepositories.put(repositoryName, repository);
			repositoryFiles.put(repositoryName, sourceFile);
		} else {
			File existingSource = repositoryFiles.get(repositoryName);
			long existingTimestamp = existingSource != null ? existingSource.lastModified() : Long.MIN_VALUE;
			long newTimestamp = sourceFile.lastModified();
			if (newTimestamp > existingTimestamp) {
				mapRepositories.put(repositoryName, repository);
				repositoryFiles.put(repositoryName, sourceFile);
				duplicateWarnings.add(repositoryName + ": replaced \"" + getFileName(existingSource) + "\" with \""
						+ sourceFile.getName() + "\" because it is newer.");
			} else {
				duplicateWarnings.add(repositoryName + ": ignored \"" + sourceFile.getName() + "\" because \""
						+ getFileName(existingSource) + "\" is newer.");
			}
		}
	}

	private String getFileName(File file) {
		return file != null ? file.getName() : "<unknown>";
	}

	public void write(Repository repository) throws WritingException {

		if (repository == null) throw new IllegalArgumentException("Repository must not be null.");

		/*
		 * A repository can be written by the check worker and by a settings dialog
		 * at nearly the same time. A process-wide lock prevents those writers from
		 * racing, while A3SFilesAccessor.write() atomically replaces the target.
		 * The old file therefore remains readable until the new serialization has
		 * completed successfully.
		 */
		synchronized (REPOSITORY_WRITE_LOCK) {
			File folder = new File(REPOSITORY_FOLDER_PATH);
			File preferredFile = buildPreferredRepositoryFile(repository.getName());
			File existingFile = repositoryFiles.get(repository.getName());
			if (existingFile == null || !existingFile.exists()) {
				File legacyFile = buildLegacyRepositoryFile(repository.getName());
				if (legacyFile.exists()) {
					existingFile = legacyFile;
				}
			}
			boolean removeLegacyFile = existingFile != null && existingFile.exists()
					&& !existingFile.equals(preferredFile);

			try {
				folder.mkdirs();
				if (!folder.exists()) {
					throw new CreateDirectoryException(folder);
				}
				Cipher cipher = EncryptionProvider.getEncryptionCipher();
				A3SFilesAccessor.write(repository, cipher, preferredFile);
				repositoryFiles.put(repository.getName(), preferredFile);

				// Delete a legacy filename only after the preferred file is valid.
				if (removeLegacyFile && FileAccessMethods.deleteFile(existingFile)) {
					System.out.println("Migrated repository file \"" + existingFile.getName() + "\" to \""
							+ preferredFile.getName() + "\" to avoid name collisions.");
				}
			} catch (Exception e) {
				e.printStackTrace();
				String message = "Failed to write file: " + FileAccessMethods.getCanonicalPath(preferredFile);
				throw new WritingException(message);
			}
		}
	}

	public SyncTreeDirectory readSync(Repository repository) throws IOException {

		if (repository == null) throw new IllegalArgumentException("Repository must not be null.");

		Path syncPath = Path.of(repository.getPath()).resolve(A3S_FOlDER_NAME).resolve(SYNC_FILE_NAME);
		File file = syncPath.toFile();
		SyncTreeDirectory sync = (SyncTreeDirectory) A3SFilesAccessor.read(file);
		return sync;
	}

	public ServerInfo readServerInfo(Repository repository) throws IOException {

		if (repository == null) throw new IllegalArgumentException("Repository must not be null.");

		Path serverInfoPath = Path.of(repository.getPath()).resolve(A3S_FOlDER_NAME).resolve(SERVERINFO_FILE_NAME);
		File file = serverInfoPath.toFile();
		ServerInfo serverInfo = (ServerInfo) A3SFilesAccessor.read(file);
		return serverInfo;
	}

	public Changelogs readChangelogs(Repository repository) throws IOException {

		if (repository == null) throw new IllegalArgumentException("Repository must not be null.");

		Path changelogsPath = Path.of(repository.getPath()).resolve(A3S_FOlDER_NAME).resolve(CHANGELOGS_FILE_NAME);
		File file = changelogsPath.toFile();
		Changelogs changelogs = (Changelogs) A3SFilesAccessor.read(file);
		return changelogs;
	}

	public AutoConfig readAutoConfig(Repository repository) throws IOException {

		if (repository == null) throw new IllegalArgumentException("Repository must not be null.");

		Path autocOnfigPath = Path.of(repository.getPath()).resolve(A3S_FOlDER_NAME).resolve(AUTOCONFIG_FILE_NAME);
		File file = autocOnfigPath.toFile();
		AutoConfig autoconfig = (AutoConfig) A3SFilesAccessor.read(file);
		return autoconfig;
	}

	public Events readEvents(Repository repository) throws IOException {

		Path eventsPath = Path.of(repository.getPath()).resolve(A3S_FOlDER_NAME).resolve(EVENTS_FILE_NAME);
		File file = eventsPath.toFile();
		Events events = (Events) A3SFilesAccessor.read(file);
		return events;
	}

	public void writeEvents(Repository repository) throws WritingException {

		if (repository == null) throw new IllegalArgumentException("Repository must not be null.");

		Events events = repository.getEvents();
		if (events != null) {
			Path a3sFolderPath = Path.of(repository.getPath()).resolve(A3S_FOlDER_NAME);
			File a3sFolder = a3sFolderPath.toFile();
			File file = new File(a3sFolder, EVENTS_FILE_NAME);
			try {
				a3sFolder.mkdir();
				if (!a3sFolder.exists()) {
					throw new CreateDirectoryException(a3sFolder);
				}
				A3SFilesAccessor.write(events, file);
			} catch (IOException e) {
				e.printStackTrace();
				String message = "Failed to write file: " + FileAccessMethods.getCanonicalPath(file);
				throw new WritingException(message);
			}
		}
	}

	private File buildPreferredRepositoryFile(String repositoryName) {

		String sanitized = sanitizeRepositoryName(repositoryName);
		String hashSegment = computeNameHash(repositoryName);
		String filename = sanitized + "__" + hashSegment + REPOSITORY_EXTENSION;
		File folder = new File(REPOSITORY_FOLDER_PATH);
		return new File(folder, filename);
	}

	private File buildLegacyRepositoryFile(String repositoryName) {

		String legacyName = (repositoryName != null ? repositoryName.replaceAll(" ", "") : "repository")
				+ REPOSITORY_EXTENSION;
		File folder = new File(REPOSITORY_FOLDER_PATH);
		return new File(folder, legacyName);
	}

	private String sanitizeRepositoryName(String repositoryName) {

		if (repositoryName == null) {
			return "repository";
		}
		String sanitized = repositoryName.trim();
		sanitized = WHITESPACE.matcher(sanitized).replaceAll("_");
		sanitized = UNSAFE_FILENAME_CHARS.matcher(sanitized).replaceAll("_");
		if (sanitized.isEmpty()) {
			return "repository";
		}
		return sanitized;
	}

	private String computeNameHash(String repositoryName) {

		if (repositoryName == null) {
			return "000000";
		}
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-1");
			byte[] hash = digest.digest(repositoryName.getBytes(StandardCharsets.UTF_8));
			int length = Math.min(hash.length, HASH_PREFIX_BYTES);
			StringBuilder builder = new StringBuilder(length * 2);
			for (int i = 0; i < length; i++) {
				int value = hash[i] & 0xFF;
				builder.append(HEX_CHARS[value >>> 4]);
				builder.append(HEX_CHARS[value & 0x0F]);
			}
			return builder.toString();
		} catch (NoSuchAlgorithmException e) {
			return Integer.toHexString(repositoryName.hashCode());
		}
	}
}

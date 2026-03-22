package fr.soe.a3s.dao.repository;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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

		assert (repository != null);

		File folder = new File(REPOSITORY_FOLDER_PATH);
		File preferredFile = buildPreferredRepositoryFile(repository.getName());
		File repositoryFile = preferredFile;
		File existingFile = repositoryFiles.get(repository.getName());
		if (existingFile == null || !existingFile.exists()) {
			File legacyFile = buildLegacyRepositoryFile(repository.getName());
			if (legacyFile.exists()) {
				existingFile = legacyFile;
			}
		}

		if (existingFile != null && existingFile.exists() && !existingFile.equals(preferredFile)) {
			if (preferredFile.exists()) {
				FileAccessMethods.deleteFile(preferredFile);
			}
			if (existingFile.renameTo(preferredFile)) {
				System.out.println(
						"Migrated repository file \"" + existingFile.getName() + "\" to \"" + preferredFile.getName()
								+ "\" to avoid name collisions.");
				repositoryFile = preferredFile;
			} else {
				System.out.println("Warning: Failed to migrate repository file \"" + existingFile.getName()
						+ "\" to \"" + preferredFile.getName() + "\". Continuing with the legacy file.");
				repositoryFile = existingFile;
			}
		} else if (existingFile != null && existingFile.exists()) {
			repositoryFile = existingFile;
		}

		File parent = repositoryFile.getParentFile();
		if (parent == null) {
			parent = folder;
		}
		File backupFile = new File(parent, repositoryFile.getName() + ".backup");

		try {
			folder.mkdirs();
			if (!folder.exists()) {
				throw new CreateDirectoryException(folder);
			}
			if (repositoryFile.exists()) {
				FileAccessMethods.deleteFile(backupFile);
				repositoryFile.renameTo(backupFile);
			}
			Cipher cipher = EncryptionProvider.getEncryptionCipher();
			A3SFilesAccessor.write(repository, cipher, repositoryFile);
			repositoryFiles.put(repository.getName(), repositoryFile);
		} catch (Exception e) {
			e.printStackTrace();
			if (backupFile.exists()) {
				backupFile.renameTo(repositoryFile);
			}
			String message = "Failed to write file: " + FileAccessMethods.getCanonicalPath(repositoryFile);
			throw new WritingException(message);
		} finally {
			if (backupFile.exists()) {
				FileAccessMethods.deleteFile(backupFile);
			}
		}
	}

	public SyncTreeDirectory readSync(Repository repository) throws IOException {

		assert (repository != null);

		String path = repository.getPath();
		String syncPath = path + "/" + A3S_FOlDER_NAME + "/" + SYNC_FILE_NAME;
		File file = new File(syncPath);
		SyncTreeDirectory sync = (SyncTreeDirectory) A3SFilesAccessor.read(file);
		return sync;
	}

	public ServerInfo readServerInfo(Repository repository) throws IOException {

		assert (repository != null);

		String path = repository.getPath();
		String serverInfoPath = path + "/" + A3S_FOlDER_NAME + "/" + SERVERINFO_FILE_NAME;
		File file = new File(serverInfoPath);
		ServerInfo serverInfo = (ServerInfo) A3SFilesAccessor.read(file);
		return serverInfo;
	}

	public Changelogs readChangelogs(Repository repository) throws IOException {

		assert (repository != null);

		String path = repository.getPath();
		String changelogsPath = path + "/" + A3S_FOlDER_NAME + "/" + CHANGELOGS_FILE_NAME;
		File file = new File(changelogsPath);
		Changelogs changelogs = (Changelogs) A3SFilesAccessor.read(file);
		return changelogs;
	}

	public AutoConfig readAutoConfig(Repository repository) throws IOException {

		assert (repository != null);

		String path = repository.getPath();
		String autocOnfigPath = path + "/" + A3S_FOlDER_NAME + "/" + AUTOCONFIG_FILE_NAME;
		File file = new File(autocOnfigPath);
		AutoConfig autoconfig = (AutoConfig) A3SFilesAccessor.read(file);
		return autoconfig;
	}

	public Events readEvents(Repository repository) throws IOException {

		String path = repository.getPath();
		String eventsPath = path + "/" + A3S_FOlDER_NAME + "/" + EVENTS_FILE_NAME;
		File file = new File(eventsPath);
		Events events = (Events) A3SFilesAccessor.read(file);
		return events;
	}

	public void writeEvents(Repository repository) throws WritingException {

		assert (repository != null);

		Events events = repository.getEvents();
		if (events != null) {
			String path = repository.getPath();
			File a3sFolder = new File(path + "/" + A3S_FOlDER_NAME);
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

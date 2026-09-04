package fr.soe.a3s.dao;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.StringTokenizer;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import fr.soe.a3s.domain.configration.Configuration;
import fr.soe.a3s.exception.CreateDirectoryException;
import fr.soe.a3s.exception.LoadingException;
import fr.soe.a3s.exception.WritingException;

public class ConfigurationDAO implements DataAccessConstants {

	private static Configuration configuration = new Configuration();
	private static final String REGQUERY_UTIL = "reg query ";
	private static final String REGSTR_TOKEN = "REG_SZ";
	private static final Set<String> REGISTRY_NEGATIVE_CACHE = ConcurrentHashMap.newKeySet();
	private static final ConcurrentHashMap<String, String> REGISTRY_CACHE = new ConcurrentHashMap<String, String>();

	public void read() throws LoadingException {

		File file = new File(CONFIGURATION_FILE_PATH);
		try {
			if (file.exists()) {
				Configuration config = (Configuration) A3SFilesAccessor.read(file);
				if (config != null) {
					configuration = config;
				}
			}
		} catch (Exception e) {
			throw new LoadingException("Failed to read file: " + FileAccessMethods.getCanonicalPath(file));
		}
	}

	public void write() throws WritingException {

		File file = new File(CONFIGURATION_FILE_PATH);
		File folder = new File(CONFIGURATION_FOLDER_PATH);
		try {
			folder.mkdirs();
			if (!folder.exists()) {
				throw new CreateDirectoryException(folder);
			}
			A3SFilesAccessor.write(configuration, file);
		} catch (IOException e) {
			e.printStackTrace();
			String message = "Failed to write file: " + FileAccessMethods.getCanonicalPath(file);
			throw new WritingException(message);
		}
	}

	public Configuration getConfiguration() {
		return configuration;
	}

	public String determineSteamPath() {

		if (!isWindows()) {
			return null;
		}

		return queryRegistryValue("\"HKLM\\SOFTWARE\\Wow6432Node\\Valve\\Steam\" /v InstallPath");
	}

	public String determineArmA3Path() {

		if (!isWindows()) {
			return null;
		}

		return queryRegistryValue("\"HKLM\\SOFTWARE\\Wow6432Node\\Bohemia Interactive\\Arma 3\" /v MAIN");
	}

	public String determineTS3path() {

		if (!isWindows()) {
			return null;
		}

		return queryRegistryValue("\"HKCU\\Software\\TeamSpeak 3 Client\" /ve");
	}

	public String determineTS3version(String ts3InstallationDirectoryPath) {

		assert (ts3InstallationDirectoryPath != null);
		String ts3Version = null;
		String changelogPath = ts3InstallationDirectoryPath + "\\" + "changelog.txt";
		File file = new File(changelogPath);
		if (!file.exists()) {
			return null;
		} else {
			try (FileInputStream fin = new FileInputStream(file);
					DataInputStream dataInputStream = new DataInputStream(fin)) {
				byte[] buffer = new byte[(int) file.length()];
				dataInputStream.readFully(buffer);
				String s = new String(buffer);
				String[] lines = s.split("\r\n|\r|\n");
				for (String line : lines) {
					if (line.toLowerCase().contains("client release")) {
						StringTokenizer stz = new StringTokenizer(line, " ");
						if (stz.countTokens() >= 4) {
							stz.nextToken();
							stz.nextToken();
							stz.nextToken();
							ts3Version = stz.nextToken();
							break;
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				ts3Version = null;
			}
		}
		return ts3Version;
	}

	public String determineArmA2Path() {

		if (!isWindows()) {
			return null;
		}

		return queryRegistryValue(
				"\"HKLM\\SOFTWARE\\Wow6432Node\\Bohemia Interactive Studio\\ArmA 2\" /v MAIN");
	}

	public String determineArmA2OAPath() {

		if (!isWindows()) {
			return null;
		}

		return queryRegistryValue(
				"\"HKLM\\SOFTWARE\\Wow6432Node\\Bohemia Interactive Studio\\ArmA 2 OA\" /v MAIN");
	}

	public String determineArmAPath() {

		if (!isWindows()) {
			return null;
		}

		return queryRegistryValue("\"HKLM\\SOFTWARE\\Wow6432Node\\Bohemia Interactive Studio\\ArmA\" /v MAIN");
	}

	public String determineTOHPath() {

		if (!isWindows()) {
			return null;
		}

		return queryRegistryValue(
				"\"HKLM\\SOFTWARE\\Wow6432Node\\Bohemia Interactive Studio\\Take On Helicopters\" /v MAIN");
	}

	public String determineRptPath() {

		String arma3RPTfolderPath = null;

		if (isWindows()) {
			String appDataFolderPath = System.getenv("APPDATA");// AppDATA\Roaming
			if (appDataFolderPath != null) {
				arma3RPTfolderPath = new File(appDataFolderPath).getParentFile().getAbsolutePath() + "\\Local\\Arma 3";
			}
		}
		return arma3RPTfolderPath;
	}

	private boolean isWindows() {
		String osName = System.getProperty("os.name");
		return osName != null && osName.contains("Windows");
	}

	private String queryRegistryValue(String queryArguments) {
		if (!isWindows()) {
			return null;
		}
		if (REGISTRY_CACHE.containsKey(queryArguments)) {
			return REGISTRY_CACHE.get(queryArguments);
		}
		if (REGISTRY_NEGATIVE_CACHE.contains(queryArguments)) {
			return null;
		}
		String value = executeRegistryQuery(queryArguments);
		if (value == null) {
			REGISTRY_NEGATIVE_CACHE.add(queryArguments);
		} else {
			REGISTRY_CACHE.put(queryArguments, value);
		}
		return value;
	}

	private String executeRegistryQuery(String queryArguments) {
		try {
			Process process = new ProcessBuilder("cmd.exe", "/C", REGQUERY_UTIL + queryArguments).start();
			StreamReader reader = new StreamReader(process);
			reader.start();
			reader.join();

			String result = reader.getResult();
			int pkeyREG_SZ = result.indexOf(REGSTR_TOKEN);
			if (pkeyREG_SZ == -1) {
				return null;
			}

			return result.substring(pkeyREG_SZ + REGSTR_TOKEN.length()).trim();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}

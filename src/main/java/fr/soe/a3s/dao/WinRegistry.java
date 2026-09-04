package fr.soe.a3s.dao;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Minimal Windows registry helper that delegates to {@code reg.exe}.
 * <p>
 * The previous implementation relied on private JDK internals (WindowsReg*)
 * whose reflective access is blocked on Java 9+. Using the official CLI keeps
 * us compatible with modern JVMs while still supporting the existing features
 * required by Arma3Sync.
 */
public final class WinRegistry {

	public static final int HKEY_CURRENT_USER = 0x80000001;
	public static final int HKEY_LOCAL_MACHINE = 0x80000002;

	private WinRegistry() {
	}

	public static String readString(int hkey, String key, String valueName)
			throws IOException, InterruptedException {
		Map<String, String> values = readStringValues(hkey, key);
		return values != null ? values.get(valueName) : null;
	}

	public static Map<String, String> readStringValues(int hkey, String key)
			throws IOException, InterruptedException {
		RegResult result = runReg("QUERY", fullPath(hkey, key));
		return parseValues(result.output());
	}

	public static List<String> readStringSubKeys(int hkey, String key)
			throws IOException, InterruptedException {
		String root = fullPath(hkey, key);
		RegResult result = runReg("QUERY", root);
		return parseSubKeys(result.output(), root);
	}

	public static void createKey(int hkey, String key)
			throws IOException, InterruptedException {
		runReg("ADD", fullPath(hkey, key), "/f");
	}

	public static void writeStringValue(int hkey, String key, String valueName, String value)
			throws IOException, InterruptedException {
		Objects.requireNonNull(value, "value");
		runReg("ADD", fullPath(hkey, key), "/v", valueName, "/t", "REG_SZ", "/d", value, "/f");
	}

	public static void deleteKey(int hkey, String key)
			throws IOException, InterruptedException {
		runReg("DELETE", fullPath(hkey, key), "/f");
	}

	public static void deleteValue(int hkey, String key, String valueName)
			throws IOException, InterruptedException {
		runReg("DELETE", fullPath(hkey, key), "/v", valueName, "/f");
	}

	private static RegResult runReg(String... args) throws IOException, InterruptedException {
		ensureWindows();
		var command = new ArrayList<String>();
		command.add("reg");
		for (var arg : args) {
			command.add(arg);
		}
		ProcessBuilder builder = new ProcessBuilder(command);
		builder.redirectErrorStream(true);
		Process process = builder.start();
		String output = read(process.getInputStream());
		int exitCode = process.waitFor();
		if (exitCode != 0) {
			throw new IOException("reg.exe exit code " + exitCode + ": " + output);
		}
		return new RegResult(exitCode, output);
	}

	private static String fullPath(int hkey, String key) {
		String normalizedKey = key == null ? "" : key.replace('/', '\\');
		String root = switch (hkey) {
		case HKEY_CURRENT_USER -> "HKCU";
		case HKEY_LOCAL_MACHINE -> "HKLM";
		default -> throw new IllegalArgumentException("Unsupported hive: " + hkey);
		};
		if (normalizedKey.isEmpty()) {
			return root;
		}
		if (normalizedKey.startsWith("\\")) {
			return root + normalizedKey;
		}
		return root + "\\" + normalizedKey;
	}

	private static Map<String, String> parseValues(String output) {
		Map<String, String> values = new LinkedHashMap<>();
		if (output == null || output.isBlank()) {
			return values;
		}
		for (var line : output.split("\\R")) {
			int typeIndex = line.indexOf("REG_");
			if (typeIndex < 0) {
				continue;
			}
			String name = line.substring(0, typeIndex).trim();
			String data = "";
			int dataIndex = line.indexOf(' ', typeIndex);
			if (dataIndex >= 0 && dataIndex < line.length()) {
				data = line.substring(dataIndex).trim();
			}
			if (!name.isEmpty()) {
				values.put(name, data);
			}
		}
		return values;
	}

	private static List<String> parseSubKeys(String output, String root) {
		List<String> subKeys = new ArrayList<>();
		if (output == null || output.isBlank()) {
			return subKeys;
		}
		for (var rawLine : output.split("\\R")) {
			String line = rawLine.trim();
			if (line.startsWith(root + "\\")) {
				String name = line.substring(root.length() + 1).trim();
				if (!name.isEmpty() && !name.contains("\\")) {
					subKeys.add(name);
				}
			}
		}
		return subKeys;
	}

	private static String read(InputStream stream) throws IOException {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
			StringBuilder builder = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				builder.append(line).append(System.lineSeparator());
			}
			return builder.toString();
		}
	}

	private static void ensureWindows() {
		String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
		if (!osName.contains("windows")) {
			throw new UnsupportedOperationException("Windows registry is only available on Windows hosts.");
		}
	}

	private record RegResult(int exitCode, String output) {
	}
}

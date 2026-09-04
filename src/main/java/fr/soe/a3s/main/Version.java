package fr.soe.a3s.main;

import java.io.IOException;
import java.io.InputStream;
import java.time.Year;
import java.util.Properties;

public class Version {

	private static final String VERSION = loadVersion();

	private static final int FIRST_RELEASE_YEAR = 2013;

	public static String getVersion() {
		return VERSION;
	}

	public static String getName() {
		String[] parts = VERSION.split("\\.");
		return parts.length >= 2 ? parts[0] + "." + parts[1] : VERSION;
	}

	public static int getBuild() {
		String[] parts = VERSION.split("\\.");
		if (parts.length < 3) return 0;
		try {
			return Integer.parseInt(parts[2]);
		} catch (NumberFormatException ignored) {
			return 0;
		}
	}

	public static String getYear() {
		int currentYear = Year.now().getValue();
		return FIRST_RELEASE_YEAR == currentYear
				? Integer.toString(FIRST_RELEASE_YEAR)
				: FIRST_RELEASE_YEAR + "-" + currentYear;
	}

	private static String loadVersion() {
		String manifestVersion = Version.class.getPackage().getImplementationVersion();
		if (manifestVersion != null && manifestVersion.matches("\\d+\\.\\d+\\.\\d+")) {
			return manifestVersion;
		}
		Properties properties = new Properties();
		try (InputStream input = Version.class.getResourceAsStream("/version.properties")) {
			if (input != null) {
				properties.load(input);
				String configured = properties.getProperty("app.version");
				if (configured != null && configured.trim().matches("\\d+\\.\\d+\\.\\d+")) {
					return configured.trim();
				}
			}
		} catch (IOException ignored) {
			// Keep startup diagnostics available even when a development classpath is incomplete.
		}
		return "0.0.0";
	}
}

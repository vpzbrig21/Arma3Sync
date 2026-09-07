package fr.soe.a3sUpdater.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Small dependency-free TOML reader for the documented updater settings. */
final class TomlConfigDAO {
    private TomlConfigDAO() { }

    static Map<String, String> read(Path file) throws IOException {
        Map<String, String> values = new LinkedHashMap<>();
        String section = "";
        for (String rawLine : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            String line = stripComment(rawLine).trim();
            if (line.isEmpty()) continue;
            if (line.startsWith("[") && line.endsWith("]")) {
                section = line.substring(1, line.length() - 1).trim();
                if (section.isEmpty()) throw new IOException("Empty TOML section in " + file);
                continue;
            }
            int equals = line.indexOf('=');
            if (equals <= 0) throw new IOException("Invalid TOML line in " + file + ": " + rawLine);
            String key = line.substring(0, equals).trim();
            String value = line.substring(equals + 1).trim();
            if (!key.matches("[A-Za-z0-9_.-]+")) {
                throw new IOException("Invalid TOML key in " + file + ": " + key);
            }
            values.put(section.isEmpty() ? key : section + "." + key, value);
        }
        return values;
    }

    static String string(Map<String, String> values, String key, String fallback) throws IOException {
        String raw = values.get(key);
        if (raw == null) return fallback;
        raw = raw.trim();
        if (raw.length() < 2 || raw.charAt(0) != '"' || raw.charAt(raw.length() - 1) != '"') {
            throw new IOException("TOML value must be a quoted string: " + key);
        }
        return unescape(raw.substring(1, raw.length() - 1));
    }

    static boolean bool(Map<String, String> values, String key, boolean fallback) throws IOException {
        String raw = values.get(key);
        if (raw == null) return fallback;
        if ("true".equalsIgnoreCase(raw)) return true;
        if ("false".equalsIgnoreCase(raw)) return false;
        throw new IOException("TOML value must be true or false: " + key);
    }

    static int integer(Map<String, String> values, String key, int fallback) throws IOException {
        String raw = values.get(key);
        if (raw == null) return fallback;
        try {
            int value = Integer.parseInt(raw);
            if (value < 1 || value > 300_000) throw new NumberFormatException();
            return value;
        } catch (NumberFormatException exception) {
            throw new IOException("Invalid positive TOML integer: " + key);
        }
    }

    private static String stripComment(String line) {
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"' && (i == 0 || line.charAt(i - 1) != '\\')) quoted = !quoted;
            if (c == '#' && !quoted) return line.substring(0, i);
        }
        return line;
    }

    private static String unescape(String value) throws IOException {
        StringBuilder result = new StringBuilder();
        boolean escaped = false;
        for (char c : value.toCharArray()) {
            if (escaped) {
                switch (c) {
                    case '"', '\\' -> result.append(c);
                    case 'n' -> result.append('\n');
                    case 'r' -> result.append('\r');
                    case 't' -> result.append('\t');
                    default -> throw new IOException("Unsupported TOML escape sequence: \\" + c);
                }
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else {
                result.append(c);
            }
        }
        if (escaped) throw new IOException("Unterminated TOML escape sequence.");
        return result.toString();
    }
}

package fr.soe.a3sUpdater.dao;

import fr.soe.a3sUpdater.model.UpdateManifest;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/** Parses the versioned JSON update manifest without adding a runtime dependency. */
public final class JsonManifestDAO {
    private static final Pattern VERSION_PATTERN = Pattern.compile(
            "\\d+(?:\\.\\d+){2,3}(?:[-+][A-Za-z0-9]+(?:[.-][A-Za-z0-9]+)*)?");
    private static final Pattern NUMBER_PATTERN = Pattern.compile(
            "-?(?:0|[1-9][0-9]*)(?:\\.[0-9]+)?(?:[eE][+-]?[0-9]+)?");

    static Map<String, Object> parseObject(String json) throws IOException {
        Object parsed = new Parser(json).parse();
        if (!(parsed instanceof Map<?, ?> values)) throw new IOException("JSON document must be an object.");
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : values.entrySet()) {
            if (!(entry.getKey() instanceof String key)) throw new IOException("JSON object key must be a string.");
            result.put(key, entry.getValue());
        }
        return result;
    }

    public UpdateManifest read(String json, URI source) throws IOException {
        Map<String, Object> values = parseObject(json);

        String version = string(values, "version", string(values, "nom", null));
        String fileName = string(values, "file", string(values, "filename", null));
        String download = string(values, "downloadUrl", string(values, "url", null));
        if (version == null || !VERSION_PATTERN.matcher(version).matches()) {
            throw new IOException("JSON manifest has no valid version.");
        }
        if (fileName == null && download != null) {
            try {
                fileName = fileName(URI.create(download));
            } catch (IllegalArgumentException exception) {
                throw new IOException("JSON manifest has an invalid download URL.", exception);
            }
        }
        if (fileName == null || !isSafeZipName(fileName)) throw new IOException("JSON manifest has no valid ZIP filename.");

        URI downloadUri;
        try {
            downloadUri = download == null || download.isBlank()
                    ? source.resolve(fileName) : source.resolve(URI.create(download));
        } catch (IllegalArgumentException exception) {
            throw new IOException("JSON manifest has an invalid download URL.", exception);
        }
        requireHttpUri(downloadUri, "download URL");
        String sha256 = string(values, "sha256", string(values, "hash", null));
        if (sha256 == null || !sha256.matches("(?i)[0-9a-f]{64}")) {
            throw new IOException("JSON manifest must contain a valid SHA-256 hash.");
        }
        long size = number(values, "size", 0L);
        return new UpdateManifest(version.trim(), fileName, downloadUri, sha256.toLowerCase(), size,
                UpdateManifest.Format.JSON, source.toString());
    }

    private static String string(Map<?, ?> object, String key, String fallback) throws IOException {
        Object value = object.get(key);
        if (value == null) return fallback;
        if (!(value instanceof String text)) throw new IOException("JSON field must be a string: " + key);
        return text.trim();
    }

    private static long number(Map<?, ?> object, String key, long fallback) throws IOException {
        Object value = object.get(key);
        if (value == null) return fallback;
        if (!(value instanceof Number number)) throw new IOException("JSON field must be a number: " + key);
        try {
            long result = new BigDecimal(number.toString()).longValueExact();
            if (result < 0) throw new ArithmeticException("negative");
            return result;
        } catch (ArithmeticException exception) {
            throw new IOException("JSON field must be a non-negative integer: " + key);
        }
    }

    private static String fileName(URI uri) throws IOException {
        String path = uri.getPath();
        if (path == null || path.isBlank()) throw new IOException("Download URL has no filename.");
        return path.substring(path.lastIndexOf('/') + 1);
    }

    private static boolean isSafeZipName(String name) {
        return !name.isBlank() && !name.contains("/") && !name.contains("\\")
                && !name.equals(".") && !name.equals("..") && name.toLowerCase().endsWith(".zip");
    }

    static boolean isSupportedVersion(String version) {
        return version != null && VERSION_PATTERN.matcher(version.trim()).matches();
    }

    static void requireHttpUri(URI uri, String field) throws IOException {
        if (uri == null || uri.getScheme() == null
                || !(uri.getScheme().equalsIgnoreCase("http") || uri.getScheme().equalsIgnoreCase("https"))
                || uri.getHost() == null || uri.getUserInfo() != null) {
            throw new IOException("Invalid HTTP(S) " + field + ".");
        }
    }

    private static final class Parser {
        private final String input;
        private int position;

        Parser(String input) { this.input = input == null ? "" : input; }

        Object parse() throws IOException {
            skipWhitespace();
            Object value = value();
            skipWhitespace();
            if (position != input.length()) throw error("Unexpected trailing JSON data");
            return value;
        }

        private Object value() throws IOException {
            skipWhitespace();
            if (position >= input.length()) throw error("Unexpected end of JSON");
            return switch (input.charAt(position)) {
                case '{' -> object();
                case '[' -> array();
                case '"' -> stringValue();
                case 't' -> literal("true", Boolean.TRUE);
                case 'f' -> literal("false", Boolean.FALSE);
                case 'n' -> literal("null", null);
                default -> numberValue();
            };
        }

        private Map<String, Object> object() throws IOException {
            Map<String, Object> object = new LinkedHashMap<>();
            position++;
            skipWhitespace();
            if (consume('}')) return object;
            while (true) {
                skipWhitespace();
                if (position >= input.length() || input.charAt(position) != '"') throw error("Object key expected");
                String key = stringValue();
                if (object.containsKey(key)) throw error("Duplicate JSON object key");
                skipWhitespace();
                expect(':');
                Object value = value();
                object.put(key, value);
                skipWhitespace();
                if (consume('}')) return object;
                expect(',');
            }
        }

        private java.util.List<Object> array() throws IOException {
            java.util.List<Object> array = new java.util.ArrayList<>();
            position++;
            skipWhitespace();
            if (consume(']')) return array;
            while (true) {
                array.add(value());
                skipWhitespace();
                if (consume(']')) return array;
                expect(',');
            }
        }

        private String stringValue() throws IOException {
            expect('"');
            StringBuilder result = new StringBuilder();
            while (position < input.length()) {
                char c = input.charAt(position++);
                if (c == '"') return result.toString();
                if (c == '\\') {
                    if (position >= input.length()) throw error("Unterminated JSON string");
                    char escaped = input.charAt(position++);
                    switch (escaped) {
                        case '"', '\\', '/' -> result.append(escaped);
                        case 'b' -> result.append('\b');
                        case 'f' -> result.append('\f');
                        case 'n' -> result.append('\n');
                        case 'r' -> result.append('\r');
                        case 't' -> result.append('\t');
                        case 'u' -> {
                            if (position + 4 > input.length()) throw error("Invalid JSON unicode escape");
                            try { result.append((char) Integer.parseInt(input.substring(position, position + 4), 16)); }
                            catch (NumberFormatException exception) { throw error("Invalid JSON unicode escape"); }
                            position += 4;
                        }
                        default -> throw error("Invalid JSON escape");
                    }
                } else {
                    if (c < 0x20) throw error("Control character in JSON string");
                    result.append(c);
                }
            }
            throw error("Unterminated JSON string");
        }

        private Number numberValue() throws IOException {
            int start = position;
            while (position < input.length() && "-+0123456789.eE".indexOf(input.charAt(position)) >= 0) position++;
            String value = input.substring(start, position);
            if (!NUMBER_PATTERN.matcher(value).matches()) throw error("Invalid JSON number");
            try {
                return value.indexOf('.') < 0 && value.indexOf('e') < 0 && value.indexOf('E') < 0
                        ? Long.parseLong(value) : new BigDecimal(value);
            } catch (NumberFormatException exception) { throw error("Invalid JSON number"); }
        }

        private Object literal(String text, Object value) throws IOException {
            if (!input.startsWith(text, position)) throw error("Invalid JSON literal");
            position += text.length();
            return value;
        }

        private void skipWhitespace() { while (position < input.length() && Character.isWhitespace(input.charAt(position))) position++; }
        private boolean consume(char expected) { if (position < input.length() && input.charAt(position) == expected) { position++; return true; } return false; }
        private void expect(char expected) throws IOException { if (!consume(expected)) throw error("Expected '" + expected + "'"); }
        private IOException error(String message) { return new IOException(message + " at JSON character " + position + "."); }
    }
}

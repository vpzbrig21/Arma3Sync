package fr.soe.a3sUpdater.dao;

import fr.soe.a3sUpdater.config.UpdateConfig;
import fr.soe.a3sUpdater.model.UpdateManifest;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;

/** Reads a GitHub Releases API response and turns one release asset into update metadata. */
public final class GitHubReleaseDAO {
    private static final String API_ACCEPT = "application/vnd.github+json";
    private static final String API_VERSION = "2022-11-28";

    public UpdateManifest read(URI apiUrl, String assetPattern, HttpDAO httpDAO,
                               UpdateConfig config) throws IOException {
        if (apiUrl == null || apiUrl.toString().isBlank()) {
            throw new IOException("GitHub API URL is empty.");
        }
        JsonManifestDAO.requireHttpUri(apiUrl, "GitHub API URL");
        if (assetPattern == null || assetPattern.isBlank()) {
            throw new IOException("GitHub asset pattern is empty.");
        }

        String response = httpDAO.readText(apiUrl, config,
                Map.of("Accept", API_ACCEPT, "X-GitHub-Api-Version", API_VERSION));
        Map<String, Object> release = JsonManifestDAO.parseObject(response);
        String tag = string(release, "tag_name");
        String version = normalizeVersion(tag);
        String expectedAsset = assetPattern.replace("{version}", version).replace("{tag}", tag);
        if (!isSafeZipName(expectedAsset)) {
            throw new IOException("GitHub asset pattern must resolve to a ZIP filename.");
        }

        Object rawAssets = release.get("assets");
        if (!(rawAssets instanceof List<?> assets)) throw new IOException("GitHub release has no assets.");
        Map<String, Object> asset = null;
        for (Object rawAsset : assets) {
            if (!(rawAsset instanceof Map<?, ?> values)) continue;
            Object name = values.get("name");
            if (expectedAsset.equals(name)) {
                asset = asStringMap(values);
                break;
            }
        }
        if (asset == null) throw new IOException("GitHub release asset not found: " + expectedAsset);

        String downloadUrl = string(asset, "browser_download_url");
        String digest = stringOrNull(asset, "digest");
        if (digest != null && digest.regionMatches(true, 0, "sha256:", 0, 7)) {
            digest = digest.substring(7).trim();
        }
        if (digest == null || !digest.matches("(?i)[0-9a-f]{64}")) {
            throw new IOException("GitHub release asset has no valid SHA-256 digest: " + expectedAsset);
        }

        URI downloadUri;
        try {
            downloadUri = URI.create(downloadUrl);
        } catch (IllegalArgumentException exception) {
            throw new IOException("GitHub asset has an invalid download URL: " + expectedAsset, exception);
        }
        JsonManifestDAO.requireHttpUri(downloadUri, "GitHub asset download URL");
        return new UpdateManifest(version, expectedAsset, downloadUri, digest.toLowerCase(),
                number(asset, "size"), UpdateManifest.Format.GITHUB, apiUrl.toString());
    }

    private static String normalizeVersion(String tag) throws IOException {
        if (tag == null || tag.isBlank()) throw new IOException("GitHub release has no tag_name.");
        String version = tag.trim();
        while (version.startsWith("v") || version.startsWith("V")) version = version.substring(1);
        if (!JsonManifestDAO.isSupportedVersion(version)) {
            throw new IOException("GitHub release tag is not a supported version: " + tag);
        }
        return version;
    }

    private static String string(Map<String, Object> object, String key) throws IOException {
        String value = stringOrNull(object, key);
        if (value == null || value.isBlank()) throw new IOException("GitHub release field is missing: " + key);
        return value.trim();
    }

    private static String stringOrNull(Map<String, Object> object, String key) throws IOException {
        Object value = object.get(key);
        if (value == null) return null;
        if (!(value instanceof String text)) throw new IOException("GitHub release field must be a string: " + key);
        return text.trim();
    }

    private static long number(Map<String, Object> object, String key) throws IOException {
        Object value = object.get(key);
        if (value == null) return 0L;
        if (!(value instanceof Number number) || number.longValue() < 0) {
            throw new IOException("GitHub release field must be a non-negative number: " + key);
        }
        return number.longValue();
    }

    private static Map<String, Object> asStringMap(Map<?, ?> values) throws IOException {
        java.util.LinkedHashMap<String, Object> result = new java.util.LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : values.entrySet()) {
            if (!(entry.getKey() instanceof String key)) throw new IOException("GitHub asset key must be a string.");
            result.put(key, entry.getValue());
        }
        return result;
    }

    private static boolean isSafeZipName(String name) {
        return name != null && !name.isBlank() && !name.contains("/") && !name.contains("\\")
                && !name.equals(".") && !name.equals("..") && name.toLowerCase().endsWith(".zip");
    }
}

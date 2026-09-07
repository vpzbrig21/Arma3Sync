package fr.soe.a3sUpdater.config;

import fr.soe.a3sUpdater.dao.DataAccessConstants;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/** Effective updater configuration, including backwards-compatible JVM overrides. */
public final class UpdateConfig implements DataAccessConstants {
    public static final String RELATIVE_PATH = "resources/configuration/updater.toml";
    private static final String USER_CONFIG_PROPERTY = "a3s.updater.userConfigPath";

    private final String manifestUrl;
    private final String legacyXmlUrl;
    private final String devManifestUrl;
    private final String devLegacyXmlUrl;
    private final boolean allowHttp;
    private final boolean githubEnabled;
    private final String githubApiUrl;
    private final String githubDevApiUrl;
    private final String githubAssetPattern;
    private final String githubDevAssetPattern;
    private final int connectTimeoutMs;
    private final int readTimeoutMs;

    private UpdateConfig(String manifestUrl, String legacyXmlUrl, String devManifestUrl,
                         String devLegacyXmlUrl, boolean allowHttp, boolean githubEnabled,
                         String githubApiUrl, String githubDevApiUrl, String githubAssetPattern,
                         String githubDevAssetPattern, int connectTimeoutMs, int readTimeoutMs) {
        this.manifestUrl = manifestUrl;
        this.legacyXmlUrl = legacyXmlUrl;
        this.devManifestUrl = devManifestUrl;
        this.devLegacyXmlUrl = devLegacyXmlUrl;
        this.allowHttp = allowHttp;
        this.githubEnabled = githubEnabled;
        this.githubApiUrl = githubApiUrl;
        this.githubDevApiUrl = githubDevApiUrl;
        this.githubAssetPattern = githubAssetPattern;
        this.githubDevAssetPattern = githubDevAssetPattern;
        this.connectTimeoutMs = connectTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
    }

    public static UpdateConfig load(Path installation) throws IOException {
        Path rootConfig = installation.resolve("updater.toml");
        Path installedConfig = installation.resolve(RELATIVE_PATH);
        String userConfigPath = System.getProperty(USER_CONFIG_PROPERTY);
        Path userConfig = userConfigPath == null || userConfigPath.isBlank()
                ? null : Path.of(userConfigPath).resolve("updater.toml");
        Path file = userConfig != null && Files.isRegularFile(userConfig) ? userConfig
                : Files.isRegularFile(rootConfig) ? rootConfig : installedConfig;
        Map<String, String> values = file == null || !Files.isRegularFile(file)
                ? Map.of() : TomlConfigDAO.read(file);

        String base = System.getProperty(HTTP_BASE_URL_PROPERTY, CURRENT_UPDATE_BASE_URL);
        String defaultManifest = trimSlash(base) + "/a3s.json";
        String defaultXml = trimSlash(base) + "/a3s.xml";
        String configuredManifest = TomlConfigDAO.string(values, "update.manifest_url", defaultManifest);
        String configuredXml = TomlConfigDAO.string(values, "update.legacy_xml_url", defaultXml);
        String devManifest = TomlConfigDAO.string(values, "update.dev_manifest_url", configuredManifest);
        String devXml = TomlConfigDAO.string(values, "update.dev_legacy_xml_url", configuredXml);
        if (devManifest.isBlank()) devManifest = configuredManifest;
        if (devXml.isBlank()) devXml = configuredXml;

        String propertyManifest = System.getProperty("a3s.updater.manifestUrl");
        if (propertyManifest != null) configuredManifest = propertyManifest;
        String propertyXml = System.getProperty("a3s.updater.legacyXmlUrl");
        if (propertyXml != null) configuredXml = propertyXml;
        String propertyDevManifest = System.getProperty("a3s.updater.manifestUrl.dev");
        if (propertyDevManifest != null) devManifest = propertyDevManifest;
        String propertyDevXml = System.getProperty("a3s.updater.legacyXmlUrl.dev");
        if (propertyDevXml != null) devXml = propertyDevXml;

        boolean allowHttp = Boolean.getBoolean("a3s.updater.allowHttp")
                || TomlConfigDAO.bool(values, "update.allow_http", false);
        boolean githubEnabled = Boolean.getBoolean("a3s.updater.github.enabled")
                || TomlConfigDAO.bool(values, "github.enabled", true);
        String githubApiUrl = TomlConfigDAO.string(values, "github.api_url",
                "https://api.github.com/repos/vpzbrig21/Arma3Sync/releases/latest");
        String githubDevApiUrl = TomlConfigDAO.string(values, "github.dev_api_url", githubApiUrl);
        String githubAssetPattern = TomlConfigDAO.string(values, "github.asset_pattern",
                "Arma3Sync-{version}.zip");
        String githubDevAssetPattern = TomlConfigDAO.string(values, "github.dev_asset_pattern", githubAssetPattern);
        String propertyGithubApi = System.getProperty("a3s.updater.github.apiUrl");
        if (propertyGithubApi != null) githubApiUrl = propertyGithubApi;
        String propertyGithubDevApi = System.getProperty("a3s.updater.github.apiUrl.dev");
        if (propertyGithubDevApi != null) githubDevApiUrl = propertyGithubDevApi;
        int connectTimeout = TomlConfigDAO.integer(values, "update.connect_timeout_ms", 30_000);
        int readTimeout = TomlConfigDAO.integer(values, "update.read_timeout_ms", 30_000);
        return new UpdateConfig(configuredManifest, configuredXml, devManifest, devXml,
                allowHttp, githubEnabled, githubApiUrl, githubDevApiUrl, githubAssetPattern,
                githubDevAssetPattern, connectTimeout, readTimeout);
    }

    public String manifestUrl(boolean devMode) { return devMode ? devManifestUrl : manifestUrl; }
    public String legacyXmlUrl(boolean devMode) { return devMode ? devLegacyXmlUrl : legacyXmlUrl; }
    public boolean allowHttp() { return allowHttp; }
    public boolean githubEnabled() { return githubEnabled; }
    public String githubApiUrl(boolean devMode) { return devMode ? githubDevApiUrl : githubApiUrl; }
    public String githubAssetPattern(boolean devMode) {
        return devMode ? githubDevAssetPattern : githubAssetPattern;
    }
    public int connectTimeoutMs() { return connectTimeoutMs; }
    public int readTimeoutMs() { return readTimeoutMs; }

    private static String trimSlash(String value) {
        if (value == null || value.isBlank()) return CURRENT_UPDATE_BASE_URL;
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}

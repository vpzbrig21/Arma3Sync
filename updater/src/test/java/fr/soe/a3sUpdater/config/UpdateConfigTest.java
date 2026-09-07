package fr.soe.a3sUpdater.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class UpdateConfigTest {

    @TempDir
    Path tempDir;

    @Test
    void prefersUserConfigurationOverInstalledDefault() throws Exception {
        Path installationConfig = tempDir.resolve("installation/resources/configuration");
        Path userConfig = tempDir.resolve("user/configuration");
        Files.createDirectories(installationConfig);
        Files.createDirectories(userConfig);
        Files.writeString(installationConfig.resolve("updater.toml"),
                "[update]\nmanifest_url = \"https://installed.example/a3s.json\"\n");
        Files.writeString(userConfig.resolve("updater.toml"),
                "[update]\nmanifest_url = \"https://user.example/a3s.json\"\n");

        String previous = System.getProperty("a3s.updater.userConfigPath");
        try {
            System.setProperty("a3s.updater.userConfigPath", userConfig.toString());
            UpdateConfig config = UpdateConfig.load(tempDir.resolve("installation"));
            assertEquals("https://user.example/a3s.json", config.manifestUrl(false));
        } finally {
            if (previous == null) {
                System.clearProperty("a3s.updater.userConfigPath");
            } else {
                System.setProperty("a3s.updater.userConfigPath", previous);
            }
        }
    }

    @Test
    void readsOptionalGithubConfiguration() throws Exception {
        Path installation = tempDir.resolve("installation");
        Files.createDirectories(installation);
        Files.writeString(installation.resolve("updater.toml"),
                "[github]\n" +
                "enabled = true\n" +
                "api_url = \"https://api.github.com/repos/example/project/releases/latest\"\n" +
                "asset_pattern = \"Arma3Sync-{tag}.zip\"\n");

        UpdateConfig config = UpdateConfig.load(installation);

        assertEquals(true, config.githubEnabled());
        assertEquals("https://api.github.com/repos/example/project/releases/latest", config.githubApiUrl(false));
        assertEquals("Arma3Sync-{tag}.zip", config.githubAssetPattern(false));
    }

    @Test
    void enablesGithubByDefaultWhenNoExplicitSettingExists() throws Exception {
        Path installation = tempDir.resolve("installation-defaults");
        Files.createDirectories(installation);
        Files.writeString(installation.resolve("updater.toml"),
                "[update]\nmanifest_url = \"https://updates.example/a3s.json\"\n");

        UpdateConfig config = UpdateConfig.load(installation);

        assertEquals(true, config.githubEnabled());
    }
}

package fr.soe.a3s.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import fr.soe.a3s.constant.CheckRepositoriesFrequency;
import fr.soe.a3s.constant.IconResize;
import fr.soe.a3s.constant.LookAndFeel;
import fr.soe.a3s.constant.MinimizationType;
import fr.soe.a3s.constant.StartWithOS;
import fr.soe.a3s.domain.Preferences;

class PreferencesDAOTest {

    @TempDir
    Path tempDir;

    @Test
    void writeThenReadPreferencesFile() throws Exception {
        Path installation = tempDir.resolve("app");
        Files.createDirectories(installation);

        String previousInstallPath = System.getProperty("a3s.installationPath");
        String previousConfigPath = System.getProperty("a3s.configPath");
        System.setProperty("a3s.installationPath", installation.toString());
        System.setProperty("a3s.configPath", installation.resolve("config").toString());
        try {
            PreferencesDAO dao = new PreferencesDAO();
            Preferences prefs = new Preferences();
            prefs.setLaunchPanelGameLaunch(MinimizationType.TRAY);
            prefs.setLookAndFeel(LookAndFeel.LAF_HIFI);
            prefs.setIconResizeSize(IconResize.AUTO);
            prefs.setStartWithOS(StartWithOS.ENABLED);
            prefs.setCheckRepositoriesFrequency(CheckRepositoriesFrequency.FREQ2);
            dao.setPreferences(prefs);

            dao.write();

            Path prefsFile = Path.of(ApplicationPaths.preferencesFilePath());
            assertTrue(Files.isRegularFile(prefsFile), "Preferences file should be created under the user config directory");

            PreferencesDAO verifier = new PreferencesDAO();
            verifier.setPreferences(new Preferences());
            verifier.read();
            Preferences restored = verifier.getPreferences();

            assertEquals(prefs.getLookAndFeel(), restored.getLookAndFeel());
            assertEquals(prefs.getStartWithOS(), restored.getStartWithOS());
            assertEquals(prefs.getCheckRepositoriesFrequency(), restored.getCheckRepositoriesFrequency());
        } finally {
            if (previousInstallPath == null) {
                System.clearProperty("a3s.installationPath");
            } else {
                System.setProperty("a3s.installationPath", previousInstallPath);
            }
            if (previousConfigPath == null) {
                System.clearProperty("a3s.configPath");
            } else {
                System.setProperty("a3s.configPath", previousConfigPath);
            }
        }
    }
}

package fr.soe.a3s.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import fr.soe.a3s.constant.CheckRepositoriesFrequency;
import fr.soe.a3s.constant.IconResize;
import fr.soe.a3s.constant.LookAndFeel;
import fr.soe.a3s.constant.MinimizationType;
import fr.soe.a3s.constant.StartWithOS;
import fr.soe.a3s.domain.Preferences;

class A3SFilesAccessorTest {

    @TempDir
    Path tempDir;

    @Test
    void writeThenReadPreferencesRoundtrip() throws Exception {
        Preferences preferences = new Preferences();
        preferences.setLaunchPanelGameLaunch(MinimizationType.TRAY);
        preferences.setLaunchPanelMinimized(MinimizationType.TASK_BAR);
        preferences.setLookAndFeel(LookAndFeel.LAF_GRAPHITE);
        preferences.setIconResizeSize(IconResize.AUTO);
        preferences.setStartWithOS(StartWithOS.ENABLED);
        preferences.setCheckRepositoriesFrequency(CheckRepositoriesFrequency.FREQ1);

        File file = tempDir.resolve("prefs.bin").toFile();
        A3SFilesAccessor.write(preferences, file);

        assertTrue(file.isFile(), "Serialized preference file should exist");

        Object result = A3SFilesAccessor.read(file);
        Preferences restored = (Preferences) result;
        assertEquals(preferences.getLookAndFeel(), restored.getLookAndFeel());
        assertEquals(preferences.getLaunchPanelGameLaunch(), restored.getLaunchPanelGameLaunch());
        assertEquals(preferences.getStartWithOS(), restored.getStartWithOS());
        assertEquals(preferences.getCheckRepositoriesFrequency(), restored.getCheckRepositoriesFrequency());
    }
}

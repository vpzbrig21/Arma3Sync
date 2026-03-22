package fr.soe.a3s.ui;

import java.awt.Window;
import java.util.prefs.Preferences;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.extras.FlatAnimatedLafChange;

import fr.soe.a3s.ui.icon.Icons;
import fr.soe.a3s.ui.theme.ThemeDefinition;
import fr.soe.a3s.ui.theme.ThemeLibrary;

public final class ThemeManager {

    private static final String NODE = "ui.theme";
    private static final String KEY_DARK = "dark";
    private static ThemeDefinition currentTheme = ThemeLibrary.dark();

    private ThemeManager() {
    }

    public static void applyInitialLaf() {
        boolean dark = isDark();
        currentTheme = ThemeLibrary.forMode(dark);
        UiBootstrap.initLaf(currentTheme);
        Icons.invalidate();
        FontInstaller.applyUIFont();
    }

    public static boolean isDark() {
        return preferences().getBoolean(KEY_DARK, true);
    }

    public static ThemeDefinition currentTheme() {
        return currentTheme;
    }

    public static boolean toggleTheme(JFrame frame) {
        boolean dark = !isDark();
        Preferences prefs = preferences();
        prefs.putBoolean(KEY_DARK, dark);
        try {
            prefs.flush();
        } catch (Exception ignore) {
        }
        currentTheme = ThemeLibrary.forMode(dark);
        FlatAnimatedLafChange.showSnapshot();
        UiBootstrap.initLaf(currentTheme);
        Icons.invalidate();
        FontInstaller.applyUIFont();
        FlatLaf.updateUI();
        FlatAnimatedLafChange.hideSnapshotWithAnimation();
        refreshAllWindows();
        return dark;
    }

    private static void refreshAllWindows() {
        for (Window window : Window.getWindows()) {
            if (window == null || !window.isDisplayable()) {
                continue;
            }
            SwingUtilities.updateComponentTreeUI(window);
            UiStyle.refreshTree(window);
            window.invalidate();
            window.validate();
            window.repaint();
        }
    }

    private static Preferences preferences() {
        return Preferences.userRoot().node(NODE);
    }
}

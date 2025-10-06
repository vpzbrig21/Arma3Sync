package fr.soe.a3s.ui;

import java.util.prefs.Preferences;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.extras.FlatAnimatedLafChange;

public final class ThemeManager {

    private static final String NODE = "ui.theme";
    private static final String KEY_DARK = "dark";

    private ThemeManager() {
    }

    public static void applyInitialLaf() {
        UiBootstrap.initLaf(isDark());
    }

    public static boolean isDark() {
        return preferences().getBoolean(KEY_DARK, true);
    }

    public static boolean toggleTheme(JFrame frame) {
        boolean dark = !isDark();
        Preferences prefs = preferences();
        prefs.putBoolean(KEY_DARK, dark);
        try {
            prefs.flush();
        } catch (Exception ignore) {
        }
        FlatAnimatedLafChange.showSnapshot();
        UiBootstrap.initLaf(dark);
        FlatLaf.updateUI();
        FlatAnimatedLafChange.hideSnapshotWithAnimation();
        SwingUtilities.updateComponentTreeUI(frame);
        frame.invalidate();
        frame.repaint();
        return dark;
    }

    private static Preferences preferences() {
        return Preferences.userRoot().node(NODE);
    }
}

package fr.soe.a3s.ui;

import java.awt.Color;

import fr.soe.a3s.ui.theme.ThemePalette;
import fr.soe.a3s.ui.theme.ThemeTokens;

/**
 * Convenience accessors for frequently used theme-driven colors so individual components
 * do not need to query ThemeTokens directly.
 */
public final class UiColors {

    private UiColors() {
    }

    private static ThemePalette palette() {
        return ThemeTokens.colors();
    }

    public static Color surface() {
        return palette().surface();
    }

    public static Color surfaceMuted() {
        return palette().surfaceMuted();
    }

    public static Color surfaceVariant() {
        return palette().surfaceVariant();
    }

    public static Color border() {
        return palette().border();
    }

    public static Color borderMuted() {
        return palette().borderMuted();
    }

    public static Color textPrimary() {
        return palette().textPrimary();
    }

    public static Color textSecondary() {
        return palette().textSecondary();
    }

    public static Color textDisabled() {
        return palette().textDisabled();
    }

    public static Color accentPrimary() {
        return palette().accentPrimary();
    }

    public static Color statusSuccess() {
        return palette().statusSuccess();
    }

    public static Color statusWarning() {
        return palette().statusWarning();
    }

    public static Color statusDanger() {
        return palette().statusDanger();
    }

    public static Color statusInfo() {
        return palette().statusInfo();
    }

    public static Color selectionBackground() {
        return palette().stateSelectionBg();
    }

    public static Color selectionForeground() {
        return palette().stateSelectionFg();
    }

    public static Color disabledSurface() {
        return palette().stateDisabledBg();
    }
}

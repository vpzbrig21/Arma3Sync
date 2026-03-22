package fr.soe.a3s.ui.theme;

import java.awt.Color;

/**
 * Immutable color palette for a UI theme.
 */
public final class ThemePalette {

    private final Color windowBackground;
    private final Color surface;
    private final Color surfaceVariant;
    private final Color surfaceMuted;
    private final Color border;
    private final Color borderMuted;
    private final Color textPrimary;
    private final Color textSecondary;
    private final Color textDisabled;
    private final Color iconDefault;
    private final Color accentPrimary;
    private final Color accentSecondary;
    private final Color statusSuccess;
    private final Color statusWarning;
    private final Color statusDanger;
    private final Color statusInfo;
    private final Color stateHoverOverlay;
    private final Color stateFocus;
    private final Color stateSelectionBg;
    private final Color stateSelectionFg;
    private final Color stateDisabledBg;

    public ThemePalette(Color windowBackground, Color surface, Color surfaceVariant, Color surfaceMuted, Color border,
            Color borderMuted, Color textPrimary, Color textSecondary, Color textDisabled, Color iconDefault,
            Color accentPrimary, Color accentSecondary, Color statusSuccess, Color statusWarning, Color statusDanger,
            Color statusInfo, Color stateHoverOverlay, Color stateFocus, Color stateSelectionBg, Color stateSelectionFg,
            Color stateDisabledBg) {
        this.windowBackground = windowBackground;
        this.surface = surface;
        this.surfaceVariant = surfaceVariant;
        this.surfaceMuted = surfaceMuted;
        this.border = border;
        this.borderMuted = borderMuted;
        this.textPrimary = textPrimary;
        this.textSecondary = textSecondary;
        this.textDisabled = textDisabled;
        this.iconDefault = iconDefault;
        this.accentPrimary = accentPrimary;
        this.accentSecondary = accentSecondary;
        this.statusSuccess = statusSuccess;
        this.statusWarning = statusWarning;
        this.statusDanger = statusDanger;
        this.statusInfo = statusInfo;
        this.stateHoverOverlay = stateHoverOverlay;
        this.stateFocus = stateFocus;
        this.stateSelectionBg = stateSelectionBg;
        this.stateSelectionFg = stateSelectionFg;
        this.stateDisabledBg = stateDisabledBg;
    }

    public Color windowBackground() {
        return windowBackground;
    }

    public Color surface() {
        return surface;
    }

    public Color surfaceVariant() {
        return surfaceVariant;
    }

    public Color surfaceMuted() {
        return surfaceMuted;
    }

    public Color border() {
        return border;
    }

    public Color borderMuted() {
        return borderMuted;
    }

    public Color textPrimary() {
        return textPrimary;
    }

    public Color textSecondary() {
        return textSecondary;
    }

    public Color textDisabled() {
        return textDisabled;
    }

    public Color iconDefault() {
        return iconDefault;
    }

    public Color accentPrimary() {
        return accentPrimary;
    }

    public Color accentSecondary() {
        return accentSecondary;
    }

    public Color statusSuccess() {
        return statusSuccess;
    }

    public Color statusWarning() {
        return statusWarning;
    }

    public Color statusDanger() {
        return statusDanger;
    }

    public Color statusInfo() {
        return statusInfo;
    }

    public Color stateHoverOverlay() {
        return stateHoverOverlay;
    }

    public Color stateFocus() {
        return stateFocus;
    }

    public Color stateSelectionBg() {
        return stateSelectionBg;
    }

    public Color stateSelectionFg() {
        return stateSelectionFg;
    }

    public Color stateDisabledBg() {
        return stateDisabledBg;
    }
}

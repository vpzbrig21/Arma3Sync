package fr.soe.a3s.ui.theme;

/**
 * Aggregates colors and metrics for a light or dark theme.
 */
public final class ThemeDefinition {

    private final String id;
    private final boolean dark;
    private final ThemePalette palette;
    private final ThemeMetrics metrics;

    public ThemeDefinition(String id, boolean dark, ThemePalette palette, ThemeMetrics metrics) {
        this.id = id;
        this.dark = dark;
        this.palette = palette;
        this.metrics = metrics;
    }

    public String id() {
        return id;
    }

    public boolean isDark() {
        return dark;
    }

    public ThemePalette palette() {
        return palette;
    }

    public ThemeMetrics metrics() {
        return metrics;
    }
}

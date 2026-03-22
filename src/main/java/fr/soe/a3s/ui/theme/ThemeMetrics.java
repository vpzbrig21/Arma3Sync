package fr.soe.a3s.ui.theme;

/**
 * Tokenised spacing, curvature and effect values shared across the UI.
 */
public final class ThemeMetrics {

    private final int spacingXs;
    private final int spacingSm;
    private final int spacingMd;
    private final int spacingLg;
    private final int spacingXl;
    private final int spacingXxl;
    private final int radiusSm;
    private final int radiusMd;
    private final int radiusLg;
    private final int focusWidth;
    private final ThemeShadow shadowLow;
    private final ThemeShadow shadowMedium;

    public ThemeMetrics(int spacingXs, int spacingSm, int spacingMd, int spacingLg, int spacingXl, int spacingXxl,
            int radiusSm, int radiusMd, int radiusLg, int focusWidth, ThemeShadow shadowLow, ThemeShadow shadowMedium) {
        this.spacingXs = spacingXs;
        this.spacingSm = spacingSm;
        this.spacingMd = spacingMd;
        this.spacingLg = spacingLg;
        this.spacingXl = spacingXl;
        this.spacingXxl = spacingXxl;
        this.radiusSm = radiusSm;
        this.radiusMd = radiusMd;
        this.radiusLg = radiusLg;
        this.focusWidth = focusWidth;
        this.shadowLow = shadowLow;
        this.shadowMedium = shadowMedium;
    }

    public int spacingXs() {
        return spacingXs;
    }

    public int spacingSm() {
        return spacingSm;
    }

    public int spacingMd() {
        return spacingMd;
    }

    public int spacingLg() {
        return spacingLg;
    }

    public int spacingXl() {
        return spacingXl;
    }

    public int spacingXxl() {
        return spacingXxl;
    }

    public int radiusSm() {
        return radiusSm;
    }

    public int radiusMd() {
        return radiusMd;
    }

    public int radiusLg() {
        return radiusLg;
    }

    public int focusWidth() {
        return focusWidth;
    }

    public ThemeShadow shadowLow() {
        return shadowLow;
    }

    public ThemeShadow shadowMedium() {
        return shadowMedium;
    }
}

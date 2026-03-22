package fr.soe.a3s.ui.theme;

import java.awt.Color;

/**
 * Provides the canonical light and dark theme definitions for the application.
 */
public final class ThemeLibrary {

    private static final ThemeDefinition LIGHT = buildLight();
    private static final ThemeDefinition DARK = buildDark();

    private ThemeLibrary() {
    }

    public static ThemeDefinition light() {
        return LIGHT;
    }

    public static ThemeDefinition dark() {
        return DARK;
    }

    public static ThemeDefinition forMode(boolean dark) {
        return dark ? dark() : light();
    }

    private static ThemeDefinition buildLight() {
        ThemePalette palette = new ThemePalette(new Color(0xF4F6FB), new Color(0xFFFFFF), new Color(0xF6F8FC),
                new Color(0xE9EDF5), new Color(0xDFE4EF), new Color(0xE4E8F0), new Color(0x1E232C), new Color(0x4B5565),
                new Color(0x9CA3AF), new Color(0x1F2937), new Color(0x2563EB), new Color(0x7C3AED), new Color(0x16A34A),
                new Color(0xF59E0B), new Color(0xDC2626), new Color(0x0284C7), new Color(15, 23, 42, 24),
                new Color(0x2563EB), new Color(0xDCE7FF), new Color(0x1E3A8A), new Color(0xE5E7EB));

        ThemeMetrics metrics = new ThemeMetrics(4, 8, 12, 16, 24, 32, 8, 12, 18, 1,
                new ThemeShadow(new Color(0x1F2937), 0.1f, 1, 6), new ThemeShadow(new Color(0x111827), 0.14f, 4, 16));

        return new ThemeDefinition("light", false, palette, metrics);
    }

    private static ThemeDefinition buildDark() {
        ThemePalette palette = new ThemePalette(new Color(0x10151E), new Color(0x1B222C), new Color(0x242B38),
                new Color(0x2D3545), new Color(0x3A4250), new Color(0x2E3645), new Color(0xF3F4F6), new Color(0xD1D5DB),
                new Color(0x8B94A7), new Color(0xCBD5F5), new Color(0x60A5FA), new Color(0xA78BFA), new Color(0x22C55E),
                new Color(0xF4A259), new Color(0xF87171), new Color(0x38BDF8), new Color(148, 163, 184, 36),
                new Color(0x93C5FD), new Color(0x1E3A8A), new Color(0xE0EAFF), new Color(0x1F2937));

        ThemeMetrics metrics = new ThemeMetrics(4, 8, 12, 16, 24, 32, 8, 12, 18, 1,
                new ThemeShadow(new Color(0x000000), 0.4f, 2, 12), new ThemeShadow(new Color(0x000000), 0.55f, 6, 28));

        return new ThemeDefinition("dark", true, palette, metrics);
    }
}

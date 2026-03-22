package fr.soe.a3s.ui.icon;

import java.awt.Color;
import java.awt.Image;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.Icon;

import com.formdev.flatlaf.extras.FlatSVGIcon;

import fr.soe.a3s.ui.ThemeManager;
import fr.soe.a3s.ui.UiColors;
import fr.soe.a3s.ui.theme.ThemeDefinition;

public final class Icons {

    private static final Map<CacheKey, Icon> CACHE = new ConcurrentHashMap<CacheKey, Icon>();
    private static final double SCALE = resolveScale();
    private static final int SCALE_THRESHOLD = 64;

    private Icons() {
    }

    public static Icon icon(UiIcon icon, int size) {
        return icon(icon, size, IconState.NORMAL);
    }

    public static Icon icon(UiIcon icon, int size, IconState state) {
        int targetSize = scaledSize(size);
        CacheKey key = new CacheKey(icon, targetSize, state, themeId());
        return CACHE.computeIfAbsent(key, k -> createIcon(k.icon(), k.size(), k.state()));
    }

    public static Image image(UiIcon icon, int size) {
        return image(icon, size, IconState.NORMAL);
    }

    public static Image image(UiIcon icon, int size, IconState state) {
        Icon svg = icon(icon, size, state);
        if (svg instanceof FlatSVGIcon) {
            return ((FlatSVGIcon) svg).getImage();
        }
        throw new IllegalStateException("Icon instance does not expose an image: " + icon);
    }

    public static void invalidate() {
        CACHE.clear();
    }

    private static Icon createIcon(UiIcon icon, int size, IconState state) {
        FlatSVGIcon svg = new FlatSVGIcon(icon.resourcePath(), size, size);
        if (icon.isThemeAware() || state == IconState.DISABLED) {
            svg.setColorFilter(toneFilter(icon, state));
        }
        return svg;
    }

    private static Color resolveToneColor(UiIcon icon, IconState state) {
        if (state == IconState.DISABLED) {
            return UiColors.textDisabled();
        }
        if (state == IconState.SELECTED) {
            return UiColors.selectionForeground();
        }
        return switch (icon.tone()) {
        case ACCENT -> UiColors.accentPrimary();
        case DANGER -> UiColors.statusDanger();
        case INFO -> UiColors.statusInfo();
        case MUTED -> UiColors.textSecondary();
        case SUCCESS -> UiColors.statusSuccess();
        case WARNING -> UiColors.statusWarning();
        case PRIMARY -> UiColors.textPrimary();
        };
    }

    private static String themeId() {
        ThemeDefinition definition = ThemeManager.currentTheme();
        if (definition != null) {
            return definition.id();
        }
        return ThemeManager.isDark() ? "dark" : "light";
    }

    private static int scaledSize(int size) {
        if (size > SCALE_THRESHOLD) {
            return size;
        }
        return Math.max(8, (int) Math.round(size * SCALE));
    }

    private static double resolveScale() {
        try {
            return Double.parseDouble(System.getProperty("a3s.iconScale", "1.5"));
        } catch (NumberFormatException ignore) {
            return 1.5d;
        }
    }

    private static FlatSVGIcon.ColorFilter toneFilter(UiIcon icon, IconState state) {
        return new FlatSVGIcon.ColorFilter(c -> resolveToneColor(icon, state));
    }

    private static record CacheKey(UiIcon icon, int size, IconState state, String theme) {
    }
}

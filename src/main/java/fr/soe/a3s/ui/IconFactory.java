package fr.soe.a3s.ui;

import java.awt.Image;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.Icon;

import com.formdev.flatlaf.extras.FlatSVGIcon;

public final class IconFactory {

    private static final String BASE_PATH = "resources/icons/";
    private static final double SCALE = resolveScale();
    private static final int SCALE_THRESHOLD = 64;
    private static final Map<String, FlatSVGIcon> CACHE = new ConcurrentHashMap<>();

    private IconFactory() {
    }

    public static Icon of(String name, int size) {
        return createIcon(name, size, true);
    }

    public static Image image(String name, int size) {
        FlatSVGIcon icon = createIcon(name, size, false);
        return icon.getImage();
    }

    private static FlatSVGIcon createIcon(String name, int size, boolean applyScale) {
        int targetSize = applyScale ? scaled(size) : size;
        String key = name + "@" + targetSize;
        return CACHE.computeIfAbsent(key, k -> new FlatSVGIcon(BASE_PATH + name + ".svg", targetSize, targetSize));
    }

    private static int scaled(int size) {
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
}

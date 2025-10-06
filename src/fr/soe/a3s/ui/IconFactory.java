package fr.soe.a3s.ui;

import java.awt.Image;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.Icon;

import com.formdev.flatlaf.extras.FlatSVGIcon;

public final class IconFactory {

    private static final String BASE_PATH = "resources/icons/";
    private static final Map<String, FlatSVGIcon> CACHE = new ConcurrentHashMap<>();

    private IconFactory() {
    }

    public static Icon of(String name, int size) {
        return createIcon(name, size);
    }

    public static Image image(String name, int size) {
        FlatSVGIcon icon = createIcon(name, size);
        return icon.getImage();
    }

    private static FlatSVGIcon createIcon(String name, int size) {
        String key = name + "@" + size;
        return CACHE.computeIfAbsent(key, k -> new FlatSVGIcon(BASE_PATH + name + ".svg", size, size));
    }
}

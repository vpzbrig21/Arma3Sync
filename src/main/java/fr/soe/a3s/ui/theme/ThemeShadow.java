package fr.soe.a3s.ui.theme;

import java.awt.Color;
import java.util.Objects;

/**
 * Simple immutable description of a drop shadow that can later be mapped to Swing borders or painters.
 */
public final class ThemeShadow {

    private final Color color;
    private final float opacity;
    private final int yOffset;
    private final int blurRadius;

    public ThemeShadow(Color color, float opacity, int yOffset, int blurRadius) {
        this.color = Objects.requireNonNull(color, "color");
        this.opacity = Math.max(0f, Math.min(1f, opacity));
        this.yOffset = yOffset;
        this.blurRadius = Math.max(0, blurRadius);
    }

    public Color color() {
        return color;
    }

    public float opacity() {
        return opacity;
    }

    public int yOffset() {
        return yOffset;
    }

    public int blurRadius() {
        return blurRadius;
    }
}

package fr.soe.a3sUpdater.ui;

import java.awt.Image;
import javax.swing.ImageIcon;

public final class ImageResizer {
    private ImageResizer() { }

    public static Image resizeToNewWidth(Image image, int width) {
        if (image == null) return null;
        ImageIcon icon = new ImageIcon(image);
        if (icon.getIconWidth() <= 0) return image;
        int height = width * icon.getIconHeight() / icon.getIconWidth();
        return image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
    }
}

package fr.soe.a3sUpdater.ui;

import java.awt.Image;
import java.awt.Toolkit;
import java.net.URL;

public interface UIConstants {
    String APPLICATION_NAME = "ArmA3Sync Updater";
    String TARGET_APPLICATION_NAME = "ArmA3Sync";
    Image ICON = image("resources/pictures/system/ArmA3SyncBlue24x24.png");
    Image PICTURE = image("resources/pictures/system/ArmA3SyncBlue64x64.png");

    private static Image image(String resource) {
        try {
            URL url = UIConstants.class.getClassLoader().getResource(resource);
            return url == null ? null : Toolkit.getDefaultToolkit().getImage(url);
        } catch (RuntimeException ignored) {
            return null;
        }
    }
}

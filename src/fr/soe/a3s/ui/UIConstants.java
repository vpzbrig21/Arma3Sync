package fr.soe.a3s.ui;

import java.awt.Image;

public interface UIConstants {

        String APPLICATION_NAME = "Arma3Sync";

        int DEFAULT_HEIGHT = 665;

        int OP_PROFILE_CHANGED = 1;
        int OP_ADDON_FILES_CHANGED = 2;
        int OP_ADDON_PRIORITY_CHANGED = 3;
        int OP_ADDON_SELECTION_CHANGED = 4;
        int OP_ONLINE_CHANGED = 5;
        int OP_REPOSITORY_CHANGED = 6;
        int OP_GROUP_CHANGED = 7;

        Image ICON = IconFactory.image("app", 32);
        Image TRAYICON = IconFactory.image("app", 16);
        Image PICTURE = IconFactory.image("app", 256);
        Image SOE = IconFactory.image("community", 256);
}

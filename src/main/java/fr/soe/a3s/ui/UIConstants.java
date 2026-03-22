package fr.soe.a3s.ui;

import java.awt.Image;

import fr.soe.a3s.ui.icon.Icons;
import fr.soe.a3s.ui.icon.UiIcon;

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

        Image ICON = Icons.image(UiIcon.APP, 32);
        Image TRAYICON = Icons.image(UiIcon.APP, 16);
        Image PICTURE = Icons.image(UiIcon.APP, 256);
        Image SOE = Icons.image(UiIcon.COMMUNITY, 256);
        Image WARNING = Icons.image(UiIcon.WARNING, 128);
        Image ACRE2_BIG = Icons.image(UiIcon.RADIO, 256);
        Image TFAR_BIG = Icons.image(UiIcon.RADIO, 256);
}

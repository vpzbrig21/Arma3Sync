package fr.soe.a3s.ui;

import java.awt.Image;
import java.awt.Toolkit;

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

	Image ICON = Toolkit
			.getDefaultToolkit()
			.getImage(
					java.lang.ClassLoader
							.getSystemResource("resources/pictures/system/icons/ArmA3SyncBlue32x32.png"));

	Image TRAYICON = Toolkit
			.getDefaultToolkit()
			.getImage(
					java.lang.ClassLoader
							.getSystemResource("resources/pictures/system/icons/ArmA3SyncBlue16x16.png"));

	Image PICTURE = Toolkit
			.getDefaultToolkit()
			.getImage(
					java.lang.ClassLoader
							.getSystemResource("resources/pictures/system/icons/ArmA3SyncBlue256x256.png"));

	Image SOE = Toolkit.getDefaultToolkit().getImage(
			java.lang.ClassLoader
					.getSystemResource("resources/pictures/system/soe2.png"));

	/* Buttons */

	Image ADD = Toolkit
			.getDefaultToolkit()
			.getImage(
					java.lang.ClassLoader
							.getSystemResource("resources/pictures/system/icons/2025_add24x24.png"));

	Image EDIT = Toolkit
			.getDefaultToolkit()
			.getImage(
					java.lang.ClassLoader
							.getSystemResource("resources/pictures/system/icons/2025_edit24x24.png"));

	Image DELETE = Toolkit
			.getDefaultToolkit()
			.getImage(
					java.lang.ClassLoader
							.getSystemResource("resources/pictures/system/icons/2025_delete24x24.png"));
	Image CANCEL = Toolkit
			.getDefaultToolkit()
			.getImage(
					java.lang.ClassLoader
							.getSystemResource("resources/pictures/system/icons/2025_cancel24x24.png"));
	
	Image DUPLICATE = Toolkit
			.getDefaultToolkit()
			.getImage(
					java.lang.ClassLoader
							.getSystemResource("resources/pictures/system/icons/2025_duplicate24x24.png"));

	Image ADMIN = Toolkit
			.getDefaultToolkit()
			.getImage(
					java.lang.ClassLoader
							.getSystemResource("resources/pictures/system/icons/2025_admin24x24.png"));

	Image REPORT = Toolkit
			.getDefaultToolkit()
			.getImage(
					java.lang.ClassLoader
							.getSystemResource("resources/pictures/system/icons/2025_report24x24.png"));

	Image CONNECT = Toolkit
			.getDefaultToolkit()
			.getImage(
					java.lang.ClassLoader
							.getSystemResource("resources/pictures/system/icons/2025_import24x24.png"));

	Image ONOFF = Toolkit
			.getDefaultToolkit()
			.getImage(
					java.lang.ClassLoader
							.getSystemResource("resources/pictures/system/icons/2025_on_off24x24.png"));

	Image REFRESH = Toolkit
			.getDefaultToolkit()
			.getImage(
					java.lang.ClassLoader
							.getSystemResource("resources/pictures/system/icons/2025_refresh24x24.png"));

	Image TOP = Toolkit
			.getDefaultToolkit()
			.getImage(
					java.lang.ClassLoader
							.getSystemResource("resources/pictures/system/icons/2025_top24x24.png"));

	Image UP = Toolkit
			.getDefaultToolkit()
			.getImage(
					java.lang.ClassLoader
							.getSystemResource("resources/pictures/system/icons/2025_up24x24.png"));

	Image DOWN = Toolkit
			.getDefaultToolkit()
			.getImage(
					java.lang.ClassLoader
							.getSystemResource("resources/pictures/system/icons/2025_down24x24.png"));

	Image REPOSITORY = Toolkit
			.getDefaultToolkit()
			.getImage(
					java.lang.ClassLoader
							.getSystemResource("resources/pictures/system/repository24x24.png"));

	Image DOWNLOAD = Toolkit
			.getDefaultToolkit()
			.getImage(
					java.lang.ClassLoader
							.getSystemResource("resources/pictures/system/download24x24.png"));

	Image EVENTS = Toolkit
			.getDefaultToolkit()
			.getImage(
					java.lang.ClassLoader
							.getSystemResource("resources/pictures/system/events24x24.png"));

	Image START = Toolkit
			.getDefaultToolkit()
			.getImage(
					java.lang.ClassLoader
							.getSystemResource("resources/pictures/system/icons/2025_start24x24.png"));

	Image CHECK = Toolkit
			.getDefaultToolkit()
			.getImage(
					java.lang.ClassLoader
							.getSystemResource("resources/pictures/system/icons/2025_check24x24.png"));

	Image PAUSE = Toolkit
			.getDefaultToolkit()
			.getImage(
					java.lang.ClassLoader
							.getSystemResource("resources/pictures/system/icons/2025_pause24x24.png"));
	Image STOP = Toolkit
			.getDefaultToolkit()
			.getImage(
					java.lang.ClassLoader
							.getSystemResource("resources/pictures/system/icons/2025_stop24x24.png"));
	
	Image UPLOAD = Toolkit
			.getDefaultToolkit()
			.getImage(
					java.lang.ClassLoader
							.getSystemResource("resources/pictures/system/icons/2025_upload24x24.png"));

	Image SAVE = Toolkit
			.getDefaultToolkit()
			.getImage(
					java.lang.ClassLoader
							.getSystemResource("resources/pictures/system/icons/2025_save24x24.png"));

	/* Menu */

	Image SHORTCUT = Toolkit
			.getDefaultToolkit()
			.getImage(
					java.lang.ClassLoader
							.getSystemResource("resources/pictures/system/icons/2025_shortcut24x24.png"));
	
	Image DONATE = Toolkit
			.getDefaultToolkit()
			.getImage(
					java.lang.ClassLoader
							.getSystemResource("resources/pictures/system/icons/2025_donate24x24.png"));
	
	Image PROXYICO = Toolkit
			.getDefaultToolkit()
			.getImage(
					java.lang.ClassLoader
							.getSystemResource("resources/pictures/system/icons/2025_proxy24x24.png"));

	Image ACRE_SMALL = Toolkit
			.getDefaultToolkit()
			.getImage(
					java.lang.ClassLoader
							.getSystemResource("resources/pictures/system/acre16x16.png"));

	Image ACRE_BIG = Toolkit
			.getDefaultToolkit()
			.getImage(
					java.lang.ClassLoader
							.getSystemResource("resources/pictures/system/acre48x48.png"));

	Image ACRE2_SMALL = Toolkit
			.getDefaultToolkit()
			.getImage(
					java.lang.ClassLoader
							.getSystemResource("resources/pictures/system/acre216x16.png"));

	Image ACRE2_BIG = Toolkit
			.getDefaultToolkit()
			.getImage(
					java.lang.ClassLoader
							.getSystemResource("resources/pictures/system/acre248x48.png"));

	Image AIA_SMALL = Toolkit
			.getDefaultToolkit()
			.getImage(
					java.lang.ClassLoader
							.getSystemResource("resources/pictures/system/transmission16x16.png"));

	Image AIA_BIG = Toolkit
			.getDefaultToolkit()
			.getImage(
					java.lang.ClassLoader
							.getSystemResource("resources/pictures/system/allinarma.png"));

	Image TFAR_SMALL = Toolkit
			.getDefaultToolkit()
			.getImage(
					java.lang.ClassLoader
							.getSystemResource("resources/pictures/system/tfar18x18.png"));

	Image TFAR_BIG = Toolkit
			.getDefaultToolkit()
			.getImage(
					java.lang.ClassLoader
							.getSystemResource("resources/pictures/system/tfar48x48.png"));

	Image RPT_SMALL = Toolkit
			.getDefaultToolkit()
			.getImage(
					java.lang.ClassLoader
							.getSystemResource("resources/pictures/system/rpt16x16.png"));

	Image BIKEY_BIG = Toolkit
			.getDefaultToolkit()
			.getImage(
					java.lang.ClassLoader
							.getSystemResource("resources/pictures/system/bikey48x48.png"));

	Image BIKEY_SMALL = Toolkit
			.getDefaultToolkit()
			.getImage(
					java.lang.ClassLoader
							.getSystemResource("resources/pictures/system/bikey16x16.png"));

	Image HELP = Toolkit
			.getDefaultToolkit()
			.getImage(
					java.lang.ClassLoader
							.getSystemResource("resources/pictures/system/icons/2025_help24x24.png"));

	Image BIS = Toolkit
			.getDefaultToolkit()
			.getImage(
					java.lang.ClassLoader
							.getSystemResource("resources/pictures/system/bis16x16.png"));

	Image PREFERENCES = Toolkit
			.getDefaultToolkit()
			.getImage(
					java.lang.ClassLoader
							.getSystemResource("resources/pictures/system/icons/2025_preferences24x24.png"));

	Image UPDATE = Toolkit
			.getDefaultToolkit()
			.getImage(
					java.lang.ClassLoader
							.getSystemResource("resources/pictures/system/icons/2025_update24x24.png"));

	Image ABOUT = Toolkit
			.getDefaultToolkit()
			.getImage(
					java.lang.ClassLoader
							.getSystemResource("resources/pictures/system/icons/2025_about24x24.png"));

	/* Repository Tree */

	Image EXCLAMATION = Toolkit
			.getDefaultToolkit()
			.getImage(
					java.lang.ClassLoader
							.getSystemResource("resources/pictures/system/icons/2025_exclamation16x16.png"));

	Image BRICK = Toolkit
			.getDefaultToolkit()
			.getImage(
					java.lang.ClassLoader
							.getSystemResource("resources/pictures/system/icons/2025_brick16x16.png"));

	/* Repository Tab */

	Image CLOSE_GRAY = Toolkit
			.getDefaultToolkit()
			.getImage(
					java.lang.ClassLoader
							.getSystemResource("resources/pictures/system/icons/2025_close_grey12x12.png"));

	Image CLOSE_RED = Toolkit
			.getDefaultToolkit()
			.getImage(
					java.lang.ClassLoader
							.getSystemResource("resources/pictures/system/icons/2025_close_red12x12.png"));

	/* Connection lost */

	Image WARNING = Toolkit
			.getDefaultToolkit()
			.getImage(
					java.lang.ClassLoader
							.getSystemResource("resources/pictures/system/icons/2025_warning32x32.png"));
}

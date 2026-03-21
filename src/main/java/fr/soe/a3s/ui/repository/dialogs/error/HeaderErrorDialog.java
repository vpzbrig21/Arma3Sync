package fr.soe.a3s.ui.repository.dialogs.error;

import java.io.File;
import java.io.IOException;

import javax.swing.JOptionPane;

import fr.soe.a3s.dao.DataAccessConstants;
import fr.soe.a3s.service.CommonService;
import fr.soe.a3s.ui.Facade;
import net.jimmc.jshortcut.JShellLink;

public class HeaderErrorDialog implements DataAccessConstants {

	private final Facade facade;
	private final String dialogTitle;
	private final String header;
	private final String repositoryName;

	public HeaderErrorDialog(Facade facade, String dialogTitle, String header, String repositoryName) {
		this.facade = facade;
		this.dialogTitle = dialogTitle;
		this.header = header;
		this.repositoryName = repositoryName;
	}

	public void show() {

		// Dialog message
		String dialogMessage = "Repository name: " + repositoryName + "\n"
				+ "The server appears to not support HTTP partial file transfer." + "\n\n"
				+ "Do you want export the server response header into file to desktop (" + LOG_FILE_NAME + ")?"
				+ "\n\n";

		int value = JOptionPane.showConfirmDialog(facade.getMainPanel(), dialogMessage, dialogTitle, 0,
				JOptionPane.WARNING_MESSAGE);

		if (value == 0) {
			// Title
			String title = dialogTitle + " " + "finished with errors for repository name: " + repositoryName + "\n"
					+ "The server appears to not support partial file transfer.";

			// Export message
			String exportMessage = title + "\n\n" + "Server response header for range request is:" + "\n" + header;

			try {
				String osName = System.getProperty("os.name");
				CommonService commonService = new CommonService();
				if (osName.toLowerCase().contains("windows")) {
					String path = JShellLink.getDirectory("desktop") + File.separator + LOG_FILE_NAME;
					commonService.exportLogFile(exportMessage, path);
					JOptionPane.showMessageDialog(facade.getMainPanel(), "Log file has been exported to desktop",
							"ArmA3Sync", JOptionPane.INFORMATION_MESSAGE);
				} else {
					String path = System.getProperty("user.home") + File.separator + LOG_FILE_NAME;
					commonService.exportLogFile(exportMessage, path);
					JOptionPane.showMessageDialog(facade.getMainPanel(), "Log file has been exported to home directory",
							"ArmA3Sync", JOptionPane.INFORMATION_MESSAGE);
				}
			} catch (IOException e1) {
				e1.printStackTrace();
				JOptionPane.showMessageDialog(facade.getMainPanel(),
						"Failed to export log file" + "\n" + e1.getMessage(), dialogTitle,
						JOptionPane.ERROR_MESSAGE);
			}
		}
	}
}

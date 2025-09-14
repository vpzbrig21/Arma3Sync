package fr.soe.a3s.ui.repository.dialogs.error;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.JOptionPane;

import fr.soe.a3s.dao.DataAccessConstants;
import fr.soe.a3s.service.CommonService;
import fr.soe.a3s.ui.Facade;
import net.jimmc.jshortcut.JShellLink;

public class UnexpectedErrorDialog implements DataAccessConstants {

	private final Facade facade;
	private final String dialogTitle;
	private final Exception e;
	private final String repositoryName;

	public UnexpectedErrorDialog(Facade facade, String dialogTitle, Exception e, String repositoryName) {
		this.facade = facade;
		this.dialogTitle = dialogTitle;
		this.e = e;
		this.repositoryName = repositoryName;
	}

	public void show() {

		// Dialog message
		String dialogMessage = "An unexpected error has occured." + "\n\n"
				+ "Do you want export the errors log file to desktop (" + LOG_FILE_NAME + ")?" + "\n\n";

		int value = JOptionPane.showConfirmDialog(facade.getMainPanel(), dialogMessage, dialogTitle, 0,
				JOptionPane.ERROR_MESSAGE);

		if (value == 0) {
			// Title
			String title = dialogTitle + " " + "finished with errors for repository name: " + repositoryName + "\n"
					+ "An unexpected error has occured.";

			// Stacktrace as a string
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			String stacktrace = sw.toString();

			// Export message
			String exportMessage = title + "\n" + "StackTrace:" + "\n" + stacktrace;

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
						"Failed to export log file" + "\n" + e1.getMessage(), dialogTitle, JOptionPane.ERROR_MESSAGE);
			}
		}
	}
}

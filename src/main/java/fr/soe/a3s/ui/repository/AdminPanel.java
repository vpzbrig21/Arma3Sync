package fr.soe.a3s.ui.repository;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import fr.soe.a3s.constant.RepositoryStatus;
import fr.soe.a3s.controller.ObserverConnectionLost;
import fr.soe.a3s.controller.ObserverEnd;
import fr.soe.a3s.controller.ObserverError;
import fr.soe.a3s.dto.RepositoryDTO;
import fr.soe.a3s.dto.ServerInfoDTO;
import fr.soe.a3s.exception.CheckException;
import fr.soe.a3s.exception.LoadingException;
import fr.soe.a3s.exception.WritingException;
import fr.soe.a3s.exception.remote.RemoteRepositoryException;
import fr.soe.a3s.exception.repository.RepositoryException;
import fr.soe.a3s.service.RepositoryService;
import fr.soe.a3s.ui.Facade;
import fr.soe.a3s.ui.UIConstants;
import fr.soe.a3s.ui.UiColors;
import fr.soe.a3s.ui.UiStyle;
import fr.soe.a3s.ui.UiStyle.ProgressTone;
import fr.soe.a3s.ui.repository.dialogs.BuildRepositoryOptionsDialog;
import fr.soe.a3s.ui.repository.dialogs.ChangelogPanel;
import fr.soe.a3s.ui.repository.dialogs.ConnectionLostDialog;
import fr.soe.a3s.ui.repository.dialogs.connection.UploadRepositoryConnectionDialog;
import fr.soe.a3s.ui.repository.dialogs.error.ErrorsListDialog;
import fr.soe.a3s.ui.repository.dialogs.error.UnexpectedErrorDialog;
import fr.soe.a3s.ui.repository.workers.RepositoryBuilder;
import fr.soe.a3s.ui.repository.workers.RepositoryChecker;
import fr.soe.a3s.ui.repository.workers.RepositoryUploader;
import fr.soe.a3s.utils.RepositoryConsoleErrorPrinter;
import fr.soe.a3s.utils.UnitConverter;

public class AdminPanel extends JPanel implements UIConstants {

	private static final DateTimeFormatter BUILD_DATE_FORMATTER = DateTimeFormatter
			.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(Locale.getDefault());

	private final Facade facade;
	private JLabel labelRevision, labelRevisionValue;
	private JLabel labelDate, labelDateValue;
	private JLabel labelStatus, labelStatusValue;
	private JLabel labelNbFiles, labelNbFilesValue;
	private JLabel labelTotalSize, labelTotalSizeValue;
	private JLabel labelChangelog;
	private JButton buttonView;
	private final RepositoryPanel repositoryPanel;
	private JButton buttonSelectMainfolderPath, buttonBuild, buttonCopyAutoConfigURL, buttonCheck;
	private String repositoryName;
	private JTextField textFieldMainSharedFolderLocation, textFieldAutoConfigURL;
	private JProgressBar buildProgressBar, checkProgressBar;
	private JButton buttonBuildOptions;
	private JProgressBar uploadrogressBar;
	private JButton buttonUploadOptions;
	private JButton buttonUpload;
	private JLabel uploadSizeLabelValue;
	private JLabel uploadedLabelValue;
	private JLabel uploadSpeedLabelValue;
	private JLabel uploadRemainingTimeValue;
	private Box uploadInformationBox;
	private Box checkInformationBox;
	private JLabel checkErrorLabel;
	private JLabel checkErrorLabelValue;

	// Sarvices
	private final RepositoryService repositoryService = new RepositoryService();

	/* Workers */
	private RepositoryUploader repositoryUploader = null;
	private RepositoryBuilder repositoryBuilder = null;
	private RepositoryChecker repositoryChecker = null;

	public AdminPanel(Facade facade, RepositoryPanel repositoryPanel) {

		this.facade = facade;
		this.repositoryPanel = repositoryPanel;
		setLayout(new BorderLayout());

		Box vertBox1 = Box.createVerticalBox();
		vertBox1.add(Box.createVerticalStrut(5));
		this.add(vertBox1, BorderLayout.CENTER);

		JPanel centerPanel = new JPanel();
		vertBox1.add(centerPanel, BorderLayout.CENTER);
		centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));

		JPanel repositoryInfoPanel = new JPanel();
		repositoryInfoPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Infos"));
		centerPanel.add(repositoryInfoPanel, BorderLayout.NORTH);

		repositoryInfoPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
		Box vBox = Box.createVerticalBox();
		repositoryInfoPanel.add(vBox);
		{
			labelRevision = new JLabel("Revision: ");
			labelRevisionValue = new JLabel();
			Box hBox = Box.createHorizontalBox();
			hBox.add(labelRevision);
			hBox.add(labelRevisionValue);
			hBox.add(Box.createHorizontalGlue());
			vBox.add(hBox);
			vBox.add(Box.createVerticalStrut(10));
		}
		{
			labelDate = new JLabel("Build date: ");
			labelDateValue = new JLabel();
			Box hBox = Box.createHorizontalBox();
			hBox.add(labelDate);
			hBox.add(labelDateValue);
			hBox.add(Box.createHorizontalGlue());
			vBox.add(hBox);
			vBox.add(Box.createVerticalStrut(10));
		}
		{
			labelStatus = new JLabel("Status: ");
			labelStatusValue = new JLabel();
			Box hBox = Box.createHorizontalBox();
			hBox.add(labelStatus);
			hBox.add(labelStatusValue);
			hBox.add(Box.createHorizontalGlue());
			vBox.add(hBox);
			vBox.add(Box.createVerticalStrut(10));
		}
		{
			labelNbFiles = new JLabel("Number of files: ");
			labelNbFilesValue = new JLabel();
			Box hBox = Box.createHorizontalBox();
			hBox.add(labelNbFiles);
			hBox.add(labelNbFilesValue);
			hBox.add(Box.createHorizontalGlue());
			vBox.add(hBox);
			vBox.add(Box.createVerticalStrut(10));
		}
		{
			labelTotalSize = new JLabel("Total files size: ");
			labelTotalSizeValue = new JLabel();
			Box hBox = Box.createHorizontalBox();
			hBox.add(labelTotalSize);
			hBox.add(labelTotalSizeValue);
			hBox.add(Box.createHorizontalGlue());
			vBox.add(hBox);
			vBox.add(Box.createVerticalStrut(5));
		}
		{
			labelChangelog = new JLabel("Changelog: ");
			buttonView = new JButton("View");
			buttonView.setFocusable(false);
			Box hBox = Box.createHorizontalBox();
			hBox.add(labelChangelog);
			hBox.add(buttonView);
			hBox.add(Box.createHorizontalGlue());
			vBox.add(hBox);
			vBox.add(Box.createVerticalStrut(5));
		}

		JPanel repositoryAdministrationPanel = new JPanel();
		repositoryAdministrationPanel
				.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Administration"));
		centerPanel.add(repositoryAdministrationPanel, BorderLayout.CENTER);
		repositoryAdministrationPanel.setLayout(new BorderLayout());
		vBox = Box.createVerticalBox();
		repositoryAdministrationPanel.add(vBox, BorderLayout.NORTH);
		{
			JPanel locationLabelPanel = new JPanel();
			locationLabelPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
			JLabel labelFtpSharedFolderLocation = new JLabel("Repository main folder location");
			locationLabelPanel.add(labelFtpSharedFolderLocation);
			vBox.add(locationLabelPanel);
		}
		{
			JPanel locationPanel = new JPanel();
			locationPanel.setLayout(new BorderLayout());
			textFieldMainSharedFolderLocation = new JTextField();
			buttonSelectMainfolderPath = new JButton("Select");
			textFieldMainSharedFolderLocation.setEditable(false);
			UiStyle.bindBackground(textFieldMainSharedFolderLocation, UiColors::surface);
			locationPanel.add(textFieldMainSharedFolderLocation, BorderLayout.CENTER);
			locationPanel.add(buttonSelectMainfolderPath, BorderLayout.EAST);
			vBox.add(locationPanel);
		}
		vBox.add(Box.createVerticalStrut(5));
		{
			JPanel buildLabelPanel = new JPanel();
			buildLabelPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
			JLabel buildLabelLocation = new JLabel("Build or update repository");
			buildLabelPanel.add(buildLabelLocation);
			vBox.add(buildLabelPanel);
		}
		{
			JPanel buildPanel = new JPanel();
			buildPanel.setLayout(new BorderLayout());
			buildProgressBar = new JProgressBar();
			UiStyle.styleProgressBar(buildProgressBar, ProgressTone.INFO);
			buttonBuildOptions = new JButton("Options");
			buttonBuild = new JButton("Build");
			buildPanel.add(buildProgressBar, BorderLayout.CENTER);
			JPanel panel = new JPanel();
			panel.setLayout(new GridLayout(1, 2));
			panel.add(buttonBuildOptions);
			panel.add(buttonBuild);
			buildPanel.add(panel, BorderLayout.EAST);
			vBox.add(buildPanel);
		}
		vBox.add(Box.createVerticalStrut(5));
		{
			JPanel uploadLabelPanel = new JPanel();
			uploadLabelPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
			JLabel uploadLabel = new JLabel("Upload repository");
			uploadLabelPanel.add(uploadLabel);

			uploadInformationBox = Box.createHorizontalBox();
			JLabel uploadSizeLabel = new JLabel("size: ");
			uploadInformationBox.add(uploadSizeLabel);
			uploadSizeLabelValue = new JLabel();
			uploadInformationBox.add(uploadSizeLabelValue);
			JLabel uploadedLabel = new JLabel(", uploaded: ");
			uploadInformationBox.add(uploadedLabel);
			uploadedLabelValue = new JLabel();
			uploadInformationBox.add(uploadedLabelValue);
			JLabel uploadSpeedLabel = new JLabel(", speed: ");
			uploadInformationBox.add(uploadSpeedLabel);
			uploadSpeedLabelValue = new JLabel();
			uploadInformationBox.add(uploadSpeedLabelValue);
			JLabel uploadRemainingTime = new JLabel(", time: ");
			uploadInformationBox.add(uploadRemainingTime);
			uploadRemainingTimeValue = new JLabel();
			uploadInformationBox.add(uploadRemainingTimeValue);
			uploadLabelPanel.add(uploadInformationBox);
			uploadInformationBox.setVisible(false);
			vBox.add(uploadLabelPanel);
		}
		{
			JPanel uploadPanel = new JPanel();
			uploadPanel.setLayout(new BorderLayout());
			uploadrogressBar = new JProgressBar();
			UiStyle.styleProgressBar(uploadrogressBar, ProgressTone.INFO);
			buttonUploadOptions = new JButton("Options");
			buttonUpload = new JButton("Upload");
			uploadPanel.add(uploadrogressBar, BorderLayout.CENTER);
			JPanel panel = new JPanel();
			panel.setLayout(new GridLayout(1, 2));
			panel.add(buttonUploadOptions);
			panel.add(buttonUpload);
			uploadPanel.add(panel, BorderLayout.EAST);
			vBox.add(uploadPanel);
		}
		vBox.add(Box.createVerticalStrut(5));
		{
			JPanel autoConfigLabelPanel = new JPanel();
			autoConfigLabelPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
			JLabel autoConfigLabelLocation = new JLabel("Public auto-config url (Anonymous access required)");
			autoConfigLabelPanel.add(autoConfigLabelLocation);
			vBox.add(autoConfigLabelPanel);
		}
		{
			JPanel autoConfigURLPanel = new JPanel();
			autoConfigURLPanel.setLayout(new BorderLayout());
			textFieldAutoConfigURL = new JTextField();
			buttonCopyAutoConfigURL = new JButton("Copy");
			textFieldAutoConfigURL.setEditable(false);
			UiStyle.bindBackground(textFieldAutoConfigURL, UiColors::surface);
			autoConfigURLPanel.add(textFieldAutoConfigURL, BorderLayout.CENTER);
			autoConfigURLPanel.add(buttonCopyAutoConfigURL, BorderLayout.EAST);
			vBox.add(autoConfigURLPanel);
		}
		vBox.add(Box.createVerticalStrut(5));
		{
			JPanel checkLabelPanel = new JPanel();
			checkLabelPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
			vBox.add(checkLabelPanel);
			{
				Box hBox = Box.createHorizontalBox();
				checkLabelPanel.add(hBox);
				{
					JLabel buildLabelLocation = new JLabel("Check repository synchronization");
					hBox.add(buildLabelLocation);
					hBox.add(Box.createHorizontalStrut(10));
					checkInformationBox = Box.createHorizontalBox();
					hBox.add(checkInformationBox);
					{
						checkErrorLabel = new JLabel("Errors: ");
						checkInformationBox.add(checkErrorLabel);
						checkErrorLabelValue = new JLabel();
						checkInformationBox.add(checkErrorLabelValue);
						checkInformationBox.setVisible(false);
					}
				}
			}
		}
		{
			JPanel checkPanel = new JPanel();
			checkPanel.setLayout(new BorderLayout());
			checkProgressBar = new JProgressBar();
			UiStyle.styleProgressBar(checkProgressBar, ProgressTone.SUCCESS);
			buttonCheck = new JButton("Check");
			checkPanel.add(checkProgressBar, BorderLayout.CENTER);
			checkPanel.add(buttonCheck, BorderLayout.EAST);
			vBox.add(checkPanel);
		}
		vBox.add(Box.createVerticalStrut(3));

		buttonSelectMainfolderPath.setPreferredSize(buttonBuildOptions.getPreferredSize());
		buttonBuild.setPreferredSize(buttonBuildOptions.getPreferredSize());
		buttonCopyAutoConfigURL.setPreferredSize(buttonBuildOptions.getPreferredSize());
		buttonCheck.setPreferredSize(buttonBuildOptions.getPreferredSize());

		buttonSelectMainfolderPath.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				buttonSelectMainfolderPathPerformed();
			}
		});
		buttonBuild.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						buttonBuildPerformed();
					}
				});
			}
		});
		buttonBuildOptions.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				buttonBuildOptionsPerformed();
			}
		});
		buttonUpload.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						buttonUploadPerformed();
					}
				});
			}
		});
		buttonUploadOptions.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				buttonUploadOptionsPerformed();
			}
		});
		buttonCopyAutoConfigURL.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				buttonCopyAutoConfigPerformed();
			}
		});
		buttonCheck.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				buttonCheckPerformed();
			}
		});
		buttonView.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				buttonViewPerformed();
			}
		});
		setContextualHelp();
	}

	private void setContextualHelp() {
		buttonBuild.setToolTipText("Build repository");
	}

	public void init(String repositoryName) {

		this.repositoryName = repositoryName;
		resetRepositoryInfoLabels();
		try {
			RepositoryDTO repositoryDTO = repositoryService.getRepository(repositoryName);

			textFieldMainSharedFolderLocation.setText(repositoryDTO.getPath());

			if (repositoryDTO.getAutoConfigURL() != null) {
				textFieldAutoConfigURL.setText(repositoryDTO.getProtocoleDTO().getProtocolType().getPrompt()
						+ repositoryDTO.getAutoConfigURL());
			}

			updateRepositoryStatus(RepositoryStatus.INDETERMINATED);
			refreshRepositoryInfoView(true);
		} catch (RepositoryException e) {
			JOptionPane.showMessageDialog(facade.getMainPanel(), e.getMessage(), repositoryName,
					JOptionPane.ERROR_MESSAGE);
		}
	}

	private void resetRepositoryInfoLabels() {

		labelRevisionValue.setText("-");
		labelDateValue.setText("-");
		labelNbFilesValue.setText("-");
		labelTotalSizeValue.setText("-");
	}

	private void refreshRepositoryInfoView(boolean reloadMetadata) throws RepositoryException {

		ServerInfoDTO serverInfoDTO;
		if (reloadMetadata) {
			serverInfoDTO = repositoryService.reloadServerInfo(repositoryName);
		} else {
			serverInfoDTO = repositoryService.getServerInfo(repositoryName);
			if (serverInfoDTO == null) {
				serverInfoDTO = repositoryService.reloadServerInfo(repositoryName);
			}
		}
		applyServerInfoToLabels(serverInfoDTO);
	}

	private void applyServerInfoToLabels(ServerInfoDTO serverInfoDTO) {

		resetRepositoryInfoLabels();
		if (serverInfoDTO != null) {
			labelRevisionValue.setText(Integer.toString(serverInfoDTO.getRevision()));
			labelDateValue.setText(formatDate(serverInfoDTO.getBuildDate()));
			labelNbFilesValue.setText(Long.toString(serverInfoDTO.getNumberOfFiles()));
			long size = serverInfoDTO.getTotalFilesSize();
			labelTotalSizeValue.setText(UnitConverter.convertSize(size));
		}
	}

	private void handleBuildSuccess() {

		repositoryBuilder = null;
		facade.getMainPanel().setBuilding(repositoryName, false);
		startPostBuildRefreshWorker();
	}

	private void handleBuildFailure(Exception exception) {

		repositoryBuilder = null;
		facade.getMainPanel().setBuilding(repositoryName, false);
		facade.getMainPanel().recoverFromTray();
		if (exception instanceof RepositoryException | exception instanceof IOException
				| exception instanceof WritingException) {
			RepositoryConsoleErrorPrinter.printRepositoryManagedError(repositoryName, exception);
			JOptionPane.showMessageDialog(facade.getMainPanel(), exception.getMessage(), repositoryName,
					JOptionPane.ERROR_MESSAGE);
		} else {
			RepositoryConsoleErrorPrinter.printRepositoryUnexpectedError(repositoryName, exception);
			UnexpectedErrorDialog dialog = new UnexpectedErrorDialog(facade, repositoryName, exception, repositoryName);
			dialog.show();
		}
		updateRepositoryStatus(RepositoryStatus.ERROR);
		System.gc();
	}

	private void startPostBuildRefreshWorker() {

		SwingWorker<ServerInfoDTO, Void> worker = new SwingWorker<ServerInfoDTO, Void>() {
			@Override
			protected ServerInfoDTO doInBackground() throws Exception {
				repositoryService.refreshRepositoryMetadata(repositoryName);
				return repositoryService.reloadServerInfo(repositoryName);
			}

			@Override
			protected void done() {
				try {
					ServerInfoDTO serverInfoDTO = get();
					applyServerInfoToLabels(serverInfoDTO);
					updateRepositoryStatus(RepositoryStatus.UPDATED);
					getRepositoryPanel().getEventsPanel().init(repositoryName);
					facade.getMainPanel().recoverFromTray();
					JOptionPane.showMessageDialog(facade.getMainPanel(), "Build repository finished.", repositoryName,
							JOptionPane.INFORMATION_MESSAGE);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					showPostBuildRefreshError(e);
				} catch (ExecutionException e) {
					showPostBuildRefreshError(e.getCause() != null ? e.getCause() : e);
				} finally {
					System.gc();
				}
			}
		};
		worker.execute();
	}

	private void showPostBuildRefreshError(Throwable throwable) {

		String message = throwable != null && throwable.getMessage() != null ? throwable.getMessage()
				: "Failed to refresh repository metadata after build.";
		JOptionPane.showMessageDialog(facade.getMainPanel(), message, repositoryName, JOptionPane.ERROR_MESSAGE);
		updateRepositoryStatus(RepositoryStatus.ERROR);
	}

	public void updateRepositoryStatus(RepositoryStatus repositoryStatus) {

		if (repositoryStatus.equals(RepositoryStatus.UPDATED)) {
			labelStatusValue.setText(RepositoryStatus.UPDATED.getDescription());
			labelStatusValue.setFont(labelStatusValue.getFont().deriveFont(Font.BOLD));
			UiStyle.applyStatusForeground(labelStatusValue, ProgressTone.SUCCESS);
		} else if (repositoryStatus.equals(RepositoryStatus.ERROR)) {
			labelStatusValue.setText(RepositoryStatus.ERROR.getDescription());
			labelStatusValue.setFont(labelStatusValue.getFont().deriveFont(Font.BOLD));
			UiStyle.applyStatusForeground(labelStatusValue, ProgressTone.DANGER);
		} else {
			labelStatusValue.setText(RepositoryStatus.INDETERMINATED.getDescription());
			labelStatusValue.setForeground(UiColors.textSecondary());
		}
	}

	private void buttonSelectMainfolderPathPerformed() {

		RepositoryDTO repositoryDTO = null;
		try {
			repositoryDTO = repositoryService.getRepository(repositoryName);
		} catch (RepositoryException e) {
			JOptionPane.showMessageDialog(facade.getMainPanel(), e.getMessage(), repositoryName,
					JOptionPane.ERROR_MESSAGE);
			return;
		}

		assert (repositoryDTO != null);

		JFileChooser fc = new JFileChooser();
		fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		int returnVal = fc.showOpenDialog(facade.getMainPanel());
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			File file = fc.getSelectedFile();
			textFieldMainSharedFolderLocation.setText(file.getAbsolutePath());
		} else {
			textFieldMainSharedFolderLocation.setText("");
			textFieldAutoConfigURL.setText("");
		}

		// Save path to repository
		try {
			repositoryService.setRepositoryPath(repositoryName, textFieldMainSharedFolderLocation.getText());
			repositoryService.write(repositoryName);
		} catch (RepositoryException | WritingException e) {
			JOptionPane.showMessageDialog(facade.getMainPanel(), e.getMessage(), repositoryName,
					JOptionPane.ERROR_MESSAGE);
			textFieldAutoConfigURL.setText("");
		}
	}

	@Deprecated
	private void checkRepositoryPath(File file, RepositoryDTO repositoryDTO) {

		/* Check consistency between repository url and main folder path */
		String mainFolderName = file.getName();
		String url = repositoryDTO.getProtocoleDTO().getUrl();
		if (url.endsWith("/")) {// there is / at the end of url
			int lastIndex = url.lastIndexOf("/");
			if (lastIndex != -1) {
				url = url.substring(0, lastIndex);
			}
		}
		int index = url.lastIndexOf("/");
		if (index != -1) {// There is a folder after the root
			String folderName = url.substring(index + 1);
			if (!mainFolderName.equalsIgnoreCase(folderName)) {
				JOptionPane.showMessageDialog(facade.getMainPanel(),
						"The selected shared folder " + file.getName()
								+ "\n does not correspond with the repository url: \n" + url,
						repositoryName, JOptionPane.WARNING_MESSAGE);
				textFieldMainSharedFolderLocation.setText("");
				textFieldAutoConfigURL.setText("");
			} else {
				textFieldMainSharedFolderLocation.setText(file.getAbsolutePath());
			}
		} else {
			textFieldMainSharedFolderLocation.setText(file.getAbsolutePath());
		}
	}

	private void buttonBuildPerformed() {

		if (repositoryBuilder == null || !facade.getMainPanel().isBuilding(repositoryName)) {

			// Repository main folder location must be set
			String path = textFieldMainSharedFolderLocation.getText();
			if (path.isEmpty()) {
				JOptionPane.showMessageDialog(facade.getMainPanel(),
						"Please set the repository main folder location first.", repositoryName,
						JOptionPane.INFORMATION_MESSAGE);
				return;
			} else if (!new File(path).exists()) {
				JOptionPane.showMessageDialog(facade.getMainPanel(), "Repository main folder location does not exists.",
						repositoryName, JOptionPane.ERROR_MESSAGE);
				return;
			}

			// Check available disk space
			// boolean isCompressed = repositoryService
			// .isCompressed(repositoryName);
			// if (isCompressed) {
			// long diskSpace = new File(path).getFreeSpace();
			// long repositorySize = FileUtils.sizeOfDirectory(new File(path));
			// if (diskSpace < repositorySize) {
			// JOptionPane
			// .showMessageDialog(
			// facade.getMainPanel(),
			// "Not enough free space on disk to add compressed pbo files into the
			// repository."
			// + "\n"
			// + "Required free space: "
			// + UnitConverter.convertSize(repositorySize) ,
			// "Build repository",
			// JOptionPane.INFORMATION_MESSAGE);
			// return;
			// }
			// }

			facade.getMainPanel().setBuilding(repositoryName, true);
			repositoryBuilder = new RepositoryBuilder(facade, repositoryName, path, this);

			repositoryBuilder.addObserverEnd(new ObserverEnd() {
				@Override
				public void end() {
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							handleBuildSuccess();
						}
					});
				}
			});

			repositoryBuilder.addObserverError(new ObserverError() {

				@Override
				public void error(List<Exception> errors) {
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							handleBuildFailure(errors.get(0));
						}
					});
				}
			});

			repositoryBuilder.setDaemon(true);
			repositoryBuilder.start();

		} else if (repositoryBuilder != null && facade.getMainPanel().isBuilding(repositoryName)) {
			repositoryBuilder.cancel();
			repositoryBuilder = null;
			facade.getMainPanel().setBuilding(repositoryName, false);
			System.gc();
		}
	}

	private void buttonBuildOptionsPerformed() {

		// Repository main folder location must be set
		if (textFieldMainSharedFolderLocation.getText().isEmpty()) {
			JOptionPane.showMessageDialog(facade.getMainPanel(),
					"Please set the repository main folder location first.", repositoryName,
					JOptionPane.INFORMATION_MESSAGE);
			return;
		}

		BuildRepositoryOptionsDialog buildOptionsPanel = new BuildRepositoryOptionsDialog(facade, repositoryName);
		buildOptionsPanel.init();
		buildOptionsPanel.setVisible(true);
	}

	private void buttonCopyAutoConfigPerformed() {

		if (textFieldAutoConfigURL.getText().isEmpty()) {
			JOptionPane.showMessageDialog(facade.getMainPanel(), "Auto-config url is empty!", repositoryName,
					JOptionPane.INFORMATION_MESSAGE);
		} else {
			try {
				StringSelection ss = new StringSelection(textFieldAutoConfigURL.getText());
				Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, null);
				JOptionPane.showMessageDialog(facade.getMainPanel(), "Auto-config url copied to clipboard.",
						repositoryName, JOptionPane.INFORMATION_MESSAGE);
			} catch (IllegalStateException e) {
				e.printStackTrace();
				// Clipboard may not be available (Windows).
			}
		}
	}

	private void buttonUploadPerformed() {

		if (repositoryUploader == null || !facade.getMainPanel().isUploading(repositoryName)) {
			String path = textFieldMainSharedFolderLocation.getText();
			// Repository main folder location must be set
			if (path.isEmpty()) {
				JOptionPane.showMessageDialog(facade.getMainPanel(),
						"Please set the repository main folder location then build the repository.", repositoryName,
						JOptionPane.INFORMATION_MESSAGE);
				return;
			}

			facade.getMainPanel().setUploading(repositoryName, true);
			repositoryUploader = new RepositoryUploader(facade, repositoryName, this);

			repositoryUploader.addObserverEnd(new ObserverEnd() {
				@Override
				public void end() {
					facade.getMainPanel().recoverFromTray();
					String message = "Repository upload finished.";
					JOptionPane.showMessageDialog(facade.getMainPanel(), message, repositoryName,
							JOptionPane.INFORMATION_MESSAGE);
					facade.getMainPanel().setUploading(repositoryName, false);
					System.gc();
				}
			});

			repositoryUploader.addObserverError(new ObserverError() {
				@Override
				public void error(List<Exception> errors) {
					Exception ex = errors.get(0);
					facade.getMainPanel().recoverFromTray();
					if (ex instanceof CheckException) {
						RepositoryConsoleErrorPrinter.printRepositoryManagedError(repositoryName, ex);
						JOptionPane.showMessageDialog(facade.getMainPanel(), ex.getMessage(), repositoryName,
								JOptionPane.WARNING_MESSAGE);
					} else if (ex instanceof RepositoryException || ex instanceof LoadingException
							|| ex instanceof IOException) {
						RepositoryConsoleErrorPrinter.printRepositoryManagedError(repositoryName, ex);
						JOptionPane.showMessageDialog(facade.getMainPanel(), ex.getMessage(), repositoryName,
								JOptionPane.ERROR_MESSAGE);
					} else {
						RepositoryConsoleErrorPrinter.printRepositoryUnexpectedError(repositoryName, ex);
						UnexpectedErrorDialog dialog = new UnexpectedErrorDialog(facade,
								"Check repository synchronization", ex, repositoryName);
						dialog.show();
					}
					facade.getMainPanel().setUploading(repositoryName, false);
					System.gc();
				}
			});

			repositoryUploader.addObserverConnectionLost(new ObserverConnectionLost() {
				@Override
				public void lost() {
					facade.getMainPanel().recoverFromTray();
					ConnectionLostDialog dialog = new ConnectionLostDialog(facade, repositoryName, repositoryName);
					dialog.init();
					dialog.setVisible(true);
					if (!dialog.reconnect()) {
						repositoryUploader = null;
						facade.getMainPanel().setUploading(repositoryName, false);
						System.gc();
					} else {
						repositoryUploader.run();
					}
				}
			});

			repositoryUploader.setDaemon(true);
			repositoryUploader.start();

		} else if (repositoryUploader != null && facade.getMainPanel().isUploading(repositoryName)) {
			repositoryUploader.cancel();
			repositoryUploader = null;
			facade.getMainPanel().setUploading(repositoryName, false);
			System.gc();
		}
	}

	private void buttonUploadOptionsPerformed() {

		// Repository main folder location must be set
		if (textFieldMainSharedFolderLocation.getText().isEmpty()) {
			JOptionPane.showMessageDialog(facade.getMainPanel(),
					"Please set the repository main folder location first.", repositoryName,
					JOptionPane.INFORMATION_MESSAGE);
			return;
		}

		UploadRepositoryConnectionDialog uploadRepositoryOptionsPanel = new UploadRepositoryConnectionDialog(facade);
		uploadRepositoryOptionsPanel.init(repositoryName);
		uploadRepositoryOptionsPanel.setVisible(true);
	}

	private void buttonCheckPerformed() {

		if (repositoryChecker == null || !facade.getMainPanel().isChecking(repositoryName)) {
			facade.getMainPanel().setChecking(repositoryName, true);
			repositoryChecker = new RepositoryChecker(facade, repositoryName, this);

			repositoryChecker.addObserverEnd(new ObserverError() {
				@Override
				public void error(List<Exception> errors) {
					facade.getMainPanel().recoverFromTray();
					if (errors.isEmpty()) {
						String message = "Repository is synchronized.";
						JOptionPane.showMessageDialog(facade.getMainPanel(), message, repositoryName,
								JOptionPane.INFORMATION_MESSAGE);
					} else {
						ErrorsListDialog dialog = new ErrorsListDialog(facade, repositoryName,
								"Check repository finished with errors:", errors, repositoryName);
						dialog.show();
					}
					facade.getMainPanel().setChecking(repositoryName, false);

					System.gc();
				}
			});

			repositoryChecker.addObserverError(new ObserverError() {
				@Override
				public void error(List<Exception> errors) {
					facade.getMainPanel().recoverFromTray();
					Exception ex = errors.get(0);
					if (ex instanceof RepositoryException || ex instanceof RemoteRepositoryException
							|| ex instanceof IOException) {
						RepositoryConsoleErrorPrinter.printRepositoryManagedError(repositoryName, ex);
						JOptionPane.showMessageDialog(facade.getMainPanel(), ex.getMessage(), repositoryName,
								JOptionPane.ERROR_MESSAGE);
					} else {
						RepositoryConsoleErrorPrinter.printRepositoryUnexpectedError(repositoryName, ex);
						UnexpectedErrorDialog dialog = new UnexpectedErrorDialog(facade, repositoryName, ex,
								repositoryName);
						dialog.show();
					}
					facade.getMainPanel().setChecking(repositoryName, false);

					System.gc();
				}
			});

			repositoryChecker.setDaemon(true);
			repositoryChecker.start();

		} else if (repositoryChecker != null && facade.getMainPanel().isChecking(repositoryName)) {
			repositoryChecker.cancel();
			facade.getMainPanel().setChecking(repositoryName, false);
			repositoryChecker = null;
			System.gc();
		}
	}

	private void buttonViewPerformed() {

		ChangelogPanel changelogPanel = new ChangelogPanel(facade, repositoryName, this);
		changelogPanel.init();
		changelogPanel.setVisible(true);
	}

	public JProgressBar getBuildProgressBar() {
		return buildProgressBar;
	}

	public JButton getButtonBuild() {
		return buttonBuild;
	}

	public JButton getButtonCheck() {
		return buttonCheck;
	}

	public JProgressBar getCheckProgressBar() {
		return checkProgressBar;
	}

	public JButton getButtonSelectRepositoryfolderPath() {
		return buttonSelectMainfolderPath;
	}

	public JButton getButtonCopyAutoConfigURL() {
		return buttonCopyAutoConfigURL;
	}

	public JButton getButtonView() {
		return buttonView;
	}

	public RepositoryPanel getRepositoryPanel() {
		return repositoryPanel;
	}

	public JButton getButtonBuildOptions() {
		return buttonBuildOptions;
	}

	public JButton getButtonUpload() {
		return buttonUpload;
	}

	public JButton getButtonUploadOptions() {
		return buttonUploadOptions;
	}

	public JProgressBar getUploadrogressBar() {
		return uploadrogressBar;
	}

	public JLabel getUploadTotalSizeLabelValue() {
		return uploadSizeLabelValue;
	}

	public JLabel getUploadedLabelValue() {
		return uploadedLabelValue;
	}

	public JLabel getUploadSpeedLabelValue() {
		return uploadSpeedLabelValue;
	}

	public JLabel getUploadRemainingTimeValue() {
		return uploadRemainingTimeValue;
	}

	public Box getUploadInformationBox() {
		return uploadInformationBox;
	}

	public JLabel getCheckErrorLabel() {
		return checkErrorLabel;
	}

	public JLabel getCheckErrorLabelValue() {
		return checkErrorLabelValue;
	}

	public Box getCheckInformationBox() {
		return checkInformationBox;
	}

	private String formatDate(java.util.Date date) {
		if (date == null) {
			return "";
		}
		ZonedDateTime zonedDateTime = date.toInstant().atZone(ZoneId.systemDefault());
		return BUILD_DATE_FORMATTER.format(zonedDateTime);
	}
}

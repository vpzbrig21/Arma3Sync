package fr.soe.a3s.ui.repository.dialogs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Locale;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ExecutionException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.swing.SwingWorker;
import javax.swing.border.BevelBorder;

import fr.soe.a3s.dto.ChangelogDTO;
import fr.soe.a3s.dto.RepositoryDTO;
import fr.soe.a3s.domain.repository.DiscordWebhookSettings;
import fr.soe.a3s.domain.repository.WorkshopMetadataCache;
import fr.soe.a3s.exception.repository.RepositoryException;
import fr.soe.a3s.service.DiscordChangelogMessageBuilder;
import fr.soe.a3s.service.DiscordWebhookClient;
import fr.soe.a3s.utils.ChangelogEntrySorter;
import fr.soe.a3s.service.RepositoryService;
import fr.soe.a3s.ui.Facade;
import fr.soe.a3s.ui.UIConstants;
import fr.soe.a3s.ui.repository.AdminPanel;

public class ChangelogPanel extends JFrame implements UIConstants {

	private static final DateTimeFormatter CHANGELOG_DATE_FORMATTER = DateTimeFormatter
			.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(Locale.getDefault());

	private final Facade facade;
	private final AdminPanel adminPanel;
	private JButton buttonSwitch;
	private JButton buttonCopy;
	private JButton buttonOK;
	private JButton buttonDiscord;
	private JButton buttonDiscordSettings;
	private static final String LAST_BUILD = "Last build";
	private static final String TEN_LAST_BUILD = "10 last builds";
	private JTextArea textArea;
	private JScrollPane scrollpane;
	private final String repositoryName;
	private final RepositoryService repositoryService = new RepositoryService();
	private List<ChangelogDTO> changelogDTOs;

	public ChangelogPanel(Facade facade, String repositoryName,
			AdminPanel adminPanel) {

		this.facade = facade;
		this.adminPanel = adminPanel;
		this.repositoryName = repositoryName;
		this.setTitle("Changelog");
		setIconImage(ICON);

		this.setLayout(new BorderLayout());
		{
			JPanel controlPanel = new JPanel();
			FlowLayout flowLayout = new FlowLayout(FlowLayout.RIGHT);
			controlPanel.setLayout(flowLayout);
			buttonSwitch = new JButton(TEN_LAST_BUILD);
			buttonCopy = new JButton("Copy");
			buttonDiscordSettings = new JButton("Discord settings");
			buttonDiscord = new JButton("Send to Discord");
			buttonOK = new JButton("OK");
			controlPanel.add(buttonSwitch);
			controlPanel.add(buttonCopy);
			controlPanel.add(buttonDiscordSettings);
			controlPanel.add(buttonDiscord);
			controlPanel.add(buttonOK);
			this.add(controlPanel, BorderLayout.SOUTH);
		}
		{
			JPanel northPanel = new JPanel();
			this.add(northPanel, BorderLayout.NORTH);
			JPanel eastPanel = new JPanel();
			this.add(eastPanel, BorderLayout.EAST);
			JPanel westPanel = new JPanel();
			this.add(westPanel, BorderLayout.WEST);
		}
		{
			textArea = new JTextArea();
			Font fontTextField = UIManager.getFont("Label.font");
			textArea.setFont(fontTextField);
			textArea.setLineWrap(true);
			textArea.setEditable(false);
			scrollpane = new JScrollPane();
			scrollpane.setViewportView(textArea);
			scrollpane.setBorder(BorderFactory
					.createEtchedBorder(BevelBorder.LOWERED));
			this.add(scrollpane, BorderLayout.CENTER);
		}

		this.pack();
		this.setMinimumSize(new Dimension(400, 400));
		this.setLocationRelativeTo(facade.getMainPanel());

		buttonSwitch.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				buttonSwitchPerformed();
			}
		});
		buttonCopy.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				buttonCopyPerformed();
			}
		});
		buttonDiscordSettings.addActionListener(e -> editDiscordSettings());
		buttonDiscord.addActionListener(e -> sendToDiscord());
		buttonOK.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				buttonOKPerformed();
			}
		});
		// Add Listeners
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				menuExitPerformed();
			}
		});

		getRootPane().setDefaultButton(buttonOK);
	}

	public void init() {

		try {
			this.adminPanel.getButtonView().setEnabled(false);
			changelogDTOs = repositoryService.getChangelogs(repositoryName);
			if (changelogDTOs == null) {
				String message = "Changelog is not available.";
				textArea.setText(message);
			} else if (changelogDTOs.size() == 0) {
				String message = "Changelog is not available.";
				textArea.setText(message);
			} else {
				// look for the first changelogDTO with changes
				int index = changelogDTOs.size() - 1;
				int topRevision = changelogDTOs.get(index).getRevision();
				for (int i = index; i >= 0; i--) {
					ChangelogDTO changelogDTO = changelogDTOs.get(i);
					boolean changed = checkChanges(changelogDTO);
					index = i;
					if (changed) {
						break;
					}
				}
				display(changelogDTOs.get(index), topRevision);
			}
			textArea.setRows(1);
			textArea.setCaretPosition(0);
		} catch (RepositoryException e) {
			String message = "Changelog is not available.";
			textArea.setText(message);
			textArea.setRows(1);
			textArea.setCaretPosition(0);
			JOptionPane.showMessageDialog(facade.getMainPanel(),
					e.getMessage(), repositoryName, JOptionPane.ERROR_MESSAGE);
		}
	}

	private void buttonSwitchPerformed() {

		if (changelogDTOs == null || changelogDTOs.isEmpty()) {
			return;
		}
		textArea.setText("");
		if (buttonSwitch.getText().equals(LAST_BUILD)) {
			// look for the first changelogDTO with changes
			int index = changelogDTOs.size() - 1;
			int topRevision = changelogDTOs.get(index).getRevision();
			for (int i = index; i >= 0; i--) {
				ChangelogDTO changelogDTO = changelogDTOs.get(i);
				boolean changed = checkChanges(changelogDTO);
				index = i;
				if (changed) {
					break;
				}
			}
			display(changelogDTOs.get(index), topRevision);
			buttonSwitch.setText(TEN_LAST_BUILD);
		} else if (buttonSwitch.getText().equals(TEN_LAST_BUILD)) {
			buttonSwitch.setText(LAST_BUILD);
			for (int i = changelogDTOs.size() - 1; i >= 0; i--) {
				display(changelogDTOs.get(i), -1);
				textArea.append("\n\n");
			}
		}
		textArea.setRows(1);
		textArea.setCaretPosition(0);
	}

	private void buttonCopyPerformed() {

		try {
			StringSelection ss = new StringSelection(textArea.getText());
			Toolkit.getDefaultToolkit().getSystemClipboard()
					.setContents(ss, null);
			JOptionPane.showMessageDialog(facade.getMainPanel(),
					"Changelog copied to clipboard.", repositoryName,
					JOptionPane.INFORMATION_MESSAGE);
		} catch (IllegalStateException e) {
			e.printStackTrace();
			// Clipboard may not be available (Windows).
		}
	}

	private void editDiscordSettings() {
		try {
			DiscordWebhookSettings settings = repositoryService.getDiscordWebhookSettings(repositoryName);
			JTextField webhookUrl = new JTextField(settings.getWebhookUrl(), 36);
			JTextField username = new JTextField(settings.getUsername(), 36);
			JTextField avatarUrl = new JTextField(settings.getAvatarUrl(), 36);
			JTextField imageUrl = new JTextField(settings.getImageUrl(), 36);
			JTextField title = new JTextField(settings.getTitle(), 36);
			JTextField titleEmoji = new JTextField(settings.getTitleEmoji(), 6);
			JTextField addedEmoji = new JTextField(settings.getAddedEmoji(), 6);
			JTextField updatedEmoji = new JTextField(settings.getUpdatedEmoji(), 6);
			JTextField removedEmoji = new JTextField(settings.getRemovedEmoji(), 6);
			for (JTextField emojiField : new JTextField[] {titleEmoji, addedEmoji, updatedEmoji, removedEmoji}) {
				emojiField.setFont(EmojiPickerDialog.emojiFont(16));
			}
			JCheckBox emojiPerEntry = new JCheckBox("Show an emoji before every entry (otherwise headings only)",
					settings.isEmojiPerEntry());
			JCheckBox workshopNamesAndLinks = new JCheckBox("Show Workshop names as links when available",
					settings.isShowWorkshopNamesAndLinks());
			workshopNamesAndLinks.setToolTipText(
					"Uses the Workshop name and published ID from each mod's meta.cpp; otherwise shows its folder name.");
			JCheckBox mentionsEnabled = new JCheckBox("Post enabled mentions above the changelog",
					settings.isMentionsEnabled());
			JCheckBox pingEveryone = new JCheckBox("Notify @everyone", settings.isPingEveryone());
			JTextField pingRoleIds = new JTextField(settings.getPingRoleIds(), 36);
			JTextField pingUserIds = new JTextField(settings.getPingUserIds(), 36);
			pingEveryone.setToolTipText("Discord must allow the webhook to mention everyone in this channel.");
			pingRoleIds.setToolTipText("Discord role IDs separated by commas or spaces. Copy an ID with Developer Mode enabled.");
			pingUserIds.setToolTipText("Discord user IDs separated by commas or spaces. Copy an ID with Developer Mode enabled.");
			Runnable updateMentionControls = () -> {
				pingEveryone.setEnabled(mentionsEnabled.isSelected());
				pingRoleIds.setEnabled(mentionsEnabled.isSelected());
				pingUserIds.setEnabled(mentionsEnabled.isSelected());
			};
			mentionsEnabled.addActionListener(event -> updateMentionControls.run());
			updateMentionControls.run();
			JPanel fields = new JPanel(new GridLayout(0, 2, 8, 6));
			fields.add(new javax.swing.JLabel("Discord webhook URL"));
			fields.add(webhookUrl);
			fields.add(new javax.swing.JLabel("Display name"));
			fields.add(username);
			fields.add(new javax.swing.JLabel("Message title"));
			fields.add(title);
			addEmojiPickerField(fields, "Title emoji", titleEmoji);
			fields.add(new javax.swing.JLabel("Avatar image URL (optional)"));
			fields.add(avatarUrl);
			fields.add(new javax.swing.JLabel("Message image URL (optional)"));
			fields.add(imageUrl);
			addEmojiPickerField(fields, "Added emoji", addedEmoji);
			addEmojiPickerField(fields, "Updated emoji", updatedEmoji);
			addEmojiPickerField(fields, "Removed emoji", removedEmoji);
			fields.add(new javax.swing.JLabel("Entry icons"));
			fields.add(emojiPerEntry);
			fields.add(new javax.swing.JLabel("Workshop entry names"));
			fields.add(workshopNamesAndLinks);
			fields.add(new javax.swing.JLabel("Discord mentions"));
			fields.add(mentionsEnabled);
			fields.add(new javax.swing.JLabel("Everyone notification"));
			fields.add(pingEveryone);
			fields.add(new javax.swing.JLabel("Notify roles (IDs, comma-separated)"));
			fields.add(pingRoleIds);
			fields.add(new javax.swing.JLabel("Notify users (IDs, comma-separated)"));
			fields.add(pingUserIds);
			int result = JOptionPane.showConfirmDialog(this, fields,
					"Discord settings · " + repositoryName, JOptionPane.OK_CANCEL_OPTION,
					JOptionPane.PLAIN_MESSAGE);
			if (result != JOptionPane.OK_OPTION) return;
			settings.setWebhookUrl(webhookUrl.getText());
			settings.setUsername(username.getText());
			settings.setTitle(title.getText());
			settings.setTitleEmoji(titleEmoji.getText());
			settings.setAvatarUrl(avatarUrl.getText());
			settings.setImageUrl(imageUrl.getText());
			settings.setAddedEmoji(addedEmoji.getText());
			settings.setUpdatedEmoji(updatedEmoji.getText());
			settings.setRemovedEmoji(removedEmoji.getText());
			settings.setEmojiPerEntry(emojiPerEntry.isSelected());
			settings.setShowWorkshopNamesAndLinks(workshopNamesAndLinks.isSelected());
			settings.setMentionsEnabled(mentionsEnabled.isSelected());
			settings.setPingEveryone(pingEveryone.isSelected());
			settings.setPingRoleIds(pingRoleIds.getText());
			settings.setPingUserIds(pingUserIds.getText());
			DiscordChangelogMessageBuilder.validateMentionSettings(settings);
			repositoryService.saveDiscordWebhookSettings(repositoryName, settings);
			JOptionPane.showMessageDialog(this, "Discord settings saved.", repositoryName,
					JOptionPane.INFORMATION_MESSAGE);
		} catch (Exception exception) {
			JOptionPane.showMessageDialog(this, exception.getMessage(), repositoryName,
					JOptionPane.ERROR_MESSAGE);
		}
	}

	private void addEmojiPickerField(JPanel fields, String label, JTextField emojiField) {
		fields.add(new javax.swing.JLabel(label));
		JPanel fieldAndPicker = new JPanel(new BorderLayout(6, 0));
		fieldAndPicker.add(emojiField, BorderLayout.CENTER);
		JButton pickerButton = new JButton("Pick…");
		pickerButton.setFocusable(false);
		pickerButton.setToolTipText("Choose from the Unicode emoji list; custom server emojis can be typed here.");
		pickerButton.addActionListener(event -> {
			String selected = EmojiPickerDialog.chooseEmoji(this, emojiField.getText());
			if (selected != null) emojiField.setText(selected);
		});
		fieldAndPicker.add(pickerButton, BorderLayout.EAST);
		fields.add(fieldAndPicker);
	}

	private void sendToDiscord() {
		try {
			DiscordWebhookSettings settings = repositoryService.getDiscordWebhookSettings(repositoryName);
			if (settings.getWebhookUrl().isBlank()) {
				JOptionPane.showMessageDialog(this, "Enter a Discord webhook URL in Discord settings first.",
						repositoryName, JOptionPane.WARNING_MESSAGE);
				return;
			}
			List<ChangelogDTO> selectedChangelogs = getChangelogsForCurrentView();
			if (selectedChangelogs.isEmpty()) {
				JOptionPane.showMessageDialog(this, "There are no changes to send.",
						repositoryName, JOptionPane.INFORMATION_MESSAGE);
				return;
			}
			RepositoryDTO repository = repositoryService.getRepository(repositoryName);
			String repositoryUrl = getRepositoryUrl(repository);
			WorkshopMetadataCache workshopMetadataCache =
					repositoryService.getDiscordWorkshopMetadataCache(repositoryName);
			List<String> payloads = DiscordChangelogMessageBuilder.buildPayloads(repositoryName,
					repositoryUrl, repository == null ? null : repository.getPath(), workshopMetadataCache,
					selectedChangelogs, settings);
			buttonDiscord.setEnabled(false);
			new SwingWorker<Void, Void>() {
				private int sentMessages;

				@Override
				protected Void doInBackground() throws Exception {
					for (String payload : payloads) {
						DiscordWebhookClient.send(settings.getWebhookUrl(), payload);
						sentMessages++;
					}
					return null;
				}

				@Override
				protected void done() {
					buttonDiscord.setEnabled(true);
					try {
						get();
						JOptionPane.showMessageDialog(ChangelogPanel.this,
								"Changelog erfolgreich an Discord gesendet."
									+ (sentMessages > 1 ? " (" + sentMessages + " Nachrichten)" : ""),
								repositoryName, JOptionPane.INFORMATION_MESSAGE);
					} catch (InterruptedException exception) {
						Thread.currentThread().interrupt();
						showDiscordError(exception);
					} catch (ExecutionException exception) {
						showDiscordError(exception.getCause() == null ? exception : exception.getCause());
					}
				}
			}.execute();
		} catch (Exception exception) {
			JOptionPane.showMessageDialog(this, exception.getMessage(), repositoryName,
					JOptionPane.ERROR_MESSAGE);
		}
	}

	private List<ChangelogDTO> getChangelogsForCurrentView() {
		if (changelogDTOs == null || changelogDTOs.isEmpty()) return Collections.emptyList();
		List<ChangelogDTO> selected = new ArrayList<ChangelogDTO>();
		if (buttonSwitch.getText().equals(TEN_LAST_BUILD)) {
			for (int i = changelogDTOs.size() - 1; i >= 0; i--) {
				if (checkChanges(changelogDTOs.get(i))) {
					selected.add(changelogDTOs.get(i));
					break;
				}
			}
		} else {
			for (int i = changelogDTOs.size() - 1; i >= 0 && selected.size() < 10; i--) {
				selected.add(changelogDTOs.get(i));
			}
		}
		return selected;
	}

	private String getRepositoryUrl(RepositoryDTO repository) {
		if (repository == null || repository.getProtocoleDTO() == null) return "";
		if (repository.getAutoConfigURL() != null && !repository.getAutoConfigURL().isBlank()) {
			return protocolPrefix(repository) + repository.getAutoConfigURL();
		}
		String url = repository.getProtocoleDTO().getUrl();
		return url == null ? "" : protocolPrefix(repository) + url;
	}

	private String protocolPrefix(RepositoryDTO repository) {
		return repository.getProtocoleDTO().getProtocolType() == null ? ""
				: repository.getProtocoleDTO().getProtocolType().getPrompt();
	}

	private void showDiscordError(Throwable exception) {
		String message = exception == null || exception.getMessage() == null
				? "Discord webhook request failed." : exception.getMessage();
		JOptionPane.showMessageDialog(this, message, repositoryName, JOptionPane.ERROR_MESSAGE);
	}

	private boolean checkChanges(ChangelogDTO changelogDTO) {

		if (changelogDTO.getNewAddons().size() == 0
				&& changelogDTO.getUpdatedAddons().size() == 0
				&& changelogDTO.getDeletedAddons().size() == 0) {
			return false;
		} else {
			return true;
		}
	}

	private void display(ChangelogDTO changelogDTO, int topRevision) {

		if (topRevision == -1 || changelogDTO.getRevision() == topRevision) {
			textArea.append("--- Revision: " + changelogDTO.getRevision()
					+ " ---");
		} else {
			textArea.append("--- Revisions: " + changelogDTO.getRevision()
					+ " - " + topRevision + " ---");
		}
		textArea.append("\nBuild date: " + formatDate(changelogDTO.getBuildDate()));
		textArea.append("\n");
		textArea.append("\nNew: " + changelogDTO.getNewAddons().size() + "\n");
		if (changelogDTO.getNewAddons().isEmpty()) {
			textArea.append("-");
		} else {
			for (String stg : ChangelogEntrySorter.sortedCopy(changelogDTO.getNewAddons())) {
				textArea.append(stg + ";");
			}
		}
		textArea.append("\n");
		textArea.append("\nUpdated: " + changelogDTO.getUpdatedAddons().size()
				+ "\n");
		if (changelogDTO.getUpdatedAddons().isEmpty()) {
			textArea.append("-");
		} else {
			for (String stg : ChangelogEntrySorter.sortedCopy(changelogDTO.getUpdatedAddons())) {
				textArea.append(stg + ";");
			}
		}
		textArea.append("\n");
		textArea.append("\nDeleted: " + changelogDTO.getDeletedAddons().size()
				+ "\n");
		if (changelogDTO.getDeletedAddons().isEmpty()) {
			textArea.append("-");
		} else {
			for (String stg : ChangelogEntrySorter.sortedCopy(changelogDTO.getDeletedAddons())) {
				textArea.append(stg + ";");
			}
		}
	}

	private void buttonOKPerformed() {
		menuExitPerformed();
	}

	private void menuExitPerformed() {
		this.dispose();
		this.adminPanel.getButtonView().setEnabled(true);
	}

	private String formatDate(java.util.Date date) {
		if (date == null) {
			return "";
		}
		ZonedDateTime zonedDateTime = date.toInstant().atZone(ZoneId.systemDefault());
		return CHANGELOG_DATE_FORMATTER.format(zonedDateTime);
	}
}

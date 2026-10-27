package fr.soe.a3s.domain.repository;

import java.io.Serializable;

/** Per-repository settings for manually publishing changelogs to Discord. */
public class DiscordWebhookSettings implements Serializable {

	private static final long serialVersionUID = 1L;

	private String webhookUrl = "";
	private String username = "Arma3Sync Update";
	private String avatarUrl = "";
	private String imageUrl = "";
	private String title = "Repository Updates";
	private String titleEmoji = "📣";
	private String addedEmoji = "🟩";
	private String updatedEmoji = "🟨";
	private String removedEmoji = "🟥";
	private boolean emojiPerEntry;
	private boolean showWorkshopNamesAndLinks;
	private boolean mentionsEnabled;
	private boolean pingEveryone;
	private String pingRoleIds = "";
	private String pingUserIds = "";

	public DiscordWebhookSettings() {
	}

	public DiscordWebhookSettings(DiscordWebhookSettings other) {
		if (other == null) return;
		webhookUrl = other.getWebhookUrl();
		username = other.getUsername();
		avatarUrl = other.getAvatarUrl();
		imageUrl = other.getImageUrl();
		title = other.getTitle();
		titleEmoji = other.getTitleEmoji();
		addedEmoji = other.getAddedEmoji();
		updatedEmoji = other.getUpdatedEmoji();
		removedEmoji = other.getRemovedEmoji();
		emojiPerEntry = other.isEmojiPerEntry();
		showWorkshopNamesAndLinks = other.isShowWorkshopNamesAndLinks();
		mentionsEnabled = other.isMentionsEnabled();
		pingEveryone = other.isPingEveryone();
		pingRoleIds = other.getPingRoleIds();
		pingUserIds = other.getPingUserIds();
	}

	public String getWebhookUrl() { return webhookUrl == null ? "" : webhookUrl; }
	public void setWebhookUrl(String value) { webhookUrl = value == null ? "" : value.trim(); }
	public String getUsername() { return username == null || username.isBlank() ? "Arma3Sync Update" : username; }
	public void setUsername(String value) { username = value == null ? "" : value.trim(); }
	public String getAvatarUrl() { return avatarUrl == null ? "" : avatarUrl; }
	public void setAvatarUrl(String value) { avatarUrl = value == null ? "" : value.trim(); }
	public String getImageUrl() { return imageUrl == null ? "" : imageUrl; }
	public void setImageUrl(String value) { imageUrl = value == null ? "" : value.trim(); }
	public String getTitle() { return title == null || title.isBlank() ? "Repository Updates" : title; }
	public void setTitle(String value) { title = value == null ? "" : value.trim(); }
	public String getTitleEmoji() { return titleEmoji == null ? "📣" : titleEmoji; }
	public void setTitleEmoji(String value) { titleEmoji = value == null ? "" : value.trim(); }
	public String getAddedEmoji() { return addedEmoji == null ? "🟩" : addedEmoji; }
	public void setAddedEmoji(String value) { addedEmoji = value == null ? "" : value.trim(); }
	public String getUpdatedEmoji() { return updatedEmoji == null ? "🟨" : updatedEmoji; }
	public void setUpdatedEmoji(String value) { updatedEmoji = value == null ? "" : value.trim(); }
	public String getRemovedEmoji() { return removedEmoji == null ? "🟥" : removedEmoji; }
	public void setRemovedEmoji(String value) { removedEmoji = value == null ? "" : value.trim(); }
	public boolean isEmojiPerEntry() { return emojiPerEntry; }
	public void setEmojiPerEntry(boolean value) { emojiPerEntry = value; }
	public boolean isShowWorkshopNamesAndLinks() { return showWorkshopNamesAndLinks; }
	public void setShowWorkshopNamesAndLinks(boolean value) { showWorkshopNamesAndLinks = value; }
	public boolean isMentionsEnabled() { return mentionsEnabled; }
	public void setMentionsEnabled(boolean value) { mentionsEnabled = value; }
	public boolean isPingEveryone() { return pingEveryone; }
	public void setPingEveryone(boolean value) { pingEveryone = value; }
	public String getPingRoleIds() { return pingRoleIds == null ? "" : pingRoleIds; }
	public void setPingRoleIds(String value) { pingRoleIds = value == null ? "" : value.trim(); }
	public String getPingUserIds() { return pingUserIds == null ? "" : pingUserIds; }
	public void setPingUserIds(String value) { pingUserIds = value == null ? "" : value.trim(); }
}

package fr.soe.a3s.service;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import fr.soe.a3s.domain.repository.DiscordWebhookSettings;
import fr.soe.a3s.dto.ChangelogDTO;
import fr.soe.a3s.utils.ChangelogEntrySorter;

/** Builds Discord Components V2 webhook payloads from repository changelog data. */
public final class DiscordChangelogMessageBuilder {

	private static final int TEXT_DISPLAY_LIMIT = 3900;
	private static final int TEXT_DISPLAYS_PER_MESSAGE = 7;
	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter
			.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(Locale.getDefault());

	private DiscordChangelogMessageBuilder() {
	}

	public static List<String> buildPayloads(String repositoryName, String repositoryUrl,
			List<ChangelogDTO> changelogs, DiscordWebhookSettings settings) {
		if (changelogs == null || changelogs.isEmpty()) {
			throw new IllegalArgumentException("There is no changelog data to send.");
		}
		String markdown = buildMarkdown(changelogs, settings);
		List<String> textDisplays = splitMarkdown(markdown);
		List<String> roleIds = settings.isMentionsEnabled()
				? parseIds(settings.getPingRoleIds(), "role") : List.of();
		List<String> userIds = settings.isMentionsEnabled()
				? parseIds(settings.getPingUserIds(), "user") : List.of();
		if (roleIds.size() + userIds.size() > 100) {
			throw new IllegalArgumentException("Discord supports up to 100 configured role and user mentions combined.");
		}
		String mentionLine = buildMentionLine(settings, roleIds, userIds);
		String allowedMentions = buildAllowedMentions(settings, roleIds, userIds);
		List<String> payloads = new ArrayList<String>();
		int totalMessages = (textDisplays.size() + TEXT_DISPLAYS_PER_MESSAGE - 1) / TEXT_DISPLAYS_PER_MESSAGE;
		for (int messageIndex = 0; messageIndex < totalMessages; messageIndex++) {
			int start = messageIndex * TEXT_DISPLAYS_PER_MESSAGE;
			int end = Math.min(textDisplays.size(), start + TEXT_DISPLAYS_PER_MESSAGE);
			String heading = "## " + settings.getTitleEmoji() + " " + markdownEscape(settings.getTitle());
			if (totalMessages > 1) heading += " · " + (messageIndex + 1) + "/" + totalMessages;
			String repoLine = repositoryUrl == null || repositoryUrl.isBlank()
					? "" : "\n- Repo-URL: " + markdownEscape(repositoryUrl);
			StringBuilder json = new StringBuilder(6000);
			json.append("{\"username\":").append(jsonString(settings.getUsername()));
			if (!settings.getAvatarUrl().isBlank()) {
				json.append(",\"avatar_url\":").append(jsonString(settings.getAvatarUrl()));
			}
			json.append(",\"flags\":32768,\"allowed_mentions\":").append(allowedMentions)
					.append(",\"components\":[");
			boolean hasTopLevelComponent = false;
			if (messageIndex == 0 && !mentionLine.isEmpty()) {
				json.append("{\"type\":10,\"content\":").append(jsonString(mentionLine)).append('}');
				hasTopLevelComponent = true;
			}
			if (hasTopLevelComponent) json.append(',');
			json.append("{\"type\":17,\"accent_color\":32511,\"components\":[");
			boolean needsComma = false;
			if (!settings.getImageUrl().isBlank()) {
				json.append("{\"type\":12,\"items\":[{\"media\":{\"url\":")
						.append(jsonString(settings.getImageUrl())).append("}}]}");
				needsComma = true;
			}
			if (needsComma) json.append(',');
			json.append("{\"type\":10,\"content\":")
					.append(jsonString(heading + "\n- " + formatDate(changelogs.get(0).getBuildDate())
							+ "\n- Repository: " + markdownEscape(repositoryName) + repoLine))
					.append("},{\"type\":14,\"divider\":true,\"spacing\":2}");
			for (int i = start; i < end; i++) {
				json.append(",{\"type\":10,\"content\":").append(jsonString(textDisplays.get(i))).append('}');
			}
			json.append("]},{\"type\":10,\"content\":")
					.append(jsonString("-# Posted by " + settings.getUsername())).append("}]}");
			payloads.add(json.toString());
		}
		return payloads;
	}

	/** Validates role ids before repository settings are persisted. */
	public static void validateMentionSettings(DiscordWebhookSettings settings) {
		if (!settings.isMentionsEnabled()) return;
		List<String> roleIds = parseIds(settings.getPingRoleIds(), "role");
		List<String> userIds = parseIds(settings.getPingUserIds(), "user");
		if (roleIds.size() + userIds.size() > 100) {
			throw new IllegalArgumentException("Discord supports up to 100 configured role and user mentions combined.");
		}
	}

	private static String buildMentionLine(DiscordWebhookSettings settings, List<String> roleIds, List<String> userIds) {
		List<String> mentions = new ArrayList<String>();
		if (settings.isMentionsEnabled() && settings.isPingEveryone()) mentions.add("@everyone");
		for (String roleId : roleIds) mentions.add("<@&" + roleId + ">");
		for (String userId : userIds) mentions.add("<@" + userId + ">");
		return String.join(" ", mentions);
	}

	private static String buildAllowedMentions(DiscordWebhookSettings settings, List<String> roleIds,
			List<String> userIds) {
		StringBuilder json = new StringBuilder("{\"parse\":[");
		if (settings.isMentionsEnabled() && settings.isPingEveryone()) json.append("\"everyone\"");
		json.append(']');
		appendAllowedIds(json, "roles", roleIds);
		appendAllowedIds(json, "users", userIds);
		return json.append('}').toString();
	}

	private static void appendAllowedIds(StringBuilder json, String key, List<String> ids) {
		if (ids.isEmpty()) return;
		json.append(",\"").append(key).append("\":[");
		for (int i = 0; i < ids.size(); i++) {
			if (i > 0) json.append(',');
			json.append(jsonString(ids.get(i)));
		}
		json.append(']');
	}

	private static List<String> parseIds(String idsText, String type) {
		if (idsText == null || idsText.isBlank()) return List.of();
		Set<String> ids = new LinkedHashSet<String>();
		for (String id : idsText.trim().split("[,;\\s]+")) {
			if (!id.matches("[0-9]{17,20}")) {
				throw new IllegalArgumentException(
						"Discord " + type + " IDs must be numeric snowflake IDs (17-20 digits), separated by commas or spaces.");
			}
			ids.add(id);
		}
		return new ArrayList<String>(ids);
	}

	private static String buildMarkdown(List<ChangelogDTO> changelogs, DiscordWebhookSettings settings) {
		StringBuilder text = new StringBuilder();
		for (int i = 0; i < changelogs.size(); i++) {
			ChangelogDTO changelog = changelogs.get(i);
			if (i > 0) text.append("\n\n");
			text.append("### Revision ").append(changelog.getRevision())
					.append(" · ").append(formatDate(changelog.getBuildDate())).append('\n');
			appendSection(text, settings.getAddedEmoji(), "Added", changelog.getNewAddons(), settings.isEmojiPerEntry());
			appendSection(text, settings.getUpdatedEmoji(), "Updated", changelog.getUpdatedAddons(), settings.isEmojiPerEntry());
			appendSection(text, settings.getRemovedEmoji(), "Removed", changelog.getDeletedAddons(), settings.isEmojiPerEntry());
		}
		return text.toString().trim();
	}

	private static void appendSection(StringBuilder text, String emoji, String label,
			List<String> entries, boolean emojiPerEntry) {
		text.append("\n**").append(emoji).append(' ').append(label).append(" (")
				.append(entries.size()).append(")**\n");
		if (entries.isEmpty()) {
			text.append("- —\n");
			return;
		}
		for (String entry : ChangelogEntrySorter.sortedCopy(entries)) {
			text.append("- ");
			if (emojiPerEntry) text.append(emoji).append(' ');
			text.append(markdownEscape(entry)).append('\n');
		}
	}

	private static List<String> splitMarkdown(String markdown) {
		List<String> parts = new ArrayList<String>();
		StringBuilder current = new StringBuilder();
		for (String line : markdown.split("\\n", -1)) {
			String candidate = current.length() == 0 ? line : current + "\n" + line;
			if (candidate.length() > TEXT_DISPLAY_LIMIT && current.length() > 0) {
				parts.add(current.toString());
				current.setLength(0);
			}
			if (line.length() > TEXT_DISPLAY_LIMIT) {
				for (int offset = 0; offset < line.length(); offset += TEXT_DISPLAY_LIMIT) {
					parts.add(line.substring(offset, Math.min(line.length(), offset + TEXT_DISPLAY_LIMIT)));
				}
			} else {
				if (current.length() > 0) current.append('\n');
				current.append(line);
			}
		}
		if (current.length() > 0 || parts.isEmpty()) parts.add(current.toString());
		return parts;
	}

	private static String formatDate(Date date) {
		return date == null ? "Date unavailable" : DATE_FORMATTER.format(date.toInstant().atZone(ZoneId.systemDefault()));
	}

	private static String markdownEscape(String value) {
		if (value == null) return "";
		return value.replace("\\", "\\\\").replace("*", "\\*").replace("_", "\\_")
				.replace("`", "\\`").replace("~", "\\~");
	}

	private static String jsonString(String value) {
		StringBuilder escaped = new StringBuilder(value.length() + 2).append('"');
		for (int i = 0; i < value.length(); i++) {
			char character = value.charAt(i);
			switch (character) {
			case '"': escaped.append("\\\""); break;
			case '\\': escaped.append("\\\\"); break;
			case '\b': escaped.append("\\b"); break;
			case '\f': escaped.append("\\f"); break;
			case '\n': escaped.append("\\n"); break;
			case '\r': escaped.append("\\r"); break;
			case '\t': escaped.append("\\t"); break;
			default:
				if (character < 0x20) escaped.append(String.format("\\u%04x", (int) character));
				else escaped.append(character);
			}
		}
		return escaped.append('"').toString();
	}
}

package fr.soe.a3s.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.soe.a3s.constant.GameDLCs;
import fr.soe.a3s.domain.Addon;
import fr.soe.a3s.dto.TreeDirectoryDTO;
import fr.soe.a3s.dto.TreeLeafDTO;
import fr.soe.a3s.dto.TreeNodeDTO;

/**
 * Generates the static HTML preset format understood by the Arma 3 Launcher.
 *
 * <p>This exporter is intentionally read-only with regard to Arma3Sync
 * repositories and events. It only inspects the selected local modset and
 * writes a user-requested HTML file.</p>
 */
public class Arma3LauncherPresetExporter {

	private static final Pattern PUBLISHED_ID_PATTERN = Pattern.compile(
			"(?m)^\\s*publishedid\\s*=\\s*(\\d+)\\s*;");
	private static final Pattern NAME_PATTERN = Pattern.compile(
			"(?m)^\\s*name\\s*=\\s*\\\"((?:\\\\.|[^\\\"\\\\])*)\\\"\\s*;");

	/**
	 * Creates an export result without writing to disk.
	 */
	public ExportResult generate(TreeDirectoryDTO modset, Function<String, Addon> addonResolver) {
		return generate(modset, addonResolver, null);
	}

	/**
	 * Creates an export result and uses the user-local metadata store for mods
	 * whose local files do not contain complete Workshop metadata.
	 */
	public ExportResult generate(TreeDirectoryDTO modset, Function<String, Addon> addonResolver,
			WorkshopMetadataStore metadataStore) {
		ExportResult result = new ExportResult(modset == null ? "" : modset.getName());
		if (modset == null) {
			result.errors.add("No modset was selected.");
			return result;
		}

		Map<String, WorkshopEntry> workshopEntries = new LinkedHashMap<String, WorkshopEntry>();
		Map<String, DlcEntry> dlcEntries = new LinkedHashMap<String, DlcEntry>();
		collectSelectedEntries(modset, addonResolver, metadataStore, workshopEntries, dlcEntries, result);

		result.workshopEntries.addAll(workshopEntries.values());
		result.dlcEntries.addAll(dlcEntries.values());
		if (result.workshopEntries.isEmpty() && result.dlcEntries.isEmpty() && result.errors.isEmpty()) {
			result.errors.add("The selected modset contains no selected Workshop mods or CDLCs.");
		}
		if (result.errors.isEmpty() && result.metadataIssues.isEmpty()) {
			result.html = renderHtml(result);
		}
		return result;
	}

	/**
	 * Writes a previously validated result as UTF-8 HTML.
	 */
	public void write(Path target, ExportResult result) throws IOException {
		if (target == null) {
			throw new IllegalArgumentException("Target path must not be null.");
		}
		if (result == null || !result.isValid()) {
			throw new IllegalArgumentException("Only a valid export result can be written.");
		}
		Path absoluteTarget = target.toAbsolutePath().normalize();
		Path parent = absoluteTarget.getParent();
		if (parent != null) {
			Files.createDirectories(parent);
		}
		Files.writeString(absoluteTarget, result.html, StandardCharsets.UTF_8);
		if (!Files.isRegularFile(absoluteTarget) || Files.size(absoluteTarget) == 0) {
			throw new IOException("The preset file was not created or is empty: " + absoluteTarget);
		}
	}

	private void collectSelectedEntries(TreeNodeDTO node, Function<String, Addon> addonResolver,
			WorkshopMetadataStore metadataStore,
			Map<String, WorkshopEntry> workshopEntries, Map<String, DlcEntry> dlcEntries,
			ExportResult result) {
		if (node.isLeaf()) {
			TreeLeafDTO leaf = (TreeLeafDTO) node;
			if (!leaf.isSelected()) {
				return;
			}

			String symbolicName = leaf.getName();
			GameDLCs dlc = GameDLCs.fromName(symbolicName);
			if (dlc != null) {
				if (!dlcEntries.containsKey(dlc.name())) {
					dlcEntries.put(dlc.name(), new DlcEntry(dlc.getDisplayName(), dlc.getSteamStoreUrl()));
				}
				return;
			}

			if (addonResolver == null) {
				result.errors.add("No addon resolver is available for " + symbolicName + ".");
				return;
			}
			Addon addon = addonResolver.apply(symbolicName);
			if (addon == null) {
				result.errors.add("Selected addon is not available locally: " + symbolicName);
				return;
			}
			Path addonDirectory = Path.of(addon.getPath(), addon.getName());
			Path metadataFile = addonDirectory.resolve("meta.cpp");
			WorkshopMetadata metadata = new WorkshopMetadata(null, null);
			try {
				if (Files.isRegularFile(metadataFile)) {
					metadata = parseMetadata(metadataFile);
				}
			} catch (IOException e) {
				result.errors.add("Could not read " + metadataFile + ": " + e.getMessage());
				return;
			}

			String addonKey = addon.getKey() == null ? symbolicName : addon.getKey();
			Optional<WorkshopMetadataStore.Entry> known = metadataStore == null
					? Optional.empty() : metadataStore.find(addonKey);
			if (metadata.publishedId == null && known.isPresent()) {
				metadata.publishedId = known.get().getPublishedId();
			}
			if (metadata.name == null && known.isPresent()) {
				metadata.name = known.get().getName();
			}

			/* A missing display name is recoverable: the local addon folder name
			 * is a stable user-visible fallback. It must not trigger the metadata
			 * dialog by itself. */
			if (metadata.name == null || metadata.name.isBlank()) {
				metadata.name = addon.getName();
			}

			if (metadata.publishedId == null || metadata.publishedId.isBlank()) {
				String reason;
				if (!Files.isRegularFile(metadataFile)) {
					reason = "meta.cpp is missing";
				} else if (metadata.publishedId == null) {
					reason = "publishedid is missing or invalid";
				} else {
					reason = "publishedid is missing or invalid";
				}
				result.metadataIssues.add(new MetadataIssue(addonKey, addon.getName(), reason,
						metadata.publishedId, metadata.name));
				return;
			}

			try {
				Long.parseLong(metadata.publishedId);
				if (metadata.publishedId.equals("0")) {
					throw new IllegalArgumentException("publishedid must be greater than zero");
				}
				String key = metadata.publishedId;
				if (!workshopEntries.containsKey(key)) {
					workshopEntries.put(key, new WorkshopEntry(metadata.name, workshopUrl(key)));
				} else {
					result.warnings.add("Duplicate Workshop ID omitted: " + key);
				}
			} catch (IllegalArgumentException e) {
				result.errors.add("Invalid meta.cpp for " + addon.getName() + ": " + e.getMessage());
			}
			return;
		}

		TreeDirectoryDTO directory = (TreeDirectoryDTO) node;
		for (TreeNodeDTO child : directory.getList()) {
			collectSelectedEntries(child, addonResolver, metadataStore, workshopEntries, dlcEntries, result);
		}
	}

	private WorkshopMetadata parseMetadata(Path metadataFile) throws IOException {
		String content = Files.readString(metadataFile, StandardCharsets.UTF_8);
		Matcher idMatcher = PUBLISHED_ID_PATTERN.matcher(content);
		String publishedId = idMatcher.find() ? idMatcher.group(1) : null;

		Matcher nameMatcher = NAME_PATTERN.matcher(content);
		String name = nameMatcher.find() ? unescapeMetaValue(nameMatcher.group(1)) : null;
		return new WorkshopMetadata(publishedId, name);
	}

	private String renderHtml(ExportResult result) {
		StringBuilder html = new StringBuilder(4096);
		html.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
		html.append("<html>\n  <!--Created by Arma 3 Launcher: https://arma3.com-->\n  <head>\n");
		html.append("    <meta name=\"arma:Type\" content=\"preset\" />\n");
		html.append("    <meta name=\"arma:PresetName\" content=\"")
				.append(escapeHtml(result.presetName)).append("\" />\n");
		html.append("    <meta name=\"generator\" content=\"Arma3Sync - https://github.com/vpzbrig21/Arma3Sync\" />\n");
		html.append("    <title>Arma 3</title>\n");
		html.append("    <link href=\"https://fonts.googleapis.com/css?family=Roboto\" rel=\"stylesheet\" type=\"text/css\" />\n");
		html.append("    <style>\n");
		html.append("body { margin: 0; padding: 0; color: #fff; background: #000; }\n");
		html.append("body, th, td { font: 95%/1.3 Roboto, Segoe UI, Tahoma, Arial, Helvetica, sans-serif; }\n");
		html.append("td { padding: 3px 30px 3px 0; }\n");
		html.append("h1 { padding: 20px 20px 0 20px; color: white; font-weight: 200; font-family: segoe ui; font-size: 3em; margin: 0; }\n");
		html.append("em { font-variant: italic; color: silver; }\n");
		html.append(".before-list { padding: 5px 20px 10px 20px; }\n");
		html.append(".mod-list, .dlc-list { background: #222222; padding: 20px; }\n");
		html.append(".footer { padding: 20px; color: gray; }\n");
		html.append(".whups { color: gray; }\n");
		html.append("a { color: #D18F21; text-decoration: underline; }\n");
		html.append("a:hover { color: #F1AF41; text-decoration: none; }\n");
		html.append(".from-steam { color: #449EBD; }\n");
		html.append(".from-local { color: gray; }\n");
		html.append("    </style>\n  </head>\n  <body>\n");
		html.append("    <h1>Arma 3 - Preset <strong>")
				.append(escapeHtml(result.presetName)).append("</strong></h1>\n");
		html.append("    <p class=\"before-list\"><em>To import this preset, drag this file onto the Launcher window. Or click the MODS tab, then PRESET in the top right, then IMPORT at the bottom, and finally select this file.</em></p>\n");
		html.append("    <div class=\"mod-list\">\n      <table>\n");
		for (WorkshopEntry entry : result.workshopEntries) {
			html.append("        <tr data-type=\"ModContainer\">\n");
			html.append("          <td data-type=\"DisplayName\">").append(escapeHtml(entry.name)).append("</td>\n");
			html.append("          <td><span class=\"from-steam\">Steam</span></td>\n");
			html.append("          <td><a href=\"").append(escapeHtml(entry.url))
					.append("\" data-type=\"Link\">").append(escapeHtml(entry.url)).append("</a></td>\n");
			html.append("        </tr>\n");
		}
		html.append("      </table>\n    </div>\n");
		if (!result.dlcEntries.isEmpty()) {
			html.append("    <div class=\"dlc-list\">\n      <table>\n");
			for (DlcEntry entry : result.dlcEntries) {
				html.append("        <tr data-type=\"DlcContainer\">\n");
				html.append("          <td data-type=\"DisplayName\">").append(escapeHtml(entry.name)).append("</td>\n");
				html.append("          <td><a href=\"").append(escapeHtml(entry.url))
						.append("\" data-type=\"Link\">").append(escapeHtml(entry.url)).append("</a></td>\n");
				html.append("        </tr>\n");
			}
			html.append("      </table>\n    </div>\n");
		}
		html.append("    <div class=\"footer\"><span>Created by Arma 3 Launcher by Bohemia Interactive. Exported by Arma3Sync.</span></div>\n");
		html.append("  </body>\n</html>\n");
		return html.toString();
	}

	private static String workshopUrl(String publishedId) {
		return "https://steamcommunity.com/sharedfiles/filedetails/?id=" + publishedId;
	}

	private static String unescapeMetaValue(String value) {
		return value.replace("\\\\", "\\").replace("\\\"", "\"").replace("\\n", "\n");
	}

	private static String escapeHtml(String value) {
		if (value == null) {
			return "";
		}
		return value.replace("&", "&amp;")
				.replace("<", "&lt;")
				.replace(">", "&gt;")
				.replace("\"", "&quot;")
				.replace("'", "&#39;");
	}

	public static final class ExportResult {
		private final String presetName;
		private final List<WorkshopEntry> workshopEntries = new ArrayList<WorkshopEntry>();
		private final List<DlcEntry> dlcEntries = new ArrayList<DlcEntry>();
		private final List<MetadataIssue> metadataIssues = new ArrayList<MetadataIssue>();
		private final List<String> warnings = new ArrayList<String>();
		private final List<String> errors = new ArrayList<String>();
		private String html;

		private ExportResult(String presetName) {
			this.presetName = presetName == null || presetName.isBlank() ? "Arma3Sync Preset" : presetName;
		}

		public boolean isValid() {
			return errors.isEmpty() && html != null;
		}

		public String getHtml() {
			return html;
		}

		public String getPresetName() {
			return presetName;
		}

		public int getWorkshopCount() {
			return workshopEntries.size();
		}

		public int getDlcCount() {
			return dlcEntries.size();
		}

		public List<String> getWarnings() {
			return List.copyOf(warnings);
		}

		public List<String> getErrors() {
			return List.copyOf(errors);
		}

		public List<MetadataIssue> getMetadataIssues() {
			return List.copyOf(metadataIssues);
		}

		public String getSummary() {
			return String.format(Locale.ROOT, "%d Workshop mods and %d CDLCs exported.",
					getWorkshopCount(), getDlcCount());
		}
	}

	private static final class WorkshopEntry {
		private final String name;
		private final String url;

		private WorkshopEntry(String name, String url) {
			this.name = name;
			this.url = url;
		}
	}

	private static final class DlcEntry {
		private final String name;
		private final String url;

		private DlcEntry(String name, String url) {
			this.name = name;
			this.url = url;
		}
	}

	private static final class WorkshopMetadata {
		private String publishedId;
		private String name;

		private WorkshopMetadata(String publishedId, String name) {
			this.publishedId = publishedId;
			this.name = name;
		}
	}

	public static final class MetadataIssue {
		private final String addonKey;
		private final String addonName;
		private final String reason;
		private final String publishedId;
		private final String name;

		private MetadataIssue(String addonKey, String addonName, String reason,
				String publishedId, String name) {
			this.addonKey = addonKey;
			this.addonName = addonName;
			this.reason = reason;
			this.publishedId = publishedId;
			this.name = name;
		}

		public String getAddonKey() {
			return addonKey;
		}

		public String getAddonName() {
			return addonName;
		}

		public String getReason() {
			return reason;
		}

		public String getPublishedId() {
			return publishedId;
		}

		public String getName() {
			return name;
		}
	}
}

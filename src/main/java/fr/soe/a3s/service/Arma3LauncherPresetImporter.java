package fr.soe.a3s.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.soe.a3s.domain.Addon;

/**
 * Imports the Workshop mod entries from an Arma 3 Launcher preset and compares
 * them with local addon metadata. The importer is deliberately read-only: it
 * does not change modsets, repository data, or local files.
 */
public final class Arma3LauncherPresetImporter {

	private static final Pattern MOD_ROW_PATTERN = Pattern.compile(
			"(?is)<tr\\b[^>]*data-type\\s*=\\s*['\"]ModContainer['\"][^>]*>(.*?)</tr\\s*>");
	private static final Pattern DISPLAY_NAME_PATTERN = Pattern.compile(
			"(?is)<[^>]*data-type\\s*=\\s*['\"]DisplayName['\"][^>]*>(.*?)</[^>]+>");
	private static final Pattern LINK_PATTERN = Pattern.compile(
			"(?is)<a\\b[^>]*data-type\\s*=\\s*['\"]Link['\"][^>]*href\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>");
	private static final Pattern LINK_REVERSED_PATTERN = Pattern.compile(
			"(?is)<a\\b[^>]*href\\s*=\\s*['\"]([^'\"]+)['\"][^>]*data-type\\s*=\\s*['\"]Link['\"][^>]*>");
	private static final Pattern WORKSHOP_ID_PATTERN = Pattern.compile("(?:[?&]id=)([1-9][0-9]*)",
			Pattern.CASE_INSENSITIVE);
	private static final Pattern HTML_TAG_PATTERN = Pattern.compile("(?is)<[^>]+>");
	private static final Pattern HTML_ENTITY_PATTERN = Pattern.compile("&(#x?[0-9a-f]+|amp|quot|apos|lt|gt);",
			Pattern.CASE_INSENSITIVE);
	private static final Pattern PUBLISHED_ID_PATTERN = Pattern.compile(
			"(?m)^\\s*publishedid\\s*=\\s*([1-9][0-9]*)\\s*;");
	private static final Pattern META_NAME_PATTERN = Pattern.compile(
			"(?m)^\\s*name\\s*=\\s*\\\"((?:\\\\.|[^\\\"\\\\])*)\\\"\\s*;");

	/**
	 * Imports and compares one preset against the supplied local addons.
	 *
	 * @param preset the Arma 3 Launcher HTML preset
	 * @param localAddons local addon definitions, normally provided by AddonService
	 * @return an immutable comparison result
	 * @throws IOException if the preset or local metadata cannot be read
	 */
	public ImportResult importPreset(Path preset, Collection<Addon> localAddons) throws IOException {
		if (preset == null) {
			throw new IllegalArgumentException("Preset path must not be null.");
		}
		if (!Files.isRegularFile(preset)) {
			throw new IOException("Preset file does not exist or is not a file: " + preset);
		}

		String html = Files.readString(preset, StandardCharsets.UTF_8);
		return compare(html, localAddons);
	}

	/**
	 * Package-visible for focused unit tests without a temporary preset file.
	 */
	ImportResult compare(String html, Collection<Addon> localAddons) throws IOException {
		if (html == null || html.isBlank()) {
			throw new IOException("The preset file is empty.");
		}

		Map<String, LocalWorkshopMod> localById = readLocalMetadata(localAddons);
		Map<String, WorkshopMod> requestedById = new LinkedHashMap<String, WorkshopMod>();
		Matcher rowMatcher = MOD_ROW_PATTERN.matcher(html);
		while (rowMatcher.find()) {
			String row = rowMatcher.group(1);
			String link = extractLink(row);
			String id = extractWorkshopId(link);
			if (id == null) {
				continue;
			}
			String name = cleanText(extractDisplayName(row));
			if (name.isEmpty()) {
				name = "Workshop item " + id;
			}
			requestedById.putIfAbsent(id, new WorkshopMod(id, name, workshopUrl(id)));
		}

		if (requestedById.isEmpty()) {
			throw new IOException("No Arma 3 Launcher Workshop mod entries were found in the selected HTML file.");
		}

		List<WorkshopMod> matched = new ArrayList<WorkshopMod>();
		List<WorkshopMod> missing = new ArrayList<WorkshopMod>();
		for (WorkshopMod requested : requestedById.values()) {
			if (localById.containsKey(requested.publishedId)) {
				LocalWorkshopMod local = localById.get(requested.publishedId);
				matched.add(requested.withLocalAddon(local.name, local.addonKey));
			} else {
				missing.add(requested);
			}
		}

		return new ImportResult(presetName(html), requestedById.size(), matched, missing);
	}

	private Map<String, LocalWorkshopMod> readLocalMetadata(Collection<Addon> localAddons) throws IOException {
		Map<String, LocalWorkshopMod> localById = new LinkedHashMap<String, LocalWorkshopMod>();
		if (localAddons == null) {
			return localById;
		}

		Set<String> visitedPaths = new LinkedHashSet<String>();
		for (Addon addon : localAddons) {
			if (addon == null || addon.getPath() == null || addon.getName() == null) {
				continue;
			}
			Path addonDirectory = Path.of(addon.getPath(), addon.getName()).toAbsolutePath().normalize();
			if (!visitedPaths.add(addonDirectory.toString().toLowerCase(Locale.ROOT))) {
				continue;
			}
			Path metadataFile = addonDirectory.resolve("meta.cpp");
			if (!Files.isRegularFile(metadataFile)) {
				continue;
			}
			String metadata = Files.readString(metadataFile, StandardCharsets.UTF_8);
			Matcher idMatcher = PUBLISHED_ID_PATTERN.matcher(metadata);
			if (!idMatcher.find()) {
				continue;
			}
			String id = idMatcher.group(1);
			String name = addon.getName();
			Matcher nameMatcher = META_NAME_PATTERN.matcher(metadata);
			if (nameMatcher.find()) {
				String parsedName = unescapeMetaValue(nameMatcher.group(1)).trim();
				if (!parsedName.isEmpty()) {
					name = parsedName;
				}
			}
			String addonKey = addon.getKey() == null || addon.getKey().isBlank() ? addon.getName() : addon.getKey();
			localById.putIfAbsent(id, new LocalWorkshopMod(name, addonKey));
		}
		return localById;
	}

	private String extractLink(String row) {
		Matcher matcher = LINK_PATTERN.matcher(row);
		if (matcher.find()) {
			return matcher.group(1);
		}
		matcher = LINK_REVERSED_PATTERN.matcher(row);
		return matcher.find() ? matcher.group(1) : "";
	}

	private String extractDisplayName(String row) {
		Matcher matcher = DISPLAY_NAME_PATTERN.matcher(row);
		return matcher.find() ? matcher.group(1) : "";
	}

	private String extractWorkshopId(String link) {
		Matcher matcher = WORKSHOP_ID_PATTERN.matcher(link == null ? "" : link);
		return matcher.find() ? matcher.group(1) : null;
	}

	private String cleanText(String value) {
		String text = HTML_TAG_PATTERN.matcher(value == null ? "" : value).replaceAll("");
		Matcher entityMatcher = HTML_ENTITY_PATTERN.matcher(text);
		StringBuffer result = new StringBuffer();
		while (entityMatcher.find()) {
			String entity = entityMatcher.group(1).toLowerCase(Locale.ROOT);
			String replacement;
			if ("amp".equals(entity)) replacement = "&";
			else if ("quot".equals(entity)) replacement = "\"";
			else if ("apos".equals(entity)) replacement = "'";
			else if ("lt".equals(entity)) replacement = "<";
			else if ("gt".equals(entity)) replacement = ">";
			else if (entity.startsWith("#x")) replacement = Character.toString((char) Integer.parseInt(entity.substring(2), 16));
			else if (entity.startsWith("#")) replacement = Character.toString((char) Integer.parseInt(entity.substring(1)));
			else replacement = entityMatcher.group();
			entityMatcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
		}
		entityMatcher.appendTail(result);
		return result.toString().replaceAll("\\s+", " ").trim();
	}

	private String presetName(String html) {
		Pattern pattern = Pattern.compile("(?is)<meta\\b[^>]*name\\s*=\\s*['\"]arma:PresetName['\"][^>]*content\\s*=\\s*['\"]([^'\"]*)['\"]");
		Matcher matcher = pattern.matcher(html);
		return matcher.find() ? cleanText(matcher.group(1)) : "Imported Arma 3 preset";
	}

	private String unescapeMetaValue(String value) {
		return value.replace("\\\\", "\\").replace("\\\"", "\"").replace("\\n", "\n");
	}

	private String workshopUrl(String publishedId) {
		return "https://steamcommunity.com/sharedfiles/filedetails/?id=" + publishedId;
	}

	public static final class ImportResult {
		private final String presetName;
		private final int requestedCount;
		private final List<WorkshopMod> matched;
		private final List<WorkshopMod> missing;

		private ImportResult(String presetName, int requestedCount, List<WorkshopMod> matched,
				List<WorkshopMod> missing) {
			this.presetName = presetName == null || presetName.isBlank() ? "Imported Arma 3 preset" : presetName;
			this.requestedCount = requestedCount;
			this.matched = List.copyOf(matched);
			this.missing = List.copyOf(missing);
		}

		public String getPresetName() { return presetName; }
		public int getRequestedCount() { return requestedCount; }
		public List<WorkshopMod> getMatched() { return matched; }
		public List<WorkshopMod> getMissing() { return missing; }
	}

	public static final class WorkshopMod {
		private final String publishedId;
		private final String name;
		private final String url;
		private final String localName;
		private final String localAddonKey;

		private WorkshopMod(String publishedId, String name, String url) {
			this(publishedId, name, url, null, null);
		}

		private WorkshopMod(String publishedId, String name, String url, String localName, String localAddonKey) {
			this.publishedId = publishedId;
			this.name = name;
			this.url = url;
			this.localName = localName;
			this.localAddonKey = localAddonKey;
		}

		private WorkshopMod withLocalAddon(String name, String key) {
			return new WorkshopMod(publishedId, this.name, url, name, key);
		}

		public String getPublishedId() { return publishedId; }
		public String getName() { return name; }
		public String getUrl() { return url; }
		public String getLocalName() { return localName; }
		public String getLocalAddonKey() { return localAddonKey; }
	}

	private static final class LocalWorkshopMod {
		private final String name;
		private final String addonKey;

		private LocalWorkshopMod(String name, String addonKey) {
			this.name = name;
			this.addonKey = addonKey;
		}
	}
}

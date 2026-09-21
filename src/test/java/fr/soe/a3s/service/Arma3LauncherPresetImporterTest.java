package fr.soe.a3s.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import fr.soe.a3s.domain.Addon;

class Arma3LauncherPresetImporterTest {

	@Test
	void reportsMissingWorkshopModsByIdAndLink() throws Exception {
		Path addonParent = Files.createTempDirectory("a3s-import-test");
		Path localAddon = Files.createDirectory(addonParent.resolve("Local Mod"));
		Files.writeString(localAddon.resolve("meta.cpp"),
				"publishedid = 1001;\nname = \"Local Display Name\";\n", StandardCharsets.UTF_8);

		String html = preset("Test preset",
				row("Local Display Name", "https://steamcommunity.com/sharedfiles/filedetails/?id=1001"),
				row("Missing Display Name", "https://steamcommunity.com/sharedfiles/filedetails/?id=2002"));

		Arma3LauncherPresetImporter.ImportResult result = new Arma3LauncherPresetImporter().compare(html,
				List.of(new Addon("local", "Local Mod", addonParent.toString())));

		assertEquals("Test preset", result.getPresetName());
		assertEquals(2, result.getRequestedCount());
		assertEquals(1, result.getMatched().size());
		assertEquals("Local Display Name", result.getMatched().get(0).getLocalName());
		assertEquals(1, result.getMissing().size());
		assertEquals("2002", result.getMissing().get(0).getPublishedId());
		assertEquals("Missing Display Name", result.getMissing().get(0).getName());
		assertEquals("https://steamcommunity.com/sharedfiles/filedetails/?id=2002",
				result.getMissing().get(0).getUrl());
	}

	@Test
	void ignoresDlcRowsAndDuplicateWorkshopIds() throws Exception {
		String html = "<html><head></head><body>"
				+ row("Same Mod", "https://steamcommunity.com/sharedfiles/filedetails/?id=3003")
				+ row("Duplicate Mod", "https://steamcommunity.com/sharedfiles/filedetails/?id=3003")
				+ "<tr data-type=\"DlcContainer\"><td data-type=\"DisplayName\">DLC</td>"
				+ "<td><a data-type=\"Link\" href=\"https://store.steampowered.com/app/123\">Link</a></td></tr>"
				+ "</body></html>";

		Arma3LauncherPresetImporter.ImportResult result = new Arma3LauncherPresetImporter().compare(html, List.of());

		assertEquals(1, result.getRequestedCount());
		assertEquals(1, result.getMissing().size());
		assertEquals("3003", result.getMissing().get(0).getPublishedId());
	}

	@Test
	void rejectsHtmlWithoutWorkshopModEntries() {
		assertThrows(java.io.IOException.class,
				() -> new Arma3LauncherPresetImporter().compare("<html><body>No mods</body></html>", List.of()));
	}

	private String preset(String name, String... rows) {
		return "<html><head><meta name=\"arma:PresetName\" content=\"" + name
				+ "\" /></head><body>" + String.join("", rows) + "</body></html>";
	}

	private String row(String name, String url) {
		return "<tr data-type=\"ModContainer\"><td data-type=\"DisplayName\">" + name
				+ "</td><td><a href=\"" + url + "\" data-type=\"Link\">" + url
				+ "</a></td></tr>";
	}
}

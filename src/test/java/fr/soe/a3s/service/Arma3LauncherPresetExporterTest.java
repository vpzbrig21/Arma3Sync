package fr.soe.a3s.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import fr.soe.a3s.constant.GameDLCs;
import fr.soe.a3s.domain.Addon;
import fr.soe.a3s.dto.TreeDirectoryDTO;
import fr.soe.a3s.dto.TreeLeafDTO;

class Arma3LauncherPresetExporterTest {

	@Test
	void generatesWorkshopAndDlcEntriesFromSelectedModset() throws Exception {
		Path addonParent = Files.createTempDirectory("a3s-export-test");
		Path addonDirectory = Files.createDirectory(addonParent.resolve("GM Enhancement"));
		Files.writeString(addonDirectory.resolve("meta.cpp"),
				"protocol = 1;\n" +
				"publishedid = 3147580028;\n" +
				"name = \"GM Enhancement & Intern\";\n" +
				"timestamp = 5250609374435198798;\n", StandardCharsets.UTF_8);

		TreeDirectoryDTO modset = modset("Event <HS13>");
		TreeLeafDTO addonLeaf = leaf("GM Enhancement");
		addonLeaf.setSelected(true);
		modset.addTreeNode(addonLeaf);
		TreeLeafDTO dlcLeaf = leaf(GameDLCs.WS.name());
		dlcLeaf.setSelected(true);
		modset.addTreeNode(dlcLeaf);

		Map<String, Addon> addons = new HashMap<String, Addon>();
		addons.put("gm enhancement", new Addon("GM Enhancement", "GM Enhancement", addonParent.toString()));

		Arma3LauncherPresetExporter.ExportResult result = new Arma3LauncherPresetExporter().generate(modset,
				name -> addons.get(name.toLowerCase()));

		assertTrue(result.isValid());
		assertTrue(result.getHtml().contains("data-type=\"ModContainer\""));
		assertTrue(result.getHtml().contains("id=3147580028"));
		assertTrue(result.getHtml().contains("GM Enhancement &amp; Intern"));
		assertTrue(result.getHtml().contains("data-type=\"DlcContainer\""));
		assertTrue(result.getHtml().contains("store.steampowered.com/app/1681170"));
	}

	@Test
	void reportsMissingMetaCppAsMetadataIssue() throws Exception {
		Path addonParent = Files.createTempDirectory("a3s-export-missing-test");
		Files.createDirectory(addonParent.resolve("Missing Metadata"));
		Path validDirectory = Files.createDirectory(addonParent.resolve("Valid Mod"));
		Files.writeString(validDirectory.resolve("meta.cpp"),
				"publishedid = 3147580028;\nname = \"Valid Mod\";\n", StandardCharsets.UTF_8);

		TreeDirectoryDTO modset = modset("Missing metadata");
		TreeLeafDTO addonLeaf = leaf("Missing Metadata");
		addonLeaf.setSelected(true);
		modset.addTreeNode(addonLeaf);
		TreeLeafDTO validLeaf = leaf("Valid Mod");
		validLeaf.setSelected(true);
		modset.addTreeNode(validLeaf);

		Map<String, Addon> addons = new HashMap<String, Addon>();
		addons.put("missing metadata", new Addon("Missing Metadata", "Missing Metadata", addonParent.toString()));
		addons.put("valid mod", new Addon("Valid Mod", "Valid Mod", addonParent.toString()));

		Arma3LauncherPresetExporter.ExportResult result = new Arma3LauncherPresetExporter().generate(modset,
				name -> addons.get(name.toLowerCase()));

		assertFalse(result.isValid());
		assertTrue(result.getMetadataIssues().stream().anyMatch(issue -> issue.getReason().contains("meta.cpp is missing")));
	}

	@Test
	void usesKnownMetadataWhenMetaCppHasNoName() throws Exception {
		Path addonParent = Files.createTempDirectory("a3s-export-fallback-test");
		Path addonDirectory = Files.createDirectory(addonParent.resolve("Folder Name Fallback"));
		Files.writeString(addonDirectory.resolve("meta.cpp"),
				"protocol = 1;\n" +
				"publishedid = 3147580028;\n" +
				"timestamp = 5250609374435198798;\n", StandardCharsets.UTF_8);

		TreeDirectoryDTO modset = modset("Fallback");
		TreeLeafDTO addonLeaf = leaf("Folder Name Fallback");
		addonLeaf.setSelected(true);
		modset.addTreeNode(addonLeaf);

		Map<String, Addon> addons = new HashMap<String, Addon>();
		addons.put("folder name fallback",
				new Addon("Folder Name Fallback", "Folder Name Fallback", addonParent.toString()));

		WorkshopMetadataStore store = new WorkshopMetadataStore(addonParent.resolve("known-workshop-mods.properties"));
		store.save("Folder Name Fallback", "3147580028", "Known Display Name");
		Arma3LauncherPresetExporter.ExportResult result = new Arma3LauncherPresetExporter().generate(modset,
				name -> addons.get(name.toLowerCase()), store);

		assertTrue(result.isValid());
		assertTrue(result.getHtml().contains("Known Display Name"));
	}

	@Test
	void usesAddonFolderNameWhenMetaCppHasNoNameAndStoreIsMissing() throws Exception {
		Path addonParent = Files.createTempDirectory("a3s-export-no-store-test");
		Path addonDirectory = Files.createDirectory(addonParent.resolve("ACRE2"));
		Files.writeString(addonDirectory.resolve("meta.cpp"),
				"protocol = 1;\n" +
				"publishedid = 751965892;\n", StandardCharsets.UTF_8);

		TreeDirectoryDTO modset = modset("No metadata database");
		TreeLeafDTO addonLeaf = leaf("ACRE2");
		addonLeaf.setSelected(true);
		modset.addTreeNode(addonLeaf);

		Map<String, Addon> addons = new HashMap<String, Addon>();
		addons.put("acre2", new Addon("ACRE2", "ACRE2", addonParent.toString()));

		Arma3LauncherPresetExporter.ExportResult result = new Arma3LauncherPresetExporter().generate(modset,
				name -> addons.get(name.toLowerCase()),
				new WorkshopMetadataStore(addonParent.resolve("missing-known-workshop-mods.properties")));

		assertTrue(result.isValid());
		assertTrue(result.getMetadataIssues().isEmpty());
		assertTrue(result.getHtml().contains("ACRE2"));
	}

	@Test
	void treatsZeroPublishedIdAsMissingMetadata() throws Exception {
		Path addonParent = Files.createTempDirectory("a3s-export-zero-id-test");
		Path addonDirectory = Files.createDirectory(addonParent.resolve("Zero ID Mod"));
		Files.writeString(addonDirectory.resolve("meta.cpp"),
				"protocol = 1;\n" +
				"publishedid = 0;\n" +
				"name = \"Zero ID Mod\";\n", StandardCharsets.UTF_8);

		TreeDirectoryDTO modset = modset("Zero ID preset");
		TreeLeafDTO addonLeaf = leaf("Zero ID Mod");
		addonLeaf.setSelected(true);
		modset.addTreeNode(addonLeaf);

		Map<String, Addon> addons = new HashMap<String, Addon>();
		addons.put("zero id mod", new Addon("Zero ID Mod", "Zero ID Mod", addonParent.toString()));

		Arma3LauncherPresetExporter.ExportResult result = new Arma3LauncherPresetExporter().generate(modset,
				name -> addons.get(name.toLowerCase()));

		assertFalse(result.isValid());
		assertTrue(result.getMetadataIssues().stream()
				.anyMatch(issue -> issue.getReason().contains("publishedid is missing or invalid")));
	}

	@Test
	void writesTheGeneratedPresetToDisk() throws Exception {
		Path addonParent = Files.createTempDirectory("a3s-export-write-test");
		Path addonDirectory = Files.createDirectory(addonParent.resolve("Writable Mod"));
		Files.writeString(addonDirectory.resolve("meta.cpp"),
				"publishedid = 3147580028;\nname = \"Writable Mod\";\n", StandardCharsets.UTF_8);

		TreeDirectoryDTO modset = modset("Writable preset");
		TreeLeafDTO addonLeaf = leaf("Writable Mod");
		addonLeaf.setSelected(true);
		modset.addTreeNode(addonLeaf);

		Map<String, Addon> addons = new HashMap<String, Addon>();
		addons.put("writable mod", new Addon("Writable Mod", "Writable Mod", addonParent.toString()));

		Arma3LauncherPresetExporter exporter = new Arma3LauncherPresetExporter();
		Arma3LauncherPresetExporter.ExportResult result = exporter.generate(modset,
				name -> addons.get(name.toLowerCase()));
		Path target = addonParent.resolve("nested").resolve("preset.html");

		exporter.write(target, result);

		assertTrue(Files.isRegularFile(target));
		assertTrue(Files.size(target) > 0);
		assertTrue(Files.readString(target, StandardCharsets.UTF_8).contains("Writable Mod"));
	}

	@Test
	void replacesAnExistingPresetOnDisk() throws Exception {
		Path addonParent = Files.createTempDirectory("a3s-export-overwrite-test");
		Path addonDirectory = Files.createDirectory(addonParent.resolve("Overwrite Mod"));
		Files.writeString(addonDirectory.resolve("meta.cpp"),
				"publishedid = 3147580028;\nname = \"Overwrite Mod\";\n", StandardCharsets.UTF_8);

		TreeDirectoryDTO modset = modset("Overwrite preset");
		TreeLeafDTO addonLeaf = leaf("Overwrite Mod");
		addonLeaf.setSelected(true);
		modset.addTreeNode(addonLeaf);

		Map<String, Addon> addons = new HashMap<String, Addon>();
		addons.put("overwrite mod", new Addon("Overwrite Mod", "Overwrite Mod", addonParent.toString()));

		Arma3LauncherPresetExporter exporter = new Arma3LauncherPresetExporter();
		Arma3LauncherPresetExporter.ExportResult result = exporter.generate(modset,
				name -> addons.get(name.toLowerCase()));
		Path target = addonParent.resolve("preset.html");
		Files.writeString(target, "old content", StandardCharsets.UTF_8);

		exporter.write(target, result);

		assertTrue(Files.isRegularFile(target));
		assertTrue(Files.readString(target, StandardCharsets.UTF_8).contains("Overwrite Mod"));
		assertTrue(!Files.readString(target, StandardCharsets.UTF_8).contains("old content"));
	}

	private TreeDirectoryDTO modset(String name) {
		TreeDirectoryDTO modset = new TreeDirectoryDTO();
		modset.setName(name);
		return modset;
	}

	private TreeLeafDTO leaf(String name) {
		TreeLeafDTO leaf = new TreeLeafDTO();
		leaf.setName(name);
		return leaf;
	}
}

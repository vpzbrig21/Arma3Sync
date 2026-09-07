package fr.soe.a3sUpdater.dao;

import fr.soe.a3sUpdater.model.UpdateManifest;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class XmlDAO implements DataAccessConstants {
    public String getVersion() throws Exception {
        UpdateManifest manifest = readLocal();
        return manifest == null ? null : manifest.version();
    }

    public String getZipFileName() throws Exception {
        UpdateManifest manifest = readLocal();
        return manifest == null ? null : manifest.fileName();
    }

    public UpdateManifest readLocal() throws Exception {
        Path metadata = DataAccessConstants.installationPath().resolve("a3s.xml");
        if (!Files.isRegularFile(metadata)) {
            return null;
        }
        try (InputStream input = Files.newInputStream(metadata)) {
            return read(input, metadata.toUri());
        }
    }

    public UpdateManifest read(InputStream input, URI source) throws Exception {
        String version = null;
        String fileName = null;

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

        Document document = factory.newDocumentBuilder().parse(input);
        version = readValue(document, "nom");
        fileName = readValue(document, "file");
        if (version == null) return null;
        if (fileName == null) fileName = "Arma3Sync-" + version + ".zip";
        if (!isSafeZipName(fileName)) throw new IOException("Invalid legacy XML ZIP filename: " + fileName);
        return new UpdateManifest(version, fileName, source.resolve(fileName), null, 0L,
                UpdateManifest.Format.XML, source.toString());
    }

    private static String readValue(Document document, String elementName) {
        NodeList nodes = document.getElementsByTagName(elementName);
        if (nodes.getLength() == 0) return null;
        String value = nodes.item(0).getTextContent();
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static boolean isSafeZipName(String name) {
        return !name.isBlank() && !name.contains("/") && !name.contains("\\")
                && !name.equals(".") && !name.equals("..") && name.toLowerCase().endsWith(".zip");
    }
}

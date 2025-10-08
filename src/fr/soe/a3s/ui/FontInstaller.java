package fr.soe.a3s.ui;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.swing.UIDefaults;
import javax.swing.UIManager;

/**
 * Utility that registers the bundled Inter font family and applies it to the Swing UI defaults.
 */
public final class FontInstaller {

    private static final Logger LOGGER = Logger.getLogger(FontInstaller.class.getName());
    private static final String FONT_RESOURCE_BASE = "/fr/soe/a3s/ui/fonts/";
    private static final String FONT_RESOURCE_RELATIVE_PREFIX = "fonts/";
    private static final int MIN_FONT_SIZE_BYTES = 10 * 1024;
    private static final byte[] SFNT_SIGNATURE_TRUETYPE = new byte[] { 0x00, 0x01, 0x00, 0x00 };
    private static final byte[] SFNT_SIGNATURE_OTTO = new byte[] { 0x4F, 0x54, 0x54, 0x4F };

    private static final String KEY_REGULAR = "regular";
    private static final String KEY_ITALIC = "italic";
    private static final String KEY_MEDIUM = "medium";
    private static final String KEY_SEMIBOLD = "semibold";
    private static final String KEY_BOLD = "bold";
    private static final String KEY_BOLD_ITALIC = "boldItalic";

    private static final FontResource[] INTER_RESOURCES = new FontResource[] {
            new FontResource(KEY_REGULAR, "Inter-Regular.ttf"),
            new FontResource(KEY_ITALIC, "Inter-Italic.ttf"),
            new FontResource(KEY_MEDIUM, "Inter-Medium.ttf"),
            new FontResource(KEY_SEMIBOLD, "Inter-SemiBold.ttf"),
            new FontResource(KEY_BOLD, "Inter-Bold.ttf"),
            new FontResource(KEY_BOLD_ITALIC, "Inter-BoldItalic.ttf") };

    private static final String[] FALLBACK_FONTS = new String[] { "Segoe UI", "SansSerif" };

    private static final String[] COMMON_FONT_KEYS = new String[] { "defaultFont", "Button.font", "Label.font",
            "Menu.font", "MenuItem.font", "CheckBox.font", "RadioButton.font", "TabbedPane.font", "TextField.font",
            "PasswordField.font", "TextArea.font", "TextPane.font", "EditorPane.font", "FormattedTextField.font",
            "ComboBox.font", "List.font", "Table.font", "TableHeader.font", "Tree.font", "ToolTip.font",
            "TitledBorder.font", "Spinner.font", "OptionPane.messageFont", "OptionPane.buttonFont", "ToggleButton.font",
            "Panel.font" };

    private static final Map<String, Font> REGISTERED_FONTS = new HashMap<String, Font>();
    private static final Set<String> REGISTERED_FAMILIES = new LinkedHashSet<String>();

    private static boolean fontsInstalled;
    private static String effectiveFontFamily;
    private static final boolean DEBUG_RESOURCE_LISTING = Boolean.getBoolean("a3s.font.debug");
    private static final boolean DEBUG_VERBOSE = DEBUG_RESOURCE_LISTING;

    private FontInstaller() {
    }

    /**
     * Registers the bundled Inter font faces. Returns the best available font family name to be used for UI defaults.
     *
     * @return the effective font family name, never {@code null}
     */
    public static synchronized String installInterFonts() {
        if (!fontsInstalled) {
            verifyBundledFontPresence();
            debugListBundledFonts();
            GraphicsEnvironment graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();
            for (FontResource resource : INTER_RESOURCES) {
                Font font = loadFont(resource.fileName);
                if (font != null) {
                    boolean registered = graphicsEnvironment.registerFont(font);
                    if (registered) {
                        REGISTERED_FONTS.put(resource.key, font);
                        REGISTERED_FAMILIES.add(font.getFamily(Locale.getDefault()));
                    } else {
                        logFontWarning(resource.fileName,
                                "Font was not accepted by the graphics environment during registration");
                    }
                }
            }
            if (!REGISTERED_FAMILIES.isEmpty() && LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Registered Inter font families: {0}", REGISTERED_FAMILIES);
            }
            fontsInstalled = true;
        }

        if (effectiveFontFamily == null) {
            if (!REGISTERED_FONTS.isEmpty()) {
                Font reference = REGISTERED_FONTS.get(KEY_REGULAR);
                if (reference == null && !REGISTERED_FONTS.isEmpty()) {
                    reference = REGISTERED_FONTS.values().iterator().next();
                }
                if (reference != null) {
                    effectiveFontFamily = reference.getFamily(Locale.getDefault());
                }
                if (effectiveFontFamily == null || effectiveFontFamily.trim().isEmpty()) {
                    effectiveFontFamily = "Inter";
                }
            } else {
                GraphicsEnvironment graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();
                String[] availableFamilies = graphicsEnvironment.getAvailableFontFamilyNames(Locale.getDefault());
                effectiveFontFamily = findFirstAvailable(availableFamilies, FALLBACK_FONTS);
                if (effectiveFontFamily == null) {
                    effectiveFontFamily = Font.SANS_SERIF;
                }
                LOGGER.log(Level.INFO,
                        "Inter fonts are unavailable – falling back to UI font family: {0}", effectiveFontFamily);
            }
        }

        return effectiveFontFamily;
    }

    /**
     * Applies the Inter font family (or a fallback) to the most common Swing UI defaults. This should be invoked after the
     * look & feel has been initialised and before components are created.
     */
    public static void applyInterUIFont() {
        String fontFamily = installInterFonts();

        UIDefaults defaults = UIManager.getLookAndFeelDefaults();
        Font baseFont = defaults.getFont("defaultFont");
        if (baseFont == null) {
            baseFont = UIManager.getFont("Label.font");
        }
        if (baseFont == null) {
            baseFont = new javax.swing.JLabel().getFont();
        }

        float baseSize = baseFont.getSize2D();
        if (baseSize <= 0f) {
            baseSize = 12f;
        }

        Font regular = createFontForSize(KEY_REGULAR, fontFamily, baseFont.getStyle(), baseSize);
        Font italic = createFontForSize(KEY_ITALIC, fontFamily, Font.ITALIC, baseSize);
        Font semiBold = createFontForSize(KEY_SEMIBOLD, fontFamily, Font.BOLD, baseSize);
        Font boldItalic = createFontForSize(KEY_BOLD_ITALIC, fontFamily, Font.BOLD | Font.ITALIC, baseSize);

        setUIDefaultFont(defaults, "defaultFont", regular);

        for (String key : COMMON_FONT_KEYS) {
            Font existing = defaults.getFont(key);
            Font replacement = selectReplacementFont(existing, regular, italic, semiBold, boldItalic, fontFamily, baseSize);
            setUIDefaultFont(defaults, key, replacement);
        }
    }

    private static Font loadFont(String fileName) {
        String absolutePath = FONT_RESOURCE_BASE + fileName;
        FontData fontData = loadFontDataFromClasspath(fileName, absolutePath);
        if (fontData == null) {
            String relativePath = FONT_RESOURCE_RELATIVE_PREFIX + fileName;
            fontData = loadFontDataFromClasspath(fileName, relativePath);
            if (fontData == null) {
                String absoluteStatus = describeResourceUrl(absolutePath);
                String relativeStatus = describeResourceUrl(relativePath);
                logMissingFontResource(fileName, absolutePath, absoluteStatus, relativePath, relativeStatus);
            }
        }

        Font font = createFontFromData(fileName, fontData);
        if (font != null) {
            return font;
        }

        Font fallbackFont = loadFromExternalLocations(fileName);
        if (fallbackFont != null) {
            return fallbackFont;
        }

        return null;
    }

    private static Font loadFromExternalLocations(String fileName) {
        Path jarDirectory = resolveJarDirectory();
        List<Path> searchDirectories = new ArrayList<Path>();
        if (jarDirectory != null) {
            searchDirectories.add(jarDirectory.resolve("fonts"));
        }
        Path workingDirectory = resolveWorkingDirectory();
        if (workingDirectory != null) {
            searchDirectories.add(workingDirectory.resolve("fonts"));
        }

        for (Path directory : searchDirectories) {
            Font font = loadFontFromDirectory(directory, fileName);
            if (font != null) {
                return font;
            }
        }
        return null;
    }

    private static Font loadFontFromDirectory(Path directory, String fileName) {
        if (directory == null || !Files.isDirectory(directory)) {
            return null;
        }
        Path candidate = resolveFontCandidate(directory, fileName);
        if (candidate == null) {
            return null;
        }

        FontData data = loadFontDataFromFile(candidate, fileName);
        Font font = createFontFromData(fileName, data);
        if (font != null) {
            LOGGER.log(Level.INFO, "Loaded font {0} from external file {1}", new Object[] { fileName, candidate });
            return font;
        }
        return null;
    }

    private static Path resolveFontCandidate(Path directory, String fileName) {
        Path exact = directory.resolve(fileName);
        if (Files.isRegularFile(exact)) {
            return exact;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, "Inter-*.ttf")) {
            for (Path path : stream) {
                String candidateName = path.getFileName().toString();
                if (candidateName.equalsIgnoreCase(fileName)) {
                    return path;
                }
            }
        } catch (IOException ex) {
            LOGGER.log(Level.FINE, "Unable to enumerate external fonts in " + directory, ex);
        }
        return null;
    }

    private static FontData loadFontDataFromClasspath(String fileName, String resourcePath) {
        if (resourcePath == null) {
            return null;
        }

        URL resourceUrl = FontInstaller.class.getResource(resourcePath);
        if (resourceUrl == null) {
            return null;
        }

        try (InputStream stream = FontInstaller.class.getResourceAsStream(resourcePath)) {
            if (stream == null) {
                return null;
            }
            byte[] data = readAllBytes(stream);
            String description = resourceUrl.toString();
            FontData fontData = new FontData(data, description, FontOrigin.CLASSPATH);
            debugFontData(fileName, fontData);
            return fontData;
        } catch (IOException ex) {
            logFontWarning(fileName, "I/O error while reading font at " + resourcePath + ": " + ex.getMessage());
            LOGGER.log(Level.FINE, "I/O error while reading font resource " + resourcePath, ex);
            return null;
        }
    }

    private static FontData loadFontDataFromFile(Path file, String requestedName) {
        if (file == null || !Files.isRegularFile(file)) {
            return null;
        }
        try (InputStream stream = Files.newInputStream(file)) {
            byte[] data = readAllBytes(stream);
            FontData fontData = new FontData(data, file.toAbsolutePath().toString(), FontOrigin.EXTERNAL);
            debugFontData(requestedName != null ? requestedName : file.getFileName().toString(), fontData);
            return fontData;
        } catch (IOException ex) {
            String name = requestedName != null ? requestedName : file.getFileName().toString();
            logFontWarning(name, "I/O error while reading external font at " + file + ": " + ex.getMessage());
            LOGGER.log(Level.FINE, "I/O error while reading external font " + file, ex);
            return null;
        }
    }

    private static Font createFontFromData(String fileName, FontData data) {
        if (data == null || data.bytes == null) {
            return null;
        }
        if (data.bytes.length < MIN_FONT_SIZE_BYTES) {
            logFontWarning(fileName, "Font resource too small (" + data.bytes.length + " bytes) at " + data.description);
            return null;
        }
        int headerOffset = findSfntHeaderOffset(data.bytes);
        if (headerOffset < 0) {
            logFontWarning(fileName,
                    "Unsupported font signature " + signatureString(data.bytes) + " at " + data.description + " (size="
                            + data.bytes.length + " bytes)");
            return null;
        }

        try (ByteArrayInputStream byteStream = new ByteArrayInputStream(data.bytes, headerOffset,
                data.bytes.length - headerOffset)) {
            Font createdFont = Font.createFont(Font.TRUETYPE_FONT, byteStream);
            LOGGER.log(Level.FINE, "Loaded font {0} from {1}", new Object[] { fileName, data.description });
            return createdFont;
        } catch (FontFormatException ex) {
            logFontWarning(fileName,
                    "Invalid TrueType font data at " + data.description + ": " + ex.getMessage() + " (size="
                            + data.bytes.length + " bytes)");
            LOGGER.log(Level.FINE, "Invalid font format in resource " + data.description, ex);
        } catch (IOException ex) {
            logFontWarning(fileName, "I/O error while processing font at " + data.description + ": " + ex.getMessage());
            LOGGER.log(Level.FINE, "I/O error while processing font resource " + data.description, ex);
        }
        return null;
    }

    private static void debugFontData(String fileName, FontData data) {
        if (!DEBUG_VERBOSE || data == null || data.bytes == null) {
            return;
        }
        LOGGER.log(Level.INFO, "Font debug: {0} from {1} ({2}) size={3} bytes head={4}",
                new Object[] { fileName, data.description, data.origin, data.bytes.length, signatureString(data.bytes) });
    }

    private static Path resolveJarDirectory() {
        try {
            CodeSource codeSource = FontInstaller.class.getProtectionDomain().getCodeSource();
            if (codeSource == null) {
                return null;
            }
            URL location = codeSource.getLocation();
            if (location == null) {
                return null;
            }
            Path path = Paths.get(location.toURI());
            if (Files.isRegularFile(path)) {
                return path.getParent();
            }
            if (Files.isDirectory(path)) {
                return path;
            }
        } catch (Exception ex) {
            LOGGER.log(Level.FINE, "Unable to resolve jar directory", ex);
        }
        return null;
    }

    private static Path resolveWorkingDirectory() {
        try {
            String userDir = System.getProperty("user.dir");
            if (userDir == null) {
                return null;
            }
            return Paths.get(userDir).toAbsolutePath();
        } catch (Exception ex) {
            LOGGER.log(Level.FINE, "Unable to resolve working directory", ex);
            return null;
        }
    }

    private static String describeResourceUrl(String resourcePath) {
        URL resourceUrl = FontInstaller.class.getResource(resourcePath);
        return resourceUrl != null ? resourceUrl.toString() : "null";
    }

    private static byte[] readAllBytes(InputStream stream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[8192];
        int nRead;
        while ((nRead = stream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        buffer.flush();
        return buffer.toByteArray();
    }

    private static int findSfntHeaderOffset(byte[] data) {
        if (data == null) {
            return -1;
        }
        int index = 0;
        while (index < data.length) {
            int current = data[index] & 0xFF;
            if (current == 0xEF) {
                if (index + 2 < data.length && (data[index + 1] & 0xFF) == 0xBB && (data[index + 2] & 0xFF) == 0xBF) {
                    index += 3;
                    continue;
                }
            }
            if (current == 0x0D || current == 0x0A || current == 0x09 || current == 0x20) {
                index++;
                continue;
            }
            break;
        }

        if (index + 4 > data.length) {
            return -1;
        }

        if (matchesSignature(data, index, SFNT_SIGNATURE_TRUETYPE) || matchesSignature(data, index, SFNT_SIGNATURE_OTTO)) {
            return index;
        }
        return -1;
    }

    private static boolean matchesSignature(byte[] data, int offset, byte[] expected) {
        if (data == null || expected == null) {
            return false;
        }
        if (offset < 0 || offset + expected.length > data.length) {
            return false;
        }
        for (int i = 0; i < expected.length; i++) {
            if (data[offset + i] != expected[i]) {
                return false;
            }
        }
        return true;
    }

    private static String signatureString(byte[] signature) {
        if (signature == null || signature.length == 0) {
            return "(empty)";
        }
        int limit = Math.min(signature.length, 16);
        StringBuilder builder = new StringBuilder(2 + (limit * 3));
        builder.append("0x");
        for (int i = 0; i < limit; i++) {
            if (i > 0) {
                builder.append(' ');
            }
            builder.append(String.format("%02X", signature[i] & 0xFF));
        }
        return builder.toString();
    }

    private static void logFontWarning(String fileName, String message) {
        LOGGER.log(Level.WARNING, "Skipping font {0}: {1}", new Object[] { fileName, message });
    }

    private static void logMissingFontResource(String fileName, String absolutePath, String absoluteStatus,
            String relativePath, String relativeStatus) {
        LOGGER.log(Level.WARNING,
                "Skipping font {0}: Font resource not found. Checked {1} (URL={2}) and {3} (URL={4})",
                new Object[] { fileName, absolutePath, absoluteStatus, relativePath, relativeStatus });
    }

    private static String findFirstAvailable(String[] availableFamilies, String[] preferredFamilies) {
        for (String preferred : preferredFamilies) {
            for (String available : availableFamilies) {
                if (available.equalsIgnoreCase(preferred)) {
                    return available;
                }
            }
        }
        return null;
    }

    private static void debugListBundledFonts() {
        if (!DEBUG_RESOURCE_LISTING) {
            return;
        }
        try {
            URL directoryUrl = FontInstaller.class.getResource(FONT_RESOURCE_BASE);
            if (directoryUrl == null) {
                LOGGER.log(Level.FINE, "Font debug: resource path {0} not found on classpath", FONT_RESOURCE_BASE);
                return;
            }

            List<String> discovered = new ArrayList<String>();
            String protocol = directoryUrl.getProtocol();
            if ("jar".equals(protocol)) {
                JarURLConnection connection = (JarURLConnection) directoryUrl.openConnection();
                String entryName = connection.getEntryName();
                try (JarFile jarFile = connection.getJarFile()) {
                    Enumeration<JarEntry> entries = jarFile.entries();
                    while (entries.hasMoreElements()) {
                        JarEntry entry = entries.nextElement();
                        if (entry.isDirectory()) {
                            continue;
                        }
                        String name = entry.getName();
                        if (name.startsWith(entryName) && name.endsWith(".ttf")) {
                            discovered.add(name.substring(entryName.length()));
                        }
                    }
                }
            } else if ("file".equals(protocol)) {
                Path directoryPath = Paths.get(directoryUrl.toURI());
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(directoryPath, "*.ttf")) {
                    for (Path path : stream) {
                        discovered.add(path.getFileName().toString());
                    }
                }
            } else {
                LOGGER.log(Level.FINE, "Font debug: unsupported protocol {0} for resource listing", protocol);
                return;
            }

            if (discovered.isEmpty()) {
                LOGGER.log(Level.FINE, "Font debug: no font resources found under {0} ({1})",
                        new Object[] { FONT_RESOURCE_BASE, directoryUrl });
            } else {
                LOGGER.log(Level.FINE, "Font debug: bundled fonts under {0}: {1}",
                        new Object[] { FONT_RESOURCE_BASE, discovered });
            }
        } catch (Exception ex) {
            LOGGER.log(Level.FINE, "Font debug: failed to enumerate bundled fonts", ex);
        }
    }

    private static void verifyBundledFontPresence() {
        try {
            Objects.requireNonNull(FontInstaller.class.getResource(FONT_RESOURCE_BASE + INTER_RESOURCES[0].fileName),
                    "Inter-Regular.ttf missing on classpath");
        } catch (NullPointerException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage() + " – ensure Inter fonts are packaged under " + FONT_RESOURCE_BASE);
        }
    }

    private static Font createFontForSize(String key, String family, int style, float size) {
        Font font = REGISTERED_FONTS.get(key);
        if (font != null) {
            return font.deriveFont(style, size);
        }
        return fallbackFont(family, style, size);
    }

    private static Font fallbackFont(String family, int style, float size) {
        Font font = new Font(family, Font.PLAIN, Math.max(1, Math.round(size)));
        return font.deriveFont(style, size);
    }

    private static void setUIDefaultFont(UIDefaults defaults, String key, Font font) {
        if (font == null || key == null) {
            return;
        }
        defaults.put(key, font);
        UIManager.put(key, font);
    }

    private static Font selectReplacementFont(Font existing, Font regular, Font italic, Font semiBold, Font boldItalic,
            String fallbackFamily, float size) {
        if (existing == null) {
            return regular;
        }
        boolean isBold = existing.isBold();
        boolean isItalic = existing.isItalic();

        if (isBold && isItalic) {
            if (boldItalic != null) {
                return boldItalic;
            }
            return fallbackFont(fallbackFamily, Font.BOLD | Font.ITALIC, size);
        }
        if (isBold) {
            if (semiBold != null) {
                return semiBold;
            }
            return fallbackFont(fallbackFamily, Font.BOLD, size);
        }
        if (isItalic) {
            if (italic != null) {
                return italic;
            }
            return fallbackFont(fallbackFamily, Font.ITALIC, size);
        }
        if (existing.getStyle() != Font.PLAIN) {
            return fallbackFont(fallbackFamily, existing.getStyle(), size);
        }
        return regular;
    }

    private static final class FontResource {
        final String key;
        final String fileName;

        FontResource(String key, String fileName) {
            this.key = key;
            this.fileName = fileName;
        }
    }

    private static final class FontData {
        final byte[] bytes;
        final String description;
        final FontOrigin origin;

        FontData(byte[] bytes, String description, FontOrigin origin) {
            this.bytes = bytes;
            this.description = description;
            this.origin = origin;
        }
    }

    private enum FontOrigin {
        CLASSPATH,
        EXTERNAL
    }
}

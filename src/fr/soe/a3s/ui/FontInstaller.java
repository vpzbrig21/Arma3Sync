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
        ResolvedFontStream resolvedStream = resolveClasspathStream(absolutePath);
        if (resolvedStream == null) {
            String relativePath = FONT_RESOURCE_RELATIVE_PREFIX + fileName;
            resolvedStream = resolveClasspathStream(relativePath);
            if (resolvedStream == null) {
                String absoluteStatus = describeResourceUrl(absolutePath);
                String relativeStatus = describeResourceUrl(relativePath);
                logMissingFontResource(fileName, absolutePath, absoluteStatus, relativePath, relativeStatus);
                return null;
            }
        }

        try (InputStream stream = resolvedStream.stream) {
            byte[] fontData = readAllBytes(stream);
            if (fontData == null || fontData.length < MIN_FONT_SIZE_BYTES) {
                logFontWarning(fileName,
                        "Font resource too small (" + (fontData == null ? 0 : fontData.length)
                                + " bytes) at " + resolvedStream.description);
                return null;
            }
            if (!isSupportedTrueType(fontData)) {
                logFontWarning(fileName,
                        "Unsupported font signature " + signatureString(fontData) + " at "
                                + resolvedStream.description);
                return null;
            }
            try (ByteArrayInputStream byteStream = new ByteArrayInputStream(fontData)) {
                Font createdFont = Font.createFont(Font.TRUETYPE_FONT, byteStream);
                LOGGER.log(Level.FINE, "Loaded font {0} from {1}",
                        new Object[] { fileName, resolvedStream.description });
                return createdFont;
            } catch (FontFormatException ex) {
                logFontWarning(fileName,
                        "Invalid TrueType font data at " + resolvedStream.description + ": " + ex.getMessage());
                LOGGER.log(Level.FINE, "Invalid font format in resource " + resolvedStream.description, ex);
            }
        } catch (IOException ex) {
            logFontWarning(fileName,
                    "I/O error while processing font at " + resolvedStream.description + ": " + ex.getMessage());
            LOGGER.log(Level.FINE, "I/O error while reading font resource " + resolvedStream.description, ex);
        }
        return null;
    }

    private static ResolvedFontStream resolveClasspathStream(String resourcePath) {
        if (resourcePath == null) {
            return null;
        }
        InputStream stream = FontInstaller.class.getResourceAsStream(resourcePath);
        if (stream == null) {
            return null;
        }
        URL resourceUrl = FontInstaller.class.getResource(resourcePath);
        String description = resourceUrl != null ? resourceUrl.toString() : resourcePath;
        return new ResolvedFontStream(stream, description);
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

    private static boolean isSupportedTrueType(byte[] fontData) {
        if (fontData == null || fontData.length < 4) {
            return false;
        }
        return matchesSignature(fontData, SFNT_SIGNATURE_TRUETYPE) || matchesSignature(fontData, SFNT_SIGNATURE_OTTO);
    }

    private static boolean matchesSignature(byte[] data, byte[] expected) {
        if (expected.length > data.length) {
            return false;
        }
        for (int i = 0; i < expected.length; i++) {
            if (data[i] != expected[i]) {
                return false;
            }
        }
        return true;
    }

    private static String signatureString(byte[] signature) {
        if (signature == null || signature.length < 4) {
            return "length < 4";
        }
        return String.format("0x%02X%02X%02X%02X", signature[0], signature[1], signature[2], signature[3]);
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

    private static final class ResolvedFontStream {
        final InputStream stream;
        final String description;

        ResolvedFontStream(InputStream stream, String description) {
            this.stream = stream;
            this.description = description;
        }
    }
}

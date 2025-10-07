package fr.soe.a3s.ui;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.UIDefaults;
import javax.swing.UIManager;

/**
 * Utility that registers the bundled Inter font family and applies it to the Swing UI defaults.
 */
public final class FontInstaller {

    private static final Logger LOGGER = Logger.getLogger(FontInstaller.class.getName());
    private static final String[] FONT_RESOURCE_CANDIDATES = new String[] {
            "/fonts/%s",
            "/resources/fonts/%s",
            "/fr/soe/a3s/ui/fonts/%s",
            "fonts/%s" };
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
    private static Path jarDirectory;
    private static boolean jarDirectoryResolved;

    private static final boolean DEBUG_EXTERNAL_FONT_SCAN = Boolean.getBoolean("a3s.font.debug");

    private FontInstaller() {
    }

    /**
     * Registers the bundled Inter font faces. Returns the best available font family name to be used for UI defaults.
     *
     * @return the effective font family name, never {@code null}
     */
    public static synchronized String installInterFonts() {
        if (!fontsInstalled) {
            debugLogExternalFontLocations();
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
            boolean hasRegisteredInter = !REGISTERED_FONTS.isEmpty();
            if (hasRegisteredInter) {
                effectiveFontFamily = "Inter";
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
        List<String> attemptedLocations = new ArrayList<String>();
        Set<String> visitedLocations = new LinkedHashSet<String>();

        while (true) {
            ResolvedFontStream resolvedStream = resolveFontStream(fileName, attemptedLocations, visitedLocations);
            if (resolvedStream == null) {
                logMissingFontResource(fileName, attemptedLocations);
                return null;
            }

            try (InputStream stream = resolvedStream.stream) {
                BufferedInputStream buffered = stream instanceof BufferedInputStream ? (BufferedInputStream) stream
                        : new BufferedInputStream(stream);
                buffered.mark(8);
                byte[] signature = new byte[4];
                int read = buffered.read(signature);
                if (read < 4) {
                    logFontWarning(fileName, "Font stream too short at " + resolvedStream.description);
                    continue;
                }
                if (!isSupportedTrueType(signature)) {
                    logFontWarning(fileName, "Unsupported font signature " + signatureString(signature) + " at "
                            + resolvedStream.description);
                    continue;
                }
                try {
                    buffered.reset();
                    Font createdFont = Font.createFont(Font.TRUETYPE_FONT, buffered);
                    LOGGER.log(Level.FINE, "Loaded font {0} from {1}",
                            new Object[] { fileName, resolvedStream.description });
                    return createdFont;
                } catch (FontFormatException ex) {
                    logFontWarning(fileName,
                            "Invalid TrueType font data at " + resolvedStream.description + ": " + ex.getMessage());
                    LOGGER.log(Level.FINE, "Invalid font format in resource " + resolvedStream.description, ex);
                } catch (IOException ex) {
                    logFontWarning(fileName,
                            "I/O error while creating font at " + resolvedStream.description + ": " + ex.getMessage());
                    LOGGER.log(Level.FINE,
                            "I/O error while creating font from resource " + resolvedStream.description, ex);
                }
            } catch (IOException ex) {
                logFontWarning(fileName,
                        "I/O error while reading font at " + resolvedStream.description + ": " + ex.getMessage());
                LOGGER.log(Level.FINE, "I/O error while reading font resource " + resolvedStream.description, ex);
            }
        }
    }

    private static ResolvedFontStream resolveFontStream(String fileName, List<String> attemptedLocations,
            Set<String> visitedLocations) {
        for (String candidate : FONT_RESOURCE_CANDIDATES) {
            String resourcePath = String.format(candidate, fileName);
            String description = "classpath:" + resourcePath;
            if (visitedLocations.contains(description)) {
                continue;
            }
            visitedLocations.add(description);
            attemptedLocations.add(description);
            InputStream stream = openClasspathStream(resourcePath);
            if (stream != null) {
                return new ResolvedFontStream(stream, description);
            }
        }

        Path jarDir = getJarDirectory();
        if (jarDir != null) {
            Path jarFontPath = jarDir.resolve("fonts").resolve(fileName).toAbsolutePath();
            String description = jarFontPath.toString();
            if (!visitedLocations.contains(description)) {
                visitedLocations.add(description);
                attemptedLocations.add(description);
                InputStream stream = openFileStream(jarFontPath);
                if (stream != null) {
                    return new ResolvedFontStream(stream, description);
                }
            }
        }

        Path workingDir = Paths.get("").toAbsolutePath();
        Path workingFontPath = workingDir.resolve("fonts").resolve(fileName).toAbsolutePath();
        String description = workingFontPath.toString();
        if (!visitedLocations.contains(description)) {
            visitedLocations.add(description);
            attemptedLocations.add(description);
            InputStream stream = openFileStream(workingFontPath);
            if (stream != null) {
                return new ResolvedFontStream(stream, description);
            }
        }

        return null;
    }

    private static InputStream openClasspathStream(String resourcePath) {
        if (resourcePath == null) {
            return null;
        }
        if (resourcePath.startsWith("/")) {
            return FontInstaller.class.getResourceAsStream(resourcePath);
        }
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader != null) {
            InputStream stream = loader.getResourceAsStream(resourcePath);
            if (stream != null) {
                return stream;
            }
        }
        loader = FontInstaller.class.getClassLoader();
        if (loader != null) {
            InputStream stream = loader.getResourceAsStream(resourcePath);
            if (stream != null) {
                return stream;
            }
        }
        return ClassLoader.getSystemResourceAsStream(resourcePath);
    }

    private static InputStream openFileStream(Path path) {
        if (path == null) {
            return null;
        }
        try {
            if (Files.isRegularFile(path)) {
                return Files.newInputStream(path);
            }
        } catch (IOException ex) {
            LOGGER.log(Level.FINE, "Failed to open font file " + path, ex);
        }
        return null;
    }

    private static boolean isSupportedTrueType(byte[] signature) {
        if (signature == null || signature.length < 4) {
            return false;
        }
        return matchesSignature(signature, SFNT_SIGNATURE_TRUETYPE) || matchesSignature(signature, SFNT_SIGNATURE_OTTO);
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

    private static void logMissingFontResource(String fileName, List<String> attemptedLocations) {
        LOGGER.log(Level.WARNING, "Skipping font {0}: Font resource not found. Tested locations: {1}",
                new Object[] { fileName, attemptedLocations });
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

    private static void debugLogExternalFontLocations() {
        if (!DEBUG_EXTERNAL_FONT_SCAN) {
            return;
        }
        Path jarDir = getJarDirectory();
        if (jarDir != null) {
            logDiscoveredFonts("JAR directory", jarDir.resolve("fonts"));
        } else {
            LOGGER.log(Level.FINE, "Font debug scan: code source location unavailable");
        }
        Path workingDir = Paths.get("").toAbsolutePath();
        logDiscoveredFonts("Working directory", workingDir.resolve("fonts"));
    }

    private static void logDiscoveredFonts(String label, Path directory) {
        List<String> discovered = new ArrayList<String>();
        if (directory != null) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, "Inter-*.ttf")) {
                for (Path path : stream) {
                    discovered.add(path.toAbsolutePath().toString());
                }
            } catch (IOException ex) {
                LOGGER.log(Level.FINE, "Font debug scan failed for " + directory, ex);
            }
        }
        if (discovered.isEmpty()) {
            LOGGER.log(Level.FINE, "Font debug scan: no Inter fonts under {0} ({1})",
                    new Object[] { label, directory });
        } else {
            LOGGER.log(Level.FINE, "Font debug scan: found Inter fonts under {0}: {1}",
                    new Object[] { label, discovered });
        }
    }

    private static Path getJarDirectory() {
        if (!jarDirectoryResolved) {
            jarDirectoryResolved = true;
            try {
                CodeSource codeSource = FontInstaller.class.getProtectionDomain().getCodeSource();
                if (codeSource != null && codeSource.getLocation() != null) {
                    Path locationPath = Paths.get(codeSource.getLocation().toURI());
                    Path parent = locationPath.getParent();
                    if (parent != null) {
                        jarDirectory = parent.toAbsolutePath();
                    } else if (Files.isDirectory(locationPath)) {
                        jarDirectory = locationPath.toAbsolutePath();
                    }
                }
            } catch (Exception ex) {
                LOGGER.log(Level.FINE, "Unable to resolve jar directory", ex);
            }
        }
        return jarDirectory;
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

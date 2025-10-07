package fr.soe.a3s.ui;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
    private static final String[] FONT_RESOURCE_CANDIDATES = new String[] {
            "/fonts/%s",
            "/resources/fonts/%s",
            "/fr/soe/a3s/ui/fonts/%s",
            "fonts/%s" };
    private static final int MIN_FONT_SIZE_BYTES = 10 * 1024;
    private static final int SFNT_SIGNATURE_TRUETYPE = 0x00010000;
    private static final int SFNT_SIGNATURE_TRUE = 0x74727565;

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

    private FontInstaller() {
    }

    /**
     * Registers the bundled Inter font faces. Returns the best available font family name to be used for UI defaults.
     *
     * @return the effective font family name, never {@code null}
     */
    public static synchronized String installInterFonts() {
        if (!fontsInstalled) {
            logAvailableFontResourcesIfDebugEnabled();
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
        List<String> attemptedPaths = new ArrayList<String>();
        boolean resourceLocated = false;
        for (String candidate : FONT_RESOURCE_CANDIDATES) {
            String resourcePath = String.format(candidate, fileName);
            attemptedPaths.add(resourcePath);
            if (!resourceExists(resourcePath)) {
                continue;
            }
            resourceLocated = true;
            InputStream inputStream = openResourceStream(resourcePath);
            if (inputStream == null) {
                continue;
            }
            try {
                byte[] fontData = readAllBytes(inputStream);
                if (fontData.length < MIN_FONT_SIZE_BYTES) {
                    logFontWarning(fileName, "Font resource is unexpectedly small (" + fontData.length
                            + " bytes) at " + resourcePath);
                    continue;
                }
                if (!isSupportedTrueType(fontData)) {
                    logFontWarning(fileName, "Font resource is not a static TrueType font (signature "
                            + signatureString(fontData) + ") at " + resourcePath);
                    continue;
                }

                try (ByteArrayInputStream fontStream = new ByteArrayInputStream(fontData)) {
                    Font createdFont = Font.createFont(Font.TRUETYPE_FONT, fontStream);
                    LOGGER.log(Level.FINE, "Loaded font {0} from {1}", new Object[] { fileName, resourcePath });
                    return createdFont;
                } catch (FontFormatException ex) {
                    logFontWarning(fileName, "Invalid font format at " + resourcePath + ": " + ex.getMessage());
                    LOGGER.log(Level.FINE, "Invalid font format in resource " + resourcePath, ex);
                }
            } catch (IOException ex) {
                logFontWarning(fileName,
                        "I/O error while reading font resource at " + resourcePath + ": " + ex.getMessage());
                LOGGER.log(Level.FINE, "I/O error while reading font resource " + resourcePath, ex);
            } finally {
                try {
                    inputStream.close();
                } catch (IOException ex) {
                    LOGGER.log(Level.FINEST, "Failed to close font resource stream for " + resourcePath, ex);
                }
            }
        }

        if (!resourceLocated) {
            logMissingFontResource(fileName, attemptedPaths);
        }
        return null;
    }

    private static byte[] readAllBytes(InputStream inputStream) throws IOException {
        try (BufferedInputStream bufferedInput = new BufferedInputStream(inputStream);
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = bufferedInput.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        }
    }

    private static boolean isSupportedTrueType(byte[] fontData) {
        if (fontData.length < 4) {
            return false;
        }
        int signature = ((fontData[0] & 0xFF) << 24) | ((fontData[1] & 0xFF) << 16) | ((fontData[2] & 0xFF) << 8)
                | (fontData[3] & 0xFF);
        return signature == SFNT_SIGNATURE_TRUETYPE || signature == SFNT_SIGNATURE_TRUE;
    }

    private static String signatureString(byte[] fontData) {
        if (fontData.length < 4) {
            return "length < 4";
        }
        return String.format("0x%02X%02X%02X%02X", fontData[0], fontData[1], fontData[2], fontData[3]);
    }

    private static void logFontWarning(String fileName, String message) {
        LOGGER.log(Level.WARNING, "Skipping font {0}: {1}", new Object[] { fileName, message });
    }

    private static void logMissingFontResource(String fileName, List<String> attemptedPaths) {
        LOGGER.log(Level.WARNING, "Skipping font {0}: Font resource not found on classpath. Tested paths: {1}",
                new Object[] { fileName, attemptedPaths });
    }

    private static InputStream openResourceStream(String resourcePath) {
        if (resourcePath == null) {
            return null;
        }
        if (resourcePath.startsWith("/")) {
            return FontInstaller.class.getResourceAsStream(resourcePath);
        }
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader != null) {
            return loader.getResourceAsStream(resourcePath);
        }
        loader = FontInstaller.class.getClassLoader();
        if (loader != null) {
            return loader.getResourceAsStream(resourcePath);
        }
        return ClassLoader.getSystemResourceAsStream(resourcePath);
    }

    private static boolean resourceExists(String resourcePath) {
        if (resourcePath == null) {
            return false;
        }
        if (resourcePath.startsWith("/")) {
            return FontInstaller.class.getResource(resourcePath) != null;
        }
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader != null && loader.getResource(resourcePath) != null) {
            return true;
        }
        loader = FontInstaller.class.getClassLoader();
        if (loader != null && loader.getResource(resourcePath) != null) {
            return true;
        }
        return ClassLoader.getSystemResource(resourcePath) != null;
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

    private static void logAvailableFontResourcesIfDebugEnabled() {
        if (!LOGGER.isLoggable(Level.FINE)) {
            return;
        }
        logResourcesUnder("/fonts");
        logResourcesUnder("/resources/fonts");
    }

    private static void logResourcesUnder(String rootPath) {
        List<String> discovered = new ArrayList<String>();
        String normalized = rootPath;
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (!normalized.endsWith("/")) {
            normalized = normalized + "/";
        }
        try {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            if (loader == null) {
                loader = FontInstaller.class.getClassLoader();
            }
            if (loader == null) {
                loader = ClassLoader.getSystemClassLoader();
            }
            if (loader == null) {
                LOGGER.log(Level.FINE, "No class loader available to enumerate resources under {0}", rootPath);
                return;
            }
            Enumeration<URL> urls = loader.getResources(normalized);
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                if ("jar".equalsIgnoreCase(url.getProtocol())) {
                    JarURLConnection connection = (JarURLConnection) url.openConnection();
                    try (JarFile jarFile = connection.getJarFile()) {
                        String baseEntry = connection.getEntryName();
                        if (baseEntry == null) {
                            baseEntry = normalized;
                        }
                        if (!baseEntry.endsWith("/")) {
                            baseEntry = baseEntry + "/";
                        }
                        Enumeration<JarEntry> entries = jarFile.entries();
                        while (entries.hasMoreElements()) {
                            JarEntry entry = entries.nextElement();
                            String name = entry.getName();
                            if (name.startsWith(baseEntry) && !entry.isDirectory()) {
                                discovered.add("jar:" + name);
                            }
                        }
                    }
                } else {
                    discovered.add(url.toString());
                }
            }
        } catch (IOException ex) {
            LOGGER.log(Level.FINE, "Failed to enumerate font resources under " + rootPath, ex);
        }
        if (discovered.isEmpty()) {
            LOGGER.log(Level.FINE, "No font resources discovered under {0}", rootPath);
        } else {
            LOGGER.log(Level.FINE, "Discovered font resources under {0}: {1}", new Object[] { rootPath, discovered });
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
}

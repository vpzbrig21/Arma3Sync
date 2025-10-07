package fr.soe.a3s.ui;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.FontFormatException;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.UIDefaults;
import javax.swing.UIManager;

/**
 * Utility that registers the bundled Inter font family and applies it to the Swing UI defaults.
 */
public final class FontInstaller {

    private static final Logger LOGGER = Logger.getLogger(FontInstaller.class.getName());
    private static final String FONT_RESOURCE_ROOT = "/fonts/";
    private static final int MIN_FONT_SIZE_BYTES = 10 * 1024;
    private static final int SFNT_SIGNATURE_TRUETYPE = 0x00010000;
    private static final int SFNT_SIGNATURE_OTTO = 0x4F54544F;

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

    private static final String[] FALLBACK_FONTS = new String[] { "Inter", "Segoe UI", "SansSerif" };

    private static final String[] COMMON_FONT_KEYS = new String[] { "defaultFont", "Button.font", "Label.font",
            "Menu.font", "MenuItem.font", "CheckBox.font", "RadioButton.font", "TabbedPane.font", "TextField.font",
            "PasswordField.font", "TextArea.font", "TextPane.font", "EditorPane.font", "FormattedTextField.font",
            "ComboBox.font", "List.font", "Table.font", "TableHeader.font", "Tree.font", "ToolTip.font",
            "TitledBorder.font", "Spinner.font", "OptionPane.messageFont", "OptionPane.buttonFont", "ToggleButton.font",
            "Panel.font" };

    private static final Map<String, Font> REGISTERED_FONTS = new HashMap<String, Font>();

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
        boolean interRegistered = false;
        if (!fontsInstalled) {
            GraphicsEnvironment graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();
            for (FontResource resource : INTER_RESOURCES) {
                Font font = loadFont(resource.fileName);
                if (font != null) {
                    boolean registered = graphicsEnvironment.registerFont(font);
                    if (registered) {
                        REGISTERED_FONTS.put(resource.key, font);
                        interRegistered = true;
                    } else {
                        logFontWarning(resource.fileName,
                                "Font was not accepted by the graphics environment during registration");
                    }
                }
            }
            fontsInstalled = true;
        }

        if (effectiveFontFamily == null) {
            GraphicsEnvironment graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();
            String[] availableFamilies = graphicsEnvironment.getAvailableFontFamilyNames(Locale.getDefault());
            effectiveFontFamily = findFirstAvailable(availableFamilies, FALLBACK_FONTS);
            boolean hasRegisteredInter = interRegistered || !REGISTERED_FONTS.isEmpty();
            if (hasRegisteredInter && !"Inter".equalsIgnoreCase(effectiveFontFamily)) {
                effectiveFontFamily = "Inter";
            }
            if (!hasRegisteredInter && !"Inter".equals(effectiveFontFamily)) {
                LOGGER.log(Level.WARNING,
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
        String resourcePath = FONT_RESOURCE_ROOT + fileName;
        try (InputStream inputStream = FontInstaller.class.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                logFontWarning(fileName, "Font resource not found on classpath at " + resourcePath);
                return null;
            }

            byte[] fontData = readAllBytes(inputStream);
            if (fontData.length < MIN_FONT_SIZE_BYTES) {
                logFontWarning(fileName, "Font resource is unexpectedly small (" + fontData.length + " bytes)");
                return null;
            }
            if (!isSupportedSfnt(fontData)) {
                logFontWarning(fileName, "Unsupported sfnt signature (" + signatureString(fontData) + ")");
                return null;
            }

            try (ByteArrayInputStream fontStream = new ByteArrayInputStream(fontData)) {
                return Font.createFont(Font.TRUETYPE_FONT, fontStream);
            } catch (FontFormatException ex) {
                logFontWarning(fileName, "Invalid font format: " + ex.getMessage());
                LOGGER.log(Level.FINE, "Invalid font format in resource " + resourcePath, ex);
            }
        } catch (IOException ex) {
            logFontWarning(fileName, "I/O error while reading font resource: " + ex.getMessage());
            LOGGER.log(Level.FINE, "I/O error while reading font resource " + resourcePath, ex);
        } catch (Exception ex) {
            logFontWarning(fileName, "Failed to create font: " + ex.getMessage());
            LOGGER.log(Level.FINE, "Failed to create font from resource " + resourcePath, ex);
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

    private static boolean isSupportedSfnt(byte[] fontData) {
        if (fontData.length < 4) {
            return false;
        }
        int signature = ((fontData[0] & 0xFF) << 24) | ((fontData[1] & 0xFF) << 16) | ((fontData[2] & 0xFF) << 8)
                | (fontData[3] & 0xFF);
        return signature == SFNT_SIGNATURE_TRUETYPE || signature == SFNT_SIGNATURE_OTTO;
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

    private static String findFirstAvailable(String[] availableFamilies, String[] preferredFamilies) {
        for (String preferred : preferredFamilies) {
            for (String available : availableFamilies) {
                if (available.equalsIgnoreCase(preferred)) {
                    return available;
                }
            }
        }
        // As a last resort, fall back to the default logical font family.
        Font labelFont = UIManager.getFont("Label.font");
        if (labelFont != null) {
            return labelFont.getFamily();
        }
        return Font.SANS_SERIF;
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

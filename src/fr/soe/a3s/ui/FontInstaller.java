package fr.soe.a3s.ui;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
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
    private static final String RESOURCE_PREFIX = "resources/fonts/";

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
        if (!fontsInstalled) {
            GraphicsEnvironment graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();
            for (FontResource resource : INTER_RESOURCES) {
                Font font = loadFont(resource.fileName);
                if (font != null) {
                    graphicsEnvironment.registerFont(font);
                    REGISTERED_FONTS.put(resource.key, font);
                }
            }
            fontsInstalled = true;
        }

        if (effectiveFontFamily == null) {
            GraphicsEnvironment graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();
            String[] availableFamilies = graphicsEnvironment.getAvailableFontFamilyNames(Locale.getDefault());
            effectiveFontFamily = findFirstAvailable(availableFamilies, FALLBACK_FONTS);
            if (!"Inter".equals(effectiveFontFamily)) {
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
        String resourcePath = RESOURCE_PREFIX + fileName;
        try (InputStream inputStream = FontInstaller.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                LOGGER.log(Level.WARNING, "Font resource not found on classpath: {0}", resourcePath);
                return null;
            }
            return Font.createFont(Font.TRUETYPE_FONT, inputStream);
        } catch (FontFormatException | IOException ex) {
            LOGGER.log(Level.WARNING, "Failed to load font resource: " + resourcePath, ex);
            return null;
        }
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

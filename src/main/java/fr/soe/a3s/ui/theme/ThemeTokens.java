package fr.soe.a3s.ui.theme;

import java.awt.Color;

import javax.swing.UIManager;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.DimensionUIResource;
import javax.swing.plaf.InsetsUIResource;

import fr.soe.a3s.ui.ThemeManager;

/**
 * Convenience accessors for the active theme and helpers to push token values into Swing/FlatLaf.
 */
public final class ThemeTokens {

    private ThemeTokens() {
    }

    public static ThemeDefinition theme() {
        ThemeDefinition definition = ThemeManager.currentTheme();
        return definition != null ? definition : ThemeLibrary.dark();
    }

    public static ThemePalette colors() {
        return theme().palette();
    }

    public static ThemeMetrics metrics() {
        return theme().metrics();
    }

    /**
     * Propagates core token values to UI defaults so Swing components inherit them without manual wiring.
     */
    public static void applyToUiDefaults(ThemeDefinition definition) {
        ThemeMetrics metrics = definition.metrics();
        ThemePalette palette = definition.palette();

        InsetsUIResource controlPadding = new InsetsUIResource(metrics.spacingSm(), metrics.spacingLg(),
                metrics.spacingSm(), metrics.spacingLg());

        UIManager.put("Component.arc", metrics.radiusMd());
        UIManager.put("Button.arc", metrics.radiusMd());
        UIManager.put("TextComponent.arc", metrics.radiusSm());
        UIManager.put("Component.focusWidth", metrics.focusWidth());
        UIManager.put("Component.focusColor", color(palette.stateFocus()));
        UIManager.put("Component.innerFocusWidth", 0);
        UIManager.put("Component.borderColor", color(palette.border()));
        UIManager.put("Component.disabledBorderColor", color(palette.borderMuted()));
        UIManager.put("Component.background", color(palette.surface()));
        UIManager.put("Component.disabledBackground", color(palette.stateDisabledBg()));
        UIManager.put("Component.hoverColor", color(palette.stateHoverOverlay()));
        UIManager.put("Component.focusedBorderColor", color(palette.stateFocus()));

        UIManager.put("Button.background", color(palette.surfaceVariant()));
        UIManager.put("Button.foreground", color(palette.textPrimary()));
        UIManager.put("Button.hoverBackground", color(palette.stateHoverOverlay()));
        UIManager.put("Button.pressedBackground", color(palette.stateSelectionBg()));
        UIManager.put("Button.disabledText", color(palette.textDisabled()));
        UIManager.put("Button.borderColor", color(palette.border()));
        UIManager.put("Button.default.background", color(palette.accentPrimary()));
        UIManager.put("Button.default.foreground", color(palette.surface()));
        UIManager.put("Button.default.hoverBackground", color(palette.accentSecondary()));
        UIManager.put("Button.default.focusColor", color(palette.stateFocus()));
        UIManager.put("Button.innerFocusWidth", metrics.focusWidth());
        UIManager.put("Button.minimumWidth", metrics.spacingXl() * 2);

        UIManager.put("ToggleButton.selectedBackground", color(palette.stateSelectionBg()));
        UIManager.put("ToggleButton.selectedForeground", color(palette.stateSelectionFg()));

        UIManager.put("TextComponent.background", color(palette.surface()));
        UIManager.put("TextComponent.foreground", color(palette.textPrimary()));
        UIManager.put("TextComponent.selectionBackground", color(palette.stateSelectionBg()));
        UIManager.put("TextComponent.selectionForeground", color(palette.stateSelectionFg()));
        UIManager.put("TextComponent.caretForeground", color(palette.accentPrimary()));
        UIManager.put("Panel.background", color(palette.surface()));
        UIManager.put("Panel.foreground", color(palette.textPrimary()));
        UIManager.put("OptionPane.background", color(palette.surface()));
        UIManager.put("OptionPane.foreground", color(palette.textPrimary()));
        UIManager.put("OptionPane.messageForeground", color(palette.textPrimary()));
        UIManager.put("OptionPane.messageAreaBorder", new InsetsUIResource(metrics.spacingSm(), metrics.spacingLg(),
                metrics.spacingSm(), metrics.spacingLg()));
        UIManager.put("TextComponent.borderColor", color(palette.border()));
        UIManager.put("TextComponent.disabledBackground", color(palette.stateDisabledBg()));
        UIManager.put("TextComponent.disabledForeground", color(palette.textDisabled()));
        UIManager.put("TextComponent.focusedBackground", color(palette.surface()));
        UIManager.put("TextComponent.margin", controlPadding);

        UIManager.put("TextArea.background", color(palette.surface()));
        UIManager.put("TextField.background", color(palette.surface()));
        UIManager.put("TextField.inactiveBackground", color(palette.stateDisabledBg()));
        UIManager.put("TextField.inactiveForeground", color(palette.textDisabled()));

        UIManager.put("ComboBox.buttonBackground", color(palette.surfaceVariant()));
        UIManager.put("ComboBox.buttonHoverBackground", color(palette.stateHoverOverlay()));
        UIManager.put("ComboBox.selectionBackground", color(palette.stateSelectionBg()));
        UIManager.put("ComboBox.selectionForeground", color(palette.stateSelectionFg()));

        UIManager.put("ScrollBar.showButtons", Boolean.FALSE);
        UIManager.put("ScrollBar.thumbArc", metrics.radiusMd());
        UIManager.put("ScrollBar.thumb", color(palette.surfaceVariant()));
        UIManager.put("ScrollBar.track", color(palette.surfaceMuted()));
        UIManager.put("ScrollBar.trackInsets", new InsetsUIResource(metrics.spacingXs(), 0, metrics.spacingXs(), 0));

        UIManager.put("TabbedPane.selectedBackground", color(palette.surface()));
        UIManager.put("TabbedPane.underlineColor", color(palette.accentPrimary()));
        UIManager.put("TabbedPane.tabInsets",
                new InsetsUIResource(metrics.spacingSm(), metrics.spacingLg(), metrics.spacingSm(), metrics.spacingLg()));
        UIManager.put("TabbedPane.tabHeight", metrics.spacingXl());
        UIManager.put("TabbedPane.contentBorderInsets",
                new InsetsUIResource(metrics.spacingMd(), metrics.spacingMd(), metrics.spacingMd(), metrics.spacingMd()));
        UIManager.put("TabbedPane.tabAreaBackground", color(palette.surface()));
        UIManager.put("TabbedPane.foreground", color(palette.textSecondary()));
        UIManager.put("TabbedPane.selectedForeground", color(palette.textPrimary()));

        UIManager.put("Table.background", color(palette.surface()));
        UIManager.put("Table.foreground", color(palette.textPrimary()));
        UIManager.put("Table.selectionBackground", color(palette.stateSelectionBg()));
        UIManager.put("Table.selectionForeground", color(palette.stateSelectionFg()));
        UIManager.put("Table.alternateRowColor", color(palette.surfaceVariant()));
        UIManager.put("Table.gridColor", color(palette.borderMuted()));
        UIManager.put("Table.showHorizontalLines", Boolean.FALSE);
        UIManager.put("Table.showVerticalLines", Boolean.FALSE);
        UIManager.put("Table.rowHeight", metrics.spacingXl());
        UIManager.put("Table.intercellSpacing", new DimensionUIResource(0, metrics.spacingXs()));
        UIManager.put("Table.scrollPaneBorder", new InsetsUIResource(0, 0, 0, 0));

        UIManager.put("TableHeader.background", color(palette.surface()));
        UIManager.put("TableHeader.foreground", color(palette.textSecondary()));
        UIManager.put("TableHeader.separatorColor", color(palette.border()));
        UIManager.put("TableHeader.height", metrics.spacingXl());

        UIManager.put("List.background", color(palette.surface()));
        UIManager.put("List.foreground", color(palette.textPrimary()));
        UIManager.put("List.selectionBackground", color(palette.stateSelectionBg()));
        UIManager.put("List.selectionForeground", color(palette.stateSelectionFg()));

        UIManager.put("Tree.background", color(palette.surface()));
        UIManager.put("Tree.foreground", color(palette.textPrimary()));
        UIManager.put("Tree.selectionBackground", color(palette.stateSelectionBg()));
        UIManager.put("Tree.selectionForeground", color(palette.stateSelectionFg()));
        UIManager.put("Tree.rowHeight", metrics.spacingXl());

        UIManager.put("MenuBar.background", color(palette.surface()));
        UIManager.put("MenuBar.foreground", color(palette.textSecondary()));
        UIManager.put("MenuBar.borderColor", color(palette.border()));
        // Keep the integrated FlatLaf title bar compact while making the app icon
        // readable next to the menu entries.
        UIManager.put("TitlePane.iconSize", new DimensionUIResource(22, 22));
        UIManager.put("Menu.background", color(palette.surface()));
        UIManager.put("Menu.foreground", color(palette.textPrimary()));
        UIManager.put("MenuItem.background", color(palette.surface()));
        UIManager.put("MenuItem.foreground", color(palette.textPrimary()));
        UIManager.put("MenuItem.selectionBackground", color(palette.stateSelectionBg()));
        UIManager.put("MenuItem.selectionForeground", color(palette.stateSelectionFg()));

        UIManager.put("PopupMenu.background", color(palette.surface()));
        UIManager.put("PopupMenu.borderColor", color(palette.border()));

        UIManager.put("ToolBar.background", color(palette.surface()));
        UIManager.put("ToolBar.foreground", color(palette.textSecondary()));
        UIManager.put("ToolBar.borderColor", color(palette.border()));

        UIManager.put("Panel.background", color(palette.surface()));
        UIManager.put("Label.foreground", color(palette.textPrimary()));
        UIManager.put("OptionPane.background", color(palette.surface()));
        UIManager.put("OptionPane.messageForeground", color(palette.textPrimary()));

        UIManager.put("Separator.foreground", color(palette.borderMuted()));

        UIManager.put("ProgressBar.trackColor", color(palette.surfaceMuted()));
        UIManager.put("ProgressBar.foreground", color(palette.statusInfo()));
        UIManager.put("ProgressBar.selectionForeground", color(palette.textPrimary()));
        UIManager.put("ProgressBar.selectionBackground", color(palette.surface()));
    }

    private static ColorUIResource color(Color value) {
        return value != null ? new ColorUIResource(value) : null;
    }
}

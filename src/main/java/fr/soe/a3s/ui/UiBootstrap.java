package fr.soe.a3s.ui;

import javax.swing.UIManager;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;

import fr.soe.a3s.ui.theme.ThemeDefinition;
import fr.soe.a3s.ui.theme.ThemeTokens;

public final class UiBootstrap {

    private UiBootstrap() {
    }

    public static void initLaf(ThemeDefinition definition) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        FlatLaf lookAndFeel = definition.isDark() ? new FlatDarkLaf() : new FlatLightLaf();
        try {
            FlatLaf.setup(lookAndFeel);
            UIManager.setLookAndFeel(lookAndFeel);
        } catch (RuntimeException ex) {
            throw new IllegalStateException("Failed to initialize FlatLaf", ex);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to update look and feel", ex);
        }

        ThemeTokens.applyToUiDefaults(definition);

        UIManager.put("OptionPane.yesButtonText", "Yes");
        UIManager.put("OptionPane.noButtonText", "No");
        UIManager.put("OptionPane.cancelButtonText", "Cancel");
    }
}

package fr.soe.a3s.ui.theme;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import javax.swing.UIManager;
import javax.swing.border.Border;

import org.junit.jupiter.api.Test;

class ThemeTokensTest {

    @Test
    void borderDefaultsUseBorderInstances() {
        Object previousOptionPaneBorder = UIManager.get("OptionPane.messageAreaBorder");
        Object previousTableBorder = UIManager.get("Table.scrollPaneBorder");
        try {
            ThemeTokens.applyToUiDefaults(ThemeLibrary.dark());

            assertInstanceOf(Border.class, UIManager.get("OptionPane.messageAreaBorder"));
            assertInstanceOf(Border.class, UIManager.get("Table.scrollPaneBorder"));
        } finally {
            UIManager.put("OptionPane.messageAreaBorder", previousOptionPaneBorder);
            UIManager.put("Table.scrollPaneBorder", previousTableBorder);
        }
    }
}

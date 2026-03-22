package fr.soe.a3s.ui.help;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Image;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.UIManager;
import javax.swing.border.BevelBorder;
import javax.swing.text.html.HTMLEditorKit;

import fr.soe.a3s.ui.AbstractDialog;
import fr.soe.a3s.ui.Facade;
import fr.soe.a3s.ui.ImagePanel;
import fr.soe.a3s.ui.ImageResizer;
import fr.soe.a3s.ui.UiColors;

public class AboutCreditsDialog extends AbstractDialog {

    public AboutCreditsDialog(Facade facade) {
        super(facade, "Credits", true);
        setResizable(false);
        setPreferredSize(new java.awt.Dimension(550, 300));

        buttonOK.setEnabled(false);
        buttonOK.setVisible(false);
        buttonCancel.setText("Close");
        getRootPane().setDefaultButton(buttonCancel);

        Color panelBackground = resolvePanelBackground();
        JPanel mainPanel = new JPanel();
        mainPanel.setBorder(BorderFactory.createEtchedBorder(BevelBorder.LOWERED));
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setBackground(panelBackground);
        this.add(mainPanel, BorderLayout.CENTER);

        Box hBox = Box.createHorizontalBox();
        hBox.setOpaque(true);
        hBox.setBackground(panelBackground);
        mainPanel.add(hBox, BorderLayout.CENTER);
        {
            JEditorPane textPane = new JEditorPane();
            textPane.setEditorKit(new HTMLEditorKit());
            Font fontTextField = UIManager.getFont("TextField.font");
            Color textColor = resolveTextColor();

            StringBuilder credits = new StringBuilder();
            credits.append("<html><head><style>body{margin:0;background-color:")
                    .append(toCssColor(panelBackground))
                    .append(";color:")
                    .append(toCssColor(textColor))
                    .append(";}" +
                    "ul{margin-top:0;margin-bottom:0;padding-left:20px;}" +
                    "h3{margin-bottom:2px;}</style></head><body style='font-family:")
                    .append(fontTextField.getFamily())
                    .append(";font-size:")
                    .append(fontTextField.getSize())
                    .append("pt;'>");
            credits.append("<h3 style='font-size:")
                    .append(fontTextField.getSize() + 2)
                    .append("pt;'>Original Development</h3>");
            credits.append("<ul>")
                    .append("<li>Software Development - [S.o.E] Major_Shepard</li>")
                    .append("<li>Graphical Design - [S.o.E] Matt2507</li>")
                    .append("</ul>");
            credits.append("<h3 style='font-size:")
                    .append(fontTextField.getSize() + 2)
                    .append("pt;'>Testing</h3>");
            credits.append("<ul>")
                    .append("<li>[S.o.E]</li>")
                    .append("<li>[F27]</li>")
                    .append("<li>[BWF]</li>")
                    .append("<li>Team Members</li>")
                    .append("</ul>");
            credits.append("<h3 style='font-size:")
                    .append(fontTextField.getSize() + 2)
                    .append("pt;'>Inspired by</h3>");
            credits.append("<ul>")
                    .append("<li>ArmA II Game Launcher by SpiritedMachine</li>")
                    .append("<li>AddonSync 2009 by Yoma</li>")
                    .append("</ul>");
            credits.append("<h3 style='font-size:")
                    .append(fontTextField.getSize() + 2)
                    .append("pt;'>Maintainer since 2025</h3>");
            credits.append("<ul>")
                    .append("<li>[PzBrig21] Soro</li>")
                    .append("</ul>");
            credits.append("<h3 style='font-size:")
                    .append(fontTextField.getSize() + 2)
                    .append("pt;'>Open Source Libraries</h3>");
            credits.append("<ul>")
                    .append("<li>FlatLaf &amp; FlatLaf-Extras – MIT License</li>")
                    .append("<li>Geist typeface – SIL Open Font License 1.1</li>")
                    .append("<li>Ikonli – Apache 2.0</li>")
                    .append("<li>MigLayout – BSD License</li>")
                    .append("</ul>");
            credits.append("</body></html>");

            textPane.setText(credits.toString());
            textPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
            textPane.setFont(fontTextField);
            textPane.setEditable(false);
            textPane.setOpaque(false);
            textPane.setBackground(panelBackground);
            textPane.setForeground(textColor);

            JScrollPane scrollPane = new JScrollPane(textPane);
            scrollPane.setBorder(null);
            scrollPane.setOpaque(true);
            scrollPane.setBackground(panelBackground);
            scrollPane.getViewport().setOpaque(true);
            scrollPane.getViewport().setBackground(panelBackground);
            scrollPane.setPreferredSize(new java.awt.Dimension(340, 260));
            scrollPane.setMinimumSize(new java.awt.Dimension(340, 260));
            hBox.add(scrollPane);
        }

        hBox.add(Box.createHorizontalStrut(10));
        {
            Box vBox = Box.createVerticalBox();
            hBox.add(vBox);
            vBox.add(Box.createVerticalGlue());
            ImagePanel imagePanel = new ImagePanel();
            Image image = ImageResizer.resizeToNewWidth(SOE, 150);
            imagePanel.setImage(image);
            imagePanel.setMinimumSize(imagePanel.getPreferredSize());
            imagePanel.repaint();
            imagePanel.setBackground(panelBackground);
            vBox.add(imagePanel);
            vBox.add(Box.createVerticalGlue());
        }

        this.pack();
        this.setLocationRelativeTo(facade.getMainPanel());
    }

    @Override
    protected void buttonOKPerformed() {
        this.dispose();
    }

    @Override
    protected void buttonCancelPerformed() {
        this.dispose();
    }

    @Override
    protected void menuExitPerformed() {
        this.dispose();
    }

    private Color resolvePanelBackground() {
        Color bg = UIManager.getColor("Panel.background");
        return bg != null ? bg : UiColors.surface();
    }

    private Color resolveTextColor() {
        Color fg = UIManager.getColor("Label.foreground");
        return fg != null ? fg : UiColors.textPrimary();
    }

    private static String toCssColor(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }
}

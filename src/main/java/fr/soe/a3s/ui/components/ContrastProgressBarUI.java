package fr.soe.a3s.ui.components;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;

import javax.swing.JComponent;
import javax.swing.JProgressBar;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicProgressBarUI;

import fr.soe.a3s.ui.UiColors;
import fr.soe.a3s.ui.ThemeManager;

/**
 * ProgressBar UI that paints the percentage text with a halo so it stays readable on
 * both the track color and the filled portion.
 */
public class ContrastProgressBarUI extends BasicProgressBarUI {

    public static ComponentUI createUI(JComponent c) {
        return new ContrastProgressBarUI();
    }

    @Override
    protected void paintString(Graphics g, int x, int y, int width, int height, int amountFull, Insets b) {
        if (!(g instanceof Graphics2D) || !progressBar.isStringPainted()) {
            super.paintString(g, x, y, width, height, amountFull, b);
            return;
        }

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setFont(progressBar.getFont());
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        String progressString = progressBar.getString();
        FontMetrics fm = g2.getFontMetrics();

        if (progressBar.getOrientation() == JProgressBar.HORIZONTAL) {
            int stringWidth = fm.stringWidth(progressString);
            int stringHeight = fm.getAscent();
            int stringX = x + Math.round((width - stringWidth) / 2f);
            int stringY = y + ((height + stringHeight) / 2) - 2;
            paintHaloText(g2, progressString, stringX, stringY);
        } else {
            // Fallback to the default behaviour for vertical bars
            super.paintString(g, x, y, width, height, amountFull, b);
        }

        g2.dispose();
    }

    private void paintHaloText(Graphics2D g2, String text, int x, int y) {
        Color textColor = UiColors.textPrimary();
        Color haloColor = ThemeManager.isDark() ? new Color(0, 0, 0, 170) : new Color(255, 255, 255, 170);

        g2.setColor(haloColor);
        g2.drawString(text, x + 1, y);
        g2.drawString(text, x, y + 1);
        g2.drawString(text, x + 1, y + 1);

        g2.setColor(textColor);
        g2.drawString(text, x, y);
    }
}

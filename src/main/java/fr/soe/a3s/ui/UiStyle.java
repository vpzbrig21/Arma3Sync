package fr.soe.a3s.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JProgressBar;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeListener;

public final class UiStyle {

    public enum ProgressTone {
        SUCCESS, INFO, WARNING
    }

    private static final String SECTION_TITLE_KEY = "a3s.sectionTitle";
    private static final String PANEL_BORDER_KEY = "a3s.panelBorder";
    private static final String PROGRESS_TONE_KEY = "a3s.progressTone";
    private static final String PROGRESS_LISTENER_KEY = "a3s.progressListener";
    private static final int PROGRESS_HEIGHT = 24;

    private UiStyle() {
    }

    public static void applySectionBorder(JComponent component, String title) {
        component.setBorder(sectionBorder(title));
        component.putClientProperty(SECTION_TITLE_KEY, title);
    }

    public static void applyPanelBorder(JComponent component) {
        component.setBorder(panelBorder());
        component.putClientProperty(PANEL_BORDER_KEY, Boolean.TRUE);
    }

    public static Border sectionBorder(String title) {
        LineBorder lineBorder = new LineBorder(borderColor(), 1, true);
        TitledBorder titledBorder = BorderFactory.createTitledBorder(lineBorder, title, TitledBorder.LEFT,
                TitledBorder.TOP, UIManager.getFont("Label.font"), titleColor());
        return BorderFactory.createCompoundBorder(titledBorder, BorderFactory.createEmptyBorder(8, 12, 12, 12));
    }

    public static Border panelBorder() {
        return BorderFactory.createCompoundBorder(new LineBorder(borderColor(), 1, true),
                BorderFactory.createEmptyBorder(8, 10, 10, 10));
    }

    public static void styleCard(JComponent component) {
        applyPanelBorder(component);
    }

    public static void styleProgressBar(JProgressBar bar, ProgressTone tone) {
        bar.setStringPainted(true);
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder());
        bar.setBackground(trackColor());
        bar.setForeground(progressColor(tone));
        Dimension preferred = bar.getPreferredSize();
        if (preferred == null) {
            preferred = new Dimension(0, PROGRESS_HEIGHT);
        }
        bar.setPreferredSize(new Dimension(preferred.width, PROGRESS_HEIGHT));
        bar.putClientProperty(PROGRESS_TONE_KEY, tone);
        bindProgressString(bar);
    }

    public static void refreshTree(Container container) {
        if (container == null) {
            return;
        }
        restyleComponent(container);
        for (Component child : container.getComponents()) {
            restyleComponent(child);
            if (child instanceof Container) {
                refreshTree((Container) child);
            }
        }
    }

    private static void restyleComponent(Component component) {
        if (!(component instanceof JComponent)) {
            return;
        }
        JComponent jc = (JComponent) component;
        Object sectionTitle = jc.getClientProperty(SECTION_TITLE_KEY);
        if (sectionTitle instanceof String) {
            jc.setBorder(sectionBorder((String) sectionTitle));
        } else if (Boolean.TRUE.equals(jc.getClientProperty(PANEL_BORDER_KEY))) {
            jc.setBorder(panelBorder());
        }
        Object tone = jc.getClientProperty(PROGRESS_TONE_KEY);
        if (tone instanceof ProgressTone && jc instanceof JProgressBar) {
            styleProgressBar((JProgressBar) jc, (ProgressTone) tone);
        }
    }

    private static Color borderColor() {
        return ThemeManager.isDark() ? new Color(0x454B57) : new Color(0xDFE4EF);
    }

    private static Color trackColor() {
        return ThemeManager.isDark() ? new Color(0x2A2F38) : new Color(0xF6F8FC);
    }

    private static Color titleColor() {
        return ThemeManager.isDark() ? new Color(0xF1F4FA) : new Color(0x1E232C);
    }

    private static Color progressColor(ProgressTone tone) {
        boolean dark = ThemeManager.isDark();
        switch (tone) {
        case SUCCESS:
            return dark ? new Color(0x45C18E) : new Color(0x58C48E);
        case WARNING:
            return dark ? new Color(0xF29E4C) : new Color(0xF6B869);
        case INFO:
            return dark ? new Color(0x4F8DFF) : new Color(0x6AA8FF);
        default:
            throw new IllegalArgumentException("Unsupported tone: " + tone);
        }
    }

    private static void bindProgressString(JProgressBar bar) {
        Object existing = bar.getClientProperty(PROGRESS_LISTENER_KEY);
        if (existing instanceof ChangeListener) {
            bar.getModel().removeChangeListener((ChangeListener) existing);
        }
        ChangeListener listener = e -> updateProgressString(bar);
        bar.getModel().addChangeListener(listener);
        bar.putClientProperty(PROGRESS_LISTENER_KEY, listener);
        updateProgressString(bar);
    }

    private static void updateProgressString(JProgressBar bar) {
        int min = bar.getMinimum();
        int max = bar.getMaximum();
        int value = bar.getValue();
        int range = max - min;
        int percent = range <= 0 ? 0 : Math.round(((value - min) * 100f) / range);
        bar.setString(percent + "%");
    }
}

package fr.soe.a3s.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.util.function.Supplier;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JProgressBar;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeListener;

import fr.soe.a3s.ui.components.ContrastProgressBarUI;
import fr.soe.a3s.ui.theme.ThemeMetrics;
import fr.soe.a3s.ui.theme.ThemeTokens;

public final class UiStyle {

    public enum ProgressTone {
        SUCCESS, INFO, WARNING, DANGER
    }

    private static final String SECTION_TITLE_KEY = "a3s.sectionTitle";
    private static final String PANEL_BORDER_KEY = "a3s.panelBorder";
    private static final String PROGRESS_TONE_KEY = "a3s.progressTone";
    private static final String PROGRESS_LISTENER_KEY = "a3s.progressListener";
    private static final String TOOLBAR_KEY = "a3s.toolbar";
    private static final String STATUS_BAR_KEY = "a3s.statusBar";
    private static final String BACKGROUND_SUPPLIER_KEY = "a3s.backgroundSupplier";
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
        ThemeMetrics metrics = ThemeTokens.metrics();
        int vGap = metrics.spacingMd();
        int hGap = metrics.spacingLg();
        return BorderFactory.createCompoundBorder(titledBorder,
                BorderFactory.createEmptyBorder(vGap, hGap, hGap, hGap));
    }

    public static Border panelBorder() {
        ThemeMetrics metrics = ThemeTokens.metrics();
        int paddingTop = metrics.spacingSm();
        int paddingSides = metrics.spacingMd();
        return BorderFactory.createCompoundBorder(new LineBorder(borderColor(), 1, true),
                BorderFactory.createEmptyBorder(paddingTop, paddingSides, paddingSides, paddingSides));
    }

    public static void styleProgressBar(JProgressBar bar, ProgressTone tone) {
        bar.setStringPainted(true);
        bar.setUI(new ContrastProgressBarUI());
        bar.setOpaque(false);
        bar.setBorder(progressBorder());
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
        if (Boolean.TRUE.equals(jc.getClientProperty(TOOLBAR_KEY))) {
            styleToolbar(jc);
        }
        if (Boolean.TRUE.equals(jc.getClientProperty(STATUS_BAR_KEY))) {
            styleStatusBar(jc);
        }
        Object bgSupplier = jc.getClientProperty(BACKGROUND_SUPPLIER_KEY);
        if (bgSupplier instanceof Supplier) {
            @SuppressWarnings("unchecked")
            Supplier<Color> supplier = (Supplier<Color>) bgSupplier;
            jc.setBackground(supplier.get());
        }
    }

    private static Color borderColor() {
        return UiColors.border();
    }

    private static Color trackColor() {
        return UiColors.surfaceMuted();
    }

    private static Color titleColor() {
        return UiColors.textPrimary();
    }

    private static Color progressColor(ProgressTone tone) {
        switch (tone) {
        case SUCCESS:
            return UiColors.statusSuccess();
        case WARNING:
            return UiColors.statusWarning();
        case INFO:
            return UiColors.statusInfo();
        case DANGER:
            return UiColors.statusDanger();
        default:
            throw new IllegalArgumentException("Unsupported tone: " + tone);
        }
    }

    private static Border progressBorder() {
        ThemeMetrics metrics = ThemeTokens.metrics();
        return BorderFactory.createCompoundBorder(new LineBorder(UiColors.borderMuted(), 1, true),
                BorderFactory.createEmptyBorder(metrics.spacingXs(), metrics.spacingSm(), metrics.spacingXs(),
                        metrics.spacingSm()));
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

    public static void styleToolbar(JComponent component) {
        ThemeMetrics metrics = ThemeTokens.metrics();
        component.setBorder(BorderFactory.createCompoundBorder(new MatteBorder(0, 0, 1, 0, UiColors.border()),
                BorderFactory.createEmptyBorder(metrics.spacingSm(), metrics.spacingLg(), metrics.spacingSm(),
                        metrics.spacingLg())));
        component.setBackground(UiColors.surface());
        component.putClientProperty(TOOLBAR_KEY, Boolean.TRUE);
    }

    public static void styleStatusBar(JComponent component) {
        ThemeMetrics metrics = ThemeTokens.metrics();
        component.setBorder(BorderFactory.createCompoundBorder(new MatteBorder(1, 0, 0, 0, UiColors.border()),
                BorderFactory.createEmptyBorder(metrics.spacingSm(), metrics.spacingLg(), metrics.spacingSm(),
                        metrics.spacingLg())));
        component.setBackground(UiColors.surfaceVariant());
        component.putClientProperty(STATUS_BAR_KEY, Boolean.TRUE);
    }

    public static void applyStatusForeground(JComponent component, ProgressTone tone) {
        if (component != null) {
            component.setForeground(statusColor(tone));
        }
    }

    public static Color statusColor(ProgressTone tone) {
        return progressColor(tone);
    }

    public static void bindBackground(JComponent component, Supplier<Color> supplier) {
        if (component == null || supplier == null) {
            return;
        }
        component.putClientProperty(BACKGROUND_SUPPLIER_KEY, supplier);
        component.setBackground(supplier.get());
    }
}

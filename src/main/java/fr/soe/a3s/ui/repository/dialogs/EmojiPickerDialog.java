package fr.soe.a3s.ui.repository.dialogs;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Dialog.ModalityType;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.ImageIcon;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/** Searchable picker for the Unicode emoji sequences bundled with the application. */
public final class EmojiPickerDialog {

	private static final Pattern EMOJI_LINE = Pattern.compile(
			"^([0-9A-F ]+)\\s*;\\s*(?:fully-qualified|component)\\s*#\\s*\\S+\\s+E[0-9.]+\\s+(.+)$");
	private static final int MAX_CACHED_ICONS = 256;
	private static final Map<String, ImageIcon> TWEMOJI_ICON_CACHE = new LinkedHashMap<String, ImageIcon>(
			MAX_CACHED_ICONS, 0.75f, true) {
		private static final long serialVersionUID = 1L;
		@Override protected boolean removeEldestEntry(Map.Entry<String, ImageIcon> eldest) {
			return size() > MAX_CACHED_ICONS;
		}
	};
	private static final Set<String> MISSING_TWEMOJI_ASSETS = new HashSet<String>();
	private static volatile List<EmojiEntry> emojiEntries;

	private EmojiPickerDialog() {
	}

	/** Returns the chosen Unicode emoji, or {@code null} when the dialog is cancelled. */
	public static String chooseEmoji(Component parent, String currentEmoji) {
		List<EmojiEntry> entries = getEmojiEntries();
		Window owner = parent == null ? null : javax.swing.SwingUtilities.getWindowAncestor(parent);
		JDialog dialog = new JDialog(owner, "Choose Unicode Emoji", ModalityType.APPLICATION_MODAL);
		dialog.setLayout(new BorderLayout(8, 8));
		dialog.getRootPane().setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		JTextField search = new JTextField();
		search.putClientProperty("JTextField.placeholderText", "Search emoji names");
		JComboBox<String> category = new JComboBox<String>();
		category.addItem("All categories");
		Set<String> groups = new LinkedHashSet<String>();
		for (EmojiEntry entry : entries) groups.add(entry.group);
		for (String group : groups) category.addItem(group);
		JPanel filters = new JPanel(new GridLayout(1, 2, 8, 0));
		filters.add(search);
		filters.add(category);
		dialog.add(filters, BorderLayout.NORTH);

		DefaultListModel<EmojiEntry> model = new DefaultListModel<EmojiEntry>();
		JList<EmojiEntry> list = new JList<EmojiEntry>(model);
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.setLayoutOrientation(JList.HORIZONTAL_WRAP);
		list.setVisibleRowCount(0);
		list.setFixedCellWidth(148);
		list.setFixedCellHeight(54);
		list.setCellRenderer(new EmojiCellRenderer());
		JScrollPane scroll = new JScrollPane(list);
		scroll.setPreferredSize(new Dimension(830, 490));
		dialog.add(scroll, BorderLayout.CENTER);

		JLabel preview = new JLabel("Select an emoji to preview it", SwingConstants.CENTER);
		preview.setFont(emojiFont(28));
		preview.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		JPanel buttons = new JPanel(new GridLayout(1, 2, 8, 0));
		JButton choose = new JButton("Use emoji");
		JButton cancel = new JButton("Cancel");
		choose.setEnabled(false);
		buttons.add(choose);
		buttons.add(cancel);
		JLabel customHint = new JLabel("Color previews use Twemoji artwork when available. Custom Discord emojis can still be typed.");
		JPanel bottom = new JPanel(new BorderLayout(6, 4));
		bottom.add(preview, BorderLayout.CENTER);
		bottom.add(buttons, BorderLayout.EAST);
		bottom.add(customHint, BorderLayout.SOUTH);
		dialog.add(bottom, BorderLayout.SOUTH);

		Runnable updateList = () -> {
			model.clear();
			String query = search.getText().trim().toLowerCase(Locale.ROOT);
			String selectedGroup = (String) category.getSelectedItem();
			for (EmojiEntry entry : entries) {
				if (selectedGroup != null && !"All categories".equals(selectedGroup)
						&& !selectedGroup.equals(entry.group)) continue;
				if (!query.isEmpty() && !entry.name.toLowerCase(Locale.ROOT).contains(query)
						&& !entry.emoji.contains(query)) continue;
				model.addElement(entry);
			}
			if (!model.isEmpty()) list.setSelectedIndex(0);
		};
		search.getDocument().addDocumentListener(new DocumentListener() {
			@Override public void insertUpdate(DocumentEvent event) { updateList.run(); }
			@Override public void removeUpdate(DocumentEvent event) { updateList.run(); }
			@Override public void changedUpdate(DocumentEvent event) { updateList.run(); }
		});
		category.addActionListener(event -> updateList.run());
		list.addListSelectionListener(event -> {
			EmojiEntry selected = list.getSelectedValue();
			choose.setEnabled(selected != null);
			if (selected != null) {
				Icon icon = twemojiIcon(selected.emoji, 40);
				preview.setIcon(icon);
				preview.setIconTextGap(10);
				preview.setText((icon == null ? selected.emoji + "   " : "") + selected.name);
			} else {
				preview.setIcon(null);
				preview.setText("Select an emoji to preview it");
			}
		});

		String[] result = new String[1];
		choose.addActionListener(event -> {
			EmojiEntry selected = list.getSelectedValue();
			if (selected != null) {
				result[0] = selected.emoji;
				dialog.dispose();
			}
		});
		cancel.addActionListener(event -> dialog.dispose());
		list.addMouseListener(new java.awt.event.MouseAdapter() {
			@Override
			public void mouseClicked(java.awt.event.MouseEvent event) {
				if (event.getClickCount() == 2 && list.getSelectedValue() != null) choose.doClick();
			}
		});
		updateList.run();
		if (currentEmoji != null && !currentEmoji.isBlank()) {
			for (int index = 0; index < model.size(); index++) {
				if (currentEmoji.equals(model.get(index).emoji)) {
					list.setSelectedIndex(index);
					list.ensureIndexIsVisible(index);
					break;
				}
			}
		}
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		dialog.pack();
		dialog.setMinimumSize(new Dimension(760, 560));
		dialog.setLocationRelativeTo(parent);
		dialog.setVisible(true);
		return result[0];
	}

	private static List<EmojiEntry> getEmojiEntries() {
		List<EmojiEntry> cached = emojiEntries;
		if (cached != null) return cached;
		synchronized (EmojiPickerDialog.class) {
			if (emojiEntries != null) return emojiEntries;
			List<EmojiEntry> loaded = new ArrayList<EmojiEntry>();
			String group = "Emoji";
			try (InputStream stream = EmojiPickerDialog.class.getResourceAsStream("emoji-test.txt")) {
				if (stream == null) throw new IOException("Unicode emoji data was not found.");
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
					String line;
					while ((line = reader.readLine()) != null) {
						if (line.startsWith("# group: ")) {
							group = line.substring("# group: ".length()).trim();
							continue;
						}
						Matcher matcher = EMOJI_LINE.matcher(line);
						if (!matcher.matches()) continue;
						StringBuilder emoji = new StringBuilder();
						for (String codePoint : matcher.group(1).trim().split("\\s+")) {
							emoji.appendCodePoint(Integer.parseInt(codePoint, 16));
						}
						loaded.add(new EmojiEntry(emoji.toString(), matcher.group(2).trim(), group));
					}
				}
			} catch (IOException | NumberFormatException exception) {
				throw new IllegalStateException("Could not load the Unicode emoji list.", exception);
			}
			emojiEntries = List.copyOf(loaded);
			return emojiEntries;
		}
	}

	static Font emojiFont(int size) {
		for (String family : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()) {
			if ("Segoe UI Emoji".equalsIgnoreCase(family) || "Noto Color Emoji".equalsIgnoreCase(family)) {
				return new Font(family, Font.PLAIN, size);
			}
		}
		return new Font(Font.SANS_SERIF, Font.PLAIN, size);
	}

	private static String twemojiSlug(String emoji) {
		StringBuilder slug = new StringBuilder();
		boolean isZwjSequence = emoji.indexOf('\u200D') >= 0;
		for (int offset = 0; offset < emoji.length();) {
			int codePoint = emoji.codePointAt(offset);
			offset += Character.charCount(codePoint);
			// Twemoji omits the presentation selector for standalone/keycap assets,
			// but retains it when it is part of a joined sequence.
			if (codePoint == 0xFE0F && !isZwjSequence) continue;
			if (slug.length() > 0) slug.append('-');
			slug.append(Integer.toHexString(codePoint));
		}
		return slug.toString();
	}

	private static synchronized ImageIcon twemojiIcon(String emoji, int size) {
		String slug = twemojiSlug(emoji);
		if (slug.isEmpty() || MISSING_TWEMOJI_ASSETS.contains(slug)) return null;
		String cacheKey = slug + '@' + size;
		ImageIcon cached = TWEMOJI_ICON_CACHE.get(cacheKey);
		if (cached != null) return cached;
		String resourcePath = "twemoji/72x72/" + slug + ".png";
		try (InputStream stream = EmojiPickerDialog.class.getResourceAsStream(resourcePath)) {
			if (stream == null) {
				MISSING_TWEMOJI_ASSETS.add(slug);
				return null;
			}
			BufferedImage source = javax.imageio.ImageIO.read(stream);
			if (source == null) {
				MISSING_TWEMOJI_ASSETS.add(slug);
				return null;
			}
			BufferedImage scaled = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
			java.awt.Graphics2D graphics = scaled.createGraphics();
			try {
				graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
						RenderingHints.VALUE_INTERPOLATION_BICUBIC);
				graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
						RenderingHints.VALUE_ANTIALIAS_ON);
				graphics.drawImage(source, 0, 0, size, size, null);
			} finally {
				graphics.dispose();
			}
			ImageIcon icon = new ImageIcon(scaled);
			TWEMOJI_ICON_CACHE.put(cacheKey, icon);
			return icon;
		} catch (IOException exception) {
			MISSING_TWEMOJI_ASSETS.add(slug);
			return null;
		}
	}

	private static final class EmojiEntry {
		private final String emoji;
		private final String name;
		private final String group;

		private EmojiEntry(String emoji, String name, String group) {
			this.emoji = emoji;
			this.name = name;
			this.group = group;
		}
	}

	private static final class EmojiCellRenderer extends DefaultListCellRenderer {
		private final Font emojiFont = emojiFont(20);

		@Override
		public Component getListCellRendererComponent(JList<?> list, Object value, int index,
				boolean isSelected, boolean cellHasFocus) {
			JPanel cell = new JPanel(new BorderLayout(8, 0));
			EmojiEntry entry = (EmojiEntry) value;
			JLabel glyph = new JLabel();
			glyph.setHorizontalAlignment(SwingConstants.CENTER);
			glyph.setVerticalAlignment(SwingConstants.CENTER);
			glyph.setFont(emojiFont);
			glyph.setPreferredSize(new Dimension(38, 42));
			Icon icon = twemojiIcon(entry.emoji, 32);
			if (icon != null) {
				glyph.setIcon(icon);
			} else {
				glyph.setText(entry.emoji);
			}
			JLabel name = new JLabel("<html><div style='width:95px'>" + escapeHtml(entry.name) + "</div></html>");
			cell.add(glyph, BorderLayout.WEST);
			cell.add(name, BorderLayout.CENTER);
			java.awt.Color background = isSelected ? list.getSelectionBackground() : list.getBackground();
			java.awt.Color foreground = isSelected ? list.getSelectionForeground() : list.getForeground();
			cell.setBackground(background);
			glyph.setForeground(foreground);
			name.setForeground(foreground);
			cell.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
			return cell;
		}

		private String escapeHtml(String text) {
			return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
		}
	}
}

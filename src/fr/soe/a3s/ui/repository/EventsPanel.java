package fr.soe.a3s.ui.repository;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import fr.soe.a3s.constant.GameDLCs;
import fr.soe.a3s.domain.Addon;
import fr.soe.a3s.dto.EventDTO;
import fr.soe.a3s.dto.TreeDirectoryDTO;
import fr.soe.a3s.dto.TreeLeafDTO;
import fr.soe.a3s.dto.TreeNodeDTO;
import fr.soe.a3s.exception.WritingException;
import fr.soe.a3s.exception.repository.RepositoryException;
import fr.soe.a3s.exception.repository.RepositoryNotFoundException;
import fr.soe.a3s.service.RepositoryService;
import fr.soe.a3s.ui.Facade;
import fr.soe.a3s.ui.ImageResizer;
import fr.soe.a3s.ui.UIConstants;
import fr.soe.a3s.ui.main.tree.AddonTreeModel;
import fr.soe.a3s.ui.main.tree.CheckTreeCellRenderer;
import fr.soe.a3s.ui.main.tree.MyRenderer;
import fr.soe.a3s.ui.repository.dialogs.connection.UploadEventsConnectionDialog;
import fr.soe.a3s.ui.repository.dialogs.progress.ProgressUploadEventsDialog;
import fr.soe.a3s.ui.repository.events.EventAddDialog;
import fr.soe.a3s.ui.repository.events.EventRenameDialog;

public class EventsPanel extends JPanel implements UIConstants {

	private static final String TAB_TITLE_AVAILABLE_ADDONS = "Repository Addons";
	private static final String TAB_TITLE_AVAILABLE_DLC = "DLC";

	private Facade facade;
	private RepositoryPanel repositoryPanel;
	private JTree arbre, arbreDLC;
	private TreePath arbreTreePath, arbreDLCTreePath;
	private JPopupMenu popup;
	private JScrollPane listScrollPanel;
	private JScrollPane arbreScrollPane, arbreDLCScrollPane;
	private JList listEvents;
	private JButton buttonNew, buttonRemove, buttonEdit, buttonDuplicate, buttonUpload, buttonSaveToDisk,
			buttonUploadOptions;
	private JMenuItem menuItemSetRequired, menuItemSetOptional;
	private JCheckBox checkBoxSelectAll, checkBoxExpandAll;
	private JTabbedPane tabbedPane;

	// Data
	private String repositoryName;
	private TreeDirectoryDTO racine, racineDLC;
	private List<EventDTO> eventDTOs;

	// Services
	private final RepositoryService repositoryService = new RepositoryService();

	public EventsPanel(Facade facade, RepositoryPanel repositoryPanel) {

		this.facade = facade;
		this.repositoryPanel = repositoryPanel;
		setLayout(new BorderLayout());

		Box vertBox1 = Box.createVerticalBox();
		vertBox1.add(Box.createVerticalStrut(5));
		this.add(vertBox1, BorderLayout.CENTER);
		{
			JPanel listEventsPanel = new JPanel();
			listEventsPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Edition"));
			listEventsPanel.setLayout(new BorderLayout());
			{
				Box hBox = Box.createHorizontalBox();
				listEventsPanel.add(hBox, BorderLayout.NORTH);
				buttonNew = new JButton("");
				ImageIcon addIcon = new ImageIcon(ImageResizer.resizeToScreenResolution(ADD));
				buttonNew.setIcon(addIcon);
				hBox.add(buttonNew);
				buttonEdit = new JButton("");
				ImageIcon editIcon = new ImageIcon(ImageResizer.resizeToScreenResolution(EDIT));
				buttonEdit.setIcon(editIcon);
				hBox.add(buttonEdit);
				buttonDuplicate = new JButton("");
				ImageIcon duplicateIcon = new ImageIcon(ImageResizer.resizeToScreenResolution(DUPLICATE));
				buttonDuplicate.setIcon(duplicateIcon);
				hBox.add(buttonDuplicate);
				buttonRemove = new JButton("");
				ImageIcon deleteIcon = new ImageIcon(ImageResizer.resizeToScreenResolution(DELETE));
				buttonRemove.setIcon(deleteIcon);
				hBox.add(buttonRemove);
				buttonUpload = new JButton("");
				ImageIcon saveUploadIcon = new ImageIcon(ImageResizer.resizeToScreenResolution(UPLOAD));
				buttonUpload.setIcon(saveUploadIcon);
				hBox.add(buttonUpload);
				buttonUploadOptions = new JButton();
				ImageIcon uploadOptionIcon = new ImageIcon(ImageResizer.resizeToScreenResolution(PREFERENCES));
				buttonUploadOptions.setIcon(uploadOptionIcon);
				hBox.add(buttonUploadOptions);
				buttonSaveToDisk = new JButton("");
				ImageIcon saveToDiskIcon = new ImageIcon(ImageResizer.resizeToScreenResolution(SAVE));
				buttonSaveToDisk.setIcon(saveToDiskIcon);
				hBox.add(buttonSaveToDisk);
			}
			{
				listEvents = new JList();
				listEvents.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
				listScrollPanel = new JScrollPane(listEvents);
				listScrollPanel.setBorder(BorderFactory.createEtchedBorder(BevelBorder.LOWERED));
				listEventsPanel.add(listScrollPanel, BorderLayout.CENTER);
			}

			JPanel addonsSelectionPanel = new JPanel();
			addonsSelectionPanel
					.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Selection"));
			addonsSelectionPanel.setLayout(new BorderLayout());
			{
				{
					Box hBox = Box.createHorizontalBox();
					checkBoxSelectAll = new JCheckBox("Select All");
					checkBoxSelectAll.setFocusable(false);
					hBox.add(checkBoxSelectAll);
					checkBoxExpandAll = new JCheckBox("Expand All");
					checkBoxExpandAll.setFocusable(false);
					hBox.add(checkBoxExpandAll);
					addonsSelectionPanel.add(hBox, BorderLayout.NORTH);
				}
				{
					{
						AddonTreeModel addonTreeModel = new AddonTreeModel(racine);
						arbre = new JTree(addonTreeModel);
						arbre.setLargeModel(true);
						arbre.setRootVisible(false);
						arbre.setEditable(false);
						arbre.setLargeModel(true);
						arbre.setShowsRootHandles(true);
						arbre.setToggleClickCount(0);
						arbre.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

						Font fontArbre = UIManager.getFont("Tree.font");
						FontMetrics metrics = arbre.getFontMetrics(fontArbre);
						int fontHeight = metrics.getAscent() + metrics.getDescent() + metrics.getLeading();
						arbre.setRowHeight(fontHeight);

						MyRenderer myRenderer = new MyRenderer();
						CheckTreeCellRenderer renderer = new CheckTreeCellRenderer(myRenderer);
						arbre.setCellRenderer(renderer);

						arbreScrollPane = new JScrollPane(arbre);
						arbreScrollPane.setBorder(BorderFactory.createEmptyBorder());
					}
					{
						AddonTreeModel addonTreeModel = new AddonTreeModel(racineDLC);
						arbreDLC = new JTree(addonTreeModel);
						arbreDLC.setRootVisible(false);
						arbreDLC.setEditable(false);
						arbreDLC.setLargeModel(true);
						arbreDLC.setShowsRootHandles(true);
						arbreDLC.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

						Font fontArbre = UIManager.getFont("Tree.font");
						FontMetrics metrics = arbreDLC.getFontMetrics(fontArbre);
						int fontHeight = metrics.getAscent() + metrics.getDescent() + metrics.getLeading();
						arbreDLC.setRowHeight(fontHeight);

						MyRenderer myRenderer = new MyRenderer();
						CheckTreeCellRenderer renderer = new CheckTreeCellRenderer(myRenderer);
						arbreDLC.setCellRenderer(renderer);

						arbreDLCScrollPane = new JScrollPane(arbreDLC);
						arbreDLCScrollPane.setBorder(BorderFactory.createEmptyBorder());
					}
					tabbedPane = new JTabbedPane();
					tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
					tabbedPane.addTab(TAB_TITLE_AVAILABLE_ADDONS, arbreScrollPane);
					tabbedPane.addTab(TAB_TITLE_AVAILABLE_DLC, arbreDLCScrollPane);
					tabbedPane.setFocusable(false);
					addonsSelectionPanel.add(tabbedPane, BorderLayout.CENTER);
				}
			}
			JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, listEventsPanel, addonsSelectionPanel);
			splitPane.setOneTouchExpandable(true);
			splitPane.setDividerLocation(200);
			flattenSplitPane(splitPane);
			vertBox1.add(splitPane);
		}

		/* Right click menu */
		popup = new JPopupMenu();

		menuItemSetRequired = new JMenuItem("Set reqired");
		menuItemSetRequired.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {
				popupActionPerformed(evt);
			}
		});
		menuItemSetRequired.setActionCommand("Set reqired");
		popup.add(menuItemSetRequired);

		menuItemSetOptional = new JMenuItem("Set optional");
		menuItemSetOptional.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {
				popupActionPerformed(evt);
			}
		});
		menuItemSetOptional.setActionCommand("Set optional");
		popup.add(menuItemSetOptional);

		popup.addPopupMenuListener(new PopupMenuListener() {

			@Override
			public void popupMenuCanceled(PopupMenuEvent arg0) {
			}

			@Override
			public void popupMenuWillBecomeInvisible(PopupMenuEvent arg0) {
			}

			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent arg0) {

				TreeNodeDTO[] nodes = getSelectedNode();
				menuItemSetRequired.setEnabled(false);
				menuItemSetOptional.setEnabled(false);
				int index = listEvents.getSelectedIndex();
				if (nodes != null && index != -1) {
					if (nodes.length > 0) {
						if (nodes[0].isLeaf()) {
							menuItemSetRequired.setEnabled(true);
							menuItemSetOptional.setEnabled(true);
						}
					}
				}
			}
		});
		buttonNew.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				buttonNewPerformed();
			}
		});
		buttonEdit.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				buttonEditPerformed();
			}
		});
		buttonDuplicate.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				buttonDuplicatePerformed();
			}
		});
		buttonRemove.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				buttonRemovePerformed();
			}
		});
		buttonUpload.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				buttonUploadPerformed();
			}
		});
		buttonUploadOptions.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				buttonUploadOptionsPerformed();
			}
		});
		buttonSaveToDisk.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				buttonSaveToDiskPerformed();
			}
		});
		listEvents.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent event) {
				boolean adjust = event.getValueIsAdjusting();
				if (!adjust) {
					eventSelectionPerformed();
				}
			}
		});
		checkBoxSelectAll.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				checkBoxSelectAllPerformed();
			}
		});
		checkBoxExpandAll.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				checkBoxExpandAllPerformed();
			}
		});
		arbre.addTreeSelectionListener(new TreeSelectionListener() {
			@Override
			public void valueChanged(TreeSelectionEvent arg0) {
				arbreTreePath = arbre.getSelectionPath();
			}
		});
		arbre.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (arbreTreePath == null) {
					return;
				}
				int hotspot = new JCheckBox().getPreferredSize().width;
				TreePath path = arbre.getPathForLocation(e.getX(), e.getY());
				if (path == null) {
					return;
				} else if (e.getX() > arbre.getPathBounds(path).x + hotspot) {
					return;
				}
				addonSelectionPerformed();
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				if (e.isPopupTrigger()) {
					popup.show((JComponent) e.getSource(), e.getX(), e.getY());
				} else if (SwingUtilities.isRightMouseButton(e)) {
					popup.show((JComponent) e.getSource(), e.getX(), e.getY());
				}
			}
		});
		arbre.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent evt) {
				if (evt.getKeyCode() == evt.VK_SPACE) {
					addonSelectionPerformed();
				}
			}
		});
		arbre.addTreeExpansionListener(new TreeExpansionListener() {
			@Override
			public void treeExpanded(TreeExpansionEvent event) {
				onArbreExpanded(event.getPath());
			}

			@Override
			public void treeCollapsed(TreeExpansionEvent event) {
				onArbreCollapsed(event.getPath());
			}
		});
		arbreDLC.addTreeSelectionListener(new TreeSelectionListener() {
			@Override
			public void valueChanged(TreeSelectionEvent arg0) {
				arbreDLCTreePath = arbreDLC.getSelectionPath();
			}
		});
		arbreDLC.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (arbreDLCTreePath == null) {
					return;
				}
				int hotspot = new JCheckBox().getPreferredSize().width;
				TreePath path = arbreDLC.getPathForLocation(e.getX(), e.getY());
				if (path == null) {
					return;
				} else if (e.getX() > arbreDLC.getPathBounds(path).x + hotspot) {
					return;
				}
				dlcSelectionPerformed();
			}
		});
		arbreDLC.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent evt) {
				if (evt.getKeyCode() == evt.VK_SPACE) {
					dlcSelectionPerformed();
				}
			}
		});
		tabbedPane.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				int index = tabbedPane.getSelectedIndex();
				checkBoxSelectAll.setEnabled(false);
				checkBoxExpandAll.setEnabled(false);
				if (index == 0) {
					int index2 = listEvents.getSelectedIndex();
					if (index2 != -1) {
						if (racine != null) {
							checkBoxSelectAll.setEnabled(true);
							checkBoxExpandAll.setEnabled(true);
						}
					}
				}
			}
		});
		setContextualHelp();
	}

	private void setContextualHelp() {

		buttonNew.setToolTipText("Add a new event");
		buttonEdit.setToolTipText("Edit event");
		buttonDuplicate.setToolTipText("Duplicate event");
		buttonRemove.setToolTipText("Remove event");
		buttonUpload.setToolTipText("Upload events informations");
		buttonUploadOptions.setToolTipText("Set upload options");
		buttonSaveToDisk.setToolTipText("Save to disk (local repository)");
	}

	public void init(String repositoryName) {

		this.repositoryName = repositoryName;

		// Update list of Events
		updateListEvents();

		// Update list of available addons
		updateListAddons();

		// Update list of available DLC
		updateListDLC();

		// Disable selection
		arbre.setEnabled(false);
		arbreDLC.setEnabled(false);
		checkBoxSelectAll.setEnabled(false);
		checkBoxExpandAll.setEnabled(false);
	}

	public void refreshViewArbre() {

		int numberRowShown = arbre.getRowCount();
		arbre.setVisibleRowCount(numberRowShown);
		arbre.setPreferredSize(arbre.getPreferredScrollableViewportSize());
		arbreScrollPane.repaint();
	}

	public void refreshViewArbreDLC() {

		int numberRowShown = arbreDLC.getRowCount();
		arbreDLC.setVisibleRowCount(numberRowShown);
		arbreDLC.setPreferredSize(arbreDLC.getPreferredScrollableViewportSize());
		arbreDLCScrollPane.repaint();
	}

	private void buttonNewPerformed() {

		listEvents.clearSelection();
		if (racine != null) {
			deselectAllDescending(racine);
		}

		// Disable selection
		arbre.setEnabled(false);

		if (racineDLC != null) {
			deselectAllDescending(racineDLC);
		}

		// Disable selection
		arbreDLC.setEnabled(false);

		checkBoxSelectAll.setEnabled(false);
		checkBoxExpandAll.setEnabled(false);

		EventAddDialog eventEditPanel = new EventAddDialog(facade, repositoryName, this);
		eventEditPanel.setVisible(true);
	}

	private void buttonEditPerformed() {

		int index = listEvents.getSelectedIndex();
		if (index != -1) {
			// Disable selection
			arbre.setEnabled(false);
			arbreDLC.setEnabled(false);
			checkBoxSelectAll.setEnabled(false);
			checkBoxExpandAll.setEnabled(false);

			EventRenameDialog eventRenamePanel = new EventRenameDialog(facade, repositoryName, this);
			eventRenamePanel.init(eventDTOs.get(index).getName(), eventDTOs.get(index).getDescription());
			eventRenamePanel.setVisible(true);

			updateListEvents();
		}
	}

	private void buttonDuplicatePerformed() {

		int index = listEvents.getSelectedIndex();
		if (index != -1) {
			try {
				repositoryService.duplicateEvent(repositoryName, eventDTOs.get(index).getName());
				updateListEvents();
				listEvents.setSelectedIndex(index);
			} catch (RepositoryNotFoundException e) {
				JOptionPane.showMessageDialog(facade.getMainPanel(), e.getMessage(), "Error",
						JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	private void buttonRemovePerformed() {

		int index = listEvents.getSelectedIndex();
		if (index != -1) {
			if (racine != null) {
				deselectAllDescending(racine);
			}
			if (racineDLC != null) {
				deselectAllDescending(racineDLC);
			}

			// Disable selection
			arbre.setEnabled(false);
			arbreDLC.setEnabled(false);
			checkBoxSelectAll.setEnabled(false);
			checkBoxExpandAll.setEnabled(false);

			try {
				repositoryService.removeEvent(repositoryName, eventDTOs.get(index).getName());
				updateListEvents();
			} catch (RepositoryException e) {
				JOptionPane.showMessageDialog(facade.getMainPanel(), e.getMessage(), repositoryName,
						JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	private void buttonUploadPerformed() {

		if (eventDTOs == null) {
			JOptionPane.showMessageDialog(facade.getMainPanel(), "Nothing to upload.", repositoryName,
					JOptionPane.INFORMATION_MESSAGE);
		} else {
			ProgressUploadEventsDialog uploadPanel = new ProgressUploadEventsDialog(facade, repositoryName);
			uploadPanel.setVisible(true);
			uploadPanel.init();
			facade.getMainPanel().updateTabs(OP_REPOSITORY_CHANGED);
		}
	}

	private void buttonUploadOptionsPerformed() {

		UploadEventsConnectionDialog uploadEventsOptionsPanel = new UploadEventsConnectionDialog(facade);
		uploadEventsOptionsPanel.init(repositoryName);
		uploadEventsOptionsPanel.setVisible(true);
	}

	private void buttonSaveToDiskPerformed() {

		// Repository path must be set
		String path = repositoryService.getRepositoryPath(repositoryName);
		if ("".equals(path) || path == null) {
			String message = "Repository main folder location is missing." + "\n"
					+ "Please checkout the Repository Administation panel.";
			JOptionPane.showMessageDialog(facade.getMainPanel(), message, repositoryName, JOptionPane.WARNING_MESSAGE);
		} else if (!(new File(path)).exists()) {
			String message = "Repository main folder location: " + path + " does not exist.";
			JOptionPane.showMessageDialog(facade.getMainPanel(), message, repositoryName, JOptionPane.ERROR_MESSAGE);
		} else {
			try {
				repositoryService.writeEvents(repositoryName);
				JOptionPane.showMessageDialog(facade.getMainPanel(),
						"Events informatons have been saved to the repository.", repositoryName,
						JOptionPane.INFORMATION_MESSAGE);
				facade.getMainPanel().updateTabs(OP_REPOSITORY_CHANGED);
			} catch (WritingException e) {
				JOptionPane.showMessageDialog(facade.getMainPanel(), e.getMessage(), repositoryName,
						JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	private void checkBoxSelectAllPerformed() {

		if (racine == null) {
			return;
		}

		int index = listEvents.getSelectedIndex();
		if (index == -1) {
			return;
		}

		if (tabbedPane.getSelectedIndex() == 0) {// Repository Addons
			if (checkBoxSelectAll.isSelected()) {
				selectAllDescending(racine);
			} else {
				deselectAllDescending(racine);
			}
			saveSelection();
			refreshViewArbre();
		}
	}

	private void checkBoxExpandAllPerformed() {

		if (racine == null) {
			return;
		}

		int index = listEvents.getSelectedIndex();
		if (index == -1) {
			return;
		}

		if (tabbedPane.getSelectedIndex() == 0) {// Repository Addons
			Set<TreePath> paths = new HashSet<TreePath>();
			if (checkBoxExpandAll.isSelected()) {
				getPathDirectories(new TreePath(arbre.getModel().getRoot()), paths);
				for (TreePath treePath : paths) {
					arbre.expandPath(treePath);
				}
			} else {
				TreePath rootPath = new TreePath(arbre.getModel().getRoot());
				for (TreeNodeDTO child : racine.getList()) {
					paths.add(rootPath.pathByAddingChild(child));
				}
				for (TreePath treePath : paths) {
					arbre.collapsePath(treePath);
				}
			}
		}
	}

	private void getPathDirectories(TreePath path, Set<TreePath> paths) {

		if (path != null) {
			TreeNodeDTO treeNodeDTO = (TreeNodeDTO) path.getLastPathComponent();
			if (!treeNodeDTO.isLeaf()) {
				TreeDirectoryDTO directory = (TreeDirectoryDTO) treeNodeDTO;
				paths.add(path);
				for (TreeNodeDTO child : directory.getList()) {
					getPathDirectories(path.pathByAddingChild(child), paths);
				}
			}
		}
	}

	public void updateListEvents() {

		List<EventDTO> eventDTOs = this.repositoryService.getEvents(repositoryName);

		Collections.sort(eventDTOs);
		this.eventDTOs = eventDTOs;

		List<String> eventTexts = new ArrayList<String>();
		for (EventDTO eventDTO : eventDTOs) {
			if (eventDTO.getDescription() == null) {
				eventTexts.add(eventDTO.getName());
			} else if (eventDTO.getDescription().isEmpty()) {
				eventTexts.add(eventDTO.getName());
			} else {
				eventTexts.add(eventDTO.getName() + " - " + eventDTO.getDescription());
			}
		}

		String[] data = new String[eventTexts.size()];
		eventTexts.toArray(data);

		listEvents.clearSelection();
		listEvents.setListData(data);
		int numberLigneShown = eventDTOs.size();
		listEvents.setVisibleRowCount(numberLigneShown);
		listEvents.setPreferredSize(listEvents.getPreferredScrollableViewportSize());
		listScrollPanel.repaint();
	}

	private void updateListAddons() {

		TreeDirectoryDTO treeDirectoryDTO = repositoryService.getGroupFromRepository(repositoryName, true);
		if (treeDirectoryDTO != null) {
			racine = treeDirectoryDTO;
			AddonTreeModel addonTreeModel = new AddonTreeModel(racine);
			arbre.setModel(addonTreeModel);
			addonTreeModel.fireTreeStructureChanged();
			int numberRowShown = arbre.getRowCount();
			arbre.setVisibleRowCount(numberRowShown);
			arbre.setPreferredSize(arbre.getPreferredScrollableViewportSize());
			arbreScrollPane.repaint();
		}
	}

	private void updateListDLC() {

		racineDLC = new TreeDirectoryDTO();
		racineDLC.setName("racineDLC");
		racineDLC.setParent(null);

		GameDLCs[] dlc = GameDLCs.values();
		for (int i = 0; i < dlc.length; i++) {
			String name = dlc[i].toString();
			TreeLeafDTO leaf = new TreeLeafDTO();
			leaf.setName(name);
			leaf.setParent(racineDLC);
			racineDLC.addTreeNode(leaf);
		}

		AddonTreeModel addonTreeModel = new AddonTreeModel(racineDLC);
		arbreDLC.setModel(addonTreeModel);
		addonTreeModel.fireTreeStructureChanged();
		int numberRowShown = arbreDLC.getRowCount();
		arbreDLC.setVisibleRowCount(numberRowShown);
		arbreDLC.setPreferredSize(arbreDLC.getPreferredScrollableViewportSize());
		arbreDLCScrollPane.repaint();
	}

	private void eventSelectionPerformed() {

		int index = listEvents.getSelectedIndex();
		if (index != -1) {

			if (racine != null) {
				arbre.setEnabled(true);
				EventDTO eventDTO = eventDTOs.get(index);
				Map<String, Boolean> mapAddonNames = eventDTO.getAddonNames();
				Map<String, Boolean> mapUserconfigFolderNames = eventDTO.getUserconfigFolderNames();
				Map<String, Boolean> map = new HashMap<String, Boolean>();
				map.putAll(mapAddonNames);
				map.putAll(mapUserconfigFolderNames);
				deselectAllDescending(racine);
				setSelection(racine, map);
				List<Boolean> selection = new ArrayList<Boolean>();
				getSelection(racine, selection);

				boolean allSelected = true;
				for (int i = 0; i < selection.size(); i++) {
					if (selection.get(i) == false) {
						allSelected = false;
						break;
					}
				}
				checkBoxSelectAll.setSelected(allSelected);

				Set<TreePath> expandedTreePaths = new HashSet<TreePath>();
				getExpandedTreePaths(new TreePath(arbre.getModel().getRoot()), expandedTreePaths);

				((AddonTreeModel) arbre.getModel()).fireTreeStructureChanged();
				refreshViewArbre();

				int numberRowShown = arbre.getRowCount();
				for (TreePath treePath : expandedTreePaths) {
					arbre.expandPath(treePath);
				}
			}
			if (racineDLC != null) {
				arbreDLC.setEnabled(true);
				EventDTO eventDTO = eventDTOs.get(index);
				Map<String, Boolean> mapAddonNames = eventDTO.getAddonNames();
				Map<String, Boolean> map = new HashMap<String, Boolean>();
				map.putAll(mapAddonNames);
				deselectAllDescending(racineDLC);
				setSelection(racineDLC, map);

				((AddonTreeModel) arbreDLC.getModel()).fireTreeStructureChanged();
				refreshViewArbreDLC();
			}

			index = tabbedPane.getSelectedIndex();
			checkBoxSelectAll.setEnabled(false);
			checkBoxExpandAll.setEnabled(false);
			if (index == 0) {
				if (racine != null) {
					checkBoxSelectAll.setEnabled(true);
					checkBoxExpandAll.setEnabled(true);
				}
			}
		}
	}

	private void setSelection(TreeNodeDTO treeNodeDTO, Map<String, Boolean> map) {

		if (treeNodeDTO.isLeaf()) {
			TreeLeafDTO leaf = (TreeLeafDTO) treeNodeDTO;
			if (map.containsKey(treeNodeDTO.getName())) {
				treeNodeDTO.setSelected(true);
				selectAllAscending(treeNodeDTO);
				boolean optional = map.get(treeNodeDTO.getName());
				leaf.setOptional(optional);
			} else {
				leaf.setSelected(false);
			}
		} else {
			TreeDirectoryDTO treeDirectoryDTO = (TreeDirectoryDTO) treeNodeDTO;
			for (TreeNodeDTO n : treeDirectoryDTO.getList()) {
				setSelection(n, map);
			}
		}
	}

	private void getSelection(TreeNodeDTO treeNodeDTO, List<Boolean> list) {

		if (treeNodeDTO.isLeaf()) {
			TreeLeafDTO leaf = (TreeLeafDTO) treeNodeDTO;
			list.add(leaf.isSelected());

		} else {
			TreeDirectoryDTO treeDirectoryDTO = (TreeDirectoryDTO) treeNodeDTO;
			for (TreeNodeDTO n : treeDirectoryDTO.getList()) {
				getSelection(n, list);
			}
		}
	}

	private void saveSelection() {

		int index = listEvents.getSelectedIndex();

		if (index != -1) {
			EventDTO eventDTO = eventDTOs.get(index);
			Map<String, Boolean> mapAddonNames = eventDTO.getAddonNames();
			Map<String, Boolean> mapUserconfigFolderNames = eventDTO.getUserconfigFolderNames();
			mapAddonNames.clear();
			mapUserconfigFolderNames.clear();
			getAddonsSelection(racine, mapAddonNames);
			getUserconfigSelection(racine, mapUserconfigFolderNames);
			getAddonsSelection(racineDLC, mapAddonNames);
			repositoryService.saveEvent(repositoryName, eventDTO);
		}
	}

	private void getAddonsSelection(TreeNodeDTO treeNodeDTO, Map<String, Boolean> mapAddonNames) {

		if (treeNodeDTO.isLeaf()) {
			TreeLeafDTO leaf = (TreeLeafDTO) treeNodeDTO;
			if (leaf.isSelected() && !mapAddonNames.containsKey(treeNodeDTO.getName())) {
				TreeNodeDTO parent = treeNodeDTO.getParent();
				boolean found = false;
				while (parent != null) {
					if (parent.getName().toLowerCase().equals("userconfig")) {
						found = true;
						break;
					}
					parent = parent.getParent();
				}
				if (!found) {
					mapAddonNames.put(treeNodeDTO.getName(), leaf.isOptional());
				}
			}
		} else {
			TreeDirectoryDTO treeDirectoryDTO = (TreeDirectoryDTO) treeNodeDTO;
			for (TreeNodeDTO n : treeDirectoryDTO.getList()) {
				getAddonsSelection(n, mapAddonNames);
			}
		}
	}

	private void getUserconfigSelection(TreeNodeDTO treeNodeDTO, Map<String, Boolean> mapUserconfigFolderNames) {

		TreeDirectoryDTO treeDirectoryDTO = (TreeDirectoryDTO) treeNodeDTO;
		for (TreeNodeDTO n : treeDirectoryDTO.getList()) {
			if (n.getName().equals("userconfig")) {
				TreeDirectoryDTO userconfig = (TreeDirectoryDTO) n;
				for (TreeNodeDTO u : userconfig.getList()) {
					if (u.isSelected() && !mapUserconfigFolderNames.containsKey(treeNodeDTO.getName())) {
						if (u.isLeaf()) {
							TreeLeafDTO leaf = (TreeLeafDTO) u;
							mapUserconfigFolderNames.put(u.getName(), leaf.isOptional());
						} else {
							mapUserconfigFolderNames.put(u.getName(), false);
						}
					}
				}
			}
		}
	}

	private void selectAllAscending(TreeNodeDTO treeNodeDTO) {

		if (treeNodeDTO != null) {
			TreeNodeDTO parent = treeNodeDTO.getParent();
			if (parent != null) {
				parent.setSelected(true);
				selectAllAscending(parent);
			}
		}
	}

	private void selectAllDescending(TreeDirectoryDTO treeDirectoryDTO) {

		treeDirectoryDTO.setSelected(true);
		for (TreeNodeDTO t : treeDirectoryDTO.getList()) {
			t.setSelected(true);
			if (!t.isLeaf()) {
				TreeDirectoryDTO d = (TreeDirectoryDTO) t;
				selectAllDescending(d);
			}
		}
	}

	private void deselectAllDescending(TreeDirectoryDTO treeDirectoryDTO) {
		treeDirectoryDTO.setSelected(false);
		for (TreeNodeDTO t : treeDirectoryDTO.getList()) {
			t.setSelected(false);
			if (!t.isLeaf()) {
				TreeDirectoryDTO d = (TreeDirectoryDTO) t;
				deselectAllDescending(d);
			}
		}
	}

	private void flattenSplitPane(JSplitPane jSplitPane) {
		jSplitPane.setUI(new BasicSplitPaneUI() {
			@Override
			public BasicSplitPaneDivider createDefaultDivider() {
				return new BasicSplitPaneDivider(this) {
					@Override
					public void setBorder(Border b) {
					}
				};
			}
		});
		jSplitPane.setBorder(null);
	}

	private void onArbreExpanded(TreePath path) {

		int numberRowShown = arbre.getRowCount();
		arbre.setVisibleRowCount(numberRowShown);
		arbre.setPreferredSize(arbre.getPreferredScrollableViewportSize());
		arbreScrollPane.repaint();
		arbre.setSelectionPath(null);
	}

	private void onArbreCollapsed(TreePath path) {

		int numberRowShown = arbre.getRowCount();
		arbre.setVisibleRowCount(numberRowShown);
		arbre.setPreferredSize(arbre.getPreferredScrollableViewportSize());
		arbreScrollPane.repaint();
		arbre.setSelectionPath(null);
	}

	private void popupActionPerformed(ActionEvent evt) {

		if (evt.getActionCommand().equals("Set reqired")) {
			setRequired();
		} else if (evt.getActionCommand().equals("Set optional")) {
			setOptional();
		}
	}

	private void setRequired() {

		TreeNodeDTO treeNodeDTO = (TreeNodeDTO) arbreTreePath.getLastPathComponent();

		if (treeNodeDTO != null) {
			if (treeNodeDTO.isLeaf()) {
				TreeLeafDTO leaf = (TreeLeafDTO) treeNodeDTO;
				leaf.setOptional(false);
			}
		}
		saveSelection();

		Set<TreePath> expandedTreePaths = new HashSet<TreePath>();
		getExpandedTreePaths(new TreePath(arbre.getModel().getRoot()), expandedTreePaths);

		((AddonTreeModel) arbre.getModel()).fireTreeStructureChanged();
		refreshViewArbre();

		int numberRowShown = arbre.getRowCount();
		for (TreePath treePath : expandedTreePaths) {
			arbre.expandPath(treePath);
		}

		if (numberRowShown == 0) {
			arbre.setToolTipText("Right click to add a group");
		} else {
			arbre.setToolTipText(null);
		}
	}

	private void setOptional() {

		TreeNodeDTO treeNodeDTO = (TreeNodeDTO) arbreTreePath.getLastPathComponent();

		if (treeNodeDTO != null) {
			if (treeNodeDTO.isLeaf()) {
				TreeLeafDTO leaf = (TreeLeafDTO) treeNodeDTO;
				leaf.setOptional(true);
			}
		}
		saveSelection();

		Set<TreePath> expandedTreePaths = new HashSet<TreePath>();
		getExpandedTreePaths(new TreePath(arbre.getModel().getRoot()), expandedTreePaths);

		((AddonTreeModel) arbre.getModel()).fireTreeStructureChanged();
		refreshViewArbre();

		int numberRowShown = arbre.getRowCount();
		for (TreePath treePath : expandedTreePaths) {
			arbre.expandPath(treePath);
		}

		if (numberRowShown == 0) {
			arbre.setToolTipText("Right click to add a group");
		} else {
			arbre.setToolTipText(null);
		}
	}

	private void addonSelectionPerformed() {

		TreeNodeDTO treeNodeDTO = (TreeNodeDTO) arbre.getLastSelectedPathComponent();

		if (treeNodeDTO == null) {
			return;
		}

		treeNodeDTO.setSelected(!treeNodeDTO.isSelected());

		if (treeNodeDTO.isLeaf() && !treeNodeDTO.isSelected()) {
			TreeDirectoryDTO treeDirectoryDTO = treeNodeDTO.getParent();
			treeDirectoryDTO.setSelected(false);
		} else if (treeNodeDTO.isLeaf() && treeNodeDTO.isSelected()) {
			TreeDirectoryDTO treeDirectoryDTO = treeNodeDTO.getParent();
			int nbNodes = treeDirectoryDTO.getList().size();
			int nbSelectedNodes = 0;
			for (TreeNodeDTO treDto : treeDirectoryDTO.getList()) {
				if (treDto.isSelected()) {
					nbSelectedNodes++;
				}
			}
			if (nbNodes == nbSelectedNodes) {
				treeDirectoryDTO.setSelected(true);
			}
		} else if (!treeNodeDTO.isLeaf()) {
			TreeDirectoryDTO treeDirectoryDTO = (TreeDirectoryDTO) treeNodeDTO;
			if (treeNodeDTO.isSelected()) {
				selectAllAscending(treeNodeDTO);
				selectAllDescending(treeDirectoryDTO);
			} else {
				deselectAllDescending(treeDirectoryDTO);
			}
		}
		saveSelection();
		List<Boolean> selection = new ArrayList<Boolean>();
		getSelection(racine, selection);
		boolean allSelected = true;
		for (int i = 0; i < selection.size(); i++) {
			if (selection.get(i) == false) {
				allSelected = false;
				break;
			}
		}
		checkBoxSelectAll.setSelected(allSelected);
		refreshViewArbre();
	}

	private void dlcSelectionPerformed() {

		TreeNodeDTO treeNodeDTO = (TreeNodeDTO) arbreDLC.getLastSelectedPathComponent();

		if (treeNodeDTO == null) {
			return;
		}

		treeNodeDTO.setSelected(!treeNodeDTO.isSelected());
		saveSelection();
		refreshViewArbreDLC();
	}

	private TreeNodeDTO[] getSelectedNode() {

		TreePath[] paths = arbre.getSelectionPaths();
		if (paths == null) {
			return null;
		} else if (paths.length == 0) {
			return null;
		} else {// paths !=null
			TreeNodeDTO[] treeNodeDTOs = new TreeNodeDTO[paths.length];
			for (int i = 0; i < paths.length; i++) {
				treeNodeDTOs[i] = (TreeNodeDTO) paths[i].getLastPathComponent();
			}
			return treeNodeDTOs;
		}
	}

	private void getExpandedTreePaths(TreePath path, Set<TreePath> expandedTreePaths) {

		if (path != null) {
			TreeNodeDTO treeNodeDTO = (TreeNodeDTO) path.getLastPathComponent();
			if (!treeNodeDTO.isLeaf()) {
				TreeDirectoryDTO directory = (TreeDirectoryDTO) treeNodeDTO;
				if (arbre.isExpanded(path)) {
					expandedTreePaths.add(path);
				}
				for (TreeNodeDTO child : directory.getList()) {
					getExpandedTreePaths(path.pathByAddingChild(child), expandedTreePaths);
				}
			}
		}
	}
}

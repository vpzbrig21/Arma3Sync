package fr.soe.a3s.ui.repository;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
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

import fr.soe.a3s.constant.GameExecutables;
import fr.soe.a3s.constant.RepositoryStatus;
import fr.soe.a3s.controller.ObserverConnectionLost;
import fr.soe.a3s.controller.ObserverEnd;
import fr.soe.a3s.controller.ObserverError;
import fr.soe.a3s.controller.ObserverStart;
import fr.soe.a3s.dto.sync.SyncTreeDirectoryDTO;
import fr.soe.a3s.dto.sync.SyncTreeNodeDTO;
import fr.soe.a3s.exception.CheckException;
import fr.soe.a3s.exception.remote.RemoteRepositoryException;
import fr.soe.a3s.exception.repository.RepositoryException;
import fr.soe.a3s.service.LaunchService;
import fr.soe.a3s.service.ProfileService;
import fr.soe.a3s.service.RepositoryService;
import fr.soe.a3s.service.synchronization.FilesSynchronizationManager;
import fr.soe.a3s.ui.Facade;
import fr.soe.a3s.ui.UIConstants;
import fr.soe.a3s.ui.IconFactory;
import fr.soe.a3s.ui.repository.dialogs.ConnectionLostDialog;
import fr.soe.a3s.ui.repository.dialogs.DownloadSettingsDialog;
import fr.soe.a3s.ui.repository.dialogs.ReportDialog;
import fr.soe.a3s.ui.repository.dialogs.error.UnexpectedErrorDialog;
import fr.soe.a3s.ui.repository.tree.AddonSyncTreeModel;
import fr.soe.a3s.ui.repository.tree.CheckTreeCellRendererRepository;
import fr.soe.a3s.ui.repository.tree.MyRendererRepository;
import fr.soe.a3s.ui.repository.workers.AddonsAutoUpdater;
import fr.soe.a3s.ui.repository.workers.AddonsChecker;
import fr.soe.a3s.ui.repository.workers.AddonsDownloader;
import fr.soe.a3s.ui.repository.workers.UserconfigUpdater;
import fr.soe.a3s.utils.RepositoryConsoleErrorPrinter;
import fr.soe.a3s.utils.UnitConverter;

/**
 * This code was edited or generated using CloudGarden's Jigloo SWT/Swing GUI
 * Builder, which is free for non-commercial use. If Jigloo is being used
 * commercially (ie, by a corporation, company or business for any purpose
 * whatever) then you should purchase a license for each developer using Jigloo.
 * Please visit www.cloudgarden.com for details. Use of Jigloo implies
 * acceptance of these licensing terms. A COMMERCIAL LICENSE HAS NOT BEEN
 * PURCHASED FOR THIS MACHINE, SO JIGLOO OR THIS CODE CANNOT BE USED LEGALLY FOR
 * ANY CORPORATE OR COMMERCIAL PURPOSE.
 */
public class DownloadPanel extends JPanel implements UIConstants {

	private static final String TAB_TITLE_REPOSITORY_CONTENT = "Repository content";
	
	private Facade facade;
	private RepositoryPanel repositoryPanel;

	private JTree arbre;
	private TreePath selectedTreePath;
	private JScrollPane arbreScrollPane;
	private JPopupMenu popup;
	private JTabbedPane tabbedPane;

	private JButton buttonCheckForAddonsStart, buttonCheckForAddonsCancel;
	private JButton buttonDownloadStart, buttonDownloadPause, buttonDownloadCancel, buttonDownloadReport;
	private JButton buttonSettings;

	private JComboBox comBoxDestinationFolder;
	private JCheckBox checkBoxSelectAll, checkBoxExpandAll, checkBoxExactMatch, checkBoxAutoDiscover;
	private JMenuItem menuItemHideExtraLocalContent, menuItemShowExtraLocalContent;
	private JProgressBar progressBarCheckForAddons, progressBarDownloadAddons, progressBarDownloadSingleAddon;

	private JLabel labelCheckForAddonsStatus;
	private JLabel labelTotalFilesSize, labelTotalFilesSizeValue;
	private JLabel labelTotalFilesUpdatedValue, labeltTotalFilesDeletedValue;
	private JLabel labelSpeed, labelSpeedValue;
	private JLabel labelDownloaded, labelDownloadedValue;
	private JLabel labelRemainingTime, labelRemainingTimeValue;
	private JLabel labelDownloadStatus;
	private JLabel labelActiveConnections;
	private JLabel labelActiveConnectionsValue;
	private JLabel labelDiskUsage, labelDiskUsageValue;

	/* Data */
	private SyncTreeDirectoryDTO racine = null;
	private String repositoryName = null;
	private String eventName = null;

	/* Const */
	private static final int PROGRESSBAR_HEIGHT = 18;
	public static final Color GREEN = new Color(45, 125, 45);

	/* Services */
	private final RepositoryService repositoryService = new RepositoryService();
	private final ProfileService profileService = new ProfileService();
	private final LaunchService launchService = new LaunchService();
	private final FilesSynchronizationManager filesManager = new FilesSynchronizationManager();

	/* Workers */
	private AddonsChecker addonsChecker;
	private AddonsDownloader addonsDownloader;
	private AddonsAutoUpdater addonsAutoUpdater;
	private UserconfigUpdater userconfigUpdater;

	public DownloadPanel(Facade facade, RepositoryPanel repositoryPanel) {

		this.facade = facade;
		this.repositoryPanel = repositoryPanel;
		this.setLayout(new BorderLayout());

		JPanel panel1 = new JPanel();
		JPanel panel2 = new JPanel();
		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panel1, panel2);
		splitPane.setOneTouchExpandable(false);
		flattenSplitPane(splitPane);
		this.add(splitPane, BorderLayout.CENTER);
		panel1.setLayout(new BorderLayout());
		Box vBox = Box.createVerticalBox();
		panel1.add(vBox, BorderLayout.NORTH);
		{
			JPanel checkForAddonsLabelPanel = new JPanel();
			checkForAddonsLabelPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
			JLabel labelCheckForAddons = new JLabel("Check for Addons");
			checkForAddonsLabelPanel.add(labelCheckForAddons);
			labelCheckForAddonsStatus = new JLabel();
			labelCheckForAddonsStatus.setFont(labelCheckForAddonsStatus.getFont().deriveFont(Font.ITALIC));
			labelCheckForAddonsStatus.setForeground(GREEN);
			checkForAddonsLabelPanel.add(labelCheckForAddonsStatus);
			vBox.add(checkForAddonsLabelPanel);
		}
		{
			JPanel checkForAddonsProgressBarPanel = new JPanel();
			checkForAddonsProgressBarPanel.setLayout(new BorderLayout());
			progressBarCheckForAddons = new JProgressBar();
			checkForAddonsProgressBarPanel.add(progressBarCheckForAddons, BorderLayout.CENTER);
			vBox.add(checkForAddonsProgressBarPanel);
		}
		{
			JPanel checkForAddonsControls = new JPanel();
			checkForAddonsControls.setLayout(new BorderLayout());
			buttonCheckForAddonsStart = new JButton("");
			buttonCheckForAddonsStart.setFocusable(false);
			buttonCheckForAddonsCancel = new JButton("");
			buttonCheckForAddonsCancel.setFocusable(false);
			Box hBox = Box.createHorizontalBox();
			hBox.add(buttonCheckForAddonsStart);
			hBox.add(buttonCheckForAddonsCancel);
                        buttonCheckForAddonsStart.setIcon(IconFactory.of("check", 18));
                        buttonCheckForAddonsCancel.setIcon(IconFactory.of("close", 18));
			checkForAddonsControls.add(hBox, BorderLayout.EAST);
			vBox.add(checkForAddonsControls);
		}
		vBox.add(Box.createVerticalStrut(10));
		{
			JPanel filesPanel = new JPanel();
			filesPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
			filesPanel.setBorder(
					BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Repository changes"));
			vBox.add(filesPanel);
			Box vBox2 = Box.createVerticalBox();
			filesPanel.add(vBox2);
			{
				JLabel totalFilesUpdated = new JLabel("Total files to update: ");
				totalFilesUpdated.setForeground(Color.RED);
				labelTotalFilesUpdatedValue = new JLabel();
				labelTotalFilesUpdatedValue.setForeground(Color.RED);
				Box hBox = Box.createHorizontalBox();
				hBox.add(totalFilesUpdated);
				hBox.add(labelTotalFilesUpdatedValue);
				hBox.add(Box.createHorizontalGlue());
				vBox2.add(hBox);
			}
			vBox2.add(Box.createVerticalStrut(10));
			{
				JLabel totalFilesDeleted = new JLabel("Total files to delete : ");
				totalFilesDeleted.setForeground(Color.BLUE);
				labeltTotalFilesDeletedValue = new JLabel();
				labeltTotalFilesDeletedValue.setForeground(Color.BLUE);
				Box hBox = Box.createHorizontalBox();
				hBox.add(totalFilesDeleted);
				hBox.add(labeltTotalFilesDeletedValue);
				hBox.add(Box.createHorizontalGlue());
				vBox2.add(hBox);
			}
		}
		vBox.add(Box.createVerticalStrut(15));
		{
			JPanel optionsPanel = new JPanel();
			optionsPanel.setLayout(new BorderLayout());
			buttonSettings = new JButton("Settings");
			buttonSettings.setFocusable(false);
                        buttonSettings.setIcon(IconFactory.of("settings", 18));
			optionsPanel.add(buttonSettings);
			vBox.add(optionsPanel);
		}
		vBox.add(Box.createVerticalStrut(10));
		{
			JPanel downloadLabelPanel = new JPanel();
			downloadLabelPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
			JLabel labelDownload = new JLabel("Download Addons");
			downloadLabelPanel.add(labelDownload);
			labelDownloadStatus = new JLabel();
			labelDownloadStatus.setFont(labelDownloadStatus.getFont().deriveFont(Font.ITALIC));
			labelDownloadStatus.setForeground(GREEN);
			downloadLabelPanel.add(labelDownloadStatus);
			vBox.add(downloadLabelPanel);
		}
		{
			JPanel downloadProgressBarPanel = new JPanel();
			downloadProgressBarPanel.setLayout(new BorderLayout());
			progressBarDownloadAddons = new JProgressBar();
			downloadProgressBarPanel.add(progressBarDownloadAddons, BorderLayout.CENTER);
			vBox.add(downloadProgressBarPanel);
		}
		{
			JPanel downloadProgressBarPanel = new JPanel();
			downloadProgressBarPanel.setLayout(new BorderLayout());
			progressBarDownloadSingleAddon = new JProgressBar();
			downloadProgressBarPanel.add(progressBarDownloadSingleAddon, BorderLayout.CENTER);
			vBox.add(downloadProgressBarPanel);
		}
		{
			JPanel downloadControls = new JPanel();
			downloadControls.setLayout(new BorderLayout());
			buttonDownloadStart = new JButton();
			buttonDownloadStart.setFocusable(false);
			buttonDownloadPause = new JButton();
			buttonDownloadPause.setFocusable(false);
			buttonDownloadCancel = new JButton();
			buttonDownloadCancel.setFocusable(false);
			buttonDownloadReport = new JButton();
			buttonDownloadReport.setFocusable(false);
			Box hBox = Box.createHorizontalBox();
			hBox.add(buttonDownloadStart);
			hBox.add(buttonDownloadPause);
			hBox.add(buttonDownloadCancel);
			hBox.add(buttonDownloadReport);
                        buttonDownloadStart.setIcon(IconFactory.of("play", 18));
                        buttonDownloadPause.setIcon(IconFactory.of("pause", 18));
                        buttonDownloadCancel.setIcon(IconFactory.of("stop", 18));
                        buttonDownloadReport.setIcon(IconFactory.of("report", 18));
			downloadControls.add(hBox, BorderLayout.EAST);
			vBox.add(downloadControls);
		}
		vBox.add(Box.createVerticalStrut(10));

		JPanel progressPanel = new JPanel();
		progressPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
		progressPanel
				.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Progress monitor"));
		panel1.add(progressPanel, BorderLayout.CENTER);
		vBox = Box.createVerticalBox();
		progressPanel.add(vBox, BorderLayout.NORTH);
		{
			labelTotalFilesSize = new JLabel("Total files size: ");
			labelTotalFilesSizeValue = new JLabel();
			Box hBox = Box.createHorizontalBox();
			hBox.add(labelTotalFilesSize);
			hBox.add(labelTotalFilesSizeValue);
			hBox.add(Box.createHorizontalGlue());
			vBox.add(hBox);
			vBox.add(Box.createVerticalStrut(10));
		}
		{
			labelDownloaded = new JLabel("Downloaded: ");
			labelDownloadedValue = new JLabel();
			Box hBox = Box.createHorizontalBox();
			hBox.add(labelDownloaded);
			hBox.add(labelDownloadedValue);
			hBox.add(Box.createHorizontalGlue());
			vBox.add(hBox);
			vBox.add(Box.createVerticalStrut(10));
		}
		{
			labelSpeed = new JLabel("Transfer speed: ");
			labelSpeedValue = new JLabel();
			Box hBox = Box.createHorizontalBox();
			hBox.add(labelSpeed);
			hBox.add(labelSpeedValue);
			hBox.add(Box.createHorizontalGlue());
			vBox.add(hBox);
			vBox.add(Box.createVerticalStrut(10));
		}
		{
			labelDiskUsage = new JLabel("Disk usage: ");
			labelDiskUsageValue = new JLabel();
			Box hBox = Box.createHorizontalBox();
			hBox.add(labelDiskUsage);
			hBox.add(labelDiskUsageValue);
			hBox.add(Box.createHorizontalGlue());
			vBox.add(hBox);
			vBox.add(Box.createVerticalStrut(10));
		}
		{
			labelRemainingTime = new JLabel("Remaining time: ");
			labelRemainingTimeValue = new JLabel();
			Box hBox = Box.createHorizontalBox();
			hBox.add(labelRemainingTime);
			hBox.add(labelRemainingTimeValue);
			hBox.add(Box.createHorizontalGlue());
			vBox.add(hBox);
			vBox.add(Box.createVerticalStrut(10));
		}
		{
			labelActiveConnections = new JLabel("Active connections: ");
			labelActiveConnectionsValue = new JLabel();
			Box hBox = Box.createHorizontalBox();
			hBox.add(labelActiveConnections);
			hBox.add(labelActiveConnectionsValue);
			hBox.add(Box.createHorizontalGlue());
			vBox.add(hBox);
			vBox.add(Box.createVerticalStrut(10));
		}

		panel2.setLayout(new BorderLayout());
		vBox = Box.createVerticalBox();
		panel2.add(vBox, BorderLayout.NORTH);
		{
			JPanel defaultDownloadLocationPanel = new JPanel();
			defaultDownloadLocationPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
			JLabel labelDefaultDestinationDirectoryPanel = new JLabel("Default destination folder");
			defaultDownloadLocationPanel.add(labelDefaultDestinationDirectoryPanel);
			vBox.add(defaultDownloadLocationPanel);
		}
		{
			JPanel locationPanel = new JPanel();
			locationPanel.setLayout(new BorderLayout());
			comBoxDestinationFolder = new JComboBox();
			comBoxDestinationFolder.setFocusable(false);
			comBoxDestinationFolder.setPreferredSize(new Dimension(this.getWidth(), 25));
			locationPanel.add(comBoxDestinationFolder, BorderLayout.CENTER);
			vBox.add(locationPanel);
		}
		vBox.add(Box.createVerticalStrut(5));
		{
			JPanel selectionPanel = new JPanel();
			selectionPanel.setLayout(new GridBagLayout());
			vBox.add(selectionPanel);
			{
				checkBoxSelectAll = new JCheckBox("Select All");
				checkBoxSelectAll.setFocusable(false);
				checkBoxAutoDiscover = new JCheckBox("Auto-discover");
				checkBoxAutoDiscover.setFocusable(false);
				checkBoxExpandAll = new JCheckBox("Expand All");
				checkBoxExpandAll.setFocusable(false);
				checkBoxExactMatch = new JCheckBox("Exact match");
				checkBoxExactMatch.setFocusable(false);
			}
			{
				GridBagConstraints c = new GridBagConstraints();
				c.fill = GridBagConstraints.HORIZONTAL;
				c.weightx = 0.5;
				c.weighty = 0;
				c.gridx = 0;
				c.gridy = 0;
				selectionPanel.add(checkBoxSelectAll, c);
			}
			{
				GridBagConstraints c = new GridBagConstraints();
				c.fill = GridBagConstraints.HORIZONTAL;
				c.weightx = 20;
				c.weighty = 0;
				c.gridx = 1;
				c.gridy = 0;
				selectionPanel.add(checkBoxAutoDiscover, c);
			}
			{
				GridBagConstraints c = new GridBagConstraints();
				c.fill = GridBagConstraints.HORIZONTAL;
				c.weightx = 0.5;
				c.weighty = 0;
				c.gridx = 0;
				c.gridy = 1;
				selectionPanel.add(checkBoxExpandAll, c);
			}
			{
				GridBagConstraints c = new GridBagConstraints();
				c.fill = GridBagConstraints.HORIZONTAL;
				c.weightx = 20;
				c.weighty = 0;
				c.gridx = 1;
				c.gridy = 1;
				selectionPanel.add(checkBoxExactMatch, c);
			}
		}
		vBox.add(Box.createVerticalStrut(5));
		{
			JPanel addonsPanel = new JPanel();
			addonsPanel.setLayout(new BorderLayout());
			AddonSyncTreeModel addonSyncTreeModel = new AddonSyncTreeModel(racine);
			arbre = new JTree(addonSyncTreeModel);
			arbre.setLargeModel(true);
			arbre.setRootVisible(false);
			arbre.setEditable(false);
			arbre.setShowsRootHandles(true);
			arbre.setToggleClickCount(0);
			arbre.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
			arbreScrollPane = new JScrollPane(arbre);
			arbreScrollPane.setBorder(BorderFactory.createEmptyBorder());
			
			tabbedPane = new JTabbedPane();
			tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
			tabbedPane.addTab(TAB_TITLE_REPOSITORY_CONTENT, arbreScrollPane);
			tabbedPane.setFocusable(false);

			addonsPanel.add(tabbedPane, BorderLayout.CENTER);
			panel2.add(addonsPanel, BorderLayout.CENTER);

			Font fontArbre = UIManager.getFont("Tree.font");
			FontMetrics metrics = arbre.getFontMetrics(fontArbre);
			int fontHeight = metrics.getAscent() + metrics.getDescent() + metrics.getLeading();
			arbre.setRowHeight(fontHeight);

			MyRendererRepository myRendererRepository = new MyRendererRepository();
			arbre.setCellRenderer(myRendererRepository);
			CheckTreeCellRendererRepository renderer = new CheckTreeCellRendererRepository(myRendererRepository);
			arbre.setCellRenderer(renderer);
		}

		/**/

		progressBarCheckForAddons.setPreferredSize(new Dimension(panel1.getPreferredSize().width, PROGRESSBAR_HEIGHT));
		progressBarDownloadAddons.setPreferredSize(new Dimension(panel1.getPreferredSize().width, PROGRESSBAR_HEIGHT));
		progressBarDownloadSingleAddon
				.setPreferredSize(new Dimension(panel1.getPreferredSize().width, PROGRESSBAR_HEIGHT));

		/* Right clic menu */
		popup = new JPopupMenu();

		menuItemHideExtraLocalContent = new JMenuItem("Hide  extra local content");
		menuItemHideExtraLocalContent.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {
				popupActionPerformed(evt);
			}
		});
		menuItemHideExtraLocalContent.setActionCommand("Hide");
		popup.add(menuItemHideExtraLocalContent);

		menuItemShowExtraLocalContent = new JMenuItem("Show extra local content");
		menuItemShowExtraLocalContent.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {
				popupActionPerformed(evt);
			}
		});
		menuItemShowExtraLocalContent.setActionCommand("Show");
		popup.add(menuItemShowExtraLocalContent);

		popup.addPopupMenuListener(new PopupMenuListener() {

			@Override
			public void popupMenuCanceled(PopupMenuEvent arg0) {
			}

			@Override
			public void popupMenuWillBecomeInvisible(PopupMenuEvent arg0) {
			}

			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent arg0) {
				SyncTreeNodeDTO syncTreeNodeDTO = (SyncTreeNodeDTO) arbre.getLastSelectedPathComponent();
				menuItemHideExtraLocalContent.setEnabled(false);
				menuItemShowExtraLocalContent.setEnabled(false);
				if (syncTreeNodeDTO == null) {
					return;
				} else if (!syncTreeNodeDTO.isLeaf()) {
					menuItemHideExtraLocalContent.setEnabled(true);
					menuItemShowExtraLocalContent.setEnabled(true);
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
		checkBoxAutoDiscover.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				checkBoxAutoDiscoverPerformed();
			}
		});
		checkBoxExactMatch.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				checkBoxExactMatchPerformed();
			}
		});
		arbre.addTreeExpansionListener(new TreeExpansionListener() {
			@Override
			public void treeExpanded(TreeExpansionEvent event) {
				onArbreExpanded();
			}

			@Override
			public void treeCollapsed(TreeExpansionEvent event) {
				onArbreCollapsed();
			}
		});
		arbre.addTreeSelectionListener(new TreeSelectionListener() {
			@Override
			public void valueChanged(TreeSelectionEvent arg0) {
				selectedTreePath = arbre.getSelectionPath();
			}
		});
		arbre.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (selectedTreePath == null) {
					return;
				}
				if (SwingUtilities.isRightMouseButton(e)) {
					return;
				}
				int hotspot = new JCheckBox().getPreferredSize().width;

				TreePath path = arbre.getPathForLocation(e.getX(), e.getY());
				if (path == null) {
					return;
				} else if (e.getX() > arbre.getPathBounds(path).x + hotspot) {
					return;
				}
				selectionPerformed();
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
					selectionPerformed();
				}
			}
		});
		comBoxDestinationFolder.addPopupMenuListener(new PopupMenuListener() {
			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent arg0) {
				defaultDestinationFolderSelection();
			}

			@Override
			public void popupMenuWillBecomeInvisible(PopupMenuEvent arg0) {
				defaultFolderDestinationReleased();
				updateExactMatchSelection();
			}

			@Override
			public void popupMenuCanceled(PopupMenuEvent arg0) {
			}
		});
		buttonCheckForAddonsStart.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				buttonCheckForAddonsStartPerformed();
			}
		});
		buttonCheckForAddonsCancel.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				buttonCheckForAddonsCancelPerformed();
			}
		});
		buttonSettings.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				buttonSettingsPerformed();
			}
		});
		buttonDownloadStart.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				buttonDownloadStartPerformed();
			}
		});
		buttonDownloadPause.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				buttonDownloadPausePerformed();
			}
		});
		buttonDownloadCancel.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				buttonDownloadCancelPerformed();
			}
		});
		buttonDownloadReport.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				buttonShowDownloadReportPerformed();
			}
		});

		setContextualHelp();
	}

	private void setContextualHelp() {

		buttonCheckForAddonsStart.setToolTipText("Check for Addons");
		buttonCheckForAddonsCancel.setToolTipText("Cancel");
		buttonDownloadStart.setToolTipText("Start/Resume");
		buttonDownloadPause.setToolTipText("Pause");
		buttonDownloadCancel.setToolTipText("Cancel");
		buttonDownloadReport.setToolTipText("View download report");
		checkBoxSelectAll.setToolTipText("Select All");
		checkBoxExpandAll.setToolTipText("Expand All");
		checkBoxAutoDiscover.setToolTipText("Auto-discover addons within all destination folders");
		checkBoxExactMatch.setToolTipText("Exact match repository content against default destination folder");
	}

	public void init(String repositoryName, String eventName) {

		this.repositoryName = repositoryName;
		this.eventName = eventName;

		defaultDestinationFolderSelection();
		updateAutoDiscoverSelection();
		updateExactMatchSelection();
		resetDownloadReport();
	}

	private void popupActionPerformed(ActionEvent evt) {

		if (evt.getActionCommand().equals("Hide")) {
			hidePerformed();
		} else if (evt.getActionCommand().equals("Show")) {
			showPerformed();
		}
	}

	private void selectionPerformed() {

		SyncTreeNodeDTO syncTreeNodeDTO = (SyncTreeNodeDTO) arbre.getLastSelectedPathComponent();

		if (syncTreeNodeDTO == null) {
			return;
		}

		syncTreeNodeDTO.setSelected(!syncTreeNodeDTO.isSelected());
		SyncTreeDirectoryDTO parent = syncTreeNodeDTO.getParent();
		if (syncTreeNodeDTO.isSelected()) {
			selectAllAscending(parent);
			if (!syncTreeNodeDTO.isLeaf()) {
				SyncTreeDirectoryDTO syncTreeDirectoryDTO = (SyncTreeDirectoryDTO) syncTreeNodeDTO;
				for (SyncTreeNodeDTO t : syncTreeDirectoryDTO.getList()) {
					selectAllDescending(t);
				}
			}
		} else {
			if (!syncTreeNodeDTO.isLeaf()) {
				SyncTreeDirectoryDTO syncTreeDirectoryDTO = (SyncTreeDirectoryDTO) syncTreeNodeDTO;
				for (SyncTreeNodeDTO t : syncTreeDirectoryDTO.getList()) {
					deselectAllDescending(t);
				}
			}
			int nbNodesSelected = 0;
			for (SyncTreeNodeDTO t : parent.getList()) {
				if (t.isSelected()) {
					nbNodesSelected++;
				}
			}
			if (nbNodesSelected == 0) {
				unselectAllAscending(parent);
			}
		}

		filesManager.update();
		labelTotalFilesSizeValue.setText(UnitConverter.convertSize(filesManager.getTotalFilesSize()));

		int numberRowShown = arbre.getRowCount();
		arbre.setVisibleRowCount(numberRowShown);
		arbre.setPreferredSize(arbre.getPreferredScrollableViewportSize());
		arbreScrollPane.repaint();
	}

	private void hidePerformed() {

		SyncTreeNodeDTO syncTreeNodeDTO = (SyncTreeNodeDTO) arbre.getLastSelectedPathComponent();

		SyncTreeDirectoryDTO directory = (SyncTreeDirectoryDTO) syncTreeNodeDTO;
		String relativePath = directory.getName();
		SyncTreeDirectoryDTO parent = directory.getParent();
		while (!parent.getName().equals("racine")) {
			relativePath = parent.getName() + "/" + relativePath;
			parent = parent.getParent();
		}

		repositoryService.addFilesToHide(relativePath, repositoryName);

		// Check for Addons
		buttonCheckForAddonsStartPerformed();
	}

	private void showPerformed() {

		SyncTreeNodeDTO syncTreeNodeDTO = (SyncTreeNodeDTO) arbre.getLastSelectedPathComponent();

		SyncTreeDirectoryDTO directory = (SyncTreeDirectoryDTO) syncTreeNodeDTO;
		String relativePath = directory.getName();
		SyncTreeDirectoryDTO parent = directory.getParent();
		while (!parent.getName().equals("racine")) {
			relativePath = parent.getName() + "/" + relativePath;
			parent = parent.getParent();
		}

		repositoryService.removeFilesToHide(relativePath, repositoryName);

		// Check for Addons
		buttonCheckForAddonsStartPerformed();
	}

	private void defaultDestinationFolderSelection() {

		List<String> set = profileService.getAddonSearchDirectoryPaths();
		Iterator iter = set.iterator();
		List<String> paths = new ArrayList<String>();
		while (iter.hasNext()) {
			paths.add((String) iter.next());
		}

		String[] tab = new String[paths.size()];
		int i = 0;
		for (String p : paths) {
			tab[i] = p;
			i++;
		}

		ComboBoxModel model = new DefaultComboBoxModel(tab);
		comBoxDestinationFolder.setModel(model);

		String path = repositoryService.getDefaultDownloadLocation(repositoryName, eventName);

		if (path == null && model.getSize() > 0) {
			comBoxDestinationFolder.setSelectedIndex(0);
		} else {
			comBoxDestinationFolder.setSelectedItem(path);
		}
	}

	private void defaultFolderDestinationReleased() {
		updateArbre(null);
	}

	private void updateAutoDiscoverSelection() {

		boolean value = repositoryService.isAutoDiscover(repositoryName);
		checkBoxAutoDiscover.setSelected(!value);
	}

	private void updateExactMatchSelection() {

		checkBoxExactMatch.setEnabled(true);
		checkBoxAutoDiscover.setEnabled(true);
		boolean isArmA3Directory = false;

		// Disable exactMath if ArmA3Directory is selected
		String path = (String) comBoxDestinationFolder.getSelectedItem();
		if (path != null && !"".equals(path)) {
			File directory = new File(path);
			if (directory != null) {
				File arma3ExeFile = new File(directory.getAbsolutePath() + "/" + GameExecutables.GAME.getDescription());
				File arma3ServerExeFile = new File(
						directory.getAbsolutePath() + "/" + GameExecutables.WIN_SERVER.getDescription());
				if (arma3ExeFile.exists() || arma3ServerExeFile.exists()) {
					isArmA3Directory = true;
				}
			}
		}

		if (isArmA3Directory) {
			checkBoxExactMatch.setEnabled(false);
			checkBoxExactMatch.setSelected(false);
			repositoryService.setExactMatch(false, repositoryName);
		} else {
			boolean value = repositoryService.isExactMatch(repositoryName);
			checkBoxExactMatch.setSelected(value);
                        if (value) {
                                checkBoxAutoDiscover.setSelected(false);
                                checkBoxAutoDiscover.setEnabled(false);
                        }
		}
	}

	private void buttonCheckForAddonsStartPerformed() {

		if (facade.getMainPanel().isCheckingForAddons(repositoryName, eventName)) {
			return;
		}

		boolean ok = checkDefaultDownloadLocation();
		if (ok) {
			String defaultDownloadLocation = (String) comBoxDestinationFolder.getSelectedItem();
			repositoryService.setDefaultDownloadLocation(repositoryName, eventName, defaultDownloadLocation);
			checkForAddons(false);
		}
	}

	private boolean checkDefaultDownloadLocation() {

		boolean ok = true;

		if (comBoxDestinationFolder.getSelectedItem() == null) {
			String tabName = facade.getMainPanel().getRepositoryTitle(repositoryName, eventName);
			String message = "Default destination folder is empty!";
			if (comBoxDestinationFolder.getModel().getSize() == 0) {
				message = message + "\n" + "Please checkout addon search directories" + "\n"
						+ "from Addon Options panel.";
			}
			displayMessage(message, tabName, JOptionPane.WARNING_MESSAGE);
			ok = false;
		} else {
			String defaultDownloadLocation = (String) comBoxDestinationFolder.getSelectedItem();

			// Add default download location to addon search directory list
			List<String> list = profileService.getAddonSearchDirectoryPaths();
			if (!list.contains(defaultDownloadLocation)) {
				profileService.addAddonSearchDirectoryPath(defaultDownloadLocation);
				facade.getMainPanel().updateTabs(OP_ADDON_FILES_CHANGED);
			}

			// Check if destination folder exists
			if (checkBoxAutoDiscover.isSelected()) {
				for (String filePath : list) {
					File file = new File(filePath);
					if (!file.exists()) {
						String tabName = facade.getMainPanel().getRepositoryTitle(repositoryName, eventName);
						String message = "Destination directory does not exist: " + filePath;
						displayMessage(message, tabName, JOptionPane.ERROR_MESSAGE);
						ok = false;
						break;
					}
				}
			} else {
				File file = new File(defaultDownloadLocation);
				if (!file.exists()) {
					String tabName = facade.getMainPanel().getRepositoryTitle(repositoryName, eventName);
					String message = "Default destination folder does not exist: " + defaultDownloadLocation;
					displayMessage(message, tabName, JOptionPane.ERROR_MESSAGE);
					ok = false;
				}
			}
		}
		return ok;
	}

	private void checkForAddons(final boolean statusInError) {

		facade.getMainPanel().setCheckingForAddons(repositoryName, eventName, true);
		addonsChecker = new AddonsChecker(facade, repositoryName, eventName, this);
		addonsChecker.addObserverEnd(new ObserverEnd() {
			@Override
			public void end() {

				updateArbre(addonsChecker.getParent());

				if (statusInError) {
					updateStatusError();
				} else {
					updateStatusOK();
				}

				facade.getAddonsPanel().getGroupManager().updateGroupModsets(repositoryName);

				addonsChecker = null;
				System.gc();
				facade.getMainPanel().setCheckingForAddons(repositoryName, eventName, false);
			}
		});
		addonsChecker.addObserverError(new ObserverError() {
			@Override
			public void error(List<Exception> errors) {

				String tabName = facade.getMainPanel().getRepositoryTitle(repositoryName, eventName);

				Exception ex = errors.get(0);
				if (ex instanceof RepositoryException || ex instanceof CheckException
						|| ex instanceof RemoteRepositoryException || ex instanceof IOException) {
					RepositoryConsoleErrorPrinter.printRepositoryManagedError(repositoryName, ex);
					displayMessage(ex.getMessage(), tabName, JOptionPane.ERROR_MESSAGE);
				} else {
					RepositoryConsoleErrorPrinter.printRepositoryUnexpectedError(repositoryName, ex);
					UnexpectedErrorDialog dialog = new UnexpectedErrorDialog(facade, tabName, ex, repositoryName);
					dialog.show();
				}

				updateArbre(null);

				updateStatusError();

				addonsChecker = null;
				System.gc();
				facade.getMainPanel().setCheckingForAddons(repositoryName, eventName, false);
			}
		});
		addonsChecker.setDaemon(true);
		addonsChecker.start();
	}

	private void updateStatusOK() {

		/* Update repository revision and status */
		repositoryService.updateRepositoryRevision(repositoryName);
		repositoryService.setRepositorySyncStatus(repositoryName, RepositoryStatus.OK);
		facade.getMainPanel().updateTabs(OP_REPOSITORY_CHANGED);
	}

	private void updateStatusError() {

		/* Reset repository revision and status */
		repositoryService.resetRepositoryRevision(repositoryName);
		repositoryService.setRepositorySyncStatus(repositoryName, RepositoryStatus.ERROR);
		facade.getMainPanel().updateTabs(OP_REPOSITORY_CHANGED);
	}

	private void buttonCheckForAddonsCancelPerformed() {

		if (addonsChecker != null) {
			addonsChecker.cancel();
			facade.getMainPanel().setCheckingForAddons(repositoryName, eventName, false);
			addonsChecker = null;
		}
	}

	private void buttonDownloadStartPerformed() {

		if (racine == null) {
			return;
		}

		if (facade.getMainPanel().isDownloading(repositoryName, eventName)) {
			return;
		}

		if ((launchService.isArmA3Running() || launchService.isArmA3ServerRunning())) {
			String tabName = facade.getMainPanel().getRepositoryTitle(repositoryName, eventName);
			String message = "ArmA 3 is currently running. \n "
					+ "Update of files may failed if these files are currently in use.";
			int ok = JOptionPane.showConfirmDialog(facade.getMainPanel(), message, tabName,
					JOptionPane.WARNING_MESSAGE);
			if (ok != 0) {
				return;
			}
		}

		// User may have removed destination directory from addon search directory in
		// the meanwhile
		boolean ok = checkDefaultDownloadLocation();
		if (ok) {
			String defaultDownloadLocation = (String) comBoxDestinationFolder.getSelectedItem();
			repositoryService.setDefaultDownloadLocation(repositoryName, eventName, defaultDownloadLocation);
			downloadAddons();
		}
	}

	private void downloadAddons() {

		facade.getMainPanel().setDownloading(repositoryName, eventName, true);
		addonsDownloader = new AddonsDownloader(facade, repositoryName, eventName, filesManager, this);
		
		addonsDownloader.addObserverEnd(new ObserverEnd() {
			@Override
			public void end() {
				/* Show end message */
				/*
				if (!facade.getMainPanel().isToTray()) {
					String tabName = facade.getMainPanel().getRepositoryTitle(repositoryName, eventName);
					String message = "Download is finished.";
					displayMessage(message, tabName, JOptionPane.INFORMATION_MESSAGE);
				}
				*/
				
				/* Check for Userconfig Update */
				if (filesManager.isUserconfigUpdated()) {
					boolean silent = facade.getMainPanel().isToTray();
					userconfigUpdater = new UserconfigUpdater(facade, repositoryName, eventName, silent, filesManager);
					userconfigUpdater.run();
				}

				facade.getMainPanel().updateTabs(OP_ADDON_FILES_CHANGED);
				
				addonsDownloader = null;
				System.gc();
				facade.getMainPanel().setDownloading(repositoryName, eventName, false);

				// Check for Addons
				checkForAddons(false);
			}
		});
		addonsDownloader.addObserverError(new ObserverError() {
			@Override
			public void error(List<Exception> errors) {
				for (Exception ex : errors) {
					if (ex instanceof RepositoryException || ex instanceof CheckException
							|| ex instanceof RemoteRepositoryException || ex instanceof IOException) {
						RepositoryConsoleErrorPrinter.printRepositoryManagedError(repositoryName, ex);
					} else {
						RepositoryConsoleErrorPrinter.printRepositoryUnexpectedError(repositoryName, ex);
					}
				}

				if (errors.size() > 0 && !facade.getMainPanel().isToTray()) {
					showDownloadReport();
				}
				
				addonsDownloader = null;
				System.gc();
				facade.getMainPanel().setDownloading(repositoryName, eventName, false);

				// Check for Addons
				checkForAddons(true);
			}
		});
		addonsDownloader.addObserverConnectionLost(new ObserverConnectionLost() {
			@Override
			public void lost() {
				String tabName = facade.getMainPanel().getRepositoryTitle(repositoryName, eventName);
				ConnectionLostDialog dialog = new ConnectionLostDialog(facade, repositoryName, tabName);
				dialog.init();
				if (!facade.getMainPanel().isToTray()) {
					dialog.setVisible(true);
				}
				if (!dialog.reconnect()) {
					// Reset tree
					updateStatusError();
					updateArbre(null);
					addonsDownloader = null;
					System.gc();
					facade.getMainPanel().setDownloading(repositoryName, eventName, false);
				} else {
					addonsDownloader.run();
				}
			}
		});

		addonsDownloader.setDaemon(true);
		addonsDownloader.start();
	}

	private void buttonDownloadPausePerformed() {

		if (addonsDownloader != null && facade.getMainPanel().isDownloading(repositoryName, eventName)) {
			addonsDownloader.pause();
			facade.getMainPanel().setDownloading(repositoryName, eventName, false);
			System.gc();
			// addonsDownloader must not be set null => cancel action would
			// failed
		}
	}

	private void buttonDownloadCancelPerformed() {

		if (addonsDownloader != null) {
			addonsDownloader.cancel();
			facade.getMainPanel().setDownloading(repositoryName, eventName, false);
			addonsDownloader = null;
			checkForAddons(false);
		}
	}

	private void buttonShowDownloadReportPerformed() {
		showDownloadReport();
	}

	public void showDownloadReport() {

		facade.getMainPanel().selectRepository(repositoryName, eventName);
		String downloadReport = repositoryService.getReport(repositoryName);
		ReportDialog reportPanel = new ReportDialog(facade, this);
		reportPanel.init(downloadReport);
		reportPanel.setVisible(true);
	}

	private void resetDownloadReport() {
		repositoryService.setReport(repositoryName, null);
	}

	public void synchronyse() {
		buttonCheckForAddonsStartPerformed();
	}

	public void autoUpdatePerformed(final ObserverEnd obs) {

		if (facade.getMainPanel().isDownloading(repositoryName, eventName)) {
			obs.end();
			return;
		}

		if ((launchService.isArmA3Running() || launchService.isArmA3ServerRunning())) {
			obs.end();
			return;
		}

		boolean ok = checkDefaultDownloadLocation();
		if (ok) {
			autoUpdate(obs);
		}
	}

	private void autoUpdate(final ObserverEnd obs) {

		addonsChecker = new AddonsChecker(facade, repositoryName, null, this);
		addonsDownloader = new AddonsDownloader(facade, repositoryName, null, filesManager, this);
		addonsAutoUpdater = new AddonsAutoUpdater(repositoryName, addonsChecker, addonsDownloader, this);

		addonsAutoUpdater.addObserverCheckStart(new ObserverStart() {
			@Override
			public void start() {
				facade.getMainPanel().setCheckingForAddons(repositoryName, null, true);
			}
		});
		addonsAutoUpdater.addObserverCheckEnd(new ObserverEnd() {
			@Override
			public void end() {
				facade.getMainPanel().setCheckingForAddons(repositoryName, null, false);
			}
		});	
		addonsAutoUpdater.addObserverDownloadStart(new ObserverStart() {
			@Override
			public void start() {
				facade.getMainPanel().setDownloading(repositoryName, null, true);
			}
		});
		addonsAutoUpdater.addObserverDownloadEnd(new ObserverEnd() {
			@Override
			public void end() {

				if (filesManager.isUserconfigUpdated()) {
					userconfigUpdater = new UserconfigUpdater(facade, repositoryName, null, true, filesManager);
					userconfigUpdater.run();
				}

				facade.getMainPanel().updateTabs(OP_ADDON_FILES_CHANGED);

				facade.getAddonsPanel().getGroupManager().updateGroupModsets(repositoryName);

				updateStatusOK();

				updateArbre(addonsChecker.getParent());

				addonsAutoUpdater = null;
				System.gc();
				facade.getMainPanel().setDownloading(repositoryName, null, false);
				obs.end();
			}
		});
		addonsAutoUpdater.addObserverDownloadError(new ObserverError() {
			@Override
			public void error(List<Exception> errors) {

				for (Exception ex : errors) {
					if (ex instanceof RepositoryException || ex instanceof CheckException
							|| ex instanceof RemoteRepositoryException || ex instanceof IOException) {
						RepositoryConsoleErrorPrinter.printRepositoryManagedError(repositoryName, ex);
					} else {
						RepositoryConsoleErrorPrinter.printRepositoryUnexpectedError(repositoryName, ex);
					}
				}
				updateStatusError();
				updateArbre(null);
				addonsAutoUpdater = null;
				System.gc();
				facade.getMainPanel().setCheckingForAddons(repositoryName, null, false);
				facade.getMainPanel().setDownloading(repositoryName, null, false);
			}
		});
		addonsAutoUpdater.addObserverConnectionLost(new ObserverConnectionLost() {
			@Override
			public void lost() {

				String tabName = facade.getMainPanel().getRepositoryTitle(repositoryName, null);
				ConnectionLostDialog dialog = new ConnectionLostDialog(facade, repositoryName, tabName);
				dialog.init();
				if (!facade.getMainPanel().isToTray()) {
					dialog.setVisible(true);
				}
				if (!dialog.reconnect()) {
					updateStatusError();
					updateArbre(null);
					addonsDownloader = null;
					System.gc();
					facade.getMainPanel().setDownloading(repositoryName, null, false);
				} else {
					addonsDownloader.run();
				}
			}
		});

		addonsAutoUpdater.setDaemon(true);
		addonsAutoUpdater.start();
	}

	private void buttonSettingsPerformed() {

		DownloadSettingsDialog advancedConfigurationPanel = new DownloadSettingsDialog(facade, repositoryName, this);
		buttonSettings.setEnabled(false);
		advancedConfigurationPanel.init();
		advancedConfigurationPanel.setVisible(true);
		buttonSettings.setEnabled(true);
	}

	public void checkBoxSelectAllPerformed() {

		if (racine == null) {
			return;
		}

		if (checkBoxSelectAll.isSelected()) {
			selectAllDescending(racine);
		} else {
			deselectAllDescending(racine);
		}

		filesManager.update();
		labelTotalFilesSizeValue.setText(UnitConverter.convertSize(filesManager.getTotalFilesSize()));

		int numberRowShown = arbre.getRowCount();
		arbre.setVisibleRowCount(numberRowShown);
		arbre.setPreferredSize(arbre.getPreferredScrollableViewportSize());
		arbreScrollPane.repaint();
	}

	private void checkBoxExpandAllPerformed() {

		if (racine == null) {
			return;
		}

		Set<TreePath> paths = new HashSet<TreePath>();
		if (checkBoxExpandAll.isSelected()) {
			getPathDirectories(new TreePath(arbre.getModel().getRoot()), paths);
			for (TreePath treePath : paths) {
				arbre.expandPath(treePath);
			}
		} else {
			TreePath rootPath = new TreePath(arbre.getModel().getRoot());
			for (SyncTreeNodeDTO child : racine.getList()) {
				paths.add(rootPath.pathByAddingChild(child));
			}
			for (TreePath treePath : paths) {
				arbre.collapsePath(treePath);
			}
		}
	}

	private void checkBoxAutoDiscoverPerformed() {

		boolean value = checkBoxAutoDiscover.isSelected();
		repositoryService.setAutoDiscover(!value, repositoryName);
		updateArbre(null);
	}

	private void checkBoxExactMatchPerformed() {

		boolean value = checkBoxExactMatch.isSelected();
		repositoryService.setExactMatch(value, repositoryName);
		checkBoxAutoDiscover.setSelected(false);
                if (value) {
                        checkBoxAutoDiscover.setEnabled(false);
                        checkBoxAutoDiscover.setSelected(false);
                } else {
                        checkBoxAutoDiscover.setEnabled(true);
                }
                updateArbre(null);
        }

	private void getPathDirectories(TreePath path, Set<TreePath> paths) {

		if (path != null) {
			SyncTreeNodeDTO syncTreeNodeDTO = (SyncTreeNodeDTO) path.getLastPathComponent();
			if (!syncTreeNodeDTO.isLeaf()) {
				SyncTreeDirectoryDTO directory = (SyncTreeDirectoryDTO) syncTreeNodeDTO;
				paths.add(path);
				for (SyncTreeNodeDTO child : directory.getList()) {
					getPathDirectories(path.pathByAddingChild(child), paths);
				}
			}
		}
	}

	public void updateArbre(SyncTreeDirectoryDTO newRacine) {

		// System.out.println("Updating DownloadPanel arbre...");

		arbre.setEnabled(false);

		racine = newRacine;
		filesManager.setParent(racine);
		filesManager.update();
		labelTotalFilesSizeValue.setText(UnitConverter.convertSize(filesManager.getTotalFilesSize()));
		labelTotalFilesUpdatedValue.setText(Integer.toString(filesManager.getTotalFilesUpdated()));
		labeltTotalFilesDeletedValue.setText(Integer.toString(filesManager.getTotalFilesDeleted()));
		checkBoxExpandAll.setSelected(false);
		checkBoxSelectAll.setSelected(false);

		AddonSyncTreeModel addonSyncTreeModel = new AddonSyncTreeModel(racine);
		arbre.setModel(addonSyncTreeModel);
		if (racine != null) {
			addonSyncTreeModel.fireTreeStructureChanged();
		}

		int numberRowShown = arbre.getRowCount();
		arbre.setVisibleRowCount(numberRowShown);
		arbre.setPreferredSize(arbre.getPreferredScrollableViewportSize());
		arbreScrollPane.repaint();

		arbre.setEnabled(true);

		// System.out.println("Updating DownloadPanel arbre done.");
	}

	private void onArbreExpanded() {

		int numberRowShown = arbre.getRowCount();
		arbre.setVisibleRowCount(numberRowShown);
		arbre.setPreferredSize(arbre.getPreferredScrollableViewportSize());
		arbreScrollPane.repaint();
		arbre.setSelectionPath(null);
	}

	private void onArbreCollapsed() {

		int numberRowShown = arbre.getRowCount();
		arbre.setVisibleRowCount(numberRowShown);
		arbre.setPreferredSize(arbre.getPreferredScrollableViewportSize());
		arbreScrollPane.repaint();
		arbre.setSelectionPath(null);
	}

	private void selectAllAscending(SyncTreeNodeDTO syncTreeNodeDTO) {

		if (syncTreeNodeDTO != null) {
			syncTreeNodeDTO.setSelected(true);
			SyncTreeNodeDTO parent = syncTreeNodeDTO.getParent();
			selectAllAscending(parent);
		}
	}

	private void unselectAllAscending(SyncTreeNodeDTO syncTreeNodeDTO) {

		if (syncTreeNodeDTO != null) {
			syncTreeNodeDTO.setSelected(false);
			SyncTreeNodeDTO parent = syncTreeNodeDTO.getParent();
			unselectAllAscending(parent);
		}
	}

	private void selectAllDescending(SyncTreeNodeDTO syncTreeNodeDTO) {

		syncTreeNodeDTO.setSelected(true);
		if (!syncTreeNodeDTO.isLeaf()) {
			SyncTreeDirectoryDTO syncTreeDirectoryDTO = (SyncTreeDirectoryDTO) syncTreeNodeDTO;
			for (SyncTreeNodeDTO t : syncTreeDirectoryDTO.getList()) {
				selectAllDescending(t);
			}
		}
	}

	private void deselectAllDescending(SyncTreeNodeDTO syncTreeNodeDTO) {

		syncTreeNodeDTO.setSelected(false);
		if (!syncTreeNodeDTO.isLeaf()) {
			SyncTreeDirectoryDTO syncTreeDirectoryDTO = (SyncTreeDirectoryDTO) syncTreeNodeDTO;
			for (SyncTreeNodeDTO t : syncTreeDirectoryDTO.getList()) {
				deselectAllDescending(t);
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

	private void displayMessage(String message, String title, int messageType) {

		facade.getMainPanel().selectRepository(repositoryName, eventName);
		JOptionPane.showMessageDialog(this, message, title, messageType);
	}

	/* Getters and Setters */

	public JButton getButtonCheckForAddonsStart() {
		return buttonCheckForAddonsStart;
	}

	public JButton getButtonCheckForAddonsCancel() {
		return buttonCheckForAddonsCancel;
	}

	public JProgressBar getProgressBarCheckForAddons() {
		return progressBarCheckForAddons;
	}

	public JLabel getLabelCheckForAddonsStatus() {
		return labelCheckForAddonsStatus;
	}

	public JProgressBar getProgressBarDownloadAddons() {
		return progressBarDownloadAddons;
	}

	public JProgressBar getProgressBarDownloadSingleAddon() {
		return progressBarDownloadSingleAddon;
	}

	public JButton getButtonDownloadStart() {
		return buttonDownloadStart;
	}

	public JButton getButtonDownloadPause() {
		return buttonDownloadPause;
	}

	public JButton getButtonDownloadCancel() {
		return buttonDownloadCancel;
	}

	public JButton getButtonDownloadReport() {
		return buttonDownloadReport;
	}

	public JLabel getLabelDownloadStatus() {
		return labelDownloadStatus;
	}

	public JLabel getLabelDownloadedValue() {
		return labelDownloadedValue;
	}

	public JLabel getLabelSpeedValue() {
		return labelSpeedValue;
	}

	public JLabel getLabelRemainingTimeValue() {
		return labelRemainingTimeValue;
	}

	public JLabel getLabelTotalFilesSizeValue() {
		return labelTotalFilesSizeValue;
	}

	public JLabel getLabelActiveConnectionsValue() {
		return labelActiveConnectionsValue;
	}

	public JTree getArbre() {
		return arbre;
	}

	public JButton getButtonSettings() {
		return buttonSettings;
	}

	public RepositoryPanel getRepositoryPanel() {
		return this.repositoryPanel;
	}

	public JComboBox getComBoxDestinationFolder() {
		return comBoxDestinationFolder;
	}

	public JCheckBox getCheckBoxSelectAll() {
		return checkBoxSelectAll;
	}

	public JCheckBox getCheckBoxExpandAll() {
		return this.checkBoxExpandAll;
	}

	public JCheckBox getCheckBoxExactMatch() {
		return this.checkBoxExactMatch;
	}

	public JCheckBox getCheckBoxAutoDiscover() {
		return this.checkBoxAutoDiscover;
	}

	public JLabel getLabelDiskUsageValue() {
		return this.labelDiskUsageValue;
	}
}

package fr.soe.a3s.ui.main.tree;

import java.awt.Component;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DropMode;
import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.tree.TreePath;

import fr.soe.a3s.dto.TreeDirectoryDTO;
import fr.soe.a3s.dto.TreeLeafDTO;
import fr.soe.a3s.dto.TreeNodeDTO;
import fr.soe.a3s.ui.Facade;

public class TreeDnD2 {

	/* Parameters */
	private final Facade facade;
	private final JTree arbre1, arbre2, arbreDLC;

	/* Constants */
	private DataFlavor TREE_PATH_FLAVOR = new DataFlavor(TreePath.class, "Tree Path");

	private TreePath expandTreePath;

	public TreeDnD2(JTree arbre1, JTree arbre2, JTree arbreDLC, Facade facade) {

		this.facade = facade;
		this.arbre1 = arbre1;
		this.arbre2 = arbre2;
		this.arbreDLC = arbreDLC;

		this.arbre1.setDragEnabled(true);
		this.arbre1.setTransferHandler(new MyTransferHandler());
		this.arbre1.setDropMode(DropMode.INSERT);

		this.arbre2.setDragEnabled(true);
		this.arbre2.setTransferHandler(new MyTransferHandler());
		this.arbre2.setDropMode(DropMode.INSERT);

		this.arbreDLC.setDragEnabled(true);
		this.arbreDLC.setTransferHandler(new MyTransferHandler());
		this.arbreDLC.setDropMode(DropMode.INSERT);

		this.expandTreePath = null;
	}

	/**
	 * Class MyTransferHandler
	 */
	private class MyTransferHandler extends TransferHandler {

		private int ACTION = TransferHandler.MOVE;
		private TreePath[] draggedPaths;

		public MyTransferHandler() {
			super();
		}

		@Override
		protected Transferable createTransferable(final JComponent c) {

			MyTransferable t = null;
			if (c instanceof JTree) {
				JTree tree = (JTree) c;
				draggedPaths = tree.getSelectionPaths();
				t = new MyTransferable(draggedPaths);
			}
			return t;
		}

		@Override
		public boolean importData(final TransferSupport support) {

			expandTreePath = null;

			Component componentDropTarget = support.getComponent();

			// Ensure target drop is arbre2 tree
			boolean ok = false;
			if (componentDropTarget instanceof JTree) {
				JTree treeDropTarget = (JTree) componentDropTarget;
				if (treeDropTarget.equals(arbre2)) {
					ok = true;
				}
			}

			if (!ok) {
				return false;
			}

			// Refuse drop if target drop is null
			JTree.DropLocation loc = (JTree.DropLocation) support.getDropLocation();
			if (loc == null) {
				return false;
			}

			// Get selected paths from arbre1 or arbre2
			TreePath[] treeDragSourcePaths = null;
			try {
				Transferable t = support.getTransferable();
				Object[] data = (Object[]) t.getTransferData(TREE_PATH_FLAVOR);
				if (data == null) {
					return false;
				} else {
					treeDragSourcePaths = (TreePath[]) data;
				}
			} catch (UnsupportedFlavorException | IOException ex) {
				ex.printStackTrace();
				return false;
			}

			// Extract nodes from selected drag paths
			List<TreeLeafDTO> leafs = new ArrayList<TreeLeafDTO>();
			List<TreeDirectoryDTO> directories = new ArrayList<TreeDirectoryDTO>();
			for (TreePath treePath : treeDragSourcePaths) {
				TreeNodeDTO node = (TreeNodeDTO) treePath.getLastPathComponent();
				if (node.isLeaf()) {
					leafs.add((TreeLeafDTO) node);
				} else {
					TreeDirectoryDTO d = (TreeDirectoryDTO) node;
					// Do not accept modset directory from arbre2
					if (d.getModsetType() == null) {
						directories.add((d));
					}
				}
			}

			// Accept drop only for 100% leaf or 100% directory selection (no mix)
			ok = false;
			if (leafs.size() > 0 && directories.size() == 0) {
				ok = true;
			} else if (leafs.size() == 0 && directories.size() > 0) {
				ok = true;
			}

			if (!ok) {
				return false;
			}

			// Accept drop for 100% leaf only if a targeted directory is selected
			TreeNodeDTO targetTreeNodeDTO = (TreeNodeDTO) loc.getPath().getLastPathComponent();
			expandTreePath = loc.getPath();
			if (leafs.size() > 0) {
				if (targetTreeNodeDTO == null) {
					return false;
				} else if (targetTreeNodeDTO.isLeaf()) {
					return false;
				} else {
					TreeDirectoryDTO targetTreeDirectoryDTO = (TreeDirectoryDTO) targetTreeNodeDTO;
					if (targetTreeNodeDTO.equals((TreeDirectoryDTO) arbre2.getModel().getRoot())) {
						return false;
					} else if (targetTreeDirectoryDTO.getModsetType() != null) {
						return false;
					} else {
						targetTreeNodeDTO = targetTreeDirectoryDTO;
					}
				}
			}
			// Force drop on root node if 100% directories are selected
			else if (directories.size() > 0) {
				targetTreeNodeDTO = (TreeDirectoryDTO) arbre2.getModel().getRoot();
				expandTreePath = new TreePath(arbre2.getModel().getRoot());
			}

			TreeDirectoryDTO targetDirectory = (TreeDirectoryDTO) targetTreeNodeDTO;

			// Add selected nodes to target node
			if (leafs.size() > 0) {
				List<TreeLeafDTO> currentLeafs = new ArrayList<TreeLeafDTO>();
				for (TreeNodeDTO node : targetDirectory.getList()) {
					if (node.isLeaf()) {
						currentLeafs.add((TreeLeafDTO) node);
					}
				}
				for (TreeLeafDTO draggedLeaf : leafs) {
					for (TreeLeafDTO currentLeaf : currentLeafs) {
						if (currentLeaf.getName().equals(draggedLeaf.getName())) {
							targetDirectory.removeTreeNode(currentLeaf);
							break;
						}
					}
					TreeLeafDTO newTreeLeaf = new TreeLeafDTO();
					newTreeLeaf.setName(draggedLeaf.getName());
					newTreeLeaf.setParent(targetDirectory);
					targetDirectory.addTreeNode(newTreeLeaf);
				}
			} else if (directories.size() > 0) {
				List<TreeDirectoryDTO> currentDitrectories = new ArrayList<TreeDirectoryDTO>();
				for (TreeNodeDTO node : targetDirectory.getList()) {
					if (!node.isLeaf()) {
						currentDitrectories.add((TreeDirectoryDTO) node);
					}
				}
				for (TreeDirectoryDTO draggedDirectory : directories) {
					for (TreeDirectoryDTO currentDirectory : currentDitrectories) {
						if (currentDirectory.getName().equals(draggedDirectory.getName())) {
							targetDirectory.removeTreeNode(currentDirectory);
							break;
						}
					}
					TreeDirectoryDTO newTreeDirectory = new TreeDirectoryDTO();
					newTreeDirectory.setName(draggedDirectory.getName());
					newTreeDirectory.setParent(targetDirectory);
					targetDirectory.addTreeNode(newTreeDirectory);
					duplicateDirectory(draggedDirectory, newTreeDirectory);
				}
			}

			return true;
		}

		private void duplicateDirectory(TreeDirectoryDTO sourceDirectory, TreeDirectoryDTO duplicateDirectory) {

			List<TreeNodeDTO> list = sourceDirectory.getList();

			for (TreeNodeDTO treeNode : list) {
				if (treeNode.isLeaf()) {
					TreeLeafDTO treeLeafDTO = (TreeLeafDTO) treeNode;
					TreeLeafDTO duplicateLeaf = duplicateLeaf(treeLeafDTO);
					duplicateLeaf.setParent(duplicateDirectory);
					duplicateDirectory.addTreeNode(duplicateLeaf);
				} else {
					TreeDirectoryDTO treeDirectory2 = (TreeDirectoryDTO) treeNode;
					TreeDirectoryDTO duplicateTreedDirectory2 = new TreeDirectoryDTO();
					duplicateTreedDirectory2.setName(treeDirectory2.getName());
					duplicateTreedDirectory2.setParent(duplicateDirectory);
					duplicateDirectory.addTreeNode(duplicateTreedDirectory2);
					duplicateDirectory(treeDirectory2, duplicateTreedDirectory2);
				}
			}
		}

		private TreeLeafDTO duplicateLeaf(TreeLeafDTO treeLeafDTO) {
			TreeLeafDTO duplicateTreeLeaf = new TreeLeafDTO();
			duplicateTreeLeaf.setName(treeLeafDTO.getName());
			return duplicateTreeLeaf;
		}

		@Override
		protected void exportDone(final JComponent source, final Transferable data, final int action) {
			if (expandTreePath != null) {
				facade.getAddonsPanel().getGroupManager().dragAndDrop(true, expandTreePath);
			}
		}

		@Override
		public int getSourceActions(final JComponent c) {
			return ACTION;
		}

		@Override
		public boolean canImport(final TransferSupport supp) {
			if (!supp.isDataFlavorSupported(TREE_PATH_FLAVOR)) {
				return false;
			} else {
				DropLocation loc = supp.getDropLocation();
				return loc != null;
			}
		}
	}

	/**
	 * Class MyTransferable
	 */
	private class MyTransferable implements Transferable {

		private final DataFlavor flavors[] = { TREE_PATH_FLAVOR };
		private final TreePath[] paths;

		public MyTransferable(TreePath[] tp) {
			paths = tp;
		}

		@Override
		public synchronized DataFlavor[] getTransferDataFlavors() {
			return flavors;
		}

		@Override
		public boolean isDataFlavorSupported(DataFlavor flavor) {
			return (flavor.getRepresentationClass() == TreePath.class);
		}

		@Override
		public synchronized Object[] getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
			if (isDataFlavorSupported(flavor)) {
				return paths;
			} else {
				throw new UnsupportedFlavorException(flavor);
			}
		}
	}
}

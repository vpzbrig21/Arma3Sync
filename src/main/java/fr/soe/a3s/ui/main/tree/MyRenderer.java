package fr.soe.a3s.ui.main.tree;

import java.awt.Component;

import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;

import fr.soe.a3s.dto.TreeNodeDTO;
import fr.soe.a3s.ui.UIConstants;
import fr.soe.a3s.ui.icon.Icons;
import fr.soe.a3s.ui.icon.UiIcon;

public class MyRenderer extends DefaultTreeCellRenderer implements UIConstants {
	private static final int TREE_ICON_SIZE = 12;

	@Override
	public Component getTreeCellRendererComponent(JTree tree, Object value,
			boolean sel, boolean expanded, boolean isLeaf, int row,
			boolean hasFocus) {
		super.getTreeCellRendererComponent(tree, value, sel, expanded, isLeaf,
				row, hasFocus);

		TreePath path = tree.getPathForRow(row);
		if (path != null) {
			TreeNodeDTO syncTreeNodeDTO = (TreeNodeDTO) value;
			setIcon(syncTreeNodeDTO, expanded);
		}
		return this;
	}

	private void setIcon(TreeNodeDTO treeNodeDTO, boolean expanded) {

		if (treeNodeDTO.isLeaf()) {
			setIcon(Icons.icon(UiIcon.PACKAGE, TREE_ICON_SIZE));
			// if (leaf.isDuplicate()) {
			// setIcon(new ImageIcon(EXCLAMATION));
			// } else {
			// setIcon(new ImageIcon(BRICK));
			// }
		} else {
			setIcon(Icons.icon(expanded ? UiIcon.FOLDER_OPEN : UiIcon.FOLDER, TREE_ICON_SIZE));
		}
	}
}

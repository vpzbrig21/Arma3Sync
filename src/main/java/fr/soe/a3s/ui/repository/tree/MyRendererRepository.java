package fr.soe.a3s.ui.repository.tree;

import java.awt.Component;

import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;

import fr.soe.a3s.dto.sync.SyncTreeDirectoryDTO;
import fr.soe.a3s.dto.sync.SyncTreeNodeDTO;
import fr.soe.a3s.ui.UIConstants;
import fr.soe.a3s.ui.icon.Icons;
import fr.soe.a3s.ui.icon.UiIcon;

public class MyRendererRepository extends DefaultTreeCellRenderer implements
		UIConstants {
	private static final int TREE_ICON_SIZE = 12;

	@Override
	public Component getTreeCellRendererComponent(JTree tree, Object value,
			boolean sel, boolean expanded, boolean isLeaf, int row,
			boolean hasFocus) {
		super.getTreeCellRendererComponent(tree, value, sel, expanded, isLeaf,
				row, hasFocus);

		TreePath path = tree.getPathForRow(row);
		if (path != null) {
			SyncTreeNodeDTO syncTreeNodeDTO = (SyncTreeNodeDTO) value;
			setIcon(syncTreeNodeDTO, expanded);
		}
		return this;
	}

	private void setIcon(SyncTreeNodeDTO syncTreeNodeDTO, boolean expanded) {
		if (!syncTreeNodeDTO.isLeaf()) {
			SyncTreeDirectoryDTO syncTreeDirectoryDTO = (SyncTreeDirectoryDTO) syncTreeNodeDTO;
			setIcon(Icons.icon(expanded ? UiIcon.FOLDER_OPEN : UiIcon.FOLDER, TREE_ICON_SIZE));

			if (syncTreeDirectoryDTO.isUpdated()
					|| syncTreeDirectoryDTO.isDeleted()
					|| syncTreeDirectoryDTO.isChanged()) {
                                setIcon(Icons.icon(UiIcon.WARNING, TREE_ICON_SIZE));
                        } else if (syncTreeDirectoryDTO.isMarkAsAddon()) {
                                setIcon(Icons.icon(UiIcon.PACKAGE, TREE_ICON_SIZE));
			}

			for (SyncTreeNodeDTO n : syncTreeDirectoryDTO.getList()) {
				if (n.isUpdated() || n.isDeleted()) {
                                        setIcon(Icons.icon(UiIcon.WARNING, TREE_ICON_SIZE));
                                        break;
                                } else if (!n.isLeaf()) {
                                        SyncTreeDirectoryDTO directory = (SyncTreeDirectoryDTO) n;
                                        if (directory.isChanged()) {
                                                setIcon(Icons.icon(UiIcon.WARNING, TREE_ICON_SIZE));
                                                break;
                                        }
                                }
			}
		}
	}
}

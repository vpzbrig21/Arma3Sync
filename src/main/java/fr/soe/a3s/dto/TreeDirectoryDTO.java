package fr.soe.a3s.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import fr.soe.a3s.constant.ModsetType;

public class TreeDirectoryDTO implements TreeNodeDTO, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6087559817135871955L;
	private String name;
	private boolean selected = false;
	private ModsetType modsetType;
	private boolean updated = false;
	private String modsetRepositoryName;
	private TreeDirectoryDTO parent;

	private final List<TreeNodeDTO> list = new ArrayList<TreeNodeDTO>();

	@Override
	public String getName() {
		return name;
	}

	@Override
	public TreeDirectoryDTO getParent() {
		return parent;
	}

	@Override
	public boolean isLeaf() {
		return false;
	}

	@Override
	public boolean isSelected() {
		return selected;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public void setParent(TreeDirectoryDTO parent) {
		this.parent = parent;
	}

	@Override
	public void setSelected(boolean value) {
		this.selected = value;
	}

	public boolean isUpdated() {
		return updated;
	}

	public void setUpdated(boolean updated) {
		this.updated = updated;
	}

	@Override
	public int compareTo(Object other) {
		String name = ((TreeDirectoryDTO) other).getName();
		int result = 1;
		if (name.compareToIgnoreCase(getName()) > 0)
			result = -1;
		else if (name.compareToIgnoreCase(getName()) == 0)
			result = 0;
		return result;
	}

	public void addTreeNode(TreeNodeDTO treeNodeDTO) {

		list.add(treeNodeDTO);

		List<TreeDirectoryDTO> directories = new ArrayList<TreeDirectoryDTO>();
		List<TreeLeafDTO> leafs = new ArrayList<TreeLeafDTO>();
		for (TreeNodeDTO t : list) {
			if (t instanceof TreeDirectoryDTO) {
				directories.add((TreeDirectoryDTO) t);
			} else {
				leafs.add((TreeLeafDTO) t);
			}
		}
		Collections.sort(directories);
		Collections.sort(leafs);
		list.clear();
		list.addAll(directories);
		list.addAll(leafs);
	}

	public void removeTreeNode(TreeNodeDTO treeNodeDTO) {
		list.remove(treeNodeDTO);
	}

	public boolean contains(TreeNodeDTO treeNodeDTO) {

		for (TreeNodeDTO t : list) {
			if (t.getName().equals(treeNodeDTO.getName())) {
				return true;
			}
		}
		return false;
	}

	public List<TreeNodeDTO> getList() {
		return list;
	}

	@Override
	public String toString() {
		String stg = name;
		if (modsetType != null) {
			stg = stg + " " + "(modset)";
		}
		if (updated) {
			stg = stg + " " + "(synchronized)";
		}
		return stg;
	}

	public ModsetType getModsetType() {
		return modsetType;
	}

	public void setModsetType(ModsetType modsetType) {
		this.modsetType = modsetType;
	}

	public String getModsetRepositoryName() {
		return modsetRepositoryName;
	}

	public void setModsetRepositoryName(String modsetRepositoryName) {
		this.modsetRepositoryName = modsetRepositoryName;
	}

}

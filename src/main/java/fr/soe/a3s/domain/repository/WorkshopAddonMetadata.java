package fr.soe.a3s.domain.repository;

import java.io.Serializable;

/** Minimal local metadata retained for one repository's Discord changelog. */
public class WorkshopAddonMetadata implements Serializable {

	private static final long serialVersionUID = 1L;

	private String workshopId;
	private String modName;
	private String folderName;

	public WorkshopAddonMetadata() {
	}

	public WorkshopAddonMetadata(String workshopId, String modName, String folderName) {
		this.workshopId = workshopId;
		this.modName = modName;
		this.folderName = folderName;
	}

	public WorkshopAddonMetadata(WorkshopAddonMetadata other) {
		this(other == null ? null : other.getWorkshopId(),
				other == null ? null : other.getModName(), other == null ? null : other.getFolderName());
	}

	public String getWorkshopId() { return workshopId == null ? "" : workshopId; }
	public void setWorkshopId(String value) { workshopId = value == null ? "" : value.trim(); }
	public String getModName() { return modName == null ? "" : modName; }
	public void setModName(String value) { modName = value == null ? "" : value.trim(); }
	public String getFolderName() { return folderName == null ? "" : folderName; }
	public void setFolderName(String value) { folderName = value == null ? "" : value.trim(); }
}

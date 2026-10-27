package fr.soe.a3s.domain.repository;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Per-repository local cache used to render Discord changelog entries. */
public class WorkshopMetadataCache implements Serializable {

	private static final long serialVersionUID = 1L;

	private List<WorkshopAddonMetadata> currentAddons = new ArrayList<WorkshopAddonMetadata>();
	private Map<Integer, List<WorkshopAddonMetadata>> removedAddonsByRevision =
			new HashMap<Integer, List<WorkshopAddonMetadata>>();

	public WorkshopMetadataCache() {
	}

	public WorkshopMetadataCache(WorkshopMetadataCache other) {
		if (other == null) return;
		setCurrentAddons(other.currentAddons);
		setRemovedAddonsByRevision(other.removedAddonsByRevision);
	}

	public List<WorkshopAddonMetadata> getCurrentAddons() {
		return copyEntries(currentAddons);
	}

	public void setCurrentAddons(List<WorkshopAddonMetadata> entries) {
		currentAddons = copyEntries(entries);
	}

	public Map<Integer, List<WorkshopAddonMetadata>> getRemovedAddonsByRevision() {
		Map<Integer, List<WorkshopAddonMetadata>> copy = new HashMap<Integer, List<WorkshopAddonMetadata>>();
		if (removedAddonsByRevision != null) {
			for (Map.Entry<Integer, List<WorkshopAddonMetadata>> entry : removedAddonsByRevision.entrySet()) {
				copy.put(entry.getKey(), copyEntries(entry.getValue()));
			}
		}
		return copy;
	}

	public void setRemovedAddonsByRevision(Map<Integer, List<WorkshopAddonMetadata>> entries) {
		removedAddonsByRevision = new HashMap<Integer, List<WorkshopAddonMetadata>>();
		if (entries == null) return;
		for (Map.Entry<Integer, List<WorkshopAddonMetadata>> entry : entries.entrySet()) {
			removedAddonsByRevision.put(entry.getKey(), copyEntries(entry.getValue()));
		}
	}

	private static List<WorkshopAddonMetadata> copyEntries(List<WorkshopAddonMetadata> entries) {
		List<WorkshopAddonMetadata> copy = new ArrayList<WorkshopAddonMetadata>();
		if (entries != null) {
			for (WorkshopAddonMetadata entry : entries) {
				if (entry != null) copy.add(new WorkshopAddonMetadata(entry));
			}
		}
		return copy;
	}
}

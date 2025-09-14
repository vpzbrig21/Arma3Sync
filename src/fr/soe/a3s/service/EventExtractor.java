package fr.soe.a3s.service;

import java.util.HashMap;
import java.util.Map;

import fr.soe.a3s.domain.repository.Event;
import fr.soe.a3s.domain.repository.Events;
import fr.soe.a3s.domain.repository.SyncTreeDirectory;
import fr.soe.a3s.domain.repository.SyncTreeNode;

public class EventExtractor {

	private SyncTreeDirectory parent;
	private final Events events;
	private final String eventName;
	private boolean found;

	public EventExtractor(SyncTreeDirectory parent, Events events,
			String eventName) {
		this.parent = parent;
		this.events = events;
		this.eventName = eventName;
	}

	public SyncTreeDirectory run() {

		if (parent != null && events != null && eventName != null) {
			Map<String, Boolean> addonNames = new HashMap<String, Boolean>();
			Map<String, Boolean> userconfigFolderNames = new HashMap<String, Boolean>();
			for (Event event : events.list) {
				if (event.getName().equals(eventName)) {
					addonNames = event.getAddonNames();
					userconfigFolderNames = event.getUserconfigFolderNames();
				}
			}

			SyncTreeDirectory newRacine = new SyncTreeDirectory(
					parent.getName(), null);
			if (!userconfigFolderNames.isEmpty()) {
				refineUserconfig(this.parent, newRacine, userconfigFolderNames);
			}
			if (!addonNames.isEmpty()) {
				refineAddons(this.parent, newRacine, addonNames);
			}
			this.parent = newRacine;
		}

		return this.parent;
	}

	private void refineAddons(SyncTreeDirectory oldRacine,
			SyncTreeDirectory newRacine, Map<String, Boolean> addonNames) {

		for (SyncTreeNode node : oldRacine.getList()) {
			if (!node.isLeaf()) {
				SyncTreeDirectory directory = (SyncTreeDirectory) node;
				if (directory.isMarkAsAddon()
						&& addonNames.containsKey(node.getName())) {
					newRacine.addTreeNode(directory);
					directory.setParent(newRacine);
					directory.setOptional(addonNames.get(node.getName()));
				} else if (!directory.isMarkAsAddon()) {
					this.found = false;
					seek(directory, addonNames);
					if (this.found) {
						SyncTreeDirectory newDirectory = new SyncTreeDirectory(
								directory.getName(), newRacine);
						newRacine.addTreeNode(newDirectory);
						refineAddons(directory, newDirectory, addonNames);
					}
				}
			}
		}
	}

	private void seek(SyncTreeDirectory seakDirectory,
			Map<String, Boolean> addonNames) {

		for (SyncTreeNode node : seakDirectory.getList()) {
			if (!node.isLeaf()) {
				SyncTreeDirectory directory = (SyncTreeDirectory) node;
				if (directory.isMarkAsAddon()
						&& addonNames.containsKey(node.getName())) {
					this.found = true;
					directory.setOptional(addonNames.get(node.getName()));
				} else {
					seek(directory, addonNames);
				}
			}
		}
	}

	private void refineUserconfig(SyncTreeDirectory oldRacine,
			SyncTreeDirectory newRacine,
			Map<String, Boolean> userconfigFolderNames) {

		SyncTreeDirectory userconfigNode = null;

		for (SyncTreeNode node : oldRacine.getList()) {
			if (!node.isLeaf()
					&& node.getName().toLowerCase().equals("userconfig")) {
				userconfigNode = (SyncTreeDirectory) node;
				break;
			}
		}

		if (userconfigNode != null) {
			SyncTreeDirectory newUserconfigNode = new SyncTreeDirectory(
					userconfigNode.getName(), newRacine);
			newRacine.addTreeNode(newUserconfigNode);
			for (SyncTreeNode node : userconfigNode.getList()) {
				if (!node.isLeaf()
						&& userconfigFolderNames.containsKey(node.getName())) {
					newUserconfigNode.addTreeNode(node);
					node.setOptional(userconfigFolderNames.get(node.getName()));
				}
			}
		}
	}
}

package fr.soe.a3s.dto;

import java.util.HashMap;
import java.util.Map;

public class EventDTO implements Comparable<EventDTO> {

	private String name;
	private String description;
	private String repositoryName;
        private Map<String, Boolean> addonNames = new HashMap<String, Boolean>();
        private Map<String, Boolean> dlcNames = new HashMap<String, Boolean>();
        private Map<String, Boolean> userconfigFolderNames = new HashMap<String, Boolean>();

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

        public Map<String, Boolean> getAddonNames() {
                return addonNames;
        }

        public Map<String, Boolean> getDlcNames() {
                return dlcNames;
        }

        public Map<String, Boolean> getUserconfigFolderNames() {
                return userconfigFolderNames;
        }

	public String getRepositoryName() {
		return repositoryName;
	}

	public void setRepositoryName(String repositoryName) {
		this.repositoryName = repositoryName;
	}

	@Override
	public int compareTo(EventDTO other) {
		String otherName = other != null && other.getName() != null ? other.getName() : "";
		String thisName = this.name != null ? this.name : "";
		return thisName.compareToIgnoreCase(otherName);
	}
}

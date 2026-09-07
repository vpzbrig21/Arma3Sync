package fr.soe.a3s.constant;

public enum GameDLCs {

	/**
	 * The enum names are part of the persisted Arma3Sync event format. Keep
	 * them stable for legacy repository compatibility.
	 */
	Contact("", "1021790"),
	GM("Global Mobilization", "1042220"),
	vn("S.O.G Prairie Fire", "1227700"),
	CSLA("Iron Curtain", "1294440"),
	WS("Western Sahara", "1681170"),
	SPE("Spearhead 1944", "1175380"),
	EF("Expeditionary Forces", "2647830"),
	RF("Reaction Forces", "2647760");

	private final String description;
	private final String steamAppId;

	private GameDLCs(String description, String steamAppId) {
		this.description = description;
		this.steamAppId = steamAppId;
	}

	public String GetDescription() {
		return this.description;
	}

	public String getDisplayName() {
		return this.description.isEmpty() ? name() : this.description;
	}

	public String getSteamAppId() {
		return this.steamAppId;
	}

	public String getSteamStoreUrl() {
		return "https://store.steampowered.com/app/" + steamAppId;
	}

	public static GameDLCs fromName(String value) {
		if (value == null) {
			return null;
		}
		for (GameDLCs dlc : values()) {
			if (dlc.name().equalsIgnoreCase(value)) {
				return dlc;
			}
		}
		return null;
	}
}

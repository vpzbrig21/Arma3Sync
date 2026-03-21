package fr.soe.a3s.constant;

public enum LookAndFeel {

	LAF_DEFAULT("Default"), LAF_ALUMINIUM("Aluminium"), LAF_GRAPHITE("Graphite"), LAF_HIFI("Hifi"), LAF_NOIRE(
			"Noire"), LAF_METAL("Metal");

	private String name;

	private LookAndFeel(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public static LookAndFeel getEnum(String lookAndFeel) {

		return switch (lookAndFeel) {
		case "Default" -> LAF_DEFAULT;
		case "Aluminium" -> LAF_ALUMINIUM;
		case "Graphite" -> LAF_GRAPHITE;
		case "Hifi" -> LAF_HIFI;
		case "Noire" -> LAF_NOIRE;
		case "Metal" -> LAF_METAL;
		default -> null;
		};
	}
}

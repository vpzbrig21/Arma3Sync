package fr.soe.a3s.constant;

public enum MinimizationType {
	NOTHING("Do nothing"), TASK_BAR("Reduce to task bar"), TRAY(
			"Reduce to tray"), CLOSE("Close");

	private String description;

	private MinimizationType(String description) {
		this.description = description;
	}

	public String getDescription() {
		return this.description;
	}

	public static MinimizationType getEnum(String designation) {
		return switch (designation) {
		case "Do nothing" -> NOTHING;
		case "Reduce to task bar" -> TASK_BAR;
		case "Reduce to tray" -> TRAY;
		default -> CLOSE;
		};
	}
}

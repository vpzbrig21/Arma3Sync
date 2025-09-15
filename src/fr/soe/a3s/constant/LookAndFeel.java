package fr.soe.a3s.constant;

public enum LookAndFeel {

        LAF_DEFAULT("Default"), LAF_FLATLAF_LIGHT("Flat Light"), LAF_FLATLAF_DARK("Flat Dark"), LAF_METAL("Metal");

	private String name;

	private LookAndFeel(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public static LookAndFeel getEnum(String lookAndFeel) {

                if (lookAndFeel.equals(LAF_DEFAULT.getName())) {
                        return LAF_DEFAULT;
                } else if (lookAndFeel.equals(LAF_FLATLAF_LIGHT.getName())) {
                        return LAF_FLATLAF_LIGHT;
                } else if (lookAndFeel.equals(LAF_FLATLAF_DARK.getName())) {
                        return LAF_FLATLAF_DARK;
                } else if (lookAndFeel.equals(LAF_METAL.getName())) {
                        return LAF_METAL;
                } else {
                        return null;
                }
	}
}

package fr.soe.a3s.ui.icon;

public enum UiIcon {
    APP("app", false, IconTone.ACCENT),
    COMMUNITY("community", false, IconTone.ACCENT),
    ADD("add", true, IconTone.PRIMARY),
    ARROW_DOUBLE_UP("arrow-double-up", true, IconTone.PRIMARY),
    ARROW_UP("arrow-up", true, IconTone.PRIMARY),
    ARROW_DOWN("arrow-down", true, IconTone.PRIMARY),
    CHECK("check", true, IconTone.SUCCESS),
    CLOSE("close", true, IconTone.DANGER),
    CLOSE_STRONG("close-strong", true, IconTone.DANGER),
    DELETE("delete", true, IconTone.DANGER),
    DUPLICATE("duplicate", true, IconTone.PRIMARY),
    DONATE("donate", true, IconTone.ACCENT),
    DOWNLOAD("download", true, IconTone.PRIMARY),
    EDIT("edit", true, IconTone.PRIMARY),
    EVENTS("events", true, IconTone.PRIMARY),
    FILE("file", true, IconTone.PRIMARY),
    FOLDER("folder", false, IconTone.WARNING),
    FOLDER_OPEN("folder-open", false, IconTone.WARNING),
    GLOBE("globe", true, IconTone.INFO),
    HELP("help", true, IconTone.INFO),
    INFO("info", true, IconTone.INFO),
    KEY("key", true, IconTone.PRIMARY),
    LINK("link", true, IconTone.INFO),
    MOON("moon", false, IconTone.PRIMARY),
    PACKAGE("package", true, IconTone.MUTED),
    PAUSE("pause", true, IconTone.PRIMARY),
    PLAY("play", true, IconTone.SUCCESS),
    POWER("power", true, IconTone.ACCENT),
    PROXY("proxy", true, IconTone.PRIMARY),
    RADIO("radio", true, IconTone.PRIMARY),
    REFRESH("refresh", true, IconTone.INFO),
    REPOSITORY("repository", true, IconTone.PRIMARY),
    REPORT("report", true, IconTone.INFO),
    SAVE("save", true, IconTone.PRIMARY),
    SETTINGS("settings", true, IconTone.PRIMARY),
    SHIELD("shield", true, IconTone.INFO),
    SHORTCUT("shortcut", true, IconTone.PRIMARY),
    STOP("stop", true, IconTone.DANGER),
    SUN("sun", false, IconTone.WARNING),
    UPDATE("update", true, IconTone.INFO),
    UPLOAD("upload", true, IconTone.ACCENT),
    WARNING("warning", true, IconTone.WARNING);

    private final String fileName;
    private final boolean themeAware;
    private final IconTone tone;

    UiIcon(String fileName, boolean themeAware, IconTone tone) {
        this.fileName = fileName;
        this.themeAware = themeAware;
        this.tone = tone;
    }

    public String fileName() {
        return fileName;
    }

    public boolean isThemeAware() {
        return themeAware;
    }

    public IconTone tone() {
        return tone;
    }

    public String resourcePath() {
        return "resources/icons/" + fileName + ".svg";
    }
}

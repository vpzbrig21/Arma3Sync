package fr.soe.a3sUpdater.ui;

import fr.soe.a3sUpdater.model.UpdateSource;

public final class Facade {
    private boolean devMode;
    private UpdateSource source = UpdateSource.CONFIGURED;
    public void setDevMode(boolean devMode) { this.devMode = devMode; }
    public boolean isDevMode() { return devMode; }
    public void setSource(UpdateSource source) { this.source = source == null ? UpdateSource.CONFIGURED : source; }
    public UpdateSource getSource() { return source; }
}

package fr.soe.a3sUpdater.dao;

import java.nio.file.Path;
import java.nio.file.Paths;

public interface DataAccessConstants {
    String INSTALLATION_PATH_PROPERTY = "a3s.updater.installationPath";
    String TRANSPORT_PROPERTY = "a3s.updater.protocol";
    String HOST_PROPERTY = "a3s.updater.host";
    String PORT_PROPERTY = "a3s.updater.port";
    String UPDATE_REPOSITORY_PROPERTY = "a3s.updater.repository";
    String UPDATE_REPOSITORY_DEV_PROPERTY = "a3s.updater.repository.dev";
    String HTTP_BASE_URL_PROPERTY = "a3s.updater.url";
    String HTTP_BASE_URL_DEV_PROPERTY = "a3s.updater.url.dev";

    String UPDATE_REPOSITORY = "/ArmA3/ArmA3Sync/download";
    String UPDATE_REPOSITORY_DEV = "/ArmA3/ArmA3Sync/development";
    String UPDTATE_REPOSITORY_ADRESS = "www.sonsofexiled.fr";
    int UPDTATE_REPOSITORY_PORT = 21;
    String UPDTATE_REPOSITORY_LOGIN = "anonymous";
    String UPDTATE_REPOSITORY_PASS = "";

    String CURRENT_UPDATE_BASE_URL = "https://arma3sync.vpzbrig21.de/updates";

    static Path installationPath() {
        String configured = System.getProperty(INSTALLATION_PATH_PROPERTY);
        return Paths.get(configured == null || configured.isBlank()
                ? System.getProperty("user.dir")
                : configured).toAbsolutePath().normalize();
    }
}

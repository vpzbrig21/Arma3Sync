package fr.soe.a3sUpdater.service;

import fr.soe.a3sUpdater.config.UpdateConfig;
import fr.soe.a3sUpdater.dao.DataAccessConstants;
import fr.soe.a3sUpdater.dao.FtpDAO;
import fr.soe.a3sUpdater.dao.GitHubReleaseDAO;
import fr.soe.a3sUpdater.dao.HttpDAO;
import fr.soe.a3sUpdater.dao.JsonManifestDAO;
import fr.soe.a3sUpdater.dao.XmlDAO;
import fr.soe.a3sUpdater.controller.Observateur;
import fr.soe.a3sUpdater.exception.FinderException;
import fr.soe.a3sUpdater.exception.FtpException;
import fr.soe.a3sUpdater.exception.WritingException;
import fr.soe.a3sUpdater.exception.XmlException;
import fr.soe.a3sUpdater.model.UpdateManifest;
import fr.soe.a3sUpdater.model.UpdateSource;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class Service implements DataAccessConstants {
    private final FtpDAO ftpDAO = new FtpDAO();
    private final HttpDAO httpDAO = new HttpDAO();
    private final GitHubReleaseDAO githubReleaseDAO = new GitHubReleaseDAO();
    private final XmlDAO xmlDAO = new XmlDAO();
    private UpdateManifest manifest;
    private UpdateConfig config;
    private boolean manifestDevMode;
    private UpdateSource sourcePreference = UpdateSource.CONFIGURED;
    private FTPClient ftpClient;

    public String getVersion() throws XmlException {
        try {
            UpdateManifest value = getManifest(false);
            return value == null ? null : value.version();
        } catch (Exception exception) {
            throw new XmlException("Can't get update version.", exception);
        }
    }

    public UpdateManifest getManifest(boolean devMode) throws XmlException {
        if (manifest != null && manifestDevMode == devMode) return manifest;
        try {
            config = UpdateConfig.load(installationPath());
            if (useHttp()) {
                manifest = readConfiguredManifest(devMode);
            } else {
                manifest = xmlDAO.readLocal();
            }
            if (manifest == null) throw new IOException("No update metadata was found.");
            manifestDevMode = devMode;
            return manifest;
        } catch (Exception exception) {
            throw new XmlException("Can't read update metadata.", exception);
        }
    }

    public void setSourcePreference(UpdateSource sourcePreference) {
        this.sourcePreference = sourcePreference == null ? UpdateSource.CONFIGURED : sourcePreference;
        this.manifest = null;
    }

    public boolean isUpdateAvailable(boolean devMode) throws XmlException {
        UpdateManifest value = getManifest(devMode);
        return VersionComparator.isNewer(value.version(), currentVersion());
    }

    public String currentVersion() {
        String configured = System.getProperty("a3s.updater.currentVersion");
        if (configured != null && !configured.isBlank()) return configured.trim();
        Path versionFile = installationPath().resolve("version.txt");
        try {
            for (String line : Files.readAllLines(versionFile, StandardCharsets.UTF_8)) {
                if (line.toLowerCase().startsWith("build ")) return line.substring(6).trim();
            }
        } catch (IOException ignored) { }
        return "0.0.0";
    }

    public long getSize(boolean devMode) throws FtpException, FinderException {
        UpdateManifest value;
        try { value = getManifest(devMode); }
        catch (XmlException exception) { throw new FinderException(exception.getMessage()); }
        if (value.fileName() == null || value.fileName().isBlank()) {
            throw new FinderException("Can't determine update archive name.");
        }
        if (useHttp()) {
            try {
                long size = value.size() > 0 ? value.size() : httpDAO.getFileSize(value.downloadUri(), config);
                if (size <= 0) throw new FinderException("Can't find update file on repository.");
                return size;
            } catch (FinderException exception) {
                throw exception;
            } catch (IOException exception) {
                throw new FtpException("Can't find update file on repository.", exception);
            }
        }
        connect();
        try {
            long size = ftpDAO.getFtpFileSize(value.fileName(), ftpClient, devMode);
            if (size <= 0) throw new FinderException("Can't find update file on repository.");
            return size;
        } catch (FinderException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new FtpException("Can't find update file on repository.", exception);
        }
    }

    public void setDownload() throws WritingException {
        try {
            if (manifest == null) getManifest(false);
            if (useHttp()) httpDAO.setDownload(manifest.fileName());
            else ftpDAO.setDownload(manifest.fileName());
        } catch (IOException | XmlException exception) {
            throw new WritingException("Can't prepare update download.", exception);
        }
    }

    public void download(boolean devMode) throws Exception {
        UpdateManifest value = getManifest(devMode);
        if (useHttp()) {
            httpDAO.download(value.downloadUri(), config, value.sha256());
        } else {
            if (ftpClient == null || !ftpClient.isConnected()) connect();
            if (!ftpDAO.download(value.fileName(), ftpClient, devMode)) throw new IOException("Update file not found.");
        }
    }

    public FtpDAO getFtpDAO() { return ftpDAO; }

    public void addDownloadObserver(Observateur observer) {
        if (useHttp()) httpDAO.addObserver(observer); else ftpDAO.addObserver(observer);
    }

    public void install() throws WritingException {
        try {
            if (useHttp()) httpDAO.install(installationPath());
            else ftpDAO.install(installationPath());
        } catch (IOException exception) {
            throw new WritingException("Can't write files to the installation directory.", exception);
        }
    }

    public Path installationPath() { return DataAccessConstants.installationPath(); }

    public void clean() {
        ftpDAO.clean();
        httpDAO.clean();
        if (ftpClient != null && ftpClient.isConnected()) {
            try { ftpClient.logout(); } catch (IOException ignored) { }
            try { ftpClient.disconnect(); } catch (IOException ignored) { }
        }
    }

    /** Requests cancellation of active transport operations without touching staging files. */
    public void cancel() {
        ftpDAO.cancel();
        httpDAO.cancel();
        if (ftpClient != null && ftpClient.isConnected()) {
            try { ftpClient.disconnect(); } catch (IOException ignored) { }
        }
    }

    private void connect() throws FtpException {
        if (ftpClient != null && ftpClient.isConnected()) return;
        String host = System.getProperty(HOST_PROPERTY, UPDTATE_REPOSITORY_ADRESS);
        int port;
        try {
            port = Integer.parseInt(System.getProperty(PORT_PROPERTY, Integer.toString(UPDTATE_REPOSITORY_PORT)));
        } catch (NumberFormatException exception) {
            throw new FtpException("Invalid FTP port.", exception);
        }

        ftpClient = new FTPClient();
        ftpClient.setConnectTimeout(30_000);
        ftpClient.setDefaultTimeout(30_000);
        ftpClient.setDataTimeout(30_000);
        try {
            ftpClient.connect(host, port);
            if (!FTPReply.isPositiveCompletion(ftpClient.getReplyCode())
                    || !ftpClient.login(UPDTATE_REPOSITORY_LOGIN, UPDTATE_REPOSITORY_PASS)) {
                throw new IOException("FTP login failed: " + ftpClient.getReplyString());
            }
            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(org.apache.commons.net.ftp.FTP.BINARY_FILE_TYPE);
        } catch (IOException exception) {
            if (ftpClient.isConnected()) {
                try { ftpClient.disconnect(); } catch (IOException ignored) { }
            }
            throw new FtpException("Fail to connect to remote repository.", exception);
        }
    }

    private boolean useHttp() {
        return !"ftp".equalsIgnoreCase(System.getProperty(TRANSPORT_PROPERTY, "https"));
    }

    private UpdateManifest readConfiguredManifest(boolean devMode) throws Exception {
        Exception githubFailure = null;
        if (config.githubEnabled() && sourcePreference != UpdateSource.MANIFEST) {
            try {
                URI apiUrl = URI.create(config.githubApiUrl(devMode));
                return githubReleaseDAO.read(apiUrl, config.githubAssetPattern(devMode), httpDAO, config);
            } catch (Exception exception) {
                githubFailure = exception;
            }
        }

        String manifestUrl = config.manifestUrl(devMode);
        Exception jsonFailure = null;
        if (manifestUrl != null && !manifestUrl.isBlank()) {
            try {
                URI source = URI.create(manifestUrl);
                String json = httpDAO.readText(source, config);
                return new JsonManifestDAO().read(json, source);
            } catch (Exception exception) {
                jsonFailure = exception;
            }
        }

        String xmlUrl = config.legacyXmlUrl(devMode);
        if (xmlUrl != null && !xmlUrl.isBlank()) {
            try {
                URI source = URI.create(xmlUrl);
                byte[] xml = httpDAO.readText(source, config).getBytes(StandardCharsets.UTF_8);
                return xmlDAO.read(new ByteArrayInputStream(xml), source);
            } catch (Exception ignored) { }
        }

        UpdateManifest local = xmlDAO.readLocal();
        if (local != null) {
            // The local XML is metadata only; HTTP must still fetch the archive from the configured repository.
            String legacyUrl = config.legacyXmlUrl(devMode);
            URI source = legacyUrl == null || legacyUrl.isBlank() ? URI.create(CURRENT_UPDATE_BASE_URL + "/a3s.xml")
                    : URI.create(legacyUrl);
            return new UpdateManifest(local.version(), local.fileName(), source.resolve(local.fileName()),
                    null, 0L, UpdateManifest.Format.XML, local.source());
        }
        if (jsonFailure != null) throw jsonFailure;
        if (githubFailure != null) throw githubFailure;
        return null;
    }
}

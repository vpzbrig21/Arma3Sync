package fr.soe.a3s.dao.connection.sftp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SftpDAOTest {

    @Test
    void normalizesPersistedSftpSchemeAndExtraSlash() {
        String normalized = SftpDAO.normalizeUrl("sftp:///enterprise.vpzbrig21.de/arma3sync/workshop");

        assertEquals("enterprise.vpzbrig21.de/arma3sync/workshop", normalized);
        assertEquals("enterprise.vpzbrig21.de", SftpDAO.getHostname(normalized));
        assertEquals("/arma3sync/workshop", SftpDAO.getRemotePath(normalized));
    }

    @Test
    void keepsConfiguredPortIndependentFromRemotePath() {
        String normalized = SftpDAO.normalizeUrl("enterprise.vpzbrig21.de");

        assertEquals("enterprise.vpzbrig21.de", SftpDAO.getHostname(normalized));
        assertEquals("", SftpDAO.getRemotePath(normalized));
    }
}

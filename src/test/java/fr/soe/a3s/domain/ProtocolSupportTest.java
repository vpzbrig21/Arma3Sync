package fr.soe.a3s.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import fr.soe.a3s.constant.ProtocolType;

class ProtocolSupportTest {

    @Test
    void resolvesSecureUploadProtocols() {
        assertEquals(ProtocolType.FTPS, ProtocolType.getEnum("FTPS"));
        assertEquals(ProtocolType.SFTP, ProtocolType.getEnum("SFTP"));
        assertEquals("21", ProtocolType.FTPS.getDefaultPort());
        assertEquals("22", ProtocolType.SFTP.getDefaultPort());
    }

    @Test
    void createsDedicatedTransportModels() {
        AbstractProtocole ftps = AbstractProtocoleFactory.getProtocol("ftps.example/updates", "21", "user",
                "password", ProtocolType.FTPS, true);
        AbstractProtocole sftp = AbstractProtocoleFactory.getProtocol("sftp.example/updates", "22", "user",
                "password", ProtocolType.SFTP, true);

        assertInstanceOf(Ftp.class, ftps);
        assertTrue(ftps.isValidateSSLCertificate());
        assertInstanceOf(Sftp.class, sftp);
        assertEquals(ProtocolType.SFTP, sftp.getProtocolType());
    }
}

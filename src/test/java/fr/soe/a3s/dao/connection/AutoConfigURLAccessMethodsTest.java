package fr.soe.a3s.dao.connection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import fr.soe.a3s.constant.ProtocolType;
import fr.soe.a3s.domain.AbstractProtocole;
import fr.soe.a3s.exception.CheckException;

class AutoConfigURLAccessMethodsTest {

    @Test
    void parsesHttpsAutoConfigAsSecureHttpsRepository() throws Exception {
        AbstractProtocole protocol = AutoConfigURLAccessMethods
                .parse("  https://a3s.vpzbrig21.de/master/.a3s/autoconfig  ");

        assertEquals(ProtocolType.HTTPS, protocol.getProtocolType());
        assertEquals("a3s.vpzbrig21.de/master/.a3s", protocol.getUrl());
        assertEquals("443", protocol.getPort());
        assertTrue(protocol.isValidateSSLCertificate());
    }

    @Test
    void rejectsUrlsThatOnlyContainAProtocolNameInThePath() {
        assertThrows(CheckException.class,
                () -> AutoConfigURLAccessMethods.parse("repo.example/http://autoconfig"));
    }
}

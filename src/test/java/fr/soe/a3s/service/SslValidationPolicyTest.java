package fr.soe.a3s.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SslValidationPolicyTest {

    @Test
    void allowsOnlyLocalHostsWithoutExplicitOverride() {
        String previous = System.getProperty("a3s.allowInsecureSsl");
        try {
            System.clearProperty("a3s.allowInsecureSsl");
            assertTrue(SslValidationPolicy.isAllowedFor("localhost/repository"));
            assertTrue(SslValidationPolicy.isAllowedFor("127.0.0.1"));
            assertFalse(SslValidationPolicy.isAllowedFor("updates.example/repository"));
        } finally {
            if (previous == null) System.clearProperty("a3s.allowInsecureSsl");
            else System.setProperty("a3s.allowInsecureSsl", previous);
        }
    }
}

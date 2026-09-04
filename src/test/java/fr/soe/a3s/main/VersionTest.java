package fr.soe.a3s.main;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Year;

import org.junit.jupiter.api.Test;

class VersionTest {

    @Test
    void copyrightYearEndsWithCurrentYear() {
        assertTrue(Version.getYear().endsWith(Integer.toString(Year.now().getValue())));
    }
}

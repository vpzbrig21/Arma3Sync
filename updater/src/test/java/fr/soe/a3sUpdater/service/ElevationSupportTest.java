package fr.soe.a3sUpdater.service;

import org.junit.jupiter.api.Test;

import java.nio.file.AccessDeniedException;
import java.nio.file.FileSystemException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ElevationSupportTest {
    @Test
    void detectsAccessDeniedExceptionInCauseChain() {
        assertTrue(ElevationSupport.isPermissionFailure(
                new IllegalStateException("install failed", new AccessDeniedException("Program Files"))));
    }

    @Test
    void detectsLocalizedFileSystemPermissionMessage() {
        assertTrue(ElevationSupport.isPermissionFailure(
                new FileSystemException("Arma3Sync.jar", null, "Zugriff verweigert")));
    }

    @Test
    void doesNotTreatFileLocksAsPermissionFailures() {
        assertFalse(ElevationSupport.isPermissionFailure(
                new FileSystemException("Arma3Sync.jar", null, "The process cannot access the file")));
    }
}

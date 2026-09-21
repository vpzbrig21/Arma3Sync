package fr.soe.a3sUpdater.service;

import org.junit.jupiter.api.Test;
import fr.soe.a3sUpdater.model.UpdateSource;

import java.nio.file.AccessDeniedException;
import java.nio.file.FileSystemException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.List;

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

    @Test
    void doesNotTreatMissingExtractedSourceAsPermissionFailure() {
        assertFalse(ElevationSupport.isPermissionFailure(
                new NoSuchFileException("extracted/runtime/legal/java.base")));
    }

    @Test
    void buildsUacCommandForUpdaterInsteadOfNativeLauncher() {
        String command = ElevationSupport.buildPowerShellCommand(
                Path.of("C:\\Users\\Daniel\\AppData\\Local\\Arma3Sync\\logs\\uac-start.ps1"),
                Path.of("C:\\Program Files (x86)\\ArmA3Sync"), false);

        assertTrue(command.contains("-Verb RunAs"));
        assertTrue(command.contains("-WindowStyle Hidden"));
        assertTrue(command.contains("powershell.exe"));
        assertTrue(command.contains("uac-start.ps1"));
        assertFalse(command.contains("-Wait"));
    }

    @Test
    void buildsElevatedScriptForUpdaterJavaProcess() {
        String script = ElevationSupport.buildElevatedScript(
                Path.of("C:\\Program Files (x86)\\ArmA3Sync\\runtime\\bin\\javaw.exe"),
                List.of("-Da3s.updater.installationPath=C:\\Program Files (x86)\\ArmA3Sync",
                        "-Da3s.updater.elevationMarker=C:\\Users\\Daniel\\AppData\\Local\\Arma3Sync\\logs\\uac.startup",
                        "-jar", "C:\\Program Files (x86)\\ArmA3Sync\\ArmA3Sync-Updater.jar"));

        assertTrue(script.contains("javaw.exe"));
        assertTrue(script.contains("-Da3s.updater.elevationMarker"));
        assertTrue(script.contains("ArmA3Sync-Updater.jar"));
        assertTrue(script.contains("Start-Process"));
        assertTrue(script.contains("-Wait -PassThru"));
        assertTrue(script.contains("\"-Da3s.updater.installationPath=C:\\Program Files (x86)\\ArmA3Sync\""));
        assertTrue(script.contains("$PSCommandPath"));
        assertFalse(script.contains("Arma3Sync.exe"));
    }

    @Test
    void elevatedInvocationIsExplicitlyRepresentedInJavaArguments() {
        String script = ElevationSupport.buildElevatedScript(
                Path.of("C:\\Program Files (x86)\\ArmA3Sync\\runtime\\bin\\javaw.exe"),
                List.of("-Da3s.updater.elevated=true", "-jar", "ArmA3Sync-Updater.jar"));

        assertTrue(script.contains("-Da3s.updater.elevated=true"));
    }

    @Test
    void writeProbeDoesNotLeaveFilesBehind() throws Exception {
        Path installation = Files.createTempDirectory("a3s-installation-");
        try {
            assertTrue(ElevationSupport.canWriteInstallation(installation));
            try (var files = Files.list(installation)) {
                assertTrue(files.findAny().isEmpty());
            }
        } finally {
            Files.deleteIfExists(installation);
        }
    }

    @Test
    void temporaryRunnerWaitsForOriginalUpdaterAndCleansItsRuntime() {
        String script = ElevationSupport.buildElevatedScript(
                Path.of("C:\\Users\\Daniel\\AppData\\Local\\Arma3Sync\\update-runner\\runtime\\bin\\javaw.exe"),
                List.of("-Da3s.updater.elevated=true", "-jar",
                        "C:\\Users\\Daniel\\AppData\\Local\\Arma3Sync\\update-runner\\ArmA3Sync-Updater.jar"),
                Path.of("C:\\Users\\Daniel\\AppData\\Local\\Arma3Sync\\update-runner"),
                1234L,
                Path.of("C:\\Users\\Daniel\\AppData\\Local\\Arma3Sync\\logs\\uac.startup"));

        assertTrue(script.contains("Wait-Process -Id 1234"));
        assertTrue(script.contains("-WorkingDirectory"));
        assertTrue(script.contains("-Recurse -Force"));
        assertTrue(script.contains("update-runner"));
        assertTrue(script.contains("uac.startup"));
    }

    @Test
    void doesNotRequestWindowsElevationOnLinux() {
        String originalOsName = System.getProperty("os.name");
        try {
            System.setProperty("os.name", "Linux");
            assertFalse(ElevationSupport.isWindows());
            assertFalse(ElevationSupport.requiresElevation(Path.of("/opt/arma3sync")));
            assertFalse(ElevationSupport.restart(UpdateSource.CONFIGURED, false,
                    Path.of("/opt/arma3sync")).started());
        } finally {
            if (originalOsName == null) System.clearProperty("os.name");
            else System.setProperty("os.name", originalOsName);
        }
    }
}

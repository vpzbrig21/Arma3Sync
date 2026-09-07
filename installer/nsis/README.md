# NSIS packaging

`Arma3Sync.nsi` packages the Gradle distribution and the rebuilt
`ArmA3Sync-Updater.jar` into a Windows installation. It additionally restores
the original root-level batch/shell entry points, `a3s.xml`, version and
license/readme files.

The canonical icon is `src/main/resources/resources/icons/app.svg`. The
versioned multi-resolution `installer/assets/Arma3Sync.ico` is used by NSIS and
jpackage. Regenerate both derived icon files with
`installer/nsis/generate-icons.ps1` when the SVG changes.
The standard profile creates a minimized Java 25 runtime with
`jlink` and installs it below `runtime\`. It uses the same native
`Arma3Sync.exe` bootstrapper and the same root-level `Arma3Sync.jar` as the
compact profile. This is intentional: the updater therefore updates the JAR
that the installed launcher actually starts.

The compact profile does not bundle Java. It uses a native
`Arma3Sync.exe` bootstrapper, which searches JAVA_HOME, PATH, the Windows
registry and common Java installation directories. It verifies Java 25 or
newer before starting the real application `Arma3Sync.jar` from the
installation root. Its manifest points to the dependency JARs below `lib`.
Build the standard variant with
`installer/nsis/build-installer.ps1`.
Build the compact variant with
`installer/nsis/build-installer.ps1 -Compact`.
The resulting files are named `Arma3Sync-<version>-setup.exe` and
`Arma3Sync-<version>-compact-setup.exe` respectively.

NSIS already uses solid LZMA compression. Further compression gives only a
small benefit because the payload consists mainly of already-compressed JAR
files; the major size difference was the bundled Java runtime.

Required inputs:

- `build/install/Arma3Sync`
- `updater/build/distribution/ArmA3Sync-Updater.jar`
- `makensis` in `PATH` or `C:\Program Files (x86)\NSIS\makensis.exe`

Both profiles target the same native Java-aware bootstrapper. Compact searches
for Java 25 or newer in the installation environment. The standard package checks
`runtime\bin\java.exe` first and never falls back to an incompatible Java 8
installation when the bundled runtime is present.

The bootstrapper is deliberately shared between both profiles. Its build uses
the Go linker size flags and excludes parser code that is not needed for the
launcher, while retaining the same Java discovery, Java 25 validation, error
handling, and root-level JAR launch behavior in Standard and Compact releases.

The installation also contains `resources/configuration/updater.toml`. GitHub
Releases are enabled by default for new installations and are tried before
the JSON manifest. Arma3Sync passes the selected source as `-github` or
`-manifest` to each update operation. GitHub still requires the exact
configured ZIP asset and its SHA-256 digest; an explicit `enabled = false`
disables it. When disabled or unavailable, the updater uses the JSON manifest
and keeps `a3s.xml` as the unchanged legacy fallback. The file is only
created when absent during an upgrade, so custom source settings survive
reinstallations and ZIP-based updates.

The release version is maintained only in `version.properties`.
`build-installer.ps1` regenerates the NSIS include and payload version file
before invoking Gradle and NSIS. It also verifies that the expected installer
was actually created and is non-empty.

The modern application stores mutable data in per-user locations and the
uninstaller does not remove them: `%APPDATA%\Arma3Sync` contains configuration
and profiles, while `%LOCALAPPDATA%\Arma3Sync` contains repositories and cache
data. The effective `updater.toml` is copied to the per-user configuration
directory so custom update URLs remain editable without elevation; the copy in
the installation is the default fallback. Legacy `profiles` and `resources/*`
directories are also preserved when upgrading or uninstalling.

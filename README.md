# Arma3Sync

> Community continuation and fork of the original Arma3Sync launcher by Sons of Exiled.  
> Maintained by volunteer contributors (current maintainer: **vpzbrig21**) to keep the tool modern, CDLC-ready, and compatible with current Bohemia updates.

---

## Contents

1. [Overview](#overview)
2. [Features](#features)
3. [Current Version](#current-version)
4. [Getting Started (Development Environment)](#getting-started-development-environment)
5. [Build & Packaging](#build--packaging)
6. [Updater and Releases](#updater-and-releases)
7. [Reporting Issues / Contributing](#reporting-issues--contributing)
8. [Credits](#credits)
9. [Licence](#licence)

---

## Overview

Arma3Sync is a cross-platform Java launcher and repository manager for Bohemia Interactive’s Arma 3. It enables communities to synchronise mods, configure launch presets, and distribute content from HTTP/HTTPS/FTP/FTPS/SFTP repositories. This fork contains the complete CDLC matrix; no manual code changes are required to support Bohemia’s official DLC/ CDLC catalogue.

## Features

- **Repository management** – build, upload, and verify custom repositories with automated integrity checks.
- **Secure repository uploads** – SFTP (SSH transport, normally port 22) is the
  default secure upload option. FTP, HTTP/HTTPS WebDAV remain selectable. FTPS
  support is retained in the code and for existing configurations, but is
  temporarily hidden from new upload selections while FileZilla TLS data-channel
  compatibility is being finalized. SFTP currently uses password authentication.
- **Parallel repository uploads** – FTP and SFTP uploads can use 1–10 separate
  upload connections. The default is 4; this setting is independent of the
  repository-check and download connection settings. The server must allow the
  selected number of simultaneous sessions.
- **Client launcher** – profile handling, addon prioritisation, command-line toggles, favourite servers, and external utilities.
- **CDLC integration** – all official DLC/CDLC entries preconfigured.
- **Cross-platform packaging** – fat JAR, Linux app-image, Compact-Installer
  and Windows-Standard-Installer with a bundled Java runtime.

## Current Version

- **Stable:** `2026.2.6`
- **Development beta:** `2026.3.13-Beta`
- Release notes: [CHANGELOG.md](CHANGELOG.md)
- Release artefacts are produced via:
  - `gradle fatJar`
  - `gradle jpackageWin`
  - `gradle jpackageLinux`
  - `powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\release\release.ps1`

---

## Getting Started (Development Environment)

1. **Clone the repository**
   ```bash
   git clone https://github.com/vpzbrig21/Arma3Sync.git
   cd Arma3Sync
   ```

2. **Prerequisites**
   - JDK 25 (including `jpackage` and `jlink`).
   - Global Gradle 8 or newer; this project intentionally does not ship a
     Gradle Wrapper.
   - Git, a Java IDE (IntelliJ IDEA/Eclipse) optional but recommended.

3. **IDE setup (optional)**
   - Import as Gradle project; the main application targets Java 25 and the
     independent updater subproject targets Java 21 for runtime compatibility.
   - Run configuration entry point: `fr.soe.a3s.main.ArmA3Sync`.

4. **Installer assets**
   - The canonical icon source is `src/main/resources/resources/icons/app.svg`.
     Generate the tracked installer assets under `installer/assets/` with
     `installer/nsis/generate-icons.ps1` when the SVG changes.
   - Required only for `jpackage` tasks; not needed to run from IDE or fat JAR.

5. **Dependencies**
   - Managed entirely through Gradle (`build.gradle`); no manual `libs/` directory needed.

---

## Build & Packaging

Download binaries from GitHub Releases if you only need the launcher. To build locally:

### Standard build
```bash
gradle clean build
```
Compiles sources, runs tests (JUnit 5), and produces jars in `build/libs/`.

### Fat JAR (self-contained)
```bash
gradle fatJar
```
Outputs `build/libs/Arma3Sync-all.jar` with dependencies bundled.

### Windows installer (`jpackage`)
```bash
gradle jpackageWin
```
Produces an `.exe` installer inside `build/jpackage/windows/`. Requires the Windows icon mentioned above.

### Windows release packages

For normal users, build the Standard package. It bundles a reduced Java 25
runtime, so no separate Java installation is required:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\release\release.ps1
```

The Standard package does not require Java to be preinstalled on Windows.

The Compact package remains available for users who already manage Java:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\release\release.ps1 -Compact
```

The user-facing term is **Java runtime 25+**. The build uses JDK 25 because it
provides `jlink`; the Standard output contains only the runtime components
needed by Arma3Sync. Both Windows variants use the same `Arma3Sync.exe`
bootstrapper and root-level `Arma3Sync.jar`, which keeps updater behavior
consistent.

Technical packaging details and the release test checklist are documented in
[`docs/JAVA_RUNTIME_PACKAGING.md`](docs/JAVA_RUNTIME_PACKAGING.md).

### Linux app-image (`jpackage`)
```bash
gradle jpackageLinux
```
Creates an app-image directory inside `build/jpackage/linux/`. Requires the PNG icon.

> **Troubleshooting:** If Gradle fails with `native-platform.dll` errors or `jpackage` is missing, update/reinstall your JDK/Gradle environment. This project intentionally uses the globally installed Gradle command; no Gradle Wrapper is maintained.

## Updater and Releases

The release package includes `ArmA3Sync-Updater.jar`. The updater source is an
independent Gradle subproject in [`updater/`](updater/), while the installer and
release automation live in [`installer/nsis/`](installer/nsis/) and
[`release/`](release/). It reads
`resources/configuration/updater.toml` from the installation. A user-specific
copy under `%APPDATA%\Arma3Sync\configuration\updater.toml` takes precedence on
Windows; on Linux and other Unix systems the corresponding XDG configuration
directory is used.

The GitHub Releases source is enabled by default for new installations. The
shipped configuration keeps the HTTPS JSON source and unchanged `a3s.xml`
fallback as alternatives:

```toml
[update]
manifest_url = "https://arma3sync.vpzbrig21.de/updates/a3s.json"
legacy_xml_url = "https://arma3sync.vpzbrig21.de/updates/a3s.xml"
dev_manifest_url = "https://arma3sync.vpzbrig21.de/updates/a3s-dev.json"
dev_legacy_xml_url = "https://arma3sync.vpzbrig21.de/updates/a3s.xml"
allow_http = false
connect_timeout_ms = 30000
read_timeout_ms = 30000

[github]
enabled = true
api_url = "https://api.github.com/repos/vpzbrig21/Arma3Sync/releases/latest"
dev_api_url = "https://api.github.com/repos/vpzbrig21/Arma3Sync/releases/latest"
asset_pattern = "Arma3Sync-{version}.zip"
dev_asset_pattern = "Arma3Sync-{version}.zip"
```

To disable GitHub Releases, set `enabled = false`. Arma3Sync passes the
selected source to the updater for both the update check and the download:
`-github` when the preference is enabled and `-manifest` otherwise. An
explicit `enabled = false` in `updater.toml` remains authoritative, so a
disabled GitHub source cannot be forced by the UI. When GitHub is enabled,
publish an exact matching asset such as `Arma3Sync-2026.2.6.zip`. `{version}` is replaced with the
release tag without a leading `v`; `{tag}` can be used when the asset name
should retain the tag, for example `Arma3Sync-{tag}.zip`. The updater selects
only the configured ZIP, uses its GitHub download URL and verifies the
SHA-256 digest before installing it. A missing release, asset or digest falls
back to the configured JSON source and then to `a3s.xml`.

GitHub's `latest` endpoint uses the newest published stable release and does
not select drafts or pre-releases. The complete updater configuration,
fallback behavior and release procedure are documented in
[`UPDATER.md`](UPDATER.md). The release procedure is also summarized in
[`release/README.md`](release/README.md).

For repository HTTPS connections, certificate validation should remain enabled.
Disabling it is limited to local test hosts. A remote development target requires
the explicit JVM property `-Da3s.allowInsecureSsl=true` and should never be used
for normal production connections.

### Diagnostic logging

For a reproducible support report, start Arma3Sync once with the optional
`-debug` parameter:

```powershell
Arma3Sync.exe -debug
```

The parameter can be combined with existing modes, for example
`Arma3Sync.exe -debug -console` or `Arma3Sync.exe -debug -check "Repository name"`.
Debug logging is disabled by default. The rotating log is written to
`%APPDATA%\Arma3Sync\configuration\arma3sync-debug.log.0`; portable installations
use their local `resources\configuration` folder instead. Older generations use
the suffixes `.1` and `.2`. It records startup,
repository checks, upload phases, protocol connection steps, timings and errors,
but never passwords. Attach this file together with the reproduction steps when
reporting an upload or connection problem.

---

## Reporting Issues / Contributing

- Use GitHub Issues for bug reports or feature requests. Include logs (`resources/configuration/a3s.cfg`, Arma 3 RPT excerpts) and reproduction steps.
- Verify on the latest `main` build (`gradle clean build`) before filing new issues to avoid duplicates.
- Pull requests are welcome. Keep them focused, reference an issue, and document any user-facing changes.
- Security-sensitive reports: contact maintainers privately via GitHub profiles.

---

## Credits

Thanks to the [s.o.E] team – Sons of Exiled (sonsofexiled.fr)

* [s.o.E] Major_Shepard – Software Conception
* [s.o.E] Matt2507 – Graphic Conception & Web Development
* [s.o.E], [BWF] & [F27] members – Testing

Special thanks to https://squadalpha.es/ for the original fork and continued testing efforts, plus the current maintainer vpzbrig21 and community testers keeping Arma3Sync alive.

---

## Licence

ArmA3Sync is distributed under the GNU General Public Licence (GPL) v3.

Portions of the project include:

- Icons and branding assets sourced from the original Arma3Sync (GPLv3) project.
- Third-party libraries declared in `build.gradle` (Apache Commons, FlatLaf, Sardine, etc.) retain their original licences; consult their documentation for details.
- Apache MINA SSHD is included for SFTP transport and retains its Apache-2.0 licence.
- Game titles, DLC names, and trademarks belong to Bohemia Interactive; all rights reserved to their respective owners.


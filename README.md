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
6. [Reporting Issues / Contributing](#reporting-issues--contributing)
7. [Credits](#credits)
8. [Licence](#licence)

---

## Overview

Arma3Sync is a cross-platform Java launcher and repository manager for Bohemia Interactive’s Arma 3. It enables communities to synchronise mods, configure launch presets, and distribute content from HTTP/HTTPS/FTP repositories. This fork contains the complete CDLC matrix; no manual code changes are required to support Bohemia’s official DLC/ CDLC catalogue.

## Features

- **Repository management** – build, upload, and verify custom repositories with automated integrity checks.
- **Client launcher** – profile handling, addon prioritisation, command-line toggles, favourite servers, and external utilities.
- **CDLC integration** – all official DLC/CDLC entries preconfigured.
- **Cross-platform packaging** – fat JAR, Linux app-image, and Windows installer via `jpackage`.

## Current Version

- **Stable:** `2026.1.1`
- Release notes: [CHANGELOG.md](CHANGELOG.md)
- Release artefacts are produced via:
  - `gradle fatJar`
  - `gradle jpackageWin`
  - `gradle jpackageLinux`

---

## Getting Started (Development Environment)

1. **Clone the repository**
   ```bash
   git clone https://github.com/vpzbrig21/Arma3Sync.git
   cd Arma3Sync
   ```

2. **Prerequisites**
   - JDK 21 or newer (with `jpackage` component).
   - Gradle 9.4.1 (system install) or use the wrapper shipped with the project.
   - Git, a Java IDE (IntelliJ IDEA/Eclipse) optional but recommended.

3. **IDE setup (optional)**
   - Import as Gradle project; the toolchain automatically targets Java 21.
   - Run configuration entry point: `fr.soe.a3s.main.ArmA3Sync`.

4. **Installer assets**
   - Create local icons under `packaging/icons/` (ignored by Git):
     - `Arma3Sync.png` for Linux
     - `Arma3Sync.ico` for Windows
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

### Linux app-image (`jpackage`)
```bash
gradle jpackageLinux
```
Creates an app-image directory inside `build/jpackage/linux/`. Requires the PNG icon.

> **Troubleshooting:** If Gradle fails with `native-platform.dll` errors or `jpackage` is missing, update/reinstall your JDK/Gradle environment or use the included wrapper.

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
- Game titles, DLC names, and trademarks belong to Bohemia Interactive; all rights reserved to their respective owners.


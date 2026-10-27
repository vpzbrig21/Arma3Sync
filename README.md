# Arma3Sync

Community-maintained continuation of the Arma3Sync launcher by Sons of Exiled.
Arma3Sync helps Arma 3 communities manage mod repositories, synchronize game
content and launch the game with saved profiles.

[![Latest release](https://img.shields.io/github/v/release/vpzbrig21/Arma3Sync?display_name=tag)](https://github.com/vpzbrig21/Arma3Sync/releases/latest)
[![Downloads](https://img.shields.io/github/downloads/vpzbrig21/Arma3Sync/total?label=downloads)](https://github.com/vpzbrig21/Arma3Sync/releases)
[![License: GPL-3.0](https://img.shields.io/badge/license-GPL--3.0-blue.svg)](LICENCE.md)
[![PayPal](https://img.shields.io/badge/PayPal-support%20development-00457C?logo=paypal&logoColor=white)](https://paypal.me/sorotech)

## Download

Download the latest release from [GitHub Releases](https://github.com/vpzbrig21/Arma3Sync/releases/latest).
For most Windows users, choose the **Standard** package: it includes a Java 25
runtime. The **Compact** package is intended for systems that already have Java
25 or newer installed. Linux users can run the Linux app image when available or
build it from source.

The downloads badge above reports the total number of files downloaded from
GitHub Releases.

## Using the GUI

The graphical application is the main way to use Arma3Sync. It brings repository
management, synchronization, profiles and game launch options together in one
place.

Typical first steps:

1. Open Arma3Sync and add a community repository or create one you will manage.
2. If you manage the repository, select its local folder and run **Build**. If
   you use a community repository, check it and synchronize its content.
3. Configure a launch profile for Arma 3, then start the game from that profile.

Repository changelogs can also be sent to Discord from the changelog view. Open
**Discord settings** for the repository to configure its webhook, display name,
title, images, emojis and optional mentions. See the
[Discord webhook guide](docs/DISCORD_CHANGELOG_WEBHOOK.md) for setup details.

## Features

- GUI for creating, building, checking and publishing game repositories.
- Synchronization of repository content for community members.
- Arma 3 profiles, addon order, launch parameters and favorite servers.
- Import and export of mod presets for the official Arma 3 Launcher.
- Support for the official DLC and Creator DLC catalogue.
- HTTP, HTTPS, FTP and SFTP repository support; existing FTPS configurations
  are retained.
- FTP and SFTP uploads with up to 10 parallel connections. SFTP uses SSH and
  password authentication.
- Per-repository Discord changelog webhooks with configurable appearance and
  notifications.

## Guides

- [Repository and synchronization guide](docs/CLI.md) — GUI, Console and CLI
  repository workflows.
- [Discord changelog webhook](docs/DISCORD_CHANGELOG_WEBHOOK.md) — webhook
  creation, embeds, emojis, role/user mentions and automation.
- [Java runtime packaging](docs/JAVA_RUNTIME_PACKAGING.md) — Standard and
  Compact package details.
- [Updater configuration](UPDATER.md) — GitHub Releases, JSON manifests and
  legacy `a3s.xml` fallback.
- [Release build guide](release/README.md) — create and verify release packages.
- [German changelog](CHANGELOGS_DE.md) · [English changelog](CHANGELOGS_EN.md)

## Console and CLI

The Console and CLI are available for server administration and automation.
They complement the GUI; some setup and management actions are only available
in the interactive Console.

Start the interactive Console from the installation directory:

| Platform | Command |
| --- | --- |
| Windows | `Arma3Sync-console.bat` |
| Linux | `./Arma3Sync-console.sh` |

The interactive Console supports `NEW`, `BUILD`, `CHECK`, `DELETE`, `UPDATE`,
`DISCORDSETUP`, and `DISCORD`, among other commands. For example, to set up
Discord without using the GUI, enter `DISCORDSETUP`, select a repository and
follow the prompts. To send the latest build changelog, enter `DISCORD`.

Some operations can also run as non-interactive CLI commands:

| Operation | Interactive Console | CLI |
| --- | --- | --- |
| Create repository | `NEW` | Not available |
| Build repository | `BUILD` | `-build "Repository Name"` |
| Check repository | `CHECK` | `-check "Repository Name"` |
| Delete repository | `DELETE` | Not available |
| Check for updates | `UPDATE` | `-update` |
| Configure Discord webhook | `DISCORDSETUP` | Not available; configure interactively first |
| Send newest build changelog | `DISCORD` | `-discord "Repository Name"` |
| Synchronize to a folder | `SYNC` | `-sync "Repository Name" "Destination" true/false` |
| Extract `.bikey` files | `EXTRACT` | `-extract "Source" "Destination"` |

Example automated Discord send after a successful build:

```bat
Arma3Sync-console.bat -build "Repository Name"
Arma3Sync-console.bat -discord "Repository Name"
```

```sh
./Arma3Sync-console.sh -build "Repository Name"
./Arma3Sync-console.sh -discord "Repository Name"
```

The webhook URL is stored in that repository's local settings and is not passed
as a command-line argument. Full instructions and platform-specific examples are
in the [Console and CLI guide](docs/CLI.md).

## Build from source

### Requirements

- JDK 25 with `jpackage` and `jlink`.
- Global Gradle 8 or newer; this project intentionally does not include a Gradle
  Wrapper.
- Git. A Java IDE is optional.

### Compile and package

```bash
gradle clean build
```

Build a self-contained JAR with `gradle fatJar`. To create Windows Standard
installer and ZIP packages, run:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\release\release.ps1
```

Add `-Compact` to create the Compact installer and ZIP. To build a Linux app
image, run `gradle jpackageLinux`. Packaging requirements and release checks are
documented in the [release guide](release/README.md).

The application entry point is `fr.soe.a3s.main.ArmA3Sync`. Dependencies are
managed in `build.gradle`; installer icons are generated from
`src/main/resources/resources/icons/app.svg` using
`installer/nsis/generate-icons.ps1`.

## Updates and support

New installations check GitHub Releases by default. The updater verifies the
configured ZIP with SHA-256 and retains JSON-manifest and `a3s.xml` sources as
fallbacks. See [UPDATER.md](UPDATER.md) for configuration details.

- Report bugs and request features through [GitHub Issues](https://github.com/vpzbrig21/Arma3Sync/issues).
- Pull requests are welcome; include context and reproduction or verification
  steps for user-facing changes.
- Diagnostic logging can be enabled with `-debug`; it avoids writing passwords.

## Credits and license

Arma3Sync is distributed under the [GNU General Public License v3](LICENCE.md).
This project continues the work of the [Sons of Exiled team](https://www.sonsofexiled.fr/).
Thanks to Major_Shepard, Matt2507, the [BWF] and [F27] communities, squadalpha.es,
vpzbrig21 and the community contributors.

Game names and trademarks belong to Bohemia Interactive. Third-party libraries
and artwork retain their own licenses; see the bundled license files and
dependency declarations.

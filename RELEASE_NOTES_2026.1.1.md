# Arma3Sync `2026.1.1`

## Modern continuation of Arma3Sync `1.7.107`

Arma3Sync `2026.1.1` is the modern continuation of the original
Arma3Sync `1.7.107`. The established workflow for repository management,
addon synchronization, profiles and launching Arma 3 remains available while
the application has been updated for current systems and a maintainable
release process.

## Highlights

- Updated runtime baseline for Java 25.
- Centralized version management using `version.properties`.
- Reproducible Gradle-based builds and automated release packaging.
- Cross-platform startup remains available with:

  ```text
  java -jar Arma3Sync.jar
  ```

- The compact Windows package does not bundle a Java runtime and starts the
  root-level `Arma3Sync.jar` through `Arma3Sync.exe`.
- An optional standalone Windows package includes its own minimized Java
  runtime and can run without a separately installed Java runtime.

## Update system

- New user-configurable `updater.toml`.
- SHA-256-protected JSON update manifests via `a3s.json`.
- Optional GitHub Releases support, disabled by default and configurable per
  installation.
- GitHub updates select the exact configured release asset, validate its
  SHA-256 digest and only then install the archive.
- Existing `a3s.xml` remains unchanged as a legacy fallback.
- Update configuration, profiles, repositories and user settings are preserved
  during installation and updates.
- GitHub, HTTPS/JSON and legacy XML sources can be selected without changing
  the updater JAR.

## Windows and platform behavior

- The compact launcher searches for a suitable Java installation through the
  configured environment, PATH and common Windows installation locations.
- The installed application can run without writing user data into
  `Program Files`.
- Windows configuration, profiles, repositories and cache data use user
  writable locations.
- Linux and Unix command-line startup remains supported.
- The existing per-profile option to start Arma 3 with administrator rights on
  Windows remains available.

## User interface

- Refined Light and Dark themes with centralized colors and layout values.
- More compact header and title-bar presentation.
- Improved visual separation between the Available Addons and Addon Groups
  panes.
- Yellow folder icons with distinct closed and open states.
- Improved status icons for addon and repository states.
- More readable progress bars and percentage text, including during active
  repository checks.
- Improved spacing, borders and contrast across the main windows.
- Dynamic copyright year in the About dialog.

## Stability and security

- Safer installation and temporary-path handling.
- ZIP extraction rejects unsafe path traversal entries.
- Update archives are staged and verified before files are copied into the
  installation.
- Repository checks always refresh the server's `.a3s/sync` file, and HTTP
  metadata requests bypass intermediary caches so removed files cannot remain
  in the client download list.
- HTTPS is used by default and unsafe HTTP downgrade behavior is rejected.
- Existing repository definitions and profiles remain readable.
- Non-destructive migration from the previous installation layout.
- Repository, synchronization, launcher and UI responsibilities are kept more
  clearly separated for maintenance.

## Credits and development process

The original project was created by the Sons of Exiled team. Arma3Sync
`2026.1.1` is maintained by `[PzBrig21] Soro`.

AI-assisted tools were used during development for codebase analysis,
refactoring support, reverse engineering assistance, documentation, test and
build support. All resulting changes were reviewed and integrated into the
project by the maintainer. AI assistance does not replace project review,
testing or responsibility for the published release.

## Compatibility notes

- Original update metadata using `a3s.xml` remains supported.
- Existing profiles, repositories and launcher configurations are intended to
  remain usable.
- The compact package requires a compatible Java 25 or newer installation.
- The standalone package includes its own runtime and is the recommended
  choice for systems without a suitable Java installation.

## Release references

- Original comparison version: `1.7.107`
- Technical development version: `1.9.260`
- Current release: `2026.1.1`

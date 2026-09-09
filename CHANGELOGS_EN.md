# Arma3Sync Change Log

Version-specific release changelogs are stored in
[`changelogs/`](changelogs/). That directory contains published versions only.
The matching file is checked by the release build and copied to the release
archive as `RELEASE_NOTES.md`.

## 2026.3.14 — Released

- FTP and SFTP repository uploads support 1 to 10 parallel file transfers; the
  default is 4 and is stored per repository.
- Upload connections are independent from repository-check and download
  connections.
- FTP sessions reuse directory listings and remote file checks during an
  upload, avoiding unnecessary round trips.
- Directories are prepared before file transfers. Synchronization metadata and
  deletion of obsolete files follow only after successful uploads.
- Progress, speed and remaining time are aggregated across upload sessions.
- SFTP supports custom ports, password authentication, remote paths,
  controlled reconnects and cancellation.
- Bounded SFTP network buffers improve compatibility with smaller SSH channel
  windows.
- The optional `-debug` argument writes a rotating diagnostic log without
  passwords.
- The Standard installer now places the bundled Java 25 runtime correctly
  below `runtime/`; no separate Java installation is required.
- ZIP and installer use the same launcher, root application JAR and runtime
  layout.
- Existing repositories, profiles and the legacy `a3s.xml` format remain
  compatible.

## 2026.2.6 — Released

- The graphical update path starts the updater without console-only mode, so
  the updater window and progress are visible again.
- Command-line operations retain explicit console mode.
- Graphical Windows updates use `javaw.exe`; checks and console operations use
  `java.exe`.
- The Standard package continues to include the reduced Java 25 runtime, while
  Compact requires Java 25 or newer.

## 2026.2.5 — Released

- Export selected Workshop modsets as valid HTML presets for the official Arma
  3 Launcher.
- Read Workshop IDs and display names from `meta.cpp`; manage Creator DLC
  metadata centrally.
- Local AppData metadata store for known Workshop mods and missing metadata.
- Keep legacy `a3s.xml`, profiles and existing repositories compatible.
- Verify updater archives with SHA-256 and harden ZIP extraction and Java
  deserialization.
- Keep valid `.a3s` metadata until a new repository scan completes and replace
  metadata atomically.
- Stabilize download, cancellation and Swing-thread handling with an exact
  ten-error limit.
- Do not increment the repository revision when published content is unchanged.
- Harden HTML export, metadata dialogs, cache locking and atomic output.
- GitHub Releases can be selected as the update source.
- The Standard package bundles a reduced Java 25 runtime; Compact requires Java
  25 or newer.

## 2026.1.1 — Released

- Modern continuation of Arma3Sync `1.7.107` with centralized versioning,
  Gradle builds and a Java 25 baseline.
- New updater with JSON manifests, SHA-256 verification and optional GitHub
  Releases support.
- Preserve established repository, profile and launcher workflows together
  with the legacy `a3s.xml` format.
- Safer installation, temporary-path handling and ZIP extraction.
- Compact and Standard Windows packages plus cross-platform JAR startup.

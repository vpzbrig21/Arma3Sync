# Arma3Sync 2026.2.1

This release continues the modernization of Arma3Sync from the original 1.7.107 codebase. It focuses on reliable updates, safer file handling, repository compatibility, and exporting Workshop modsets for the official Arma 3 Launcher.

## Highlights

- Added export of selected Workshop modsets as valid Arma 3 Launcher HTML presets.
- Workshop metadata is read from each mod's `meta.cpp`, including the Workshop ID and display name.
- Added centralized Creator DLC metadata for DLC entries included in a modset.
- Added a local AppData metadata store for known Workshop mods and missing metadata recovery.
- Preserved the legacy `a3s.xml` repository format while supporting the newer updater configuration and manifest formats.
- Improved synchronization behavior by using fresh repository metadata and avoiding stale local cache data.
- Restored compatibility with the compact legacy layout, including configuration
  files stored beside `Arma3Sync.jar` and repositories under `resources/ftp`.
- Relaxed the legacy deserialization filter only for valid Arma3Sync container
  types used by existing repositories, including object arrays and map entries.
- Repository checks no longer probe directory URLs; HTTP repositories that
  correctly return `403 Forbidden` for directory requests can be checked again.
- HTTPS auto-config imports now use regular certificate validation again, so
  remote URLs such as `https://.../.a3s/autoconfig` are not rejected by the
  restricted insecure-SSL policy.
- Auto-config protocol detection now accepts schemes only at the URL prefix and
  distinguishes HTTPS explicitly from HTTP.
- Closing the main window now cancels active repository workers before shutdown.
- Repository definitions are now serialized within the application and replaced
  atomically. The previous valid file remains until the new write succeeds,
  preventing an interrupted or concurrent write from emptying the repository list.

## Windows packages and Java runtime

- The regular Windows release is the **Standard** package:
  `Arma3Sync-2026.2.1.zip` and `Arma3Sync-2026.2.1-setup.exe`. It includes a
  reduced Java 25 runtime under `runtime/`, so users do **not** need to have
  Java or a JDK pre-installed on Windows.
- An optional **Compact** package is available as
  `Arma3Sync-2026.2.1-compact.zip` and
  `Arma3Sync-2026.2.1-compact-setup.exe`. It does not include a runtime and
  requires a separately installed Java runtime 25 or newer.
- Both Windows variants contain the same root-level `Arma3Sync.jar` and
  `Arma3Sync.exe` launcher. The Standard launcher uses the bundled runtime;
  the Compact launcher searches for a compatible system runtime.
- The shared native launcher was reduced internally without changing its
  behavior. Both variants retain the same Java 25 validation, runtime selection
  order, error handling, and root-JAR start path.
- The application JAR remains available for Linux, command-line use, and
  development environments. Those use cases still require a compatible
  system Java runtime unless a platform-specific runtime package is used;
  there is no separate `standalone` release package in this naming scheme.

## Reliability and security

- Added SHA-256 verification for updater archives before installation.
- Hardened ZIP extraction against path traversal, absolute paths, and symlink escapes.
- Restricted Java object deserialization to the legacy Arma3Sync model and approved JDK types with stream limits.
- Kept user-managed profiles, repository settings, and updater configuration protected during updates.
- Improved update cancellation so active HTTP/FTP transfers are interrupted before temporary update data is cleaned up.
- Fixed Windows case-insensitive file tracking so a launcher JAR renamed only by
  letter case is not deleted during the update.
- Repository builds now retain the last valid `.a3s` metadata until a new scan has completed, so an aborted build does not leave the repository without synchronization data.
- Repository metadata is written to temporary files and atomically replaced, reducing the risk of corrupted `sync`, `serverinfo`, or `autoconfig` files after an interrupted write.
- Addon path matching now uses normalized paths with directory boundaries, preventing similarly named directories from being matched accidentally.
- Parallel download and completion processing now protects shared error, termination, and semaphore state; the download error limit is applied exactly at ten errors.
- Input and state validation no longer depends on Java `assert` statements, which are disabled unless the runtime is started with `-ea`.
- Repository worker progress, status, and completion updates are dispatched through Swing's Event Dispatch Thread for safer UI updates.
- Shared UI-worker cancellation state is now visible across threads, preventing late progress callbacks from restoring stale UI state after cancellation.
- Centralized the application version at `version.properties`; this release uses `2026.2.1`.
- Updated standard and compact packaging for the modern Java 25 runtime and launcher layout. The regular Standard release bundles its own runtime, while the optional Compact release remains available without the bundled runtime.
- Removed forced `System.gc()` calls from active build, synchronization, and UI paths;
  normal resource cleanup remains explicit. Console output was retained where it is
  part of CLI progress, diagnostics, or the exportable synchronization report.
- Repository total-size reporting now counts the complete repository content while
  excluding only `.a3s` administration files; it does not represent an update delta.
- Repository revisions now remain unchanged when the published file paths, sizes, and
  SHA-1 content are unchanged. A new changelog entry is created only for real content
  changes.
- Replaced selected high-risk local path concatenations in repository metadata and
  build handling with `Path.resolve()` and sibling-path operations.
- Tightened the updater JSON parser: RFC-compatible numbers, integer sizes, strict
  release versions, HTTP(S)-only download URLs, and duplicate-key rejection are now
  enforced before update processing.
- HTML preset export now writes through a same-directory temporary file with an atomic
  replacement where supported and verifies that the resulting document is complete.
  Replacing an existing preset also has a Windows-compatible fallback, so saving over
  an earlier export no longer silently fails.
- Non-positive or malformed Workshop IDs in `meta.cpp` are treated as missing metadata
  and open the guided metadata dialog instead of silently producing an incomplete export.
- The metadata scan now runs directly as part of the export action. The missing-
  metadata prompt is opened immediately after the scan and before the save
  chooser, without a background worker completion callback or a nested progress
  dialog. The save chooser is shown only after all critical metadata has been
  completed and validated by a second scan.
- Export status, error, and metadata prompts now use the standard Swing dialog
  API with the visible Addons panel as parent, preventing dialogs from being
  created behind the main window.
- The export path now runs without a delayed intermediate callback and writes
  phase diagnostics to `launcher-preset-export.log`, making it possible to
  identify whether button handling, modset selection, metadata dialogs, the
  save chooser, or file writing was reached.
- Fixed a theme-default type error where option-pane and table border settings
  were registered as insets instead of Swing `Border` instances. This could
  cause metadata, error, and success dialogs to fail before becoming visible.
- The Workshop metadata cache now uses a sidecar file lock and an atomic replacement;
  concurrent exports no longer overwrite each other's newly added metadata.
- Disabled HTTPS certificate validation is now restricted to local test hosts by
  default. Remote development targets require the explicit
  `-Da3s.allowInsecureSsl=true` override and receive a clear warning in the UI.
- The original external Google Fonts reference remains unchanged for compatibility
  with the official launcher preset appearance.

## Compatibility

- Existing repositories remain compatible with older Arma3Sync versions because the legacy `a3s.xml` format is retained.
- The Launcher HTML export is an additional option and does not change repository storage or synchronization behavior.
- GitHub-based updates can be enabled or disabled in the updater configuration.

## Build and release layout

- The updater is now included as an independent Gradle subproject at
  `updater/`, while remaining versioned and tested with the main application.
- NSIS packaging and icon generation are included under `installer/nsis/`.
- Release automation is included under `release/` and produces the standard
  artifacts by default; compact artifacts are optional and carry the
  `-compact` suffix in `release/output/`.
- The standard ZIP and installer are the recommended end-user downloads and
  require no pre-installed Java on Windows. Compact ZIP and installer files
  are explicitly marked with `-compact` and require Java 25+ on the target
  system.
- `version.properties` is the single version source for the application,
  updater, NSIS metadata, manifests and installer names.
- The release script validates the required build inputs and confirms that the
  expected installer was actually created and is non-empty.
- The release script supports `-SkipBuild` so a previously tested application
  build can be packaged without recompiling it.
- A Gradle Wrapper is intentionally not introduced; the documented global
  Gradle installation remains the build prerequisite.

## Credits

AI assistance was used during the modernization process for code analysis, implementation support, test design, troubleshooting, and documentation review. All changes were checked against the project source and validated with automated tests where applicable.

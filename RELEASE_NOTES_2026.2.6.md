# Arma3Sync 2026.2.6

Patch release for the updater workflow.

## Fixed

- Fixed the graphical update path. After confirming an available update,
  Arma3Sync now starts the updater without the console-only mode, so the
  updater window and progress information are visible.
- Kept the explicit console mode for command-line update operations.
- On Windows, graphical updater launches use `javaw.exe`; checks and console
  operations continue to use `java.exe`.

## Packaging

- The central application version is `2026.2.6`.
- The Standard Windows package continues to include its reduced Java 25
  runtime and does not require Java to be installed separately.
- The Compact package continues to require a separately installed Java 25 or
  newer runtime.

## Compatibility

- Existing repositories and the legacy `a3s.xml` format remain compatible.
- This release is intended as a direct update from 2026.2.5.

## Release files

- `Arma3Sync-2026.2.6.zip`
- `Arma3Sync-2026.2.6-setup.exe`
- Optional Compact artifacts use the `-compact` suffix.

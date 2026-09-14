# Arma3Sync – Unreleased update fix

This temporary changelog collects changes for the next release. It will be
moved to `changelogs/<version>.md` when the release version is finalized.

## Fixed

- The updater now detects write-permission failures in protected installation
  directories such as `C:\Program Files\Arma3Sync`.
- On Windows, the native launcher can restart the updater through UAC only
  when elevated rights are actually required. The application is not moved to
  `AppData`, and existing legacy installation paths remain supported.
- The elevated updater keeps the selected update source, development mode,
  current version and user configuration path.
- UAC cancellation and failure now result in a visible error instead of a
  silent update abort.
- Update write errors now include the installation path, original exception
  type and underlying message to improve diagnosis of permissions and file
  locking problems.
- Added native bootstrapper tests for internal updater arguments and Windows
  command-line quoting.
- Documented the protected-installation and on-demand UAC behavior in the
  updater documentation.

## Release tooling

- Added an automatic beta-release workflow. A beta series such as `2026.4`
  starts at revision `1`, increases consistently for the next complete beta
  build and uses the same revision for Standard and Compact artifacts.
- Failed beta builds restore the previous central version so failed attempts do
  not consume a revision.

## Compatibility

- Existing profiles, repository definitions, user configuration and the
  legacy `a3s.xml` format remain unchanged.
- The normal launcher and bundled Java runtime layout remain unchanged.
- Non-Windows platforms retain the existing updater behavior; UAC elevation is
  Windows-specific.

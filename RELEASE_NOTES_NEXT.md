# Arma3Sync – Unreleased update fix

This temporary changelog collects changes for the next release. It will be
moved to `changelogs/<version>.md` when the release version is finalized.

## Fixed

- The updater now detects write-permission failures in protected installation
  directories such as `C:\Program Files\Arma3Sync`.
- On Windows, the updater now restarts the updater Java process itself through
  PowerShell's UAC `runas` flow only when elevated rights are actually
  required. This avoids `CreateProcess` error 740 from older or elevated
  native launchers. The application is not moved to `AppData`, and existing
  legacy installation paths remain supported.
- Console-mode updates use the same direct elevated updater restart and keep
  their console output instead of silently aborting after a protected-target
  write failure.
- The original updater now considers UAC successful as soon as the elevated
  updater process is accepted by Windows; a later update failure is no longer
  misreported as a failed UAC start.
- The elevated GUI updater is started visibly so the user can see its progress
  and any error instead of the original updater closing without feedback.
- Added an optional updater diagnostic log controlled by
  `-Da3s.updater.debug=true`. It records metadata loading, version comparison,
  download, archive staging, installation, UAC restart and complete exception
  details in the writable `%LOCALAPPDATA%\\Arma3Sync\\logs` directory.
- Debug settings are forwarded to the elevated updater process so the complete
  update attempt remains traceable across the UAC boundary.
- UAC elevation now uses a temporary update runner outside the installation.
  The runner waits until the original updater exits before starting Java, so
  the installed Runtime DLLs are not locked by the process that updates them.
- The UAC launch now uses a temporary PowerShell helper to invoke the updater
  Java process, avoiding direct elevation of legacy native launchers and
  making argument forwarding independent of the old executable.
- The temporary runner copies the bundled Runtime and updater JAR into a
  user-writable staging directory and removes the staging data after the
  elevated update process exits.
- Temporary Runtime staging now uses a complete file-tree copy and validates
  the Java launcher, `release`, `conf`, `lib` and server JVM (`jvm.dll`) before
  starting. An incomplete Runtime is rejected before the UAC update begins.
- The elevated helper now waits explicitly for the temporary Java process before
  removing its Runtime. This prevents `javaw.exe` from starting while
  `jvm.dll` is being cleaned up.
- Fixed Windows argument quoting for the elevated Java process. Installation
  paths containing spaces, including the legacy `Program Files (x86)` path,
  are now passed as intact JVM arguments instead of being split by
  `Start-Process` before Java starts.
- The Windows PowerShell elevation helper now runs hidden while the UAC
  consent prompt remains visible. Linux keeps its existing non-UAC updater
  path; a regression test verifies that Windows elevation is never requested
  on Linux.
- Repository update notifications are now deduplicated per remote revision,
  so an unresolved update is not reported again on every background check.
- System-tray repository notifications now honor the `NOTIFY` setting and use
  the same per-revision de-duplication as the update dialog.
- Automatic synchronization is only started for repositories that were
  actually detected as updated. Automatic updates no longer trigger a stale
  pending-update notification from the pre-update status snapshot.
- Fixed the UAC helper argument assembly: the complete elevated Java command
  (`-jar`, startup marker, runtime and update parameters) is now written before
  the helper is launched. Previously the helper could be accepted by UAC but
  exit without starting the updater JVM.
- Corrected JVM argument ordering so forwarded `-D...` settings remain JVM
  properties instead of being interpreted as updater arguments after `-jar`.
- Protected installations are now detected after update availability is known
  but before archive download; the elevated updater performs the complete update
  instead of downloading first and requesting UAC only during the final copy.
- The permission check runs only after a newer version was confirmed, so an
  already-current installation does not trigger an unnecessary UAC prompt.
- Fixed installation of Windows-packaged ZIP archives whose directory entries
  use backslashes. Runtime directories such as `runtime\\legal\\` are now
  recognized correctly instead of being copied as files over existing folders.
- Elevated updater processes are marked explicitly and do not recursively
  request UAC again if a separate file lock or ACL still prevents writing.
- The elevated updater keeps the selected update source, development mode,
  current version and user configuration path.
- UAC cancellation and failure now result in a visible error instead of a
  silent update abort.
- Update write errors now include the installation path, original exception
  type and underlying message to improve diagnosis of permissions and file
  locking problems.
- Update archives are now validated completely, including ZIP structure,
  safe entry paths and checksums, before installation changes the target.
- The installation copy phase uses a dedicated extraction directory and a
  stable source snapshot. Missing extracted entries now produce a specific
  incomplete-archive error instead of an ambiguous write failure.
- The same stable extraction path is now used for HTTP and GitHub updates;
  previously that path could still be cleaned or invalidated during the
  installation copy phase.
- UAC elevation remains limited to actual target write-permission failures;
  corrupt or incomplete update archives are reported directly.
- Added native bootstrapper tests for internal updater arguments and Windows
  command-line quoting.
- Documented the protected-installation and on-demand UAC behavior in the
  updater documentation.

## Added

- Added an `Import HTML` action beside the existing Arma 3 Launcher preset
  export action.
- The importer reads Workshop `ModContainer` entries from a standard Arma 3
  Launcher HTML preset and compares their `publishedid` values with the
  `meta.cpp` metadata of the currently discovered local addons.
- The verification result clearly shows the total Workshop mods, locally found
  mods and missing mods. Every missing mod includes its Workshop ID and direct
  Steam Workshop link.
- Verification itself is read-only with regard to repository definitions and
  local addon files; importing intentionally persists one new addon group.
- Imported Workshop presets now also create a persisted, uniquely named addon
  group. Locally matched mods use their resolved internal addon keys, while
  missing entries are added and visibly marked as missing so the imported
  group can be reviewed immediately.
- Missing Workshop mod reports now render each direct Workshop link as a
  clickable link that opens the system browser.
- Arma 3 Launcher preset imports now process `DlcContainer` entries. Installed
  CDLCs are added to the imported addon group, while missing CDLCs are marked
  accordingly and shown with a clickable Steam Store link.
- Imported Workshop and CDLC entries now retain their Workshop ID or Steam App
  ID in the addon group. Refreshing the local addon list rechecks previously
  missing entries and automatically resolves them after installation.

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

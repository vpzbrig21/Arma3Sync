# Testing Guide

## Automated Tests

Run all automated checks with:

```
gradle test
```

Current automated coverage focuses on persistence primitives that are critical after the Java 25 migration:

- `A3SFilesAccessorTest` verifies that gzip-compressed Java serialization of `Preferences` still round-trips.
- `PreferencesDAOTest` exercises the full read/write workflow against the on-disk `resources/configuration` structure using an isolated temporary installation path.

These tests give quick feedback that configuration data survives upgrades and can be written in environments without elevated permissions.

## Manual Smoke Tests (still required)

| Area | Why manual? | Suggested smoke test |
| --- | --- | --- |
| Windows Registry integration | Relies on the host `reg.exe` tool and real registry hives; not portable to CI. | On Windows, toggle “Start with OS” in the UI or run `PreferencesDAO.addToWindowsRegistry()` and confirm the Run key entry is added/removed. |
| Network stack (HTTP/FTP/WebDAV) | Requires reachable repositories, authentication data, and TLS negotiation with real servers. | Perform a repository sync and upload using FTP/WebDAV endpoints that represent your production environment. |
| Swing UI & theming | Automated UI testing would require a headless robot or snapshot tooling not present in this repo. | Launch the app (`gradle run`), switch between light/dark themes, open key dialogs (Repository, Preferences) and ensure icons/fonts render correctly. |
| Single-instance enforcement | Depends on OS window manager focus and the native `junique` lock. | Start the app twice; verify the first instance is focused and no duplicate window appears. |
| External tool integrations (bikey extractor, TFAR/ACRE installers) | They spawn processes and rely on local game files. | Execute each tool once against sample data to confirm dialogs open and progress bars update. |

Document results of the manual runs alongside release notes so regressions can be traced quickly.

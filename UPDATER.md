# ArmA3Sync Updater – Release 2026.2.6

This document describes the updater that is shipped with the ArmA3Sync
release package. The updater source is an independent Gradle subproject under
`updater/`, but it is versioned and tested together with the main application.
Its runtime contract is part of every release.

## Files in an installation

| File | Purpose |
|---|---|
| `ArmA3Sync-Updater.jar` | Update checker and installer |
| `resources/configuration/updater.toml` | User-editable update configuration |
| `a3s.xml` | Legacy update metadata and fallback |
| `version.txt` | Installed application version |

The installer creates the default `updater.toml` only when it does not already
exist. Updates preserve it as well as profiles, repositories and application
settings. On Windows, the user-specific configuration is normally located at
`%APPDATA%\Arma3Sync\configuration\updater.toml`; this allows source changes
without write access to `Program Files`.

## Configuration

The default configuration for release `2026.2.6` is:

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

The TOML reader supports quoted strings, booleans, positive integers and `#`
comments. Other users can change the URLs and asset patterns without changing
the updater JAR.

## GitHub Releases

GitHub Releases are enabled by default in new installations. They can be
disabled in the configuration or bypassed for an individual update:

```toml
[github]
enabled = true
api_url = "https://api.github.com/repos/vpzbrig21/Arma3Sync/releases/latest"
asset_pattern = "Arma3Sync-{version}.zip"
```

For release `2026.2.6`, the updater expects the asset
`Arma3Sync-2026.2.6.zip`. The following placeholders are supported:

- `{version}`: normalized version, for example `2026.2.6`.
- `{tag}`: original GitHub tag, for example `v2026.2.6`.

The updater requests the configured release API, finds the exact ZIP asset,
uses its `browser_download_url` and requires a valid SHA-256 digest. The
archive is not extracted until the digest matches. An arbitrary ZIP, a missing
asset or a missing/invalid digest is rejected.

`api_url` may point to another public GitHub repository or a GitHub Enterprise
API endpoint. A public repository does not require credentials. The `latest`
endpoint does not include draft or pre-release versions; development builds
should therefore use an explicitly configured release endpoint and asset
pattern.

Arma3Sync passes `-github` or `-manifest` to the updater for each update check
and download. `-github` prefers GitHub only when `[github].enabled = true`;
an explicit `enabled = false` remains authoritative. `-manifest` skips GitHub
for that update and uses the configured JSON/XML source. Direct updater calls
without either flag use the source configured in `updater.toml`.

## Source priority and fallback

For HTTP/HTTPS updates, the order is:

1. GitHub Releases when `[github].enabled = true`.
2. `manifest_url` or `dev_manifest_url` as a signed-by-hash JSON manifest.
3. `legacy_xml_url` or `dev_legacy_xml_url`.
4. Local `a3s.xml` metadata as the final legacy fallback.

The GitHub and JSON paths require SHA-256 verification. XML remains available
for compatibility with existing installations, but XML-only updates do not
provide an archive hash.

## JSON manifest example

```json
{
  "schemaVersion": 1,
  "version": "2026.2.6",
  "file": "Arma3Sync-2026.2.6.zip",
  "sha256": "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
  "size": 12345678
}
```

The JSON manifest must identify one ZIP file and contain a 64-character
SHA-256 hash. The updater downloads it to a temporary staging directory,
verifies the hash, rejects unsafe ZIP paths and copies the result into the
installation. Files installed by the updater are tracked in
`.a3s-updater-files`; on subsequent updates, tracked files missing from the
new archive are removed. Existing installations without this state file are
not scanned or deleted automatically, so unknown user files remain protected.

## Legacy XML example

The file remains named `a3s.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<ArmA3Sync>
    <nom>2026.2.6</nom>
    <file>Arma3Sync-2026.2.6.zip</file>
</ArmA3Sync>
```

The `<file>` element is optional. Without it, the updater derives
`Arma3Sync-2026.2.6.zip` from `<nom>`.

## Creating a release

1. Set `app.version=2026.2.6` in the central version file before creating the
   release.
2. Build the release package so that the ZIP and manifests are generated.
3. Create a GitHub Release with a tag such as `v2026.2.6`.
4. Upload the exact asset `Arma3Sync-2026.2.6.zip`.
5. Mark the release as the published stable release.
6. Set `github.enabled = false` for installations that should continue using
   only the existing update server.

The standard and compact packages use different archive names. Their asset
patterns must not be mixed; configure the matching ZIP explicitly.

## Compatibility

The updater still supports direct invocation and the existing modes:

```text
java -jar ArmA3Sync-Updater.jar -check
java -jar ArmA3Sync-Updater.jar -console
java -jar ArmA3Sync-Updater.jar -dev -check
java -jar ArmA3Sync-Updater.jar -dev -console
java -jar ArmA3Sync-Updater.jar -github -check
java -jar ArmA3Sync-Updater.jar -manifest -check
```

The complete workspace-level maintenance documentation is maintained in
`Documents/UPDATER.md`.

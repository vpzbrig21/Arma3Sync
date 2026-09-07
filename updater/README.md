# Arma3Sync Updater

This is a maintainable reconstruction of the updater contained in the
original ArmA3Sync 1.7.107 distribution. It is kept as an independent Gradle
subproject inside this repository so its source, tests and release artifact
are versioned together with the main application.

The release version is centralized in `../version.properties`; do not edit
version declarations separately.

## Compatibility

The entry point and command-line forms are preserved:

```text
java -jar ArmA3Sync-Updater.jar
java -jar ArmA3Sync-Updater.jar -dev
java -jar ArmA3Sync-Updater.jar -console
java -jar ArmA3Sync-Updater.jar -dev -console
java -jar ArmA3Sync-Updater.jar -check
java -jar ArmA3Sync-Updater.jar -dev -check
```

The updater reads `resources/configuration/updater.toml` from the installation
directory. A root-level `updater.toml` takes precedence. JSON is preferred and
the existing `a3s.xml` remains the unchanged legacy fallback.

## GitHub Releases source

GitHub can be selected without changing the updater binary. It is enabled by
default in new installations and is tried before the configured JSON manifest
when enabled:

```toml
[github]
enabled = true
api_url = "https://api.github.com/repos/vpzbrig21/Arma3Sync/releases/latest"
dev_api_url = "https://api.github.com/repos/vpzbrig21/Arma3Sync/releases/latest"
asset_pattern = "Arma3Sync-{version}.zip"
dev_asset_pattern = "Arma3Sync-{version}.zip"
```

`{version}` is replaced by the release tag without a leading `v`, while
`{tag}` keeps the original tag. The matching release asset must be a ZIP and
must expose a SHA-256 digest through the GitHub Releases API. The updater
downloads the asset from its `browser_download_url` and verifies the digest
before extraction. If the GitHub request fails, the normal JSON manifest and
then the legacy XML source are attempted.

When Arma3Sync starts the updater it passes `-github` or `-manifest` for the
current update operation. `-github` still respects `[github].enabled = false`
and falls back to JSON/XML; `-manifest` deliberately skips GitHub. A direct
updater invocation without a source flag follows the configuration value.

`api_url` can be changed for another public repository or GitHub Enterprise
API endpoint. `asset_pattern` must resolve to the exact release asset name;
the updater never chooses an arbitrary ZIP. GitHub's `latest` endpoint ignores
draft and pre-release versions, so a development channel should use a separate
configured endpoint or release policy.

Preferred JSON metadata:

```json
{
  "version": "2026.2.5",
  "file": "Arma3Sync-2026.2.5.zip",
  "sha256": "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
}
```

Legacy metadata remains supported:

```xml
<ArmA3Sync>
    <nom>2026.2.5</nom>
    <file>Arma3Sync-2026.2.5.zip</file>
</ArmA3Sync>
```

The current launcher uses HTTPS by default (`https://arma3sync.vpzbrig21.de/updates`).
The original FTP endpoint is still available with `-Da3s.updater.protocol=ftp`.
For local testing, override it with `-Da3s.updater.url`,
`-Da3s.updater.protocol`, and `-Da3s.updater.installationPath`.

## Build

From the repository root:

```text
gradle clean test
gradle :updater:test :updater:copyRuntimeJar
```

`:updater:copyRuntimeJar` creates
`updater/build/distribution/ArmA3Sync-Updater.jar`. The updater uses the
versioned dependency at `../resources/lib/commons-net-3.10.0.jar`, so no
external updater checkout is required.

## Deliberate hardening

- XML external entities and external schemas are disabled.
- Archive names are restricted to a single ZIP filename.
- ZIP entries are normalized and rejected if they escape the extraction root.
- The update is staged in a temporary directory and copied only after a
  complete archive download.
- FTP connect, login, and data timeouts are bounded.
- JSON archives are verified against the manifest's SHA-256 before extraction.
- User-managed `updater.toml` is not overwritten by the installer or update ZIP.
- The updater records the files it installed in `.a3s-updater-files`. On later
  updates, files from that list that are absent from the new archive are
  removed, so renamed or deleted application files do not remain as artifacts.
- Existing installations without `.a3s-updater-files` are intentionally not
  scanned or deleted on the first update; this protects unknown user files.
- Cancelling an update first requests worker and transport cancellation; the
  temporary staging directory is cleaned only after the worker has finished.

The application separately protects repository HTTPS connections: disabled
certificate validation is accepted only for local test hosts by default. A
remote development exception requires the explicit JVM property
`-Da3s.allowInsecureSsl=true`.

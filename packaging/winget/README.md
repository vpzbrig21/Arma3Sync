# WinGet packaging

The WinGet package identifier is `VPZBrig21.Arma3Sync`. The Standard Windows
installer is used because it includes the bundled Java 25 runtime. Compact
artifacts are intentionally not submitted as a second WinGet installer.

## One-time initial submission

The candidate manifest for the existing stable release 2026.3.14 is in:

```text
packaging/winget/VPZBrig21.Arma3Sync/2026.3.14/
```

Copy only those three YAML files into the matching path in a fork of
`microsoft/winget-pkgs`:

```text
manifests/v/VPZBrig21/Arma3Sync/2026.3.14/
```

Run the validation and installation test on Windows before opening the PR:

```powershell
winget validate --manifest .\packaging\winget\VPZBrig21.Arma3Sync\2026.3.14
winget settings --enable LocalManifestFiles
winget install --manifest .\packaging\winget\VPZBrig21.Arma3Sync\2026.3.14
```

The initial PR to `microsoft/winget-pkgs` must contain only this one manifest
set. The package has to be accepted once before the automated updater can use
it as a base for future versions.

## GitHub Actions setup

The workflow `.github/workflows/publish-winget.yml` runs when a GitHub release
is published. It skips prereleases, selects only the Standard NSIS installer,
and opens a PR against `microsoft/winget-pkgs` for the new version. It also has
a manual `workflow_dispatch` input for rerunning a specific release tag.

Before enabling it:

1. Fork `microsoft/winget-pkgs` as `vpzbrig21/winget-pkgs`.
2. Create a classic GitHub Personal Access Token with the `public_repo` scope.
3. Add it in this repository under **Settings → Secrets and variables →
   Actions → New repository secret** with the name `WINGET_TOKEN`.
4. Publish a stable GitHub release with a `v<version>` tag and an asset named
   `Arma3Sync-<version>-setup.exe`.

The token must not be committed to the repository or placed in workflow text.
The action only creates/updates the WinGet submission PR; Microsoft/community
validation and merging remain a separate step.

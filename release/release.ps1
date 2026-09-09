param(
    [switch]$Compact,
    [switch]$SkipBuild,
    [string]$ReleaseNotesPath
)

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot
$versionFile = Join-Path $repoRoot "version.properties"
$updaterRoot = Join-Path $repoRoot "updater"
$releaseRoot = Join-Path $repoRoot "release\output"
$buildRoot = Join-Path $repoRoot "release\build"
$changelogRoot = Join-Path $repoRoot "changelogs"
$installerScript = Join-Path $repoRoot "installer\nsis\build-installer.ps1"
$versionScript = Join-Path $repoRoot "release\generate-version.ps1"

function Read-CentralVersion {
    if (-not (Test-Path -LiteralPath $versionFile -PathType Leaf)) {
        throw "Zentrale Versionsdatei fehlt: $versionFile"
    }
    $line = Get-Content -LiteralPath $versionFile -Encoding UTF8 |
        Where-Object { $_ -match '^\s*app\.version\s*=\s*(.+?)\s*$' } |
        Select-Object -First 1
    if (-not $line -or $line -notmatch '^\s*app\.version\s*=\s*(.+?)\s*$') {
        throw "version.properties muss app.version=x.y.z enthalten."
    }
    $value = $Matches[1].Trim()
    if ($value -notmatch '^\d+\.\d+\.\d+$') {
        throw "Ungültige Version in version.properties: $value"
    }
    return $value
}

function Require-File([string]$Path, [string]$Description) {
    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        throw "$Description fehlt: $Path"
    }
}

function Require-Directory([string]$Path, [string]$Description) {
    if (-not (Test-Path -LiteralPath $Path -PathType Container)) {
        throw "$Description fehlt: $Path"
    }
}

try {
    Require-File $versionScript "Versionsgenerator"
    Require-File $installerScript "NSIS-Buildskript"

    & powershell.exe -NoProfile -ExecutionPolicy Bypass -File $versionScript -WorkspaceRoot $repoRoot
    if ($LASTEXITCODE -ne 0) { throw "Versionsdateien konnten nicht erzeugt werden: $LASTEXITCODE" }
    $version = Read-CentralVersion
    if ([string]::IsNullOrWhiteSpace($ReleaseNotesPath)) {
        $releaseNotesPath = Join-Path $changelogRoot "$version.md"
    } elseif (-not [System.IO.Path]::IsPathRooted($ReleaseNotesPath)) {
        $releaseNotesPath = Join-Path $repoRoot $ReleaseNotesPath
    }
    Require-File $releaseNotesPath "Release-Changelog für $version"

    New-Item -ItemType Directory -Force -Path $releaseRoot | Out-Null
    # The old standalone naming was never an active release contract. Remove
    # only the exact legacy artifacts for the version being rebuilt so they
    # cannot be mistaken for current release components.
    @(
        "Arma3Sync-$version-standalone.zip",
        "Arma3Sync-$version-standalone-setup.exe",
        "a3s-standalone.json",
        "a3s-standalone.xml"
    ) | ForEach-Object {
        $obsoleteArtifact = Join-Path $releaseRoot $_
        if (Test-Path -LiteralPath $obsoleteArtifact) {
            Remove-Item -LiteralPath $obsoleteArtifact -Force
        }
    }
    if (Test-Path -LiteralPath $buildRoot) {
        Remove-Item -LiteralPath $buildRoot -Recurse -Force
    }
    New-Item -ItemType Directory -Force -Path $buildRoot | Out-Null

    Write-Host "Arma3Sync Release $version wird erstellt..." -ForegroundColor Cyan
    $installerArgs = @('-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', $installerScript)
    if ($Compact) { $installerArgs += '-Compact' }
    if ($SkipBuild) { $installerArgs += '-SkipBuild' }
    & powershell.exe @installerArgs
    if ($LASTEXITCODE -ne 0) { throw "Installer-Build fehlgeschlagen: $LASTEXITCODE" }

    $stageName = if ($Compact) { "compact" } else { "standard" }
    $stageRoot = Join-Path $buildRoot $stageName
    New-Item -ItemType Directory -Force -Path $stageRoot | Out-Null

    $appBuildRoot = Join-Path $repoRoot "build"
    $compactLauncher = Join-Path $appBuildRoot "compact-launcher"
    $updaterJar = Join-Path $updaterRoot "build\distribution\ArmA3Sync-Updater.jar"
    Require-File $updaterJar "Updater-JAR"

    $appDist = Join-Path $appBuildRoot "install\Arma3Sync"
    Require-Directory $appDist "Gradle-Distribution"
    Require-File (Join-Path $compactLauncher 'Arma3Sync.exe') "Kompakter Launcher"
    Require-File (Join-Path $compactLauncher 'Arma3Sync.jar') "Kompaktes Anwendungs-JAR"
    Copy-Item -Path (Join-Path $appDist '*') -Destination $stageRoot -Recurse -Force
    Copy-Item -LiteralPath (Join-Path $compactLauncher 'Arma3Sync.exe') -Destination $stageRoot -Force
    Copy-Item -LiteralPath (Join-Path $compactLauncher 'Arma3Sync.jar') -Destination $stageRoot -Force
    Remove-Item -LiteralPath (Join-Path $stageRoot "lib\Arma3Sync-$version.jar") -Force -ErrorAction SilentlyContinue
    if (-not $Compact) {
        $nativeRuntime = Join-Path $appBuildRoot "jpackage\windows-runtime"
        Require-Directory $nativeRuntime "Gebündelte Java-Runtime"
        Require-File (Join-Path $nativeRuntime "bin\java.exe") "Gebündelte Java-Runtime"
        $runtimeStage = Join-Path $stageRoot 'runtime'
        if (Test-Path -LiteralPath $runtimeStage) {
            Remove-Item -LiteralPath $runtimeStage -Recurse -Force
        }
        Copy-Item -LiteralPath $nativeRuntime -Destination $runtimeStage -Recurse -Force
    }

    Copy-Item -LiteralPath $updaterJar -Destination $stageRoot -Force
    Copy-Item -LiteralPath (Join-Path $repoRoot 'installer\assets\Arma3Sync.ico') -Destination $stageRoot -Force
    $payloadRoot = Join-Path $repoRoot 'installer\nsis\payload'
    Copy-Item -Path (Join-Path $payloadRoot 'Arma3Sync*') -Destination $stageRoot -Force
    Copy-Item -LiteralPath (Join-Path $payloadRoot 'version.txt') -Destination $stageRoot -Force
    Copy-Item -LiteralPath (Join-Path $repoRoot 'README.md') -Destination $stageRoot -Force
    Copy-Item -LiteralPath (Join-Path $repoRoot 'LICENCE.md') -Destination $stageRoot -Force
    Copy-Item -LiteralPath $releaseNotesPath -Destination (Join-Path $stageRoot 'RELEASE_NOTES.md') -Force

    $configurationRoot = Join-Path $stageRoot 'resources\configuration'
    New-Item -ItemType Directory -Force -Path $configurationRoot | Out-Null
    Copy-Item -LiteralPath (Join-Path $payloadRoot 'updater.toml') -Destination $configurationRoot -Force

    $suffix = if ($Compact) { '-compact' } else { '' }
    $archiveName = "Arma3Sync-$version$suffix.zip"
    $xml = @"
<?xml version="1.0" encoding="UTF-8"?>
<ArmA3Sync>
    <nom>$version</nom>
    <file>$archiveName</file>
</ArmA3Sync>
"@.Trim()
    Set-Content -LiteralPath (Join-Path $stageRoot 'a3s.xml') -Value $xml -Encoding UTF8

    $archivePath = Join-Path $releaseRoot $archiveName
    if (Test-Path -LiteralPath $archivePath) { Remove-Item -LiteralPath $archivePath -Force }
    Compress-Archive -Path (Join-Path $stageRoot '*') -DestinationPath $archivePath -CompressionLevel Optimal
    Require-File $archivePath "Release-Archiv"

    $archiveInfo = Get-Item -LiteralPath $archivePath
    $archiveHash = (Get-FileHash -LiteralPath $archivePath -Algorithm SHA256).Hash.ToLowerInvariant()
    $manifestName = if ($Compact) { 'a3s-compact.json' } else { 'a3s.json' }
    $manifest = [ordered]@{
        schemaVersion = 1
        version = $version
        file = $archiveName
        sha256 = $archiveHash
        size = $archiveInfo.Length
    }
    $manifestPath = Join-Path $releaseRoot $manifestName
    $manifest | ConvertTo-Json | Set-Content -LiteralPath $manifestPath -Encoding UTF8
    $xmlReleaseName = if ($Compact) { 'a3s-compact.xml' } else { 'a3s.xml' }
    Set-Content -LiteralPath (Join-Path $releaseRoot $xmlReleaseName) -Value $xml -Encoding UTF8

    $installerName = "Arma3Sync-$version$(if ($Compact) { '-compact' })-setup.exe"
    $installerPath = Join-Path $releaseRoot $installerName
    Require-File $installerPath "Erwarteter NSIS-Installer"

    Write-Host "`nRelease erfolgreich erstellt:" -ForegroundColor Green
    Write-Host "  Version:  $version"
    Write-Host "  Archiv:   $archivePath ($($archiveInfo.Length) Bytes)"
    Write-Host "  SHA-256:  $archiveHash"
    Write-Host "  Manifest: $(Join-Path $releaseRoot $manifestName)"
    Write-Host "  XML:      $(Join-Path $releaseRoot $xmlReleaseName)"
    Write-Host "  Installer: $installerPath"
    Write-Host "  Notes:    $releaseNotesPath"
} catch {
    Write-Error $_.Exception.Message
    exit 1
}

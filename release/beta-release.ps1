param(
    [Parameter(Mandatory = $true)]
    [ValidatePattern('^\d+\.\d+$')]
    [string]$BaseVersion,
    [switch]$Compact,
    [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot
$versionFile = Join-Path $repoRoot "version.properties"
$temporaryNotesSource = Join-Path $repoRoot "RELEASE_NOTES_NEXT.md"
$releaseScript = Join-Path $repoRoot "release\release.ps1"

function Read-CentralVersion {
    $line = Get-Content -LiteralPath $versionFile -Encoding UTF8 |
        Where-Object { $_ -match '^\s*app\.version\s*=\s*(.+?)\s*$' } |
        Select-Object -First 1
    if (-not $line -or $line -notmatch '^\s*app\.version\s*=\s*(.+?)\s*$') {
        throw "version.properties muss app.version=x.y.z enthalten."
    }
    return $Matches[1].Trim()
}

function Set-CentralVersion([string]$Version) {
    $content = Get-Content -LiteralPath $versionFile -Encoding UTF8 |
        ForEach-Object {
            if ($_ -match '^\s*app\.version\s*=') { "app.version=$Version" } else { $_ }
        }
    $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllText(
        $versionFile,
        (($content -join [Environment]::NewLine) + [Environment]::NewLine),
        $utf8NoBom)
}

if (-not (Test-Path -LiteralPath $versionFile -PathType Leaf)) {
    throw "Zentrale Versionsdatei fehlt: $versionFile"
}
if (-not (Test-Path -LiteralPath $temporaryNotesSource -PathType Leaf)) {
    throw "Temporärer Changelog fehlt: $temporaryNotesSource"
}
if (-not (Test-Path -LiteralPath $releaseScript -PathType Leaf)) {
    throw "Release-Skript fehlt: $releaseScript"
}

$currentVersion = Read-CentralVersion
$escapedBase = [regex]::Escape($BaseVersion)
$revision = 1
if ($currentVersion -match "^$escapedBase\.(\d+)$") {
    $revision = [int]$Matches[1] + 1
}
$version = "$BaseVersion.$revision"
$temporaryNotes = Join-Path ([System.IO.Path]::GetTempPath()) "Arma3Sync-$version-Beta-ReleaseNotes.md"
$notes = Get-Content -LiteralPath $temporaryNotesSource -Raw -Encoding UTF8
$notes = [regex]::Replace($notes, '(?m)^# .*$' , "# Arma3Sync $version Beta", 1)
if ($notes -notmatch '(?m)^# Arma3Sync ') {
    $notes = "# Arma3Sync $version Beta" + [Environment]::NewLine + [Environment]::NewLine + $notes
}
$notes += [Environment]::NewLine + [Environment]::NewLine +
    "## Beta build" + [Environment]::NewLine + [Environment]::NewLine +
    "- Automatically generated from the central version '$version'." + [Environment]::NewLine
Set-Content -LiteralPath $temporaryNotes -Value $notes -Encoding UTF8

try {
    Set-CentralVersion $version
    $variants = if ($Compact) { @($true) } else { @($false, $true) }
    foreach ($buildCompact in $variants) {
        $arguments = @(
            '-NoProfile',
            '-ExecutionPolicy', 'Bypass',
            '-File', $releaseScript,
            '-ReleaseNotesPath', $temporaryNotes
        )
        if ($buildCompact) { $arguments += '-Compact' }
        if ($SkipBuild) { $arguments += '-SkipBuild' }

        & powershell.exe @arguments
        if ($LASTEXITCODE -ne 0) {
            throw "Beta-Release fehlgeschlagen: $LASTEXITCODE"
        }
    }
    Write-Host "Beta-Revision erstellt: $version" -ForegroundColor Green
}
catch {
    Set-CentralVersion $currentVersion
    throw
}
finally {
    Remove-Item -LiteralPath $temporaryNotes -Force -ErrorAction SilentlyContinue
}

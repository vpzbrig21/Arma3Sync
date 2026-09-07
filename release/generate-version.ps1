param(
    [string]$WorkspaceRoot = (Split-Path -Parent $PSScriptRoot)
)

$ErrorActionPreference = "Stop"
$versionFile = Join-Path $WorkspaceRoot "version.properties"
$nsisVersionFile = Join-Path $WorkspaceRoot "installer\nsis\include\Version.nsh"
$payloadVersionFile = Join-Path $WorkspaceRoot "installer\nsis\payload\version.txt"

if (-not (Test-Path -LiteralPath $versionFile -PathType Leaf)) {
    throw "Central version file not found: $versionFile"
}

$versionLine = Get-Content -LiteralPath $versionFile -Encoding UTF8 |
    Where-Object { $_ -match '^\s*app\.version\s*=\s*(.+?)\s*$' } |
    Select-Object -First 1
if (-not $versionLine -or $versionLine -notmatch '^\s*app\.version\s*=\s*(.+?)\s*$') {
    throw "version.properties must contain app.version=x.y.z"
}

$version = $Matches[1].Trim()
if ($version -notmatch '^\d+\.\d+\.\d+$') {
    throw "app.version must use numeric major.minor.build format: $version"
}
$parts = $version.Split('.')
$name = "$($parts[0]).$($parts[1])"

$nsisContent = @"
!ifndef ARMA3SYNC_VERSION_NSH
!define ARMA3SYNC_VERSION_NSH

!define APP_NAME "Arma3Sync"
!define APP_VERSION "$version"
!define APP_PUBLISHER "Arma3Sync community"
!define APP_ID "Arma3Sync"

!endif
"@
Set-Content -LiteralPath $nsisVersionFile -Value $nsisContent -Encoding ASCII
Set-Content -LiteralPath $payloadVersionFile -Value @("Version $name", "Build $version") -Encoding ASCII

Write-Output "Central version: $version"
Write-Output "Generated: $nsisVersionFile"
Write-Output "Generated: $payloadVersionFile"

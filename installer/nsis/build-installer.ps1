param(
    [string]$Nsis = "makensis",
    [switch]$SkipBuild,
    [switch]$Compact
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$app = $root
$updater = Join-Path $root "updater"
$dist = Join-Path $app "build\install\Arma3Sync"
$compactLauncher = Join-Path $app "build\compact-launcher"
$updaterJar = Join-Path $updater "build\distribution\ArmA3Sync-Updater.jar"
$versionScript = Join-Path $root "release\generate-version.ps1"
$versionFile = Join-Path $root "version.properties"

if (-not (Test-Path -LiteralPath $versionScript -PathType Leaf)) {
    throw "Version generator not found: $versionScript"
}
& $versionScript -WorkspaceRoot $root
if ($LASTEXITCODE -ne $null -and $LASTEXITCODE -ne 0) {
    throw "Central version generation failed with exit code $LASTEXITCODE"
}
$versionLine = Get-Content -LiteralPath $versionFile -Encoding UTF8 |
    Where-Object { $_ -match '^\s*app\.version\s*=\s*(.+?)\s*$' } |
    Select-Object -First 1
if (-not $versionLine -or $versionLine -notmatch '^\s*app\.version\s*=\s*(.+?)\s*$') {
    throw "Central version file must contain app.version=x.y.z"
}
$version = $Matches[1].Trim()
$releaseOutput = Join-Path $root "release\output"
New-Item -ItemType Directory -Force -Path $releaseOutput | Out-Null

if ($Nsis -eq "makensis" -and -not (Get-Command makensis -ErrorAction SilentlyContinue)) {
    $installedNsis = "C:\Program Files (x86)\NSIS\makensis.exe"
    if (Test-Path -LiteralPath $installedNsis -PathType Leaf) { $Nsis = $installedNsis }
}
if (-not (Test-Path -LiteralPath $Nsis -PathType Leaf) -and -not (Get-Command $Nsis -ErrorAction SilentlyContinue)) {
    throw "NSIS makensis.exe not found. Install NSIS 3.12 or pass -Nsis with its full path."
}

if (-not $SkipBuild) {
    & gradle -p $root createCompactLauncher
    if ($LASTEXITCODE -ne 0) { throw "Gradle application build failed with exit code $LASTEXITCODE" }
    if (-not $Compact) {
        & gradle -p $root jlinkRuntimeWin
        if ($LASTEXITCODE -ne 0) { throw "Bundled Java runtime build failed with exit code $LASTEXITCODE" }
    }
    & gradle -p $root :updater:copyRuntimeJar
    if ($LASTEXITCODE -ne 0) { throw "Gradle updater build failed with exit code $LASTEXITCODE" }
}

if (-not (Test-Path -LiteralPath $dist -PathType Container)) {
    throw "Gradle distribution not found: $dist"
}
if (-not (Test-Path -LiteralPath $updaterJar -PathType Leaf)) {
    throw "Updater JAR not found: $updaterJar"
}
if (-not (Test-Path -LiteralPath (Join-Path $compactLauncher "Arma3Sync.exe") -PathType Leaf)) {
    throw "Compact launcher EXE not found: $compactLauncher"
}
if (-not (Test-Path -LiteralPath (Join-Path $compactLauncher "Arma3Sync.jar") -PathType Leaf)) {
    throw "Compact launcher JAR not found: $compactLauncher"
}
if (-not $Compact) {
    $nativeRuntime = Join-Path $app "build\jpackage\windows-runtime"
    if (-not (Test-Path -LiteralPath (Join-Path $nativeRuntime "bin\java.exe") -PathType Leaf)) {
        throw "Bundled Java runtime not found: $nativeRuntime"
    }
}
if (-not (Test-Path -LiteralPath (Join-Path $root "installer\assets\Arma3Sync.ico") -PathType Leaf)) {
    throw "Installer icon not found: $(Join-Path $root 'installer\assets\Arma3Sync.ico')"
}

$nsisArgs = @()
if ($Compact) {
    $nsisArgs += "/DCOMPACT"
}
$suffix = if ($Compact) { "-compact" } else { "" }
$installerOutput = Join-Path $releaseOutput "Arma3Sync-$version$suffix-setup.exe"
if (Test-Path -LiteralPath $installerOutput) {
    Remove-Item -LiteralPath $installerOutput -Force
}
& $Nsis @nsisArgs (Join-Path $PSScriptRoot "Arma3Sync.nsi")
if ($LASTEXITCODE -ne 0) { throw "NSIS failed with exit code $LASTEXITCODE" }
if (-not (Test-Path -LiteralPath $installerOutput -PathType Leaf)) {
    throw "NSIS completed without creating the expected installer: $installerOutput"
}
if ((Get-Item -LiteralPath $installerOutput).Length -le 0) {
    throw "The generated installer is empty: $installerOutput"
}
Write-Host "Created installer: $installerOutput"

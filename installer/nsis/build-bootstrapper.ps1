param(
    [Parameter(Mandatory = $true)] [string]$OutputExe,
    [Parameter(Mandatory = $true)] [string]$IconFile
)

$ErrorActionPreference = "Stop"
$sourceRoot = Join-Path $PSScriptRoot "bootstrapper"
$resourceFile = Join-Path $sourceRoot "resource.syso"
$go = (Get-Command go.exe -ErrorAction Stop).Source
$rsrc = Join-Path (& go env GOPATH) "bin\rsrc.exe"

if (-not (Test-Path -LiteralPath $IconFile -PathType Leaf)) {
    throw "Icon file not found: $IconFile"
}
if (-not (Test-Path -LiteralPath $rsrc -PathType Leaf)) {
    throw "rsrc.exe not found: $rsrc. Install github.com/akavel/rsrc first."
}

$outputDirectory = Split-Path -Parent $OutputExe
New-Item -ItemType Directory -Path $outputDirectory -Force | Out-Null

try {
    & $rsrc -arch amd64 -ico $IconFile -o $resourceFile
    if ($LASTEXITCODE -ne 0) { throw "Icon resource generation failed: $LASTEXITCODE" }

    Push-Location $sourceRoot
    try {
        & $go build -trimpath -ldflags "-H=windowsgui -s -w" -o $OutputExe .
        if ($LASTEXITCODE -ne 0) { throw "Go bootstrapper build failed: $LASTEXITCODE" }
    } finally {
        Pop-Location
    }
} finally {
    Remove-Item -LiteralPath $resourceFile -Force -ErrorAction SilentlyContinue
}

Write-Host "Created Java 25 bootstrapper: $OutputExe"

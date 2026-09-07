param(
    [string]$Magick = "magick"
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$source = Join-Path $root "src\main\resources\resources\icons\app.svg"
$iconOut = Join-Path $root "installer\assets\Arma3Sync.ico"
$pngOut = Join-Path $root "installer\assets\Arma3Sync.png"
$runtimePngOut = Join-Path $root "src\main\resources\resources\icons\app-rendered.png"

if (-not (Test-Path -LiteralPath $source -PathType Leaf)) {
    throw "Icon source not found: $source"
}

if ($Magick -eq "magick" -and -not (Get-Command magick -ErrorAction SilentlyContinue)) {
    $installedMagick = "C:\Program Files\ImageMagick-7.1.2-Q16-HDRI\magick.exe"
    if (Test-Path -LiteralPath $installedMagick -PathType Leaf) {
        $Magick = $installedMagick
    }
}

if ($Magick -eq "magick" -and -not (Get-Command magick -ErrorAction SilentlyContinue)) {
    throw "ImageMagick was not found. Pass -Magick with the path to magick.exe."
}

& $Magick -background none -define "icon:auto-resize=256,128,64,48,32,16" $source $iconOut
if ($LASTEXITCODE -ne 0) { throw "ICO generation failed with exit code $LASTEXITCODE" }

& $Magick -background none $source -resize 512x512 $pngOut
if ($LASTEXITCODE -ne 0) { throw "PNG generation failed with exit code $LASTEXITCODE" }

& $Magick -background none $source -resize 512x512 $runtimePngOut
if ($LASTEXITCODE -ne 0) { throw "Runtime PNG generation failed with exit code $LASTEXITCODE" }

Write-Host "Generated $iconOut"
Write-Host "Generated $pngOut"
Write-Host "Generated $runtimePngOut"

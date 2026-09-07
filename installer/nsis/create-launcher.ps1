param(
    [Parameter(Mandatory = $true)] [string]$SourceExe,
    [Parameter(Mandatory = $true)] [string]$OutputExe,
    [Parameter(Mandatory = $true)] [string]$IconFile,
    [string]$JarName = "Arma3Sync.jar"
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path -LiteralPath $SourceExe -PathType Leaf)) {
    throw "Source launcher not found: $SourceExe"
}
if (-not (Test-Path -LiteralPath $IconFile -PathType Leaf)) {
    throw "Icon file not found: $IconFile"
}

$outputDirectory = Split-Path -Parent $OutputExe
New-Item -ItemType Directory -Path $outputDirectory -Force | Out-Null
Copy-Item -LiteralPath $SourceExe -Destination $OutputExe -Force

# The original Launch4j wrapper embeds the legacy ArmA3Sync.jar name.
# Replace that target while keeping the original Java discovery behavior.
$launcherBytes = [IO.File]::ReadAllBytes($OutputExe)
$oldJarBytes = [Text.Encoding]::ASCII.GetBytes('ArmA3Sync.jar')
$newJarBytes = [Text.Encoding]::ASCII.GetBytes($JarName)
if ($oldJarBytes.Length -ne $newJarBytes.Length) {
    throw "Launcher JAR name must have the same byte length as the original embedded name."
}
$replacementCount = 0
for ($index = 0; $index -le $launcherBytes.Length - $oldJarBytes.Length; $index++) {
    $match = $true
    for ($offset = 0; $offset -lt $oldJarBytes.Length; $offset++) {
        if ($launcherBytes[$index + $offset] -ne $oldJarBytes[$offset]) {
            $match = $false
            break
        }
    }
    if ($match) {
        [Array]::Copy($newJarBytes, 0, $launcherBytes, $index, $newJarBytes.Length)
        $replacementCount++
        $index += $oldJarBytes.Length - 1
    }
}
if ($replacementCount -eq 0) {
    throw "Embedded launcher target 'ArmA3Sync.jar' was not found in $SourceExe"
}
[IO.File]::WriteAllBytes($OutputExe, $launcherBytes)

Add-Type @'
using System;
using System.IO;
using System.Runtime.InteropServices;

public static class Win32ResourceUpdater
{
    [DllImport("kernel32.dll", CharSet = CharSet.Unicode, SetLastError = true)]
    public static extern IntPtr BeginUpdateResource(string fileName, bool deleteExistingResources);

    [DllImport("kernel32.dll", SetLastError = true)]
    public static extern bool UpdateResource(
        IntPtr update,
        IntPtr type,
        IntPtr name,
        ushort language,
        byte[] data,
        uint dataSize);

    [DllImport("kernel32.dll", SetLastError = true)]
    public static extern bool EndUpdateResource(IntPtr update, bool discard);

    public static IntPtr ResourceId(ushort id)
    {
        return new IntPtr(id);
    }
}
'@

$iconBytes = [IO.File]::ReadAllBytes($IconFile)
if ($iconBytes.Length -lt 6) { throw "Invalid ICO file: $IconFile" }
$reserved = [BitConverter]::ToUInt16($iconBytes, 0)
$type = [BitConverter]::ToUInt16($iconBytes, 2)
$count = [BitConverter]::ToUInt16($iconBytes, 4)
if ($reserved -ne 0 -or $type -ne 1 -or $count -eq 0) {
    throw "Invalid ICO header: $IconFile"
}

$update = [Win32ResourceUpdater]::BeginUpdateResource($OutputExe, $false)
if ($update -eq [IntPtr]::Zero) {
    throw "BeginUpdateResource failed: $([Runtime.InteropServices.Marshal]::GetLastWin32Error())"
}

try {
    $groupStream = New-Object IO.MemoryStream
    $groupWriter = New-Object IO.BinaryWriter($groupStream)
    $groupWriter.Write([uint16]0)
    $groupWriter.Write([uint16]1)
    $groupWriter.Write([uint16]$count)

    for ($index = 0; $index -lt $count; $index++) {
        $offset = 6 + ($index * 16)
        $width = $iconBytes[$offset]
        $height = $iconBytes[$offset + 1]
        $colors = $iconBytes[$offset + 2]
        $planes = [BitConverter]::ToUInt16($iconBytes, $offset + 4)
        $bits = [BitConverter]::ToUInt16($iconBytes, $offset + 6)
        $length = [BitConverter]::ToUInt32($iconBytes, $offset + 8)
        $imageOffset = [BitConverter]::ToUInt32($iconBytes, $offset + 12)
        $image = New-Object byte[] $length
        [Array]::Copy($iconBytes, [int]$imageOffset, $image, 0, [int]$length)
        $resourceId = [uint16]($index + 1)

        if (-not [Win32ResourceUpdater]::UpdateResource(
                $update,
                [Win32ResourceUpdater]::ResourceId(3),
                [Win32ResourceUpdater]::ResourceId($resourceId),
                1033,
                $image,
                [uint32]$image.Length)) {
            throw "Icon resource update failed: $([Runtime.InteropServices.Marshal]::GetLastWin32Error())"
        }

        $groupWriter.Write([byte]$width)
        $groupWriter.Write([byte]$height)
        $groupWriter.Write([byte]$colors)
        $groupWriter.Write([byte]0)
        $groupWriter.Write([uint16]$planes)
        $groupWriter.Write([uint16]$bits)
        $groupWriter.Write([uint32]$length)
        $groupWriter.Write([uint16]$resourceId)
    }

    $groupWriter.Flush()
    $groupData = $groupStream.ToArray()
    if (-not [Win32ResourceUpdater]::UpdateResource(
            $update,
            [Win32ResourceUpdater]::ResourceId(14),
            [Win32ResourceUpdater]::ResourceId(1),
            1033,
            $groupData,
            [uint32]$groupData.Length)) {
        throw "Icon group update failed: $([Runtime.InteropServices.Marshal]::GetLastWin32Error())"
    }
}
finally {
    $groupWriter.Dispose()
    $groupStream.Dispose()
    if (-not [Win32ResourceUpdater]::EndUpdateResource($update, $false)) {
        throw "EndUpdateResource failed: $([Runtime.InteropServices.Marshal]::GetLastWin32Error())"
    }
}

Write-Host "Created compact launcher: $OutputExe"

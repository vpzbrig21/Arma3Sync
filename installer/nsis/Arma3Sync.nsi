Unicode true
RequestExecutionLevel admin
SetCompressor /SOLID lzma
SetDatablockOptimize on

!include "MUI2.nsh"
!include "x64.nsh"
!include "WinVer.nsh"
!include "include\Version.nsh"

!define DIST_ROOT "..\..\build\install\Arma3Sync"
!define UPDATER_JAR "..\..\updater\build\distribution\ArmA3Sync-Updater.jar"
!define NATIVE_RUNTIME "..\..\build\jpackage\windows-runtime"
!define COMPACT_LAUNCHER_DIR "..\..\build\compact-launcher"
!define COMPACT_EXE "${COMPACT_LAUNCHER_DIR}\Arma3Sync.exe"
!define COMPACT_JAR "${COMPACT_LAUNCHER_DIR}\Arma3Sync.jar"
!define ICON_FILE "..\assets\Arma3Sync.ico"
!define PAYLOAD_ROOT "payload"
!define LICENSE_FILE "..\..\LICENCE.md"
!define README_FILE "..\..\README.md"

!define LAUNCHER_TARGET "$INSTDIR\Arma3Sync.exe"
!ifdef COMPACT
    !define OUTFILE_SUFFIX "-compact"
!else
    !define OUTFILE_SUFFIX ""
!endif

Name "${APP_NAME} ${APP_VERSION}"
OutFile "..\..\release\output\Arma3Sync-${APP_VERSION}${OUTFILE_SUFFIX}-setup.exe"
InstallDir "$PROGRAMFILES64\${APP_NAME}"
InstallDirRegKey HKLM "Software\${APP_PUBLISHER}\${APP_ID}" "InstallDir"
ShowInstDetails show
ShowUnInstDetails show
BrandingText "${APP_NAME}"

!define MUI_ABORTWARNING
!define MUI_ICON "${ICON_FILE}"
!define MUI_UNICON "${ICON_FILE}"
!insertmacro MUI_PAGE_WELCOME
!insertmacro MUI_PAGE_DIRECTORY
!insertmacro MUI_PAGE_INSTFILES
!insertmacro MUI_PAGE_FINISH
!insertmacro MUI_LANGUAGE "German"

VIProductVersion "${APP_VERSION}.0"
VIAddVersionKey ProductName "${APP_NAME}"
VIAddVersionKey ProductVersion "${APP_VERSION}"
VIAddVersionKey FileVersion "${APP_VERSION}.0"
VIAddVersionKey FileDescription "Arma3Sync launcher and addon synchronizer"
VIAddVersionKey LegalCopyright "GPLv3"

Function .onInit
    ${IfNot} ${RunningX64}
        MessageBox MB_ICONSTOP "Diese Version benötigt Windows 64-bit."
        Abort
    ${EndIf}
    ${IfNot} ${AtLeastWin7}
        MessageBox MB_ICONSTOP "Windows 7 oder neuer wird benötigt."
        Abort
    ${EndIf}
FunctionEnd

Section "Arma3Sync" SEC_MAIN
    SectionIn RO
    SetOutPath "$INSTDIR"
    ; The compact JAR is installed once in the root and is referenced by the
    ; generated start scripts. Do not package a second copy below lib/.
    File /r /x "lib\Arma3Sync-${APP_VERSION}.jar" "${DIST_ROOT}\*"
    File "${UPDATER_JAR}"
    ; Both variants use the same launcher and root-level application JAR.
    ; The standard package additionally installs runtime\ so the launcher
    ; never falls back to an incompatible system Java installation.
    File /oname=Arma3Sync.exe "${COMPACT_EXE}"
    File /oname=Arma3Sync.jar "${COMPACT_JAR}"
    !ifndef COMPACT
        ; NSIS stores recursive files below the current SetOutPath. Keep the
        ; bundled runtime isolated so the launcher can resolve runtime\bin\java.exe.
        SetOutPath "$INSTDIR\runtime"
        File /r "${NATIVE_RUNTIME}\*"
        SetOutPath "$INSTDIR"
    !endif
    File "${ICON_FILE}"
    File "..\..\a3s.xml"
    File "${PAYLOAD_ROOT}\Arma3Sync.bat"
    File "${PAYLOAD_ROOT}\Arma3Sync-console.bat"
    File "${PAYLOAD_ROOT}\Arma3Sync-noSingleInstancelock.bat"
    File "${PAYLOAD_ROOT}\Arma3Sync.sh"
    File "${PAYLOAD_ROOT}\Arma3Sync-console.sh"
    File "${PAYLOAD_ROOT}\version.txt"
    File /oname=README.md "${README_FILE}"
    File /oname=LICENCE.md "${LICENSE_FILE}"

    SetOutPath "$INSTDIR\resources\configuration"
    CreateDirectory "$INSTDIR\resources\configuration"
    ; Keep administrator/user customizations when an existing installation is upgraded.
    SetOverwrite off
    File "${PAYLOAD_ROOT}\updater.toml"
    SetOverwrite on
    ; Mutable profiles, repositories and caches are created below the current
    ; user's Windows data directories by the application. Keeping them out of
    ; Program Files lets the installed application run without elevation.

    WriteUninstaller "$INSTDIR\uninstall.exe"
    WriteRegStr HKLM "Software\${APP_PUBLISHER}\${APP_ID}" "InstallDir" "$INSTDIR"
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APP_ID}" "DisplayName" "${APP_NAME}"
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APP_ID}" "DisplayVersion" "${APP_VERSION}"
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APP_ID}" "Publisher" "${APP_PUBLISHER}"
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APP_ID}" "UninstallString" '"$INSTDIR\uninstall.exe"'
    WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APP_ID}" "NoModify" 1
    WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APP_ID}" "NoRepair" 1

    CreateDirectory "$SMPROGRAMS\${APP_NAME}"
    CreateShortCut "$SMPROGRAMS\${APP_NAME}\${APP_NAME}.lnk" "${LAUNCHER_TARGET}" "" "$INSTDIR\ArmA3Sync.ico"
    CreateShortCut "$DESKTOP\${APP_NAME}.lnk" "${LAUNCHER_TARGET}" "" "$INSTDIR\ArmA3Sync.ico"
SectionEnd

Section "Uninstall"
    Delete "$DESKTOP\${APP_NAME}.lnk"
    Delete "$SMPROGRAMS\${APP_NAME}\${APP_NAME}.lnk"
    RMDir "$SMPROGRAMS\${APP_NAME}"
    DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APP_ID}"
    DeleteRegKey HKLM "Software\${APP_PUBLISHER}\${APP_ID}"
    ; Preserve profiles/configuration/repositories for recoverability.
    RMDir /r "$INSTDIR\bin"
    RMDir /r "$INSTDIR\lib"
    RMDir /r "$INSTDIR\app"
    RMDir /r "$INSTDIR\runtime"
    Delete "$INSTDIR\ArmA3Sync-Updater.jar"
    Delete "$INSTDIR\Arma3Sync.exe"
    Delete "$INSTDIR\Arma3Sync.jar"
    ; Remove old launcher names from installations created before the naming fix.
    Delete "$INSTDIR\ArmA3Sync.exe"
    Delete "$INSTDIR\ArmA3Sync.jar"
    Delete "$INSTDIR\ArmA3Sync.ico"
    Delete "$INSTDIR\a3s.xml"
    Delete "$INSTDIR\README.md"
    Delete "$INSTDIR\LICENCE.md"
    Delete "$INSTDIR\Arma3Sync.bat"
    Delete "$INSTDIR\Arma3Sync-console.bat"
    Delete "$INSTDIR\Arma3Sync-noSingleInstancelock.bat"
    Delete "$INSTDIR\Arma3Sync.sh"
    Delete "$INSTDIR\Arma3Sync-console.sh"
    Delete "$INSTDIR\ArmA3Sync.bat"
    Delete "$INSTDIR\ArmA3Sync-console.bat"
    Delete "$INSTDIR\ArmA3Sync-noSingleInstancelock.bat"
    Delete "$INSTDIR\ArmA3Sync.sh"
    Delete "$INSTDIR\ArmA3Sync-console.sh"
    Delete "$INSTDIR\version.txt"
    Delete "$INSTDIR\uninstall.exe"
    RMDir "$INSTDIR"
SectionEnd

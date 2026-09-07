# BUILD & PACKAGING GUIDE

## Voraussetzungen
- JDK 25 (inkl. `jpackage` im `PATH`).
- Gradle 8+ global installiert. Ein Gradle Wrapper wird bewusst nicht
  mitgeliefert.
- Windows-spezifische Pakete können nur auf Windows, Linux-Pakete nur auf Linux erzeugt werden (Limitation von `jpackage`).

## Standard-Builds
| Zweck | Kommando | Ergebnis |
| --- | --- | --- |
| Clean Build + Tests | `gradle clean build` | kompiliert den Code und führt JUnit-Tests aus |
| Schlankes Distributions-Layout | `gradle installDist` | `build/install/Arma3Sync` mit Startskripten für Win/Linux |
| ZIP/TAR Distributionsarchive | `gradle packageApp` | `build/distributions/Arma3Sync.zip` / `.tar` |
| Fat JAR (alle Abhängigkeiten) | `gradle fatJar` | `build/libs/Arma3Sync-all.jar` – startbar via `java -jar` |
| Updater testen und JAR kopieren | `gradle :updater:test :updater:copyRuntimeJar` | `updater/build/distribution/ArmA3Sync-Updater.jar` |

## Startbares JAR
1. `gradle fatJar`
2. `java -jar build/libs/Arma3Sync-all.jar`

## Plattform-Pakete
### Windows (.exe Installer)
```
gradle jpackageWin
```
- Output: `build/jpackage/windows/Arma3Sync-${version}.exe`
- Enthält alle Abhängigkeiten und erstellt bei Installation optional eine Desktop-Verknüpfung.
- Kein Code-Signing integriert – muss bei Bedarf nach dem Build erfolgen.

### Windows-Release mit integrierter Java-Laufzeit

Für Endnutzer wird das Standardpaket empfohlen. Es erzeugt aus JDK 25
mit `jlink` eine reduzierte Runtime und legt sie unter `runtime/` neben dem
Anwendungs-JAR ab. Der gleiche native Bootstrapper wird in beiden Varianten
verwendet; im Standardpaket wird die lokale Runtime bevorzugt.

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\release\release.ps1
```

Die Compact-Variante bleibt für erfahrene Nutzer und kleine Archive verfügbar:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\release\release.ps1 -Compact
```

Das Standardpaket benötigt auf dem Zielsystem keine separat installierte
Java-Version. Die JAR-Datei bleibt für Linux, Kommandozeile und
Entwicklungsumgebungen verfügbar; dort wird weiterhin eine kompatible
System-Java-Laufzeit benötigt, sofern kein plattformspezifisches Runtime-Paket
gebaut wird.

### Linux (App-Image)
```
gradle jpackageLinux
```
- Output: `build/jpackage/linux/Arma3Sync`
- Liefert ein selbständiges App-Image; für `.deb`/`.rpm` ist eine Erweiterung der jpackage-Optionen nötig.

## Laufzeit-Hinweise
- Sowohl `fatJar` als auch jpackage-Bundles nutzen dieselbe Java-25-Toolchain, was reproduzierbare Builds sicherstellt.
- Die `resources/lib`-basierten Bibliotheken (JTattoo, jshortcut, junique) werden automatisch in jede Distribution kopiert.
- Auf Systemen ohne verfügbares `jpackage` schlagen die Tasks `jpackageWin`/`jpackageLinux` bewusst fehl. In diesem Fall entweder auf dem Zielbetriebssystem bauen oder das Distributions-ZIP (`gradle packageApp`) verwenden.

## Release-Build

Die integrierte Release-Automation liegt unter `release/`. Sie verwendet
`version.properties` als einzige Versionsquelle, baut den Updater als
unabhängiges Gradle-Subprojekt und ruft den NSIS-Build unter `installer/nsis/`
auf. Für den vollständigen Ablauf siehe [`release/README.md`](release/README.md).

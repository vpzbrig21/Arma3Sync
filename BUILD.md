# BUILD & PACKAGING GUIDE

## Voraussetzungen
- JDK 25 (inkl. `jpackage` im `PATH`).
- Gradle 8+ lokal installiert (oder `gradlew`, sobald verfügbar).
- Windows-spezifische Pakete können nur auf Windows, Linux-Pakete nur auf Linux erzeugt werden (Limitation von `jpackage`).

## Standard-Builds
| Zweck | Kommando | Ergebnis |
| --- | --- | --- |
| Clean Build + Tests | `gradle clean build` | kompiliert den Code und führt JUnit-Tests aus |
| Schlankes Distributions-Layout | `gradle installDist` | `build/install/Arma3Sync` mit Startskripten für Win/Linux |
| ZIP/TAR Distributionsarchive | `gradle packageApp` | `build/distributions/Arma3Sync.zip` / `.tar` |
| Fat JAR (alle Abhängigkeiten) | `gradle fatJar` | `build/libs/Arma3Sync-all.jar` – startbar via `java -jar` |

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

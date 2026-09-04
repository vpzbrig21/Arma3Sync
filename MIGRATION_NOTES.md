# Build Migration Notes

## Überblick
- Die alte Eclipse/Ant-Struktur wurde durch ein Gradle-Projekt ersetzt (`build.gradle`, `settings.gradle`).  
- Der gesamte Anwendungscode liegt jetzt unter `src/main/java`, Ressourcen (SVG-Icons, Schriftdateien) unter `src/main/resources`, sodass Standard-Java-Builds funktionieren.
- Alle bestehenden Drittanbieter-Bibliotheken werden weiterhin aus `resources/lib` eingebunden, damit die Anwendung unverändert bleibt. Gradle kopiert sie automatisch in die Distributions-Artefakte.
- Die Java-Toolchain ist auf Version 25 fixiert (`java.toolchain.languageVersion = 25` und `--release 25`), wodurch sich Builds konsistent über Windows und Linux hinweg verhalten (vorausgesetzt, JDK 25 ist installiert).

## Build & Tasks
| Zweck | Kommando |
| --- | --- |
| Kompilieren | `gradle compileJava` |
| Tests (Platzhalter, nutzt JUnit Platform) | `gradle test` |
| Anwendung starten | `gradle run` |
| Klassisches Jar bauen | `gradle jar` |
| Distributable Paket (ZIP unter `build/distributions/`) | `gradle packageApp` |

Zusätzlich stehen durch das `application`-Plugin auch `gradle installDist`, `gradle distZip` und `gradle distTar` zur Verfügung, die Startskripte für Windows und Linux erzeugen.

## Hinweise
- Die Gradle-Wrapper-Skripte können hinzugefügt werden, sobald ein Download des offiziellen `gradle-wrapper.jar` möglich ist (`gradle wrapper --gradle-version 8.7`). Bis dahin muss eine lokale Gradle-Installation (8.6+) verwendet werden.
- Binäre Assets wie die Geist/Inter-Schriften wurden in `src/main/resources/fr/soe/a3s/ui/fonts` verschoben, damit sie automatisch in die Artefakte kopiert werden.
- Laufzeitressourcen außerhalb der Anwendung (z.B. `resources/configuration`) bleiben unverändert auf Projektebene und können nach Bedarf in Installationspakete übernommen oder extern gepflegt werden.

# Release-Build

Alle für ein Release benötigten Quellen liegen innerhalb dieses Repositories.
Die externen Ordner `source_arma3sync_updater`, `nsis` und `scripts` im
übergeordneten Arbeitsverzeichnis sind nur noch historische Arbeitskopien und
werden vom kanonischen Releaseablauf nicht verwendet.

## Zentrale Version

Die einzige zu bearbeitende Versionsdatei ist:

```text
version.properties
```

Dort wird ausschließlich `app.version=x.y.z` geändert. Der
Versionsgenerator aktualisiert daraus automatisch die NSIS-Version und die
Versionsdatei im Installationspaket.

## Voraussetzungen

- JDK 25 mit `java`, `jpackage` und `jlink` im `PATH`.
- Global installiertes Gradle 8 oder neuer. Ein Gradle Wrapper wird bewusst
  nicht mitgeliefert.
- NSIS 3.12 unter `C:\Program Files (x86)\NSIS` oder `makensis` im `PATH`.

## Release erzeugen

Aus dem Repository-Root `source_arma3sync` starten:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\release\release.ps1
```

Das Standardpaket benötigt auf dem Zielsystem keine vorinstallierte
Java-Installation.

Für das Compactpaket ohne gebündelte Runtime:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\release\release.ps1 -Compact
```

Das Skript prüft die zentrale Version, erzeugt Anwendung und Updater, baut
den NSIS-Installer und bricht ab, wenn ein erwartetes Artefakt fehlt oder leer
ist. Die Ergebnisse liegen in `release/output/`:

- `Arma3Sync-<version>-setup.exe`
- `Arma3Sync-<version>.zip`
- `a3s.json` und `a3s.xml`
- für die Compact-Variante zusätzlich die jeweils mit `-compact` bezeichneten Dateien

Die ZIP-Datei enthält den Release-Inhalt und die SHA-256-Prüfsumme steht in
der JSON-Manifestdatei. `a3s.xml` bleibt für ältere Updater erhalten.

## Einzelne Komponenten

Der Updater ist ein unabhängiges Gradle-Projekt unter `updater/`, aber als
Subprojekt in `settings.gradle` eingebunden:

```powershell
gradle :updater:test :updater:copyRuntimeJar
```

NSIS und die Icon-Erzeugung liegen unter `installer/nsis/`. Das SVG unter
`src/main/resources/resources/icons/app.svg` ist die Quelle; abgeleitete
Installer-Assets werden mit folgendem Skript erzeugt:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\installer\nsis\generate-icons.ps1
```

Ein vollständiger Testlauf des Hauptprojekts ist:

```powershell
gradle clean test
```

Build- und Release-Ausgaben sind über `.gitignore` ausgeschlossen. Quellen,
Tests, NSIS-Dateien und Dokumentation bleiben dagegen versionierbar und können
gemeinsam mit dem Hauptprogramm veröffentlicht werden.

## Standard und Compact

Die Compact-Variante enthält keine Java-Runtime. Ihr nativer
`Arma3Sync.exe`-Bootstrapper sucht eine kompatible Java-Version ab 25 in
`JAVA_HOME`, `PATH`, der Windows-Registrierung und den üblichen
Installationspfaden. Die Dokumentation spricht deshalb bewusst von einer
„Java-Laufzeit 25+“; Nutzer müssen für diese Variante technisch meist ein JDK
installieren, weil Oracle ab Java 11 keinen separaten klassischen JRE-Installer
mehr anbietet.

Das Standardpaket erzeugt mit `jlink` eine auf Arma3Sync zugeschnittene
Runtime und liefert sie unter `runtime/` mit aus. Der Bootstrapper prüft diese
Runtime zuerst und startet anschließend immer das zentrale
`Arma3Sync.jar` im Installationsroot. Es gibt keinen separaten jpackage-
Anwendungslauncher und kein zweites zu aktualisierendes JAR. Dadurch ist das
Standardpaket für normale Windows-Nutzer die bevorzugte Ausgabe.

Die gebündelte Runtime muss pro Zielplattform und Architektur gebaut werden.
Das Windows-Release ist daher kein Ersatz für die plattformübergreifende JAR;
Linux-Nutzer verwenden weiterhin die vorhandenen Shell-/JAR-Startwege oder ein
separat gebautes Linux-App-Image.

Die technische Detailbeschreibung und Testcheckliste steht in
[`docs/JAVA_RUNTIME_PACKAGING.md`](../docs/JAVA_RUNTIME_PACKAGING.md).

### Prüfungen vor der Veröffentlichung

Nach einem Standard-Build muss das Release-Archiv mindestens diese Struktur
enthalten:

```text
Arma3Sync.exe
Arma3Sync.jar
runtime/bin/java.exe
runtime/bin/javaw.exe
```

Der Test sollte außerdem sicherstellen, dass das Programm mit einer absichtlich
früher im `PATH` stehenden Java-8-Installation weiterhin startet und dass ein
Update das Root-JAR aktualisiert, ohne die lokale Runtime oder Benutzer-
Konfiguration zu beschädigen.

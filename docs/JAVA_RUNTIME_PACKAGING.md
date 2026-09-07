# Java-Laufzeit und Windows-Pakete

## Ziel

Arma3Sync wird für Windows in zwei Varianten veröffentlicht:

| Variante | Java auf dem Zielsystem | Geeignet für |
| --- | --- | --- |
| Standard | keine vorinstallierte Java-Installation erforderlich | normale Endnutzer |
| Compact | Java 25 oder neuer muss vorhanden sein | Fortgeschrittene Nutzer und kleine Archive |

Das Standardpaket ist die empfohlene Ausgabe für GitHub-Releases. Es enthält
eine reduzierte Java-25-Runtime und kann direkt auf einem Windows-System ohne
vorinstalliertes Java gestartet werden.

## Gemeinsamer Startpfad

Beide Varianten enthalten dieselben zentralen Dateien:

```text
Arma3Sync.exe
Arma3Sync.jar
lib/...
```

`Arma3Sync.exe` ist ein kleiner Windows-Bootstrapper. Er startet immer das
`Arma3Sync.jar` im Installationsroot. Eine Standardinstallation ergänzt:

```text
runtime/bin/java.exe
runtime/bin/javaw.exe
runtime/conf/...
runtime/lib/...
```

Der Bootstrapper verwendet `runtime/bin/javaw.exe` zuerst. Nur beim
Compactpaket, das keine lokale Runtime enthält, sucht er in `JAVA_HOME`,
`JDK_HOME`, `PATH`, der Windows-Registrierung und üblichen
Java-Installationsordnern nach Java 25 oder neuer. Eine erkannte Java-8-
Installation wird abgelehnt.

Diese Reihenfolge verhindert insbesondere, dass Oracle-Java-8-Verknüpfungen
aus `PATH` die mitgelieferte Java-25-Runtime oder die gewünschte System-Java
überdecken.

## Runtime-Erzeugung

Die Runtime wird mit JDK 25 und `jlink` erzeugt. Die Modulmenge ist im
`jlinkRuntimeWin`-Task in `build.gradle` festgelegt. Die Ausgabe liegt unter:

```text
build/jpackage/windows-runtime/
```

Der Task entfernt Debugsymbole, Header und Manpages und komprimiert die
Runtime. Die `runtime/legal/`-Dateien bleiben Bestandteil des Pakets und
enthalten die zugehörigen Laufzeit-Hinweise.

## Release-Build

Aus `source_arma3sync`:

```powershell
# Standardpaket mit gebündelter Java-25-Runtime (empfohlen)
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\release\release.ps1

# Compactpaket ohne Runtime; Zielsystem benötigt Java 25+
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\release\release.ps1 -Compact
```

Die Standard-Ergebnisse sind:

```text
release/output/Arma3Sync-<version>-setup.exe
release/output/Arma3Sync-<version>.zip
release/output/a3s.json
release/output/a3s.xml
```

Die Compact-Ergebnisse tragen zur eindeutigen Unterscheidung den Suffix
`-compact` beziehungsweise `a3s-compact.*`.

Die Versionsnummer kommt ausschließlich aus `version.properties`. Die
Standard-ZIP wird aus demselben Inhalt wie der NSIS-Installer erzeugt.

## Update-Verhalten

Das Update darf nur das zentrale Root-JAR ersetzen:

```text
Arma3Sync.jar
```

Der Launcher startet genau diese Datei. Die gebündelte Runtime liegt getrennt
unter `runtime/` und wird nur durch einen ausdrücklich dafür vorgesehenen
Runtime- oder Installationsrelease aktualisiert. Dadurch gibt es kein zweites
Anwendungs-JAR unter `app/`, das vom Updater unbemerkt weiterverwendet werden
könnte.

## Testcheckliste

Vor der Veröffentlichung eines Standardpakets:

1. `gradle clean test :updater:test` erfolgreich ausführen.
2. `release.ps1` aus dem Repository-Root starten.
3. Im Archiv prüfen, dass `Arma3Sync.exe`, `Arma3Sync.jar`,
   `runtime/bin/java.exe` und `runtime/bin/javaw.exe` vorhanden sind.
4. Auf einem Testsystem Java 8 in `PATH` voranstellen und die Standard-EXE
   starten. Das Programm muss trotzdem Java 25 aus `runtime/` verwenden.
5. Das Root-JAR durch ein Testupdate ersetzen und prüfen, dass der nächste
   Start die aktualisierte Version verwendet.
6. Standard und Compact getrennt testen; die Compact-Variante muss bei
   fehlender Java-25+-Installation eine verständliche Fehlermeldung anzeigen.

## Lizenz- und Wartungshinweis

Die gebündelte Runtime ist ein Teil des Standard-Releases und muss bei jedem
Wechsel der Java-Version erneut gebaut und getestet werden. Die Lizenz- und
Drittanbieterhinweise unter `runtime/legal/` dürfen nicht entfernt werden.
Bei einem Wechsel der JDK-Distribution sind deren Lizenzbedingungen sowie die
Weitergaberegeln erneut zu prüfen.

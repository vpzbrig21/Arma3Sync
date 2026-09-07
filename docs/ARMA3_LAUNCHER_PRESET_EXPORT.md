# Arma 3 Launcher-Preset-Export

## Zweck

Arma3Sync kann ein ausgewähltes Modset auf ausdrückliche Benutzeraktion als
statisches HTML-Preset für den offiziellen Arma-3-Launcher exportieren. Das
Preset ist eine zusätzliche Komfortfunktion für Nutzer, die ein Event lieber
über den offiziellen Launcher starten möchten.

Der Export ersetzt nicht die Arma3Sync-Repositories. Das HTML-Preset enthält
Verweise auf Steam-Workshop-Items und kann deshalb weder Repository-Hashes noch
einen exakten Arma3Sync-Dateistand festschreiben.

## Kompatibilität mit alten Repositories

Der Export ist read-only bezüglich Repository- und Event-Daten:

- `a3s.xml` wird nicht umbenannt oder ersetzt.
- Die bestehende `Event`-Struktur und die Maps für Addon-/CDLC-Auswahl bleiben
  unverändert.
- Die neuen CDLC-Storeinformationen sind statische Programmdaten und werden
  nicht in ein Repository geschrieben.
- Alte Arma3Sync-Versionen sehen weiterhin das bisherige Repository- und
  Eventformat.
- Die HTML-Datei wird nur gespeichert, wenn der Benutzer den Export auslöst.

## Workshop-Mods

Für jeden ausgewählten Workshop-Mod wird die `meta.cpp` im lokalen Modordner
gelesen. Benötigt werden:

- `publishedid` als Steam-Workshop-ID
- `name` als Anzeigename; falls dieser Eintrag fehlt, wird der lokale
  Modordnername als Fallback verwendet

Aus der ID wird ein Link im Format
`https://steamcommunity.com/sharedfiles/filedetails/?id=<publishedid>`
gebildet. Die Ermittlung erfolgt ausschließlich innerhalb des ausgewählten
Modsets und ausschließlich für aktivierte Modset-Einträge. Andere lokal
installierte Addons werden nicht traversiert.

Fehlt die `meta.cpp` oder die `publishedid`, wird der Export vor der Auswahl
des Ziels angehalten und fragt nach den fehlenden Daten. Ein fehlender
Anzeigename ist dagegen kein schwerwiegender Fehler und wird durch den lokalen
Addon-/Ordnernamen ersetzt. Nach der Bestätigung werden ergänzte Daten lokal
gespeichert und der Export automatisch wiederholt. Wird der Metadaten-Dialog
abgebrochen, wird keine Datei geschrieben.

## CDLCs

Die Auswahl der CDLCs wird aus der bestehenden CDLC-Auswahl des Modsets/Event-
Eintrags übernommen. Die zentrale Definition in `GameDLCs` enthält zusätzlich
die Steam-App-ID für den HTML-Export:

| Arma3Sync-Kürzel | Anzeigename | Steam-App-ID |
| --- | --- | ---: |
| `Contact` | Contact | 1021790 |
| `GM` | Global Mobilization | 1042220 |
| `vn` | S.O.G Prairie Fire | 1227700 |
| `CSLA` | Iron Curtain | 1294440 |
| `WS` | Western Sahara | 1681170 |
| `SPE` | Spearhead 1944 | 1175380 |
| `EF` | Expeditionary Forces | 2647830 |
| `RF` | Reaction Forces | 2647760 |

Nur aktivierte CDLCs werden als `DlcContainer` in die HTML-Datei geschrieben.

## Benutzerablauf

1. Ein Modset im Bereich **Addon Groups** auswählen.
2. **Export HTML** anklicken.
3. Arma3Sync prüft das ausgewählte Modset vollständig.
4. Falls kritische Metadaten fehlen, Workshop-ID und Anzeigename im Dialog ergänzen.
5. Erst danach Speicherort und Dateinamen bestätigen.
6. Die erzeugte Datei im offiziellen Arma-3-Launcher importieren oder auf das
   Launcher-Fenster ziehen.

Wenn kritische Metadaten fehlen, erscheint der Dialog für Workshop-ID und
Anzeigename vor dem Speicherdialog. Erst wenn alle Angaben vollständig und gültig
sind, wird der Speicherdialog geöffnet. Wird die Metadatenabfrage abgebrochen,
wird keine Zieldatei ausgewählt oder geschrieben.
Die Datenbank liegt im Benutzerprofil
unter `%APPDATA%\Arma3Sync\configuration\known-workshop-mods.properties` auf
Windows beziehungsweise unter dem plattformüblichen Konfigurationsverzeichnis
auf Linux/macOS. Sie ist kein Repository-Bestandteil und verändert weder
`a3s.xml` noch Event- oder Modset-Dateien.

Für die Fehlersuche schreibt der Export zusätzlich eine Statusdatei
`launcher-preset-export.log` in dasselbe Konfigurationsverzeichnis. Sie enthält
keine Workshop-Dateiinhalte, sondern nur die Exportphasen und technische Fehler.
Bei einer portablen Testinstallation liegt sie unter
`resources/configuration/launcher-preset-export.log`.

Nach dem Speichern wird die erzeugte Datei geprüft. Doppelte Workshop-IDs
werden nur einmal exportiert und als Warnung angezeigt.

## Wartung

Wenn ein neues CDLC unterstützt werden soll, muss ausschließlich die zentrale
`GameDLCs`-Definition um Anzeigename und Steam-App-ID ergänzt werden. Die
persistierten Repository-Dateien müssen dafür nicht migriert werden.

Die HTML-Struktur orientiert sich an den vom offiziellen Launcher exportierten
Presets. Bei Änderungen des Launcher-Importformats sollte die generierte Datei
gegen einen aktuellen offiziellen Export verglichen und der Exporter-Test
entsprechend erweitert werden.

# Arma3Sync 2026.3.12-Beta

## FTP-Upload-Performance

- FTP- und SFTP-Uploads können jetzt mit mehreren unabhängigen Verbindungen
  parallel ausgeführt werden. Die Einstellung erlaubt 1 bis 10 Verbindungen;
  der Standardwert für bestehende und neue Repositorys ist 4.
- Die parallele Einstellung betrifft ausschließlich FTP und SFTP. HTTP/WEBDAV,
  HTTPS/WEBDAV und bestehende FTPS-Konfigurationen bleiben beim bisherigen
  seriellen Upload-Verhalten.
- Verzeichnisse werden vor den parallelen Dateiübertragungen vorbereitet.
  Synchronisationsmetadaten werden erst nach erfolgreichem Abschluss aller
  Dateiübertragungen geschrieben.
- Der Fortschritt wird über alle Upload-Verbindungen zusammengeführt. Bei
  parallelen Uploads wird kein unsicherer Resume-Index in zufälliger
  Abschlussreihenfolge gespeichert.

- Die Prüfung vorhandener Remote-Dateien verwendet innerhalb einer FTP-
  Upload-Session gecachte Verzeichnislisten. Dadurch wird nicht mehr für
  jede Datei ein separater `MLST`-Roundtrip benötigt.
- Bekannte FTP-Verzeichnisse werden innerhalb der Session wiederverwendet;
  unnötige Wechsel in das Basisverzeichnis entfallen bei aufeinanderfolgenden
  Dateien im selben Verzeichnis.
- Beim Löschen wird eine zusätzliche Existenzprüfung vermieden. Dateien und
  Verzeichnisse werden direkt gelöscht; bereits entfernte Dateien werden dabei
  als erledigt behandelt.

## Kompatibilität

- Die bestehende dauerhafte FTP-Upload-Verbindung bleibt erhalten.
- Jede parallele FTP-/SFTP-Dateiübertragung verwendet eine eigene Session;
  FTP- oder SFTP-Client-Sessions werden nicht von mehreren Threads geteilt.
- Die konfigurierte Anzahl der Upload-Verbindungen ist unabhängig von der
  Einstellung für Repository-Inhaltsprüfungen und der Download-Verbindungen.
- Wenn ein FTP-Server keine nutzbare Verzeichnisliste liefert, fällt der
  Export weiterhin auf `MLST` und anschließend auf die bisherige Listing-
  Prüfung zurück.
- SFTP, FTPS, HTTP/WEBDAV und HTTPS/WEBDAV werden durch diese Optimierung
  nicht verändert.
- Bestehende Repositorys und das vorhandene `.a3s`-Format bleiben kompatibel.

## Diagnose

- Das Debug-Log enthält nun Session-Zähler für geladene Verzeichnislisten und
  daraus bediente Dateiprüfungen.
- Das Debug-Log weist zusätzlich die verwendete Upload-Verbindungsanzahl aus.
- Ein erfolgreicher Upload erzeugt keinen irreführenden
  `cancellation requested`-Eintrag mehr. Normale Bereinigung und echter
  Benutzerabbruch werden getrennt behandelt.

## Validierung

- `gradle clean test` erfolgreich
- Ein bestehender FTP-Update-Lauf mit Datei-Uploads und Löschungen wurde als
  Referenz für die Optimierung verwendet.

## Geplante Release-Artefakte

- `Arma3Sync-2026.3.12.zip`
- `Arma3Sync-2026.3.12-compact.zip`
- `Arma3Sync-2026.3.12-setup.exe`
- `Arma3Sync-2026.3.12-compact-setup.exe`

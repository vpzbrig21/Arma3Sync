# Arma3Sync 2026.3.13-Beta

Test-Beta für parallele Repository-Uploads.

## Änderungen

### FTP-/SFTP-Upload

- Für FTP und SFTP kann die Anzahl paralleler Upload-Verbindungen zwischen 1
  und 10 gewählt werden.
- Der Standardwert beträgt 4 und gilt automatisch auch für ältere
  Repository-Konfigurationen.
- Jede parallele Übertragung verwendet eine eigene FTP-/SFTP-Session.
- Verzeichnisse werden vor dem Datei-Upload vorbereitet.
- Repository-Metadaten und Löschvorgänge werden erst nach Abschluss der
  Dateiübertragungen ausgeführt.
- Fortschritt und Geschwindigkeit werden über alle Sessions aggregiert.

### Abbruchverhalten

- Ein erfolgreicher Upload erzeugt keinen irreführenden
  `cancellation requested`-Eintrag mehr.
- Ein echter Benutzerabbruch beendet alle aktiven Upload-Sessions nur einmal.
- Fehler in einem Worker beenden die übrigen parallelen Uploads kontrolliert.

## Kompatibilität

- Die Einstellung für parallele Uploads ist unabhängig von Download- und
  Repository-Prüfverbindungen.
- HTTP/WEBDAV, HTTPS/WEBDAV und vorhandene FTPS-Konfigurationen bleiben
  seriell.
- Bestehende Repository-Dateien und das `.a3s`-Format bleiben kompatibel.

## Tests

- `gradle clean test` erfolgreich
- `gradle build` erfolgreich

## Release-Artefakte

- `Arma3Sync-2026.3.13.zip`
- `Arma3Sync-2026.3.13-compact.zip`
- `Arma3Sync-2026.3.13-setup.exe`
- `Arma3Sync-2026.3.13-compact-setup.exe`

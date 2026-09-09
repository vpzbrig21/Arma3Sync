# Arma3Sync 2026.3.11-Beta

## SFTP-Upload großer Dateien

- Die SFTP-Schreibpufferung verwendet jetzt 64 KB statt 1 MB.
- Damit werden SFTP-Server unterstützt, die bei größeren ausstehenden
  Schreibblöcken den SFTP-Kanal schließen.
- Der lokale Lesepuffer bleibt bei 1 MB; die Anpassung betrifft nur die
  Netzwerkübertragung.
- Ein 67-MB-PBO wurde mit der gebündelten Java-25-Runtime erfolgreich über
  SFTP übertragen.

## Fehlerbehebung

- Behebt vorzeitige `EOFException`-Fehler beim Upload großer Dateien.
- Kleine Dateien und der bestehende persistente SFTP-Upload bleiben kompatibel.
- Die Korrekturen aus `2026.3.10-Beta` für die gebündelte Runtime bleiben
  enthalten.

## Kompatibilität

- Bestehende SFTP-Konfigurationen mit benutzerdefinierten Ports wie `2022`,
  Passwortauthentifizierung und Remote-Pfaden bleiben kompatibel.
- FTP, FTPS, HTTP/WEBDAV und HTTPS/WEBDAV werden nicht verändert.
- Das Standardpaket benötigt weiterhin keine separat installierte Java-
  Laufzeit.
- Das Compactpaket benötigt weiterhin eine separat installierte Java-25-
  Laufzeit.

## Validierung

- `gradle clean test` erfolgreich
- SFTP-Verbindung und Authentifizierung mit der gebündelten Runtime erfolgreich
- Upload einer 67-MB-Datei mit 64-KB-SFTP-Puffer erfolgreich

## Geplante Release-Artefakte

- `Arma3Sync-2026.3.11.zip`
- `Arma3Sync-2026.3.11-compact.zip`
- `Arma3Sync-2026.3.11-setup.exe`
- `Arma3Sync-2026.3.11-compact-setup.exe`

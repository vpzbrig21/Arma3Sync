# Arma3Sync 2026.3.9-Beta

## SFTP-Verbindungsaufbau

- SFTP-Ziele werden vor dem SSH-Aufbau explizit per DNS aufgelöst.
- Bei mehreren verfügbaren Adressen wird IPv4 bevorzugt. Das verhindert lange
  Wartezeiten durch nicht erreichbare IPv6-Routen und behält die Unterstützung
  reiner IPv6-Ziele bei.
- Die ausgewählte Zieladresse wird direkt an Apache MINA SSHD übergeben. Der
  konfigurierte Hostname, Port und Remote-Pfad werden nicht verändert.

## Diagnose

- Der optionale Startparameter `-debug` protokolliert nun zusätzlich:
  - verwendete Java-Runtime und tatsächlichen Anwendungspfad,
  - tatsächlich geladene Apache-MINA-SSHD-Bibliotheken,
  - DNS-Auflösung und ausgewählte Zieladresse,
  - Versand und Abschluss der SSH-Verbindungsanforderung,
  - Fehler des SSH-Verbindungs-Futures.
- Passwörter und andere Zugangsdaten werden weiterhin nicht gespeichert.
- Der Stop-Button kann einen ausstehenden SFTP-Verbindungsversuch weiterhin
  abbrechen.

## Kompatibilität

- Bestehende SFTP-Konfigurationen mit benutzerdefinierten Ports, einschließlich
  Port `2022`, bleiben kompatibel.
- FTP, FTPS, HTTP/WEBDAV und HTTPS/WEBDAV werden nicht verändert.
- Das Standardpaket enthält weiterhin die reduzierte Java-25-Runtime und
  benötigt keine separat installierte Java-Laufzeit.
- Das Compactpaket benötigt weiterhin eine separat installierte Java-25-
  Laufzeit.

## Validierung

- `gradle clean test` erfolgreich
- Apache MINA SSHD `2.15.0` erfolgreich eingebunden
- Standard- und Compact-Release-Build vorgesehen

## Geplante Release-Artefakte

- `Arma3Sync-2026.3.9.zip`
- `Arma3Sync-2026.3.9-compact.zip`
- `Arma3Sync-2026.3.9-setup.exe`
- `Arma3Sync-2026.3.9-compact-setup.exe`

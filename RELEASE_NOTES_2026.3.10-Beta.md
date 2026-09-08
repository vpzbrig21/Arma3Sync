# Arma3Sync 2026.3.10-Beta

## SFTP-Verbindung mit der Standardinstallation

- Die mitgelieferte reduzierte Java-25-Runtime enthält jetzt die zusätzlich
  erforderlichen Module `java.management` und `java.rmi`.
- SFTP-Verbindungen bleiben dadurch nicht mehr beim SSH-Verbindungsaufbau im
  Status `Pending` stehen.
- Die Verbindung wurde mit der gebündelten Runtime gegen einen erreichbaren
  SFTP-Server getestet: SSH-Verbindung, Passwortauthentifizierung und Öffnen
  des SFTP-Subsystems waren erfolgreich.

## Diagnose

- Die Diagnose aus `2026.3.9-Beta` bleibt enthalten und protokolliert Java-
  Runtime, Anwendungspfad, geladene SSHD-Bibliotheken, DNS-Auflösung,
  Zieladresse und den Abschluss des SSH-Verbindungs-Futures.
- Zugangsdaten werden weiterhin nicht protokolliert.

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
- Gebündelte Runtime mit Apache MINA SSHD 2.15.0 erfolgreich getestet
- Standard- und Compact-Release-Build vorgesehen

## Geplante Release-Artefakte

- `Arma3Sync-2026.3.10.zip`
- `Arma3Sync-2026.3.10-compact.zip`
- `Arma3Sync-2026.3.10-setup.exe`
- `Arma3Sync-2026.3.10-compact-setup.exe`

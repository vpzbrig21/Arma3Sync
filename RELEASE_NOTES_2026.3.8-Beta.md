# Arma3Sync 2026.3.8-Beta

## SFTP-Kompatibilität

- Apache MINA SSHD wurde von `2.14.0` auf `2.15.0` aktualisiert.
- Die Aktualisierung behebt eine bekannte Regression der Version `2.14.0`, die
  bei erreichbaren SFTP-Servern zu langen oder scheinbar hängenden
  SSH-Verbindungsversuchen führen konnte.
- SFTP-Verbindungen mit benutzerdefinierten Ports, einschließlich Port `2022`,
  bleiben unterstützt.
- Passwortauthentifizierung und die bestehende persistente Upload-Sitzung
  bleiben unverändert.

## Diagnose und Abbruch

- Der optionale Debug-Log protokolliert SFTP-Verbindungsphasen und vollständige
  Fehlerursachen weiterhin ohne Passwörter zu speichern.
- Ein laufender SFTP-Verbindungsversuch kann über den Stop-Button abgebrochen
  werden, ohne die Swing-Oberfläche zu blockieren.

## Kompatibilität

- Bestehende Repositorys, Profile und Upload-Konfigurationen bleiben erhalten.
- Das Standardpaket enthält weiterhin die reduzierte Java-25-Runtime und
  benötigt keine separat installierte Java-Laufzeit.
- Das Compactpaket benötigt weiterhin eine separat installierte Java-25-
  Laufzeit.

## Validierung

- `gradle clean test` erfolgreich
- Hauptprogramm- und Updater-Tests erfolgreich
- Runtime-Distribution erfolgreich erzeugt
- Gebündelte SSHD-Bibliotheken: `sshd-common`, `sshd-core` und `sshd-sftp`
  jeweils in Version `2.15.0`

## Artefakte

- `Arma3Sync-2026.3.8.zip`
- `Arma3Sync-2026.3.8-compact.zip`
- `Arma3Sync-2026.3.8-setup.exe`
- `Arma3Sync-2026.3.8-compact-setup.exe`

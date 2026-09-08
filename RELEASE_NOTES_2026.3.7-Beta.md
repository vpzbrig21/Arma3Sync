# Arma3Sync 2026.3.7-Beta

## SFTP und Benutzeroberfläche

- Apache MINA SSHD wurde von `2.14.0` auf `2.15.0` aktualisiert. Dies behebt
  eine bekannte Regression im SSH-Verbindungsaufbau, die trotz erreichbarem
  TCP-Port zu langen oder scheinbar hängenden SFTP-Verbindungsversuchen führen
  konnte.
- Der automatische Reconnect nach einem fehlgeschlagenen Upload blockiert die
  Swing-Oberfläche nicht mehr.
- Der erneute Upload läuft wieder in einem eigenen Hintergrund-Thread. Buttons,
  Fenster und Abbruchfunktionen bleiben dadurch während des Verbindungsaufbaus
  erreichbar.
- Der Debug-Log enthält jetzt vollständige Stacktraces einschließlich der
  ursprünglichen Ursache.
- Bei einem langen SSH-Verbindungsaufbau wird regelmäßig protokolliert, dass der
  Verbindungsversuch noch läuft und wie viel Timeout verbleibt.

## Testhinweis

Der Log sollte nun eindeutig zeigen, ob die Verbindung bei TCP/SSH-Aufbau,
Authentifizierung, SFTP-Subsystem, Remote-Pfad oder Dateiübertragung scheitert.
Ein Upload beginnt erst nach erfolgreichem Abschluss aller Verbindungsphasen.

```powershell
Arma3Sync.exe -debug
```

Die erste Log-Datei liegt unter
`%APPDATA%\Arma3Sync\configuration\arma3sync-debug.log.0`; portable
Installationen verwenden `resources\configuration`.

## Kompatibilität

- Bestehende Repositorys, Profile und Upload-Konfigurationen bleiben erhalten.
- Benutzerdefinierte SFTP-Ports wie `2022` werden weiterhin unverändert
  verwendet.
- Das Standardpaket enthält die reduzierte Java-25-Runtime.
- Das Compactpaket setzt eine separat verfügbare Java-25+-Laufzeit voraus.

## Artefakte

- `Arma3Sync-2026.3.7.zip`
- `Arma3Sync-2026.3.7-compact.zip`
- `Arma3Sync-2026.3.7-setup.exe`
- `Arma3Sync-2026.3.7-compact-setup.exe`

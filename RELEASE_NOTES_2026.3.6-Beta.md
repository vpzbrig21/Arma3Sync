# Arma3Sync 2026.3.6-Beta

## SFTP-Verbindungsaufbau

- Der SSH-Verbindungsaufbau wird jetzt in kurzen Intervallen überwacht.
- Der Button „Stop“ beendet einen laufenden SFTP-Verbindungsversuch aktiv und
  wartet nicht mehr auf den vollständigen konfigurierten Timeout.
- Fehler beim Erreichen des SFTP-Servers und echte Verbindungs-Timeouts werden
  sauber an den Upload-Ablauf weitergegeben und im Debug-Log festgehalten.
- Die bestehende SFTP-Konfiguration bleibt erhalten: Host, benutzerdefinierter
  Port wie `2022`, Benutzerkonto und Remote-Pfad werden unverändert verwendet.

## Diagnose

Mit `-debug` lässt sich der Ablauf nachvollziehen:

```powershell
Arma3Sync.exe -debug
```

Die erste Log-Datei liegt unter
`%APPDATA%\Arma3Sync\configuration\arma3sync-debug.log.0`; portable
Installationen verwenden `resources\configuration`.

## Wichtiger Testhinweis

Der Upload kann erst beginnen, wenn der konfigurierte SFTP-Port vom Zielserver
erreichbar ist. Ein korrekter Benutzername und ein korrektes Passwort reichen
nicht aus, wenn der Port durch Firewall, NAT, Portweiterleitung oder einen nicht
laufenden SFTP-Dienst blockiert wird.

## Kompatibilität

- Bestehende Repositorys, Profile und Upload-Protokolle bleiben kompatibel.
- Das Standardpaket enthält die reduzierte Java-25-Runtime.
- Das Compactpaket setzt eine separat verfügbare Java-25+-Laufzeit voraus.

## Artefakte

- `Arma3Sync-2026.3.6.zip`
- `Arma3Sync-2026.3.6-compact.zip`
- `Arma3Sync-2026.3.6-setup.exe`
- `Arma3Sync-2026.3.6-compact-setup.exe`

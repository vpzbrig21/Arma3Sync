# Arma3Sync 2026.3.5-Beta

## Debugging und Support

- Ein optionaler Startparameter `-debug` aktiviert einen rotierenden
  Diagnose-Log für reproduzierbare Supportfälle.
- Der Parameter kann mit den bestehenden GUI-, Konsolen-, Repository-Prüf- und
  Sync-Aufrufen kombiniert werden, ohne die bisherige Befehlsauswertung zu
  verändern.
- Repository-Prüfungen und Uploads protokollieren ihre Phasen, den Aufbau der
  Verbindung, Remote-Pfade, Fortschrittsmarken, Dateigrößen, Laufzeiten,
  Abbrüche und Fehler.
- FTP, FTPS und SFTP werden für den Upload diagnostisch getrennt erfasst. Das
  hilft insbesondere bei blockierten SSH-/SFTP-Verbindungen und TLS-
  Datenkanalproblemen.
- Der Diagnose-Log wird nur bei ausdrücklicher Aktivierung erzeugt und enthält
  keine Passwörter.

## Verwendung

```powershell
Arma3Sync.exe -debug
```

Die erste Datei liegt unter
`%APPDATA%\Arma3Sync\configuration\arma3sync-debug.log.0`. Portable
Installationen verwenden `resources\configuration`; ältere Log-Generationen
tragen die Endungen `.1` und `.2`.

## Kompatibilität

- Bestehende Repositorys, Profile und die bisherigen Upload-Protokolle bleiben
  kompatibel.
- Das Standardpaket enthält weiterhin die reduzierte Java-25-Runtime. Das
  Compactpaket setzt eine separat verfügbare Java-25+-Laufzeit voraus.
- Diese Version ist ein Beta-Build für gezielte Tests, insbesondere der
  Upload-Diagnose.

## Artefakte

- `Arma3Sync-2026.3.5.zip`
- `Arma3Sync-2026.3.5-compact.zip`
- `Arma3Sync-2026.3.5-setup.exe`
- `Arma3Sync-2026.3.5-compact-setup.exe`

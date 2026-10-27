# Repository-Verwaltung über Console und CLI

Die aktuelle Arma3Sync-Version enthält weiterhin eine interaktive
Repository-Console. Repositorys lassen sich dort mit `NEW` anlegen und mit
`BUILD` aufbauen. Alternativ können Repositorys über die GUI angelegt und
verwaltet werden.

## Interaktive Console

Starte im Installationsordner das mitgelieferte Console-Skript:

```powershell
Arma3Sync-console.bat
```

Auf Linux verwende den mitgelieferten Programmstarter:

```sh
./Arma3Sync-console.sh
```

Gib anschließend einen der angezeigten Befehle ein:

- `NEW` – Repository-Verbindung und lokalen Hauptordner einrichten.
- `BUILD` – lokalen Repository-Inhalt prüfen und Repository-Metadaten sowie
  Changelog erzeugen. Ein frisch angelegtes Repository muss zuerst gebaut
  werden.
- `CHECK` – Repository-Synchronisierung prüfen.
- `LIST` – konfigurierte Repositorys anzeigen.
- `SYNC` – Inhalte eines Repositorys mit einem Zielordner synchronisieren.
- `DELETE` – lokal gespeichertes Repository entfernen.
- `DISCORD` – den zuletzt gespeicherten Build-Changelog an den Webhook des
  ausgewählten Repositorys senden.
- `DISCORDSETUP` – Discord-Webhook-Einstellungen für ein Repository interaktiv
  einrichten oder ändern.
- `COMMANDS`, `VERSION`, `UPDATE`, `EXTRACT`, `QUIT` – Hilfe, Version,
  Update-Prüfung, Bikey-Extraktion und Beenden.

## Console und CLI im Vergleich

Die Console ist interaktiv und bietet auch Repository-Erstellung, Löschen und
Discord-Konfiguration. Die nicht-interaktive CLI kann nur die unten mit einem
Argument aufgeführten Operationen ausführen.

| Operation | Interaktive Console | CLI-Argument |
| --- | --- | --- |
| Repository erstellen | `NEW` | — |
| Repository bauen | `BUILD` | `-build "Repository Name"` |
| Repository prüfen | `CHECK` | `-check "Repository Name"` |
| Repository löschen | `DELETE` | — |
| Updates prüfen | `UPDATE` | `-update` |
| Discord einrichten | `DISCORDSETUP` | — (interaktiv einrichten) |
| Changelog senden | `DISCORD` | `-discord "Repository Name"` |
| Synchronisieren | `SYNC` | `-sync "Repository Name" "Ziel" true/false` |
| `.bikey`-Dateien extrahieren | `EXTRACT` | `-extract "Quelle" "Ziel"` |

`NEW`, `DELETE` und `DISCORDSETUP` sind derzeit Console-Befehle; es gibt dafür
keine gleichnamigen nicht-interaktiven CLI-Argumente. `CHECK`, `UPDATE`, `BUILD`
und `DISCORD` sind sowohl interaktiv als auch per CLI verfügbar.

Beispiel für einen typischen initialen Ablauf: `NEW`, danach `BUILD`.
Discord-Einstellungen lassen sich in der GUI oder vollständig über
`DISCORDSETUP` in der Console konfigurieren. Details stehen in der
[Discord-Webhook-Anleitung](DISCORD_CHANGELOG_WEBHOOK.md).

### Komplett ohne GUI einrichten

1. Starte `Arma3Sync-console.bat` (Linux: `./Arma3Sync-console.sh`).
2. Gib `NEW` ein und beantworte die Fragen für das Repository. Führe danach
   `BUILD` aus, damit der erste Changelog erzeugt wird.
3. Gib `DISCORDSETUP` ein und wähle den Repository-Namen. Die Console fragt
   nacheinander nach Webhook-URL, Anzeigename, Titel, Emojis, optionalen
   Bild-URLs und Mentions.
4. Gib `DISCORD` ein und wähle dasselbe Repository, um den jüngsten Build an
   Discord zu senden.

Bei `DISCORDSETUP` lässt Enter den aktuellen Wert unverändert. Für optionale
Felder löscht `-` den gespeicherten Wert. Die Webhook-URL ist erforderlich.
Die Console validiert das Discord-Webhook-Format und Mention-IDs und speichert
die Angaben im lokalen Repository-Profil. Ein späterer Aufruf von
`DISCORDSETUP` kann die Werte ändern.

## Nicht-interaktive CLI

Die ausführbare Datei akzeptiert diese Aufgaben als Argumente. Repository-Namen
mit Leerzeichen müssen in Anführungszeichen stehen.

```powershell
Arma3Sync-console.bat -build "Repository Name"
Arma3Sync-console.bat -check "Repository Name"
Arma3Sync-console.bat -sync "Repository Name" "D:\Arma 3" false
Arma3Sync-console.bat -extract "D:\Source" "D:\Keys"
Arma3Sync-console.bat -update
Arma3Sync-console.bat -discord "Repository Name"
```

Auf Linux sind die entsprechenden Befehle zum Beispiel:

```sh
./Arma3Sync-console.sh -build "Repository Name"
./Arma3Sync-console.sh -check "Repository Name"
./Arma3Sync-console.sh -sync "Repository Name" "/srv/arma3" false
./Arma3Sync-console.sh -extract "/srv/source" "/srv/keys"
./Arma3Sync-console.sh -update
./Arma3Sync-console.sh -discord "Repository Name"
```

`-sync` erwartet `true` oder `false` für den exakten Dateiabgleich. Bei `true`
können zusätzliche Dateien im Zielordner gelöscht werden.

`-discord` veröffentlicht den Changelog des jüngsten lokal gespeicherten Builds
und verwendet die pro Repository gespeicherten
Webhook-, Anzeige-, Bild-, Emoji- und Mention-Einstellungen. Der Befehl führt
selbst keinen Build oder Check aus. Für einen automatisierten Ablauf daher nach
erfolgreichem `-build "Repository Name"` den `-discord`-Befehl starten. Die
Webhook-URL wird nicht als CLI-Argument übergeben. Wenn der jüngste Build keine
Repository-Inhaltsänderung hatte, wird eine leere Änderungsübersicht mit dessen
Build-Datum gesendet statt eines älteren Changelogs.

## Datenordner und Automatisierung

Console und CLI greifen auf dieselben lokalen Arma3Sync-Einstellungen wie die
GUI zu. Automatisierte Aufrufe müssen daher mit demselben Betriebssystemkonto
und demselben Datenordner laufen, unter dem die Repositorys konfiguriert sind.
Der Prozess liefert für `-discord` den Exit-Code `0` bei Erfolg und `1` bei
Fehler, damit Skripte den Versand überwachen können.

Beispiel Windows-Batchdatei für einen automatisierten Versand nach einem
separaten, erfolgreich abgeschlossenen Build:

```bat
@echo off
Arma3Sync-console.bat -discord "Repository Name"
exit /b %errorlevel%
```

CLI/Console-Aufruf und Sonderfälle sind in der
[Discord-Anleitung](DISCORD_CHANGELOG_WEBHOOK.md) ebenfalls zusammengefasst.

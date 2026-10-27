# Discord-Changelog per Webhook senden

Arma3Sync kann den Changelog eines Repositories als Discord-Nachricht mit
Components V2 senden. Optional kann oberhalb des Changelogs eine Ping-Zeile mit
`@everyone`, Rollen und/oder einzelnen Benutzern erscheinen. Die Einstellungen
werden für jedes Repository getrennt gespeichert.

## Webhook einrichten

1. Öffne in Discord die Servereinstellungen und gehe zu **Integrationen →
   Webhooks**.
2. Erstelle einen Webhook für den gewünschten Textkanal und kopiere seine URL.
3. Öffne in Arma3Sync den Repository-Changelog und wähle **Discord settings**.
4. Trage die Webhook-URL ein und speichere die Einstellungen.

Behandle die Webhook-URL wie ein Passwort: Wer sie kennt, kann Nachrichten in
den verbundenen Kanal senden.

## Workshop-Namen und Links

Optional kann Arma3Sync die Ordnernamen der Mods in Discord durch deren
Workshop-Namen ersetzen und als Steam-Workshop-Links formatieren. Aktiviere
dazu in **Discord settings** die Option **Show Workshop names as links when
available**. Arma3Sync liest dafür `name` und `publishedid` aus der `meta.cpp`
im jeweiligen Modordner. Die Einträge werden dann nach dem angezeigten Namen
sortiert.

Fehlt eine gültige `meta.cpp`, verwendet Arma3Sync den Ordnernamen. Das gilt
auch für entfernte Mods, deren lokaler Ordner beim Senden nicht mehr vorhanden
ist. Die Einstellung gilt nur für das jeweilige Repository und wird auch beim
Versand über `DISCORD` oder `-discord` verwendet. In der Console lässt sie sich
mit **DISCORDSETUP** über die Frage **Show Workshop names as links when
available** ein- oder ausschalten.

## Mentions aktivieren

1. Öffne erneut **Discord settings** beim gewünschten Repository.
2. Aktiviere **Post enabled mentions above the changelog**.
3. Aktiviere bei Bedarf **Notify @everyone**.
4. Trage bei Bedarf Rollen-IDs und Benutzer-IDs in die jeweiligen Felder ein.
5. Speichere die Einstellungen. Die Ping-Zeile wird bei **Send to Discord**
   oberhalb des Changelogs eingefügt.

Ohne aktivierte Mention-Option werden keine Benachrichtigungen ausgelöst. Ist
der Changelog so lang, dass Arma3Sync ihn in mehrere Discord-Nachrichten teilen
muss, steht die Ping-Zeile nur über der ersten Nachricht.

## Rollen benachrichtigen

Discord-Rollen werden über ihre numerische Rollen-ID angesprochen, nicht über
ihren Namen oder das Textformat `<@&ID>`.

1. Aktiviere in Discord **User Settings → Advanced → Developer Mode**.
2. Öffne die Servereinstellungen und gehe zu **Roles**.
3. Öffne das Kontextmenü der gewünschten Rolle und wähle **Copy Role ID**.
4. Füge die reine ID in **Notify roles (IDs, comma-separated)** ein.

Mehrere Rollen-IDs können durch Kommas, Semikolons oder Leerzeichen getrennt
werden, zum Beispiel:

```text
111111111111111111, 222222222222222222
```

Arma3Sync erzeugt daraus Mentions im Format `<@&111111111111111111>` und lässt
Discord nur die eingetragenen Rollen auswerten.

## Einzelne Benutzer benachrichtigen

Benutzer werden über ihre numerische Benutzer-ID angesprochen, nicht über
Benutzernamen, Anzeigenamen oder das Textformat `<@ID>`.

1. Aktiviere **Developer Mode**, falls noch nicht geschehen.
2. Öffne das Kontextmenü des Benutzers in der Mitgliederliste oder auf seinem
   Profil und wähle **Copy User ID**.
3. Füge die reine ID in **Notify users (IDs, comma-separated)** ein.

Mehrere Benutzer-IDs können wie Rollen-IDs durch Kommas, Semikolons oder
Leerzeichen getrennt werden. Beispiel:

```text
333333333333333333, 444444444444444444
```

Arma3Sync erzeugt daraus Mentions im Format `<@333333333333333333>`.

## @everyone und Berechtigungen

`@everyone` wird über die Checkbox **Notify @everyone** aktiviert. Rollen und
Benutzer werden nur benachrichtigt, wenn ihre IDs in den passenden Feldern
stehen. Die IDs der beiden Felder werden nicht miteinander vermischt.

Discord akzeptiert höchstens **100 Rollen- und Benutzer-IDs zusammen**. Doppelte
IDs werden nur einmal verwendet. Rollen-Pings funktionieren nur, wenn die Rolle
erwähnbar ist oder Discord dem Absender die nötige Mention-Berechtigung gibt.
Für `@everyone` muss Discord das Erwähnen aller Mitglieder erlauben. Discords Server-, Kanal- und
Benutzer-Benachrichtigungseinstellungen können beeinflussen, ob eine Push-
Benachrichtigung zugestellt wird.

## Falls kein Ping ankommt

- Prüfe, ob **Post enabled mentions above the changelog** aktiviert und die
  Einstellung gespeichert ist.
- Prüfe, dass nur numerische IDs (17 bis 20 Ziffern) eingetragen sind.
- Prüfe, dass die Rolle erwähnbar ist oder der Webhook `@everyone` erwähnen
  darf.
- Benutzer müssen im Discord-Server sein; Discords persönliche
  Benachrichtigungseinstellungen können Push-Mitteilungen unterdrücken.
- Prüfe, dass die Nachricht im erwarteten Kanal erscheint und dass dieser
  Webhook zu diesem Kanal gehört.

## Versand über Console oder CLI

Webhook-Einstellungen werden pro Repository in Arma3Sync gespeichert. Richte
den Webhook deshalb zunächst in der GUI unter **Changelog → Discord settings**
ein. Die CLI verwendet diese gespeicherten Einstellungen; die Webhook-URL muss
nicht als Befehlszeilenargument übergeben werden und erscheint dadurch nicht in
der Shell-History.

Für einen einzelnen Versand unter Windows:

```powershell
Arma3Sync-console.bat -discord "Repository Name"
```

Unter Linux verwende den mitgelieferten Programmstarter und dieselbe Option:

```sh
./Arma3Sync-console.sh -discord "Repository Name"
```

Der Befehl sendet den Changelog des jüngsten lokal gespeicherten Builds. Falls
dieser Build keine Repository-Inhaltsänderung hatte, sendet Arma3Sync eine leere
Änderungsübersicht mit dem Build-Datum des jüngsten Builds statt eines älteren
Changelogs. Falls die Übersicht zu lang für eine Discord-Nachricht ist, wird sie
automatisch auf mehrere Nachrichten aufgeteilt.

Für interaktive Nutzung starte `Arma3Sync-console.bat` (Linux:
`./Arma3Sync-console.sh`), gib `DISCORD` ein und danach den Repository-Namen. Die übrigen
Console-Befehle zeigt `COMMANDS` an.

### Einstellungen ausschließlich in der Console

Auch ohne GUI kann ein Repository vollständig eingerichtet werden:

1. Starte `Arma3Sync-console.bat` unter Windows oder `./Arma3Sync-console.sh`
   unter Linux.
2. Lege bei Bedarf mit `NEW` ein Repository an und führe danach `BUILD` aus.
3. Gib `DISCORDSETUP` ein, wähle das Repository und beantworte die Fragen für
   Webhook, Name, Titel, Emojis, Bilder und Mentions.
4. Sende den Changelog mit `DISCORD`.

Bei der Einrichtung lässt Enter den aktuellen Wert unverändert. `-` löscht
optionale Werte wie Avatar- oder Nachrichtenbild-URL. Die Webhook-URL ist
erforderlich. Der Setup-Befehl validiert sie und die aktivierten Rollen- und
Benutzer-IDs vor dem Speichern. Starte `DISCORDSETUP` erneut, um Konfigurationen
später anzupassen.

Die CLI liest dieselben Repository- und Webhook-Einstellungen wie die GUI. Bei
automatisierten Aufgaben muss sie deshalb unter demselben Betriebssystemkonto
und mit derselben Arma3Sync-Installation beziehungsweise demselben Datenordner
laufen, in dem die Einstellungen eingerichtet wurden. Nach einem neuen Build
kann `-discord` direkt aufgerufen werden. Die Option führt selbst keinen Build
und keinen Repository-Check aus.

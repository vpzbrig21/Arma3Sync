# Arma3Sync Changelog

## 2026.3.13-Beta – Unreleased

### Parallele FTP-/SFTP-Uploads

- FTP- und SFTP-Uploads unterstützen jetzt 1 bis 10 unabhängige Upload-
  Verbindungen. Der Standardwert ist 4.
- Die Upload-Anzahl ist unabhängig von den Download- und
  Repository-Prüfverbindungen und wird in der Repository-Konfiguration
  gespeichert.
- Verzeichnisse werden vor den parallelen Dateiübertragungen vorbereitet.
  Synchronisationsmetadaten und Löschungen folgen erst nach den
  Dateiübertragungen.
- Fortschritt und Geschwindigkeit werden über alle aktiven Upload-Sessions
  zusammengeführt.
- Ein paralleler Upload verwendet pro Worker eine eigene FTP-/SFTP-Session;
  Client-Sessions werden nicht zwischen Threads geteilt.

### Upload-Abbruch und Diagnose

- Die normale Bereinigung nach einem erfolgreichen Upload wird nicht mehr als
  Benutzerabbruch protokolliert.
- Echte Benutzerabbrüche werden nur einmal an den Upload-Sessions ausgeführt.
- Das Debug-Log protokolliert die konfigurierte und tatsächlich aktive Anzahl
  der Upload-Verbindungen.

### Kompatibilität

- Alte Repository-Konfigurationen bleiben kompatibel und verwenden für die
  neue Einstellung automatisch den Standardwert 4.
- HTTP/WEBDAV, HTTPS/WEBDAV und bestehende FTPS-Konfigurationen bleiben beim
  bisherigen seriellen Upload-Verhalten.

### Validierung

- `gradle clean test` erfolgreich
- `gradle build` erfolgreich

## 2026.3.12-Beta – Unreleased

### FTP-Upload-Performance

- FTP- und SFTP-Uploads können mit 1 bis 10 unabhängigen Verbindungen parallel
  ausgeführt werden. Der Standardwert beträgt 4 und ist unabhängig von den
  Download- und Repository-Prüfverbindungen.
- Verzeichnisse und Repository-Metadaten bleiben kontrolliert sequenziell;
  Dateiübertragungen werden parallel ausgeführt und der Gesamtfortschritt
  zusammengeführt.
- Die normale Bereinigung nach einem erfolgreichen Upload wird nicht mehr als
  Benutzerabbruch protokolliert.
- Die Prüfung vorhandener Remote-Dateien verwendet innerhalb einer FTP-
  Upload-Session gecachte Verzeichnislisten. Dadurch wird nicht mehr für
  jede Datei ein separater `MLST`-Roundtrip benötigt.
- Bekannte FTP-Verzeichnisse werden innerhalb der Session wiederverwendet;
  unnötige Wechsel in das Basisverzeichnis entfallen bei aufeinanderfolgenden
  Dateien im selben Verzeichnis.
- Beim Löschen wird eine zusätzliche Existenzprüfung vermieden. Dateien und
  Verzeichnisse werden direkt gelöscht; bereits entfernte Dateien werden dabei
  als erledigt behandelt.
- Die FTP-Kompatibilität bleibt erhalten: Wenn eine Verzeichnisliste nicht
  verfügbar ist, verwendet Arma3Sync weiterhin `MLST` und dessen bisherigen
  Listing-Fallback.

### Diagnose

- Das Debug-Log weist nun aus, wie viele Verzeichnislisten für eine Session
  geladen und wie viele Dateiprüfungen daraus bedient wurden.

### Validierung

- `gradle clean test` erfolgreich
- Bestehende FTP- und SFTP-Session-Mechanismen bleiben unverändert aktiv.

## 2026.3.11-Beta – Unreleased

### SFTP-Dateiübertragung

- Die SFTP-Schreibpufferung wurde auf 64 KB begrenzt. Dadurch bleibt die
  Netzwerkpufferung mit SFTP-Servern kompatibel, die kleinere Kanal- oder
  Fenstergrößen verwenden.
- Der lokale Dateilesepuffer bleibt bei 1 MB, sodass die Anpassung die lokale
  Datenträgerleistung nicht unnötig reduziert.
- Große PBO-Dateien werden nicht mehr wegen einer zu großen SFTP-
  Schreibpufferung vorzeitig mit `EOFException` abgebrochen.

### Validierung

- 67-MB-Datei mit der gebündelten Java-25-Runtime erfolgreich übertragen
- SFTP-Upload mit 64-KB-Netzwerkpuffer erfolgreich abgeschlossen
- `gradle clean test` erfolgreich

## 2026.3.10-Beta – Unreleased

### Gebündelte Java-Runtime und SFTP

- Die Windows-Standardruntime enthält jetzt zusätzlich `java.management` und
  `java.rmi`, die Apache MINA SSHD für den vollständigen SSH-Verbindungsaufbau
  benötigt.
- Dadurch bleibt der SFTP-Verbindungsaufbau auch mit der mitgelieferten
  reduzierten Java-25-Runtime funktionsfähig und bleibt nicht mehr im Pending-
  Zustand stehen.
- Die Diagnose- und SFTP-Anpassungen aus `2026.3.9-Beta` bleiben enthalten.
- Der Compact- und der Standard-Launcher verwenden weiterhin denselben
  Anwendungscode; die Änderung betrifft nur die gebündelte Standardruntime.

### Validierung

- Verbindung mit der gebündelten Java-25-Runtime getestet
- SSH-Verbindung erfolgreich hergestellt
- Passwortauthentifizierung erfolgreich durchgeführt
- SFTP-Subsystem erfolgreich geöffnet
- `gradle clean test` erfolgreich

## 2026.3.9-Beta – Unreleased

### SFTP-Verbindungsdiagnose und Stabilität

- Der SFTP-Endpunkt wird vor dem SSH-Aufbau explizit per DNS aufgelöst. Wenn
  mehrere Adressen vorhanden sind, wird IPv4 bevorzugt und die gewählte Adresse
  im Diagnose-Log vermerkt. Dadurch werden lange Wartezeiten durch eine nicht
  erreichbare IPv6-Route vermieden, ohne reine IPv6-Server auszuschließen.
- Der SSH-Verbindungsaufbau verwendet die aufgelöste Zieladresse direkt. Host,
  Port und der konfigurierte Remote-Pfad bleiben dabei unverändert.
- Das Debug-Log protokolliert nun zusätzlich die tatsächlich verwendete Java-
  Runtime, den geladenen Anwendungspfad, DNS-Ergebnisse und den Abschluss des
  SSH-Verbindungs-Futures. Passwörter werden weiterhin nicht protokolliert.
- Bei einem erfolgreichen Verbindungsaufbau wird der Ablauf klar bis zur
  Authentifizierung und zum SFTP-Subsystem nachvollziehbar. Fehler beim
  asynchronen SSH-Aufbau werden ebenfalls direkt erfasst.

### Kompatibilität

- Bestehende SFTP-Konfigurationen mit Hostnamen, benutzerdefinierten Ports wie
  `2022`, Passwortauthentifizierung und Remote-Pfaden bleiben kompatibel.
- FTP, FTPS, HTTP/WEBDAV und HTTPS/WEBDAV werden durch die Änderung nicht
  beeinflusst.

## 2026.3.8-Beta – Unreleased

### SFTP-Kompatibilität

- Apache MINA SSHD wurde auf `2.15.0` aktualisiert. Dadurch wird eine bekannte
  Regression aus `2.14.0` im SSH-Verbindungsaufbau behoben, die bei erreichbaren
  SFTP-Servern zu langen oder scheinbar hängenden Verbindungsversuchen führen
  konnte.
- Die bestehende SFTP-Konfiguration, benutzerdefinierte Ports wie `2022`,
  Passwortauthentifizierung und die persistente Upload-Sitzung bleiben
  kompatibel.
- Die vollständigen Hauptprogramm- und Updater-Tests sowie der Aufbau der
  Laufzeitdistribution wurden erfolgreich geprüft.

## 2026.3.7-Beta – Unreleased

### SFTP-Timeout und Reconnect

- Apache MINA SSHD wurde von `2.14.0` auf `2.15.0` aktualisiert. Damit wird eine
  bekannte Regression im SSH-Verbindungsaufbau der Version 2.14 behoben, die
  bei erreichbaren SFTP-Servern zu langen oder scheinbar hängenden
  Verbindungsversuchen führen konnte.
- Ein automatischer Reconnect startet Netzwerkoperationen nicht mehr direkt auf
  dem Swing-Event-Thread. Dadurch bleibt die Oberfläche während eines erneuten
  SFTP-Verbindungsversuchs bedienbar.
- Der Debug-Log schreibt bei SFTP-Fehlern nun die vollständige Exception samt
  Ursache und protokolliert lange ausstehende SSH-Verbindungen regelmäßig.
- Die Diagnose trennt weiterhin den SSH-Verbindungsaufbau klar von
  Authentifizierung, SFTP-Kanal, Remote-Pfad und eigentlichem Datei-Upload.

## 2026.3.6-Beta – Unreleased

### SFTP-Verbindungsaufbau und Diagnose

- Der SFTP-SSH-Verbindungsaufbau wird in kurzen Intervallen überwacht und
  blockiert den Upload-Worker nicht mehr ununterbrechbar bis zum vollständigen
  Socket-Timeout.
- Der Stop-Befehl bricht einen noch laufenden SSH-Verbindungsversuch aktiv ab;
  dadurch wird die Oberfläche nicht mehr minutenlang blockiert.
- SFTP-Verbindungsfehler und Timeouts werden als konkrete Fehlerursache im
  Diagnose-Log erfasst.
- Ein Upload beginnt weiterhin erst nach erfolgreichem SSH-/SFTP-Aufbau,
  Authentifizierung und Ermittlung des konfigurierten Remote-Verzeichnisses.

## 2026.3.5-Beta – Unreleased

### Diagnose und Support

- Ein optionaler Startparameter `-debug` aktiviert einen rotierenden
  Diagnose-Log im benutzerspezifischen Konfigurationsordner.
- Der Schalter kann mit GUI-, Konsolen-, Repository-Prüf- und Sync-Aufrufen
  kombiniert werden und verändert deren bestehende Parameterlogik nicht.
- Repository-Prüfungen und Uploads protokollieren jetzt ihre Phasen,
  Verbindungsaufbau, Authentifizierung, Remote-Pfade, Fortschrittsmarken,
  Dateigrößen, Laufzeiten, Abbrüche und Fehler.
- Passwörter werden nicht in den Diagnose-Log geschrieben; ohne `-debug` bleibt
  das zusätzliche Logging deaktiviert.

### SFTP upload

- SFTP connection setup now applies explicit connection, authentication,
  channel-opening and idle timeouts instead of allowing SSH operations to wait
  indefinitely.
- The Stop action now actively closes an in-progress SFTP session without
  blocking the Swing user interface.
- SFTP cancellation state is safely visible across the worker and UI threads.
- The upload progress display now reports that the SFTP connection is being
  established before remote files are checked. Size, upload speed and remaining
  time become available once the remote-file check has completed and file
  transfer begins.

## 2026.3.3-Beta – Unreleased

### Upload protocols

- SFTP is now the default upload protocol for new repository and event upload
  configurations.
- The selectable upload protocols are now SFTP, FTP, HTTP/WEBDAV and
  HTTPS/WEBDAV.
- FTPS remains implemented and existing FTPS configurations remain supported,
  but FTPS is temporarily hidden from new selections until TLS data-channel
  compatibility with FileZilla Server is finalized.
- Corrected SFTP host/path normalization for saved values such as
  `sftp:///host/path`; the configured port, including custom ports such as
  `2022`, is preserved.

### Validation

- Full Gradle test suite and updater tests pass.

## 2026.3.2-Beta – Unreleased

### Secure repository uploads

- Added FTPS as an upload protocol. Explicit TLS is negotiated before login,
  and private data-channel protection is enabled. This fixes compatibility with
  FTP servers that return `503 Use AUTH first` to plain FTP clients.
- Added SFTP upload support through Apache MINA SSHD with password
  authentication, standard port `22`, safe remote-path validation and one
  persistent SSH/SFTP session per repository upload.
- Added FTPS and SFTP to the repository and event upload dialogs while keeping
  existing FTP, HTTP, HTTPS, WebDAV and legacy repository formats compatible.

## 2026.3.1-Beta – Unreleased

### Performance: Repository operations

- Repository content checks now use a bounded pool of up to four independent
  connections instead of checking every remote file serially. Existing client
  connection settings remain respected up to this safety limit.
- Real connection errors cancel the other check workers; missing files remain
  reported individually as before.
- SHA-1 calculations now use a bounded worker pool of up to eight threads and
  merge results safely into the existing cache format.
- SHA-1 and remote-check progress updates are reduced to visible percentage
  changes, lowering unnecessary Swing event-queue traffic for large repositories.
- Repository size calculation no longer materializes the complete file tree in
  memory.
- Large local sync comparisons use hash-based lookups instead of repeated
  linear searches.
- Automatic repository checks are limited to four repositories at once to avoid
  uncontrolled server connection bursts.
- Repository content-check parallelism now has its own setting (default `4`),
  independent of the configured parallel download connections. Its safety
  limit is four concurrent connections.
- FTP repository uploads now reuse one authenticated session across remote
  existence checks, file transfers, metadata uploads and cleanup operations.
  The configured FTP base directory is restored before each operation.
- Repository upload settings now include FTPS and SFTP alongside FTP and
  WebDAV. FTPS performs explicit TLS negotiation before authentication and
  protects data channels, which is required by servers returning `503 Use AUTH
  first` for a plain FTP login. SFTP uses Apache MINA SSHD, password
  authentication, safe remote-path handling and one persistent upload session.

### Compatibility and scope

- Repository formats, legacy `a3s.xml`, protocol behavior and serialized cache
  structures remain unchanged.
- The central release version for this beta build is `2026.3.1`; the beta is
  not yet a final release.
- `.zsync` generation remains single-threaded for now because its parallel
  safety has not yet been fully validated.
- Parallel FTP uploads remain disabled until their ordering, directory
  creation, cancellation and progress behavior can be validated independently.

## 2026.2.6 – Patch Release

### Updater

- Der grafische Updatepfad startet den Updater wieder als sichtbaren
  Anwendungsprozess. Nach der Bestätigung eines Updates wird dadurch das
  Fortschrittsfenster angezeigt, statt den Vorgang ausschließlich unsichtbar
  im Konsolenmodus auszuführen.
- Der Kommandozeilenmodus bleibt unverändert und verwendet weiterhin den
  expliziten Konsolenpfad.
- Der Windows-GUI-Start verwendet `javaw.exe`, während Prüfungen und
  Kommandozeilen-Updates `java.exe` verwenden.
- Die Release-Artefakte und Update-Metadaten werden für diese Korrektur unter
  der neuen Version `2026.2.6` erzeugt.

## 2026.2.5 – Release

### Build- und Releasepflege

- Der gemeinsame native Windows-Bootstrapper wurde weiter verschlankt,
  ohne den Startablauf von Standard- oder Compact-Paketen zu verändern.
- GitHub Releases sind für neue Installationen nun die bevorzugte Updatequelle.
  Arma3Sync übergibt für jeden Updatevorgang explizit `-github` oder
  `-manifest`; eine ausdrückliche Deaktivierung in `updater.toml` bleibt
  vorrangig und JSON/XML bleiben als Fallback erhalten.
- Standard- und Compact-Artefakte, Installer und Update-Metadaten wurden auf
  die zentrale Version `2026.2.5` neu erzeugt.
- Die Release-Notes und Updater-Beispiele verwenden jetzt die tatsächlichen
  Dateinamen `Arma3Sync-2026.2.5.zip` und `Arma3Sync-2026.2.5-compact.zip`.
- Die vollständige Änderungsübersicht der Modernisierung von `1.7.107` ist
  weiterhin im Abschnitt `2026.2.1` dokumentiert; dieser Abschnitt bleibt als
  historische Referenz unverändert.

## 2026.2.1 – Release

### Packaging und Java-Laufzeit

- Das Windows-Standardpaket verwendet jetzt denselben nativen
  Bootstrapper und dasselbe Root-JAR wie die Compact-Variante.
- Standard-Releases bündeln eine mit `jlink` aus JDK 25 erzeugte, reduzierte
  Java-Laufzeit unter `runtime\` und benötigen keine separate Java-Installation.
- Der Bootstrapper bevorzugt `runtime\bin\javaw.exe` und verhindert dadurch,
  dass eine ältere Java-8-Installation aus `PATH` oder den Oracle-Verknüpfungen
  verwendet wird.
- Der gemeinsame native Bootstrapper wurde intern verschlankt, indem nicht
  benötigte Parser-Abhängigkeiten und ungenutzte Diagnoseinformationen entfernt
  wurden. Standard- und Compact-Verhalten, Java-25-Prüfung und Runtime-Auswahl
  bleiben dabei identisch.
- Der bisherige separate `jpackage`-Anwendungslauncher mit eigenem `app\`-JAR
  wird für Standard-Releases nicht mehr verwendet. Dadurch aktualisiert der
  Updater genau das JAR, das der Launcher startet.
- Dokumentation in `README.md`, `BUILD.md`, `release/README.md` und
  `installer/nsis/README.md` beschreibt die Unterschiede zwischen Standard und
  Compact sowie die Prüfungen für die gebündelte Runtime.

### Neue Funktionen und Korrekturen

- Export ausgewählter Modsets als kompatibles HTML-Preset für den Arma-3-
  Launcher einschließlich Workshop-Mods und CDLCs.
- Abfrage fehlender Workshop-Metadaten mit lokaler Speicherung der ergänzten
  Workshop-ID und des Anzeigenamens.
- Der Sync-Baum eines Repositorys wird bei jeder Prüfung frisch geladen, damit
  entfernte Dateien nicht aus veralteten lokalen Metadaten angefordert werden.
- HTTP-Metadatenabrufe umgehen veraltete Cacheantworten.
- Legacy-Java-Deserialisierung akzeptiert beim Lesen nur noch bekannte
  Arma3Sync-Domänen- und notwendige Standardklassen und begrenzt Tiefe,
  Referenzen und Eingabegröße.
- ZIP-Extraktion validiert Einträge jetzt gegen das Zielverzeichnis und weist
  Pfad-Traversal sowie Ausweichpfade über vorhandene Symlinks zurück.
- Der Updater protokolliert die von ihm verwalteten Programmdateien und entfernt
  bei Folgeupdates daraus entfallene Dateien, ohne Benutzerkonfigurationen,
  Profile oder unbekannte Dateien zu löschen.
- Der Abbrechen-Pfad beendet den SwingWorker und die aktive HTTP-/FTP-
  Übertragung, bevor temporäre Update-Daten bereinigt werden.
- Repository-Builds behalten die letzte gültige `.a3s`-Metadatenbasis, bis der
  neue Scan abgeschlossen ist; ein abgebrochener Build lässt das Repository
  dadurch nicht ohne Synchronisationsdaten zurück.
- Verwaltungsdateien werden zunächst vollständig in temporäre Dateien
  geschrieben und anschließend atomar ersetzt, damit Teilinhalte nach einem
  Prozessabbruch oder Stromausfall nicht als gültige Metadaten erscheinen.
- Addon-Pfade werden bei der lokalen Zuordnung normalisiert und nur noch mit
  echter Verzeichnisgrenze verglichen; ähnlich benannte Ordner werden nicht
  mehr durch Teilzeichenketten verwechselt.
- Parallele Download- und Completion-Prozesse schützen gemeinsam genutzte
  Fehlerlisten, Abschlusszustände und Semaphore; die Abbruchgrenze greift jetzt
  exakt beim zehnten Fehler.
- Eingabe- und Zustandsprüfungen verwenden keine deaktivierbaren Java-
  `assert`-Anweisungen mehr, sondern explizite Rückgaben oder aussagekräftige
  Laufzeitfehler.
- Fortschritts-, Status- und Abschlussmeldungen der Repository-Worker werden
  über den Swing Event Dispatch Thread an die Oberfläche zugestellt.
- Der gemeinsame Abbruchstatus der UI-Worker ist sichtbar zwischen Threads,
  damit nach einem Abbruch keine verspäteten Fortschrittsmeldungen den Zustand
  wieder überschreiben.
- Aktive Release-, Updater- und Legacy-Metadaten wurden auf `2026.2.1`
  ausgerichtet; historische Angaben zu `2026.1.1` bleiben als Referenz erhalten.
- Erzwungene `System.gc()`-Aufrufe wurden aus aktiven Build-, Sync- und UI-Pfaden
  entfernt. Konsolenausgaben bleiben dort erhalten, wo sie CLI-Fortschritt,
  Diagnose oder den exportierbaren Synchronisationsbericht liefern.
- Die Repository-Gesamtgröße zählt nun den vollständigen Repository-Inhalt ohne
  `.a3s`-Verwaltungsdaten; sie ist ausdrücklich nicht die Update-Deltagröße.
- Die Repository-Revision bleibt bei unverändertem Pfad-, Größen- und SHA-1-Inhalt
  stabil. Ein neuer Changelog-Eintrag entsteht nur noch bei echten Inhaltsänderungen.
- Ausgewählte lokale Dateipfade im Repository-Build und bei Metadaten werden
  plattformgerechter über `Path.resolve()` gebildet.
- Der JSON-Parser des Updaters validiert Zahlen, ganzzahlige Größen, Versionsformate,
  HTTP(S)-Download-URLs und doppelte JSON-Schlüssel nun strikt.
- Der HTML-Export schreibt über eine temporäre Datei mit atomarem Ersetzen, soweit
  vom Dateisystem unterstützt, und prüft das erzeugte Dokument anschließend auf
  Vollständigkeit. Für Windows-Dateisysteme, die atomisches Ersetzen einer bereits
  vorhandenen Zieldatei ablehnen, gibt es einen kompatiblen Ersetzungs-Fallback;
  bestehende Presets können dadurch zuverlässig überschrieben werden.
- Nicht-positive oder ungültige Workshop-IDs in `meta.cpp` werden beim HTML-Export
  nun als fehlende Metadaten behandelt. Dadurch erscheint der Ergänzungsdialog auch
  bei Einträgen wie `publishedid = 0`, statt dass der Export ohne verwertbares
  Ergebnis abbricht.
- Der Metadaten-Scan läuft nun direkt im Export-Event. Die Abfrage fehlender
  Metadaten wird unmittelbar nach dem Scan und vor der Auswahl des Speicherpfads
  geöffnet, ohne Hintergrund-Callback eines SwingWorkers und ohne verschachtelten
  Fortschrittsdialog. Der Speicherdialog erscheint erst nach erfolgreicher
  Vervollständigung und erneuter Prüfung aller kritischen Angaben.
- Status-, Fehler- und Metadatenabfragen des Exports verwenden nun die standardisierte
  Swing-Dialog-API mit dem sichtbaren Addons-Panel als Parent. Dadurch können die
  Dialoge nicht mehr unbemerkt hinter dem Hauptfenster erzeugt werden.
- Der Exportablauf wird ohne verzögerten Zwischen-Callback direkt ausgeführt und
  protokolliert seine Phasen zusätzlich in `launcher-preset-export.log`. Dadurch
  lässt sich unterscheiden, ob der Button, die Modset-Auswahl, der Metadaten-Dialog,
  der Speicherdialog oder das Schreiben der Datei erreicht wurde.
- Ein Fehler in den globalen Theme-Defaults wurde behoben: `OptionPane`- und
  Tabellen-Randeinstellungen wurden fälschlich als `Insets` statt als Swing-
  `Border` registriert. Dadurch konnten Metadaten-, Fehler- und Erfolgsdialoge
  vor dem Anzeigen mit einer `ClassCastException` abbrechen.
- Die lokale Workshop-Metadatenbank verwendet einen Seitenlock sowie atomaren
  Dateiersatz, damit parallele Exporte keine Einträge verlieren.
- Deaktivierte HTTPS-Zertifikatsprüfung ist standardmäßig nur für lokale Testziele
  zulässig. Für entfernte Entwicklungsziele ist der explizite Startparameter
  `-Da3s.allowInsecureSsl=true` erforderlich; die Oberfläche warnt entsprechend.
- Die externe Google-Fonts-Referenz des Launcher-HTML bleibt aus
  Kompatibilitätsgründen unverändert.
- Updater, NSIS-Definitionen und Release-Skripte liegen jetzt innerhalb der
  Hauptrepository unter `updater/`, `installer/nsis/` und `release/` und werden
  gemeinsam versioniert.
- Der Updater bleibt ein unabhängiges Gradle-Subprojekt, wird aber über
  `settings.gradle` in den Repository-Build und die Tests einbezogen.
- Der Releaseablauf baut Standard- und Compact-Installer aus den integrierten
  Quellen und bricht bei fehlenden oder leeren erwarteten Artefakten ab.
- Das Standardpaket ist jetzt der reguläre Windows-Release ohne Dateisuffix und
  enthält die reduzierte Java-25-Runtime. Das optionale Compactpaket trägt den
  Suffix `-compact` und setzt eine vorhandene Java-25+-Laufzeit voraus.
- Die früher verwendeten `a3s-standalone.*`- und `*-standalone*`-Namen werden
  nicht mehr erzeugt; veraltete Artefakte derselben Version werden beim
  Release-Neubau entfernt.
- Der Updater installiert weiterhin alle Dateien aus dem geprüften Release-
  Archiv, führt neue und geänderte Dateien zusammen und entfernt Dateien aus
  vorherigen Updater-Läufen, wenn sie im neuen Archiv nicht mehr enthalten sind.
  Benutzerdateien, Profile, Repositorydefinitionen und die persönliche
  `updater.toml` bleiben geschützt.
- Der Updater behandelt Dateiänderungen ohne Beachtung der Groß-/Kleinschreibung
  unter Windows korrekt. Dadurch wird die umbenannte `Arma3Sync.jar` beim Update
  nicht versehentlich als alte Datei entfernt.
- `release.ps1 -SkipBuild` kann einen bereits getesteten Build verpacken,
  ohne die Anwendung erneut zu kompilieren.
- Die zentrale `version.properties` steuert Anwendung, Updater, NSIS und
  Release-Metadaten; externe Arbeitskopien werden für Releases nicht mehr
  benötigt.
- Das kompakte Legacy-Layout mit `a3s.cfg`/`a3s.prefs` neben dem JAR und
  Repositorys unter `resources/ftp` wird wieder vollständig verwendet.
- Der sichere Legacy-Deserialisierungsfilter akzeptiert die für bestehende
  Arma3Sync-Metadaten benötigten Objekt-Arrays und `Map.Entry`-Container.
- Repository-Prüfungen fragen keine Verzeichnis-URLs mehr ab; HTTP-Server mit
  korrektem `403 Forbidden` auf Verzeichnisse blockieren die Prüfung dadurch
  nicht mehr.
- HTTPS-Auto-Config-Importe verwenden wieder die reguläre Zertifikatsprüfung.
  Dadurch können entfernte Repositorys wie `https://.../.a3s/autoconfig` importiert
  werden, ohne die eingeschränkte unsichere Zertifikatsausnahme zu aktivieren.
- Die Auto-Config-Protokollerkennung akzeptiert Protokolle nur noch am Anfang der
  URL und unterscheidet HTTPS ausdrücklich von HTTP.
- Beim Schließen des Hauptfensters werden laufende Repository-Worker zunächst
  abgebrochen, bevor die Anwendung beendet wird.
- Repositorydefinitionen werden beim Speichern nun innerhalb der Anwendung
  synchronisiert und atomar ersetzt. Die bisherige gültige Datei bleibt bis zum
  erfolgreichen Abschluss erhalten; dadurch führen parallele Schreibvorgänge
  oder ein Abbruch nicht mehr zu einer leeren Repositoryliste beim nächsten Start.

## 2026.1.1 – Release

Diese Version ist die moderne Weiterentwicklung der originalen
Arma3Sync-Version `1.7.107`. Der bisherige technische Stand `1.9.260` wird
mit diesem Release unter dem neuen, kalenderbasierten Versionsschema als
`2026.1.1` veröffentlicht.

### Anwendung und Build

- Migration auf Java 25 und einen reproduzierbaren Gradle-Build.
- Zentrale Versionsdefinition in `version.properties`.
- Start über das Root-JAR `Arma3Sync.jar` wie bei der kompakten Originalvariante.
- Weiterhin startbar über `java -jar Arma3Sync.jar` unter Linux und Unix.
- Aktualisierte Projekt-, Build- und Testdokumentation.
- Unbenutzte Schriftressourcen aus dem Anwendungspaket entfernt.

### Plattform und Kompatibilität

- Plattformgerechte Speicherorte für Konfiguration, Profile und Cache.
- Bestehende Profile und Repositorydefinitionen bleiben lesbar.
- Nicht-destruktive Migration aus dem früheren Installationslayout.
- Benutzerdateien werden bei Aktualisierungen nicht ungewollt überschrieben.
- HTTPS-Unterstützung für Repositoryverbindungen bleibt erhalten.
- Die Option zum Starten von ArmA 3 mit Administratorrechten unter Windows
  bleibt pro Profil erhalten.
- Bestehende DLC-/CDLC-Strukturen und Legacy-Formate bleiben kompatibel.

### Update-Integration

- Der Updateablauf verwendet weiterhin die vorhandene XML-Kompatibilität und
  kann eine externe Aktualisierungskomponente aus der Anwendung heraus starten.
- Installationspfad, aktuelle Version und benutzerbezogene Daten werden an
  den Updateprozess übergeben, damit Updates unabhängig vom Arbeitsordner
  funktionieren.
- Der XML-basierte Bestand bleibt für bestehende Installationen kompatibel.

### Sicherheit und Robustheit

- Sichere Normalisierung von Installations- und temporären Pfaden.
- Keine dauerhaften Schreibrechte im Installationsordner für Benutzerdaten
  erforderlich.
- HTTP-Weiterleitungen von HTTPS zurück auf HTTP werden abgelehnt.
- Die Repository-Dateiliste (`.a3s/sync`) wird bei jeder Prüfung frisch geladen;
  dadurch werden entfernte Dateien nicht mehr aus einem veralteten lokalen
  Manifest angefordert.
- Repository-, Datei- und Aktualisierungslogik bleiben von der UI getrennt.

### Benutzeroberfläche

- Modernisierte Light- und Dark-Themes mit zentralen Farb- und Layoutwerten.
- Kompaktere Kopfzeile und abgestimmte Titelzeilen-Icons.
- Dauerhafte Trennlinie zwischen „Available Addons“ und „Addon Groups“.
- Geschlossene und geöffnete gelbe Ordner-Icons in den Addon-Bäumen.
- Status-Icons für geänderte, gelöschte oder als Addon markierte Verzeichnisse.
- Vereinheitlichte Fortschrittsbalken mit kontrastreicher Prozentanzeige.
- Verbesserte Abstände, Rahmen und Kontraste in beiden Themes.

### Repository- und Launcher-Funktionen

- Bestehende Repositoryverwaltung, Addon-Synchronisierung, Profilverwaltung
  und ArmA-3-Startparameter bleiben erhalten.
- Direkte Unterstützung der vorhandenen HTTP-, HTTPS-, FTP- und WebDAV-
  Verbindungsarten bleibt erhalten.
- Der Windows-Start unterstützt die moderne kompakte JAR-Struktur ohne eine
  verpflichtend gebündelte Java-Runtime.

### Referenz

- Originale Vergleichsversion: `1.7.107`.
- Technischer Vorrelease: `1.9.260`.
- Finaler Release: `2026.1.1`.

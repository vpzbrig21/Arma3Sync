# Arma3Sync Changelog

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

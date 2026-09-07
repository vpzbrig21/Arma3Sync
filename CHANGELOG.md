# Arma3Sync Changelog

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

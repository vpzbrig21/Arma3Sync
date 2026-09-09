# Arma3Sync Änderungsverlauf

Die versionsbezogenen Release-Changelogs liegen unter
[`changelogs/`](changelogs/). Dort werden ausschließlich veröffentlichte
Versionen geführt. Die jeweilige Datei wird beim Release-Build geprüft und als
`RELEASE_NOTES.md` in das Release-Archiv übernommen.

## 2026.3.14 — Veröffentlicht

- FTP- und SFTP-Repository-Uploads unterstützen 1 bis 10 parallele
  Dateiübertragungen; Standardwert ist 4 und wird pro Repository gespeichert.
- Upload-Verbindungen sind unabhängig von Prüf- und Download-Verbindungen.
- FTP-Sitzungen verwenden Verzeichnislisten und Dateiprüfungen innerhalb eines
  Uploads wieder, wodurch unnötige Roundtrips entfallen.
- Verzeichnisse werden vor den Dateiübertragungen vorbereitet. Metadaten und
  Löschvorgänge folgen erst nach erfolgreichen Uploads.
- Fortschritt, Geschwindigkeit und Restzeit werden über alle Upload-Sitzungen
  zusammengeführt.
- SFTP unterstützt benutzerdefinierte Ports, Passwortauthentifizierung,
  Remote-Pfade, kontrollierte Reconnects und sicheren Abbruch.
- Begrenzte SFTP-Netzwerkpuffer verbessern die Kompatibilität mit kleineren
  SSH-Kanalfenstern.
- Das optionale `-debug`-Argument erzeugt ein rotierendes Diagnose-Log ohne
  Passwörter.
- Der Standard-Installer installiert die mitgelieferte Java-25-Runtime korrekt
  unter `runtime/`; eine separate Java-Installation ist nicht nötig.
- ZIP und Installer verwenden denselben Launcher, dasselbe Root-JAR und
  dieselbe Runtime-Struktur.
- Bestehende Repositorys, Profile und das alte `a3s.xml`-Format bleiben
  kompatibel.

## 2026.2.6 — Veröffentlicht

- Der grafische Updatepfad startet den Updater wieder ohne Konsolenmodus,
  sodass Fenster und Fortschritt sichtbar sind.
- Kommandozeilenoperationen behalten den expliziten Konsolenmodus.
- Grafische Windows-Updates verwenden `javaw.exe`; Prüfungen und
  Konsolenoperationen verwenden `java.exe`.
- Die Standardvariante enthält weiterhin die reduzierte Java-25-Runtime, die
  Compact-Variante benötigt Java 25 oder neuer.

## 2026.2.5 — Veröffentlicht

- Export ausgewählter Workshop-Modsets als gültige HTML-Presets für den
  offiziellen Arma-3-Launcher.
- Workshop-ID und Anzeigename werden aus `meta.cpp` gelesen; Creator-DLCs
  werden zentral verwaltet.
- Lokaler AppData-Metadatenspeicher für bekannte Workshop-Mods und fehlende
  Metadaten.
- Legacy-`a3s.xml`, Profile und bestehende Repositorys bleiben kompatibel.
- Updater-Archive werden per SHA-256 geprüft; ZIP-Extraktion und
  Java-Deserialisierung sind abgesichert.
- Repository-Builds behalten gültige `.a3s`-Metadaten bis zum Abschluss eines
  neuen Scans; Metadaten werden atomar ersetzt.
- Stabile Download-, Abbruch- und Swing-Thread-Verarbeitung sowie genaues
  Fehlerlimit von zehn Fehlern.
- Unveränderte Repository-Inhalte erhöhen die Revision nicht.
- HTML-Export, Metadaten-Dialog, Cache-Sperre und atomare Ausgabedatei wurden
  robust umgesetzt.
- GitHub Releases können als Updatequelle verwendet werden.
- Das Standardpaket bringt eine reduzierte Java-25-Runtime mit; Compact
  benötigt Java 25 oder neuer.

## 2026.1.1 — Veröffentlicht

- Moderne Fortführung von Arma3Sync `1.7.107` mit zentraler Versionierung,
  Gradle-Builds und aktualisierter Java-25-Basis.
- Neuer Updater mit JSON-Manifesten, SHA-256-Prüfung und optionaler
  GitHub-Releases-Unterstützung.
- Bewährte Repository-, Profil- und Launcher-Funktionen sowie das alte
  `a3s.xml`-Format bleiben erhalten.
- Sicherere Installation, temporäre Pfade und ZIP-Extraktion.
- Compact- und Standardpaket für Windows sowie plattformübergreifender
  JAR-Start.

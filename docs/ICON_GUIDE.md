# Icon Architecture

## Ziele
- Alle UI-Icons sind SVG-basiert und liegen unter `src/main/resources/resources/icons`.
- Icons werden ausschließlich über den Service `fr.soe.a3s.ui.icon.Icons` geladen, der Theme-abhängige Farben, Größen und Zustände zentral verwaltet.
- Die Symbolpalette ist über das Enum `fr.soe.a3s.ui.icon.UiIcon` benannt; jede Konstante verweist auf genau eine SVG-Datei und definiert, ob das Icon getönt werden darf (`themeAware`) sowie welchen Grund-Ton (`IconTone`) es hat.
- `IconState` beschreibt die Zustände `NORMAL`, `DISABLED` und `SELECTED`. Der Service wandelt diese in die richtigen Farben (z. B. Disabled = `UiColors.textDisabled()`).

## Wichtige Klassen
| Datei | Aufgabe |
| --- | --- |
| `src/main/java/fr/soe/a3s/ui/icon/Icons.java` | Liefert `Icon` oder `Image` Objekte, cacht sie pro Theme/Größe/Zustand und invalidiert automatisch bei Theme-Wechsel (siehe `ThemeManager`). |
| `src/main/java/fr/soe/a3s/ui/icon/UiIcon.java` | Einheitliche Icon-Tokens inkl. Dateiname, Theme-Awareness und Tonwert. |
| `src/main/java/fr/soe/a3s/ui/icon/IconState.java` | Definiert visuelle Zustände (normal, disabled, selected). |
| `src/main/java/fr/soe/a3s/ui/icon/IconTone.java` | Legt fest, welche Theme-Farbe in `themeAware`-Icons verwendet wird (z. B. `ACCENT`, `SUCCESS`). |
| `src/main/java/fr/soe/a3s/ui/UIConstants.java` | Bezieht App-, Tray- und Splash-Icons über den Icon-Service statt eigener Loader. |

## Verwendung im Code
```
import fr.soe.a3s.ui.icon.Icons;
import fr.soe.a3s.ui.icon.UiIcon;
import fr.soe.a3s.ui.icon.IconState;

JButton button = new JButton("Download");
button.setIcon(Icons.icon(UiIcon.DOWNLOAD, 18));
button.setDisabledIcon(Icons.icon(UiIcon.DOWNLOAD, 18, IconState.DISABLED));
```
- Icons werden immer über `Icons.icon(token, size)` gezogen; direkte Pfade (`new ImageIcon("/foo.png")`) sind nicht mehr erlaubt.
- Für Roh-Bilder (z. B. Fenster- oder Tray-Icons) steht `Icons.image(token, size)` zur Verfügung.

## Neues Icon hinzufügen
1. SVG im Format 24×24 in `src/main/resources/resources/icons` speichern. Benennung: kebab-case (`upload-cloud.svg`).
2. Konstante in `UiIcon` ergänzen und auf die neue Datei verweisen:
   ```java
   UPLOAD_CLOUD("upload-cloud", true, IconTone.ACCENT)
   ```
   - `themeAware = true`, wenn das Icon einfärbbar ist (Outline/Monochrom).
   - `IconTone` bestimmt, welcher Theme-Farbwert angewendet wird (siehe `UiColors`).
3. Komponente auf das neue Icon umstellen (`Icons.icon(UiIcon.UPLOAD_CLOUD, 16)`).

## Projekt-spezifische Icons
- Brand-/Sponsor-Logos oder farbige Mehrfarb-Icons können mit `themeAware = false` hinterlegt werden. Der Service liefert dann das unveränderte SVG.
- Falls Light/Dark-Varianten benötigt werden, sollte das SVG als Outline aufgebaut und `themeAware = true` gesetzt werden, damit `UiColors` automatisch für beide Themes korrekt einfärbt.

## Zustände & Skalierung
- Größen werden in Logik-Pixeln angegeben. Der Service skaliert bis zu 64 px automatisch mit dem Faktor `a3s.iconScale` (System-Property, Standard 1.5), damit HiDPI/LoDPI konsistent bleiben.
- `IconState.DISABLED` nutzt `UiColors.textDisabled()`, `IconState.SELECTED` `UiColors.selectionForeground()`. Weitere Töne (z. B. Hover) können später ergänzt werden, ohne die Aufrufer anzupassen.

## Migration & Review
- Die alte Klasse `IconFactory` sowie alle direkten `ImageIcon`-Aufrufe wurden entfernt. Jede UI-Komponente nutzt jetzt `Icons.icon(...)` oder `UIConstants.*`.
- SVGs mit veralteten oder duplizierten Namen (`*.bak`) sind markiert und können bereinigt werden, sobald keine Alt-Dialoge mehr referenzieren.
- Beim Theme-Wechsel ruft `ThemeManager.toggleTheme()` automatisch `Icons.invalidate()`, damit gecachte Bitmaps neu getönt werden.

## Tests / Checks
- `gradle clean build` kompiliert aktuell nicht im Sandbox-Container, weil kein lokales Gradle installiert ist. Auf einem Entwickler-System mit Gradle 8.6+ sollten die Änderungen ohne weitere Schritte durchlaufen.

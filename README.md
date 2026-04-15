# Projektnotizen und Setup

Dieses Repository dient als Arbeitsgrundlage fuer die Geoinformatik-/Kartenanwendung.

## Entwicklungsumgebung (Dev Container)

Die Dev-Container-Konfiguration ist bewusst schlank gehalten:

- Basis: Ubuntu 24.04
- Laufzeit: Java 17 (Temurin)
- Sprache: Groovy
- Framework: Grails, mit Kompatibilitaet zu Tomcat 9
- Datenbank: externe PostgreSQL-Verbindung per Umgebungsvariablen

### Dev Container in VS Code verwenden

1. Docker Desktop/Engine starten.
2. Dieses Repository in VS Code oeffnen.
3. Command Palette oeffnen (`Ctrl+Shift+P`).
4. `Dev Containers: Reopen in Container` ausfuehren.
5. Warten, bis das Image gebaut und der Container gestartet ist.
6. Terminal im Container oeffnen und Build/Run-Befehle dort ausfuehren.

Hinweis:

- Bei Aenderungen an der Containerkonfiguration: `Dev Containers: Rebuild and Reopen in Container`.
- VS Code-Erweiterungen fuer den Container werden automatisch vorgeschlagen/installiert.

### Hinweise fuer WSL und macOS

#### Windows mit WSL2

1. WSL2 installieren (z. B. Ubuntu 24.04).
2. Docker Desktop installieren und WSL-Integration aktivieren.
3. Repository im WSL-Dateisystem ablegen (z. B. unter `~/projects/...`, nicht unter `/mnt/c/...`).
4. In VS Code den WSL-Ordner oeffnen (Remote - WSL) und dann `Dev Containers: Reopen in Container` nutzen.

Warum so?

- Bessere Dateisystem-Performance als im gemounteten Windows-Pfad.
- Stabilere Toolchain mit Docker + Dev Containers.

#### macOS

1. Docker Desktop fuer macOS installieren und starten.
2. VS Code mit Erweiterung Dev Containers verwenden.
3. Repository lokal klonen und in VS Code oeffnen.
4. Danach `Dev Containers: Reopen in Container` ausfuehren.

Hinweis fuer Apple Silicon (M1/M2/M3):

- Falls einzelne Images/Tools nur fuer `amd64` verfuegbar sind, ggf. Plattform explizit auf `linux/amd64` setzen.
- Standardmaessig zuerst die native Ausfuehrung nutzen.

### Erwartete PostgreSQL-Variablen

- `DB_HOST`
- `DB_PORT` (Standard: `5432`)
- `DB_NAME`
- `DB_USER`
- `DB_PASSWORD`

Die Werte vor dem Starten des Containers in lokaler Shell oder VS Code Dev-Container-Umgebung setzen, damit keine sensiblen Daten ins Repository gelangen.

## Organisatorische Notizen

- Es gibt eine Agilogik-VM mit Ubuntu 24.04 fuer die Applikation.
- Ein zentrales Repository-Verzeichnis auf der VM wird in den naechsten zwei Wochen eingerichtet.
- Regelmaessig einchecken/committen, damit die Versionierung nutzbar bleibt.
- Entwicklungsansatz: iterativ und inkrementell (Anforderungen werden im Projektverlauf geschaerft; kein starres V-Modell).
- Aus dem Ideen-Portfolio eine Auswahl treffen und in der naechsten Woche rueckspiegeln.

## Git-Setup und Zugriff

### Repository klonen

`git clone ssh://<USERNAME>@iu.servicecluster.de:11422/media/sf_iu/git/elgreti_dsbadpsii01.git`

### Git-Identitaet lokal setzen

Im lokalen Repository-Ordner ausfuehren:

- `git config user.email "<USERNAME>@iu-study.org"`
- `git config user.name "<USERNAME>"`

### SSH-Key erzeugen

Auf dem Client im Home-Verzeichnis (`~`) ausfuehren:

- `ssh-keygen`

### Public Key auf den Server kopieren

- `ssh-copy-id -i ~/.ssh/<KEY_NAME>.pub -p 11422 <USERNAME>@iu.servicecluster.de`

Wenn der Key korrekt hinterlegt ist, sollte bei SSH-Login oder `git push` keine Passwortabfrage mehr kommen.

## Fachlicher Kontext (GIS/OSM)

- GIS = Geoinformationssysteme.
- OSM/OpenStreetMap als Datenbasis.
- Geoinformatik = Verortung von Objekten (z. B. POIs, Adressen, Distanzen, Routen).
- Kartendarstellung i. d. R. in Merkatorprojektion und in Zoomstufen.
- Kachelgroesse: `256x256` Pixel (PNG).
- Kein Vektorrendering auf Clientseite; Nutzung des bereits implementierten Kachel-Renderers.

## Relevante Bausteine und Dienste

### Leaflet (Karte im Client)

- Website: https://leafletjs.com/
- Repository: https://github.com/Leaflet/Leaflet
- Aufgabe: Einarbeitung in Kartendarstellung, Layer, Verbindungslinien, Grafiken, Schattierungen.
- Ziel: Daten in die sichtbare Karte programmieren (Leaflet-Integration).

### Nominatim (Geocoding)

- Website: https://nominatim.org/
- Service: https://nominatim.servicecluster.de
- Endpunkte:
	- `/search` fuer Geocoding (Adresse -> Koordinaten)
	- `/reverse` fuer Reverse-Geocoding (Koordinaten -> Adresse)
- Bis naechste Woche mit API vertraut machen.

Beispiele:

- Suche:
	- https://nominatim.servicecluster.de/search?q=Steingaden+Welfenstr+14&limit=5&format=json&addressdetails=1
	- https://nominatim.servicecluster.de/search?q=Frankfurt+Darmst%C3%A4dter%20Landstra%C3%9Fe+21&format=json
- Reverse:
	- https://nominatim.servicecluster.de/reverse?format=json&lat=47.7003827&lon=10.8616748
	- https://nominatim.servicecluster.de/reverse?format=json&lat=50.1021338&lon=8.6911658

### OSRM (Routing)

- Website: https://project-osrm.org/
- Service: http://osrm.servicecluster.de/
- Fokus: Fahrstrassenrouting.
- Aufgabe: Bis zum naechsten Termin aktiv mit OSRM-Anfragen arbeiten.

Beispiel:

- https://osrm.servicecluster.de/route/v1/driving/11.603602,48.11.6153552;8.6911658,50.1021338?overview

### Tile-Server

- Host/IP: `65.108.103.22`
- Demo: https://gis.servicecluster.de/demaps.html
- Tile-URL:
	- https://gis.servicecluster.de/de_tiles/{z}/{x}/{y}.png
	- Beispiel: https://gis.servicecluster.de/de_tiles/0/0/0.png
- Style-Hinweis:
	- `de_tiles` liefert DE-Style (nicht OSM Bright).
- Achsen:
	- `z` = Zoomstufe (praktisch meist `0-20`)
	- `x` = horizontale Kachelkoordinate
	- `y` = vertikale Kachelkoordinate

Kachelanzahl je Zoomstufe:

- Pro Achse: `2^z`
- Gesamtzahl Kacheln: `4^z`
- `z=0`: `1x1` = 1 Kachel
- `z=1`: `2x2` = 4 Kacheln
- `z=2`: `4x4` = 16 Kacheln

## Demo-System

- https://agilogikmap.servicecluster.de/
- Themen: Verortung, Wegdistanzen, Geocoding/Reverse-Geocoding.

## Begriffe

- Verortung: Positionierung von Objekten auf der Karte (Adresse oder `lat/lng`).
- Geocoding: Adresse -> `lat/lng`.
- Reverse-Geocoding: `lat/lng` -> Adresse.

## Arbeitsauftraege bis zum naechsten Termin

- Leaflet vertieft einarbeiten.
- Nominatim-API praktisch testen (`/search`, `/reverse`).
- OSRM-Endpunkte testen und typische Request/Response-Muster dokumentieren.
- Aus dem Ideen-Portfolio konkrete Vorschlaege vorbereiten.

## Quellen und Dokumentation

Quellenangaben sinnvoll in schriftlichen Ausarbeitungen fuehren (nicht zwingend im Handout, aber in den abgegebenen Aufgaben).

## Hinweis zu Tomcat 9

Tomcat 9 ist mit Java 17 in dieser Konfiguration kompatibel. Die konkrete Grails-/Tomcat-Version wird projektspezifisch im Code/Build definiert.

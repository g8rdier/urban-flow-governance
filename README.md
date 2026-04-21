# Urban Flow Governance

Webbasierte Governance-Anwendung zur Verwaltung und Visualisierung von urbanen Verkehrs- und Geodaten. Gebaut mit Grails/Groovy auf Java 17, mit GIS-Integration via Leaflet, Nominatim und OSRM.

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

### App starten

Im Container-Terminal:

```bash
cd ~/ufg/ufg-app
grails run-app
```

Die App ist danach unter `http://localhost:8080` erreichbar.

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

Die Werte vor dem Starten des Containers in lokaler Shell oder `.env`-Datei setzen, damit keine sensiblen Daten ins Repository gelangen.

## Git-Setup und Zugriff

### Repository klonen

```bash
git clone ssh://<USERNAME>@iu-server/media/sf_iu/git/elgreti/urban-flow-governance.git
```

### Git-Identitaet lokal setzen

Im lokalen Repository-Ordner ausfuehren:

```bash
git config user.email "<USERNAME>@iu-study.org"
git config user.name "<Vorname Nachname>"
```

### SSH-Key erzeugen

```bash
ssh-keygen
```

### Public Key auf den Server kopieren

```bash
ssh-copy-id -i ~/.ssh/<KEY_NAME>.pub <USERNAME>@iu-server
```

Wenn der Key korrekt hinterlegt ist, sollte bei SSH-Login oder `git push` keine Passwortabfrage mehr kommen.

## Authentifizierung und Autorisierung

Die Anwendung verfuegt ueber ein eigenes Session-basiertes Auth-System ohne externe Bibliotheken:

- Login/Logout ueber `UserManagerController`
- Rollenbasierte Zugriffskontrolle via `@RequiredRoles`-Annotation und `RoleCheckInterceptor`
- Rollen: `NUTZER`, `ADMIN`
- Admin-Funktionen: User-CRUD unter `/userManager/`

Details und Sequenzdiagramme: [`docs/USER_MANAGEMENT.md`](docs/USER_MANAGEMENT.md)

## Fachlicher Kontext (GIS/OSM)

- GIS = Geoinformationssysteme.
- OSM/OpenStreetMap als Datenbasis.
- Geoinformatik = Verortung von Objekten (z. B. POIs, Adressen, Distanzen, Routen).
- Kartendarstellung i. d. R. in Merkatorprojektion und in Zoomstufen.
- Kachelgroesse: `256x256` Pixel (PNG).

## Relevante Bausteine und Dienste

### Leaflet (Karte im Client)

- Website: https://leafletjs.com/
- Aufgabe: Kartendarstellung, Layer, Verbindungslinien, Grafiken, Schattierungen.

### Nominatim (Geocoding)

- Website: https://nominatim.org/
- Service: https://nominatim.servicecluster.de
- Endpunkte:
	- `/search` fuer Geocoding (Adresse -> Koordinaten)
	- `/reverse` fuer Reverse-Geocoding (Koordinaten -> Adresse)

Beispiele:

- https://nominatim.servicecluster.de/search?q=Steingaden+Welfenstr+14&limit=5&format=json&addressdetails=1
- https://nominatim.servicecluster.de/reverse?format=json&lat=47.7003827&lon=10.8616748

### OSRM (Routing)

- Website: https://project-osrm.org/
- Service: http://osrm.servicecluster.de/
- Fokus: Fahrstrassenrouting.

### Tile-Server

- Demo: https://gis.servicecluster.de/demaps.html
- Tile-URL: `https://gis.servicecluster.de/de_tiles/{z}/{x}/{y}.png`
- Achsen: `z` = Zoomstufe, `x` = horizontal, `y` = vertikal

Kachelanzahl je Zoomstufe:

- Pro Achse: `2^z`
- Gesamtzahl Kacheln: `4^z`

## Demo-System

- https://agilogikmap.servicecluster.de/

## Begriffe

- Verortung: Positionierung von Objekten auf der Karte (Adresse oder `lat/lng`).
- Geocoding: Adresse -> `lat/lng`.
- Reverse-Geocoding: `lat/lng` -> Adresse.

## Hinweis zu Tomcat 9

Tomcat 9 ist mit Java 17 in dieser Konfiguration kompatibel. Die konkrete Grails-/Tomcat-Version wird projektspezifisch im Code/Build definiert.

# Urban Flow Governance — Leitstand für dynamische Sperrzonen

## Projektübersicht

**Urban Flow Governance** ist ein Leitstand-System für Stadtverwaltungen zur **Echtzeitverwaltung dynamischer Sperrzonen** (Marathons, Baustellen, Umweltalarme) und deren Auswirkungen auf das Stadtrouting.

Das System prüft automatisch, ob berechnete Fahrtrouten durch aktive Sperrzonen verlaufen und gibt Alarme aus — ein **Infrastruktur-Management-Tool** für städtische Mobilität.

## Kernkonzept

### Geometrisches Modell: Polygone statt Punkte

Statt einzelner Punkte verwenden wir **Polygone** zur Darstellung von Sperrzonen:
- Jede Zone ist ein geschlossenes Polygon auf der Karte
- Zonen können beliebige Formen haben (rechteckig, kreisförmig, unregelmäßig), wir konzentrieren uns vorerst allerdings erst einmal ausschl. auf eine Form

### Point-in-Polygon-Check (Kernalgorithmus)

Die zentrale Logik prüft: „Liegt eine berechnete Route innerhalb einer Sperrzone?"

1. OSRM berechnet eine Route (A → B) als Sequenz von Koordinaten
2. Backend prüft jeden Punkt der Route gegen alle aktiven Sperrzonen
3. Bei Intersection: Alarm + visuelle Markierung auf der Karte (rote Warnung)
4. Keine komplexe Umleitung: Einfach der berechneten Route folgen und Kollision erkennen

**Vorteil dieser Vereinfachung**: Mathematisch elegant, keine Graph-Manipulation nötig, OSRM bleibt unverändert.

## Architektur

### 1. Frontend (Browser)

Leaflet.js — Kartendarstellung und Benutzerinteraktion
- Visualisierung der Stadt (Hintergrund: Kachtiles von Tile-Server)
- Zeichenwerkzeuge: Polygone für neue Sperrzonen anlegen (Draw-Erweiterung)
- Routenvisualisierung: Eingabe Start/Ziel, Anzeige der berechneten Route
- Echtzeit-Alerts: Farbliche Hervorhebung bei Zonenkollisionen (grün = okay, rot = Warnung)

HTML5 / CSS3 — Responsive UI für Desktop 
- Sidebar mit Zonenverwaltung (Status, Zeitstempel)
- Suchfeld für Stadtteile/Straßen (Nominatim-Integration)
- Routenberechnung (Eingabeformular)

### 2. Backend (Logik & API)

Apache Tomcat 9 + Groovy/Java (Grails)
- RESTful API-Endpunkte für Frontend
- Point-in-Polygon-Logik (Koordinatenprüfung gegen Polygone)
- Kommunikation mit PostgreSQL (Abfrage Geodaten & Zonenstatus)
- Kommunikation mit externen Services via HTTP

Externe Service-Integration (via REST/JSON):
- OSRM: Route berechnen zwischen zwei Punkten
- Nominatim: Adresse/Straße → Koordinaten (für Zonendefition)

### 3. Datenbank

PostgreSQL + PostGIS
- Geodaten (OSM): Straßennetz, Gebäude, POIs (Punkte, Linien, Polygone)
- Sperrzonen: Polygon-Geometrien mit Metadaten
- Zone-Status: Zustandsverwaltung (geplant, aktiv, abgelaufen)
- Audit-Log: Historie von Zonenerstellungen/Änderungen

PostGIS-Funktionen:
- `ST_Contains()` — Punkt in Polygon prüfen (Core-Algorithmus)
- `ST_Intersects()` — Route (LineString) mit Polygon prüfen
- Räumliche Indizes für Performance

### 4. Infrastructure & Deployment

**Development**:
- Docker-basierter Dev Container (Ubuntu 24.04)
- Java 17, Grails, lokale PostgreSQL-Verbindung

Production (Agilogik-VM):
- Ubuntu 24.04 Server
- PostgreSQL mit PostGIS-Extension
- Tomcat 9
- ggf. Ansible für Config-Management (YAML)

Versionskontrolle: Git + Conventional Commits

## Tech Stack — Übersicht

| Schicht | Komponente | Zweck |
|---------|-----------|-------|
| **Frontend** | Leaflet.js | Kartendarstellung & Polygon-Zeichnung |
| | HTML5/CSS3 | UI-Struktur & Styling |
| **Backend** | Grails/Groovy | REST-API, Point-in-Polygon, Service-Orchestration |
| | Tomcat 9 | Application Server |
| **Externe APIs** | OSRM | Routenberechnung |
| | Nominatim | Geocoding (Adresse ↔ Koordinaten) |
| | Tile-Server | Kartenkacheln (256x256 PNG) |
| **Datenbank** | PostgreSQL | Persistierung |
| | PostGIS | Räumliche Indizes & Funktionen |
| **Infra** | Docker | Dev Container |
| | Ubuntu 24.04 | Betriebssystem |
| | Git | Versionskontrolle |

## Workflow-Beispiel

```
User-Szenario: Marathon wird kurzfristig geplant

1. Admin öffnet Urban Flow Governance
2. Klick: „Neue Zone anlegen"
3. Zeichnet Polygon um die Marathon-Strecke (Leaflet)
4. Status: „In Planung" → später „Aktiv" (Zeitplan-Aktivierung)
5. Taxifahrer plant Route A → B
6. Frontend sendet Request: GET /route?from=A&to=B
7. Backend fragt OSRM an, erhält Route als Koordinatenliste
8. Backend prüft: Liegen Routenpunkte in Marathon-Polygon?
9. Ergebnis: ✓ Kollision erkannt → rote Warnung auf Karte
10. Taxifahrer sieht: „Route kreuzt aktive Sperrzone (Marathon 14:00-18:00)"
11. Alternative: Manuell andere Strecke wählen ODER neuen Route-Request
```

## Zustandsverwaltung (UML-Zustandsautomat)

Jede Sperrzone durchläuft Zustände:

```
[In Planung] 
    → (Admin klickt „Aktivieren")
[Aktiv/Gesperrt]
    → (Zeitplan-Ende oder manuelles Deaktivieren)
[Abgelaufen]
    → (Archiviert)
```

**Backend-Logik**: Nur Zonen im Status „Aktiv" beeinflussen Routenprüfung.

## Key Features (MVP)

1. Zone Management
   - Polygone zeichnen, speichern, löschen
   - Status-Übergänge (Planung → Aktiv → Abgelaufen)
   - Metadaten: Grund (Marathon/Baustelle/Umwelt), Start/End-Zeit

2. Routenprüfung
   - OSRM-Integration: Route A → B
   - Point-in-Polygon-Check gegen alle aktiven Zonen
   - Alert bei Kollision

3. Visualisierung
   - Leaflet-Karte mit Zonen (Polygon-Rendering)
   - Routenvisualisierung (grün bei okay, rot bei Warnung)
   - Echtzeit-Statusanzeige

4.*Suchfunktion
   - Nominatim-Integration: Stadtteil/Straße suchen
   - Auto-Complete für Start/Ziel

## Algorithmus-Details: Vereinfachte Variante

Warum nicht komplexe Umleitung?
- Einfachheit: Keine Graph-Manipulation nötig
- Wartbarkeit: OSRM-Server bleibt unverändert
- Mathematische Eleganz: Punkt-in-Polygon ist O(n) pro Punkt

Ablauf:
```
route = OSRM.compute(A, B)                    // Liste von Koordinaten
for each point in route:
  for each zone in active_zones:
    if point_in_polygon(point, zone.polygon):
      alert = true
      break
if alert:
  display_red_warning()
else:
  display_green_ok()
```

## Nächste Schritte (Development-Roadmap)

1. Basis-Setup (diese Woche)
   - Repository-Struktur (Grails-Projekt initialisieren)
   - Dev Container validieren
   - PostgreSQL + PostGIS lokal konfigurieren

2. Leaflet-Frontend (Woche 2)
   - Karte mit Tiles laden
   - Polygon-Draw-Erweiterung integrieren
   - Zone-Formular (Name, Status, Zeitraum)

3. Backend-API (Woche 2–3)
   - `/zones` (CRUD für Sperrzonen)
   - `/route` (Route berechnen + Prüfung)
   - Nominatim & OSRM HTTP-Clients

4. PostGIS-Integration (Woche 3)
   - Polygon-Speicherung in PostgreSQL
   - Point-in-Polygon-Queries mit `ST_Contains()`
   - Räumliche Indizes

5. Zustandsverwaltung & Alerts (Woche 4)
   - Zone-Status-Automat (UI + Backend)
   - Echtzeit-Alerts bei Routenkollision

6. Testing & Deployment (Woche 5)
   - Integration Tests
   - Deployment auf Agilogik-VM
   - Demo mit echten Daten (OSM)

## Ressourcen & Service-Endpoints

| Service | Endpoint | Nutzung |
|---------|----------|--------|
| OSRM | http://osrm.servicecluster.de/ | Route berechnen |
| Nominatim | https://nominatim.servicecluster.de/ | Geocoding |
| Tile-Server | https://gis.servicecluster.de/de_tiles/{z}/{x}/{y}.png | Kartenkacheln |
| PostgreSQL | intern (env-vars) | Datenspeicherung |
| Demo-System | https://agilogikmap.servicecluster.de/ | Referenzimplementierung |

## Development-Umgebung

Siehe `README.md` für Setup-Anleitung (Dev Container, Docker, WSL2, macOS).

---

Status: Detaillierte Spezifikation (v1)  
Letzte Aktualisierung: 2026-04-21  

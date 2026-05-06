# Kanban-Board — Urban Flow Governance

Arbeitsboard auf Basis der User Stories aus [USER_STORIES.md](USER_STORIES.md) und [USER_STORIES.csv](USER_STORIES.csv).

## Zuständigkeiten

- **Elizat Mairambek kyzy** — Frontend, Karte, Geocoding, Routing
- **Gregor Kobilarov** — Backend, GIS, API, Frontend
- **Tim Lanzendoerfer** — Backend, Auth, User Management, Infrastruktur, Datenbank

## Board

> Erfordert Mermaid ≥ 11.4 (VS Code Mermaid Preview Extension o.ä.).

```mermaid
---
config:
  kanban:
    ticketBaseUrl: ''
---
kanban
  Backlog
    b1[US-2.3 Alternative Route anfordern]
    b3[US-5.2 Benachrichtigung bei Zone-Aktivierung]
    b4[US-6.1 Zone-Verlauf ansehen]

  ToDo[To Do]
    t2[US-3.3 Route-Konflikt visuell hervorheben]
    t3[US-3.2 Zone-Details beim Klick anzeigen]
    t8["Deploy to Prod (Tim)"]

  InProgress[In Bearbeitung]
    p1["US-1.1 Sperrzone erstellen (Gregor)"]
    p2["US-1.4 Zone-Status verwalten (Gregor)"]
    p3["US-3.1 Sperrzonen auf Karte anzeigen (Elizat)"]
    p4["US-5.1 Aktive Zonen anzeigen (Gregor)"]
    t7["Datenbank Schema erstellen Prod (Tim)"]

  Review
    r1["US-1.2 Sperrzone bearbeiten (Gregor)"]
    r2["US-1.3 Sperrzone loeschen (Gregor)"]
    r3["US-2.2 Warnung bei Sperrzone-Konflikt (Gregor)"]
    r4["US-7.1 Login als Admin/Nutzer (Tim)"]
    r5["US-7.2 Logout als Admin/Nutzer (Tim)"]
    r6["US-7.3 Session Timeout (Tim)"]
    r7["US-8.1 Nutzer erstellen (Tim)"]
    r8["US-8.2 Nutzer loeschen (Tim)"]
    r9["US-8.3 Admin-Rolle zuordnen (Tim)"]
    p5["PostgreSQL in Dev einbinden (Tim)"]

  Erledigt
    d1["OpenAPI + Swagger UI (Gregor)"]
    d2["GET /api/zones/active Endpoint (Gregor)"]
    d3["Auth-Scope alle Endpoints (Gregor)"]
    d4["US-2.1 Route berechnen (Elizat/Gregor)"]
    d5["US-4.1 Stadtteil/Straße suchen (Elizat)"]
    d6["US-4.2 Startpunkt/Ziel suchen (Elizat)"]
```

## Kommentare

| ID | Kommentar |
|----|-----------|
| US-2.1 | Route-Berechnung via OSRM fertig; Auto-Trigger bei Eingabe beider Felder; Grails-Integration ausstehend |
| US-4.1 | Autocomplete via Nominatim fertig; Grails-Integration ausstehend |
| US-4.2 | Start-/Zielmarker auf Karte fertig |
| US-1.1 | Backend/API + Tests fertig (save, PLANNED-Status); Polygon-Zeichnen + Admin-UI offen |
| US-1.4 | Status-Enum + activate/deactivate + ZoneTransitionService (60s-Scheduler) fertig; UI fehlt noch |
| US-3.1 | Prototyp mit Leaflet + Mock-Daten; API-Anbindung + Grails-Integration ausstehend |
| US-5.1 | Service + REST-Endpoint fertig; Frontend zeigt alle Zonen farbkodiert, dediziertes Panel fehlt noch |
| Auth-Scope | Alle Endpoints erhalten @RequiredRoles. GET /api/zones, GET /api/zones/{id}, GET /api/zones/active, POST /api/route/check → ADMIN + NUTZER; schreibende Zone-Endpoints + User-Management → ADMIN |
| US-1.2 | update-Endpoint + Integrationstest grün; Frontend-Integration ausstehend |
| US-1.3 | delete-Endpoint + Integrationstest grün; Bestätigungsdialog im Frontend offen |
| US-2.2 | RouteCheckService liefert OK/WARNING mit Zone + Zeitraum; UI-Darstellung offen |
| US-7.1–7.3 | Ready für Review. Frontend-Integration ausstehend |
| US-8.1–8.3 | Ready für Review. Frontend-Integration ausstehend |

## Hinweise

- Eintraege koennen im Format `US-x.y Titel | Bearbeiter: Name | Kommentare: Kurznotiz` gepflegt werden.
- Bearbeiter bleibt leer bzw. `offen`, bis eine Aufgabe zugewiesen ist.
- Kommentare dienen fuer kurze Rueckfragen, Abhaengigkeiten oder Statushinweise.

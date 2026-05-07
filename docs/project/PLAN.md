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

  InProgress[In Bearbeitung]
    p1["US-1.1 Sperrzone erstellen (Gregor)"]
    p2["US-1.4 Zone-Status verwalten (Gregor)"]

  Review
    r1["US-1.2 Sperrzone bearbeiten (Gregor)"]
    r2["US-1.3 Sperrzone loeschen (Gregor)"]
    r3["US-2.2 Warnung bei Sperrzone-Konflikt (Gregor)"]
    r5["US-7.2 Logout als Admin/Nutzer (Tim)"]
    r6["US-7.3 Session Timeout (Tim)"]
    r8["US-8.2 Nutzer loeschen (Tim)"]
    r9["US-8.3 Admin-Rolle zuordnen (Tim)"]
    p3["US-3.1 Sperrzonen auf Karte anzeigen (Elizat)"]
    p4["US-5.1 Aktive Zonen anzeigen (Gregor)"]

  Erledigt
    d1["OpenAPI + Swagger UI (Gregor)"]
    d2["GET /api/zones/active Endpoint (Gregor)"]
    d3["Auth-Scope alle Endpoints (Gregor)"]
    d4["US-2.1 Route berechnen (Elizat)"]
    d5["US-4.1 Stadtteil/Straße suchen (Elizat)"]
    d6["US-4.2 Startpunkt/Ziel suchen (Elizat)"]
    r4["US-7.1 Login als Admin/Nutzer (Tim/Gregor)"]
    r7["US-8.1 Nutzer erstellen (Tim/Gregor)"]
    p5["PostgreSQL in Dev einbinden (Tim)"]
    t7["Datenbank Schema erstellen Prod (Tim)"]
    t8["Deploy to Prod (Tim)"]
    d7["Dark/Light Mode Toggle (Gregor)"]
    d8["Start/Ziel tauschen (Gregor)"]
    d9["Deployment Pipeline pre-push (Gregor)"]
    d10["CORS fix (Gregor)"]
    d11["Auth-Fehlermeldungen Login/Registrierung (Gregor)"]
```

## Kommentare

| ID | Kommentar |
|----|-----------|
| US-2.1 | Route-Berechnung via OSRM fertig; Auto-Trigger bei Eingabe beider Felder; Grails-Integration ausstehend |
| US-4.1 | Autocomplete via Nominatim fertig; Grails-Integration ausstehend |
| US-4.2 | Start-/Zielmarker auf Karte fertig |
| US-1.1 | Backend/API + Tests fertig (save, PLANNED-Status); Polygon-Zeichnen + Admin-UI offen |
| US-1.4 | Status-Enum + activate/deactivate + ZoneTransitionService (60s-Scheduler) fertig; UI fehlt noch |
| US-3.1 | API-Anbindung fertig; Zonen laden via API und farbkodiert in Prod; dedizierter Review-Schritt mit Elizat offen |
| US-5.1 | Zonen werden farbkodiert via API in Prod angezeigt (blau/rot/grau); dediziertes Sidebar-Panel mit Name, Grund, verbleibender Zeit fehlt noch |
| Auth-Scope | Alle Endpoints erhalten @RequiredRoles. GET /api/zones, GET /api/zones/{id}, GET /api/zones/active, POST /api/route/check → ADMIN + NUTZER; schreibende Zone-Endpoints + User-Management → ADMIN |
| US-1.2 | update-Endpoint + Integrationstest grün; Frontend-Integration ausstehend |
| US-1.3 | delete-Endpoint + Integrationstest grün; Bestätigungsdialog im Frontend offen |
| US-2.2 | RouteCheckService liefert OK/WARNING mit Zone + Zeitraum; UI-Darstellung offen |
| US-7.1 | Login-Frontend fertig und in Prod; Fehlermeldungen für falschen Benutzernamen und falsches Passwort; Bearer-Token in localStorage |
| US-7.2 | Backend fertig; Logout-Button im Frontend fehlt noch |
| US-7.3 | Backend fertig (TOKEN_TTL_MINUTES); kein Frontend-Feedback bei abgelaufener Session |
| US-8.1 | Registrierungs-Frontend fertig und in Prod; Auto-Login nach Registrierung; Fehlermeldung bei vergebenem Benutzernamen |
| US-8.2 | delete-Endpoint + Integrationstest grün; Frontend-Integration ausstehend |
| US-8.3 | update-Endpoint unterstützt Rollenvergabe; Frontend-Integration ausstehend |
| Dark/Light Mode | Toggle in Sidebar; Präferenz in localStorage gespeichert; Leaflet-Tiles per CSS-Filter invertiert |
| Start/Ziel tauschen | Swap-Button zwischen Start/Ziel per getBoundingClientRect() zentriert; tauscht Werte, Koordinaten und Marker |
| Deployment Pipeline | pre-push Hook deployt frontend/ via SCP auf Apache automatisch bei Push auf main |
| CORS fix | Doppelten Access-Control-Allow-Origin-Header entfernt (Grails-Config entfernt, Tomcat-Filter bleibt); OPTIONS-Preflight in RoleCheckInterceptor behandelt |
| Auth-Fehlermeldungen | Login: „Benutzername nicht gefunden" / „Falsches Passwort"; Registrierung: „Benutzername bereits vergeben" |

## Hinweise

- Eintraege koennen im Format `US-x.y Titel | Bearbeiter: Name | Kommentare: Kurznotiz` gepflegt werden.
- Bearbeiter bleibt leer bzw. `offen`, bis eine Aufgabe zugewiesen ist.
- Kommentare dienen fuer kurze Rueckfragen, Abhaengigkeiten oder Statushinweise.

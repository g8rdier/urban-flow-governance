# Kanban-Board — Urban Flow Governance

Arbeitsboard auf Basis der User Stories aus [USER_STORIES.md](USER_STORIES.md) und [USER_STORIES.csv](USER_STORIES.csv).

## Backlog

- US-2.3 Alternative Route anfordert | Bearbeiter: offen | Kommentare: -
- US-4.2 Startpunkt/Ziel suchen | Bearbeiter: offen | Kommentare: -
- US-5.2 Benachrichtigung bei Zone-Aktivierung | Bearbeiter: offen | Kommentare: -
- US-6.1 Zone-Verlauf ansehen | Bearbeiter: offen | Kommentare: -

## To Do

- US-2.1 Route berechnen | Bearbeiter: offen | Kommentare: -
- US-3.1 Sperrzonen auf Karte anzeigen | Bearbeiter: offen | Kommentare: -
- US-3.3 Route-Konflikt visuell hervorheben | Bearbeiter: offen | Kommentare: -
- US-3.2 Zone-Details beim Klick anzeigen | Bearbeiter: offen | Kommentare: -
- US-4.1 Stadtteil/Straße suchen | Bearbeiter: offen | Kommentare: -

## In Bearbeitung

- US-1.1 Sperrzone erstellen | Bearbeiter: Gregor Kobilarov | Kommentare: Backend/API + Tests fertig (save, PLANNED-Status); Polygon-Zeichnen + Admin-UI offen
- US-1.4 Zone-Status verwalten | Bearbeiter: Gregor Kobilarov | Kommentare: Status-Enum + activate/deactivate fertig, RouteCheck nutzt nur ACTIVE; zeitgesteuerte Übergänge + UI fehlen
- US-5.1 Aktive Zonen anzeigen | Bearbeiter: Gregor Kobilarov | Kommentare: Service.listActive vorhanden und getestet; REST-Endpoint + Sidebar/Panel fehlen

- US-8.2 Nutzer loeschen | Bearbeiter: Tim Lanzendoerfer | Kommentare: Es fehlen noch tests
- US-7.1 Login als Admin/Nutzer | Bearbeiter: Tim Lanzendoerfer | Kommentare: Es fehlen noch tests
- US-7.2 Logout als Admin/Nutzer | Bearbeiter: Tim Lanzendoerfer | Kommentare: Es fehlen noch tests
- US-7.3 Session Timeout als Admin/Nutzer | Bearbeiter: Tim Lanzendoerfer | Kommentare: Es fehlen noch tests
- US-8.1 Nutzer erstellen | Bearbeiter: Tim Lanzendoerfer | Kommentare: Es fehlen noch tests
- US-8.3 Admin-Rolle zuordnen | Bearbeiter: Tim Lanzendoerfer | Kommentare: Es fehlen noch tests

## Review

- US-1.2 Sperrzone bearbeiten | Bearbeiter: Gregor Kobilarov | Kommentare: update-Endpoint + Integrationstest grün; Frontend-Integration ausstehend
- US-1.3 Sperrzone loeschen | Bearbeiter: Gregor Kobilarov | Kommentare: delete-Endpoint + Integrationstest grün; Bestätigungsdialog im Frontend offen
- US-2.2 Warnung bei Sperrzone-Konflikt | Bearbeiter: Gregor Kobilarov | Kommentare: RouteCheckService liefert OK/WARNING mit Zone + Zeitraum, nur ACTIVE; UI-Darstellung offen

## Erledigt

- Keine Eintraege | Bearbeiter: - | Kommentare: -

## Hinweise

- Eintraege koennen im Format `US-x.y Titel | Bearbeiter: Name | Kommentare: Kurznotiz` gepflegt werden.
- Bearbeiter bleibt leer bzw. `offen`, bis eine Aufgabe zugewiesen ist.
- Kommentare dienen fuer kurze Rueckfragen, Abhaengigkeiten oder Statushinweise.
# Zone Management

## Sequenzdiagramm: Route-Check

```mermaid
sequenceDiagram
    autonumber
    participant U as Nutzer (Browser)
    participant FE as Frontend (Leaflet)
    participant BE as Backend (Grails)
    participant OSRM as OSRM Service
    participant DB as PostgreSQL (PostGIS)

    U->>FE: Start und Ziel eingeben
    FE->>BE: GET /route?from=<start>&to=<ziel>
    BE->>OSRM: GET /route/v1/driving/<start>;<ziel>
    OSRM-->>BE: Route als Koordinatenliste

    BE->>DB: SELECT aktive Sperrzonen (status = 'ACTIVE')
    DB-->>BE: Liste aktiver Zonen

    loop Jeder Routenpunkt
        BE->>BE: ST_Contains(zone.geometry, punkt)?
        alt Kollision erkannt
            BE->>BE: alert = true, betroffene Zone merken
        end
    end

    alt Keine Kollision
        BE-->>FE: 200 { route, status: "OK" }
        FE-->>U: Route gruen anzeigen
    else Kollision
        BE-->>FE: 200 { route, status: "WARNING", zones: [...] }
        FE-->>U: Route rot markieren, Warnung anzeigen
    end
```

## Sequenzdiagramm: Zone verwalten

```mermaid
sequenceDiagram
    autonumber
    participant A as Admin (Browser)
    participant FE as Frontend (Leaflet)
    participant BE as Backend (Grails)
    participant RCI as RoleCheckInterceptor
    participant DB as PostgreSQL

    note over A,DB: Zone anlegen

    A->>FE: Polygon auf Karte zeichnen + Formular ausfuellen
    FE->>BE: POST /zones (name, reason, geometry, startTime, endTime)
    BE->>RCI: before()
    alt Nicht eingeloggt oder keine ADMIN-Rolle
        RCI-->>A: 401 / 403
    else Autorisiert
        RCI-->>BE: erlaubt
        BE->>DB: INSERT RestrictedZone (status = 'PLANNED')
        DB-->>BE: Zone gespeichert
        BE-->>FE: 201 { zone }
        FE-->>A: Zone als Polygon auf Karte anzeigen
    end

    note over A,DB: Zone aktivieren

    A->>FE: Klickt "Aktivieren" bei einer Zone
    FE->>BE: PUT /zones/<id>/activate
    BE->>RCI: before()
    alt Nicht autorisiert
        RCI-->>A: 401 / 403
    else Autorisiert
        RCI-->>BE: erlaubt
        BE->>DB: UPDATE status = 'ACTIVE'
        DB-->>BE: Zone aktualisiert
        BE-->>FE: 200 { zone }
        FE-->>A: Zone rot hervorheben (aktiv)
    end

    note over A,DB: Zone deaktivieren

    A->>FE: Klickt "Deaktivieren" bei einer Zone
    FE->>BE: PUT /zones/<id>/deactivate
    BE->>RCI: before()
    alt Nicht autorisiert
        RCI-->>A: 401 / 403
    else Autorisiert
        RCI-->>BE: erlaubt
        BE->>DB: UPDATE status = 'EXPIRED'
        DB-->>BE: Zone aktualisiert
        BE-->>FE: 200 { zone }
        FE-->>A: Zone ausgegraut (abgelaufen)
    end
```

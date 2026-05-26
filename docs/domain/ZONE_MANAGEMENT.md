# Zone Management

## Datenmodell

```mermaid
erDiagram
    RESTRICTED_ZONE {
        Long id PK
        String name
        String description
        String reason
        String geometryWKT
        Date startTime
        Date endTime
        String status
        Date createdAt
        Date updatedAt
        String createdBy
    }

    USER ||--o{ RESTRICTED_ZONE : "creates"
```

**Wichtige Punkte:**
- `geometryWKT` speichert die Polygon-Geometrie als WKT-String (Well-Known Text Format)
- `status` kann einen der Werte haben: `PLANNED`, `ACTIVE`, `EXPIRED`
- `reason` ist einer von: `Marathon`, `Baustelle`, `Umweltalarm`
- `createdBy` speichert den Username des Benutzers, der die Zone angelegt hat
- `startTime` und `endTime` definieren den Gültigkeitszeitraum der Zone

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
    FE->>BE: GET /route?from=A&to=B
    
    BE->>OSRM: Routenberechnung anfordern
    OSRM-->>BE: Route als Koordinatenliste
    
    BE->>DB: SELECT aktive Sperrzonen
    DB-->>BE: Liste aktiver Zonen
    
    Note over BE: Prüfe jeden Punkt gegen Zonen
    loop für jeden Punkt der Route
        rect rgb(200, 150, 255)
        BE->>BE: ST_Contains(zone.polygon, punkt)?
        BE->>BE: Falls ja: alert=true
        end
    end
    
    alt alert = false (Keine Kollision)
        BE-->>FE: 200 OK {status: "OK"}
        FE-->>U: ✓ Route grün anzeigen
    else alert = true (Kollision!)
        BE-->>FE: 200 {status: "WARNING", zones: [...]}
        FE-->>U: ⚠️ Route rot + Warnung
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

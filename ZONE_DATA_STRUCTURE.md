# Sperrzone als Datenstruktur

## Modellierung

Eine Sperrzone wird durch folgende Eigenschaften definiert:

### Grails Domain Class (Groovy)

```groovy
// grails-app/domain/RestrictedZone.groovy

import com.vividsolutions.jts.geom.Polygon

class RestrictedZone {
    String name
    String description
    String reason  // "Marathon", "Baustelle", "Umweltalarm"
    
    // Polygon-Geometrie (PostGIS)
    Polygon geometry
    
    // Zeitliche Gültigkeit
    Date startTime
    Date endTime
    
    // Status
    String status  // "PLANNED", "ACTIVE", "EXPIRED"
    
    // Metadaten
    Date createdAt
    Date updatedAt
    String createdBy
    
    static constraints = {
        name blank: false
        geometry nullable: false
        startTime nullable: false
        endTime nullable: false, validator: { val, obj -> val > obj.startTime }
        status inList: ["PLANNED", "ACTIVE", "EXPIRED"]
    }
    
    static mapping = {
        geometry type: 'org.hibernate.spatial.GeometryType'
    }
}
```

## Beispiel: Marathon-Sperrzone

### Als Java/Groovy-Objekt

```groovy
import com.vividsolutions.jts.geom.GeometryFactory
import com.vividsolutions.jts.geom.Coordinate
import java.time.LocalDateTime

def geometryFactory = new GeometryFactory()

// Koordinaten der Marathon-Strecke (Polygon: Rechteck in München)
def coords = [
    new Coordinate(11.5500, 48.1350),  // SW corner
    new Coordinate(11.6200, 48.1350),  // SE corner
    new Coordinate(11.6200, 48.1550),  // NE corner
    new Coordinate(11.5500, 48.1550),  // NW corner
    new Coordinate(11.5500, 48.1350),  // zurück zum Start (Polygon muss geschlossen sein)
] as Coordinate[]

def polygon = geometryFactory.createPolygon(coords)

def marathonZone = new RestrictedZone(
    name: "München Marathon 2026",
    description: "Stadtmarathon mit Streckensperrung",
    reason: "Marathon",
    geometry: polygon,
    startTime: new Date(126, 5, 15, 14, 0, 0),  // 15.06.2026 14:00 UTC
    endTime: new Date(126, 5, 15, 18, 0, 0),    // 15.06.2026 18:00 UTC
    status: "PLANNED",
    createdBy: "admin@example.com"
)

marathonZone.save()
```

## JSON-Repräsentation (API)

```json
{
  "id": 42,
  "name": "München Marathon 2026",
  "description": "Stadtmarathon mit Streckensperrung",
  "reason": "Marathon",
  "geometry": {
    "type": "Polygon",
    "coordinates": [
      [
        [11.5500, 48.1350],
        [11.6200, 48.1350],
        [11.6200, 48.1550],
        [11.5500, 48.1550],
        [11.5500, 48.1350]
      ]
    ]
  },
  "startTime": "2026-06-15T14:00:00Z",
  "endTime": "2026-06-15T18:00:00Z",
  "status": "PLANNED",
  "createdAt": "2026-04-21T12:00:00Z",
  "updatedAt": "2026-04-21T12:00:00Z",
  "createdBy": "admin@example.com"
}
```

## Point-in-Polygon Check (Core-Algorithmus)

```groovy
// Prüfung: Liegt Punkt (lat, lng) in Zone?

import com.vividsolutions.jts.geom.Coordinate
import com.vividsolutions.jts.geom.GeometryFactory

def pointInPolygon(double latitude, double longitude, RestrictedZone zone) {
    def geometryFactory = new GeometryFactory()
    def point = geometryFactory.createPoint(new Coordinate(longitude, latitude))
    
    // True wenn Punkt innerhalb des Polygons liegt
    return zone.geometry.contains(point)
}

// Beispiel: Ist die Koordinate (48.14, 11.58) in der Marathon-Zone?
def isInZone = pointInPolygon(48.14, 11.58, marathonZone)
println "Punkt in Marathon-Zone: ${isInZone}"  // true
```

## Route-Collision Check

```groovy
// Prüfung: Schneidet Route die Zone?

def routeIntersectsZone(List<Coordinate> routeCoordinates, RestrictedZone zone) {
    def geometryFactory = new GeometryFactory()
    def routeLine = geometryFactory.createLineString(routeCoordinates as Coordinate[])
    
    // True wenn Route das Polygon schneidet
    return zone.geometry.intersects(routeLine)
}

// Beispiel: OSRM gibt Route als Liste von Koordinaten zurück
def osrmRoute = [
    new Coordinate(11.4500, 48.1200),  // Start
    new Coordinate(11.5000, 48.1250),
    new Coordinate(11.5800, 48.1400),  // Schneidet Marathon-Zone!
    new Coordinate(11.6500, 48.1500),
    new Coordinate(11.7000, 48.1600),  // Ziel
]

def collision = routeIntersectsZone(osrmRoute, marathonZone)
if (collision) {
    println "⚠️  Route schneidet aktive Sperrzone: ${marathonZone.name}"
} else {
    println "✓ Route OK"
}
```

## GeoJSON für Frontend (Leaflet)

```javascript
// Polygon-Daten für Leaflet-Darstellung (JavaScript/Frontend)

const marathonZoneGeoJSON = {
  "type": "Feature",
  "properties": {
    "id": 42,
    "name": "München Marathon 2026",
    "status": "PLANNED",
    "startTime": "2026-06-15T14:00:00Z",
    "endTime": "2026-06-15T18:00:00Z"
  },
  "geometry": {
    "type": "Polygon",
    "coordinates": [
      [
        [11.5500, 48.1350],
        [11.6200, 48.1350],
        [11.6200, 48.1550],
        [11.5500, 48.1550],
        [11.5500, 48.1350]
      ]
    ]
  }
};

// In Leaflet rendern:
L.geoJSON(marathonZoneGeoJSON, {
  style: {
    fillColor: '#ff0000',
    fillOpacity: 0.3,
    color: '#ff0000',
    weight: 2
  }
}).addTo(map);
```

## SQL (PostgreSQL + PostGIS)

```sql
-- Tabelle für Sperrzonen
CREATE TABLE restricted_zone (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    reason VARCHAR(50),
    geometry GEOMETRY(Polygon, 4326) NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    status VARCHAR(20) DEFAULT 'PLANNED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255)
);

-- Räumlicher Index für Performance
CREATE INDEX idx_zone_geometry ON restricted_zone USING GIST(geometry);

-- Beispiel-Insert
INSERT INTO restricted_zone (name, description, reason, geometry, start_time, end_time, status, created_by)
VALUES (
    'München Marathon 2026',
    'Stadtmarathon mit Streckensperrung',
    'Marathon',
    ST_GeomFromText('POLYGON((11.55 48.135, 11.62 48.135, 11.62 48.155, 11.55 48.155, 11.55 48.135))', 4326),
    '2026-06-15 14:00:00',
    '2026-06-15 18:00:00',
    'PLANNED',
    'admin@example.com'
);

-- Point-in-Polygon Query
SELECT * FROM restricted_zone 
WHERE ST_Contains(geometry, ST_Point(11.58, 48.14));

-- Route-Intersection Query
SELECT * FROM restricted_zone 
WHERE ST_Intersects(geometry, ST_GeomFromText('LINESTRING(...)', 4326));
```

---

**Zusammenfassung**:
- **Datenstruktur**: Domain Class mit Polygon-Geometrie
- **Speicherung**: PostgreSQL mit PostGIS für räumliche Indizes
- **Algorithmen**: `ST_Contains()` für Punkt-in-Polygon, `ST_Intersects()` für Routen-Kollision
- **Frontend**: GeoJSON für Leaflet-Visualisierung

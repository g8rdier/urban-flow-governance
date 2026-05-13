package ufg.app

import grails.gorm.transactions.Transactional
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.MultiLineString
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.Polygon

class RouteCheckService {

    static transactional = false

    private final GeometryFactory geometryFactory = new GeometryFactory()

    boolean isPointInZone(double latitude, double longitude, RestrictedZone zone) {
        if (!zone?.geometry) return false
        Point point = geometryFactory.createPoint(new Coordinate(longitude, latitude))
        zone.geometry.contains(point)
    }

    boolean isPointInPolygon(double latitude, double longitude, Polygon polygon) {
        if (!polygon) return false
        Point point = geometryFactory.createPoint(new Coordinate(longitude, latitude))
        polygon.contains(point)
    }

    boolean routeIntersectsZone(List<List<Double>> routeCoordinates, RestrictedZone zone) {
        if (!zone?.geometry || !routeCoordinates || routeCoordinates.size() < 2) return false
        LineString route = buildLineString(routeCoordinates)
        zone.geometry.intersects(route)
    }

    boolean routeIntersectsPolygon(List<List<Double>> routeCoordinates, Polygon polygon) {
        if (!polygon || !routeCoordinates || routeCoordinates.size() < 2) return false
        LineString route = buildLineString(routeCoordinates)
        polygon.intersects(route)
    }

    @Transactional(readOnly = true)
    Map checkRoute(List<List<Double>> routeCoordinates) {
        List<RestrictedZone> activeZones = RestrictedZone.findAllByStatus("ACTIVE")
        LineString route = buildLineString(routeCoordinates)

        List<Map> collidingZones = activeZones.findAll {
            it.geometry?.intersects(route)
        }.collect { zone ->
            [
                id          : zone.id,
                name        : zone.name,
                reason      : zone.reason,
                intersection: geometryToGeoJson(zone.geometry.intersection(route))
            ]
        }

        [
            status: collidingZones.isEmpty() ? "OK" : "WARNING",
            zones : collidingZones
        ]
    }

    private Map geometryToGeoJson(Geometry geom) {
        if (!geom || geom.isEmpty()) return null
        if (geom instanceof LineString) {
            return [type: "LineString", coordinates: geom.coordinates.collect { [it.x, it.y] }]
        }
        if (geom instanceof MultiLineString) {
            return [type: "MultiLineString", coordinates: (0..<geom.numGeometries).collect { i ->
                geom.getGeometryN(i).coordinates.collect { [it.x, it.y] }
            }]
        }
        def lines = (0..<geom.numGeometries).collect { i -> geom.getGeometryN(i) }
            .findAll { it instanceof LineString && !it.isEmpty() }
            .collect { it.coordinates.collect { c -> [c.x, c.y] } }
        if (!lines) return null
        return lines.size() == 1
            ? [type: "LineString", coordinates: lines[0]]
            : [type: "MultiLineString", coordinates: lines]
    }

    private LineString buildLineString(List<List<Double>> routeCoordinates) {
        Coordinate[] coords = routeCoordinates.collect { pair ->
            new Coordinate(pair[1] as double, pair[0] as double)
        } as Coordinate[]
        geometryFactory.createLineString(coords)
    }
}

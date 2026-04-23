package ufg.app

import grails.gorm.transactions.Transactional
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LineString
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
        List<RestrictedZone> collidingZones = activeZones.findAll {
            routeIntersectsZone(routeCoordinates, it)
        }

        [
            status: collidingZones.isEmpty() ? "OK" : "WARNING",
            zones : collidingZones.collect { [id: it.id, name: it.name, reason: it.reason] }
        ]
    }

    private LineString buildLineString(List<List<Double>> routeCoordinates) {
        Coordinate[] coords = routeCoordinates.collect { pair ->
            new Coordinate(pair[1] as double, pair[0] as double)
        } as Coordinate[]
        geometryFactory.createLineString(coords)
    }
}

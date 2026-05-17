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

    def grailsApplication

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

    @Transactional(readOnly = true)
    Map findAlternativeRoute(List<Double> start, List<Double> end) {
        String osrmBase = grailsApplication.config.getProperty('ufg.osrm.url', String)

        // Step 1: try OSRM's natural alternatives
        def osrmData = callOsrm(osrmBase, [start, end], true)
        if (!osrmData?.routes) return [status: "NONE_FOUND"]

        for (def route : osrmData.routes) {
            List<List<Double>> coords = toLatLonList(route.geometry.coordinates)
            if (checkRoute(coords).status == "OK") {
                return [status: "OK", geometry: route.geometry, distance: route.distance, duration: route.duration]
            }
        }

        // Step 2: find which zones block the main route
        List<List<Double>> mainCoords = toLatLonList(osrmData.routes[0].geometry.coordinates)
        LineString mainRoute = buildLineString(mainCoords)
        List<RestrictedZone> intersecting = RestrictedZone.findAllByStatus("ACTIVE")
            .findAll { it.geometry?.intersects(mainRoute) }

        // Step 3: try waypoints around each blocking zone,
        // collect all zone-free candidates and return the shortest one
        List<Map> cleanRoutes = []

        for (def zone : intersecting) {
            def env     = zone.geometry.envelopeInternal
            def centroid = zone.geometry.centroid
            double mx   = Math.max(env.width, env.height) * 0.15 + 0.005

            // Exterior ring vertices offset outward from centroid
            List<List<Double>> vertices = (zone.geometry.exteriorRing.coordinates as List)
                .init()
                .collect { coord ->
                    double dlat = coord.y - centroid.y
                    double dlon = coord.x - centroid.x
                    double dist = Math.sqrt(dlat * dlat + dlon * dlon)
                    if (dist < 0.0001) return null
                    double scale = (dist + mx) / dist
                    [centroid.y + dlat * scale, centroid.x + dlon * scale]
                }
                .findAll { it != null }

            // Single-waypoint: each vertex + N/S/E/W midpoints
            double midLat = (env.minY + env.maxY) / 2
            double midLon = (env.minX + env.maxX) / 2
            List<List<Double>> singles = vertices + [
                [env.maxY + mx, midLon],
                [env.minY - mx, midLon],
                [midLat,        env.maxX + mx],
                [midLat,        env.minX - mx],
            ]
            for (def wp : singles) {
                def d = callOsrm(osrmBase, [start, wp, end], false)
                if (!d?.routes) continue
                if (checkRoute(toLatLonList(d.routes[0].geometry.coordinates)).status == "OK") {
                    def r = d.routes[0]
                    cleanRoutes << [status: "OK", geometry: r.geometry, distance: r.distance, duration: r.duration]
                }
            }

            // Double-waypoint: pairs of consecutive vertices (routes around each side of the zone)
            for (int i = 0; i < vertices.size(); i++) {
                def w1 = vertices[i]
                def w2 = vertices[(i + 1) % vertices.size()]
                def d  = callOsrm(osrmBase, [start, w1, w2, end], false)
                if (!d?.routes) continue
                if (checkRoute(toLatLonList(d.routes[0].geometry.coordinates)).status == "OK") {
                    def r = d.routes[0]
                    cleanRoutes << [status: "OK", geometry: r.geometry, distance: r.distance, duration: r.duration]
                }
            }
        }

        if (!cleanRoutes) return [status: "NONE_FOUND"]
        cleanRoutes.min { it.distance as double }
    }

    private def callOsrm(String base, List<List<Double>> waypoints, boolean alternatives) {
        String coords = waypoints.collect { "${it[1]},${it[0]}" }.join(';')
        String url = "${base}/route/v1/driving/${coords}?overview=full&geometries=geojson${alternatives ? '&alternatives=true' : ''}"
        try {
            new groovy.json.JsonSlurper().parse(new URL(url))
        } catch (e) {
            null
        }
    }

    private List<List<Double>> toLatLonList(def coordinates) {
        (coordinates as List).collect { [it[1] as double, it[0] as double] }
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

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
            def intersectionGeom = null
            try { intersectionGeom = geometryToGeoJson(zone.geometry.buffer(0).intersection(route)) }
            catch (ignored) {}
            [
                id          : zone.id,
                name        : zone.name,
                reason      : zone.reason,
                intersection: intersectionGeom
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

        List<RestrictedZone> activeZones = RestrictedZone.findAllByStatus("ACTIVE")

        for (def zone : intersecting) {
            def centroid   = zone.geometry.centroid
            def ringCoords = (zone.geometry.exteriorRing.coordinates as List).init()

            double zoneRadius = ringCoords.collect { coord ->
                double dlat = coord.y - centroid.y
                double dlon = coord.x - centroid.x
                Math.sqrt(dlat * dlat + dlon * dlon)
            }.max() ?: 0.003

            // Phase 1 — boundary sampling: waypoints directly on the polygon ring,
            // offset outward by 50 m–300 m. Closest possible waypoints to the zone
            // boundary → shortest achievable detour.
            [0.0005, 0.001, 0.0015, 0.002, 0.0025, 0.003].each { double mx ->
                List<List<Double>> candidates = []
                ringCoords.each { coord ->
                    double dlat = coord.y - centroid.y
                    double dlon = coord.x - centroid.x
                    double dist = Math.sqrt(dlat * dlat + dlon * dlon)
                    if (dist < 0.0001) return
                    double scale = (dist + mx) / dist
                    candidates << [centroid.y + dlat * scale, centroid.x + dlon * scale]
                }
                (0..<ringCoords.size()).each { i ->
                    def c1 = ringCoords[i]
                    def c2 = ringCoords[(i + 1) % ringCoords.size()]
                    double mlat = (c1.y + c2.y) / 2
                    double mlon = (c1.x + c2.x) / 2
                    double dlat = mlat - centroid.y
                    double dlon = mlon - centroid.x
                    double dist = Math.sqrt(dlat * dlat + dlon * dlon)
                    if (dist < 0.0001) return
                    double scale = (dist + mx) / dist
                    candidates << [centroid.y + dlat * scale, centroid.x + dlon * scale]
                }
                for (def wp : candidates) {
                    def d = callOsrm(osrmBase, [start, wp, end], false)
                    if (!d?.routes) continue
                    if (isClearOfZones(toLatLonList(d.routes[0].geometry.coordinates), activeZones)) {
                        def r = d.routes[0]
                        cleanRoutes << [status: "OK", geometry: r.geometry, distance: r.distance, duration: r.duration]
                    }
                }
            }

            // Phase 2 — angular rings: uniform coverage for all zone shapes,
            // guarantees a solution is always found even if Phase 1 misses corridors.
            int steps = 24  // every 15°
            for (int level = 1; level <= 15; level++) {
                double offset = zoneRadius + level * 0.003
                List<List<Double>> ring = (0..<steps).collect { s ->
                    double angle = s * 2 * Math.PI / steps
                    [centroid.y + offset * Math.cos(angle), centroid.x + offset * Math.sin(angle)]
                }
                for (def wp : ring) {
                    def d = callOsrm(osrmBase, [start, wp, end], false)
                    if (!d?.routes) continue
                    if (isClearOfZones(toLatLonList(d.routes[0].geometry.coordinates), activeZones)) {
                        def r = d.routes[0]
                        cleanRoutes << [status: "OK", geometry: r.geometry, distance: r.distance, duration: r.duration]
                    }
                }
            }

            // Phase 3 — double-waypoint fallback: only when both phases found nothing
            if (cleanRoutes.isEmpty()) {
                for (int level = 1; level <= 10; level++) {
                    double offset = zoneRadius + level * 0.003
                    List<List<Double>> ring = (0..<steps).collect { s ->
                        double angle = s * 2 * Math.PI / steps
                        [centroid.y + offset * Math.cos(angle), centroid.x + offset * Math.sin(angle)]
                    }
                    for (int i = 0; i < steps; i += 2) {
                        for (int gap = 6; gap <= 12; gap += 3) {
                            def w1 = ring[i]
                            def w2 = ring[(i + gap) % steps]
                            def d  = callOsrm(osrmBase, [start, w1, w2, end], false)
                            if (!d?.routes) continue
                            if (isClearOfZones(toLatLonList(d.routes[0].geometry.coordinates), activeZones)) {
                                def r = d.routes[0]
                                cleanRoutes << [status: "OK", geometry: r.geometry, distance: r.distance, duration: r.duration]
                            }
                        }
                    }
                    if (!cleanRoutes.isEmpty()) break
                }
            }
        }

        if (!cleanRoutes) return [status: "NONE_FOUND"]
        cleanRoutes.min { it.distance as double }
    }

    private boolean isClearOfZones(List<List<Double>> coords, List<RestrictedZone> zones) {
        LineString route = buildLineString(coords)
        zones.every { zone -> !zone.geometry?.intersects(route) }
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

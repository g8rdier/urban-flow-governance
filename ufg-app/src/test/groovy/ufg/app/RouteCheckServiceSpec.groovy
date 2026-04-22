package ufg.app

import grails.testing.services.ServiceUnitTest
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Polygon
import org.locationtech.jts.io.WKTWriter
import spock.lang.Specification
import spock.lang.Unroll

class RouteCheckServiceSpec extends Specification implements ServiceUnitTest<RouteCheckService> {

    private GeometryFactory gf = new GeometryFactory()

    private Polygon munichSquare() {
        Coordinate[] coords = [
            new Coordinate(11.5500d, 48.1350d),
            new Coordinate(11.6200d, 48.1350d),
            new Coordinate(11.6200d, 48.1550d),
            new Coordinate(11.5500d, 48.1550d),
            new Coordinate(11.5500d, 48.1350d)
        ] as Coordinate[]
        gf.createPolygon(coords)
    }

    private RestrictedZone zoneWith(Polygon polygon, String status = "ACTIVE") {
        new RestrictedZone(
            name: "Test Zone",
            reason: "Marathon",
            geometryWKT: new WKTWriter().write(polygon),
            startTime: new Date(),
            endTime: new Date(System.currentTimeMillis() + 3600_000L),
            status: status
        )
    }

    @Unroll
    void "point-in-polygon: (#lat, #lng) -> #expected"() {
        given:
        Polygon polygon = munichSquare()

        expect:
        service.isPointInPolygon(lat, lng, polygon) == expected

        where:
        lat     | lng     || expected
        48.145d | 11.585d || true   // inside
        48.100d | 11.585d || false  // south outside
        48.200d | 11.585d || false  // north outside
        48.145d | 11.500d || false  // west outside
        48.145d | 11.700d || false  // east outside
    }

    void "point-in-polygon: null polygon returns false"() {
        expect:
        !service.isPointInPolygon(48.14d, 11.58d, null)
    }

    void "isPointInZone: uses zone geometry"() {
        given:
        RestrictedZone zone = zoneWith(munichSquare())

        expect:
        service.isPointInZone(48.145d, 11.585d, zone)
        !service.isPointInZone(48.100d, 11.585d, zone)
    }

    void "isPointInZone: null zone returns false"() {
        expect:
        !service.isPointInZone(48.14d, 11.58d, null)
    }

    void "routeIntersectsPolygon: route crossing polygon"() {
        given:
        Polygon polygon = munichSquare()
        List<List<Double>> route = [
            [48.120d, 11.450d],  // outside west
            [48.145d, 11.585d],  // inside
            [48.170d, 11.700d]   // outside east
        ]

        expect:
        service.routeIntersectsPolygon(route, polygon)
    }

    void "routeIntersectsPolygon: route entirely outside"() {
        given:
        Polygon polygon = munichSquare()
        List<List<Double>> route = [
            [48.100d, 11.450d],
            [48.100d, 11.700d]
        ]

        expect:
        !service.routeIntersectsPolygon(route, polygon)
    }

    void "routeIntersectsPolygon: route entirely inside still intersects"() {
        given:
        Polygon polygon = munichSquare()
        List<List<Double>> route = [
            [48.140d, 11.560d],
            [48.150d, 11.610d]
        ]

        expect:
        service.routeIntersectsPolygon(route, polygon)
    }

    void "routeIntersectsPolygon: route touching edge counts as intersect"() {
        given:
        Polygon polygon = munichSquare()
        List<List<Double>> route = [
            [48.1350d, 11.500d],  // touches southern edge
            [48.1350d, 11.700d]
        ]

        expect:
        service.routeIntersectsPolygon(route, polygon)
    }

    void "routeIntersectsPolygon: empty or too-short route returns false"() {
        given:
        Polygon polygon = munichSquare()

        expect:
        !service.routeIntersectsPolygon([], polygon)
        !service.routeIntersectsPolygon([[48.14d, 11.58d]], polygon)
    }

    void "routeIntersectsPolygon: null polygon returns false"() {
        expect:
        !service.routeIntersectsPolygon([[48.14d, 11.58d], [48.15d, 11.59d]], null)
    }

    void "routeIntersectsZone: delegates to geometry"() {
        given:
        RestrictedZone zone = zoneWith(munichSquare())
        List<List<Double>> crossing = [[48.120d, 11.450d], [48.170d, 11.700d]]
        List<List<Double>> outside  = [[48.100d, 11.450d], [48.100d, 11.700d]]

        expect:
        service.routeIntersectsZone(crossing, zone)
        !service.routeIntersectsZone(outside, zone)
    }
}

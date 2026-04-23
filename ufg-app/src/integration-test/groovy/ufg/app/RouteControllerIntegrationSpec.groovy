package ufg.app

import grails.testing.mixin.integration.Integration
import grails.gorm.transactions.Rollback
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.io.WKTWriter
import spock.lang.Specification

@Integration
@Rollback
class RouteControllerIntegrationSpec extends Specification {

    RestrictedZoneService restrictedZoneService
    RouteCheckService routeCheckService

    private String munichSquareWKT() {
        def gf = new GeometryFactory()
        Coordinate[] coords = [
            new Coordinate(11.55d, 48.135d),
            new Coordinate(11.62d, 48.135d),
            new Coordinate(11.62d, 48.155d),
            new Coordinate(11.55d, 48.155d),
            new Coordinate(11.55d, 48.135d)
        ] as Coordinate[]
        new WKTWriter().write(gf.createPolygon(coords))
    }

    private RestrictedZone persistZone(String status = "PLANNED") {
        Map params = [
            name       : "Zone ${UUID.randomUUID()}",
            description: "Test",
            reason     : "Marathon",
            geometryWKT: munichSquareWKT(),
            startTime  : new Date(System.currentTimeMillis() + 3600_000L),
            endTime    : new Date(System.currentTimeMillis() + 7200_000L),
            createdBy  : "admin@test.com"
        ]
        RestrictedZone zone = restrictedZoneService.create(params)
        if (status == "ACTIVE") restrictedZoneService.activate(zone.id)
        zone
    }

    void "checkRoute: route with no ACTIVE zones returns OK"() {
        given:
        persistZone("PLANNED")  // planned zone should be ignored

        when:
        Map result = routeCheckService.checkRoute([
            [48.120d, 11.450d],
            [48.170d, 11.700d]
        ])

        then:
        result.status == "OK"
        result.zones.isEmpty()
    }

    void "checkRoute: route crossing ACTIVE zone returns WARNING"() {
        given:
        RestrictedZone active = persistZone("ACTIVE")

        when:
        Map result = routeCheckService.checkRoute([
            [48.120d, 11.450d],
            [48.170d, 11.700d]
        ])

        then:
        result.status == "WARNING"
        result.zones.size() == 1
        result.zones[0].id == active.id
    }

    void "checkRoute: route outside zones returns OK"() {
        given:
        persistZone("ACTIVE")

        when:
        Map result = routeCheckService.checkRoute([
            [48.100d, 11.450d],
            [48.100d, 11.700d]
        ])

        then:
        result.status == "OK"
        result.zones.isEmpty()
    }

    void "checkRoute: multiple ACTIVE zones returns all in WARNING"() {
        given:
        RestrictedZone zone1 = persistZone("ACTIVE")
        RestrictedZone zone2 = persistZone("ACTIVE")

        when:
        Map result = routeCheckService.checkRoute([
            [48.120d, 11.450d],
            [48.170d, 11.700d]
        ])

        then:
        result.status == "WARNING"
        result.zones.size() == 2
        result.zones*.id.containsAll([zone1.id, zone2.id])
    }
}

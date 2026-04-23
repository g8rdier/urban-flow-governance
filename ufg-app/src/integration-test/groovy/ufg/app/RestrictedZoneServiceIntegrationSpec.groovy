package ufg.app

import grails.testing.mixin.integration.Integration
import grails.gorm.transactions.Rollback
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.io.WKTWriter
import spock.lang.Specification

@Integration
@Rollback
class RestrictedZoneServiceIntegrationSpec extends Specification {

    RestrictedZoneService restrictedZoneService

    private String samplePolygonWKT() {
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

    private Map sampleParams() {
        [
            name       : "Marathon 2026",
            description: "Stadtmarathon",
            reason     : "Marathon",
            geometryWKT: samplePolygonWKT(),
            startTime  : new Date(System.currentTimeMillis() + 3600_000L),
            endTime    : new Date(System.currentTimeMillis() + 7200_000L),
            createdBy  : "admin@test.com"
        ]
    }

    void "create persists zone with PLANNED status"() {
        when:
        RestrictedZone zone = restrictedZoneService.create(sampleParams())

        then:
        zone.id != null
        zone.status == "PLANNED"
        zone.createdAt != null
        zone.updatedAt != null
    }

    void "activate transitions PLANNED -> ACTIVE"() {
        given:
        RestrictedZone created = restrictedZoneService.create(sampleParams())

        when:
        RestrictedZone activated = restrictedZoneService.activate(created.id)

        then:
        activated.status == "ACTIVE"
    }

    void "activate from non-PLANNED throws"() {
        given:
        RestrictedZone created = restrictedZoneService.create(sampleParams())
        restrictedZoneService.activate(created.id)

        when:
        restrictedZoneService.activate(created.id)

        then:
        thrown(IllegalStateException)
    }

    void "deactivate transitions ACTIVE -> EXPIRED"() {
        given:
        RestrictedZone created = restrictedZoneService.create(sampleParams())
        restrictedZoneService.activate(created.id)

        when:
        RestrictedZone expired = restrictedZoneService.deactivate(created.id)

        then:
        expired.status == "EXPIRED"
    }

    void "deactivate from PLANNED throws"() {
        given:
        RestrictedZone created = restrictedZoneService.create(sampleParams())

        when:
        restrictedZoneService.deactivate(created.id)

        then:
        thrown(IllegalStateException)
    }

    void "listActive only returns ACTIVE zones"() {
        given:
        RestrictedZone planned = restrictedZoneService.create(sampleParams())
        RestrictedZone active = restrictedZoneService.create(sampleParams())
        restrictedZoneService.activate(active.id)

        when:
        List<RestrictedZone> activeZones = restrictedZoneService.listActive()

        then:
        activeZones*.id.contains(active.id)
        !activeZones*.id.contains(planned.id)
    }

    void "update modifies fields"() {
        given:
        RestrictedZone created = restrictedZoneService.create(sampleParams())

        when:
        RestrictedZone updated = restrictedZoneService.update(created.id, [name: "Renamed"])

        then:
        updated.name == "Renamed"
    }

    void "delete removes the zone"() {
        given:
        RestrictedZone created = restrictedZoneService.create(sampleParams())

        when:
        boolean deleted = restrictedZoneService.delete(created.id)

        then:
        deleted
        restrictedZoneService.get(created.id) == null
    }
}

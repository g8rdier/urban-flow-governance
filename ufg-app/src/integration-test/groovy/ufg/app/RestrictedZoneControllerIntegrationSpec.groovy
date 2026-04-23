package ufg.app

import grails.testing.mixin.integration.Integration
import grails.gorm.transactions.Rollback
import grails.validation.ValidationException
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.io.WKTWriter
import spock.lang.Specification

@Integration
@Rollback
class RestrictedZoneControllerIntegrationSpec extends Specification {

    RestrictedZoneService restrictedZoneService

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

    private Map sampleParams() {
        [
            name       : "Marathon 2026",
            description: "Stadtmarathon",
            reason     : "Marathon",
            geometryWKT: munichSquareWKT(),
            startTime  : new Date(System.currentTimeMillis() + 3600_000L),
            endTime    : new Date(System.currentTimeMillis() + 7200_000L),
            createdBy  : "admin@test.com"
        ]
    }

    void "index returns ApiResponse with zones list"() {
        given:
        RestrictedZone zone = restrictedZoneService.create(sampleParams())

        when:
        List<RestrictedZone> zones = restrictedZoneService.listAll()

        then:
        zones.size() >= 1
        zones*.id.contains(zone.id)
    }

    void "show returns zone when exists"() {
        given:
        RestrictedZone zone = restrictedZoneService.create(sampleParams())

        when:
        RestrictedZone result = restrictedZoneService.get(zone.id)

        then:
        result != null
        result.id == zone.id
        result.name == "Marathon 2026"
    }

    void "show returns null for non-existent zone"() {
        when:
        RestrictedZone result = restrictedZoneService.get(999999L)

        then:
        result == null
    }

    void "save creates zone with PLANNED status"() {
        when:
        RestrictedZone zone = restrictedZoneService.create(sampleParams())

        then:
        zone.id != null
        zone.status == "PLANNED"
        zone.name == "Marathon 2026"
    }

    void "save rejects invalid parameters"() {
        when:
        restrictedZoneService.create([name: "Incomplete"])

        then:
        thrown(ValidationException)
    }

    void "update modifies zone fields"() {
        given:
        RestrictedZone zone = restrictedZoneService.create(sampleParams())

        when:
        RestrictedZone updated = restrictedZoneService.update(zone.id, [name: "Updated Name"])

        then:
        updated.name == "Updated Name"
        updated.id == zone.id
    }

    void "update returns null for non-existent zone"() {
        when:
        RestrictedZone result = restrictedZoneService.update(999999L, [name: "Test"])

        then:
        result == null
    }

    void "delete removes zone"() {
        given:
        RestrictedZone zone = restrictedZoneService.create(sampleParams())

        when:
        boolean deleted = restrictedZoneService.delete(zone.id)

        then:
        deleted
        restrictedZoneService.get(zone.id) == null
    }

    void "delete returns false for non-existent zone"() {
        when:
        boolean deleted = restrictedZoneService.delete(999999L)

        then:
        !deleted
    }

    void "activate transitions PLANNED to ACTIVE"() {
        given:
        RestrictedZone zone = restrictedZoneService.create(sampleParams())

        when:
        RestrictedZone activated = restrictedZoneService.activate(zone.id)

        then:
        activated.status == "ACTIVE"
    }

    void "activate throws on non-PLANNED zone"() {
        given:
        RestrictedZone zone = restrictedZoneService.create(sampleParams())
        restrictedZoneService.activate(zone.id)

        when:
        restrictedZoneService.activate(zone.id)

        then:
        thrown(IllegalStateException)
    }

    void "deactivate transitions ACTIVE to EXPIRED"() {
        given:
        RestrictedZone zone = restrictedZoneService.create(sampleParams())
        restrictedZoneService.activate(zone.id)

        when:
        RestrictedZone deactivated = restrictedZoneService.deactivate(zone.id)

        then:
        deactivated.status == "EXPIRED"
    }

    void "deactivate throws on non-ACTIVE zone"() {
        given:
        RestrictedZone zone = restrictedZoneService.create(sampleParams())

        when:
        restrictedZoneService.deactivate(zone.id)

        then:
        thrown(IllegalStateException)
    }
}

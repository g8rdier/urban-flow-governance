package ufg.app

import grails.converters.JSON
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.io.WKTWriter
import ufg.app.security.RequiredRoles

class RestrictedZoneController {

    static allowedMethods = [
        save: "POST", update: "PUT", delete: "DELETE",
        activate: "PUT", deactivate: "PUT"
    ]
    static responseFormats = ['json']

    RestrictedZoneService restrictedZoneService

    private final GeometryFactory geometryFactory = new GeometryFactory()

    @RequiredRoles(["ADMIN", "NUTZER"])
    def index() {
        List<Map> zones = restrictedZoneService.listAll().collect { toMap(it) }
        render contentType: 'application/json', text: (ApiResponse.success('', [zones: zones]) as JSON)
    }

    @RequiredRoles(["ADMIN", "NUTZER"])
    def active() {
        List<Map> zones = restrictedZoneService.listActive().collect { toMap(it) }
        render contentType: 'application/json', text: (ApiResponse.success('', [zones: zones]) as JSON)
    }

    @RequiredRoles(["ADMIN", "NUTZER"])
    def show(Long id) {
        RestrictedZone zone = restrictedZoneService.get(id)
        if (!zone) {
            render status: 404, contentType: 'application/json', text: (ApiResponse.failure("Zone ${id} not found") as JSON)
            return
        }
        render contentType: 'application/json', text: (ApiResponse.success('', [zone: toMap(zone)]) as JSON)
    }

    @RequiredRoles(["ADMIN"])
    def save() {
        try {
            Map params = parseZonePayload(request.JSON)
            RestrictedZone zone = restrictedZoneService.create(params)
            render status: 201, contentType: 'application/json', text: (ApiResponse.success("Zone ${zone.name} created", [zone: toMap(zone)]) as JSON)
        } catch (IllegalArgumentException e) {
            render status: 400, contentType: 'application/json', text: (ApiResponse.failure(e.message) as JSON)
        }
    }

    @RequiredRoles(["ADMIN"])
    def update(Long id) {
        try {
            Map params = parseZonePayload(request.JSON)
            RestrictedZone zone = restrictedZoneService.update(id, params)
            if (!zone) {
                render status: 404, contentType: 'application/json', text: (ApiResponse.failure("Zone ${id} not found") as JSON)
                return
            }
            render contentType: 'application/json', text: (ApiResponse.success("Zone ${zone.name} updated", [zone: toMap(zone)]) as JSON)
        } catch (IllegalArgumentException e) {
            render status: 400, contentType: 'application/json', text: (ApiResponse.failure(e.message) as JSON)
        }
    }

    @RequiredRoles(["ADMIN"])
    def delete(Long id) {
        boolean ok = restrictedZoneService.delete(id)
        if (!ok) {
            render status: 404, contentType: 'application/json', text: (ApiResponse.failure("Zone ${id} not found") as JSON)
            return
        }
        render status: 200, contentType: 'application/json', text: (ApiResponse.success('Zone deleted') as JSON)
    }

    @RequiredRoles(["ADMIN"])
    def activate(Long id) {
        try {
            RestrictedZone zone = restrictedZoneService.activate(id)
            if (!zone) {
                render status: 404, contentType: 'application/json', text: (ApiResponse.failure("Zone ${id} not found") as JSON)
                return
            }
            render contentType: 'application/json', text: (ApiResponse.success("Zone ${zone.name} activated", [zone: toMap(zone)]) as JSON)
        } catch (IllegalStateException e) {
            render status: 409, contentType: 'application/json', text: (ApiResponse.failure(e.message) as JSON)
        }
    }

    @RequiredRoles(["ADMIN"])
    def deactivate(Long id) {
        try {
            RestrictedZone zone = restrictedZoneService.deactivate(id)
            if (!zone) {
                render status: 404, contentType: 'application/json', text: (ApiResponse.failure("Zone ${id} not found") as JSON)
                return
            }
            render contentType: 'application/json', text: (ApiResponse.success("Zone ${zone.name} deactivated", [zone: toMap(zone)]) as JSON)
        } catch (IllegalStateException e) {
            render status: 409, contentType: 'application/json', text: (ApiResponse.failure(e.message) as JSON)
        }
    }

    private Map parseZonePayload(def json) {
        if (!json) throw new IllegalArgumentException("Request body required")

        Map params = [
            name       : json.name,
            description: json.description,
            reason     : json.reason,
            startTime  : parseDate(json.startTime),
            endTime    : parseDate(json.endTime),
            createdBy  : json.createdBy
        ]
        if (json.status) params.status = json.status
        if (json.geometry) params.geometryWKT = geoJsonToWKT(json.geometry)
        params
    }

    private String geoJsonToWKT(def geometry) {
        if (geometry.type != "Polygon") {
            throw new IllegalArgumentException("Geometry must be of type Polygon")
        }
        def rings = geometry.coordinates
        if (!rings || rings.isEmpty()) {
            throw new IllegalArgumentException("Polygon must have at least one ring")
        }
        Coordinate[] coords = rings[0].collect { new Coordinate(it[0] as double, it[1] as double) } as Coordinate[]
        def polygon = geometryFactory.createPolygon(coords)
        new WKTWriter().write(polygon)
    }

    private Date parseDate(String input) {
        if (!input) return null
        Date.parse("yyyy-MM-dd'T'HH:mm:ss'Z'", input)
    }

    private Map toMap(RestrictedZone zone) {
        [
            id         : zone.id,
            name       : zone.name,
            description: zone.description,
            reason     : zone.reason,
            geometry   : geometryToGeoJson(zone),
            startTime  : zone.startTime?.format("yyyy-MM-dd'T'HH:mm:ss'Z'"),
            endTime    : zone.endTime?.format("yyyy-MM-dd'T'HH:mm:ss'Z'"),
            status     : zone.status,
            createdAt  : zone.createdAt?.format("yyyy-MM-dd'T'HH:mm:ss'Z'"),
            updatedAt  : zone.updatedAt?.format("yyyy-MM-dd'T'HH:mm:ss'Z'"),
            createdBy  : zone.createdBy
        ]
    }

    private Map geometryToGeoJson(RestrictedZone zone) {
        def polygon = zone.geometry
        if (!polygon) return null
        [
            type       : "Polygon",
            coordinates: [polygon.coordinates.collect { [it.x, it.y] }]
        ]
    }
}

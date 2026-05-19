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
        } catch (Exception e) {
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
        } catch (Exception e) {
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
            startTime  : parseDate(json.startTime as String),
            endTime    : parseDate(json.endTime as String),
            createdBy  : json.createdBy
        ]
        if (json.status) params.status = json.status
        def geom = json.geometry
        if (geom && geom != 'null') params.geometryWKT = geoJsonToWKT(geom)
        params
    }

    private String geoJsonToWKT(def geometry) {
        if (!geometry) throw new IllegalArgumentException("Geometry is required")
        if (geometry.type != "Polygon") {
            throw new IllegalArgumentException("Geometry must be of type Polygon")
        }
        def rawRings = geometry.coordinates
        if (!rawRings) {
            throw new IllegalArgumentException("Polygon must have at least one ring")
        }
        List rings = rawRings as List
        if (rings.isEmpty()) {
            throw new IllegalArgumentException("Polygon must have at least one ring")
        }
        def outerRing = rings[0]
        if (!outerRing) {
            throw new IllegalArgumentException("Outer ring must not be empty or null")
        }
        Coordinate[] coords = (outerRing as List).collect { coord ->
            if (coord == null) throw new IllegalArgumentException("Null coordinate in polygon ring")
            List pair = coord as List
            if (pair.size() < 2) throw new IllegalArgumentException("Coordinate pair must have at least 2 values")
            new Coordinate(pair[0] as double, pair[1] as double)
        } as Coordinate[]
        def polygon = geometryFactory.createPolygon(coords)
        new WKTWriter().write(polygon)
    }

    private Date parseDate(String input) {
        if (!input) return null
        new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").parse(input)
    }

    private Map toMap(RestrictedZone zone) {
        def fmt = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
        [
            id         : zone.id,
            name       : zone.name,
            description: zone.description,
            reason     : zone.reason,
            geometry   : geometryToGeoJson(zone),
            startTime  : zone.startTime ? fmt.format(zone.startTime) : null,
            endTime    : zone.endTime ? fmt.format(zone.endTime) : null,
            status     : zone.status,
            createdAt  : zone.createdAt ? fmt.format(zone.createdAt) : null,
            updatedAt  : zone.updatedAt ? fmt.format(zone.updatedAt) : null,
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


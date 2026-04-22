package ufg.app

import grails.converters.JSON
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.io.WKTWriter

class RestrictedZoneController {

    static allowedMethods = [
        save: "POST", update: "PUT", delete: "DELETE",
        activate: "PUT", deactivate: "PUT"
    ]
    static responseFormats = ['json']

    RestrictedZoneService restrictedZoneService

    private final GeometryFactory geometryFactory = new GeometryFactory()

    def index() {
        render restrictedZoneService.listAll().collect { toMap(it) } as JSON
    }

    def show(Long id) {
        RestrictedZone zone = restrictedZoneService.get(id)
        if (!zone) { render status: 404; return }
        render toMap(zone) as JSON
    }

    def save() {
        try {
            Map params = parseZonePayload(request.JSON)
            RestrictedZone zone = restrictedZoneService.create(params)
            response.status = 201
            render toMap(zone) as JSON
        } catch (IllegalArgumentException e) {
            response.status = 400
            render([error: e.message] as JSON)
        }
    }

    def update(Long id) {
        try {
            Map params = parseZonePayload(request.JSON)
            RestrictedZone zone = restrictedZoneService.update(id, params)
            if (!zone) { render status: 404; return }
            render toMap(zone) as JSON
        } catch (IllegalArgumentException e) {
            response.status = 400
            render([error: e.message] as JSON)
        }
    }

    def delete(Long id) {
        boolean ok = restrictedZoneService.delete(id)
        render status: ok ? 204 : 404
    }

    def activate(Long id) {
        try {
            RestrictedZone zone = restrictedZoneService.activate(id)
            if (!zone) { render status: 404; return }
            render toMap(zone) as JSON
        } catch (IllegalStateException e) {
            response.status = 409
            render([error: e.message] as JSON)
        }
    }

    def deactivate(Long id) {
        try {
            RestrictedZone zone = restrictedZoneService.deactivate(id)
            if (!zone) { render status: 404; return }
            render toMap(zone) as JSON
        } catch (IllegalStateException e) {
            response.status = 409
            render([error: e.message] as JSON)
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

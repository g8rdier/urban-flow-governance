package ufg.app

import org.locationtech.jts.geom.Polygon
import org.locationtech.jts.io.WKTReader
import org.locationtech.jts.io.WKTWriter

class RestrictedZone {

    static final List<String> VALID_STATUSES = ["PLANNED", "ACTIVE", "EXPIRED"]
    static final List<String> VALID_REASONS = ["Marathon", "Baustelle", "Umweltalarm"]

    String name
    String description
    String reason
    String geometryWKT
    Date startTime
    Date endTime
    String status = "PLANNED"
    Date createdAt
    Date updatedAt
    String createdBy

    static constraints = {
        name blank: false, maxSize: 255
        description nullable: true
        reason blank: false, inList: VALID_REASONS
        geometryWKT blank: false, validator: { val ->
            try {
                def geom = new WKTReader().read(val)
                return geom instanceof Polygon
            } catch (Exception ignored) {
                return false
            }
        }
        startTime nullable: false
        endTime nullable: false, validator: { val, obj -> val > obj.startTime }
        status inList: VALID_STATUSES
        createdAt nullable: true
        updatedAt nullable: true
        createdBy nullable: true
    }

    static mapping = {
        table "restricted_zone"
        id generator: 'identity'
        geometryWKT type: 'text'
    }

    static transients = ['geometry']

    Polygon getGeometry() {
        geometryWKT ? (Polygon) new WKTReader().read(geometryWKT) : null
    }

    void setGeometry(Polygon polygon) {
        geometryWKT = polygon ? new WKTWriter().write(polygon) : null
    }

    def beforeInsert() {
        def now = new Date()
        createdAt = now
        updatedAt = now
    }

    def beforeUpdate() {
        updatedAt = new Date()
    }
}

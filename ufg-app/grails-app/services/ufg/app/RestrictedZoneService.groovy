package ufg.app

import grails.gorm.transactions.Transactional

@Transactional
class RestrictedZoneService {

    RestrictedZone create(Map params) {
        RestrictedZone zone = new RestrictedZone(params)
        zone.save(failOnError: true, flush: true)
        zone
    }

    RestrictedZone update(Long id, Map params) {
        RestrictedZone zone = RestrictedZone.get(id)
        if (!zone) return null
        zone.properties = params
        zone.save(failOnError: true, flush: true)
        zone
    }

    boolean delete(Long id) {
        RestrictedZone zone = RestrictedZone.get(id)
        if (!zone) return false
        zone.delete(flush: true)
        true
    }

    @Transactional(readOnly = true)
    RestrictedZone get(Long id) {
        RestrictedZone.get(id)
    }

    @Transactional(readOnly = true)
    List<RestrictedZone> listAll() {
        RestrictedZone.list()
    }

    @Transactional(readOnly = true)
    List<RestrictedZone> listActive() {
        RestrictedZone.findAllByStatus("ACTIVE")
    }

    RestrictedZone activate(Long id) {
        transition(id, "PLANNED", "ACTIVE")
    }

    RestrictedZone deactivate(Long id) {
        RestrictedZone zone = RestrictedZone.get(id)
        if (!zone) return null
        if (zone.status != "ACTIVE") {
            throw new IllegalStateException("Zone $id is not active (status: ${zone.status})")
        }
        zone.status = "EXPIRED"
        zone.save(failOnError: true, flush: true)
        zone
    }

    private RestrictedZone transition(Long id, String from, String to) {
        RestrictedZone zone = RestrictedZone.get(id)
        if (!zone) return null
        if (zone.status != from) {
            throw new IllegalStateException("Zone $id cannot transition from ${zone.status} to $to")
        }
        zone.status = to
        zone.save(failOnError: true, flush: true)
        zone
    }
}

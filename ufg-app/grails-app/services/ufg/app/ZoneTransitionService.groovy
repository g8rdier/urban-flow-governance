package ufg.app

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.springframework.scheduling.annotation.Scheduled

@Slf4j
class ZoneTransitionService {

    @Scheduled(fixedDelay = 60000L)
    @Transactional
    void processTransitions() {
        Date now = new Date()

        List<RestrictedZone> toActivate = RestrictedZone.findAllByStatusAndStartTimeLessThanEquals("PLANNED", now)
        toActivate.each { zone ->
            zone.status = "ACTIVE"
            zone.save(flush: true)
            log.info("Zone ${zone.id} (${zone.name}) auto-activated")
        }

        List<RestrictedZone> toExpire = RestrictedZone.findAllByStatusAndEndTimeLessThanEquals("ACTIVE", now)
        toExpire.each { zone ->
            zone.status = "EXPIRED"
            zone.save(flush: true)
            log.info("Zone ${zone.id} (${zone.name}) auto-expired")
        }
    }
}

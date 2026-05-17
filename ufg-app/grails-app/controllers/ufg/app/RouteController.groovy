package ufg.app

import grails.converters.JSON
import ufg.app.security.RequiredRoles

class RouteController {

    static allowedMethods = [check: "POST", alternative: "POST"]
    static responseFormats = ['json']

    RouteCheckService routeCheckService

    @RequiredRoles(["ADMIN", "NUTZER"])
    def check() {
        def json = request.JSON
        if (!json?.route) {
            render status: 400, contentType: 'application/json', text: (ApiResponse.failure("Field 'route' required (array of [lat, lng] pairs)") as JSON)
            return
        }

        List<List<Double>> route = (json.route as List).collect { pair ->
            [(pair[0] as double), (pair[1] as double)]
        }

        if (route.size() < 2) {
            render status: 400, contentType: 'application/json', text: (ApiResponse.failure("Route must contain at least two coordinates") as JSON)
            return
        }

        Map result = routeCheckService.checkRoute(route)
        render contentType: 'application/json', text: (ApiResponse.success('', result) as JSON)
    }

    @RequiredRoles(["ADMIN", "NUTZER"])
    def alternative() {
        def json = request.JSON
        if (!json?.start || !json?.end) {
            render status: 400, contentType: 'application/json', text: (ApiResponse.failure("Fields 'start' and 'end' required ([lat, lng])") as JSON)
            return
        }

        List<Double> start = [json.start[0] as double, json.start[1] as double]
        List<Double> end   = [json.end[0]   as double, json.end[1]   as double]

        Map result = routeCheckService.findAlternativeRoute(start, end)
        render contentType: 'application/json', text: (ApiResponse.success('', result) as JSON)
    }
}

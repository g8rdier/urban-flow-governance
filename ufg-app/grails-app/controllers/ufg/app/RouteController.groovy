package ufg.app

import grails.converters.JSON

class RouteController {

    static allowedMethods = [check: "POST"]
    static responseFormats = ['json']

    RouteCheckService routeCheckService

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
}

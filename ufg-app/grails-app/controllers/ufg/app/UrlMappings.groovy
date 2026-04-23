package ufg.app

class UrlMappings {

    static mappings = {
        "/$controller/$action?/$id?(.$format)?"{
            constraints {
                // apply constraints here
            }
        }

        group "/api", {
            "/zones"(controller: "restrictedZone") {
                action = [GET: "index", POST: "save"]
            }
            "/zones/$id"(controller: "restrictedZone") {
                action = [GET: "show", PUT: "update", DELETE: "delete"]
            }
            "/zones/$id/activate"(controller: "restrictedZone") {
                action = [PUT: "activate"]
            }
            "/zones/$id/deactivate"(controller: "restrictedZone") {
                action = [PUT: "deactivate"]
            }

            "/route/check"(controller: "route") {
                action = [POST: "check"]
            }
        }

        "/"(view:"/index")
        "500"(view:'/error')
        "404"(view:'/notFound')
    }
}

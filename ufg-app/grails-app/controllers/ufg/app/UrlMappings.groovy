package ufg.app

class UrlMappings {

    static mappings = {
        "/$controller/$action?/$id?(.$format)?"{
            constraints {
                // apply constraints here
            }
        }

        "/zones"(controller: "restrictedZone") {
            action = [GET: "index", POST: "save"]
        }
        "/zones/$id"(controller: "restrictedZone") {
            action = [GET: "show", PUT: "update", DELETE: "delete"]
        }
        "/zones/$id/activate"(controller: "restrictedZone", action: "activate", method: "PUT")
        "/zones/$id/deactivate"(controller: "restrictedZone", action: "deactivate", method: "PUT")

        "/route/check"(controller: "route", action: "check", method: "POST")

        "/"(view:"/index")
        "500"(view:'/error')
        "404"(view:'/notFound')
    }
}

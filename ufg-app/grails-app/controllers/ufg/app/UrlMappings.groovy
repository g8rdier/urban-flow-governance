package ufg.app

class UrlMappings {

    static mappings = {
        group "/api", {
            "/session"(controller: "userManager") {
                action = [GET: "getUserinfo", POST: "login", DELETE: "logout"]
            }

            "/users"(controller: "userManager") {
                action = [POST: "create"]
            }

            "/users/$id"(controller: "userManager") {
                action = [PUT: "update", PATCH: "update", DELETE: "delete"]
            }

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

        group "/greeting", {
            "/"(controller: "greeting") {
                action = "index"
            }

            "/admin"(controller: "greeting") {
                action = "admin"
            }

            "/nutzer"(controller: "greeting") {
                action = "nutzer"
            }

            "/both"(controller: "greeting") {
                action = "both"
            }
        }

        "/"(view:"/index")
        "500"(view:'/error')
        "404"(view:'/notFound')
    }
}

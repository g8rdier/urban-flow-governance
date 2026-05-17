package ufg.app

class UrlMappings {

    static mappings = {
        group "/api", {
            "/session"(controller: "userManager") {
                action = [GET: "getSessioninfo", POST: "login", DELETE: "logout", OPTIONS: "getSessioninfo"]
            }

            "/users"(controller: "userManager") {
                action = [POST: "create", GET: "list", OPTIONS: "list"]
            }

            "/users/$id"(controller: "userManager") {
                action = [PUT: "update", PATCH: "update", DELETE: "delete", OPTIONS: "list"]
            }

            "/zones"(controller: "restrictedZone") {
                action = [GET: "index", POST: "save", OPTIONS: "index"]
            }

            "/zones/active"(controller: "restrictedZone") {
                action = [GET: "active", OPTIONS: "active"]
            }

            "/zones/$id"(controller: "restrictedZone") {
                action = [GET: "show", PUT: "update", DELETE: "delete", OPTIONS: "show"]
            }

            "/zones/$id/activate"(controller: "restrictedZone") {
                action = [PUT: "activate", OPTIONS: "activate"]
            }

            "/zones/$id/deactivate"(controller: "restrictedZone") {
                action = [PUT: "deactivate", OPTIONS: "deactivate"]
            }

            "/route/check"(controller: "route") {
                action = [POST: "check", OPTIONS: "check"]
            }

            "/route/alternative"(controller: "route") {
                action = [POST: "alternative", OPTIONS: "alternative"]
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

        "/openapi.yaml"(controller: "swagger", action: "spec")
        "/swagger-ui"(controller: "swagger", action: "ui")
        "/swagger-ui/"(controller: "swagger", action: "ui")
        "/swagger-ui/index.html"(controller: "swagger", action: "ui")

        "/"(view:"/index")
        "500"(view:'/error')
        "404"(view:'/notFound')
    }
}

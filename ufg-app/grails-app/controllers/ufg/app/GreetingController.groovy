package ufg.app

import ufg.app.security.RequiredRoles

class GreetingController {

    UserManagerService userManagerService

    // curl "http://localhost:8080/greeting/index"
    // curl "http://localhost:8080/greeting/index" -H "Authorization: Bearer <TOKEN>"
    def index() {
        String authorization = request.getHeader('Authorization')
        String token = BearerTokenUtil.extractBearerToken(authorization)

        if (token) {
            Map tokenCheck = userManagerService.validateToken(token)
            if (tokenCheck.valid) {
                User user = tokenCheck.user as User
                render "User: ${user.username} (${user.role}) is logged in"
                return
            }
        }

        render "No user logged in"
    }

    // curl "http://localhost:8080/greeting/admin" -H "Authorization: Bearer <TOKEN>"
    @RequiredRoles(["ADMIN"])
    def admin() {
        render "Admin area. You have the ADMIN role."
    }

    // curl "http://localhost:8080/greeting/nutzer" -H "Authorization: Bearer <TOKEN>"
    @RequiredRoles(["NUTZER"])
    def nutzer() {
        render "Nutzer area. You have the NUTZER role."
    }

    // curl "http://localhost:8080/greeting/both" -H "Authorization: Bearer <TOKEN>"
    @RequiredRoles(["ADMIN", "NUTZER"])
    def both() {
        render "Both area. You either have the NUTZER or ADMIN role."
    }

}

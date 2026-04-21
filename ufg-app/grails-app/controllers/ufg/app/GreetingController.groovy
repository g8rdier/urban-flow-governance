package ufg.app

import ufg.app.security.RequiredRoles

class GreetingController {

    def index() {
        Long userId = session.userId as Long
        def sk = session.sessionKey

        if (userId) {
            def user = User.get(userId)
            if (user) {
                render "User: ${user.username} (${user.role}) logged in at ${sk?.creation_time}"
                return
            }
        }

        render "No user logged in"
    }

    @RequiredRoles(["ADMIN"])
    def admin() {
        render "Admin area. You have the ADMIN role."
    }

    @RequiredRoles(["NUTZER"])
    def nutzer() {
        render "Nutzer area. You have the NUTZER role."
    }

    @RequiredRoles(["ADMIN", "NUTZER"])
    def both() {
        render "Both area. You either have the NUTZER or ADMIN role."
    }

}

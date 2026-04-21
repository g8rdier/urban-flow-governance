package ufg.app

class RoleCheckInterceptor {

    // Dieses Mapping nutzt den controllerName:actionName als Key und
    // die Rolle als Value.
    // So kann man neue Rollen bzw. die neue Pfade einfach hinzufügen ohne etwas
    // an der Logik zu aendern.
    private static final Map<String, String> REQUIRED_ROLE_BY_ENDPOINT = [
        "greeting:admin": "ADMIN",
        "greeting:nutzer": "NUTZER"
    ]

    RoleCheckInterceptor() {
        matchAll()
    }

    boolean before() {
        String endpointKey = "${controllerName}:${actionName}"
        String requiredRole = REQUIRED_ROLE_BY_ENDPOINT[endpointKey]

        if (!requiredRole) {
            return true
        }

        Long userId = session.userId as Long
        if (!userId) {
            render status: 401, text: "Login required"
            return false
        }

        def user = User.get(userId)
        if (!user) {
            session.invalidate()
            render status: 401, text: "Invalid session"
            return false
        }

        if (user.role != requiredRole) {
            render status: 403, text: "Role ${requiredRole} required"
            return false
        }

        true
    }
}

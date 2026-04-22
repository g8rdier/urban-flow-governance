package ufg.app

import grails.converters.JSON
import grails.util.Holders
import ufg.app.security.RequiredRoles

class RoleCheckInterceptor {

    RoleCheckInterceptor() {
        matchAll()
    }

    boolean before() {
        List<String> requiredRoles = resolveRequiredRoles(controllerName, actionName)

        if (!requiredRoles) {
            return true
        }

        Long userId = session.userId as Long
        if (!userId) {
            render status: 401, contentType: 'application/json', text: (ApiResponse.failure('Login required') as JSON)
            return false
        }

        def user = User.get(userId)
        if (!user) {
            session.invalidate()
            render status: 401, contentType: 'application/json', text: (ApiResponse.failure('Invalid session') as JSON)
            return false
        }

        if (!requiredRoles.contains(user.role)) {
            render status: 403, contentType: 'application/json', text: (ApiResponse.failure("Role ${requiredRoles.join(' or ')} required") as JSON)
            return false
        }

        true
    }

    private List<String> resolveRequiredRoles(String currentControllerName, String currentActionName) {
        def controllerArtefact = Holders.grailsApplication?.getArtefactByLogicalPropertyName("Controller", currentControllerName)
        Class controllerClass = controllerArtefact?.clazz
        if (!controllerClass) {
            return null
        }

        def actionMethod = controllerClass.declaredMethods.find { it.name == currentActionName }
        if (!actionMethod) {
            return null
        }

        RequiredRoles annotation = actionMethod.getAnnotation(RequiredRoles)
        annotation ? annotation.value().toList() : null
    }
}

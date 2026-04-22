package ufg.app

import java.time.LocalDateTime
import grails.converters.JSON
import ufg.app.security.RequiredRoles

class UserManagerController {

    UserManagerService userManagerService

    // Wird bereits in UrlMappings abgefangen,
    // sorgt hier aber fuer korrekte 405 Antworten
    static allowedMethods = [
        login: "POST",
        logout: "DELETE",
        create: "POST",
        update: ["PUT", "PATCH"],
        delete: "DELETE"
    ]

    // curl -X POST "http://localhost:8080/api/session" -d "username=admin&password=secret"
    def login(String username, String password) {
        if (!username || !password) {
            render status: 400, contentType: 'application/json', text: (ApiResponse.failure('Provide username and password') as JSON)
            return
        }

        Map result = userManagerService.login(username, password)
        if (!result.success) {
            render status: result.status, contentType: 'application/json', text: (result.response as JSON)
            return
        }

        def user = result.user
        session.userId = user.id
        session.sessionKey = ["username": user.username, "role": user.role, "creation_time": LocalDateTime.now()]
        render status: result.status, contentType: 'application/json', text: (result.response as JSON)
    }

    // curl -X DELETE "http://localhost:8080/api/session"
    def logout() {
        session.userId = null
        session.sessionKey = null
        render status: 200, contentType: 'application/json', text: (ApiResponse.success('Logout successful') as JSON)
    }

    // POST /api/users
    // curl -X POST "http://localhost:8080/api/users" -d "username=max&password=secret&role=NUTZER"
    @RequiredRoles(["ADMIN"])
    def create(String username, String password, String role) {
        Map result = userManagerService.createUser(username, password, role)
        if (!result.success) {
            render status: result.status, contentType: 'application/json', text: (result.response as JSON)
            return
        }

        render status: result.status, contentType: 'application/json', text: (result.response as JSON)
    }

    // PUT/PATCH /api/users/{id}
    // curl -X PUT "http://localhost:8080/api/users/1" -d "username=max2&role=ADMIN"
    @RequiredRoles(["ADMIN"])
    def update(Long id, String username, String password, String role) {
        Map result = userManagerService.updateUser(id, username, password, role)
        if (!result.success) {
            render status: result.status, contentType: 'application/json', text: (result.response as JSON)
            return
        }

        render status: result.status, contentType: 'application/json', text: (result.response as JSON)
    }

    // DELETE /api/users/{id}
    // curl -X DELETE "http://localhost:8080/api/users/1"
    @RequiredRoles(["ADMIN"])
    def delete(Long id) {
        Map result = userManagerService.deleteUser(id)
        if (!result.success) {
            render status: result.status, contentType: 'application/json', text: (result.response as JSON)
            return
        }

        render status: result.status, contentType: 'application/json', text: (result.response as JSON)
    }

}

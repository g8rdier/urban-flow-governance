package ufg.app

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

    // curl.exe -X POST "http://localhost:8080/api/session" -d "username=admin&password=adminpass"
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
        String token = userManagerService.createTokenForUser(user)
        render status: 200, contentType: 'application/json', text: (ApiResponse.success('Login successful', [
            token: token,
            token_type: 'Bearer',
            expires_in_minutes: UserManagerService.TOKEN_TTL_MINUTES
        ]) as JSON)
    }

    // curl.exe -X GET "http://localhost:8080/api/session" -H "Authorization: Bearer <TOKEN>"
    @RequiredRoles(["ADMIN", "NUTZER"])
    def getSessioninfo() {
        String authorization = request.getHeader('Authorization')
        String token = BearerTokenUtil.extractBearerToken(authorization)

        if (!token) {
            render status: 401, contentType: 'application/json', text: (ApiResponse.failure('Login required') as JSON)
            return
        }

        Map sessionInfo = userManagerService.getSessionInfo(token)
        if (!sessionInfo) {
            render status: 401, contentType: 'application/json', text: (ApiResponse.failure('Invalid token') as JSON)
            return
        }

        render status: 200, contentType: 'application/json', text: (ApiResponse.success('User info loaded', sessionInfo) as JSON)
    }

    // curl.exe -X DELETE "http://localhost:8080/api/session" -H "Authorization: Bearer <TOKEN>"
    @RequiredRoles(["ADMIN", "NUTZER"])
    def logout() {
        String authorization = request.getHeader('Authorization')
        String token = BearerTokenUtil.extractBearerToken(authorization)

        if (!token) {
            render status: 401, contentType: 'application/json', text: (ApiResponse.failure('Login required') as JSON)
            return
        }

        def user = userManagerService.getUserByToken(token)
        if (!user) {
            render status: 401, contentType: 'application/json', text: (ApiResponse.failure('Invalid token') as JSON)
            return
        }

        userManagerService.deleteToken(token)
        render status: 200, contentType: 'application/json', text: (ApiResponse.success('Logout successful') as JSON)
    }

    // POST /api/users
    // curl.exe -X POST "http://localhost:8080/api/users" -d "username=max&password=secret&role=NUTZER"
    def create(String username, String password, String role) {
        String authorization = request.getHeader('Authorization')
        String token = BearerTokenUtil.extractBearerToken(authorization)

        Map result = userManagerService.createUser(username, password, role, token)
        if (!result.success) {
            render status: result.status, contentType: 'application/json', text: (result.response as JSON)
            return
        }

        render status: result.status, contentType: 'application/json', text: (result.response as JSON)
    }

    // PUT/PATCH /api/users/{id}
    // curl.exe -X PUT "http://localhost:8080/api/users/1" -H "Authorization: Bearer <TOKEN>" -d "username=max2&role=ADMIN"
    @RequiredRoles(["ADMIN", "NUTZER"])
    def update(Long id, String username, String password, String role) {
        String authorization = request.getHeader('Authorization')
        String token = BearerTokenUtil.extractBearerToken(authorization)

        Map result = userManagerService.updateUser(id, username, password, role, token)
        if (!result.success) {
            render status: result.status, contentType: 'application/json', text: (result.response as JSON)
            return
        }

        render status: result.status, contentType: 'application/json', text: (result.response as JSON)
    }

    // DELETE /api/users/{id}
    // curl.exe -X DELETE "http://localhost:8080/api/users/1" -H "Authorization: Bearer <TOKEN>"
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

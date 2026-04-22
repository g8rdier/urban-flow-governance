package ufg.app

import java.time.LocalDateTime
import ufg.app.security.RequiredRoles

class UserManagerController {

    static allowedMethods = [
        login: "GET",
        logout: "GET",
        create: "GET",
        update: "GET",
        delete: "GET"
    ]

    def login(String username, String password) {
        if (!username || !password) {
            render status: 400, text: "Provide username and password"
            return
        }

        def user = User.findByUsernameAndPassword(username, password)
        if (!user) {
            render status: 401, text: "Invalid credentials"
            return
        }

        session.userId = user.id
        session.sessionKey = ["username": user.username, "role": user.role, "creation_time": LocalDateTime.now()]
        redirect controller: "greeting", action: "index"
    }

    def logout() {
        session.userId = null
        session.sessionKey = null
        redirect controller: "greeting", action: "index"
    }

    // POST /userManager/create
    @RequiredRoles(["ADMIN"])
    def create(String username, String password, String role) {
        String requestedRole = role ?: "NUTZER"

        User user = new User(username: username, password: password, role: requestedRole)
        if (!user.save(flush: true)) {
            render status: 400, text: user.errors.allErrors.collect { it.defaultMessage }.join(", ")
            return
        }

        render status: 201, text: "User ${user.username} created"
    }

    // PUT /userManager/update?id=<id>
    @RequiredRoles(["ADMIN"])
    def update(Long id, String username, String password, String role) {
        User user = User.get(id)
        if (!user) {
            render status: 404, text: "User not found"
            return
        }

        if (username) {
            user.username = username
        }

        if (password) {
            user.password = password
        }

        if (role) {
            user.role = role
        }

        if (!user.save(flush: true)) {
            render status: 400, text: user.errors.allErrors.collect { it.defaultMessage }.join(", ")
            return
        }

        render status: 200, text: "User ${user.username} updated"
    }

    // DELETE /userManager/delete?id=<id>
    @RequiredRoles(["ADMIN"])
    def delete(Long id) {
        User user = User.get(id)
        if (!user) {
            render status: 404, text: "User not found"
            return
        }

        user.delete(flush: true)
        render status: 200, text: "User deleted"
    }

}

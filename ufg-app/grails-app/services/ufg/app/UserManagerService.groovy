package ufg.app

import grails.gorm.transactions.Transactional

@Transactional
class UserManagerService {

    Map login(String username, String password) {
        def user = User.findByUsernameAndPassword(username, password)
        if (!user) {
            return failure(401, 'Invalid credentials')
        }

        success(200, 'Login successful', [user: user])
    }

    Map createUser(String username, String password, String role) {
        String requestedRole = role ?: 'NUTZER'

        User user = new User(username: username, password: password, role: requestedRole)
        if (!user.save(flush: true)) {
            return failure(400, user.errors.allErrors.collect { it.defaultMessage }.join(', '))
        }

        success(201, "User ${user.username} created")
    }

    Map updateUser(Long id, String username, String password, String role) {
        User user = User.get(id)
        if (!user) {
            return failure(404, 'User not found')
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
            return failure(400, user.errors.allErrors.collect { it.defaultMessage }.join(', '))
        }

        success(200, "User ${user.username} updated")
    }

    Map deleteUser(Long id) {
        User user = User.get(id)
        if (!user) {
            return failure(404, 'User not found')
        }

        user.delete(flush: true)
        success(200, 'User deleted')
    }

    private Map success(int status, String msg, Map extra = [:]) {
        [success: true, status: status, response: ApiResponse.success(msg)] + extra
    }

    private Map failure(int status, String msg) {
        [success: false, status: status, response: ApiResponse.failure(msg)]
    }
}
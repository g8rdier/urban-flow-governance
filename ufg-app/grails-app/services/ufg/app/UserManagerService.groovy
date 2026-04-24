package ufg.app

import grails.gorm.transactions.Transactional
import java.util.UUID

@Transactional
class UserManagerService {

    static final long TOKEN_TTL_MINUTES = resolveTokenTtlMinutes()
    PasswordHashService passwordHashService

    Map login(String username, String password) {
        User user = User.findByUsername(username)
        if (!user || !user.credential) {
            return failure(401, 'Invalid credentials')
        }

        boolean valid = passwordHashService.verifyPassword(
            password,
            user.credential.salt,
            user.credential.passwordHash
        )
        if (!valid) {
            return failure(401, 'Invalid credentials')
        }

        success(200, 'Login successful', [user: user])
    }

    String createTokenForUser(User user) {
        // Beschraenkt den Zugriff auf eine einzige Session
        AuthToken.executeUpdate('delete AuthToken t where t.user = :user', [user: user])

        AuthToken authToken = new AuthToken(
            token: UUID.randomUUID().toString(),
            createdAt: new Date(),
            user: user
        )
        authToken.save(failOnError: true, flush: true)
        authToken.token
    }

    Map validateToken(String tokenValue) {
        if (!tokenValue) {
            return [valid: false, message: 'Login required']
        }

        AuthToken authToken = AuthToken.findByToken(tokenValue)
        if (!authToken) {
            return [valid: false, message: 'Invalid token']
        }

        if (!authToken.user) {
            authToken.delete(flush: true)
            return [valid: false, message: 'Invalid token']
        }

        if (minutesSince(authToken.createdAt) > TOKEN_TTL_MINUTES) {
            authToken.delete(flush: true)
            return [valid: false, message: 'Session expired. Please login again.']
        }

        [valid: true, user: authToken.user]
    }

    void deleteToken(String tokenValue) {
        AuthToken authToken = AuthToken.findByToken(tokenValue)
        if (authToken) {
            authToken.delete(flush: true)
        }
    }

    Map createUser(String username, String password, String role) {
        String requestedRole = role ?: 'NUTZER'

        User user = new User(username: username, role: requestedRole)
        if (!user.save(flush: true)) {
            return failure(400, user.errors.allErrors.collect { it.defaultMessage }.join(', '))
        }

        String salt = passwordHashService.generateSalt()
        String passwordHash = passwordHashService.hashPassword(password, salt)
        UserCredential credential = new UserCredential(
            user: user,
            salt: salt,
            passwordHash: passwordHash
        )
        if (!credential.save(flush: true)) {
            transactionStatus.setRollbackOnly()
            return failure(400, credential.errors.allErrors.collect { it.defaultMessage }.join(', '))
        }

        user.credential = credential

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
            UserCredential credential = user.credential ?: new UserCredential(user: user)
            String salt = passwordHashService.generateSalt()
            credential.salt = salt
            credential.passwordHash = passwordHashService.hashPassword(password, salt)

            if (!credential.save(flush: true)) {
                return failure(400, credential.errors.allErrors.collect { it.defaultMessage }.join(', '))
            }

            user.credential = credential
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

        UserCredential credential = user.credential
        if (credential) {
            credential.delete(flush: true)
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

    private long minutesSince(Date date) {
        long diffMillis = System.currentTimeMillis() - date.time
        (long) (diffMillis / 60000L)
    }

    private static long resolveTokenTtlMinutes() {
        String raw = System.getenv('TOKEN_TTL_MINUTES')
        if (!raw) {
            return 60L
        }

        try {
            long parsed = raw as Long
            parsed > 0L ? parsed : 60L
        } catch (Exception ignored) {
            60L
        }
    }
}
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
            return failure(401, 'Benutzername nicht gefunden.')
        }

        boolean valid = passwordHashService.verifyPassword(
            password,
            user.credential.salt,
            user.credential.passwordHash
        )
        if (!valid) {
            return failure(401, 'Falsches Passwort.')
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
        AuthToken authToken = findValidAuthToken(tokenValue)
        if (!authToken) {
            return [valid: false, message: tokenValue ? 'Invalid token' : 'Login required']
        }

        [valid: true, user: authToken.user]
    }

    Map getUserByToken(String tokenValue) {
        AuthToken authToken = findValidAuthToken(tokenValue)
        if (!authToken || !authToken.user) {
            return null
        }

        // Verhindert die Ausgabe des 'credential' Feldes
        User user = authToken.user
        [
            id: user.id,
            username: user.username,
            role: user.role
        ]
    }

    Map getSessionInfo(String tokenValue) {
        AuthToken authToken = findValidAuthToken(tokenValue)
        if (!authToken || !authToken.user) {
            return null
        }

        User user = authToken.user
        [
            user: [
                id: user.id,
                username: user.username,
                role: user.role
            ],
            remaining_ttl_minutes: remainingTokenTtlMinutes(authToken.createdAt)
        ]
    }

    void deleteToken(String tokenValue) {
        AuthToken authToken = AuthToken.findByToken(tokenValue)
        if (authToken) {
            authToken.delete(flush: true)
        }
    }

    Map createUser(String username, String password, String role, String tokenValue = null) {
        String requestedRole = role ?: 'NUTZER'

        Map tokenCheck = tokenValue ? validateToken(tokenValue) : [valid: true, user: null]
        if (!tokenCheck.valid) {
            return failure(401, tokenCheck.message as String)
        }

        User currentUser = tokenCheck.user as User
        if (requestedRole == 'ADMIN' && currentUser?.role != 'ADMIN') {
            return failure(403, 'Role ADMIN required')
        }

        if (User.countByUsername(username) > 0) {
            return failure(409, 'Benutzername bereits vergeben.')
        }

        User user = new User(username: username, role: requestedRole)
        if (!user.save(flush: true)) {
            if (user.errors.getFieldError('username')?.code == 'unique') {
                return failure(409, 'Benutzername bereits vergeben.')
            }
            return failure(400, 'Registrierung fehlgeschlagen.')
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

    Map updateUser(Long id, String username, String password, String role, String tokenValue) {
        Map tokenCheck = validateToken(tokenValue)
        if (!tokenCheck.valid) {
            return failure(401, tokenCheck.message as String)
        }

        User currentUser = tokenCheck.user as User
        User user = User.get(id)
        if (!user) {
            return failure(404, 'User not found')
        }

        if (currentUser.role != 'ADMIN' && currentUser.id != user.id) {
            return failure(403, 'You can only update your own account')
        }

        if (role == 'ADMIN' && currentUser.role != 'ADMIN') {
            return failure(403, 'Role ADMIN required')
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

        AuthToken authToken = AuthToken.findByUser(user)
        if (authToken) {
            authToken.delete(flush: true)
        }

        UserCredential credential = user.credential
        if (credential) {
            credential.delete(flush: true)
        }

        user.delete(flush: true)
        success(200, 'User deleted')
    }

    Map listUsers() {
        List<User> users = User.list()
        List<Map> sanitized = users.collect { u ->
            [
                id: u.id,
                username: u.username,
                role: u.role
            ]
        }

        success(200, 'Users loaded', [users: sanitized])
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

    private long remainingTokenTtlMinutes(Date createdAt) {
        long remaining = TOKEN_TTL_MINUTES - minutesSince(createdAt)
        Math.max(0L, remaining)
    }

    private AuthToken findValidAuthToken(String tokenValue) {
        if (!tokenValue) {
            return null
        }

        AuthToken authToken = AuthToken.findByToken(tokenValue)
        if (!authToken) {
            return null
        }

        if (!authToken.user) {
            authToken.delete(flush: true)
            return null
        }

        if (minutesSince(authToken.createdAt) > TOKEN_TTL_MINUTES) {
            authToken.delete(flush: true)
            return null
        }

        authToken
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
package ufg.app

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import spock.lang.Specification

class UserManagerServiceSpec extends Specification implements ServiceUnitTest<UserManagerService>, DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        [User, UserCredential, AuthToken] as Class[]
    }

    void "login fails when user does not exist"() {
        when:
        Map result = service.login("unknown", "secret")

        then:
        !result.success
        result.status == 401
        result.response.msg == "Invalid credentials"
    }

    void "login succeeds when password verification passes"() {
        given:
        User user = new User(username: "admin", role: "ADMIN").save(failOnError: true)
        UserCredential credential = new UserCredential(user: user, salt: "salt-1", passwordHash: "hash-1").save(failOnError: true)
        user.credential = credential
        user.save(failOnError: true)

        service.passwordHashService = Stub(PasswordHashService) {
            verifyPassword("secret", "salt-1", "hash-1") >> true
        }

        when:
        Map result = service.login("admin", "secret")

        then:
        result.success
        result.status == 200
        result.response.msg == "Login successful"
        result.user == user
    }

    void "validateToken fails when token is missing"() {
        when:
        Map result = service.validateToken(null)

        then:
        !result.valid
        result.message == "Login required"
    }

    void "validateToken fails when token is not found"() {
        when:
        Map result = service.validateToken("missing-token")

        then:
        !result.valid
        result.message == "Invalid token"
    }

    void "validateToken deletes expired token"() {
        given:
        User user = new User(username: "max", role: "NUTZER").save(failOnError: true)
        Date expired = new Date(System.currentTimeMillis() - (UserManagerService.TOKEN_TTL_MINUTES + 1L) * 60_000L)

        AuthToken authToken = new AuthToken(token: "expired-token", createdAt: expired, user: user).save(failOnError: true)

        when:
        Map result = service.validateToken("expired-token")

        then:
        AuthToken.findByToken("expired-token") == null
        !result.valid
        result.message == "Invalid token"
    }

    void "getUserByToken returns safe user data only"() {
        given:
        User user = new User(username: "alice", role: "ADMIN").save(failOnError: true)
        new AuthToken(token: "valid-token", createdAt: new Date(), user: user).save(failOnError: true)

        when:
        Map result = service.getUserByToken("valid-token")

        then:
        result == [id: user.id, username: "alice", role: "ADMIN"]
        !result.containsKey("credential")
    }

    void "updateUser forbids non admins from assigning ADMIN role"() {
        given:
        User user = new User(username: "max", role: "NUTZER").save(failOnError: true)
        service.metaClass.validateToken = { String token -> [valid: true, user: user] }

        when:
        Map result = service.updateUser(user.id, "max2", null, "ADMIN", "token-1")

        then:
        !result.success
        result.status == 403
        result.response.msg == "Role ADMIN required"
    }

    void "updateUser allows admins to assign ADMIN role"() {
        given:
        User admin = new User(username: "admin", role: "ADMIN").save(failOnError: true)
        User target = new User(username: "max", role: "NUTZER").save(failOnError: true)
        service.metaClass.validateToken = { String token -> [valid: true, user: admin] }

        when:
        Map result = service.updateUser(target.id, "max2", null, "ADMIN", "token-2")

        then:
        result.success
        result.status == 200
        User.get(target.id).role == "ADMIN"
    }

    void "deleteUser deletes user credential and auth token"() {
        given:
        User user = new User(username: "bob", role: "NUTZER").save(failOnError: true)
        UserCredential credential = new UserCredential(user: user, salt: "salt-1", passwordHash: "hash-1").save(failOnError: true)
        user.credential = credential
        user.save(failOnError: true)
        new AuthToken(token: "token-bob", createdAt: new Date(), user: user).save(failOnError: true)

        when:
        Map result = service.deleteUser(user.id)

        then:
        result.success
        result.status == 200
        User.get(user.id) == null
        UserCredential.get(credential.id) == null
        AuthToken.findByToken("token-bob") == null
    }
}

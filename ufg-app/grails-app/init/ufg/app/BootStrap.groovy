package ufg.app

class BootStrap {

    PasswordHashService passwordHashService

    def init = { servletContext ->
    User.withTransaction {
        if (!User.findByUsername("testuser")) {
            User user = new User(username: "testuser", role: "NUTZER").save(failOnError: true)
            String salt = passwordHashService.generateSalt()
            new UserCredential(
                user: user,
                salt: salt,
                passwordHash: passwordHashService.hashPassword("testpass", salt)
            ).save(flush: true, failOnError: true)
        }
        if (!User.findByUsername("admin")) {
            User user = new User(username: "admin", role: "ADMIN").save(failOnError: true)
            String salt = passwordHashService.generateSalt()
            new UserCredential(
                user: user,
                salt: salt,
                passwordHash: passwordHashService.hashPassword("adminpass", salt)
            ).save(flush: true, failOnError: true)
        }
    }
}

    def destroy = {
    }

}

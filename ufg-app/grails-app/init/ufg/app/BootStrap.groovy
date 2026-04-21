package ufg.app

class BootStrap {

    def init = {
        if (!User.findByUsername("testuser")) {
            new User(username: "testuser", password: "testpass", role: "USER").save(failOnError: true)
        }

        if (!User.findByUsername("admin")) {
            new User(username: "admin", password: "adminpass", role: "ADMIN").save(failOnError: true)
        }
    }

    def destroy = {
    }

}

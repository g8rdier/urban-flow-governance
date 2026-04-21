package ufg.app

import java.time.LocalDateTime

class UserManagerController {

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

}

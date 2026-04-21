package myapp

class GreetingController {

    def index() {
        Long userId = session.userId as Long
        def sk = session.sessionKey

        if (userId) {
            def user = User.get(userId)
            if (user) {
                render "User: ${user.username} (${user.role}) logged in at ${sk?.creation_time}"
                return
            }
        }

        render "No user logged in"
    }

    def admin() {
        render "Admin area. You have ADMIN role."
    }

    def nutzer() {
        render "Nutzer area. You have NUTZER role."
    }

}

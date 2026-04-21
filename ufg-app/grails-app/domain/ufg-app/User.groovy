package ufg.app

class User {

    String username
    String password
    String role = "NUTZER"

    static constraints = {
        username blank: false, unique: true
        password blank: false
        role blank: false, inList: ["NUTZER", "ADMIN"]
    }

    static mapping = {
        table "app_user"
    }
}

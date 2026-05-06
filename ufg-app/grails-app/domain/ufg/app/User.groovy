package ufg.app

class User {

    String username
    String role = "NUTZER"

    static hasOne = [credential: UserCredential]

    static constraints = {
        username blank: false, unique: true
        role blank: false, inList: ["NUTZER", "ADMIN"]
        credential nullable: true
    }

    static mapping = {
        table "app_user"
        id generator: 'identity'
    }
}

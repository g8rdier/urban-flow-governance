package ufg.app

class UserCredential {

    String passwordHash
    String salt
    User user

    static belongsTo = [user: User]

    static constraints = {
        passwordHash blank: false
        salt blank: false
        user nullable: false, unique: true
    }

    static mapping = {
        table "user_credential"
    }
}

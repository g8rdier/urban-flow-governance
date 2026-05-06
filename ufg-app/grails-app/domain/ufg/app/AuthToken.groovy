package ufg.app

class AuthToken {

    String token
    Date createdAt = new Date()
    User user

    static constraints = {
        token blank: false, unique: true
        createdAt nullable: false
        user nullable: false
    }

    static mapping = {
        table "auth_token"
        id generator: 'identity'
    }
}
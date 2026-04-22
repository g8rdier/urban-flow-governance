package ufg.app

class BearerTokenUtil {

    static String extractBearerToken(String authorizationHeader) {
        if (!authorizationHeader?.startsWith('Bearer ')) {
            return null
        }

        authorizationHeader.substring(7).trim()
    }
}
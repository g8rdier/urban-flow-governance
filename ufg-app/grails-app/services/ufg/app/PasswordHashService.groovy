package ufg.app

import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import java.security.SecureRandom
import java.util.Base64

class PasswordHashService {

    private static final int ITERATIONS = 65536
    private static final int KEY_LENGTH = 256
    private static final int SALT_BYTES = 16
    private static final String ALGORITHM = 'PBKDF2WithHmacSHA256'

    String generateSalt() {
        byte[] saltBytes = new byte[SALT_BYTES]
        new SecureRandom().nextBytes(saltBytes)
        Base64.getEncoder().encodeToString(saltBytes)
    }

    String hashPassword(String plainPassword, String salt) {
        if (!plainPassword || !salt) {
            return null
        }

        byte[] saltBytes = Base64.getDecoder().decode(salt)
        PBEKeySpec spec = new PBEKeySpec(plainPassword.toCharArray(), saltBytes, ITERATIONS, KEY_LENGTH)
        SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM)
        byte[] hash = factory.generateSecret(spec).encoded
        Base64.getEncoder().encodeToString(hash)
    }

    boolean verifyPassword(String plainPassword, String salt, String expectedHash) {
        if (!plainPassword || !salt || !expectedHash) {
            return false
        }

        String actualHash = hashPassword(plainPassword, salt)
        if (!actualHash) {
            return false
        }

        // Constant-time comparison to reduce timing side channels.
        byte[] left = Base64.getDecoder().decode(actualHash)
        byte[] right = Base64.getDecoder().decode(expectedHash)

        if (left.length != right.length) {
            return false
        }

        int result = 0
        for (int i = 0; i < left.length; i++) {
            result |= (left[i] ^ right[i])
        }

        result == 0
    }
}

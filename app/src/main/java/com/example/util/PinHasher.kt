package com.example.util

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PinHasher {
    private const val ITERATIONS = 310_000
    private const val KEY_BITS = 256
    private const val SALT_SIZE = 16
    private const val ALGORITHM = "PBKDF2WithHmacSHA1"
    data class Hash(val salt: String, val value: String)

    fun create(pin: String): Hash {
        require(pin.length == 4 && pin.all(Char::isDigit)) { "PIN must contain exactly 4 digits" }
        val salt = ByteArray(SALT_SIZE).also { SecureRandom().nextBytes(it) }
        return Hash(Base64.encodeToString(salt, Base64.NO_WRAP), Base64.encodeToString(derive(pin, salt), Base64.NO_WRAP))
    }

    fun verify(pin: String, salt: String, expected: String): Boolean {
        if (pin.length != 4 || !pin.all(Char::isDigit) || salt.isBlank() || expected.isBlank()) return false
        return runCatching {
            MessageDigest.isEqual(derive(pin, Base64.decode(salt, Base64.NO_WRAP)), Base64.decode(expected, Base64.NO_WRAP))
        }.getOrDefault(false)
    }

    private fun derive(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_BITS)
        return try { SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded }
        finally { spec.clearPassword() }
    }
}
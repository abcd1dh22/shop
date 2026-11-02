package com.hawatraders.manager.data.crypto

import java.security.MessageDigest

/** Hashing of the settings password: PBKDF2-HMAC-SHA256, random 16-byte salt, constant-time compare. */
object PasswordHasher {
    const val ITERATIONS = 120_000
    const val SALT_BYTES = 16
    private const val HASH_BYTES = 32

    fun newSalt(): ByteArray = Pbkdf2.randomBytes(SALT_BYTES)

    fun hash(password: CharArray, salt: ByteArray, iterations: Int = ITERATIONS): ByteArray =
        Pbkdf2.deriveKey(password, salt, iterations, HASH_BYTES)

    fun verify(password: CharArray, salt: ByteArray, expected: ByteArray, iterations: Int): Boolean {
        val actual = hash(password, salt, iterations)
        // MessageDigest.isEqual is constant-time for equal-length inputs.
        return MessageDigest.isEqual(actual, expected)
    }
}

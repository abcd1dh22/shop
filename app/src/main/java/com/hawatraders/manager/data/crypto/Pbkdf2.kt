package com.hawatraders.manager.data.crypto

import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * PBKDF2-HMAC-SHA256 implemented on top of HmacSHA256.
 *
 * Android's built-in "PBKDF2WithHmacSHA256" only exists from API 26 and may encode passwords
 * differently across providers; this app supports API 24+ and must produce identical keys on every
 * device (a backup made on one phone must open on another), so the algorithm (RFC 8018) lives here
 * with an explicit UTF-8 password encoding.
 */
object Pbkdf2 {
    private const val HMAC = "HmacSHA256"
    private const val HASH_LEN = 32

    fun deriveKey(password: CharArray, salt: ByteArray, iterations: Int, lengthBytes: Int): ByteArray {
        require(iterations > 0 && lengthBytes > 0 && password.isNotEmpty())
        val pw = String(password).toByteArray(Charsets.UTF_8)
        try {
            val mac = Mac.getInstance(HMAC)
            mac.init(SecretKeySpec(pw, HMAC))
            val blocks = (lengthBytes + HASH_LEN - 1) / HASH_LEN
            val out = ByteArray(lengthBytes)
            for (block in 1..blocks) {
                mac.update(salt)
                mac.update(
                    byteArrayOf(
                        (block ushr 24).toByte(), (block ushr 16).toByte(),
                        (block ushr 8).toByte(), block.toByte(),
                    )
                )
                var u = mac.doFinal() // doFinal() resets the Mac to its keyed initial state
                val t = u.copyOf()
                for (i in 2..iterations) {
                    u = mac.doFinal(u)
                    for (j in t.indices) t[j] = (t[j].toInt() xor u[j].toInt()).toByte()
                }
                val offset = (block - 1) * HASH_LEN
                System.arraycopy(t, 0, out, offset, minOf(HASH_LEN, lengthBytes - offset))
            }
            return out
        } finally {
            pw.fill(0)
        }
    }

    fun randomBytes(n: Int): ByteArray = ByteArray(n).also { SecureRandom().nextBytes(it) }
}

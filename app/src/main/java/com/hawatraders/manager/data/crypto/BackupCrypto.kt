package com.hawatraders.manager.data.crypto

import com.hawatraders.manager.util.AppException
import com.hawatraders.manager.util.Strings
import java.security.GeneralSecurityException
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Backup file layout: "HTB1" (4 bytes) | salt (16) | IV (12) | AES-256-GCM ciphertext+tag.
 * The magic header is also authenticated as GCM additional data.
 * The key comes from PBKDF2-HMAC-SHA256 (120,000 iterations) over the backup password.
 */
object BackupCrypto {
    private val MAGIC = "HTB1".toByteArray(Charsets.US_ASCII)
    private const val SALT_LEN = 16
    private const val IV_LEN = 12
    private const val TAG_BITS = 128
    private const val ITERATIONS = 120_000
    private const val KEY_BYTES = 32
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    fun encrypt(plain: ByteArray, password: CharArray): ByteArray {
        val salt = Pbkdf2.randomBytes(SALT_LEN)
        val iv = Pbkdf2.randomBytes(IV_LEN)
        val key = Pbkdf2.deriveKey(password, salt, ITERATIONS, KEY_BYTES)
        try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, iv))
            cipher.updateAAD(MAGIC)
            val ct = cipher.doFinal(plain)
            return MAGIC + salt + iv + ct
        } finally {
            key.fill(0)
        }
    }

    /** @throws AppException with a Bengali message for a non-backup file or wrong password/tampering. */
    fun decrypt(file: ByteArray, password: CharArray): ByteArray {
        val headerLen = MAGIC.size + SALT_LEN + IV_LEN
        if (file.size < headerLen + TAG_BITS / 8) throw AppException(Strings.errNotBackup)
        for (i in MAGIC.indices) if (file[i] != MAGIC[i]) throw AppException(Strings.errNotBackup)

        val salt = file.copyOfRange(MAGIC.size, MAGIC.size + SALT_LEN)
        val iv = file.copyOfRange(MAGIC.size + SALT_LEN, headerLen)
        val key = Pbkdf2.deriveKey(password, salt, ITERATIONS, KEY_BYTES)
        try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, iv))
            cipher.updateAAD(MAGIC)
            return cipher.doFinal(file, headerLen, file.size - headerLen)
        } catch (e: GeneralSecurityException) {
            // GCM cannot tell a wrong password from a damaged file; both fail tag verification.
            throw AppException(Strings.errWrongBackupPassword, e)
        } finally {
            key.fill(0)
        }
    }
}

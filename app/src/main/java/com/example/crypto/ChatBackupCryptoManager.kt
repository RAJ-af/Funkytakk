package com.example.crypto

import android.util.Log
import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object ChatBackupCryptoManager {
    private const val TAG = "ChatBackupCrypto"

    // Magic header bytes: "FTBK" (FunkyTalk Backup)
    private val MAGIC_BYTES = byteArrayOf('F'.code.toByte(), 'T'.code.toByte(), 'B'.code.toByte(), 'K'.code.toByte())
    private const val FORMAT_VERSION: Byte = 1
    private const val KDF_TYPE_PBKDF2: Byte = 1
    private const val PBKDF2_ITERATIONS = 600_000
    private const val SALT_SIZE = 16
    private const val NONCE_SIZE = 12
    private const val GCM_TAG_LENGTH = 128
    private const val KEY_SIZE_BITS = 256
    private const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"

    /**
     * Encrypts plaintext backup payload using PBKDF2-HMAC-SHA256 (600,000 iterations) and AES-256-GCM.
     * Header = magic (4) + format_version (1) + kdf_type (1) + iterations (4) + salt (16) + nonce (12) + ciphertext.
     */
    fun encryptBackup(password: String, plaintextBytes: ByteArray): ByteArray {
        require(password.isNotBlank()) { "Vault password cannot be blank" }

        val random = SecureRandom()
        val salt = ByteArray(SALT_SIZE).also { random.nextBytes(it) }
        val nonce = ByteArray(NONCE_SIZE).also { random.nextBytes(it) }

        val secretKey = deriveKey(password, salt, PBKDF2_ITERATIONS)

        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH, nonce))
        val ciphertext = cipher.doFinal(plaintextBytes)

        // Total header size = 4 + 1 + 1 + 4 + 16 + 12 = 38 bytes
        val totalSize = 38 + ciphertext.size
        val buffer = ByteBuffer.allocate(totalSize)

        buffer.put(MAGIC_BYTES)
        buffer.put(FORMAT_VERSION)
        buffer.put(KDF_TYPE_PBKDF2)
        buffer.putInt(PBKDF2_ITERATIONS)
        buffer.put(salt)
        buffer.put(nonce)
        buffer.put(ciphertext)

        Log.d(TAG, "Encrypted backup successfully. Header size: 38 bytes, Payload size: ${plaintextBytes.size} bytes, Total size: $totalSize bytes")
        return buffer.array()
    }

    /**
     * Decrypts AES-256-GCM encrypted backup payload using the derived vault password key.
     * Validates header magic bytes, version, KDF params, salt, and nonce.
     * Throws IllegalArgumentException on wrong password (AEAD Bad Tag Exception).
     */
    fun decryptBackup(password: String, encryptedBytes: ByteArray): ByteArray {
        require(password.isNotBlank()) { "Vault password cannot be blank" }
        if (encryptedBytes.size < 38) {
            throw IllegalArgumentException("Invalid or corrupted backup archive: header too short")
        }

        val buffer = ByteBuffer.wrap(encryptedBytes)

        val magic = ByteArray(4)
        buffer.get(magic)
        if (!magic.contentEquals(MAGIC_BYTES)) {
            throw IllegalArgumentException("Invalid backup format: header magic mismatch")
        }

        val version = buffer.get()
        if (version != FORMAT_VERSION) {
            throw IllegalArgumentException("Unsupported backup format version: $version")
        }

        val kdfType = buffer.get()
        if (kdfType != KDF_TYPE_PBKDF2) {
            throw IllegalArgumentException("Unsupported KDF algorithm type: $kdfType")
        }

        val iterations = buffer.int
        if (iterations < 100_000) {
            throw IllegalArgumentException("Invalid KDF iteration count: $iterations")
        }

        val salt = ByteArray(SALT_SIZE)
        buffer.get(salt)

        val nonce = ByteArray(NONCE_SIZE)
        buffer.get(nonce)

        val ciphertext = ByteArray(buffer.remaining())
        buffer.get(ciphertext)

        try {
            val secretKey = deriveKey(password, salt, iterations)
            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH, nonce))
            val decryptedBytes = cipher.doFinal(ciphertext)
            Log.d(TAG, "Decrypted backup successfully. Decrypted size: ${decryptedBytes.size} bytes")
            return decryptedBytes
        } catch (e: javax.crypto.AEADBadTagException) {
            Log.w(TAG, "Backup decryption failed: AEAD auth tag validation failed (wrong vault password)")
            throw IllegalArgumentException("Wrong vault password. Please try again.")
        } catch (e: Exception) {
            Log.e(TAG, "Backup decryption error", e)
            throw e
        }
    }

    private fun deriveKey(password: String, salt: ByteArray, iterations: Int): SecretKey {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, KEY_SIZE_BITS)
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }
}

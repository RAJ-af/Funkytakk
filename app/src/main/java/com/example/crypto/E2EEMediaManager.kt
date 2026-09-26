package com.example.crypto

import android.util.Base64
import android.util.Log
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

data class EncryptedMediaResult(
    val ciphertextBytes: ByteArray,
    val keyBytes: ByteArray,
    val nonceBytes: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as EncryptedMediaResult
        if (!ciphertextBytes.contentEquals(other.ciphertextBytes)) return false
        if (!keyBytes.contentEquals(other.keyBytes)) return false
        if (!nonceBytes.contentEquals(other.nonceBytes)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = ciphertextBytes.contentHashCode()
        result = 31 * result + keyBytes.contentHashCode()
        result = 31 * result + nonceBytes.contentHashCode()
        return result
    }
}

object E2EEMediaManager {
    private const val TAG = "E2EEMediaManager"
    private const val KEY_SIZE_BYTES = 32 // AES-256
    private const val NONCE_SIZE_BYTES = 12 // 96-bit GCM nonce
    private const val GCM_TAG_LENGTH_BITS = 128

    /**
     * Layer 1: Encrypts raw media bytes (image or audio) with a one-time AES-256-GCM key + 12-byte nonce.
     */
    fun encryptMediaPayload(rawBytes: ByteArray): EncryptedMediaResult {
        val random = SecureRandom()
        val keyBytes = ByteArray(KEY_SIZE_BYTES)
        val nonceBytes = ByteArray(NONCE_SIZE_BYTES)
        random.nextBytes(keyBytes)
        random.nextBytes(nonceBytes)

        val secretKey = SecretKeySpec(keyBytes, "AES")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, nonceBytes)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)

        val ciphertextBytes = cipher.doFinal(rawBytes)
        Log.d(TAG, "Encrypted media payload: rawSize=${rawBytes.size}, cipherSize=${ciphertextBytes.size}")
        return EncryptedMediaResult(ciphertextBytes, keyBytes, nonceBytes)
    }

    /**
     * Decrypts encrypted media ciphertext bytes with AES-256-GCM using the provided key and nonce.
     */
    fun decryptMediaPayload(ciphertextBytes: ByteArray, keyBytes: ByteArray, nonceBytes: ByteArray): ByteArray {
        val secretKey = SecretKeySpec(keyBytes, "AES")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, nonceBytes)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)

        val decryptedBytes = cipher.doFinal(ciphertextBytes)
        Log.d(TAG, "Decrypted media payload successfully: size=${decryptedBytes.size}")
        return decryptedBytes
    }
}

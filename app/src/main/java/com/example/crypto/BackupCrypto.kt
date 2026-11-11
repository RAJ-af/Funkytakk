package com.example.crypto

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import java.util.Base64
import com.google.crypto.tink.subtle.AesGcmHkdfStreaming
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

data class BackupHeader(
    val version: Int = 1,
    val iterations: Int = 600_000,
    val saltBase64: String,
    val nonceBase64: String,
    val wrappedKeyBase64: String,
    val uid: String
) {
    fun toJson(): String {
        return JSONObject().apply {
            put("version", version)
            put("iterations", iterations)
            put("salt", saltBase64)
            put("nonce", nonceBase64)
            put("wrapped_key", wrappedKeyBase64)
            put("uid", uid)
        }.toString()
    }

    companion object {
        fun fromJson(jsonStr: String): BackupHeader {
            val obj = JSONObject(jsonStr)
            return BackupHeader(
                version = obj.optInt("version", 1),
                iterations = obj.optInt("iterations", 600_000),
                saltBase64 = obj.getString("salt"),
                nonceBase64 = obj.getString("nonce"),
                wrappedKeyBase64 = obj.getString("wrapped_key"),
                uid = obj.getString("uid")
            )
        }
    }
}

object BackupCrypto {
    private const val TAG = "BackupCrypto"
    private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val PBKDF2_ITERATIONS = 600_000
    private const val KEY_SIZE_BITS = 256
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val NONCE_SIZE_BYTES = 12
    private const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"

    private const val KEYSTORE_ALIAS_BK = "funkytalk_bk_keystore_alias"
    private const val KEYSTORE_PREFS_NAME = "funkytalk_bk_keystore_prefs"
    private const val PREF_WRAPPED_BK = "wrapped_bk_keystore"

    fun generateMasterKey(): ByteArray {
        return ByteArray(32).also { SecureRandom().nextBytes(it) }
    }

    suspend fun deriveKek(password: CharArray, salt: ByteArray, iterations: Int = PBKDF2_ITERATIONS): SecretKey = withContext(Dispatchers.Default) {
        val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
        val spec = PBEKeySpec(password, salt, iterations, KEY_SIZE_BITS)
        val secretKey = factory.generateSecret(spec)
        SecretKeySpec(secretKey.encoded, "AES")
    }

    suspend fun wrapMasterKey(
        masterKey: ByteArray,
        password: String,
        firebaseUid: String
    ): BackupHeader = withContext(Dispatchers.Default) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val kek = deriveKek(password.toCharArray(), salt, PBKDF2_ITERATIONS)

        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, kek)
        val nonce = cipher.iv

        val aad = "funkytalk-backup-v1:$firebaseUid".toByteArray(Charsets.UTF_8)
        cipher.updateAAD(aad)

        val wrappedKey = cipher.doFinal(masterKey)

        BackupHeader(
            version = 1,
            iterations = PBKDF2_ITERATIONS,
            saltBase64 = Base64.getEncoder().encodeToString(salt),
            nonceBase64 = Base64.getEncoder().encodeToString(nonce),
            wrappedKeyBase64 = Base64.getEncoder().encodeToString(wrappedKey),
            uid = firebaseUid
        )
    }

    suspend fun unwrapMasterKey(
        headerJson: String,
        password: String,
        firebaseUid: String
    ): ByteArray = withContext(Dispatchers.Default) {
        val header = BackupHeader.fromJson(headerJson)
        if (header.uid != firebaseUid) {
            throw IllegalArgumentException("This backup belongs to another FunkyTalk account")
        }

        val salt = Base64.getDecoder().decode(header.saltBase64)
        val nonce = Base64.getDecoder().decode(header.nonceBase64)
        val wrappedKey = Base64.getDecoder().decode(header.wrappedKeyBase64)

        val kek = deriveKek(password.toCharArray(), salt, header.iterations)

        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, kek, GCMParameterSpec(GCM_TAG_LENGTH_BITS, nonce))

        val aad = "funkytalk-backup-v1:$firebaseUid".toByteArray(Charsets.UTF_8)
        cipher.updateAAD(aad)

        cipher.doFinal(wrappedKey)
    }

    /**
     * Encrypts a data chunk with AES-256-GCM using BK and a fresh 12-byte nonce.
     * Returns: 12-byte Nonce + Ciphertext (with 16-byte GCM tag).
     */
    fun encryptChunk(data: ByteArray, chunkId: String, masterKey: ByteArray): ByteArray {
        val nonce = ByteArray(NONCE_SIZE_BYTES).also { SecureRandom().nextBytes(it) }
        val keySpec = SecretKeySpec(masterKey, "AES")
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, GCMParameterSpec(GCM_TAG_LENGTH_BITS, nonce))

        val aad = chunkId.toByteArray(Charsets.UTF_8)
        cipher.updateAAD(aad)

        val ciphertext = cipher.doFinal(data)

        val result = ByteBuffer.allocate(NONCE_SIZE_BYTES + ciphertext.size)
        result.put(nonce)
        result.put(ciphertext)
        return result.array()
    }

    /**
     * Decrypts an AES-256-GCM data chunk. Verifies nonce, AAD and GCM tag.
     */
    fun decryptChunk(encryptedData: ByteArray, chunkId: String, masterKey: ByteArray): ByteArray {
        if (encryptedData.size < NONCE_SIZE_BYTES + 16) {
            throw IllegalArgumentException("Corrupted encrypted chunk payload.")
        }

        val buffer = ByteBuffer.wrap(encryptedData)
        val nonce = ByteArray(NONCE_SIZE_BYTES)
        buffer.get(nonce)

        val ciphertext = ByteArray(buffer.remaining())
        buffer.get(ciphertext)

        val keySpec = SecretKeySpec(masterKey, "AES")
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, GCMParameterSpec(GCM_TAG_LENGTH_BITS, nonce))

        val aad = chunkId.toByteArray(Charsets.UTF_8)
        cipher.updateAAD(aad)

        return cipher.doFinal(ciphertext)
    }

    // ------------------------------------------------------------------
    // Android Keystore persistence for Master Key BK (Unattended Auto Backup)
    // ------------------------------------------------------------------

    fun saveMasterKeyToKeystore(context: Context, masterKey: ByteArray) {
        try {
            val wrapperKey = getOrCreateKeystoreWrapperKey()
            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, wrapperKey)
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(masterKey)

            val buffer = ByteBuffer.allocate(NONCE_SIZE_BYTES + encryptedBytes.size)
            buffer.put(iv)
            buffer.put(encryptedBytes)

            val base64 = Base64.getEncoder().encodeToString(buffer.array())
            context.getSharedPreferences(KEYSTORE_PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(PREF_WRAPPED_BK, base64)
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save master key to Keystore", e)
        }
    }

    fun loadMasterKeyFromKeystore(context: Context): ByteArray? {
        val base64 = context.getSharedPreferences(KEYSTORE_PREFS_NAME, Context.MODE_PRIVATE)
            .getString(PREF_WRAPPED_BK, null) ?: return null
        return try {
            val payloadBytes = Base64.getDecoder().decode(base64)
            if (payloadBytes.size <= NONCE_SIZE_BYTES) return null

            val buffer = ByteBuffer.wrap(payloadBytes)
            val iv = ByteArray(NONCE_SIZE_BYTES)
            buffer.get(iv)

            val ciphertext = ByteArray(buffer.remaining())
            buffer.get(ciphertext)

            val wrapperKey = getOrCreateKeystoreWrapperKey()
            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, wrapperKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))

            cipher.doFinal(ciphertext)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load master key from Keystore", e)
            null
        }
    }

    fun clearMasterKeyFromKeystore(context: Context) {
        context.getSharedPreferences(KEYSTORE_PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(PREF_WRAPPED_BK)
            .apply()
    }

    private fun getOrCreateKeystoreWrapperKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (keyStore.containsAlias(KEYSTORE_ALIAS_BK)) {
            val entry = keyStore.getEntry(KEYSTORE_ALIAS_BK, null) as? KeyStore.SecretKeyEntry
            if (entry != null) return entry.secretKey
        }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val builder = KeyGenParameterSpec.Builder(
            KEYSTORE_ALIAS_BK,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(KEY_SIZE_BITS)
            .setUserAuthenticationRequired(false)

        keyGenerator.init(builder.build())
        return keyGenerator.generateKey()
    }

    // ------------------------------------------------------------------
    // Tink Streaming AEAD (AES256_GCM_HKDF_1MB) for Media Files up to 100 MB
    // ------------------------------------------------------------------

    fun encryptMediaStream(
        inputStream: InputStream,
        outputStream: OutputStream,
        masterKey: ByteArray,
        associatedData: ByteArray = ByteArray(0)
    ) {
        val ikm = masterKey
        val hkdfAlg = "HmacSha256"
        val keySizeInBytes = 32
        val ciphertextSegmentSize = 1 * 1024 * 1024 // 1 MB segments
        val firstSegmentOffset = 0

        val streamingAead = AesGcmHkdfStreaming(
            ikm,
            hkdfAlg,
            keySizeInBytes,
            ciphertextSegmentSize,
            firstSegmentOffset
        )

        val encryptingStream = streamingAead.newEncryptingStream(outputStream, associatedData)
        val buffer = ByteArray(64 * 1024)
        var read: Int
        while (inputStream.read(buffer).also { read = it } != -1) {
            encryptingStream.write(buffer, 0, read)
        }
        encryptingStream.close()
    }

    fun decryptMediaStream(
        inputStream: InputStream,
        outputStream: OutputStream,
        masterKey: ByteArray,
        associatedData: ByteArray = ByteArray(0)
    ) {
        val ikm = masterKey
        val hkdfAlg = "HmacSha256"
        val keySizeInBytes = 32
        val ciphertextSegmentSize = 1 * 1024 * 1024
        val firstSegmentOffset = 0

        val streamingAead = AesGcmHkdfStreaming(
            ikm,
            hkdfAlg,
            keySizeInBytes,
            ciphertextSegmentSize,
            firstSegmentOffset
        )

        val decryptingStream = streamingAead.newDecryptingStream(inputStream, associatedData)
        val buffer = ByteArray(64 * 1024)
        var read: Int
        while (decryptingStream.read(buffer).also { read = it } != -1) {
            outputStream.write(buffer, 0, read)
        }
        decryptingStream.close()
    }
}

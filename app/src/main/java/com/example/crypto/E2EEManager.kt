package com.example.crypto

import android.content.Context
import android.util.Base64
import android.util.Log
import com.example.SupabaseClient
import com.example.SupabaseSessionManager
import com.example.UpsertUserPublicKeyRequest
import com.google.crypto.tink.BinaryKeysetReader
import com.google.crypto.tink.BinaryKeysetWriter
import com.google.crypto.tink.CleartextKeysetHandle
import com.google.crypto.tink.HybridDecrypt
import com.google.crypto.tink.HybridEncrypt
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.hybrid.HybridConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object E2EEManager {
    private const val TAG = "E2EEManager"
    private const val KEYSET_NAME = "e2ee_hpke_private_keyset"
    private const val PREFS_FILE = "e2ee_tink_prefs"
    private const val MASTER_KEY_URI = "android-keystore://e2ee_hpke_master_key"
    private const val KEY_TEMPLATE_STRING = "DHKEM_X25519_HKDF_SHA256_HKDF_SHA256_AES_256_GCM"

    private val CONTEXT_INFO = ByteArray(0) // Safe RFC 9180 HPKE context info

    @Volatile
    private var initialized = false

    @Volatile
    private var cachedKeysetHandle: KeysetHandle? = null

    @Volatile
    private var cachedPublicKeyBase64: String? = null

    init {
        try {
            HybridConfig.register()
            initialized = true
        } catch (e: Exception) {
            Log.e(TAG, "Error registering Tink HybridConfig", e)
        }
    }

    /**
     * Initializes the user's Tink HPKE identity keypair.
     * Uses AndroidKeysetManager backed by Android Keystore so private keys are never in plaintext.
     * Exports and publishes the public keyset to Supabase user_public_keys.
     */
    suspend fun ensureIdentityKeyInitialized(context: Context): String? = withContext(Dispatchers.IO) {
        try {
            if (!initialized) {
                HybridConfig.register()
                initialized = true
            }

            val keysetHandle = cachedKeysetHandle ?: synchronized(this) {
                cachedKeysetHandle ?: run {
                    val keyTemplate = KeyTemplates.get(KEY_TEMPLATE_STRING)
                    val manager = AndroidKeysetManager.Builder()
                        .withSharedPref(context.applicationContext, KEYSET_NAME, PREFS_FILE)
                        .withKeyTemplate(keyTemplate)
                        .withMasterKeyUri(MASTER_KEY_URI)
                        .build()
                    manager.keysetHandle.also { cachedKeysetHandle = it }
                }
            }

            // Export public keyset as Base64 string
            val publicKeysetHandle = keysetHandle.publicKeysetHandle
            val stream = ByteArrayOutputStream()
            CleartextKeysetHandle.write(publicKeysetHandle, BinaryKeysetWriter.withOutputStream(stream))
            val publicKeyBytes = stream.toByteArray()
            val publicKeyBase64 = Base64.encodeToString(publicKeyBytes, Base64.NO_WRAP)
            cachedPublicKeyBase64 = publicKeyBase64

            // Upsert public key to Supabase user_public_keys
            val myUserId = SupabaseSessionManager.getUserId(context)
            val token = SupabaseSessionManager.getBearerToken(context)
            if (!myUserId.isNullOrBlank() && !token.isNullOrBlank()) {
                val isoDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }.format(Date())

                val resp = SupabaseClient.api.upsertUserPublicKey(
                    apiKey = com.example.BuildConfig.SUPABASE_ANON_KEY,
                    auth = token,
                    prefer = "resolution=merge-duplicates",
                    body = UpsertUserPublicKeyRequest(
                        user_id = myUserId,
                        identity_public_key = publicKeyBase64,
                        key_version = 1,
                        updated_at = isoDate
                    )
                )
                if (resp.isSuccessful) {
                    Log.d(TAG, "Successfully synced public key to Supabase user_public_keys for user $myUserId")
                } else {
                    Log.w(TAG, "Failed to upsert public key: code=${resp.code()} msg=${resp.message()}")
                }
            }

            publicKeyBase64
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing identity key", e)
            null
        }
    }

    /**
     * Encrypts plaintext using recipient's Tink HPKE public keyset.
     * Output is Base64-encoded ciphertext.
     */
    fun encryptMessage(recipientPublicKeyBase64: String, plaintext: String): String {
        val publicKeyBytes = Base64.decode(recipientPublicKeyBase64, Base64.DEFAULT)
        val publicKeysetHandle = CleartextKeysetHandle.read(BinaryKeysetReader.withBytes(publicKeyBytes))
        val hybridEncrypt = publicKeysetHandle.getPrimitive(HybridEncrypt::class.java)
        val ciphertextBytes = hybridEncrypt.encrypt(plaintext.toByteArray(Charsets.UTF_8), CONTEXT_INFO)
        return Base64.encodeToString(ciphertextBytes, Base64.NO_WRAP)
    }

    /**
     * Decrypts Base64-encoded ciphertext using our private keyset.
     * Output is decrypted UTF-8 plaintext.
     */
    fun decryptMessage(context: Context, ciphertextBase64: String): String {
        val keysetHandle = cachedKeysetHandle ?: run {
            val keyTemplate = KeyTemplates.get(KEY_TEMPLATE_STRING)
            val manager = AndroidKeysetManager.Builder()
                .withSharedPref(context.applicationContext, KEYSET_NAME, PREFS_FILE)
                .withKeyTemplate(keyTemplate)
                .withMasterKeyUri(MASTER_KEY_URI)
                .build()
            manager.keysetHandle.also { cachedKeysetHandle = it }
        }

        val hybridDecrypt = keysetHandle.getPrimitive(HybridDecrypt::class.java)
        val ciphertextBytes = Base64.decode(ciphertextBase64, Base64.DEFAULT)
        val decryptedBytes = hybridDecrypt.decrypt(ciphertextBytes, CONTEXT_INFO)
        return String(decryptedBytes, Charsets.UTF_8)
    }

    fun getMyPublicKeyBase64(): String? = cachedPublicKeyBase64
}

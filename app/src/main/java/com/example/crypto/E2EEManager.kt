package com.example.crypto

import android.content.Context
import android.os.SystemClock
import android.util.Base64
import android.util.Log
import android.util.LruCache
import com.example.ActiveUserManager
import com.example.BuildConfig
import com.example.LegacySecurityMigration
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
import java.security.KeyStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.ConcurrentHashMap

/**
 * Debug-only stage timing tracker for DM & Group message latency.
 * Logs ONLY anonymized clientId prefix and durations in ms; never logs message content, keys, or tokens.
 */
object ChatTimingTracker {
    private const val TAG = "ChatTiming"
    private val tapTimestamps = ConcurrentHashMap<String, Long>()
    private val bubbleReported = ConcurrentHashMap.newKeySet<String>()

    fun nowMs(): Long = SystemClock.elapsedRealtime()

    fun markTap(clientId: String, tapElapsedMs: Long = SystemClock.elapsedRealtime()) {
        if (!BuildConfig.DEBUG || clientId.isBlank()) return
        if (tapTimestamps.size > 256) tapTimestamps.clear()
        tapTimestamps[clientId] = tapElapsedMs
        bubbleReported.remove(clientId)
    }

    fun markBubbleShown(channel: String, clientId: String) {
        if (!BuildConfig.DEBUG || clientId.isBlank()) return
        val tapMs = tapTimestamps[clientId] ?: return
        if (bubbleReported.add(clientId)) {
            if (bubbleReported.size > 256) bubbleReported.clear()
            val dt = (SystemClock.elapsedRealtime() - tapMs).coerceAtLeast(0L)
            Log.d(TAG, "[$channel] id=${clientId.take(8)} tap_to_bubble_ms=$dt")
        }
    }

    fun logEncrypt(channel: String, clientId: String, durationMs: Long, recipients: Int = 1) {
        if (!BuildConfig.DEBUG) return
        Log.d(TAG, "[$channel] id=${clientId.take(8)} encrypt_ms=$durationMs recipients=$recipients")
    }

    fun logSendRequest(channel: String, clientId: String, durationMs: Long, success: Boolean) {
        if (!BuildConfig.DEBUG) return
        val tapMs = tapTimestamps[clientId]
        val totalMs = if (tapMs != null) (SystemClock.elapsedRealtime() - tapMs).coerceAtLeast(0L) else -1L
        Log.d(TAG, "[$channel] id=${clientId.take(8)} send_request_ms=$durationMs total_to_ack_ms=$totalMs ok=$success")
    }

    fun logServerEchoOrReceive(channel: String, refId: String, receiveLatencyMs: Long, isEcho: Boolean) {
        if (!BuildConfig.DEBUG) return
        val tapMs = tapTimestamps.remove(refId)
        val roundTripMs = if (tapMs != null) (SystemClock.elapsedRealtime() - tapMs).coerceAtLeast(0L) else -1L
        Log.d(
            TAG,
            "[$channel] id=${refId.take(8)} ${if (isEcho) "server_echo_ms" else "realtime_receive_ms"}=$receiveLatencyMs roundtrip_ms=$roundTripMs"
        )
    }
}

object E2EEManager {
    private const val TAG = "E2EEManager"
    private const val KEY_TEMPLATE_STRING = "DHKEM_X25519_HKDF_SHA256_HKDF_SHA256_AES_256_GCM"
    private const val KEY_PREF_VERSION = "e2ee_key_version"

    private val CONTEXT_INFO = ByteArray(0) // Safe RFC 9180 HPKE context info

    @Volatile
    private var initialized = false

    @Volatile
    private var cachedUid: String? = null

    @Volatile
    private var cachedKeysetHandle: KeysetHandle? = null

    @Volatile
    private var cachedHybridDecrypt: HybridDecrypt? = null

    @Volatile
    private var cachedPublicKeyBase64: String? = null

    private val syncedUids = ConcurrentHashMap.newKeySet<String>()
    private val encryptPrimitiveCache = object : LruCache<String, HybridEncrypt>(64) {}

    init {
        try {
            HybridConfig.register()
            initialized = true
        } catch (e: Exception) {
            Log.e(TAG, "Error registering Tink HybridConfig", e)
        }
    }

    private fun ensureHybridRegistered() {
        if (!initialized) {
            HybridConfig.register()
            initialized = true
        }
    }

    private fun getKeyVersion(context: Context, uid: String): Int {
        val prefs = context.applicationContext.getSharedPreferences(
            ActiveUserManager.e2eePrefsName(uid),
            Context.MODE_PRIVATE
        )
        return prefs.getInt(KEY_PREF_VERSION, 1).coerceAtLeast(1)
    }

    private fun setKeyVersion(context: Context, uid: String, version: Int) {
        context.applicationContext.getSharedPreferences(
            ActiveUserManager.e2eePrefsName(uid),
            Context.MODE_PRIVATE
        ).edit().putInt(KEY_PREF_VERSION, version.coerceAtLeast(1)).commit()
    }

    private fun getOrBuildKeysetHandle(context: Context, uid: String): KeysetHandle {
        ensureHybridRegistered()
        if (cachedUid == uid && cachedKeysetHandle != null) {
            return cachedKeysetHandle!!
        }
        return synchronized(this) {
            if (cachedUid != uid) {
                cachedKeysetHandle = null
                cachedHybridDecrypt = null
                cachedPublicKeyBase64 = null
                cachedUid = uid
            }
            cachedKeysetHandle ?: run {
                val keyTemplate = KeyTemplates.get(KEY_TEMPLATE_STRING)
                val manager = AndroidKeysetManager.Builder()
                    .withSharedPref(
                        context.applicationContext,
                        ActiveUserManager.e2eeKeysetName(uid),
                        ActiveUserManager.e2eePrefsName(uid)
                    )
                    .withKeyTemplate(keyTemplate)
                    .withMasterKeyUri(ActiveUserManager.e2eeMasterKeyUri(uid))
                    .build()
                manager.keysetHandle.also {
                    cachedKeysetHandle = it
                    cachedHybridDecrypt = null
                    cachedUid = uid
                }
            }
        }
    }

    private fun getOrBuildHybridDecrypt(context: Context, uid: String): HybridDecrypt {
        if (cachedUid == uid && cachedHybridDecrypt != null) {
            return cachedHybridDecrypt!!
        }
        val handle = getOrBuildKeysetHandle(context, uid)
        return synchronized(this) {
            if (cachedUid == uid && cachedHybridDecrypt != null) {
                cachedHybridDecrypt!!
            } else {
                handle.getPrimitive(HybridDecrypt::class.java).also {
                    cachedHybridDecrypt = it
                }
            }
        }
    }

    private fun getOrBuildHybridEncrypt(recipientPublicKeyBase64: String): HybridEncrypt {
        ensureHybridRegistered()
        synchronized(encryptPrimitiveCache) {
            encryptPrimitiveCache.get(recipientPublicKeyBase64)?.let { return it }
        }
        val publicKeyBytes = Base64.decode(recipientPublicKeyBase64, Base64.DEFAULT)
        val publicKeysetHandle = CleartextKeysetHandle.read(BinaryKeysetReader.withBytes(publicKeyBytes))
        val primitive = publicKeysetHandle.getPrimitive(HybridEncrypt::class.java)
        synchronized(encryptPrimitiveCache) {
            encryptPrimitiveCache.put(recipientPublicKeyBase64, primitive)
        }
        return primitive
    }

    private fun exportPublicKeyBase64(keysetHandle: KeysetHandle): String {
        val publicKeysetHandle = keysetHandle.publicKeysetHandle
        val stream = ByteArrayOutputStream()
        CleartextKeysetHandle.write(publicKeysetHandle, BinaryKeysetWriter.withOutputStream(stream))
        return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
    }

    private fun deleteKeysetForUid(context: Context, uid: String) {
        synchronized(this) {
            if (cachedUid == uid) {
                cachedKeysetHandle = null
                cachedHybridDecrypt = null
                cachedPublicKeyBase64 = null
                cachedUid = null
            }
            syncedUids.remove(uid)
        }
        LegacySecurityMigration.deleteSharedPrefsCompat(
            context.applicationContext,
            ActiveUserManager.e2eePrefsName(uid)
        )
        try {
            val alias = ActiveUserManager.e2eeMasterKeyAlias(uid)
            val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            if (ks.containsAlias(alias)) {
                ks.deleteEntry(alias)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error deleting E2EE Keystore alias for $uid", e)
        }
    }

    /**
     * Initializes the active user's Tink HPKE identity keypair strictly scoped by UID.
     * Exports and publishes the public keyset to Supabase user_public_keys.
     * Automatically recovers from `identity_key_in_use` (23505) by regenerating a fresh UID-scoped keypair.
     */
    suspend fun ensureIdentityKeyInitialized(context: Context, forceSync: Boolean = false): String? = withContext(Dispatchers.IO) {
        try {
            val uid = ActiveUserManager.getActiveUid(context) ?: return@withContext null
            val keysetHandle = getOrBuildKeysetHandle(context, uid)
            var publicKeyBase64 = if (cachedUid == uid && !cachedPublicKeyBase64.isNullOrBlank()) {
                cachedPublicKeyBase64!!
            } else {
                exportPublicKeyBase64(keysetHandle).also { cachedPublicKeyBase64 = it }
            }

            if (!forceSync && syncedUids.contains(uid)) {
                return@withContext publicKeyBase64
            }

            val token = SupabaseSessionManager.getBearerToken(context)
            if (token.isNotBlank()) {
                val isoDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }.format(Date())

                val keyVersion = getKeyVersion(context, uid)
                val resp = SupabaseClient.api.upsertUserPublicKey(
                    apiKey = BuildConfig.SUPABASE_ANON_KEY,
                    auth = token,
                    prefer = "resolution=merge-duplicates",
                    body = UpsertUserPublicKeyRequest(
                        user_id = uid,
                        identity_public_key = publicKeyBase64,
                        key_version = keyVersion,
                        updated_at = isoDate
                    )
                )

                if (resp.isSuccessful) {
                    syncedUids.add(uid)
                    Log.d(TAG, "Synced public key to Supabase user_public_keys for user $uid (v$keyVersion)")
                } else {
                    val errBody = resp.errorBody()?.string().orEmpty()
                    val isIdentityInUse = resp.code() == 409 ||
                        errBody.contains("identity_key_in_use", ignoreCase = true) ||
                        errBody.contains("23505")

                    if (isIdentityInUse) {
                        Log.w(TAG, "identity_key_in_use detected for user $uid; wiping UID keyset and regenerating...")
                        val nextVersion = keyVersion + 1
                        deleteKeysetForUid(context, uid)
                        setKeyVersion(context, uid, nextVersion)

                        val freshHandle = getOrBuildKeysetHandle(context, uid)
                        publicKeyBase64 = exportPublicKeyBase64(freshHandle)
                        cachedPublicKeyBase64 = publicKeyBase64

                        val retryResp = SupabaseClient.api.upsertUserPublicKey(
                            apiKey = BuildConfig.SUPABASE_ANON_KEY,
                            auth = token,
                            prefer = "resolution=merge-duplicates",
                            body = UpsertUserPublicKeyRequest(
                                user_id = uid,
                                identity_public_key = publicKeyBase64,
                                key_version = nextVersion,
                                updated_at = isoDate
                            )
                        )
                        if (retryResp.isSuccessful) {
                            syncedUids.add(uid)
                            Log.d(TAG, "Successfully synced regenerated public key for user $uid (v$nextVersion)")
                        } else {
                            Log.w(TAG, "Retry upsertUserPublicKey failed: code=${retryResp.code()} msg=${retryResp.message()}")
                        }
                    } else {
                        Log.w(TAG, "Failed to upsert public key: code=${resp.code()} msg=${resp.message()}")
                    }
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
     */
    fun encryptMessage(recipientPublicKeyBase64: String, plaintext: String): String {
        val hybridEncrypt = getOrBuildHybridEncrypt(recipientPublicKeyBase64)
        val ciphertextBytes = hybridEncrypt.encrypt(plaintext.toByteArray(Charsets.UTF_8), CONTEXT_INFO)
        return Base64.encodeToString(ciphertextBytes, Base64.NO_WRAP)
    }

    suspend fun encryptMessageAsync(recipientPublicKeyBase64: String, plaintext: String): String =
        withContext(Dispatchers.Default) {
            encryptMessage(recipientPublicKeyBase64, plaintext)
        }

    /**
     * Decrypts Base64-encoded ciphertext using the active user's private keyset.
     */
    fun decryptMessage(context: Context, ciphertextBase64: String): String {
        val uid = ActiveUserManager.requireActiveUid(context)
        val hybridDecrypt = getOrBuildHybridDecrypt(context, uid)
        val ciphertextBytes = Base64.decode(ciphertextBase64, Base64.DEFAULT)
        val decryptedBytes = hybridDecrypt.decrypt(ciphertextBytes, CONTEXT_INFO)
        return String(decryptedBytes, Charsets.UTF_8)
    }

    suspend fun decryptMessageAsync(context: Context, ciphertextBase64: String): String =
        withContext(Dispatchers.Default) {
            decryptMessage(context, ciphertextBase64)
        }

    fun getMyPublicKeyBase64(context: Context? = null): String? {
        val uid = ActiveUserManager.getActiveUid(context) ?: return null
        return if (cachedUid == uid) cachedPublicKeyBase64 else null
    }

    /**
     * Clears in-memory handles on logout without deleting on-disk scoped keys.
     */
    fun resetForLogout() {
        synchronized(this) {
            cachedKeysetHandle = null
            cachedHybridDecrypt = null
            cachedPublicKeyBase64 = null
            cachedUid = null
            syncedUids.clear()
        }
        synchronized(encryptPrimitiveCache) {
            encryptPrimitiveCache.evictAll()
        }
    }

    /**
     * Completely deletes the E2EE Tink keyset and AndroidKeyStore master key for the specified UID.
     */
    fun wipeForUid(context: Context, uid: String) {
        if (uid.isBlank()) return
        deleteKeysetForUid(context, uid)
    }
}

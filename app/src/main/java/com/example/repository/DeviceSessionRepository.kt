package com.example.repository

import android.content.Context
import android.util.Log
import com.example.ActiveUserManager
import com.example.BuildConfig
import com.example.CurrentDeviceRequest
import com.example.DeviceIdentity
import com.example.RevokeDeviceRequest
import com.example.SupabaseClient
import com.example.SupabaseSessionManager
import com.example.TouchDeviceRequest
import com.example.TouchDeviceResponse
import com.example.UserDeviceRow
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean

object DeviceSessionRepository {
    private const val TAG = "DeviceSessionRepo"

    const val DEVICE_REVOKED_MESSAGE =
        "You were signed out because your account was opened on another device."
    const val SESSION_MINT_FAILED_MESSAGE = "Couldn't sign in. Try again."

    private val revocationTriggered = AtomicBoolean(false)

    private val _deviceRevokedNotice = MutableStateFlow<String?>(null)
    val deviceRevokedNotice: StateFlow<String?> = _deviceRevokedNotice.asStateFlow()

    private val _remoteSignOutCounter = MutableStateFlow(0L)
    val remoteSignOutCounter: StateFlow<Long> = _remoteSignOutCounter.asStateFlow()

    /**
     * Resets the single-shot revocation guard once the user explicitly starts a fresh interactive sign-in.
     */
    fun resetRevokedGuardForInteractiveSignIn() {
        revocationTriggered.set(false)
    }

    fun consumeDeviceRevokedNotice() {
        _deviceRevokedNotice.value = null
    }

    fun isRevocationInProgress(): Boolean = revocationTriggered.get()

    /**
     * Signs out locally while keeping encrypted local chat data on disk (only locking the Vault),
     * clears Supabase + Firebase sessions cleanly, and emits a one-time revoked banner when
     * [showNotice] is true. Safe to call from parallel interceptors/coroutines without looping.
     */
    fun triggerLocalSignOutForRevocation(
        context: Context,
        showNotice: Boolean = true
    ) {
        val appCtx = context.applicationContext ?: context
        if (showNotice && !revocationTriggered.compareAndSet(false, true)) {
            // Already handled in this session — never emit duplicate UI alerts or loop
            performMinimalLocalSignOut(appCtx)
            return
        }
        if (!showNotice) {
            revocationTriggered.set(false)
        }

        performMinimalLocalSignOut(appCtx)

        if (showNotice) {
            _deviceRevokedNotice.value = DEVICE_REVOKED_MESSAGE
        }
        _remoteSignOutCounter.value = _remoteSignOutCounter.value + 1L
    }

    private fun performMinimalLocalSignOut(appCtx: Context) {
        val effectiveUid = SupabaseSessionManager.getUserId(appCtx)
            ?: try { FirebaseAuth.getInstance().currentUser?.uid } catch (_: Exception) { null }
            ?: ""

        // Capture FCM token cleanup + full singleton reset while preserving local encrypted chat DB/vault files.
        // PushNotificationManager.launchApplicationLogoutCleanup invokes VaultSecurityManager.resetForLogout()
        // which strictly locks in-memory keys (VaultSecurityManager.lock()) and preserves local encrypted data.
        try {
            com.example.service.PushNotificationManager.launchApplicationLogoutCleanup(
                context = appCtx,
                effectiveUid = effectiveUid,
                firebaseAuth = try { FirebaseAuth.getInstance() } catch (_: Exception) { null }
            )
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.w(TAG, "Error during PushNotificationManager cleanup: ${e.javaClass.simpleName}")
            }
            try { FirebaseAuth.getInstance().signOut() } catch (_: Exception) {}
            try { SupabaseSessionManager.clearSession(appCtx) } catch (_: Exception) {}
            try { ActiveUserManager.clear() } catch (_: Exception) {}
            try { com.example.crypto.VaultSecurityManager.lock() } catch (_: Exception) {}
        }
    }

    suspend fun touchDevice(context: Context): TouchDeviceResponse? = withContext(Dispatchers.IO) {
        val appCtx = context.applicationContext ?: context
        if (revocationTriggered.get()) return@withContext null

        val uid = SupabaseSessionManager.ensureActiveSession(appCtx) ?: return@withContext null
        if (uid.isBlank() || revocationTriggered.get()) return@withContext null

        val auth = SupabaseSessionManager.getAuthenticatedBearerToken(appCtx) ?: return@withContext null
        val meta = DeviceIdentity.getDeviceMetadata(appCtx)

        try {
            val response = SupabaseClient.api.touchDeviceRpc(
                auth = auth,
                body = TouchDeviceRequest(
                    p_device_id = meta.deviceId,
                    p_device_name = meta.name,
                    p_os = meta.os,
                    p_app_version = meta.appVersion
                )
            )
            if (response.isSuccessful) {
                val parsed = parseTouchDeviceResponse(response.body()?.string())
                if (parsed?.revoked == true) {
                    if (BuildConfig.DEBUG) {
                        Log.w(TAG, "touch_device reported revoked=true; triggering local sign-out")
                    }
                    triggerLocalSignOutForRevocation(appCtx, showNotice = true)
                }
                return@withContext parsed
            } else {
                val errBody = try { response.errorBody()?.string() } catch (_: Exception) { null }
                val errCode = extractErrorCode(errBody)
                if (response.code() == 403 && errCode == "device_revoked") {
                    triggerLocalSignOutForRevocation(appCtx, showNotice = true)
                }
                if (BuildConfig.DEBUG) {
                    Log.w(TAG, "touch_device HTTP ${response.code()} code=$errCode")
                }
                null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.w(TAG, "touch_device failed: ${e.javaClass.simpleName}")
            }
            null
        }
    }

    suspend fun getMyDevices(context: Context): Result<List<UserDeviceRow>> = withContext(Dispatchers.IO) {
        val appCtx = context.applicationContext ?: context
        try {
            val uid = SupabaseSessionManager.ensureActiveSession(appCtx)
            if (uid.isNullOrBlank()) {
                return@withContext Result.failure(IllegalStateException("Not authenticated"))
            }
            val auth = SupabaseSessionManager.getAuthenticatedBearerToken(appCtx)
                ?: return@withContext Result.failure(IllegalStateException("Not authenticated"))
            val currentDeviceId = DeviceIdentity.getDeviceId(appCtx)

            val response = SupabaseClient.api.getMyDevicesRpc(
                auth = auth,
                body = CurrentDeviceRequest(p_current_device_id = currentDeviceId)
            )
            if (response.isSuccessful) {
                val raw = response.body()?.string()
                Result.success(parseDeviceRows(raw))
            } else {
                val errBody = try { response.errorBody()?.string() } catch (_: Exception) { null }
                val errCode = extractErrorCode(errBody)
                if (response.code() == 403 && errCode == "device_revoked") {
                    triggerLocalSignOutForRevocation(appCtx, showNotice = true)
                }
                Result.failure(IllegalStateException("Failed to load devices (${response.code()})"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun revokeDevice(
        context: Context,
        device: UserDeviceRow
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val appCtx = context.applicationContext ?: context
        try {
            val auth = SupabaseSessionManager.getAuthenticatedBearerToken(appCtx)
                ?: return@withContext Result.failure(IllegalStateException("Not authenticated"))
            val response = SupabaseClient.api.revokeDeviceRpc(
                auth = auth,
                body = RevokeDeviceRequest(p_device_row_id = device.id)
            )
            if (response.isSuccessful) {
                if (device.is_current) {
                    triggerLocalSignOutForRevocation(appCtx, showNotice = false)
                }
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException("Could not log out device (${response.code()})"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun revokeOtherDevices(context: Context): Result<Unit> = withContext(Dispatchers.IO) {
        val appCtx = context.applicationContext ?: context
        try {
            val auth = SupabaseSessionManager.getAuthenticatedBearerToken(appCtx)
                ?: return@withContext Result.failure(IllegalStateException("Not authenticated"))
            val currentDeviceId = DeviceIdentity.getDeviceId(appCtx)
            val response = SupabaseClient.api.revokeOtherDevicesRpc(
                auth = auth,
                body = CurrentDeviceRequest(p_current_device_id = currentDeviceId)
            )
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException("Could not log out other devices (${response.code()})"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun extractErrorCode(errorBody: String?): String? {
        if (errorBody.isNullOrBlank()) return null
        return try {
            val trimmed = errorBody.trim()
            if (!trimmed.startsWith("{")) return null
            JSONObject(trimmed).optString("code", "").trim().takeIf { it.isNotEmpty() }
        } catch (_: Exception) {
            null
        }
    }

    fun parseTouchDeviceResponse(raw: String?): TouchDeviceResponse? {
        if (raw.isNullOrBlank()) return null
        return try {
            val trimmed = raw.trim()
            val obj = when {
                trimmed.startsWith("[") -> {
                    val arr = JSONArray(trimmed)
                    if (arr.length() == 0) return null
                    arr.getJSONObject(0)
                }
                trimmed.startsWith("{") -> JSONObject(trimmed)
                else -> return null
            }
            TouchDeviceResponse(
                success = obj.optBoolean("success", false),
                known = obj.optBoolean("known", false),
                revoked = obj.optBoolean("revoked", false)
            )
        } catch (_: Exception) {
            null
        }
    }

    fun parseDeviceRows(raw: String?): List<UserDeviceRow> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val trimmed = raw.trim()
            val arr = when {
                trimmed.startsWith("[") -> JSONArray(trimmed)
                trimmed.startsWith("{") -> {
                    val obj = JSONObject(trimmed)
                    obj.optJSONArray("devices") ?: obj.optJSONArray("data") ?: JSONArray().put(obj)
                }
                else -> return emptyList()
            }
            val list = ArrayList<UserDeviceRow>(arr.length())
            for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i) ?: continue
                val id = item.opt("id")?.toString()?.trim().orEmpty()
                if (id.isBlank()) continue
                list.add(
                    UserDeviceRow(
                        id = id,
                        device_name = item.optString("device_name", "").ifBlank { "Android Device" },
                        os = item.optString("os", "").ifBlank { "Android" },
                        app_version = item.optString("app_version", ""),
                        last_login_at = item.optString("last_login_at", "").takeIf { it.isNotBlank() && it != "null" },
                        last_seen_at = item.optString("last_seen_at", "").takeIf { it.isNotBlank() && it != "null" },
                        revoked = item.optBoolean("revoked", false),
                        is_current = item.optBoolean("is_current", false)
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }
}

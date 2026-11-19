package com.example

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject

data class AppFlags(
    val coinStoreEnabled: Boolean = false,
    val vipEnabled: Boolean = false,
    val minSupportedVersionCode: Int = 0
)

/**
 * Reads remote feature/release flags from Supabase `get_app_flags` RPC (anon-readable, pre-login).
 *
 * Security & fail-closed guarantees:
 * - Defaults to `AppFlags(coinStoreEnabled = false, vipEnabled = false, minSupportedVersionCode = 0)`.
 * - Fails closed on network failure, 3s timeout, unexpected payload, or missing/non-typed keys.
 * - Caches the last valid server payload in SharedPreferences (`app_flags_cache`) for immediate cold-start reads.
 * - Debug overrides are compiled/applied ONLY when `BuildConfig.DEBUG` is true and never exist in release UI.
 */
object AppFlagsRepository {
    private const val TAG = "AppFlagsRepository"
    private const val PREFS_NAME = "app_flags_cache"
    private const val KEY_HAS_CACHED = "has_cached"
    private const val KEY_COIN_STORE = "coin_store_enabled"
    private const val KEY_VIP = "vip_enabled"
    private const val KEY_MIN_VERSION = "min_supported_version_code"
    private const val STALE_AFTER_MS = 10L * 60L * 1000L
    private const val RPC_TIMEOUT_MS = 3000L

    const val UPDATE_URL = "https://funkytalk.app/download" // TODO: replace with release download URL

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val refreshMutex = Mutex()

    @Volatile
    private var initialized = false

    @Volatile
    private var lastFetchAtMs: Long = 0L

    @Volatile
    private var serverFlags = AppFlags()

    // Debug-only overrides (never settable or active in release builds)
    @Volatile
    private var debugOverrideCoinStore: Boolean? = null

    @Volatile
    private var debugOverrideVip: Boolean? = null

    @Volatile
    private var debugOverrideMinVersion: Int? = null

    private val _flags = MutableStateFlow(AppFlags())
    val flags: StateFlow<AppFlags> = _flags.asStateFlow()

    fun init(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            if (prefs.getBoolean(KEY_HAS_CACHED, false)) {
                serverFlags = AppFlags(
                    coinStoreEnabled = prefs.getBoolean(KEY_COIN_STORE, false),
                    vipEnabled = prefs.getBoolean(KEY_VIP, false),
                    minSupportedVersionCode = prefs.getInt(KEY_MIN_VERSION, 0).coerceAtLeast(0)
                )
                publishEffectiveFlags()
            } else {
                serverFlags = AppFlags()
                publishEffectiveFlags()
            }
            initialized = true
        }
    }

    fun refreshAsync(context: Context) {
        val appContext = context.applicationContext
        init(appContext)
        scope.launch {
            refresh(appContext)
        }
    }

    fun refreshIfStale(context: Context) {
        val appContext = context.applicationContext
        init(appContext)
        val now = System.currentTimeMillis()
        if (lastFetchAtMs == 0L || (now - lastFetchAtMs) > STALE_AFTER_MS) {
            scope.launch {
                refresh(appContext)
            }
        }
    }

    suspend fun refresh(context: Context): AppFlags {
        val appContext = context.applicationContext
        init(appContext)
        return refreshMutex.withLock {
            val fetched = withTimeoutOrNull(RPC_TIMEOUT_MS) {
                try {
                    val response = SupabaseClient.api.getAppFlags(
                        apiKey = BuildConfig.SUPABASE_ANON_KEY,
                        auth = "Bearer ${BuildConfig.SUPABASE_ANON_KEY}"
                    )
                    if (!response.isSuccessful) {
                        if (BuildConfig.DEBUG) {
                            Log.w(TAG, "get_app_flags returned HTTP ${response.code()}; failing closed")
                        }
                        return@withTimeoutOrNull null
                    }
                    val raw = response.body()?.string()?.trim().orEmpty()
                    parseFlagsJson(raw)
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    if (BuildConfig.DEBUG) {
                        Log.w(TAG, "get_app_flags failed: ${e.javaClass.simpleName}; failing closed")
                    }
                    null
                }
            }

            if (fetched != null) {
                serverFlags = fetched
                lastFetchAtMs = System.currentTimeMillis()
                appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean(KEY_HAS_CACHED, true)
                    .putBoolean(KEY_COIN_STORE, fetched.coinStoreEnabled)
                    .putBoolean(KEY_VIP, fetched.vipEnabled)
                    .putInt(KEY_MIN_VERSION, fetched.minSupportedVersionCode)
                    .apply()
            } else {
                // Fail closed when RPC fails, times out, or returns an unexpected payload.
                serverFlags = AppFlags()
            }
            publishEffectiveFlags()
            _flags.value
        }
    }

    private fun parseFlagsJson(raw: String): AppFlags? {
        if (raw.isBlank() || !raw.startsWith("{")) return null
        return try {
            val obj = JSONObject(raw)
            val coinStore = when (val v = obj.opt(KEY_COIN_STORE)) {
                is Boolean -> v
                else -> false
            }
            val vip = when (val v = obj.opt(KEY_VIP)) {
                is Boolean -> v
                else -> false
            }
            val minVersion = when (val v = obj.opt(KEY_MIN_VERSION)) {
                is Number -> v.toInt().coerceAtLeast(0)
                else -> 0
            }
            AppFlags(
                coinStoreEnabled = coinStore,
                vipEnabled = vip,
                minSupportedVersionCode = minVersion
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun publishEffectiveFlags() {
        _flags.value = if (BuildConfig.DEBUG) {
            AppFlags(
                coinStoreEnabled = debugOverrideCoinStore ?: serverFlags.coinStoreEnabled,
                vipEnabled = debugOverrideVip ?: serverFlags.vipEnabled,
                minSupportedVersionCode = debugOverrideMinVersion ?: serverFlags.minSupportedVersionCode
            )
        } else {
            serverFlags
        }
    }

    // -------------------------------------------------------------------------
    // Debug-only overrides (no-op in release builds)
    // -------------------------------------------------------------------------
    fun setDebugCoinStoreOverride(enabled: Boolean?) {
        if (!BuildConfig.DEBUG) return
        debugOverrideCoinStore = enabled
        publishEffectiveFlags()
    }

    fun setDebugVipOverride(enabled: Boolean?) {
        if (!BuildConfig.DEBUG) return
        debugOverrideVip = enabled
        publishEffectiveFlags()
    }

    fun setDebugMinVersionOverride(versionCode: Int?) {
        if (!BuildConfig.DEBUG) return
        debugOverrideMinVersion = versionCode?.coerceAtLeast(0)
        publishEffectiveFlags()
    }

    fun hasDebugOverrides(): Boolean {
        if (!BuildConfig.DEBUG) return false
        return debugOverrideCoinStore != null || debugOverrideVip != null || debugOverrideMinVersion != null
    }

    fun clearDebugOverrides() {
        if (!BuildConfig.DEBUG) return
        debugOverrideCoinStore = null
        debugOverrideVip = null
        debugOverrideMinVersion = null
        publishEffectiveFlags()
    }
}

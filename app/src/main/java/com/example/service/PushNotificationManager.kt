package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.BuildConfig
import com.example.GoogleServicesConfig
import com.example.MainActivity
import com.example.PushTokenRequest
import com.example.R
import com.example.RegisterPushTokenRequest
import com.example.SupabaseClient
import com.example.SupabaseSessionManager
import com.example.crypto.VaultSecurityManager
import com.example.repository.EncryptedChatRepository
import com.example.repository.GroupChatRepository
import com.example.util.SystemNotificationHelper
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PendingChatNavigationTarget(
    val kind: String? = null,           // "dm" or "group"
    val conversationId: String? = null,
    val groupId: String? = null,
    val senderId: String? = null,
    val partnerUserId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class PushDebugState(
    val isRegistered: Boolean = false,
    val lastToken: String? = null,
    val lastUserId: String? = null,
    val lastRegisteredAtMillis: Long = 0L,
    val lastHttpStatus: String? = null,
    val lastError: String? = null,
    val isReRegistering: Boolean = false
) {
    val tokenSuffix: String
        get() = lastToken?.takeIf { it.isNotBlank() }?.takeLast(6) ?: "none"

    val formattedLastRegisteredTime: String
        get() = if (lastRegisteredAtMillis > 0L) {
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(lastRegisteredAtMillis))
        } else {
            "never"
        }

    val summaryLine: String
        get() {
            val statusStr = if (isRegistered) "registered" else "not registered"
            return "Push: $statusStr · Token: …$tokenSuffix · Last: $formattedLastRegisteredTime"
        }

    val httpStatusLine: String
        get() {
            val statusPart = lastHttpStatus?.takeIf { it.isNotBlank() } ?: if (isRegistered) "HTTP 200 OK" else "none"
            val errPart = lastError?.takeIf { it.isNotBlank() }
            return if (errPart != null && !statusPart.contains(errPart)) {
                "RPC status: $statusPart · Error: $errPart"
            } else {
                "RPC status: $statusPart"
            }
        }
}

object PushNotificationManager : DefaultLifecycleObserver {
    private const val TAG = "PushNotificationManager"
    const val CHANNEL_ID_MESSAGES = "messages"
    private const val CHANNEL_NAME_MESSAGES = "Messages & Chats"

    private const val PREFS_NAME = "push_notification_debug_prefs"
    private const val KEY_IS_REGISTERED = "is_registered"
    private const val KEY_LAST_TOKEN = "last_token"
    private const val KEY_LAST_USER_ID = "last_user_id"
    private const val KEY_LAST_REGISTERED_AT = "last_registered_at"
    private const val KEY_LAST_HTTP_STATUS = "last_http_status"
    private const val KEY_LAST_ERROR = "last_error"

    private const val MAX_RETRY_ATTEMPTS = 3
    private const val DEBOUNCE_WINDOW_MS = 60_000L

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val registrationMutex = Mutex()

    @Volatile
    private var activeRegistrationJob: Job? = null

    @Volatile
    private var isObserverInitialized = false

    private val coldStartTriggered = java.util.concurrent.atomic.AtomicBoolean(false)

    @Volatile
    private var appContext: Context? = null

    @Volatile
    var isAppInForeground: Boolean = false
        private set

    @Volatile
    var activeDmConversationId: String? = null
        private set

    @Volatile
    var activeDmPartnerUserId: String? = null
        private set

    private val _debugState = MutableStateFlow(PushDebugState())
    val debugState: StateFlow<PushDebugState> = _debugState.asStateFlow()

    private val _pendingChatNavigation = MutableStateFlow<PendingChatNavigationTarget?>(null)
    val pendingChatNavigation: StateFlow<PendingChatNavigationTarget?> = _pendingChatNavigation.asStateFlow()

    fun setPendingChatNavigation(target: PendingChatNavigationTarget?) {
        _pendingChatNavigation.value = target
    }

    fun clearPendingChatNavigation() {
        _pendingChatNavigation.value = null
    }

    fun triggerInboxDrainOnTap(context: Context) {
        val appCtx = context.applicationContext
        scope.launch {
            try {
                EncryptedChatRepository.drainPendingDmMessages(appCtx)
                GroupChatRepository.drainPendingGroupMessages(appCtx)
            } catch (e: Exception) {
                Log.w(TAG, "Error draining inbox on push tap: ${e.message}")
            }
        }
    }

    fun setActiveOpenDm(conversationId: String?, partnerUserId: String?) {
        activeDmConversationId = conversationId?.takeIf { it.isNotBlank() }
        activeDmPartnerUserId = partnerUserId?.takeIf { it.isNotBlank() }
    }

    fun isChatOpenInForeground(
        conversationId: String? = null,
        groupId: String? = null,
        partnerUserId: String? = null
    ): Boolean {
        if (!isAppInForeground) return false
        if (!groupId.isNullOrBlank() && GroupChatRepository.activeOpenGroupId == groupId) {
            return true
        }
        if (!conversationId.isNullOrBlank() && activeDmConversationId == conversationId) {
            return true
        }
        if (!partnerUserId.isNullOrBlank() && activeDmPartnerUserId == partnerUserId) {
            return true
        }
        return false
    }

    fun notificationIdForChat(
        conversationId: String? = null,
        groupId: String? = null,
        fallbackPartnerId: String? = null
    ): Int {
        return when {
            !conversationId.isNullOrBlank() -> "dm_${conversationId.trim()}".hashCode()
            !groupId.isNullOrBlank() -> "group_${groupId.trim()}".hashCode()
            !fallbackPartnerId.isNullOrBlank() -> "dm_partner_${fallbackPartnerId.trim()}".hashCode()
            else -> "funkytalk_push_default".hashCode()
        }
    }

    fun cancelNotificationForChat(
        context: Context,
        conversationId: String? = null,
        groupId: String? = null,
        partnerUserId: String? = null
    ) {
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            if (!conversationId.isNullOrBlank()) {
                nm.cancel(notificationIdForChat(conversationId = conversationId))
            }
            if (!groupId.isNullOrBlank()) {
                nm.cancel(notificationIdForChat(groupId = groupId))
            }
            if (!partnerUserId.isNullOrBlank()) {
                nm.cancel(notificationIdForChat(fallbackPartnerId = partnerUserId))
            }
        } catch (_: Exception) {}
    }

    fun getPermissionAndChannelDebugLine(context: Context): String {
        val permGranted = SystemNotificationHelper.hasNotificationPermission(context)
        val channelEnabled = isMessagesChannelEnabled(context)
        val permStr = if (permGranted) "enabled" else "disabled"
        val chanStr = if (channelEnabled) "enabled" else "disabled"
        return "Permission: $permStr · Channel \"messages\": $chanStr"
    }

    fun isMessagesChannelEnabled(context: Context): Boolean {
        return try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return false
            if (!nm.areNotificationsEnabled()) return false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val ch = nm.getNotificationChannel(CHANNEL_ID_MESSAGES) ?: return false
                ch.importance != NotificationManager.IMPORTANCE_NONE
            } else {
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    fun cancelInFlightRegistration() {
        try {
            activeRegistrationJob?.cancel()
        } catch (_: Exception) {}
        activeRegistrationJob = null
    }

    fun initialize(context: Context) {
        val appCtx = context.applicationContext
        appContext = appCtx
        createNotificationChannels(appCtx)
        loadDebugStateFromPrefs(appCtx)

        if (!isObserverInitialized) {
            isObserverInitialized = true
            try {
                ProcessLifecycleOwner.get().lifecycle.addObserver(this)
                Log.d(TAG, "PushNotificationManager registered with ProcessLifecycleOwner")
            } catch (e: Exception) {
                Log.e(TAG, "Failed registering ProcessLifecycleOwner observer", e)
            }
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        isAppInForeground = true
        val ctx = appContext ?: return
        createNotificationChannels(ctx)
        if (isUserLoggedIn(ctx)) {
            Log.d(TAG, "App entered foreground (onStart) while logged in -> triggering FCM token registration")
            scope.launch {
                registerCurrentToken(ctx)
            }
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        isAppInForeground = false
    }

    fun onUserLoggedIn(context: Context) {
        val appCtx = context.applicationContext
        appContext = appCtx
        createNotificationChannels(appCtx)
        Log.d(TAG, "[AUTH_LOGIN] onUserLoggedIn triggered -> launching registerCurrentToken(force=true)")
        scope.launch {
            registerCurrentToken(appCtx, force = true)
        }
    }

    fun onColdStart(context: Context) {
        val appCtx = context.applicationContext
        appContext = appCtx
        createNotificationChannels(appCtx)
        if (!coldStartTriggered.compareAndSet(false, true)) {
            Log.d(TAG, "[COLD_START] onColdStart already triggered for this process lifecycle; skipping duplicate")
            return
        }
        if (!isUserLoggedIn(appCtx)) {
            Log.d(TAG, "[COLD_START] User not logged in on cold start; skipping token registration until login")
            return
        }
        Log.d(TAG, "[COLD_START] onColdStart triggered for logged-in user -> launching registerCurrentToken(force=false)")
        scope.launch {
            registerCurrentToken(appCtx, force = false)
        }
    }

    private fun isUserLoggedIn(context: Context): Boolean {
        val supaUid = SupabaseSessionManager.getUserId(context)
        val supaToken = SupabaseSessionManager.getAccessToken(context)
        if (!supaUid.isNullOrBlank() || !supaToken.isNullOrBlank()) return true
        return try {
            ensureFirebaseInitialized(context)
            FirebaseAuth.getInstance().currentUser != null
        } catch (_: Exception) {
            false
        }
    }

    fun ensureFirebaseInitialized(context: Context) {
        try {
            val appCtx = context.applicationContext
            val apps = FirebaseApp.getApps(appCtx)
            if (apps.isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApiKey(GoogleServicesConfig.getApiKey(appCtx))
                    .setApplicationId(GoogleServicesConfig.getApplicationId(appCtx))
                    .setProjectId(GoogleServicesConfig.getProjectId(appCtx))
                    .setGcmSenderId(GoogleServicesConfig.getGcmSenderId(appCtx))
                    .build()
                FirebaseApp.initializeApp(appCtx, options)
                Log.d(TAG, "FirebaseApp initialized dynamically with GCM sender ID")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error ensuring FirebaseApp initialization: ${e.message}", e)
        }
    }

    private fun loadDebugStateFromPrefs(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            _debugState.value = PushDebugState(
                isRegistered = prefs.getBoolean(KEY_IS_REGISTERED, false),
                lastToken = prefs.getString(KEY_LAST_TOKEN, null),
                lastUserId = prefs.getString(KEY_LAST_USER_ID, null),
                lastRegisteredAtMillis = prefs.getLong(KEY_LAST_REGISTERED_AT, 0L),
                lastHttpStatus = prefs.getString(KEY_LAST_HTTP_STATUS, null),
                lastError = prefs.getString(KEY_LAST_ERROR, null),
                isReRegistering = false
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed loading push debug state from SharedPreferences", e)
        }
    }

    private fun updateDebugState(
        context: Context,
        isRegistered: Boolean = _debugState.value.isRegistered,
        lastToken: String? = _debugState.value.lastToken,
        lastUserId: String? = _debugState.value.lastUserId,
        lastRegisteredAtMillis: Long = _debugState.value.lastRegisteredAtMillis,
        lastHttpStatus: String? = _debugState.value.lastHttpStatus,
        lastError: String? = _debugState.value.lastError,
        isReRegistering: Boolean = _debugState.value.isReRegistering
    ) {
        val next = PushDebugState(
            isRegistered = isRegistered,
            lastToken = lastToken,
            lastUserId = lastUserId,
            lastRegisteredAtMillis = lastRegisteredAtMillis,
            lastHttpStatus = lastHttpStatus,
            lastError = lastError,
            isReRegistering = isReRegistering
        )
        _debugState.value = next
        try {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putBoolean(KEY_IS_REGISTERED, isRegistered)
                .putString(KEY_LAST_TOKEN, lastToken)
                .putString(KEY_LAST_USER_ID, lastUserId)
                .putLong(KEY_LAST_REGISTERED_AT, lastRegisteredAtMillis)
                .putString(KEY_LAST_HTTP_STATUS, lastHttpStatus)
                .putString(KEY_LAST_ERROR, lastError)
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed saving push debug state to SharedPreferences", e)
        }
    }

    fun getSavedToken(context: Context): String? {
        return _debugState.value.lastToken?.takeIf { it.isNotBlank() }
            ?: context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_LAST_TOKEN, null)
                ?.takeIf { it.isNotBlank() }
    }

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                val channel = NotificationChannel(
                    CHANNEL_ID_MESSAGES,
                    CHANNEL_NAME_MESSAGES,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "High priority notifications for direct and group chat messages"
                    enableVibration(true)
                    setShowBadge(true)
                    lockscreenVisibility = android.app.Notification.VISIBILITY_PRIVATE
                }
                notificationManager?.createNotificationChannel(channel)
            } catch (e: Exception) {
                Log.e(TAG, "Failed creating notification channel '$CHANNEL_ID_MESSAGES'", e)
            }
        }
    }

    /**
     * Awaits FirebaseMessaging.getInstance().token and calls rpc register_push_token(p_token = token, p_platform = 'android')
     * with exponential backoff (max 3 tries) and 60s debounce when already registered for the same user & token.
     */
    suspend fun registerCurrentToken(context: Context, force: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        val appCtx = context.applicationContext
        createNotificationChannels(appCtx)
        return@withContext runTrackableRegistrationJob {
            registrationMutex.withLock {
                registerTokenWithRetryLocked(
                    context = appCtx,
                    explicitToken = null,
                    force = force,
                    previousTokenToUnregister = null
                )
            }
        }
    }

    /**
     * Registers a specific FCM token (e.g., from onNewToken) with exponential backoff (max 3 tries).
     * If onNewToken rotated from an older token, unregisters the old token on the server first.
     */
    suspend fun registerToken(context: Context, token: String): Boolean = withContext(Dispatchers.IO) {
        val appCtx = context.applicationContext
        createNotificationChannels(appCtx)
        val trimmedNew = token.trim()
        val oldToken = getSavedToken(appCtx)?.takeIf { it.isNotBlank() && it != trimmedNew }
        if (trimmedNew.isNotBlank()) {
            updateDebugState(appCtx, lastToken = trimmedNew)
        }
        return@withContext runTrackableRegistrationJob {
            registrationMutex.withLock {
                registerTokenWithRetryLocked(
                    context = appCtx,
                    explicitToken = trimmedNew,
                    force = true,
                    previousTokenToUnregister = oldToken
                )
            }
        }
    }

    private suspend fun runTrackableRegistrationJob(block: suspend () -> Boolean): Boolean = coroutineScope {
        val job = launch { }
        val currentJob = coroutineContext[Job] ?: job
        activeRegistrationJob = currentJob
        try {
            block()
        } finally {
            if (activeRegistrationJob === currentJob) {
                activeRegistrationJob = null
            }
        }
    }

    private suspend fun registerTokenWithRetryLocked(
        context: Context,
        explicitToken: String?,
        force: Boolean,
        previousTokenToUnregister: String?
    ): Boolean {
        val appCtx = context.applicationContext
        loadDebugStateFromPrefs(appCtx)

        var previousUnregistered = previousTokenToUnregister.isNullOrBlank()

        for (attempt in 1..MAX_RETRY_ATTEMPTS) {
            if (attempt > 1) {
                val backoffMs = 1500L * (1 shl (attempt - 2)) // 1.5s, 3.0s
                Log.d(TAG, "Retrying push token registration (attempt $attempt/$MAX_RETRY_ATTEMPTS) after ${backoffMs}ms...")
                delay(backoffMs)
            }

            try {
                ensureFirebaseInitialized(appCtx)

                // 1. Await FCM token if not explicitly provided, with graceful fallback to cached token
                var fetchThrottled = false
                val token = if (!explicitToken.isNullOrBlank()) {
                    explicitToken.trim()
                } else {
                    val freshToken = try {
                        withTimeoutOrNull(10000L) {
                            FirebaseMessaging.getInstance().token.await()?.trim()
                        }
                    } catch (e: Exception) {
                        val errMsg = e.message ?: ""
                        fetchThrottled = errMsg.contains("TOO_MANY_REGISTRATIONS", ignoreCase = true)
                        if (fetchThrottled) {
                            Log.w(TAG, "FCM returned TOO_MANY_REGISTRATIONS from Google Play Services; falling back to cached token if available.")
                        } else {
                            Log.w(TAG, "Failed retrieving fresh FCM token (${e.javaClass.simpleName}: $errMsg); falling back to cached token.")
                        }
                        null
                    }
                    val resolvedToken = freshToken?.takeIf { it.isNotBlank() } ?: getSavedToken(appCtx)
                    if (resolvedToken.isNullOrBlank() && fetchThrottled) {
                        try {
                            com.google.firebase.installations.FirebaseInstallations.getInstance().delete().await()
                            Log.w(TAG, "Reset FirebaseInstallation after TOO_MANY_REGISTRATIONS")
                        } catch (_: Exception) {}
                        val msg = "FCM token throttled (TOO_MANY_REGISTRATIONS) and no cached token available yet"
                        Log.w(TAG, "Push token registration postponed: $msg")
                        updateDebugState(appCtx, isRegistered = false, lastHttpStatus = "Throttled", lastError = msg)
                        return false
                    }
                    resolvedToken
                }

                if (token.isNullOrBlank()) {
                    val msg = "FirebaseMessaging token unavailable (fresh fetch failed/timed out and no cached token)"
                    Log.w(TAG, "Attempt $attempt/$MAX_RETRY_ATTEMPTS: $msg")
                    updateDebugState(appCtx, isRegistered = false, lastHttpStatus = "Token Empty", lastError = msg)
                    continue
                }

                // Persist the fetched token immediately so suffix is visible in debug UI
                updateDebugState(appCtx, lastToken = token)

                // 2. Ensure active Supabase session and obtain authenticated Bearer token
                SupabaseSessionManager.ensureActiveSession(appCtx)
                val currentUserId = SupabaseSessionManager.getUserId(appCtx)
                val authToken = SupabaseSessionManager.getAuthenticatedBearerToken(appCtx)
                    ?: SupabaseSessionManager.getAccessToken(appCtx)
                        ?.takeIf { it.isNotBlank() && it != BuildConfig.SUPABASE_ANON_KEY && !SupabaseSessionManager.isTokenExpired(it) }
                        ?.let { "Bearer $it" }

                if (authToken.isNullOrBlank() || currentUserId.isNullOrBlank()) {
                    val msg = "No authenticated Supabase session token available (user not logged in yet)"
                    Log.d(TAG, "Push token registration skipped: $msg")
                    updateDebugState(appCtx, isRegistered = false, lastToken = token, lastHttpStatus = "No Session", lastError = msg)
                    return false
                }

                // 3. Debounce check: skip if the same token was registered for the same user in the last 60 seconds unless forced
                val nowCheck = System.currentTimeMillis()
                if (!force &&
                    _debugState.value.isRegistered &&
                    _debugState.value.lastToken == token &&
                    _debugState.value.lastUserId == currentUserId &&
                    (nowCheck - _debugState.value.lastRegisteredAtMillis) in 0..DEBOUNCE_WINDOW_MS
                ) {
                    Log.d(TAG, "Skipping register_push_token (debounced: registered ${(nowCheck - _debugState.value.lastRegisteredAtMillis) / 1000}s ago for same user & token, last6=${token.takeLast(6)})")
                    return true
                }

                // 4. If token rotated from an older token, unregister the old token first
                if (!previousUnregistered && !previousTokenToUnregister.isNullOrBlank() && previousTokenToUnregister != token) {
                    try {
                        val unregResp = SupabaseClient.api.unregisterPushToken(
                            apiKey = BuildConfig.SUPABASE_ANON_KEY,
                            auth = authToken,
                            body = PushTokenRequest(p_token = previousTokenToUnregister)
                        )
                        if (unregResp.isSuccessful) {
                            previousUnregistered = true
                            Log.d(TAG, "Unregistered rotated old push token (last6=${previousTokenToUnregister.takeLast(6)})")
                        } else {
                            Log.w(TAG, "Failed unregistering rotated old push token: HTTP ${unregResp.code()}")
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Exception unregistering rotated old push token: ${e.message}")
                    }
                }

                // 5. Call RPC register_push_token(p_token = token, p_platform = 'android')
                Log.e(
                    TAG,
                    "[PUSH_RPC_CALL] >> BEFORE calling register_push_token: attempt=$attempt/$MAX_RETRY_ATTEMPTS, user=$currentUserId, tokenSuffix=${token.takeLast(6)}, authHeaderLength=${authToken.length}"
                )

                val resp = SupabaseClient.api.registerPushToken(
                    apiKey = BuildConfig.SUPABASE_ANON_KEY,
                    auth = authToken,
                    body = RegisterPushTokenRequest(
                        p_token = token,
                        p_platform = "android"
                    )
                )

                val httpStatusText = "HTTP ${resp.code()} ${resp.message()}".trim()
                val errBody = if (!resp.isSuccessful) {
                    try { resp.errorBody()?.string() } catch (_: Exception) { null }
                } else null

                Log.e(
                    TAG,
                    "[PUSH_RPC_CALL] << AFTER calling register_push_token: code=${resp.code()}, isSuccess=${resp.isSuccessful}, msg=${resp.message()}, errorBody=$errBody"
                )

                if (resp.isSuccessful) {
                    val now = System.currentTimeMillis()
                    updateDebugState(
                        context = appCtx,
                        isRegistered = true,
                        lastToken = token,
                        lastUserId = currentUserId,
                        lastRegisteredAtMillis = now,
                        lastHttpStatus = httpStatusText,
                        lastError = null
                    )
                    Log.d(TAG, "Successfully registered push token on attempt $attempt/$MAX_RETRY_ATTEMPTS (last6=${token.takeLast(6)})")
                    return true
                } else {
                    val errMsg = httpStatusText + (if (!errBody.isNullOrBlank()) " - $errBody" else "")
                    Log.e(TAG, "Attempt $attempt/$MAX_RETRY_ATTEMPTS register_push_token RPC failed: $errMsg")
                    updateDebugState(
                        context = appCtx,
                        isRegistered = false,
                        lastToken = token,
                        lastHttpStatus = httpStatusText,
                        lastError = errMsg
                    )

                    // On HTTP 401 force-refresh the Supabase session before the next retry
                    if (resp.code() == 401 && attempt < MAX_RETRY_ATTEMPTS) {
                        Log.w(TAG, "HTTP 401 on register_push_token -> force-refreshing Supabase session before next retry")
                        SupabaseSessionManager.forceRefreshSession(appCtx)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val errMsg = "${e.javaClass.simpleName}: ${e.message}"
                Log.e(TAG, "Attempt $attempt/$MAX_RETRY_ATTEMPTS threw exception during push token registration: $errMsg", e)
                updateDebugState(
                    context = appCtx,
                    isRegistered = false,
                    lastHttpStatus = e.javaClass.simpleName,
                    lastError = errMsg
                )
            }
        }

        Log.w(TAG, "Push token registration completed with status: ${_debugState.value.lastHttpStatus}, error: ${_debugState.value.lastError}")
        return false
    }

    /**
     * Cancels any in-flight registration job, acquires registrationMutex,
     * deletes the current FCM token (FirebaseMessaging.getInstance().deleteToken()),
     * fetches a brand-new FCM token, and registers it with Supabase.
     */
    suspend fun forceReregisterPush(context: Context): Boolean = withContext(Dispatchers.IO) {
        val appCtx = context.applicationContext
        cancelInFlightRegistration()
        updateDebugState(appCtx, isReRegistering = true, lastError = null)
        return@withContext registrationMutex.withLock {
            try {
                ensureFirebaseInitialized(appCtx)
                val oldToken = getSavedToken(appCtx)

                try {
                    FirebaseMessaging.getInstance().deleteToken().await()
                    Log.d(TAG, "Deleted old FCM token before re-registering")
                } catch (e: Exception) {
                    Log.e(TAG, "Error deleting old FCM token during re-register: ${e.message}", e)
                }

                val newToken = try {
                    FirebaseMessaging.getInstance().token.await()?.trim()
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching new FCM token after deleteToken: ${e.message}", e)
                    null
                }

                val ok = registerTokenWithRetryLocked(
                    context = appCtx,
                    explicitToken = newToken,
                    force = true,
                    previousTokenToUnregister = oldToken
                )
                updateDebugState(appCtx, isReRegistering = false)
                ok
            } catch (e: CancellationException) {
                updateDebugState(appCtx, isReRegistering = false)
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error in forceReregisterPush: ${e.message}", e)
                updateDebugState(
                    context = appCtx,
                    isRegistered = false,
                    lastHttpStatus = e.javaClass.simpleName,
                    lastError = "${e.javaClass.simpleName}: ${e.message}",
                    isReRegistering = false
                )
                false
            }
        }
    }

    /**
     * Executes the full logout cleanup on PushNotificationManager's application-level scope:
     * 1. Cancels any in-flight registration job and acquires registrationMutex.
     * 2. Refreshes the Supabase session if expired BEFORE firebaseAuth.signOut() / clearSession
     *    so unregister_push_token always has a valid JWT.
     * 3. Signs out of FirebaseAuth, clears SupabaseSessionManager, locks Vault, and cleans up local state.
     * 4. Calls rpc unregister_push_token(p_token) and deletes the FCM token.
     */
    fun launchApplicationLogoutCleanup(
        context: Context,
        effectiveUid: String,
        firebaseAuth: FirebaseAuth?
    ) {
        val appCtx = context.applicationContext
        cancelInFlightRegistration()

        scope.launch {
            registrationMutex.withLock {
                // 1. Refresh the Supabase session if expired BEFORE firebaseAuth.signOut() / clearSession
                var validBearerToken = SupabaseSessionManager.getAuthenticatedBearerToken(appCtx)
                if (validBearerToken.isNullOrBlank()) {
                    try {
                        ensureFirebaseInitialized(appCtx)
                        if (FirebaseAuth.getInstance().currentUser != null) {
                            Log.d(TAG, "Logout: Supabase JWT missing/expired; refreshing session BEFORE firebaseAuth.signOut()")
                            SupabaseSessionManager.forceRefreshSession(appCtx)
                            validBearerToken = SupabaseSessionManager.getAuthenticatedBearerToken(appCtx)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Logout: Failed refreshing expired session before signOut: ${e.message}")
                    }
                }

                // 2. Capture current push token before signing out
                val tokenToUnregister = getSavedToken(appCtx) ?: try {
                    ensureFirebaseInitialized(appCtx)
                    FirebaseMessaging.getInstance().token.await()?.trim()
                } catch (_: Exception) {
                    null
                }

                // 3. Now sign out of Firebase Auth and clear SupabaseSessionManager & local session state
                try {
                    (firebaseAuth ?: FirebaseAuth.getInstance()).signOut()
                } catch (e: Exception) {
                    Log.w(TAG, "Error calling firebaseAuth.signOut()", e)
                }

                SupabaseSessionManager.clearSession(appCtx)
                if (effectiveUid.isNotEmpty()) {
                    appCtx.getSharedPreferences("funky_talk_prefs", Context.MODE_PRIVATE)
                        .edit()
                        .remove("active_session_id_$effectiveUid")
                        .apply()
                }
                com.example.util.ChatBackupPreferencesManager.clearAll(appCtx)
                try {
                    androidx.work.WorkManager.getInstance(appCtx)
                        .cancelUniqueWork(com.example.service.ChatBackupWorker.WORK_NAME)
                } catch (e: Exception) {
                    Log.w(TAG, "Error cancelling backup work on logout", e)
                }
                VaultSecurityManager.lock()
                try {
                    com.example.util.GoogleDriveBackupHelper.signOut(appCtx)
                } catch (e: Exception) {
                    Log.w(TAG, "Error signing out Drive account on logout", e)
                }

                // 4. Unregister push token on server using the valid pre-signOut JWT and delete local FCM token
                unregisterTokenLocked(
                    appCtx = appCtx,
                    capturedBearerToken = validBearerToken,
                    explicitToken = tokenToUnregister
                )
            }
        }
    }

    /**
     * Cancels any in-flight registration job, acquires registrationMutex,
     * calls rpc unregister_push_token(p_token), and deletes the FCM token.
     */
    suspend fun unregisterCurrentToken(
        context: Context,
        capturedBearerToken: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val appCtx = context.applicationContext
        cancelInFlightRegistration()
        return@withContext registrationMutex.withLock {
            var resolvedBearer = capturedBearerToken?.takeIf { it.isNotBlank() }
                ?: SupabaseSessionManager.getAuthenticatedBearerToken(appCtx)
            if (resolvedBearer.isNullOrBlank()) {
                try {
                    ensureFirebaseInitialized(appCtx)
                    if (FirebaseAuth.getInstance().currentUser != null) {
                        SupabaseSessionManager.forceRefreshSession(appCtx)
                        resolvedBearer = SupabaseSessionManager.getAuthenticatedBearerToken(appCtx)
                    }
                } catch (_: Exception) {}
            }
            unregisterTokenLocked(
                appCtx = appCtx,
                capturedBearerToken = resolvedBearer,
                explicitToken = null
            )
        }
    }

    private suspend fun unregisterTokenLocked(
        appCtx: Context,
        capturedBearerToken: String?,
        explicitToken: String?
    ): Boolean {
        var rpcSuccess = false
        try {
            ensureFirebaseInitialized(appCtx)

            val authToken = capturedBearerToken?.takeIf { it.isNotBlank() }
                ?: SupabaseSessionManager.getAuthenticatedBearerToken(appCtx)

            val token = explicitToken?.takeIf { it.isNotBlank() }
                ?: getSavedToken(appCtx)
                ?: try {
                    FirebaseMessaging.getInstance().token.await()?.trim()
                } catch (e: Exception) {
                    Log.e(TAG, "Error awaiting FCM token during unregisterCurrentToken: ${e.message}", e)
                    null
                }

            if (!token.isNullOrBlank() && !authToken.isNullOrBlank()) {
                try {
                    val resp = SupabaseClient.api.unregisterPushToken(
                        apiKey = BuildConfig.SUPABASE_ANON_KEY,
                        auth = authToken,
                        body = PushTokenRequest(p_token = token)
                    )
                    if (resp.isSuccessful) {
                        rpcSuccess = true
                        Log.d(TAG, "Successfully unregistered push token on server (last6=${token.takeLast(6)})")
                    } else {
                        val errBody = try { resp.errorBody()?.string() } catch (_: Exception) { null }
                        Log.e(TAG, "Failed unregistering push token: HTTP ${resp.code()} ${resp.message()} | body=$errBody")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Exception calling unregister_push_token RPC: ${e.message}", e)
                }
            } else {
                Log.w(TAG, "Skipping unregister_push_token RPC: tokenPresent=${!token.isNullOrBlank()}, authPresent=${!authToken.isNullOrBlank()}")
            }

            try {
                FirebaseMessaging.getInstance().deleteToken().await()
                Log.d(TAG, "Deleted FCM token on logout")
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting FCM token on logout: ${e.message}", e)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error in unregisterCurrentToken", e)
        } finally {
            updateDebugState(
                context = appCtx,
                isRegistered = false,
                lastToken = null,
                lastUserId = null,
                lastRegisteredAtMillis = 0L,
                lastHttpStatus = null,
                lastError = null,
                isReRegistering = false
            )
        }
        return rpcSuccess
    }

    fun showPushNotification(
        context: Context,
        title: String?,
        body: String?,
        data: Map<String, String>
    ) {
        createNotificationChannels(context)
        if (!SystemNotificationHelper.hasNotificationPermission(context)) {
            return
        }

        val conversationId = data["conversation_id"]?.takeIf { it.isNotBlank() }
        val groupId = data["group_id"]?.takeIf { it.isNotBlank() }
        val senderId = (data["sender_id"] ?: data["partnerUserId"])?.takeIf { it.isNotBlank() }

        // Suppress notifications for the conversation that is currently open in the foreground
        if (isChatOpenInForeground(conversationId = conversationId, groupId = groupId, partnerUserId = senderId)) {
            Log.d(TAG, "Suppressing push notification for active foreground conversation")
            return
        }

        val isVaultLocked = VaultSecurityManager.isVaultLocked()
        val displayTitle = if (isVaultLocked) "FunkyTalk" else (title?.takeIf { it.isNotBlank() } ?: "New message")
        val displayBody = if (isVaultLocked) "New encrypted message received • Tap to unlock" else (body?.takeIf { it.isNotBlank() } ?: "You received a new message")

        val notificationId = notificationIdForChat(
            conversationId = conversationId,
            groupId = groupId,
            fallbackPartnerId = senderId
        )

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            data.forEach { (k, v) -> putExtra(k, v) }
            putExtra("from_push", true)
            putExtra("navigate_to", "home")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val publicNotification = NotificationCompat.Builder(context, CHANNEL_ID_MESSAGES)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("FunkyTalk")
            .setContentText("New message")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .build()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_MESSAGES)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(displayTitle)
            .setContentText(displayBody)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicNotification)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(notificationId, notification)
    }
}

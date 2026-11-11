package com.example

import android.content.Context
import android.util.Log
import com.example.realtime.RealtimeTableEvent
import com.example.realtime.VoiceRoomRealtimeManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*

object SupportChatManager {
    private const val TAG = "SupportChatManager"
    private const val PREFS_NAME = "funkytalk_support_prefs"
    private const val KEY_LAST_AUTO_OPEN_ADMIN_MSG_ID = "last_auto_open_admin_msg_id"

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _session = MutableStateFlow<GetMySupportSessionResponse?>(null)
    val session: StateFlow<GetMySupportSessionResponse?> = _session.asStateFlow()

    private val _conversation = MutableStateFlow<SupabaseSupportConversation?>(null)
    val conversation: StateFlow<SupabaseSupportConversation?> = _conversation.asStateFlow()

    private val _messages = MutableStateFlow<List<SupabaseSupportMessage>>(emptyList())
    val messages: StateFlow<List<SupabaseSupportMessage>> = _messages.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    private val _latestMessagePreview = MutableStateFlow<String?>("Chat with FunkyTalk support")
    val latestMessagePreview: StateFlow<String?> = _latestMessagePreview.asStateFlow()

    private val _latestMessageTime = MutableStateFlow<String?>("")
    val latestMessageTime: StateFlow<String?> = _latestMessageTime.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    // Event emitted when support chat should automatically open (one-shot per admin message)
    private val _autoOpenChatEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val autoOpenChatEvent: SharedFlow<Unit> = _autoOpenChatEvent.asSharedFlow()

    private var subscribedConversationId: String? = null
    private var isListening = false
    private var lastAutoOpenMsgId: String = ""

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        lastAutoOpenMsgId = prefs.getString(KEY_LAST_AUTO_OPEN_ADMIN_MSG_ID, "").orEmpty()

        if (!isListening) {
            isListening = true
            scope.launch {
                VoiceRoomRealtimeManager.events.collect { event ->
                    when (event) {
                        is RealtimeTableEvent.SupportMessageChanged -> {
                            Log.d(TAG, "Support message changed via Realtime: ${event.eventType}")
                            fetchSupportSession(context)
                            fetchSupportMessages(context)
                        }
                        is RealtimeTableEvent.SupportConversationChanged -> {
                            Log.d(TAG, "Support conversation changed via Realtime: until=${event.chatOpenUntil}")
                            fetchSupportSession(context)
                        }
                        is RealtimeTableEvent.NotificationChanged -> {
                            val record = event.record
                            val notifType = record?.optString("type")
                            val actionType = record?.optString("action_type")
                            if (notifType == "support_message" && actionType == "support_chat_open") {
                                Log.d(TAG, "Received support_chat_open notification event")
                                fetchSupportSession(context)
                            }
                        }
                        is RealtimeTableEvent.Reconnected -> {
                            fetchSupportSession(context)
                            fetchSupportMessages(context)
                        }
                        else -> Unit
                    }
                }
            }
        }
        fetchSupportSession(context)
    }

    /**
     * Calls get_my_support_session() RPC -> {"open": bool, "reason": "support_opened"|"appeal", "until": timestamp, "conversation_id": uuid, "unread": number, "can_appeal": bool}
     */
    fun fetchSupportSession(context: Context, onComplete: ((GetMySupportSessionResponse?) -> Unit)? = null) {
        scope.launch {
            try {
                val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                if (token.isNullOrBlank()) {
                    _session.value = null
                    withContext(Dispatchers.Main) { onComplete?.invoke(null) }
                    return@launch
                }

                val response = SupabaseClient.api.getMySupportSession(auth = token)
                if (response.isSuccessful && response.body() != null) {
                    val sess = response.body()!!
                    _session.value = sess
                    _unreadCount.value = sess.unread

                    val convId = sess.conversation_id
                    if (!convId.isNullOrBlank() && convId != subscribedConversationId) {
                        subscribedConversationId = convId
                        VoiceRoomRealtimeManager.subscribeSupportMessages(convId)
                    }

                    if (sess.open || !convId.isNullOrBlank()) {
                        fetchSupportMessages(context)
                    }

                    withContext(Dispatchers.Main) { onComplete?.invoke(sess) }
                } else {
                    Log.w(TAG, "getMySupportSession failed: ${response.code()}")
                    withContext(Dispatchers.Main) { onComplete?.invoke(_session.value) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching support session", e)
                withContext(Dispatchers.Main) { onComplete?.invoke(_session.value) }
            }
        }
    }

    fun fetchSupportMessages(context: Context, onComplete: (() -> Unit)? = null) {
        scope.launch {
            try {
                val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                if (token.isNullOrBlank()) return@launch

                val sess = _session.value
                val convId = sess?.conversation_id
                if (convId.isNullOrBlank()) {
                    _messages.value = emptyList()
                    return@launch
                }

                _isLoading.value = true
                val msgRes = SupabaseClient.api.getSupportMessages(
                    auth = token,
                    conversationIdFilter = "eq.$convId"
                )

                if (msgRes.isSuccessful && msgRes.body() != null) {
                    val rawList = msgRes.body()!!
                    val distinctList = rawList
                        .distinctBy { it.id }
                        .sortedBy { parseIsoToMillis(it.created_at) }

                    _messages.value = distinctList

                    val lastMsg = distinctList.lastOrNull()
                    if (lastMsg != null) {
                        _latestMessagePreview.value = if (lastMsg.sender_type == "admin") {
                            lastMsg.body
                        } else {
                            "You: ${lastMsg.body}"
                        }
                        _latestMessageTime.value = formatMessageTime(lastMsg.created_at)

                        val latestAdminMsg = distinctList.lastOrNull { it.sender_type == "admin" }
                        val isSessionOpen = _session.value?.open == true
                        val hasUnread = (_session.value?.unread ?: 0) > 0 || distinctList.any { it.sender_type == "admin" && !it.is_read_by_user }

                        if (isSessionOpen && hasUnread && latestAdminMsg != null && latestAdminMsg.id != lastAutoOpenMsgId) {
                            lastAutoOpenMsgId = latestAdminMsg.id
                            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                                .edit()
                                .putString(KEY_LAST_AUTO_OPEN_ADMIN_MSG_ID, lastAutoOpenMsgId)
                                .apply()
                            Log.d(TAG, "Triggering auto-open support chat for admin message ${latestAdminMsg.id}")
                            _autoOpenChatEvent.tryEmit(Unit)
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching support messages", e)
            } finally {
                _isLoading.value = false
                withContext(Dispatchers.Main) { onComplete?.invoke() }
            }
        }
    }

    fun sendSupportMessage(
        context: Context,
        messageText: String,
        onSuccess: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        val trimmed = messageText.trim()
        if (trimmed.isBlank()) return

        val sess = _session.value
        if (sess == null || !sess.open || isSessionExpired(sess)) {
            onError?.invoke("This chat is closed.")
            return
        }

        scope.launch {
            try {
                _isSending.value = true
                val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                if (token.isNullOrBlank()) {
                    withContext(Dispatchers.Main) {
                        onError?.invoke("Authentication required.")
                    }
                    return@launch
                }

                // Optimistic local preview message
                val tempId = "temp_${System.currentTimeMillis()}"
                val optimisticMsg = SupabaseSupportMessage(
                    id = tempId,
                    conversation_id = sess.conversation_id ?: "pending",
                    sender_type = "user",
                    body = trimmed,
                    is_read_by_user = true,
                    is_read_by_admin = false,
                    created_at = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                    }.format(Date())
                )
                _messages.value = (_messages.value + optimisticMsg).distinctBy { it.id }
                _latestMessagePreview.value = "You: $trimmed"
                _latestMessageTime.value = "Just now"

                val response = SupabaseClient.api.sendSupportMessage(
                    auth = token,
                    body = SendSupportMessageRequest(p_message = trimmed)
                )

                if (response.isSuccessful) {
                    Log.d(TAG, "send_support_message RPC successful")
                    fetchSupportSession(context) { updatedSess ->
                        fetchSupportMessages(context) {
                            onSuccess?.invoke()
                        }
                    }
                } else {
                    val errBody = response.errorBody()?.string().orEmpty()
                    Log.e(TAG, "send_support_message RPC failed: $errBody")
                    val friendlyError = if (errBody.contains("support_chat_closed", ignoreCase = true)) {
                        _session.value = _session.value?.copy(open = false)
                        "This chat is closed."
                    } else {
                        "Failed to send message. Please try again."
                    }
                    withContext(Dispatchers.Main) {
                        onError?.invoke(friendlyError)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Exception calling send_support_message", e)
                withContext(Dispatchers.Main) {
                    onError?.invoke(e.message ?: "Failed to send message")
                }
            } finally {
                _isSending.value = false
            }
        }
    }

    fun markSupportConversationRead(context: Context) {
        _unreadCount.value = 0
        _session.value = _session.value?.copy(unread = 0)
        _messages.value = _messages.value.map {
            if (it.sender_type == "admin") it.copy(is_read_by_user = true) else it
        }

        scope.launch {
            try {
                val token = SupabaseSessionManager.getAuthenticatedBearerToken(context) ?: return@launch
                val res = SupabaseClient.api.markSupportConversationRead(auth = token)
                if (res.isSuccessful) {
                    Log.d(TAG, "mark_support_conversation_read RPC successful")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Error marking support conversation read", e)
            }
        }
    }

    fun isSessionExpired(untilIso: String?): Boolean {
        if (untilIso.isNullOrBlank()) return true
        val targetMillis = parseIsoToMillis(untilIso)
        return targetMillis > 0 && System.currentTimeMillis() >= targetMillis
    }

    fun isSessionExpired(sess: GetMySupportSessionResponse?): Boolean {
        if (sess == null || !sess.open) return true
        if (sess.reason == "appeal") return false
        return isSessionExpired(sess.until)
    }

    fun formatRemainingCountdown(untilIso: String?): String {
        if (untilIso.isNullOrBlank()) return "0m"
        val targetMillis = parseIsoToMillis(untilIso)
        val diffMillis = targetMillis - System.currentTimeMillis()
        if (diffMillis <= 0) return "0m"
        val totalMinutes = (diffMillis / (1000 * 60)).toInt()
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return if (hours > 0) {
            "${hours}h ${minutes}m"
        } else {
            "${minutes}m"
        }
    }

    fun parseIsoToMillis(isoString: String?): Long {
        if (isoString.isNullOrBlank()) return 0L
        return try {
            val clean = isoString.replace("Z", "+0000").replace("T", " ")
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            format.parse(clean)?.time ?: 0L
        } catch (_: Exception) {
            0L
        }
    }

    fun formatMessageTime(isoString: String?): String {
        if (isoString.isNullOrBlank()) return "Just now"
        return try {
            val clean = isoString.replace("Z", "+0000").replace("T", " ")
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val date = format.parse(clean) ?: Date()
            val outFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
            outFormat.format(date)
        } catch (_: Exception) {
            "Recently"
        }
    }
}

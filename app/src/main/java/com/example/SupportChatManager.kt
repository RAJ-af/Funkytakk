package com.example

import android.content.Context
import android.util.Log
import com.example.realtime.RealtimeTableEvent
import com.example.realtime.VoiceRoomRealtimeManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*

object SupportChatManager {
    private const val TAG = "SupportChatManager"
    private const val PREFS_NAME = "funkytalk_support_prefs"
    private const val KEY_READ_IDS = "read_admin_msg_ids"
    private const val KEY_LAST_READ_TIME = "last_read_timestamp"

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _conversation = MutableStateFlow<SupabaseSupportConversation?>(null)
    val conversation: StateFlow<SupabaseSupportConversation?> = _conversation.asStateFlow()

    private val _messages = MutableStateFlow<List<SupabaseSupportMessage>>(emptyList())
    val messages: StateFlow<List<SupabaseSupportMessage>> = _messages.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    private val _latestMessagePreview = MutableStateFlow<String?>("How can we help you today? Tap to chat with FunkyTalk support.")
    val latestMessagePreview: StateFlow<String?> = _latestMessagePreview.asStateFlow()

    private val _latestMessageTime = MutableStateFlow<String?>("")
    val latestMessageTime: StateFlow<String?> = _latestMessageTime.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private var subscribedConversationId: String? = null
    private var isListening = false

    private fun getReadMessageIds(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_READ_IDS, emptySet()) ?: emptySet()
    }

    private fun saveReadMessageIds(context: Context, ids: Set<String>, timestamp: Long) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existing = prefs.getStringSet(KEY_READ_IDS, emptySet()) ?: emptySet()
        prefs.edit()
            .putStringSet(KEY_READ_IDS, existing + ids)
            .putLong(KEY_LAST_READ_TIME, timestamp)
            .apply()
    }

    private fun parseIsoToMillis(isoString: String?): Long {
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

    fun init(context: Context) {
        if (!isListening) {
            isListening = true
            scope.launch {
                VoiceRoomRealtimeManager.events.collect { event ->
                    when (event) {
                        is RealtimeTableEvent.SupportMessageChanged -> {
                            Log.d(TAG, "Support message changed via Realtime: ${event.eventType}")
                            fetchSupportConversationAndMessages(context)
                        }
                        is RealtimeTableEvent.Reconnected -> {
                            fetchSupportConversationAndMessages(context)
                        }
                        else -> Unit
                    }
                }
            }
        }
        fetchSupportConversationAndMessages(context)
    }

    fun fetchSupportConversationAndMessages(context: Context, onComplete: (() -> Unit)? = null) {
        scope.launch {
            try {
                val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                if (token.isNullOrBlank()) {
                    Log.d(TAG, "fetchSupportConversation: No active token")
                    return@launch
                }

                _isLoading.value = true
                val convRes = SupabaseClient.api.getMySupportConversation(auth = token)
                if (convRes.isSuccessful) {
                    val conversations = convRes.body() ?: emptyList()
                    val myConv = conversations.firstOrNull()
                    _conversation.value = myConv

                    if (myConv != null) {
                        val convId = myConv.id
                        if (convId.isNotBlank() && convId != subscribedConversationId) {
                            subscribedConversationId = convId
                            VoiceRoomRealtimeManager.subscribeSupportMessages(convId)
                        }

                        // Fetch messages
                        val msgRes = SupabaseClient.api.getSupportMessages(
                            auth = token,
                            conversationIdFilter = "eq.$convId"
                        )
                        if (msgRes.isSuccessful && msgRes.body() != null) {
                            val rawList = msgRes.body()!!

                            // Deduplicate raw messages by ID and by signature (sender + body + time)
                            val distinctList = rawList
                                .distinctBy { it.id }
                                .distinctBy { "${it.sender_type}|${it.body.trim()}|${it.created_at}" }

                            val locallyReadIds = getReadMessageIds(context)
                            val lastReadTime = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                                .getLong(KEY_LAST_READ_TIME, 0L)

                            val processedList = distinctList.map { msg ->
                                val msgTimestamp = parseIsoToMillis(msg.created_at)
                                if (msg.id in locallyReadIds || (lastReadTime > 0 && msgTimestamp > 0 && msgTimestamp <= lastReadTime)) {
                                    msg.copy(is_read_by_user = true)
                                } else {
                                    msg
                                }
                            }
                            _messages.value = processedList

                            // Calculate unread admin messages strictly
                            val unreadAdmin = processedList.count { it.sender_type == "admin" && !it.is_read_by_user }
                            _unreadCount.value = unreadAdmin

                            // Update latest preview
                            val lastMsg = processedList.lastOrNull()
                            if (lastMsg != null) {
                                _latestMessagePreview.value = if (lastMsg.sender_type == "admin") {
                                    lastMsg.body
                                } else {
                                    "You: ${lastMsg.body}"
                                }
                                _latestMessageTime.value = formatMessageTime(lastMsg.created_at)
                            }
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching support conversation", e)
            } finally {
                _isLoading.value = false
                withContext(Dispatchers.Main) {
                    onComplete?.invoke()
                }
            }
        }
    }

    fun sendSupportMessage(
        context: Context,
        messageText: String,
        reportId: String? = null,
        onSuccess: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        val trimmed = messageText.trim()
        if (trimmed.isBlank()) return

        scope.launch {
            try {
                _isSending.value = true
                val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                if (token.isNullOrBlank()) {
                    withContext(Dispatchers.Main) {
                        onError?.invoke("Authentication required to send support message.")
                    }
                    return@launch
                }

                // Optimistic local preview message
                val tempId = "temp_${System.currentTimeMillis()}"
                val optimisticMsg = SupabaseSupportMessage(
                    id = tempId,
                    conversation_id = _conversation.value?.id,
                    sender_type = "user",
                    body = trimmed,
                    report_id = reportId,
                    is_read_by_user = true,
                    is_read_by_admin = false,
                    created_at = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                    }.format(Date())
                )
                val current = _messages.value
                _messages.value = (current + optimisticMsg).distinctBy { it.id }
                _latestMessagePreview.value = "You: $trimmed"
                _latestMessageTime.value = "Just now"

                val response = SupabaseClient.api.sendSupportMessage(
                    auth = token,
                    body = SendSupportMessageRequest(
                        p_message = trimmed,
                        p_report_id = reportId?.ifBlank { null }
                    )
                )

                if (response.isSuccessful) {
                    Log.d(TAG, "send_support_message RPC successful")
                    fetchSupportConversationAndMessages(context) {
                        onSuccess?.invoke()
                    }
                } else {
                    val err = response.errorBody()?.string() ?: response.message()
                    Log.e(TAG, "send_support_message RPC failed: $err")
                    withContext(Dispatchers.Main) {
                        onError?.invoke("Failed to send message. Please try again.")
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
        // Mark all current admin messages as read in local storage and memory
        val currentMsgs = _messages.value
        val adminIds = currentMsgs.filter { it.sender_type == "admin" }.map { it.id }.toSet()
        saveReadMessageIds(context, adminIds, System.currentTimeMillis())

        _messages.value = currentMsgs.map {
            if (it.sender_type == "admin") it.copy(is_read_by_user = true) else it
        }
        _unreadCount.value = 0

        scope.launch {
            try {
                val token = SupabaseSessionManager.getAuthenticatedBearerToken(context) ?: return@launch
                val res = SupabaseClient.api.markSupportConversationRead(auth = token)
                if (res.isSuccessful) {
                    Log.d(TAG, "mark_support_conversation_read RPC successful")
                } else {
                    Log.w(TAG, "mark_support_conversation_read failed: ${res.code()}")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Error marking support conversation read", e)
            }
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

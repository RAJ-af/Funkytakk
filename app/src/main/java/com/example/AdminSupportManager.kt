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
import java.time.Instant
import java.util.*

object AdminSupportManager {
    private const val TAG = "AdminSupportManager"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _conversations = MutableStateFlow<List<SupabaseSupportConversation>>(emptyList())
    val conversations: StateFlow<List<SupabaseSupportConversation>> = _conversations.asStateFlow()

    private val _messages = MutableStateFlow<List<SupabaseSupportMessage>>(emptyList())
    val messages: StateFlow<List<SupabaseSupportMessage>> = _messages.asStateFlow()

    private val _lastMessagesMap = MutableStateFlow<Map<String, SupabaseSupportMessage>>(emptyMap())
    val lastMessagesMap: StateFlow<Map<String, SupabaseSupportMessage>> = _lastMessagesMap.asStateFlow()

    private val _unreadConversationsSet = MutableStateFlow<Set<String>>(emptySet())
    val unreadConversationsSet: StateFlow<Set<String>> = _unreadConversationsSet.asStateFlow()

    private val _appealUserIdsSet = MutableStateFlow<Set<String>>(emptySet())
    val appealUserIdsSet: StateFlow<Set<String>> = _appealUserIdsSet.asStateFlow()

    private val _unreadConversationsCount = MutableStateFlow(0)
    val unreadConversationsCount: StateFlow<Int> = _unreadConversationsCount.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private var selectedUserId: String? = null
    private var selectedConversationId: String? = null
    private var isListening = false

    fun init(context: Context) {
        if (!isListening) {
            isListening = true
            scope.launch {
                VoiceRoomRealtimeManager.events.collect { event ->
                    when (event) {
                        is RealtimeTableEvent.SupportMessageChanged -> {
                            Log.d(TAG, "Admin: Realtime support message changed")
                            fetchConversations(context)
                            if (!selectedConversationId.isNullOrBlank()) {
                                fetchMessagesForConversation(context, selectedConversationId!!)
                            }
                        }
                        is RealtimeTableEvent.Reconnected -> {
                            fetchConversations(context)
                        }
                        else -> Unit
                    }
                }
            }
        }
        fetchConversations(context)
    }

    fun fetchConversations(context: Context, onComplete: (() -> Unit)? = null) {
        scope.launch {
            try {
                _isLoading.value = true
                SupabaseSessionManager.ensureActiveSession(context)
                val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                if (token.isNullOrBlank()) {
                    Log.e(TAG, "No auth token available for admin support conversations")
                    return@launch
                }

                val response = SupabaseClient.api.getAllSupportConversations(auth = token)
                if (response.isSuccessful && response.body() != null) {
                    val list = response.body()!!
                    _conversations.value = list

                    // Fetch metadata (last messages, unread flags, and appeal statuses)
                    fetchConversationMetadata(context, token, list)
                } else {
                    Log.e(TAG, "Failed to fetch support conversations: ${response.code()}")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching support conversations", e)
            } finally {
                _isLoading.value = false
                withContext(Dispatchers.Main) {
                    onComplete?.invoke()
                }
            }
        }
    }

    private suspend fun fetchConversationMetadata(
        context: Context,
        token: String,
        conversations: List<SupabaseSupportConversation>
    ) {
        val lastMsgs = mutableMapOf<String, SupabaseSupportMessage>()
        val unreadSet = mutableSetOf<String>()
        val appealSet = mutableSetOf<String>()

        // 1. Fetch active moderation statuses for appeal badge check
        try {
            val modRes = SupabaseClient.api.getAllActiveModerationStatuses(auth = token)
            if (modRes.isSuccessful && modRes.body() != null) {
                modRes.body()!!.forEach { mod ->
                    if (!mod.user_id.isNullOrBlank() && !mod.action_type.isNullOrBlank() && mod.action_type != "none") {
                        appealSet.add(mod.user_id)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch moderation statuses for appeal check: ${e.message}")
        }
        _appealUserIdsSet.value = appealSet

        // 2. Fetch messages for each conversation to find last message and unread indicator
        conversations.forEach { conv ->
            if (conv.id.isBlank()) return@forEach
            try {
                val res = SupabaseClient.api.getSupportMessages(
                    auth = token,
                    conversationIdFilter = "eq.${conv.id}"
                )
                if (res.isSuccessful && res.body() != null) {
                    val msgs = res.body()!!
                    if (msgs.isNotEmpty()) {
                        val lastMsg = msgs.maxByOrNull { it.created_at ?: "" } ?: msgs.last()
                        lastMsgs[conv.id] = lastMsg
                    }
                    if (msgs.any { it.sender_type == "user" && !it.is_read_by_admin }) {
                        unreadSet.add(conv.id)
                    }
                }
            } catch (_: Exception) {}
        }

        _lastMessagesMap.value = lastMsgs
        _unreadConversationsSet.value = unreadSet
        _unreadConversationsCount.value = unreadSet.size
    }

    fun selectConversation(context: Context, conversation: SupabaseSupportConversation) {
        selectedConversationId = conversation.id
        selectedUserId = conversation.user_id

        if (!conversation.id.isNullOrBlank()) {
            VoiceRoomRealtimeManager.subscribeSupportMessages(conversation.id)
            fetchMessagesForConversation(context, conversation.id)
        }

        if (!conversation.user_id.isNullOrBlank()) {
            adminMarkRead(context, conversation.user_id!!)
        }
    }

    fun fetchMessagesForConversation(context: Context, conversationId: String) {
        scope.launch {
            try {
                val token = SupabaseSessionManager.getAuthenticatedBearerToken(context) ?: return@launch
                val res = SupabaseClient.api.getSupportMessages(
                    auth = token,
                    conversationIdFilter = "eq.$conversationId"
                )
                if (res.isSuccessful && res.body() != null) {
                    _messages.value = res.body()!!
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching support messages for conversation $conversationId", e)
            }
        }
    }

    fun adminSendMessage(
        context: Context,
        userId: String,
        messageText: String,
        reportId: String? = null,
        openHours: Int = 2,
        onSuccess: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        val trimmed = messageText.trim()
        if (trimmed.isBlank()) {
            onError?.invoke("Message text cannot be empty.")
            return
        }
        if (userId.isBlank()) {
            onError?.invoke("Target user account not found.")
            return
        }

        scope.launch {
            try {
                _isSending.value = true
                val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                if (token.isNullOrBlank()) {
                    withContext(Dispatchers.Main) {
                        onError?.invoke("Staff admin privileges required to send support messages.")
                    }
                    return@launch
                }

                val currentStaffUid = SupabaseSessionManager.getUserId(context)

                val response = SupabaseClient.api.adminSendSupportMessage(
                    auth = token,
                    body = AdminSendSupportMessageRequest(
                        p_user_id = userId,
                        p_message = trimmed,
                        p_report_id = reportId?.ifBlank { null },
                        p_open_hours = openHours
                    )
                )

                if (response.isSuccessful) {
                    Log.d(TAG, "admin_send_support_message RPC successful")
                    if (!selectedConversationId.isNullOrBlank()) {
                        fetchMessagesForConversation(context, selectedConversationId!!)
                    }
                    fetchConversations(context)
                    withContext(Dispatchers.Main) {
                        onSuccess?.invoke()
                    }
                } else {
                    val rawErr = response.errorBody()?.string() ?: response.message()
                    Log.e(TAG, "admin_send_support_message RPC failed: $rawErr")
                    val mapped = mapErrorString(rawErr)
                    withContext(Dispatchers.Main) {
                        onError?.invoke(mapped)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Exception sending admin support message", e)
                withContext(Dispatchers.Main) {
                    onError?.invoke(com.example.util.ErrorMapper.map(e))
                }
            } finally {
                _isSending.value = false
            }
        }
    }

    fun adminCloseSupportChat(
        context: Context,
        userId: String,
        onSuccess: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        if (userId.isBlank()) {
            onError?.invoke("Target user account not found.")
            return
        }
        scope.launch {
            try {
                val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                if (token.isNullOrBlank()) {
                    withContext(Dispatchers.Main) {
                        onError?.invoke("Staff admin privileges required.")
                    }
                    return@launch
                }

                val response = SupabaseClient.api.adminCloseSupportChat(
                    auth = token,
                    body = AdminCloseSupportChatRequest(p_user_id = userId)
                )

                if (response.isSuccessful) {
                    Log.d(TAG, "admin_close_support_chat RPC successful for $userId")
                    fetchConversations(context)
                    if (!selectedConversationId.isNullOrBlank()) {
                        fetchMessagesForConversation(context, selectedConversationId!!)
                    }
                    withContext(Dispatchers.Main) {
                        onSuccess?.invoke()
                    }
                } else {
                    val rawErr = response.errorBody()?.string() ?: response.message()
                    Log.e(TAG, "admin_close_support_chat RPC failed: $rawErr")
                    val mapped = mapErrorString(rawErr)
                    withContext(Dispatchers.Main) {
                        onError?.invoke(mapped)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception closing support chat", e)
                withContext(Dispatchers.Main) {
                    onError?.invoke(com.example.util.ErrorMapper.map(e))
                }
            }
        }
    }

    fun adminMarkRead(context: Context, userId: String) {
        if (userId.isBlank()) return
        scope.launch {
            try {
                val token = SupabaseSessionManager.getAuthenticatedBearerToken(context) ?: return@launch
                val res = SupabaseClient.api.adminMarkSupportConversationRead(
                    auth = token,
                    body = AdminMarkSupportConversationReadRequest(p_user_id = userId)
                )
                if (res.isSuccessful) {
                    Log.d(TAG, "admin_mark_support_conversation_read RPC successful for user $userId")
                    // Update local unread set
                    val currentConv = _conversations.value.find { it.user_id == userId }
                    if (currentConv != null) {
                        _unreadConversationsSet.value = _unreadConversationsSet.value - currentConv.id
                        _unreadConversationsCount.value = _unreadConversationsSet.value.size
                    }
                }
            } catch (_: Exception) {}
        }
    }

    private fun mapErrorString(rawErr: String?): String {
        if (rawErr.isNullOrBlank()) return "Action failed. Please try again."
        return when {
            rawErr.contains("staff_required", ignoreCase = true) -> "Staff admin privileges required to perform this action."
            rawErr.contains("empty_message", ignoreCase = true) -> "Message text cannot be empty."
            rawErr.contains("invalid_open_hours", ignoreCase = true) -> "Invalid chat open duration specified."
            rawErr.contains("target_user_not_found", ignoreCase = true) -> "Target user account not found."
            else -> com.example.util.ErrorMapper.mapHttp(0, rawErr)
        }
    }

    fun formatChatOpenStatus(chatOpenUntil: String?): String {
        if (chatOpenUntil.isNullOrBlank()) return "Chat closed"
        return try {
            val instant = Instant.parse(chatOpenUntil)
            if (instant.isAfter(Instant.now())) {
                val sdf = SimpleDateFormat("HH:mm", Locale.getDefault()).apply {
                    timeZone = TimeZone.getDefault()
                }
                val formatted = sdf.format(Date.from(instant))
                "Chat open until $formatted"
            } else {
                "Chat closed"
            }
        } catch (_: Exception) {
            "Chat closed"
        }
    }

    fun formatRelativeTime(isoString: String?): String {
        if (isoString.isNullOrBlank()) return "Just now"
        return try {
            val clean = isoString.replace("Z", "+0000").replace("T", " ")
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val date = format.parse(clean) ?: Date()
            val now = System.currentTimeMillis()
            val diffMs = now - date.time

            val seconds = diffMs / 1000
            val minutes = seconds / 60
            val hours = minutes / 60
            val days = hours / 24

            when {
                seconds < 45 -> "Just now"
                minutes < 60 -> "${minutes}m ago"
                hours < 24 -> "${hours}h ago"
                days == 1L -> "Yesterday"
                days < 7 -> "${days}d ago"
                else -> {
                    val outFormat = SimpleDateFormat("MMM d", Locale.getDefault())
                    outFormat.format(date)
                }
            }
        } catch (_: Exception) {
            "Recently"
        }
    }

    fun formatDateHeader(isoString: String?): String {
        if (isoString.isNullOrBlank()) return "Today"
        return try {
            val clean = isoString.replace("Z", "+0000").replace("T", " ")
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val date = format.parse(clean) ?: Date()
            val nowCal = Calendar.getInstance()
            val msgCal = Calendar.getInstance().apply { time = date }

            if (nowCal.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR) &&
                nowCal.get(Calendar.DAY_OF_YEAR) == msgCal.get(Calendar.DAY_OF_YEAR)) {
                "Today"
            } else {
                val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
                if (yesterdayCal.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR) &&
                    yesterdayCal.get(Calendar.DAY_OF_YEAR) == msgCal.get(Calendar.DAY_OF_YEAR)) {
                    "Yesterday"
                } else {
                    SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(date)
                }
            }
        } catch (_: Exception) {
            "Messages"
        }
    }
}

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

object AdminSupportManager {
    private const val TAG = "AdminSupportManager"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _conversations = MutableStateFlow<List<SupabaseSupportConversation>>(emptyList())
    val conversations: StateFlow<List<SupabaseSupportConversation>> = _conversations.asStateFlow()

    private val _messages = MutableStateFlow<List<SupabaseSupportMessage>>(emptyList())
    val messages: StateFlow<List<SupabaseSupportMessage>> = _messages.asStateFlow()

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

                    // Fetch messages to calculate unread for admin
                    val unreadCount = calculateUnreadCount(context, token, list)
                    _unreadConversationsCount.value = unreadCount
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

    private suspend fun calculateUnreadCount(
        context: Context,
        token: String,
        conversations: List<SupabaseSupportConversation>
    ): Int {
        var count = 0
        conversations.forEach { conv ->
            try {
                val res = SupabaseClient.api.getSupportMessages(
                    auth = token,
                    conversationIdFilter = "eq.${conv.id}"
                )
                if (res.isSuccessful && res.body() != null) {
                    val msgs = res.body()!!
                    if (msgs.any { it.sender_type == "user" && !it.is_read_by_admin }) {
                        count++
                    }
                }
            } catch (_: Exception) {}
        }
        return count
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
        onSuccess: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        val trimmed = messageText.trim()
        if (trimmed.isBlank() || userId.isBlank()) return

        scope.launch {
            try {
                _isSending.value = true
                val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                if (token.isNullOrBlank()) {
                    withContext(Dispatchers.Main) {
                        onError?.invoke("Authentication required")
                    }
                    return@launch
                }

                val currentStaffUid = SupabaseSessionManager.getUserId(context)

                // Optimistic local add
                val tempMsg = SupabaseSupportMessage(
                    id = "temp_${System.currentTimeMillis()}",
                    conversation_id = selectedConversationId,
                    sender_type = "admin",
                    sender_id = currentStaffUid,
                    body = trimmed,
                    report_id = reportId,
                    is_read_by_user = false,
                    is_read_by_admin = true,
                    created_at = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                    }.format(Date())
                )
                _messages.value = _messages.value + tempMsg

                val response = SupabaseClient.api.adminSendSupportMessage(
                    auth = token,
                    body = AdminSendSupportMessageRequest(
                        p_user_id = userId,
                        p_message = trimmed,
                        p_report_id = reportId?.ifBlank { null }
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
                    val err = response.errorBody()?.string() ?: response.message()
                    Log.e(TAG, "admin_send_support_message RPC failed: $err")
                    withContext(Dispatchers.Main) {
                        onError?.invoke("Failed to send reply. Please try again.")
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Exception sending admin support message", e)
                withContext(Dispatchers.Main) {
                    onError?.invoke(e.localizedMessage ?: "Error sending message")
                }
            } finally {
                _isSending.value = false
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
                }
            } catch (_: Exception) {}
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
}

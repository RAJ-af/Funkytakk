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

object NotificationsManager {
    private const val TAG = "NotificationsManager"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _notifications = MutableStateFlow<List<SupabaseNotification>>(emptyList())
    val notifications: StateFlow<List<SupabaseNotification>> = _notifications.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var currentUserId: String? = null
    private var isListening = false

    fun init(context: Context) {
        if (!isListening) {
            isListening = true
            scope.launch {
                VoiceRoomRealtimeManager.events.collect { event ->
                    when (event) {
                        is RealtimeTableEvent.NotificationChanged -> {
                            if (event.userId.isNullOrBlank() || event.userId == currentUserId) {
                                Log.d(TAG, "Notification changed via Realtime: ${event.eventType}")
                                fetchNotifications(context)
                            }
                        }
                        is RealtimeTableEvent.Reconnected -> {
                            fetchNotifications(context)
                        }
                        else -> Unit
                    }
                }
            }
        }
        fetchNotifications(context)
    }

    fun clear() {
        currentUserId = null
        _notifications.value = emptyList()
        _unreadCount.value = 0
        _isLoading.value = false
    }

    fun fetchNotifications(context: Context) {
        scope.launch {
            try {
                val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                val uid = SupabaseSessionManager.getUserId(context)
                if (token.isNullOrBlank() || uid.isNullOrBlank()) {
                    Log.d(TAG, "fetchNotifications: No active session or user ID, clearing notifications")
                    _notifications.value = emptyList()
                    _unreadCount.value = 0
                    return@launch
                }

                if (uid != currentUserId) {
                    // Switched user account: clear prior account's data immediately
                    _notifications.value = emptyList()
                    _unreadCount.value = 0
                    currentUserId = uid
                    VoiceRoomRealtimeManager.subscribeUserNotifications(uid)
                }

                _isLoading.value = true
                // Strictly filter on database query by user_id = eq.$uid
                val response = SupabaseClient.api.getNotifications(
                    auth = token,
                    userIdFilter = "eq.$uid"
                )
                if (response.isSuccessful && response.body() != null) {
                    // Strictly filter on client side: only ever show the current session's own rows
                    val list = response.body()!!.filter { it.user_id == uid }
                    val prevUnread = _unreadCount.value
                    _notifications.value = list
                    val newUnread = list.count { !it.is_read }
                    _unreadCount.value = newUnread
                    Log.d(TAG, "Fetched ${list.size} notifications, unread: $newUnread")

                    // If new unread notifications arrived, trigger system notification
                    if (newUnread > prevUnread) {
                        val latest = list.find { !it.is_read }
                        if (latest != null) {
                            com.example.util.SystemNotificationHelper.showSystemNotification(
                                context = context,
                                title = latest.title ?: "FunkyTalk Alert",
                                message = latest.message ?: "You have a new update",
                                channelId = com.example.util.SystemNotificationHelper.CHANNEL_UPDATES_ID
                            )
                        }
                    }
                } else {
                    Log.e(TAG, "Failed to fetch notifications: ${response.code()} ${response.message()}")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching notifications", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun markAsRead(context: Context, notificationId: String, onComplete: (() -> Unit)? = null) {
        if (notificationId.isBlank()) return
        
        // Optimistically update local state immediately
        val currentList = _notifications.value
        val target = currentList.find { it.id == notificationId }
        if (target != null && !target.is_read) {
            val updatedList = currentList.map {
                if (it.id == notificationId) it.copy(is_read = true) else it
            }
            _notifications.value = updatedList
            _unreadCount.value = updatedList.count { !it.is_read }
        }

        scope.launch {
            try {
                val token = SupabaseSessionManager.getAuthenticatedBearerToken(context) ?: return@launch
                val response = SupabaseClient.api.updateNotificationRead(
                    auth = token,
                    idFilter = "eq.$notificationId",
                    body = UpdateNotificationReadRequest(is_read = true)
                )
                if (response.isSuccessful) {
                    Log.d(TAG, "Successfully marked notification $notificationId as read")
                } else {
                    Log.w(TAG, "Failed to mark notification as read: ${response.code()}")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Error updating notification read status", e)
            } finally {
                withContext(Dispatchers.Main) {
                    onComplete?.invoke()
                }
            }
        }
    }

    fun markAllAsRead(context: Context) {
        val currentList = _notifications.value
        val unreadItems = currentList.filter { !it.is_read }
        if (unreadItems.isEmpty()) return

        // Optimistically update all to read
        val updatedList = currentList.map { it.copy(is_read = true) }
        _notifications.value = updatedList
        _unreadCount.value = 0

        scope.launch {
            unreadItems.forEach { item ->
                try {
                    val token = SupabaseSessionManager.getAuthenticatedBearerToken(context) ?: return@forEach
                    SupabaseClient.api.updateNotificationRead(
                        auth = token,
                        idFilter = "eq.${item.id}",
                        body = UpdateNotificationReadRequest(is_read = true)
                    )
                } catch (_: Exception) {}
            }
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

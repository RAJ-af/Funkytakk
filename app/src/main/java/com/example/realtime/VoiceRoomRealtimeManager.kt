package com.example.realtime

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.BuildConfig
import com.example.SupabaseRoomParticipant
import com.example.SupabaseVoiceRoom
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.*
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

data class UserSnippet(val name: String, val avatar: String, val countryCode: String = "in")

sealed class RealtimeTableEvent {
    data class VoiceRoomChanged(val eventType: String, val record: JSONObject?) : RealtimeTableEvent()
    data class ParticipantChanged(
        val eventType: String,
        val record: JSONObject?,
        val roomId: String?,
        val userId: String? = null,
        val role: String? = null,
        val isMicEnabled: Boolean? = null,
        val userName: String? = null,
        val userAvatar: String? = null
    ) : RealtimeTableEvent()
    data class RoomMessageInserted(val record: JSONObject?, val roomId: String?) : RealtimeTableEvent()
    data class RoomEmojiBroadcast(val roomId: String, val emoji: String, val userId: String) : RealtimeTableEvent()
    data class RoomChatBroadcast(
        val roomId: String,
        val messageId: String,
        val userId: String,
        val message: String,
        val senderName: String,
        val senderAvatar: String
    ) : RealtimeTableEvent()
    data class SpeakerRequestChanged(
        val eventType: String,
        val record: JSONObject?,
        val roomId: String?,
        val userId: String?,
        val status: String?,
        val requestId: String?,
        val kind: String? = null,
        val invitedBy: String? = null
    ) : RealtimeTableEvent()
    data class ModerationActionEnforced(
        val roomId: String,
        val targetUserId: String,
        val actionType: String,
        val reason: String?
    ) : RealtimeTableEvent()
    data class RoomLifecycleEvent(
        val roomId: String,
        val action: String, // "room_ended", "host_away", "host_returned"
        val hostLeftAt: String? = null
    ) : RealtimeTableEvent()
    data class NotificationChanged(
        val eventType: String,
        val record: JSONObject?,
        val userId: String? = null
    ) : RealtimeTableEvent()
    data class SupportMessageChanged(
        val eventType: String,
        val record: JSONObject?,
        val conversationId: String? = null
    ) : RealtimeTableEvent()
    data class DmMessageReceived(
        val eventType: String,
        val record: JSONObject?,
        val recipientId: String?,
        val senderId: String?,
        val conversationId: String?
    ) : RealtimeTableEvent()
    data class GroupMessageReceived(
        val eventType: String,
        val record: JSONObject?,
        val rowId: String? = null,
        val recipientId: String?,
        val messageId: String?,
        val groupId: String? = null,
        val senderId: String? = null,
        val messageType: String? = null,
        val ciphertext: String? = null,
        val nonce: String? = null,
        val storagePath: String? = null
    ) : RealtimeTableEvent()
    data class GroupEventInserted(
        val eventType: String,
        val record: JSONObject?,
        val groupId: String?,
        val groupEventType: String?,
        val actorId: String?,
        val targetUserId: String?,
        val meta: JSONObject?
    ) : RealtimeTableEvent()
    data class GroupMemberChanged(
        val eventType: String,
        val record: JSONObject?,
        val groupId: String?,
        val userId: String?,
        val role: String?,
        val status: String?
    ) : RealtimeTableEvent()
    data class GroupConversationChanged(
        val eventType: String,
        val record: JSONObject?,
        val groupId: String?
    ) : RealtimeTableEvent()
    data class Reconnected(val timestamp: Long = System.currentTimeMillis()) : RealtimeTableEvent()
}

object VoiceRoomRealtimeManager {
    private const val TAG = "VoiceRoomRealtime"
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private val refCounter = AtomicInteger(1)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var heartbeatJob: Job? = null
    private var reconnectJob: Job? = null

    private val subscribedRoomChannels = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    private val subscribedGroupIds = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    @Volatile
    private var subscribedGroupRecipientId: String? = null

    private val userSnippets = java.util.concurrent.ConcurrentHashMap<String, UserSnippet>()

    fun recordUserSnippet(userId: String, name: String?, avatar: String?, countryCode: String? = null) {
        if (userId.isBlank()) return
        val current = userSnippets[userId]
        val newName = name?.ifBlank { null } ?: current?.name ?: "Learner"
        val newAvatar = avatar?.ifBlank { null } ?: current?.avatar ?: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150"
        val newCountry = countryCode?.ifBlank { null } ?: current?.countryCode ?: "in"
        userSnippets[userId] = UserSnippet(newName, newAvatar, newCountry)
    }

    fun getUserSnippet(userId: String): UserSnippet? = userSnippets[userId]

    private val _roomLiveParticipants = MutableStateFlow<Map<String, List<SupabaseRoomParticipant>>>(emptyMap())
    val roomLiveParticipants: StateFlow<Map<String, List<SupabaseRoomParticipant>>> = _roomLiveParticipants.asStateFlow()

    fun ensureLiveParticipant(
        roomId: String,
        userId: String,
        role: String = "listener",
        isMicEnabled: Boolean = false,
        userName: String? = null,
        userAvatar: String? = null,
        countryCode: String? = null
    ) {
        if (roomId.isBlank() || userId.isBlank() || userId == "anon_user" || userId == "System") return
        recordUserSnippet(userId, userName, userAvatar, countryCode)
        val currentMap = _roomLiveParticipants.value.toMutableMap()
        val currentList = currentMap[roomId]?.toMutableList() ?: mutableListOf()
        val index = currentList.indexOfFirst { it.user_id == userId }
        val updated = SupabaseRoomParticipant(
            room_id = roomId,
            user_id = userId,
            role = role,
            is_mic_enabled = isMicEnabled
        )
        if (index >= 0) {
            currentList[index] = updated
        } else {
            currentList.add(updated)
        }
        currentMap[roomId] = currentList
        _roomLiveParticipants.value = currentMap
    }

    fun removeLiveParticipant(roomId: String, userId: String) {
        if (roomId.isBlank() || userId.isBlank()) return
        val currentMap = _roomLiveParticipants.value.toMutableMap()
        val currentList = currentMap[roomId]?.toMutableList() ?: return
        val removed = currentList.removeAll { it.user_id == userId }
        if (removed) {
            currentMap[roomId] = currentList
            _roomLiveParticipants.value = currentMap
        }
    }

    fun getLiveParticipants(roomId: String): List<SupabaseRoomParticipant> {
        return _roomLiveParticipants.value[roomId] ?: emptyList()
    }

    fun getAllLiveParticipants(): List<SupabaseRoomParticipant> {
        return _roomLiveParticipants.value.values.flatten().distinctBy { "${it.room_id}_${it.user_id}" }
    }

    private val _events = MutableSharedFlow<RealtimeTableEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<RealtimeTableEvent> = _events.asSharedFlow()

    @Volatile
    private var isConnected = false
    @Volatile
    private var currentAuthToken: String? = null

    fun setAuthToken(token: String?) {
        val cleanToken = if (token == BuildConfig.SUPABASE_ANON_KEY) null else token
        if (currentAuthToken != cleanToken) {
            currentAuthToken = cleanToken
            val ws = webSocket
            if (ws != null && isConnected && cleanToken != null) {
                try {
                    val authMsg = JSONObject().apply {
                        put("topic", "realtime:public:speaker_requests")
                        put("event", "access_token")
                        put("payload", JSONObject().apply {
                            put("access_token", cleanToken)
                        })
                        put("ref", refCounter.getAndIncrement().toString())
                    }
                    ws.send(authMsg.toString())
                } catch (e: Exception) {
                    Log.w(TAG, "Error sending access_token update: ${e.message}")
                }
            }
        }
    }

    fun start() {
        if (isConnected || webSocket != null) return
        connect()
    }

    private fun connect() {
        try {
            val host = BuildConfig.SUPABASE_URL.replace("https://", "").replace("http://", "").trimEnd('/')
            val tokenParam = currentAuthToken?.let { "&token=$it" } ?: ""
            val wsUrl = "wss://$host/realtime/v1/websocket?apikey=${BuildConfig.SUPABASE_ANON_KEY}$tokenParam&vsn=1.0.0"

            val request = Request.Builder()
                .url(wsUrl)
                .build()

            webSocket = client.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(ws: WebSocket, response: Response) {
                    Log.d(TAG, "Connected to Supabase Realtime WebSocket")
                    isConnected = true
                    subscribeToTopics(ws)
                    startHeartbeat(ws)
                    _events.tryEmit(RealtimeTableEvent.Reconnected())
                }

                override fun onMessage(ws: WebSocket, text: String) {
                    handleMessage(text)
                }

                override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                    Log.d(TAG, "Realtime WebSocket closed: $code / $reason")
                    isConnected = false
                    cleanup()
                    scheduleReconnect()
                }

                override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                    Log.w(TAG, "Realtime WebSocket failure: ${t.message}")
                    isConnected = false
                    cleanup()
                    scheduleReconnect()
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error initiating Realtime connection", e)
            scheduleReconnect()
        }
    }

    private fun subscribeToTopics(ws: WebSocket) {
        val token = currentAuthToken

        // Subscribe to voice_rooms changes
        val joinVoiceRooms = JSONObject().apply {
            put("topic", "realtime:public:voice_rooms")
            put("event", "phx_join")
            put("payload", JSONObject().apply {
                if (!token.isNullOrBlank()) {
                    put("access_token", token)
                }
                put("config", JSONObject().apply {
                    put("postgres_changes", org.json.JSONArray().apply {
                        put(JSONObject().apply {
                            put("event", "*")
                            put("schema", "public")
                            put("table", "voice_rooms")
                        })
                    })
                })
            })
            put("ref", refCounter.getAndIncrement().toString())
        }
        ws.send(joinVoiceRooms.toString())

        // Subscribe to room_participants changes
        val joinParticipants = JSONObject().apply {
            put("topic", "realtime:public:room_participants")
            put("event", "phx_join")
            put("payload", JSONObject().apply {
                if (!token.isNullOrBlank()) {
                    put("access_token", token)
                }
                put("config", JSONObject().apply {
                    put("postgres_changes", org.json.JSONArray().apply {
                        put(JSONObject().apply {
                            put("event", "*")
                            put("schema", "public")
                            put("table", "room_participants")
                        })
                    })
                })
            })
            put("ref", refCounter.getAndIncrement().toString())
        }
        ws.send(joinParticipants.toString())

        // Subscribe to room_messages INSERT changes
        val joinRoomMessages = JSONObject().apply {
            put("topic", "realtime:public:room_messages")
            put("event", "phx_join")
            put("payload", JSONObject().apply {
                if (!token.isNullOrBlank()) {
                    put("access_token", token)
                }
                put("config", JSONObject().apply {
                    put("postgres_changes", org.json.JSONArray().apply {
                        put(JSONObject().apply {
                            put("event", "INSERT")
                            put("schema", "public")
                            put("table", "room_messages")
                        })
                    })
                })
            })
            put("ref", refCounter.getAndIncrement().toString())
        }
        ws.send(joinRoomMessages.toString())

        // Subscribe to speaker_requests changes
        val joinSpeakerRequests = JSONObject().apply {
            put("topic", "realtime:public:speaker_requests")
            put("event", "phx_join")
            put("payload", JSONObject().apply {
                if (!token.isNullOrBlank()) {
                    put("access_token", token)
                }
                put("config", JSONObject().apply {
                    put("postgres_changes", org.json.JSONArray().apply {
                        put(JSONObject().apply {
                            put("event", "*")
                            put("schema", "public")
                            put("table", "speaker_requests")
                        })
                    })
                })
            })
            put("ref", refCounter.getAndIncrement().toString())
        }
        ws.send(joinSpeakerRequests.toString())

        // Subscribe to public notifications table changes
        val joinNotifications = JSONObject().apply {
            put("topic", "realtime:public:notifications")
            put("event", "phx_join")
            put("payload", JSONObject().apply {
                if (!token.isNullOrBlank()) {
                    put("access_token", token)
                }
                put("config", JSONObject().apply {
                    put("postgres_changes", org.json.JSONArray().apply {
                        put(JSONObject().apply {
                            put("event", "*")
                            put("schema", "public")
                            put("table", "notifications")
                        })
                    })
                })
            })
            put("ref", refCounter.getAndIncrement().toString())
        }
        ws.send(joinNotifications.toString())

        // Subscribe to support_messages table changes
        val joinSupportMessages = JSONObject().apply {
            put("topic", "realtime:public:support_messages")
            put("event", "phx_join")
            put("payload", JSONObject().apply {
                if (!token.isNullOrBlank()) {
                    put("access_token", token)
                }
                put("config", JSONObject().apply {
                    put("postgres_changes", org.json.JSONArray().apply {
                        put(JSONObject().apply {
                            put("event", "*")
                            put("schema", "public")
                            put("table", "support_messages")
                        })
                    })
                })
            })
            put("ref", refCounter.getAndIncrement().toString())
        }
        ws.send(joinSupportMessages.toString())

        // Subscribe to dm_messages table changes
        val joinDmMessages = JSONObject().apply {
            put("topic", "realtime:public:dm_messages")
            put("event", "phx_join")
            put("payload", JSONObject().apply {
                if (!token.isNullOrBlank()) {
                    put("access_token", token)
                }
                put("config", JSONObject().apply {
                    put("postgres_changes", org.json.JSONArray().apply {
                        put(JSONObject().apply {
                            put("event", "*")
                            put("schema", "public")
                            put("table", "dm_messages")
                        })
                    })
                })
            })
            put("ref", refCounter.getAndIncrement().toString())
        }
        ws.send(joinDmMessages.toString())

        // Subscribe to group_events, group_members, and group_conversations changes
        listOf("group_events", "group_members", "group_conversations").forEach { tbl ->
            val joinGroupTbl = JSONObject().apply {
                put("topic", "realtime:public:$tbl")
                put("event", "phx_join")
                put("payload", JSONObject().apply {
                    if (!token.isNullOrBlank()) {
                        put("access_token", token)
                    }
                    put("config", JSONObject().apply {
                        put("postgres_changes", org.json.JSONArray().apply {
                            put(JSONObject().apply {
                                put("event", "*")
                                put("schema", "public")
                                put("table", tbl)
                            })
                        })
                    })
                })
                put("ref", refCounter.getAndIncrement().toString())
            }
            ws.send(joinGroupTbl.toString())
        }

        // Resubscribe to group inbox if registered
        subscribedGroupRecipientId?.let { rid ->
            if (rid.isNotBlank()) {
                subscribeGroupInbox(rid)
            }
        }

        // Resubscribe to per-group channels
        subscribedGroupIds.forEach { gid ->
            subscribeGroupRealtime(gid)
        }

        // Resubscribe to any active room broadcast channels
        subscribedRoomChannels.forEach { roomId ->
            sendJoinRoomChannel(ws, roomId)
            sendJoinRoomSpeakerRequestsChannel(ws, roomId)
        }
    }

    private fun startHeartbeat(ws: WebSocket) {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive && isConnected) {
                delay(25000)
                try {
                    val heartbeat = JSONObject().apply {
                        put("topic", "phoenix")
                        put("event", "heartbeat")
                        put("payload", JSONObject())
                        put("ref", refCounter.getAndIncrement().toString())
                    }
                    ws.send(heartbeat.toString())
                } catch (e: Exception) {
                    Log.w(TAG, "Heartbeat error: ${e.message}")
                }
            }
        }
    }

    private fun handleMessage(text: String) {
        try {
            val json = JSONObject(text)
            val event = json.optString("event")
            val topic = json.optString("topic")
            val payload = json.optJSONObject("payload")

            // Realtime Broadcast Event on per-room channel (e.g. topic "realtime:room:xyz" or "room:xyz")
            if (event == "broadcast" || event == "emoji" || event == "chat_message" || event == "participant_update" || event == "speaker_request" || event == "speaker_request_update") {
                val roomId = when {
                    topic.contains("room:") -> topic.substringAfter("room:").substringBefore(":")
                    else -> ""
                }
                val innerPayload = payload?.optJSONObject("payload") ?: payload
                val broadcastType = innerPayload?.optString("event") ?: innerPayload?.optString("type") ?: event
                val emoji = innerPayload?.optString("emoji") ?: ""
                val chatText = innerPayload?.optString("message") ?: innerPayload?.optString("text") ?: ""
                val msgId = innerPayload?.optString("id") ?: ""
                val userId = innerPayload?.optString("user_id") ?: ""
                val senderName = innerPayload?.optString("sender_name") ?: ""
                val senderAvatar = innerPayload?.optString("sender_avatar") ?: ""

                if (broadcastType == "speaker_request" || broadcastType == "speaker_request_update" || broadcastType == "speaker_request_changed") {
                    val targetRoomId = innerPayload?.optString("room_id")?.ifBlank { roomId } ?: roomId
                    val reqId = innerPayload?.optString("request_id")?.ifBlank { innerPayload?.optString("id") } ?: ""
                    val pUserId = innerPayload?.optString("user_id")?.ifBlank { userId } ?: userId
                    val pStatus = innerPayload?.optString("status") ?: "pending"
                    val action = innerPayload?.optString("action") ?: "requested"
                    val pName = innerPayload?.optString("user_name")?.ifBlank { senderName } ?: senderName
                    val pAvatar = innerPayload?.optString("user_avatar")?.ifBlank { senderAvatar } ?: senderAvatar

                    if (pUserId.isNotBlank()) {
                        recordUserSnippet(pUserId, pName, pAvatar)
                    }

                    Log.d(TAG, "Received broadcast speaker_request ($action / $pStatus) for room '$targetRoomId' reqId='$reqId' user='$pUserId' (name=$pName)")
                    _events.tryEmit(
                        RealtimeTableEvent.SpeakerRequestChanged(
                            eventType = action,
                            record = innerPayload,
                            roomId = targetRoomId,
                            userId = pUserId,
                            status = pStatus,
                            requestId = reqId
                        )
                    )
                } else if (broadcastType == "participant_update" || broadcastType == "participant_changed") {
                    val targetRoomId = innerPayload?.optString("room_id")?.ifBlank { roomId } ?: roomId
                    val action = innerPayload?.optString("action") ?: "UPDATE"
                    val pUserId = innerPayload?.optString("user_id")?.ifBlank { userId } ?: userId
                    val pRole = innerPayload?.optString("role") ?: "listener"
                    val pMic = innerPayload?.optBoolean("is_mic_enabled", false) ?: false
                    val pName = innerPayload?.optString("user_name")?.ifBlank { senderName } ?: senderName
                    val pAvatar = innerPayload?.optString("user_avatar")?.ifBlank { senderAvatar } ?: senderAvatar
                    val pCountry = innerPayload?.optString("country_code")

                    if (targetRoomId.isNotBlank() && pUserId.isNotBlank()) {
                        val actLower = action.lowercase()
                        if (actLower in listOf("joined", "sync_presence", "update", "insert")) {
                            ensureLiveParticipant(targetRoomId, pUserId, pRole, pMic, pName, pAvatar, pCountry)
                        } else if (actLower in listOf("left", "delete", "kicked")) {
                            removeLiveParticipant(targetRoomId, pUserId)
                        }
                    }

                    Log.d(TAG, "Received broadcast participant_update ($action) for room '$targetRoomId' from '$pUserId' (name=$pName, role=$pRole)")
                    _events.tryEmit(
                        RealtimeTableEvent.ParticipantChanged(
                            eventType = action,
                            record = innerPayload,
                            roomId = targetRoomId,
                            userId = pUserId,
                            role = pRole,
                            isMicEnabled = pMic,
                            userName = pName,
                            userAvatar = pAvatar
                        )
                    )
                } else if (roomId.isNotBlank() && emoji.isNotBlank() && broadcastType != "chat_message") {
                    Log.d(TAG, "Received broadcast emoji '$emoji' for room '$roomId' from '$userId'")
                    _events.tryEmit(RealtimeTableEvent.RoomEmojiBroadcast(roomId, emoji, userId))
                } else if (roomId.isNotBlank() && chatText.isNotBlank()) {
                    Log.d(TAG, "Received broadcast chat message for room '$roomId' from '$userId': $chatText")
                    if (userId.isNotBlank() && userId != "anon_user" && userId != "System") {
                        ensureLiveParticipant(roomId, userId, "listener", false, senderName, senderAvatar)
                    }
                    _events.tryEmit(
                        RealtimeTableEvent.RoomChatBroadcast(
                            roomId = roomId,
                            messageId = msgId.ifBlank { "msg_${System.currentTimeMillis()}" },
                            userId = userId,
                            message = chatText,
                            senderName = senderName,
                            senderAvatar = senderAvatar
                        )
                    )
                } else if (broadcastType == "moderation_action") {
                    val targetRoomId = innerPayload?.optString("room_id")?.ifBlank { roomId } ?: roomId
                    val targetUid = innerPayload?.optString("target_user_id") ?: ""
                    val actType = innerPayload?.optString("action_type") ?: ""
                    val modReason = innerPayload?.optString("reason")
                    Log.d(TAG, "Received broadcast moderation_action for target '$targetUid': $actType in room '$targetRoomId'")
                    if (targetUid.isNotBlank()) {
                        _events.tryEmit(
                            RealtimeTableEvent.ModerationActionEnforced(
                                roomId = targetRoomId,
                                targetUserId = targetUid,
                                actionType = actType,
                                reason = modReason
                            )
                        )
                    }
                } else if (broadcastType == "room_lifecycle" || broadcastType == "room_ended") {
                    val targetRoomId = innerPayload?.optString("room_id")?.ifBlank { roomId } ?: roomId
                    val action = innerPayload?.optString("action") ?: broadcastType
                    val hostLeftAt = innerPayload?.optString("host_left_at")
                    Log.d(TAG, "Received broadcast room_lifecycle: action=$action for room $targetRoomId")
                    if (targetRoomId.isNotBlank()) {
                        _events.tryEmit(
                            RealtimeTableEvent.RoomLifecycleEvent(
                                roomId = targetRoomId,
                                action = action,
                                hostLeftAt = hostLeftAt
                            )
                        )
                    }
                }
            } else if (event == "postgres_changes" && payload != null) {
                val data = payload.optJSONObject("data")
                val record = data?.optJSONObject("record") ?: data?.optJSONObject("old_record")
                val eventType = data?.optString("type") ?: ""
                val table = data?.optString("table") ?: ""

                if (table == "room_messages" || topic.contains("room_messages")) {
                    val roomId = record?.optString("room_id")
                    val msgType = record?.optString("message_type")
                    val textMsg = record?.optString("message")
                    Log.d(TAG, "[Realtime] Received room_messages postgres_changes (type=$eventType): roomId=$roomId, message_type='$msgType', text='$textMsg', record=$record")
                    _events.tryEmit(RealtimeTableEvent.RoomMessageInserted(record, roomId))
                } else if (table == "voice_rooms" || topic.contains("voice_rooms")) {
                    _events.tryEmit(RealtimeTableEvent.VoiceRoomChanged(eventType, record))
                } else if (table == "room_participants" || topic.contains("room_participants")) {
                    val roomId = record?.optString("room_id")
                    val pUserId = record?.optString("user_id")
                    val role = record?.optString("role") ?: "listener"
                    val isMic = record?.optBoolean("is_mic_enabled", false) ?: false
                    if (!roomId.isNullOrBlank() && !pUserId.isNullOrBlank()) {
                        if (eventType.equals("DELETE", ignoreCase = true)) {
                            removeLiveParticipant(roomId, pUserId)
                        } else {
                            ensureLiveParticipant(roomId, pUserId, role, isMic)
                        }
                    }
                    _events.tryEmit(
                        RealtimeTableEvent.ParticipantChanged(
                            eventType = eventType,
                            record = record,
                            roomId = roomId,
                            userId = pUserId,
                            role = role,
                            isMicEnabled = isMic
                        )
                    )
                } else if (table == "speaker_requests" || topic.contains("speaker_requests")) {
                    val roomId = record?.optString("room_id")
                    val userId = record?.optString("user_id")
                    val status = record?.optString("status")
                    val reqId = record?.optString("id")
                    val kind = record?.optString("kind")?.takeIf { it.isNotBlank() }
                    val invitedBy = record?.optString("invited_by")?.takeIf { it.isNotBlank() }
                    _events.tryEmit(
                        RealtimeTableEvent.SpeakerRequestChanged(
                            eventType = eventType,
                            record = record,
                            roomId = roomId,
                            userId = userId,
                            status = status,
                            requestId = reqId,
                            kind = kind,
                            invitedBy = invitedBy
                        )
                    )
                } else if (table == "notifications" || topic.contains("notifications")) {
                    val userId = record?.optString("user_id")
                    Log.d(TAG, "[Realtime] Received notifications event ($eventType) for user $userId: $record")
                    _events.tryEmit(
                        RealtimeTableEvent.NotificationChanged(
                            eventType = eventType,
                            record = record,
                            userId = userId
                        )
                    )
                } else if (table == "support_messages" || topic.contains("support_messages")) {
                    val conversationId = record?.optString("conversation_id")
                    Log.d(TAG, "[Realtime] Received support_messages event ($eventType) for conversation $conversationId: $record")
                    _events.tryEmit(
                        RealtimeTableEvent.SupportMessageChanged(
                            eventType = eventType,
                            record = record,
                            conversationId = conversationId
                        )
                    )
                } else if (table == "dm_messages" || topic.contains("dm_messages")) {
                    val recipientId = record?.optString("recipient_id")
                    val senderId = record?.optString("sender_id")
                    val convId = record?.optString("conversation_id")
                    Log.d(TAG, "[Realtime] Received dm_messages event ($eventType) for recipient $recipientId from $senderId")
                    _events.tryEmit(
                        RealtimeTableEvent.DmMessageReceived(
                            eventType = eventType,
                            record = record,
                            recipientId = recipientId,
                            senderId = senderId,
                            conversationId = convId
                        )
                    )
                } else if (table == "group_message_recipients" || topic.contains("group_message_recipients")) {
                    val rowId = record?.optString("id")
                    val recipientId = record?.optString("recipient_id")
                    val messageId = record?.optString("message_id")
                    val groupId = record?.optString("group_id")
                    val senderId = record?.optString("sender_id")
                    val messageType = record?.optString("message_type")
                    val ciphertext = record?.optString("ciphertext")
                    val nonce = record?.optString("nonce")
                    val storagePath = record?.optString("storage_path")
                    Log.d(TAG, "[Realtime] Received group_message_recipients event ($eventType) for recipient $recipientId, messageId $messageId, groupId $groupId")
                    _events.tryEmit(
                        RealtimeTableEvent.GroupMessageReceived(
                            eventType = eventType,
                            record = record,
                            rowId = rowId,
                            recipientId = recipientId,
                            messageId = messageId,
                            groupId = groupId,
                            senderId = senderId,
                            messageType = messageType,
                            ciphertext = ciphertext,
                            nonce = nonce,
                            storagePath = storagePath
                        )
                    )
                } else if (table == "group_events" || topic.contains("group_events")) {
                    val groupId = record?.optString("group_id")
                    val groupEventType = record?.optString("event_type")?.ifBlank { record.optString("type") }
                    val actorId = record?.optString("actor_id")?.ifBlank { record.optString("user_id") }
                    val targetUserId = record?.optString("target_user_id")
                    val meta = record?.optJSONObject("meta") ?: record?.optJSONObject("payload") ?: record?.optJSONObject("data")
                    Log.d(TAG, "[Realtime] Received group_events ($eventType): group=$groupId, type=$groupEventType")
                    _events.tryEmit(
                        RealtimeTableEvent.GroupEventInserted(
                            eventType = eventType,
                            record = record,
                            groupId = groupId,
                            groupEventType = groupEventType,
                            actorId = actorId,
                            targetUserId = targetUserId,
                            meta = meta
                        )
                    )
                } else if (table == "group_members" || topic.contains("group_members")) {
                    val groupId = record?.optString("group_id")
                    val userId = record?.optString("user_id")
                    val role = record?.optString("role")
                    val status = record?.optString("status")
                    Log.d(TAG, "[Realtime] Received group_members ($eventType): group=$groupId, user=$userId, role=$role, status=$status")
                    _events.tryEmit(
                        RealtimeTableEvent.GroupMemberChanged(
                            eventType = eventType,
                            record = record,
                            groupId = groupId,
                            userId = userId,
                            role = role,
                            status = status
                        )
                    )
                } else if (table == "group_conversations" || topic.contains("group_conversations")) {
                    val groupId = record?.optString("id")?.ifBlank { record.optString("group_id") }
                    Log.d(TAG, "[Realtime] Received group_conversations ($eventType): group=$groupId")
                    _events.tryEmit(
                        RealtimeTableEvent.GroupConversationChanged(
                            eventType = eventType,
                            record = record,
                            groupId = groupId
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling message", e)
        }
    }

    fun subscribeGroupInbox(recipientId: String) {
        if (recipientId.isBlank()) return
        subscribedGroupRecipientId = recipientId
        val ws = webSocket ?: return
        try {
            val token = currentAuthToken
            val joinMsg = JSONObject().apply {
                put("topic", "realtime:public:group_message_recipients:recipient_id=eq.$recipientId")
                put("event", "phx_join")
                put("payload", JSONObject().apply {
                    if (!token.isNullOrBlank()) {
                        put("access_token", token)
                    }
                    put("config", JSONObject().apply {
                        put("postgres_changes", org.json.JSONArray().apply {
                            put(JSONObject().apply {
                                put("event", "INSERT")
                                put("schema", "public")
                                put("table", "group_message_recipients")
                                put("filter", "recipient_id=eq.$recipientId")
                            })
                        })
                    })
                })
                put("ref", refCounter.getAndIncrement().toString())
            }
            ws.send(joinMsg.toString())
            Log.d(TAG, "Subscribed to postgres_changes group_message_recipients for recipient:$recipientId")
        } catch (e: Exception) {
            Log.w(TAG, "Error sending join for group_message_recipients recipient channel $recipientId: ${e.message}")
        }
    }

    fun subscribeGroupRealtime(groupId: String) {
        if (groupId.isBlank()) return
        subscribedGroupIds.add(groupId)
        val ws = webSocket ?: return
        try {
            val token = currentAuthToken
            // 1. group_events for groupId
            val joinEvents = JSONObject().apply {
                put("topic", "realtime:public:group_events:group_id=eq.$groupId")
                put("event", "phx_join")
                put("payload", JSONObject().apply {
                    if (!token.isNullOrBlank()) put("access_token", token)
                    put("config", JSONObject().apply {
                        put("postgres_changes", org.json.JSONArray().apply {
                            put(JSONObject().apply {
                                put("event", "INSERT")
                                put("schema", "public")
                                put("table", "group_events")
                                put("filter", "group_id=eq.$groupId")
                            })
                        })
                    })
                })
                put("ref", refCounter.getAndIncrement().toString())
            }
            ws.send(joinEvents.toString())

            // 2. group_members for groupId
            val joinMembers = JSONObject().apply {
                put("topic", "realtime:public:group_members:group_id=eq.$groupId")
                put("event", "phx_join")
                put("payload", JSONObject().apply {
                    if (!token.isNullOrBlank()) put("access_token", token)
                    put("config", JSONObject().apply {
                        put("postgres_changes", org.json.JSONArray().apply {
                            put(JSONObject().apply {
                                put("event", "*")
                                put("schema", "public")
                                put("table", "group_members")
                                put("filter", "group_id=eq.$groupId")
                            })
                        })
                    })
                })
                put("ref", refCounter.getAndIncrement().toString())
            }
            ws.send(joinMembers.toString())

            // 3. group_conversations for groupId
            val joinConv = JSONObject().apply {
                put("topic", "realtime:public:group_conversations:id=eq.$groupId")
                put("event", "phx_join")
                put("payload", JSONObject().apply {
                    if (!token.isNullOrBlank()) put("access_token", token)
                    put("config", JSONObject().apply {
                        put("postgres_changes", org.json.JSONArray().apply {
                            put(JSONObject().apply {
                                put("event", "*")
                                put("schema", "public")
                                put("table", "group_conversations")
                                put("filter", "id=eq.$groupId")
                            })
                        })
                    })
                })
                put("ref", refCounter.getAndIncrement().toString())
            }
            ws.send(joinConv.toString())
        } catch (e: Exception) {
            Log.w(TAG, "Error subscribing to group realtime for $groupId: ${e.message}")
        }
    }

    fun subscribeDmInbox(recipientId: String) {
        if (recipientId.isBlank()) return
        val ws = webSocket ?: return
        try {
            val token = currentAuthToken
            val joinMsg = JSONObject().apply {
                put("topic", "realtime:public:dm_messages:recipient_id=eq.$recipientId")
                put("event", "phx_join")
                put("payload", JSONObject().apply {
                    if (!token.isNullOrBlank()) {
                        put("access_token", token)
                    }
                    put("config", JSONObject().apply {
                        put("postgres_changes", org.json.JSONArray().apply {
                            put(JSONObject().apply {
                                put("event", "INSERT")
                                put("schema", "public")
                                put("table", "dm_messages")
                                put("filter", "recipient_id=eq.$recipientId")
                            })
                        })
                    })
                })
                put("ref", refCounter.getAndIncrement().toString())
            }
            ws.send(joinMsg.toString())
            Log.d(TAG, "Subscribed to postgres_changes dm_messages for recipient:$recipientId")
        } catch (e: Exception) {
            Log.w(TAG, "Error sending join for dm_messages recipient channel $recipientId: ${e.message}")
        }
    }

    fun subscribeUserNotifications(userId: String) {
        if (userId.isBlank()) return
        val ws = webSocket ?: return
        try {
            val token = currentAuthToken
            val joinMsg = JSONObject().apply {
                put("topic", "realtime:public:notifications:user_id=eq.$userId")
                put("event", "phx_join")
                put("payload", JSONObject().apply {
                    if (!token.isNullOrBlank()) {
                        put("access_token", token)
                    }
                    put("config", JSONObject().apply {
                        put("postgres_changes", org.json.JSONArray().apply {
                            put(JSONObject().apply {
                                put("event", "*")
                                put("schema", "public")
                                put("table", "notifications")
                                put("filter", "user_id=eq.$userId")
                            })
                        })
                    })
                })
                put("ref", refCounter.getAndIncrement().toString())
            }
            ws.send(joinMsg.toString())
            Log.d(TAG, "Subscribed to postgres_changes notifications for user:$userId")
        } catch (e: Exception) {
            Log.w(TAG, "Error sending join for notifications user channel $userId: ${e.message}")
        }
    }

    fun subscribeSupportMessages(conversationId: String) {
        if (conversationId.isBlank()) return
        val ws = webSocket ?: return
        try {
            val token = currentAuthToken
            val joinMsg = JSONObject().apply {
                put("topic", "realtime:public:support_messages:conversation_id=eq.$conversationId")
                put("event", "phx_join")
                put("payload", JSONObject().apply {
                    if (!token.isNullOrBlank()) {
                        put("access_token", token)
                    }
                    put("config", JSONObject().apply {
                        put("postgres_changes", org.json.JSONArray().apply {
                            put(JSONObject().apply {
                                put("event", "*")
                                put("schema", "public")
                                put("table", "support_messages")
                                put("filter", "conversation_id=eq.$conversationId")
                            })
                        })
                    })
                })
                put("ref", refCounter.getAndIncrement().toString())
            }
            ws.send(joinMsg.toString())
            Log.d(TAG, "Subscribed to postgres_changes support_messages for conversation:$conversationId")
        } catch (e: Exception) {
            Log.w(TAG, "Error sending join for support_messages channel $conversationId: ${e.message}")
        }
    }

    private fun sendJoinRoomChannel(ws: WebSocket, roomId: String) {
        try {
            val joinMsg = JSONObject().apply {
                put("topic", "realtime:room:$roomId")
                put("event", "phx_join")
                put("payload", JSONObject().apply {
                    put("config", JSONObject().apply {
                        put("broadcast", JSONObject().apply {
                            put("self", true)
                            put("ack", false)
                        })
                        put("presence", JSONObject().apply {
                            put("key", "")
                        })
                    })
                })
                put("ref", refCounter.getAndIncrement().toString())
            }
            ws.send(joinMsg.toString())
            Log.d(TAG, "Subscribed to broadcast channel realtime:room:$roomId")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending join for room channel $roomId", e)
        }
    }

    private fun sendJoinRoomSpeakerRequestsChannel(ws: WebSocket, roomId: String) {
        try {
            val token = currentAuthToken
            val joinMsg = JSONObject().apply {
                put("topic", "realtime:public:speaker_requests:room_id=eq.$roomId")
                put("event", "phx_join")
                put("payload", JSONObject().apply {
                    if (!token.isNullOrBlank()) {
                        put("access_token", token)
                    }
                    put("config", JSONObject().apply {
                        put("postgres_changes", org.json.JSONArray().apply {
                            put(JSONObject().apply {
                                put("event", "*")
                                put("schema", "public")
                                put("table", "speaker_requests")
                                put("filter", "room_id=eq.$roomId")
                            })
                        })
                    })
                })
                put("ref", refCounter.getAndIncrement().toString())
            }
            ws.send(joinMsg.toString())
            Log.d(TAG, "Subscribed to postgres_changes speaker_requests for room:$roomId")
        } catch (e: Exception) {
            Log.w(TAG, "Error sending join for speaker_requests room channel $roomId: ${e.message}")
        }
    }

    fun subscribeRoomSpeakerRequests(roomId: String) {
        if (roomId.isBlank()) return
        val ws = webSocket ?: return
        sendJoinRoomSpeakerRequestsChannel(ws, roomId)
    }

    fun subscribeRoomChannel(roomId: String) {
        if (roomId.isBlank()) return
        subscribedRoomChannels.add(roomId)
        val ws = webSocket ?: return
        sendJoinRoomChannel(ws, roomId)
        sendJoinRoomSpeakerRequestsChannel(ws, roomId)
    }

    fun leaveRoomChannel(roomId: String) {
        if (roomId.isBlank()) return
        subscribedRoomChannels.remove(roomId)
        val ws = webSocket ?: return
        try {
            val leaveMsg = JSONObject().apply {
                put("topic", "realtime:room:$roomId")
                put("event", "phx_leave")
                put("payload", JSONObject())
                put("ref", refCounter.getAndIncrement().toString())
            }
            ws.send(leaveMsg.toString())
            Log.d(TAG, "Left broadcast channel realtime:room:$roomId")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending leave for room channel $roomId", e)
        }
    }

    fun broadcastEmoji(roomId: String, emoji: String, userId: String) {
        if (roomId.isBlank() || emoji.isBlank()) return
        val ws = webSocket ?: return
        try {
            if (!subscribedRoomChannels.contains(roomId)) {
                subscribeRoomChannel(roomId)
            }
            val broadcastMsg = JSONObject().apply {
                put("topic", "realtime:room:$roomId")
                put("event", "broadcast")
                put("payload", JSONObject().apply {
                    put("type", "broadcast")
                    put("event", "emoji")
                    put("payload", JSONObject().apply {
                        put("emoji", emoji)
                        put("user_id", userId)
                    })
                })
                put("ref", refCounter.getAndIncrement().toString())
            }
            ws.send(broadcastMsg.toString())
            Log.d(TAG, "Broadcasted emoji '$emoji' to room:$roomId from $userId")
        } catch (e: Exception) {
            Log.e(TAG, "Error broadcasting emoji to room $roomId", e)
        }
    }

    fun broadcastRoomMessage(
        roomId: String,
        message: String,
        userId: String,
        senderName: String,
        senderAvatar: String,
        messageId: String = "msg_${System.currentTimeMillis()}"
    ) {
        if (roomId.isBlank() || message.isBlank()) return
        val ws = webSocket ?: return
        try {
            if (!subscribedRoomChannels.contains(roomId)) {
                subscribeRoomChannel(roomId)
            }
            val broadcastMsg = JSONObject().apply {
                put("topic", "realtime:room:$roomId")
                put("event", "broadcast")
                put("payload", JSONObject().apply {
                    put("type", "broadcast")
                    put("event", "chat_message")
                    put("payload", JSONObject().apply {
                        put("id", messageId)
                        put("message", message)
                        put("user_id", userId)
                        put("sender_name", senderName)
                        put("sender_avatar", senderAvatar)
                    })
                })
                put("ref", refCounter.getAndIncrement().toString())
            }
            ws.send(broadcastMsg.toString())
            Log.d(TAG, "Broadcasted chat message to room:$roomId from $userId")
        } catch (e: Exception) {
            Log.e(TAG, "Error broadcasting chat message to room $roomId", e)
        }
    }

    fun broadcastRoomReport(
        roomId: String,
        reporterId: String,
        category: String,
        reason: String? = null,
        details: String? = null
    ) {
        if (roomId.isBlank()) return
        val ws = webSocket ?: return
        try {
            if (!subscribedRoomChannels.contains(roomId)) {
                subscribeRoomChannel(roomId)
            }
            val payloadObj = JSONObject().apply {
                put("room_id", roomId)
                put("reporter_id", reporterId)
                put("category", category)
                if (!reason.isNullOrBlank()) put("reason", reason)
                if (!details.isNullOrBlank()) put("details", details)
                put("timestamp", System.currentTimeMillis())
            }
            val broadcastMsg = JSONObject().apply {
                put("topic", "realtime:room:$roomId")
                put("event", "broadcast")
                put("payload", JSONObject().apply {
                    put("type", "broadcast")
                    put("event", "room_reported")
                    put("payload", payloadObj)
                })
                put("ref", refCounter.getAndIncrement().toString())
            }
            ws.send(broadcastMsg.toString())
            Log.d(TAG, "Broadcasted room_reported to room:$roomId by $reporterId")
        } catch (e: Exception) {
            Log.w(TAG, "Error broadcasting room_reported to room $roomId: ${e.message}")
        }
    }

    fun broadcastModerationAction(
        roomId: String,
        targetUserId: String,
        actionType: String,
        reason: String? = null
    ) {
        if (targetUserId.isBlank()) return
        val targetRoom = roomId.ifBlank { "global" }
        val ws = webSocket ?: return
        try {
            if (!subscribedRoomChannels.contains(targetRoom)) {
                subscribeRoomChannel(targetRoom)
            }
            val payloadObj = JSONObject().apply {
                put("room_id", targetRoom)
                put("target_user_id", targetUserId)
                put("action_type", actionType)
                if (!reason.isNullOrBlank()) put("reason", reason)
                put("timestamp", System.currentTimeMillis())
            }
            val broadcastMsg = JSONObject().apply {
                put("topic", "realtime:room:$targetRoom")
                put("event", "broadcast")
                put("payload", JSONObject().apply {
                    put("type", "broadcast")
                    put("event", "moderation_action")
                    put("payload", payloadObj)
                })
                put("ref", refCounter.getAndIncrement().toString())
            }
            ws.send(broadcastMsg.toString())
            Log.d(TAG, "Broadcasted moderation_action for $targetUserId ($actionType) in room $targetRoom")
        } catch (e: Exception) {
            Log.w(TAG, "Error broadcasting moderation_action to room $targetRoom: ${e.message}")
        }
    }

    fun broadcastParticipantUpdate(
        roomId: String,
        userId: String,
        action: String = "joined",
        role: String = "listener",
        isMicEnabled: Boolean = false,
        userName: String? = null,
        userAvatar: String? = null,
        countryCode: String? = null
    ) {
        if (roomId.isBlank()) return

        val actLower = action.lowercase()
        if (actLower in listOf("joined", "sync_presence", "update", "insert")) {
            ensureLiveParticipant(roomId, userId, role, isMicEnabled, userName, userAvatar, countryCode)
        } else if (actLower in listOf("left", "delete", "kicked")) {
            removeLiveParticipant(roomId, userId)
        }

        val ws = webSocket ?: return
        try {
            if (!subscribedRoomChannels.contains(roomId)) {
                subscribeRoomChannel(roomId)
            }
            val payloadObj = JSONObject().apply {
                put("room_id", roomId)
                put("user_id", userId)
                put("action", action)
                put("role", role)
                put("is_mic_enabled", isMicEnabled)
                if (!userName.isNullOrBlank()) put("user_name", userName)
                if (!userAvatar.isNullOrBlank()) put("user_avatar", userAvatar)
                if (!countryCode.isNullOrBlank()) put("country_code", countryCode)
            }

            // 1. Broadcast to room channel: realtime:room:$roomId
            val broadcastMsg = JSONObject().apply {
                put("topic", "realtime:room:$roomId")
                put("event", "broadcast")
                put("payload", JSONObject().apply {
                    put("type", "broadcast")
                    put("event", "participant_update")
                    put("payload", payloadObj)
                })
                put("ref", refCounter.getAndIncrement().toString())
            }
            ws.send(broadcastMsg.toString())

            // 2. Also broadcast to public:room_participants for global/HomeScreen visibility
            val publicBroadcastMsg = JSONObject().apply {
                put("topic", "realtime:public:room_participants")
                put("event", "broadcast")
                put("payload", JSONObject().apply {
                    put("type", "broadcast")
                    put("event", "participant_update")
                    put("payload", payloadObj)
                })
                put("ref", refCounter.getAndIncrement().toString())
            }
            ws.send(publicBroadcastMsg.toString())

            Log.d(TAG, "Broadcasted participant_update ($action) to room:$roomId for user $userId (name=$userName)")
        } catch (e: Exception) {
            Log.e(TAG, "Error broadcasting participant_update to room $roomId", e)
        }
    }

    fun broadcastSpeakerRequest(
        roomId: String,
        requestId: String,
        userId: String,
        action: String = "requested",
        status: String = "pending",
        userName: String? = null,
        userAvatar: String? = null
    ) {
        if (roomId.isBlank()) return
        if (userId.isNotBlank()) {
            recordUserSnippet(userId, userName, userAvatar)
        }
        val ws = webSocket ?: return
        try {
            if (!subscribedRoomChannels.contains(roomId)) {
                subscribeRoomChannel(roomId)
            }
            val payloadObj = JSONObject().apply {
                put("room_id", roomId)
                put("request_id", requestId)
                put("user_id", userId)
                put("action", action)
                put("status", status)
                if (!userName.isNullOrBlank()) put("user_name", userName)
                if (!userAvatar.isNullOrBlank()) put("user_avatar", userAvatar)
                put("timestamp", System.currentTimeMillis())
            }

            // 1. Broadcast to room channel: realtime:room:$roomId
            val broadcastMsg = JSONObject().apply {
                put("topic", "realtime:room:$roomId")
                put("event", "broadcast")
                put("payload", JSONObject().apply {
                    put("type", "broadcast")
                    put("event", "speaker_request")
                    put("payload", payloadObj)
                })
                put("ref", refCounter.getAndIncrement().toString())
            }
            ws.send(broadcastMsg.toString())

            // 2. Also broadcast to public:speaker_requests channel
            val publicBroadcastMsg = JSONObject().apply {
                put("topic", "realtime:public:speaker_requests")
                put("event", "broadcast")
                put("payload", JSONObject().apply {
                    put("type", "broadcast")
                    put("event", "speaker_request")
                    put("payload", payloadObj)
                })
                put("ref", refCounter.getAndIncrement().toString())
            }
            ws.send(publicBroadcastMsg.toString())

            Log.d(TAG, "Broadcasted speaker_request ($action / $status) to room:$roomId for user $userId (reqId=$requestId)")
        } catch (e: Exception) {
            Log.e(TAG, "Error broadcasting speaker_request to room $roomId", e)
        }
    }

    fun broadcastRoomLifecycle(
        roomId: String,
        action: String, // "room_ended", "host_away", "host_returned"
        hostLeftAt: String? = null
    ) {
        if (roomId.isBlank()) return
        val ws = webSocket ?: return
        try {
            if (!subscribedRoomChannels.contains(roomId)) {
                subscribeRoomChannel(roomId)
            }
            val payloadObj = JSONObject().apply {
                put("room_id", roomId)
                put("action", action)
                if (!hostLeftAt.isNullOrBlank()) put("host_left_at", hostLeftAt)
                put("timestamp", System.currentTimeMillis())
            }

            // Broadcast to room channel: realtime:room:$roomId
            val broadcastMsg = JSONObject().apply {
                put("topic", "realtime:room:$roomId")
                put("event", "broadcast")
                put("payload", JSONObject().apply {
                    put("type", "broadcast")
                    put("event", "room_lifecycle")
                    put("payload", payloadObj)
                })
                put("ref", refCounter.getAndIncrement().toString())
            }
            ws.send(broadcastMsg.toString())

            // Also broadcast to public:voice_rooms channel
            val publicBroadcastMsg = JSONObject().apply {
                put("topic", "realtime:public:voice_rooms")
                put("event", "broadcast")
                put("payload", JSONObject().apply {
                    put("type", "broadcast")
                    put("event", "room_lifecycle")
                    put("payload", payloadObj)
                })
                put("ref", refCounter.getAndIncrement().toString())
            }
            ws.send(publicBroadcastMsg.toString())

            Log.d(TAG, "Broadcasted room_lifecycle ($action) to room:$roomId")
        } catch (e: Exception) {
            Log.e(TAG, "Error broadcasting room_lifecycle to room $roomId", e)
        }
    }

    fun subscribeRoomMessages(roomId: String) {
        val ws = webSocket ?: return
        try {
            val joinMsg = JSONObject().apply {
                put("topic", "realtime:public:room_messages:room_id=eq.$roomId")
                put("event", "phx_join")
                put("payload", JSONObject().apply {
                    put("config", JSONObject().apply {
                        put("postgres_changes", org.json.JSONArray().apply {
                            put(JSONObject().apply {
                                put("event", "INSERT")
                                put("schema", "public")
                                put("table", "room_messages")
                                put("filter", "room_id=eq.$roomId")
                            })
                        })
                    })
                })
                put("ref", refCounter.getAndIncrement().toString())
            }
            ws.send(joinMsg.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Error subscribing to room messages for $roomId", e)
        }
    }

    private fun cleanup() {
        heartbeatJob?.cancel()
        heartbeatJob = null
        webSocket = null
    }

    private fun scheduleReconnect() {
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(5000)
            if (!isConnected) {
                connect()
            }
        }
    }

    fun stop() {
        isConnected = false
        cleanup()
        reconnectJob?.cancel()
        reconnectJob = null
    }
}

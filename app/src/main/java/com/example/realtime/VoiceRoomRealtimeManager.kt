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
        val requestId: String?
    ) : RealtimeTableEvent()
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

    fun start() {
        if (isConnected || webSocket != null) return
        connect()
    }

    private fun connect() {
        try {
            val host = BuildConfig.SUPABASE_URL.replace("https://", "").replace("http://", "").trimEnd('/')
            val wsUrl = "wss://$host/realtime/v1/websocket?apikey=${BuildConfig.SUPABASE_ANON_KEY}&vsn=1.0.0"

            val request = Request.Builder()
                .url(wsUrl)
                .build()

            webSocket = client.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(ws: WebSocket, response: Response) {
                    Log.d(TAG, "Connected to Supabase Realtime WebSocket")
                    isConnected = true
                    subscribeToTopics(ws)
                    startHeartbeat(ws)
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
        // Subscribe to voice_rooms changes
        val joinVoiceRooms = JSONObject().apply {
            put("topic", "realtime:public:voice_rooms")
            put("event", "phx_join")
            put("payload", JSONObject().apply {
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

        // Resubscribe to any active room broadcast channels
        subscribedRoomChannels.forEach { roomId ->
            sendJoinRoomChannel(ws, roomId)
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
            if (event == "broadcast" || event == "emoji" || event == "chat_message" || event == "participant_update") {
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

                if (broadcastType == "participant_update" || broadcastType == "participant_changed") {
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
                    _events.tryEmit(
                        RealtimeTableEvent.SpeakerRequestChanged(
                            eventType = eventType,
                            record = record,
                            roomId = roomId,
                            userId = userId,
                            status = status,
                            requestId = reqId
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling message", e)
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

    fun subscribeRoomChannel(roomId: String) {
        if (roomId.isBlank()) return
        subscribedRoomChannels.add(roomId)
        val ws = webSocket ?: return
        sendJoinRoomChannel(ws, roomId)
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

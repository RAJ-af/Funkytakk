package com.example.realtime

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.BuildConfig
import com.example.SupabaseRoomParticipant
import com.example.SupabaseVoiceRoom
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.*
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

sealed class RealtimeTableEvent {
    data class VoiceRoomChanged(val eventType: String, val record: JSONObject?) : RealtimeTableEvent()
    data class ParticipantChanged(val eventType: String, val record: JSONObject?, val roomId: String?) : RealtimeTableEvent()
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
            if (event == "broadcast" || event == "emoji" || event == "chat_message") {
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

                if (roomId.isNotBlank() && emoji.isNotBlank() && broadcastType != "chat_message") {
                    Log.d(TAG, "Received broadcast emoji '$emoji' for room '$roomId' from '$userId'")
                    _events.tryEmit(RealtimeTableEvent.RoomEmojiBroadcast(roomId, emoji, userId))
                } else if (roomId.isNotBlank() && chatText.isNotBlank()) {
                    Log.d(TAG, "Received broadcast chat message for room '$roomId' from '$userId': $chatText")
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
                    _events.tryEmit(RealtimeTableEvent.RoomMessageInserted(record, roomId))
                } else if (table == "voice_rooms" || topic.contains("voice_rooms")) {
                    _events.tryEmit(RealtimeTableEvent.VoiceRoomChanged(eventType, record))
                } else if (table == "room_participants" || topic.contains("room_participants")) {
                    val roomId = record?.optString("room_id")
                    _events.tryEmit(RealtimeTableEvent.ParticipantChanged(eventType, record, roomId))
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

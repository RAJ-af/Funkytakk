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

            if (event == "postgres_changes" && payload != null) {
                val data = payload.optJSONObject("data")
                val record = data?.optJSONObject("record") ?: data?.optJSONObject("old_record")
                val eventType = data?.optString("type") ?: ""

                if (topic.contains("voice_rooms")) {
                    _events.tryEmit(RealtimeTableEvent.VoiceRoomChanged(eventType, record))
                } else if (topic.contains("room_participants")) {
                    val roomId = record?.optString("room_id")
                    _events.tryEmit(RealtimeTableEvent.ParticipantChanged(eventType, record, roomId))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling message", e)
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

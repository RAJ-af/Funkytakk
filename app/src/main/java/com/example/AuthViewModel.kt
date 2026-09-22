package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.realtime.VoiceRoomRealtimeManager
import com.example.realtime.RealtimeTableEvent

data class SpeakerRequestItem(
    val id: String,
    val roomId: String,
    val userId: String,
    val requesterName: String,
    val requesterAvatar: String
)

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val user: UserProfile) : AuthState()
    data class Error(val message: String) : AuthState()
    data class AccountCollision(val email: String, val message: String) : AuthState()
}

data class UserProfile(
    val uid: String,
    val email: String,
    val displayName: String?,
    val isEmailVerified: Boolean,
    val selectedLanguages: List<String> = emptyList(),
    val username: String? = null,
    val avatar: String? = null,
    val dob: String? = null,
    val age: Int? = null,
    val gender: String? = null,
    val nativeLanguage: String? = null,
    val learningLanguage: String? = null,
    val country: String? = null,
    val countryCode: String? = null,
    val hobbies: List<String> = emptyList(),
    val onboardingComplete: Boolean = false
)

class AuthViewModel : ViewModel() {

    // Onboarding persistent state across configuration changes and navigation flow backstacks
    var tempOnboardingUsername = ""
    var tempOnboardingDisplayName = ""
    var tempOnboardingAvatar = ""
    var tempOnboardingDob = ""
    var tempOnboardingGender = ""
    var tempOnboardingCountry = ""
    var tempOnboardingCountryCode = ""
    var tempOnboardingNativeLanguage = ""
    var tempOnboardingLearningLanguage = ""
    var tempOnboardingHobbies = emptyList<String>()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    fun resetAuthState() {
        _authState.value = AuthState.Idle
    }

    private val _supabaseProfiles = MutableStateFlow<List<UserProfile>>(emptyList())
    val supabaseProfiles: StateFlow<List<UserProfile>> = _supabaseProfiles

    private val _activeRoomId = MutableStateFlow<String?>(null)
    val activeRoomId: StateFlow<String?> = _activeRoomId

    private val _activeRoomTitle = MutableStateFlow<String?>(null)
    val activeRoomTitle: StateFlow<String?> = _activeRoomTitle

    private val _activeRoomHostId = MutableStateFlow<String?>(null)
    val activeRoomHostId: StateFlow<String?> = _activeRoomHostId

    private val _isMicEnabled = MutableStateFlow<Boolean>(true)
    val isMicEnabled: StateFlow<Boolean> = _isMicEnabled

    private val _activeRoomSpeakers = MutableStateFlow<List<UserProfile>>(emptyList())
    val activeRoomSpeakers: StateFlow<List<UserProfile>> = _activeRoomSpeakers

    private val _pendingSpeakerRequest = MutableStateFlow<SpeakerRequestItem?>(null)
    val pendingSpeakerRequest: StateFlow<SpeakerRequestItem?> = _pendingSpeakerRequest

    init {
        viewModelScope.launch {
            VoiceRoomRealtimeManager.events.collect { event ->
                when (event) {
                    is RealtimeTableEvent.SpeakerRequestChanged -> {
                        val currentRoomId = _activeRoomId.value
                        if (currentRoomId != null && event.roomId == currentRoomId) {
                            val hostId = _activeRoomHostId.value
                            val myUid = (authState.value as? AuthState.Success)?.user?.uid
                            val isHost = hostId != null && myUid == hostId

                            if (isHost && event.status == "pending") {
                                val user = _supabaseProfiles.value.find { it.uid == event.userId }
                                val name = user?.displayName ?: user?.username ?: "Listener"
                                val avatar = user?.avatar ?: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400"
                                _pendingSpeakerRequest.value = SpeakerRequestItem(
                                    id = event.requestId ?: "req_${System.currentTimeMillis()}",
                                    roomId = currentRoomId,
                                    userId = event.userId ?: "",
                                    requesterName = name,
                                    requesterAvatar = avatar
                                )
                            } else if (event.status in listOf("accepted", "declined") || event.eventType == "DELETE") {
                                if (_pendingSpeakerRequest.value?.id == event.requestId || _pendingSpeakerRequest.value?.userId == event.userId) {
                                    _pendingSpeakerRequest.value = null
                                }
                            }
                        }
                    }
                    is RealtimeTableEvent.ParticipantChanged -> {
                        val currentRoomId = _activeRoomId.value
                        if (currentRoomId != null && (event.roomId == currentRoomId || event.roomId == null)) {
                            refreshActiveRoomSpeakers(currentRoomId)
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    fun updateActiveRoomTitle(title: String) {
        _activeRoomTitle.value = title
    }

    fun setActiveRoomHostId(hostId: String?) {
        _activeRoomHostId.value = hostId
    }

    fun toggleMic(context: Context? = null) {
        val newState = !_isMicEnabled.value
        setMicEnabled(newState, context)
    }

    fun setMicEnabled(enabled: Boolean, context: Context? = null) {
        _isMicEnabled.value = enabled
        com.example.service.VoiceRoomOverlayManager.setMicEnabled(enabled)
        val roomId = _activeRoomId.value ?: return
        val ctx = context
        val token = ctx?.let { SupabaseSessionManager.getBearerToken(it) } ?: "Bearer ${BuildConfig.SUPABASE_ANON_KEY}"
        val uid = ctx?.let { SupabaseSessionManager.getUserId(it) }
            ?: (authState.value as? AuthState.Success)?.user?.uid
            ?: return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val res = SupabaseClient.api.updateRoomParticipantMic(
                    auth = token,
                    roomIdEq = "eq.$roomId",
                    userIdEq = "eq.$uid",
                    body = UpdateParticipantMicRequest(is_mic_enabled = enabled)
                )
                Log.d("AuthViewModel", "Participant mic updated: $enabled (code ${res.code()})")
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error updating participant mic", e)
            }
        }
    }

    fun respondToSpeakerRequest(requestId: String, accept: Boolean, context: Context? = null) {
        val ctx = context
        val token = ctx?.let { SupabaseSessionManager.getBearerToken(it) } ?: "Bearer ${BuildConfig.SUPABASE_ANON_KEY}"
        _pendingSpeakerRequest.value = null

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val res = SupabaseClient.api.respondToSpeakerRequest(
                    auth = token,
                    body = RespondSpeakerRequestBody(request_id = requestId, accept = accept)
                )
                Log.d("AuthViewModel", "respondToSpeakerRequest: $requestId, accept=$accept, code=${res.code()}")
                _activeRoomId.value?.let { refreshActiveRoomSpeakers(it, context) }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error calling respondToSpeakerRequest", e)
            }
        }
    }

    fun refreshActiveRoomSpeakers(roomId: String, context: Context? = null) {
        val ctx = context
        val token = ctx?.let { SupabaseSessionManager.getBearerToken(it) } ?: "Bearer ${BuildConfig.SUPABASE_ANON_KEY}"
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val res = SupabaseClient.api.getRoomParticipants(auth = token, roomIdEq = "eq.$roomId")
                if (res.isSuccessful) {
                    val list = res.body() ?: emptyList()
                    val speakerUserIds = list.filter { it.role == "speaker" }.map { it.user_id }
                    val currentProfiles = _supabaseProfiles.value
                    val speakers = speakerUserIds.mapNotNull { uid ->
                        currentProfiles.find { it.uid == uid } ?: UserProfile(
                            uid = uid,
                            email = "",
                            displayName = "Speaker",
                            isEmailVerified = false,
                            avatar = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400"
                        )
                    }
                    _activeRoomSpeakers.value = speakers
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error refreshing active room speakers", e)
            }
        }
    }

    fun checkPendingSpeakerRequestsForHost(roomId: String, context: Context? = null) {
        val ctx = context
        val token = ctx?.let { SupabaseSessionManager.getBearerToken(it) } ?: "Bearer ${BuildConfig.SUPABASE_ANON_KEY}"
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val res = SupabaseClient.api.getPendingSpeakerRequests(auth = token, roomIdEq = "eq.$roomId")
                if (res.isSuccessful) {
                    val list = res.body() ?: emptyList()
                    val firstPending = list.firstOrNull()
                    if (firstPending != null) {
                        val user = _supabaseProfiles.value.find { it.uid == firstPending.user_id }
                        val name = user?.displayName ?: user?.username ?: "Listener"
                        val avatar = user?.avatar ?: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400"
                        _pendingSpeakerRequest.value = SpeakerRequestItem(
                            id = firstPending.id,
                            roomId = roomId,
                            userId = firstPending.user_id,
                            requesterName = name,
                            requesterAvatar = avatar
                        )
                    } else {
                        _pendingSpeakerRequest.value = null
                    }
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error checking pending speaker requests", e)
            }
        }
    }

    fun joinRoom(
        roomId: String,
        title: String,
        context: Context? = null,
        role: String = "listener",
        hostId: String? = null
    ) {
        _activeRoomId.value = roomId
        _activeRoomTitle.value = title
        _isMicEnabled.value = (role == "speaker")
        if (hostId != null) {
            _activeRoomHostId.value = hostId
        }
        
        val ctx = context
        val token = ctx?.let { SupabaseSessionManager.getBearerToken(it) } ?: "Bearer ${BuildConfig.SUPABASE_ANON_KEY}"
        val uid = ctx?.let { SupabaseSessionManager.getUserId(it) }
            ?: (authState.value as? AuthState.Success)?.user?.uid
            ?: return

        refreshActiveRoomSpeakers(roomId, context)
        if (hostId == uid || _activeRoomHostId.value == uid) {
            checkPendingSpeakerRequestsForHost(roomId, context)
        }

        val userAvatar = (authState.value as? AuthState.Success)?.user?.avatar
        com.example.service.VoiceRoomOverlayManager.updateSession(
            roomId = roomId,
            roomTitle = title,
            avatarUrl = userAvatar,
            isMicEnabled = (role == "speaker")
        )
        com.example.service.VoiceRoomOverlayManager.onToggleMicAction = { toggleMic(context) }
        com.example.service.VoiceRoomOverlayManager.onLeaveRoomAction = { leaveRoom(context) }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                SupabaseClient.api.joinRoomParticipant(
                    auth = token,
                    participant = SupabaseRoomParticipant(
                        room_id = roomId,
                        user_id = uid,
                        role = role,
                        is_mic_enabled = (role == "speaker")
                    )
                )
                Log.d("AuthViewModel", "User $uid joined room $roomId as $role via Supabase room_participants")
                
                // Send a system message that the user has joined, so everyone in the room sees it
                val userObj = (authState.value as? AuthState.Success)?.user
                val username = userObj?.displayName?.ifBlank { null } ?: userObj?.username?.ifBlank { null } ?: "Someone"

                // 1. Broadcast client-side join message via WebSocket realtime for live listeners
                try {
                    VoiceRoomRealtimeManager.broadcastRoomMessage(
                        roomId = roomId,
                        message = "$username joined the room",
                        userId = uid,
                        senderName = "System",
                        senderAvatar = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=200",
                        messageId = "sys_${System.currentTimeMillis()}"
                    )
                } catch (bEx: Exception) {
                    Log.e("AuthViewModel", "Error broadcasting join message", bEx)
                }

                // 2. Persist to Postgres room_messages table via REST API
                try {
                    SupabaseClient.api.sendRoomMessage(
                        auth = token,
                        request = SendRoomMessageRequest(
                            room_id = roomId,
                            user_id = uid,
                            message = "$username joined the room",
                            message_type = "system"
                        )
                    )
                } catch (msgEx: Exception) {
                    Log.e("AuthViewModel", "Error sending system join message", msgEx)
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error joining room $roomId in Supabase", e)
            }
        }
    }

    fun leaveRoom(context: Context? = null) {
        val roomId = _activeRoomId.value
        val ctx = context
        val token = ctx?.let { SupabaseSessionManager.getBearerToken(it) } ?: "Bearer ${BuildConfig.SUPABASE_ANON_KEY}"
        val uid = ctx?.let { SupabaseSessionManager.getUserId(it) }
            ?: (authState.value as? AuthState.Success)?.user?.uid

        _activeRoomId.value = null
        _activeRoomTitle.value = null
        _activeRoomHostId.value = null
        _pendingSpeakerRequest.value = null
        _activeRoomSpeakers.value = emptyList()

        if (ctx != null) {
            com.example.service.VoiceRoomOverlayManager.stopService(ctx)
        }
        com.example.service.VoiceRoomOverlayManager.clear()

        if (roomId != null && uid != null) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    SupabaseClient.api.leaveRoomParticipant(
                        auth = token,
                        roomIdEq = "eq.$roomId",
                        userIdEq = "eq.$uid"
                    )
                    Log.d("AuthViewModel", "User $uid left room $roomId in Supabase room_participants")
                } catch (e: Exception) {
                    Log.e("AuthViewModel", "Error leaving room $roomId in Supabase", e)
                }
            }
        }
    }

    fun processDeepLinkUri(uri: Uri?, context: Context, onNavigateToRoom: (() -> Unit)? = null) {
        if (uri == null) return
        if (uri.scheme?.lowercase() != "funkytalk") return

        val host = uri.host?.lowercase() ?: ""
        val path = uri.path ?: ""
        val pathSegments = uri.pathSegments

        Log.d("DeepLink", "Processing deep link: $uri, host=$host, path=$path, segments=$pathSegments")

        // Handle funkytalk://open
        if (host == "open") {
            Log.d("DeepLink", "Opened app via funkytalk://open")
            return
        }

        // Handle funkytalk://room/{code} or funkytalk://room?code={code} or funkytalk:///room/{code}
        var roomCode: String? = null
        if (host == "room") {
            roomCode = pathSegments.firstOrNull() ?: uri.getQueryParameter("code") ?: uri.getQueryParameter("id")
        } else if (pathSegments.contains("room")) {
            val idx = pathSegments.indexOf("room")
            if (idx >= 0 && idx + 1 < pathSegments.size) {
                roomCode = pathSegments[idx + 1]
            }
        } else if (host.isNotBlank() && host != "open") {
            // Fallback for funkytalk://{code}
            roomCode = host
        }

        val cleanCode = roomCode?.trim()?.removePrefix("/")
        if (!cleanCode.isNullOrBlank()) {
            joinRoomByCodeOrDirect(
                codeOrId = cleanCode,
                context = context,
                onSuccess = onNavigateToRoom
            )
        }
    }

    fun joinRoomByCodeOrDirect(
        codeOrId: String,
        context: Context,
        onSuccess: (() -> Unit)? = null
    ) {
        val clean = codeOrId.trim()
        if (clean.isBlank()) return

        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Opening room $clean...", Toast.LENGTH_SHORT).show()
            }

            val token = SupabaseSessionManager.getBearerToken(context)
            var targetRoomId: String? = null
            var targetRoomTitle: String? = null

            // 1. Try public room lookup by ID first
            try {
                val response = SupabaseClient.api.getVoiceRoomById(
                    auth = token,
                    idEq = "eq.$clean"
                )
                if (response.isSuccessful) {
                    val room = response.body()?.firstOrNull()
                    if (room != null && room.is_private != true) {
                        targetRoomId = room.id
                        targetRoomTitle = room.title ?: "Voice Room"
                    }
                }
            } catch (e: Exception) {
                Log.w("DeepLink", "Direct room ID lookup failed: ${e.message}")
            }

            // 2. If not resolved, check all voice rooms for matching join_code or public room ID
            if (targetRoomId == null) {
                try {
                    val allRoomsResp = SupabaseClient.api.getVoiceRooms(auth = token)
                    if (allRoomsResp.isSuccessful) {
                        val matchingPublicRoom = allRoomsResp.body()?.find {
                            (it.id == clean || it.join_code.equals(clean, ignoreCase = true)) && it.is_private != true
                        }
                        if (matchingPublicRoom != null) {
                            targetRoomId = matchingPublicRoom.id
                            targetRoomTitle = matchingPublicRoom.title ?: "Voice Room"
                        }
                    }
                } catch (e: Exception) {
                    Log.w("DeepLink", "All rooms query check failed: ${e.message}")
                }
            }

            // 3. If not found or if private, call the existing RPC join_room_by_code
            if (targetRoomId == null) {
                try {
                    val rpcResponse = SupabaseClient.api.joinRoomByCode(
                        auth = token,
                        body = mapOf("entered_code" to clean)
                    )
                    val rawBody = rpcResponse.body()?.string()?.trim() ?: ""
                    Log.d("DeepLink", "joinRoomByCode RPC response status: ${rpcResponse.code()}, body: $rawBody")

                    if (rpcResponse.isSuccessful && rawBody.isNotBlank() && rawBody != "null") {
                        if (rawBody.startsWith("{")) {
                            val json = org.json.JSONObject(rawBody)
                            targetRoomId = json.optString("room_id").ifBlank { json.optString("id") }
                            targetRoomTitle = json.optString("title").ifBlank { "Voice Room" }
                        } else if (rawBody.startsWith("[")) {
                            val array = org.json.JSONArray(rawBody)
                            if (array.length() > 0) {
                                val json = array.getJSONObject(0)
                                targetRoomId = json.optString("room_id").ifBlank { json.optString("id") }
                                targetRoomTitle = json.optString("title").ifBlank { "Voice Room" }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("DeepLink", "Error calling joinRoomByCode for deep link $clean", e)
                }
            }

            // 4. Default fallback for preset room IDs
            if (targetRoomId == null) {
                val knownRooms = listOf("FT5272", "FT1902", "FT8821", "FT4490", "FT7312", "FT9021")
                if (knownRooms.any { it.equals(clean, ignoreCase = true) }) {
                    targetRoomId = clean.uppercase()
                    targetRoomTitle = when (clean.uppercase()) {
                        "FT5272" -> "English Speaking Club 🎙️"
                        "FT1902" -> "Korean Daily Talk 🇰🇷"
                        "FT8821" -> "Spanish Practice Room 🇪🇸"
                        "FT4490" -> "Japanese Manga Chat 🇯🇵"
                        "FT7312" -> "German Grammar & Idioms 🇩🇪"
                        "FT9021" -> "French Cafe Casual 🇫🇷"
                        else -> "Voice Room $clean"
                    }
                }
            }

            withContext(Dispatchers.Main) {
                if (!targetRoomId.isNullOrBlank()) {
                    val finalTitle = targetRoomTitle ?: "Voice Room"
                    Toast.makeText(context, "Entering $finalTitle...", Toast.LENGTH_SHORT).show()
                    joinRoom(targetRoomId, finalTitle, context)
                    onSuccess?.invoke()
                } else {
                    Toast.makeText(context, "Could not join room '$clean'. Room not found or invalid code.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private var firebaseAuth: FirebaseAuth? = null

    fun initializeFirebaseIfNeeded(context: Context) {
        if (firebaseAuth != null) return

        try {
            val apps = FirebaseApp.getApps(context)
            val app = if (apps.isEmpty()) {
                val apiKey = GoogleServicesConfig.getApiKey(context)
                val appId = GoogleServicesConfig.getApplicationId(context)
                val projectId = GoogleServicesConfig.getProjectId(context)

                val options = com.google.firebase.FirebaseOptions.Builder()
                    .setApiKey(apiKey)
                    .setApplicationId(appId)
                    .setProjectId(projectId)
                    .build()
                FirebaseApp.initializeApp(context.applicationContext, options)
            } else {
                apps[0]
            }

            if (app != null) {
                firebaseAuth = FirebaseAuth.getInstance()
                checkCurrentUser(context)
                Log.i("AuthViewModel", "Firebase dynamically initialized with google-services.json credentials successfully!")
            }
        } catch (e: Exception) {
            Log.e("AuthViewModel", "Dynamic Firebase initialization failed: ${e.message}")
        }
    }

    private fun checkCurrentUser(context: Context? = null) {
        val authUser = firebaseAuth?.currentUser
        if (authUser != null) {
            if (context != null) {
                val isGoogleAccount = authUser.providerData.any { it.providerId == "google.com" }
                val effectiveUid = SupabaseSessionManager.getUserId(context) ?: authUser.uid
                checkAndCleanupUnverifiedAccount(context, authUser) {
                    val prefs = context.getSharedPreferences("funky_talk_prefs", Context.MODE_PRIVATE)
                    val isCustomVerified = if (isGoogleAccount) true else prefs.getBoolean("custom_email_verified_${effectiveUid}", false)
                    if (isGoogleAccount) {
                        prefs.edit().putBoolean("custom_email_verified_${effectiveUid}", true).apply()
                    }
                    loadUserProfileFromSpOrFirestore(context, effectiveUid) { finalProfile ->
                        val initialVerified = isCustomVerified
                        _authState.value = AuthState.Success(
                            finalProfile.copy(
                                isEmailVerified = initialVerified
                            )
                        )
                        
                        // Start real-time session tracking on boot for secure single active device enforcement
                        startSessionListener(context, effectiveUid)
                        
                        // Concurrent secure validation & auto-detection on boot (only for password accounts)
                        if (!isGoogleAccount) {
                            viewModelScope.launch {
                                val realVerify = performRemoteVerificationStatusCheck(context, effectiveUid, false)
                                if (realVerify != initialVerified) {
                                    prefs.edit().putBoolean("custom_email_verified_${effectiveUid}", realVerify).apply()
                                    _authState.value = AuthState.Success(
                                        finalProfile.copy(
                                            isEmailVerified = realVerify
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // If context is null, emit what we have.
                _authState.value = AuthState.Success(
                    UserProfile(
                        uid = authUser.uid,
                        email = authUser.email ?: "",
                        displayName = authUser.displayName,
                        isEmailVerified = false
                    )
                )
            }
        }
    }

    private suspend fun performRemoteVerificationStatusCheck(context: Context, userId: String, isFirebaseVerified: Boolean): Boolean {
        // 1. Check Supabase first
        try {
            val bearerAuth = SupabaseSessionManager.getBearerToken(context)
            val response = withContext(Dispatchers.IO) {
                SupabaseClient.api.getUser(
                    auth = bearerAuth,
                    uidEq = "eq.$userId"
                )
            }
            if (response.isSuccessful) {
                val users = response.body()
                if (!users.isNullOrEmpty() && users.first().custom_email_verified == true) {
                    val prefs = context.getSharedPreferences("funky_talk_prefs", Context.MODE_PRIVATE)
                    prefs.edit().putBoolean("custom_email_verified_${userId}", true).apply()
                    return true
                }
            }
        } catch (e: Exception) {
            Log.e("AuthViewModel", "Remote boot check Supabase failed: ${e.message}")
        }

        // Supabase is the sole source of truth
        return false
    }

    private fun checkAndCleanupUnverifiedAccount(context: Context, user: FirebaseUser, onCleaned: () -> Unit) {
        // Skip cleanup for Google Auth accounts - Google accounts are inherently verified by provider
        val isGoogleUser = user.providerData.any { it.providerId == "google.com" }
        if (isGoogleUser) {
            onCleaned()
            return
        }

        val creationTime = user.metadata?.creationTimestamp ?: 0L
        val effectiveUid = SupabaseSessionManager.getUserId(context) ?: user.uid
        val prefs = context.getSharedPreferences("funky_talk_prefs", Context.MODE_PRIVATE)
        val isCustomVerified = prefs.getBoolean("custom_email_verified_${effectiveUid}", false)

        if (!isCustomVerified && creationTime > 0L && (System.currentTimeMillis() - creationTime) > 2L * 3600L * 1000L) {
            _authState.value = AuthState.Loading
            viewModelScope.launch {
                // Double-check remote verification state in Supabase
                val remoteVerified = performRemoteVerificationStatusCheck(context, effectiveUid, false)
                if (remoteVerified) {
                    prefs.edit().putBoolean("custom_email_verified_${effectiveUid}", true).apply()
                    loadUserProfileFromSpOrFirestore(context, effectiveUid) { profile ->
                        _authState.value = AuthState.Success(profile.copy(isEmailVerified = true))
                        onCleaned()
                    }
                } else {
                    // True unverified account older than 2 hours: Delete!
                    prefs.edit().apply {
                        remove("auth_token_${user.uid}")
                        remove("custom_email_verified_${user.uid}")
                        apply()
                    }
                    
                    withContext(Dispatchers.Main) {
                        user.delete().addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Toast.makeText(context, "Unverified account deleted automatically (2-hour limit). Please sign up again.", Toast.LENGTH_LONG).show()
                            } else {
                                Log.e("AuthViewModel", "Failed to delete Firebase user: ${task.exception?.message}")
                            }
                            _authState.value = AuthState.Error("Registration session expired (2 hours max). Please sign up again.")
                            onCleaned()
                        }
                    }
                }
            }
        } else {
            onCleaned()
        }
    }

    fun signUpWithEmail(
        email: String, 
        password: String, 
        context: Context, 
        onLoginNeeded: () -> Unit = {}, 
        onVerificationSent: () -> Unit
    ) {
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Please enter your email and password.")
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            _authState.value = AuthState.Error("Please enter a valid email address.")
            return
        }
        if (password.length < 6) {
            _authState.value = AuthState.Error("Password must be at least 6 characters.")
            return
        }
        _authState.value = AuthState.Loading
        initializeFirebaseIfNeeded(context)

        val auth = firebaseAuth
        if (auth != null) {
            // STEP 1: Firebase.createUserWithEmailAndPassword(email, password)
            auth.createUserWithEmailAndPassword(cleanEmail, password)
                .addOnSuccessListener { result ->
                    val firebaseUser = result.user
                    if (firebaseUser != null) {
                        firebaseUser.getIdToken(true)
                            .addOnSuccessListener { tokenResult ->
                                val firebaseIdToken = tokenResult.token
                                if (!firebaseIdToken.isNullOrEmpty()) {
                                    viewModelScope.launch {
                                        // STEP 2: POST to verify-firebase-token edge function
                                        try {
                                            val verifyReq = VerifyFirebaseTokenRequest(
                                                firebase_token = firebaseIdToken,
                                                firebaseIdToken = firebaseIdToken
                                            )
                                            val verifyRes = withContext(Dispatchers.IO) {
                                                SupabaseClient.api.verifyFirebaseToken(request = verifyReq)
                                            }

                                            if (verifyRes.isSuccessful && verifyRes.body() != null) {
                                                val body = verifyRes.body()!!
                                                val accessToken = body.access_token
                                                val refreshToken = body.refresh_token
                                                val userId = body.user_id ?: body.user?.id ?: firebaseUser.uid

                                                if (!accessToken.isNullOrEmpty()) {
                                                    // STEP 3: supabase.auth.setSession(access_token, refresh_token)
                                                    SupabaseSessionManager.setSession(
                                                        context = context,
                                                        accessToken = accessToken,
                                                        refreshToken = refreshToken,
                                                        userId = userId
                                                    )

                                                    // Save user locally
                                                    saveUserToFirestore(firebaseUser)
                                                    val prefs = context.getSharedPreferences("funky_talk_prefs", Context.MODE_PRIVATE)
                                                    prefs.edit()
                                                        .putBoolean("custom_email_verified_${userId}", false)
                                                        .putLong("last_verification_send_time_${userId}", System.currentTimeMillis())
                                                        .putInt("verification_resend_count_${userId}", 0)
                                                        .apply()

                                                    // Call send-verification-email with Authorization: Bearer <access_token>
                                                    // The edge function generates token server-side and derives uid from JWT
                                                    val bearerAuth = "Bearer $accessToken"
                                                    val sendReq = SendVerificationRequest(
                                                        displayName = firebaseUser.displayName
                                                    )

                                                    val sendRes = withContext(Dispatchers.IO) {
                                                        SupabaseClient.api.sendVerificationEmail(
                                                            auth = bearerAuth,
                                                            request = sendReq
                                                        )
                                                    }

                                                    if (sendRes.isSuccessful) {
                                                        Log.d("AuthViewModel", "Verification email dispatched successfully via Brevo/Supabase")
                                                    } else {
                                                        Log.w("AuthViewModel", "send-verification-email call status: ${sendRes.code()}")
                                                    }

                                                    _authState.value = AuthState.Success(
                                                        UserProfile(
                                                            uid = userId,
                                                            email = cleanEmail,
                                                            displayName = null,
                                                            isEmailVerified = false
                                                        )
                                                    )
                                                    onVerificationSent()
                                                } else {
                                                    _authState.value = AuthState.Error(body.error ?: "Unable to mint Supabase session tokens.")
                                                }
                                            } else {
                                                val err = verifyRes.errorBody()?.string() ?: verifyRes.message()
                                                Log.e("AuthViewModel", "verify-firebase-token failed: $err")
                                                _authState.value = AuthState.Error("Token verification failed: $err")
                                            }
                                        } catch (e: Exception) {
                                            Log.e("AuthViewModel", "verify-firebase-token exception: ${e.message}", e)
                                            _authState.value = AuthState.Error(e.localizedMessage ?: "Network error connecting to auth service.")
                                        }
                                    }
                                } else {
                                    _authState.value = AuthState.Error("Failed to obtain Firebase ID token.")
                                }
                            }
                            .addOnFailureListener { e ->
                                _authState.value = AuthState.Error("Failed to fetch Firebase token: ${e.localizedMessage}")
                            }
                    } else {
                        _authState.value = AuthState.Error("Account creation error. Please try again.")
                    }
                }
                .addOnFailureListener { e ->
                    val errorCode = (e as? com.google.firebase.auth.FirebaseAuthException)?.errorCode
                    if (errorCode == "ERROR_EMAIL_ALREADY_IN_USE" || errorCode == "email-already-in-use" || e.message?.contains("already in use", ignoreCase = true) == true) {
                        _authState.value = AuthState.AccountCollision(
                            email = cleanEmail,
                            message = "An account with this email address already exists. Only one account is permitted per email address."
                        )
                    } else if (errorCode == "ERROR_INVALID_EMAIL" || errorCode == "invalid-email") {
                        _authState.value = AuthState.Error("Please enter a valid email address.")
                    } else if (errorCode == "ERROR_WEAK_PASSWORD" || errorCode == "weak-password") {
                        _authState.value = AuthState.Error("Password is too weak. Please use a stronger password.")
                    } else {
                        _authState.value = AuthState.Error(e.localizedMessage ?: "Sign up failed. Please try again.")
                    }
                }
        } else {
            _authState.value = AuthState.Error("Auth service is not ready. Please try again in a moment.")
        }
    }

    fun loginWithEmail(
        email: String, 
        password: String, 
        context: Context, 
        onSignUpNeeded: () -> Unit,
        onSuccess: () -> Unit
    ) {
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Please enter your email and password.")
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            _authState.value = AuthState.Error("Please enter a valid email address.")
            return
        }
        if (password.length < 6) {
            _authState.value = AuthState.Error("Password must be at least 6 characters.")
            return
        }
        _authState.value = AuthState.Loading
        initializeFirebaseIfNeeded(context)

        val auth = firebaseAuth
        if (auth != null) {
            // STEP 1: Firebase.signInWithEmailAndPassword(email, password)
            auth.signInWithEmailAndPassword(cleanEmail, password)
                .addOnSuccessListener { result ->
                    val firebaseUser = result.user
                    if (firebaseUser != null) {
                        firebaseUser.getIdToken(true)
                            .addOnSuccessListener { tokenResult ->
                                val firebaseIdToken = tokenResult.token
                                if (!firebaseIdToken.isNullOrEmpty()) {
                                    viewModelScope.launch {
                                        // STEP 2: POST to verify-firebase-token edge function
                                        try {
                                            val verifyReq = VerifyFirebaseTokenRequest(
                                                firebase_token = firebaseIdToken,
                                                firebaseIdToken = firebaseIdToken
                                            )
                                            val verifyRes = withContext(Dispatchers.IO) {
                                                SupabaseClient.api.verifyFirebaseToken(request = verifyReq)
                                            }

                                            if (verifyRes.isSuccessful && verifyRes.body() != null) {
                                                val body = verifyRes.body()!!
                                                val accessToken = body.access_token
                                                val refreshToken = body.refresh_token
                                                val userId = body.user_id ?: body.user?.id ?: firebaseUser.uid

                                                if (!accessToken.isNullOrEmpty()) {
                                                    // STEP 3: supabase.auth.setSession(...)
                                                    SupabaseSessionManager.setSession(
                                                        context = context,
                                                        accessToken = accessToken,
                                                        refreshToken = refreshToken,
                                                        userId = userId
                                                    )

                                                    // STEP 4: Query public.users for custom_email_verified (Ignore Firebase isEmailVerified)
                                                    var isCustomVerified = false
                                                    val bearerAuth = "Bearer $accessToken"

                                                    try {
                                                        val userRes = withContext(Dispatchers.IO) {
                                                            SupabaseClient.api.getUser(
                                                                auth = bearerAuth,
                                                                uidEq = "eq.$userId"
                                                            )
                                                        }
                                                        if (userRes.isSuccessful) {
                                                            val supaUsers = userRes.body()
                                                            if (!supaUsers.isNullOrEmpty() && supaUsers.first().custom_email_verified == true) {
                                                                isCustomVerified = true
                                                            }
                                                        }
                                                    } catch (e: Exception) {
                                                        Log.e("AuthViewModel", "Error fetching user from Supabase: ${e.message}")
                                                    }

                                                    val prefs = context.getSharedPreferences("funky_talk_prefs", Context.MODE_PRIVATE)
                                                    prefs.edit().putBoolean("custom_email_verified_${userId}", isCustomVerified).apply()

                                                    loadUserProfileFromSpOrFirestore(context, userId) { finalProfile ->
                                                        val finalProfileWithVerified = finalProfile.copy(isEmailVerified = isCustomVerified)
                                                        _authState.value = AuthState.Success(finalProfileWithVerified)

                                                        if (isCustomVerified) {
                                                            registerAndStartSession(context, userId)
                                                            // Send login notification email via Supabase Edge Function
                                                            triggerSupabaseLoginNotification(
                                                                context = context,
                                                                email = firebaseUser.email ?: cleanEmail,
                                                                displayName = finalProfile.displayName,
                                                                accessToken = accessToken
                                                            )
                                                            onSuccess()
                                                        } else {
                                                            // Redirect to "Please verify your email" screen
                                                            Toast.makeText(context, "Please verify your email before continuing.", Toast.LENGTH_LONG).show()
                                                            onSignUpNeeded()
                                                        }
                                                    }
                                                } else {
                                                    _authState.value = AuthState.Error(body.error ?: "Failed to authenticate session.")
                                                }
                                            } else {
                                                val err = verifyRes.errorBody()?.string() ?: verifyRes.message()
                                                Log.e("AuthViewModel", "verify-firebase-token failed: $err")
                                                _authState.value = AuthState.Error("Authentication failed: $err")
                                            }
                                        } catch (e: Exception) {
                                            Log.e("AuthViewModel", "verify-firebase-token exception: ${e.message}", e)
                                            _authState.value = AuthState.Error(e.localizedMessage ?: "Network error during login.")
                                        }
                                    }
                                } else {
                                    _authState.value = AuthState.Error("Failed to obtain security credentials.")
                                }
                            }
                            .addOnFailureListener { e ->
                                _authState.value = AuthState.Error("Failed to fetch token: ${e.localizedMessage}")
                            }
                    } else {
                        _authState.value = AuthState.Error("Unable to find user account. Please try again.")
                    }
                }
                .addOnFailureListener { e ->
                    val errorCode = (e as? com.google.firebase.auth.FirebaseAuthException)?.errorCode
                    val isUserNotFound = errorCode == "ERROR_USER_NOT_FOUND" ||
                                         errorCode == "user-not-found" ||
                                         e.message?.contains("no user record", ignoreCase = true) == true
                    
                    val isWrongPassword = errorCode == "ERROR_WRONG_PASSWORD" ||
                                          errorCode == "wrong-password" ||
                                          e.message?.contains("password is invalid", ignoreCase = true) == true
                    
                    if (isUserNotFound) {
                        _authState.value = AuthState.Error("No account found with this email. Tap Sign Up below.")
                    } else if (isWrongPassword) {
                        _authState.value = AuthState.Error("Wrong password. Double check or tap 'Forgot password?'.")
                    } else if (errorCode == "invalid-credential" || errorCode == "INVALID_LOGIN_CREDENTIALS") {
                        _authState.value = AuthState.Error("Incorrect password or credentials. Please try again.")
                    } else if (errorCode == "ERROR_INVALID_EMAIL" || errorCode == "invalid-email") {
                        _authState.value = AuthState.Error("Please enter a valid email address.")
                    } else {
                        _authState.value = AuthState.Error(e.localizedMessage ?: "Login failed. Please try again.")
                    }
                }
        } else {
            _authState.value = AuthState.Error("Auth service is not ready. Please try again in a moment.")
        }
    }

    private var verificationJob: kotlinx.coroutines.Job? = null

    fun startAutoVerificationCheck(context: Context, onVerified: () -> Unit) {
        initializeFirebaseIfNeeded(context)
        verificationJob?.cancel()
        verificationJob = viewModelScope.launch {
            val auth = firebaseAuth
            if (auth != null) {
                while (true) {
                    delay(3000)
                    val supabaseUserId = SupabaseSessionManager.getUserId(context) ?: auth.currentUser?.uid
                    if (supabaseUserId != null) {
                        var isVerifiedRemote = false
                        val bearerToken = SupabaseSessionManager.getBearerToken(context)
                        
                        // 1. Check Supabase custom_email_verified
                        try {
                            val response = withContext(Dispatchers.IO) {
                                SupabaseClient.api.getUser(
                                    auth = bearerToken,
                                    uidEq = "eq.$supabaseUserId"
                                )
                            }
                            if (response.isSuccessful) {
                                val users = response.body()
                                if (!users.isNullOrEmpty() && users.first().custom_email_verified == true) {
                                    isVerifiedRemote = true
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("AuthViewModel", "Error polling verification in Supabase: ${e.message}")
                        }

                        if (isVerifiedRemote) {
                            val prefs = context.getSharedPreferences("funky_talk_prefs", Context.MODE_PRIVATE)
                            prefs.edit().putBoolean("custom_email_verified_${supabaseUserId}", true).apply()
                            
                            loadUserProfileFromSpOrFirestore(context, supabaseUserId) { finalProfile ->
                                _authState.value = AuthState.Success(finalProfile.copy(isEmailVerified = true))
                                Toast.makeText(context, "Email verified successfully!", Toast.LENGTH_SHORT).show()
                                onVerified()
                            }
                            break
                        }
                    }
                }
            } else {
                Toast.makeText(context, "Error: Auth service not initialized.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun stopAutoVerificationCheck() {
        verificationJob?.cancel()
        verificationJob = null
    }

    fun checkAndRefreshVerification(context: Context, onVerified: () -> Unit) {
        val auth = firebaseAuth
        val supabaseUserId = SupabaseSessionManager.getUserId(context) ?: auth?.currentUser?.uid
        if (auth != null && supabaseUserId != null) {
            _authState.value = AuthState.Loading
            viewModelScope.launch {
                var isVerifiedRemote = false
                val bearerToken = SupabaseSessionManager.getBearerToken(context)
                
                // Check Supabase custom_email_verified
                try {
                    val response = withContext(Dispatchers.IO) {
                        SupabaseClient.api.getUser(
                            auth = bearerToken,
                            uidEq = "eq.$supabaseUserId"
                        )
                    }
                    if (response.isSuccessful) {
                        val users = response.body()
                        if (!users.isNullOrEmpty() && users.first().custom_email_verified == true) {
                            isVerifiedRemote = true
                        }
                    }
                } catch (e: Exception) {
                    Log.e("AuthViewModel", "Manual check error in Supabase: ${e.message}")
                }

                val prefs = context.getSharedPreferences("funky_talk_prefs", Context.MODE_PRIVATE)
                if (isVerifiedRemote) {
                    prefs.edit().putBoolean("custom_email_verified_${supabaseUserId}", true).apply()
                }

                loadUserProfileFromSpOrFirestore(context, supabaseUserId) { finalProfile ->
                    _authState.value = AuthState.Success(finalProfile.copy(isEmailVerified = isVerifiedRemote))
                    if (isVerifiedRemote) {
                        Toast.makeText(context, "Email is verified!", Toast.LENGTH_SHORT).show()
                        onVerified()
                    } else {
                        Toast.makeText(context, "Email is still not verified. Please click the link in your inbox.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        } else {
            Toast.makeText(context, "Error: User not found.", Toast.LENGTH_SHORT).show()
        }
    }

    fun triggerSupabaseLoginNotification(
        context: Context,
        email: String,
        displayName: String?,
        accessToken: String? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val bearerToken = if (!accessToken.isNullOrBlank()) {
                    if (accessToken.startsWith("Bearer ")) accessToken else "Bearer $accessToken"
                } else {
                    SupabaseSessionManager.getBearerToken(context)
                }

                val currentIsoTimestamp = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
                    timeZone = java.util.TimeZone.getTimeZone("UTC")
                }.format(java.util.Date())

                val req = SendLoginNotificationRequest(
                    email = email,
                    displayName = displayName?.ifBlank { null } ?: "FunkyTalk Learner",
                    deviceType = "Android",
                    timestamp = currentIsoTimestamp
                )

                Log.d("AuthViewModel", "Sending login notification to $email with timestamp $currentIsoTimestamp")
                val response = SupabaseClient.api.sendLoginNotification(
                    auth = bearerToken,
                    request = req
                )
                if (response.isSuccessful) {
                    Log.d("AuthViewModel", "Login notification sent successfully to $email")
                } else {
                    Log.w("AuthViewModel", "send-login-notification response code: ${response.code()} ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.w("AuthViewModel", "send-login-notification fire-and-forget failed: ${e.message}")
            }
        }
    }

    fun sendPasswordResetEmail(
        email: String,
        context: Context,
        onSuccess: () -> Unit = {}
    ) {
        if (email.isBlank()) {
            Toast.makeText(context, "Please enter your email to request a reset link.", Toast.LENGTH_SHORT).show()
            return
        }
        _authState.value = AuthState.Loading
        firebaseAuth?.sendPasswordResetEmail(email)
            ?.addOnSuccessListener {
                _authState.value = AuthState.Error("A secure password reset link has been sent to your email inbox: $email")
                Toast.makeText(context, "Password reset email sent successfully! Please check your inbox.", Toast.LENGTH_LONG).show()
                onSuccess()
            }
            ?.addOnFailureListener { e ->
                _authState.value = AuthState.Error(e.localizedMessage ?: "Failed to send reset link.")
                Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
    }

    fun registerAndStartSession(context: Context, uid: String) {
        val newSessionId = java.util.UUID.randomUUID().toString()
        val prefs = context.getSharedPreferences("funky_talk_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("active_session_id_${uid}", newSessionId).apply()
        Log.d("AuthViewModel", "Registered active session: $newSessionId")
    }

    fun startSessionListener(context: Context, uid: String) {
        val prefs = context.getSharedPreferences("funky_talk_prefs", Context.MODE_PRIVATE)
        val localSessionId = prefs.getString("active_session_id_${uid}", null)
        if (localSessionId == null) {
            registerAndStartSession(context, uid)
        }
    }

    private fun logoutDueToSessionMismatch(context: Context, uid: String) {
        viewModelScope.launch(Dispatchers.Main) {
            withContext(Dispatchers.IO) {
                firebaseAuth?.signOut()
                val prefs = context.getSharedPreferences("funky_talk_prefs", Context.MODE_PRIVATE)
                prefs.edit().remove("active_session_id_${uid}").apply()
            }
            
            _authState.value = AuthState.Idle
            Toast.makeText(context, "Security Alert: Signed out because your account was logged in from another device/browser.", Toast.LENGTH_LONG).show()
        }
    }

    private fun hashTokenSha256(token: String): String {
        val bytes = java.security.MessageDigest.getInstance("SHA-256").digest(token.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun triggerSupabaseVerificationEmail(
        context: Context,
        displayName: String?,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                val bearerAuth = SupabaseSessionManager.getBearerToken(context)

                val response = withContext(Dispatchers.IO) {
                    val req = SendVerificationRequest(
                        displayName = displayName ?: "FunkyTalk Learner"
                    )
                    SupabaseClient.api.sendVerificationEmail(
                        auth = bearerAuth,
                        request = req
                    )
                }

                if (response.isSuccessful) {
                    Toast.makeText(context, "Verification email sent securely!", Toast.LENGTH_LONG).show()
                    onComplete()
                } else {
                    val errMsg = response.errorBody()?.string() ?: response.message() ?: "Unknown error"
                    Log.e("AuthViewModel", "Supabase verification send failed: $errMsg")
                    Toast.makeText(context, "Please check your inbox or click resend.", Toast.LENGTH_SHORT).show()
                    onComplete()
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Supabase Edge call exception: ${e.message}")
                Toast.makeText(context, "Network issue sending verification email.", Toast.LENGTH_LONG).show()
                onComplete()
            }
        }
    }

    fun resendVerificationEmail(context: Context) {
        initializeFirebaseIfNeeded(context)
        val auth = firebaseAuth
        val user = auth?.currentUser
        val supabaseUserId = SupabaseSessionManager.getUserId(context) ?: user?.uid
        if (auth != null && user != null && supabaseUserId != null) {
            val prefs = context.getSharedPreferences("funky_talk_prefs", Context.MODE_PRIVATE)
            val lastSend = prefs.getLong("last_verification_send_time_${supabaseUserId}", 0L)
            val resendCount = prefs.getInt("verification_resend_count_${supabaseUserId}", 0)
            
            val currentTime = System.currentTimeMillis()
            val timeSinceLastResend = currentTime - lastSend
            
            // 1st resend (count = 0) needs 30s, 2nd (count = 1) needs 60s, 3rd (count = 2) needs 120s, and so on...
            val requiredDelayMs = 30000L * (1L shl resendCount.coerceAtMost(5))
            
            if (timeSinceLastResend < requiredDelayMs) {
                val remainingSeconds = (requiredDelayMs - timeSinceLastResend + 999) / 1000
                Toast.makeText(context, "Please wait $remainingSeconds seconds before requesting another email.", Toast.LENGTH_SHORT).show()
                return
            }
            
            saveUserToFirestore(user)
            
            prefs.edit()
                .putLong("last_verification_send_time_${supabaseUserId}", currentTime)
                .putInt("verification_resend_count_${supabaseUserId}", resendCount + 1)
                .apply()

            triggerSupabaseVerificationEmail(context, user.displayName) {
                Toast.makeText(context, "Verification email resent successfully!", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Error: User not found.", Toast.LENGTH_SHORT).show()
        }
    }

    fun getVerificationUrl(context: Context): String? {
        val prefs = context.getSharedPreferences("funky_talk_prefs", Context.MODE_PRIVATE)
        val userId = SupabaseSessionManager.getUserId(context) ?: firebaseAuth?.currentUser?.uid ?: return null
        val token = prefs.getString("auth_token_${userId}", null)
        var baseUrl = BuildConfig.SUPABASE_URL.trim().trimEnd('/')
        if (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
            baseUrl = "https://$baseUrl"
        }
        return if (!token.isNullOrEmpty()) {
            "$baseUrl/functions/v1/verify-email?token=$token&uid=$userId"
        } else {
            "$baseUrl/functions/v1/verify-email?uid=$userId"
        }
    }

    fun openVerificationLinkInBrowser(context: Context) {
        try {
            val url = getVerificationUrl(context) ?: "https://mail.google.com"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("AuthViewModel", "Error opening verification link in browser: ${e.message}", e)
            Toast.makeText(context, "Could not open browser: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun openEmailApp(context: Context) {
        try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_APP_EMAIL)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(intent, "Open Email App").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            try {
                val mailtoIntent = Intent(Intent.ACTION_VIEW, Uri.parse("mailto:")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(mailtoIntent)
            } catch (e2: Exception) {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://mail.google.com")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(browserIntent)
            }
        }
    }

    fun loginWithGoogle(
        context: Context, 
        idToken: String, 
        onSuccess: () -> Unit
    ) {
        _authState.value = AuthState.Loading
        initializeFirebaseIfNeeded(context)
        val auth = firebaseAuth
        if (auth != null) {
            val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
            auth.signInWithCredential(credential)
                .addOnSuccessListener { result ->
                    val user = result.user
                    if (user != null) {
                        user.getIdToken(true).addOnSuccessListener { tokenResult ->
                            val firebaseIdToken = tokenResult.token
                            if (!firebaseIdToken.isNullOrEmpty()) {
                                viewModelScope.launch {
                                    var supaAccessToken: String? = null
                                    var sUserId: String = user.uid
                                    try {
                                        val verifyReq = VerifyFirebaseTokenRequest(
                                            firebase_token = firebaseIdToken,
                                            firebaseIdToken = firebaseIdToken
                                        )
                                        val verifyRes = withContext(Dispatchers.IO) {
                                            SupabaseClient.api.verifyFirebaseToken(request = verifyReq)
                                        }
                                        if (verifyRes.isSuccessful && verifyRes.body() != null) {
                                            val body = verifyRes.body()!!
                                            supaAccessToken = body.access_token
                                            sUserId = body.user_id ?: body.user?.id ?: user.uid
                                            if (!supaAccessToken.isNullOrEmpty()) {
                                                SupabaseSessionManager.setSession(
                                                    context = context,
                                                    accessToken = supaAccessToken!!,
                                                    refreshToken = body.refresh_token,
                                                    userId = sUserId
                                                )
                                            }
                                        }
                                    } catch (e: Exception) {
                                        Log.e("AuthViewModel", "Google Supabase session exchange error: ${e.message}")
                                    }

                                    // For Google Sign-In: Google accounts are pre-verified via Firebase's own email_verified claim.
                                    // DO NOT generate verification token or call send-verification-email.
                                    val prefs = context.getSharedPreferences("funky_talk_prefs", Context.MODE_PRIVATE)
                                    prefs.edit().putBoolean("custom_email_verified_${sUserId}", true).apply()

                                    // Direct check custom_email_verified on Supabase (optional confirmation)
                                    if (!supaAccessToken.isNullOrEmpty()) {
                                        try {
                                            val bearerAuth = "Bearer $supaAccessToken"
                                            val userRes = withContext(Dispatchers.IO) {
                                                SupabaseClient.api.getUser(auth = bearerAuth, uidEq = "eq.$sUserId")
                                            }
                                            if (userRes.isSuccessful) {
                                                val supaUsers = userRes.body()
                                                if (!supaUsers.isNullOrEmpty() && supaUsers.first().custom_email_verified == true) {
                                                    prefs.edit().putBoolean("custom_email_verified_${sUserId}", true).apply()
                                                }
                                            }
                                        } catch (e: Exception) {
                                            Log.d("AuthViewModel", "Google Supabase custom_email_verified check note: ${e.message}")
                                        }
                                    }

                                    loadUserProfileFromSpOrFirestore(context, sUserId) { finalProfile ->
                                        saveUserToFirestore(user, isVerified = true)
                                        _authState.value = AuthState.Success(finalProfile.copy(isEmailVerified = true))
                                        registerAndStartSession(context, sUserId)
                                        triggerSupabaseLoginNotification(
                                            context = context,
                                            email = user.email ?: "",
                                            displayName = finalProfile.displayName,
                                            accessToken = supaAccessToken
                                        )
                                        Toast.makeText(context, "Logged in with Google successfully!", Toast.LENGTH_SHORT).show()
                                        onSuccess()
                                    }
                                }
                            } else {
                                val sUserId = user.uid
                                val prefs = context.getSharedPreferences("funky_talk_prefs", Context.MODE_PRIVATE)
                                prefs.edit().putBoolean("custom_email_verified_${sUserId}", true).apply()
                                loadUserProfileFromSpOrFirestore(context, sUserId) { finalProfile ->
                                    saveUserToFirestore(user, isVerified = true)
                                    _authState.value = AuthState.Success(finalProfile.copy(isEmailVerified = true))
                                    registerAndStartSession(context, sUserId)
                                    triggerSupabaseLoginNotification(context, user.email ?: "", finalProfile.displayName)
                                    Toast.makeText(context, "Logged in with Google successfully!", Toast.LENGTH_SHORT).show()
                                    onSuccess()
                                }
                            }
                        }.addOnFailureListener {
                            val sUserId = user.uid
                            val prefs = context.getSharedPreferences("funky_talk_prefs", Context.MODE_PRIVATE)
                            prefs.edit().putBoolean("custom_email_verified_${sUserId}", true).apply()
                            loadUserProfileFromSpOrFirestore(context, sUserId) { finalProfile ->
                                saveUserToFirestore(user, isVerified = true)
                                _authState.value = AuthState.Success(finalProfile.copy(isEmailVerified = true))
                                registerAndStartSession(context, sUserId)
                                triggerSupabaseLoginNotification(context, user.email ?: "", finalProfile.displayName)
                                Toast.makeText(context, "Logged in with Google successfully!", Toast.LENGTH_SHORT).show()
                                onSuccess()
                            }
                        }
                    } else {
                        _authState.value = AuthState.Error("Failed to obtain signed in Google user details.")
                    }
                }
                .addOnFailureListener { e ->
                    val errorCode = (e as? com.google.firebase.auth.FirebaseAuthException)?.errorCode
                    if (errorCode == "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" ||
                        errorCode == "account-exists-with-different-credential" ||
                        e is com.google.firebase.auth.FirebaseAuthUserCollisionException ||
                        e.message?.contains("already exists", ignoreCase = true) == true
                    ) {
                        val collisionEmail = (e as? com.google.firebase.auth.FirebaseAuthUserCollisionException)?.email ?: ""
                        _authState.value = AuthState.AccountCollision(
                            email = collisionEmail,
                            message = "An account with this email address already exists. Only one account is permitted per email address."
                        )
                    } else {
                        _authState.value = AuthState.Error(e.localizedMessage ?: "Google sign in failed")
                    }
                }
        } else {
            _authState.value = AuthState.Error("Google Sign-In failed. Please try again.")
        }
    }

    fun logout(context: Context? = null) {
        val supabaseUid = if (context != null) SupabaseSessionManager.getUserId(context) else null
        val currentProfileUid = (_authState.value as? AuthState.Success)?.user?.uid
        val effectiveUid = supabaseUid ?: currentProfileUid ?: firebaseAuth?.currentUser?.uid.orEmpty()
        
        firebaseAuth?.signOut()
        _authState.value = AuthState.Idle
        
        if (context != null) {
            SupabaseSessionManager.clearSession(context)
            if (effectiveUid.isNotEmpty()) {
                val prefs = context.getSharedPreferences("funky_talk_prefs", Context.MODE_PRIVATE)
                prefs.edit().remove("active_session_id_${effectiveUid}").apply()
            }
        }
    }

    fun patchProfileStep(
        context: Context,
        updates: Map<String, Any?>,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val supaUid = SupabaseSessionManager.getUserId(context)
                val token = SupabaseSessionManager.getBearerToken(context)
                if (supaUid.isNullOrBlank()) {
                    Log.e("AuthViewModel", "User session not found for patchProfileStep")
                    onSuccess()
                    return@launch
                }

                val response = SupabaseClient.api.patchProfile(
                    auth = token,
                    authUserIdEq = "eq.$supaUid",
                    updates = updates
                )

                val prefs = context.getSharedPreferences("funky_talk_prefs", Context.MODE_PRIVATE)
                val editor = prefs.edit()

                if (updates.containsKey("username")) {
                    editor.putString("profile_username_${supaUid}", updates["username"] as? String ?: "")
                }
                if (updates.containsKey("profile_name")) {
                    editor.putString("profile_displayName_${supaUid}", updates["profile_name"] as? String ?: "")
                }
                if (updates.containsKey("avatar_url")) {
                    editor.putString("profile_avatar_${supaUid}", updates["avatar_url"] as? String ?: "")
                }
                if (updates.containsKey("age")) {
                    val ageVal = (updates["age"] as? Number)?.toInt() ?: 0
                    editor.putInt("profile_age_${supaUid}", ageVal)
                }
                if (updates.containsKey("gender")) {
                    editor.putString("profile_gender_${supaUid}", updates["gender"] as? String ?: "")
                }
                if (updates.containsKey("country")) {
                    editor.putString("profile_country_${supaUid}", updates["country"] as? String ?: "")
                }
                if (updates.containsKey("native_languages")) {
                    val list = updates["native_languages"] as? List<*> ?: emptyList<Any>()
                    editor.putString("profile_nativeLanguage_${supaUid}", list.joinToString(", "))
                }
                if (updates.containsKey("learning_languages")) {
                    val list = updates["learning_languages"] as? List<*> ?: emptyList<Any>()
                    editor.putString("profile_learningLanguage_${supaUid}", list.joinToString(", "))
                }
                if (updates.containsKey("hobbies")) {
                    val list = updates["hobbies"] as? List<*> ?: emptyList<Any>()
                    editor.putString("profile_hobbies_${supaUid}", list.joinToString("||"))
                }
                if (updates.containsKey("onboarding_complete")) {
                    val complete = updates["onboarding_complete"] as? Boolean ?: false
                    editor.putBoolean("profile_onboarding_complete_${supaUid}", complete)
                }
                editor.apply()

                val current = _authState.value
                if (current is AuthState.Success) {
                    val u = current.user
                    val updatedUser = u.copy(
                        username = if (updates.containsKey("username")) updates["username"] as? String else u.username,
                        displayName = if (updates.containsKey("profile_name")) updates["profile_name"] as? String else u.displayName,
                        avatar = if (updates.containsKey("avatar_url")) updates["avatar_url"] as? String else u.avatar,
                        age = if (updates.containsKey("age")) (updates["age"] as? Number)?.toInt() else u.age,
                        gender = if (updates.containsKey("gender")) updates["gender"] as? String else u.gender,
                        country = if (updates.containsKey("country")) updates["country"] as? String else u.country,
                        nativeLanguage = if (updates.containsKey("native_languages")) (updates["native_languages"] as? List<*>)?.joinToString(", ") else u.nativeLanguage,
                        learningLanguage = if (updates.containsKey("learning_languages")) (updates["learning_languages"] as? List<*>)?.joinToString(", ") else u.learningLanguage,
                        hobbies = if (updates.containsKey("hobbies")) (updates["hobbies"] as? List<String>) ?: u.hobbies else u.hobbies,
                        onboardingComplete = if (updates.containsKey("onboarding_complete")) updates["onboarding_complete"] as? Boolean ?: u.onboardingComplete else u.onboardingComplete
                    )
                    _authState.value = AuthState.Success(updatedUser)
                }

                if (response.isSuccessful) {
                    Log.d("AuthViewModel", "Profile patched successfully: $updates")
                    onSuccess()
                } else {
                    Log.e("AuthViewModel", "Failed to patch profile: ${response.code()} ${response.errorBody()?.string()}")
                    onSuccess()
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Exception patching profile", e)
                onSuccess()
            }
        }
    }

    fun patchStep1Profile(
        context: Context,
        username: String,
        displayName: String,
        avatar: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            var finalAvatar = avatar
            try {
                var inputStream: java.io.InputStream? = null
                if (avatar.startsWith("content://")) {
                    val uri = android.net.Uri.parse(avatar)
                    inputStream = context.contentResolver.openInputStream(uri)
                } else if (avatar.startsWith("/") || avatar.startsWith("file://")) {
                    val purePath = avatar.removePrefix("file://")
                    val file = java.io.File(purePath)
                    if (file.exists()) {
                        inputStream = java.io.FileInputStream(file)
                    }
                }
                if (inputStream != null) {
                    val bytes = inputStream.readBytes()
                    inputStream.close()
                    val mediaType = "image/*".toMediaTypeOrNull()
                    val requestFile = bytes.toRequestBody(mediaType, 0, bytes.size)
                    val supaUid = SupabaseSessionManager.getUserId(context) ?: "anon"
                    val filename = "avatar_${supaUid}_${System.currentTimeMillis()}.jpg"
                    val body = okhttp3.MultipartBody.Part.createFormData("file", filename, requestFile)
                    val bearerAuth = SupabaseSessionManager.getBearerToken(context)
                    val response = SupabaseClient.api.uploadAvatar(
                        auth = bearerAuth,
                        filename = filename,
                        file = body
                    )
                    if (response.isSuccessful) {
                        val baseUrl = BuildConfig.SUPABASE_URL.let {
                            if (!it.startsWith("http")) "https://$it" else it
                        }.trimEnd('/')
                        finalAvatar = "$baseUrl/storage/v1/object/public/avatars/$filename"
                    }
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Avatar upload exception", e)
            }

            patchProfileStep(
                context = context,
                updates = mapOf(
                    "username" to username,
                    "profile_name" to displayName,
                    "avatar_url" to finalAvatar
                ),
                onSuccess = onSuccess,
                onError = onError
            )
        }
    }

    fun updateProfile(
        context: Context,
        username: String,
        displayName: String,
        avatar: String,
        dob: String,
        gender: String,
        nativeLanguage: String,
        learningLanguage: String,
        country: String,
        countryCode: String,
        hobbies: List<String>,
        onSuccess: () -> Unit
    ) {
        val currentState = _authState.value
        if (currentState is AuthState.Success) {
            viewModelScope.launch {
                var finalAvatar = avatar
                try {
                    // 1. Upload avatar to Supabase Storage if it's a content URI or local file path
                    var inputStream: java.io.InputStream? = null
                    if (avatar.startsWith("content://")) {
                        val uri = android.net.Uri.parse(avatar)
                        inputStream = context.contentResolver.openInputStream(uri)
                    } else if (avatar.startsWith("/") || avatar.startsWith("file://")) {
                        val purePath = avatar.removePrefix("file://")
                        val file = java.io.File(purePath)
                        if (file.exists()) {
                            inputStream = java.io.FileInputStream(file)
                        }
                    }
                    if (inputStream != null) {
                        val bytes = inputStream.readBytes()
                        inputStream.close()
                        val mediaType = "image/*".toMediaTypeOrNull()
                        val requestFile = bytes.toRequestBody(mediaType, 0, bytes.size)
                        val filename = "avatar_${currentState.user.uid}_${System.currentTimeMillis()}.jpg"
                        val body = okhttp3.MultipartBody.Part.createFormData("file", filename, requestFile)
                        val response = SupabaseClient.api.uploadAvatar(filename = filename, file = body)
                        if (response.isSuccessful) {
                            val baseUrl = BuildConfig.SUPABASE_URL.let {
                                if (!it.startsWith("http")) "https://$it" else it
                            }.trimEnd('/')
                            finalAvatar = "$baseUrl/storage/v1/object/public/avatars/$filename"
                            Log.d("AuthViewModel", "Avatar uploaded to Supabase: $finalAvatar")
                        } else {
                            Log.e("AuthViewModel", "Failed to upload avatar: ${response.code()} ${response.message()}")
                        }
                    }

                    val supaUid = SupabaseSessionManager.getUserId(context) ?: currentState.user.uid
                    val bearerAuth = SupabaseSessionManager.getBearerToken(context)
                    val nativeList = nativeLanguage.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    val learningList = learningLanguage.split(",").map { it.trim() }.filter { it.isNotEmpty() }

                    SupabaseClient.api.patchProfile(
                        auth = bearerAuth,
                        authUserIdEq = "eq.$supaUid",
                        updates = mapOf(
                            "username" to username,
                            "profile_name" to displayName,
                            "avatar_url" to finalAvatar,
                            "gender" to gender,
                            "country" to country,
                            "native_languages" to nativeList,
                            "learning_languages" to learningList,
                            "hobbies" to hobbies,
                            "onboarding_complete" to true
                        )
                    )
                } catch (e: Exception) {
                    Log.e("AuthViewModel", "Supabase update error: ${e.message}", e)
                }

                val userId = currentState.user.uid
                val prefs = context.getSharedPreferences("funky_talk_prefs", Context.MODE_PRIVATE)
                prefs.edit().apply {
                    putString("profile_username_${userId}", username)
                    putString("profile_displayName_${userId}", displayName)
                    putString("profile_avatar_${userId}", finalAvatar)
                    putString("profile_dob_${userId}", dob)
                    putString("profile_gender_${userId}", gender)
                    putString("profile_nativeLanguage_${userId}", nativeLanguage)
                    putString("profile_learningLanguage_${userId}", learningLanguage)
                    putString("profile_country_${userId}", country)
                    putString("profile_countryCode_${userId}", countryCode)
                    putString("profile_hobbies_${userId}", hobbies.joinToString("||"))
                    putBoolean("profile_onboarding_complete_${userId}", true)
                    apply()
                }

                val updatedUser = currentState.user.copy(
                    username = username,
                    displayName = displayName,
                    avatar = finalAvatar,
                    dob = dob,
                    gender = gender,
                    nativeLanguage = nativeLanguage,
                    learningLanguage = learningLanguage,
                    country = country,
                    countryCode = countryCode,
                    hobbies = hobbies,
                    onboardingComplete = true
                )
                _authState.value = AuthState.Success(updatedUser)
                onSuccess()
            }
        }
    }

    fun loadUserProfileFromSpOrFirestore(context: Context, userId: String, onComplete: (UserProfile) -> Unit = {}) {
        val prefs = context.getSharedPreferences("funky_talk_prefs", Context.MODE_PRIVATE)
        val isCustomVerified = prefs.getBoolean("custom_email_verified_${userId}", false)
        val resolvedVerified = isCustomVerified

        val username = prefs.getString("profile_username_${userId}", null)
        val displayName = prefs.getString("profile_displayName_${userId}", null)
        val avatar = prefs.getString("profile_avatar_${userId}", null)
        val ageVal = prefs.getInt("profile_age_${userId}", 0).takeIf { it > 0 }
        val dob = prefs.getString("profile_dob_${userId}", null)
        val gender = prefs.getString("profile_gender_${userId}", null)
        val nativeLanguage = prefs.getString("profile_nativeLanguage_${userId}", null)
        val learningLanguage = prefs.getString("profile_learningLanguage_${userId}", null)
        val country = prefs.getString("profile_country_${userId}", null)
        val countryCode = prefs.getString("profile_countryCode_${userId}", null)
        val hobbiesStr = prefs.getString("profile_hobbies_${userId}", null)
        val hobbies = hobbiesStr?.split("||")?.filter { it.isNotEmpty() } ?: emptyList()
        val isOnboardingComplete = prefs.getBoolean("profile_onboarding_complete_${userId}", false)

        val currentState = _authState.value
        val currentProfile = if (currentState is AuthState.Success) currentState.user else UserProfile(
            uid = userId,
            email = firebaseAuth?.currentUser?.email ?: "",
            displayName = displayName,
            isEmailVerified = resolvedVerified,
            username = username,
            avatar = avatar,
            age = ageVal,
            dob = dob,
            gender = gender,
            nativeLanguage = nativeLanguage,
            learningLanguage = learningLanguage,
            country = country,
            countryCode = countryCode,
            hobbies = hobbies,
            onboardingComplete = isOnboardingComplete
        )

        val mergedProfile = currentProfile.copy(
            isEmailVerified = resolvedVerified,
            username = username ?: currentProfile.username,
            displayName = displayName ?: currentProfile.displayName,
            avatar = avatar ?: currentProfile.avatar,
            age = ageVal ?: currentProfile.age,
            dob = dob ?: currentProfile.dob,
            gender = gender ?: currentProfile.gender,
            nativeLanguage = nativeLanguage ?: currentProfile.nativeLanguage,
            learningLanguage = learningLanguage ?: currentProfile.learningLanguage,
            country = country ?: currentProfile.country,
            countryCode = countryCode ?: currentProfile.countryCode,
            hobbies = if (hobbies.isNotEmpty()) hobbies else currentProfile.hobbies,
            onboardingComplete = isOnboardingComplete || currentProfile.onboardingComplete
        )

        viewModelScope.launch {
            try {
                val bearerToken = SupabaseSessionManager.getBearerToken(context)
                val response = SupabaseClient.api.getProfile(
                    auth = bearerToken,
                    authUserIdEq = "eq.$userId"
                )
                if (response.isSuccessful) {
                    val supaProfiles = response.body()
                    if (!supaProfiles.isNullOrEmpty()) {
                        val supaProfile = supaProfiles.first()
                        val supaComplete = supaProfile.onboarding_complete == true
                        val nativeStr = supaProfile.native_languages?.joinToString(", ")
                        val learningStr = supaProfile.learning_languages?.joinToString(", ")
                        val hobbiesList = supaProfile.hobbies ?: emptyList()

                        prefs.edit().apply {
                            putString("profile_username_${userId}", supaProfile.username ?: "")
                            putString("profile_displayName_${userId}", supaProfile.profile_name ?: "")
                            putString("profile_avatar_${userId}", supaProfile.avatar_url ?: "")
                            putInt("profile_age_${userId}", supaProfile.age ?: 0)
                            putString("profile_gender_${userId}", supaProfile.gender ?: "")
                            putString("profile_nativeLanguage_${userId}", nativeStr ?: "")
                            putString("profile_learningLanguage_${userId}", learningStr ?: "")
                            putString("profile_country_${userId}", supaProfile.country ?: "")
                            putString("profile_hobbies_${userId}", hobbiesList.joinToString("||"))
                            putBoolean("profile_onboarding_complete_${userId}", supaComplete)
                            apply()
                        }

                        val finalProfile = mergedProfile.copy(
                            isEmailVerified = resolvedVerified,
                            username = supaProfile.username ?: mergedProfile.username,
                            displayName = supaProfile.profile_name ?: mergedProfile.displayName,
                            avatar = supaProfile.avatar_url ?: mergedProfile.avatar,
                            age = supaProfile.age ?: mergedProfile.age,
                            gender = supaProfile.gender ?: mergedProfile.gender,
                            nativeLanguage = nativeStr ?: mergedProfile.nativeLanguage,
                            learningLanguage = learningStr ?: mergedProfile.learningLanguage,
                            country = supaProfile.country ?: mergedProfile.country,
                            hobbies = if (hobbiesList.isNotEmpty()) hobbiesList else mergedProfile.hobbies,
                            onboardingComplete = supaComplete
                        )
                        _authState.value = AuthState.Success(finalProfile)
                        onComplete(finalProfile)
                        return@launch
                    }
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Failed to load profile from Supabase: ${e.message}", e)
            }

            _authState.value = AuthState.Success(mergedProfile)
            onComplete(mergedProfile)
        }
    }

    fun checkUsernameUnique(username: String, currentUserId: String?, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val trimmedUsername = username.trim()
                if (trimmedUsername.isEmpty()) {
                    callback(false)
                    return@launch
                }
                val resolvedUserId = currentUserId ?: (_authState.value as? AuthState.Success)?.user?.uid
                
                // 1. Check Supabase users table by username (primary table for registered users)
                val userRes = withContext(Dispatchers.IO) {
                    try {
                        SupabaseClient.api.getUserByUsername(usernameEq = "ilike.$trimmedUsername")
                    } catch (e: Exception) {
                        null
                    }
                }
                
                if (userRes != null && userRes.isSuccessful) {
                    val users = userRes.body() ?: emptyList()
                    val conflict = users.any { it.uid != resolvedUserId }
                    callback(!conflict)
                    return@launch
                }

                // Retry with eq on users table if ilike failed
                val userEqRes = withContext(Dispatchers.IO) {
                    try {
                        SupabaseClient.api.getUserByUsername(usernameEq = "eq.$trimmedUsername")
                    } catch (e: Exception) {
                        null
                    }
                }
                if (userEqRes != null && userEqRes.isSuccessful) {
                    val users = userEqRes.body() ?: emptyList()
                    val conflict = users.any { it.uid != resolvedUserId }
                    callback(!conflict)
                    return@launch
                }

                // 2. Fallback to profiles table
                val profileRes = withContext(Dispatchers.IO) {
                    try {
                        SupabaseClient.api.getProfileByUsername(usernameEq = "ilike.$trimmedUsername")
                    } catch (e: Exception) {
                        null
                    }
                }
                if (profileRes != null && profileRes.isSuccessful) {
                    val profiles = profileRes.body() ?: emptyList()
                    val conflict = profiles.any { it.auth_user_id != resolvedUserId }
                    callback(!conflict)
                    return@launch
                }

                val profileEqRes = withContext(Dispatchers.IO) {
                    try {
                        SupabaseClient.api.getProfileByUsername(usernameEq = "eq.$trimmedUsername")
                    } catch (e: Exception) {
                        null
                    }
                }
                if (profileEqRes != null && profileEqRes.isSuccessful) {
                    val profiles = profileEqRes.body() ?: emptyList()
                    val conflict = profiles.any { it.auth_user_id != resolvedUserId }
                    callback(!conflict)
                    return@launch
                }

                // If remote query could not find any conflict or backend does not have the column yet, allow proceeding
                Log.d("AuthViewModel", "checkUsernameUnique: No existing conflict found or allowed by default")
                callback(true)
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error checking username uniqueness in Supabase", e)
                callback(true)
            }
        }
    }

    fun saveUserToFirestore(user: FirebaseUser, code: String? = null, token: String? = null, isVerified: Boolean = false) {
        // No-op: Supabase profiles and users tables are the sole source of truth
    }

    fun fetchSupabaseProfiles(context: Context) {
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    SupabaseClient.api.getAllProfiles()
                }
                if (response.isSuccessful) {
                    val supaProfiles = response.body() ?: emptyList()
                    val profiles = supaProfiles.map { supaProfile ->
                        UserProfile(
                            uid = supaProfile.auth_user_id ?: "",
                            email = supaProfile.email ?: "",
                            displayName = supaProfile.profile_name ?: "Learner",
                            isEmailVerified = true,
                            username = supaProfile.username ?: "learner_user",
                            avatar = supaProfile.avatar_url ?: "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=150",
                            dob = "2000-01-01",
                            gender = supaProfile.gender ?: "Non-binary",
                            nativeLanguage = supaProfile.native_languages?.joinToString(", ") ?: "Hindi",
                            learningLanguage = supaProfile.learning_languages?.joinToString(", ") ?: "English",
                            country = supaProfile.country ?: "India",
                            countryCode = supaProfile.country_code ?: "in",
                            hobbies = supaProfile.hobbies ?: emptyList()
                        )
                    }
                    _supabaseProfiles.value = profiles
                    Log.d("AuthViewModel", "Fetched ${profiles.size} profiles from Supabase successfully")
                } else {
                    Log.e("AuthViewModel", "Failed to fetch Supabase profiles: ${response.code()} ${response.message()}")
                    _supabaseProfiles.value = emptyList()
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error fetching Supabase profiles: ${e.message}", e)
                _supabaseProfiles.value = emptyList()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
    }
}

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
import kotlinx.coroutines.flow.asStateFlow
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

sealed class SessionVerificationState {
    object Idle : SessionVerificationState()
    object Verifying : SessionVerificationState()
    object Verified : SessionVerificationState()
    data class Failed(val reason: String? = null) : SessionVerificationState()
}

data class UserProfile(
    val uid: String,
    val email: String,
    val displayName: String?,
    val isEmailVerified: Boolean,
    val selectedLanguages: List<String> = emptyList(),
    val username: String? = null,
    val avatar: String? = null,
    val coverUrl: String? = null,
    val dob: String? = null,
    val age: Int? = null,
    val gender: String? = null,
    val nativeLanguage: String? = null,
    val learningLanguage: String? = null,
    val country: String? = null,
    val countryCode: String? = null,
    val hobbies: List<String> = emptyList(),
    val onboardingComplete: Boolean = false,
    val bio: String? = null,
    val city: String? = null
)

sealed class DeletionStatusState {
    object Idle : DeletionStatusState()
    object Checking : DeletionStatusState()
    data class Pending(val info: DeletionStatusResponse) : DeletionStatusState()
    data class Failed(val error: String) : DeletionStatusState()
    object Clear : DeletionStatusState()
}

data class DeletionReceiptData(
    val receiptId: String,
    val scheduledFor: String,
    val graceDays: Int,
    val eraseLocalChats: Boolean
)

data class EditProfileSnapshot(
    val username: String = "",
    val displayName: String = "",
    val avatar: String = "",
    val coverUrl: String = "",
    val bio: String = "",
    val age: Int? = null,
    val gender: String = "",
    val country: String = "",
    val countryCode: String = "",
    val city: String = "",
    val nativeLanguages: List<String> = emptyList(),
    val learningLanguages: List<String> = emptyList(),
    val hobbies: List<String> = emptyList()
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

    private val _sessionVerificationState = MutableStateFlow<SessionVerificationState>(SessionVerificationState.Idle)
    val sessionVerificationState: StateFlow<SessionVerificationState> = _sessionVerificationState.asStateFlow()

    fun resetAuthState() {
        _authState.value = AuthState.Idle
    }

    fun retryBootSessionVerification(context: Context) {
        initializeFirebaseIfNeeded(context)
        checkCurrentUser(context)
    }

    private val _supabaseProfiles = MutableStateFlow<List<UserProfile>>(emptyList())
    val supabaseProfiles: StateFlow<List<UserProfile>> = _supabaseProfiles

    private val _activeRoomId = MutableStateFlow<String?>(null)
    val activeRoomId: StateFlow<String?> = _activeRoomId

    private val _activeRoomTitle = MutableStateFlow<String?>(null)
    val activeRoomTitle: StateFlow<String?> = _activeRoomTitle

    private val _activeRoomHostId = MutableStateFlow<String?>(null)
    val activeRoomHostId: StateFlow<String?> = _activeRoomHostId

    val isMicEnabled: StateFlow<Boolean> = com.example.realtime.RoomMicController.micOn

    private val _activeRoomSpeakers = MutableStateFlow<List<UserProfile>>(emptyList())
    val activeRoomSpeakers: StateFlow<List<UserProfile>> = _activeRoomSpeakers

    private val _pendingSpeakerRequests = MutableStateFlow<List<SpeakerRequestItem>>(emptyList())
    val pendingSpeakerRequests: StateFlow<List<SpeakerRequestItem>> = _pendingSpeakerRequests

    private val _pendingSpeakerRequest = MutableStateFlow<SpeakerRequestItem?>(null)
    val pendingSpeakerRequest: StateFlow<SpeakerRequestItem?> = _pendingSpeakerRequest

    private val _mySpeakerRequestStatus = MutableStateFlow<String?>(null)
    val mySpeakerRequestStatus: StateFlow<String?> = _mySpeakerRequestStatus

    private val _myModerationStatus = MutableStateFlow<MyModerationStatus?>(null)
    val myModerationStatus: StateFlow<MyModerationStatus?> = _myModerationStatus

    private val _lastRoomTokenRole = MutableStateFlow<String?>(null)
    val lastRoomTokenRole: StateFlow<String?> = _lastRoomTokenRole.asStateFlow()

    private val _lastRoomTokenCanPublish = MutableStateFlow<Boolean>(false)
    val lastRoomTokenCanPublish: StateFlow<Boolean> = _lastRoomTokenCanPublish.asStateFlow()

    private val _deletionStatus = MutableStateFlow<DeletionStatusState>(DeletionStatusState.Idle)
    val deletionStatus: StateFlow<DeletionStatusState> = _deletionStatus.asStateFlow()

    private val _deletionReceipt = MutableStateFlow<DeletionReceiptData?>(null)
    val deletionReceipt: StateFlow<DeletionReceiptData?> = _deletionReceipt.asStateFlow()

    private val _accountUnderReview = MutableStateFlow<Boolean>(false)
    val accountUnderReview: StateFlow<Boolean> = _accountUnderReview.asStateFlow()

    private val _profileEditLimits = MutableStateFlow<Map<String, ProfileFieldLimitDto>>(emptyMap())
    val profileEditLimits: StateFlow<Map<String, ProfileFieldLimitDto>> = _profileEditLimits.asStateFlow()

    fun setTimeoutError(message: String) {
        _authState.value = AuthState.Error(message)
    }

    fun formatIsoToLocalDate(iso: String?): String {
        val instant = parseSupabaseTimestamp(iso) ?: return iso?.trim().orEmpty().take(10)
        return try {
            val zoneDate = instant.atZone(java.time.ZoneId.systemDefault()).toLocalDate()
            java.time.format.DateTimeFormatter
                .ofLocalizedDate(java.time.format.FormatStyle.MEDIUM)
                .withLocale(java.util.Locale.getDefault())
                .format(zoneDate)
        } catch (_: Exception) {
            iso?.trim().orEmpty().take(10)
        }
    }

    fun formatIsoToLocalDateTime(iso: String?): String {
        val instant = parseSupabaseTimestamp(iso) ?: return iso?.trim().orEmpty()
        return try {
            val zdt = instant.atZone(java.time.ZoneId.systemDefault())
            java.time.format.DateTimeFormatter
                .ofLocalizedDateTime(java.time.format.FormatStyle.MEDIUM, java.time.format.FormatStyle.SHORT)
                .withLocale(java.util.Locale.getDefault())
                .format(zdt)
        } catch (_: Exception) {
            iso?.trim().orEmpty()
        }
    }

    private fun parseSupabaseTimestamp(raw: String?): java.time.Instant? {
        val trimmed = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        try {
            return java.time.Instant.parse(trimmed)
        } catch (_: Exception) {}
        try {
            return java.time.OffsetDateTime.parse(trimmed).toInstant()
        } catch (_: Exception) {}

        // Handle PostgreSQL timestamptz::text like "2026-12-14 08:43:22.123456+00"
        var normalized = trimmed.replaceFirst(' ', 'T')
        val shortTzRegex = Regex("([+-]\\d{2})$")
        val compactTzRegex = Regex("([+-]\\d{2})(\\d{2})$")
        normalized = when {
            shortTzRegex.containsMatchIn(normalized) -> normalized.replace(shortTzRegex, "$1:00")
            compactTzRegex.containsMatchIn(normalized) -> normalized.replace(compactTzRegex, "$1:$2")
            else -> normalized
        }
        return try {
            java.time.OffsetDateTime.parse(normalized).toInstant()
        } catch (_: Exception) {
            null
        }
    }

    private fun profileLimitFieldLabel(field: String): String = when (field) {
        "username" -> "username"
        "profile_name" -> "display name"
        "age" -> "age"
        "gender" -> "gender"
        "avatar_url" -> "profile photo"
        "cover_url" -> "cover photo"
        "bio" -> "bio"
        "country_code" -> "country"
        "city" -> "city"
        else -> field.replace("_", " ")
    }

    fun parseProfileErrorMessage(rawErrorBody: String): String {
        var msgText = ""
        var hintText = ""
        try {
            val json = org.json.JSONObject(rawErrorBody)
            msgText = json.optString("message", "").trim()
            hintText = json.optString("hint", "").trim()
        } catch (_: Exception) {
            msgText = rawErrorBody.trim()
        }

        if (msgText.startsWith("profile_limit_")) {
            val field = msgText.removePrefix("profile_limit_").trim()
            val label = profileLimitFieldLabel(field)
            val whenText = formatIsoToLocalDateTime(hintText).ifBlank { "later" }
            return "You've reached the limit for $label. You can change it again on $whenText."
        }

        val limitRegex = Regex("profile_limit_([a-z_]+)")
        val limitMatch = limitRegex.find(rawErrorBody)
        if (limitMatch != null) {
            val field = limitMatch.groupValues[1]
            val label = profileLimitFieldLabel(field)
            val whenText = formatIsoToLocalDateTime(hintText).ifBlank { "later" }
            return "You've reached the limit for $label. You can change it again on $whenText."
        }

        val combined = "$msgText $rawErrorBody"
        return when {
            msgText == "invalid_username" || combined.contains("invalid_username") ->
                "Use 3-20 letters, numbers, dots or underscores"
            msgText == "username_reserved" || combined.contains("username_reserved") ->
                "This name is not allowed"
            msgText == "username_recently_used" || combined.contains("username_recently_used") ->
                "This name was used recently by someone else"
            msgText == "username_taken" || combined.contains("username_taken") || combined.contains("23505") ->
                "Already taken"
            msgText == "not_authenticated" || combined.contains("not_authenticated") ->
                "Please log in again"
            else -> "Something went wrong. Try again"
        }
    }

    suspend fun fetchProfileEditLimits(context: Context): Map<String, ProfileFieldLimitDto> {
        return withContext(Dispatchers.IO) {
            try {
                SupabaseSessionManager.ensureActiveSession(context)
                val bearerAuth = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                    ?: SupabaseSessionManager.getBearerToken(context)
                val res = SupabaseClient.api.getProfileEditLimits(auth = bearerAuth)
                if (res.isSuccessful) {
                    val map = res.body().orEmpty()
                        .filter { it.field.isNotBlank() }
                        .associateBy { it.field }
                    _profileEditLimits.value = map
                    return@withContext map
                } else {
                    Log.w("AuthViewModel", "get_profile_edit_limits failed (${res.code()})")
                }
            } catch (e: Exception) {
                Log.w("AuthViewModel", "get_profile_edit_limits error: ${e.message}")
            }
            _profileEditLimits.value
        }
    }

    suspend fun checkUsernameAvailability(
        context: Context,
        username: String
    ): Result<UsernameAvailabilityResponse> {
        return withContext(Dispatchers.IO) {
            try {
                SupabaseSessionManager.ensureActiveSession(context)
                val bearerAuth = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                    ?: SupabaseSessionManager.getBearerToken(context)
                val clean = username.trim().lowercase()
                val res = SupabaseClient.api.checkUsernameAvailable(
                    auth = bearerAuth,
                    body = CheckUsernameRequest(p_username = clean)
                )
                if (res.isSuccessful && res.body() != null) {
                    Result.success(res.body()!!)
                } else {
                    val err = res.errorBody()?.string().orEmpty()
                    Result.failure(Exception(parseProfileErrorMessage(err)))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    fun checkAccountReview(context: Context, onReviewEnded: (() -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val res = ModerationManager.fetchMyAccountReview(context)
                val isUnderReview = res.getOrNull()?.under_review == true
                val wasUnderReview = _accountUnderReview.value
                _accountUnderReview.value = isUnderReview
                if (wasUnderReview && !isUnderReview) {
                    withContext(Dispatchers.Main) {
                        onReviewEnded?.invoke()
                    }
                }
            } catch (e: Exception) {
                Log.w("AuthViewModel", "checkAccountReview error: ${e.message}")
            }
        }
    }

    fun refreshModerationStatus(context: Context) {
        checkAccountReview(context)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val status = ModerationManager.fetchMyModerationStatus(context)
                _myModerationStatus.value = status
            } catch (e: Exception) {
                Log.w("AuthViewModel", "Failed to refresh moderation status: ${e.message}")
            }
        }
    }

    fun setModerationStatus(status: MyModerationStatus?) {
        _myModerationStatus.value = status
    }

    init {
        viewModelScope.launch {
            VoiceRoomRealtimeManager.events.collect { event ->
                when (event) {
                    is RealtimeTableEvent.Reconnected -> {
                        val currentRoomId = _activeRoomId.value
                        if (!currentRoomId.isNullOrBlank()) {
                            val hostId = _activeRoomHostId.value
                            val appContext = FunkyTalkApplication.appContext
                            val myUid = appContext?.let { SupabaseSessionManager.getUserId(it) }
                                ?: (authState.value as? AuthState.Success)?.user?.uid
                            val isHost = (hostId != null && myUid != null && hostId.equals(myUid, ignoreCase = true))

                            Log.d("HostSpeakerRequests", "[Realtime Reconnect] Reconnected to Realtime. Refreshing room $currentRoomId (isHost=$isHost)")
                            if (isHost) {
                                checkPendingSpeakerRequestsForHost(currentRoomId, appContext)
                            } else {
                                checkMySpeakerRequest(currentRoomId, appContext)
                            }
                            refreshActiveRoomSpeakers(currentRoomId, appContext)
                        }
                    }
                    is RealtimeTableEvent.SpeakerRequestChanged -> {
                        val currentRoomId = _activeRoomId.value
                        if (currentRoomId != null && (event.roomId == currentRoomId || event.roomId.isNullOrBlank())) {
                            val hostId = _activeRoomHostId.value
                            val appContext = FunkyTalkApplication.appContext
                            val myUid = appContext?.let { SupabaseSessionManager.getUserId(it) }
                                ?: (authState.value as? AuthState.Success)?.user?.uid

                            val isHost = hostId != null && myUid != null && hostId.equals(myUid, ignoreCase = true)

                            Log.d("HostSpeakerRequests", "[Realtime Event] SpeakerRequestChanged: status=${event.status}, eventType=${event.eventType}, reqId=${event.requestId}, user=${event.userId}, isHost=$isHost, currentRoom=$currentRoomId")

                            // 1. HOST Handling: Immediately query and refresh pending requests list from Supabase
                            if (isHost) {
                                val isPendingState = event.status == "pending" || event.eventType == "requested" || event.eventType == "INSERT"
                                val isResolvedState = event.status in listOf("accepted", "declined") || event.eventType in listOf("DELETE", "accepted", "declined")

                                if (isPendingState && !isResolvedState) {
                                    val user = _supabaseProfiles.value.find { it.uid == event.userId }
                                    val snippet = com.example.realtime.VoiceRoomRealtimeManager.getUserSnippet(event.userId ?: "")
                                    val name = user?.displayName?.ifBlank { null }
                                        ?: user?.username?.ifBlank { null }
                                        ?: snippet?.name?.ifBlank { null }
                                        ?: "Listener"
                                    val avatar = user?.avatar?.ifBlank { null }
                                        ?: snippet?.avatar?.ifBlank { null }
                                        ?: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400"
                                    val newItem = SpeakerRequestItem(
                                        id = event.requestId?.ifBlank { null } ?: "req_${System.currentTimeMillis()}",
                                        roomId = currentRoomId,
                                        userId = event.userId ?: "",
                                        requesterName = name,
                                        requesterAvatar = avatar
                                    )
                                    val updated = (listOf(newItem) + _pendingSpeakerRequests.value.filter { it.id != newItem.id && (it.userId.isBlank() || it.userId != newItem.userId) })
                                    _pendingSpeakerRequests.value = updated
                                    _pendingSpeakerRequest.value = updated.firstOrNull()
                                } else if (isResolvedState) {
                                    val updated = _pendingSpeakerRequests.value.filterNot { it.id == event.requestId || (it.userId.isNotBlank() && it.userId == event.userId) }
                                    _pendingSpeakerRequests.value = updated
                                    _pendingSpeakerRequest.value = updated.firstOrNull()
                                    if (event.status == "accepted" || event.eventType == "accepted") {
                                        refreshActiveRoomSpeakers(currentRoomId, appContext)
                                    }
                                }
                                checkPendingSpeakerRequestsForHost(currentRoomId, appContext)
                            }

                            // 2. LISTENER Handling: Update requester's local state
                            if (myUid != null && event.userId == myUid) {
                                when (event.status ?: event.eventType) {
                                    "pending", "requested" -> {
                                        _mySpeakerRequestStatus.value = "pending"
                                    }
                                    "accepted" -> {
                                        _mySpeakerRequestStatus.value = "accepted"
                                        com.example.realtime.RoomMicController.onRoleChanged("speaker", true)
                                        refreshActiveRoomSpeakers(currentRoomId, appContext)
                                    }
                                    "declined" -> {
                                        _mySpeakerRequestStatus.value = "declined"
                                    }
                                    else -> {
                                        if (event.eventType == "DELETE") {
                                            _mySpeakerRequestStatus.value = null
                                        }
                                    }
                                }
                            }
                        }
                    }
                    is RealtimeTableEvent.ParticipantChanged -> {
                        val currentRoomId = _activeRoomId.value
                        if (currentRoomId != null && (event.roomId == currentRoomId || event.roomId == null)) {
                            val appContext = FunkyTalkApplication.appContext
                            val myUid = appContext?.let { SupabaseSessionManager.getUserId(it) }
                                ?: (authState.value as? AuthState.Success)?.user?.uid
                            val role = event.role ?: event.record?.optString("role")
                            val mic = event.isMicEnabled ?: event.record?.optBoolean("is_mic_enabled", false)
                            if (myUid != null && event.userId == myUid) {
                                if (role == "speaker") {
                                    val wasAccepted = (_mySpeakerRequestStatus.value == "accepted")
                                    _mySpeakerRequestStatus.value = "accepted"
                                    if (!wasAccepted) {
                                        com.example.realtime.RoomMicController.onRoleChanged("speaker", mic ?: true)
                                    }
                                } else if (role == "listener") {
                                    _mySpeakerRequestStatus.value = null
                                    com.example.realtime.RoomMicController.onRoleChanged("listener", false)
                                }
                            }
                            refreshActiveRoomSpeakers(currentRoomId, appContext)
                        }
                    }
                    is RealtimeTableEvent.VoiceRoomChanged -> {
                        val currentRoomId = _activeRoomId.value
                        val record = event.record
                        val changedRoomId = record?.optString("id")
                        val isLive = record?.optBoolean("is_live", true) ?: true
                        val status = record?.optString("status") ?: "active"
                        if (currentRoomId != null && (changedRoomId == currentRoomId || changedRoomId.isNullOrBlank())) {
                            if (!isLive || status == "ended" || event.eventType.equals("DELETE", ignoreCase = true)) {
                                Log.d("AuthViewModel", "Active room $currentRoomId changed to ended/deleted in voice_rooms table")
                                _activeRoomId.value = null
                                _activeRoomTitle.value = null
                                _activeRoomHostId.value = null
                                _pendingSpeakerRequests.value = emptyList()
                                _pendingSpeakerRequest.value = null
                                _mySpeakerRequestStatus.value = null
                                _activeRoomSpeakers.value = emptyList()
                                com.example.service.VoiceRoomOverlayManager.stopService(FunkyTalkApplication.appContext)
                                com.example.service.VoiceRoomOverlayManager.clear()
                            }
                        }
                    }
                    is RealtimeTableEvent.ModerationActionEnforced -> {
                        val currentUid = (authState.value as? AuthState.Success)?.user?.uid ?: ""
                        if (event.targetUserId.isNotBlank() && (event.targetUserId.equals(currentUid, ignoreCase = true) || currentUid.isBlank())) {
                            Log.d("AuthViewModel", "Received moderation realtime event for me: ${event.actionType}")
                            // Re-fetch via SupabaseClient to ensure backend source of truth
                            try {
                                val context = FunkyTalkApplication.appContext
                                refreshModerationStatus(context)
                            } catch (e: Exception) {
                                Log.w("AuthViewModel", "Could not get appContext for moderation refresh: ${e.message}")
                            }
                        }
                    }
                    is RealtimeTableEvent.RoomLifecycleEvent -> {
                        val currentRoomId = _activeRoomId.value
                        if (currentRoomId != null && event.roomId == currentRoomId) {
                            Log.d("AuthViewModel", "RoomLifecycleEvent received for active room $currentRoomId: action=${event.action}")
                            if (event.action == "room_ended") {
                                Log.d("AuthViewModel", "Room $currentRoomId was ended by host.")
                                _activeRoomId.value = null
                                _activeRoomTitle.value = null
                                _activeRoomHostId.value = null
                                _pendingSpeakerRequests.value = emptyList()
                                _pendingSpeakerRequest.value = null
                                _mySpeakerRequestStatus.value = null
                                _activeRoomSpeakers.value = emptyList()
                                com.example.service.VoiceRoomOverlayManager.stopService(FunkyTalkApplication.appContext)
                                com.example.service.VoiceRoomOverlayManager.clear()
                            }
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

    @Volatile
    private var isMicToggleInFlight = false

    fun toggleMic(context: Context? = null) {
        com.example.realtime.RoomMicController.toggleMic(context)
    }

    fun setMicEnabled(enabled: Boolean, context: Context? = null, onComplete: ((Boolean) -> Unit)? = null) {
        com.example.realtime.RoomMicController.setMic(enabled, "AuthViewModel_setMicEnabled", context)
        onComplete?.invoke(true)
    }

    fun respondToSpeakerRequest(requestId: String, accept: Boolean, context: Context? = null) {
        val ctx = context ?: FunkyTalkApplication.appContext
        val token = ctx?.let { SupabaseSessionManager.getBearerToken(it) } ?: "Bearer ${BuildConfig.SUPABASE_ANON_KEY}"
        
        // Find target user from current pending list before removing
        val targetItem = _pendingSpeakerRequests.value.find { it.id == requestId }
        val targetUserId = targetItem?.userId ?: ""

        // Optimistically remove from pending list
        val remaining = _pendingSpeakerRequests.value.filter { it.id != requestId }
        _pendingSpeakerRequests.value = remaining
        _pendingSpeakerRequest.value = remaining.firstOrNull()

        val currentRoom = _activeRoomId.value

        // Broadcast decision immediately to room so listener gets notified instantly
        if (currentRoom != null && targetUserId.isNotBlank()) {
            com.example.realtime.VoiceRoomRealtimeManager.broadcastSpeakerRequest(
                roomId = currentRoom,
                requestId = requestId,
                userId = targetUserId,
                action = if (accept) "accepted" else "declined",
                status = if (accept) "accepted" else "declined"
            )
            if (accept) {
                com.example.realtime.VoiceRoomRealtimeManager.broadcastParticipantUpdate(
                    roomId = currentRoom,
                    userId = targetUserId,
                    action = "speaker_promoted",
                    role = "speaker",
                    isMicEnabled = true,
                    userName = targetItem?.requesterName,
                    userAvatar = targetItem?.requesterAvatar
                )
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val res = SupabaseClient.api.respondToSpeakerRequest(
                    auth = token,
                    body = RespondSpeakerRequestBody(request_id = requestId, accept = accept)
                )
                Log.d("AuthViewModel", "respondToSpeakerRequest: $requestId, accept=$accept, code=${res.code()}")
                if (currentRoom != null) {
                    checkPendingSpeakerRequestsForHost(currentRoom, ctx)
                    refreshActiveRoomSpeakers(currentRoom, ctx)
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error calling respondToSpeakerRequest", e)
            }
        }
    }

    fun refreshActiveRoomSpeakers(roomId: String, context: Context? = null) {
        val ctx = context ?: FunkyTalkApplication.appContext
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

                    // If current user is now a speaker, update local state
                    val myUid = ctx?.let { SupabaseSessionManager.getUserId(it) }
                    if (myUid != null && speakerUserIds.contains(myUid)) {
                        _mySpeakerRequestStatus.value = "accepted"
                        com.example.realtime.RoomMicController.onRoleChanged("speaker", true)
                    }
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error refreshing active room speakers", e)
            }
        }
    }

    fun checkPendingSpeakerRequestsForHost(roomId: String, context: Context? = null) {
        val ctx = context ?: FunkyTalkApplication.appContext
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (ctx != null) {
                    SupabaseSessionManager.ensureActiveSession(ctx)
                }
                val myAuthUid = ctx?.let { SupabaseSessionManager.getUserId(it) }
                    ?: (authState.value as? AuthState.Success)?.user?.uid
                val hostId = _activeRoomHostId.value
                val token = ctx?.let { SupabaseSessionManager.getBearerToken(it) } ?: "Bearer ${BuildConfig.SUPABASE_ANON_KEY}"
                val isHostVerified = (myAuthUid != null && hostId != null && myAuthUid.equals(hostId, ignoreCase = true))

                // Mandatory integration logging for Host speaker requests
                Log.d("HostSpeakerRequests", "=== [DEBUG] Host Speaker Requests Check ===")
                Log.d("HostSpeakerRequests", "1. Authenticated Supabase auth.uid(): $myAuthUid")
                Log.d("HostSpeakerRequests", "2. Current room ID: $roomId, voice_rooms.host_id: $hostId")
                Log.d("HostSpeakerRequests", "3. Verification (auth.uid == host_id): $isHostVerified")

                val res = SupabaseClient.api.getPendingSpeakerRequests(auth = token, roomIdEq = "eq.$roomId")
                Log.d("HostSpeakerRequests", "4. Supabase API query status: ${res.code()}, isSuccessful: ${res.isSuccessful}")

                if (res.isSuccessful) {
                    val list = res.body() ?: emptyList()
                    Log.d("HostSpeakerRequests", "5. Loaded ${list.size} pending speaker requests: $list")
                    val currentProfiles = _supabaseProfiles.value
                    val items = list.map { req ->
                        val snippet = com.example.realtime.VoiceRoomRealtimeManager.getUserSnippet(req.user_id)
                        val pProfile = currentProfiles.find { it.uid == req.user_id }
                        val name = pProfile?.displayName?.ifBlank { null }
                            ?: pProfile?.username?.ifBlank { null }
                            ?: snippet?.name?.ifBlank { null }
                            ?: "Listener"
                        val avatar = pProfile?.avatar?.ifBlank { null }
                            ?: snippet?.avatar?.ifBlank { null }
                            ?: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400"
                        SpeakerRequestItem(
                            id = req.id,
                            roomId = req.room_id.ifBlank { roomId },
                            userId = req.user_id,
                            requesterName = name,
                            requesterAvatar = avatar
                        )
                    }
                    withContext(Dispatchers.Main) {
                        _pendingSpeakerRequests.value = items
                        _pendingSpeakerRequest.value = items.firstOrNull()
                    }

                    // Enriches names in background if user profile not yet cached
                    for (item in items) {
                        if (item.requesterName == "Listener") {
                            try {
                                val uRes = SupabaseClient.api.getUser(auth = token, uidEq = "eq.${item.userId}")
                                if (uRes.isSuccessful && !uRes.body().isNullOrEmpty()) {
                                    val fetchedUser = uRes.body()!!.first()
                                    val fName = fetchedUser.display_name?.ifBlank { null } ?: fetchedUser.username?.ifBlank { null } ?: "Listener"
                                    val fAvatar = fetchedUser.avatar?.ifBlank { null } ?: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400"
                                    com.example.realtime.VoiceRoomRealtimeManager.recordUserSnippet(item.userId, fName, fAvatar, fetchedUser.country_code)
                                    withContext(Dispatchers.Main) {
                                        _pendingSpeakerRequests.value = _pendingSpeakerRequests.value.map { cur ->
                                            if (cur.id == item.id) cur.copy(requesterName = fName, requesterAvatar = fAvatar) else cur
                                        }
                                        _pendingSpeakerRequest.value = _pendingSpeakerRequests.value.firstOrNull()
                                    }
                                }
                            } catch (_: Exception) {}
                        }
                    }
                } else {
                    val err = res.errorBody()?.string() ?: res.message()
                    Log.e("HostSpeakerRequests", "Failed to query pending speaker requests: ${res.code()} - $err")
                }
            } catch (e: Exception) {
                Log.e("HostSpeakerRequests", "Error in checkPendingSpeakerRequestsForHost", e)
            }
        }
    }

    fun checkMySpeakerRequest(roomId: String, context: Context? = null) {
        val ctx = context ?: FunkyTalkApplication.appContext
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (ctx != null) {
                    SupabaseSessionManager.ensureActiveSession(ctx)
                }
                val token = ctx?.let { SupabaseSessionManager.getBearerToken(it) } ?: "Bearer ${BuildConfig.SUPABASE_ANON_KEY}"
                val uid = ctx?.let { SupabaseSessionManager.getUserId(it) }
                    ?: (authState.value as? AuthState.Success)?.user?.uid
                    ?: return@launch

                Log.d("HostSpeakerRequests", "checkMySpeakerRequest for user $uid in room $roomId")
                val res = SupabaseClient.api.getPendingSpeakerRequests(auth = token, roomIdEq = "eq.$roomId")
                if (res.isSuccessful) {
                    val list = res.body() ?: emptyList()
                    val hasPending = list.any { it.user_id == uid && it.status == "pending" }
                    withContext(Dispatchers.Main) {
                        if (hasPending) {
                            _mySpeakerRequestStatus.value = "pending"
                        } else if (_mySpeakerRequestStatus.value == "pending") {
                            _mySpeakerRequestStatus.value = null
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("HostSpeakerRequests", "Error checking my speaker request", e)
            }
        }
    }

    fun requestSpeakerSeat(
        roomId: String,
        context: Context,
        onResult: ((Boolean, String?) -> Unit)? = null
    ) {
        val uid = SupabaseSessionManager.getUserId(context)
            ?: (authState.value as? AuthState.Success)?.user?.uid

        if (uid.isNullOrBlank()) {
            onResult?.invoke(false, "Please sign in to request a speaker seat.")
            return
        }

        // Edge case: Already a speaker or host -> do not show/request another seat
        val hostId = _activeRoomHostId.value
        val isHost = (hostId != null && hostId == uid)
        val isAlreadySpeaker = _activeRoomSpeakers.value.any { it.uid == uid }
        if (isHost || isAlreadySpeaker) {
            onResult?.invoke(false, "You are already a speaker in this room.")
            return
        }

        // Edge case: Existing pending request -> do not create another
        if (_mySpeakerRequestStatus.value == "pending") {
            onResult?.invoke(true, "Your request to speak is pending host approval.")
            return
        }

        val myProfile = _supabaseProfiles.value.find { it.uid == uid }
            ?: (authState.value as? AuthState.Success)?.user
        val myName = myProfile?.displayName?.ifBlank { null }
            ?: myProfile?.username?.ifBlank { null }
            ?: "Learner"
        val myAvatar = myProfile?.avatar?.ifBlank { null }
            ?: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400"

        val token = SupabaseSessionManager.getBearerToken(context)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                var reqId = "req_${System.currentTimeMillis()}"
                var isSuccess = false

                // 1. Try atomic request_speaker_seat RPC first (which resets any stale records)
                try {
                    val rpcRes = SupabaseClient.api.requestSpeakerSeat(
                        auth = token,
                        body = mapOf("p_room_id" to roomId)
                    )
                    if (rpcRes.isSuccessful) {
                        isSuccess = true
                        val rawBody = rpcRes.body()?.string()
                        if (!rawBody.isNullOrBlank()) {
                            try {
                                val obj = org.json.JSONObject(rawBody)
                                val idFromObj = obj.optString("request_id")
                                if (!idFromObj.isNullOrBlank()) reqId = idFromObj
                            } catch (_: Exception) {}
                        }
                    }
                } catch (_: Exception) {}

                // 2. Fallback to direct table insert if RPC was not handled
                if (!isSuccess) {
                    val res = SupabaseClient.api.createSpeakerRequest(
                        auth = token,
                        body = SpeakerRequestInsert(
                            room_id = roomId,
                            user_id = uid,
                            status = "pending"
                        )
                    )
                    if (res.isSuccessful) {
                        isSuccess = true
                        try {
                            val rawBody = res.body()?.string()
                            if (!rawBody.isNullOrBlank()) {
                                if (rawBody.trim().startsWith("[")) {
                                    val arr = org.json.JSONArray(rawBody)
                                    if (arr.length() > 0) {
                                        val obj = arr.getJSONObject(0)
                                        val idFromObj = obj.optString("id")
                                        if (!idFromObj.isNullOrBlank()) reqId = idFromObj
                                    }
                                } else if (rawBody.trim().startsWith("{")) {
                                    val obj = org.json.JSONObject(rawBody)
                                    val idFromObj = obj.optString("id")
                                    if (!idFromObj.isNullOrBlank()) reqId = idFromObj
                                }
                            }
                        } catch (_: Exception) {}
                    }
                }

                withContext(Dispatchers.Main) {
                    if (isSuccess) {
                        _mySpeakerRequestStatus.value = "pending"

                        // Broadcast speaker request live to room & host
                        com.example.realtime.VoiceRoomRealtimeManager.broadcastSpeakerRequest(
                            roomId = roomId,
                            requestId = reqId,
                            userId = uid,
                            action = "requested",
                            status = "pending",
                            userName = myName,
                            userAvatar = myAvatar
                        )

                        onResult?.invoke(true, "Request to join as speaker sent to host!")
                    } else {
                        // Query backend state in case constraint treated it as duplicate
                        checkMySpeakerRequest(roomId, context)
                        onResult?.invoke(false, "Could not submit speaker request. Please try again.")
                    }
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error creating speaker request", e)
                withContext(Dispatchers.Main) {
                    onResult?.invoke(false, "Error submitting speaker request. Please try again.")
                }
            }
        }
    }

    private val _roomVoiceSession = MutableStateFlow<com.example.voice.RoomVoiceSession?>(null)
    val roomVoiceSession: StateFlow<com.example.voice.RoomVoiceSession?> = _roomVoiceSession.asStateFlow()

    fun startVoiceSession(context: Context? = null, roomId: String, userId: String, userName: String, initialRole: String) {
        val current = _roomVoiceSession.value
        if (current != null && current.roomId == roomId) {
            current.onRoleChanged(initialRole)
            Log.d("AuthViewModel", "RoomVoiceSession already running for roomId=$roomId")
            return
        }
        current?.leave()
        val safeAppContext = context?.applicationContext
            ?: com.example.FunkyTalkApplication.getSafeAppContext()
        val session = com.example.voice.RoomVoiceSession(
            context = safeAppContext,
            roomId = roomId,
            userId = userId,
            userName = userName,
            initialRole = initialRole,
            scope = viewModelScope
        )
        _roomVoiceSession.value = session
        session.start()
    }

    fun endVoiceSession() {
        _roomVoiceSession.value?.leave()
        _roomVoiceSession.value = null
    }

    fun fetchRoomToken(
        roomId: String,
        context: Context? = null,
        onResult: ((Boolean, GetRoomTokenResponse?, String?) -> Unit)? = null
    ) {
        val ctx = context ?: FunkyTalkApplication.appContext

        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (ctx != null) {
                    SupabaseSessionManager.ensureActiveSession(ctx)
                }
                var token = ctx?.let {
                    SupabaseSessionManager.getAuthenticatedBearerToken(it)
                        ?: SupabaseSessionManager.getBearerToken(it)
                } ?: "Bearer ${BuildConfig.SUPABASE_ANON_KEY}"
                val req = GetRoomTokenRequest(room_id = roomId)
                var res = SupabaseClient.api.getRoomToken(auth = token, body = req)
                if (res.code() == 401 && ctx != null) {
                    SupabaseSessionManager.forceRefreshSession(ctx)
                    token = SupabaseSessionManager.getAuthenticatedBearerToken(ctx)
                        ?: SupabaseSessionManager.getBearerToken(ctx)
                    res = SupabaseClient.api.getRoomToken(auth = token, body = req)
                }
                if (res.isSuccessful && res.body() != null) {
                    val body = res.body()!!
                    val role = body.role ?: (if (body.can_publish) "publisher" else "audience")
                    val canPublish = body.can_publish
                    val rtcToken = body.token ?: body.rtc_token

                    withContext(Dispatchers.Main) {
                        _lastRoomTokenRole.value = role
                        _lastRoomTokenCanPublish.value = canPublish
                        if (!rtcToken.isNullOrBlank()) {
                            com.example.realtime.AgoraVoiceManager.renewToken(rtcToken)
                        }
                        if (canPublish) {
                            com.example.realtime.AgoraVoiceManager.setClientRole(com.example.realtime.AgoraVoiceManager.CLIENT_ROLE_BROADCASTER)
                            com.example.realtime.AgoraVoiceManager.enableLocalAudio(true)
                            com.example.realtime.AgoraVoiceManager.muteLocalAudioStream(!isMicEnabled.value)
                        } else {
                            com.example.realtime.AgoraVoiceManager.setClientRole(com.example.realtime.AgoraVoiceManager.CLIENT_ROLE_AUDIENCE)
                            com.example.realtime.AgoraVoiceManager.muteLocalAudioStream(true)
                        }
                        onResult?.invoke(true, body, null)
                    }
                } else {
                    val errCode = res.code()
                    val errBodyStr = res.errorBody()?.string() ?: ""
                    val errorMsg = when (errCode) {
                        401 -> "Session expired. Please sign in again."
                        403 -> "Account suspended or restricted from speaking."
                        404 -> "Voice room not found."
                        410 -> "This voice room has ended."
                        503 -> "All voice room servers are busy. Please try again later."
                        else -> {
                            if (errBodyStr.contains("seats_full", ignoreCase = true)) {
                                "All speaker seats are full (max 4 speakers)."
                            } else {
                                com.example.util.ErrorMapper.mapHttp(errCode)
                            }
                        }
                    }
                    Log.e("AuthViewModel", "getRoomToken error $errCode: $errBodyStr")
                    withContext(Dispatchers.Main) {
                        onResult?.invoke(false, null, errorMsg)
                    }
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error fetching room token", e)
                withContext(Dispatchers.Main) {
                    onResult?.invoke(false, null, com.example.util.ErrorMapper.map(e))
                }
            }
        }
    }

    fun inviteToSpeakerSeat(
        roomId: String,
        targetUserId: String,
        context: Context? = null,
        onResult: ((Boolean, String?) -> Unit)? = null
    ) {
        val ctx = context ?: FunkyTalkApplication.appContext
        val token = ctx?.let { SupabaseSessionManager.getBearerToken(it) } ?: "Bearer ${BuildConfig.SUPABASE_ANON_KEY}"

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val res = SupabaseClient.api.inviteToSpeakerSeat(
                    auth = token,
                    body = mapOf("p_room_id" to roomId, "p_user_id" to targetUserId)
                )
                if (res.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        onResult?.invoke(true, "Speaker invitation sent!")
                    }
                } else {
                    val errStr = res.errorBody()?.string() ?: ""
                    val msg = if (errStr.contains("seats_full", ignoreCase = true)) {
                        "All speaker seats are full (max 4 speakers)."
                    } else {
                        com.example.util.ErrorMapper.mapHttp(res.code(), errStr)
                    }
                    withContext(Dispatchers.Main) {
                        onResult?.invoke(false, msg)
                    }
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error in inviteToSpeakerSeat", e)
                withContext(Dispatchers.Main) {
                    onResult?.invoke(false, com.example.util.ErrorMapper.map(e))
                }
            }
        }
    }

    fun respondToSpeakerInvite(
        requestId: String,
        accept: Boolean,
        roomId: String,
        context: Context? = null,
        onResult: ((Boolean, String?) -> Unit)? = null
    ) {
        val ctx = context ?: FunkyTalkApplication.appContext
        val token = ctx?.let { SupabaseSessionManager.getBearerToken(it) } ?: "Bearer ${BuildConfig.SUPABASE_ANON_KEY}"

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val res = SupabaseClient.api.respondToSpeakerInvite(
                    auth = token,
                    body = mapOf("p_request_id" to requestId, "p_accept" to accept)
                )
                if (res.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        if (accept) {
                            fetchRoomToken(roomId, ctx)
                            _mySpeakerRequestStatus.value = "accepted"
                            com.example.realtime.RoomMicController.onRoleChanged("speaker", true)
                            onResult?.invoke(true, "You are now a speaker!")
                        } else {
                            _mySpeakerRequestStatus.value = null
                            onResult?.invoke(true, "Invitation declined.")
                        }
                    }
                } else {
                    val errStr = res.errorBody()?.string() ?: ""
                    val msg = if (errStr.contains("seats_full", ignoreCase = true)) {
                        "All speaker seats are full (max 4 speakers)."
                    } else {
                        com.example.util.ErrorMapper.mapHttp(res.code(), errStr)
                    }
                    withContext(Dispatchers.Main) {
                        onResult?.invoke(false, msg)
                    }
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error in respondToSpeakerInvite", e)
                withContext(Dispatchers.Main) {
                    onResult?.invoke(false, com.example.util.ErrorMapper.map(e))
                }
            }
        }
    }

    fun demoteSpeaker(
        roomId: String,
        targetUserId: String? = null,
        context: Context? = null,
        onResult: ((Boolean, String?) -> Unit)? = null
    ) {
        val ctx = context ?: FunkyTalkApplication.appContext
        val token = ctx?.let { SupabaseSessionManager.getBearerToken(it) } ?: "Bearer ${BuildConfig.SUPABASE_ANON_KEY}"
        val myUid = ctx?.let { SupabaseSessionManager.getUserId(it) }
            ?: (authState.value as? AuthState.Success)?.user?.uid

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val map = mutableMapOf<String, Any?>("p_room_id" to roomId)
                if (!targetUserId.isNullOrBlank()) {
                    map["p_user_id"] = targetUserId
                }
                val res = SupabaseClient.api.demoteSpeaker(
                    auth = token,
                    body = map
                )
                if (res.isSuccessful) {
                    val isSelfDemotion = targetUserId.isNullOrBlank() || targetUserId == myUid
                    withContext(Dispatchers.Main) {
                        if (isSelfDemotion) {
                            fetchRoomToken(roomId, ctx)
                            com.example.realtime.RoomMicController.onRoleChanged("listener", false)
                            _mySpeakerRequestStatus.value = null
                            com.example.realtime.AgoraVoiceManager.setClientRole(com.example.realtime.AgoraVoiceManager.CLIENT_ROLE_AUDIENCE)
                            com.example.realtime.AgoraVoiceManager.muteLocalAudioStream(true)
                            onResult?.invoke(true, "You stepped down to listener.")
                        } else {
                            refreshActiveRoomSpeakers(roomId, ctx)
                            onResult?.invoke(true, "Speaker moved to listeners.")
                        }
                    }
                } else {
                    val errStr = res.errorBody()?.string() ?: ""
                    withContext(Dispatchers.Main) {
                        onResult?.invoke(false, com.example.util.ErrorMapper.mapHttp(res.code(), errStr))
                    }
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error in demoteSpeaker", e)
                withContext(Dispatchers.Main) {
                    onResult?.invoke(false, com.example.util.ErrorMapper.map(e))
                }
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
        if (hostId != null) {
            _activeRoomHostId.value = hostId
        }
        roomJoinedAtMs = System.currentTimeMillis()
        TelemetryManager.logRoomJoined()
        
        val ctx = context
        val token = ctx?.let { SupabaseSessionManager.getBearerToken(it) } ?: "Bearer ${BuildConfig.SUPABASE_ANON_KEY}"
        val uid = ctx?.let { SupabaseSessionManager.getUserId(it) }
            ?: (authState.value as? AuthState.Success)?.user?.uid
            ?: return

        com.example.realtime.RoomMicController.initRoom(
            roomId = roomId,
            userId = uid,
            initialRole = role,
            initialMic = (role == "host" || role == "speaker")
        )

        refreshActiveRoomSpeakers(roomId, context)
        if (hostId == uid || _activeRoomHostId.value == uid) {
            checkPendingSpeakerRequestsForHost(roomId, context)
        } else {
            checkMySpeakerRequest(roomId, context)
        }

        val userAvatar = (authState.value as? AuthState.Success)?.user?.avatar
        com.example.service.VoiceRoomOverlayManager.updateSession(
            roomId = roomId,
            roomTitle = title,
            avatarUrl = userAvatar,
            isMicEnabled = (role == "speaker" || role == "host")
        )
        com.example.service.VoiceRoomOverlayManager.onToggleMicAction = { toggleMic(context) }
        com.example.service.VoiceRoomOverlayManager.onLeaveRoomAction = { leaveRoom(context, trigger = "notification_leave_action") }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Resolve hostId from DB if not known yet
                var effectiveHostId = hostId ?: _activeRoomHostId.value
                if (effectiveHostId == null) {
                    try {
                        val roomRes = SupabaseClient.api.getVoiceRoomById(auth = token, idEq = "eq.$roomId")
                        if (roomRes.isSuccessful) {
                            effectiveHostId = roomRes.body()?.firstOrNull()?.host_id
                            if (effectiveHostId != null) {
                                withContext(Dispatchers.Main) { _activeRoomHostId.value = effectiveHostId }
                            }
                        }
                    } catch (_: Exception) {}
                }

                val isHost = (uid == effectiveHostId)
                Log.i("VoiceRoomLifecycle", "joinRoom executed: roomId=$roomId, role=$role, isHost=$isHost")

                // If Host rejoins, cancel the 3-minute grace period and restore host
                if (isHost) {
                    try {
                        Log.i("VoiceRoomLifecycle", "RPC host_rejoin_voice_room: roomId=$roomId, trigger=join_room, isHost=true")
                        val rejoinRes = SupabaseClient.api.hostRejoinVoiceRoom(
                            auth = token,
                            body = mapOf("p_room_id" to roomId)
                        )
                        Log.i("VoiceRoomLifecycle", "host_rejoin_voice_room status: ${rejoinRes.code()}")
                    } catch (rejoinEx: Exception) {
                        Log.e("VoiceRoomLifecycle", "host_rejoin_voice_room error for roomId=$roomId", rejoinEx)
                    }

                    try {
                        SupabaseClient.api.updateVoiceRoom(
                            auth = token,
                            idEq = "eq.$roomId",
                            updates = UpdateVoiceRoomRequest(host_left_at = null)
                        )
                    } catch (_: Exception) {}

                    try {
                        VoiceRoomRealtimeManager.broadcastRoomLifecycle(
                            roomId = roomId,
                            action = "host_returned"
                        )
                    } catch (_: Exception) {}
                }

                SupabaseClient.api.joinRoomParticipant(
                    auth = token,
                    participant = SupabaseRoomParticipant(
                        room_id = roomId,
                        user_id = uid,
                        role = if (isHost) "host" else role,
                        is_mic_enabled = (role == "speaker" || isHost)
                    )
                )
                Log.d("AuthViewModel", "User $uid joined room $roomId as ${if (isHost) "host" else role} via Supabase room_participants")
                
                // Send a system message that the user has joined, so everyone in the room sees it
                val userObj = (authState.value as? AuthState.Success)?.user
                val username = userObj?.displayName?.ifBlank { null } ?: userObj?.username?.ifBlank { null } ?: "Someone"
                val avatar = userObj?.avatar ?: ""

                // 1. Broadcast participant update via WebSocket realtime so everyone's participant list refreshes immediately
                try {
                    VoiceRoomRealtimeManager.broadcastParticipantUpdate(
                        roomId = roomId,
                        userId = uid,
                        action = "joined",
                        role = role,
                        isMicEnabled = (role == "speaker"),
                        userName = username,
                        userAvatar = avatar
                    )
                } catch (pEx: Exception) {
                    Log.e("AuthViewModel", "Error broadcasting participant update", pEx)
                }

                // 2. Broadcast client-side join message via WebSocket realtime for live listeners
                try {
                    VoiceRoomRealtimeManager.broadcastRoomMessage(
                        roomId = roomId,
                        message = "$username joined the room",
                        userId = uid,
                        senderName = "System",
                        senderAvatar = avatar.ifBlank { "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=200" },
                        messageId = "sys_${System.currentTimeMillis()}"
                    )
                } catch (bEx: Exception) {
                    Log.e("AuthViewModel", "Error broadcasting join message", bEx)
                }

                // 3. Persist to Postgres room_messages table via REST API
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

    fun hostRejoinVoiceRoom(roomId: String, context: Context? = null, trigger: String = "foreground_return") {
        val ctx = context ?: FunkyTalkApplication.appContext
        val token = ctx?.let { SupabaseSessionManager.getBearerToken(it) } ?: "Bearer ${BuildConfig.SUPABASE_ANON_KEY}"
        val uid = ctx?.let { SupabaseSessionManager.getUserId(it) }
            ?: (authState.value as? AuthState.Success)?.user?.uid
        val hostId = _activeRoomHostId.value
        val isHost = (uid != null && (hostId == null || uid.equals(hostId, ignoreCase = true)))

        Log.i("VoiceRoomLifecycle", "RPC host_rejoin_voice_room: roomId=$roomId, trigger=$trigger, isHost=$isHost")

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val resp = SupabaseClient.api.hostRejoinVoiceRoom(
                    auth = token,
                    body = mapOf("p_room_id" to roomId)
                )
                Log.i("VoiceRoomLifecycle", "host_rejoin_voice_room status: ${resp.code()} for roomId=$roomId")
            } catch (e: Exception) {
                Log.e("VoiceRoomLifecycle", "host_rejoin_voice_room failed for $roomId", e)
            }

            try {
                SupabaseClient.api.updateVoiceRoom(
                    auth = token,
                    idEq = "eq.$roomId",
                    updates = UpdateVoiceRoomRequest(host_left_at = null)
                )
            } catch (_: Exception) {}

            try {
                VoiceRoomRealtimeManager.broadcastRoomLifecycle(
                    roomId = roomId,
                    action = "host_returned"
                )
            } catch (_: Exception) {}
        }
    }

    fun leaveRoom(context: Context? = null, trigger: String = "user_leave_button") {
        val roomId = _activeRoomId.value
        val ctx = context ?: FunkyTalkApplication.appContext
        val token = ctx?.let { SupabaseSessionManager.getBearerToken(it) } ?: "Bearer ${BuildConfig.SUPABASE_ANON_KEY}"
        val uid = ctx?.let { SupabaseSessionManager.getUserId(it) }
            ?: (authState.value as? AuthState.Success)?.user?.uid
        val hostId = _activeRoomHostId.value
        val isHost = (uid != null && hostId != null && uid.equals(hostId, ignoreCase = true))

        Log.i("VoiceRoomLifecycle", "leaveRoom invoked: roomId=$roomId, trigger=$trigger, isHost=$isHost")

        // CRITICAL: For Host, leave_voice_room must ONLY be invoked if user explicitly taps Leave/End button!
        // Never allow from background, screen lock, rotation, navigation, or disposal.
        val isExplicitUserLeave = trigger in listOf(
            "user_leave_button",
            "user_end_button",
            "host_leave_only_dialog",
            "notification_leave_action",
            "floating_bubble_leave_button",
            "dynamic_island_leave_button"
        )
        if (isHost && !isExplicitUserLeave) {
            Log.w("VoiceRoomLifecycle", "Suppressed non-user leave_voice_room for host on roomId=$roomId with trigger='$trigger'")
            return
        }

        _activeRoomId.value = null
        _activeRoomTitle.value = null
        _activeRoomHostId.value = null
        if (roomJoinedAtMs > 0L) {
            val durationSec = ((System.currentTimeMillis() - roomJoinedAtMs) / 1000L).coerceAtLeast(0L)
            TelemetryManager.logRoomLeft(durationSec)
            roomJoinedAtMs = 0L
        }
        _pendingSpeakerRequests.value = emptyList()
        _pendingSpeakerRequest.value = null
        _mySpeakerRequestStatus.value = null
        _activeRoomSpeakers.value = emptyList()
        com.example.realtime.RoomMicController.reset()
        endVoiceSession()

        if (ctx != null) {
            com.example.service.VoiceRoomOverlayManager.stopService(ctx)
        }
        com.example.service.VoiceRoomOverlayManager.clear()

        if (roomId != null && uid != null) {
            VoiceRoomRealtimeManager.removeLiveParticipant(roomId, uid)
            val userObj = (authState.value as? AuthState.Success)?.user
            val username = userObj?.displayName?.ifBlank { null } ?: userObj?.username?.ifBlank { null } ?: "Someone"

            viewModelScope.launch(Dispatchers.IO) {
                // 1. Broadcast cancellation of any pending speaker request for this user
                try {
                    VoiceRoomRealtimeManager.broadcastSpeakerRequest(
                        roomId = roomId,
                        requestId = "cancelled_$uid",
                        userId = uid,
                        action = "cancelled",
                        status = "cancelled"
                    )
                } catch (_: Exception) {}

                // 2. Broadcast participant left
                try {
                    VoiceRoomRealtimeManager.broadcastParticipantUpdate(
                        roomId = roomId,
                        userId = uid,
                        action = "left",
                        role = "listener",
                        isMicEnabled = false
                    )
                } catch (_: Exception) {}

                try {
                    VoiceRoomRealtimeManager.broadcastRoomMessage(
                        roomId = roomId,
                        message = "$username left the room",
                        userId = uid,
                        senderName = "System",
                        senderAvatar = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=200",
                        messageId = "sys_${System.currentTimeMillis()}"
                    )
                } catch (_: Exception) {}

                // 3. Clean up pending speaker requests for this user in this room on Supabase
                try {
                    SupabaseClient.api.deletePendingSpeakerRequest(
                        auth = token,
                        roomIdEq = "eq.$roomId",
                        userIdEq = "eq.$uid"
                    )
                } catch (_: Exception) {}

                // 4. Remove participant / execute leave_voice_room RPC
                try {
                    Log.i("VoiceRoomLifecycle", "RPC leave_voice_room: roomId=$roomId, trigger=$trigger, isHost=$isHost")
                    val leaveRes = SupabaseClient.api.leaveVoiceRoom(
                        auth = token,
                        body = mapOf("p_room_id" to roomId)
                    )
                    Log.i("VoiceRoomLifecycle", "leave_voice_room status: ${leaveRes.code()} for roomId=$roomId")
                } catch (_: Exception) {
                    try {
                        SupabaseClient.api.leaveRoomParticipant(
                            auth = token,
                            roomIdEq = "eq.$roomId",
                            userIdEq = "eq.$uid"
                        )
                    } catch (e: Exception) {
                        Log.e("VoiceRoomLifecycle", "Error leaving room $roomId in Supabase", e)
                    }
                }
            }
        }
    }

    /**
     * Host chooses "Leave Only": leaves current room screen but does NOT end the room.
     * Room remains open for others, floating bubble can run in background, and 3-minute grace period starts.
     */
    fun hostLeaveRoomOnly(context: Context? = null, trigger: String = "host_leave_only_dialog", onComplete: (() -> Unit)? = null) {
        val roomId = _activeRoomId.value
        val ctx = context ?: FunkyTalkApplication.appContext
        val token = ctx?.let { SupabaseSessionManager.getBearerToken(it) } ?: "Bearer ${BuildConfig.SUPABASE_ANON_KEY}"
        val uid = ctx?.let { SupabaseSessionManager.getUserId(it) }
            ?: (authState.value as? AuthState.Success)?.user?.uid

        Log.i("VoiceRoomLifecycle", "hostLeaveRoomOnly invoked: roomId=$roomId, trigger=$trigger, isHost=true")

        _activeRoomId.value = null
        _activeRoomTitle.value = null
        _activeRoomHostId.value = null
        _pendingSpeakerRequests.value = emptyList()
        _pendingSpeakerRequest.value = null
        _mySpeakerRequestStatus.value = null
        _activeRoomSpeakers.value = emptyList()
        com.example.realtime.RoomMicController.reset()

        if (roomId != null && uid != null) {
            VoiceRoomRealtimeManager.removeLiveParticipant(roomId, uid)
            val nowIso = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }.format(java.util.Date())

            viewModelScope.launch(Dispatchers.IO) {
                // Broadcast host away event
                try {
                    VoiceRoomRealtimeManager.broadcastRoomLifecycle(
                        roomId = roomId,
                        action = "host_away",
                        hostLeftAt = nowIso
                    )
                } catch (_: Exception) {}

                try {
                    VoiceRoomRealtimeManager.broadcastParticipantUpdate(
                        roomId = roomId,
                        userId = uid,
                        action = "host_away",
                        role = "host",
                        isMicEnabled = false
                    )
                } catch (_: Exception) {}

                // Set host_left_at in database to start server-authoritative 3-minute grace period
                try {
                    Log.i("VoiceRoomLifecycle", "RPC leave_voice_room (hostLeaveRoomOnly): roomId=$roomId, trigger=$trigger, isHost=true")
                    val leaveRes = SupabaseClient.api.leaveVoiceRoom(
                        auth = token,
                        body = mapOf("p_room_id" to roomId)
                    )
                    Log.i("VoiceRoomLifecycle", "leave_voice_room status: ${leaveRes.code()} for roomId=$roomId")
                } catch (_: Exception) {}

                try {
                    SupabaseClient.api.updateVoiceRoom(
                        auth = token,
                        idEq = "eq.$roomId",
                        updates = UpdateVoiceRoomRequest(host_left_at = nowIso)
                    )
                } catch (_: Exception) {}
            }
        }
        onComplete?.invoke()
    }

    /**
     * Host chooses "End Room": explicitly closes room, ends active sessions, and notifies everyone.
     */
    fun endActiveVoiceRoom(context: Context? = null, onComplete: (() -> Unit)? = null) {
        val roomId = _activeRoomId.value
        val ctx = context ?: FunkyTalkApplication.appContext
        val token = ctx?.let { SupabaseSessionManager.getBearerToken(it) } ?: "Bearer ${BuildConfig.SUPABASE_ANON_KEY}"
        val uid = ctx?.let { SupabaseSessionManager.getUserId(it) }
            ?: (authState.value as? AuthState.Success)?.user?.uid

        _activeRoomId.value = null
        _activeRoomTitle.value = null
        _activeRoomHostId.value = null
        _pendingSpeakerRequests.value = emptyList()
        _pendingSpeakerRequest.value = null
        _mySpeakerRequestStatus.value = null
        _activeRoomSpeakers.value = emptyList()

        if (ctx != null) {
            com.example.service.VoiceRoomOverlayManager.stopService(ctx)
        }
        com.example.service.VoiceRoomOverlayManager.clear()

        if (roomId != null) {
            val nowIso = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }.format(java.util.Date())

            viewModelScope.launch(Dispatchers.IO) {
                // Broadcast room_ended to realtime immediately so all participants exit
                try {
                    VoiceRoomRealtimeManager.broadcastRoomLifecycle(
                        roomId = roomId,
                        action = "room_ended"
                    )
                } catch (_: Exception) {}

                try {
                    VoiceRoomRealtimeManager.broadcastRoomMessage(
                        roomId = roomId,
                        message = "The host has ended this voice room.",
                        userId = uid ?: "",
                        senderName = "System",
                        senderAvatar = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=200",
                        messageId = "sys_ended_${System.currentTimeMillis()}"
                    )
                } catch (_: Exception) {}

                // Call end_voice_room RPC
                try {
                    SupabaseClient.api.endVoiceRoom(
                        auth = token,
                        body = mapOf("p_room_id" to roomId)
                    )
                } catch (_: Exception) {}

                // Explicit fallback update
                try {
                    SupabaseClient.api.updateVoiceRoom(
                        auth = token,
                        idEq = "eq.$roomId",
                        updates = UpdateVoiceRoomRequest(
                            is_live = false,
                            status = "ended",
                            ended_at = nowIso
                        )
                    )
                } catch (_: Exception) {}
            }
        }
        onComplete?.invoke()
    }

    /**
     * Verifies against Supabase that the room is still live and active before navigating in.
     * If the room has ended, clears active room state, stops the floating overlay service, and triggers onRoomEnded.
     */
    fun verifyAndNavigateToRoom(
        context: Context,
        onRoomActive: (String) -> Unit,
        onRoomEnded: () -> Unit
    ) {
        val roomId = _activeRoomId.value ?: com.example.service.VoiceRoomOverlayManager.roomId.value
        if (roomId.isNullOrBlank()) {
            com.example.service.VoiceRoomOverlayManager.stopService(context)
            com.example.service.VoiceRoomOverlayManager.clear()
            onRoomEnded()
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val token = SupabaseSessionManager.getBearerToken(context)
                val resp = SupabaseClient.api.getVoiceRoomById(auth = token, idEq = "eq.$roomId")
                val room = if (resp.isSuccessful) resp.body()?.firstOrNull() else null
                val isActive = room != null && (room.is_live == true) && (room.status != "ended")

                withContext(Dispatchers.Main) {
                    if (isActive) {
                        _activeRoomId.value = room?.id ?: roomId
                        if (!room?.title.isNullOrBlank()) _activeRoomTitle.value = room?.title
                        if (!room?.host_id.isNullOrBlank()) _activeRoomHostId.value = room?.host_id
                        val uid = SupabaseSessionManager.getUserId(context) ?: (authState.value as? AuthState.Success)?.user?.uid
                        val effectiveTargetRoomId = room?.id ?: roomId
                        if (room?.host_id != null && room.host_id.equals(uid, ignoreCase = true)) {
                            hostRejoinVoiceRoom(effectiveTargetRoomId, context, trigger = "verify_and_navigate_reopen")
                        }
                        onRoomActive(effectiveTargetRoomId)
                    } else {
                        _activeRoomId.value = null
                        _activeRoomTitle.value = null
                        _activeRoomHostId.value = null
                        _pendingSpeakerRequests.value = emptyList()
                        _pendingSpeakerRequest.value = null
                        _mySpeakerRequestStatus.value = null
                        _activeRoomSpeakers.value = emptyList()
                        com.example.service.VoiceRoomOverlayManager.stopService(context)
                        com.example.service.VoiceRoomOverlayManager.clear()
                        onRoomEnded()
                    }
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error verifying room status for room $roomId", e)
                withContext(Dispatchers.Main) {
                    onRoomActive(roomId)
                }
            }
        }
    }

    fun processDeepLinkUri(uri: Uri?, context: Context, onNavigateToRoom: (() -> Unit)? = null) {
        if (uri == null) return
        val scheme = uri.scheme?.lowercase() ?: ""
        if (scheme != "funkytalk" && scheme != "https" && scheme != "http") return

        val host = uri.host?.lowercase() ?: ""
        val path = uri.path ?: ""
        val pathSegments = uri.pathSegments

        Log.d("DeepLink", "Processing deep link: $uri, host=$host, path=$path, segments=$pathSegments")

        // Handle funkytalk://group/{code} or https://<GROUP_INVITE_HOST>/g/{code} or https://funkytalk.app/group/{code}
        if (
            host == "group" ||
            pathSegments.contains("group") ||
            pathSegments.contains("g") ||
            host.equals(BuildConfig.GROUP_INVITE_HOST, ignoreCase = true)
        ) {
            val groupCode = com.example.repository.GroupChatRepository.parseInviteCode(uri.toString())
            if (groupCode.isNotBlank()) {
                com.example.repository.GroupChatRepository.setPendingInviteCode(groupCode)
                context.getSharedPreferences("funky_talk_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .putString("pending_group_invite_code", groupCode)
                    .apply()
            }
            return
        }

        if (scheme != "funkytalk") return

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
                            (it.id == clean || it.join_code.equals(clean, ignoreCase = true) ||
                             com.example.util.RoomShareHelper.getDisplayRoomCode(it.id, it.join_code).equals(clean, ignoreCase = true)) && it.is_private != true
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
                val gcmSenderId = GoogleServicesConfig.getGcmSenderId(context)

                val options = com.google.firebase.FirebaseOptions.Builder()
                    .setApiKey(apiKey)
                    .setApplicationId(appId)
                    .setProjectId(projectId)
                    .setGcmSenderId(gcmSenderId)
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
        if (authUser == null) {
            _sessionVerificationState.value = SessionVerificationState.Idle
            return
        }

        if (context != null) {
            val isGoogleAccount = authUser.providerData.any { it.providerId == "google.com" }
            val hasLocalSession = SupabaseSessionManager.hasValidAuthenticatedSession(context)
            val cachedUid = SupabaseSessionManager.getUserId(context)
                ?: com.example.data.StartupPrefsStore.getFastSnapshot(context).lastUid
                ?: authUser.uid
            val cachedLocalProfile = com.example.data.StartupPrefsStore.buildLocalCachedUserProfile(
                context = context,
                userId = cachedUid,
                fallbackEmail = authUser.email ?: ""
            )
            val hasLocalProfileData = cachedLocalProfile.onboardingComplete || !cachedLocalProfile.username.isNullOrBlank()

            // Local-first startup: never make splash wait on network when local session or profile exists
            if (hasLocalSession || hasLocalProfileData) {
                val prefs = context.getSharedPreferences("funky_talk_prefs", Context.MODE_PRIVATE)
                val initialVerified = if (isGoogleAccount) {
                    prefs.edit().putBoolean("custom_email_verified_${cachedUid}", true).apply()
                    true
                } else {
                    cachedLocalProfile.isEmailVerified
                }
                _sessionVerificationState.value = SessionVerificationState.Verified
                TelemetryManager.onLogin(cachedUid)
                _authState.value = AuthState.Success(cachedLocalProfile.copy(isEmailVerified = initialVerified))
                startSessionListener(context, cachedUid)

                // Revalidate session and profile silently in the background (SWR)
                viewModelScope.launch {
                    val verifiedUid = com.example.util.StartupTimingTracker.measure("verify_firebase_token") {
                        if (hasLocalSession) {
                            SupabaseSessionManager.getUserId(context)
                        } else {
                            SupabaseSessionManager.ensureActiveSession(context)
                                ?: if (!com.example.repository.DeviceSessionRepository.isRevocationInProgress() &&
                                    firebaseAuth?.currentUser != null &&
                                    com.example.ui.components.AppNetworkManager.checkConnection(context)
                                ) {
                                    SupabaseSessionManager.forceRefreshSession(context)
                                } else {
                                    null
                                }
                        }
                    }

                    if (com.example.repository.DeviceSessionRepository.isRevocationInProgress() ||
                        firebaseAuth?.currentUser == null
                    ) {
                        _sessionVerificationState.value = SessionVerificationState.Idle
                        _authState.value = AuthState.Idle
                        return@launch
                    }

                    val online = com.example.ui.components.AppNetworkManager.isOnline.value
                    if (verifiedUid.isNullOrBlank()) {
                        // Never lock out an already-cached/onboarded user with verify_error on cold start or expired JWT;
                        // log quietly for diagnostics while API calls auto-refresh when needed.
                        Log.w("AuthViewModel", "Background boot session refresh deferred (online=$online, hasLocalSession=$hasLocalSession)")
                        com.example.diagnostics.CrashAndErrorReporter.recordNonFatal(
                            source = "BootSessionRefresh",
                            message = "Deferred background token refresh (online=$online)"
                        )
                        return@launch
                    }

                    loadUserProfileFromSpOrFirestore(
                        context = context,
                        userId = verifiedUid,
                        emitCachedImmediately = true
                    ) { refreshed ->
                        if (!isGoogleAccount && online) {
                            viewModelScope.launch {
                                val realVerify = performRemoteVerificationStatusCheck(context, verifiedUid, false)
                                if (realVerify != refreshed.isEmailVerified) {
                                    prefs.edit().putBoolean("custom_email_verified_${verifiedUid}", realVerify).apply()
                                    _authState.value = AuthState.Success(refreshed.copy(isEmailVerified = realVerify))
                                }
                            }
                        }
                    }
                }
                return
            }

            _sessionVerificationState.value = SessionVerificationState.Verifying
            _authState.value = AuthState.Loading
            viewModelScope.launch {
                val verifiedUid = com.example.util.StartupTimingTracker.measure("verify_firebase_token") {
                    SupabaseSessionManager.ensureActiveSession(context)
                        ?: if (!com.example.repository.DeviceSessionRepository.isRevocationInProgress() &&
                            firebaseAuth?.currentUser != null
                        ) {
                            SupabaseSessionManager.forceRefreshSession(context)
                        } else {
                            null
                        }
                }

                if (com.example.repository.DeviceSessionRepository.isRevocationInProgress() ||
                    firebaseAuth?.currentUser == null
                ) {
                    _sessionVerificationState.value = SessionVerificationState.Idle
                    _authState.value = AuthState.Idle
                    return@launch
                }

                if (verifiedUid.isNullOrBlank()) {
                    Log.w("AuthViewModel", "Boot session verification failed")
                    _sessionVerificationState.value = SessionVerificationState.Failed(com.example.util.ErrorMapper.MSG_UNAUTHORIZED)
                    _authState.value = AuthState.Error(com.example.util.ErrorMapper.MSG_UNAUTHORIZED)
                    return@launch
                }

                _sessionVerificationState.value = SessionVerificationState.Verified
                val effectiveUid = verifiedUid

                checkAndCleanupUnverifiedAccount(context, authUser) {
                    val prefs = context.getSharedPreferences("funky_talk_prefs", Context.MODE_PRIVATE)
                    val isCustomVerified = if (isGoogleAccount) true else prefs.getBoolean("custom_email_verified_${effectiveUid}", false)
                    if (isGoogleAccount) {
                        prefs.edit().putBoolean("custom_email_verified_${effectiveUid}", true).apply()
                    }
                    loadUserProfileFromSpOrFirestore(context, effectiveUid, emitCachedImmediately = true) { finalProfile ->
                        val initialVerified = isCustomVerified
                        TelemetryManager.onLogin(effectiveUid)
                        _authState.value = AuthState.Success(
                            finalProfile.copy(isEmailVerified = initialVerified)
                        )
                        startSessionListener(context, effectiveUid)

                        if (!isGoogleAccount) {
                            viewModelScope.launch {
                                val realVerify = performRemoteVerificationStatusCheck(context, effectiveUid, false)
                                if (realVerify != initialVerified) {
                                    prefs.edit().putBoolean("custom_email_verified_${effectiveUid}", realVerify).apply()
                                    _authState.value = AuthState.Success(
                                        finalProfile.copy(isEmailVerified = realVerify)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            _sessionVerificationState.value = SessionVerificationState.Verified
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
                        try {
                            FirebaseAuth.getInstance().signOut()
                        } catch (_: Exception) {}
                        Toast.makeText(context, "Registration session expired (2-hour limit). Please sign up again.", Toast.LENGTH_LONG).show()
                        _authState.value = AuthState.Error("Registration session expired (2 hours max). Please sign up again.")
                        onCleaned()
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
                    val isNewFirebaseUser = result.additionalUserInfo?.isNewUser ?: true
                    fun cleanupOrphanedSignupUser() {
                        if (firebaseUser != null && isNewFirebaseUser) {
                            try {
                                firebaseUser.delete().addOnCompleteListener {
                                    try { auth.signOut() } catch (_: Exception) {}
                                }
                            } catch (_: Exception) {
                                try { auth.signOut() } catch (_: Exception) {}
                            }
                        } else {
                            try { auth.signOut() } catch (_: Exception) {}
                        }
                    }

                    if (firebaseUser != null) {
                        firebaseUser.getIdToken(true)
                            .addOnSuccessListener { tokenResult ->
                                val firebaseIdToken = tokenResult.token
                                if (!firebaseIdToken.isNullOrEmpty()) {
                                    viewModelScope.launch {
                                        // STEP 2: POST to verify-firebase-token edge function
                                        try {
                                            val exchange = SupabaseSessionManager.exchangeFirebaseToken(
                                                context = context,
                                                firebaseIdToken = firebaseIdToken,
                                                fallbackUserId = firebaseUser.uid,
                                                kind = SupabaseSessionManager.KIND_LOGIN
                                            )

                                            if (exchange is SupabaseSessionManager.ExchangeResult.Success) {
                                                val accessToken = exchange.accessToken
                                                val userId = exchange.userId

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
                                                    if (BuildConfig.DEBUG) {
                                                        Log.d("AuthViewModel", "Verification email dispatched successfully via Brevo/Supabase")
                                                    }
                                                } else {
                                                    Log.w("AuthViewModel", "send-verification-email call status: ${sendRes.code()}")
                                                }

                                                TelemetryManager.onLogin(userId)
                                                TelemetryManager.logSignupCompleted()
                                                _authState.value = AuthState.Success(
                                                    UserProfile(
                                                        uid = userId,
                                                        email = cleanEmail,
                                                        displayName = null,
                                                        isEmailVerified = false
                                                    )
                                                )
                                                onVerificationSent()
                                            } else if (exchange is SupabaseSessionManager.ExchangeResult.DeviceRevoked) {
                                                _authState.value = AuthState.Idle
                                            } else {
                                                val failMsg = (exchange as? SupabaseSessionManager.ExchangeResult.Failure)?.message
                                                    ?: com.example.repository.DeviceSessionRepository.SESSION_MINT_FAILED_MESSAGE
                                                Log.e("AuthViewModel", "verify-firebase-token failed during signup")
                                                cleanupOrphanedSignupUser()
                                                _authState.value = AuthState.Error(failMsg)
                                            }
                                        } catch (e: kotlinx.coroutines.CancellationException) {
                                            throw e
                                        } catch (e: Exception) {
                                            Log.e("AuthViewModel", "verify-firebase-token exception: ${e.javaClass.simpleName}")
                                            cleanupOrphanedSignupUser()
                                            _authState.value = AuthState.Error(
                                                com.example.repository.DeviceSessionRepository.SESSION_MINT_FAILED_MESSAGE
                                            )
                                        }
                                    }
                                } else {
                                    cleanupOrphanedSignupUser()
                                    _authState.value = AuthState.Error("Failed to obtain Firebase ID token.")
                                }
                            }
                            .addOnFailureListener { e ->
                                cleanupOrphanedSignupUser()
                                _authState.value = AuthState.Error(com.example.util.ErrorMapper.mapFirebaseAuthError(e))
                            }
                    } else {
                        _authState.value = AuthState.Error("Account creation error. Please try again.")
                    }
                }
                .addOnFailureListener { e ->
                    val errorCode = (e as? com.google.firebase.auth.FirebaseAuthException)?.errorCode
                    if (e is com.google.firebase.auth.FirebaseAuthUserCollisionException ||
                        errorCode == "ERROR_EMAIL_ALREADY_IN_USE" ||
                        errorCode == "email-already-in-use" ||
                        e.message?.contains("already in use", ignoreCase = true) == true
                    ) {
                        _authState.value = AuthState.AccountCollision(
                            email = cleanEmail,
                            message = "An account with this email address already exists. Only one account is permitted per email address."
                        )
                    } else {
                        _authState.value = AuthState.Error(com.example.util.ErrorMapper.mapFirebaseAuthError(e))
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
                                            val exchange = SupabaseSessionManager.exchangeFirebaseToken(
                                                context = context,
                                                firebaseIdToken = firebaseIdToken,
                                                fallbackUserId = firebaseUser.uid,
                                                kind = SupabaseSessionManager.KIND_LOGIN
                                            )

                                            if (exchange is SupabaseSessionManager.ExchangeResult.Success) {
                                                val accessToken = exchange.accessToken
                                                val userId = exchange.userId

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
                                                    Log.e("AuthViewModel", "Error fetching user from Supabase: ${e.javaClass.simpleName}")
                                                }

                                                val prefs = context.getSharedPreferences("funky_talk_prefs", Context.MODE_PRIVATE)
                                                prefs.edit().putBoolean("custom_email_verified_${userId}", isCustomVerified).apply()

                                                loadUserProfileFromSpOrFirestore(context, userId) { finalProfile ->
                                                    val finalProfileWithVerified = finalProfile.copy(isEmailVerified = isCustomVerified)
                                                    TelemetryManager.onLogin(userId)
                                                    TelemetryManager.logLoginCompleted()
                                                    _authState.value = AuthState.Success(finalProfileWithVerified)
                                                    com.example.service.PushNotificationManager.onUserLoggedIn(context)

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
                                            } else if (exchange is SupabaseSessionManager.ExchangeResult.DeviceRevoked) {
                                                _authState.value = AuthState.Idle
                                            } else {
                                                val failMsg = (exchange as? SupabaseSessionManager.ExchangeResult.Failure)?.message
                                                    ?: com.example.repository.DeviceSessionRepository.SESSION_MINT_FAILED_MESSAGE
                                                Log.e("AuthViewModel", "verify-firebase-token failed during login")
                                                try { auth.signOut() } catch (_: Exception) {}
                                                _authState.value = AuthState.Error(failMsg)
                                            }
                                        } catch (e: kotlinx.coroutines.CancellationException) {
                                            throw e
                                        } catch (e: Exception) {
                                            Log.e("AuthViewModel", "verify-firebase-token exception: ${e.javaClass.simpleName}")
                                            try { auth.signOut() } catch (_: Exception) {}
                                            _authState.value = AuthState.Error(
                                                com.example.repository.DeviceSessionRepository.SESSION_MINT_FAILED_MESSAGE
                                            )
                                        }
                                    }
                                } else {
                                    _authState.value = AuthState.Error("Failed to obtain security credentials.")
                                }
                            }
                            .addOnFailureListener { e ->
                                _authState.value = AuthState.Error(com.example.util.ErrorMapper.mapFirebaseAuthError(e))
                            }
                    } else {
                        _authState.value = AuthState.Error("Unable to find user account. Please try again.")
                    }
                }
                .addOnFailureListener { e ->
                    _authState.value = AuthState.Error(com.example.util.ErrorMapper.mapFirebaseAuthError(e))
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
                        registerAndStartSession(context, supabaseUserId)
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
                val friendly = com.example.util.ErrorMapper.mapFirebaseAuthError(e)
                _authState.value = AuthState.Error(friendly)
                Toast.makeText(context, friendly, Toast.LENGTH_LONG).show()
            }
    }

    fun registerAndStartSession(context: Context, uid: String) {
        val newSessionId = java.util.UUID.randomUUID().toString()
        val prefs = context.getSharedPreferences("funky_talk_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("active_session_id_${uid}", newSessionId).apply()
        Log.d("AuthViewModel", "Registered active session: $newSessionId")
        com.example.service.PushNotificationManager.onUserLoggedIn(context)
    }

    fun startSessionListener(context: Context, uid: String) {
        val prefs = context.getSharedPreferences("funky_talk_prefs", Context.MODE_PRIVATE)
        val localSessionId = prefs.getString("active_session_id_${uid}", null)
        if (localSessionId == null) {
            registerAndStartSession(context, uid)
        } else {
            com.example.service.PushNotificationManager.onUserLoggedIn(context)
        }
    }

    private fun logoutDueToSessionMismatch(context: Context, uid: String) {
        viewModelScope.launch(Dispatchers.Main) {
            logout(context)
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
            Log.e("AuthViewModel", "Error opening verification link in browser", e)
            Toast.makeText(context, "Could not open your browser. Please check your email app.", Toast.LENGTH_SHORT).show()
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

    private var roomJoinedAtMs: Long = 0L

    fun loginWithGoogle(
        context: Context,
        idToken: String,
        isFromSignUpMode: Boolean = false,
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
                    val isNewFirebaseUser = result.additionalUserInfo?.isNewUser ?: false
                    if (user != null) {
                        if (isFromSignUpMode && !isNewFirebaseUser) {
                            val existingEmail = user.email.orEmpty()
                            try { auth.signOut() } catch (_: Exception) {}
                            _authState.value = AuthState.AccountCollision(
                                email = existingEmail,
                                message = "An account with this email address already exists. Only one account is permitted per email address."
                            )
                            return@addOnSuccessListener
                        }
                        user.getIdToken(true).addOnSuccessListener { tokenResult ->
                            val firebaseIdToken = tokenResult.token
                            if (!firebaseIdToken.isNullOrEmpty()) {
                                viewModelScope.launch(Dispatchers.IO + kotlinx.coroutines.NonCancellable) {
                                    val exchange = try {
                                        SupabaseSessionManager.exchangeFirebaseToken(
                                            context = context,
                                            firebaseIdToken = firebaseIdToken,
                                            fallbackUserId = user.uid,
                                            kind = SupabaseSessionManager.KIND_LOGIN
                                        )
                                    } catch (e: Exception) {
                                        if (e is kotlinx.coroutines.CancellationException) throw e
                                        Log.e("AuthViewModel", "Google Supabase session exchange error: ${e.javaClass.simpleName}")
                                        SupabaseSessionManager.ExchangeResult.Failure(
                                            message = com.example.repository.DeviceSessionRepository.SESSION_MINT_FAILED_MESSAGE
                                        )
                                    }

                                    when (exchange) {
                                        is SupabaseSessionManager.ExchangeResult.DeviceRevoked -> {
                                            withContext(Dispatchers.Main) {
                                                _authState.value = AuthState.Idle
                                            }
                                            return@launch
                                        }
                                        is SupabaseSessionManager.ExchangeResult.Failure -> {
                                            try { auth.signOut() } catch (_: Exception) {}
                                            withContext(Dispatchers.Main) {
                                                _authState.value = AuthState.Error(exchange.message)
                                            }
                                            return@launch
                                        }
                                        is SupabaseSessionManager.ExchangeResult.Success -> Unit
                                    }

                                    val supaAccessToken = exchange.accessToken
                                    val sUserId = exchange.userId

                                    // For Google Sign-In: Google accounts are pre-verified via Firebase's own email_verified claim.
                                    // DO NOT generate verification token or call send-verification-email.
                                    val prefs = context.getSharedPreferences("funky_talk_prefs", Context.MODE_PRIVATE)
                                    prefs.edit().putBoolean("custom_email_verified_${sUserId}", true).apply()

                                    // Direct check custom_email_verified on Supabase (optional confirmation)
                                    if (supaAccessToken.isNotEmpty()) {
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
                                            if (BuildConfig.DEBUG) {
                                                Log.d("AuthViewModel", "Google Supabase custom_email_verified check note: ${e.javaClass.simpleName}")
                                            }
                                        }
                                    }

                                    loadUserProfileFromSpOrFirestore(context, sUserId, supaAccessToken) { finalProfile ->
                                        saveUserToFirestore(user, isVerified = true)
                                        TelemetryManager.onLogin(sUserId)
                                        if (isNewFirebaseUser) TelemetryManager.logSignupCompleted() else TelemetryManager.logLoginCompleted()
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
                                try { auth.signOut() } catch (_: Exception) {}
                                _authState.value = AuthState.Error(
                                    com.example.repository.DeviceSessionRepository.SESSION_MINT_FAILED_MESSAGE
                                )
                            }
                        }.addOnFailureListener {
                            try { auth.signOut() } catch (_: Exception) {}
                            _authState.value = AuthState.Error(
                                com.example.repository.DeviceSessionRepository.SESSION_MINT_FAILED_MESSAGE
                            )
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
                        _authState.value = AuthState.Error(com.example.util.ErrorMapper.mapFirebaseAuthError(e))
                    }
                }
        } else {
            _authState.value = AuthState.Error("Google Sign-In failed. Please try again.")
        }
    }

    fun clearStateOnRemoteSignOut() {
        TelemetryManager.onLogout()
        roomJoinedAtMs = 0L
        _activeRoomId.value = null
        _activeRoomTitle.value = null
        _activeRoomHostId.value = null
        _activeRoomSpeakers.value = emptyList()
        _pendingSpeakerRequests.value = emptyList()
        _pendingSpeakerRequest.value = null
        _mySpeakerRequestStatus.value = null
        _supabaseProfiles.value = emptyList()
        _myModerationStatus.value = null
        _accountUnderReview.value = false
        _profileEditLimits.value = emptyMap()
        _deletionStatus.value = DeletionStatusState.Idle
        _deletionReceipt.value = null
        com.example.realtime.RoomMicController.reset()
        endVoiceSession()
        _authState.value = AuthState.Idle
        _sessionVerificationState.value = SessionVerificationState.Idle
    }

    fun logout(context: Context? = null) {
        val supabaseUid = if (context != null) SupabaseSessionManager.getUserId(context) else null
        val currentProfileUid = (_authState.value as? AuthState.Success)?.user?.uid
        val effectiveUid = supabaseUid ?: currentProfileUid ?: firebaseAuth?.currentUser?.uid.orEmpty()

        TelemetryManager.onLogout()
        roomJoinedAtMs = 0L
        _activeRoomId.value = null
        _activeRoomTitle.value = null
        _activeRoomHostId.value = null
        _activeRoomSpeakers.value = emptyList()
        _pendingSpeakerRequests.value = emptyList()
        _pendingSpeakerRequest.value = null
        _mySpeakerRequestStatus.value = null
        _supabaseProfiles.value = emptyList()
        _myModerationStatus.value = null
        _accountUnderReview.value = false
        _profileEditLimits.value = emptyMap()
        _deletionStatus.value = DeletionStatusState.Idle
        _deletionReceipt.value = null
        com.example.realtime.RoomMicController.reset()
        endVoiceSession()
        _authState.value = AuthState.Idle
        _sessionVerificationState.value = SessionVerificationState.Idle

        if (context != null) {
            try { com.example.service.VoiceRoomOverlayManager.stopService(context) } catch (_: Exception) {}
            try { com.example.service.VoiceRoomOverlayManager.clear() } catch (_: Exception) {}
            com.example.service.PushNotificationManager.launchApplicationLogoutCleanup(
                context = context.applicationContext,
                effectiveUid = effectiveUid,
                firebaseAuth = firebaseAuth
            )
        } else {
            com.example.service.PushNotificationManager.cancelInFlightRegistration()
            firebaseAuth?.signOut()
            ActiveUserManager.clear()
            com.example.crypto.VaultSecurityManager.resetForLogout()
            com.example.crypto.E2EEManager.resetForLogout()
            com.example.repository.EncryptedChatRepository.resetForLogout()
            com.example.repository.GroupChatRepository.resetForLogout()
            com.example.ChatPreferencesManager.resetForLogout()
            com.example.repository.BlockRepository.resetForLogout()
            com.example.SupportChatManager.resetForLogout()
            com.example.CloudAiManager.resetForLogout()
            com.example.NotificationsManager.clear()
            com.example.repository.TombstoneManager.resetForLogout()
            com.example.repository.InboxDrainTracker.reset()
            com.example.repository.FeedbackRepository.resetForLogout()
            com.example.store.CosmeticsManager.clearForLogout()
            com.example.gifting.GiftManager.resetForLogout()
            com.example.ModerationManager.resetForLogout()
            com.example.realtime.VoiceRoomRealtimeManager.resetForLogout()
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
                var supaUid = SupabaseSessionManager.getUserId(context)?.ifBlank { null }
                    ?: (_authState.value as? AuthState.Success)?.user?.uid?.ifBlank { null }
                    ?: firebaseAuth?.currentUser?.uid?.ifBlank { null }
                    ?: try { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid?.ifBlank { null } } catch (e: Exception) { null }

                if (supaUid.isNullOrBlank()) {
                    Log.e("AuthViewModel", "User session not found for patchProfileStep")
                    onSuccess()
                    return@launch
                }

                // Cache user ID if it wasn't present in session manager
                if (SupabaseSessionManager.getUserId(context).isNullOrBlank()) {
                    SupabaseSessionManager.setSession(
                        context = context,
                        accessToken = SupabaseSessionManager.getAccessToken(context) ?: BuildConfig.SUPABASE_ANON_KEY,
                        userId = supaUid
                    )
                }

                val token = SupabaseSessionManager.getBearerToken(context)

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
                if (updates.containsKey("country_code")) {
                    editor.putString("profile_countryCode_${supaUid}", updates["country_code"] as? String ?: "")
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
                        countryCode = if (updates.containsKey("country_code")) updates["country_code"] as? String else u.countryCode,
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
            val cleanUsername = username.trim().lowercase()

            if (avatar.startsWith("content://") || avatar.startsWith("file://") || avatar.startsWith("/")) {
                SupabaseSessionManager.ensureActiveSession(context)
                val supaUid = SupabaseSessionManager.getUserId(context) ?: "anon"
                val bearerAuth = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                    ?: SupabaseSessionManager.getBearerToken(context)

                var avatarBytes: ByteArray? = null
                try {
                    if (avatar.startsWith("content://")) {
                        val uri = android.net.Uri.parse(avatar)
                        context.contentResolver.openInputStream(uri)?.use { avatarBytes = it.readBytes() }
                    } else {
                        val purePath = avatar.removePrefix("file://")
                        val file = java.io.File(purePath)
                        if (file.exists()) avatarBytes = file.readBytes()
                    }
                } catch (e: Exception) {
                    Log.e("AuthViewModel", "Error reading avatar file bytes in step 1", e)
                    onError("Could not read selected photo. Please pick another.")
                    return@launch
                }

                if (avatarBytes == null || avatarBytes!!.isEmpty()) {
                    onError("Selected avatar image could not be read.")
                    return@launch
                }

                val bytes = avatarBytes!!
                val uniqueId = java.util.UUID.randomUUID().toString()
                val filename = "avatar_${supaUid}_${uniqueId}_${System.currentTimeMillis()}.jpg"

                val uploadResult = AppwriteUploader.upload(
                    context = context,
                    fileBytes = bytes,
                    filename = filename,
                    mimeType = "image/jpeg",
                    purpose = "avatar",
                    folderSuffix = null
                )

                when (uploadResult) {
                    is AppwriteUploader.UploadResult.Success -> {
                        finalAvatar = uploadResult.url
                        Log.d("AuthViewModel", "Avatar uploaded via Appwrite proxy in step 1: $finalAvatar")
                    }
                    is AppwriteUploader.UploadResult.Error -> {
                        Log.e("AuthViewModel", "Failed to upload avatar in step 1: ${uploadResult.message}")
                        onError("Failed to upload avatar: ${uploadResult.message}")
                        return@launch
                    }
                }

                if (!finalAvatar.startsWith("http://") && !finalAvatar.startsWith("https://")) {
                    onError("Failed to obtain valid avatar upload URL.")
                    return@launch
                }
            }

            val cleanDisplayName = com.example.ui.screens.profile.sanitizeProfileName(displayName)
            patchProfileStep(
                context = context,
                updates = mapOf(
                    "username" to cleanUsername,
                    "profile_name" to cleanDisplayName,
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
        age: Int? = null,
        bio: String? = null,
        city: String? = null,
        coverUrl: String? = null,
        loadedSnapshot: EditProfileSnapshot? = null,
        onUsernameNextChange: ((String?) -> Unit)? = null,
        onError: ((String) -> Unit)? = null,
        onSuccess: () -> Unit
    ) {
        val currentState = _authState.value
        if (currentState is AuthState.Success) {
            viewModelScope.launch {
                val cleanUsername = username.trim().lowercase()
                val cleanDisplayName = com.example.ui.screens.profile.sanitizeProfileName(displayName)
                val cleanBio = (bio ?: dob).trim()
                val cleanCity = city?.trim().orEmpty()
                var finalAvatar = avatar
                var finalCover = coverUrl?.trim()?.ifBlank { null }

                SupabaseSessionManager.ensureActiveSession(context)
                val supaUid = SupabaseSessionManager.getUserId(context) ?: currentState.user.uid
                val bearerAuth = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                    ?: SupabaseSessionManager.getBearerToken(context)

                val userId = currentState.user.uid
                val prefs = context.getSharedPreferences("funky_talk_prefs", Context.MODE_PRIVATE)
                var nextUsernameChangeAt: String? = null
                var effectiveUsername = loadedSnapshot?.username?.trim()?.lowercase()?.ifBlank { cleanUsername } ?: cleanUsername
                var avatarOrCoverChanged = false

                try {
                    val limits = _profileEditLimits.value.ifEmpty { fetchProfileEditLimits(context) }

                    val avatarIsLocal = avatar.startsWith("content://") || avatar.startsWith("file://") || avatar.startsWith("/")
                    val avatarChanged = loadedSnapshot == null || avatar != loadedSnapshot.avatar
                    if (avatarIsLocal && avatarChanged) {
                        val avatarLimit = limits["avatar_url"]
                        if (avatarLimit != null && avatarLimit.remaining <= 0) {
                            val refreshed = fetchProfileEditLimits(context)
                            val whenText = formatIsoToLocalDateTime(refreshed["avatar_url"]?.next_allowed_at).ifBlank { "later" }
                            onError?.invoke("You've reached the limit for profile photo. You can change it again on $whenText.")
                            return@launch
                        }
                    }

                    val coverIsLocal = !finalCover.isNullOrBlank() &&
                        (finalCover.startsWith("content://") || finalCover.startsWith("file://") || finalCover.startsWith("/"))
                    val coverChanged = loadedSnapshot == null || (finalCover ?: "") != loadedSnapshot.coverUrl
                    if (coverIsLocal && coverChanged) {
                        val coverLimit = limits["cover_url"]
                        if (coverLimit != null && coverLimit.remaining <= 0) {
                            val refreshed = fetchProfileEditLimits(context)
                            val whenText = formatIsoToLocalDateTime(refreshed["cover_url"]?.next_allowed_at).ifBlank { "later" }
                            onError?.invoke("You've reached the limit for cover photo. You can change it again on $whenText.")
                            return@launch
                        }
                    }

                    // 1. Upload avatar to Appwrite Storage proxy if local URI selected
                    if (avatarIsLocal) {
                        var avatarBytes: ByteArray? = null
                        try {
                            if (avatar.startsWith("content://")) {
                                val uri = android.net.Uri.parse(avatar)
                                context.contentResolver.openInputStream(uri)?.use { avatarBytes = it.readBytes() }
                            } else {
                                val purePath = avatar.removePrefix("file://")
                                val file = java.io.File(purePath)
                                if (file.exists()) avatarBytes = file.readBytes()
                            }
                        } catch (e: Exception) {
                            Log.e("AuthViewModel", "Error reading avatar file bytes", e)
                            onError?.invoke("Could not read selected photo. Please pick another.")
                            return@launch
                        }

                        if (avatarBytes == null || avatarBytes!!.isEmpty()) {
                            Log.e("AuthViewModel", "Avatar image bytes are empty")
                            onError?.invoke("Selected avatar image could not be read.")
                            return@launch
                        }

                        val bytes = avatarBytes!!
                        val uniqueId = java.util.UUID.randomUUID().toString()
                        val filename = "avatar_${supaUid}_${uniqueId}_${System.currentTimeMillis()}.jpg"

                        val uploadResult = AppwriteUploader.upload(
                            context = context,
                            fileBytes = bytes,
                            filename = filename,
                            mimeType = "image/jpeg",
                            purpose = "avatar",
                            folderSuffix = null
                        )

                        when (uploadResult) {
                            is AppwriteUploader.UploadResult.Success -> {
                                finalAvatar = uploadResult.url
                                avatarOrCoverChanged = true
                                Log.d("AuthViewModel", "Avatar uploaded via Appwrite proxy: $finalAvatar")
                            }
                            is AppwriteUploader.UploadResult.Error -> {
                                Log.e("AuthViewModel", "Failed to upload avatar: ${uploadResult.message}")
                                onError?.invoke(uploadResult.message)
                                return@launch
                            }
                        }
                    } else if (avatarChanged) {
                        avatarOrCoverChanged = true
                    }

                    // 2. Upload cover photo to Appwrite Storage proxy if local URI selected
                    if (coverIsLocal && !finalCover.isNullOrBlank()) {
                        var coverBytes: ByteArray? = null
                        try {
                            if (finalCover.startsWith("content://")) {
                                val uri = android.net.Uri.parse(finalCover)
                                context.contentResolver.openInputStream(uri)?.use { coverBytes = it.readBytes() }
                            } else {
                                val purePath = finalCover.removePrefix("file://")
                                val file = java.io.File(purePath)
                                if (file.exists()) coverBytes = file.readBytes()
                            }
                        } catch (e: Exception) {
                            Log.e("AuthViewModel", "Error reading cover file bytes", e)
                            onError?.invoke("Could not read selected cover photo. Please pick another.")
                            return@launch
                        }

                        if (coverBytes == null || coverBytes!!.isEmpty()) {
                            Log.e("AuthViewModel", "Cover image bytes are empty")
                            onError?.invoke("Selected cover image could not be read.")
                            return@launch
                        }

                        val bytes = coverBytes!!
                        val uniqueId = java.util.UUID.randomUUID().toString()
                        val filename = "cover_${uniqueId}_${System.currentTimeMillis()}.jpg"

                        val uploadResult = AppwriteUploader.upload(
                            context = context,
                            fileBytes = bytes,
                            filename = filename,
                            mimeType = "image/jpeg",
                            purpose = "cover",
                            folderSuffix = null
                        )

                        when (uploadResult) {
                            is AppwriteUploader.UploadResult.Success -> {
                                finalCover = uploadResult.url
                                avatarOrCoverChanged = true
                                Log.d("AuthViewModel", "Cover uploaded via Appwrite proxy: $finalCover")
                            }
                            is AppwriteUploader.UploadResult.Error -> {
                                Log.e("AuthViewModel", "Failed to upload cover: ${uploadResult.message}")
                                onError?.invoke("Failed to upload cover photo: ${uploadResult.message}")
                                return@launch
                            }
                        }
                    } else if (coverChanged) {
                        avatarOrCoverChanged = true
                    }

                    // STRICT SAFEGUARD: Only save HTTP / HTTPS public URLs to database (or preset avatars)!
                    val isPreset = com.example.ui.components.PresetAvatarList.any { it.second == finalAvatar }
                    if (finalAvatar.isNotBlank() && !finalAvatar.startsWith("http://") && !finalAvatar.startsWith("https://") && !isPreset) {
                        Log.e("AuthViewModel", "Prevented saving invalid avatar_url: $finalAvatar")
                        onError?.invoke("Invalid avatar URL.")
                        return@launch
                    }
                    if (!finalCover.isNullOrBlank() && !finalCover.startsWith("http://") && !finalCover.startsWith("https://")) {
                        Log.e("AuthViewModel", "Prevented saving invalid cover_url: $finalCover")
                        onError?.invoke("Invalid cover photo URL.")
                        return@launch
                    }

                    // 3. Username changes MUST go through change_username RPC, never direct PATCH
                    val usernameChanged = loadedSnapshot == null || cleanUsername != loadedSnapshot.username.trim().lowercase()
                    if (usernameChanged && cleanUsername.isNotBlank()) {
                        val rpcRes = SupabaseClient.api.changeUsername(
                            auth = bearerAuth,
                            body = ChangeUsernameRequest(p_username = cleanUsername)
                        )
                        if (!rpcRes.isSuccessful) {
                            val errBody = rpcRes.errorBody()?.string().orEmpty()
                            Log.e("AuthViewModel", "change_username failed (${rpcRes.code()}): $errBody")
                            fetchProfileEditLimits(context)
                            onError?.invoke(parseProfileErrorMessage(errBody))
                            return@launch
                        }
                        val rpcBody = rpcRes.body()
                        effectiveUsername = rpcBody?.username?.trim()?.lowercase()?.ifBlank { cleanUsername } ?: cleanUsername
                        nextUsernameChangeAt = rpcBody?.next_change_at

                        prefs.edit().apply {
                            putString("profile_username_${userId}", effectiveUsername)
                            if (supaUid != userId) putString("profile_username_${supaUid}", effectiveUsername)
                            apply()
                        }
                        (_authState.value as? AuthState.Success)?.let { latestSuccess ->
                            _authState.value = AuthState.Success(latestSuccess.user.copy(username = effectiveUsername))
                        }
                    }

                    val nativeList = nativeLanguage.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    val learningList = learningLanguage.split(",").map { it.trim() }.filter { it.isNotEmpty() }

                    // 4. Build diff-only updates map (NEVER send username, id, auth_user_id, created_at, deletion_scheduled_at)
                    val updates = mutableMapOf<String, Any?>()
                    if (loadedSnapshot == null) {
                        updates["profile_name"] = cleanDisplayName
                        updates["avatar_url"] = finalAvatar
                        updates["gender"] = gender
                        updates["country"] = country
                        updates["country_code"] = countryCode
                        updates["native_languages"] = nativeList
                        updates["learning_languages"] = learningList
                        updates["hobbies"] = hobbies
                        updates["bio"] = cleanBio
                        if (age != null && age > 0) updates["age"] = age
                        if (cleanCity.isNotBlank()) updates["city"] = cleanCity
                        if (!finalCover.isNullOrBlank()) updates["cover_url"] = finalCover
                    } else {
                        if (cleanDisplayName != loadedSnapshot.displayName.trim()) {
                            updates["profile_name"] = cleanDisplayName
                        }
                        if (finalAvatar != loadedSnapshot.avatar) {
                            updates["avatar_url"] = finalAvatar
                        }
                        if ((finalCover ?: "") != loadedSnapshot.coverUrl && !finalCover.isNullOrBlank()) {
                            updates["cover_url"] = finalCover
                        }
                        if (cleanBio != loadedSnapshot.bio.trim()) {
                            updates["bio"] = cleanBio
                        }
                        if (age != null && age > 0 && age != loadedSnapshot.age) {
                            updates["age"] = age
                        }
                        if (gender != loadedSnapshot.gender) {
                            updates["gender"] = gender
                        }
                        if (countryCode != loadedSnapshot.countryCode || country != loadedSnapshot.country) {
                            updates["country"] = country
                            updates["country_code"] = countryCode
                        }
                        if (cleanCity != loadedSnapshot.city.trim()) {
                            updates["city"] = cleanCity
                        }
                        if (nativeList != loadedSnapshot.nativeLanguages) {
                            updates["native_languages"] = nativeList
                        }
                        if (learningList != loadedSnapshot.learningLanguages) {
                            updates["learning_languages"] = learningList
                        }
                        if (hobbies != loadedSnapshot.hobbies) {
                            updates["hobbies"] = hobbies
                        }
                    }

                    if (updates.isNotEmpty()) {
                        val patchRes = SupabaseClient.api.patchProfile(
                            auth = bearerAuth,
                            authUserIdEq = "eq.$supaUid",
                            updates = updates
                        )

                        if (!patchRes.isSuccessful) {
                            val errBody = patchRes.errorBody()?.string().orEmpty()
                            Log.e("AuthViewModel", "Supabase patchProfile failed (${patchRes.code()}): $errBody")
                            fetchProfileEditLimits(context)
                            onError?.invoke(parseProfileErrorMessage(errBody))
                            return@launch
                        }
                    }
                } catch (e: Exception) {
                    Log.e("AuthViewModel", "Supabase update error: ${e.message}", e)
                    fetchProfileEditLimits(context)
                    onError?.invoke(parseProfileErrorMessage(e.message.orEmpty()))
                    return@launch
                }

                prefs.edit().apply {
                    putString("profile_username_${userId}", effectiveUsername)
                    putString("profile_displayName_${userId}", cleanDisplayName)
                    putString("profile_avatar_${userId}", finalAvatar)
                    if (supaUid != userId) {
                        putString("profile_username_${supaUid}", effectiveUsername)
                        putString("profile_displayName_${supaUid}", cleanDisplayName)
                        putString("profile_avatar_${supaUid}", finalAvatar)
                    }
                    if (!finalCover.isNullOrBlank()) {
                        putString("profile_cover_${userId}", finalCover)
                        if (supaUid != userId) putString("profile_cover_${supaUid}", finalCover)
                    } else {
                        remove("profile_cover_${userId}")
                        if (supaUid != userId) remove("profile_cover_${supaUid}")
                    }
                    putString("profile_dob_${userId}", cleanBio)
                    putString("profile_bio_${userId}", cleanBio)
                    if (age != null && age > 0) putInt("profile_age_${userId}", age)
                    if (cleanCity.isNotBlank()) putString("profile_city_${userId}", cleanCity)
                    putString("profile_gender_${userId}", gender)
                    putString("profile_nativeLanguage_${userId}", nativeLanguage)
                    putString("profile_learningLanguage_${userId}", learningLanguage)
                    putString("profile_country_${userId}", country)
                    putString("profile_countryCode_${userId}", countryCode)
                    putString("profile_hobbies_${userId}", hobbies.joinToString("||"))
                    putBoolean("profile_onboarding_complete_${userId}", true)
                    apply()
                }

                // Invalidate Coil cache only when avatar or cover changed
                if (avatarOrCoverChanged) {
                    try {
                        val loader = coil.Coil.imageLoader(context)
                        loader.memoryCache?.clear()
                        loader.diskCache?.clear()
                    } catch (_: Exception) {}
                }

                val latestUser = (_authState.value as? AuthState.Success)?.user ?: currentState.user
                val updatedUser = latestUser.copy(
                    username = effectiveUsername,
                    displayName = cleanDisplayName,
                    avatar = finalAvatar,
                    coverUrl = finalCover ?: latestUser.coverUrl,
                    dob = cleanBio,
                    bio = cleanBio,
                    age = age ?: latestUser.age,
                    city = cleanCity.ifBlank { latestUser.city },
                    gender = gender,
                    nativeLanguage = nativeLanguage,
                    learningLanguage = learningLanguage,
                    country = country,
                    countryCode = countryCode,
                    hobbies = hobbies,
                    onboardingComplete = true
                )
                _authState.value = AuthState.Success(updatedUser)
                fetchProfileEditLimits(context)
                onUsernameNextChange?.invoke(nextUsernameChangeAt)
                onSuccess()
            }
        }
    }

    fun loadUserProfileFromSpOrFirestore(
        context: Context,
        userId: String,
        explicitAccessToken: String? = null,
        emitCachedImmediately: Boolean = false,
        onComplete: (UserProfile) -> Unit = {}
    ) {
        val prefs = context.getSharedPreferences("funky_talk_prefs", Context.MODE_PRIVATE)
        val isCustomVerified = prefs.getBoolean("custom_email_verified_${userId}", false)
        val resolvedVerified = isCustomVerified

        val username = prefs.getString("profile_username_${userId}", null)
        val displayName = prefs.getString("profile_displayName_${userId}", null)
        val avatar = prefs.getString("profile_avatar_${userId}", null)
        val coverVal = prefs.getString("profile_cover_${userId}", null)
        val ageVal = prefs.getInt("profile_age_${userId}", 0).takeIf { it > 0 }
        val dob = prefs.getString("profile_dob_${userId}", null)
        val bioVal = prefs.getString("profile_bio_${userId}", null)
        val cityVal = prefs.getString("profile_city_${userId}", null)
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
            coverUrl = coverVal,
            age = ageVal,
            dob = dob,
            bio = bioVal ?: dob,
            city = cityVal,
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
            coverUrl = coverVal ?: currentProfile.coverUrl,
            age = ageVal ?: currentProfile.age,
            dob = dob ?: currentProfile.dob,
            bio = bioVal ?: currentProfile.bio ?: dob,
            city = cityVal ?: currentProfile.city,
            gender = gender ?: currentProfile.gender,
            nativeLanguage = nativeLanguage ?: currentProfile.nativeLanguage,
            learningLanguage = learningLanguage ?: currentProfile.learningLanguage,
            country = country ?: currentProfile.country,
            countryCode = countryCode ?: currentProfile.countryCode,
            hobbies = if (hobbies.isNotEmpty()) hobbies else currentProfile.hobbies,
            onboardingComplete = isOnboardingComplete || currentProfile.onboardingComplete
        )

        val canUseCached = emitCachedImmediately &&
            (mergedProfile.onboardingComplete || !mergedProfile.username.isNullOrBlank())
        if (canUseCached) {
            _authState.value = AuthState.Success(mergedProfile)
            onComplete(mergedProfile)
        }

        viewModelScope.launch {
            try {
                var bearerToken = if (!explicitAccessToken.isNullOrBlank()) {
                    "Bearer $explicitAccessToken"
                } else {
                    SupabaseSessionManager.getAuthenticatedBearerToken(context)
                        ?: SupabaseSessionManager.getBearerToken(context)
                }
                val supaUid = SupabaseSessionManager.getUserId(context)
                val queryUid = if (supaUid != null && SupabaseSessionManager.isValidUuid(supaUid)) supaUid else userId
                var response = com.example.util.StartupTimingTracker.measure("get_profile") {
                    SupabaseClient.api.getProfile(
                        auth = bearerToken,
                        authUserIdEq = "eq.$queryUid"
                    )
                }
                if (response.code() == 401) {
                    SupabaseSessionManager.forceRefreshSession(context)
                    bearerToken = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                        ?: SupabaseSessionManager.getBearerToken(context)
                    response = SupabaseClient.api.getProfile(
                        auth = bearerToken,
                        authUserIdEq = "eq.$queryUid"
                    )
                }
                if (response.isSuccessful) {
                    val supaProfiles = response.body()
                    if (!supaProfiles.isNullOrEmpty()) {
                        val supaProfile = supaProfiles.first()
                        val inferredComplete = !supaProfile.username.isNullOrBlank() &&
                            !supaProfile.profile_name.isNullOrBlank() &&
                            !supaProfile.gender.isNullOrBlank() &&
                            !supaProfile.country.isNullOrBlank() &&
                            !supaProfile.native_languages.isNullOrEmpty()
                        val supaComplete = (supaProfile.onboarding_complete == true) || inferredComplete
                        val nativeStr = supaProfile.native_languages?.joinToString(", ")
                        val learningStr = supaProfile.learning_languages?.joinToString(", ")
                        val hobbiesList = supaProfile.hobbies ?: emptyList()
                        val supaBio = supaProfile.bio?.ifBlank { null } ?: bioVal

                        prefs.edit().apply {
                            putString("profile_username_${userId}", supaProfile.username ?: "")
                            putString("profile_displayName_${userId}", supaProfile.profile_name ?: "")
                            putString("profile_avatar_${userId}", supaProfile.avatar_url ?: "")
                            if (supaUid != null && supaUid != userId) {
                                putString("profile_username_${supaUid}", supaProfile.username ?: "")
                                putString("profile_displayName_${supaUid}", supaProfile.profile_name ?: "")
                                putString("profile_avatar_${supaUid}", supaProfile.avatar_url ?: "")
                            }
                            if (!supaProfile.cover_url.isNullOrBlank()) {
                                putString("profile_cover_${userId}", supaProfile.cover_url)
                                if (supaUid != null && supaUid != userId) putString("profile_cover_${supaUid}", supaProfile.cover_url)
                            } else {
                                remove("profile_cover_${userId}")
                                if (supaUid != null && supaUid != userId) remove("profile_cover_${supaUid}")
                            }
                            putInt("profile_age_${userId}", supaProfile.age ?: (ageVal ?: 0))
                            if (!supaBio.isNullOrBlank()) putString("profile_bio_${userId}", supaBio)
                            putString("profile_gender_${userId}", supaProfile.gender ?: "")
                            putString("profile_nativeLanguage_${userId}", nativeStr ?: "")
                            putString("profile_learningLanguage_${userId}", learningStr ?: "")
                            putString("profile_country_${userId}", supaProfile.country ?: "")
                            putString("profile_countryCode_${userId}", supaProfile.country_code ?: "")
                            putString("profile_hobbies_${userId}", hobbiesList.joinToString("||"))
                            putBoolean("profile_onboarding_complete_${userId}", supaComplete)
                            apply()
                        }
                        com.example.data.StartupPrefsStore.recordActiveUid(context, userId, supaComplete)

                        val finalProfile = mergedProfile.copy(
                            isEmailVerified = resolvedVerified,
                            username = supaProfile.username ?: mergedProfile.username,
                            displayName = supaProfile.profile_name ?: mergedProfile.displayName,
                            avatar = supaProfile.avatar_url ?: mergedProfile.avatar,
                            coverUrl = supaProfile.cover_url ?: mergedProfile.coverUrl,
                            age = supaProfile.age ?: mergedProfile.age,
                            bio = supaBio ?: mergedProfile.bio,
                            city = cityVal ?: mergedProfile.city,
                            gender = supaProfile.gender ?: mergedProfile.gender,
                            nativeLanguage = nativeStr ?: mergedProfile.nativeLanguage,
                            learningLanguage = learningStr ?: mergedProfile.learningLanguage,
                            country = supaProfile.country ?: mergedProfile.country,
                            countryCode = supaProfile.country_code ?: mergedProfile.countryCode,
                            hobbies = if (hobbiesList.isNotEmpty()) hobbiesList else mergedProfile.hobbies,
                            onboardingComplete = supaComplete
                        )
                        _authState.value = AuthState.Success(finalProfile)
                        if (!canUseCached) {
                            onComplete(finalProfile)
                        }
                        return@launch
                    }
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Failed to load profile from Supabase: ${e.javaClass.simpleName}", e)
            }

            if (!canUseCached) {
                _authState.value = AuthState.Success(mergedProfile)
                onComplete(mergedProfile)
            }
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

    fun updateOrAddProfile(profile: UserProfile) {
        if (profile.uid.isBlank()) return
        val current = _supabaseProfiles.value.toMutableList()
        val idx = current.indexOfFirst { it.uid == profile.uid }
        if (idx >= 0) {
            val existing = current[idx]
            current[idx] = existing.copy(
                displayName = profile.displayName ?: existing.displayName,
                avatar = profile.avatar ?: existing.avatar,
                username = profile.username ?: existing.username,
                country = profile.country ?: existing.country,
                countryCode = profile.countryCode ?: existing.countryCode
            )
        } else {
            current.add(profile)
        }
        _supabaseProfiles.value = current
    }

    fun fetchSupabaseProfiles(context: Context) {
        viewModelScope.launch {
            try {
                // Ensure active authenticated session
                SupabaseSessionManager.ensureActiveSession(context)
                var token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                    ?: SupabaseSessionManager.getBearerToken(context)

                var response = withContext(Dispatchers.IO) {
                    try {
                        SupabaseClient.api.getAllProfiles(
                            apiKey = BuildConfig.SUPABASE_ANON_KEY,
                            auth = token
                        )
                    } catch (e: Exception) {
                        Log.e("AuthViewModel", "Exception calling getAllProfiles: ${e.message}", e)
                        null
                    }
                }

                // If 401, force-refresh session once and retry
                if (response != null && response.code() == 401) {
                    val errBody = response.errorBody()?.string().orEmpty()
                    Log.e("AuthViewModel", "GET /rest/v1/profiles returned 401: $errBody. Refreshing session and retrying...")
                    SupabaseSessionManager.forceRefreshSession(context)
                    token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                        ?: SupabaseSessionManager.getBearerToken(context)
                    response = withContext(Dispatchers.IO) {
                        try {
                            SupabaseClient.api.getAllProfiles(
                                apiKey = BuildConfig.SUPABASE_ANON_KEY,
                                auth = token
                            )
                        } catch (e: Exception) {
                            Log.e("AuthViewModel", "Retry exception calling getAllProfiles: ${e.message}", e)
                            null
                        }
                    }
                }

                if (response != null && response.isSuccessful) {
                    val supaProfiles = response.body() ?: emptyList()
                    val profiles = supaProfiles.map { supaProfile ->
                        val pName = supaProfile.profile_name?.takeIf { it.isNotBlank() }
                            ?: supaProfile.username?.takeIf { it.isNotBlank() }
                            ?: "Learner"
                        val pAvatar = supaProfile.avatar_url?.takeIf { it.isNotBlank() }
                            ?: "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=150"
                        UserProfile(
                            uid = supaProfile.auth_user_id ?: "",
                            email = supaProfile.email ?: "",
                            displayName = pName,
                            isEmailVerified = true,
                            username = supaProfile.username ?: "learner_user",
                            avatar = pAvatar,
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

                    // Also cache them in VoiceRoomProfileManager
                    profiles.forEach { p ->
                        if (p.uid.isNotBlank()) {
                            com.example.realtime.VoiceRoomProfileManager.recordProfile(
                                userId = p.uid,
                                name = p.displayName,
                                avatar = p.avatar,
                                countryCode = p.countryCode,
                                username = p.username
                            )
                        }
                    }
                } else if (response != null) {
                    val code = response.code()
                    val err = response.errorBody()?.string().orEmpty()
                    Log.e("AuthViewModel", "Failed to fetch Supabase profiles: HTTP $code - $err")
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error fetching Supabase profiles: ${e.message}", e)
            }
        }
    }

    // --- Account Deletion Flow ---

    fun checkDeletionStatus(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val prefs = context.applicationContext.getSharedPreferences("funky_deletion_cache", Context.MODE_PRIVATE)
            val cachedPending = prefs.getBoolean("pending_deletion", false)
            if (cachedPending && _deletionStatus.value !is DeletionStatusState.Pending) {
                _deletionStatus.value = DeletionStatusState.Checking
            }
            try {
                val activeUid = SupabaseSessionManager.ensureActiveSession(context)
                if (activeUid.isNullOrBlank()) {
                    if (!cachedPending) {
                        _deletionStatus.value = DeletionStatusState.Clear
                    }
                    return@launch
                }
                var token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                    ?: SupabaseSessionManager.getBearerToken(context)
                if (token.isBlank() || token == "Bearer ${BuildConfig.SUPABASE_ANON_KEY}") {
                    if (!cachedPending) {
                        _deletionStatus.value = DeletionStatusState.Clear
                    }
                    return@launch
                }
                var response = SupabaseClient.api.getMyDeletionStatus(
                    apiKey = BuildConfig.SUPABASE_ANON_KEY,
                    auth = token,
                    body = emptyMap()
                )
                if (response.code() == 401) {
                    SupabaseSessionManager.forceRefreshSession(context)
                    token = SupabaseSessionManager.getAuthenticatedBearerToken(context) ?: token
                    response = SupabaseClient.api.getMyDeletionStatus(
                        apiKey = BuildConfig.SUPABASE_ANON_KEY,
                        auth = token,
                        body = emptyMap()
                    )
                }
                if (response.isSuccessful) {
                    val rawJson = response.body()?.string()?.trim().orEmpty()
                    Log.d("AuthViewModel", "getMyDeletionStatus raw: $rawJson")
                    val json = if (rawJson.startsWith("{")) org.json.JSONObject(rawJson) else org.json.JSONObject()
                    val pending = json.optBoolean("pending", false)
                    prefs.edit().putBoolean("pending_deletion", pending).apply()
                    if (pending) {
                        val statusResp = DeletionStatusResponse(
                            pending = true,
                            receipt_id = json.optString("receipt_id", null),
                            status = json.optString("status", null),
                            scheduled_for = json.optString("scheduled_for", null),
                            can_cancel = if (json.has("can_cancel")) json.optBoolean("can_cancel", true) else true
                        )
                        _deletionStatus.value = DeletionStatusState.Pending(statusResp)
                    } else {
                        _deletionStatus.value = DeletionStatusState.Clear
                    }
                } else {
                    val errBody = response.errorBody()?.string()?.trim().orEmpty()
                    Log.w("AuthViewModel", "getMyDeletionStatus error ${response.code()}: $errBody")
                    com.example.diagnostics.CrashAndErrorReporter.recordNonFatal(
                        source = "DeletionStatusCheck",
                        message = "HTTP ${response.code()} checking deletion status"
                    )
                    if (cachedPending) {
                        _deletionStatus.value = DeletionStatusState.Failed(com.example.util.ErrorMapper.mapHttp(response.code(), errBody))
                    } else {
                        _deletionStatus.value = DeletionStatusState.Clear
                    }
                }
            } catch (e: Exception) {
                Log.w("AuthViewModel", "checkDeletionStatus exception: ${e.message}")
                com.example.diagnostics.CrashAndErrorReporter.recordNonFatal(
                    source = "DeletionStatusCheck",
                    message = e.message ?: "Network error",
                    throwable = e
                )
                if (cachedPending) {
                    _deletionStatus.value = DeletionStatusState.Failed(com.example.util.ErrorMapper.map(e))
                } else {
                    _deletionStatus.value = DeletionStatusState.Clear
                }
            }
        }
    }

    fun cancelAccountDeletion(context: Context, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val token = SupabaseSessionManager.getBearerToken(context)
                val response = SupabaseClient.api.cancelMyAccountDeletion(
                    apiKey = BuildConfig.SUPABASE_ANON_KEY,
                    auth = token,
                    body = emptyMap()
                )
                if (response.isSuccessful) {
                    val rawJson = response.body()?.string()?.trim().orEmpty()
                    val json = if (rawJson.startsWith("{")) org.json.JSONObject(rawJson) else org.json.JSONObject()
                    val ok = json.optBoolean("success", true)
                    withContext(Dispatchers.Main) {
                        if (ok) {
                            _deletionStatus.value = DeletionStatusState.Clear
                            Toast.makeText(context, "Deletion cancelled", Toast.LENGTH_SHORT).show()
                            val uid = SupabaseSessionManager.getUserId(context)
                            if (uid != null) {
                                loadUserProfileFromSpOrFirestore(context, uid) {}
                            }
                            onResult(true, null)
                        } else {
                            val err = com.example.util.ErrorMapper.map(Exception(json.optString("error", "")))
                            onResult(false, err)
                        }
                    }
                } else {
                    val errBody = response.errorBody()?.string()?.trim().orEmpty()
                    withContext(Dispatchers.Main) {
                        onResult(false, com.example.util.ErrorMapper.mapHttp(response.code(), errBody))
                    }
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "cancelAccountDeletion exception", e)
                withContext(Dispatchers.Main) {
                    onResult(false, com.example.util.ErrorMapper.map(e))
                }
            }
        }
    }

    fun requestAccountDeletion(
        reason: String?,
        eraseLocalChats: Boolean,
        context: Context,
        onReauthRequired: () -> Unit,
        onSuccess: (DeletionReceiptData) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val fbUser = FirebaseAuth.getInstance().currentUser
                if (fbUser == null) {
                    withContext(Dispatchers.Main) { onReauthRequired() }
                    return@launch
                }

                // Force a fresh ID token (server requires sign-in within last 5 minutes)
                val tokenResult = try {
                    com.google.android.gms.tasks.Tasks.await(fbUser.getIdToken(true))
                } catch (e: Exception) {
                    Log.d("AuthViewModel", "getIdToken failed: ${e.message}")
                    withContext(Dispatchers.Main) { onReauthRequired() }
                    return@launch
                }

                val freshToken = tokenResult.token
                if (freshToken.isNullOrBlank()) {
                    withContext(Dispatchers.Main) { onReauthRequired() }
                    return@launch
                }

                val deviceId = com.example.util.DeviceInfoProvider.getDeviceId(context)
                val deviceName = com.example.util.DeviceInfoProvider.getDeviceName()

                // Persist eraseLocalChats choice & UID immediately
                val targetUid = ActiveUserManager.getActiveUid(context)
                    ?: SupabaseSessionManager.getUserId(context)
                    ?: fbUser.uid
                context.getSharedPreferences("account_deletion_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean("pending_erase_local_chats", eraseLocalChats)
                    .putString("pending_erase_uid", targetUid)
                    .commit()

                val req = RequestAccountDeletionRequest(
                    firebase_token = freshToken,
                    confirm = true,
                    reason = reason?.take(300),
                    device = DeviceDeletionInfo(device_id = deviceId, name = deviceName)
                )

                val response = SupabaseClient.api.requestAccountDeletion(
                    apiKey = BuildConfig.SUPABASE_ANON_KEY,
                    request = req
                )

                Log.d("AuthViewModel", "requestAccountDeletion HTTP status: ${response.code()}")

                if (response.isSuccessful) {
                    val body = response.body()
                    val receiptId = body?.receipt_id ?: "REC-${System.currentTimeMillis()}"
                    val scheduledFor = body?.scheduled_for.orEmpty()
                    val graceDays = body?.grace_days ?: 7

                    val receipt = DeletionReceiptData(
                        receiptId = receiptId,
                        scheduledFor = scheduledFor,
                        graceDays = graceDays,
                        eraseLocalChats = eraseLocalChats
                    )
                    _deletionReceipt.value = receipt

                    withContext(Dispatchers.Main) {
                        onSuccess(receipt)
                    }
                } else {
                    val rawErr = response.errorBody()?.string()?.trim().orEmpty()
                    Log.d("AuthViewModel", "requestAccountDeletion error status ${response.code()}: $rawErr")
                    val errCode = parseErrorJson(rawErr)

                    withContext(Dispatchers.Main) {
                        when {
                            response.code() == 401 && (errCode == "reauth_required" || errCode == "invalid_or_expired_token") -> {
                                onReauthRequired()
                            }
                            response.code() == 429 || errCode == "too_many_attempts" -> {
                                onError("Too many attempts, try again later")
                            }
                            response.code() == 404 -> {
                                onError("Account not found")
                            }
                            response.code() == 400 && errCode == "confirmation_required" -> {
                                onError("Confirmation required")
                            }
                            else -> {
                                onError(com.example.util.ErrorMapper.mapHttp(response.code(), rawErr))
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d("AuthViewModel", "requestAccountDeletion exception: ${e.javaClass.simpleName}")
                withContext(Dispatchers.Main) {
                    onError(com.example.util.ErrorMapper.map(e))
                }
            }
        }
    }

    fun finalizeDeletionAndSignOut(context: Context, onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val prefs = context.getSharedPreferences("account_deletion_prefs", Context.MODE_PRIVATE)
            val shouldErase = prefs.getBoolean("pending_erase_local_chats", false)
            val targetUid = prefs.getString("pending_erase_uid", null)
                ?: ActiveUserManager.getActiveUid(context)
                ?: SupabaseSessionManager.getUserId(context)

            // 1. Delete FCM token
            try {
                com.google.android.gms.tasks.Tasks.await(com.google.firebase.messaging.FirebaseMessaging.getInstance().deleteToken())
                Log.d("AuthViewModel", "FCM token deleted")
            } catch (e: Exception) {
                Log.w("AuthViewModel", "FCM token delete failed", e)
            }

            // 2. Conditional Per-UID Local Data & Key Wipe
            if (shouldErase && !targetUid.isNullOrBlank()) {
                wipeAllLocalDataForUid(context, targetUid)
            }

            // 3. Complete logout isolation (Note: DeviceInfoProvider uses 'device_identity_prefs' so deviceId is untouched!)
            com.example.service.PushNotificationManager.launchApplicationLogoutCleanup(
                context = context.applicationContext,
                effectiveUid = targetUid.orEmpty(),
                firebaseAuth = firebaseAuth
            )

            prefs.edit().clear().commit()

            withContext(Dispatchers.Main) {
                _deletionReceipt.value = null
                _deletionStatus.value = DeletionStatusState.Idle
                _supabaseProfiles.value = emptyList()
                _myModerationStatus.value = null
                _accountUnderReview.value = false
                _authState.value = AuthState.Idle
                onComplete()
            }
        }
    }

    fun runPendingEraseLocalChatsIfNeeded(context: Context) {
        val prefs = context.getSharedPreferences("account_deletion_prefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("pending_erase_local_chats", false)) {
            try {
                val targetUid = prefs.getString("pending_erase_uid", null)
                    ?: ActiveUserManager.getActiveUid(context)
                    ?: SupabaseSessionManager.getUserId(context)
                if (!targetUid.isNullOrBlank()) {
                    wipeAllLocalDataForUid(context, targetUid)
                }
            } catch (e: Exception) {
                Log.w("AuthViewModel", "Error executing pending local erase", e)
            } finally {
                prefs.edit().clear().commit()
            }
        }
    }

    private fun wipeAllLocalDataForUid(context: Context, uid: String) {
        try { com.example.crypto.VaultSecurityManager.wipeForUid(context, uid) } catch (e: Exception) {
            Log.w("AuthViewModel", "Vault wipe error for $uid", e)
        }
        try { com.example.crypto.E2EEManager.wipeForUid(context, uid) } catch (e: Exception) {
            Log.w("AuthViewModel", "E2EE wipe error for $uid", e)
        }
        try { com.example.ChatPreferencesManager.wipeForUid(context, uid) } catch (e: Exception) {
            Log.w("AuthViewModel", "Chat prefs wipe error for $uid", e)
        }
        try { com.example.util.ChatBackupPreferencesManager.wipeForUid(context, uid) } catch (e: Exception) {
            Log.w("AuthViewModel", "Backup prefs wipe error for $uid", e)
        }
        try { com.example.SupportChatManager.wipeForUid(context, uid) } catch (e: Exception) {
            Log.w("AuthViewModel", "Support prefs wipe error for $uid", e)
        }
        try { com.example.CloudAiManager.wipeForUid(context, uid) } catch (e: Exception) {
            Log.w("AuthViewModel", "Cloud AI prefs wipe error for $uid", e)
        }
    }

    private fun parseErrorJson(raw: String): String? {
        if (raw.isBlank()) return null
        return try {
            if (raw.startsWith("{")) {
                val j = org.json.JSONObject(raw)
                j.optString("error").ifBlank { j.optString("message").ifBlank { null } }
            } else null
        } catch (_: Exception) { null }
    }

    override fun onCleared() {
        endVoiceSession()
        super.onCleared()
    }
}

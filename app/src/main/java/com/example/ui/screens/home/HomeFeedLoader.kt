package com.example.ui.screens.home

import android.content.Context
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.BuildConfig
import com.example.SupabaseClient
import com.example.SupabaseEvent
import com.example.SupabaseFollower
import com.example.SupabaseRoomParticipant
import com.example.SupabaseSessionManager
import com.example.SupabaseVoiceRoom
import com.example.UserProfile
import com.example.data.AppCacheStore
import com.example.data.CachedFriendItem
import com.example.data.CachedRoomItem
import com.example.data.HomeFeedCachePayload
import com.example.realtime.VoiceRoomRealtimeManager
import com.example.ui.screens.LanguageData
import com.example.ui.screens.home.models.CustomRoom
import com.example.ui.screens.home.models.FollowedFriend
import com.example.ui.screens.home.models.decideSupabaseRoomTag
import com.example.ui.screens.home.models.getBadgeColor
import com.example.util.ErrorMapper
import com.example.util.MappedErrorException
import com.example.util.StartupTimingTracker
import com.example.util.withLoadingTimeout
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext

data class HomeFeedResult(
    val rooms: List<CustomRoom>,
    val friends: List<FollowedFriend>,
    val events: List<SupabaseEvent>
)

object HomeFeedLoader {
    private const val CACHE_KEY_HOME_FEED = "home_feed_v1"

    fun loadCached(context: Context): HomeFeedResult? {
        val cached = AppCacheStore.getSync<HomeFeedCachePayload>(
            context = context,
            key = CACHE_KEY_HOME_FEED,
            type = HomeFeedCachePayload::class.java
        ) ?: return null
        val payload = cached.data
        return HomeFeedResult(
            rooms = payload.rooms.mapIndexed { idx, item -> item.toCustomRoom(idx) },
            friends = payload.friends.map { it.toFollowedFriend() },
            events = payload.events
        )
    }

    suspend fun fetchFresh(
        context: Context,
        currentUserId: String,
        supabaseProfiles: List<UserProfile>
    ): Result<HomeFeedResult> = withContext(Dispatchers.IO) {
        try {
            val result = withLoadingTimeout {
                val authToken = SupabaseSessionManager.getBearerToken(context)

                // Run independent home startup requests in parallel
                val roomsDeferred = async {
                    StartupTimingTracker.measure("home_rooms") {
                        SupabaseClient.api.getVoiceRooms(auth = authToken)
                    }
                }
                val eventsDeferred = async {
                    StartupTimingTracker.measure("home_events") {
                        runCatching { SupabaseClient.api.getActiveEvents() }.getOrNull()
                    }
                }
                val followersDeferred = async {
                    StartupTimingTracker.measure("home_followers") {
                        runCatching {
                            if (currentUserId.isBlank() || currentUserId == "guest") null
                            else SupabaseClient.api.getFollowers(
                                auth = "Bearer ${BuildConfig.SUPABASE_ANON_KEY}",
                                followerIdEq = "eq.$currentUserId"
                            )
                        }.getOrNull()
                    }
                }

                val roomsRes = roomsDeferred.await()
                if (!roomsRes.isSuccessful) {
                    val finalRoomsRes = if (roomsRes.code() == 401) {
                        val refreshed = SupabaseSessionManager.forceRefreshSession(context)
                        if (!refreshed.isNullOrBlank()) {
                            SupabaseClient.api.getVoiceRooms(
                                auth = SupabaseSessionManager.getBearerToken(context)
                            )
                        } else roomsRes
                    } else roomsRes

                    if (!finalRoomsRes.isSuccessful) {
                        val raw = try { finalRoomsRes.errorBody()?.string() } catch (_: Exception) { null }
                        throw MappedErrorException(ErrorMapper.mapHttp(finalRoomsRes.code(), raw), finalRoomsRes.code())
                    }
                    return@withLoadingTimeout buildFeedResult(
                        context = context,
                        currentUserId = currentUserId,
                        supabaseProfiles = supabaseProfiles,
                        supaRooms = (finalRoomsRes.body() ?: emptyList()).filter { (it.is_live == true) && (it.status != "ended") },
                        events = eventsDeferred.await()?.takeIf { it.isSuccessful }?.body().orEmpty(),
                        followers = followersDeferred.await()?.takeIf { it.isSuccessful }?.body().orEmpty(),
                        authToken = SupabaseSessionManager.getBearerToken(context)
                    )
                }

                val supaRooms = (roomsRes.body() ?: emptyList()).filter {
                    (it.is_live == true) && (it.status != "ended")
                }
                val events = eventsDeferred.await()?.takeIf { it.isSuccessful }?.body().orEmpty()
                val followers = followersDeferred.await()?.takeIf { it.isSuccessful }?.body().orEmpty()

                buildFeedResult(
                    context = context,
                    currentUserId = currentUserId,
                    supabaseProfiles = supabaseProfiles,
                    supaRooms = supaRooms,
                    events = events,
                    followers = followers,
                    authToken = authToken
                )
            }

            // Persist to cache for instant startup next time
            AppCacheStore.put(
                context = context,
                key = CACHE_KEY_HOME_FEED,
                value = HomeFeedCachePayload(
                    rooms = result.rooms.map { it.toCacheItem() },
                    friends = result.friends.map { it.toCacheItem() },
                    events = result.events
                ),
                type = HomeFeedCachePayload::class.java
            )
            Result.success(result)
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            Result.failure(MappedErrorException(ErrorMapper.map(t)))
        }
    }

    private suspend fun buildFeedResult(
        context: Context,
        currentUserId: String,
        supabaseProfiles: List<UserProfile>,
        supaRooms: List<SupabaseVoiceRoom>,
        events: List<SupabaseEvent>,
        followers: List<SupabaseFollower>,
        authToken: String
    ): HomeFeedResult {
        val visibleRoomIds = supaRooms.mapNotNull { it.id?.takeIf { id -> id.isNotBlank() } }
        val supaParticipants: List<SupabaseRoomParticipant> = if (visibleRoomIds.isNotEmpty()) {
            StartupTimingTracker.measure("home_participants") {
                runCatching {
                    val roomIdFilter = "in.(${visibleRoomIds.joinToString(",")})"
                    val participantsRes = SupabaseClient.api.getAllRoomParticipants(
                        auth = authToken,
                        roomIdIn = roomIdFilter
                    )
                    participantsRes.body() ?: emptyList()
                }.getOrDefault(emptyList())
            }
        } else {
            emptyList()
        }

        val liveParticipants = VoiceRoomRealtimeManager.getAllLiveParticipants()
        val allParticipants = (supaParticipants + liveParticipants).distinctBy { "${it.room_id}_${it.user_id}" }

        val followedUserIds = followers.map { it.following_id }.toSet()
        val derivedFriends = if (supabaseProfiles.isNotEmpty()) {
            val followedProfiles = supabaseProfiles.filter {
                it.uid != currentUserId && followedUserIds.contains(it.uid)
            }
            val targetProfiles = if (followedProfiles.isNotEmpty()) {
                followedProfiles
            } else {
                supabaseProfiles.filter { it.uid != currentUserId }.take(10)
            }
            targetProfiles.map { p ->
                val pId = p.uid
                val activePart = allParticipants.firstOrNull { it.user_id == pId }
                val isOnline = activePart != null
                val isSpeaking = isOnline && (activePart.role == "speaker" || activePart.role == "host") && activePart.is_mic_enabled
                val statusText = when {
                    !isOnline -> "Offline"
                    activePart?.role == "speaker" || activePart?.role == "host" -> "Speaking"
                    else -> "Listening"
                }
                FollowedFriend(
                    id = pId,
                    name = p.displayName ?: p.username ?: "Learner",
                    avatarUrl = p.avatar ?: "",
                    flagCode = p.countryCode?.ifBlank { null } ?: "in",
                    isSpeaking = isSpeaking,
                    statusText = statusText,
                    currentRoomId = activePart?.room_id ?: "",
                    isOnline = isOnline
                )
            }
        } else {
            emptyList()
        }

        val mappedRooms = supaRooms.mapIndexed { index, r ->
            val rId = r.id ?: "room_$index"
            val roomParts = allParticipants.filter { it.room_id == rId }
            val realSpeaking = if (roomParts.isNotEmpty()) {
                roomParts.count { it.role == "speaker" || it.role == "host" }.coerceAtLeast(1)
            } else {
                r.speaking_count ?: 1
            }
            val realListening = if (roomParts.isNotEmpty()) {
                roomParts.count { it.role == "listener" || (it.role != "speaker" && it.role != "host") }
            } else {
                r.listening_count ?: 0
            }
            val decidedTag = decideSupabaseRoomTag(
                room = r,
                allRooms = supaRooms,
                realSpeakingCount = realSpeaking,
                realListeningCount = realListening
            )
            val rawAvatars = r.preview_avatars.orEmpty()
            val roomAvatars = if (rawAvatars.isNotEmpty()) {
                rawAvatars
            } else {
                supabaseProfiles.take(3).mapNotNull { it.avatar?.takeIf { a -> a.isNotBlank() } }
            }
            val hostProfile = supabaseProfiles.find { it.uid == r.host_id }
            val hostNatives = listOfNotNull(hostProfile?.nativeLanguage?.takeIf { it.isNotBlank() })
            val hostLearnings = (
                listOfNotNull(hostProfile?.learningLanguage?.takeIf { it.isNotBlank() }) +
                    (hostProfile?.selectedLanguages ?: emptyList())
                ).distinct()

            CachedRoomItem(
                id = rId,
                title = r.title ?: "Voice Circle",
                subtitle = r.description ?: "Practice and speak together",
                language = r.language ?: "English",
                flagCode = LanguageData.resolveFlagCode(
                    languageName = r.language,
                    storedCode = r.language_code.orEmpty().ifBlank { r.country_code.orEmpty() }
                ),
                speakingCount = realSpeaking,
                listeningCount = realListening,
                tag = r.level_tag.orEmpty().ifBlank { r.tag.orEmpty() }.ifBlank { "All Levels" },
                imageUrl = r.banner_image_url.orEmpty().ifBlank { "drawable://img_anime_banner_sakura_station_1794610601752" },
                badgeText = decidedTag,
                avatars = roomAvatars,
                hostId = r.host_id ?: "",
                hostNativeLanguages = hostNatives,
                hostLearningLanguages = hostLearnings
            ).toCustomRoom(index)
        }

        return HomeFeedResult(
            rooms = mappedRooms,
            friends = derivedFriends,
            events = events
        )
    }

    private fun CachedRoomItem.toCustomRoom(index: Int): CustomRoom {
        val overlayVal = when (index % 4) {
            0 -> Color(0x22FCD34D)
            1 -> Color(0x22D8B4FE)
            2 -> Color(0x22FDA4AF)
            else -> Color(0x223B82F6)
        }
        return CustomRoom(
            id = id,
            title = title,
            subtitle = subtitle,
            language = language,
            flagCode = flagCode,
            speakingCount = speakingCount,
            listeningCount = listeningCount,
            tag = tag,
            bgGradient = Brush.linearGradient(listOf(Color(0xFFFFFBEB), Color(0xFFFEF3C7))),
            imageUrl = imageUrl,
            overlayColor = overlayVal,
            badgeText = badgeText,
            badgeColor = getBadgeColor(badgeText),
            avatars = avatars,
            hostId = hostId,
            hostNativeLanguages = hostNativeLanguages,
            hostLearningLanguages = hostLearningLanguages
        )
    }

    private fun CustomRoom.toCacheItem(): CachedRoomItem = CachedRoomItem(
        id = id,
        title = title,
        subtitle = subtitle,
        language = language,
        flagCode = flagCode,
        speakingCount = speakingCount,
        listeningCount = listeningCount,
        tag = tag,
        imageUrl = imageUrl,
        badgeText = badgeText,
        avatars = avatars,
        hostId = hostId,
        hostNativeLanguages = hostNativeLanguages,
        hostLearningLanguages = hostLearningLanguages
    )

    private fun CachedFriendItem.toFollowedFriend(): FollowedFriend = FollowedFriend(
        id = id,
        name = name,
        avatarUrl = avatarUrl,
        flagCode = flagCode,
        isSpeaking = isSpeaking,
        statusText = statusText,
        currentRoomId = currentRoomId,
        isOnline = isOnline
    )

    private fun FollowedFriend.toCacheItem(): CachedFriendItem = CachedFriendItem(
        id = id,
        name = name,
        avatarUrl = avatarUrl,
        flagCode = flagCode,
        isSpeaking = isSpeaking,
        statusText = statusText,
        currentRoomId = currentRoomId,
        isOnline = isOnline
    )
}

package com.example.leaderboard

import android.content.Context
import com.example.CurrentDeviceRequest
import com.example.DeviceIdentity
import com.example.DiscoverableProfile
import com.example.SupabaseClient
import com.example.SupabaseFollower
import com.example.SupabaseSessionManager
import com.example.SupabaseVoiceRoom
import com.example.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

enum class LeaderboardCategory(val label: String) {
    VOICE_TIME("Voice Rooms"),
    POPULAR("Popular"),
    MOST_LOGINS("Most Logins")
}

data class LeaderboardEntry(
    val rank: Int,
    val userId: String,
    val displayName: String,
    val username: String,
    val avatarUrl: String,
    val coverUrl: String = "",
    val countryCode: String = "",
    val bio: String = "",
    val nativeLanguages: List<String> = emptyList(),
    val learningLanguages: List<String> = emptyList(),
    val hobbies: List<String> = emptyList(),
    val isVerified: Boolean = false,
    val isOnline: Boolean = false,
    val isLiveNow: Boolean = false,
    val roomTitle: String? = null,
    val roomStartedAtMs: Long? = null,
    val scoreLabel: String,
    val subtitle: String,
    val sortValue: Long
)

data class LiveLeaderboardSnapshot(
    val voiceRoomEntries: List<LeaderboardEntry>,
    val popularEntries: List<LeaderboardEntry>,
    val mostLoginsEntries: List<LeaderboardEntry>,
    val fetchedAtMs: Long
)

object LeaderboardRepository {

    private const val PREFS_NAME = "funky_talk_prefs"
    private const val KEY_LOGIN_COUNT_PREFIX = "app_login_count_"
    private const val KEY_SESSION_RECORDED_PREFIX = "app_login_session_ts_"

    fun recordLocalAppLogin(context: Context, userId: String?) {
        val uid = userId?.takeIf { it.isNotBlank() && it != "guest" } ?: return
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val lastRecorded = prefs.getLong("$KEY_SESSION_RECORDED_PREFIX$uid", 0L)
        if (now - lastRecorded > 60_000L) {
            val current = prefs.getInt("$KEY_LOGIN_COUNT_PREFIX$uid", 0)
            prefs.edit()
                .putInt("$KEY_LOGIN_COUNT_PREFIX$uid", current + 1)
                .putLong("$KEY_SESSION_RECORDED_PREFIX$uid", now)
                .apply()
        }
    }

    fun getLocalAppLoginCount(context: Context, userId: String?): Int {
        val uid = userId?.takeIf { it.isNotBlank() && it != "guest" } ?: return 0
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt("$KEY_LOGIN_COUNT_PREFIX$uid", 1)
    }

    suspend fun fetchLiveLeaderboard(
        context: Context,
        cachedProfiles: List<UserProfile> = emptyList()
    ): Result<LiveLeaderboardSnapshot> = withContext(Dispatchers.IO) {
        runCatching {
            val token = SupabaseSessionManager.getBearerToken(context)
            val selfUserId = SupabaseSessionManager.getUserId(context).orEmpty()
            val nowMs = System.currentTimeMillis()

            val (rooms, discoverProfiles, followers, myDeviceCount, rpcLoginCounts) = coroutineScope {
                val roomsDeferred = async {
                    runCatching {
                        val resp = SupabaseClient.api.getVoiceRooms(auth = token)
                        if (resp.isSuccessful) resp.body().orEmpty() else emptyList()
                    }.getOrDefault(emptyList())
                }
                val profilesDeferred = async {
                    runCatching {
                        val resp = SupabaseClient.api.getDiscoverableProfiles(
                            auth = token,
                            options = mapOf("limit" to "120")
                        )
                        if (resp.isSuccessful) resp.body().orEmpty() else emptyList()
                    }.getOrDefault(emptyList())
                }
                val followersDeferred = async {
                    runCatching {
                        val resp = SupabaseClient.api.getFollowers(auth = token)
                        if (resp.isSuccessful) resp.body().orEmpty() else emptyList()
                    }.getOrDefault(emptyList())
                }
                val devicesDeferred = async {
                    if (selfUserId.isBlank()) 0 else runCatching {
                        val resp = SupabaseClient.api.getMyDevicesRpc(
                            auth = token,
                            body = CurrentDeviceRequest(DeviceIdentity.getDeviceId(context))
                        )
                        if (resp.isSuccessful) {
                            val raw = resp.body()?.string().orEmpty()
                            if (raw.trim().startsWith("[")) JSONArray(raw).length() else 0
                        } else 0
                    }.getOrDefault(0)
                }
                val rpcLoginsDeferred = async {
                    runCatching {
                        val resp = SupabaseClient.api.getLeaderboardLoginsRpc(auth = token)
                        if (resp.isSuccessful) {
                            val raw = resp.body()?.string().orEmpty()
                            parseRpcLoginCounts(raw)
                        } else emptyMap()
                    }.getOrDefault(emptyMap())
                }

                Tuple5(
                    roomsDeferred.await(),
                    profilesDeferred.await(),
                    followersDeferred.await(),
                    devicesDeferred.await(),
                    rpcLoginsDeferred.await()
                )
            }

            val profileById = HashMap<String, DiscoverableProfile>()
            discoverProfiles.forEach { p ->
                if (p.auth_user_id.isNotBlank()) profileById[p.auth_user_id] = p
            }
            // Merge fallback user info from AuthViewModel cached profiles if any ID wasn't in discoverable
            cachedProfiles.forEach { u ->
                if (u.uid.isNotBlank() && !profileById.containsKey(u.uid)) {
                    profileById[u.uid] = DiscoverableProfile(
                        auth_user_id = u.uid,
                        username = u.username,
                        profile_name = u.displayName,
                        avatar_url = u.avatar,
                        cover_url = u.coverUrl,
                        native_languages = u.nativeLanguage?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() },
                        learning_languages = u.learningLanguage?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() },
                        hobbies = u.hobbies,
                        bio = u.bio,
                        country_code = u.countryCode
                    )
                }
            }

            val voiceEntries = buildVoiceRoomEntries(
                rooms = rooms,
                profileById = profileById,
                nowMs = nowMs
            )

            val popularEntries = buildPopularEntries(
                followers = followers,
                profiles = profileById.values.toList()
            )

            val mostLoginsEntries = buildMostLoginsEntries(
                context = context,
                selfUserId = selfUserId,
                myDeviceCount = myDeviceCount,
                rpcLoginCounts = rpcLoginCounts,
                profiles = profileById.values.toList(),
                nowMs = nowMs
            )

            LiveLeaderboardSnapshot(
                voiceRoomEntries = voiceEntries,
                popularEntries = popularEntries,
                mostLoginsEntries = mostLoginsEntries,
                fetchedAtMs = nowMs
            )
        }
    }

    private fun buildVoiceRoomEntries(
        rooms: List<SupabaseVoiceRoom>,
        profileById: Map<String, DiscoverableProfile>,
        nowMs: Long
    ): List<LeaderboardEntry> {
        // Strictly LIVE voice rooms open right now, ranked longest open duration first
        val liveRooms = rooms.filter { it.is_live != false && it.status != "ended" }
        val sortedByLongest = liveRooms
            .mapNotNull { room ->
                val hostId = room.host_id?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val startMs = parseIsoTimestampMs(room.created_at) ?: nowMs
                val elapsedSec = ((nowMs - startMs) / 1000L).coerceAtLeast(1L)
                Triple(room, hostId, startMs to elapsedSec)
            }
            .groupBy { it.second }
            .values
            .mapNotNull { hostRooms -> hostRooms.maxByOrNull { it.third.second } }
            .sortedByDescending { it.third.second }

        return sortedByLongest.mapIndexed { idx, (room, hostId, timing) ->
            val (startMs, elapsedSec) = timing
            val prof = profileById[hostId]
            val name = prof?.profile_name?.takeIf { it.isNotBlank() }
                ?: prof?.username?.takeIf { it.isNotBlank() }
                ?: "Host"
            val avatar = prof?.avatar_url?.takeIf { it.isNotBlank() }
                ?: room.preview_avatars?.firstOrNull().orEmpty()
            val roomTitle = room.title?.takeIf { it.isNotBlank() } ?: "Live Voice Room"
            val listeners = (room.listening_count ?: 0) + (room.speaking_count ?: 1)

            LeaderboardEntry(
                rank = idx + 1,
                userId = hostId,
                displayName = name,
                username = prof?.username.orEmpty(),
                avatarUrl = avatar,
                coverUrl = prof?.cover_url.orEmpty(),
                countryCode = prof?.country_code ?: room.country_code.orEmpty(),
                bio = prof?.bio.orEmpty(),
                nativeLanguages = prof?.native_languages.orEmpty(),
                learningLanguages = prof?.learning_languages.orEmpty(),
                hobbies = prof?.hobbies.orEmpty(),
                isVerified = prof?.is_verified == true || room.host_is_verified == true,
                isOnline = true,
                isLiveNow = true,
                roomTitle = roomTitle,
                roomStartedAtMs = startMs,
                scoreLabel = formatLiveDuration(elapsedSec),
                subtitle = "$roomTitle · $listeners live",
                sortValue = elapsedSec
            )
        }
    }

    private fun buildPopularEntries(
        followers: List<SupabaseFollower>,
        profiles: List<DiscoverableProfile>
    ): List<LeaderboardEntry> {
        val followerCounts = HashMap<String, Int>()
        followers.forEach { rel ->
            if (rel.following_id.isNotBlank()) {
                followerCounts[rel.following_id] = (followerCounts[rel.following_id] ?: 0) + 1
            }
        }

        val ranked = profiles
            .filter { it.auth_user_id.isNotBlank() }
            .sortedWith(
                compareByDescending<DiscoverableProfile> { followerCounts[it.auth_user_id] ?: 0 }
                    .thenByDescending { it.is_online == true }
                    .thenByDescending { it.is_verified == true }
                    .thenByDescending { parseIsoTimestampMs(it.last_seen) ?: 0L }
            )

        return ranked.mapIndexed { idx, prof ->
            val count = followerCounts[prof.auth_user_id] ?: 0
            val name = prof.profile_name?.takeIf { it.isNotBlank() }
                ?: prof.username?.takeIf { it.isNotBlank() }
                ?: "Speaker"
            val handle = prof.username?.takeIf { it.isNotBlank() }?.let { "@$it" }
                ?: if (prof.is_online == true) "Online now" else "Member"

            LeaderboardEntry(
                rank = idx + 1,
                userId = prof.auth_user_id,
                displayName = name,
                username = prof.username.orEmpty(),
                avatarUrl = prof.avatar_url.orEmpty(),
                coverUrl = prof.cover_url.orEmpty(),
                countryCode = prof.country_code.orEmpty(),
                bio = prof.bio.orEmpty(),
                nativeLanguages = prof.native_languages.orEmpty(),
                learningLanguages = prof.learning_languages.orEmpty(),
                hobbies = prof.hobbies.orEmpty(),
                isVerified = prof.is_verified == true,
                isOnline = prof.is_online == true,
                isLiveNow = false,
                scoreLabel = "$count",
                subtitle = if (count == 1) "$handle · 1 follower" else "$handle · $count followers",
                sortValue = count.toLong()
            )
        }
    }

    private fun buildMostLoginsEntries(
        context: Context,
        selfUserId: String,
        myDeviceCount: Int,
        rpcLoginCounts: Map<String, Int>,
        profiles: List<DiscoverableProfile>,
        nowMs: Long
    ): List<LeaderboardEntry> {
        val localSelfLogins = getLocalAppLoginCount(context, selfUserId).coerceAtLeast(myDeviceCount)

        data class LoginCandidate(
            val profile: DiscoverableProfile,
            val explicitLogins: Int?,
            val isOnline: Boolean,
            val lastSeenMs: Long
        )

        val candidates = profiles
            .filter { it.auth_user_id.isNotBlank() }
            .map { prof ->
                val explicit = rpcLoginCounts[prof.auth_user_id]
                    ?: if (prof.auth_user_id == selfUserId && localSelfLogins > 0) localSelfLogins else null
                val seenMs = parseIsoTimestampMs(prof.last_seen) ?: parseIsoTimestampMs(prof.created_at) ?: 0L
                LoginCandidate(
                    profile = prof,
                    explicitLogins = explicit,
                    isOnline = prof.is_online == true,
                    lastSeenMs = seenMs
                )
            }
            .sortedWith(
                compareByDescending<LoginCandidate> { it.explicitLogins ?: 0 }
                    .thenByDescending { it.isOnline }
                    .thenByDescending { it.lastSeenMs }
            )

        return candidates.mapIndexed { idx, item ->
            val prof = item.profile
            val name = prof.profile_name?.takeIf { it.isNotBlank() }
                ?: prof.username?.takeIf { it.isNotBlank() }
                ?: "Member"
            val scoreLabel = when {
                item.explicitLogins != null -> "${item.explicitLogins} logins"
                item.isOnline -> "Active"
                item.lastSeenMs > 0L -> formatShortAgo(nowMs - item.lastSeenMs)
                else -> "Member"
            }
            val sub = when {
                item.isOnline && item.explicitLogins != null -> "Online now · ${item.explicitLogins} logins"
                item.isOnline -> "Active session now"
                item.lastSeenMs > 0L -> "Last active ${formatShortAgo(nowMs - item.lastSeenMs)}"
                else -> prof.username?.takeIf { it.isNotBlank() }?.let { "@$it" } ?: "FunkyTalk user"
            }

            LeaderboardEntry(
                rank = idx + 1,
                userId = prof.auth_user_id,
                displayName = name,
                username = prof.username.orEmpty(),
                avatarUrl = prof.avatar_url.orEmpty(),
                coverUrl = prof.cover_url.orEmpty(),
                countryCode = prof.country_code.orEmpty(),
                bio = prof.bio.orEmpty(),
                nativeLanguages = prof.native_languages.orEmpty(),
                learningLanguages = prof.learning_languages.orEmpty(),
                hobbies = prof.hobbies.orEmpty(),
                isVerified = prof.is_verified == true,
                isOnline = item.isOnline,
                isLiveNow = item.isOnline,
                scoreLabel = scoreLabel,
                subtitle = sub,
                sortValue = (item.explicitLogins?.toLong() ?: 0L) * 1_000_000_000L + item.lastSeenMs
            )
        }
    }

    private fun parseRpcLoginCounts(rawJson: String): Map<String, Int> {
        val trimmed = rawJson.trim()
        if (!trimmed.startsWith("[")) return emptyMap()
        val arr = JSONArray(trimmed)
        val map = HashMap<String, Int>(arr.length())
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val uid = obj.optString("user_id").ifBlank { obj.optString("auth_user_id") }
            val count = obj.optInt("login_count", -1).takeIf { it >= 0 }
                ?: obj.optInt("logins", -1).takeIf { it >= 0 }
                ?: continue
            if (uid.isNotBlank()) map[uid] = count
        }
        return map
    }

    fun formatLiveDuration(totalSeconds: Long): String {
        val sec = totalSeconds.coerceAtLeast(0L)
        val hours = sec / 3600L
        val mins = (sec % 3600L) / 60L
        val remSec = sec % 60L
        return when {
            hours > 0L -> "${hours}h ${mins}m"
            mins > 0L -> "${mins}m ${remSec}s"
            else -> "${remSec}s"
        }
    }

    private fun formatShortAgo(diffMs: Long): String {
        val mins = (diffMs / 60_000L).coerceAtLeast(1L)
        val hours = mins / 60L
        val days = hours / 24L
        return when {
            mins < 60L -> "${mins}m ago"
            hours < 24L -> "${hours}h ago"
            else -> "${days}d ago"
        }
    }

    fun parseIsoTimestampMs(raw: String?): Long? {
        if (raw.isNullOrBlank()) return null
        val normalized = raw.trim()
            .replace(Regex("\\.\\d+"), "")
            .replace("Z", "+0000")
            .replace(Regex("([+-]\\d{2}):(\\d{2})$"), "$1$2")
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ssZ",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd HH:mm:ssZ",
            "yyyy-MM-dd HH:mm:ss"
        )
        for (pattern in patterns) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                val parsed = sdf.parse(normalized)
                if (parsed != null) return parsed.time
            } catch (_: Throwable) {
            }
        }
        return null
    }

    private data class Tuple5<A, B, C, D, E>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D,
        val fifth: E
    )
}

package com.example.ui.screens.home.tabs

import android.content.Context
import com.example.BadgeManager
import com.example.BuildConfig
import com.example.GiftTransactionItem
import com.example.MomentDto
import com.example.ProfileBadge
import com.example.SupabaseClient
import com.example.SupabaseSessionManager
import com.example.UserProfile
import com.example.data.AppCacheStore
import com.example.data.SelfProfileCachePayload
import com.example.gifting.GiftManager
import com.example.repository.MomentsRepository
import com.example.ui.screens.FeedPost
import com.example.ui.screens.formatJoinedDate
import com.example.ui.screens.toFeedPost
import com.example.util.ErrorMapper
import com.example.util.MappedErrorException
import com.example.util.StartupTimingTracker
import com.example.util.withLoadingTimeout
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

data class SelfProfileFetchResult(
    val payload: SelfProfileCachePayload,
    val posts: List<FeedPost>
)

object SelfProfileLoader {
    private const val CACHE_KEY_SELF_PROFILE = "self_profile_v1"

    fun loadCached(context: Context): SelfProfileFetchResult? {
        val cached = AppCacheStore.getSync<SelfProfileCachePayload>(
            context = context,
            key = CACHE_KEY_SELF_PROFILE,
            type = SelfProfileCachePayload::class.java
        ) ?: return null
        val payload = cached.data
        return SelfProfileFetchResult(
            payload = payload,
            posts = payload.moments.map { it.toFeedPost() }
        )
    }

    suspend fun fetchParallel(
        context: Context,
        userId: String,
        fallbackProfile: UserProfile,
        existingPayload: SelfProfileCachePayload?
    ): Result<SelfProfileFetchResult> = withContext(Dispatchers.IO) {
        try {
            val result = withLoadingTimeout {
                coroutineScope {
                    val supaUid = SupabaseSessionManager.getUserId(context) ?: userId
                    val effectiveUid = supaUid.takeIf { SupabaseSessionManager.isValidUuid(it) }
                        ?: (if (SupabaseSessionManager.isValidUuid(userId)) userId else supaUid)

                    val profileDeferred = async {
                        StartupTimingTracker.measure("profile_get_profile") {
                            ErrorMapper.executeWithSingle401Retry(context) { auth ->
                                SupabaseClient.api.getProfile(
                                    apiKey = BuildConfig.SUPABASE_ANON_KEY,
                                    auth = auth,
                                    authUserIdEq = "eq.$effectiveUid"
                                )
                            }
                        }
                    }

                    val discoverableDeferred = async {
                        StartupTimingTracker.measure("profile_discoverable") {
                            ErrorMapper.executeWithSingle401Retry(context) { auth ->
                                SupabaseClient.api.getDiscoverableProfiles(
                                    apiKey = BuildConfig.SUPABASE_ANON_KEY,
                                    auth = auth,
                                    options = mapOf("auth_user_id" to "eq.$effectiveUid")
                                )
                            }
                        }
                    }

                    val badgesDeferred = async {
                        StartupTimingTracker.measure("profile_badges") {
                            runCatching { BadgeManager.getBadgesForUser(effectiveUid) }.getOrNull()
                        }
                    }

                    val giftsDeferred = async {
                        StartupTimingTracker.measure("profile_gifts") {
                            runCatching { GiftManager.fetchGiftsReceived(context, effectiveUid) }.getOrNull()
                        }
                    }

                    val followersDeferred = async {
                        StartupTimingTracker.measure("profile_followers_count") {
                            ErrorMapper.executeWithSingle401Retry(context) { auth ->
                                SupabaseClient.api.countFollowers(
                                    apiKey = BuildConfig.SUPABASE_ANON_KEY,
                                    auth = auth,
                                    followingIdEq = "eq.$effectiveUid"
                                )
                            }
                        }
                    }

                    val followingDeferred = async {
                        StartupTimingTracker.measure("profile_following_count") {
                            ErrorMapper.executeWithSingle401Retry(context) { auth ->
                                SupabaseClient.api.countFollowing(
                                    apiKey = BuildConfig.SUPABASE_ANON_KEY,
                                    auth = auth,
                                    followerIdEq = "eq.$effectiveUid"
                                )
                            }
                        }
                    }

                    val momentsDeferred = async {
                        StartupTimingTracker.measure("profile_user_moments") {
                            runCatching {
                                MomentsRepository.getUserMoments(context, userId = null, limit = 20, offset = 0)
                            }.getOrNull()
                        }
                    }

                    val profileRes = profileDeferred.await()
                    val discoverableRes = discoverableDeferred.await()

                    val primaryRow = profileRes.getOrNull()?.body()?.firstOrNull()
                    val discRow = discoverableRes.getOrNull()?.body()?.firstOrNull()

                    if (primaryRow == null && discRow == null && existingPayload == null) {
                        val failure = profileRes.exceptionOrNull() ?: discoverableRes.exceptionOrNull()
                        throw MappedErrorException(ErrorMapper.map(failure))
                    }

                    val prefs = context.getSharedPreferences("funky_talk_prefs", Context.MODE_PRIVATE)
                    if (primaryRow != null) {
                        val prefsEditor = prefs.edit()
                        primaryRow.avatar_url?.let {
                            prefsEditor.putString("profile_avatar_$userId", it)
                            if (supaUid != userId) prefsEditor.putString("profile_avatar_$supaUid", it)
                        } ?: run {
                            prefsEditor.remove("profile_avatar_$userId")
                            if (supaUid != userId) prefsEditor.remove("profile_avatar_$supaUid")
                        }
                        primaryRow.cover_url?.let {
                            prefsEditor.putString("profile_cover_$userId", it)
                            if (supaUid != userId) prefsEditor.putString("profile_cover_$supaUid", it)
                        } ?: run {
                            prefsEditor.remove("profile_cover_$userId")
                            if (supaUid != userId) prefsEditor.remove("profile_cover_$supaUid")
                        }
                        prefsEditor.apply()
                    }

                    val badges: List<ProfileBadge> = badgesDeferred.await() ?: existingPayload?.badges.orEmpty()
                    val gifts: List<GiftTransactionItem> = giftsDeferred.await() ?: existingPayload?.gifts.orEmpty()
                    val moments: List<MomentDto> = momentsDeferred.await()?.getOrNull() ?: existingPayload?.moments.orEmpty()

                    val followersRes = followersDeferred.await().getOrNull()
                    val followersCount = if (followersRes != null && followersRes.isSuccessful) {
                        val cr = followersRes.headers()["Content-Range"]
                        cr?.substringAfter("/")?.toIntOrNull() ?: followersRes.body()?.size ?: 0
                    } else {
                        existingPayload?.followersCount ?: 0
                    }

                    val followingRes = followingDeferred.await().getOrNull()
                    val followingCount = if (followingRes != null && followingRes.isSuccessful) {
                        val cr = followingRes.headers()["Content-Range"]
                        cr?.substringAfter("/")?.toIntOrNull() ?: followingRes.body()?.size ?: 0
                    } else {
                        existingPayload?.followingCount ?: 0
                    }

                    val resolvedCountry = primaryRow?.country?.takeIf { it.isNotBlank() }
                        ?: discRow?.country?.takeIf { it.isNotBlank() }
                        ?: existingPayload?.country?.takeIf { it.isNotBlank() }
                        ?: fallbackProfile.country.orEmpty()

                    val resolvedCountryCode = resolveCountryCode(
                        resolvedCountry,
                        primaryRow?.country_code ?: discRow?.country_code ?: existingPayload?.countryCode ?: fallbackProfile.countryCode
                    )

                    val resolvedName = primaryRow?.profile_name?.takeIf { it.isNotBlank() }
                        ?: discRow?.profile_name?.takeIf { it.isNotBlank() }
                        ?: primaryRow?.username?.takeIf { it.isNotBlank() }
                        ?: discRow?.username?.takeIf { it.isNotBlank() }
                        ?: existingPayload?.displayName?.takeIf { it.isNotBlank() }
                        ?: fallbackProfile.displayName?.takeIf { it.isNotBlank() }
                        ?: fallbackProfile.username?.takeIf { it.isNotBlank() }
                        ?: "FunkyTalk User"

                    val resolvedUsername = primaryRow?.username?.takeIf { it.isNotBlank() }
                        ?: discRow?.username?.takeIf { it.isNotBlank() }
                        ?: existingPayload?.username?.takeIf { it.isNotBlank() }
                        ?: fallbackProfile.username?.takeIf { it.isNotBlank() }
                        ?: resolvedName.lowercase().replace(" ", "")

                    val resolvedBio = primaryRow?.bio?.takeIf { it.isNotBlank() }
                        ?: discRow?.bio?.takeIf { it.isNotBlank() }
                        ?: existingPayload?.bio?.takeIf { it.isNotBlank() }
                        ?: fallbackProfile.bio.orEmpty()

                    val payload = SelfProfileCachePayload(
                        userId = effectiveUid,
                        displayName = resolvedName,
                        username = resolvedUsername,
                        avatarUrl = primaryRow?.avatar_url ?: discRow?.avatar_url ?: existingPayload?.avatarUrl ?: fallbackProfile.avatar.orEmpty(),
                        coverUrl = primaryRow?.cover_url ?: discRow?.cover_url ?: existingPayload?.coverUrl ?: fallbackProfile.coverUrl.orEmpty(),
                        bio = resolvedBio,
                        country = resolvedCountry,
                        countryCode = resolvedCountryCode,
                        city = primaryRow?.city?.takeIf { it.isNotBlank() }
                            ?: discRow?.city?.takeIf { it.isNotBlank() }
                            ?: existingPayload?.city
                            ?: fallbackProfile.city.orEmpty(),
                        age = primaryRow?.age?.takeIf { it > 0 }
                            ?: discRow?.age?.takeIf { it > 0 }
                            ?: existingPayload?.age
                            ?: fallbackProfile.age
                            ?: 0,
                        gender = primaryRow?.gender?.takeIf { it.isNotBlank() }
                            ?: discRow?.gender?.takeIf { it.isNotBlank() }
                            ?: existingPayload?.gender
                            ?: fallbackProfile.gender.orEmpty(),
                        nativeLanguage = primaryRow?.native_languages?.takeIf { it.isNotEmpty() }?.joinToString(", ")
                            ?: existingPayload?.nativeLanguage?.takeIf { it.isNotBlank() }
                            ?: fallbackProfile.nativeLanguage.orEmpty(),
                        learningLanguage = primaryRow?.learning_languages?.takeIf { it.isNotEmpty() }?.joinToString(", ")
                            ?: existingPayload?.learningLanguage?.takeIf { it.isNotBlank() }
                            ?: fallbackProfile.learningLanguage.orEmpty(),
                        isVerified = discRow?.is_verified ?: existingPayload?.isVerified ?: fallbackProfile.isEmailVerified,
                        isOnline = discRow?.is_online ?: existingPayload?.isOnline ?: true,
                        joinedText = discRow?.created_at?.let { formatJoinedDate(it) }
                            ?: existingPayload?.joinedText
                            ?: "Joined recently",
                        postsCount = moments.size,
                        followersCount = followersCount,
                        followingCount = followingCount,
                        badges = badges,
                        gifts = gifts,
                        moments = moments
                    )

                    AppCacheStore.put(
                        context = context,
                        key = CACHE_KEY_SELF_PROFILE,
                        value = payload,
                        type = SelfProfileCachePayload::class.java
                    )

                    SelfProfileFetchResult(
                        payload = payload,
                        posts = moments.map { it.toFeedPost() }
                    )
                }
            }
            Result.success(result)
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            Result.failure(MappedErrorException(ErrorMapper.map(t)))
        }
    }
}

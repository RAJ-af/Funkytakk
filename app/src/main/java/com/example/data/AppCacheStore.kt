package com.example.data

import android.content.Context
import com.example.DiscoverableProfile
import com.example.GiftTransactionItem
import com.example.MomentDto
import com.example.ProfileBadge
import com.example.SupabaseEvent
import com.example.SupabaseSessionManager
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap

@JsonClass(generateAdapter = true)
data class CachedEnvelope<T>(
    val savedAtMs: Long,
    val data: T
)

@JsonClass(generateAdapter = true)
data class CachedRoomItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val language: String,
    val flagCode: String,
    val speakingCount: Int,
    val listeningCount: Int,
    val tag: String,
    val imageUrl: String,
    val badgeText: String,
    val avatars: List<String>,
    val hostId: String = "",
    val hostNativeLanguages: List<String> = emptyList(),
    val hostLearningLanguages: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class CachedFriendItem(
    val id: String,
    val name: String,
    val avatarUrl: String,
    val flagCode: String,
    val isSpeaking: Boolean,
    val statusText: String,
    val currentRoomId: String? = null,
    val isOnline: Boolean = true
)

@JsonClass(generateAdapter = true)
data class HomeFeedCachePayload(
    val rooms: List<CachedRoomItem> = emptyList(),
    val friends: List<CachedFriendItem> = emptyList(),
    val events: List<SupabaseEvent> = emptyList()
)

@JsonClass(generateAdapter = true)
data class CachedDiscoverPartnerItem(
    val id: String,
    val name: String,
    val username: String = "",
    val age: Int,
    val isOnline: Boolean,
    val avatarUrl: String,
    val coverUrl: String? = null,
    val countryCode: String,
    val nativeLang: String,
    val learningLang: String,
    val bio: String,
    val interests: List<String> = emptyList(),
    val vibe: String = "Chill",
    val isWaved: Boolean = false,
    val nativeLangFull: String = "",
    val learningLangFull: String = "",
    val city: String = "",
    val country: String = ""
)

@JsonClass(generateAdapter = true)
data class SelfProfileCachePayload(
    val userId: String,
    val displayName: String,
    val username: String,
    val avatarUrl: String,
    val coverUrl: String,
    val bio: String,
    val country: String,
    val countryCode: String,
    val city: String,
    val age: Int,
    val gender: String,
    val nativeLanguage: String,
    val learningLanguage: String,
    val isVerified: Boolean,
    val isOnline: Boolean,
    val joinedText: String,
    val postsCount: Int,
    val followersCount: Int,
    val followingCount: Int,
    val badges: List<ProfileBadge> = emptyList(),
    val gifts: List<GiftTransactionItem> = emptyList(),
    val moments: List<MomentDto> = emptyList()
)

data class CacheLookupResult<T>(
    val data: T,
    val savedAtMs: Long,
    val isFresh: Boolean
)

/**
 * Stale-while-revalidate memory + disk JSON cache scoped by authenticated UID.
 * Cleared cleanly on logout to enforce multi-account privacy isolation.
 */
object AppCacheStore {
    const val DEFAULT_FRESH_TTL_MS = 5 * 60_000L
    private const val CACHE_DIR_NAME = "app_swr_cache"

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val memoryCache = ConcurrentHashMap<String, CacheEntry>()

    private data class CacheEntry(
        val savedAtMs: Long,
        val value: Any
    )

    private fun resolveOwnerKey(context: Context): String {
        val uid = SupabaseSessionManager.getUserId(context)?.takeIf { it.isNotBlank() } ?: "anon"
        return uid.replace(Regex("[^a-zA-Z0-9_-]"), "_")
    }

    private fun cacheFileFor(context: Context, ownerKey: String, cacheKey: String): File {
        val safeName = cacheKey.replace(Regex("[^a-zA-Z0-9_-]"), "_") + ".json"
        val dir = File(context.applicationContext.filesDir, "$CACHE_DIR_NAME/$ownerKey")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return File(dir, safeName)
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getSync(
        context: Context,
        key: String,
        type: java.lang.reflect.Type,
        ttlMs: Long = DEFAULT_FRESH_TTL_MS,
        nowMs: Long = System.currentTimeMillis()
    ): CacheLookupResult<T>? {
        val owner = resolveOwnerKey(context)
        val memKey = "$owner::$key"
        memoryCache[memKey]?.let { entry ->
            val value = entry.value as? T
            if (value != null) {
                return CacheLookupResult(
                    data = value,
                    savedAtMs = entry.savedAtMs,
                    isFresh = (nowMs - entry.savedAtMs) <= ttlMs
                )
            }
        }

        return try {
            val file = cacheFileFor(context, owner, key)
            if (!file.exists()) return null
            val raw = file.readText(Charsets.UTF_8)
            if (raw.isBlank()) return null
            val wrapperType = Types.newParameterizedType(CachedEnvelope::class.java, type)
            val adapter = moshi.adapter<CachedEnvelope<T>>(wrapperType)
            val decoded = adapter.fromJson(raw) ?: return null
            memoryCache[memKey] = CacheEntry(decoded.savedAtMs, decoded.data)
            CacheLookupResult(
                data = decoded.data,
                savedAtMs = decoded.savedAtMs,
                isFresh = (nowMs - decoded.savedAtMs) <= ttlMs
            )
        } catch (_: Exception) {
            null
        }
    }

    suspend fun <T : Any> get(
        context: Context,
        key: String,
        type: java.lang.reflect.Type,
        ttlMs: Long = DEFAULT_FRESH_TTL_MS,
        nowMs: Long = System.currentTimeMillis()
    ): CacheLookupResult<T>? = withContext(Dispatchers.IO) {
        getSync(context, key, type, ttlMs, nowMs)
    }

    suspend fun <T : Any> put(
        context: Context,
        key: String,
        value: T,
        type: java.lang.reflect.Type,
        nowMs: Long = System.currentTimeMillis()
    ) = withContext(Dispatchers.IO) {
        putSync(context, key, value, type, nowMs)
    }

    fun <T : Any> putSync(
        context: Context,
        key: String,
        value: T,
        type: java.lang.reflect.Type,
        nowMs: Long = System.currentTimeMillis()
    ) {
        val owner = resolveOwnerKey(context)
        val memKey = "$owner::$key"
        memoryCache[memKey] = CacheEntry(nowMs, value)
        try {
            val wrapperType = Types.newParameterizedType(CachedEnvelope::class.java, type)
            val adapter = moshi.adapter<CachedEnvelope<T>>(wrapperType)
            val json = adapter.toJson(CachedEnvelope(savedAtMs = nowMs, data = value))
            cacheFileFor(context, owner, key).writeText(json, Charsets.UTF_8)
        } catch (_: Exception) {
        }
    }

    fun clearAll(context: Context) {
        memoryCache.clear()
        try {
            val root = File(context.applicationContext.filesDir, CACHE_DIR_NAME)
            if (root.exists()) {
                root.deleteRecursively()
            }
        } catch (_: Exception) {
        }
    }

    fun listType(clazz: Class<*>): java.lang.reflect.Type =
        Types.newParameterizedType(List::class.java, clazz)

    fun discoverPartnerListType(): java.lang.reflect.Type =
        listType(CachedDiscoverPartnerItem::class.java)

    fun momentListType(): java.lang.reflect.Type =
        listType(MomentDto::class.java)

    fun discoverableProfileListType(): java.lang.reflect.Type =
        listType(DiscoverableProfile::class.java)
}

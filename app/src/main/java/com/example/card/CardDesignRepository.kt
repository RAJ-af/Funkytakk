package com.example.card

import android.content.Context
import android.util.LruCache
import com.example.BuildConfig
import com.example.ListRemixableCardsRequest
import com.example.RemixCardDesignRequest
import com.example.SaveCardDesignRequest
import com.example.StoreItemRow
import com.example.SupabaseClient
import com.example.SupabaseSessionManager
import com.example.store.CosmeticsConfigParser
import com.example.store.CosmeticsManager
import com.example.util.ErrorMapper
import com.example.util.MappedErrorException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class MyCardDesignState(
    val design: CardDesign = CardDesign(),
    val remixable: Boolean = true,
    val hasSavedCard: Boolean = false
)

data class RemixApplyResult(
    val design: CardDesign,
    val droppedLockedPackStickers: Int
)

object CardDesignRepository {
    private const val CACHE_TTL_MS = 10 * 60 * 1000L // 10 minutes
    private const val MAX_BATCH_SIZE = 50
    private const val DRAFT_PREFS_NAME = "card_workshop_drafts_v1"

    private data class CachedEntry(
        val fetchedAtMs: Long,
        val design: CardDesign? // null = user has no custom card row
    )

    private val memoryCache = LruCache<String, CachedEntry>(250)

    private val _designsMap = MutableStateFlow<Map<String, CardDesign>>(emptyMap())
    val designsMap: StateFlow<Map<String, CardDesign>> = _designsMap.asStateFlow()

    private val _stickerPacks = MutableStateFlow<List<StoreItemRow>>(emptyList())
    val stickerPacks: StateFlow<List<StoreItemRow>> = _stickerPacks.asStateFlow()
    private var packsFetchedAtMs: Long = 0L

    fun getCachedDesign(userId: String?): CardDesign? {
        if (userId.isNullOrBlank()) return null
        val entry = memoryCache.get(userId)
        if (entry != null && (System.currentTimeMillis() - entry.fetchedAtMs) < CACHE_TTL_MS) {
            return entry.design
        }
        return _designsMap.value[userId]
    }

    fun clearForLogout() {
        memoryCache.evictAll()
        _designsMap.value = emptyMap()
        _stickerPacks.value = emptyList()
        packsFetchedAtMs = 0L
    }

    suspend fun ensureStickerPacksLoaded(
        context: Context,
        forceRefresh: Boolean = false
    ): List<StoreItemRow> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (!forceRefresh && _stickerPacks.value.isNotEmpty() && (now - packsFetchedAtMs) < CACHE_TTL_MS) {
            return@withContext _stickerPacks.value
        }
        val res = CosmeticsManager.getStoreItems(context, type = "sticker_pack", ownedOnly = false)
        res.onSuccess { list ->
            _stickerPacks.value = list
            packsFetchedAtMs = System.currentTimeMillis()
        }
        return@withContext _stickerPacks.value
    }

    fun resolvePackEmoji(packCode: String?, packId: Long?, idx: Int?): String? {
        val packs = _stickerPacks.value
        if (packs.isEmpty()) return null
        val matched = packs.firstOrNull { item ->
            (!packCode.isNullOrBlank() && item.code.equals(packCode, ignoreCase = true)) ||
                (packId != null && item.id == packId)
        } ?: return null
        val emojis = CosmeticsConfigParser.parseStickerPack(matched.config).emojis
        if (emojis.isEmpty()) return null
        val safeIdx = (idx ?: 0).coerceIn(0, emojis.lastIndex)
        return emojis.getOrNull(safeIdx)
    }

    suspend fun batchFetchCardDesigns(
        context: Context,
        userIds: List<String>,
        forceRefresh: Boolean = false
    ): Map<String, CardDesign> = withContext(Dispatchers.IO) {
        val cleanIds = userIds.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        if (cleanIds.isEmpty()) return@withContext emptyMap()

        val now = System.currentTimeMillis()
        val resolved = linkedMapOf<String, CardDesign>()
        val missingIds = mutableListOf<String>()

        for (id in cleanIds) {
            val cached = if (forceRefresh) null else memoryCache.get(id)
            if (cached != null && (now - cached.fetchedAtMs) < CACHE_TTL_MS) {
                if (cached.design != null) {
                    resolved[id] = cached.design
                }
            } else {
                missingIds.add(id)
            }
        }

        if (missingIds.isNotEmpty()) {
            try {
                for (chunk in missingIds.chunked(MAX_BATCH_SIZE)) {
                    val filter = "in.(${chunk.joinToString(",")})"
                    val res = ErrorMapper.executeWithSingle401Retry(context) { auth ->
                        SupabaseClient.api.getUserCardDesigns(
                            apiKey = BuildConfig.SUPABASE_ANON_KEY,
                            auth = auth,
                            select = "user_id,design",
                            userIdFilter = filter
                        )
                    }
                    res.onSuccess { response ->
                        val raw = response.body()?.string().orEmpty()
                        val arr = runCatching { JSONArray(raw.ifBlank { "[]" }) }.getOrNull() ?: JSONArray()
                        val seenInChunk = hashSetOf<String>()
                        val fetchTs = System.currentTimeMillis()
                        for (i in 0 until arr.length()) {
                            val row = arr.optJSONObject(i) ?: continue
                            val uid = row.optString("user_id", "").trim()
                            if (uid.isBlank()) continue
                            seenInChunk.add(uid)
                            val designObj = row.optJSONObject("design")
                            val parsed = if (designObj != null) {
                                CardDesignCodec.fromJsonObject(designObj)
                            } else {
                                CardDesignCodec.fromJsonString(row.optString("design", ""))
                            }
                            memoryCache.put(uid, CachedEntry(fetchTs, parsed))
                            if (parsed != null) {
                                resolved[uid] = parsed
                            }
                        }
                        for (id in chunk) {
                            if (!seenInChunk.contains(id)) {
                                memoryCache.put(id, CachedEntry(fetchTs, null))
                            }
                        }
                    }
                }
                if (resolved.isNotEmpty()) {
                    _designsMap.value = _designsMap.value + resolved
                    if (resolved.values.any { d -> d.stickers.any { it.kind == CardStickerKind.PACK && it.emoji.isBlank() } }) {
                        ensureStickerPacksLoaded(context)
                    }
                }
            } catch (ce: CancellationException) {
                throw ce
            } catch (_: Throwable) {
                // Non-fatal: cards without fetched design gracefully render default bold style.
            }
        }

        return@withContext resolved
    }

    suspend fun fetchMyCardDesign(context: Context): Result<MyCardDesignState> = withContext(Dispatchers.IO) {
        try {
            SupabaseSessionManager.ensureActiveSession(context)
            val myUid = SupabaseSessionManager.getUserId(context).orEmpty()
            if (myUid.isBlank()) {
                return@withContext Result.failure(MappedErrorException(ErrorMapper.MSG_UNAUTHORIZED))
            }

            val res = ErrorMapper.executeWithSingle401Retry(context) { auth ->
                SupabaseClient.api.getUserCardDesigns(
                    apiKey = BuildConfig.SUPABASE_ANON_KEY,
                    auth = auth,
                    select = "user_id,design,remixable",
                    userIdFilter = "eq.$myUid"
                )
            }

            res.fold(
                onSuccess = { response ->
                    val raw = response.body()?.string().orEmpty()
                    val arr = runCatching { JSONArray(raw.ifBlank { "[]" }) }.getOrNull() ?: JSONArray()
                    val first = arr.optJSONObject(0)
                    if (first == null) {
                        Result.success(MyCardDesignState(design = CardDesign(), remixable = true, hasSavedCard = false))
                    } else {
                        val designObj = first.optJSONObject("design")
                        val parsed = if (designObj != null) {
                            CardDesignCodec.fromJsonObject(designObj)
                        } else {
                            CardDesignCodec.fromJsonString(first.optString("design", "")) ?: CardDesign()
                        }
                        val remixable = first.optBoolean("remixable", true)
                        memoryCache.put(myUid, CachedEntry(System.currentTimeMillis(), parsed))
                        _designsMap.value = _designsMap.value + (myUid to parsed)
                        Result.success(MyCardDesignState(design = parsed, remixable = remixable, hasSavedCard = true))
                    }
                },
                onFailure = { err ->
                    Result.failure(err)
                }
            )
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            Result.failure(MappedErrorException(ErrorMapper.map(t)))
        }
    }

    suspend fun saveMyCardDesign(
        context: Context,
        design: CardDesign,
        remixable: Boolean
    ): Result<CardDesign> = withContext(Dispatchers.IO) {
        try {
            if (CardTextSafetyFilter.containsBlockedWord(design.hiLabel)) {
                return@withContext Result.failure(MappedErrorException("Please choose another word"))
            }
            if (design.stickers.any { it.kind == CardStickerKind.WORD && CardTextSafetyFilter.containsBlockedWord(it.text) }) {
                return@withContext Result.failure(MappedErrorException("Please choose another word"))
            }

            SupabaseSessionManager.ensureActiveSession(context)
            val wireMap = CardDesignCodec.toWireMap(design)
            val res = ErrorMapper.executeWithSingle401Retry(context) { auth ->
                SupabaseClient.api.saveMyCardDesign(
                    apiKey = BuildConfig.SUPABASE_ANON_KEY,
                    auth = auth,
                    body = SaveCardDesignRequest(
                        p_design = wireMap,
                        p_remixable = remixable
                    )
                )
            }

            res.fold(
                onSuccess = { response ->
                    val raw = response.body()?.string().orEmpty()
                    val parsed = parseDesignFromRpcBody(raw) ?: design
                    val myUid = SupabaseSessionManager.getUserId(context).orEmpty()
                    if (myUid.isNotBlank()) {
                        memoryCache.put(myUid, CachedEntry(System.currentTimeMillis(), parsed))
                        _designsMap.value = _designsMap.value + (myUid to parsed)
                    }
                    Result.success(parsed)
                },
                onFailure = { err ->
                    Result.failure(err)
                }
            )
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            Result.failure(MappedErrorException(ErrorMapper.map(t)))
        }
    }

    suspend fun resetMyCardDesign(context: Context): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            SupabaseSessionManager.ensureActiveSession(context)
            val res = ErrorMapper.executeWithSingle401Retry(context) { auth ->
                SupabaseClient.api.resetMyCardDesign(
                    apiKey = BuildConfig.SUPABASE_ANON_KEY,
                    auth = auth
                )
            }
            res.fold(
                onSuccess = {
                    val myUid = SupabaseSessionManager.getUserId(context).orEmpty()
                    if (myUid.isNotBlank()) {
                        memoryCache.put(myUid, CachedEntry(System.currentTimeMillis(), null))
                        _designsMap.value = _designsMap.value - myUid
                    }
                    Result.success(Unit)
                },
                onFailure = { err ->
                    Result.failure(err)
                }
            )
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            Result.failure(MappedErrorException(ErrorMapper.map(t)))
        }
    }

    suspend fun listRemixableCards(
        context: Context,
        limit: Int = 20,
        offset: Int = 0
    ): Result<List<RemixableCardItem>> = withContext(Dispatchers.IO) {
        try {
            SupabaseSessionManager.ensureActiveSession(context)
            val res = ErrorMapper.executeWithSingle401Retry(context) { auth ->
                SupabaseClient.api.listRemixableCards(
                    apiKey = BuildConfig.SUPABASE_ANON_KEY,
                    auth = auth,
                    body = ListRemixableCardsRequest(p_limit = limit, p_offset = offset)
                )
            }
            res.fold(
                onSuccess = { response ->
                    val raw = response.body()?.string().orEmpty()
                    val arr = when {
                        raw.trim().startsWith("[") -> runCatching { JSONArray(raw) }.getOrNull()
                        raw.trim().startsWith("{") -> runCatching {
                            val obj = JSONObject(raw)
                            obj.optJSONArray("items") ?: obj.optJSONArray("cards") ?: obj.optJSONArray("data")
                        }.getOrNull()
                        else -> null
                    } ?: JSONArray()

                    val list = mutableListOf<RemixableCardItem>()
                    for (i in 0 until arr.length()) {
                        val row = arr.optJSONObject(i) ?: continue
                        val uid = row.optString("user_id", "").ifBlank { row.optString("auth_user_id", "") }.trim()
                        if (uid.isBlank()) continue
                        val designObj = row.optJSONObject("design")
                        val parsedDesign = if (designObj != null) {
                            CardDesignCodec.fromJsonObject(designObj)
                        } else {
                            CardDesignCodec.fromJsonString(row.optString("design", "")) ?: CardDesign()
                        }
                        val creatorName = row.optString("profile_name", "")
                            .ifBlank { row.optString("creator_name", "") }
                            .ifBlank { row.optString("username", "") }
                            .ifBlank { "Learner" }
                        val creatorUsername = row.optString("username", "")
                        val creatorAvatar = row.optString("avatar_url", "")
                        val countryCode = row.optString("country_code", "")
                        val remixCount = row.optInt("remix_count", row.optInt("remixes_count", 0)).coerceAtLeast(0)
                        list.add(
                            RemixableCardItem(
                                userId = uid,
                                creatorName = creatorName,
                                creatorUsername = creatorUsername,
                                creatorAvatarUrl = creatorAvatar,
                                countryCode = countryCode,
                                remixCount = remixCount,
                                design = parsedDesign
                            )
                        )
                    }
                    Result.success(list)
                },
                onFailure = { err ->
                    Result.failure(err)
                }
            )
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            Result.failure(MappedErrorException(ErrorMapper.map(t)))
        }
    }

    suspend fun remixCardDesign(
        context: Context,
        sourceUserId: String,
        availablePacks: List<StoreItemRow>
    ): Result<RemixApplyResult> = withContext(Dispatchers.IO) {
        try {
            SupabaseSessionManager.ensureActiveSession(context)
            val res = ErrorMapper.executeWithSingle401Retry(context) { auth ->
                SupabaseClient.api.remixCardDesign(
                    apiKey = BuildConfig.SUPABASE_ANON_KEY,
                    auth = auth,
                    body = RemixCardDesignRequest(p_source = sourceUserId)
                )
            }
            res.fold(
                onSuccess = { response ->
                    val raw = response.body()?.string().orEmpty()
                    val parsed = parseDesignFromRpcBody(raw)
                        ?: return@fold Result.failure(MappedErrorException(ErrorMapper.MSG_GENERIC))

                    val unlockedIds = availablePacks
                        .filter { it.owned || it.price_coins == 0 }
                        .map { it.id }
                        .toSet()
                    val unlockedCodes = availablePacks
                        .filter { it.owned || it.price_coins == 0 }
                        .map { it.code.lowercase() }
                        .toSet()

                    var dropped = 0
                    val filteredStickers = if (availablePacks.isEmpty()) {
                        parsed.stickers
                    } else {
                        parsed.stickers.filter { st ->
                            if (st.kind != CardStickerKind.PACK) {
                                true
                            } else {
                                val idAllowed = st.packId != null && unlockedIds.contains(st.packId)
                                val codeAllowed = !st.packCode.isNullOrBlank() && unlockedCodes.contains(st.packCode.lowercase())
                                val keep = idAllowed || codeAllowed
                                if (!keep) dropped++
                                keep
                            }
                        }
                    }

                    val finalDesign = parsed.copy(stickers = filteredStickers)
                    val myUid = SupabaseSessionManager.getUserId(context).orEmpty()
                    if (myUid.isNotBlank()) {
                        memoryCache.put(myUid, CachedEntry(System.currentTimeMillis(), finalDesign))
                        _designsMap.value = _designsMap.value + (myUid to finalDesign)
                    }
                    Result.success(RemixApplyResult(finalDesign, dropped))
                },
                onFailure = { err ->
                    Result.failure(err)
                }
            )
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            Result.failure(MappedErrorException(ErrorMapper.map(t)))
        }
    }

    private fun parseDesignFromRpcBody(raw: String): CardDesign? {
        if (raw.isBlank()) return null
        val obj = runCatching { JSONObject(raw) }.getOrNull() ?: return null
        val nested = obj.optJSONObject("design") ?: obj.optJSONObject("p_design") ?: obj
        return CardDesignCodec.fromJsonObject(nested)
    }

    fun saveDraft(context: Context, userId: String, design: CardDesign, remixable: Boolean) {
        if (userId.isBlank()) return
        val prefs = context.applicationContext.getSharedPreferences(DRAFT_PREFS_NAME, Context.MODE_PRIVATE)
        val payload = JSONObject().apply {
            put("remixable", remixable)
            put("design", JSONObject(CardDesignCodec.toWireMap(design)))
        }
        prefs.edit().putString("draft_$userId", payload.toString()).apply()
    }

    fun loadDraft(context: Context, userId: String): Pair<CardDesign, Boolean>? {
        if (userId.isBlank()) return null
        val prefs = context.applicationContext.getSharedPreferences(DRAFT_PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString("draft_$userId", null) ?: return null
        val obj = runCatching { JSONObject(raw) }.getOrNull() ?: return null
        val designObj = obj.optJSONObject("design") ?: return null
        val remixable = obj.optBoolean("remixable", true)
        return CardDesignCodec.fromJsonObject(designObj) to remixable
    }

    fun clearDraft(context: Context, userId: String) {
        if (userId.isBlank()) return
        context.applicationContext.getSharedPreferences(DRAFT_PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove("draft_$userId")
            .apply()
    }
}

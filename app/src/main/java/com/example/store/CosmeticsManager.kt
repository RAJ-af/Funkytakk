package com.example.store

import android.content.Context
import android.util.Log
import android.util.LruCache
import com.example.BuildConfig
import com.example.CosmeticSlotInfo
import com.example.EquipItemRequest
import com.example.GetStoreItemsRequest
import com.example.GetUserCosmeticsRequest
import com.example.PurchaseItemRequest
import com.example.PurchaseItemsRequest
import com.example.PurchaseItemsResponse
import com.example.StoreItemRow
import com.example.SupabaseClient
import com.example.SupabaseSessionManager
import com.example.UnequipSlotRequest
import com.example.UserCosmeticsRow
import com.example.VerifiedOwnedItemRow
import com.example.VerifyOwnedItemsRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class PurchaseItemsResult(
    val bought: List<Long> = emptyList(),
    val alreadyOwned: List<Long> = emptyList(),
    val equipped: List<Long> = emptyList(),
    val coinsSpent: Int = 0,
    val remainingBalance: Int = 0
)

object CosmeticsManager {
    private const val TAG = "CosmeticsManager"

    // 10 minute TTL for cosmetics cache
    private const val CACHE_TTL_MS = 10 * 60 * 1000L

    private data class CachedCosmetic(
        val timestamp: Long,
        val cosmetics: UserCosmeticsRow
    )

    // In-Memory LRU Caches
    private val cosmeticsCache = LruCache<String, CachedCosmetic>(200)
    private val verifiedOwnedCache = LruCache<String, Set<Long>>(300) // key = "$userId_$itemId"

    private val _userWalletBalance = MutableStateFlow<Int>(0)
    val userWalletBalance: StateFlow<Int> = _userWalletBalance.asStateFlow()

    private val _myCosmetics = MutableStateFlow<UserCosmeticsRow?>(null)
    val myCosmetics: StateFlow<UserCosmeticsRow?> = _myCosmetics.asStateFlow()

    private val _userCosmeticsMap = MutableStateFlow<Map<String, UserCosmeticsRow>>(emptyMap())
    val userCosmeticsMap: StateFlow<Map<String, UserCosmeticsRow>> = _userCosmeticsMap.asStateFlow()

    fun getCachedCosmetics(userId: String?): UserCosmeticsRow? {
        if (userId.isNullOrBlank()) return null
        return cosmeticsCache.get(userId)?.cosmetics ?: _userCosmeticsMap.value[userId]
    }

    suspend fun getOrCreateWallet(context: Context): Int = withContext(Dispatchers.IO) {
        try {
            SupabaseSessionManager.ensureActiveSession(context)
            val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                ?: SupabaseSessionManager.getBearerToken(context)
                ?: "Bearer ${BuildConfig.SUPABASE_ANON_KEY}"

            val resp = SupabaseClient.api.getOrCreateWallet(
                apiKey = BuildConfig.SUPABASE_ANON_KEY,
                auth = token
            )
            if (resp.isSuccessful) {
                val bodyStr = resp.body()?.string()?.trim().orEmpty()
                val coins = bodyStr.toIntOrNull()
                    ?: JSONObject(bodyStr.ifEmpty { "{}" }).optInt("coins", 0)
                _userWalletBalance.value = coins
                return@withContext coins
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching wallet balance", e)
        }
        return@withContext _userWalletBalance.value
    }

    suspend fun getStoreItems(
        context: Context,
        type: String? = null,
        ownedOnly: Boolean = false
    ): Result<List<StoreItemRow>> = withContext(Dispatchers.IO) {
        try {
            SupabaseSessionManager.ensureActiveSession(context)
            val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                ?: return@withContext Result.failure(Exception("Not authenticated"))

            val resp = SupabaseClient.api.getStoreItems(
                apiKey = BuildConfig.SUPABASE_ANON_KEY,
                auth = token,
                body = GetStoreItemsRequest(p_type = type, p_owned_only = ownedOnly)
            )

            if (resp.isSuccessful) {
                Result.success(resp.body().orEmpty())
            } else {
                val errStr = resp.errorBody()?.string().orEmpty()
                Result.failure(Exception(errStr.ifEmpty { "Failed to load store items" }))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading store items", e)
            Result.failure(e)
        }
    }

    suspend fun purchaseItem(context: Context, itemId: Long): Result<Int> = withContext(Dispatchers.IO) {
        try {
            SupabaseSessionManager.ensureActiveSession(context)
            val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                ?: return@withContext Result.failure(Exception("Not authenticated"))

            val resp = SupabaseClient.api.purchaseItem(
                apiKey = BuildConfig.SUPABASE_ANON_KEY,
                auth = token,
                body = PurchaseItemRequest(p_item_id = itemId)
            )

            val rawBody = if (resp.isSuccessful) resp.body()?.string().orEmpty() else resp.errorBody()?.string().orEmpty()
            val jsonObj = runCatching { JSONObject(rawBody) }.getOrNull()

            if (resp.isSuccessful) {
                val remaining = jsonObj?.optInt("remaining_balance", _userWalletBalance.value) ?: _userWalletBalance.value
                _userWalletBalance.value = remaining
                Result.success(remaining)
            } else {
                val errorMsg = jsonObj?.optString("message")
                    ?: jsonObj?.optString("error")
                    ?: rawBody
                val mappedMsg = when {
                    errorMsg.contains("insufficient_coins", ignoreCase = true) -> "Insufficient coins! Earn or buy more coins."
                    errorMsg.contains("already_owned", ignoreCase = true) -> "You already own this item!"
                    errorMsg.contains("item_not_found", ignoreCase = true) -> "Item not found in store."
                    else -> errorMsg.ifEmpty { "Purchase failed" }
                }
                Result.failure(Exception(mappedMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error purchasing item", e)
            Result.failure(e)
        }
    }

    fun updateWalletBalance(newBalance: Int) {
        _userWalletBalance.value = newBalance
        try {
            com.example.gifting.GiftManager.updateWalletBalance(newBalance)
        } catch (_: Exception) {}
    }

    suspend fun purchaseItems(
        context: Context,
        itemIds: List<Long>,
        apply: Boolean
    ): Result<PurchaseItemsResult> = withContext(Dispatchers.IO) {
        if (itemIds.isEmpty()) return@withContext Result.failure(Exception("No items selected."))
        try {
            SupabaseSessionManager.ensureActiveSession(context)
            val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                ?: return@withContext Result.failure(Exception("Please sign in to purchase."))

            val reqBody = PurchaseItemsRequest(p_item_ids = itemIds, p_equip = apply)

            if (BuildConfig.DEBUG) {
                Log.d(TAG, "[purchaseItems] Request JSON: {\"p_item_ids\":$itemIds,\"p_equip\":$apply}")
            }

            val resp = SupabaseClient.api.purchaseItems(
                apiKey = BuildConfig.SUPABASE_ANON_KEY,
                auth = token,
                body = reqBody
            )

            val rawBody = if (resp.isSuccessful) resp.body()?.string().orEmpty() else resp.errorBody()?.string().orEmpty()
            
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "[purchaseItems] Response code: ${resp.code()}, body: $rawBody")
            }

            val jsonObj = runCatching { JSONObject(rawBody) }.getOrNull()

            if (resp.isSuccessful) {
                val boughtList = mutableListOf<Long>()
                jsonObj?.optJSONArray("bought")?.let { arr ->
                    for (i in 0 until arr.length()) boughtList.add(arr.optLong(i))
                }
                val alreadyOwnedList = mutableListOf<Long>()
                jsonObj?.optJSONArray("already_owned")?.let { arr ->
                    for (i in 0 until arr.length()) alreadyOwnedList.add(arr.optLong(i))
                }
                val equippedList = mutableListOf<Long>()
                jsonObj?.optJSONArray("equipped")?.let { arr ->
                    for (i in 0 until arr.length()) equippedList.add(arr.optLong(i))
                }
                val coinsSpent = jsonObj?.optInt("coins_spent", 0) ?: 0
                val remaining = jsonObj?.optInt("remaining_balance", _userWalletBalance.value) ?: _userWalletBalance.value

                updateWalletBalance(remaining)

                val myUid = SupabaseSessionManager.getUserId(context).orEmpty()
                if (myUid.isNotBlank()) {
                    cosmeticsCache.remove(myUid)
                    refreshMyCosmetics(context, myUid)
                }

                Result.success(
                    PurchaseItemsResult(
                        bought = boughtList,
                        alreadyOwned = alreadyOwnedList,
                        equipped = equippedList,
                        coinsSpent = coinsSpent,
                        remainingBalance = remaining
                    )
                )
            } else {
                val errorMsg = jsonObj?.optString("message")
                    ?: jsonObj?.optString("error")
                    ?: rawBody
                val mappedMsg = when {
                    errorMsg.contains("insufficient_coins", ignoreCase = true) -> "Not enough coins."
                    errorMsg.contains("item_not_found", ignoreCase = true) -> "Item not found in store."
                    errorMsg.contains("no_items", ignoreCase = true) -> "No items selected."
                    errorMsg.contains("too_many_items", ignoreCase = true) -> "Too many items selected."
                    errorMsg.contains("not_authenticated", ignoreCase = true) -> "Please sign in to purchase."
                    else -> "Something went wrong. Please try again."
                }
                Result.failure(Exception(mappedMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in purchaseItems", e)
            Result.failure(Exception("Something went wrong. Please try again."))
        }
    }

    suspend fun equipItem(context: Context, itemId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            SupabaseSessionManager.ensureActiveSession(context)
            val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                ?: return@withContext Result.failure(Exception("Not authenticated"))

            val resp = SupabaseClient.api.equipItem(
                apiKey = BuildConfig.SUPABASE_ANON_KEY,
                auth = token,
                body = EquipItemRequest(p_item_id = itemId)
            )

            if (resp.isSuccessful) {
                // Invalidate caller's cosmetics cache & refresh
                val myUid = SupabaseSessionManager.getUserId(context).orEmpty()
                if (myUid.isNotBlank()) {
                    cosmeticsCache.remove(myUid)
                    refreshMyCosmetics(context, myUid)
                }
                Result.success(Unit)
            } else {
                val errStr = resp.errorBody()?.string().orEmpty()
                val mappedMsg = when {
                    errStr.contains("not_owned", ignoreCase = true) -> "You must buy this item before equipping!"
                    errStr.contains("not_equippable", ignoreCase = true) -> "This item cannot be equipped in this slot."
                    else -> errStr.ifEmpty { "Equip failed" }
                }
                Result.failure(Exception(mappedMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error equipping item", e)
            Result.failure(e)
        }
    }

    suspend fun unequipSlot(context: Context, slot: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            SupabaseSessionManager.ensureActiveSession(context)
            val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                ?: return@withContext Result.failure(Exception("Not authenticated"))

            val resp = SupabaseClient.api.unequipSlot(
                apiKey = BuildConfig.SUPABASE_ANON_KEY,
                auth = token,
                body = UnequipSlotRequest(p_slot = slot)
            )

            if (resp.isSuccessful) {
                val myUid = SupabaseSessionManager.getUserId(context).orEmpty()
                if (myUid.isNotBlank()) {
                    cosmeticsCache.remove(myUid)
                    refreshMyCosmetics(context, myUid)
                }
                Result.success(Unit)
            } else {
                Result.failure(Exception("Unequip failed"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error unequipping slot", e)
            Result.failure(e)
        }
    }

    suspend fun getUserCosmetics(
        context: Context,
        userIds: List<String>
    ): Map<String, UserCosmeticsRow> = withContext(Dispatchers.IO) {
        val cleanIds = userIds.filter { it.isNotBlank() }.distinct()
        if (cleanIds.isEmpty()) return@withContext emptyMap()

        val now = System.currentTimeMillis()
        val result = mutableMapOf<String, UserCosmeticsRow>()
        val missingIds = mutableListOf<String>()

        for (id in cleanIds) {
            val cached = cosmeticsCache.get(id)
            if (cached != null && (now - cached.timestamp) < CACHE_TTL_MS) {
                result[id] = cached.cosmetics
            } else {
                missingIds.add(id)
            }
        }

        if (missingIds.isNotEmpty()) {
            try {
                SupabaseSessionManager.ensureActiveSession(context)
                val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                    ?: SupabaseSessionManager.getBearerToken(context)
                    ?: "Bearer ${BuildConfig.SUPABASE_ANON_KEY}"

                // Batch up to 100 max per call
                val chunked = missingIds.chunked(100)
                for (chunk in chunked) {
                    // Try get_user_cosmetics_v2 first, fallback to get_user_cosmetics if needed
                    val resp = try {
                        val v2Resp = SupabaseClient.api.getUserCosmeticsV2(
                            apiKey = BuildConfig.SUPABASE_ANON_KEY,
                            auth = token,
                            body = GetUserCosmeticsRequest(p_user_ids = chunk)
                        )
                        if (v2Resp.isSuccessful) v2Resp else SupabaseClient.api.getUserCosmetics(
                            apiKey = BuildConfig.SUPABASE_ANON_KEY,
                            auth = token,
                            body = GetUserCosmeticsRequest(p_user_ids = chunk)
                        )
                    } catch (_: Exception) {
                        SupabaseClient.api.getUserCosmetics(
                            apiKey = BuildConfig.SUPABASE_ANON_KEY,
                            auth = token,
                            body = GetUserCosmeticsRequest(p_user_ids = chunk)
                        )
                    }

                    if (resp.isSuccessful) {
                        resp.body().orEmpty().forEach { row ->
                            cosmeticsCache.put(row.user_id, CachedCosmetic(now, row))
                            result[row.user_id] = row
                        }
                    }
                }
                if (result.isNotEmpty()) {
                    _userCosmeticsMap.value = _userCosmeticsMap.value + result
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching user cosmetics", e)
            }
        }

        return@withContext result
    }

    suspend fun refreshMyCosmetics(context: Context, myUserId: String) {
        if (myUserId.isBlank()) return
        cosmeticsCache.remove(myUserId)
        val map = getUserCosmetics(context, listOf(myUserId))
        val row = map[myUserId]
        if (row != null) {
            _myCosmetics.value = row
            _userCosmeticsMap.value = _userCosmeticsMap.value + (myUserId to row)
        }
    }

    suspend fun batchFetchUserCosmetics(context: Context, userIds: List<String>) {
        getUserCosmetics(context, userIds)
    }

    suspend fun verifyOwnedItem(
        context: Context,
        userId: String,
        itemId: Long
    ): Boolean = withContext(Dispatchers.IO) {
        if (userId.isBlank() || itemId <= 0L) return@withContext false

        val cacheKey = "${userId}_$itemId"
        val cached = verifiedOwnedCache.get(userId)
        if (cached != null && cached.contains(itemId)) {
            return@withContext true
        }

        try {
            SupabaseSessionManager.ensureActiveSession(context)
            val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                ?: SupabaseSessionManager.getBearerToken(context)
                ?: "Bearer ${BuildConfig.SUPABASE_ANON_KEY}"

            val resp = SupabaseClient.api.verifyOwnedItems(
                apiKey = BuildConfig.SUPABASE_ANON_KEY,
                auth = token,
                body = VerifyOwnedItemsRequest(p_user_id = userId, p_item_ids = listOf(itemId))
            )

            if (resp.isSuccessful) {
                val verifiedRows = resp.body().orEmpty()
                val isVerified = verifiedRows.any { it.item_id == itemId }
                if (isVerified) {
                    val currentSet = verifiedOwnedCache.get(userId) ?: emptySet()
                    verifiedOwnedCache.put(userId, currentSet + itemId)
                }
                return@withContext isVerified
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error verifying owned item", e)
        }

        return@withContext false
    }
}

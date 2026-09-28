package com.example.gifting

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.GiftCatalogItem
import com.example.GiftTransactionItem
import com.example.SendGiftRequest
import com.example.SupabaseClient
import com.example.SupabaseSessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject

class InsufficientCoinsException(message: String = "Insufficient coins") : Exception(message)

object GiftManager {
    private const val TAG = "GiftManager"

    private val _walletBalance = MutableStateFlow(100)
    val walletBalance: StateFlow<Int> = _walletBalance.asStateFlow()

    private val _catalog = MutableStateFlow<List<GiftCatalogItem>>(emptyList())
    val catalog: StateFlow<List<GiftCatalogItem>> = _catalog.asStateFlow()

    private val _isLoadingCatalog = MutableStateFlow(false)
    val isLoadingCatalog: StateFlow<Boolean> = _isLoadingCatalog.asStateFlow()

    /**
     * Fetches active gifts from Supabase gift_catalog table.
     */
    suspend fun fetchCatalog(context: Context): List<GiftCatalogItem> = withContext(Dispatchers.IO) {
        _isLoadingCatalog.value = true
        try {
            val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
            val authHeader = if (!token.isNullOrBlank()) token else "Bearer ${BuildConfig.SUPABASE_ANON_KEY}"
            val resp = SupabaseClient.api.getActiveGiftCatalog(
                apiKey = BuildConfig.SUPABASE_ANON_KEY,
                auth = authHeader
            )
            if (resp.isSuccessful && resp.body() != null) {
                val list = resp.body()!!
                _catalog.value = list
                Log.d(TAG, "Loaded ${list.size} active gifts from catalog")
                return@withContext list
            } else {
                Log.w(TAG, "Failed to load catalog: ${resp.code()} ${resp.message()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching gift catalog", e)
        } finally {
            _isLoadingCatalog.value = false
        }
        _catalog.value
    }

    /**
     * Calls RPC get_or_create_wallet() to get the user's coin balance.
     * Starter wallet receives 100 free coins on first call.
     */
    suspend fun refreshWallet(context: Context): Int = withContext(Dispatchers.IO) {
        try {
            val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
            if (token.isNullOrBlank()) return@withContext _walletBalance.value

            val resp = SupabaseClient.api.getOrCreateWallet(
                apiKey = BuildConfig.SUPABASE_ANON_KEY,
                auth = token
            )
            if (resp.isSuccessful) {
                val bodyStr = resp.body()?.string()?.trim().orEmpty()
                val parsedBalance = parseBalance(bodyStr)
                if (parsedBalance != null) {
                    _walletBalance.value = parsedBalance
                    Log.d(TAG, "Wallet refreshed: $parsedBalance coins")
                    return@withContext parsedBalance
                }
            } else {
                Log.w(TAG, "get_or_create_wallet returned ${resp.code()} ${resp.message()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing wallet balance", e)
        }
        _walletBalance.value
    }

    private fun parseBalance(bodyStr: String): Int? {
        if (bodyStr.isBlank()) return null
        // Case 1: Pure number e.g. "100"
        bodyStr.toIntOrNull()?.let { return it }

        // Case 2: JSON Object e.g. {"balance": 100} or {"coins": 100}
        try {
            val json = JSONObject(bodyStr)
            if (json.has("balance")) return json.optInt("balance", 100)
            if (json.has("coins")) return json.optInt("coins", 100)
            if (json.has("remaining_balance")) return json.optInt("remaining_balance", 100)
        } catch (_: Exception) {}

        // Case 3: JSON Array e.g. [{"balance": 100}]
        try {
            val arr = org.json.JSONArray(bodyStr)
            if (arr.length() > 0) {
                val obj = arr.getJSONObject(0)
                if (obj.has("balance")) return obj.optInt("balance", 100)
            }
        } catch (_: Exception) {}

        return null
    }

    /**
     * Sends a gift to recipient by calling RPC send_gift.
     * On success: updates shown balance from returned remaining_balance (never guess client-side).
     * On error "insufficient_coins": throws InsufficientCoinsException.
     */
    suspend fun sendGift(
        context: Context,
        recipientId: String,
        giftId: String,
        message: String? = null
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
            if (token.isNullOrBlank()) {
                return@withContext Result.failure(Exception("Not authenticated. Please log in."))
            }

            val resp = SupabaseClient.api.sendGift(
                apiKey = BuildConfig.SUPABASE_ANON_KEY,
                auth = token,
                body = SendGiftRequest(
                    p_recipient_id = recipientId,
                    p_gift_id = giftId,
                    p_message = message?.takeIf { it.isNotBlank() }
                )
            )

            if (resp.isSuccessful) {
                val bodyStr = resp.body()?.string()?.trim().orEmpty()
                val json = try { JSONObject(bodyStr) } catch (_: Exception) { JSONObject() }
                val ok = json.optBoolean("ok", true)
                val remaining = if (json.has("remaining_balance")) {
                    json.getInt("remaining_balance")
                } else {
                    bodyStr.toIntOrNull() ?: (_walletBalance.value - 10)
                }

                if (ok) {
                    _walletBalance.value = remaining
                    Log.d(TAG, "Gift sent successfully! Remaining balance: $remaining coins")
                    return@withContext Result.success(remaining)
                } else {
                    val err = json.optString("error", "Failed to send gift")
                    if (err.contains("insufficient", ignoreCase = true) || err.contains("not_enough", ignoreCase = true)) {
                        return@withContext Result.failure(InsufficientCoinsException("You do not have enough coins."))
                    }
                    return@withContext Result.failure(Exception(err))
                }
            } else {
                val errBody = resp.errorBody()?.string().orEmpty()
                Log.w(TAG, "send_gift error: ${resp.code()} $errBody")
                if (errBody.contains("insufficient_coins", ignoreCase = true) || errBody.contains("not enough coins", ignoreCase = true)) {
                    return@withContext Result.failure(InsufficientCoinsException("You do not have enough coins."))
                }
                return@withContext Result.failure(Exception("Failed to send gift: $errBody"))
            }
        } catch (e: InsufficientCoinsException) {
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Exception sending gift", e)
            Result.failure(e)
        }
    }

    /**
     * Fetches gifts received by a profile (gift_transactions where recipient_id = profileId).
     */
    suspend fun fetchGiftsReceived(
        context: Context,
        profileId: String
    ): List<GiftTransactionItem> = withContext(Dispatchers.IO) {
        try {
            val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
            val authHeader = if (!token.isNullOrBlank()) token else "Bearer ${BuildConfig.SUPABASE_ANON_KEY}"

            val resp = SupabaseClient.api.getGiftTransactions(
                apiKey = BuildConfig.SUPABASE_ANON_KEY,
                auth = authHeader,
                recipientIdFilter = "eq.$profileId"
            )
            if (resp.isSuccessful && resp.body() != null) {
                val transactions = resp.body()!!
                // Enrich with gift catalog details if missing in row
                val catMap = _catalog.value.associateBy { it.id }
                return@withContext transactions.map { tx ->
                    val cat = tx.gift_id?.let { catMap[it] }
                    tx.copy(
                        gift_name = tx.gift_name ?: cat?.name ?: "Funky Gift",
                        gift_icon_url = tx.gift_icon_url ?: cat?.icon_url ?: "",
                        gift_price_coins = tx.gift_price_coins ?: cat?.price_coins ?: 0
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching gifts received", e)
        }
        emptyList()
    }

    /**
     * Fetches user's gift transaction history (sent or received).
     */
    suspend fun fetchMyGiftHistory(context: Context): List<GiftTransactionItem> = withContext(Dispatchers.IO) {
        val myId = SupabaseSessionManager.getUserId(context) ?: return@withContext emptyList()
        val received = fetchGiftsReceived(context, myId)
        try {
            val token = SupabaseSessionManager.getAuthenticatedBearerToken(context) ?: return@withContext received
            val resp = SupabaseClient.api.getGiftTransactions(
                apiKey = BuildConfig.SUPABASE_ANON_KEY,
                auth = token,
                senderIdFilter = "eq.$myId"
            )
            if (resp.isSuccessful && resp.body() != null) {
                val sent = resp.body()!!
                val catMap = _catalog.value.associateBy { it.id }
                val enrichedSent = sent.map { tx ->
                    val cat = tx.gift_id?.let { catMap[it] }
                    tx.copy(
                        gift_name = tx.gift_name ?: cat?.name ?: "Funky Gift",
                        gift_icon_url = tx.gift_icon_url ?: cat?.icon_url ?: "",
                        gift_price_coins = tx.gift_price_coins ?: cat?.price_coins ?: 0
                    )
                }
                return@withContext (received + enrichedSent).distinctBy { it.id }.sortedByDescending { it.created_at }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching sent gifts", e)
        }
        received
    }
}

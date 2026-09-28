package com.example

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ProfileBadge(
    val code: String,
    val name: String,
    val description: String,
    val iconUrl: String?,
    val awardedAt: String? = null
)

object BadgeManager {
    private const val TAG = "BadgeManager"

    suspend fun getBadgesForUser(userId: String): List<ProfileBadge> = withContext(Dispatchers.IO) {
        try {
            val res = SupabaseClient.api.getUserBadges(userIdEq = "eq.$userId")
            if (res.isSuccessful) {
                val list = res.body() ?: emptyList()
                return@withContext list.mapNotNull { item ->
                    val catalog = item.badge_catalog
                    val code = catalog?.code ?: item.code
                    val name = catalog?.name ?: item.name
                    val desc = catalog?.description ?: item.description ?: ""
                    val icon = catalog?.icon_url ?: item.icon_url
                    if (code != null && name != null) {
                        ProfileBadge(
                            code = code,
                            name = name,
                            description = desc,
                            iconUrl = icon,
                            awardedAt = item.awarded_at
                        )
                    } else null
                }
            } else {
                Log.w(TAG, "getUserBadges returned code ${res.code()}: ${res.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user badges", e)
        }
        emptyList()
    }

    suspend fun fetchActiveCatalog(): List<BadgeCatalogItem> = withContext(Dispatchers.IO) {
        try {
            val res = SupabaseClient.api.getBadgeCatalog()
            if (res.isSuccessful) {
                return@withContext res.body() ?: emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching badge catalog", e)
        }
        emptyList()
    }

    suspend fun awardBadge(context: Context, targetUserId: String, badgeCode: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val auth = SupabaseSessionManager.getBearerToken(context)
            val req = AwardBadgeRequest(p_user_id = targetUserId, p_badge_code = badgeCode)
            val res = SupabaseClient.api.awardBadge(auth = auth, body = req)
            if (res.isSuccessful) {
                Result.success("Badge '$badgeCode' successfully awarded!")
            } else {
                val err = res.errorBody()?.string() ?: "Failed to award badge (HTTP ${res.code()})"
                Result.failure(Exception(err))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun revokeBadge(context: Context, targetUserId: String, badgeCode: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val auth = SupabaseSessionManager.getBearerToken(context)
            val req = RevokeBadgeRequest(p_user_id = targetUserId, p_badge_code = badgeCode)
            val res = SupabaseClient.api.revokeBadge(auth = auth, body = req)
            if (res.isSuccessful) {
                Result.success("Badge '$badgeCode' successfully revoked.")
            } else {
                val err = res.errorBody()?.string() ?: "Failed to revoke badge (HTTP ${res.code()})"
                Result.failure(Exception(err))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

package com.example.migration

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.SupabaseClient
import com.example.SupabaseProfile
import com.example.SupabaseSessionManager
import com.example.SupabaseVoiceRoom
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

data class FlaggedRoom(
    val roomId: String,
    val title: String,
    val hostId: String,
    val reason: String
)

data class MigrationResult(
    val totalFound: Int,
    val migratedCount: Int,
    val flaggedRooms: List<FlaggedRoom>,
    val logMessages: List<String>
)

object RoomMigrationManager {
    private const val TAG = "RoomMigrationManager"

    suspend fun runMigration(context: Context): MigrationResult = withContext(Dispatchers.IO) {
        val logs = mutableListOf<String>()
        val flagged = mutableListOf<FlaggedRoom>()
        var migrated = 0
        var total = 0

        fun log(msg: String) {
            Log.d(TAG, msg)
            logs.add(msg)
        }

        try {
            log("Starting Firestore to Supabase Voice Rooms migration...")
            val firestore = FirebaseFirestore.getInstance()
            val bearerToken = SupabaseSessionManager.getBearerToken(context)

            // 1. Fetch Supabase profiles to build lookup table
            log("Fetching Supabase profiles for user identity matching...")
            val profilesRes = SupabaseClient.api.getProfile(
                auth = "Bearer ${BuildConfig.SUPABASE_ANON_KEY}",
                authUserIdEq = "neq.00000000-0000-0000-0000-000000000000"
            )
            val supabaseProfiles = profilesRes.body() ?: emptyList()
            log("Loaded ${supabaseProfiles.size} Supabase profiles.")

            // 2. Fetch Firestore users collection to map Firestore UID -> Email
            log("Fetching Firestore users for UID to email resolution...")
            val firestoreUsersSnapshot = try {
                firestore.collection("users").get().await()
            } catch (e: Exception) {
                log("Notice: Firestore users fetch: ${e.message}")
                null
            }
            val fbUidToEmailMap = mutableMapOf<String, String>()
            firestoreUsersSnapshot?.documents?.forEach { doc ->
                val email = doc.getString("email")?.trim()?.lowercase(Locale.ROOT)
                val uid = doc.id
                if (!email.isNullOrBlank()) {
                    fbUidToEmailMap[uid] = email
                }
            }
            log("Cached ${fbUidToEmailMap.size} Firestore user mappings.")

            // 3. Fetch Firestore rooms collection
            log("Fetching Firestore rooms collection...")
            val roomsSnapshot = firestore.collection("rooms").get().await()
            val roomDocs = roomsSnapshot.documents
            total = roomDocs.size
            log("Found $total room documents in Firestore.")

            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }

            for (doc in roomDocs) {
                val roomId = doc.getString("roomId") ?: doc.id
                val title = doc.getString("title") ?: "Untitled Room"
                val rawHostId = doc.getString("hostId") ?: ""

                // Identity resolution: match Firestore hostId to Supabase user UUID
                var matchedSupabaseUserId: String? = null

                // Direct UUID match
                val directMatch = supabaseProfiles.find { it.auth_user_id.equals(rawHostId, ignoreCase = true) }
                if (directMatch != null) {
                    matchedSupabaseUserId = directMatch.auth_user_id
                }

                // Match via Firestore users email
                if (matchedSupabaseUserId == null && fbUidToEmailMap.containsKey(rawHostId)) {
                    val email = fbUidToEmailMap[rawHostId]
                    val profileMatch = supabaseProfiles.find { it.email?.trim()?.equals(email, ignoreCase = true) == true }
                    if (profileMatch != null) {
                        matchedSupabaseUserId = profileMatch.auth_user_id
                    }
                }

                // Match if rawHostId itself is an email
                if (matchedSupabaseUserId == null && rawHostId.contains("@")) {
                    val profileMatch = supabaseProfiles.find { it.email?.trim()?.equals(rawHostId.trim(), ignoreCase = true) == true }
                    if (profileMatch != null) {
                        matchedSupabaseUserId = profileMatch.auth_user_id
                    }
                }

                if (matchedSupabaseUserId == null) {
                    val reason = "Host identifier '$rawHostId' could not be matched to any registered Supabase user."
                    log("[FLAGGED] Room '$title' (ID: $roomId): $reason")
                    flagged.add(FlaggedRoom(roomId = roomId, title = title, hostId = rawHostId, reason = reason))
                    continue
                }

                // Field mapping: camelCase -> snake_case
                val createdAtMillis = doc.getLong("createdAt") ?: System.currentTimeMillis()
                val lastActivityMillis = doc.getLong("lastActivityAt") ?: createdAtMillis

                val previewAvatars = try {
                    @Suppress("UNCHECKED_CAST")
                    (doc.get("previewAvatars") as? List<String>) ?: emptyList()
                } catch (e: Exception) {
                    emptyList()
                }

                val voiceRoom = SupabaseVoiceRoom(
                    title = title,
                    language = doc.getString("language") ?: "English",
                    host_id = matchedSupabaseUserId,
                    participants = doc.getLong("totalParticipants")?.toInt() ?: 1,
                    created_at = isoFormat.format(Date(createdAtMillis)),
                    country_code = doc.getString("languageCode") ?: doc.getString("countryCode") ?: "us",
                    tag = doc.getString("levelTag") ?: doc.getString("tag") ?: "General",
                    room_type = doc.getString("roomType") ?: "voice",
                    description = doc.getString("description") ?: "",
                    banner_image_url = doc.getString("bannerImageUrl") ?: "",
                    card_background_type = doc.getString("cardBackgroundType") ?: "image",
                    host_is_verified = doc.getBoolean("hostIsVerified") ?: false,
                    host_is_vip = doc.getBoolean("hostIsVip") ?: false,
                    is_live = doc.getBoolean("isLive") ?: true,
                    language_code = doc.getString("languageCode") ?: "us",
                    language_flag = doc.getString("languageFlag") ?: "",
                    level_tag = doc.getString("levelTag") ?: "",
                    listening_count = doc.getLong("listeningCount")?.toInt() ?: 0,
                    speaking_count = doc.getLong("speakingCount")?.toInt() ?: 1,
                    preview_avatars = previewAvatars,
                    status = doc.getString("status") ?: "active",
                    status_tag = doc.getString("statusTag") ?: "LIVE",
                    total_visits_24h = doc.getLong("totalVisits24h")?.toInt() ?: 1,
                    last_1h_joins = doc.getLong("last1HourJoins")?.toInt() ?: 0,
                    last_2h_growth_rate = doc.getDouble("last2HourGrowthRate") ?: 0.0,
                    last_activity_at = isoFormat.format(Date(lastActivityMillis))
                )

                val insertRes = SupabaseClient.api.createVoiceRoom(
                    auth = bearerToken,
                    room = voiceRoom
                )

                if (insertRes.isSuccessful) {
                    migrated++
                    log("[MIGRATED] Room '$title' (ID: $roomId) -> Supabase Host: $matchedSupabaseUserId")
                } else {
                    log("[ERROR] Failed to insert room '$title': ${insertRes.code()} ${insertRes.errorBody()?.string()}")
                }
            }

            log("Migration completed: $migrated migrated, ${flagged.size} flagged.")
        } catch (e: Exception) {
            log("Migration encountered exception: ${e.message}")
            Log.e(TAG, "Migration error", e)
        }

        MigrationResult(
            totalFound = total,
            migratedCount = migrated,
            flaggedRooms = flagged,
            logMessages = logs
        )
    }
}

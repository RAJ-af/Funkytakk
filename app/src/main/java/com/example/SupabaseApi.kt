package com.example

import com.squareup.moshi.JsonClass
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

@JsonClass(generateAdapter = true)
data class VerifyFirebaseTokenRequest(
    val firebase_token: String? = null,
    val firebaseIdToken: String? = null
)

@JsonClass(generateAdapter = true)
data class VerifyFirebaseTokenResponse(
    val access_token: String? = null,
    val refresh_token: String? = null,
    val user_id: String? = null,
    val user: SupabaseAuthUser? = null,
    val error: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseAuthUser(
    val id: String? = null,
    val email: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseUser(
    val uid: String, // from Firebase Auth
    val email: String,
    val display_name: String? = null,
    val username: String? = null,
    val avatar: String? = null,
    val dob: String? = null,
    val gender: String? = null,
    val native_language: String? = null,
    val learning_language: String? = null,
    val country: String? = null,
    val country_code: String? = null,
    val hobbies: List<String>? = null,
    val custom_email_verified: Boolean? = null,
    val email_verification_token: String? = null,
    val email_verification_expires_at: String? = null
)

@JsonClass(generateAdapter = true)
data class UpdateEmailVerificationRequest(
    val email_verification_token: String,
    val custom_email_verified: Boolean
)

@JsonClass(generateAdapter = true)
data class SendVerificationRequest(
    val displayName: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseVoiceRoom(
    val id: String? = null,
    val title: String? = null,
    val language: String? = null,
    val host_id: String? = null,
    val participants: Int? = 1,
    val created_at: String? = null,
    val country_code: String? = null,
    val tag: String? = null,
    val room_type: String? = "voice",
    val description: String? = null,
    val banner_image_url: String? = null,
    val card_background_type: String? = "image",
    val host_is_verified: Boolean? = false,
    val host_is_vip: Boolean? = false,
    val is_live: Boolean? = true,
    val language_code: String? = null,
    val language_flag: String? = null,
    val level_tag: String? = null,
    val listening_count: Int? = 0,
    val speaking_count: Int? = 1,
    val preview_avatars: List<String>? = emptyList(),
    val status: String? = "active",
    val status_tag: String? = "LIVE",
    val total_visits_24h: Int? = 1,
    val last_1h_joins: Int? = 0,
    val last_2h_growth_rate: Double? = 0.0,
    val last_activity_at: String? = null,
    val is_private: Boolean? = false,
    val join_code: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseRoomParticipant(
    val room_id: String,
    val user_id: String,
    val role: String = "speaker",
    val is_mic_enabled: Boolean = true,
    val joined_at: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseEvent(
    val id: String? = null,
    val title: String = "",
    val description: String? = null,
    val category_tag: String? = null,
    val banner_image_url: String? = null,
    val event_time: String? = null,
    val timezone_label: String? = null,
    val join_room_id: String? = null,
    val join_url: String? = null,
    val is_active: Boolean? = true
)

@JsonClass(generateAdapter = true)
data class ModeratorActionRequest(
    val target_user_id: String,
    val action_type: String,
    val room_id: String? = null,
    val reason: String = ""
)

@JsonClass(generateAdapter = true)
data class SendLoginNotificationRequest(
    val email: String,
    val displayName: String?,
    val deviceType: String?,
    val timestamp: String
)

@JsonClass(generateAdapter = true)
data class GeminiProxyRequest(
    val prompt: String,
    val model: String? = "gemini-2.5-flash",
    val systemInstruction: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiProxyResponse(
    val success: Boolean? = null,
    val text: String? = null,
    val error: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseProfile(
    val id: Long? = null,
    val auth_user_id: String? = null,
    val username: String? = null,
    val profile_name: String? = null,
    val avatar_url: String? = null,
    val age: Int? = null,
    val gender: String? = null,
    val country: String? = null,
    val country_code: String? = null,
    val bio: String? = null,
    val email: String? = null,
    val native_languages: List<String>? = null,
    val learning_languages: List<String>? = null,
    val hobbies: List<String>? = null,
    val onboarding_complete: Boolean? = null
)

interface SupabaseApi {

    @POST("functions/v1/verify-firebase-token")
    suspend fun verifyFirebaseToken(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Body request: VerifyFirebaseTokenRequest
    ): Response<VerifyFirebaseTokenResponse>

    @PATCH("rest/v1/users")
    suspend fun updateEmailVerification(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Query("uid") uidEq: String, // e.g. "eq.<uid>"
        @Body request: UpdateEmailVerificationRequest
    ): Response<Unit>

    @POST("functions/v1/send-verification-email")
    suspend fun sendVerificationEmail(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body request: SendVerificationRequest
    ): Response<Unit>

    @POST("functions/v1/gemini-proxy")
    suspend fun callGeminiProxy(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String = "Bearer ${BuildConfig.SUPABASE_ANON_KEY}",
        @Body request: GeminiProxyRequest
    ): Response<GeminiProxyResponse>

    @POST("functions/v1/send-login-notification")
    suspend fun sendLoginNotification(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body request: SendLoginNotificationRequest
    ): Response<Unit>

    @POST("rest/v1/users")
    suspend fun upsertUser(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String = "Bearer ${BuildConfig.SUPABASE_ANON_KEY}",
        @Header("Prefer") prefer: String = "resolution=merge-duplicates",
        @Body user: SupabaseUser
    ): Response<Unit>

    @Multipart
    @POST("storage/v1/object/avatars/{filename}")
    suspend fun uploadAvatar(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String = "Bearer ${BuildConfig.SUPABASE_ANON_KEY}",
        @Path("filename") filename: String,
        @Part file: MultipartBody.Part
    ): Response<Unit>

    @GET("rest/v1/users?select=*")
    suspend fun getUser(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String = "Bearer ${BuildConfig.SUPABASE_ANON_KEY}",
        @Query("uid") uidEq: String // e.g. "eq.123-abc"
    ): Response<List<SupabaseUser>>

    @GET("rest/v1/users?select=uid,username")
    suspend fun getUserByUsername(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String = "Bearer ${BuildConfig.SUPABASE_ANON_KEY}",
        @Query("username") usernameEq: String // e.g. "ilike.Raj" or "eq.Raj"
    ): Response<List<SupabaseUser>>

    @GET("rest/v1/profiles?select=*")
    suspend fun getProfileByUsername(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String = "Bearer ${BuildConfig.SUPABASE_ANON_KEY}",
        @Query("username") usernameEq: String // e.g. "ilike.Raj" or "eq.Raj"
    ): Response<List<SupabaseProfile>>

    @GET("rest/v1/profiles")
    suspend fun getAllProfiles(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String = "Bearer ${BuildConfig.SUPABASE_ANON_KEY}"
    ): Response<List<SupabaseProfile>>

    @GET("rest/v1/users")
    suspend fun getAllUsers(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String = "Bearer ${BuildConfig.SUPABASE_ANON_KEY}"
    ): Response<List<SupabaseUser>>

    @GET("rest/v1/profiles")
    suspend fun getProfile(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Query("auth_user_id") authUserIdEq: String // e.g. "eq.<uuid>"
    ): Response<List<SupabaseProfile>>

    @PATCH("rest/v1/profiles")
    suspend fun patchProfile(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Query("auth_user_id") authUserIdEq: String, // e.g. "eq.<uuid>"
        @Body updates: Map<String, @JvmSuppressWildcards Any?>
    ): Response<Unit>

    @POST("rest/v1/profiles")
    suspend fun upsertProfile(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Header("Prefer") prefer: String = "resolution=merge-duplicates",
        @Body profile: SupabaseProfile
    ): Response<Unit>

    @GET("rest/v1/voice_rooms?select=*&order=created_at.desc")
    suspend fun getVoiceRooms(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String = "Bearer ${BuildConfig.SUPABASE_ANON_KEY}"
    ): Response<List<SupabaseVoiceRoom>>

    @GET("rest/v1/voice_rooms?select=*")
    suspend fun getVoiceRoomById(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String = "Bearer ${BuildConfig.SUPABASE_ANON_KEY}",
        @Query("id") idEq: String // e.g. "eq.<uuid>"
    ): Response<List<SupabaseVoiceRoom>>

    @POST("rest/v1/voice_rooms")
    suspend fun createVoiceRoom(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Header("Prefer") prefer: String = "return=representation",
        @Body room: SupabaseVoiceRoom
    ): Response<List<SupabaseVoiceRoom>>

    @PATCH("rest/v1/voice_rooms")
    suspend fun updateVoiceRoom(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Query("id") idEq: String,
        @Body updates: Map<String, @JvmSuppressWildcards Any?>
    ): Response<Unit>

    @GET("rest/v1/room_participants?select=*")
    suspend fun getRoomParticipants(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String = "Bearer ${BuildConfig.SUPABASE_ANON_KEY}",
        @Query("room_id") roomIdEq: String // e.g. "eq.<uuid>"
    ): Response<List<SupabaseRoomParticipant>>

    @GET("rest/v1/room_participants?select=*")
    suspend fun getAllRoomParticipants(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String = "Bearer ${BuildConfig.SUPABASE_ANON_KEY}"
    ): Response<List<SupabaseRoomParticipant>>

    @GET("rest/v1/room_participants?select=*")
    suspend fun getUserRoomParticipants(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String = "Bearer ${BuildConfig.SUPABASE_ANON_KEY}",
        @Query("user_id") userIdEq: String // e.g. "eq.<uuid>"
    ): Response<List<SupabaseRoomParticipant>>

    @POST("rest/v1/room_participants")
    suspend fun joinRoomParticipant(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Header("Prefer") prefer: String = "resolution=merge-duplicates",
        @Body participant: SupabaseRoomParticipant
    ): Response<Unit>

    @DELETE("rest/v1/room_participants")
    suspend fun leaveRoomParticipant(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Query("room_id") roomIdEq: String,
        @Query("user_id") userIdEq: String
    ): Response<Unit>

    @POST("rest/v1/rpc/execute_moderator_action")
    suspend fun executeModeratorAction(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body request: ModeratorActionRequest
    ): Response<Map<String, @JvmSuppressWildcards Any?>>

    @GET("rest/v1/events?select=*&is_active=eq.true&order=event_time.asc")
    suspend fun getActiveEvents(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String = "Bearer ${BuildConfig.SUPABASE_ANON_KEY}"
    ): Response<List<SupabaseEvent>>

    @POST("rest/v1/rpc/join_room_by_code")
    suspend fun joinRoomByCode(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: Map<String, String>
    ): Response<ResponseBody>
}

@JsonClass(generateAdapter = true)
data class JoinRoomResponse(
    val room_id: String,
    val title: String
)


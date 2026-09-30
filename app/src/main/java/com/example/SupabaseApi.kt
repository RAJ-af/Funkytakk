package com.example

import com.squareup.moshi.JsonClass
import okhttp3.MultipartBody
import okhttp3.RequestBody
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
    val join_code: String? = null,
    val host_left_at: String? = null,
    val ended_at: String? = null
)

@JsonClass(generateAdapter = true)
data class UpdateVoiceRoomRequest(
    val title: String? = null,
    val description: String? = null,
    val language: String? = null,
    val country_code: String? = null,
    val language_code: String? = null,
    val language_flag: String? = null,
    val tag: String? = null,
    val is_private: Boolean? = null,
    val join_code: String? = null,
    val speaking_count: Int? = null,
    val listening_count: Int? = null,
    val participants: Int? = null,
    val is_live: Boolean? = null,
    val status: String? = null,
    val host_left_at: String? = null,
    val ended_at: String? = null
)

@JsonClass(generateAdapter = true)
data class UpdateParticipantMicRequest(
    val is_mic_enabled: Boolean
)

@JsonClass(generateAdapter = true)
data class SpeakerRequestInsert(
    val room_id: String,
    val user_id: String,
    val status: String = "pending"
)

@JsonClass(generateAdapter = true)
data class SupabaseSpeakerRequest(
    val id: String,
    val room_id: String,
    val user_id: String,
    val status: String = "pending",
    val created_at: String? = null
)

@JsonClass(generateAdapter = true)
data class RespondSpeakerRequestBody(
    val request_id: String,
    val accept: Boolean
)

@JsonClass(generateAdapter = true)
data class SupabaseRoomParticipant(
    val room_id: String,
    val user_id: String,
    val role: String = "listener",
    val is_mic_enabled: Boolean = false,
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
data class SupabaseRoomMessage(
    val id: String? = null,
    val room_id: String? = null,
    val user_id: String? = null,
    val message: String? = null,
    val created_at: String? = null,
    val message_type: String? = null
)

@JsonClass(generateAdapter = true)
data class SendRoomMessageRequest(
    val room_id: String,
    val user_id: String,
    val message: String,
    val message_type: String? = null
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
    val cover_url: String? = null,
    val age: Int? = null,
    val gender: String? = null,
    val country: String? = null,
    val country_code: String? = null,
    val city: String? = null,
    val bio: String? = null,
    val email: String? = null,
    val native_languages: List<String>? = null,
    val learning_languages: List<String>? = null,
    val hobbies: List<String>? = null,
    val onboarding_complete: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class DiscoverableProfile(
    val auth_user_id: String,
    val username: String? = null,
    val profile_name: String? = null,
    val avatar_url: String? = null,
    val cover_url: String? = null,
    val age: Int? = null,
    val gender: String? = null,
    val native_languages: List<String>? = null,
    val learning_languages: List<String>? = null,
    val country: String? = null,
    val country_code: String? = null,
    val city: String? = null,
    val hobbies: List<String>? = null,
    val bio: String? = null,
    val is_online: Boolean? = null,
    val last_seen: String? = null,
    val created_at: String? = null,
    val is_verified: Boolean? = false
)

@JsonClass(generateAdapter = true)
data class BadgeCatalogItem(
    val id: Long? = null,
    val code: String,
    val name: String,
    val description: String? = null,
    val icon_url: String? = null,
    val is_active: Boolean? = true
)

@JsonClass(generateAdapter = true)
data class UserBadgeItem(
    val id: Long? = null,
    val badge_id: Long? = null,
    val user_id: String? = null,
    val awarded_at: String? = null,
    val awarded_by: String? = null,
    val badge_catalog: BadgeCatalogItem? = null,
    val name: String? = null,
    val description: String? = null,
    val icon_url: String? = null,
    val code: String? = null
)

@JsonClass(generateAdapter = true)
data class AwardBadgeRequest(
    val p_user_id: String,
    val p_badge_code: String
)

@JsonClass(generateAdapter = true)
data class RevokeBadgeRequest(
    val p_user_id: String,
    val p_badge_code: String
)

@JsonClass(generateAdapter = true)
data class SupabaseMoment(
    val id: String,
    val user_id: String? = null,
    val content: String? = null,
    val image_urls: List<String>? = null,
    val likes_count: Int? = 0,
    val comments_count: Int? = 0,
    val created_at: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseFollower(
    val id: String? = null,
    val follower_id: String,
    val following_id: String,
    val created_at: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseMomentLike(
    val id: String? = null,
    val moment_id: String,
    val user_id: String,
    val created_at: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseMomentComment(
    val id: String? = null,
    val moment_id: String,
    val user_id: String,
    val content: String,
    val created_at: String? = null
)

@JsonClass(generateAdapter = true)
data class CloudinarySignatureResponse(
    val signature: String,
    val timestamp: Long,
    val api_key: String,
    val cloud_name: String,
    val folder: String
)

@JsonClass(generateAdapter = true)
data class CloudinaryUploadResponse(
    val secure_url: String? = null
)

@JsonClass(generateAdapter = true)
data class UploadToAppwriteResponse(
    val ok: Boolean? = null,
    val file_id: String? = null,
    val url: String? = null,
    val error: String? = null,
    val message: String? = null,
    val detail: String? = null
)

interface SupabaseApi {

    @Multipart
    @POST("functions/v1/upload-to-appwrite")
    suspend fun uploadToAppwrite(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Part file: MultipartBody.Part,
        @Part("purpose") purpose: RequestBody,
        @Part("folder_suffix") folderSuffix: RequestBody? = null
    ): Response<UploadToAppwriteResponse>

    @POST("functions/v1/verify-firebase-token")
    suspend fun verifyFirebaseToken(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Body request: VerifyFirebaseTokenRequest
    ): Response<VerifyFirebaseTokenResponse>

    @POST("functions/v1/get-cloudinary-signature")
    suspend fun getCloudinarySignature(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String
    ): Response<CloudinarySignatureResponse>

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
        @Header("x-upsert") upsert: String = "true",
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

    @GET("rest/v1/discoverable_profiles?select=*")
    suspend fun getDiscoverableProfiles(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @QueryMap options: Map<String, String>
    ): Response<List<DiscoverableProfile>>

    @GET("rest/v1/moments?select=id,user_id,content,image_urls,likes_count,comments_count,created_at")
    suspend fun getMomentsByUserId(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Query("user_id") userIdEq: String, // e.g. "eq.<uuid>"
        @Query("order") order: String = "created_at.desc"
    ): Response<List<SupabaseMoment>>

    @GET("rest/v1/moments?select=id,user_id,content,image_urls,likes_count,comments_count,created_at")
    suspend fun getMomentById(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Query("id") idEq: String // e.g. "eq.<id>"
    ): Response<List<SupabaseMoment>>

    @GET("rest/v1/moments?select=id")
    suspend fun countMomentsByUserId(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Header("Prefer") prefer: String = "count=exact",
        @Query("user_id") userIdEq: String // e.g. "eq.<uuid>"
    ): Response<List<Map<String, Any>>>

    @GET("rest/v1/followers?select=*")
    suspend fun getFollowers(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String = "Bearer ${BuildConfig.SUPABASE_ANON_KEY}",
        @Query("follower_id") followerIdEq: String? = null,
        @Query("following_id") followingIdEq: String? = null
    ): Response<List<SupabaseFollower>>

    @GET("rest/v1/followers?select=id")
    suspend fun countFollowers(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String = "Bearer ${BuildConfig.SUPABASE_ANON_KEY}",
        @Header("Prefer") prefer: String = "count=exact",
        @Query("following_id") followingIdEq: String // e.g. "eq.<profileId>"
    ): Response<List<Map<String, Any>>>

    @GET("rest/v1/followers?select=id")
    suspend fun countFollowing(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String = "Bearer ${BuildConfig.SUPABASE_ANON_KEY}",
        @Header("Prefer") prefer: String = "count=exact",
        @Query("follower_id") followerIdEq: String // e.g. "eq.<profileId>"
    ): Response<List<Map<String, Any>>>

    @POST("rest/v1/followers")
    suspend fun followUser(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Header("Prefer") prefer: String = "return=representation",
        @Body follower: SupabaseFollower
    ): Response<List<SupabaseFollower>>

    @GET("rest/v1/moments?select=id,user_id,content,image_urls,likes_count,comments_count,created_at&order=created_at.desc")
    suspend fun getAllMoments(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String
    ): Response<List<SupabaseMoment>>

    @POST("rest/v1/moments")
    suspend fun createMoment(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Header("Prefer") prefer: String = "return=representation",
        @Body moment: Map<String, @JvmSuppressWildcards Any?>
    ): Response<List<SupabaseMoment>>

    @GET("rest/v1/moment_likes?select=*")
    suspend fun getMomentLikes(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Query("moment_id") momentIdEq: String,
        @Query("user_id") userIdEq: String
    ): Response<List<SupabaseMomentLike>>

    @POST("rest/v1/moment_likes")
    suspend fun likeMoment(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Header("Prefer") prefer: String = "return=representation",
        @Body like: SupabaseMomentLike
    ): Response<List<SupabaseMomentLike>>

    @DELETE("rest/v1/moment_likes")
    suspend fun unlikeMoment(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Query("moment_id") momentIdEq: String,
        @Query("user_id") userIdEq: String
    ): Response<Unit>

    @GET("rest/v1/moment_comments?select=*&order=created_at.asc")
    suspend fun getMomentComments(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Query("moment_id") momentIdEq: String
    ): Response<List<SupabaseMomentComment>>

    @POST("rest/v1/moment_comments")
    suspend fun postMomentComment(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Header("Prefer") prefer: String = "return=representation",
        @Body comment: SupabaseMomentComment
    ): Response<List<SupabaseMomentComment>>

    @DELETE("rest/v1/moment_comments")
    suspend fun deleteMomentComment(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Query("id") idEq: String,
        @Query("user_id") userIdEq: String
    ): Response<Unit>

    @DELETE("rest/v1/followers")
    suspend fun unfollowUser(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Query("follower_id") followerIdEq: String, // e.g. "eq.<self>"
        @Query("following_id") followingIdEq: String // e.g. "eq.<profileId>"
    ): Response<Unit>

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

    @GET("rest/v1/voice_rooms?select=*&is_live=eq.true&order=created_at.desc")
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
        @Header("Prefer") prefer: String = "return=representation",
        @Query("id") idEq: String,
        @Body updates: UpdateVoiceRoomRequest
    ): Response<ResponseBody>

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

    @PATCH("rest/v1/room_participants")
    suspend fun updateRoomParticipantMic(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Header("Prefer") prefer: String = "return=representation",
        @Query("room_id") roomIdEq: String,
        @Query("user_id") userIdEq: String,
        @Body body: UpdateParticipantMicRequest
    ): Response<ResponseBody>

    @POST("rest/v1/speaker_requests")
    suspend fun createSpeakerRequest(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Header("Prefer") prefer: String = "return=representation",
        @Body body: SpeakerRequestInsert
    ): Response<ResponseBody>

    @GET("rest/v1/speaker_requests?select=*&status=eq.pending")
    suspend fun getPendingSpeakerRequests(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Query("room_id") roomIdEq: String
    ): Response<List<SupabaseSpeakerRequest>>

    @DELETE("rest/v1/speaker_requests")
    suspend fun deletePendingSpeakerRequest(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Query("room_id") roomIdEq: String,
        @Query("user_id") userIdEq: String,
        @Query("status") statusEq: String = "eq.pending"
    ): Response<ResponseBody>

    @POST("rest/v1/rpc/leave_voice_room")
    suspend fun leaveVoiceRoom(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: Map<String, String> // {"p_room_id": roomId}
    ): Response<ResponseBody>

    @POST("rest/v1/rpc/host_rejoin_voice_room")
    suspend fun hostRejoinVoiceRoom(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: Map<String, String> // {"p_room_id": roomId}
    ): Response<ResponseBody>

    @POST("rest/v1/rpc/end_voice_room")
    suspend fun endVoiceRoom(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: Map<String, String> // {"p_room_id": roomId}
    ): Response<ResponseBody>

    @POST("rest/v1/rpc/request_speaker_seat")
    suspend fun requestSpeakerSeat(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: Map<String, String> // {"p_room_id": roomId}
    ): Response<ResponseBody>

    @POST("rest/v1/rpc/cancel_speaker_request")
    suspend fun cancelSpeakerRequest(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: Map<String, String> // {"p_room_id": roomId}
    ): Response<ResponseBody>

    @POST("rest/v1/rpc/check_and_cleanup_ended_rooms")
    suspend fun checkAndCleanupEndedRooms(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: Map<String, String> = emptyMap()
    ): Response<ResponseBody>

    @POST("rest/v1/rpc/respond_to_speaker_request")
    suspend fun respondToSpeakerRequest(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: RespondSpeakerRequestBody
    ): Response<ResponseBody>

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

    @GET("rest/v1/room_messages?select=*&order=created_at.asc")
    suspend fun getRoomMessages(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String = "Bearer ${BuildConfig.SUPABASE_ANON_KEY}",
        @Query("room_id") roomIdEq: String // e.g. "eq.<uuid>"
    ): Response<List<SupabaseRoomMessage>>

    @POST("rest/v1/room_messages")
    suspend fun sendRoomMessage(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Header("Prefer") prefer: String = "return=representation",
        @Body request: SendRoomMessageRequest
    ): Response<List<SupabaseRoomMessage>>

    @POST("rest/v1/rpc/join_room_by_code")
    suspend fun joinRoomByCode(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: Map<String, String>
    ): Response<ResponseBody>

    @POST("rest/v1/rpc/admin_apply_moderation_action")
    suspend fun adminApplyModerationAction(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: AdminApplyModerationActionRequest
    ): Response<ResponseBody>

    @POST("rest/v1/rpc/admin_revoke_moderation_action")
    suspend fun adminRevokeModerationAction(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: AdminRevokeModerationActionRequest
    ): Response<ResponseBody>

    @DELETE("rest/v1/user_moderation_status")
    suspend fun deleteUserModerationStatus(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Query("user_id") userIdEq: String
    ): Response<ResponseBody>

    @POST("rest/v1/rpc/get_my_moderation_status")
    suspend fun getMyModerationStatus(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: Map<String, String> = emptyMap()
    ): Response<ResponseBody>

    @GET("rest/v1/user_moderation_status?select=*")
    suspend fun getUserModerationStatus(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Query("user_id") userIdEq: String
    ): Response<ResponseBody>

    @GET("rest/v1/user_moderation_status?select=*&order=started_at.desc")
    suspend fun getAllActiveModerationStatuses(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String
    ): Response<List<MyModerationStatus>>

    @POST("rest/v1/room_reports")
    suspend fun createRoomReport(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Header("Prefer") prefer: String = "return=minimal",
        @Body report: CreateRoomReportRequest
    ): Response<ResponseBody>

    @GET("rest/v1/user_roles?select=user_id,role")
    suspend fun getMyUserRoles(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String
    ): Response<List<UserRoleItem>>

    @GET("rest/v1/room_reports?select=*&order=created_at.desc")
    suspend fun getRoomReports(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String
    ): Response<List<RoomReportItem>>

    @PATCH("rest/v1/room_reports")
    suspend fun updateRoomReportStatus(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Query("id") idFilter: String,
        @Body body: Map<String, String>
    ): Response<ResponseBody>

    @DELETE("rest/v1/room_reports")
    suspend fun deleteRoomReport(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Query("id") idFilter: String
    ): Response<ResponseBody>

    @Multipart
    @POST("storage/v1/object/{bucket}/{filename}")
    suspend fun uploadStorageFile(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String = "Bearer ${BuildConfig.SUPABASE_ANON_KEY}",
        @Header("x-upsert") upsert: String = "true",
        @Path("bucket") bucket: String,
        @Path("filename") filename: String,
        @Part file: MultipartBody.Part
    ): Response<ResponseBody>

    @POST("storage/v1/object/{bucket}/{path}")
    suspend fun uploadStorageRawBytes(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String = "Bearer ${BuildConfig.SUPABASE_ANON_KEY}",
        @Header("x-upsert") upsert: String = "true",
        @Path("bucket") bucket: String,
        @Path(value = "path", encoded = true) path: String,
        @Body body: RequestBody
    ): Response<ResponseBody>

    @GET("rest/v1/badge_catalog?select=*&is_active=eq.true&order=id.asc")
    suspend fun getBadgeCatalog(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String = "Bearer ${BuildConfig.SUPABASE_ANON_KEY}"
    ): Response<List<BadgeCatalogItem>>

    @GET("rest/v1/user_badges?select=id,badge_id,user_id,awarded_at,awarded_by,badge_catalog(id,code,name,description,icon_url)&order=awarded_at.asc")
    suspend fun getUserBadges(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String = "Bearer ${BuildConfig.SUPABASE_ANON_KEY}",
        @Query("user_id") userIdEq: String // e.g. "eq.<userId>"
    ): Response<List<UserBadgeItem>>

    @POST("rest/v1/rpc/award_badge")
    suspend fun awardBadge(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: AwardBadgeRequest
    ): Response<ResponseBody>

    @POST("rest/v1/rpc/revoke_badge")
    suspend fun revokeBadge(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: RevokeBadgeRequest
    ): Response<ResponseBody>

    @Multipart
    @POST("storage/v1/object/covers/{path}")
    suspend fun uploadCoverMultipart(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Header("x-upsert") upsert: String = "true",
        @Path(value = "path", encoded = true) path: String,
        @Part file: MultipartBody.Part
    ): Response<ResponseBody>

    @POST("storage/v1/object/covers/{path}")
    suspend fun uploadCoverRawBytes(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Header("x-upsert") upsert: String = "true",
        @Path(value = "path", encoded = true) path: String,
        @Body body: RequestBody
    ): Response<ResponseBody>

    @POST("storage/v1/object/sign/{bucket}/{filename}")
    suspend fun createSignedUrl(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Path("bucket") bucket: String,
        @Path(value = "filename", encoded = true) filename: String,
        @Body body: CreateSignedUrlRequest = CreateSignedUrlRequest()
    ): Response<CreateSignedUrlResponse>

    @GET("storage/v1/object/authenticated/{bucket}/{filename}")
    suspend fun downloadStorageObject(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Path("bucket") bucket: String,
        @Path(value = "filename", encoded = true) filename: String
    ): Response<ResponseBody>

    // ----------------------------------------------------
    // IN-APP NOTIFICATIONS (Feature 1)
    // ----------------------------------------------------
    @GET("rest/v1/notifications?select=*&order=created_at.desc")
    suspend fun getNotifications(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String
    ): Response<List<SupabaseNotification>>

    @PATCH("rest/v1/notifications")
    suspend fun updateNotificationRead(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Header("Prefer") prefer: String = "return=minimal",
        @Query("id") idFilter: String, // format: "eq.<uuid>"
        @Body body: UpdateNotificationReadRequest
    ): Response<ResponseBody>

    // ----------------------------------------------------
    // 1:1 SUPPORT CHAT (Feature 2)
    // ----------------------------------------------------
    @GET("rest/v1/support_conversations?select=*&limit=1")
    suspend fun getMySupportConversation(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String
    ): Response<List<SupabaseSupportConversation>>

    @GET("rest/v1/support_messages?select=*&order=created_at.asc")
    suspend fun getSupportMessages(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Query("conversation_id") conversationIdFilter: String // format: "eq.<conversation_id>"
    ): Response<List<SupabaseSupportMessage>>

    @POST("rest/v1/rpc/send_support_message")
    suspend fun sendSupportMessage(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: SendSupportMessageRequest
    ): Response<ResponseBody>

    @POST("rest/v1/rpc/mark_support_conversation_read")
    suspend fun markSupportConversationRead(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: Map<String, String> = emptyMap()
    ): Response<ResponseBody>

    @GET("rest/v1/support_conversations?select=*&order=last_message_at.desc")
    suspend fun getAllSupportConversations(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String
    ): Response<List<SupabaseSupportConversation>>

    @POST("rest/v1/rpc/admin_send_support_message")
    suspend fun adminSendSupportMessage(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: AdminSendSupportMessageRequest
    ): Response<ResponseBody>

    @POST("rest/v1/rpc/admin_mark_support_conversation_read")
    suspend fun adminMarkSupportConversationRead(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: AdminMarkSupportConversationReadRequest
    ): Response<ResponseBody>

    // ----------------------------------------------------
    // E2EE 1:1 USER CHAT (PART 1 & 2)
    // ----------------------------------------------------
    @GET("rest/v1/user_public_keys")
    suspend fun getUserPublicKey(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Query("user_id") userIdFilter: String
    ): Response<List<UserPublicKeyItem>>

    @POST("rest/v1/user_public_keys")
    suspend fun upsertUserPublicKey(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Header("Prefer") prefer: String = "resolution=merge-duplicates",
        @Body body: UpsertUserPublicKeyRequest
    ): Response<ResponseBody>

    @POST("rest/v1/rpc/get_or_create_dm_conversation")
    suspend fun getOrCreateDmConversation(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: GetOrCreateDmConversationRequest
    ): Response<ResponseBody>

    @GET("rest/v1/dm_messages?select=*&order=created_at.asc")
    suspend fun getPendingDmMessages(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Query("recipient_id") recipientIdFilter: String
    ): Response<List<SupabaseDmMessage>>

    @POST("rest/v1/dm_messages")
    suspend fun sendDmMessage(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Header("Prefer") prefer: String = "return=minimal",
        @Body body: SendDmMessageRequest
    ): Response<ResponseBody>

    @DELETE("rest/v1/dm_messages")
    suspend fun deleteDmMessage(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Query("id") idFilter: String
    ): Response<ResponseBody>

    @POST("storage/v1/object/dm-media/{path}")
    suspend fun uploadDmMedia(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Header("x-upsert") upsert: String = "true",
        @Path(value = "path", encoded = true) path: String,
        @Body body: RequestBody
    ): Response<ResponseBody>

    @Multipart
    @POST("storage/v1/object/dm-media/{path}")
    suspend fun uploadDmMediaMultipart(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Header("x-upsert") upsert: String = "true",
        @Path(value = "path", encoded = true) path: String,
        @Part file: MultipartBody.Part
    ): Response<ResponseBody>

    @GET("storage/v1/object/authenticated/dm-media/{path}")
    suspend fun downloadDmMediaAuthenticated(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Path(value = "path", encoded = true) path: String
    ): Response<ResponseBody>

    @GET("storage/v1/object/dm-media/{path}")
    suspend fun downloadDmMedia(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Path(value = "path", encoded = true) path: String
    ): Response<ResponseBody>

    @GET
    suspend fun downloadFromUrl(
        @Url url: String,
        @Header("apikey") apiKey: String? = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String? = null
    ): Response<ResponseBody>

    @DELETE("storage/v1/object/dm-media/{path}")
    suspend fun deleteDmMedia(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Path(value = "path", encoded = true) path: String
    ): Response<ResponseBody>

    @POST("rest/v1/rpc/heartbeat")
    suspend fun heartbeat(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: Map<String, String> = emptyMap()
    ): Response<ResponseBody>

    @POST("rest/v1/rpc/go_offline")
    suspend fun goOffline(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: Map<String, String> = emptyMap()
    ): Response<ResponseBody>

    // ----------------------------------------------------
    // GIFTING & WALLET
    // ----------------------------------------------------
    @GET("rest/v1/gift_catalog?select=*&is_active=eq.true&order=price_coins.asc")
    suspend fun getActiveGiftCatalog(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String
    ): Response<List<GiftCatalogItem>>

    @POST("rest/v1/rpc/get_or_create_wallet")
    suspend fun getOrCreateWallet(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: Map<String, String> = emptyMap()
    ): Response<ResponseBody>

    @POST("rest/v1/rpc/send_gift")
    suspend fun sendGift(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: SendGiftRequest
    ): Response<ResponseBody>

    @GET("rest/v1/gift_transactions?select=*&order=created_at.desc")
    suspend fun getGiftTransactions(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Query("recipient_id") recipientIdFilter: String? = null,
        @Query("sender_id") senderIdFilter: String? = null
    ): Response<List<GiftTransactionItem>>

    // ----------------------------------------------------
    // GROUP CHAT (E2EE FAN-OUT)
    // ----------------------------------------------------
    @POST("rest/v1/rpc/create_group")
    suspend fun createGroup(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: CreateGroupRequest
    ): Response<ResponseBody>

    @POST("rest/v1/rpc/accept_group_invite")
    suspend fun acceptGroupInvite(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: AcceptGroupInviteRequest
    ): Response<ResponseBody>

    @POST("rest/v1/rpc/decline_group_invite")
    suspend fun declineGroupInvite(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: DeclineGroupInviteRequest
    ): Response<ResponseBody>

    @POST("rest/v1/rpc/add_group_member")
    suspend fun addGroupMember(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: AddGroupMemberRequest
    ): Response<ResponseBody>

    @POST("rest/v1/rpc/remove_group_member")
    suspend fun removeGroupMember(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: RemoveGroupMemberRequest
    ): Response<ResponseBody>

    @POST("rest/v1/rpc/send_group_message")
    suspend fun sendGroupMessage(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: SendGroupMessageRequest
    ): Response<ResponseBody>

    @GET("rest/v1/group_conversations?select=*")
    suspend fun getGroupConversations(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Query("id") idFilter: String? = null
    ): Response<List<SupabaseGroupConversation>>

    @GET("rest/v1/group_members?select=*")
    suspend fun getGroupMembers(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Query("group_id") groupIdFilter: String? = null,
        @Query("user_id") userIdFilter: String? = null,
        @Query("status") statusFilter: String? = null
    ): Response<List<SupabaseGroupMember>>

    @GET("rest/v1/group_message_recipients?select=*&order=created_at.asc")
    suspend fun getPendingGroupMessageRecipients(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Query("recipient_id") recipientIdFilter: String
    ): Response<List<SupabaseGroupRecipientRow>>

    @GET("rest/v1/group_messages?select=*")
    suspend fun getGroupMessagesByIds(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Query("id") idFilter: String
    ): Response<List<SupabaseGroupMessageMeta>>

    @DELETE("rest/v1/group_message_recipients")
    suspend fun deleteGroupMessageRecipient(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Query("id") idFilter: String
    ): Response<ResponseBody>

    @POST("storage/v1/object/group-media/{path}")
    suspend fun uploadGroupMedia(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Header("x-upsert") upsert: String = "true",
        @Path(value = "path", encoded = true) path: String,
        @Body body: RequestBody
    ): Response<ResponseBody>

    @GET("storage/v1/object/group-media/{path}")
    suspend fun downloadGroupMedia(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Path(value = "path", encoded = true) path: String
    ): Response<ResponseBody>

    // ----------------------------------------------------
    // GENERALIZED REPORTING
    // ----------------------------------------------------
    @POST("rest/v1/rpc/report_user")
    suspend fun reportUser(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: ReportUserRequest
    ): Response<ResponseBody>
}

@JsonClass(generateAdapter = true)
data class CreateSignedUrlRequest(
    val expiresIn: Int = 3600
)

@JsonClass(generateAdapter = true)
data class CreateSignedUrlResponse(
    val signedURL: String? = null,
    val signedUrl: String? = null,
    val error: String? = null,
    val message: String? = null,
    val statusCode: String? = null
) {
    fun getResolvedPath(): String? = signedURL ?: signedUrl
}

@JsonClass(generateAdapter = true)
data class AdminApplyModerationActionRequest(
    val p_report_id: String,
    val p_target_user_id: String,
    val p_action_type: String, // "suspend", "warning", "mute"
    val p_duration_hours: Int? = null,
    val p_reason: String? = null
)

@JsonClass(generateAdapter = true)
data class AdminRevokeModerationActionRequest(
    val p_target_user_id: String,
    val p_reason: String? = null
)

@JsonClass(generateAdapter = true)
data class MyModerationStatus(
    val user_id: String? = null,
    val action_type: String? = null,
    val started_at: String? = null,
    val expires_at: String? = null,
    val reason: String? = null,
    val report_id: String? = null
)

@JsonClass(generateAdapter = true)
data class RoomReportItem(
    val id: String? = null,
    val room_id: String? = null,
    val reporter_id: String? = null,
    val reported_user_id: String? = null,
    val context_type: String? = "room",
    val context_id: String? = null,
    val category: String? = null,
    val reason: String? = null,
    val message_snapshot: List<ReportMessageSnapshot>? = null,
    val speaker_log: List<ReportSpeakerLogItem>? = null,
    val screenshot_url: String? = null,
    val audio_clip_url: String? = null,
    val additional_details: String? = null,
    val status: String? = "pending",
    val created_at: String? = null
)

@JsonClass(generateAdapter = true)
data class CreateRoomReportRequest(
    val room_id: String,
    val reporter_id: String,
    val category: String,
    val reason: String? = null,
    val message_snapshot: List<ReportMessageSnapshot>? = null,
    val speaker_log: List<ReportSpeakerLogItem>? = null,
    val screenshot_url: String? = null,
    val audio_clip_url: String? = null,
    val additional_details: String? = null,
    val status: String = "pending"
)

@JsonClass(generateAdapter = true)
data class ReportMessageSnapshot(
    val sender: String = "",
    val text: String = "",
    val timestamp: String = ""
)

@JsonClass(generateAdapter = true)
data class ReportSpeakerLogItem(
    val user_id: String = "",
    val name: String = "",
    val role: String = "listener",
    val is_mic_enabled: Boolean = false
)

@JsonClass(generateAdapter = true)
data class JoinRoomResponse(
    val room_id: String,
    val title: String
)

@JsonClass(generateAdapter = true)
data class UserRoleItem(
    val user_id: String? = null,
    val role: String? = null
)

// ----------------------------------------------------
// IN-APP NOTIFICATIONS MODELS
// ----------------------------------------------------
@JsonClass(generateAdapter = true)
data class SupabaseNotification(
    val id: String = "",
    val user_id: String? = null,
    val type: String? = null,
    val title: String? = null,
    val message: String? = null,
    val report_id: String? = null,
    val group_id: String? = null,
    val action_type: String? = null,
    val is_read: Boolean = false,
    val created_at: String? = null
)

@JsonClass(generateAdapter = true)
data class UpdateNotificationReadRequest(
    val is_read: Boolean = true
)

// ----------------------------------------------------
// 1:1 SUPPORT CHAT MODELS
// ----------------------------------------------------
@JsonClass(generateAdapter = true)
data class SupabaseSupportConversation(
    val id: String = "",
    val user_id: String? = null,
    val status: String? = "open",
    val last_message_at: String? = null,
    val created_at: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseSupportMessage(
    val id: String = "",
    val conversation_id: String? = null,
    val sender_type: String = "user", // "user" or "admin"
    val sender_id: String? = null,
    val body: String = "",
    val report_id: String? = null,
    val is_read_by_user: Boolean = false,
    val is_read_by_admin: Boolean = false,
    val created_at: String? = null
)

@JsonClass(generateAdapter = true)
data class SendSupportMessageRequest(
    val p_message: String,
    val p_report_id: String? = null
)

@JsonClass(generateAdapter = true)
data class AdminSendSupportMessageRequest(
    val p_user_id: String,
    val p_message: String,
    val p_report_id: String? = null
)

@JsonClass(generateAdapter = true)
data class AdminMarkSupportConversationReadRequest(
    val p_user_id: String
)

// ----------------------------------------------------
// E2EE 1:1 USER CHAT MODELS
// ----------------------------------------------------
@JsonClass(generateAdapter = true)
data class UserPublicKeyItem(
    val user_id: String? = null,
    val identity_public_key: String? = null,
    val key_version: Int = 1,
    val updated_at: String? = null
)

@JsonClass(generateAdapter = true)
data class UpsertUserPublicKeyRequest(
    val user_id: String,
    val identity_public_key: String,
    val key_version: Int = 1,
    val updated_at: String? = null
)

@JsonClass(generateAdapter = true)
data class GetOrCreateDmConversationRequest(
    val p_other_user_id: String
)

@JsonClass(generateAdapter = true)
data class SendDmMessageRequest(
    val id: String? = null,
    val conversation_id: String,
    val sender_id: String,
    val recipient_id: String,
    val ciphertext: String,
    val nonce: String = "",
    val key_version: Int = 1,
    val message_type: String = "text",
    val storage_path: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseDmMessage(
    val id: String = "",
    val conversation_id: String = "",
    val sender_id: String = "",
    val recipient_id: String = "",
    val ciphertext: String = "",
    val nonce: String = "",
    val key_version: Int = 1,
    val message_type: String? = "text",
    val storage_path: String? = null,
    val created_at: String? = null,
    val delivered_at: String? = null
)

// ----------------------------------------------------
// GIFTING MODELS
// ----------------------------------------------------
@JsonClass(generateAdapter = true)
data class GiftCatalogItem(
    val id: String = "",
    val name: String = "",
    val icon_url: String = "",
    val price_coins: Int = 0,
    val is_active: Boolean = true,
    val created_at: String? = null
)

@JsonClass(generateAdapter = true)
data class SendGiftRequest(
    val p_recipient_id: String,
    val p_gift_id: String,
    val p_message: String? = null
)

@JsonClass(generateAdapter = true)
data class GiftTransactionItem(
    val id: String = "",
    val sender_id: String? = null,
    val recipient_id: String? = null,
    val gift_id: String? = null,
    val message: String? = null,
    val created_at: String? = null,
    val gift_name: String? = null,
    val gift_icon_url: String? = null,
    val gift_price_coins: Int? = null
)

// ----------------------------------------------------
// GROUP CHAT MODELS
// ----------------------------------------------------
@JsonClass(generateAdapter = true)
data class CreateGroupRequest(
    val p_name: String,
    val p_member_ids: List<String>
)

@JsonClass(generateAdapter = true)
data class AcceptGroupInviteRequest(
    val p_group_id: String
)

@JsonClass(generateAdapter = true)
data class DeclineGroupInviteRequest(
    val p_group_id: String
)

@JsonClass(generateAdapter = true)
data class AddGroupMemberRequest(
    val p_group_id: String,
    val p_user_id: String
)

@JsonClass(generateAdapter = true)
data class RemoveGroupMemberRequest(
    val p_group_id: String,
    val p_user_id: String
)

@JsonClass(generateAdapter = true)
data class GroupRecipientPayload(
    val recipient_id: String,
    val ciphertext: String,
    val nonce: String = "",
    val storage_path: String? = null
)

@JsonClass(generateAdapter = true)
data class SendGroupMessageRequest(
    val p_group_id: String,
    val p_message_type: String = "text",
    val p_recipients: List<GroupRecipientPayload>
)

@JsonClass(generateAdapter = true)
data class SupabaseGroupConversation(
    val id: String = "",
    val name: String = "",
    val avatar_url: String? = null,
    val created_by: String? = null,
    val created_at: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseGroupMember(
    val id: String? = null,
    val group_id: String = "",
    val user_id: String = "",
    val role: String = "member", // "admin" or "member"
    val status: String = "invited", // "invited", "joined", "left"
    val joined_at: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseGroupRecipientRow(
    val id: String = "",
    val message_id: String = "",
    val recipient_id: String = "",
    val ciphertext: String = "",
    val nonce: String = "",
    val storage_path: String? = null,
    val is_read: Boolean = false,
    val created_at: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseGroupMessageMeta(
    val id: String = "",
    val group_id: String = "",
    val sender_id: String = "",
    val message_type: String = "text",
    val created_at: String? = null
)

// ----------------------------------------------------
// GENERALIZED REPORTING MODELS
// ----------------------------------------------------
@JsonClass(generateAdapter = true)
data class ReportUserRequest(
    val p_reported_user_id: String,
    val p_context_type: String, // "profile", "dm", "moment"
    val p_category: String,
    val p_reason: String,
    val p_context_id: String? = null,
    val p_screenshot_url: String? = null
)






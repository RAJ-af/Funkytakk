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
    val status: String = "pending", // 'pending' | 'accepted' | 'declined'
    val kind: String = "request", // 'request' | 'invite'
    val invited_by: String? = null,
    val created_at: String? = null
)

@JsonClass(generateAdapter = true)
data class RespondSpeakerRequestBody(
    val request_id: String,
    val accept: Boolean
)

@JsonClass(generateAdapter = true)
data class GetRoomTokenRequest(
    val room_id: String
)

@JsonClass(generateAdapter = true)
data class GetRoomTokenResponse(
    val token: String? = null,
    val rtc_token: String? = null,
    val role: String? = null, // "publisher" | "audience"
    val can_publish: Boolean = false,
    val provider: String? = null, // "agora" | "zego" | "daily"
    val channel: String? = null,
    val app_id: String? = null,
    val uid: Any? = null,
    val max_speakers: Int? = 4,
    val error: String? = null
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
    val message_type: String? = null,
    val payload: Map<String, Any?>? = null
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
data class SupabaseFollower(
    val id: String? = null,
    val follower_id: String,
    val following_id: String,
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
    val secure_url: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val duration: Double? = null,
    val resource_type: String? = null
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

    @POST("functions/v1/request-account-deletion")
    suspend fun requestAccountDeletion(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Body request: RequestAccountDeletionRequest
    ): Response<RequestAccountDeletionResponse>

    @POST("rest/v1/rpc/get_my_deletion_status")
    suspend fun getMyDeletionStatus(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: Map<String, String> = emptyMap()
    ): Response<ResponseBody>

    @POST("rest/v1/rpc/cancel_my_account_deletion")
    suspend fun cancelMyAccountDeletion(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: Map<String, String> = emptyMap()
    ): Response<ResponseBody>

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
        @Header("Authorization") auth: String
    ): Response<List<SupabaseProfile>>

    @GET("rest/v1/profiles")
    suspend fun getProfilesIn(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Query("auth_user_id") authUserIdIn: String // e.g. "in.(uuid1,uuid2)"
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

    @POST("rest/v1/rpc/invite_to_speaker_seat")
    suspend fun inviteToSpeakerSeat(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: Map<String, String> // {"p_room_id": roomId, "p_user_id": userId}
    ): Response<ResponseBody>

    @POST("rest/v1/rpc/respond_to_speaker_invite")
    suspend fun respondToSpeakerInvite(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: Map<String, @JvmSuppressWildcards Any> // {"p_request_id": requestId, "p_accept": accept}
    ): Response<ResponseBody>

    @POST("rest/v1/rpc/demote_speaker")
    suspend fun demoteSpeaker(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: Map<String, @JvmSuppressWildcards Any?> // {"p_room_id": roomId, "p_user_id": userId} (userId null = demote self)
    ): Response<ResponseBody>

    @POST("functions/v1/get-room-token")
    suspend fun getRoomToken(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: GetRoomTokenRequest
    ): Response<GetRoomTokenResponse>

    @GET("rest/v1/speaker_requests?select=*")
    suspend fun getSpeakerRequests(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Query("room_id") roomIdEq: String, // e.g. "eq.<uuid>"
        @Query("user_id") userIdEq: String? = null, // e.g. "eq.<uuid>"
        @Query("kind") kindEq: String? = null, // e.g. "eq.invite"
        @Query("status") statusEq: String? = null // e.g. "eq.pending"
    ): Response<List<SupabaseSpeakerRequest>>

    @POST("rest/v1/rpc/send_room_invite")
    suspend fun sendRoomInvite(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: Map<String, String>
    ): Response<ResponseBody>

    @POST("rest/v1/rpc/respond_to_room_invite")
    suspend fun respondToRoomInvite(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Response<ResponseBody>

    @POST("rest/v1/rpc/list_invitable_following")
    suspend fun listInvitableFollowing(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: Map<String, String>
    ): Response<List<InvitableUserResponse>>

    @GET("rest/v1/room_end_summaries?select=*")
    suspend fun getRoomEndSummary(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String = "Bearer ${BuildConfig.SUPABASE_ANON_KEY}",
        @Query("room_id") roomIdEq: String
    ): Response<List<RoomEndSummaryResponse>>

    @GET("rest/v1/room_invites?select=*")
    suspend fun getPendingRoomInvites(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String = "Bearer ${BuildConfig.SUPABASE_ANON_KEY}",
        @Query("invitee_id") inviteeIdEq: String,
        @Query("status") statusEq: String = "eq.pending"
    ): Response<List<RoomInviteItem>>

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

    @POST("rest/v1/rpc/admin_take_charge")
    suspend fun adminTakeCharge(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: AdminTakeChargeRequest
    ): Response<ResponseBody>

    @POST("rest/v1/rpc/admin_release_report")
    suspend fun adminReleaseReport(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: AdminReleaseReportRequest
    ): Response<ResponseBody>

    @POST("rest/v1/rpc/admin_apply_moderation_action")
    suspend fun adminApplyModerationAction(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: AdminApplyModerationActionRequest
    ): Response<ResponseBody>

    @POST("rest/v1/rpc/admin_dismiss_report")
    suspend fun adminDismissReport(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: AdminDismissReportRequest
    ): Response<ResponseBody>

    @POST("rest/v1/rpc/admin_reopen_report")
    suspend fun adminReopenReport(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: AdminReopenReportRequest
    ): Response<ResponseBody>

    @POST("rest/v1/rpc/admin_challenge_reopen")
    suspend fun adminChallengeReopen(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: AdminChallengeReopenRequest
    ): Response<ResponseBody>

    @POST("rest/v1/rpc/owner_decide_dispute")
    suspend fun ownerDecideDispute(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: OwnerDecideDisputeRequest
    ): Response<ResponseBody>

    @POST("rest/v1/rpc/get_my_account_review")
    suspend fun getMyAccountReview(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: Map<String, String> = emptyMap()
    ): Response<MyAccountReviewResponse>

    @POST("rest/v1/rpc/admin_get_report_queue")
    suspend fun adminGetReportQueue(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: AdminGetReportQueueRequest
    ): Response<List<AdminReportQueueItem>>

    @POST("rest/v1/rpc/admin_get_report_detail")
    suspend fun adminGetReportDetail(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: AdminGetReportDetailRequest
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

    @GET("rest/v1/user_roles?select=user_id,role")
    suspend fun getMyUserRoles(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String
    ): Response<List<UserRoleItem>>

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
        @Header("Authorization") auth: String,
        @Query("user_id") userIdFilter: String? = null // e.g. "eq.<uuid>"
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

    @POST("rest/v1/rpc/get_my_support_session")
    suspend fun getMySupportSession(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: Map<String, String> = emptyMap()
    ): Response<GetMySupportSessionResponse>

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

    @POST("rest/v1/rpc/admin_close_support_chat")
    suspend fun adminCloseSupportChat(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: AdminCloseSupportChatRequest
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
        @Query("recipient_id") recipientIdFilter: String,
        @Query("delivered_at") deliveredAtFilter: String? = "is.null"
    ): Response<List<SupabaseDmMessage>>

    @PATCH("rest/v1/dm_messages")
    suspend fun markDmDelivered(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Query("id") idFilter: String,
        @Body body: MarkDmDeliveredRequest
    ): Response<ResponseBody>

    @POST("rest/v1/rpc/register_push_token")
    suspend fun registerPushToken(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: RegisterPushTokenRequest
    ): Response<ResponseBody>

    @POST("rest/v1/rpc/unregister_push_token")
    suspend fun unregisterPushToken(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: PushTokenRequest
    ): Response<ResponseBody>

    @GET("rest/v1/group_tombstones?select=*&order=created_at.asc")
    suspend fun getGroupTombstones(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String
    ): Response<List<GroupTombstoneRow>>

    @DELETE("rest/v1/group_tombstones")
    suspend fun deleteGroupTombstone(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Query("id") idFilter: String
    ): Response<ResponseBody>

    @POST("rest/v1/rpc/get_group_invite_details")
    suspend fun getGroupInviteDetails(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: GroupIdRequest
    ): Response<GroupInviteDetailsResponse>

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

    @POST("rest/v1/rpc/send_room_gift")
    suspend fun sendRoomGift(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: SendRoomGiftRequest
    ): Response<ResponseBody>

    @GET("rest/v1/gift_transactions?select=*&order=created_at.desc")
    suspend fun getGiftTransactions(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Query("recipient_id") recipientIdFilter: String? = null,
        @Query("sender_id") senderIdFilter: String? = null
    ): Response<List<GiftTransactionItem>>

    // ----------------------------------------------------
    // STORE & COSMETICS RPCs
    // ----------------------------------------------------
    @POST("rest/v1/rpc/get_store_items")
    suspend fun getStoreItems(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: GetStoreItemsRequest
    ): Response<List<StoreItemRow>>

    @POST("rest/v1/rpc/purchase_item")
    suspend fun purchaseItem(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: PurchaseItemRequest
    ): Response<ResponseBody>

    @POST("rest/v1/rpc/purchase_items")
    suspend fun purchaseItems(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: PurchaseItemsRequest
    ): Response<ResponseBody>

    @POST("rest/v1/rpc/equip_item")
    suspend fun equipItem(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: EquipItemRequest
    ): Response<ResponseBody>

    @POST("rest/v1/rpc/unequip_slot")
    suspend fun unequipSlot(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: UnequipSlotRequest
    ): Response<ResponseBody>

    @POST("rest/v1/rpc/get_user_cosmetics")
    suspend fun getUserCosmetics(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: GetUserCosmeticsRequest
    ): Response<List<UserCosmeticsRow>>

    @POST("rest/v1/rpc/get_user_cosmetics_v2")
    suspend fun getUserCosmeticsV2(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: GetUserCosmeticsRequest
    ): Response<List<UserCosmeticsRow>>

    @POST("rest/v1/rpc/verify_owned_items")
    suspend fun verifyOwnedItems(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: VerifyOwnedItemsRequest
    ): Response<List<VerifiedOwnedItemRow>>

    // ----------------------------------------------------
    // GROUP CHAT (E2EE FAN-OUT & RPCs)
    // ----------------------------------------------------
    @POST("rest/v1/rpc/get_my_groups")
    suspend fun getMyGroups(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: Map<String, String> = emptyMap()
    ): Response<List<MyGroupRow>>

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

    @POST("rest/v1/rpc/get_group_overview")
    suspend fun getGroupOverview(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: GroupIdRequest
    ): Response<ResponseBody>

    @POST("rest/v1/rpc/update_group_settings")
    suspend fun updateGroupSettings(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: UpdateGroupSettingsRequest
    ): Response<ResponseBody>

    @POST("rest/v1/rpc/update_my_group_prefs")
    suspend fun updateMyGroupPrefs(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: UpdateMyGroupPrefsRequest
    ): Response<ResponseBody>

    @POST("rest/v1/rpc/get_group_members")
    suspend fun getGroupMembersRpc(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: GroupIdRequest
    ): Response<List<SupabaseGroupMember>>

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

    @POST("rest/v1/rpc/set_member_role")
    suspend fun setMemberRole(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: SetMemberRoleRequest
    ): Response<ResponseBody>

    @POST("rest/v1/rpc/approve_join_request")
    suspend fun approveJoinRequest(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: GroupUserActionRequest
    ): Response<ResponseBody>

    @POST("rest/v1/rpc/reject_join_request")
    suspend fun rejectJoinRequest(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: GroupUserActionRequest
    ): Response<ResponseBody>

    @POST("rest/v1/rpc/leave_group")
    suspend fun leaveGroup(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: GroupIdRequest
    ): Response<ResponseBody>

    @POST("rest/v1/rpc/block_group")
    suspend fun blockGroup(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: GroupIdRequest
    ): Response<ResponseBody>

    @POST("rest/v1/rpc/unblock_group")
    suspend fun unblockGroup(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: GroupIdRequest
    ): Response<ResponseBody>

    @POST("rest/v1/rpc/get_my_blocked_groups")
    suspend fun getMyBlockedGroups(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: Map<String, String> = emptyMap()
    ): Response<List<BlockedGroupRow>>

    @GET("rest/v1/group_events?select=*")
    suspend fun getGroupEventsRaw(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Query("group_id") groupIdFilter: String,
        @Query("created_at") createdAtFilter: String? = null,
        @Query("order") order: String = "created_at.asc",
        @Query("limit") limit: Int = 200
    ): Response<ResponseBody>

    @POST("rest/v1/rpc/delete_group")
    suspend fun deleteGroup(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: GroupIdRequest
    ): Response<ResponseBody>

    @POST("rest/v1/rpc/report_group")
    suspend fun reportGroup(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: ReportGroupRequest
    ): Response<ResponseBody>

    @POST("rest/v1/rpc/get_group_invite")
    suspend fun getGroupInvite(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: GroupIdRequest
    ): Response<ResponseBody>

    @POST("rest/v1/rpc/reset_group_invite")
    suspend fun resetGroupInvite(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: GroupIdRequest
    ): Response<ResponseBody>

    @POST("rest/v1/rpc/set_group_invite_enabled")
    suspend fun setGroupInviteEnabled(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: SetGroupInviteEnabledRequest
    ): Response<ResponseBody>

    @POST("rest/v1/rpc/preview_group_by_invite")
    suspend fun previewGroupByInvite(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: InviteCodeRequest
    ): Response<ResponseBody>

    @POST("rest/v1/rpc/join_group_by_invite")
    suspend fun joinGroupByInvite(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: InviteCodeRequest
    ): Response<ResponseBody>

    @POST("rest/v1/rpc/send_group_message")
    suspend fun sendGroupMessage(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: SendGroupMessageRequest
    ): Response<ResponseBody>

    @GET("rest/v1/group_message_recipients?select=*&order=created_at.asc")
    suspend fun getPendingGroupMessageRecipients(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Query("recipient_id") recipientIdFilter: String
    ): Response<List<SupabaseGroupRecipientRow>>

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

    @POST("rest/v1/rpc/attach_report_proofs")
    suspend fun attachReportProofs(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: AttachReportProofsRequest
    ): Response<ResponseBody>

    // ----------------------------------------------------
    // CHAT BACKUP & RESTORE
    // ----------------------------------------------------
    @POST("rest/v1/rpc/record_chat_backup")
    suspend fun recordChatBackup(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: RecordChatBackupRequest
    ): Response<ResponseBody>

    @POST("rest/v1/rpc/get_chat_backup_status")
    suspend fun getChatBackupStatus(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: Map<String, String> = emptyMap()
    ): Response<ChatBackupStatusResponse>

    @POST("rest/v1/rpc/delete_chat_backup_meta")
    suspend fun deleteChatBackupMeta(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: Map<String, String> = emptyMap()
    ): Response<ResponseBody>

    // ----------------------------------------------------
    // CLOUD AI ASSIST & CONSENT
    // ----------------------------------------------------
    @POST("rest/v1/rpc/get_ai_consent")
    suspend fun getAiConsent(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: Map<String, String> = emptyMap()
    ): Response<AiConsentResponse>

    @POST("rest/v1/rpc/accept_ai_consent")
    suspend fun acceptAiConsent(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: AcceptAiConsentRequest = AcceptAiConsentRequest(p_version = 1)
    ): Response<ResponseBody>

    @POST("rest/v1/rpc/revoke_ai_consent")
    suspend fun revokeAiConsent(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: Map<String, String> = emptyMap()
    ): Response<ResponseBody>

    @POST("functions/v1/ai-assist")
    suspend fun callAiAssist(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: AiAssistRequest
    ): Response<AiAssistResponse>

    // ----------------------------------------------------
    // MOMENTS V2 RPCs
    // ----------------------------------------------------
    @POST("rest/v1/rpc/get_moment_options")
    suspend fun getMomentOptions(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: Map<String, String> = emptyMap()
    ): Response<MomentOptionsResponse>

    @POST("rest/v1/rpc/get_moments_feed")
    suspend fun getMomentsFeed(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: GetMomentsFeedRequest
    ): Response<List<MomentDto>>

    @POST("rest/v1/rpc/get_user_moments")
    suspend fun getUserMoments(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: GetUserMomentsRequest
    ): Response<List<MomentDto>>

    @POST("rest/v1/rpc/get_moment")
    suspend fun getMomentRpc(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: MomentIdRequest
    ): Response<MomentDto>

    @POST("rest/v1/rpc/create_moment")
    suspend fun createMomentRpc(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: CreateMomentRequest
    ): Response<MomentDto>

    @POST("rest/v1/rpc/delete_moment")
    suspend fun deleteMomentRpc(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: MomentIdRequest
    ): Response<ResponseBody>

    @POST("rest/v1/rpc/toggle_moment_like")
    suspend fun toggleMomentLikeRpc(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: MomentIdRequest
    ): Response<ToggleLikeResponse>

    @POST("rest/v1/rpc/get_moment_comments")
    suspend fun getMomentCommentsRpc(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: GetMomentCommentsRequest
    ): Response<List<MomentCommentDto>>

    @POST("rest/v1/rpc/get_comment_replies")
    suspend fun getCommentRepliesRpc(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: GetCommentRepliesRequest
    ): Response<List<MomentCommentDto>>

    @POST("rest/v1/rpc/add_moment_comment")
    suspend fun addMomentCommentRpc(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: AddMomentCommentRequest
    ): Response<MomentCommentDto>

    @POST("rest/v1/rpc/add_moment_correction")
    suspend fun addMomentCorrectionRpc(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: AddMomentCorrectionRequest
    ): Response<MomentCommentDto>

    @POST("rest/v1/rpc/toggle_comment_like")
    suspend fun toggleCommentLikeRpc(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: CommentIdRequest
    ): Response<ToggleLikeResponse>

    @POST("rest/v1/rpc/accept_correction")
    suspend fun acceptCorrectionRpc(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: CommentIdRequest
    ): Response<AcceptCorrectionResponse>

    @POST("rest/v1/rpc/delete_moment_comment")
    suspend fun deleteMomentCommentRpc(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: CommentIdRequest
    ): Response<ResponseBody>

    @POST("rest/v1/rpc/mark_moment_not_interested")
    suspend fun markMomentNotInterestedRpc(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String,
        @Body body: MomentIdRequest
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
data class AdminTakeChargeRequest(
    val p_report_id: String
)

@JsonClass(generateAdapter = true)
data class AdminReleaseReportRequest(
    val p_report_id: String
)

@JsonClass(generateAdapter = true)
data class AdminDismissReportRequest(
    val p_report_id: String,
    val p_reason: String? = null
)

@JsonClass(generateAdapter = true)
data class AdminReopenReportRequest(
    val p_report_id: String,
    val p_brief: String
)

@JsonClass(generateAdapter = true)
data class AdminChallengeReopenRequest(
    val p_reopen_id: String,
    val p_brief: String
)

@JsonClass(generateAdapter = true)
data class OwnerDecideDisputeRequest(
    val p_reopen_id: String,
    val p_decision: String,
    val p_note: String? = null
)

@JsonClass(generateAdapter = true)
data class AdminGetReportQueueRequest(
    val p_status: String? = "pending",
    val p_limit: Int = 50,
    val p_offset: Int = 0
)

@JsonClass(generateAdapter = true)
data class AdminGetReportDetailRequest(
    val p_report_id: String
)

@JsonClass(generateAdapter = true)
data class AdminReportQueueItem(
    val report_id: String,
    val created_at: String? = null,
    val context_type: String? = null,
    val category: String? = null,
    val reason: String? = null,
    val status: String? = null,
    val evidence_status: String? = null,
    val reporter_id: String? = null,
    val reporter_name: String? = null,
    val reported_user_id: String? = null,
    val reported_name: String? = null,
    val reported_score: Int? = 100,
    val reports_against_total: Long? = 0L,
    val reports_against_pending: Long? = 0L,
    val claimed_by: String? = null,
    val claimed_by_name: String? = null,
    val claim_expires_at: String? = null,
    val is_mine: Boolean? = false,
    val reopen_status: String? = null
)

@JsonClass(generateAdapter = true)
data class MyAccountReviewResponse(
    val under_review: Boolean = false,
    val since: String? = null
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
    val chat_open_until: String? = null,
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
data class GetMySupportSessionResponse(
    val open: Boolean = false,
    val reason: String? = null, // "support_opened" | "appeal"
    val until: String? = null,
    val conversation_id: String? = null,
    val unread: Int = 0,
    val can_appeal: Boolean = false
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
    val p_report_id: String? = null,
    val p_open_hours: Int? = 2
)

@JsonClass(generateAdapter = true)
data class AdminCloseSupportChatRequest(
    val p_user_id: String
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
    val sort_order: Int? = null,
    val created_at: String? = null
)

@JsonClass(generateAdapter = true)
data class SendGiftRequest(
    val p_recipient_id: String,
    val p_gift_id: String,
    val p_message: String? = null
)

@JsonClass(generateAdapter = true)
data class SendRoomGiftRequest(
    val p_room_id: String,
    val p_recipient_id: String,
    val p_gift_id: String,
    val p_quantity: Int = 1
)

@JsonClass(generateAdapter = true)
data class SendRoomGiftResponse(
    val ok: Boolean = false,
    val remaining_balance: Int? = null,
    val coins_spent: Int? = null,
    val message_id: String? = null,
    val error: String? = null
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
data class RegisterPushTokenRequest(
    val p_token: String,
    val p_platform: String = "android"
)

@JsonClass(generateAdapter = true)
data class PushTokenRequest(
    val p_token: String
)

@JsonClass(generateAdapter = true)
data class MarkDmDeliveredRequest(
    val delivered_at: String
)

@JsonClass(generateAdapter = true)
data class GroupTombstoneRow(
    val id: String = "",
    val group_id: String = "",
    val group_name: String = "",
    val reason: String = "deleted", // 'deleted' | 'removed' | 'request_rejected'
    val deleted_by_name: String? = null,
    val created_at: String? = null
)

@JsonClass(generateAdapter = true)
data class InviteMemberPreviewItem(
    val user_id: String = "",
    val avatar_url: String? = null,
    val profile_name: String? = null,
    val username: String? = null
)

@JsonClass(generateAdapter = true)
data class GroupInviteDetailsResponse(
    val group_id: String = "",
    val name: String = "",
    val avatar_url: String? = null,
    val description: String? = null,
    val inviter_name: String? = null,
    val invited_by_name: String? = null,
    val inviter_avatar: String? = null,
    val invited_by_avatar: String? = null,
    val member_count: Int = 1,
    val joined_count: Int = 1,
    val member_preview: List<InviteMemberPreviewItem>? = null,
    val who_can_send: String? = "everyone",
    val approve_new_members: Boolean = false,
    val disappearing_seconds: Int = 0
)

@JsonClass(generateAdapter = true)
data class MyGroupRow(
    val group_id: String = "",
    val name: String = "",
    val avatar_url: String? = null,
    val description: String? = null,
    val invited_by_name: String? = null,
    val my_role: String? = "member",
    val my_status: String? = "joined", // joined | invited | requested
    val joined_count: Int? = 1,
    val muted_until: String? = null,
    val notify_level: String? = "all",
    val who_can_send: String? = "everyone",
    val disappearing_seconds: Int? = 0,
    val created_at: String? = null
)

@JsonClass(generateAdapter = true)
data class CreateGroupRequest(
    val p_name: String,
    val p_member_ids: List<String>,
    val p_description: String? = null,
    val p_avatar_url: String? = null
)

@JsonClass(generateAdapter = true)
data class GroupIdRequest(
    val p_group_id: String
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
data class GroupUserActionRequest(
    val p_group_id: String,
    val p_user_id: String
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
data class SetMemberRoleRequest(
    val p_group_id: String,
    val p_user_id: String,
    val p_role: String // "admin" | "member"
)

@JsonClass(generateAdapter = true)
data class UpdateGroupSettingsRequest(
    val p_group_id: String,
    val p_patch: Map<String, @JvmSuppressWildcards Any?>
)

@JsonClass(generateAdapter = true)
data class UpdateMyGroupPrefsRequest(
    val p_group_id: String,
    val p_muted_until: String? = null,
    val p_clear_mute: Boolean? = null,
    val p_notify_level: String? = null, // "all" | "mentions" | "none"
    val p_media_auto_download: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class ReportGroupRequest(
    val p_group_id: String,
    val p_category: String,
    val p_reason: String,
    val p_evidence: Map<String, Any?>? = null,
    val p_deletion_log: List<Map<String, Any?>>? = null
)

@JsonClass(generateAdapter = true)
data class SetGroupInviteEnabledRequest(
    val p_group_id: String,
    val p_enabled: Boolean
)

@JsonClass(generateAdapter = true)
data class InviteCodeRequest(
    val p_code: String
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
    val group_id: String? = "",
    val user_id: String = "",
    val role: String = "member", // "admin" or "member"
    val status: String = "joined", // "invited", "joined", "requested"
    val joined_at: String? = null,
    val username: String? = null,
    val profile_name: String? = null,
    val avatar_url: String? = null,
    val presence_status: String? = null,
    val country: String? = null,
    val country_code: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseGroupRecipientRow(
    val id: String = "",
    val message_id: String = "",
    val recipient_id: String = "",
    val group_id: String? = null,
    val sender_id: String? = null,
    val message_type: String? = "text",
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

@JsonClass(generateAdapter = true)
data class BlockedGroupRow(
    val group_id: String = "",
    val name: String = "",
    val avatar_url: String? = null,
    val blocked_at: String? = null
)

// ----------------------------------------------------
// GENERALIZED REPORTING MODELS
// ----------------------------------------------------
@JsonClass(generateAdapter = true)
data class ReportUserRequest(
    val p_reported_user_id: String,
    val p_context_type: String, // "room", "profile", "dm", "moment"
    val p_category: String,
    val p_reason: String? = null,
    val p_context_id: String? = null,
    val p_screenshot_url: String? = null,
    val p_evidence: Map<String, Any?>? = null,
    val p_deletion_log: List<Map<String, Any?>>? = null
)

@JsonClass(generateAdapter = true)
data class AttachReportProofsRequest(
    val p_report_id: String,
    val p_media_paths: List<String>
)

// ----------------------------------------------------
// CHAT BACKUP & RESTORE MODELS
// ----------------------------------------------------
@JsonClass(generateAdapter = true)
data class RecordChatBackupRequest(
    val p_size_bytes: Long,
    val p_format_version: Int = 1,
    val p_includes_media: Boolean = false,
    val p_key_version: Int = 1
)

@JsonClass(generateAdapter = true)
data class ChatBackupStatusResponse(
    val has_backup: Boolean = false,
    val last_backup_at: String? = null,
    val size_bytes: Long = 0,
    val format_version: Int = 1,
    val includes_media: Boolean = false,
    val backup_key_version: Int = 1,
    val current_key_version: Int = 1
)

// ----------------------------------------------------
// CLOUD AI DATA MODELS
// ----------------------------------------------------
@JsonClass(generateAdapter = true)
data class AiConsentResponse(
    val accepted: Boolean? = false,
    val consent_version: Int? = 0
)

@JsonClass(generateAdapter = true)
data class AcceptAiConsentRequest(
    val p_version: Int = 1
)

@JsonClass(generateAdapter = true)
data class AiAssistRequest(
    val feature: String, // "translate" | "improve"
    val text: String,
    val target_language: String? = null,
    val style: String? = null, // "fix" | "friendly" | "formal" | "short"
    val language_hint: String? = null
)

@JsonClass(generateAdapter = true)
data class AiAssistResponse(
    val text: String? = null,
    val provider: String? = null, // "sarvam" | "cerebras" | "gemini"
    val error: String? = null,
    val code: String? = null
)

// ----------------------------------------------------
// STORE & COSMETICS MODELS
// ----------------------------------------------------
@JsonClass(generateAdapter = true)
data class GetStoreItemsRequest(
    val p_type: String? = null,
    val p_owned_only: Boolean = false
)

@JsonClass(generateAdapter = true)
data class StoreItemRow(
    val id: Long,
    val code: String,
    val item_type: String, // "bubble_theme", "chat_bg", "avatar_frame", "name_effect", "sticker_pack"
    val name: String,
    val description: String? = null,
    val preview_url: String? = null,
    val price_coins: Int = 0,
    val config: Map<String, Any?>? = null,
    val owned: Boolean = false,
    val equipped: Boolean = false
)

@JsonClass(generateAdapter = true)
data class PurchaseItemRequest(
    val p_item_id: Long
)

@JsonClass(generateAdapter = true)
data class PurchaseItemsRequest(
    val p_item_ids: List<Long>,
    val p_equip: Boolean = true
)

@JsonClass(generateAdapter = true)
data class PurchaseItemsResponse(
    val bought: List<Long> = emptyList(),
    val already_owned: List<Long> = emptyList(),
    val equipped: List<Long> = emptyList(),
    val coins_spent: Int = 0,
    val remaining_balance: Int = 0
)

@JsonClass(generateAdapter = true)
data class EquipItemRequest(
    val p_item_id: Long
)

@JsonClass(generateAdapter = true)
data class UnequipSlotRequest(
    val p_slot: String // "bubble_theme", "chat_bg", "avatar_frame", "name_effect"
)

@JsonClass(generateAdapter = true)
data class GetUserCosmeticsRequest(
    val p_user_ids: List<String>
)

@JsonClass(generateAdapter = true)
data class CosmeticSlotInfo(
    val id: Long? = null,
    val code: String? = null,
    val config: Map<String, Any?>? = null
)

@JsonClass(generateAdapter = true)
data class UserCosmeticsRow(
    val user_id: String,
    val bubble_theme: CosmeticSlotInfo? = null,
    val avatar_frame: CosmeticSlotInfo? = null,
    val name_effect: CosmeticSlotInfo? = null,
    val chat_bg: CosmeticSlotInfo? = null,
    val entry_animation: CosmeticSlotInfo? = null
)

@JsonClass(generateAdapter = true)
data class VerifyOwnedItemsRequest(
    val p_user_id: String,
    val p_item_ids: List<Long>
)

@JsonClass(generateAdapter = true)
data class VerifiedOwnedItemRow(
    val item_id: Long,
    val item_type: String,
    val config: Map<String, Any?>? = null
)

@JsonClass(generateAdapter = true)
data class InvitableUserResponse(
    val user_id: String,
    val username: String? = null,
    val avatar_url: String? = null,
    val country_code: String? = null,
    val is_online: Boolean = false,
    val in_room: Boolean = false,
    val invite_status: String? = null
)

@JsonClass(generateAdapter = true)
data class RoomEndSummaryResponse(
    val room_id: String,
    val title: String? = null,
    val host_id: String? = null,
    val host_username: String? = null,
    val host_avatar_url: String? = null,
    val started_at: String? = null,
    val ended_at: String? = null,
    val duration_seconds: Int? = null,
    val total_joins: Int? = null,
    val peak_participants: Int? = null,
    val ended_reason: String = "unknown"
)

@JsonClass(generateAdapter = true)
data class RoomInviteItem(
    val id: String,
    val room_id: String,
    val inviter_id: String,
    val invitee_id: String,
    val status: String,
    val created_at: String
)

@JsonClass(generateAdapter = true)
data class DeviceDeletionInfo(
    val device_id: String,
    val name: String
)

@JsonClass(generateAdapter = true)
data class RequestAccountDeletionRequest(
    val firebase_token: String,
    val confirm: Boolean = true,
    val reason: String? = null,
    val device: DeviceDeletionInfo
)

@JsonClass(generateAdapter = true)
data class RequestAccountDeletionResponse(
    val success: Boolean? = null,
    val receipt_id: String? = null,
    val scheduled_for: String? = null,
    val grace_days: Int? = null,
    val already_requested: Boolean? = null,
    val error: String? = null
)

@JsonClass(generateAdapter = true)
data class DeletionStatusResponse(
    val pending: Boolean = false,
    val receipt_id: String? = null,
    val status: String? = null,
    val scheduled_for: String? = null,
    val can_cancel: Boolean? = null
)

// ----------------------------------------------------
// MOMENTS V2 RPC MODELS
// ----------------------------------------------------
@JsonClass(generateAdapter = true)
data class MomentTopicDto(
    val slug: String,
    val label: String
)

@JsonClass(generateAdapter = true)
data class MomentLimitsDto(
    val title: Int = 60,
    val content: Int = 500,
    val translation: Int = 300,
    val topics: Int = 3,
    val media: Int = 10,
    val location: Int = 80,
    val comment: Int = 500,
    val correction_text: Int = 300,
    val language: Int = 40
)

@JsonClass(generateAdapter = true)
data class MomentOptionsResponse(
    val topics: List<MomentTopicDto> = emptyList(),
    val limits: MomentLimitsDto = MomentLimitsDto()
)

@JsonClass(generateAdapter = true)
data class MomentMediaDto(
    val type: String = "image", // "image" | "video"
    val url: String = "",
    val thumb_url: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val duration_sec: Int? = null
)

@JsonClass(generateAdapter = true)
data class MomentAuthorDto(
    val id: String = "",
    val username: String? = null,
    val name: String? = null,
    val avatar_url: String? = null,
    val city: String? = null,
    val country_code: String? = null,
    val is_verified: Boolean = false,
    val native_languages: List<String> = emptyList(),
    val learning_languages: List<String> = emptyList(),
    val is_native: Boolean = false,
    val is_me: Boolean = false,
    val following: Boolean = false
)

@JsonClass(generateAdapter = true)
data class MomentDto(
    val id: String = "",
    val title: String? = null,
    val content: String? = null,
    val translation: String? = null,
    val language: String = "English",
    val bg_key: String? = null,
    val media: List<MomentMediaDto> = emptyList(),
    val topics: List<String> = emptyList(),
    val location: String? = null,
    val fix_requested: Boolean = true,
    val status: String = "active",
    val likes_count: Int = 0,
    val comments_count: Int = 0,
    val corrections_count: Int = 0,
    val created_at: String? = null,
    val liked_by_me: Boolean = false,
    val author: MomentAuthorDto = MomentAuthorDto()
)

@JsonClass(generateAdapter = true)
data class MomentCommentAuthorDto(
    val id: String = "",
    val username: String? = null,
    val name: String? = null,
    val avatar_url: String? = null,
    val is_me: Boolean = false
)

@JsonClass(generateAdapter = true)
data class MomentCommentDto(
    val id: String = "",
    val moment_id: String = "",
    val parent_id: String? = null,
    val kind: String = "comment", // "comment" | "correction"
    val content: String? = null,
    val original_text: String? = null,
    val corrected_text: String? = null,
    val by_native: Boolean = false,
    val is_accepted: Boolean = false,
    val likes_count: Int = 0,
    val liked_by_me: Boolean = false,
    val replies_count: Int = 0,
    val created_at: String? = null,
    val author: MomentCommentAuthorDto = MomentCommentAuthorDto()
)

@JsonClass(generateAdapter = true)
data class ToggleLikeResponse(
    val liked: Boolean = false,
    val likes_count: Int = 0
)

@JsonClass(generateAdapter = true)
data class AcceptCorrectionResponse(
    val is_accepted: Boolean = false
)

@JsonClass(generateAdapter = true)
data class GetMomentsFeedRequest(
    val p_tab: String,
    val p_limit: Int = 20,
    val p_offset: Int = 0
)

@JsonClass(generateAdapter = true)
data class GetUserMomentsRequest(
    val p_user_id: String? = null,
    val p_limit: Int = 20,
    val p_offset: Int = 0
)

@JsonClass(generateAdapter = true)
data class MomentIdRequest(
    val p_moment_id: String
)

@JsonClass(generateAdapter = true)
data class CommentIdRequest(
    val p_comment_id: String
)

@JsonClass(generateAdapter = true)
data class CreateMomentRequest(
    val p_title: String? = null,
    val p_content: String? = null,
    val p_language: String,
    val p_translation: String? = null,
    val p_bg_key: String? = null,
    val p_media: List<MomentMediaDto>? = null,
    val p_topics: List<String>? = null,
    val p_location: String? = null,
    val p_fix_requested: Boolean = true
)

@JsonClass(generateAdapter = true)
data class GetMomentCommentsRequest(
    val p_moment_id: String,
    val p_kind: String = "all",
    val p_limit: Int = 30,
    val p_offset: Int = 0
)

@JsonClass(generateAdapter = true)
data class GetCommentRepliesRequest(
    val p_comment_id: String,
    val p_limit: Int = 30,
    val p_offset: Int = 0
)

@JsonClass(generateAdapter = true)
data class AddMomentCommentRequest(
    val p_moment_id: String,
    val p_content: String,
    val p_parent_id: String? = null
)

@JsonClass(generateAdapter = true)
data class AddMomentCorrectionRequest(
    val p_moment_id: String,
    val p_original: String,
    val p_corrected: String,
    val p_explanation: String? = null
)







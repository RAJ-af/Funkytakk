package com.example

import com.squareup.moshi.JsonClass
import okhttp3.MultipartBody
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
    val uid: String,
    val email: String,
    val displayName: String?,
    val token: String
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
        @Header("Authorization") auth: String = "Bearer ${BuildConfig.SUPABASE_ANON_KEY}",
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

    @GET("rest/v1/users")
    suspend fun getUser(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String = "Bearer ${BuildConfig.SUPABASE_ANON_KEY}",
        @Query("uid") uidEq: String // e.g. "eq.123-abc"
    ): Response<List<SupabaseUser>>

    @GET("rest/v1/users")
    suspend fun getUserByUsername(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String = "Bearer ${BuildConfig.SUPABASE_ANON_KEY}",
        @Query("username") usernameEq: String // e.g. "eq.Raj"
    ): Response<List<SupabaseUser>>

    @GET("rest/v1/users")
    suspend fun getAllUsers(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String = "Bearer ${BuildConfig.SUPABASE_ANON_KEY}"
    ): Response<List<SupabaseUser>>
}

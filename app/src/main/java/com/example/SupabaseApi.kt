package com.example

import com.squareup.moshi.JsonClass
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class SupabaseUser(
    val uid: String, // from Firebase Auth
    val email: String,
    val display_name: String?,
    val username: String?,
    val avatar: String?,
    val dob: String?,
    val gender: String?,
    val native_language: String?,
    val learning_language: String?,
    val country: String?,
    val country_code: String?,
    val hobbies: List<String>?,
    val custom_email_verified: Boolean? = null,
    val email_verification_token: String? = null
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

interface SupabaseApi {

    @POST("functions/v1/send-verification-email")
    suspend fun sendVerificationEmail(
        @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String = "Bearer ${BuildConfig.SUPABASE_ANON_KEY}",
        @Body request: SendVerificationRequest
    ): Response<Unit>

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
}

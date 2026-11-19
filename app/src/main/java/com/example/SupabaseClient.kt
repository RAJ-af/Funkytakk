package com.example

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object SupabaseClient {

    /**
     * Retries authenticated requests once on HTTP 401 after performing a single-flight
     * session refresh via [SupabaseSessionManager]. Skips auth-minting endpoints and
     * anonymous-key calls to prevent recursion or deadlocks.
     */
    private val authRetryInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val response = chain.proceed(originalRequest)

        if (response.code != 401) {
            return@Interceptor response
        }

        val path = originalRequest.url.encodedPath
        if (path.contains("verify-firebase-token")) {
            return@Interceptor response
        }
        if (originalRequest.header("X-Auth-Retried") == "1") {
            return@Interceptor response
        }

        val authHeader = originalRequest.header("Authorization")
        val anonBearer = "Bearer ${BuildConfig.SUPABASE_ANON_KEY}"
        if (authHeader.isNullOrBlank() || authHeader == anonBearer) {
            return@Interceptor response
        }

        val ctx = try {
            FunkyTalkApplication.getSafeAppContext()
        } catch (_: Exception) {
            null
        } ?: return@Interceptor response

        val refreshedUid = try {
            runBlocking {
                SupabaseSessionManager.forceRefreshSession(ctx)
            }
        } catch (_: Exception) {
            null
        }

        val updatedToken = SupabaseSessionManager.getAccessToken(ctx)
        if (refreshedUid.isNullOrBlank() || updatedToken.isNullOrBlank() || updatedToken == BuildConfig.SUPABASE_ANON_KEY) {
            return@Interceptor response
        }

        val freshBearer = "Bearer $updatedToken"
        response.close()
        val retriedRequest = originalRequest.newBuilder()
            .header("Authorization", freshBearer)
            .header("X-Auth-Retried", "1")
            .build()
        chain.proceed(retriedRequest)
    }

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(authRetryInterceptor)
        .apply {
            if (BuildConfig.DEBUG) {
                val loggingInterceptor = HttpLoggingInterceptor { message ->
                    android.util.Log.d("OkHttp", message)
                }.apply {
                    redactHeader("Authorization")
                    redactHeader("apikey")
                    redactHeader("Cookie")
                    redactHeader("Set-Cookie")
                    level = HttpLoggingInterceptor.Level.HEADERS
                }
                addInterceptor(loggingInterceptor)
            }
        }
        .build()

    // Add https:// prefix if missing and trailing slash
    private val baseUrl = BuildConfig.SUPABASE_URL.let {
        var url = it
        if (!url.startsWith("http")) {
            url = "https://$url"
        }
        if (!url.endsWith("/")) {
            url = "$url/"
        }
        url
    }

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    val api: SupabaseApi by lazy {
        retrofit.create(SupabaseApi::class.java)
    }
}

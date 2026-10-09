package com.example

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object SupabaseClient {
    private val pushTokenLogRegex = Regex("(\"p_token\"\\s*:\\s*\")([^\"]+)(\")")

    private val loggingInterceptor = HttpLoggingInterceptor { message ->
        val sanitized = pushTokenLogRegex.replace(message) { matchResult ->
            val prefix = matchResult.groupValues[1]
            val rawToken = matchResult.groupValues[2]
            val suffix = matchResult.groupValues[3]
            "${prefix}…${rawToken.takeLast(6)}${suffix}"
        }
        android.util.Log.d("OkHttp", sanitized)
    }.apply {
        redactHeader("Authorization")
        redactHeader("apikey")
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor)
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

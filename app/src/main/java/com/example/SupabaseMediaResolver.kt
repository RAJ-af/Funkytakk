package com.example

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

/**
 * Secure media resolver for Supabase Storage.
 *
 * Designed to work with private Storage buckets (e.g. `reports`) where access is guarded
 * by Row Level Security (RLS) on `storage.objects` using `private.is_admin()` and `public.user_roles`.
 *
 * Rules:
 * 1. Uses the authenticated Supabase session established by the app.
 * 2. Loads Storage objects through authenticated Supabase Storage client endpoints.
 * 3. Never falls back to or uses a public URL for private report evidence.
 * 4. Never exposes service_role credentials.
 * 5. Bypasses no RLS checks.
 */
object SupabaseMediaResolver {
    private const val TAG = "SupabaseMediaResolver"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    // In-memory cache of raw identifier -> resolved signed URL
    private val signedUrlCache = mutableMapOf<String, CachedSignedUrl>()

    private data class CachedSignedUrl(
        val url: String,
        val expiresAtEpochMs: Long
    )

    /**
     * Extracts bucket name and relative object path from a raw path or URL.
     * Examples:
     * - "report_shot_123.jpg" -> Pair("reports", "report_shot_123.jpg")
     * - "reports/report_shot_123.jpg" -> Pair("reports", "report_shot_123.jpg")
     * - "https://.../storage/v1/object/public/reports/shot.jpg" -> Pair("reports", "shot.jpg")
     * - "https://.../storage/v1/object/authenticated/reports/shot.jpg" -> Pair("reports", "shot.jpg")
     * - "https://.../storage/v1/object/sign/reports/shot.jpg?token=..." -> Pair("reports", "shot.jpg")
     */
    fun extractBucketAndPath(rawInput: String?): Pair<String, String>? {
        if (rawInput.isNullOrBlank()) return null
        val clean = rawInput.trim()
        val queryIndex = clean.indexOf('?')
        val pathPart = if (queryIndex >= 0) clean.substring(0, queryIndex) else clean

        // Full URL with /storage/v1/object/
        val marker = "/storage/v1/object/"
        if (pathPart.contains(marker)) {
            val afterMarker = pathPart.substringAfter(marker)
            val segments = afterMarker.split('/').filter { it.isNotEmpty() }
            if (segments.isEmpty()) return null
            val startIndex = when (segments[0]) {
                "public", "authenticated", "sign" -> 1
                else -> 0
            }
            if (startIndex < segments.size) {
                val bucket = segments[startIndex]
                val objectPath = segments.drop(startIndex + 1).joinToString("/")
                if (objectPath.isNotEmpty()) {
                    return Pair(bucket, objectPath)
                }
            }
        }

        // Relative path
        val trimmedPath = pathPart.trimStart('/')
        if (trimmedPath.startsWith("reports/")) {
            return Pair("reports", trimmedPath.removePrefix("reports/"))
        }
        if (trimmedPath.startsWith("avatars/")) {
            return Pair("avatars", trimmedPath.removePrefix("avatars/"))
        }

        // Default: Evidence object path inside private 'reports' bucket
        return Pair("reports", trimmedPath)
    }

    /**
     * Resolves a private report evidence storage object to an authenticated signed URL.
     * Calls Supabase Storage `/storage/v1/object/sign/{bucket}/{filename}` with the
     * authenticated user's session token.
     *
     * If the caller possesses the admin role, backend RLS allows the signature and
     * returns a signed URL. If not, access is denied.
     */
    suspend fun resolveSignedEvidenceUrl(
        context: Context,
        rawPathOrUrl: String?,
        expiresInSeconds: Int = 3600
    ): String? = withContext(Dispatchers.IO) {
        if (rawPathOrUrl.isNullOrBlank()) return@withContext null
        val trimmed = rawPathOrUrl.trim()

        // If it is already a complete signed URL with active token parameter
        if (trimmed.contains("/object/sign/") && trimmed.contains("token=")) {
            return@withContext trimmed
        }

        // Check cache (return if valid for at least 1 more minute)
        val now = System.currentTimeMillis()
        synchronized(signedUrlCache) {
            val cached = signedUrlCache[trimmed]
            if (cached != null && cached.expiresAtEpochMs > now + 60_000L) {
                return@withContext cached.url
            }
        }

        // 1. Ensure authenticated session
        SupabaseSessionManager.ensureActiveSession(context)
        val bearerAuth = SupabaseSessionManager.getAuthenticatedBearerToken(context)
        if (bearerAuth == null) {
            Log.w(TAG, "resolveSignedEvidenceUrl: No authenticated session available; cannot request signed URL.")
            return@withContext null
        }

        // 2. Extract bucket and object path
        val parsed = extractBucketAndPath(trimmed) ?: run {
            Log.w(TAG, "resolveSignedEvidenceUrl: Could not parse bucket/path from '$trimmed'")
            return@withContext null
        }
        val (bucket, objectPath) = parsed

        // 3. Request temporary signed URL via authenticated Supabase Storage client
        val baseUrl = BuildConfig.SUPABASE_URL.trimEnd('/')
        val prefix = if (baseUrl.startsWith("http")) baseUrl else "https://$baseUrl"

        try {
            val response = SupabaseClient.api.createSignedUrl(
                auth = bearerAuth,
                bucket = bucket,
                filename = objectPath,
                body = CreateSignedUrlRequest(expiresIn = expiresInSeconds)
            )

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val signedPath = body.getResolvedPath()
                if (!signedPath.isNullOrBlank()) {
                    val formattedPath = if (signedPath.startsWith("/")) signedPath else "/$signedPath"
                    val fullSignedUrl = if (formattedPath.startsWith("/storage/v1")) {
                        "$prefix$formattedPath"
                    } else {
                        "$prefix/storage/v1$formattedPath"
                    }

                    Log.d(TAG, "resolveSignedEvidenceUrl: Successfully generated signed URL for $bucket/$objectPath")
                    synchronized(signedUrlCache) {
                        if (signedUrlCache.size > 200) signedUrlCache.clear()
                        signedUrlCache[trimmed] = CachedSignedUrl(
                            url = fullSignedUrl,
                            expiresAtEpochMs = now + (expiresInSeconds * 1000L)
                        )
                    }
                    return@withContext fullSignedUrl
                } else {
                    Log.w(TAG, "resolveSignedEvidenceUrl: Success response had null signedURL for $bucket/$objectPath")
                }
            } else {
                Log.w(TAG, "resolveSignedEvidenceUrl: createSignedUrl denied/failed (${response.code()}): ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "resolveSignedEvidenceUrl: Exception requesting signed URL for $bucket/$objectPath", e)
        }

        // Never fall back to a public URL for report evidence in private bucket
        return@withContext null
    }

    /**
     * Downloads an evidence file securely to local cache using authenticated storage request.
     * Used for audio clips to guarantee playback without native player crashes.
     */
    suspend fun downloadEvidenceFile(
        context: Context,
        rawPathOrUrl: String?,
        targetFile: File
    ): Boolean = withContext(Dispatchers.IO) {
        if (rawPathOrUrl.isNullOrBlank()) return@withContext false
        val trimmed = rawPathOrUrl.trim()

        SupabaseSessionManager.ensureActiveSession(context)
        val bearerAuth = SupabaseSessionManager.getAuthenticatedBearerToken(context)
        if (bearerAuth == null) {
            Log.w(TAG, "downloadEvidenceFile: Cannot download without authenticated admin session")
            return@withContext false
        }

        val parsed = extractBucketAndPath(trimmed)

        // Method 1: Direct authenticated Storage download
        if (parsed != null) {
            val (bucket, objectPath) = parsed
            try {
                val directRes = SupabaseClient.api.downloadStorageObject(
                    auth = bearerAuth,
                    bucket = bucket,
                    filename = objectPath
                )
                if (directRes.isSuccessful && directRes.body() != null) {
                    val bytes = directRes.body()!!.bytes()
                    if (bytes.isNotEmpty() && !isErrorBytes(bytes)) {
                        FileOutputStream(targetFile).use { fos ->
                            fos.write(bytes)
                            fos.flush()
                        }
                        Log.d(TAG, "downloadEvidenceFile: Direct authenticated download succeeded for $bucket/$objectPath (${bytes.size} bytes)")
                        return@withContext true
                    }
                } else {
                    Log.d(TAG, "downloadEvidenceFile: Direct authenticated download returned ${directRes.code()}")
                }
            } catch (e: Exception) {
                Log.w(TAG, "downloadEvidenceFile: Direct download attempt failed: ${e.message}")
            }
        }

        // Method 2: Generate temporary signed URL and download with signed token
        val signedUrl = resolveSignedEvidenceUrl(context, trimmed)
        if (!signedUrl.isNullOrBlank()) {
            try {
                val req = Request.Builder()
                    .url(signedUrl)
                    .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
                    .get()
                    .build()
                val callRes = httpClient.newCall(req).execute()
                if (callRes.isSuccessful && callRes.body != null) {
                    val bytes = callRes.body!!.bytes()
                    if (bytes.isNotEmpty() && !isErrorBytes(bytes)) {
                        FileOutputStream(targetFile).use { fos ->
                            fos.write(bytes)
                            fos.flush()
                        }
                        Log.d(TAG, "downloadEvidenceFile: Signed URL download succeeded (${bytes.size} bytes)")
                        return@withContext true
                    }
                }
                callRes.close()
            } catch (e: Exception) {
                Log.w(TAG, "downloadEvidenceFile: Signed URL download attempt failed: ${e.message}")
            }
        }

        return@withContext false
    }

    /**
     * Helper for backward compatibility with upload screen or external callers.
     */
    suspend fun resolveMediaUrl(rawUrl: String?, context: Context? = null): String? {
        val ctx = context ?: try {
            FunkyTalkApplication.appContext
        } catch (_: Exception) {
            null
        }
        return if (ctx != null) {
            resolveSignedEvidenceUrl(ctx, rawUrl)
        } else {
            null
        }
    }

    private fun isErrorBytes(bytes: ByteArray): Boolean {
        if (bytes.isEmpty()) return true
        val checkLen = minOf(bytes.size, 256)
        val snippet = String(bytes, 0, checkLen).trim()
        return snippet.startsWith("{") ||
               snippet.startsWith("<") ||
               snippet.contains("\"error\"", ignoreCase = true) ||
               snippet.contains("\"message\"", ignoreCase = true) ||
               snippet.contains("<!DOCTYPE", ignoreCase = true) ||
               snippet.contains("<html", ignoreCase = true)
    }
}

package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.BufferedSink
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.roundToInt

class MomentUploadException(
    override val message: String,
    val isValidationError: Boolean = false
) : Exception(message)

object CloudinaryUploader {
    private const val TAG = "CloudinaryUploader"
    private const val MOMENT_TAG = "MomentUpload"

    private const val MAX_PHOTO_BYTES = 10L * 1024L * 1024L
    private const val MAX_VIDEO_BYTES = 50L * 1024L * 1024L
    private const val MAX_VIDEO_DURATION_SEC = 60
    private const val PHOTO_MAX_LONG_EDGE = 1600
    private const val PHOTO_JPEG_QUALITY = 85
    private const val SIGNATURE_MAX_AGE_MS = 30L * 60L * 1000L

    private data class CachedMomentSignature(
        val cacheKey: String,
        val data: CloudinarySignatureResponse,
        val fetchedAtMs: Long
    )

    private val sigLock = Any()

    @Volatile
    private var cachedMomentSignature: CachedMomentSignature? = null

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .callTimeout(5, TimeUnit.MINUTES)
        .build()

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    private class CountingFileRequestBody(
        private val file: File,
        private val contentType: MediaType?,
        private val onProgress: (Int) -> Unit
    ) : RequestBody() {
        override fun contentType(): MediaType? = contentType

        override fun contentLength(): Long = file.length()

        override fun writeTo(sink: BufferedSink) {
            val totalBytes = contentLength().coerceAtLeast(1L)
            val buffer = ByteArray(32 * 1024)
            var uploadedBytes = 0L
            var lastReportedPct = -1
            onProgress(0)
            FileInputStream(file).use { input ->
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    sink.write(buffer, 0, read)
                    uploadedBytes += read
                    val pct = ((uploadedBytes * 100L) / totalBytes).toInt().coerceIn(0, 100)
                    if (pct != lastReportedPct) {
                        lastReportedPct = pct
                        onProgress(pct)
                    }
                }
            }
        }
    }

    private fun parseSignatureJson(rawBody: String?): CloudinarySignatureResponse? {
        if (rawBody.isNullOrBlank()) return null
        return try {
            val obj = JSONObject(rawBody)
            val signature = obj.optString("signature", "")
            val timestamp = obj.optLong("timestamp", 0L)
            val apiKey = obj.optString("api_key", "")
            val cloudName = obj.optString("cloud_name", "")
            val folder = obj.optString("folder", "")
            if (signature.isBlank() || timestamp <= 0L || apiKey.isBlank() || cloudName.isBlank() || folder.isBlank()) {
                null
            } else {
                CloudinarySignatureResponse(
                    signature = signature,
                    timestamp = timestamp,
                    api_key = apiKey,
                    cloud_name = cloudName,
                    folder = folder
                )
            }
        } catch (e: Exception) {
            Log.e(MOMENT_TAG, "Failed parsing signature JSON: ${e.message}", e)
            null
        }
    }

    suspend fun uploadImageBytes(
        context: Context,
        token: String,
        imageBytes: ByteArray,
        folderName: String = "profile_media",
        mimeType: String = "image/jpeg"
    ): String? = withContext(Dispatchers.IO) {
        try {
            val sigResp = SupabaseClient.api.getCloudinarySignature(
                apiKey = BuildConfig.SUPABASE_ANON_KEY,
                auth = token
            )
            val rawSigBody = sigResp.body()?.string()
            val sigData = if (sigResp.isSuccessful) parseSignatureJson(rawSigBody) else null
            if (!sigResp.isSuccessful || sigData == null) {
                Log.e(TAG, "Failed to fetch Cloudinary signature: code=${sigResp.code()} err=${sigResp.errorBody()?.string() ?: rawSigBody}")
                return@withContext null
            }

            val resourceType = if (mimeType.contains("video", ignoreCase = true)) "video" else "image"
            val uploadUrl = "https://api.cloudinary.com/v1_1/${sigData.cloud_name}/$resourceType/upload"
            val targetFolder = if (sigData.folder.isNotBlank()) sigData.folder else folderName

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", "file.jpg", imageBytes.toRequestBody(mimeType.toMediaTypeOrNull()))
                .addFormDataPart("api_key", sigData.api_key)
                .addFormDataPart("timestamp", sigData.timestamp.toString())
                .addFormDataPart("signature", sigData.signature)
                .addFormDataPart("folder", targetFolder)
                .build()

            val request = Request.Builder().url(uploadUrl).post(requestBody).build()
            client.newCall(request).execute().use { response ->
                val respBodyStr = response.body?.string()
                if (response.isSuccessful && respBodyStr != null) {
                    val uploadResp = moshi.adapter(CloudinaryUploadResponse::class.java).fromJson(respBodyStr)
                    val url = uploadResp?.secure_url
                    Log.d(TAG, "Successfully uploaded media to Cloudinary: $url")
                    return@withContext url
                } else {
                    Log.e(TAG, "Cloudinary upload request failed: code=${response.code} body=$respBodyStr")
                    return@withContext null
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Exception uploading media bytes to Cloudinary", e)
            return@withContext null
        }
    }

    suspend fun uploadMomentMedia(
        context: Context,
        token: String,
        uri: Uri,
        onProgress: (Int) -> Unit = {}
    ): Result<MomentMediaDto> = withContext(Dispatchers.IO) {
        try {
            val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
            val isVideo = mimeType.startsWith("video", ignoreCase = true)

            if (!isVideo) {
                val jpegBytes = try {
                    decodeDownscaleCompressPhoto(context, uri)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: MomentUploadException) {
                    return@withContext Result.failure(e)
                } catch (e: Exception) {
                    Log.e(MOMENT_TAG, "Photo compression failed: ${e.javaClass.simpleName}: ${e.message}", e)
                    TelemetryManager.recordMediaDecodeFailure("moment_photo", e)
                    // TODO remove detail before release
                    return@withContext Result.failure(
                        MomentUploadException("Upload failed (${e.javaClass.simpleName}: ${e.message ?: "compression error"})")
                    )
                }

                if (jpegBytes.size.toLong() > MAX_PHOTO_BYTES) {
                    val mb = String.format(java.util.Locale.US, "%.1f", jpegBytes.size / (1024.0 * 1024.0))
                    val msg = "Photo is $mb MB (max 10 MB)"
                    Log.w(MOMENT_TAG, msg)
                    return@withContext Result.failure(MomentUploadException(msg, isValidationError = true))
                }

                val sigData = getOrFetchMomentSignature(context, token)
                val uploaded = uploadToCloudinaryAsync(
                    sigData = sigData,
                    resourceType = "image",
                    fileName = "moment_${System.currentTimeMillis()}.jpg",
                    requestBody = jpegBytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
                )
                val url = uploaded.secure_url?.takeIf { it.isNotBlank() }
                    ?: throw MomentUploadException(
                        // TODO remove detail before release
                        "Upload failed (IllegalStateException: Missing secure_url in Cloudinary response)"
                    )

                return@withContext Result.success(
                    MomentMediaDto(
                        type = "image",
                        url = url,
                        width = uploaded.width,
                        height = uploaded.height
                    )
                )
            } else {
                val declaredSize = queryUriSizeBytes(context, uri)
                if (declaredSize != null && declaredSize > MAX_VIDEO_BYTES) {
                    val msg = "Video is too large (max 50 MB)"
                    Log.w(MOMENT_TAG, "$msg (declaredSize=$declaredSize)")
                    return@withContext Result.failure(MomentUploadException(msg, isValidationError = true))
                }

                val tempFile = File(context.cacheDir, "moment_vid_${System.currentTimeMillis()}.mp4")
                try {
                    val copiedBytes = copyUriToFileWithLimit(context, uri, tempFile, MAX_VIDEO_BYTES)
                    if (copiedBytes > MAX_VIDEO_BYTES) {
                        val msg = "Video is too large (max 50 MB)"
                        Log.w(MOMENT_TAG, "$msg (copiedBytes=$copiedBytes)")
                        return@withContext Result.failure(MomentUploadException(msg, isValidationError = true))
                    }

                    val durationMs = extractVideoDurationMs(tempFile, context, uri)
                    if (durationMs != null && durationMs > (MAX_VIDEO_DURATION_SEC * 1000L + 500L)) {
                        val secs = (durationMs / 1000.0).roundToInt()
                        val msg = "Video is ${secs}s (max 60 seconds)"
                        Log.w(MOMENT_TAG, msg)
                        return@withContext Result.failure(MomentUploadException(msg, isValidationError = true))
                    }

                    val videoMime = mimeType.takeIf { it.contains("/") && it != "video/*" } ?: "video/mp4"
                    val sigData = getOrFetchMomentSignature(context, token)
                    val videoRequestBody = CountingFileRequestBody(
                        file = tempFile,
                        contentType = videoMime.toMediaTypeOrNull(),
                        onProgress = onProgress
                    )
                    val uploadedVideo = uploadToCloudinaryAsync(
                        sigData = sigData,
                        resourceType = "video",
                        fileName = "moment_${System.currentTimeMillis()}.mp4",
                        requestBody = videoRequestBody
                    )

                    val videoUrl = uploadedVideo.secure_url?.takeIf { it.isNotBlank() }
                        ?: throw MomentUploadException(
                            // TODO remove detail before release
                            "Upload failed (IllegalStateException: Missing secure_url in Cloudinary video response)"
                        )
                    val durationSec = uploadedVideo.duration?.roundToInt()?.coerceAtLeast(1)
                    val thumbUrl = deriveVideoThumbUrl(videoUrl)

                    return@withContext Result.success(
                        MomentMediaDto(
                            type = "video",
                            url = videoUrl,
                            thumb_url = thumbUrl,
                            width = uploadedVideo.width,
                            height = uploadedVideo.height,
                            duration_sec = durationSec
                        )
                    )
                } finally {
                    tempFile.delete()
                }
            }
        } catch (e: CancellationException) {
            Log.i(MOMENT_TAG, "Moment media upload cancelled")
            throw e
        } catch (e: MomentUploadException) {
            Log.e(MOMENT_TAG, "Moment media upload error: ${e.message}", e)
            return@withContext Result.failure(e)
        } catch (e: Exception) {
            Log.e(MOMENT_TAG, "Unexpected exception uploading moment media: ${e.javaClass.simpleName}: ${e.message}", e)
            // TODO remove detail before release
            return@withContext Result.failure(
                MomentUploadException("Upload failed (${e.javaClass.simpleName}: ${e.message ?: "unknown error"})")
            )
        }
    }

    private fun decodeDownscaleCompressPhoto(context: Context, uri: Uri): ByteArray {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        val decoded = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            val w = info.size.width
            val h = info.size.height
            val longEdge = maxOf(w, h)
            if (longEdge > PHOTO_MAX_LONG_EDGE && w > 0 && h > 0) {
                val scale = PHOTO_MAX_LONG_EDGE.toFloat() / longEdge.toFloat()
                decoder.setTargetSize(
                    (w * scale).roundToInt().coerceAtLeast(1),
                    (h * scale).roundToInt().coerceAtLeast(1)
                )
            }
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }

        val bitmapForJpeg = if (decoded.hasAlpha()) {
            val flattened = Bitmap.createBitmap(decoded.width, decoded.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(flattened)
            canvas.drawColor(Color.WHITE)
            canvas.drawBitmap(decoded, 0f, 0f, null)
            decoded.recycle()
            flattened
        } else {
            decoded
        }

        return try {
            ByteArrayOutputStream().use { baos ->
                if (!bitmapForJpeg.compress(Bitmap.CompressFormat.JPEG, PHOTO_JPEG_QUALITY, baos)) {
                    throw MomentUploadException(
                        // TODO remove detail before release
                        "Upload failed (IllegalStateException: Failed to compress JPEG)"
                    )
                }
                baos.toByteArray()
            }
        } finally {
            if (!bitmapForJpeg.isRecycled) {
                bitmapForJpeg.recycle()
            }
        }
    }

    private fun queryUriSizeBytes(context: Context, uri: Uri): Long? {
        try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (idx >= 0 && !cursor.isNull(idx)) {
                        val size = cursor.getLong(idx)
                        if (size > 0L) return size
                    }
                }
            }
        } catch (_: Exception) {}
        return try {
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { afd ->
                afd.length.takeIf { it > 0L }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun copyUriToFileWithLimit(
        context: Context,
        uri: Uri,
        destFile: File,
        maxBytes: Long
    ): Long {
        val input = context.contentResolver.openInputStream(uri)
            ?: throw MomentUploadException(
                // TODO remove detail before release
                "Upload failed (IOException: Unable to open selected video)"
            )
        var total = 0L
        input.use { src ->
            FileOutputStream(destFile).use { dst ->
                val buf = ByteArray(16 * 1024)
                while (true) {
                    val read = src.read(buf)
                    if (read <= 0) break
                    total += read
                    if (total > maxBytes) {
                        return total
                    }
                    dst.write(buf, 0, read)
                }
            }
        }
        return total
    }

    private fun extractVideoDurationMs(file: File, context: Context, uri: Uri): Long? {
        val retriever = MediaMetadataRetriever()
        return try {
            try {
                retriever.setDataSource(file.absolutePath)
            } catch (_: Exception) {
                retriever.setDataSource(context, uri)
            }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
        } catch (e: Exception) {
            Log.w(MOMENT_TAG, "Unable to read local video duration before upload: ${e.message}")
            TelemetryManager.recordMediaDecodeFailure("moment_video_duration", e)
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }
    }

    private fun deriveVideoThumbUrl(secureUrl: String): String {
        val withTransform = secureUrl.replaceFirst("/video/upload/", "/video/upload/so_0,f_jpg/")
        val queryIdx = withTransform.indexOf('?')
        val basePart = if (queryIdx >= 0) withTransform.substring(0, queryIdx) else withTransform
        val queryPart = if (queryIdx >= 0) withTransform.substring(queryIdx) else ""
        val slashIdx = basePart.lastIndexOf('/')
        val dotIdx = basePart.lastIndexOf('.')
        val replacedExt = if (dotIdx > slashIdx) {
            basePart.substring(0, dotIdx) + ".jpg"
        } else {
            "$basePart.jpg"
        }
        return replacedExt + queryPart
    }

    private suspend fun getOrFetchMomentSignature(
        context: Context,
        token: String
    ): CloudinarySignatureResponse {
        val cacheKey = SupabaseSessionManager.getUserId(context) ?: token
        val now = System.currentTimeMillis()
        synchronized(sigLock) {
            val cached = cachedMomentSignature
            if (cached != null &&
                cached.cacheKey == cacheKey &&
                (now - cached.fetchedAtMs) < SIGNATURE_MAX_AGE_MS
            ) {
                Log.d(
                    MOMENT_TAG,
                    "Reusing cached signature: cloud_name=${cached.data.cloud_name} folder=${cached.data.folder} timestamp=${cached.data.timestamp}"
                )
                return cached.data
            }
        }

        val sigResp = try {
            SupabaseClient.api.getCloudinarySignature(
                apiKey = BuildConfig.SUPABASE_ANON_KEY,
                auth = token
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(MOMENT_TAG, "get-cloudinary-signature exception: ${e.javaClass.simpleName}: ${e.message}", e)
            // TODO remove detail before release
            throw MomentUploadException(
                "Upload failed (${e.javaClass.simpleName}: ${e.message ?: "signature request error"})"
            )
        }

        val rawBody = try {
            if (sigResp.isSuccessful) sigResp.body()?.string() else sigResp.errorBody()?.string()
        } catch (e: Exception) {
            null
        }

        val sigBody = if (sigResp.isSuccessful) parseSignatureJson(rawBody) else null
        if (!sigResp.isSuccessful || sigBody == null) {
            val errMsg = try {
                rawBody?.let { JSONObject(it).optString("error").ifBlank { JSONObject(it).optString("message") } }
            } catch (_: Exception) {
                null
            }?.takeIf { it.isNotBlank() } ?: rawBody?.take(200) ?: "Invalid signature response"

            Log.e(
                MOMENT_TAG,
                "get-cloudinary-signature failed: http=${sigResp.code()} body=$rawBody"
            )
            TelemetryManager.recordCloudinaryFailure("signature", "HTTP ${sigResp.code()}: $errMsg")
            // TODO remove detail before release
            throw MomentUploadException("Upload failed (${sigResp.code()}: $errMsg)")
        }

        // Log response fields WITHOUT signature value
        Log.i(
            MOMENT_TAG,
            "get-cloudinary-signature ok: http=${sigResp.code()} cloud_name=${sigBody.cloud_name} folder=${sigBody.folder} timestamp=${sigBody.timestamp} api_key_len=${sigBody.api_key.length}"
        )

        synchronized(sigLock) {
            cachedMomentSignature = CachedMomentSignature(
                cacheKey = cacheKey,
                data = sigBody,
                fetchedAtMs = System.currentTimeMillis()
            )
        }
        return sigBody
    }

    private suspend fun executeCallCancellable(call: Call): Pair<Int, String?> =
        suspendCancellableCoroutine { cont ->
            cont.invokeOnCancellation {
                try {
                    call.cancel()
                } catch (_: Exception) {}
            }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (cont.isActive) {
                        cont.resumeWithException(e)
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use { resp ->
                        val bodyStr = try {
                            resp.body?.string()
                        } catch (e: Exception) {
                            if (cont.isActive) cont.resumeWithException(e)
                            return
                        }
                        if (cont.isActive) {
                            cont.resume(resp.code to bodyStr)
                        }
                    }
                }
            })
        }

    private suspend fun uploadToCloudinaryAsync(
        sigData: CloudinarySignatureResponse,
        resourceType: String,
        fileName: String,
        requestBody: RequestBody
    ): CloudinaryUploadResponse {
        val uploadUrl = "https://api.cloudinary.com/v1_1/${sigData.cloud_name}/$resourceType/upload"
        Log.i(
            MOMENT_TAG,
            "Starting Cloudinary upload: url=$uploadUrl cloud_name=${sigData.cloud_name} folder=${sigData.folder} timestamp=${sigData.timestamp}"
        )

        val multipart = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", fileName, requestBody)
            .addFormDataPart("api_key", sigData.api_key)
            .addFormDataPart("timestamp", sigData.timestamp.toString())
            .addFormDataPart("signature", sigData.signature)
            .addFormDataPart("folder", sigData.folder)
            .build()

        val request = Request.Builder().url(uploadUrl).post(multipart).build()
        val (httpCode, respBodyStr) = try {
            executeCallCancellable(client.newCall(request))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(
                MOMENT_TAG,
                "Cloudinary call exception: url=$uploadUrl cloud_name=${sigData.cloud_name} folder=${sigData.folder} err=${e.javaClass.simpleName}: ${e.message}",
                e
            )
            // TODO remove detail before release
            throw MomentUploadException("Upload failed (${e.javaClass.simpleName}: ${e.message ?: "network error"})")
        }

        if (httpCode in 200..299 && !respBodyStr.isNullOrBlank()) {
            Log.i(
                MOMENT_TAG,
                "Cloudinary upload success: url=$uploadUrl http=$httpCode cloud_name=${sigData.cloud_name} folder=${sigData.folder}"
            )
            return try {
                moshi.adapter(CloudinaryUploadResponse::class.java).fromJson(respBodyStr)
                    ?: throw MomentUploadException(
                        // TODO remove detail before release
                        "Upload failed (JsonDataException: Null Cloudinary response object)"
                    )
            } catch (e: MomentUploadException) {
                throw e
            } catch (e: Exception) {
                Log.e(MOMENT_TAG, "Failed parsing Cloudinary response JSON: url=$uploadUrl http=$httpCode body=$respBodyStr", e)
                // TODO remove detail before release
                throw MomentUploadException("Upload failed (${e.javaClass.simpleName}: ${e.message ?: "parse error"})")
            }
        }

        val parsedMessage = try {
            respBodyStr?.let { JSONObject(it).optJSONObject("error")?.optString("message") }
        } catch (_: Exception) {
            null
        }?.takeIf { it.isNotBlank() } ?: respBodyStr?.take(200) ?: "HTTP $httpCode"

        Log.e(
            MOMENT_TAG,
            "Cloudinary upload failed: url=$uploadUrl cloud_name=${sigData.cloud_name} folder=${sigData.folder} http=$httpCode message=$parsedMessage body=$respBodyStr"
        )
        TelemetryManager.recordCloudinaryFailure("upload", "HTTP $httpCode: $parsedMessage")

        if (httpCode == 401 || parsedMessage.contains("signature", ignoreCase = true)) {
            synchronized(sigLock) { cachedMomentSignature = null }
        }

        // TODO remove detail before release
        throw MomentUploadException("Upload failed ($httpCode: $parsedMessage)")
    }
}

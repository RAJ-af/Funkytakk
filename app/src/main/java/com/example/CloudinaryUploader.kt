package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

object CloudinaryUploader {
    private const val TAG = "CloudinaryUploader"
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

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
            if (!sigResp.isSuccessful || sigResp.body() == null) {
                Log.e(TAG, "Failed to fetch Cloudinary signature: code=${sigResp.code()} err=${sigResp.errorBody()?.string()}")
                return@withContext null
            }

            val sigData = sigResp.body()!!
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
            val response = client.newCall(request).execute()
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
        } catch (e: Exception) {
            Log.e(TAG, "Exception uploading media bytes to Cloudinary", e)
            return@withContext null
        }
    }

    suspend fun uploadMomentMedia(
        context: Context,
        token: String,
        uri: Uri
    ): MomentMediaDto? = withContext(Dispatchers.IO) {
        try {
            val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
            val isVideo = mimeType.startsWith("video", ignoreCase = true)

            if (!isVideo) {
                val rawBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: return@withContext null
                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, opts)
                val width = opts.outWidth.takeIf { it > 0 }
                val height = opts.outHeight.takeIf { it > 0 }

                val ext = when {
                    mimeType.contains("png", ignoreCase = true) -> "png"
                    mimeType.contains("webp", ignoreCase = true) -> "webp"
                    mimeType.contains("gif", ignoreCase = true) -> "gif"
                    else -> "jpg"
                }
                val uploaded = uploadPartToCloudinary(
                    token = token,
                    resourceType = "image",
                    fileName = "moment_${System.currentTimeMillis()}.$ext",
                    requestBody = rawBytes.toRequestBody(mimeType.toMediaTypeOrNull())
                ) ?: return@withContext null

                val url = uploaded.secure_url?.takeIf { it.isNotBlank() } ?: return@withContext null
                return@withContext MomentMediaDto(
                    type = "image",
                    url = url,
                    width = uploaded.width ?: width,
                    height = uploaded.height ?: height
                )
            } else {
                val tempFile = File(context.cacheDir, "moment_vid_${System.currentTimeMillis()}.mp4")
                try {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(tempFile).use { output ->
                            input.copyTo(output)
                        }
                    } ?: return@withContext null

                    var metaWidth: Int? = null
                    var metaHeight: Int? = null
                    var metaDurationSec: Int? = null
                    var thumbBytes: ByteArray? = null

                    val retriever = MediaMetadataRetriever()
                    try {
                        retriever.setDataSource(context, uri)
                        metaWidth = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull()
                        metaHeight = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull()
                        val durMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
                        if (durMs != null && durMs > 0L) {
                            metaDurationSec = (durMs / 1000.0).roundToInt().coerceAtLeast(1)
                        }
                        val frame = retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                        if (frame != null) {
                            ByteArrayOutputStream().use { baos ->
                                frame.compress(Bitmap.CompressFormat.JPEG, 82, baos)
                                thumbBytes = baos.toByteArray()
                            }
                            frame.recycle()
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed extracting video metadata/thumbnail", e)
                    } finally {
                        try {
                            retriever.release()
                        } catch (_: Exception) {}
                    }

                    val uploadedVideo = uploadPartToCloudinary(
                        token = token,
                        resourceType = "video",
                        fileName = "moment_${System.currentTimeMillis()}.mp4",
                        requestBody = tempFile.asRequestBody(mimeType.toMediaTypeOrNull())
                    ) ?: return@withContext null

                    val videoUrl = uploadedVideo.secure_url?.takeIf { it.isNotBlank() } ?: return@withContext null

                    val uploadedThumbUrl = thumbBytes?.let { bytes ->
                        uploadPartToCloudinary(
                            token = token,
                            resourceType = "image",
                            fileName = "moment_thumb_${System.currentTimeMillis()}.jpg",
                            requestBody = bytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
                        )?.secure_url?.takeIf { it.isNotBlank() }
                    }

                    val finalDuration = uploadedVideo.duration?.roundToInt()?.takeIf { it > 0 } ?: metaDurationSec
                    return@withContext MomentMediaDto(
                        type = "video",
                        url = videoUrl,
                        thumb_url = uploadedThumbUrl,
                        width = uploadedVideo.width ?: metaWidth,
                        height = uploadedVideo.height ?: metaHeight,
                        duration_sec = finalDuration
                    )
                } finally {
                    tempFile.delete()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception uploading moment media to Cloudinary", e)
            return@withContext null
        }
    }

    private suspend fun uploadPartToCloudinary(
        token: String,
        resourceType: String,
        fileName: String,
        requestBody: RequestBody
    ): CloudinaryUploadResponse? {
        val sigResp = SupabaseClient.api.getCloudinarySignature(
            apiKey = BuildConfig.SUPABASE_ANON_KEY,
            auth = token
        )
        if (!sigResp.isSuccessful || sigResp.body() == null) {
            Log.e(TAG, "Failed to fetch Cloudinary signature: code=${sigResp.code()} err=${sigResp.errorBody()?.string()}")
            return null
        }

        val sigData = sigResp.body()!!
        val uploadUrl = "https://api.cloudinary.com/v1_1/${sigData.cloud_name}/$resourceType/upload"
        val multipart = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", fileName, requestBody)
            .addFormDataPart("api_key", sigData.api_key)
            .addFormDataPart("timestamp", sigData.timestamp.toString())
            .addFormDataPart("signature", sigData.signature)
            .apply {
                if (sigData.folder.isNotBlank()) {
                    addFormDataPart("folder", sigData.folder)
                }
            }
            .build()

        val request = Request.Builder().url(uploadUrl).post(multipart).build()
        val response = client.newCall(request).execute()
        val respBodyStr = response.body?.string()
        if (response.isSuccessful && respBodyStr != null) {
            return moshi.adapter(CloudinaryUploadResponse::class.java).fromJson(respBodyStr)
        } else {
            Log.e(TAG, "Cloudinary upload failed ($resourceType): code=${response.code} body=$respBodyStr")
            return null
        }
    }
}


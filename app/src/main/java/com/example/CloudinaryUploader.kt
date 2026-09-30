package com.example

import android.content.Context
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

object CloudinaryUploader {
    private const val TAG = "CloudinaryUploader"
    private val client = OkHttpClient()
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
                .addFormDataPart("file", "file.jpg", RequestBody.create(mimeType.toMediaTypeOrNull(), imageBytes))
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
}

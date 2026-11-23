package com.example

import android.content.Context
import android.util.Log
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

object AppwriteUploader {
    private const val TAG = "AppwriteUploader"

    sealed class UploadResult {
        data class Success(val fileId: String, val url: String) : UploadResult()
        data class Error(val message: String, val statusCode: Int = 0) : UploadResult()
    }

    /**
     * Uploads media bytes to Appwrite Storage via Supabase edge function proxy `upload-to-appwrite`.
     *
     * @param context Android context to access sessions
     * @param fileBytes Raw bytes or encrypted ciphertext payload
     * @param filename Filename to assign to the multipart file part
     * @param mimeType MIME type of the file (e.g. image/jpeg, application/octet-stream)
     * @param purpose One of "avatar", "cover", "dm-media", "group-media"
     * @param folderSuffix ConversationId for "dm-media", GroupId for "group-media", or null for avatar/cover
     * @param maxRetries Maximum automatic retries (defaults to 1 per requirement)
     */
    suspend fun upload(
        context: Context,
        fileBytes: ByteArray,
        filename: String,
        mimeType: String,
        purpose: String,
        folderSuffix: String? = null,
        maxRetries: Int = 1
    ): UploadResult {
        // Ensure active session & get token
        SupabaseSessionManager.ensureActiveSession(context)
        val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
            ?: (SupabaseSessionManager.getAccessToken(context)?.let { "Bearer $it" })

        if (token.isNullOrBlank()) {
            return UploadResult.Error("Not authenticated. Please sign in again.", 401)
        }

        return uploadWithToken(
            token = token,
            fileBytes = fileBytes,
            filename = filename,
            mimeType = mimeType,
            purpose = purpose,
            folderSuffix = folderSuffix,
            maxRetries = maxRetries
        )
    }

    suspend fun uploadWithToken(
        token: String,
        fileBytes: ByteArray,
        filename: String,
        mimeType: String,
        purpose: String,
        folderSuffix: String? = null,
        maxRetries: Int = 1
    ): UploadResult {
        var attempts = 0
        val effectiveAuth = if (token.startsWith("Bearer ")) token else "Bearer $token"

        while (true) {
            attempts++
            try {
                val fileRequestBody = fileBytes.toRequestBody(mimeType.toMediaTypeOrNull(), 0, fileBytes.size)
                val filePart = MultipartBody.Part.createFormData("file", filename, fileRequestBody)

                val textMediaType = "text/plain".toMediaTypeOrNull()
                val purposeBody = purpose.toRequestBody(textMediaType)
                val folderSuffixBody = if (!folderSuffix.isNullOrBlank()) {
                    folderSuffix.toRequestBody(textMediaType)
                } else null

                val response = SupabaseClient.api.uploadToAppwrite(
                    apiKey = BuildConfig.SUPABASE_ANON_KEY,
                    auth = effectiveAuth,
                    file = filePart,
                    purpose = purposeBody,
                    folderSuffix = folderSuffixBody
                )

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    val url = body.url
                    val fileId = body.file_id.orEmpty()
                    if (body.ok == true && !url.isNullOrBlank()) {
                        Log.d(TAG, "upload-to-appwrite success (purpose=$purpose): url=$url, file_id=$fileId")
                        return UploadResult.Success(fileId = fileId, url = url)
                    } else {
                        val errMsg = body.error ?: body.message ?: "Upload rejected"
                        Log.e(TAG, "upload-to-appwrite returned ok=false: $errMsg")
                        return UploadResult.Error(errMsg, response.code())
                    }
                } else {
                    val statusCode = response.code()
                    val errBody = response.errorBody()?.string().orEmpty()
                    val userMessage = com.example.util.ErrorMapper.mapHttp(statusCode, errBody)

                    Log.e(TAG, "upload-to-appwrite failed with code $statusCode")

                    // Don't retry client errors (400, 401, 403) or if maxRetries reached
                    if (statusCode in 400..499 || attempts > maxRetries) {
                        return UploadResult.Error(userMessage, statusCode)
                    }
                    // Transient 5xx retry: only retry once
                    continue
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Exception during upload-to-appwrite (attempt $attempts)", e)
                if (attempts > maxRetries) {
                    return UploadResult.Error(com.example.util.ErrorMapper.map(e), 0)
                }
            }
        }
    }
}

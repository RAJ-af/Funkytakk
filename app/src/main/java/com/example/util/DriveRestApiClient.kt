package com.example.util

import android.accounts.Account
import android.content.Context
import android.util.Log
import com.google.android.gms.auth.GoogleAuthUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSink
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.TimeUnit

data class DriveFileItem(
    val id: String,
    val name: String,
    val mimeType: String,
    val size: Long = 0L
)

class StorageQuotaExceededException(message: String) : Exception(message)

object DriveRestApiClient {
    private const val TAG = "DriveRestApiClient"
    const val SCOPE_APPDATA = "oauth2:https://www.googleapis.com/auth/drive.appdata"

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun getAccessToken(context: Context, accountEmail: String): String = withContext(Dispatchers.IO) {
        val account = Account(accountEmail, "com.google")
        GoogleAuthUtil.getToken(context, account, SCOPE_APPDATA)
    }

    suspend fun listAppDataFiles(
        context: Context,
        accountEmail: String,
        query: String? = null
    ): List<DriveFileItem> = withRetry(context, accountEmail) { token ->
        var url = "https://www.googleapis.com/drive/v3/files?spaces=appDataFolder&fields=files(id,name,mimeType,size)"
        if (!query.isNullOrBlank()) {
            val encodedQ = java.net.URLEncoder.encode(query, "UTF-8")
            url += "&q=$encodedQ"
        }

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            checkResponseErrors(response)
            val bodyStr = response.body?.string().orEmpty()
            val json = JSONObject(bodyStr)
            val filesArr = json.optJSONArray("files") ?: JSONArray()
            val list = mutableListOf<DriveFileItem>()
            for (i in 0 until filesArr.length()) {
                val item = filesArr.getJSONObject(i)
                list.add(
                    DriveFileItem(
                        id = item.getString("id"),
                        name = item.getString("name"),
                        mimeType = item.optString("mimeType", "application/octet-stream"),
                        size = item.optLong("size", 0L)
                    )
                )
            }
            list
        }
    }

    suspend fun uploadSmallFile(
        context: Context,
        accountEmail: String,
        name: String,
        mimeType: String,
        content: ByteArray
    ): DriveFileItem = withRetry(context, accountEmail) { token ->
        val metadataJson = JSONObject().apply {
            put("name", name)
            put("parents", JSONArray().put("appDataFolder"))
        }.toString()

        val multipartBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addPart(
                metadataJson.toRequestBody("application/json; charset=UTF-8".toMediaType())
            )
            .addPart(
                content.toRequestBody(mimeType.toMediaTypeOrNull())
            )
            .build()

        val request = Request.Builder()
            .url("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart&spaces=appDataFolder")
            .header("Authorization", "Bearer $token")
            .post(multipartBody)
            .build()

        client.newCall(request).execute().use { response ->
            checkResponseErrors(response)
            val bodyStr = response.body?.string().orEmpty()
            val json = JSONObject(bodyStr)
            DriveFileItem(
                id = json.getString("id"),
                name = json.optString("name", name),
                mimeType = json.optString("mimeType", mimeType),
                size = content.size.toLong()
            )
        }
    }

    suspend fun uploadResumableFile(
        context: Context,
        accountEmail: String,
        name: String,
        mimeType: String,
        file: File
    ): DriveFileItem = withRetry(context, accountEmail) { token ->
        // Step 1: Initiate Resumable Session
        val metadataJson = JSONObject().apply {
            put("name", name)
            put("parents", JSONArray().put("appDataFolder"))
        }.toString()

        val initRequest = Request.Builder()
            .url("https://www.googleapis.com/upload/drive/v3/files?uploadType=resumable&spaces=appDataFolder")
            .header("Authorization", "Bearer $token")
            .header("X-Upload-Content-Type", mimeType)
            .header("X-Upload-Content-Length", file.length().toString())
            .post(metadataJson.toRequestBody("application/json; charset=UTF-8".toMediaType()))
            .build()

        val locationUrl = client.newCall(initRequest).execute().use { response ->
            checkResponseErrors(response)
            response.header("Location") ?: throw IOException("Missing Resumable Upload Location header")
        }

        // Step 2: Upload File Content to Location URL
        val uploadRequest = Request.Builder()
            .url(locationUrl)
            .put(file.asRequestBody(mimeType.toMediaTypeOrNull()))
            .build()

        client.newCall(uploadRequest).execute().use { response ->
            checkResponseErrors(response)
            val bodyStr = response.body?.string().orEmpty()
            val json = JSONObject(bodyStr)
            DriveFileItem(
                id = json.getString("id"),
                name = json.optString("name", name),
                mimeType = json.optString("mimeType", mimeType),
                size = file.length()
            )
        }
    }

    suspend fun downloadFileContent(
        context: Context,
        accountEmail: String,
        fileId: String
    ): ByteArray = withRetry(context, accountEmail) { token ->
        val request = Request.Builder()
            .url("https://www.googleapis.com/drive/v3/files/$fileId?alt=media")
            .header("Authorization", "Bearer $token")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            checkResponseErrors(response)
            response.body?.bytes() ?: throw IOException("Empty response body from Drive download")
        }
    }

    suspend fun downloadFileToStream(
        context: Context,
        accountEmail: String,
        fileId: String,
        outputStream: OutputStream
    ) = withRetry(context, accountEmail) { token ->
        val request = Request.Builder()
            .url("https://www.googleapis.com/drive/v3/files/$fileId?alt=media")
            .header("Authorization", "Bearer $token")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            checkResponseErrors(response)
            val body = response.body ?: throw IOException("Empty response body from Drive download")
            body.byteStream().use { inputStream ->
                inputStream.copyTo(outputStream)
            }
        }
    }

    suspend fun deleteFile(
        context: Context,
        accountEmail: String,
        fileId: String
    ) = withRetry<Unit>(context, accountEmail) { token ->
        val request = Request.Builder()
            .url("https://www.googleapis.com/drive/v3/files/$fileId")
            .header("Authorization", "Bearer $token")
            .delete()
            .build()

        client.newCall(request).execute().use { response ->
            if (response.code != 204 && response.code != 404) {
                checkResponseErrors(response)
            }
        }
    }

    private fun checkResponseErrors(response: okhttp3.Response) {
        if (!response.isSuccessful) {
            val code = response.code
            val body = response.body?.string().orEmpty()
            Log.e(TAG, "Drive API HTTP Error $code: $body")

            if (code == 403 && body.contains("storageQuotaExceeded", ignoreCase = true)) {
                throw StorageQuotaExceededException("Your Google Drive is full")
            }
            if (code == 401) {
                throw SecurityException("401_UNAUTHORIZED")
            }
            throw IOException("Drive API error $code: $body")
        }
    }

    private suspend fun <T> withRetry(
        context: Context,
        accountEmail: String,
        block: suspend (String) -> T
    ): T = withContext(Dispatchers.IO) {
        var token = getAccessToken(context, accountEmail)
        var attempts = 0
        var lastException: Exception? = null

        while (attempts < 3) {
            try {
                return@withContext block(token)
            } catch (e: SecurityException) {
                if (e.message == "401_UNAUTHORIZED" && attempts == 0) {
                    Log.w(TAG, "Token 401, invalidating and retrying...")
                    GoogleAuthUtil.invalidateToken(context, token)
                    token = getAccessToken(context, accountEmail)
                    attempts++
                } else {
                    throw e
                }
            } catch (e: StorageQuotaExceededException) {
                throw e
            } catch (e: IOException) {
                lastException = e
                attempts++
                if (attempts < 3) {
                    val backoffMs = (1000L * Math.pow(2.0, attempts.toDouble())).toLong()
                    Log.w(TAG, "Drive request failed (attempt $attempts), backing off $backoffMs ms", e)
                    delay(backoffMs)
                }
            }
        }
        throw lastException ?: IOException("Drive API request failed after retries")
    }
}

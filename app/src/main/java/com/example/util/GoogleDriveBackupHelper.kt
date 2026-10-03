package com.example.util

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

data class DriveFileItem(
    val id: String,
    val name: String,
    val size: String? = null,
    val modifiedTime: String? = null
)

data class DriveFileListResponse(
    val files: List<DriveFileItem> = emptyList()
)

object GoogleDriveBackupHelper {
    private const val TAG = "GoogleDriveBackup"
    private const val BACKUP_FILE_NAME = "funkytalk_chat_backup.enc"
    val DRIVE_APPDATA_SCOPE = Scope("https://www.googleapis.com/auth/drive.appdata")

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val fileListAdapter = moshi.adapter(DriveFileListResponse::class.java)

    /**
     * Gets GoogleSignInOptions configured specifically for Drive appDataFolder access.
     */
    fun getGoogleSignInOptions(): GoogleSignInOptions {
        return GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestScopes(DRIVE_APPDATA_SCOPE)
            .requestEmail()
            .build()
    }

    /**
     * Obtains AuthorizationRequest for modern Identity AuthorizationClient API.
     */
    fun getAuthorizationRequest(): AuthorizationRequest {
        return AuthorizationRequest.builder()
            .setRequestedScopes(listOf(DRIVE_APPDATA_SCOPE))
            .build()
    }

    /**
     * Gets current signed in Google account for Drive appDataFolder scope, if present.
     */
    fun getSignedInAccount(context: Context): GoogleSignInAccount? {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        return if (account != null && GoogleSignIn.hasPermissions(account, DRIVE_APPDATA_SCOPE)) {
            account
        } else {
            null
        }
    }

    /**
     * Signs out the connected Google Drive account and revokes cached tokens.
     */
    suspend fun signOut(context: Context) = withContext(Dispatchers.IO) {
        try {
            GoogleSignIn.getClient(context, getGoogleSignInOptions()).signOut().await()
        } catch (e: Exception) {
            Log.w(TAG, "Error signing out GoogleSignInClient", e)
        }
    }

    /**
     * Obtains OAuth Bearer token for Drive API calls on IO thread.
     */
    suspend fun fetchOAuthToken(context: Context, account: GoogleSignInAccount): String? = withContext(Dispatchers.IO) {
        try {
            val accountName = account.account ?: return@withContext null
            val scopeStr = "oauth2:https://www.googleapis.com/auth/drive.appdata"
            GoogleAuthUtil.getToken(context, accountName, scopeStr)
        } catch (e: Exception) {
            Log.e(TAG, "Error obtaining OAuth token for Drive API", e)
            null
        }
    }

    /**
     * Finds the backup file in Drive appDataFolder.
     * Picks the newest file by modifiedTime and deletes any duplicate files (Requirement 8).
     */
    suspend fun findBackupFileId(accessToken: String): DriveFileItem? = withContext(Dispatchers.IO) {
        try {
            val url = "https://www.googleapis.com/drive/v3/files?spaces=appDataFolder&q=name%3D%27$BACKUP_FILE_NAME%27+and+trashed%3Dfalse&fields=files(id,name,size,modifiedTime)"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $accessToken")
                .get()
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.e(TAG, "Error querying Drive appDataFolder: HTTP ${response.code} ${response.message}")
                return@withContext null
            }

            val bodyString = response.body?.string().orEmpty()
            val fileList = fileListAdapter.fromJson(bodyString)
            val files = fileList?.files.orEmpty()

            if (files.isEmpty()) {
                return@withContext null
            }

            // Sort files by modifiedTime descending (newest first)
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            val sortedFiles = files.sortedByDescending { file ->
                val timeStr = file.modifiedTime.orEmpty()
                try {
                    sdf.parse(timeStr)?.time ?: 0L
                } catch (_: Exception) {
                    0L
                }
            }

            val newestFile = sortedFiles.first()

            // Requirement 8: Delete duplicate backup files if present
            if (sortedFiles.size > 1) {
                Log.w(TAG, "Found ${sortedFiles.size} duplicate backup files in appDataFolder. Deleting ${sortedFiles.size - 1} older duplicates...")
                for (duplicate in sortedFiles.drop(1)) {
                    try {
                        deleteBackupFile(accessToken, duplicate.id)
                        Log.d(TAG, "Deleted older duplicate backup file ${duplicate.id}")
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to delete duplicate backup file ${duplicate.id}", e)
                    }
                }
            }

            Log.d(TAG, "Selected newest Drive backup file: ${newestFile.id} size=${newestFile.size} modified=${newestFile.modifiedTime}")
            newestFile
        } catch (e: Exception) {
            Log.e(TAG, "Exception querying Drive appDataFolder", e)
            null
        }
    }

    /**
     * Uploads or updates the encrypted backup file using temp file streaming (Requirement 11).
     */
    suspend fun uploadBackupFile(accessToken: String, encryptedFile: File): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (!encryptedFile.exists() || encryptedFile.length() == 0L) {
                return@withContext Result.failure(IOException("Encrypted backup temp file is missing or empty"))
            }

            val existingFile = findBackupFileId(accessToken)

            if (existingFile != null) {
                // Overwrite / Update existing file via patch
                val url = "https://www.googleapis.com/upload/drive/v3/files/${existingFile.id}?uploadType=media"
                val mediaType = "application/octet-stream".toMediaType()
                val requestBody = encryptedFile.asRequestBody(mediaType)

                val request = Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer $accessToken")
                    .patch(requestBody)
                    .build()

                val response = okHttpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    Log.d(TAG, "Updated existing Drive backup file ${existingFile.id} (${encryptedFile.length()} bytes)")
                    Result.success(existingFile.id)
                } else {
                    val code = response.code
                    if (code == 403 || code == 507) {
                        Result.failure(IOException("Google Drive quota full or storage exceeded."))
                    } else if (code == 401) {
                        Result.failure(IOException("Drive authorization token expired. Please reconnect."))
                    } else {
                        Result.failure(IOException("Failed to upload backup to Drive (HTTP $code)"))
                    }
                }
            } else {
                // Create new file in appDataFolder via multipart
                val url = "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart"
                val metadataJson = """{"name":"$BACKUP_FILE_NAME","parents":["appDataFolder"]}"""

                val multipartBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addPart(metadataJson.toRequestBody("application/json; charset=UTF-8".toMediaType()))
                    .addPart(encryptedFile.asRequestBody("application/octet-stream".toMediaType()))
                    .build()

                val request = Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer $accessToken")
                    .post(multipartBody)
                    .build()

                val response = okHttpClient.newCall(request).execute()
                val responseStr = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val newFile = moshi.adapter(DriveFileItem::class.java).fromJson(responseStr)
                    val newId = newFile?.id.orEmpty()
                    Log.d(TAG, "Created new Drive backup file $newId (${encryptedFile.length()} bytes)")
                    Result.success(newId)
                } else {
                    val code = response.code
                    if (code == 403 || code == 507) {
                        Result.failure(IOException("Google Drive quota full or storage exceeded."))
                    } else {
                        Result.failure(IOException("Failed to create backup in Drive (HTTP $code)"))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading backup to Drive", e)
            Result.failure(e)
        }
    }

    /**
     * Downloads the backup file directly to a destination temp file (Requirement 11 streaming).
     */
    suspend fun downloadBackupFileToTemp(accessToken: String, fileId: String, outputFile: File): Result<File> = withContext(Dispatchers.IO) {
        try {
            outputFile.parentFile?.mkdirs()
            val url = "https://www.googleapis.com/drive/v3/files/$fileId?alt=media"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $accessToken")
                .get()
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val byteStream = response.body?.byteStream()
                if (byteStream != null) {
                    FileOutputStream(outputFile).use { fos ->
                        byteStream.copyTo(fos)
                    }
                    Log.d(TAG, "Downloaded backup file to temp file (${outputFile.length()} bytes)")
                    Result.success(outputFile)
                } else {
                    Result.failure(IOException("Downloaded backup file stream was null"))
                }
            } else {
                Result.failure(IOException("Failed to download backup from Drive (HTTP ${response.code})"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading backup from Drive to temp file", e)
            Result.failure(e)
        }
    }

    /**
     * Overload to upload ByteArray payload by creating a temp file.
     */
    suspend fun uploadBackupFile(accessToken: String, encryptedBytes: ByteArray): Result<String> = withContext(Dispatchers.IO) {
        val tempFile = File.createTempFile("backup_upload_", ".enc")
        try {
            tempFile.writeBytes(encryptedBytes)
            uploadBackupFile(accessToken, tempFile)
        } finally {
            tempFile.delete()
        }
    }

    /**
     * Downloads the backup file as ByteArray.
     */
    suspend fun downloadBackupFile(accessToken: String, fileId: String): Result<ByteArray> = withContext(Dispatchers.IO) {
        val tempFile = File.createTempFile("backup_download_", ".enc")
        try {
            val downloadRes = downloadBackupFileToTemp(accessToken, fileId, tempFile)
            if (downloadRes.isSuccess) {
                Result.success(tempFile.readBytes())
            } else {
                Result.failure(downloadRes.exceptionOrNull() ?: IOException("Failed to download backup"))
            }
        } finally {
            tempFile.delete()
        }
    }
    suspend fun deleteBackupFile(accessToken: String, fileId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val url = "https://www.googleapis.com/drive/v3/files/$fileId"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $accessToken")
                .delete()
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful || response.code == 404) {
                Log.d(TAG, "Deleted backup file $fileId from Drive appDataFolder")
                Result.success(Unit)
            } else {
                Result.failure(IOException("Failed to delete backup from Drive (HTTP ${response.code})"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting backup from Drive", e)
            Result.failure(e)
        }
    }
}

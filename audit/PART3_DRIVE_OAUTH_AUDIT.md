# FUNKYTALK CHAT BACKUP — PART 3
# GOOGLE DRIVE + OAUTH + ACCOUNT SECURITY AUDIT

---

# 1. OAUTH FLOW

### Actual Implementation Tracing
1. **User Action**: Tapping "Connect Google Drive" in `ChatBackupScreen.kt` or `RestoreChatBackupScreen.kt`.
2. **Authorization Launch**:
   ```kotlin
   val gso = GoogleDriveBackupHelper.getGoogleSignInOptions()
   val client = GoogleSignIn.getClient(context, gso)
   googleSignInLauncher.launch(client.signInIntent)
   ```
3. **Requested Scopes Configuration**:
   ```kotlin
   GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
       .requestScopes(Scope("https://www.googleapis.com/auth/drive.appdata"))
       .requestEmail()
       .build()
   ```
4. **Result Processing**: Activity result launcher receives `result.data` and parses `GoogleSignIn.getSignedInAccountFromIntent(intent)`.
5. **Token Acquisition**:
   In `GoogleDriveBackupHelper.fetchOAuthToken()`:
   ```kotlin
   val accountName = account.account
   val scopeStr = "oauth2:https://www.googleapis.com/auth/drive.appdata"
   GoogleAuthUtil.getToken(context, accountName, scopeStr)
   ```
6. **API Execution**: Bearer token string is attached to HTTP request headers (`Authorization: Bearer <accessToken>`) for OkHttp calls to Google Drive v3 REST endpoints (`https://www.googleapis.com/drive/v3/files`).

### API Modernization Assessment
- **Current APIs Used**: `GoogleSignInOptions`, `GoogleSignIn.getClient`, `GoogleSignInAccount`, `GoogleAuthUtil.getToken`.
- **Status**: **DEPRECATED / LEGACY**. Google is actively deprecating standard `GoogleSignIn` and `GoogleAuthUtil` APIs in favor of Google Identity Services (`CredentialManager` and `AuthorizationClient`).
- **Target Modern Architecture**: `com.google.android.gms:play-services-auth` `AuthorizationClient` / `Identity.getAuthorizationClient(context).authorize(...)`.

---

# 2. REQUESTED SCOPES

The application requests the following exact scopes:

1. `https://www.googleapis.com/auth/drive.appdata`: Restricted scope granting access exclusively to the application's hidden `appDataFolder`.
2. `https://www.googleapis.com/auth/userinfo.email` (via `.requestEmail()`): Basic profile scope to retrieve the Google Account email address for UI display.

**Audit Result**: **PASSED**. No broad or unnecessary Drive permissions (`drive`, `drive.readonly`, or `drive.file`) are requested.

---

# 3. TOKEN FLOW

1. **Acquisition**: Fetched on-demand on `Dispatchers.IO` via `GoogleAuthUtil.getToken(context, accountName, "oauth2:https://www.googleapis.com/auth/drive.appdata")`.
2. **Storage & Caching**:
   - Access tokens are **NOT** persisted to disk or `SharedPreferences`. Tokens are requested dynamically.
   - `GoogleAuthUtil` handles token caching in Google Play Services.
3. **Security & Leaks**:
   - Access tokens are never logged to `Logcat`.
   - Transmitted exclusively over TLS (`https://`).
4. **Error & Revocation Handling in Worker**:
   - In `ChatBackupWorker.kt`: If `fetchOAuthToken` returns `null` (e.g. revoked token, network failure, or account removed), `ChatBackupWorker` logs a warning and returns `Result.retry()`.

---

# 4. GOOGLE ACCOUNT FLOW

1. **Account Identification**: Stored as account email in `SharedPreferences` (`drive_account_name`).
2. **Account Switching**: Tapping "Change Google Account" triggers `signInIntent` again, updating the saved email on completion.
3. **Multi-Account Isolation Check**:
   - User A and User B on the same device using different Google Accounts remain isolated because Google Drive `appDataFolder` is partitioned per Google Account.
   - Even if User B connects User A's Google Account, User B cannot decrypt User A's backup without User A's Vault Password due to AES-256-GCM AEAD encryption protection.
4. **Revocation Test**: Revoking permission via `myaccount.google.com/permissions` causes `GoogleAuthUtil.getToken` / API calls to fail. `ChatBackupWorker` retries until interactive re-authentication is performed in UI.

---

# 5. DRIVE API FLOW

### Endpoint & Request Trace
- **File Name**: `funkytalk_chat_backup.enc`
- **Location**: `parents = ["appDataFolder"]`
- **Search Call**: `GET https://www.googleapis.com/drive/v3/files?spaces=appDataFolder&q=name%3D%27funkytalk_chat_backup.enc%27+and+trashed%3Dfalse&fields=files(id,name,size,modifiedTime)`
- **Create Call**: `POST https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart`
  - Part 1 (`application/json; charset=UTF-8`): `{"name":"funkytalk_chat_backup.enc","parents":["appDataFolder"]}`
  - Part 2 (`application/octet-stream`): Encrypted binary payload bytes.
- **Update Call**: `PATCH https://www.googleapis.com/upload/drive/v3/files/{fileId}?uploadType=media`
- **Download Call**: `GET https://www.googleapis.com/drive/v3/files/{fileId}?alt=media`
- **Delete Call**: `DELETE https://www.googleapis.com/drive/v3/files/{fileId}`

---

# 6. BACKUP IDENTITY

A FunkyTalk chat backup is identified by a 3-tier binding matrix:
1. **Supabase Binding**: Logged-in Supabase `user_id` stored in `chat_backup_meta`.
2. **Google Drive Binding**: Drive `fileId` inside the connected Google Account's hidden `appDataFolder`.
3. **Vault Encryption Binding**: AES-256-GCM key derived from the user's Vault Password via `PBKDF2-HMAC-SHA256`.

---

# 7. RELIABILITY FINDINGS

1. **Drive Upload vs. Supabase Metadata Handshake**:
   - Supabase RPC `record_chat_backup` is called **ONLY** after `GoogleDriveBackupHelper.uploadBackupFile` returns success.
   - If Drive upload fails, Supabase metadata is not updated.
2. **Memory Limit on Download**:
   - `downloadBackupFile` reads `response.body?.bytes()`, loading the full backup into RAM `ByteArray`, linking to the Part 2 OOM finding for large backups.

---

# 8. SECURITY FINDINGS

### [MEDIUM] FINDING 1: Use of Deprecated Google Sign-In & Auth APIs
- **File:** `app/src/main/java/com/example/util/GoogleDriveBackupHelper.kt`
- **Class/function:** `GoogleDriveBackupHelper.getGoogleSignInOptions()` / `fetchOAuthToken()`
- **Evidence:** Lines 50–55 & 76 in `GoogleDriveBackupHelper.kt` use `GoogleSignInOptions` and `GoogleAuthUtil.getToken()`.
- **Problem:** `GoogleSignIn` and `GoogleAuthUtil` are legacy Google Sign-In APIs being phased out.
- **Impact:** Potential deprecation rejections on Google Play Services updates.
- **Recommended Fix:** Migrate to modern Google Identity `AuthorizationClient` / `Identity.getAuthorizationClient(context)`.

---

### [MEDIUM] FINDING 2: Duplicate Backup Handling in `appDataFolder`
- **File:** `app/src/main/java/com/example/util/GoogleDriveBackupHelper.kt`
- **Class/function:** `GoogleDriveBackupHelper.findBackupFileId()`
- **Evidence:** Line 103 uses `fileList?.files?.firstOrNull()`.
- **Problem:** If multiple backup files exist in `appDataFolder`, only the first is updated, leaving orphaned duplicates.
- **Impact:** Unnecessary Drive storage consumption.
- **Recommended Fix:** Target the most recently modified file and delete orphaned duplicate file IDs.

---

### [LOW] FINDING 3: Unhandled Drive File Deletion Error Handshake
- **File:** `app/src/main/java/com/example/ui/screens/profile/ChatBackupScreen.kt`
- **Class/function:** `executeDeleteBackup()`
- **Evidence:** Lines 180–193 execute Supabase `deleteChatBackupMeta` even if Drive file deletion fails.
- **Problem:** Supabase metadata is deleted while the encrypted backup file remains on Google Drive.
- **Impact:** Orphaned encrypted file on Google Drive.
- **Recommended Fix:** Call Supabase metadata deletion only after Drive file deletion succeeds or returns 404.

---

# 9. RELEASE BLOCKERS

1. **Migrate Legacy Google Authorization APIs**: Replace `GoogleSignIn` / `GoogleAuthUtil` with modern Google Identity `AuthorizationClient`.
2. **Drive File Deletion Handshake**: Ensure Supabase metadata deletion is conditioned on confirmed Google Drive file deletion.

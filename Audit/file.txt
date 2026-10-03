# FUNKYTALK CHAT BACKUP
# FINAL PRODUCTION AUDIT

---

## Executive Summary

This is the **Final Production Audit** of the end-to-end encrypted Chat Backup & Restore system for the FunkyTalk Android application (`com.funkytalk.app`).

The system enables users to create password-authenticated, end-to-end encrypted backups of their local SQLCipher Room chat database, Tink HPKE identity keypair, and optional chat media directly to Google Drive's hidden `appDataFolder` (`funkytalk_chat_backup.enc`), with backup status metadata tracked on Supabase.

### Overall Assessment
While the core cryptographic foundation (PBKDF2-HMAC-SHA256 with 600,000 iterations + AES-256-GCM AEAD encryption) and Drive `appDataFolder` isolation are mathematically sound, the current implementation contains **Critical Release Blockers** in **Account Isolation**, **Secret Preference Storage**, and **Restore Atomicity** that must be resolved prior to production release.

---

## Architecture Verification

```
[ UI Layer ]
 ChatBackupScreen.kt / RestoreChatBackupScreen.kt / ProfileTabContent.kt
      │
      ▼
[ Activity & Auth Control ]
 MainActivity.kt / AuthViewModel.kt / SupabaseSessionManager.kt
      │
      ├───────────────────────────────────┬───────────────────────────────────┐
      ▼                                   ▼                                   ▼
[ Local Crypto Engine ]          [ Background Auto-Backup ]          [ Cloud Sync & Storage ]
 ├── ChatBackupCryptoManager      └── ChatBackupWorker.kt             ├── GoogleDriveBackupHelper.kt
 ├── ChatBackupArchiveManager         (WorkManager)                   │    (Drive v3 appDataFolder)
 └── VaultSecurityManager                                             └── SupabaseApi.kt
      │                                                                    (RPC Metadata)
      ▼
[ Local Storage ]
 ├── SQLCipher Room DB: encrypted_dm_chat_vault.db
 ├── Tink Keyset: e2ee_tink_prefs
 └── Preferences: chat_backup_prefs.xml
```

---

## Backup Flow

1. **Trigger**: User taps "Back up now" in `ChatBackupScreen.kt` or `ChatBackupWorker.kt` runs periodic background work.
2. **Archive Creation (`ChatBackupArchiveManager.kt`)**:
   - Checkpoints Room WAL database: `db.openHelper.writableDatabase.execSQL("PRAGMA wal_checkpoint(FULL)")`.
   - Snapshots `encrypted_dm_chat_vault.db` file.
   - Exports Tink HPKE keyset entries from `e2ee_tink_prefs` as `tink_keyset.json`.
   - Optionally gathers files from `context.filesDir/chat_media/`.
   - Compresses entries into an in-memory ZIP `ByteArray`.
3. **Outer Encryption (`ChatBackupCryptoManager.kt`)**:
   - Generates random 16-byte salt and 12-byte nonce via `SecureRandom`.
   - Derives 256-bit AES key via `PBKDF2WithHmacSHA256` (600,000 iterations).
   - Encrypts ZIP payload using `AES/GCM/NoPadding` (128-bit authentication tag).
   - prepends 38-byte binary header (`FTBK` magic + format version 1 + KDF type + iterations + salt + nonce).
4. **Google Drive Upload (`GoogleDriveBackupHelper.kt`)**:
   - Queries `spaces = appDataFolder` for `funkytalk_chat_backup.enc`.
   - Overwrites existing file via `PATCH /upload/drive/v3/files/{id}?uploadType=media` or creates new file via `POST /upload/drive/v3/files?uploadType=multipart`.
5. **Metadata Sync (`SupabaseApi.kt`)**:
   - On upload success, calls Supabase RPC `record_chat_backup(p_size_bytes, p_format_version, p_includes_media, p_key_version)`.
   - Updates local `ChatBackupPreferencesManager` timestamp and size.

---

## Restore Flow

1. **Detection (`MainActivity.kt`)**:
   - User logs into FunkyTalk on a fresh device.
   - If local vault is unconfigured (`isVaultConfigured == false`), invokes Supabase RPC `get_chat_backup_status()`.
   - If `has_backup == true`, routes user to `RestoreChatBackupScreen.kt`.
2. **Authorization & Download (`RestoreChatBackupScreen.kt`)**:
   - User connects Google Drive (`GoogleDriveBackupHelper`).
   - Locates backup file in Drive `appDataFolder` and downloads encrypted payload via `GET /drive/v3/files/{id}?alt=media`.
3. **Decryption & Authentication (`ChatBackupCryptoManager.kt`)**:
   - Validates header magic bytes (`FTBK`), version 1, and KDF params.
   - User enters Vault Password.
   - Derives AES-256 key using header salt and iteration count.
   - Executes `Cipher.doFinal(ciphertext)`. AEAD tag validation ensures incorrect passwords fail safely without modifying local disk files.
4. **Restoration (`ChatBackupArchiveManager.kt`)**:
   - Parses ZIP entries from decrypted stream.
   - Writes `tink_keyset.json` into `e2ee_tink_prefs`.
   - Closes open database connections (`EncryptedChatDatabase.closeInstance()`).
   - Overwrites `encrypted_dm_chat_vault.db` and deletes leftover `-wal` and `-shm` sidecar files.
   - Restores media files to `filesDir/chat_media/`.
   - Unlocks vault in `VaultSecurityManager` using the provided password.
   - Navigates user to `"home"`.

---

## Crypto & Archive Security

### Scenarios & Test Results

| Scenario | Code Behavior | Result | Evidence |
| :--- | :--- | :--- | :--- |
| **TEST 1: Fresh Install Restore** | Complete flow downloads, decrypts, and restores DB + keys. | **PASS** | `RestoreChatBackupScreen.kt` lines 91–157. |
| **TEST 2: Wrong Password** | `AEADBadTagException` caught; rate-limited delay; DB untouched. | **PASS** | `ChatBackupCryptoManager.kt` lines 110–112; `RestoreChatBackupScreen.kt` lines 132–140. |
| **TEST 3: Corrupted Ciphertext** | `AEADBadTagException` / `GeneralSecurityException` caught; aborts safely. | **PASS** | `ChatBackupCryptoManager.kt` line 113. |
| **TEST 4: Truncated Archive** | Header check `< 38` bytes throws `IllegalArgumentException`; aborts. | **PASS** | `ChatBackupCryptoManager.kt` lines 67–69. |
| **TEST 5: Zip Slip Path Traversal** | Unpacks `media/*` using `removePrefix("media/")` without checking `canonicalPath`. | **FAIL (CRITICAL)** | `ChatBackupArchiveManager.kt` lines 154–161. |
| **TEST 6: Missing DB Snapshot** | If `db_snapshot.db` missing, DB is not overwritten. | **PARTIAL** | `ChatBackupArchiveManager.kt` line 135. |
| **TEST 7: Missing Tink Keyset** | DB restored, but Tink keys missing; existing E2EE messages unreadable. | **PARTIAL** | `ChatBackupArchiveManager.kt` line 115. |
| **TEST 8: Missing Media Files** | Media is optional; restore completes successfully. | **PASS** | `ChatBackupArchiveManager.kt` line 154. |
| **TEST 9: Drive Returns 404** | OkHttp 404 caught; displays error message; DB untouched. | **PASS** | `GoogleDriveBackupHelper.kt` line 206; `RestoreChatBackupScreen.kt` line 121. |
| **TEST 10: OAuth Token Expired** | HTTP 401 caught; displays reconnect prompt in UI. | **PASS** | `GoogleDriveBackupHelper.kt` line 139; `RestoreChatBackupScreen.kt` line 106. |
| **TEST 11: User Cancels Restore** | Dialog warns user; skipping initializes clean empty vault. | **PASS** | `RestoreChatBackupScreen.kt` lines 270–292. |
| **TEST 12: Process Dies Mid-Restore** | Direct overwrite of `encrypted_dm_chat_vault.db` leaves truncated file on crash. | **FAIL (HIGH)** | `ChatBackupArchiveManager.kt` lines 139–143. |

---

## Google Drive & OAuth

- **Scope Requested**: `https://www.googleapis.com/auth/drive.appdata` + `email`. Minimal scope verified. Personal Drive files are not accessible.
- **Legacy API Deprecation**: `GoogleDriveBackupHelper.kt` uses legacy `GoogleSignInOptions`, `GoogleSignIn.getClient`, and `GoogleAuthUtil.getToken()`. Should be migrated to `AuthorizationClient` / `Identity.getAuthorizationClient()`.
- **Orphaned Duplicate Files**: `findBackupFileId()` selects `files.firstOrNull()`. If multiple files exist in `appDataFolder`, only the first is updated while duplicates consume Drive quota.

---

## Supabase / RPC / RLS

- **User ID Spoofing Check**: `RecordChatBackupRequest` does NOT accept `p_user_id`. Server function derives caller identity directly from `auth.uid()`. **PASSED**.
- **Remote DDL Verification**: RPC function SQL and RLS policies on `chat_backup_meta` are **NOT VERIFIED FROM AVAILABLE PROJECT SOURCES** (managed on remote Supabase instance).

---

## WorkManager

- **Configuration**: `ChatBackupWorker` (`CoroutineWorker`) enqueued with unique name `"funkytalk_chat_auto_backup"`. Enforces `NetworkType.UNMETERED` (or `CONNECTED`) and `setRequiresBatteryNotLow(true)`.
- **Concurrency Collision**: No `Mutex` or execution lock exists between manual backup (`ChatBackupScreen.kt`) and `ChatBackupWorker.kt`. Concurrent triggers cause race conditions when uploading `funkytalk_chat_backup.enc`.

---

## Account Isolation

### [CRITICAL RELEASE BLOCKER]
- **Scenario**: User A logs out of FunkyTalk. User B logs into FunkyTalk on the same device.
- **Audit Findings**:
  1. `AuthViewModel.logout()` clears session memory, but does **NOT** invoke `ChatBackupPreferencesManager.clearAll(context)` or `WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)`.
  2. User A's cached vault password (`KEY_CACHED_VAULT_PASSWORD`) and Google account email (`KEY_DRIVE_ACCOUNT_NAME`) persist in `chat_backup_prefs.xml`.
  3. User B logging in inherits User A's connected Google account, vault password, and active background WorkManager worker job.
  4. Tapping "Back up now" under User B's profile encrypts data with User A's password and uploads it to User A's Google Drive account!

---

## Password Change

- Updating or resetting the vault password in `VaultUnlockDialog.kt` or `OnboardingChatVaultScreen.kt` updates `KEY_CACHED_VAULT_PASSWORD` locally, but does **NOT** immediately trigger a background re-upload to Google Drive.
- **Consistency Window**: Remote Google Drive backup remains encrypted under the old vault password until the next manual or scheduled auto-backup run.

---

## Delete Backup

- In `ChatBackupScreen.kt`, `executeDeleteBackup()` calls `GoogleDriveBackupHelper.deleteBackupFile()` followed by Supabase `deleteChatBackupMeta`.
- **Flaw**: If `deleteBackupFile` fails due to a network timeout, code still proceeds to execute `deleteChatBackupMeta` on Supabase, deleting metadata while leaving an orphaned file on Google Drive.

---

## Memory & Performance

- `ChatBackupArchiveManager.createArchive()` creates the ZIP in `ByteArrayOutputStream`. `ChatBackupCryptoManager.encryptBackup()` allocates full `ByteArray` ciphertext. `GoogleDriveBackupHelper.downloadBackupFile()` reads full `response.body?.bytes()`.
- **Risk**: For backups containing photos/media exceeding 50MB–100MB, holding 3 copies of the binary array in RAM will cause ART JVM `OutOfMemoryError` crashes on low-RAM devices.

---

## UI / UX

- **State Representation**: `ChatBackupScreen.kt` provides clean feedback for Google Drive connection, last backup date/size, auto-backup frequency, and media settings.
- **Warning Banners**: Includes explicit warning: *"If you forget your vault password, backups cannot be recovered."*
- **Destructive Actions**: Delete Backup and Skip Restore actions are protected by confirmation dialogs.

---

## End-to-End Failure Matrix

| Failure Scenario | Current Behavior | Data Safety | User Experience | Required Fix | Severity |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **User Logout** | Preferences & WorkManager persist across users. | **COMPROMISED** | Cross-account leak | Clear prefs & cancel WorkManager on logout. | **CRITICAL** |
| **Malicious Zip Entry (`../`)** | Unpacks outside `chat_media`. | **COMPROMISED** | Arbitrary file write | Validate `canonicalPath.startsWith()`. | **CRITICAL** |
| **Crash During Restore Write** | Overwrites DB directly in-place. | **COMPROMISED** | Corrupted DB on crash | Stage restore in temp dir + atomic swap. | **HIGH** |
| **Large Media Backup (>100MB)** | Loads entire archive into RAM. | **RISK** | OOM App Crash | Implement file-based streaming streams. | **MEDIUM** |
| **Concurrent Manual + Auto Backup**| Both execute simultaneously. | **SAFE** | Race condition / wasted data | Implement thread-safe execution Mutex. | **MEDIUM** |
| **Drive Delete Network Failure** | Deletes Supabase meta anyway. | **SAFE** | Orphaned Drive file | Only delete Supabase meta if Drive delete succeeds/404. | **LOW** |
| **Vault Password Change** | Delayed remote re-encryption. | **SAFE** | Stale remote backup password | Schedule immediate re-upload task on password change. | **LOW** |

---

## Security Findings

### [CRITICAL] FINDING 1: Lack of Account Isolation on Logout
- **File:** `app/src/main/java/com/example/AuthViewModel.kt`
- **Class/function:** `AuthViewModel.logout()`
- **Evidence:** `logout()` does not clear `ChatBackupPreferencesManager` or cancel `ChatBackupWorker`.
- **Problem:** Cached vault password and Google Drive credentials survive logout, leaking across user accounts on shared devices.
- **Impact:** User B can backup into User A's Google Drive or execute auto-backups under User A's identity.

### [CRITICAL] FINDING 2: Zip Slip Path Traversal Vulnerability
- **File:** `app/src/main/java/com/example/crypto/ChatBackupArchiveManager.kt`
- **Class/function:** `ChatBackupArchiveManager.restoreFromArchive()`
- **Evidence:** `fileName = name.removePrefix("media/")` without checking `targetFile.canonicalPath.startsWith(mediaDir.canonicalPath)`.
- **Problem:** Malicious ZIP entries can write files outside `chat_media`.
- **Impact:** Arbitrary local file overwrite during restore.

### [HIGH] FINDING 3: Non-Atomic Database Restoration
- **File:** `app/src/main/java/com/example/crypto/ChatBackupArchiveManager.kt`
- **Class/function:** `ChatBackupArchiveManager.restoreFromArchive()`
- **Evidence:** Overwrites `encrypted_dm_chat_vault.db` directly in-place during ZIP stream reading.
- **Problem:** A crash or power loss mid-write corrupts the local SQLite database file unrecoverably.
- **Impact:** Irrecoverable loss of local chat history on interrupted restore.

---

## Release Blockers

1. **Clear Preferences & Cancel WorkManager on Logout** (`AuthViewModel.kt`).
2. **Sanitize ZIP Paths Against Path Traversal / Zip Slip** (`ChatBackupArchiveManager.kt`).
3. **Implement Staging & Atomic File Replacement for Restore** (`ChatBackupArchiveManager.kt`).

---

## Required Fix Plan

### PHASE 1 — ACCOUNT ISOLATION & LOGOUT FIX
- Invoke `ChatBackupPreferencesManager.clearAll(context)` in `AuthViewModel.logout()`.
- Invoke `WorkManager.getInstance(context).cancelUniqueWork(ChatBackupWorker.WORK_NAME)` in `AuthViewModel.logout()`.

### PHASE 2 — RESTORE SECURITY & ATOMICITY
- In `ChatBackupArchiveManager.restoreFromArchive()`, add canonical path validation for `media/*` entries:
  `require(targetFile.canonicalPath.startsWith(mediaDir.canonicalPath))`
- Extract `db_snapshot.db` to a temporary file (`encrypted_dm_chat_vault.db.tmp`). Validate database header/integrity before atomically replacing the live `.db` file via `File.renameTo()`.

### PHASE 3 — CONCURRENCY & MEMORY OPTIMIZATION
- Introduce a singleton `Mutex` guarding backup execution across UI and `ChatBackupWorker`.
- Transition ZIP compression and AES-256-GCM cipher streams to temp files (`CipherOutputStream` / `CipherInputStream`).

---

## Final Release Verdict

# **NOT READY**

**Release Blockers to Resolve First**:
1. Fix account isolation leak on logout (`AuthViewModel.kt`).
2. Add Zip Slip path traversal checks (`ChatBackupArchiveManager.kt`).
3. Implement atomic temporary file staging for database restore (`ChatBackupArchiveManager.kt`).

---

## Verified vs Not Verified

- **VERIFIED FROM CODE**:
  - Entire client-side cryptography, PBKDF2 params (600,000 iterations), AES-256-GCM header structure (`FTBK`).
  - Google Drive REST v3 calls targeting `spaces = appDataFolder` with scope `https://www.googleapis.com/auth/drive.appdata`.
  - Account isolation flaw on logout in `AuthViewModel.kt`.
  - Zip Slip path traversal vulnerability in `ChatBackupArchiveManager.kt`.
  - Direct in-place file overwrite vulnerability in `restoreFromArchive()`.
- **NOT VERIFIED FROM AVAILABLE PROJECT SOURCES**:
  - Remote Supabase database DDL for table `chat_backup_meta` and RPC SQL functions (`record_chat_backup`, `get_chat_backup_status`, `delete_chat_backup_meta`).
  - Remote Supabase Row Level Security (RLS) policies.
  - Production Google Cloud Console OAuth 2.0 Client ID SHA-1 fingerprint registration.

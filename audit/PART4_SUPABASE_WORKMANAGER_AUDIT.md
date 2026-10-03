# FUNKYTALK CHAT BACKUP — PART 4
# SUPABASE + RLS + RPC + WORKMANAGER + RELIABILITY AUDIT

---

# 1. SUPABASE ARCHITECTURE

### Remote RPC Endpoints (Client API Mapping)
The application defines three RPC methods in `SupabaseApi.kt`:

1. **`record_chat_backup`**:
   - HTTP Endpoint: `POST rest/v1/rpc/record_chat_backup`
   - Headers: `apikey: <SUPABASE_ANON_KEY>`, `Authorization: Bearer <user_jwt_token>`
   - Client Model: `RecordChatBackupRequest(p_size_bytes, p_format_version, p_includes_media, p_key_version)`
   - Server Function: Records or upserts the user's latest backup metadata row in the database.

2. **`get_chat_backup_status`**:
   - HTTP Endpoint: `POST rest/v1/rpc/get_chat_backup_status`
   - Headers: `apikey: <SUPABASE_ANON_KEY>`, `Authorization: Bearer <user_jwt_token>`
   - Client Response Model: `ChatBackupStatusResponse(has_backup, last_backup_at, size_bytes, format_version, includes_media, backup_key_version, current_key_version)`
   - Server Function: Queries the backup metadata table for the caller's identity (`auth.uid()`) and returns status.

3. **`delete_chat_backup_meta`**:
   - HTTP Endpoint: `POST rest/v1/rpc/delete_chat_backup_meta`
   - Headers: `apikey: <SUPABASE_ANON_KEY>`, `Authorization: Bearer <user_jwt_token>`
   - Server Function: Removes the caller's row from the backup metadata table.

---

# 2. CHAT_BACKUP_META SCHEMA

From client Retrofit data models (`RecordChatBackupRequest` & `ChatBackupStatusResponse`), the table schema columns are:

| Column | Type | Description |
| :--- | :--- | :--- |
| `user_id` | UUID (PK) | References `auth.users(id)`. Binds metadata to user identity. |
| `size_bytes` | BIGINT | Size of encrypted backup in bytes. |
| `format_version` | INT | Backup archive binary format version (default 1). |
| `includes_media` | BOOLEAN | Indicates whether chat media files are included. |
| `backup_key_version` | INT | Encryption key version of the backup. |
| `last_backup_at` / `updated_at` | TIMESTAMPTZ | Timestamp of last successful upload. |

**Drive File & Google Account Privacy**:
Google Drive File IDs and Google Account email addresses are **NOT** transmitted to or stored on Supabase. They remain exclusively local on the user's device and inside Google Drive `appDataFolder`.

**Remote DDL Verification**:
- SQL table definition and RPC migration DDL: **NOT VERIFIED FROM AVAILABLE PROJECT SOURCES** (managed on remote Supabase instance).

---

# 3. RPC & RLS SECURITY

### User ID Spoofing Test
- `RecordChatBackupRequest` contains ONLY `p_size_bytes`, `p_format_version`, `p_includes_media`, and `p_key_version`.
- It does **NOT** accept or pass a `p_user_id` parameter from the client.
- **Audit Result**: **PASSED**. A malicious client cannot supply or override another user's ID in the RPC payload. The server function must derive user identity directly from the authenticated JWT token (`auth.uid()`).

---

# 4. SUPABASE ↔ DRIVE CONSISTENCY

| Scenario | Drive State | Supabase State | System Outcome |
| :--- | :--- | :--- | :--- |
| **Scenario A: Drive Upload Fails** | Failed / Old File | Unchanged | Consistent. Supabase metadata reflects previous valid state. |
| **Scenario B: Drive Upload Succeeds, Supabase RPC Fails** | New File Uploaded | Stale Metadata | Discrepancy. File exists on Drive, but metadata timestamp lags behind. On restore, latest Drive file is still fetched. |
| **Scenario C: Drive Succeeds → Supabase Succeeds → App Crash** | New File Uploaded | Updated Metadata | Consistent. System state preserved across crash/restart. |
| **Scenario D: Drive File Deleted Externally** | File Removed | Metadata Exists | Discrepancy. Restore screen offers restore, but download returns 404 error. |
| **Scenario E: Supabase Metadata Deleted** | File Exists | Metadata Removed | Discrepancy. On fresh install, `get_chat_backup_status` returns `has_backup=false`, bypassing restore prompt. File remains as orphaned archive in Drive. |

---

# 5. WORKMANAGER AUDIT

### Worker Configuration (`ChatBackupWorker.kt`)
- **Worker Class**: `ChatBackupWorker : CoroutineWorker(appContext, workerParams)`
- **Unique Work Key**: `WORK_NAME = "funkytalk_chat_auto_backup"`
- **Enqueue Policy**: `ExistingPeriodicWorkPolicy.UPDATE`
- **Frequencies**: `"DAILY"` (1 day) / `"WEEKLY"` (7 days) / `"OFF"` (cancels work).
- **Constraints**:
  - Network: `NetworkType.UNMETERED` (if Wi-Fi only enabled) or `NetworkType.CONNECTED`.
  - Battery: `setRequiresBatteryNotLow(true)`.
- **System Persistence**: WorkManager automatically persists scheduled unique work across application restarts and device reboots.

---

# 6. DUPLICATE JOBS & CONCURRENCY

### Concurrency Audit
- **Scenario**: User taps "Back up now" in `ChatBackupScreen.kt` while `ChatBackupWorker` auto-backup is running in the background.
- **Audit Result**:
  - `ChatBackupScreen.kt` launches a manual backup in `coroutineScope.launch`.
  - `ChatBackupWorker.kt` runs independently in WorkManager's background thread pool.
  - No `Mutex` or synchronization lock exists between manual and automated backup execution.
  - Both jobs attempt to checkpoint Room DB, compress ZIP archive, and issue OkHttp `PATCH` / `POST` calls to Google Drive `appDataFolder`.
  - **Risk**: Concurrent file writes to Google Drive `funkytalk_chat_backup.enc` cause race conditions and waste network bandwidth.

---

# 7. ACCOUNT SWITCHING & LOGOUT AUDIT

### Account Isolation Vulnerability
- **Scenario**: User A logs out of FunkyTalk. User B logs into FunkyTalk on the same device.
- **Audit Findings**:
  1. In `AuthViewModel.kt`, logging out clears user session memory, but `ChatBackupPreferencesManager` preferences (`chat_backup_prefs.xml`) are **NOT** cleared upon logout.
  2. `KEY_CACHED_VAULT_PASSWORD` (User A's vault password) and `KEY_DRIVE_ACCOUNT_NAME` (User A's Google email) remain stored on disk.
  3. `ChatBackupWorker` background periodic job is **NOT** cancelled upon logout.
  4. When User B logs into FunkyTalk, `ChatBackupPreferencesManager` still contains User A's cached vault password and Google account email.
  5. If User B opens Settings > Chat Backup, User A's Google account appears connected. Tapping "Back up now" creates a backup using User A's credentials and uploads it to User A's Google Drive account!

---

# 8. PASSWORD CHANGE AUDIT

### Immediate Re-encryption Audit
- When a user changes or resets their vault password in `OnboardingChatVaultScreen.kt` or `VaultUnlockDialog.kt`:
  - `ChatBackupPreferencesManager.setCachedVaultPassword(context, newPassword)` updates the local cached password.
  - **Audit Finding**: The app does **NOT** immediately re-encrypt or re-upload the existing Google Drive backup file.
  - **Result**: The existing backup file in Google Drive remains encrypted under the **OLD** vault password until the next scheduled auto-backup worker run or manual backup trigger.

---

# 9. WORKER FAILURE & RETRY MATRIX

| Error Condition | Worker Action | Audit Assessment |
| :--- | :--- | :--- |
| **No Google Account Linked** | `Result.failure()` | **Correct**. Halts execution if no account connected. |
| **Failed OAuth Token Fetch** | `Result.retry()` | **Acceptable for transient network errors**, but causes infinite retries if user revoked Google account access. |
| **No Cached Vault Password** | `Result.failure()` | **Correct**. Halts execution if vault password missing. |
| **Drive Upload HTTP Failure** | `Result.retry()` | **Correct**. Schedules exponential backoff retry. |
| **Supabase RPC Failure** | `Result.success()` (with warning log) | **Acceptable**. Drive file was successfully updated; metadata failure is non-fatal for backup preservation. |

---

# 10. SECURITY FINDINGS

### [HIGH] FINDING 1: Uncleared Backup Preferences & Active Worker on Logout
- **File:** `app/src/main/java/com/example/AuthViewModel.kt` & `ChatBackupPreferencesManager.kt`
- **Class/function:** `AuthViewModel.logout()`
- **Evidence:** Logging out does not call `ChatBackupPreferencesManager.clearAll(context)` or `WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)`.
- **Problem:** User A's cached vault password, Google account email, and background WorkManager auto-backup job persist across logout.
- **Impact:** User B logging into the same device inherits User A's Drive account connection, vault password, and background auto-backup job, leading to cross-account data leakage.
- **Recommended Fix:** Invoke `ChatBackupPreferencesManager.clearAll(context)` and `WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)` inside the logout flow.

---

### [MEDIUM] FINDING 2: Missing Concurrency Lock Between Manual and Auto Backups
- **File:** `app/src/main/java/com/example/ui/screens/profile/ChatBackupScreen.kt` & `ChatBackupWorker.kt`
- **Class/function:** `executeBackupNow()` / `ChatBackupWorker.doWork()`
- **Evidence:** Manual backup and WorkManager worker both execute full archive generation and Drive upload without checking an active execution mutex.
- **Problem:** Simultaneous manual and auto-backup triggers create race conditions when patching `funkytalk_chat_backup.enc`.
- **Impact:** Redundant network data usage and potential upload conflict errors.
- **Recommended Fix:** Implement a thread-safe singleton `Mutex` or atomic boolean state guarding backup execution.

---

### [LOW] FINDING 3: Delayed Backup Re-encryption on Password Change
- **File:** `app/src/main/java/com/example/ui/screens/chat/VaultUnlockDialog.kt`
- **Class/function:** `VaultSetupDialog` / `VaultUnlockDialog`
- **Evidence:** Updating the vault password caches the new password locally, but does not launch an immediate re-encryption task for the remote Drive backup.
- **Problem:** Google Drive backup file remains encrypted with the old password until the next backup cycle.
- **Impact:** Temporary password mismatch between local vault and remote Drive backup.
- **Recommended Fix:** Enqueue an immediate one-time WorkManager backup task whenever the vault password is changed.

---

# 11. RELEASE BLOCKERS

1. **Clear Preferences & Cancel WorkManager on Logout**: Must clear `ChatBackupPreferencesManager` and cancel `ChatBackupWorker` in `AuthViewModel.logout()` to prevent cross-account backup contamination.
2. **Backup Execution Synchronization**: Implement a concurrency guard to prevent simultaneous manual and automated background uploads.

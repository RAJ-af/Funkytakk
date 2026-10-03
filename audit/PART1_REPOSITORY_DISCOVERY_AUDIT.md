# FUNKYTALK CHAT BACKUP — PART 1
# REPOSITORY DISCOVERY & ARCHITECTURE AUDIT

---

## 1. PROJECT STRUCTURE

The following tree outlines all relevant files and directories within the repository (`app/src/main/java/com/example/` and root Gradle files):

```
app/
├── build.gradle.kts
├── src/
│   └── main/
│       ├── AndroidManifest.xml
│       └── java/
│           └── com/
│               └── example/
│                   ├── AuthViewModel.kt
│                   ├── FunkyTalkApplication.kt
│                   ├── GoogleServicesConfig.kt
│                   ├── MainActivity.kt
│                   ├── SupabaseApi.kt
│                   ├── SupabaseClient.kt
│                   ├── SupabaseSessionManager.kt
│                   ├── crypto/
│                   │   ├── ChatBackupArchiveManager.kt
│                   │   ├── ChatBackupCryptoManager.kt
│                   │   ├── E2EEManager.kt
│                   │   ├── E2EEMediaManager.kt
│                   │   └── VaultSecurityManager.kt
│                   ├── db/
│                   │   ├── DmConversationDao.kt
│                   │   ├── DmMessageDao.kt
│                   │   ├── EncryptedChatDatabase.kt
│                   │   ├── GroupConversationDao.kt
│                   │   ├── GroupMessageDao.kt
│                   │   ├── LocalDmConversation.kt
│                   │   ├── LocalDmMessage.kt
│                   │   ├── LocalGroupConversation.kt
│                   │   └── LocalGroupMessage.kt
│                   ├── repository/
│                   │   ├── EncryptedChatRepository.kt
│                   │   └── GroupChatRepository.kt
│                   ├── service/
│                   │   └── ChatBackupWorker.kt
│                   ├── ui/
│                   │   ├── components/
│                   │   │   ├── CustomSoftIcons.kt
│                   │   │   ├── GroupCustomIcons.kt
│                   │   │   └── Soft3DIcons.kt
│                   │   └── screens/
│                   │       ├── AuthScreen.kt
│                   │       ├── HomeScreen.kt
│                   │       ├── OnboardingChatVaultScreen.kt
│                   │       ├── PrivacyScreen.kt
│                   │       ├── WelcomeScreen.kt
│                   │       ├── chat/
│                   │       │   ├── DirectChatScreen.kt
│                   │       │   ├── GroupChatScreen.kt
│                   │       │   ├── RestoreChatBackupScreen.kt
│                   │       │   └── VaultUnlockDialog.kt
│                   │       ├── home/
│                   │       │   └── tabs/
│                   │       │       ├── ChatsTabContent.kt
│                   │       │       └── ProfileTabContent.kt
│                   │       └── profile/
│                   │           └── ChatBackupScreen.kt
│                   └── util/
│                       ├── ChatBackupPreferencesManager.kt
│                       ├── GoogleDriveBackupHelper.kt
│                       └── SystemNotificationHelper.kt
gradle/
└── libs.versions.toml
```

---

## 2. ARCHITECTURE MAP

### General Data & UI Architecture
```
[ Jetpack Compose Screens ]
 (ChatBackupScreen, RestoreChatBackupScreen, ProfileTabContent, DirectChatScreen, AuthScreen)
      │
      ▼
[ ViewModels & Activity Controller ]
 (AuthViewModel, MainActivity, Local State)
      │
      ▼
[ Repositories & Helpers ]
 (EncryptedChatRepository, GroupChatRepository, GoogleDriveBackupHelper)
      │
      ├───────────────────────────────┬───────────────────────────────┐
      ▼                               ▼                               ▼
[ Local SQLCipher Room DB ]  [ Local Tink Key Store ]      [ Remote Supabase API ]
 (EncryptedChatDatabase)     (e2ee_tink_prefs / Keystore)   (SupabaseApi / Retrofit)
      │
      ▼
[ SQLite File + Media Files ]
 (encrypted_dm_chat_vault.db, chat_media/)
```

### Secure Chat Vault & Local Storage Chain
```
[ User Vault Password ]
      │
      ▼
[ VaultSecurityManager ]
 ├── KEK Derivation: PBKDF2WithHmacSHA256 (310,000 iterations + 16-byte salt)
 └── Wraps 256-bit Vault Master Key -> saved in vault_envelope_prefs
      │
      ▼
[ EncryptedChatDatabase (SQLCipher Room) ]
 ├── Database: encrypted_dm_chat_vault.db
 └── Net.sqlcipher.database.SupportFactory(vaultKey)
      │
      ▼
[ Local DMs & Group Messages ]
 (LocalDmMessage, LocalGroupMessage)
```

### Encrypted Chat Backup Architecture
```
[ Trigger: Manual / WorkManager ]
 (ChatBackupScreen / ChatBackupWorker)
      │
      ▼
[ Step 1: Checkpoint & Archive ]
 (ChatBackupArchiveManager.createArchive)
  ├── Room DB WAL Checkpoint: PRAGMA wal_checkpoint(FULL)
  ├── DB File Snapshot: encrypted_dm_chat_vault.db
  ├── Tink Keyset Prefs: e2ee_tink_prefs (tink_keyset.json)
  └── Optional Media: chat_media/
  └── Output: Compressed ZIP ByteArray
      │
      ▼
[ Step 2: Streaming Outer Encryption ]
 (ChatBackupCryptoManager.encryptBackup)
  ├── PBKDF2-HMAC-SHA256 Key Derivation (600,000 iterations + random 16-byte salt)
  ├── AES-256-GCM Encryption (random 12-byte nonce)
  └── Binary Header: Magic 'FTBK' (4B) + Version 1 (1B) + KDF Type (1B) + Iterations (4B) + Salt (16B) + Nonce (12B)
      │
      ▼
[ Step 3: Google Drive Upload ]
 (GoogleDriveBackupHelper.uploadBackupFile)
  ├── Scope: https://www.googleapis.com/auth/drive.appdata
  ├── Drive v3 REST API: POST/PATCH /upload/drive/v3/files
  └── Destination: Google Drive appDataFolder (funkytalk_chat_backup.enc)
      │
      ▼
[ Step 4: Metadata Registration ]
 (SupabaseApi.recordChatBackup)
  └── POST rest/v1/rpc/record_chat_backup
```

### Encrypted Chat Restore Architecture
```
[ Fresh Login / App Startup ]
 (MainActivity -> LaunchedEffect)
      │
      ▼
[ Check Backup Metadata ]
 (SupabaseApi.getChatBackupStatus)
  └── POST rest/v1/rpc/get_chat_backup_status -> returns has_backup=true
      │
      ▼
[ UI: RestoreChatBackupScreen ]
  ├── User Connects Google Drive (GoogleDriveBackupHelper)
  └── User Enters Vault Password
      │
      ▼
[ Step 1: Download Archive ]
 (GoogleDriveBackupHelper.downloadBackupFile)
  └── Drive v3 REST API: GET /drive/v3/files/{fileId}?alt=media
      │
      ▼
[ Step 2: Header Validation & Decryption ]
 (ChatBackupCryptoManager.decryptBackup)
  ├── Validate Magic 'FTBK', Format Version 1, and KDF Params
  ├── Derive Key with PBKDF2-HMAC-SHA256 (600,000 iterations + Salt from header)
  └── AES-256-GCM Decrypt (AEAD authentication tag check; handles AEADBadTagException)
      │
      ▼
[ Step 3: Restore Database & Keys ]
 (ChatBackupArchiveManager.restoreFromArchive)
  ├── Restore e2ee_tink_prefs (tink_keyset.json)
  ├── Close active DB connections & overwrite encrypted_dm_chat_vault.db
  ├── Unlock Vault in VaultSecurityManager
  └── Unpack media files to chat_media/
```

---

## 3. IMPORTANT FILES

| Area | File Path | Class / Function / Symbol | Purpose |
| :--- | :--- | :--- | :--- |
| **App Entry & Navigation** | `app/src/main/java/com/example/MainActivity.kt` | `class MainActivity : FragmentActivity()` | Entry activity; manages screen state routing (`welcome`, `auth`, `restore_chat_backup`, `home`); checks backup status on login when vault is unconfigured. |
| **Authentication State** | `app/src/main/java/com/example/AuthViewModel.kt` | `class AuthViewModel : ViewModel()` | Manages authentication flow state (`AuthState`), user profile initialization, and Firebase/Supabase session binding. |
| **Supabase Session** | `app/src/main/java/com/example/SupabaseSessionManager.kt` | `object SupabaseSessionManager` | Encapsulates storage and access for Supabase JWT bearer tokens and user ID in local SharedPreferences. |
| **Supabase Retrofit Service** | `app/src/main/java/com/example/SupabaseApi.kt` | `interface SupabaseApi` | Defines API routes and RPC calls including `recordChatBackup`, `getChatBackupStatus`, and `deleteChatBackupMeta`. |
| **Supabase Client Engine** | `app/src/main/java/com/example/SupabaseClient.kt` | `object SupabaseClient` | Configures Retrofit and OkHttpClient singletons for communicating with Supabase backend. |
| **Vault Key & Envelope** | `app/src/main/java/com/example/crypto/VaultSecurityManager.kt` | `object VaultSecurityManager` | Generates 256-bit vault master key, wraps key with PBKDF2-derived KEK (310,000 iterations), manages biometric Keystore wrapping (`vault_biometric_master_key`), and controls `VaultStatus`. |
| **Tink HPKE Identity Keys** | `app/src/main/java/com/example/crypto/E2EEManager.kt` | `object E2EEManager` | Handles Tink HPKE keypair generation (`DHKEM_X25519_HKDF_SHA256_HKDF_SHA256_AES_256_GCM`), exports public keys to Supabase `user_public_keys`, and persists private keysets to `e2ee_tink_prefs`. |
| **Backup Outer Encryption** | `app/src/main/java/com/example/crypto/ChatBackupCryptoManager.kt` | `object ChatBackupCryptoManager` | Implements PBKDF2-HMAC-SHA256 (600,000 iterations) key derivation, AES-256-GCM encryption/decryption, binary header (`FTBK`) validation, and AEAD password authentication error handling. |
| **Backup Archive Packing** | `app/src/main/java/com/example/crypto/ChatBackupArchiveManager.kt` | `object ChatBackupArchiveManager` | Checkpoints Room DB WAL (`PRAGMA wal_checkpoint(FULL)`), packs Room DB file snapshot, Tink keyset preferences, and media into a ZIP archive, and performs restore operations. |
| **Google Drive REST Helper** | `app/src/main/java/com/example/util/GoogleDriveBackupHelper.kt` | `object GoogleDriveBackupHelper` | Manages Google Drive OAuth scope (`https://www.googleapis.com/auth/drive.appdata`), OAuth token retrieval via `GoogleAuthUtil`, and REST v3 HTTP operations (`search`, `upload`, `download`, `delete`) in Drive `appDataFolder`. |
| **Backup Preferences** | `app/src/main/java/com/example/util/ChatBackupPreferencesManager.kt` | `object ChatBackupPreferencesManager` | Persists drive account email, last backup timestamp, size, auto-backup frequency ("OFF", "DAILY", "WEEKLY"), include media toggle, Wi-Fi only toggle, and cached vault password. |
| **Background Auto-Backup** | `app/src/main/java/com/example/service/ChatBackupWorker.kt` | `class ChatBackupWorker : CoroutineWorker()` | Background `WorkManager` task enforcing battery and network constraints, executing background backup creation and upload, and calling Supabase `record_chat_backup`. |
| **SQLCipher Room Database** | `app/src/main/java/com/example/db/EncryptedChatDatabase.kt` | `abstract class EncryptedChatDatabase : RoomDatabase()` | Room Database (`version = 2`, file `encrypted_dm_chat_vault.db`) instantiated using `net.sqlcipher.database.SupportFactory(vaultKey)`. |
| **Encrypted DM Repository** | `app/src/main/java/com/example/repository/EncryptedChatRepository.kt` | `object EncryptedChatRepository` | Handles local storage, retrieval, and Tink HPKE encryption/decryption of direct messages. |
| **Encrypted Group Repository**| `app/src/main/java/com/example/repository/GroupChatRepository.kt` | `object GroupChatRepository` | Handles local storage, group key derivation, and group message processing. |
| **Chat Backup Settings UI** | `app/src/main/java/com/example/ui/screens/profile/ChatBackupScreen.kt` | `fun ChatBackupScreen(...)` | Jetpack Compose UI for Settings > Chat Backup. Manages Google Drive authorization, manual backup trigger, auto-backup options, and backup deletion. |
| **Restore Screen UI** | `app/src/main/java/com/example/ui/screens/chat/RestoreChatBackupScreen.kt` | `fun RestoreChatBackupScreen(...)` | Jetpack Compose UI for fresh install chat restore. Connects Drive, accepts vault password, decrypts, and restores local DB and identity keys. |
| **Vault Dialogs & Prompts** | `app/src/main/java/com/example/ui/screens/chat/VaultUnlockDialog.kt` | `fun VaultUnlockDialog(...)` | Handles user password prompt for vault unlock and updates cached vault password on success. |
| **Profile Options Sheet** | `app/src/main/java/com/example/ui/screens/home/tabs/ProfileTabContent.kt` | `fun ProfileTabContent(...)` | Displays profile settings bottom sheet containing the entry point for opening `ChatBackupScreen`. |

---

## 4. EXISTING BACKUP IMPLEMENTATION

### IMPLEMENTED
- **Google Drive OAuth Scope Request**: Configured for `https://www.googleapis.com/auth/drive.appdata` in `GoogleDriveBackupHelper.kt` using `GoogleSignInOptions` and `Scope`.
- **Google Drive REST v3 Client**: Direct OkHttp REST v3 client in `GoogleDriveBackupHelper.kt` targeting `appDataFolder` (`funkytalk_chat_backup.enc`) for file discovery, upload, download, and deletion.
- **Outer Encryption Engine**: `ChatBackupCryptoManager.kt` using PBKDF2-HMAC-SHA256 (600,000 iterations) key derivation, AES-256-GCM encryption with 12-byte nonce, and binary header validation (`FTBK` magic, format version, salt, nonce).
- **Archive Generator & Extractor**: `ChatBackupArchiveManager.kt` checkpointing Room WAL (`PRAGMA wal_checkpoint(FULL)`), snapshotting `encrypted_dm_chat_vault.db`, exporting `e2ee_tink_prefs` (`tink_keyset.json`), archiving optional media, and restoring components.
- **Supabase RPC Integration**: `SupabaseApi.kt` endpoints for `record_chat_backup`, `get_chat_backup_status`, and `delete_chat_backup_meta`.
- **Settings Screen UI**: Full Material 3 Compose screen in `ChatBackupScreen.kt` featuring Drive account connection status, manual "Back up now", auto-backup frequency selector (Off/Daily/Weekly), media toggle, Wi-Fi only toggle, backup deletion, and password recovery warning banner.
- **Restore Screen UI**: `RestoreChatBackupScreen.kt` handling post-login restore prompt when `has_backup = true`, password entry, decryption attempt, rate-limited retry delay on wrong password, key/DB restoration, and skip fallback option.
- **Background Worker**: `ChatBackupWorker.kt` registered with `WorkManager` enforcing network and battery constraints.
- **Vault Password Reuse & Caching**: Caching of vault password in `ChatBackupPreferencesManager` upon successful unlock/setup for background worker usage.

### PARTIALLY IMPLEMENTED
- **Immediate Re-encryption on Vault Password Change**: Vault setup and unlock handlers in `OnboardingChatVaultScreen.kt` and `VaultUnlockDialog.kt` update the cached password in `ChatBackupPreferencesManager`, but do not immediately trigger an upload task to overwrite the existing Drive file upon password change.
- **Media Archiving**: `ChatBackupArchiveManager.kt` includes files from `context.filesDir/chat_media/`, but media caching in `E2EEMediaManager.kt` also places files in `cacheDir` which are not currently included in the archive sweep.

### NOT IMPLEMENTED
- **Formal Google Drive Android SDK**: The project uses direct REST HTTP requests via `OkHttpClient` instead of the official Google Drive Java/Android Client library (`com.google.apis:google-api-services-drive`).
- **Granular Progress Bar**: Streaming chunked progress percentages for uploading/downloading large files (transfers are performed in single-pass ByteArrays).

### UNKNOWN
- **Supabase Database RPC Functions & RLS Policies**: The database function definitions (`record_chat_backup`, `get_chat_backup_status`, `delete_chat_backup_meta`) and SQL RLS policies on table `chat_backup_meta` exist on the remote Supabase server. NOT VERIFIED FROM REPOSITORY (client-side repository only).

---

## 5. DEPENDENCIES

Extracted directly from `gradle/libs.versions.toml` and `app/build.gradle.kts`:

| Domain | Dependency Notation | Version | Purpose |
| :--- | :--- | :--- | :--- |
| **Google Identity** | `com.google.android.gms:play-services-auth` | `21.2.0` (`playServicesAuth`) | Google Sign-In and OAuth token retrieval via `GoogleAuthUtil.getToken`. |
| **Drive API** | `com.squareup.okhttp3:okhttp` | `4.10.0` (`okhttp`) | Used for calling Google Drive v3 REST API endpoints directly. |
| **Supabase HTTP** | `com.squareup.retrofit2:retrofit` | `2.12.0` (`retrofit`) | Retrofit REST client for Supabase RPCs and API endpoints. |
| **Supabase JSON** | `com.squareup.retrofit2:converter-moshi` | `2.12.0` (`converterMoshi`) | Moshi converter for Retrofit API responses. |
| **Room Database** | `androidx.room:room-runtime`, `androidx.room:room-ktx` | `2.7.0` (`roomRuntime`) | Jetpack Room persistence library. |
| **SQLCipher** | `net.zetetic:android-database-sqlcipher` | `4.5.4` (`sqlcipher`) | Full SQLite database encryption layer for Room. |
| **Crypto (Tink)** | `com.google.crypto.tink:tink-android` | `1.15.0` (`tinkAndroid`) | Google Tink HPKE hybrid encryption library. |
| **Crypto (Biometric)**| `androidx.biometric:biometric` | `1.2.0-alpha05` (`biometric`) | Android Biometric Prompt API for Keystore key unwrapping. |
| **WorkManager** | `androidx.work:work-runtime-ktx` | `2.9.1` (`workRuntime`) | Background task scheduling for periodic auto-backup. |

---

## 6. ARCHITECTURE RISKS

1. **In-Memory Archive Memory Limits (OOM Risk)**:
   - `ChatBackupArchiveManager.createArchive` and `ChatBackupCryptoManager.encryptBackup` load the complete ZIP archive and ciphertext into in-memory `ByteArray` buffers (`ByteArrayOutputStream`, `ByteBuffer`).
   - *Risk*: If the user enables "Include Photos & Media" and the total media size exceeds ~50 MB–100 MB, creating or decrypting the in-memory array can trigger `java.lang.OutOfMemoryError` on devices with tight heap limits. Streaming file-based `CipherInputStream`/`CipherOutputStream` should be considered for large media backups.

2. **In-Memory Vault Password Caching**:
   - `ChatBackupPreferencesManager` stores the plaintext vault password in standard `SharedPreferences` (`chat_backup_prefs`) under `cached_vault_password` so `ChatBackupWorker` can run background auto-backups without prompting the user.
   - *Risk*: On rooted devices or via local device extraction, plaintext passwords in standard preferences files are accessible.

3. **Database Instantiation Concurrency During Restore**:
   - During restore, `ChatBackupArchiveManager.restoreFromArchive` calls `EncryptedChatDatabase.closeInstance()` and overwrites `encrypted_dm_chat_vault.db`.
   - *Risk*: If background threads or active UI Composables attempt to execute database queries via Room DAOs concurrently while the underlying `.db` file is being replaced, SQLite disk I/O errors or `SQLiteDatabaseLockedException` may occur.

4. **Lack of OAuth Token Auto-Refresh in Worker**:
   - `ChatBackupWorker.doWork()` attempts to retrieve an OAuth token using `GoogleAuthUtil.getToken()`.
   - *Risk*: If the user's Google OAuth refresh token is invalidated or revoked, `GoogleAuthUtil.getToken()` throws an exception, causing the worker to retry repeatedly without prompting the user to re-authenticate in the UI.

---

## 7. AUDIT HANDOFF

### What Was Inspected
- Complete codebase in `/app/src/main/java/com/example/` including crypto, db, repository, service, ui, and util packages.
- Build configurations in `/app/build.gradle.kts` and `/gradle/libs.versions.toml`.
- Manifest declarations in `/app/src/main/AndroidManifest.xml`.

### What Remains to Audit
- Remote Supabase Database RPC SQL definitions (`record_chat_backup`, `get_chat_backup_status`, `delete_chat_backup_meta`) and table schema for `chat_backup_meta`.
- Live testing of Google Drive OAuth authorization under production signing keys and Google Cloud Console OAuth consent screen configurations.

### Important Files for Subsequent Parts
- `app/src/main/java/com/example/crypto/ChatBackupCryptoManager.kt`
- `app/src/main/java/com/example/crypto/ChatBackupArchiveManager.kt`
- `app/src/main/java/com/example/util/GoogleDriveBackupHelper.kt`
- `app/src/main/java/com/example/service/ChatBackupWorker.kt`
- `app/src/main/java/com/example/ui/screens/profile/ChatBackupScreen.kt`
- `app/src/main/java/com/example/ui/screens/chat/RestoreChatBackupScreen.kt`
- `app/src/main/java/com/example/SupabaseApi.kt`
- `app/src/main/java/com/example/MainActivity.kt`

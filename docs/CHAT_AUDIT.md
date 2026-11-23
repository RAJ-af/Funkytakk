# FunkyTalk Chat Audit (`CHAT_AUDIT.md`)

> **Scope:** Read-only architectural and failure-mode audit of the current 1:1 DM and Group E2EE chat subsystems before adding P7/chat upgrades.  
> **Missing attachment note:** `docs/CHAT_UPGRADE_PLAN.md` is **not present** in `docs/` (`docs/` contains `2026-11-01-voice-rtc-integration.md`, `BACKEND.md`, `DESIGN_SYSTEM.md`, `apk_analysis_report.txt`, and `qa-notes.md`). This audit is derived directly from the Kotlin/Compose source code on disk.

---

## 1. File Map (Chat-Related Files)

### UI — Screens & Navigation
- `app/src/main/java/com/example/ui/screens/chat/DirectChatScreen.kt` — 1:1 DM screen: Room/optimistic timeline, voice/image composer, playback, reactions, report/block dialogs, vault gate on send.
- `app/src/main/java/com/example/ui/screens/chat/GroupChatScreen.kt` — Group E2EE screen: timeline, mentions, replies, voice/image send & playback, reactions, retry/delete failed sheet.
- `app/src/main/java/com/example/ui/screens/chat/GroupFlowContainer.kt` — Navigation/container host coordinating GroupChatScreen, GroupInfoScreen, ManageMembersScreen, and GroupSettingsScreen.
- `app/src/main/java/com/example/ui/screens/chat/GroupInfoScreen.kt` — Group profile, member list, shared media preview, mute/notification level, leave/report actions.
- `app/src/main/java/com/example/ui/screens/chat/GroupSettingsScreen.kt` — Group disappearing timer, `mediaAutoDownload` toggle, invite link controls, admin settings.
- `app/src/main/java/com/example/ui/screens/chat/GroupInviteSheet.kt` — Bottom sheet for sharing/regenerating group invite codes (`funkytalk://group/{code}` / `https://funkytalk-f1aae.web.app/g/{code}`).
- `app/src/main/java/com/example/ui/screens/chat/ManageMembersScreen.kt` — Admin UI to approve join requests, invite friends, promote/demote, or remove members.
- `app/src/main/java/com/example/ui/screens/chat/VaultUnlockDialog.kt` — `VaultManagementDialogs`, `VaultSetupDialog`, `VaultUnlockDialog`, and `VaultForgotPasswordDialog` (password, biometric, Drive restore, vault reset).
- `app/src/main/java/com/example/ui/screens/chat/RestoreChatBackupScreen.kt` — Google Drive encrypted backup discovery and restore UI.
- `app/src/main/java/com/example/ui/screens/profile/ChatBackupScreen.kt` — Google Drive backup configuration, schedule, include-media toggle, and manual backup trigger UI.
- `app/src/main/java/com/example/ui/screens/profile/ChatThemeStoreSheet.kt` — Chat wallpaper/theme preset selector modal.
- `app/src/main/java/com/example/ui/screens/OnboardingChatVaultScreen.kt` — Onboarding step creating the initial vault password and optional biometric wrap.
- `app/src/main/java/com/example/ui/screens/home/tabs/ChatsTabContent.kt` — Chats tab list, search/filters, inline vault lock/setup cards (`ModernUnlockVaultCard`, `ModernSetupPasswordCard`), and undecryptable message warning.

### UI — Chat Kit, Bubbles, Helpers & Components
- `app/src/main/java/com/example/ui/chat/ChatTheme.kt` — Preset themes (Butter, Sky, Mint, Graphite), dark/light adaptation, typography sizing.
- `app/src/main/java/com/example/ui/chat/ChatChrome.kt` — Reusable chat headers, composer dock, reply banner, jump-to-bottom FAB, context menu sheets.
- `app/src/main/java/com/example/ui/chat/ChatBubbles.kt` — Text, reply-quote, reaction pills, media frame, and `VoiceNoteContent` composables.
- `app/src/main/java/com/example/ui/chat/ChatMediaAndVoiceHelpers.kt` — `VoicePlaybackController` (ExoPlayer), `LiveVoiceRecorder` (`MediaRecorder`), `TelephotoMediaViewerDialog`, `AiWritingAssistBottomSheet`, `MessageLongPressOverlay`.
- `app/src/main/java/com/example/ui/chat/TelegramChatMedia.kt` — Pre-send multi-photo review sheet (`TelegramPendingMediaDialog`), `readChatImageBytes`, multi-photo album grid.
- `app/src/main/java/com/example/ui/chat/ChatEmojiPicker.kt` — Emoji/reaction picker panel.
- `app/src/main/java/com/example/ui/chat/ChatAppearanceAndDebugModals.kt` — Text scale / preset modal and debug inspection helpers.
- `app/src/main/java/com/example/ui/chat/ChatCustomIcons.kt` — Vector/SVG chat UI icons.
- `app/src/main/java/com/example/ui/chat/ChatKitGallery.kt` — Debug showcase for chat UI components.
- `app/src/main/java/com/example/ui/screens/chat/model/ChatModels.kt` — Legacy/shared chat UI data models (`ChatMessage`, status/type enums).
- `app/src/main/java/com/example/ui/screens/chat/components/AIActionButton.kt` — Composer AI assist trigger button.
- `app/src/main/java/com/example/ui/screens/chat/components/ChatActionPillsBar.kt` — Quick action chip bar in chat.
- `app/src/main/java/com/example/ui/screens/chat/components/ChatBottomInputBar.kt` — Legacy/component chat composer bar.
- `app/src/main/java/com/example/ui/screens/chat/components/ChatHeader.kt` — Legacy/component chat header.
- `app/src/main/java/com/example/ui/screens/chat/components/ChatImagePreviewDialog.kt` — Single image preview dialog.
- `app/src/main/java/com/example/ui/screens/chat/components/ChatMessageBubble.kt` — Component-level message bubble wrapper.
- `app/src/main/java/com/example/ui/screens/chat/components/ChatMessageContextMenu.kt` — Long-press actions dialog.
- `app/src/main/java/com/example/ui/screens/chat/components/ChatTopBar.kt` — Top bar component.
- `app/src/main/java/com/example/ui/screens/chat/components/CustomSvgIcon.kt` — Custom SVG icon loader.
- `app/src/main/java/com/example/ui/screens/chat/components/DmReactionComponents.kt` — DM reaction summary/sheet UI.
- `app/src/main/java/com/example/ui/screens/chat/components/GroupReactionComponents.kt` — Group reaction summary/sheet UI.
- `app/src/main/java/com/example/ui/screens/chat/components/ImageMessageBubble.kt` — Image bubble component.
- `app/src/main/java/com/example/ui/screens/chat/components/IncomingMessageBubble.kt` — Incoming bubble wrapper.
- `app/src/main/java/com/example/ui/screens/chat/components/LanguagePartnerBanner.kt` — Language-exchange tip banner in DM.
- `app/src/main/java/com/example/ui/screens/chat/components/LanguagePartnerCard.kt` — Language partner header card.
- `app/src/main/java/com/example/ui/screens/chat/components/MessageComposer.kt` — Text/voice composer widget.
- `app/src/main/java/com/example/ui/screens/chat/components/MessageContextMenu.kt` — Copy/Reply/Translate/Delete menu.
- `app/src/main/java/com/example/ui/screens/chat/components/OutgoingMessageBubble.kt` — Outgoing bubble wrapper.
- `app/src/main/java/com/example/ui/screens/chat/components/ReactionBadge.kt` — Reaction count badge.
- `app/src/main/java/com/example/ui/screens/chat/components/VoiceMessageBubble.kt` — Legacy voice note bubble UI.

### Repositories, Realtime & ViewModel Helpers
- `app/src/main/java/com/example/repository/EncryptedChatRepository.kt` — 1:1 DM outbox/inbox, optimistic overlay, HPKE encrypt/decrypt, Supabase `dm-media` upload/download, Realtime & cold-start inbox drain.
- `app/src/main/java/com/example/repository/GroupChatRepository.kt` — Group outbox/inbox, multi-recipient HPKE fan-out (`send_group_message`), Appwrite media upload, Realtime & recipient drain, member/group sync.
- `app/src/main/java/com/example/realtime/VoiceRoomRealtimeManager.kt` — Shared Phoenix WebSocket client handling `postgres_changes` for `dm_messages`, `group_message_recipients`, `group_events`, `group_members`, `group_conversations`, plus voice rooms.
- `app/src/main/java/com/example/repository/InboxDrainTracker.kt` — Tracks last drain fetched/saved/failed counts and undecryptable failure reasons for UI banners/Settings debug.
- `app/src/main/java/com/example/repository/TombstoneManager.kt` — Local deletion/tombstone tracking so deleted chats/messages do not resurrect.
- `app/src/main/java/com/example/repository/BlockRepository.kt` — Blocked user IDs state & Supabase block/unblock RPCs checked during DM send/UI display.
- `app/src/main/java/com/example/AuthViewModel.kt` — Triggers vault/E2EE teardown on logout (`resetForLogout`) and wipe on account deletion (`wipeForUid`).
- `app/src/main/java/com/example/ChatPreferencesManager.kt` — Per-UID `SharedPreferences` for pinned/muted/deleted chats, minimalist mode, theme preset, text scale.

### Crypto, Security & Identity Scoping
- `app/src/main/java/com/example/crypto/E2EEManager.kt` — Tink HPKE (`DHKEM_X25519_HKDF_SHA256_HKDF_SHA256_AES_256_GCM`) keypair management, `user_public_keys` sync, `encryptMessage`/`decryptMessage`, and `ChatTimingTracker`.
- `app/src/main/java/com/example/crypto/E2EEMediaManager.kt` — Per-attachment one-time AES-256-GCM encryption/decryption (`encryptMediaPayload`, `decryptMediaPayload`).
- `app/src/main/java/com/example/crypto/VaultSecurityManager.kt` — Vault password PBKDF2 wrap (310,000 iters), Android Keystore biometric wrap, cooldown tracking, and in-memory SQLCipher `vaultKey`.
- `app/src/main/java/com/example/crypto/BackupCrypto.kt` — PBKDF2 backup master key wrapping (600,000 iters), AES-256-GCM chunk crypto, Tink `AesGcmHkdfStreaming` for media streams, and Keystore persistence for background backups.
- `app/src/main/java/com/example/crypto/DriveBackupEngine.kt` — Encrypted NDJSON chunked Google Drive backup & restore engine for DMs, groups, reactions, and group local media.
- `app/src/main/java/com/example/ActiveUserManager.kt` — UID-scoped file/pref/Keystore alias naming so accounts on the same device never mix local DBs or keys.

### Local Storage (Room + SQLCipher)
- `app/src/main/java/com/example/db/EncryptedChatDatabase.kt` — SQLCipher (`SupportFactory(vaultKey)`) Room DB v10 (`chat_<uid>.db`) with 8 entities.
- `app/src/main/java/com/example/db/LocalDmEntities.kt` — `LocalDmConversation`, `LocalDmMessage`, `LocalDmMessageReaction` entities.
- `app/src/main/java/com/example/db/LocalDmDao.kt` — `DmConversationDao`, `DmMessageDao`, `DmMessageReactionDao` queries & optimistic reconcile logic.
- `app/src/main/java/com/example/db/LocalGroupEntities.kt` — `LocalGroupConversation`, `LocalGroupMessage`, `LocalMessageReaction`, `LocalMessageTranslation` entities.
- `app/src/main/java/com/example/db/LocalGroupDao.kt` — `GroupConversationDao`, `GroupMessageDao`, `MessageReactionDao`, `MessageTranslationDao`.
- `app/src/main/java/com/example/db/InboxFailureEntity.kt` — `InboxFailureEntity` (`inbox_failures` table) & `InboxFailureDao` for undecryptable row tracking.

### Network, Media Storage, Workers & Services
- `app/src/main/java/com/example/SupabaseApi.kt` — Retrofit REST/RPC/Storage API for `dm_messages`, `dm_conversations`, `dm-media`, `send_group_message`, `group_message_recipients`, `get_my_groups`, `upload-to-appwrite`, `user_public_keys`.
- `app/src/main/java/com/example/SupabaseClient.kt` — OkHttp/Retrofit singleton (does **not** attach `X-Appwrite-Project` header).
- `app/src/main/java/com/example/AppwriteUploader.kt` — Uploads bytes to Appwrite via Supabase Edge Function `functions/v1/upload-to-appwrite`.
- `app/src/main/java/com/example/util/VoiceRecorderManager.kt` — Singleton `MediaRecorder` helper (`.aac` filename, MPEG_4 container).
- `app/src/main/java/com/example/util/DriveRestApiClient.kt` — Google Drive `appDataFolder` REST client for encrypted backups.
- `app/src/main/java/com/example/util/ChatBackupPreferencesManager.kt` — Backup schedule, Drive account, include-media flag, and Tink-encrypted cached vault password for background backup.
- `app/src/main/java/com/example/service/ChatBackupWorker.kt` — WorkManager `CoroutineWorker` running periodic `DriveBackupEngine.performBackup`.
- `app/src/main/java/com/example/service/ChatRestoreWorker.kt` — WorkManager `CoroutineWorker` running Drive restore.
- `app/src/main/java/com/example/service/ChatBackupScheduler.kt` — Schedules periodic/one-off `ChatBackupWorker` jobs.
- `app/src/main/java/com/example/service/PushNotificationManager.kt` — FCM token sync, foreground open-chat suppression, push notification routing for DMs/groups.
- `app/src/main/java/com/example/service/FunkyFirebaseMessagingService.kt` — Receives FCM pushes, triggers background inbox drain / notification posting while respecting vault lock.
- `app/src/main/java/com/example/util/SystemNotificationHelper.kt` — Builds Android chat notifications (hides preview text if vault is locked).

---

## 2. Crypto API Inventory

### 2.1 Message & Identity Keypair E2EE (`E2EEManager.kt`) — **KEEP UNCHANGED**
Algorithm: Google Tink Hybrid Encryption `DHKEM_X25519_HKDF_SHA256_HKDF_SHA256_AES_256_GCM` (RFC 9180 HPKE, empty `CONTEXT_INFO = ByteArray(0)`), private keyset stored via `AndroidKeysetManager` encrypted under `android-keystore://e2ee_hpke_master_key_<uid>`.

| Function | Exact Signature | Must Stay Unchanged? |
|---|---|---|
| Key init & publish | `suspend fun ensureIdentityKeyInitialized(context: Context, forceSync: Boolean = false): String?` (`Dispatchers.IO`) | **YES** (server schema & version bump compatibility) |
| Encrypt (sync) | `fun encryptMessage(recipientPublicKeyBase64: String, plaintext: String): String` | **YES** (wire HPKE format) |
| Encrypt (async) | `suspend fun encryptMessageAsync(recipientPublicKeyBase64: String, plaintext: String): String` (`Dispatchers.Default`) | **YES** |
| Decrypt (sync) | `fun decryptMessage(context: Context, ciphertextBase64: String): String` | **YES** (wire HPKE format) |
| Decrypt (async) | `suspend fun decryptMessageAsync(context: Context, ciphertextBase64: String): String` (`Dispatchers.Default`) | **YES** |
| Cached self pubkey | `fun getMyPublicKeyBase64(context: Context? = null): String?` | **YES** |
| Teardown / wipe | `fun resetForLogout()` / `fun wipeForUid(context: Context, uid: String)` | **YES** |

### 2.2 Attachment Media E2EE (`E2EEMediaManager.kt`) — **KEEP UNCHANGED**
Algorithm: Fresh random 32-byte AES key + 12-byte GCM nonce per file (`AES/GCM/NoPadding`, 128-bit tag).

| Function | Exact Signature | Must Stay Unchanged? |
|---|---|---|
| Encrypt blob | `fun encryptMediaPayload(rawBytes: ByteArray): EncryptedMediaResult` | **YES** (DM & Group wire format) |
| Decrypt blob | `fun decryptMediaPayload(ciphertextBytes: ByteArray, keyBytes: ByteArray, nonceBytes: ByteArray): ByteArray` | **YES** (DM & Group wire format) |

### 2.3 Vault Key & Unlock (`VaultSecurityManager.kt`)
Derives KEK with `PBKDF2WithHmacSHA256` (310,000 iters) to wrap 32-byte SQLCipher `vaultKey`, plus optional `BiometricPrompt.CryptoObject` AES-256-GCM wrap (`vault_biometric_master_key_<uid>`).

| Function | Exact Signature | Status / Notes |
|---|---|---|
| Status init | `fun init(context: Context)` | Safe to extend for P7 lock-timeout check |
| Active key load | `fun getActiveVaultKey(context: Context? = null): ByteArray?` | **Currently bypasses lock UI** by silently calling `BackupCrypto.loadVaultKeyFromKeystore(context, uid)` (`userAuthenticationRequired=false`) |
| Password setup | `suspend fun setupVaultWithPassword(context: Context, password: String): Result<ByteArray>` | **Keep PBKDF2/AES-GCM envelope format unchanged** |
| Password unlock | `suspend fun unlockWithPassword(context: Context, password: String): Result<ByteArray>` | **Keep PBKDF2/AES-GCM & cooldown unchanged** |
| Biometric wrap | `fun createBiometricEncryptCryptoObject(context: Context? = null): Result<BiometricPrompt.CryptoObject>` & `fun saveBiometricWrappedVaultKey(context: Context, authenticatedCipher: Cipher): Result<Unit>` | Can supplement with P7 Level 1 + Level 2 Keystore remember-key |
| Biometric unwrap | `fun createBiometricDecryptCryptoObject(context: Context): Result<BiometricPrompt.CryptoObject>` & `fun unlockWithBiometricCipher(context: Context, authenticatedCipher: Cipher): Result<ByteArray>` | Same as above |
| Lock / Wipe | `fun lock()`, `fun resetForLogout()`, `fun resetVault(context: Context, uid: String? = null)`, `fun wipeForUid(context: Context, uid: String)` | **Keep SQLCipher `vaultKey` length (32B) & wipe semantics unchanged** |

### 2.4 Backup Crypto (`BackupCrypto.kt`) — **KEEP UNCHANGED**
- `wrapMasterKey(masterKey, password, firebaseUid)` / `unwrapMasterKey(headerJson, password, firebaseUid)` (PBKDF2 600k + AAD `"funkytalk-backup-v1:$uid"`).
- `encryptChunk(data, chunkId, masterKey)` / `decryptChunk(encryptedData, chunkId, masterKey)`.
- `encryptMediaStream(inputStream, outputStream, masterKey, associatedData)` / `decryptMediaStream(...)` (Tink `AesGcmHkdfStreaming`, 1 MB segments).
- `saveVaultKeyToKeystore(context, vaultKey, uid)` / `loadVaultKeyFromKeystore(context, uid)`.

---

## 3. Send Flow & Receive Flow (Step-by-Step)

### 3.1 1:1 DM Text Send (`EncryptedChatRepository.kt`)
1. **Tap & Optimistic UI (`DirectChatScreen` -> `enqueueSendMessage`)**: Runs on caller (`Main`) -> synchronously generates `clientId = UUID`, records `ChatTimingTracker.markTap(clientId)`, prepends `LocalDmMessage(status=SENDING)` to `_optimisticMessages` (`StateFlow`) so the bubble renders before any DB/network work, and updates conversation preview in memory.
2. **Local DB Insert (`Dispatchers.IO` inside `repoScope`)**: Upserts `LocalDmConversation` (using temporary `"dm_pending_<uid>"` if new) and inserts `LocalDmMessage(status=SENDING)` via `db.messageDao()`, then removes `clientId` from `_optimisticMessages` once DB emits.
3. **Outbox Dispatch (`dispatchDmOutboxMessage` on `Dispatchers.IO`, serialized per-message via `activeDmSendIds` + `Mutex`)**: Retries up to 3 attempts (exponential backoff 1.2s / 2.4s / 4.8s; skips retry on `UserBlockedException`, missing key/recipient, HTTP 400/403).
4. **Key Fetch & Server Conv Resolution (`executeDmSendAttempt`)**:
   - Fetches recipient's public key from in-memory `publicKeyCache` (15-min TTL) or `GET rest/v1/user_public_keys`.
   - Resolves or creates `dm_conversations` row (`resolveOrConfirmServerConversationId`), migrating local `"dm_pending_<uid>"` rows via `migrateTempConversation` and adding alias mapping.
5. **HPKE Encrypt (`Dispatchers.Default`)**: `E2EEManager.encryptMessageAsync(recipientPublicKey, plaintext)` -> Base64 ciphertext (`nonce = ""`, `key_version`).
6. **Network POST**: `POST rest/v1/dm_messages` (`SupabaseApi.sendDmMessage`). HTTP 2xx or 409/23505 (idempotent duplicate PK) marks `LocalDmMessage.status = LocalMessageStatus.SENT`.

### 3.2 1:1 DM Receive (`EncryptedChatRepository.kt` & `VoiceRoomRealtimeManager.kt`)
1. **Triggers**:
   - **Realtime WebSocket**: `VoiceRoomRealtimeManager` subscribes on OkHttp background thread to `postgres_changes` (`table = dm_messages`, filtered by `recipient_id=eq.<myUid>`) -> emits `RealtimeTableEvent.DmMessageReceived` -> collected by `EncryptedChatRepository.startRealtimeListener` (`repoScope`).
   - **Inbox Drain**: `fetchAndProcessPendingInbox` (`Dispatchers.IO`, `Semaphore(4)`) runs on repo init (when vault unlocked), `onAppResumed`, Realtime `Reconnected`, and FCM wake -> `GET rest/v1/dm_messages?select=*&order=created_at.asc&recipient_id=eq.<myUid>`.
2. **Vault & Dedup Gate**: If `database == null` (vault locked), returns `false` and leaves row on server for later drain. Deduplicates against `processedMessageIds` and `db.messageDao().messageExists(msgId)`.
3. **Decrypt (`Dispatchers.Default`)**: `E2EEManager.decryptMessageAsync(context, ciphertext)`. On failure, inserts row in local `inbox_failures` (`type="DM"`) and leaves server row untouched.
4. **Save & Mailbox Delete (`processIncomingDmMessage`)**:
   - Reaction JSON: updates `dm_message_reactions`, then calls `DELETE rest/v1/dm_messages?id=eq.<msgId>`.
   - Text: calls `saveDecryptedMessageLocally` (inserts `LocalDmMessage`, updates `LocalDmConversation` preview & unread count if chat not foregrounded), **then** calls `DELETE rest/v1/dm_messages?id=eq.<msgId>` and clears `inbox_failures`.
   - Media JSON: saves placeholder `"[img]downloading..."` / `"[voice]downloading...|N"` in Room **and deletes the `dm_messages` row immediately**, before launching an unawaited `scope.launch(Dispatchers.IO)` to download the media blob.

### 3.3 Group Text Send (`GroupChatRepository.kt`)
1. **Tap & Optimistic UI (`enqueueSendGroupMessage`)**: Generates `clientId = UUID`, prepends `LocalGroupMessage(status=SENDING)` to `_optimisticGroupMessages` (`StateFlow`), records `ChatTimingTracker.markTap(clientId)`.
2. **DB Insert (`Dispatchers.IO`)**: Inserts `LocalGroupMessage(status=SENDING)` into `group_messages`, drops optimistic overlay.
3. **Outbox Dispatch (`dispatchGroupOutboxMessage`, `Dispatchers.IO`, per-message `Mutex`, up to 3 attempts)**:
   - Fetches joined member UIDs (`getJoinedMemberIdsForSend`, cached short TTL) & missing `user_public_keys` in parallel.
   - Builds plaintext or `__e2ee_v2` JSON envelope (text, `mentions`, `reply_to_*`).
   - **Multi-recipient HPKE Fan-out (`Dispatchers.Default`)**: Calls `E2EEManager.encryptMessage(pubKey, payload)` for every joined member **including self** (`nonce = clientId` for self-echo reconciliation; random UUID for others).
   - **Network RPC**: `POST rest/v1/rpc/send_group_message` with `{ p_group_id, p_message_type, p_recipients }`. Marks `status = SENT` (HTTP 409 also treated as sent).

### 3.4 Group Receive (`GroupChatRepository.kt` & `VoiceRoomRealtimeManager.kt`)
1. **Triggers**:
   - **Realtime WebSocket**: Subscribes to `postgres_changes` on `group_message_recipients` (filtered `recipient_id=eq.<myUid>`), `group_events`, `group_members`, `group_conversations`, plus per-group channels -> emits `RealtimeTableEvent.GroupRecipientReceived`.
   - **Inbox Drain**: `drainPendingGroupMessages` (`Dispatchers.IO`, `Semaphore(4)`) calls `GET rest/v1/group_message_recipients?select=*&order=created_at.asc&recipient_id=eq.<myUid>`.
2. **Vault & Group Check**: If `database == null`, logs `[GroupInbox] ... error=Database not unlocked` and returns `false`. If `groupId` not in Room, calls `syncUserGroups` (`rpc/get_my_groups`).
3. **Decrypt (`Dispatchers.Default`)**: `E2EEManager.decryptMessageAsync(context, row.ciphertext)`. Failure writes to `inbox_failures` (`type="GROUP"`).
4. **Reconcile / Save & Mailbox Delete (`processSingleRecipientRow`)**:
   - If `senderId == myUserId`, calls `db.groupMessageDao().reconcileIncomingFromMe(localMsg, echoClientId = row.nonce)` (updates optimistic/local row instead of duplicating).
   - Inserts into `group_messages`, updates `group_conversations` (`unreadDelta`), posts notification if closed.
   - **Mailbox row deleted asynchronously**: `scope.launch(Dispatchers.IO) { deleteRecipientRow(token, row.id) }` -> `DELETE rest/v1/group_message_recipients?id=eq.<rowId>`.

---

## 4. Media Flow (Image & Voice) + Silent Failure Audit

### 4.1 Recording & Capturing
- **Images**: Picked via zero-permission `PickMultipleVisualMedia(10)` -> `TelegramPendingMediaDialog` -> `readChatImageBytes()` (`TelegramChatMedia.kt`) decodes/compresses to JPEG bytes, written to `filesDir/encrypted_vault_media/<uid>/` before outbox enqueue.
- **Voice (`LiveVoiceRecorder` in `ChatMediaAndVoiceHelpers.kt`)**:
  - Uses `MediaRecorder` with `OutputFormat.MPEG_4` + `AudioEncoder.AAC` (44.1 kHz, 128 kbps) written to `cacheDir/voice_recordings/rec_<ts>.m4a`.
  - Reads raw bytes on `stop()`, passes `mimeType = "audio/aac"` and `durationSec` to `enqueueSendMediaMessage` / `enqueueSendGroupMediaMessage`.

### 4.2 Upload, `storage_path`, Download, Decrypt, Cache & Playback

| Stage | 1:1 DM (`EncryptedChatRepository`) | Group (`GroupChatRepository`) |
|---|---|---|
| **Encrypt blob** | `E2EEMediaManager.encryptMediaPayload(rawBytes)` on `Dispatchers.Default` (AES-256-GCM) | Same (`E2EEMediaManager.encryptMediaPayload`) |
| **Upload target** | **Direct Supabase Storage** (`PUT`/`POST storage/v1/object/dm-media/{path}` via `SupabaseClient.api.uploadDmMedia`) — does **NOT** use `upload-to-appwrite` | **Edge Function `upload-to-appwrite`** (`AppwriteUploader.uploadWithToken(..., purpose="group-media", folderSuffix=groupId)`) |
| **What goes into `storage_path`** | Relative Supabase object path `"$serverConvId/$clientId.bin"` (stored both in `dm_messages.storage_path` column AND encrypted descriptor JSON) | Full **Appwrite file view URL** `https://.../v1/storage/buckets/.../files/.../view?project=...` (or relative path if legacy) |
| **Download on receiver** | Immediately triggers `scope.launch(Dispatchers.IO)` `downloadDmMediaBlob` with 4 tiers: direct HTTP URL -> `storage/v1/object/authenticated/dm-media/{path}` -> `storage/v1/object/dm-media/{path}` -> `createSignedUrl("dm-media")` | If `group.mediaAutoDownload == true`, triggers `scope.launch(Dispatchers.IO)` `downloadAndDecryptGroupMediaBlob` via `SupabaseClient.api.downloadFromUrl(url)` (for `http...`) or `downloadGroupMedia`. Manual tap retry exists via `downloadGroupMessageMediaOnDemand` |
| **Decrypt & Cache** | `E2EEMediaManager.decryptMediaPayload` -> `saveLocalMediaFile` saves to `filesDir/encrypted_vault_media/<uid>/` (`mimeType="audio/aac"` -> **`.aac`**; `"image/jpeg"` -> **`.jpg`**), updates Room `text` to `"[voice]file://<absPath>|<sec>"` / `"[img]file://<absPath>"`, then **deletes server storage object** (`DELETE storage/v1/object/dm-media/{cleanPath}`) | `E2EEMediaManager.decryptMediaPayload` -> saves to `filesDir/encrypted_vault_media/<uid>/group_<msgId>.m4a` (voice) or `.jpg` (image) and updates `LocalGroupMessage.localMediaPath` (does NOT delete Appwrite object) |
| **UI Playback / Render** | Images: Coil `AsyncImage(model = url)`. Voice: `DirectChatScreen` extracts `path = rawText.removePrefix("[voice]").substringBefore("|")` -> passes `"file:///data/..."` to `VoicePlaybackController.playOrPause()` | Images: `msg.localMediaPath ?: rawText...`. Voice: `msg.localMediaPath ?: rawText...` -> `VoicePlaybackController.playOrPause()` |

### 4.3 Silent Failure Points & Code Handling

| Potential Failure Point | Currently Handled? | Exact Behavior in Code |
|---|---|---|
| **1. `file://` prefix passed to `VoicePlaybackController`** | **NO (BROKEN)** | `EncryptedChatRepository` writes `"[voice]file:///data/...|N"` into `LocalDmMessage.text` (both outgoing L1291 and incoming L739) and leaves `localMediaPath` unused on UI. `DirectChatScreen` L1271 passes `"file:///data/..."` to `VoicePlaybackController.resolveAudioSourceToLocalFile`, which calls `File(source)` **without stripping `"file://"`** -> `File.exists()` is always `false` -> ExoPlayer never loads; shows `"Audio file could not be downloaded or decrypted"`. |
| **2. Group Appwrite download missing `X-Appwrite-Project` / HTTP error JSON** | **PARTIAL / BROKEN** | `FunkyTalkApplication.newImageLoader()` adds `X-Appwrite-Project` header for Coil, **not** for Retrofit `SupabaseClient.api.downloadFromUrl()` used in `GroupChatRepository.downloadAndDecryptGroupMediaBlob()`. Also `GroupChatRepository` does not check `isJsonErrorBytes()` before decrypting, so an Appwrite error JSON throws `AEADBadTagException`, caught silently (`return null`). |
| **3. DM deletes `dm_messages` row BEFORE media finishes downloading** | **NO (STUCK FOREVER)** | `processIncomingMediaDescriptor` deletes the `dm_messages` row (L705) before background download/decrypt completes (L708), and never saves `storage_path`/`media_key`/`media_nonce` to Room. If download fails (offline, timeout, process death, token refresh race), the bubble stays stuck on `downloading...` with **no `downloadDmMediaOnDemand` retry**! |
| **4. Media payload decryption exception (`AEADBadTagException` / wrong key)** | **PARTIAL** | Caught in `EncryptedChatRepository` L757 & `GroupChatRepository` L2988 (`Log.w` + returns `null`), leaving DM stuck on `downloading...` or Group `localMediaPath = null`. No user-visible retry or failure badge on standalone voice/DM image bubbles. |
| **5. Missing `RECORD_AUDIO` permission** | **YES** | Checked via `ContextCompat.checkSelfPermission` and requested with `ActivityResultContracts.RequestPermission()`. Refusal triggers a `Toast` (no crash). |
| **6. Voice recording < 500ms or stop failure** | **SILENT** | `LiveVoiceRecorder.stop()` (L412) returns `null` silently if duration < 500ms with no user toast. `MediaRecorder.start()` failure catches and shows Toast. |
| **7. Container vs `.aac` extension mismatch** | **NO** | `LiveVoiceRecorder` & `VoiceRecorderManager` write `OutputFormat.MPEG_4` + `AudioEncoder.AAC`, but `EncryptedChatRepository.saveLocalMediaFile` saves `"audio/aac"` with `.aac` extension (raw ADTS extension for an MP4 container). |
| **8. Expired/deleted DM storage object** | **NO** | Receiver calls `deleteStorageObjectFromServer` right after decrypt. If multi-device or retry happens, object returns HTTP 404/JSON error (`isJsonErrorBytes` catches it -> returns `null` -> permanent `downloading...`). |
| **9. Group voice `duration_seconds` omitted** | **NO** | `GroupChatRepository.executeGroupSendAttempt` (L2348) omits `duration_seconds` in the encrypted JSON descriptor and receiver sets `text = "Voice message"`, so `GroupChatScreen` L1010 parses `"0"` -> always shows `0:00` until played. |
| **10. Group `mediaAutoDownload == false` on voice/single image** | **PARTIAL** | Album items show a download trigger, while standalone Group `MessageItem` voice/image bubbles do not wire `downloadGroupMessageMediaOnDemand` to a tap retry control. |

---

## 5. Why Voice / Image Can Fail on the Other Phone (Ordered by Likelihood)

1. **`file://` URI prefix breaks `VoicePlaybackController` in DMs (and fallback paths in Groups)**: `File("file:///data/user/0/...")` does not exist on Android (`ChatMediaAndVoiceHelpers.kt:276`). **100% failure rate for DM voice notes** even when download & decrypt actually succeeded!
2. **DM inbox row deleted before media download finishes + no retry/persisted key**: Killing/backgrounding the app or a transient network blip right after `processIncomingMediaDescriptor` leaves `"[voice]downloading..."` / `"[img]downloading..."` permanently unrecoverable because `media_key`, `media_nonce`, and `storage_path` were never persisted to `LocalDmMessage`.
3. **Group media Appwrite download auth / header gap**: `GroupChatRepository.downloadAndDecryptGroupMediaBlob()` uses `SupabaseClient.api.downloadFromUrl(storagePath)` with `apiKey = null, auth = null` and no `X-Appwrite-Project` header. If the Appwrite bucket/URL requires that header or token, download returns 401/403 JSON (or HTTP failure) and silently returns `null`.
4. **Recipient `user_public_keys` mismatch (`key_version` / reinstall without backup) or locked vault**: If receiver reinstalled or regenerated HPKE keys (`identity_key_in_use` recovery in `E2EEManager`) and sender cached an old `publicKeyCache` entry (15-min TTL), Tink `HybridDecrypt.decrypt()` throws `GeneralSecurityException`.
5. **MPEG_4 container saved as `.aac` in DMs**: `EncryptedChatRepository.saveLocalMediaFile()` writes MP4-wrapped AAC bytes to `*.aac`, which can confuse OEM extractors vs `.m4a`.
6. **Group `mediaAutoDownload = false` or 401 token expiry mid-download**: Standalone Group image/voice bubbles lack a manual `downloadGroupMessageMediaOnDemand` trigger.

### Logcat Tags & Lines to Check
- `ChatMediaAndVoiceHelpers`: tag `VoicePlayback` -> `"Failed to resolve audio source: ..."` or ExoPlayer `"Playback failed: ..."`, and tag `VoiceRecorder` -> `"Failed to start recording"` / `"Stop failed"`.
- `EncryptedChatRepository`: `"downloadDmMediaBlob"`, `"Media blob unavailable at path ..."`, `"Background media processing failed for ..."` , `"Failed to decrypt DM message ..."`.
- `GroupChatRepository`: `"[GroupInbox] id=..., message_type=..., result=decrypt_failed"`, `"Failed downloading/decrypting media blob for ..."`.
- `AppwriteUploader`: `"upload-to-appwrite failed with code ..."`.
- `E2EEMediaManager`: `"Encrypted media payload: rawSize=..."`, `"Decrypted media payload successfully: size=..."` (never logs keys or plaintext).
- `InboxDrainTracker` / `SettingsScreen` (Debug): shows `"Inbox: fetched X, saved Y, failed Z"`.

---

## 6. Local Storage & Chat Backup Contract

### 6.1 Room + SQLCipher (`EncryptedChatDatabase.kt`, DB v10)
- **File**: `databases/chat_<sanitizedUid>.db` encrypted with SQLCipher `SupportFactory(vaultKey)` (32-byte random key).
- **Entities (8)**:
  1. `dm_conversations` (`LocalDmConversation`: `id`, `otherUserId`, `otherUserName`, `otherUserAvatar`, `lastMessageText`, `lastMessageTime`, `unreadCount`, ...)
  2. `dm_messages` (`LocalDmMessage`: `id`, `conversationId`, `senderId`, `recipientId`, `text`, `messageType`, `timestamp`, `isOutgoing`, `replyToText`, `replyToSender`, `clientId`, `status`, `failReason`, `retryCount`, `nextRetryAt`, `localMediaPath`, `mediaMimeType`, `mediaDurationSec`, `mediaCaption`)
  3. `dm_message_reactions` (`LocalDmMessageReaction`: `conversationId`, `targetMessageId`, `senderId`, `emoji`, `ts`)
  4. `group_conversations` (`LocalGroupConversation`: `id`, `name`, `description`, `avatarUrl`, `createdBy`, `isCreator`, `myRole`, `disappearingSeconds`, `mediaAutoDownload`, `mutedUntil`, `notifyLevel`, ...)
  5. `group_messages` (`LocalGroupMessage`: `id`, `groupId`, `senderId`, `senderName`, `senderAvatar`, `text`, `messageType`, `eventType`, `localMediaPath`, `storagePath`, `mediaKey`, `mediaNonce`, `mimeType`, `timestamp`, `expiresAt`, `isOutgoing`, `mentionsJson`, `isMentioned`, `replyToMessageId`, `replyToSenderId`, `replyToSenderName`, `replyToPreview`, `clientId`, `status`, `failReason`, `retryCount`, `nextRetryAt`)
  6. `group_message_reactions` (`LocalMessageReaction`)
  7. `message_translations` (`LocalMessageTranslation`)
  8. `inbox_failures` (`InboxFailureEntity`: `messageId`, `type`, `reason`, `timestamp`)

### 6.2 Files on Disk
- **DM & Group decrypted local media**: `filesDir/encrypted_vault_media/<uid>/` (plus `filesDir/media/<uid>/` for Drive-restored group media).
- **Voice note recordings (temp pre-send)**: `cacheDir/voice_recordings/rec_<ts>.m4a` (`LiveVoiceRecorder`) or `cacheDir/voice_rec_<ts>.aac` (`VoiceRecorderManager`); ExoPlayer remote cache in `cacheDir/voice_notes/`.
- **Image loader cache**: `cacheDir/image_cache/` (Coil 150 MB).

### 6.3 SharedPreferences & Keystore (UID-Scoped via `ActiveUserManager`)
- `e2ee_tink_prefs_<uid>`: AndroidKeysetManager encrypted Tink HPKE keyset (`e2ee_hpke_master_key_<uid>`) + `e2ee_key_version`.
- `vault_envelope_prefs_<uid>`: PBKDF2 salt/IV/wrapped `vaultKey`, biometric IV/wrapped `vaultKey` (`vault_biometric_master_key_<uid>`), failed attempts & cooldown.
- `funkytalk_bk_keystore_prefs_<uid>`: Android Keystore (`funkytalk_bk_keystore_alias_<uid>`) wrapped backup master key (`wrapped_bk_keystore`) and wrapped `vaultKey` (`wrapped_vault_key_keystore`).
- `chat_backup_prefs_<uid>`: Drive email, backup frequency, `include_media`, last backup timestamp/size, and Tink AEAD encrypted cached vault password (`vault_password_wrapper_key_<uid>`).
- `funkytalk_chat_prefs_<uid>`: Chat theme preset, text scale, minimalist mode, pinned/muted/deleted lists.

### 6.4 Exact Chat Backup Read / Write Contract (`DriveBackupEngine.kt` & `BackupCrypto.kt`)
Any future schema/storage changes **must preserve** these exact fields:
- **Key lookup**: `DriveBackupEngine.performBackup` obtains `activeVaultKey = VaultSecurityManager.getActiveVaultKey(context) ?: BackupCrypto.loadVaultKeyFromKeystore(context)` to open `EncryptedChatDatabase`.
- **Google Drive `appDataFolder` files**:
  - `ft_header.enc`: JSON (`version=1`, `iterations=600000`, `salt`, `nonce`, `wrapped_key`, `uid=firebaseUid`) wrapping 32B Master Key (`BK`) under PBKDF2-derived KEK + AAD `"funkytalk-backup-v1:$firebaseUid"`.
  - `c_<uuid>.enc`: Gzipped NDJSON chunk encrypted with `BK` (12B nonce + AES-GCM ciphertext, AAD = `chunkId`), up to 1000 messages per chunk:
    - DM records read/restored: `"type":"dm_conv"` (`id`, `other_user_id`, `other_user_name`, `other_user_avatar`, `last_msg_text`, `last_msg_time`), `"type":"dm_msg"` (`id`, `conv_id`, `sender_id`, `recipient_id`, `text`, `msg_type`, `timestamp`, `is_outgoing`, `reply_text`, `reply_sender`), `"type":"dm_reaction"` (`conv_id`, `target_id`, `sender_id`, `emoji`, `ts`). *(Note: DM `localMediaPath` files referenced inside `text` as `[img]file://...` are not separately uploaded as `m_<sha256>.enc` chunks; Group `msg.localMediaPath` files < 100 MB ARE streamed via `AesGcmHkdfStreaming` into `m_<sha256>.enc` when `includeMedia = true`).*
    - Group records read/restored: `"type":"group_conv"`, `"type":"group_msg"` (includes `local_media_path`), `"type":"group_reaction"`.
  - `ft_manifest.enc`: Encrypted `BackupManifest` JSON listing all chunk IDs, types (`"messages"` / `"media"`), SHA-256, and byte sizes.

---

## 7. Speed Profile (Tap -> Bubble)

Latency instrumentation already exists in `E2EEManager.kt` (`ChatTimingTracker`, **DEBUG-only**, logs **only** 8-char `clientId` prefix and millisecond durations — zero content/keys/tokens) under logcat tag **`ChatTiming`**:
- `ChatTimingTracker.markTap(clientId)` -> `markBubbleShown(channel, clientId)`: logs `[DM|GROUP] id=<8char> tap_to_bubble_ms=<ms>`.
- **Stage breakdown from code**:
  1. **Tap -> Optimistic Bubble (~5–30 ms)**: `_optimisticMessages` / `_optimisticGroupMessages` `MutableStateFlow` updates synchronously in memory before `Dispatchers.IO` Room write -> bubble renders immediately with `Sending` status.
  2. **Room Insert (~10–40 ms)**: SQLCipher insert & reactive `Flow` emission replaces optimistic overlay seamlessly.
  3. **Key Resolution (cached 0 ms; cold cache 150–600 ms)**: `getRecipientPublicKeyCached` (DM, 15m TTL) or `fetchMemberPublicKeysParallel` + `getJoinedMemberIdsForSend` (Group).
  4. **Encrypt (`ChatTiming` -> `encrypt_ms`)**:
     - DM text: single Tink HPKE X25519+AES-256-GCM call on `Dispatchers.Default` (~1–5 ms).
     - Group text: N parallel/loop Tink HPKE calls (1 per joined member), scales linearly (~1–4 ms per recipient).
     - Media (image/voice): AES-256-GCM payload encrypt + storage upload (`dm-media` or `upload-to-appwrite` Edge Function), typically 300–2500+ ms on mobile networks.
  5. **Network Send (`ChatTiming` -> `send_request_ms`, `total_to_ack_ms`)**:
     - DM: `POST rest/v1/dm_messages`.
     - Group: `POST rest/v1/rpc/send_group_message`.
  6. **Realtime Delivery (`ChatTiming` -> `realtime_receive_ms` / `server_echo_ms`)**: Phoenix WebSocket `postgres_changes` push + Tink decrypt + Room write.

*(Exact device millisecond numbers require running a debug build on hardware; no Android device/emulator is attached in this build environment.)*

---

## 8. Keep / Replace / Missing List

### Keep Untouched (Working & Contract-Critical)
- `E2EEManager` HPKE keypair, `user_public_keys` schema, and ciphertext format.
- `E2EEMediaManager` AES-256-GCM payload encryption/decryption format.
- `VaultSecurityManager` PBKDF2 password key-wrapping format & escalating brute-force cooldown.
- `DriveBackupEngine` & `BackupCrypto` backup container, chunk NDJSON schema, and Drive REST client.
- `BlockRepository` enforcement & `ReportUserDialog` last-20 DM proof payload.
- Outbox state machine (`SENDING`/`SENT`/`FAILED`, optimistic `StateFlow`, 409 idempotency) and DM/Group image capture + pre-send `TelegramPendingMediaDialog` flow.
- `SecureWindow` / `setRecentsScreenshotEnabled(false)` privacy protection.

### Broken (Needs Targeted Fix)
1. **DM Voice Playback (`file://` URI mismatch)**: `VoicePlaybackController.resolveAudioSourceToLocalFile()` does not strip `"file://"`, causing DM voice notes to fail playback immediately.
2. **DM Media Download Durability**: `dm_messages` server row is deleted before blob download completes and `storage_path`/`media_key`/`media_nonce` are not saved on `LocalDmMessage`, leaving failed downloads permanently stuck on `downloading...` with no retry.
3. **Group Media Appwrite Download**: `downloadAndDecryptGroupMediaBlob()` omits `X-Appwrite-Project` / error-body checks when calling `downloadFromUrl`.
4. **DM Voice Extension & Group Voice Duration**: DMs save MPEG_4 audio with `.aac` extension; Group media JSON omits `duration_seconds`.
5. **Vault Lock / Biometric Remember Bypass (addressed in P7)**: `VaultSecurityManager.getActiveVaultKey()` currently calls `BackupCrypto.loadVaultKeyFromKeystore()` unconditionally without checking lock policy/timeout/14-day rule, while `DirectChatScreen` / `GroupChatScreen` lack an automatic cold-start unlock overlay.

### Missing
- On-demand retry (`downloadDmMediaOnDemand`) and persisted media keys for `LocalDmMessage` (mirroring `LocalGroupMessage`).
- Retry UI affordance on failed/un-downloaded standalone voice and single-image bubbles.
- Inline error feedback when stopping a voice recording shorter than 500 ms.
- Hardware benchmark timings from `ChatTiming` logcat on target test phones (Oppo A78 / Galaxy M02).

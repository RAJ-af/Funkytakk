# C1b — DM media must survive offline, app kill and retry (audit "Broken" #2 and "Missing")

WORK IN ONE ANSWER: list the files you need (max 5 lines), then IMMEDIATELY implement, show every changed file in full, and finish with a list of what you could not verify. If it is too big, finish steps 1-4 completely and say what is left.

Attach `docs/CHAT_AUDIT.md`. Do NOT touch: E2EEManager, E2EEMediaManager (format), VaultSecurityManager, DriveBackupEngine/BackupCrypto, block/report, image sending UI. Never log text, keys, tokens or file paths.

Problem (audit 3.2 and 4.3): for an incoming DM image/voice the app deletes the `dm_messages` row BEFORE the blob is downloaded and never saves `storage_path`, `media_key`, `media_nonce`. One network blip or app kill = the bubble stays on "downloading..." forever.
Server facts (already ready): bucket `dm-media` is private, 25 MB limit, both chat participants can read and delete objects. Blobs that nobody downloaded are now removed by the server after 4 days (fixed today, a nightly job).

1. **Room (DB v10 -> v11):** add nullable columns to `LocalDmMessage`: `storagePath`, `mediaKey` (base64), `mediaNonce` (base64), `mediaDownloadState` (PENDING, DOWNLOADING, READY, FAILED, EXPIRED), `mediaFailReason`. Write an explicit `Migration(10, 11)` (no destructive fallback) and a migration test on a v10 database. The database is SQLCipher-encrypted, so keys are safe there.
2. **Chat Backup contract (audit 6.4) must not change:** the new columns are NOT exported; restore of an old backup must still work. Confirm `DriveBackupEngine` still compiles and say how you checked.
3. **New order in `processIncomingMediaDescriptor`:** (a) ONE Room transaction: insert the message with the descriptor and state PENDING; (b) only then delete the `dm_messages` row; (c) start a unique WorkManager download job per message (network constraint, exponential backoff, survives process death); (d) on success: decrypt, save the file, state READY, update the bubble, and ONLY THEN delete the blob with the Storage API (as today); (e) on failure: state FAILED with a reason (network / HTTP code / decrypt) and keep the blob.
4. **Retry:** `downloadDmMediaOnDemand(messageId)` and, at app start and when the network returns, resume PENDING/DOWNLOADING/FAILED items (max 2 at the same time).
5. **Expired:** after retries, HTTP 404 or a JSON error body = EXPIRED, bubble says "Media expired. Ask them to send it again."
6. **UI states for standalone voice and image bubbles in DM (and group when auto-download is off or a download failed):** "Tap to download" + size, progress ring, "Failed. Tap to retry", "Expired". Wire the existing `downloadGroupMessageMediaOnDemand` to the tap in groups.
7. Debug-only list in Settings (debug): media items with state and reason code (no content).
Done when (two phones): receiver in airplane mode when the voice note arrives -> network back -> it downloads by itself and plays; app killed during download -> resumes; old text chats and backups unaffected.

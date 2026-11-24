# C1a — Voice and media quick fixes (small and safe; from the audit "Broken" list)

WORK IN ONE ANSWER: list the files you need (max 5 lines), then IMMEDIATELY implement, show every changed file in full, and finish with a list of what you could not verify.

Attach `docs/CHAT_AUDIT.md`. Do NOT touch: E2EEManager, E2EEMediaManager, VaultSecurityManager, backup code, block/report, image sending. Never log text, keys or tokens.

1. **DM voice never plays (audit 4.3 #1):** `VoicePlaybackController.resolveAudioSourceToLocalFile()` (ChatMediaAndVoiceHelpers.kt ~L276) does `File(source)` with a `file:///...` string. Fix in ONE place: accept `file:///...`, `file://...`, plain absolute paths and `content://`; for the file scheme use `Uri.parse(source).path`; require the file to exist and have size > 0. If it does not exist, show the existing error text and log (tag VoicePlayback) only the reason, not the path. Do not change what is stored in Room.
2. **Extension/MIME (audit #7):** `EncryptedChatRepository.saveLocalMediaFile()` writes an MPEG_4 container as `.aac`. Save voice as `.m4a` (mime `audio/mp4`). Keep reading old `.aac` files that are already on disk.
3. **Group voice duration (audit #9):** add `duration_seconds` to the encrypted descriptor JSON in `GroupChatRepository.executeGroupSendAttempt` (~L2348) and read it on receive so `GroupChatScreen` (~L1010) shows the real length instead of 0:00. It is an extra JSON field only: old apps must ignore it, no other wire change.
4. **Short recording (audit #6):** when a recording is shorter than 500 ms show a toast "Hold the mic and speak" (today it silently does nothing).
5. **Group Appwrite download (audit #2):** in `downloadAndDecryptGroupMediaBlob()` (a) only for URLs on the Appwrite host, add header `X-Appwrite-Project` taken from the `project=` query parameter of the URL (do not hardcode it), (b) never send the Supabase token or apikey to Appwrite, (c) check `isJsonErrorBytes()` BEFORE decrypting, and on an error mark the item failed with a reason (HTTP code or decrypt) instead of returning null silently.
6. Small unit tests: path resolver (file:///, file://, plain path, missing file), voice extension/MIME mapping, duration JSON round trip.

Done when: a voice note sent from phone A plays on phone B (DM and group), the group voice shows the right duration, a 200 ms press shows the toast, and old voice files still play.

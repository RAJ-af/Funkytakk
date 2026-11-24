# C1c — Video and audio files + "Save to gallery" (new features, after C1a and C1b work)

WORK IN ONE ANSWER: list the files you need (max 5 lines), then IMMEDIATELY implement, show every changed file in full, and finish with a list of what you could not verify.

Attach `docs/CHAT_AUDIT.md`. Reuse the existing media pipeline and the C1b download states. Do NOT change E2EEMediaManager's format, crypto, backup, block, report. Never log text, keys, tokens.

Server facts (ready): message types `video`, `audio`, `file`, `sticker` are now accepted for DM and group. DM media goes to the private Supabase bucket `dm-media` (limit 25 MB). Group media goes through `upload-to-appwrite` (tell me the real size limit you find). Undownloaded blobs are removed after 4 days.

1. **Video (DM + group):** pick/record, compress to 720p H.264 with Media3 Transformer (add the dependency if missing and tell me), hard limit 20 MB after compression (show a clear message if larger), small thumbnail, send as message_type `video` using the same encrypt/upload/descriptor flow as images. `E2EEMediaManager` works on a whole ByteArray: check memory use with a 20 MB file on a low-end phone (Galaxy M02) and tell me if there is an out-of-memory risk instead of guessing.
2. **Audio files (mp3/m4a/aac) as message_type `audio`:** small player card with title, duration, play/pause, speed 1x/1.5x/2x.
3. **Receiver:** same state machine as C1b (tap to download, progress, retry, expired). Inline video player (Media3, tap for full screen), no auto-play with sound.
4. **Save to gallery (images and videos):** long-press -> "Save to gallery" using MediaStore (Pictures/FunkyTalk, Movies/FunkyTalk), no storage permission on API 29+; also "Share". Show a toast "Saved".
5. **Privacy:** strip EXIF/GPS from images before sending; downloaded media stays in app-private storage until the user saves it.
Done when (two phones): a 15 MB video and a 2-minute voice note arrive, play and can be saved; an old app version shows nothing broken.

# 2026-10-30 Chat: voice notes, images, reactions, replies, locked vault

Backend status: LIVE (migration `20261030000000_dm_reactions_media_policies_room_end_fix.sql`).

What the backend check found (so the app work is targeted):
- Voice notes / images: CORRECTION (2026-10-30, later): the app stores chat media in APPWRITE through the Edge Function `upload-to-appwrite` (purpose `dm-media` / `group-media`, `folder_suffix` = conversation/group id), NOT in Supabase Storage. The Supabase buckets `dm-media` / `group-media` were never used (they are empty and had no policies; the policies were added anyway and are harmless). The real cause of "Audio file could not be downloaded or decrypted" is NOT known yet, so the pipeline below must log every step to find it.
- Reactions never reached the other person: `dm_messages.message_type` only allowed text/image/voice, so a 'reaction' row was rejected. 'reaction' is now allowed (group chat already allowed it). The server does not send a push for reactions.
- Replies: nothing on the server is needed, the reply info must travel inside the encrypted payload (the app currently sends only the text).

Paste the block below into Google AI Studio.

```text
You are working on the Funkytalk Android app (Kotlin, Jetpack Compose, Supabase backend, end-to-end encrypted 1:1 and group chat with Tink HPKE). The server is the dumb mailbox: it only ever sees ciphertext. Fix and finish chat media, reactions and replies. Keep everything that already works (text messages, the inbox drain, push, the vault), do not change the encryption scheme, and keep decrypting old messages.

## Server contract (already live, do not change)
- Table public.dm_messages: id, conversation_id, sender_id, recipient_id, ciphertext, nonce, key_version, created_at, delivered_at, message_type ('text' | 'image' | 'voice' | 'reaction'), storage_path.
- Media storage = Appwrite via the existing Edge Function upload-to-appwrite (verify_jwt, send the normal Supabase Authorization header). multipart/form-data fields: purpose ('dm-media' | 'group-media' | 'avatar' | 'cover'), folder_suffix (conversation_id for dm-media, group_id for group-media; required for those two), file (the bytes). It returns {ok:true, file_id, url, folder}. For chat media upload the ENCRYPTED bytes only (any file name, e.g. <uuid>.bin). Max 25 MB. The returned url (or file_id) goes into the encrypted message descriptor / dm_messages.storage_path. Do NOT upload chat media to Supabase Storage.
- Files are public-by-URL in Appwrite (read "any"), which is acceptable ONLY because chat media is encrypted with a one-time key that exists only inside the encrypted message. Never upload plaintext chat media.
- The server tracks every upload in public.media_files and DELETES chat media (dm-media / group-media) automatically 4 days after upload (cron worker). There is no client-side delete call. So the receiver must download and save media locally right away, and must never rely on the server copy later. avatar / cover uploads keep only the newest 2 files per user.
- The server skips push notifications for message_type 'reaction'.

## 1. Voice notes and images must just work (like WhatsApp / Telegram)
Today the receiver sees the toast "Audio file could not be downloaded or decrypted". Rebuild the media pipeline for 1:1 and group chat:
 a) SEND: record/pick -> compress -> encrypt with a fresh one-time AES-GCM key -> upload through the Edge Function upload-to-appwrite (see the server contract; keep the returned file_id/url) -> ONLY IF the upload succeeded insert the message row (storage_path + the encrypted descriptor that already carries the one-time key). Never insert a media message whose upload failed.
 b) The sender saves its own plaintext copy in app-private storage BEFORE uploading and always plays/shows from that local copy. The sender never needs the server copy again.
 c) RECEIVE: when a media message arrives (Realtime, push, or inbox drain) download and decrypt it IMMEDIATELY (do not wait for the user to tap play), save it in app-private storage, update the message to "ready", and only then delete the dm_messages row (do not try to delete the file, the server removes it after 4 days). If download or decrypt fails, keep the server row, show a "Tap to retry" state and retry automatically with backoff when the app is opened or the network returns.
 d) Message states in the bubble: sending (progress), sent, failed ("Tap to retry"), downloading (spinner), ready. No silent failures: always log HTTP status and body of failed storage/RPC calls with Log.d (never log keys or plaintext) and show a short toast.
 e) Voice notes: hold-to-record with slide-to-cancel and lock, AAC in .m4a (MediaRecorder), min 1 s, max 5 min, live timer, then a bubble with play/pause, seek bar, duration, 1x/1.5x/2x speed; only one player at a time; handle audio focus; recording permission request with a clear explanation.
 f) Images: system photo picker, compress (long side max 1600 px, JPEG quality ~80), blurred placeholder while downloading, tap to open a full-screen viewer with pinch-zoom.
 g) Group chat uses the same pipeline with purpose 'group-media' and the existing per-recipient fan-out.
 h) Add a debug log line for each step (picked, compressed, encrypted, uploaded, row inserted / row received, downloaded, decrypted, saved, object deleted, row deleted) so a failure shows exactly where it stops.

## 2. Reactions must reach the other person
Right now only I see the reactions I add. Implement reactions as real messages:
 - Add a stable client_message_id (UUID, generated by the sender) inside the encrypted payload of EVERY message, and store it in the local DB, so both devices can refer to the same message. For old messages without it, fall back to the server message id.
 - Sending a reaction: insert a dm_messages row with message_type = 'reaction' whose encrypted payload is {"type":"reaction","target_client_message_id":"<uuid>","emoji":"<emoji>","action":"add"|"remove"}. Group chat: same, through the existing group fan-out with message_type 'reaction'.
 - One reaction per person per message (a new emoji replaces the old one; "remove" clears it). Show the reaction chips under the bubble with counts, my own highlighted; tapping my own chip removes it.
 - Receiving: decrypt, apply to the local reactions table, then delete the server row. A reaction must NOT create an unread badge, a chat-list preview change, a notification, or a new bubble.
 - Update my own UI immediately (optimistic) and retry the send if it fails.

## 3. Replies must reach the other person
Right now the reply quote is not sent, only the message text. Include in the encrypted payload: "reply_to": {"client_message_id":"<uuid>","sender_id":"<uuid>","type":"text"|"image"|"voice","preview":"<first 100 chars, or Photo / Voice message>"}. The receiver stores it with the message and renders the same quote block as the sender. Tapping the quote scrolls to the original message if it exists locally (otherwise do nothing). Works in 1:1 and group chat.

## 4. Locked vault: show an unlock prompt, not an empty list
Chats can only be read after the user enters the Secure Chat Vault password. When the vault is locked, the Chats list (on the Chat tab AND in the "Chats" popup inside a voice room) currently looks clean/empty, which is confusing. Instead show a clear card: lock icon, "Unlock your chats", a password field with a show/hide toggle and an "Unlock" button (offer biometric unlock if the vault already supports it). Wrong password -> inline error, no crash. After unlocking, show the real list. Never show an empty chat list while the vault is locked. Do NOT remove the password requirement.

## 5. Chat inside a voice room must not take over the screen
Opening a conversation from the "Chats" popup in a voice room currently opens a full-screen chat and the room is lost. Open the conversation inside the same bottom sheet instead (full-height sheet with its own top bar and a back arrow that returns to the chat list sheet). The room, its audio state and the room screen stay alive underneath. The input bar must sit above the keyboard and above the system navigation bar.

## 6. General
 - Everything must use real data, no mock data.
 - Handle every error with a short user message, never swallow exceptions.
 - Make sure the project compiles and list every file changed.
 - At the end give me a table: step (picked, compressed, encrypted, uploaded, row inserted, downloaded, decrypted, saved) vs. result for one real voice note and one real image, taken from your debug logs.
```

## Add to docs/BACKEND.md

- `dm_messages.message_type` allows 'text', 'image', 'voice', 'reaction'. `notify_message_push` skips reactions.
- Chat media / avatars / covers live in Appwrite (Edge Function `upload-to-appwrite`), tracked in `media_files`; dm/group media is deleted by the cron worker 4 days after upload. Supabase Storage buckets `dm-media`, `group-media`, `covers` have path-scoped policies but are not used by the app.
- `discoverable_profiles` is no longer readable by `anon`.
- Known gap: the `avatars` bucket INSERT policy still has no ownership/path restriction.

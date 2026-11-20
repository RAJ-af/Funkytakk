# Funkytalk Backend Documentation

## Voice Rooms Seat Management Architecture (Backend Live Status)

### Edge Function: `get-room-token` (v21)
- **Endpoint:** `POST /functions/v1/get-room-token`
- **Request Body:** Strictly `{"room_id": "<uuid>"}` (Client MUST NOT send `"role"` field; server determines publisher vs audience role authoritatively based on `voice_rooms.host_id` and `room_participants.role`).
- **Response Fields:**
  - `token` / `rtc_token`: RTC token for voice engine (Agora).
  - `role`: `"publisher"` | `"audience"`
  - `can_publish`: `boolean` (`true` for host/speakers, `false` for listeners).
  - `max_speakers`: `4`
- **Error Codes:**
  - `401`: Unauthenticated request.
  - `403`: User suspended or restricted from speaking.
  - `404`: Room not found.
  - `410`: Room has ended.
  - `503`: All voice providers at capacity limit.
  - `seats_full`: Returned when all 4 speaker seats are occupied and a 5th user attempts to publish.

---

### Supabase Database RPCs

1. **`request_speaker_seat(p_room_id)`**
   - **Access:** Any listener in the room.
   - **Behavior:** Inserts or resets a row in `public.speaker_requests` with `kind = 'request'` and `status = 'pending'`.

2. **`cancel_speaker_request(p_room_id)`**
   - **Access:** Requester.
   - **Behavior:** Removes or cancels user's pending request in `public.speaker_requests`.

3. **`respond_to_speaker_request(request_id, accept)`**
   - **Access:** Host / Moderator only.
   - **Parameters:** `request_id` (UUID), `accept` (boolean).
   - **Behavior:** Updates request status. On accept, updates `public.room_participants.role` to `'speaker'` and `is_mic_enabled = true`.

4. **`invite_to_speaker_seat(p_room_id, p_user_id)`**
   - **Access:** Host / Moderator only.
   - **Target:** Must be a listener in the room.
   - **Behavior:** Creates an invitation row in `public.speaker_requests` with `kind = 'invite'` and `status = 'pending'`. Error `'seats_full'` if 4 speaker seats are already taken.

5. **`respond_to_speaker_invite(p_request_id, p_accept)`**
   - **Access:** Invited listener only.
   - **Parameters:** `p_request_id` (UUID), `p_accept` (boolean).
   - **Behavior:** Updates invite status. On accept, updates `public.room_participants.role` to `'speaker'` and `is_mic_enabled = true`.

6. **`demote_speaker(p_room_id, p_user_id default null)`**
   - **Access:** Host/Mod (for any speaker) OR Speaker (for self-demotion).
   - **Parameters:** `p_room_id` (UUID), `p_user_id` (UUID or null). `null` or caller's own ID = step down from seat myself; another user's ID = host moves speaker back to listener.
   - **Behavior:** Updates `public.room_participants.role` to `'listener'` and `is_mic_enabled = false`.

---

### Realtime Subscriptions
- `public.speaker_requests`: Listens to `INSERT`, `UPDATE`, `DELETE` events (`id, room_id, user_id, status, kind, invited_by, created_at`).
- `public.room_participants`: Listens to `INSERT`, `UPDATE`, `DELETE` events (`room_id, user_id, role, is_mic_enabled, joined_at`).

---

### Seat Grid & Constraints
- **Total Capacity:** Exactly 1 Host Seat + 4 Speaker Seats = 5 Seats.
- Server enforces `max_speakers = 4`. UI disables invites and shows "All seats are full (4/4)" when 4 speakers are seated.

---

## Room Ended & Participant Retention Contracts
- `end_voice_room` and the cron `check_and_cleanup_ended_rooms` set `left_at` on participant rows (to book RTC minutes) rather than deleting them instantly when a room ends. The cron deletes leftover participant rows approximately 1 minute later.
- Clients treat "room ended" signals (`voice_rooms` update/delete, `room_end_summaries` insert, or 5s polling fallback) as authoritative sources of truth, rather than relying solely on individual participant row disappearance.
- `room_end_summaries` is included in the Realtime publication.

---

## Chat Media & Reactions Storage Policies
- `dm_messages.message_type` allows `'text'`, `'image'`, `'voice'`, `'reaction'`. `notify_message_push` skips push notifications for reactions.
- Storage policies (all path-scoped by first folder):
  - `dm-media`: `<conversation_id>/...` (participants upload/read/delete).
  - `group-media`: `<group_id>/...` (joined members upload/read, uploader deletes).
  - `covers`: `<user_id>/...` (owner only).
- Leftover media purge jobs execute every 4 days. Receiving client deletes the `dm-media` storage object after downloading, decrypting, and saving locally.

---

## Account Deletion Architecture (Backend Live)

- **Tables:**
  - `account_deletions`: Permanent proof record (receipt id, `supabase_user_id`, `firebase_uid`, `email_hash` = HMAC-SHA256 of the lower-case email, `username`, `account_created_at`, status `scheduled` | `cancelled` | `purging` | `completed` | `needs_review` | `failed`, request time/IP/country/user agent/device, `auth_provider` and `reauth_auth_time`, `was_banned`, `had_active_suspension`, `steps`, `deleted_counts`, `residue`, `attempts`, `last_error`, email flags).
  - `media_files`: Inventory of Appwrite/Cloudinary files. Both tables have RLS enabled with service role access only.
- **Flow:**
  - `request-account-deletion`: Requires a Firebase ID token with `auth_time` within the last 5 minutes. On success, sets status to `scheduled`, hides profile immediately (`profiles.deletion_scheduled_at`, excluded from `discoverable_profiles`), revokes all user devices, and sends receipt email.
  - After the grace period (default 7 days via `DELETION_GRACE_DAYS`, or 0 for instant testing), `process-account-deletions` cron worker runs (pg_cron every 10 min, header `x-push-secret`, same secret as send-push):
    1. Media purge: Appwrite (tracked files + avatar/cover), Cloudinary (`moments/<uid>/`), and Supabase Storage.
    2. Firebase Auth user deletion via Identity Toolkit (`accounts:delete`).
    3. Database purge via `purge_user_data()`.
    4. Supabase Auth user deletion via `auth.admin.deleteUser`.
    5. Residue scan via `find_user_residue()` (must return `{}`; otherwise status becomes `needs_review`).
    6. Sends completion confirmation email.
  - Steps are idempotent and repeatable; failures set status to `failed` and are retried up to 10 attempts. Nothing is marked completed unless Firebase and the database are fully purged.
- **Kept for Audit & Safety:**
  - Gift history without sender/recipient identifiers.
  - Reports without reporter/reported personal identity.
  - Room end summaries without host personal identity.
  - The minimal deletion proof record (`account_deletions`).
- **Secrets Used:**
  - `FCM_SERVICE_ACCOUNT_JSON`: Must be the Firebase Admin SDK service account of project `funkytalk-f1aae` with permission to delete Auth users (`Firebase Authentication Admin`).
  - `BREVO_API_KEY`: For receipt and completion emails.
  - `APPWRITE_*`: For file uploads and purges.
  - `CLOUDINARY_*`: For Moments media purges.
  - `DELETION_GRACE_DAYS`: Default 7 (0 for testing).
  - `DELETION_PROOF_PEPPER`: Constant secret for HMAC-SHA256 email hash.
- **Staff Lookup:**
  - Edge Function `lookup-deletion-proof` (admin/superadmin JWT) with `{"email": "..."} | {"receipt_id": "..."} | {"username": "..."} | {"user_id": "..."}`.
- **Client RPCs:**
  - `get_my_deletion_status()`: Returns `{"pending": false}` or `{"pending": true, "receipt_id": "...", "status": "...", "scheduled_for": "...", "can_cancel": boolean}`.
  - `cancel_my_account_deletion()`: Cancels scheduled deletion and unhides the profile.
- **Chat Media in Appwrite:**
  - Chat media, avatars, and covers live in Appwrite via Edge Function `upload-to-appwrite`. Files in Appwrite are public by URL (`read("any")`); only encrypted chat media (with one-time keys stored in end-to-end encrypted message payloads) is permitted. Chat media is purged by cron worker 4 days after upload.
- **Known Limits:**
  - `room_reports.message_snapshot` (JSON) may retain historic chat text of other participants; residue scan verifies text/UUID columns across tables.

---

## Voice RTC Providers Multi-Vendor Architecture (Agora, Zego, Daily)

- `rtc_provider_settings` (provider, enabled, priority, monthly_limit) and `rtc_settings` (strategy 'priority' | 'balanced'). `assign_rtc_provider_for_room` picks among ENABLED providers below their limit. A room keeps its provider for its entire lifecycle.
- Staff: `admin_set_rtc_provider(p_provider, p_enabled)`, `admin_set_rtc_strategy(p_strategy)`; `get_rtc_provider_minutes_status()` (service role).
- `get-room-token` v22: Agora tokens last 1 hour (duration in seconds, not timestamp). Zego Token04 is server-generated; Daily `permissions.canSend` enforces publisher/listener rights.
- Usage counters estimate time spent inside rooms (join to leave), not exact SDK minutes. Compare with provider dashboards.

---

## Release Flags, User Blocking, Beta Signups & Session Minting (v22)
- `app_flags` + `get_app_flags()`: server switches the app reads at start (`coin_store_enabled`, `vip_enabled`, `min_supported_version_code`). Change with: `update public.app_flags set value='true'::jsonb, updated_at=now() where key='coin_store_enabled';`
- Blocking: `user_blocks` (RPC only), `block_user`, `unblock_user`, `get_my_blocked_users`, `is_blocked_with`; enforced server-side in `get_or_create_dm_conversation`, `send_gift`, `send_room_invite`.
- Beta signups: Edge Function `beta-signup` (public, no JWT) -> table `beta_signups`; rate limit 5/hour per hashed IP in `beta_signup_attempts`. Optional secrets: `BREVO_API_KEY` (welcome mail), `BETA_ALLOWED_ORIGINS` (comma list of site origins), `BETA_IP_SALT`.
- `verify-firebase-token` v22: retry-safe session minting (`503` + code `session_mint_failed` instead of `500`).

---

### Decoration metadata v2 (2026-11-20)
- `store_items.config` for asset avatar frames (`kind = "asset"`, `schema_version = 2`) includes `asset`:
  - `url`, `format`, `fps`, `size_px`, `source_size_px`, `animated`, `loop`, `inner_ratio`, `offset_x` (`-0.25..0.25`), `offset_y` (`-0.25..0.25`), `overflow_ratio` (`0..0.5`), `z` (`"above"` | `"below"`), `min_render_px`, `version`, `calibrated`, `avatar_ratio` (legacy copy), plus optional `meta` (unknown keys ignored).
- Shared `FramedAvatar` sizing rule:
  - Avatar photo always renders at `avatarSize` (circular, centred).
  - Asset frame renders at `canvas = avatarSize / inner_ratio` (`requiredSize(canvas)`), centred on the avatar, shifted by `(offset_x * canvas, offset_y * canvas)` with unclipped overflow (`outerSize = canvas * (1 + overflow_ratio)`).
  - `inner_ratio` fallback order: `inner_ratio` -> `avatar_ratio` -> `scale` -> `0.6`.
  - Respects `loop`, skips asset frame if rendered avatar px < `min_render_px`, and caches by `url + version`.
- Staff RPC: `admin_set_frame_calibration(p_code, p_inner_ratio, p_offset_x, p_offset_y, p_overflow_ratio)` -> returns updated `asset` JSON. Error codes: `forbidden` ("Only admins can do this"), `invalid_inner_ratio`, `invalid_offset`, `invalid_overflow_ratio`, `not_an_asset_frame`.


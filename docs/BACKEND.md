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

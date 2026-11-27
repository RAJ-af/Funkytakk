# Gemini prompt: Voice-room screen share (Android client)

Copy everything below the line into Gemini (Android Studio / AI Studio). Backend is DONE and deployed; this prompt is only for the app.

---

You are helping on **Funkytalk**, an Android language-exchange app. Backend = Supabase (project `dulpqochkxtamqztauea`). Voice rooms use **Agora for audio**. Do NOT touch or replace the Agora code.

## What I want
A **host-only screen share** inside a voice room. The screen VIDEO travels over **Daily.co** (a second, separate connection). Audio of the room stays on Agora exactly as today.

## Backend that already exists (do not change it, just call it)

Feature flag: `app_flags.screen_share_enabled` (read it with the existing `get_app_flags()` RPC). If false or missing, hide every screen-share button.

Edge Function `get-screenshare-token` (POST, send the normal user JWT):
- Start (host only): `{ "action": "start", "room_id": "<uuid>" }`
- Watch (anyone in the room): `{ "action": "watch", "share_id": "<uuid>" }`

Success response (JSON):
```json
{
  "provider": "DAILY",
  "share_id": "uuid",
  "room_id": "uuid",
  "host_id": "uuid",
  "is_sharer": true,
  "daily_room_name": "ss-<room uuid>",
  "room_url": "https://<domain>.daily.co/ss-<room uuid>",
  "domain": "<domain>",
  "token": "<daily meeting token>",
  "screen_quality": { "max_height": 720, "max_fps": 15 },
  "heartbeat_seconds": 30
}
```

Error response: `{ "error": "<code>" }` with these codes. Show a short friendly message for each:

| code | meaning | message to show |
|---|---|---|
| screen_share_disabled | flag is off | hide the feature |
| host_only | caller is not the host | "Only the host can share" |
| host_away | host left the room | "Host is away" |
| room_ended / share_ended | finished | close the viewer, no error toast |
| not_in_room | not a current participant | "Join the room first" |
| blocked / user_restricted | not allowed | "You can't watch this" |
| viewer_limit | too many watchers | "Too many people are watching" |
| daily_quota_reached | monthly cap | "Screen share is resting for now, try later" |
| daily_not_configured / daily_room_setup_failed / daily_token_failed | server problem | "Couldn't start. Try again" |
| unauthenticated | JWT problem | use the existing re-login handling |

Direct RPCs (Supabase, signed-in user, no extra checks needed on the client):
- `screen_share_heartbeat(p_share_id uuid)` returns `{ "active": true|false }`. Call every `heartbeat_seconds` (30 s) while sharing OR watching. If `active` is false, close everything.
- `leave_screen_share(p_share_id uuid)`: a viewer calls it to stop watching; the host calls it to stop sharing (this ends it for everyone).

Table `room_screen_shares` (read-only for the app, Realtime enabled, RLS lets people currently in the room read it). Columns: `id, room_id, host_id, status ('active'|'ended'), started_at, ended_at, end_reason`.
- On entering a room: `select ... from room_screen_shares where room_id = :room and status = 'active'` (0 or 1 row).
- Subscribe to Realtime `postgres_changes` on that table filtered by `room_id=eq.<room>` to show/hide the "Watch" banner live.

## Android work to do

### Host
1. Show a **Share screen** button only if: flag is on AND I am `voice_rooms.host_id`.
2. Before sharing, show a short **safe-share screen**: "Everything on your screen will be visible. Close private chats and notifications." with a 3-2-1 countdown, then continue.
3. Ask Android's MediaProjection permission and run a **foreground service of type mediaProjection** with a visible notification that has a **Stop** action. Follow Daily's official Android guide: https://docs.daily.co/guides/products/mobile/android-screen-share (check the current Daily Android SDK artifact/version in their docs and use the latest stable one; do not guess the version).
4. Call `get-screenshare-token` with `action: "start"`.
5. Join the Daily room with `room_url` + `token`, **microphone OFF, camera OFF** (the token cannot send them anyway). Do NOT ask for microphone permission for Daily. Start the screen share at max **720p, 15 fps** (`screen_quality`).
6. Start the heartbeat timer.
7. **Auto-stop:** if Daily reports **0 remote participants for 60 s in a row**, stop sharing (call `leave_screen_share`, leave Daily, stop the service). This saves free minutes.
8. Stop button (in the room UI and in the notification) calls `leave_screen_share`, leaves Daily, stops the service.
9. Also stop automatically when: I leave the room, the room ends, the app is killed, or the heartbeat says `active: false`.

### Viewer
1. When a row with `status='active'` exists for my room and I am not the host, show a banner **"<host> is sharing their screen. Watch"**. Do NOT join Daily yet.
2. Only when the person taps **Watch**: call `get-screenshare-token` with `action: "watch"`, join Daily (mic/camera OFF, receive only), and render the remote **screen** track full-width in a video view. (Every watcher costs one Daily minute per minute, so never auto-join.)
3. Heartbeat every 30 s. Show **Stop watching** (calls `leave_screen_share`, leaves Daily).
4. Leave Daily + call `leave_screen_share` when: the person leaves the room, closes the viewer, the share row becomes `ended`, or the app stays in the background for more than 60 s.
5. Keep the Agora audio running normally the whole time so people keep talking while one watches the screen.

## Hard rules
- Never use Daily for room audio. Agora stays the only audio path.
- Never auto-join Daily; viewers join only after tapping Watch.
- Every exit path must call `leave_screen_share` (it is what stops the minute counter). Add this to `onDestroy`/lifecycle too.
- Never log tokens.
- Keep it light: the target phone is a low-end Samsung Galaxy M02 (small RAM). One video view, no extra animations.
- Don't create a new Supabase client or auth flow. Reuse the app's existing one.

## When done, give me
1. The list of files changed/added.
2. A test checklist for TWO phones (host and viewer): start, watch, stop from host, stop from viewer, kill the host app, background the viewer for 70 s, host leaves the room, flag turned off.
3. Anything you were not sure about.

## Optional later (do NOT build now)
"Sparkle pointer": watchers tap the shared screen and a small sparkle appears for everyone, sent as a tiny Daily app message (no video cost).

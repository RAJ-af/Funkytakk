# 2026-10-30 Voice room fixes (invite list, mic, end screen), language icons, insets and screen sizes

Backend status: LIVE (migration `20261030000000_dm_reactions_media_policies_room_end_fix.sql`).

What the backend check found:
- Invite list: `list_invitable_following(p_room_id)` was tested as a real signed-in user with a real follower and returned the followed people. So the empty list is an app-side problem.
- Mic flicker: the server accepts `is_mic_enabled` updates from speakers and from the host (tested), so the flicker is an app-side feedback loop.
- Room-ended screen: the server used to delete all participant rows in the same instant the room ended, so clients could see "my row vanished" before "room ended". Now participant rows stay about 1 minute after the end (left_at is set), and the summary row (`room_end_summaries`) is published to Realtime.

Paste the block below into Google AI Studio.

```text
You are working on the Funkytalk Android app (Kotlin, Jetpack Compose, Supabase backend). Fix the voice room screen, the language icons and the system-bar / screen-size handling. Do NOT integrate any RTC/voice SDK in this task. No mock data anywhere.

## 1. "Invite Friend" list shows nobody (people I follow must appear)
The server is verified: rpc list_invitable_following(p_room_id) returns the people I follow (user_id, username, avatar_url, country_code, is_online, in_room, invite_status), online first, max 100. It works when called by a signed-in user. So find the app bug:
 - Call it exactly like this: supabase.rpc("list_invitable_following", buildJsonObject { put("p_room_id", <voice_rooms.id as the full UUID>) }). The header shows a short code like "FT6913"; that is NOT the id, do not pass it.
 - Do not filter the result client-side (no "online only"), do not require is_online.
 - Log the raw response and any error with Log.d, and show errors in the UI as a toast with the message. Show the empty state "Follow people to invite them" ONLY when the call succeeded with zero rows.
 - Show a loading state, and refresh the list every time the popup opens.
 - Buttons: "Invite" -> rpc send_room_invite(p_room_id, p_user_id); "Invited" when invite_status = 'pending'; "In room" when in_room = true. Error codes: already_invited, not_following, already_in_room, not_in_room, room_not_live, rate_limited, cannot_invite_self.

## 2. Speaker mic icon still flickers (second attempt, do it strictly)
Server facts: a speaker's own update of room_participants.is_mic_enabled works. The flicker is in the app. Do exactly this:
 - Create ONE class (for example RoomMicController) that owns my mic state: a MutableStateFlow<Boolean> micOn. Nothing else may change it except (a) my tap on the mic button and (b) the single moment my role changes from non-speaker to 'speaker' (set it once to the server's value).
 - My own seat/mic icon is drawn from micOn ONLY. Never draw my own icon from the list of participants coming from the database.
 - When a Realtime event for room_participants arrives: for OTHER users update their is_mic_enabled; for MY OWN row ignore the is_mic_enabled field completely.
 - Writing to the database is only a mirror: debounce 300 ms, skip the write if the value equals the last value I wrote, serialize writes (one at a time), never read the result back into micOn.
 - Remove every other piece of code that sets the mic state (search the whole project for is_mic_enabled, isMicEnabled, micEnabled, setMic, toggleMic and list them in your answer).
 - Add Log.d for every micOn change with its source (USER_TAP, ROLE_CHANGE, ...), so I can send you the log if it still flickers.
 - The host path must use the same controller.

## 3. Programming-language icons are still country flags
In "Select Language" -> Programming (and the Popular Tech row, the selected chip, the room card and the room header) every programming language still shows the US flag. Fix it in ONE place: a composable LanguageIcon(language) that is used everywhere. Rules:
 - Spoken languages: keep the flag.
 - Programming languages: NEVER show a flag. Step 1 (must work offline, no libraries): a rounded-square badge with the letters below on the brand colour (white text, black text on yellow). Step 2 (optional, only if step 1 is done and working): try the official logo from https://cdn.jsdelivr.net/gh/devicons/devicon/icons/<slug>/<slug>-original.svg with the Coil SVG decoder and fall back to the badge when it fails.
 - Badge table (name -> letters, colour): Python Py #3776AB; JavaScript JS #F7DF1E (black text); TypeScript TS #3178C6; Kotlin Kt #7F52FF; Java Jv #ED8B00; C++ C++ #00599C; Ruby Rb #CC342D; SQL SQL #336791; HTML/CSS H5 #E34F26; R R #276DC3; Scala Sc #DC322F; Shell/Bash sh #4EAA25; Go Go #00ADD8; Rust Rs #CE412B; Swift Sw #F05138; PHP PHP #777BB4; C# C# #512BD4; Dart Dt #0175C2; C C #5C6BC0. For any other of the 19 languages use 1-3 letters and a stable colour derived from the name.
 - voice_rooms.language_flag may hold a flag/emoji for spoken rooms; for programming rooms store the language code so LanguageIcon can draw the badge (and accept an icon URL if it starts with "http").
 - Tell me exactly which files draw a language flag today and confirm they all use LanguageIcon now.

## 4. Room-ended screen never appears
Server facts: when a room ends, public.voice_rooms gets is_live = false, status = 'ended', ended_at, ended_reason ('host_ended' | 'host_timeout' | 'moderation'); a row is written in public.room_end_summaries (room_id, title, host_id, host_username, host_avatar_url, started_at, ended_at, duration_seconds, total_joins, peak_participants, ended_reason) which is in Realtime and readable by any signed-in user. The participant rows are now kept for about 1 minute after the end. The ended voice_rooms row is deleted a few minutes later, so the summary table is the reliable source. NOTE: for private rooms a former member may no longer be able to read voice_rooms after the end, so never depend only on voice_rooms.
Build it so that ANY of these signals opens the RoomEndedScreen, using a single function onRoomEnded(reason?) that is idempotent:
 a) Realtime UPDATE on voice_rooms (id = this room) with status = 'ended' or is_live = false, or Realtime DELETE of that row;
 b) Realtime INSERT on room_end_summaries with room_id = this room;
 c) a fallback poll every 5 seconds while I am in a room: select status, is_live from voice_rooms where id = this room; if the row is missing, also select from room_end_summaries where room_id = this room; either result means the room ended;
 d) the host's own End button after rpc end_voice_room(p_room_id) succeeds (parameter name p_room_id).
Rules:
 - Do NOT leave the screen because my own room_participants row disappeared. When my row disappears, first check (c) up to 3 times, 1 second apart; only if the room is still live show "You were removed from the room".
 - onRoomEnded immediately stops the mic and releases any voice connection, hides the seats and shows RoomEndedScreen full screen. Never auto-close it, system back goes to the rooms list.
 - RoomEndedScreen: title, host avatar + name, message by reason (host_ended = "The host ended this room", host_timeout = "The host was away for more than 3 minutes", moderation = "This room was closed by a moderator", otherwise "This room has ended"), stats (duration h:mm:ss, Total visitors = total_joins, Peak listeners = peak_participants), button "Back to rooms". Load the summary from room_end_summaries (retry 3 x 500 ms); if it is missing still show the screen with what you know.
 - Add Log.d lines for every signal (a, b, c, d) so I can see which one fired.
 - Before ending, the host sees a confirmation "End room for everyone?".
 - Host-away banner: while voice_rooms.host_left_at is not null and the room is live show "Host is away - room closes in m:ss" counting down to host_left_at + 3 minutes; hide it when host_left_at becomes null.

## 5. Gesture navigation, 3-button navigation (old phones) and different screen sizes
The app is not prepared for system bars and different displays. Fix globally:
 - Call enableEdgeToEdge() in MainActivity and handle insets explicitly everywhere. Use Scaffold with contentWindowInsets, and WindowInsets.systemBars / navigationBars / statusBars / ime. Bottom bars, bottom sheets, input bars and the room's bottom bar (Chat / People / Mic / Reactions / More) must never sit under the gesture pill or the 3-button navigation bar: add navigationBarsPadding() or the matching inset padding. Chat and message inputs use imePadding() so they stay above the keyboard. Set android:windowSoftInputMode="adjustResize".
 - Make status-bar and navigation-bar icon colours correct for the current screen background (light/dark).
 - No fixed pixel/dp heights for whole screens or sheets; use weights, fillMaxSize, maxHeight fractions and BoxWithConstraints. Seat grids and lists must adapt (use LazyVerticalGrid / FlowRow with adaptive cells). Check small phones (360x640), normal (411x891), tall (412x915), tablets/foldables, "Display size: Large" and font scale 1.3.
 - Bottom sheets: limit height to the available space minus the status bar, and make their content scrollable.
 - Add @Preview annotations for the main screens with device specs for a small phone, a normal phone and a tablet, and fix everything that clips or overlaps.
 - List every screen you changed and what the problem was.

## General
 - Handle every RPC error with a short user message; log error codes with Log.d (never tokens).
 - Make sure the project compiles, and list every file changed.
```

## Add to docs/BACKEND.md

- `end_voice_room` and the cron `check_and_cleanup_ended_rooms` no longer delete participant rows at the moment a room ends: they set `left_at` (books RTC minutes) and the cron deletes the rows about a minute later. Clients must treat "room ended" signals (voice_rooms update, `room_end_summaries` insert, poll) as the truth and not the disappearance of their own participant row.
- `room_end_summaries` is in the Realtime publication.

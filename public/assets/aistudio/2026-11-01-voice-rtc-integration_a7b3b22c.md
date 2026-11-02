# 2026-11-01 Real voice in rooms: Agora + Zego + Daily

Backend status: LIVE
- migration `20261101000000_rtc_provider_settings.sql` (which providers are on, how new rooms are spread)
- Edge Function `get-room-token` v22 (Agora token lifetime fixed to 1 hour)

Why three steps, not one: a room keeps the provider it got when it was created, and by default the server uses Agora until its monthly limit and only then Zego, then Daily. Without switches the Zego and Daily code would stay untested until Agora runs out (surprise outage). So: build one shared layer + Agora first, test it alone, then add Zego, test it alone, then Daily, test it alone, and only then turn all three on together.

Server switches (Supabase SQL editor; a room keeps its provider, so create a NEW room after switching)

```sql
-- test ONE provider only (example: zego)
update public.rtc_provider_settings set enabled = (provider = 'zego');
-- all three on, new rooms spread evenly by usage
update public.rtc_provider_settings set enabled = true;
update public.rtc_settings set strategy = 'balanced' where id = 1;
-- first enabled provider until its limit, then the next
update public.rtc_settings set strategy = 'priority' where id = 1;
-- a provider misbehaves: switch it off for NEW rooms
update public.rtc_provider_settings set enabled = false where provider = 'daily';
-- usage (estimate = time people spend inside rooms)
select * from public.get_rtc_provider_minutes_status();
```

Shared server contract (all three prompts)
- POST https://<project>.supabase.co/functions/v1/get-room-token, headers: Authorization: Bearer <Supabase access token>, apikey, Content-Type: application/json. Body: {"room_id": "<voice_rooms.id full UUID>"} (never send a role, the server decides).
- 200: {"provider": "AGORA" | "ZEGO" | "DAILY", "room_id": "...", "token": "...", "app_id": "...", "role": "publisher" | "audience", "can_publish": true | false}. Daily: app_id is the Daily domain (maybe with or without ".daily.co").
- Errors: 401 unauthenticated, 403 user_suspended, 404 room_not_found, 410 room_ended, 503 all_providers_at_limit, 502 daily_*, 500 token_generation_failed.
- Tokens live 1 hour. Identity: Agora user account = Supabase user id (string), Zego userID = Supabase user id, Daily user_id = Supabase user id. Channel / room name = the room UUID.
- The room provider never changes during the life of a room.

---

## PROMPT 1 of 3: shared voice layer + Agora

```text
You are working on the Funkytalk Android app (Kotlin, Jetpack Compose, Supabase). Add REAL voice to voice rooms. This task builds the shared voice layer and the Agora implementation only. Do not add the Zego or Daily SDKs yet (leave clean TODO stubs that throw "not implemented").

## Shared layer
Create package voice with:
 - interface VoiceEngine { suspend fun join(cfg: VoiceJoinConfig); suspend fun leave(); fun setMicEnabled(enabled: Boolean); suspend fun upgradeToPublisher(newToken: String); fun downgradeToListener(); fun renewToken(newToken: String); val connectionState: StateFlow<VoiceConnectionState>; val speakingUserIds: StateFlow<Set<String>>; val remoteUserIds: StateFlow<Set<String>> } with VoiceConnectionState = Idle | Connecting | Connected | Reconnecting | Failed(reason).
 - data class VoiceJoinConfig(roomId, userId, userName, provider, appId, token, canPublish).
 - VoiceEngineFactory.create(provider: String) -> AgoraVoiceEngine for "AGORA"; "ZEGO" and "DAILY" return a stub that fails with Failed("provider not implemented yet").
 - RoomVoiceSession (one per open room, owned by the room ViewModel) that: calls get-room-token, creates the engine from the response, joins, exposes the states above, and handles: role change (my room_participants.role becomes 'speaker' -> fetch a NEW token and call upgradeToPublisher; becomes 'listener' -> downgradeToListener), token refresh (renew 5 minutes before expiry and on the SDK's token-will-expire callback by calling get-room-token again), reconnect (if Failed or disconnected for more than 20 s: fetch a fresh token and rejoin, max 3 tries, then show "Voice connection failed, tap to retry"), leaving on: user leaves, room ended (the existing onRoomEnded), kicked, ViewModel cleared, app process shutdown. Always release the engine so no audio minutes keep running.
 - Mic: RoomMicController (from the earlier mic-flicker fix) stays the single owner of mic on/off; it drives engine.setMicEnabled. Listeners (can_publish = false) never create a microphone track and do not need the RECORD_AUDIO permission. Ask for RECORD_AUDIO only when someone becomes a speaker/host, with a short explanation; if denied, stay listener and show a clear message.
 - Speaking indicator: engine.speakingUserIds drives a ring/animation around that user's avatar and seat (map SDK ids to Supabase user ids).
 - Audio output: speakerphone ON by default; follow wired / bluetooth headset when connected (BLUETOOTH_CONNECT permission on Android 12+, request only when needed); handle audio focus and phone calls (pause/duck and restore).
 - Background: keep the voice alive when the app goes to the background with a foreground service of type microphone (permissions FOREGROUND_SERVICE, FOREGROUND_SERVICE_MICROPHONE, POST_NOTIFICATIONS), with an ongoing notification "In voice room <title>" and a Leave action. Stop the service on leave / room end.
 - UI: a small connection banner ("Connecting...", "Reconnecting...", "Voice failed - Retry"), remove any remaining fake voice code, keep the existing seat/mic UI.
 - Logging: Log.d for every step with provider and error codes (join, token fetch, role change, renew, leave). NEVER log tokens.

## Agora implementation (AgoraVoiceEngine)
 - Use the Agora Voice SDK 4.x (audio only; pick the current audio-only Maven artifact, smaller than the full video SDK). Add the dependency and ProGuard keep rules.
 - The server builds tokens for a USER ACCOUNT (string), so join with joinChannelWithUserAccount(token, channelName = roomId, userAccount = userId, options), NOT with an integer uid. Handle the local-user-registered and user-info-updated callbacks to map Agora uids to user accounts (needed for the speaking indicator and remoteUserIds).
 - Channel profile LIVE_BROADCASTING. Publishers: clientRoleType BROADCASTER, publishMicrophoneTrack = true. Listeners: clientRoleType AUDIENCE, publishMicrophoneTrack = false, autoSubscribeAudio = true.
 - Upgrade (listener -> speaker): call renewToken(newToken) first, then updateChannelMediaOptions with clientRoleType BROADCASTER and publishMicrophoneTrack = true. Downgrade: AUDIENCE and publishMicrophoneTrack = false.
 - setMicEnabled -> muteLocalAudioStream(!enabled) (and/or enableLocalAudio) consistently.
 - enableAudioVolumeIndication(200, 3, true) -> speakingUserIds (threshold about 15).
 - Callbacks: onTokenPrivilegeWillExpire -> RoomVoiceSession renew; onConnectionStateChanged / onError -> map to VoiceConnectionState; onLeaveChannel.
 - leave(): leaveChannel() then RtcEngine.destroy() (release the engine completely).
 - Token note: the server now issues 1-hour tokens. If join fails with a token expired / invalid token error right away, log the exact error code and tell me, do not work around it.

## Test checklist (report each result, with the debug log lines)
 With the server set to Agora only: 1) host creates a room, hears nothing yet (alone). 2) a second phone joins as listener and hears the host. 3) listener requests a seat, host accepts, the listener is heard after the upgrade without rejoining. 4) mute / unmute works and the icon does not flicker. 5) host removes the speaker -> speaker can no longer be heard. 6) background the app for 2 minutes -> audio continues and the notification shows. 7) turn airplane mode on for 10 s and off -> it reconnects. 8) host ends the room -> everyone leaves the channel and the end screen shows. 9) after leaving, the minutes stop (check the Agora console usage).
Make sure the project compiles and list every file changed.
```

---

## PROMPT 2 of 3: Zego

Run this only after Prompt 1 passed all tests with Agora. Then switch the server to Zego only (SQL above) and create new rooms.

```text
Add the Zego implementation (ZegoVoiceEngine) to the existing voice layer. Do not change the shared interfaces unless a real problem appears (then explain why).
 - Use the ZEGOCLOUD Express SDK, audio-only package (pick the current audio-only artifact). Create the engine once with ZegoEngineProfile(appID = response.app_id as Long, scenario = a chat-room / standard-voice scenario suited to many listeners, application = app context).
 - Login: loginRoom(roomId, ZegoUser(userId, userName), ZegoRoomConfig().also { it.token = token }). The token is a Zego "Token04" generated by our server and it has NOT been verified against a real Zego SDK yet. If login fails with a token error, log the exact error code and the room/user ids (never the token) and tell me; do not work around it.
 - Publishers: after login startPublishingStream("s_" + userId) with the microphone on. Listeners never publish (their token has no publish privilege). Remote streams: in onRoomStreamUpdate ADD -> startPlayingStream(streamId); DELETE -> stopPlayingStream. Map stream ids back to user ids (strip "s_") for remoteUserIds and speakingUserIds.
 - Upgrade to publisher: renewToken(roomId, newToken) then startPublishingStream. Downgrade: stopPublishingStream (and muteMicrophone(true)).
 - setMicEnabled -> muteMicrophone(!enabled). Speaking indicator: startSoundLevelMonitor() with onCapturedSoundLevelUpdate (me) and onRemoteSoundLevelUpdate (others), threshold about 10.
 - Token refresh: onRoomTokenWillExpire -> RoomVoiceSession renew -> renewToken(roomId, token).
 - Map onRoomStateChanged / onNetworkQuality to VoiceConnectionState (reconnecting states included).
 - leave(): stop publishing / playing, logoutRoom(roomId), then ZegoExpressEngine.destroyEngine(null).
 - Register ZEGO in VoiceEngineFactory. Add ProGuard keep rules for the SDK.
 - Run the same 9 tests as for Agora and report each result.
Make sure the project compiles and list every file changed.
```

---

## PROMPT 3 of 3: Daily

Run this only after Zego passed. Then switch the server to Daily only and create new rooms.

```text
Add the Daily implementation (DailyVoiceEngine) to the existing voice layer.
 - Use the Daily Android client SDK (CallClient, audio only: camera always off, no video views). Pick the current artifact.
 - Room URL: "https://" + domain + "/" + roomId where domain is response.app_id; if it already ends with ".daily.co" use it as is, otherwise append ".daily.co". Join with the meeting token from the response.
 - Publishers: microphone enabled; listeners: microphone disabled. The meeting token for listeners sets permissions.canSend = false; this server-side setting is UNTESTED, so check that a listener really cannot send audio and tell me what you see.
 - Upgrade to publisher: Daily cannot swap tokens in a live call, so leave and rejoin the SAME room with the new token (keep the UI stable and keep the mic muted state), then enable the microphone. Downgrade: disable the microphone (and rejoin with a listener token if the permission is still enabled).
 - Active speaker / speakingUserIds from the SDK's participant audio-level or active-speaker events; map participants to user ids through the participant's user_id (set by the server to the Supabase user id).
 - Token lifetime 1 hour: the meeting token expires, so before expiry get a new token and rejoin quietly (brief reconnect banner only).
 - Map CallState (joining, joined, leaving, left) and network events to VoiceConnectionState.
 - leave(): leave the call and destroy the CallClient.
 - Register DAILY in VoiceEngineFactory. Run the same 9 tests and report each result.
Make sure the project compiles and list every file changed.
```

---

## Final rollout (after all three passed)
1. `update public.rtc_provider_settings set enabled = true;` and `update public.rtc_settings set strategy = 'balanced' where id = 1;`
2. Create 6 rooms in a row and check with `select provider, count(*) from public.voice_rooms where created_at > now() - interval '1 hour' group by provider;` that all three providers were used.
3. Keep an eye on `select * from public.get_rtc_provider_minutes_status();` and on the three provider dashboards (the server number counts the time people spend in rooms, so compare it with the real dashboards in the first week).

## Add to docs/BACKEND.md
- `rtc_provider_settings` (provider, enabled, priority, monthly_limit) and `rtc_settings` (strategy 'priority' | 'balanced'). `assign_rtc_provider_for_room` picks among ENABLED providers below their limit. A room keeps its provider.
- Staff: `admin_set_rtc_provider(p_provider, p_enabled)`, `admin_set_rtc_strategy(p_strategy)`; `get_rtc_provider_minutes_status()` (service role).
- `get-room-token` v22: Agora tokens last 1 hour (duration, not timestamp). Zego Token04 is hand-built and unverified; Daily `permissions.canSend` is unverified.
- Usage counters estimate time spent inside rooms (join to leave), not exact SDK minutes. Compare with provider dashboards.

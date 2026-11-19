# 2026-11-19 — P4: one active device, new-device email, hidden recents thumbnail

Why this is needed (found on 2026-11-19):
- The app logs in with only {"firebase_token": ...} (the QA log shows no "device" field) and never calls touch_device. So `user_devices` is EMPTY (0 rows), no new-device email can ever fire, and "log out other devices" has nothing to work with. The server side already existed; the app just never used it.
- Server is now ready (Edge Function verify-firebase-token v23 + migration 20261119b): body may carry `device` and `kind`; flag `single_device_login` = true in app_flags (set it to false to allow many devices).
- The chat screen shows in the Android recents list. The recents thumbnail must be hidden.

Server contract (live):
- POST /functions/v1/verify-firebase-token  body: { "firebase_token": "...", "device": { "device_id": "<stable id>", "name": "Samsung SM-M022G", "os": "Android 11", "app_version": "0.9.0-beta1" }, "kind": "login" | "refresh" }
  - kind "login" = the user just signed in by hand (email/password, Google). Everything else (cold start, hourly refresh) = "refresh" (default).
  - single_device_login=true: a login or a first-seen device signs every other device out and deletes their sessions.
  - A refresh from a signed-out device answers HTTP 403 {"code":"device_revoked"} and no session.
  - A brand-new device on an account that already had another device triggers the security email (server side, Brevo).
- RPC touch_device(p_device_id, p_device_name, p_os, p_app_version) -> {success, known, revoked}
- RPC get_my_devices(p_current_device_id) -> id, device_name, os, app_version, last_login_at, last_seen_at, revoked, is_current
- RPC revoke_device(p_device_row_id), revoke_other_devices(p_current_device_id)

```
WORK IN ONE ANSWER: list the files you need (max 5 lines), then IMMEDIATELY implement, show every changed file in full, and finish with a list of what you could not verify.

App: FunkyTalk (Kotlin, Jetpack Compose, Supabase, Firebase Auth). English text only.

A) Send the device with every call of the verify-firebase-token Edge Function
1. DeviceIdentity: a random UUID created once and stored in EncryptedSharedPreferences (not ANDROID_ID, not the advertising id). It must match ^[A-Za-z0-9._-]{8,64}$. Keep it across logouts, it resets only on reinstall. name = "${Build.MANUFACTURER} ${Build.MODEL}" (max 80 chars), os = "Android ${Build.VERSION.RELEASE}", app_version = BuildConfig.VERSION_NAME.
2. Add "device" and "kind" to the request body. kind = "login" only right after an interactive sign-in (email/password, Google, sign-up); kind = "refresh" for cold start, background refresh and every automatic retry. The body currently also contains a duplicate field "firebaseIdToken"; remove it if nothing on the server needs it (the function reads only "firebase_token").
3. Handle HTTP 403 with {"code":"device_revoked"}: stop retrying, sign out locally (Supabase session and Firebase), lock the chat vault (keep local encrypted data), go to the login screen and show once: "You were signed out because your account was opened on another device." Never loop on this error.
4. Also handle HTTP 503 {"code":"session_mint_failed"}: retry with backoff (2 s, 4 s, 8 s, max 3 tries), then show "Couldn't sign in. Try again."

B) Notice a remote sign-out within a minute
Call the RPC touch_device while the app is in the foreground every 60 s and on every resume. If the answer has revoked = true, run the same sign-out flow as A3. If any API call gets 401, refresh once through the Edge Function (kind "refresh"); a 403 device_revoked there also means sign-out.

C) Settings -> Logged-in devices
List from get_my_devices(current device id): name, OS, app version, "Last active <relative time>", a "This device" badge on the current one, revoked ones greyed with "Signed out". Buttons: "Log out this device" (revoke_device for a row, then if it is the current device run the sign-out flow) and "Log out all other devices" (revoke_other_devices). Confirmation dialog for both. Empty state: "No devices yet."

D) Hide the app from the recents screen (privacy for chats)
1. In MainActivity.onCreate: if Build.VERSION.SDK_INT >= 33 call setRecentsScreenshotEnabled(false) so the whole app shows a blank thumbnail in recents.
2. Create a Compose helper SecureWindow(enabled: Boolean = true) that sets WindowManager.LayoutParams.FLAG_SECURE on the Activity window with a DisposableEffect and clears it when it leaves composition. Use it on: every chat screen (chat list, 1:1 chat, group/room chat, support chat), the chat vault unlock/reset/backup screens, and Settings -> Logged-in devices. These screens then show black in recents and cannot be screenshotted. Moments, profiles and the store stay screenshot-able.
3. For Android < 33: in MainActivity add FLAG_SECURE in onPause and clear it in onResume (best effort for the other screens).
4. Do not break screen recording of voice rooms for the user; FLAG_SECURE only applies to the screens above.

E) Tests to run yourself and report (adb or by hand): (1) log in on phone A, then on phone B -> A is signed out within 60 s and shows the message; (2) B gets a security email? (only if the account already had a device; check the Edge Function logs for "login alert"); (3) open a DM, press Home, open recents -> thumbnail is black/blank; (4) screenshot inside a DM is blocked; (5) screenshot on Moments works.

Rules: no new dependencies; never log tokens; must compile. Do not change any server code.
```

### Owner checks (not for Gemini)
- Supabase secret BREVO_API_KEY must exist for the Edge Functions, and the sender `noreply@funkytalk.jo3.org` must be a verified sender/authenticated domain in Brevo, otherwise Brevo rejects the mail (look for "login alert: Brevo status" in Edge Function logs).
- To allow several devices again: `update public.app_flags set value='false'::jsonb, updated_at=now() where key='single_device_login';`
- E2EE note: chats live on the device; a second device would need its own vault restore. Single active device avoids two phones fighting over the same encrypted mailbox.

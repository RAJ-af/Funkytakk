# 2026-11-19 — release APK: flags (coin store off), user blocking, release build

Backend already live in Supabase (migration `20261119_app_flags_beta_signups_user_blocks.sql`, tested with SQL only, NOT from the app):
- `app_flags` table + RPC `get_app_flags()` (callable by anon and signed-in users). Keys: `coin_store_enabled` = false, `vip_enabled` = false, `min_supported_version_code` = 0.
- Blocking: table `user_blocks` (no direct access), RPCs `block_user(p_user_id)`, `unblock_user(p_user_id)`, `get_my_blocked_users()` -> (user_id, username, profile_name, avatar_url, blocked_at), `is_blocked_with(p_user_id)` (true if EITHER side blocked). Blocking also deletes follows in both directions. The server now refuses with error `user_blocked` in: `get_or_create_dm_conversation`, `send_gift`, `send_room_invite` (returns success=false, error=user_blocked).
- NOT yet enforced on the server: `send_room_gift`, `list_invitable_following`, moments/comments feeds, room chat. The app filters those on the client for now.
- `verify-firebase-token` v22: retries session creation; a failure now answers HTTP 503 with {"code":"session_mint_failed"} and a Retry-After header (before: 500).

Run P1, P2, P3 in this order, each as its own Gemini run. Release key facts: no Google Play developer account yet, so the beta APK is shared outside Play (Firebase App Distribution) and the coin store must stay off.

---

## P1 — server flags + hide the coin store and VIP

```
WORK IN ONE ANSWER: list the files you need (max 5 lines), then IMMEDIATELY implement, show every changed file in full, and finish with a list of what you could not verify.

App: FunkyTalk (Kotlin, Jetpack Compose, Supabase). Reply text in the app is English only.

Goal: the release build must not offer buying coins, and VIP must be off. Both are controlled by a server switch so we can turn them on later without a new APK.

1) Add AppFlagsRepository (one place): calls the Supabase RPC get_app_flags (no body; allowed with the anon key even before login) and returns a data class AppFlags(coinStoreEnabled: Boolean, vipEnabled: Boolean, minSupportedVersionCode: Int). FAIL CLOSED: if the call fails, times out (3 s) or a key is missing, coinStoreEnabled = false and vipEnabled = false, minSupportedVersionCode = 0. Cache the last good value in normal SharedPreferences and use it immediately on the next start, then refresh in the background (also refresh when the app returns to the foreground after more than 10 minutes). Expose it as a StateFlow so screens react.
2) Hide everything that sells or promotes coins when coinStoreEnabled is false: the "Coin store"/"Buy coins" screen and its menu entries, the "+" next to the wallet balance, any "Get coins" banner, deep links into the coin store. Do not delete the code, only gate it. The Decoration Store stays available (items are free for now). In places where the user runs out of coins (error insufficient_coins), show the text "You don't have enough coins." and nothing that suggests buying.
3) When vipEnabled is false: remove the VIP tag (host_is_vip) from room cards and remove the "Upload banner / VIP" tile; custom banner upload is not offered.
4) Update gate: if BuildConfig.VERSION_CODE < minSupportedVersionCode show a full-screen blocking "Update required" screen with a button that opens the app's download link constant UPDATE_URL (leave it as a clear TODO constant).
5) Add a debug-only way to flip the three flags locally for testing (a debug menu or BuildConfig.DEBUG override); it must not exist in release builds.

Rules: no new dependencies. Do not change purchase RPC calls. Do not log flag values with user data. Build must compile.
```

---

## P2 — user blocking in the app

```
WORK IN ONE ANSWER: list the files you need (max 5 lines), then IMMEDIATELY implement, show every changed file in full, and finish with a list of what you could not verify.

App: FunkyTalk (Kotlin, Jetpack Compose, Supabase). English text only. Google Play requires that users can block other users.

Backend (live): RPC block_user(p_user_id uuid), unblock_user(p_user_id uuid), get_my_blocked_users() returning user_id, username, profile_name, avatar_url, blocked_at, and is_blocked_with(p_user_id uuid) (true if either side blocked the other). The server refuses with error text "user_blocked" in DM creation, send_gift and send_room_invite.

Build:
1) BlockRepository + a small in-memory StateFlow<Set<String>> of blocked user ids, loaded after login and refreshed after every block/unblock (call get_my_blocked_users).
2) "Block user" / "Unblock" in: the user profile overflow menu, the 1:1 chat header menu, and the voice-room person sheet. Confirmation dialog: "Block <name>? They will not be able to message you, and you will not see their posts." After blocking, go back to the previous screen.
3) Settings -> Privacy -> "Blocked users": list with avatar, name and an Unblock button, empty state text "You haven't blocked anyone."
4) Client-side hiding while the server does not yet filter: hide blocked users' moments and comments in every feed/tab, hide them in Discover, the invite-friends list, followers/following lists and room participant lists, and ignore (do not show) their room chat messages. Their old encrypted DMs stay in the local database but the chat is removed from the chat list while blocked.
5) Disable the Wave, Send gift, Message and Invite buttons on a blocked user's profile (show "You blocked this user" with an Unblock button instead).
6) Map the server error "user_blocked" to the message "You can't interact with this user." everywhere it can appear (DM open, gift, invite).
7) Reporting stays separate: add "Block this user too" as an optional checkbox at the end of the report flow.

Rules: no new dependencies; no network call per list item (use the cached id set); must compile.
```

---

## P3 — release build configuration (APK shared outside Google Play)

```
WORK IN ONE ANSWER: list the files you need (max 5 lines), then IMMEDIATELY implement, show every changed file in full, and finish with a list of what you could not verify.

App: com.funkytalk.app. Goal: a small, safe release APK for beta testers, signed with our own key, no debug leftovers.

1) Signing: read the release keystore from a git-ignored keystore.properties (storeFile, storePassword, keyAlias, keyPassword). If the file is missing, the release build must fail with a clear message (not fall back to the debug key). Add keystore.properties and *.jks to .gitignore.
2) buildTypes.release: isDebuggable=false, isMinifyEnabled=true, isShrinkResources=true, proguard rules that keep what is needed (Supabase/Ktor/kotlinx.serialization models, Tink/E2EE classes, Agora, Firebase, Coil, Compose, Room if used). After building, scan the R8 output/warnings and fix missing -keep rules; list every rule you added and why.
3) ABIs: release builds only arm64-v8a and armeabi-v7a (drop x86/x86_64). Use abi splits to produce one APK per ABI plus a universal APK. Keep the debug filter as it is now.
4) Logging: no network body logging and no token/message text logging in release (HttpLoggingInterceptor and any custom logger only when BuildConfig.DEBUG, level HEADERS at most, redact Authorization, apikey, Cookie, Set-Cookie). Strip Log.d/Log.v in release with R8 rules. Remove the "TODO remove detail before release" detail from the Moments upload error toast (release shows only "Upload failed. Please try again.").
5) Manifest: android:enableOnBackInvokedCallback="true", android:allowBackup="false" (or keep the existing exclusion rules), usesCleartextTraffic=false, no exported component without a reason. Remove any debug-only activity/menu from release.
6) Versioning: versionCode and versionName from gradle.properties (APP_VERSION_CODE, APP_VERSION_NAME) so each shared build has a unique code. Start at versionCode 1, versionName "0.9.0-beta1".
7) Crashlytics: make sure the mapping file uploads for release builds and the user id is a hash, not an email.
8) Print a short checklist at the end: the exact Gradle commands (assembleRelease, bundleRelease), where the APKs land, and how to verify the signature with apksigner and that the APK is not debuggable (aapt dump badging | grep debuggable).
```

### Things only the owner can do for P3 (not Gemini)
1. Create the release key once and BACK IT UP (two places). Losing it means testers must uninstall to update:
   `keytool -genkeypair -v -keystore funkytalk-release.jks -alias funkytalk -keyalg RSA -keysize 2048 -validity 10000`
2. Get the SHA-1 and SHA-256: `keytool -list -v -keystore funkytalk-release.jks -alias funkytalk`
3. Add both fingerprints to the Firebase project (Project settings -> Android app -> Add fingerprint) and to the Android OAuth client in Google Cloud (Google sign-in and Drive backup break in the release APK without this). Download the new google-services.json.
4. After building, run `python3 scripts/apk_secret_scan.py app-release.apk` and fix every CRITICAL hit before sharing.
5. Share through Firebase App Distribution (testers by email), not by public link.

## Snippet for docs/BACKEND.md
- `app_flags` + `get_app_flags()`: server switches the app reads at start (coin_store_enabled, vip_enabled, min_supported_version_code). Change with: `update public.app_flags set value='true'::jsonb, updated_at=now() where key='coin_store_enabled';`
- Blocking: `user_blocks` (RPC only), `block_user`, `unblock_user`, `get_my_blocked_users`, `is_blocked_with`; enforced server-side in `get_or_create_dm_conversation`, `send_gift`, `send_room_invite`.
- Beta signups: Edge Function `beta-signup` (public, no JWT) -> table `beta_signups`; rate limit 5/hour per hashed IP in `beta_signup_attempts`. Optional secrets: BREVO_API_KEY (welcome mail), BETA_ALLOWED_ORIGINS (comma list of site origins), BETA_IP_SALT.
- `verify-firebase-token` v22: retry-safe session minting (503 + code session_mint_failed instead of 500).

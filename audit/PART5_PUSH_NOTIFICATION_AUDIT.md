# FUNKYTALK PUSH NOTIFICATION
# FINAL PRODUCTION AUDIT

---

## Executive Summary

This Final Production Security & End-to-End (E2E) Audit synthesizes and verifies all findings from Parts 1–4 against the current FunkyTalk Android codebase (`com.funkytalk.app`, v2.0 / versionCode 11).

- **Client Token Acquisition & Lifecycle**: Implemented across cold start (`FunkyTalkApplication.onCreate` → `ProcessLifecycleOwner.onStart`), foreground resume (`MainActivity` `ON_RESUME`), login/session restoration (`AuthViewModel` + `MainActivity` `LaunchedEffect(authState)`), token rotation (`FunkyFirebaseMessagingService.onNewToken`), and manual debug re-registration (`forceReregisterPush`), backed by a 3-attempt exponential backoff loop (`1.5s`, `3.0s`).
- **Critical & High-Severity Blockers Identified**:
  1. **Unverified Server-Side Database / RLS / RPC Definitions (`CRITICAL`)**: The SQL schema for `public.push_tokens`, the RPC bodies for `register_push_token` and `unregister_push_token`, RLS policies, and role grants are not checked into `supabase_schema.sql` or `supabase/migrations/`.
  2. **Logout / Re-Login Concurrency & Expired-JWT Unregister Bugs (`CRITICAL`)**: `unregisterCurrentToken` does not acquire `registrationMutex`, and `AuthViewModel.logout()` signs out of Firebase and clears `SupabaseSessionManager` *before* the async unregister coroutine runs—causing `unregister_push_token` to fail if the 1-hour Supabase JWT is expired and allowing User A's delayed `deleteToken()` to destroy User B's token during a fast account switch.
  3. **Broken Notification Tap Navigation (`CRITICAL`)**: Tapping a decrypted DM notification posted by `SystemNotificationHelper` routes `MainActivity` to `currentScreen = "direct_chat"`, which does not exist in `MainActivity`'s `when (screen)` router, rendering a blank screen. Tapping a push notification posted by `PushNotificationManager` ignores `from_push`, `conversation_id`, and `group_id`.
  4. **E2EE Plaintext Message Leakage to Logcat & Duplicate Notifications (`HIGH`)**: When `onMessageReceived` drains pending DMs while the vault is unlocked in memory, `SystemNotificationHelper.kt:124` logs the decrypted E2EE plaintext message to Android Logcat (`Log.d`) and posts a second duplicate notification alongside `PushNotificationManager.showPushNotification`.

---

## E2E Architecture

### Verified End-to-End Execution Trace

```text
1. LOGIN & SESSION ESTABLISHMENT
   ├── User authenticates via Firebase Auth (Email/Password or Google CredentialManager)
   ├── AuthViewModel exchanges Firebase ID Token via Supabase Edge Function:
   │     POST /functions/v1/verify-firebase-token -> mints 1-hour Supabase JWT
   ├── SupabaseSessionManager.setSession(context, accessToken, refreshToken, userId)
   └── AuthViewModel emits AuthState.Success & calls PushNotificationManager.onUserLoggedIn(context)

2. FCM TOKEN ACQUISITION & REGISTRATION
   ├── Triggered by: ProcessLifecycleOwner.onStart, MainActivity ON_RESUME,
   │   LaunchedEffect(authState), FunkyFirebaseMessagingService.onNewToken, or Debug Re-register
   ├── PushNotificationManager.registerTokenWithRetry(appCtx, explicitToken) [registrationMutex]
   │     ├── Ensures FirebaseApp initialized with gcmSenderId = "1093102729820"
   │     ├── Awaits FirebaseMessaging.getInstance().token.await()?.trim()
   │     ├── Calls SupabaseSessionManager.ensureActiveSession(appCtx)
   │     ├── Verifies non-blank Bearer JWT != BuildConfig.SUPABASE_ANON_KEY
   │     └── Calls Retrofit: POST /rest/v1/rpc/register_push_token
   │           Headers: apikey = SUPABASE_ANON_KEY, Authorization = "Bearer <user_jwt>"
   │           Body:    {"p_token": "<fcm_token>", "p_platform": "android"}
   └── Retries on failure up to 3 attempts (0ms -> 1500ms -> 3000ms)

3. SUPABASE STORAGE & SERVER NOTIFICATION SENDER
   ├── Supabase RPC writes row to public.push_tokens [SQL NOT IN REPO]
   ├── Sender inserts encrypted message into public.dm_messages or public.group_message_recipients
   └── Remote Backend / Trigger dispatches FCM payload via FCM HTTP v1 API [NOT IN REPO]
         (Verified: Zero Firebase Admin SDK service-account private keys in Android APK/repo)

4. ANDROID FCM DELIVERY & NOTIFICATION DISPLAY
   ├── FunkyFirebaseMessagingService.onMessageReceived(remoteMessage)
   │     ├── Ensures NotificationChannel("messages", IMPORTANCE_HIGH) exists
   │     ├── Calls PushNotificationManager.showPushNotification(context, title, body, data)
   │     │     ├── Checks VaultSecurityManager.isVaultLocked() (masks title/body if locked)
   │     │     └── Posts Notification #1 on channel "messages" (id = conversation_id/group_id hash)
   │     └── Launches background inbox drain:
   │           ├── EncryptedChatRepository.drainPendingDmMessages(applicationContext)
   │           │     └── Decrypts DM, saves to SQLCipher Room DB, deletes server inbox row,
   │           │         and calls SystemNotificationHelper.showSystemNotification() [Posts Notification #2!]
   │           └── GroupChatRepository.drainPendingGroupMessages(applicationContext)
   │                 └── Decrypts group msg, saves to Room DB, deletes group_message_recipients row

5. LOGOUT, UNREGISTER & TOKEN DELETION
   ├── AuthViewModel.logout(context)
   │     ├── Captures capturedBearerToken synchronously from SupabaseSessionManager
   │     ├── Launches async PushNotificationManager.unregisterCurrentToken(appCtx, capturedBearerToken)
   │     ├── Calls firebaseAuth?.signOut() & SupabaseSessionManager.clearSession(context) immediately
   └── PushNotificationManager.unregisterCurrentToken (on Dispatchers.IO, NO registrationMutex):
         ├── Awaits FirebaseMessaging.getInstance().token.await() ?: getSavedToken(appCtx)
         ├── Calls POST /rest/v1/rpc/unregister_push_token {"p_token": "<fcm_token>"}
         ├── Calls FirebaseMessaging.getInstance().deleteToken().await()
         └── Resets PushDebugState & SharedPreferences (isRegistered = false, lastToken = null)
```

---

## Security

### 1. Client-to-Supabase Authentication & RPC Security
- **Client Cannot Spoof `user_id` (`VERIFIED`)**: `RegisterPushTokenRequest` (`p_token`, `p_platform`) and `PushTokenRequest` (`p_token`) in `SupabaseApi.kt:1789–1797` do not accept a `user_id` parameter. User identity is derived strictly from the `Authorization: Bearer <jwt>` header.
- **Anonymous Key Blocked on Client (`VERIFIED`)**: Both `registerTokenWithRetry` (`PushNotificationManager.kt:291–301`) and `unregisterCurrentToken` (`PushNotificationManager.kt:405–418`) explicitly filter out `BuildConfig.SUPABASE_ANON_KEY`.
- **Database RLS, RPC `auth.uid()` Enforcement & Grants (`NOT VERIFIED`)**: No SQL migration for `public.push_tokens`, `register_push_token`, or `unregister_push_token` exists in `supabase_schema.sql` or `supabase/migrations/`.

### 2. Server-Side FCM Security & Service-Account Key Audit
- **Where FCM Credentials Live**:
  - Push dispatch is performed server-side (outside the Android client). The Android client never calls `fcm.googleapis.com` and contains no FCM sender code.
- **Service-Account Private Key Search (`VERIFIED — SAFE`)**:
  - Full repository search (`grep -rnI "BEGIN PRIVATE KEY\|service_account\|private_key_id\|fcm.googleapis.com\|firebase-adminsdk"`) confirmed **zero** Firebase Admin SDK service-account JSON files or private keys exist anywhere in the source tree, Gradle configuration, or APK assets.
  - `app/build.gradle.kts:90–94` explicitly excludes server-side secrets (`GEMINI_API_KEY`, `BREVO_API_KEY`, `BREVO_TEMPLATE_ID`) from `BuildConfig`.

### 3. Account Isolation & Logout Security
- **Unsynchronized Logout vs. Registration (`CRITICAL`)**: `unregisterCurrentToken` (`PushNotificationManager.kt:396`) does not acquire `registrationMutex` (`L83`). An in-flight `registerTokenWithRetry` for User A can complete *after* `unregisterCurrentToken`, or User A's delayed `deleteToken().await()` (`L440`) can execute *after* User B has logged in and registered.
- **Session Cleared Before Unregister RPC (`CRITICAL`)**: `AuthViewModel.logout` (`L2205–2209`) signs out of Firebase and clears `SupabaseSessionManager` synchronously before `unregisterCurrentToken` runs on `Dispatchers.IO`. If User A's 1-hour Supabase JWT is expired at logout time, `capturedBearerToken` contains the expired JWT (`L2190`), cannot be refreshed, and `unregister_push_token` fails with `HTTP 401`.
- **Session Resurrection Race (`HIGH`)**: If `SupabaseSessionManager.ensureActiveSession` (`L213–239`) is in flight awaiting `verifyFirebaseToken` when `logout()` clears the session, its completion calls `setSession(...)` (`L236`), writing User A's session back into `SharedPreferences` after logout.

---

## Reliability

- **Multi-Trigger Redundancy**: Registration is triggered on cold start (`ProcessLifecycleOwner.onStart`), foreground resume (`MainActivity` `ON_RESUME`), login (`AuthViewModel.registerAndStartSession` + `MainActivity` `LaunchedEffect(authState)`), permission grant (`MainActivity.kt:49–51`), and `onNewToken` (`FunkyFirebaseMessagingService.kt:22`).
- **Bounded Retry with Exponential Backoff**: `registerTokenWithRetry` (`PushNotificationManager.kt:255–350`) executes up to `MAX_RETRY_ATTEMPTS = 3` attempts with `1500ms` and `3000ms` backoff delays under `registrationMutex.withLock`, properly rethrowing `CancellationException` (`L335`).
- **HTTP 401 Retry Defect**: When `registerPushToken` returns `HTTP 401`, `registerTokenWithRetry` calls `ensureActiveSession` on the next attempt, which returns `true` without refreshing if the cached token has not passed its local `expiresAtMillis` timestamp (or if `expiresAtMillis == 0L`).
- **Notification Tap Routing Reliability Failure**:
  - `SystemNotificationHelper.showSystemNotification` (`L97`) sets `putExtra("navigate_to", "direct_chat")`, which sets `currentScreen = "direct_chat"` in `MainActivity.kt:80`—an unhandled screen state that renders a blank screen.
  - `PushNotificationManager.showPushNotification` (`L474–478`) sets `from_push = true` and copies `data` extras (`conversation_id`, `group_id`), which `MainActivity.kt` never reads.

---

## Privacy

| Audit Check | Status | Evidence & Details |
| :--- | :---: | :--- |
| **FCM Token Logging in App Code** | **SAFE (Redacted)** | `PushNotificationManager.kt:322, 427` and `FunkyFirebaseMessagingService.kt:24` log only `token.takeLast(6)`. |
| **FCM Token Logging in HTTP Interceptor** | **EXPOSED IN DEBUG ONLY** | `SupabaseClient.kt:16–18` sets `HttpLoggingInterceptor.Level.BODY` when `BuildConfig.DEBUG == true` (`Level.NONE` in release), logging full JSON request bodies (`p_token`) and `Authorization` JWT headers in debug Logcat. |
| **Private Message Content in Notification & Logs** | **FAIL (HIGH)** | 1. `SystemNotificationHelper.kt:124` logs `Log.d(TAG, "System Notification posted successfully: $title - $message")` where `$message` is the **decrypted E2EE plaintext DM** (`EncryptedChatRepository.kt:736–741`)!<br>2. Neither `PushNotificationManager.showPushNotification` nor `SystemNotificationHelper.showSystemNotification` sets `NotificationCompat.VISIBILITY_PRIVATE`, exposing decrypted DM previews on the lock screen when the vault is unlocked in memory. |
| **Tokens in URLs** | **SAFE** | Sent exclusively in HTTP `POST` `@Body` JSON (`SupabaseApi.kt:1065–1077`), never in URL paths or query parameters. |
| **Tokens in Analytics** | **SAFE** | No analytics SDK or telemetry pipeline collects FCM tokens. |
| **Tokens in Crash Logs / Exceptions** | **SAFE** | Exception handlers (`PushNotificationManager.kt:338–344, 382–388, 432–434`) log only `e.javaClass.simpleName` and `e.message`, never the FCM token. |
| **Sensitive Notification Payloads** | **PARTIAL** | `PushNotificationManager.showPushNotification` (`L470–472`) masks `title` and `body` when `VaultSecurityManager.isVaultLocked() == true`, but `FunkyFirebaseMessagingService.kt:33` logs `remoteMessage.data` to `Log.d`, and background FCM `notification` payloads (if sent by server) bypass `onMessageReceived`. |

---

## Test Matrix

| # | Test | Expected | Actual | PASS/FAIL | Evidence |
| :- | :--- | :--- | :--- | :---: | :--- |
| **1** | **Register while logged out** | Blocked; must not call RPC with anon key or null user | `isUserLoggedIn` returns `false` on `onStart`; if `registerTokenWithRetry` runs (e.g. `onNewToken`), `authToken` filters out `SUPABASE_ANON_KEY` and aborts before RPC | **PASS** | `PushNotificationManager.kt:115, 291–301` |
| **2** | **Register after login** | Immediately awaits FCM token and calls `register_push_token(p_token, 'android')` | `AuthViewModel` sets session then triggers `PushNotificationManager.onUserLoggedIn` (`L1882`) and `MainActivity` `LaunchedEffect(authState)` calls `registerCurrentToken` (`L184`) | **PASS** | `AuthViewModel.kt:1645, 1882, 2109`, `MainActivity.kt:184` |
| **3** | **Register after restart** | Cold start while logged in awaits FCM token and registers with Supabase | `FunkyTalkApplication.onCreate` → `PushNotificationManager.initialize` → `ProcessLifecycleOwner.onStart` (`L111`) + `MainActivity` `ON_RESUME` (`L95`) + `LaunchedEffect(authState)` (`L184`) | **PASS** | `FunkyTalkApplication.kt:24`, `PushNotificationManager.kt:103, 111–120`, `MainActivity.kt:94–96, 184` |
| **4** | **Register after foreground** | Returning to foreground while logged in registers token | `ProcessLifecycleOwner.onStart` (`L111–120`) and `MainActivity` `ON_RESUME` (`L92–97`) both invoke registration | **PASS** | `PushNotificationManager.kt:111–120`, `MainActivity.kt:92–97` |
| **5** | **Token rotation** | `onNewToken` registers new token `Token B` and cleans up `Token A` | `FunkyFirebaseMessagingService.onNewToken` registers `Token B` via `registerTokenWithRetry`, but overwrites `lastToken` at `L250` without unregistering `Token A` on client (server cleanup unverified) | **FAIL** | `FunkyFirebaseMessagingService.kt:22–29`, `PushNotificationManager.kt:246–253` |
| **6** | **Duplicate registration** | Concurrent triggers do not race; repeated calls are idempotent | Client serializes concurrent calls via `registrationMutex.withLock` (`L258`); however, Cold Start fires 3 back-to-back RPC calls (`onStart`, `ON_RESUME`, `LaunchedEffect`), and server upsert SQL is unverified | **FAIL** | `PushNotificationManager.kt:83, 118, 258`, `MainActivity.kt:95, 184` |
| **7** | **User A → User B** | User A token removed; User B gets isolated token; no cross-user state race | `unregisterCurrentToken` lacks `registrationMutex`; if User B logs in while User A's logout coroutine is in flight, User A's `deleteToken().await()` (`L440`) and `finally` (`L450`) destroy User B's token and debug state. Also if User A's JWT was expired at logout, `unregister_push_token` fails (401) | **FAIL** | `PushNotificationManager.kt:258, 396–458`, `AuthViewModel.kt:2188–2209` |
| **8** | **Own token deletion** | Logout calls `unregister_push_token(p_token)` and `deleteToken()` | Works when JWT is valid (`L420–441`), **but** fails with 401 if User A's 1h Supabase JWT is expired at logout because `firebaseAuth.signOut()` (`L2205`) runs before `unregisterCurrentToken` | **FAIL** | `AuthViewModel.kt:2188–2209`, `PushNotificationManager.kt:405–444` |
| **9** | **Delete another user's token** | Server must enforce `WHERE token = p_token AND user_id = auth.uid()` | Client sends only `p_token` + caller's JWT (`SupabaseApi.kt:1072`), but server SQL for `unregister_push_token` is not in the repository | **FAIL** *(Not Verified)* | `SupabaseApi.kt:1072–1077`, `audit/PART3_PUSH_NOTIFICATION_AUDIT.md` |
| **10** | **Register another user's token** | Client cannot pass `user_id`; server binds `auth.uid()` and blocks `SELECT` on `push_tokens` | Client `RegisterPushTokenRequest` omits `user_id` (`SupabaseApi.kt:1789`), but server RLS and `register_push_token` SQL are not in the repository | **FAIL** *(Not Verified)* | `SupabaseApi.kt:1789–1792`, `audit/PART3_PUSH_NOTIFICATION_AUDIT.md` |
| **11** | **Logout during registration** | Logout cancels or serializes with in-flight registration so logged-out user is never registered | `unregisterCurrentToken` does not lock `registrationMutex` or cancel `scope`; in-flight `ensureActiveSession` (`SupabaseSessionManager.kt:236`) or `registerPushToken` (`L304`) can complete *after* `clearSession` | **FAIL** | `PushNotificationManager.kt:82–83, 258, 290, 396`, `SupabaseSessionManager.kt:222–239` |
| **12** | **`deleteToken` failure** | Caught, logged, and does not crash app | `try { FirebaseMessaging.getInstance().deleteToken().await() } catch (e: Exception) { Log.e(...) }` in both `forceReregisterPush` (`L361–366`) and `unregisterCurrentToken` (`L439–444`) | **PASS** | `PushNotificationManager.kt:361–366, 439–444` |
| **13** | **Supabase offline** | Logs error via `Log.e`, retries with backoff (max 3 tries), updates debug state, no crash | Caught at `L337–345`, logged with `Log.e`, retried at `1.5s` and `3.0s` (`MAX_RETRY_ATTEMPTS = 3`), sets `isRegistered = false` and `lastError` | **PASS** | `PushNotificationManager.kt:80, 262–267, 337–349` |
| **14** | **HTTP 401** | Refreshes expired/invalid JWT and retries registration | `ensureActiveSession` (`L290`) does **not** force-refresh when a cached token is rejected with HTTP 401; Attempts 2 and 3 resend the same rejected JWT | **FAIL** | `PushNotificationManager.kt:290–334`, `SupabaseSessionManager.kt:127–135` |
| **15** | **HTTP 403** | Logs exact HTTP 403 error and stops without infinite loop | Logs HTTP code, message, and error body (`L325–327`), bounded to 3 attempts (`L262`) | **PASS** | `PushNotificationManager.kt:262, 325–334` |
| **16** | **HTTP 429** | Logs HTTP 429 and retries with exponential backoff (max 3 tries) | Logs HTTP 429 (`L325–327`) and retries after `1500ms` and `3000ms` (`L264`) | **PASS** | `PushNotificationManager.kt:262–267, 325–334` |
| **17** | **HTTP 5xx** | Logs HTTP 5xx error body and retries with exponential backoff (max 3 tries) | Logs HTTP 5xx status + error body (`L325–327`) and retries after `1500ms` and `3000ms` (`L264`) | **PASS** | `PushNotificationManager.kt:262–267, 325–334` |
| **18** | **Notification permission denied** | App does not crash; token still registers; notifications safely suppressed | Token registration still succeeds; `SystemNotificationHelper` checks `hasNotificationPermission` (`L87`), and platform `NotificationManager.notify` in `PushNotificationManager` (`L499`) does not throw on Android 13+ | **PASS** | `MainActivity.kt:44–58`, `SystemNotificationHelper.kt:66–75, 87–90`, `PushNotificationManager.kt:497–499` |
| **19** | **Notification permission granted** | Channel `"messages"` (`IMPORTANCE_HIGH`) active; grant callback triggers registration | Channel `"messages"` created with `IMPORTANCE_HIGH` (`L217–220`); grant callback in `MainActivity.kt:49–51` triggers `PushNotificationManager.onUserLoggedIn` | **PASS** | `MainActivity.kt:44–52`, `PushNotificationManager.kt:213–231` |
| **20** | **Process killed** | Background FCM data push wakes service, displays notification, and drains inbox | `FunkyFirebaseMessagingService` (`MESSAGING_EVENT`) wakes process, runs `Application.onCreate` → creates `"messages"` channel, posts notification, and drains DM/group inboxes (`L31–57`) | **PASS** | `AndroidManifest.xml:91–101`, `FunkyFirebaseMessagingService.kt:31–57` |
| **21** | **Device reboot** | Push delivery and token registration remain reliable after reboot | Google Play Services maintains FCM registration across reboots; next app launch re-verifies token via `ProcessLifecycleOwner.onStart` (`L111–120`) | **PASS** | `PushNotificationManager.kt:103, 111–120` |
| **22** | **Full token logging search** | Full FCM token is never logged in production logs | App logs only `token.takeLast(6)` (`PushNotificationManager.kt:322, 427`, `FunkyFirebaseMessagingService.kt:24`). However, `SupabaseClient.kt:16` enables `HttpLoggingInterceptor.Level.BODY` in `BuildConfig.DEBUG` builds, which logs full request bodies (`p_token`) in debug Logcat | **FAIL** *(Debug Logcat)* | `SupabaseClient.kt:16–18`, `PushNotificationManager.kt:322, 427` |
| **23** | **Service-account key search** | Zero Firebase service-account private keys in Android repo/APK | Repository-wide `grep` for `BEGIN PRIVATE KEY`, `service_account`, `private_key_id`, `firebase-adminsdk` returned 0 matches | **PASS** | Verified via repo-wide search (`0 matches`) |

---

## Findings

### [CRITICAL] FINDING 1: Missing Version-Controlled SQL Migration for `push_tokens`, `register_push_token`, `unregister_push_token`, and RLS Policies
- **File**: `supabase_schema.sql`, `supabase/migrations/`
- **Function**: `public.register_push_token`, `public.unregister_push_token`, `public.push_tokens` RLS policies
- **Evidence**: Repository contains DDL for `profiles`, `voice_rooms`, `dm_messages`, etc., in `supabase_schema.sql` and 3 voice room migrations in `supabase/migrations/`, but zero SQL definitions for `push_tokens` or its RPCs.
- **Impact**: Cannot verify from project sources that RLS blocks cross-user `SELECT/INSERT/UPDATE/DELETE` on `public.push_tokens`, that `unregister_push_token` filters by `WHERE user_id = auth.uid()`, or that `register_push_token` upserts on `UNIQUE (token)` to reassign device ownership when users switch accounts.
- **Reproduction**: Search repository for `CREATE TABLE.*push_tokens` or `register_push_token` in `*.sql`.
- **Fix**: Check in a versioned migration `supabase/migrations/<timestamp>_push_tokens_and_rpcs.sql` defining `public.push_tokens` (`UNIQUE (token)`), enabling RLS, revoking `anon` access, and defining `SECURITY DEFINER` RPCs with `SET search_path = public` that strictly enforce `auth.uid()`.
- **Release Blocker**: **YES**

### [CRITICAL] FINDING 2: `unregisterCurrentToken` Lacks `registrationMutex` and Runs After Session/Firebase Sign-Out (Logout & User-Switch Race)
- **File**: `app/src/main/java/com/example/service/PushNotificationManager.kt` (`L258`, `L396–459`), `app/src/main/java/com/example/AuthViewModel.kt` (`L2188–2209`), `app/src/main/java/com/example/SupabaseSessionManager.kt` (`L213–239`)
- **Function**: `PushNotificationManager.unregisterCurrentToken()`, `AuthViewModel.logout()`, `SupabaseSessionManager.ensureActiveSession()`
- **Evidence**:
  1. `registerTokenWithRetry` locks `registrationMutex` (`L258`), whereas `unregisterCurrentToken` (`L396`) does not.
  2. `AuthViewModel.logout` (`L2197–2209`) launches `unregisterCurrentToken` asynchronously on `Dispatchers.IO` and immediately calls `firebaseAuth?.signOut()` (`L2205`) and `SupabaseSessionManager.clearSession(context)` (`L2209`) on the main thread.
  3. If User A's 1-hour Supabase JWT is expired at logout time, `getAuthenticatedBearerToken` returns `null`, `capturedBearerToken` falls back to the expired JWT (`L2190`), and `unregisterCurrentToken` cannot refresh it because Firebase Auth is already signed out.
- **Impact**:
  - User A's push token remains active in `public.push_tokens` if User A logs out after 1 hour of inactivity or while a registration coroutine is still in flight.
  - If User B logs in within 1–3 seconds of User A logging out, User A's background `deleteToken().await()` (`L440`) and `finally` state reset (`L450`) execute *after* User B's registration, deleting User B's FCM token.
- **Reproduction**:
  - *Scenario A*: Wait for Supabase JWT to expire (>1h), tap Logout → `unregister_push_token` returns HTTP 401.
  - *Scenario B*: Log out of User A and immediately log into User B while `unregisterCurrentToken` is awaiting network I/O.
- **Fix**:
  1. Wrap `unregisterCurrentToken` (and `forceReregisterPush`) inside `registrationMutex.withLock`.
  2. In `AuthViewModel.logout()`, refresh the token if expired *before* calling `firebaseAuth?.signOut()`, or await `unregisterCurrentToken` inside the mutex before clearing `FirebaseAuth` and `SupabaseSessionManager`.
  3. In `SupabaseSessionManager.ensureActiveSession()`, verify `FirebaseAuth.getInstance().currentUser?.uid == firebaseUser.uid` before calling `setSession()`.
- **Release Blocker**: **YES**

### [CRITICAL] FINDING 3: Tapping a Decrypted DM Notification Routes `MainActivity` to Unhandled `"direct_chat"` Screen (Blank Screen)
- **File**: `app/src/main/java/com/example/util/SystemNotificationHelper.kt` (`L95–100`), `app/src/main/java/com/example/MainActivity.kt` (`L38–40, 66–83, 232–448, 489–491`), `app/src/main/java/com/example/service/PushNotificationManager.kt` (`L474–478`)
- **Function**: `SystemNotificationHelper.showSystemNotification()`, `MainActivity.onCreate()`, `MainActivity.onNewIntent()`, `PushNotificationManager.showPushNotification()`
- **Evidence**:
  - `SystemNotificationHelper.kt:97` sets `putExtra("navigate_to", "direct_chat")` when `partnerUserId` is present.
  - `MainActivity.kt:80` sets `currentScreen = target` (`"direct_chat"`), which has no matching case in `when (screen)` (`MainActivity.kt:232–448`).
  - `PushNotificationManager.kt:476–477` sets `from_push = true` and copies FCM `data` extras (`conversation_id`, `group_id`), which `MainActivity.kt` never reads.
- **Impact**: Tapping a DM notification posted by `SystemNotificationHelper` leaves the user stranded on a blank cream-colored background screen (`Color(0xFFFAFAEE)`). Tapping a push notification posted by `PushNotificationManager` fails to navigate to the target DM or group chat.
- **Reproduction**: Receive a DM while logged in (triggering `SystemNotificationHelper.showSystemNotification`), tap the notification in the system tray → `MainActivity` transitions to `"direct_chat"` and renders a blank screen.
- **Fix**: Route `"direct_chat"` (and `from_push` / `conversation_id` / `group_id` intent extras) in `MainActivity.kt` to `currentScreen = "home"` and pass the target `partnerUserId` / `conversationId` / `groupId` to `HomeScreen` to open the corresponding chat dialog.
- **Release Blocker**: **YES**

### [HIGH] FINDING 4: Decrypted E2EE Plaintext Logged to Logcat & Duplicate Notifications Posted on Push Arrival
- **File**: `app/src/main/java/com/example/util/SystemNotificationHelper.kt` (`L82, 124`), `app/src/main/java/com/example/repository/EncryptedChatRepository.kt` (`L728–745`), `app/src/main/java/com/example/service/FunkyFirebaseMessagingService.kt` (`L41–55`)
- **Function**: `SystemNotificationHelper.showSystemNotification()`, `EncryptedChatRepository.saveIncomingMessageToLocalDb()`, `FunkyFirebaseMessagingService.onMessageReceived()`
- **Evidence**:
  1. `SystemNotificationHelper.kt:124` executes `Log.d(TAG, "System Notification posted successfully: $title - $message")`, where `$message` is the decrypted `plaintext` from `EncryptedChatRepository.kt:736–741`.
  2. When an FCM push arrives while the vault is unlocked in memory, `onMessageReceived` posts a notification via `PushNotificationManager.showPushNotification` (`L41`), and then calls `drainPendingDmMessages` (`L51`), which calls `saveIncomingMessageToLocalDb` → `SystemNotificationHelper.showSystemNotification` (`L738`), posting a **second** notification with a different `notificationId`.
  3. Neither notification sets `NotificationCompat.VISIBILITY_PRIVATE`.
- **Impact**: Breaks E2EE confidentiality on the device by writing decrypted message bodies to system Logcat, clutters the notification shade with duplicate notifications for every incoming DM push, and exposes plaintext previews on the lock screen.
- **Reproduction**: Send a DM to a logged-in device with an unlocked vault while monitoring `adb logcat -s SystemNotificationHelper`.
- **Fix**: Remove `$title - $message` from `Log.d` in `SystemNotificationHelper.kt:124`, consolidate notification posting so `onMessageReceived` and `drainPendingDmMessages` update the same deterministic `notificationId` (or suppress the generic push banner when the decrypted notification is posted), suppress notifications for the currently open chat, and set `NotificationCompat.VISIBILITY_PRIVATE`.
- **Release Blocker**: **YES**

### [MEDIUM] FINDING 5: `registerTokenWithRetry` Does Not Force-Refresh Rejected JWT on HTTP 401
- **File**: `app/src/main/java/com/example/service/PushNotificationManager.kt` (`L290–334`), `app/src/main/java/com/example/SupabaseSessionManager.kt` (`L127–135`)
- **Function**: `PushNotificationManager.registerTokenWithRetry()`, `SupabaseSessionManager.ensureActiveSession()`
- **Evidence**: On Attempt 1, if `registerPushToken` returns `HTTP 401`, Attempt 2 calls `SupabaseSessionManager.ensureActiveSession(appCtx)` (`L290`), which returns `true` immediately without refreshing if the cached token has not passed its local `expiresAtMillis` (or if `expiresAtMillis == 0L`).
- **Impact**: All 3 retry attempts send the same rejected JWT and fail.
- **Reproduction**: Invalidate the Supabase JWT secret or have clock skew where local `expiresAtMillis` is in the future while server rejects with 401.
- **Fix**: When `resp.code() == 401` in `registerTokenWithRetry`, call `SupabaseSessionManager.refreshSupabaseJwtFromFirebase(appCtx)` directly before the next retry attempt.
- **Release Blocker**: **NO**

### [MEDIUM] FINDING 6: Old FCM Token Not Unregistered on `onNewToken` Rotation or `forceReregisterPush`
- **File**: `app/src/main/java/com/example/service/PushNotificationManager.kt` (`L246–253, 356–378`)
- **Function**: `PushNotificationManager.registerToken()`, `PushNotificationManager.forceReregisterPush()`
- **Evidence**: `registerToken(context, token)` immediately overwrites `lastToken = token` (`L250`), and `forceReregisterPush` calls `deleteToken().await()` (`L362`) without first calling `unregister_push_token` for the previous token.
- **Impact**: Leaves stale token rows in `public.push_tokens` on the server until pruned by the backend.
- **Reproduction**: Click "Re-register push" in Settings > About (Debug) twice; observe `register_push_token` called for each new token without `unregister_push_token` for the replaced token.
- **Fix**: Capture `val previousToken = getSavedToken(appCtx)` before rotation/re-registration and call `unregisterPushToken(previousToken)` if it differs from the new token.
- **Release Blocker**: **NO**

### [LOW] FINDING 7: `HttpLoggingInterceptor.Level.BODY` Logs Full `p_token` and `Authorization` JWT in Debug Builds
- **File**: `app/src/main/java/com/example/SupabaseClient.kt` (`L16–18`)
- **Function**: `SupabaseClient.okHttpClient`
- **Evidence**: `level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE` without `redactHeader("Authorization")` or `redactHeader("apikey")`.
- **Impact**: Full FCM token (`p_token`) and Supabase Bearer JWT are printed to Logcat in debug builds (`BuildConfig.DEBUG == true`). Release builds use `Level.NONE`.
- **Reproduction**: Run a debug build and inspect `okhttp.OkHttpClient` logs during `register_push_token`.
- **Fix**: Add `loggingInterceptor.redactHeader("Authorization")` and `loggingInterceptor.redactHeader("apikey")`, or use `Level.BASIC` / `Level.HEADERS`.
- **Release Blocker**: **NO**

### [LOW] FINDING 8: Triple Redundant `register_push_token` Execution on Cold Start & Duplicate `"messages"` Channel Definitions
- **File**: `app/src/main/java/com/example/service/PushNotificationManager.kt` (`L111–120, 213–231`), `app/src/main/java/com/example/MainActivity.kt` (`L34–35, 92–98, 184`), `app/src/main/java/com/example/util/SystemNotificationHelper.kt` (`L19, 36–45`)
- **Function**: `PushNotificationManager.onStart()`, `MainActivity.onCreate()`, `SystemNotificationHelper.createNotificationChannels()`
- **Evidence**: On cold start while logged in, `ProcessLifecycleOwner.onStart` (`PushNotificationManager.kt:118`), `MainActivity` `ON_RESUME` (`MainActivity.kt:95`), and `MainActivity` `LaunchedEffect(authState)` (`MainActivity.kt:184`) all fire and queue 3 sequential `register_push_token` RPC calls. Additionally, both `PushNotificationManager` and `SystemNotificationHelper` define channel `"messages"` with different names (`"Messages & Chats"` vs `"Messages & Direct Chats"`).
- **Impact**: Unnecessary network traffic on every cold start (2 extra RPC round-trips) and minor channel name inconsistency.
- **Reproduction**: Cold-start the app while logged in and observe 3 sequential `POST /rest/v1/rpc/register_push_token` requests.
- **Fix**: Remove the duplicate `ON_RESUME` push registration call in `MainActivity.kt:95` (since `ProcessLifecycleOwner.onStart` already covers cold start and every foreground transition) and unify `"messages"` channel creation in `PushNotificationManager`.
- **Release Blocker**: **NO**

### [INFO] FINDING 9: Plaintext Storage of `last_token` in App-Private `SharedPreferences`
- **File**: `app/src/main/java/com/example/service/PushNotificationManager.kt` (`L74–78, 195–200`)
- **Function**: `PushNotificationManager.updateDebugState()`
- **Evidence**: Stores `last_token` in `push_notification_debug_prefs.xml` (`Context.MODE_PRIVATE`).
- **Impact**: Informational only; FCM already stores its own token in app-private `SharedPreferences` (`com.google.android.gms.appid`).
- **Reproduction**: Inspect `/data/data/com.funkytalk.app/shared_prefs/push_notification_debug_prefs.xml`.
- **Fix**: Optional; keep in `MODE_PRIVATE` prefs as needed for logout fallback (`getSavedToken`), or clear immediately on logout.
- **Release Blocker**: **NO**

---

## Release Blockers

1. **[CRITICAL] Check In & Verify Supabase SQL Migration for `push_tokens`, `register_push_token`, `unregister_push_token`, and RLS Policies** (`supabase/migrations/`):
   - Verify `UNIQUE (token)` upsert behavior, `ENABLE ROW LEVEL SECURITY`, `auth.uid()` binding in both RPCs, and `REVOKE ALL / EXECUTE FROM anon`.
2. **[CRITICAL] Synchronize `unregisterCurrentToken` with `registrationMutex` and Fix Logout Ordering** (`PushNotificationManager.kt:258, 396`, `AuthViewModel.kt:2188–2209`, `SupabaseSessionManager.kt:213–239`):
   - Acquire `registrationMutex` in `unregisterCurrentToken`, refresh expired JWTs *before* `firebaseAuth?.signOut()`, and prevent `ensureActiveSession` from writing a session after logout.
3. **[CRITICAL] Fix Notification Tap Routing in `MainActivity.kt`** (`MainActivity.kt:38, 66–83, 232–448`, `SystemNotificationHelper.kt:95–100`, `PushNotificationManager.kt:474–478`):
   - Handle `"direct_chat"` and `from_push` / `conversation_id` / `group_id` intent extras in `MainActivity.kt` so tapping any push notification opens the target chat instead of rendering a blank screen.
4. **[HIGH] Remove E2EE Plaintext Logging & Deduplicate Push Notifications** (`SystemNotificationHelper.kt:124`, `FunkyFirebaseMessagingService.kt:41–55`, `EncryptedChatRepository.kt:728–745`):
   - Delete `$title - $message` from `Log.d` in `SystemNotificationHelper.kt:124`, prevent posting two notifications for the same DM push, and set `NotificationCompat.VISIBILITY_PRIVATE`.

---

## Verified

- **Firebase Configuration & Manifest**:
  - `applicationId = "com.funkytalk.app"` matches `app/google-services.json` (`package_name = "com.funkytalk.app"`).
  - `FunkyFirebaseMessagingService` is registered with `android:exported="false"` and `com.google.firebase.MESSAGING_EVENT` (`AndroidManifest.xml:95–101`).
  - Default FCM channel metadata is set to `"messages"` (`AndroidManifest.xml:91–93`).
  - Dynamic fallback initialization includes `gcmSenderId = "1093102729820"` (`GoogleServicesConfig.kt:13, 23`, `PushNotificationManager.kt:153`, `AuthViewModel.kt:1261`).
- **No Server-Side FCM Private Keys in Client**:
  - Zero Firebase service-account private keys exist anywhere in the Android repository or APK.
- **Notification Channel & Runtime Permission**:
  - Channel `"messages"` is created with `NotificationManager.IMPORTANCE_HIGH` in `Application.onCreate()` before any push can arrive (`FunkyTalkApplication.kt:24`, `PushNotificationManager.kt:217–220`).
  - `POST_NOTIFICATIONS` is declared in `AndroidManifest.xml:12` and requested on Android 13+ (`MainActivity.kt:44–58`).
- **Client RPC Payload & Auth Guard**:
  - Calls `register_push_token(p_token = token, p_platform = "android")` (`SupabaseApi.kt:1065, 1789`) and `unregister_push_token(p_token = token)` (`SupabaseApi.kt:1072, 1795`).
  - Rejects `BuildConfig.SUPABASE_ANON_KEY` and unauthenticated sessions (`PushNotificationManager.kt:291–301, 405–418`).
- **Retry & Backoff**:
  - Bounded to 3 attempts (`1500ms`, `3000ms` exponential backoff), logs exact errors via `Log.e`, and rethrows `CancellationException` (`PushNotificationManager.kt:262–350`).
- **Debug UI (`Settings > About`)**:
  - Strictly gated by `BuildConfig.DEBUG` (`ProfileTabContent.kt:1665`, `ChatAppearanceAndDebugModals.kt:376`), displays only the last 6 characters of the token (`takeLast(6)`), last registration timestamp, and provides the `"Re-register push"` button (`forceReregisterPush`).

---

## Not Verified

- **Supabase `public.push_tokens` Table Schema & Constraints**: **NOT VERIFIED FROM AVAILABLE PROJECT SOURCES**
- **Supabase `public.register_push_token` & `public.unregister_push_token` SQL Function Definitions**: **NOT VERIFIED FROM AVAILABLE PROJECT SOURCES**
- **Supabase Row Level Security (RLS) Policies & Role Grants (`anon`, `authenticated`, `service_role`) on `public.push_tokens`**: **NOT VERIFIED FROM AVAILABLE PROJECT SOURCES**
- **Remote Backend FCM Dispatcher Implementation (Edge Function / Webhook / Trigger & Payload Type)**: **NOT VERIFIED FROM AVAILABLE PROJECT SOURCES**

---

## Final Verdict

**NOT READY**

### Hardening Recommendations
1. **Lock-Screen Privacy (`VISIBILITY_PRIVATE`)**: Configure `.setVisibility(NotificationCompat.VISIBILITY_PRIVATE)` with a generic `.setPublicVersion(...)` notification on both `PushNotificationManager` and `SystemNotificationHelper`.
2. **Foreground Chat Suppression**: Track the currently open `conversationId` / `groupId` in memory and suppress heads-up push notifications when the user is actively viewing that exact conversation in the foreground.
3. **Avoid Unnecessary `deleteToken()` Churn on Normal Logout**: Once `unregister_push_token(p_token)` succeeds on the server and `register_push_token` is verified to upsert by `UNIQUE (token)`, consider keeping the device FCM instance token or rotating it inside `registrationMutex` so fast logout/login cycles do not suffer Google Play Services token-regeneration latency.
4. **Redact Headers in Debug OkHttp Interceptor**: Call `loggingInterceptor.redactHeader("Authorization")` and `loggingInterceptor.redactHeader("apikey")` in `SupabaseClient.kt`.

### Test Gaps
1. **No Unit / Robolectric Tests for `PushNotificationManager`**: No automated tests currently cover `registerTokenWithRetry` backoff behavior, `SUPABASE_ANON_KEY` rejection, HTTP 401 refresh recovery, or `unregisterCurrentToken` concurrency with `registerCurrentToken`.
2. **No SQL / pgTAP Tests for `push_tokens` RLS & RPCs**: No automated database tests verify cross-user isolation on `public.push_tokens`, `register_push_token`, and `unregister_push_token`.
3. **No Compose UI / Intent Navigation Tests for Push Tap**: No Robolectric test verifies tapping a notification PendingIntent (`navigate_to = "direct_chat"` or `from_push = true`) in `MainActivity`.

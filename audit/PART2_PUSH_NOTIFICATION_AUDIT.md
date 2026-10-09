# PART 2 AUDIT
# FUNKYTALK PUSH NOTIFICATION AUDIT — PART 2: AUTH + ACCOUNT ISOLATION + TOKEN LIFECYCLE

---

## Authentication Race

### 1. App Start → FCM Token Available → Supabase Session Not Ready
- **Code Path**:
  - `FunkyFirebaseMessagingService.onNewToken(token)` (`FunkyFirebaseMessagingService.kt:22–29`) or `PushNotificationManager.onStart()` (`PushNotificationManager.kt:111–120`) invokes `registerTokenWithRetry(appCtx, explicitToken)` (`PushNotificationManager.kt:255–350`).
- **Behavior**:
  1. `FirebaseMessaging.getInstance().token.await()` resolves the FCM token (`L276`) and saves `lastToken = token` locally (`L287`).
  2. `SupabaseSessionManager.ensureActiveSession(appCtx)` (`L290`) attempts to validate or refresh the Supabase JWT.
  3. Lines `291–294` resolve `authToken`:
     ```kotlin
     val authToken = SupabaseSessionManager.getAuthenticatedBearerToken(appCtx)
         ?: SupabaseSessionManager.getAccessToken(appCtx)
             ?.takeIf { it.isNotBlank() && it != BuildConfig.SUPABASE_ANON_KEY }
             ?.let { "Bearer $it" }
     ```
  4. If no user is authenticated, `authToken` is `null` (`L296–301`). The anon key (`BuildConfig.SUPABASE_ANON_KEY`) is **explicitly rejected**.
- **Verdict**: **SAFE against anonymous/unauthenticated registration.** A token cannot be registered without a real user JWT.

### 2. Login → Session Established → Token Registration
- **Code Path**:
  - **Email Login (`AuthViewModel.kt:1609–1646`)**: Calls `SupabaseSessionManager.setSession(context, accessToken, refreshToken, userId)` at `L1609` **before** setting `_authState.value = AuthState.Success(...)` (`L1642`) and calling `registerAndStartSession(context, userId)` (`L1645`).
  - **Email Sign-Up (`AuthViewModel.kt:1467–1510`)**: Calls `SupabaseSessionManager.setSession(...)` at `L1467` **before** setting `_authState.value = AuthState.Success(...)` at `L1503` (which triggers `MainActivity.kt:184` `registerCurrentToken`).
  - **Google Sign-In (`AuthViewModel.kt:2060, 2075, 2108–2109`)**: Calls `SupabaseSessionManager.setSession(...)` before `_authState.value = AuthState.Success(...)` and `registerAndStartSession(context, sUserId)`.
  - **Fallback Google Sign-In Failure Branches (`AuthViewModel.kt:2122–2132, 2140–2150`)**:
    - **Defect Found**: If `user.getIdToken(true)` fails or returns null during Google Sign-In (`L2120` or `L2138`), `AuthViewModel` calls `SupabaseSessionManager.setSession(context, accessToken = getAccessToken(context) ?: SUPABASE_ANON_KEY, userId = sUserId)` and then calls `registerAndStartSession(context, sUserId)`. In `registerTokenWithRetry`, `ensureActiveSession(appCtx)` (`L290`) will attempt to exchange the Firebase token again before calling `register_push_token`.

### 3. Logout → Session Cleared → Old Registration Coroutine Still Running
- **CRITICAL RACE CONDITION IDENTIFIED**:
  1. **`unregisterCurrentToken` does NOT acquire `registrationMutex` (`PushNotificationManager.kt:83, 258, 396`)**:
     - `registerTokenWithRetry` runs on `PushNotificationManager.scope` (`SupervisorJob() + Dispatchers.IO`) holding `registrationMutex`.
     - `unregisterCurrentToken` runs on `AuthViewModel.viewModelScope` (`Dispatchers.IO`) **without** acquiring `registrationMutex` and **without** cancelling in-flight registration jobs.
     - If `registerTokenWithRetry` for User A has already read `val authToken` at `L291` right before User A taps Logout, `unregisterCurrentToken` (`L420`) and `registerPushToken` (`L304`) race on the network. If `registerPushToken` lands on Supabase *after* `unregisterPushToken`, **User A's token is re-inserted into `public.push_tokens` after User A has logged out.**
  2. **`SupabaseSessionManager.ensureActiveSession` can resurrect User A's session after `clearSession` (`SupabaseSessionManager.kt:213–239`)**:
     - If `registerTokenWithRetry` calls `ensureActiveSession(appCtx)` (`L290`) just before `AuthViewModel.logout()` runs, and `ensureActiveSession` is awaiting `SupabaseClient.api.verifyFirebaseToken(...)` (`SupabaseSessionManager.kt:222`) while `AuthViewModel.logout()` executes `firebaseAuth?.signOut()` (`L2205`) and `SupabaseSessionManager.clearSession(context)` (`L2209`), then when `verifyFirebaseToken` returns 100ms later, `ensureActiveSession` executes `setSession(context, accessToken, refreshToken, userId)` (`L236`)—**restoring User A's JWT into `SupabaseSessionManager` after logout and registering User A's push token!**

---

## Account Isolation

### Trace: `USER A (login → token registered → logout)` → `USER B (login)`

| Aspect | Current Behavior | Isolated / Safe? | Evidence |
| :--- | :--- | :---: | :--- |
| **User A Token vs. User B Token** | `unregisterCurrentToken` calls `FirebaseMessaging.getInstance().deleteToken().await()` (`L440`) on logout, which tells FCM to invalidate User A's token so User B gets a new token. **However**, `unregisterCurrentToken` runs asynchronously without `registrationMutex`. | **NO (Race Condition)** | `AuthViewModel.kt:2197–2203`, `PushNotificationManager.kt:396–459` |
| **Cached State (`PushDebugState` / `SharedPreferences`)** | `unregisterCurrentToken` resets `is_registered = false, last_token = null` in its `finally` block (`L450–457`) *after* network RPC + `deleteToken()`. If User B logs in before User A's `unregisterCurrentToken` finishes, User A's `finally` block wipes User B's newly saved `PushDebugState`. | **NO (Race Condition)** | `PushNotificationManager.kt:450–457` |
| **Async Registration vs. Async Logout** | User A's `unregisterCurrentToken` is not serialized with User B's `registerTokenWithRetry`. User A's delayed `deleteToken().await()` (`L440`) can execute *after* User B has already registered the token, invalidating User B's active FCM token! | **NO (CRITICAL)** | `PushNotificationManager.kt:258` vs `L396` |
| **Database Ownership (`push_tokens`)** | If User A logs out while offline or with an expired JWT (>1h old), `unregister_push_token` fails (`L429–434`) and is never retried. If `deleteToken()` also fails offline (`L442`), User B logging in on the same device obtains the **exact same FCM token** (`Token A`). | **NO (HIGH)** | `PushNotificationManager.kt:405–444` |
| **Notification Routing** | If `unregister_push_token` fails on User A's logout (e.g., expired JWT or offline), `push_tokens` still maps `Token A` to `User A` (unless User B's `register_push_token` overwrites the row by `UNIQUE(token)`). While logged out or before User B registers, **User A's notifications will still be delivered to the device.** | **NO (HIGH)** | `AuthViewModel.kt:2188–2193`, `PushNotificationManager.kt:418–437` |

### Can User B inherit User A's token?
- **Yes, in two concrete scenarios**:
  1. **Fast User Switch (User A Logout → User B Login within 1–3 seconds)**:
     - Because `AuthViewModel.logout()` launches `unregisterCurrentToken` asynchronously (`AuthViewModel.kt:2197`) and immediately navigates to the login screen, `deleteToken().await()` (`PushNotificationManager.kt:440`) runs only *after* `token.await()` (`L412`) and the `unregister_push_token` HTTP request (`L420`).
     - If User B logs in while `unregister_push_token` is still in flight, User B's `registerTokenWithRetry` calls `FirebaseMessaging.getInstance().token.await()` (`L276`) and receives **User A's undeleted token**! Even worse, milliseconds later, User A's coroutine reaches `L440` (`deleteToken().await()`) and **destroys the token User B just registered**.
  2. **Offline / Failure During User A Logout**:
     - If `FirebaseMessaging.getInstance().deleteToken().await()` throws an IOException (offline/timeout at `L440–444`), the exception is swallowed and logged. When User B subsequently logs in online, `FirebaseMessaging.getInstance().token.await()` returns **User A's original FCM token**.

---

## Logout

### Exact Logout Execution Order (`AuthViewModel.kt:2184–2229` & `PushNotificationManager.kt:396–460`)

```text
[Main Thread: AuthViewModel.logout(context)]
  1. L2185-2187: Resolve effectiveUid
  2. L2188-2193: Capture capturedBearerToken synchronously from SupabaseSessionManager
                 (WARNING: If access token is expired, getAuthenticatedBearerToken() returns null
                  and fallback L2190 captures the EXPIRED access token!)
  3. L2197-2203: Launch async coroutine on viewModelScope(Dispatchers.IO):
                 PushNotificationManager.unregisterCurrentToken(appCtx, capturedBearerToken)
  4. L2205:      firebaseAuth?.signOut()                 <-- Executes BEFORE unregisterCurrentToken runs!
  5. L2206:      _authState.value = AuthState.Idle
  6. L2209:      SupabaseSessionManager.clearSession()   <-- Executes BEFORE unregisterCurrentToken runs!
  7. L2214-2220: Clear ChatBackupPreferencesManager, cancel WorkManager, lock VaultSecurityManager

[Background Thread (Dispatchers.IO): PushNotificationManager.unregisterCurrentToken]
  8. L405-409:   Read authToken (uses capturedBearerToken since session is already cleared)
  9. L411-416:   Call FirebaseMessaging.getInstance().token.await() ?: getSavedToken(appCtx)
 10. L420-424:   Call SupabaseClient.api.unregisterPushToken(auth = authToken, body = PushTokenRequest(token))
 11. L440:       Call FirebaseMessaging.getInstance().deleteToken().await()
 12. L450-457:   finally: updateDebugState(isRegistered = false, lastToken = null, ...)
```

### Critical Logout Findings
1. **Unregister Executes AFTER Local Authentication Is Cleared**:
   - `firebaseAuth?.signOut()` (`L2205`) and `SupabaseSessionManager.clearSession(context)` (`L2209`) run synchronously on the main thread **before** the `Dispatchers.IO` coroutine (`L2197`) executes `unregisterCurrentToken`.
   - Although `capturedBearerToken` is passed into `unregisterCurrentToken`, if User A's Supabase JWT had expired (tokens expire after 1 hour; see `SupabaseSessionManager.isTokenExpired` at `L68–86`), `capturedBearerToken` holds an **expired JWT**. Because `firebaseAuth?.signOut()` has already executed on `L2205`, `SupabaseSessionManager.ensureActiveSession` **cannot** refresh the token from Firebase Auth! Result: `unregister_push_token` fails with HTTP 401 and **the token remains active in `public.push_tokens` on the server**.
2. **`token.await()` Called Instead of Reading `getSavedToken(appCtx)` First (`PushNotificationManager.kt:411–416`)**:
   - Line 412 calls `FirebaseMessaging.getInstance().token.await()` first and only uses `getSavedToken(appCtx)` if `token.await()` throws. If the FCM SDK already rotated the token or is slow, it delays unregistration instead of immediately unregistering the exact token stored in `getSavedToken(appCtx)`.
3. **Local Debug State Cleared Last (`L450–457`)**:
   - `updateDebugState(isRegistered = false, lastToken = null)` runs in `finally` *after* the network calls, creating a window where a new login can have its state overwritten by the finishing logout coroutine.

---

## Token Rotation

### Inspection of `FunkyFirebaseMessagingService.onNewToken(token: String)` (`FunkyFirebaseMessagingService.kt:22–29`)

```text
FCM rotates Token A -> Token B
       │
       ▼
FunkyFirebaseMessagingService.onNewToken(Token B)
       │
       ▼
PushNotificationManager.registerToken(applicationContext, Token B)  [L246-253]
       ├── L250: Immediately overwrites lastToken = Token B in SharedPreferences
       └── L252: Calls registerTokenWithRetry(appCtx, explicitToken = Token B)
             └── Calls register_push_token(p_token = Token B, p_platform = "android")
```

| Verification Item | Behavior | Status |
| :--- | :--- | :---: |
| **Token A Cleanup** | `onNewToken` overwrites `lastToken` with `Token B` at `L250` **without** calling `unregister_push_token(Token A)`. Unless the server RPC deletes prior tokens, `Token A` remains in `push_tokens`. | **NOT CLEANED BY CLIENT** |
| **Token B Registration** | Registered via `registerTokenWithRetry(appCtx, explicitToken = Token B)` with `p_platform = "android"`. | **VERIFIED** |
| **Authentication Requirement** | Calls `SupabaseSessionManager.ensureActiveSession(appCtx)` (`L290`) and requires a valid non-anon Bearer JWT (`L291–301`). | **VERIFIED** |
| **Duplicate Rows** | Depends on server `push_tokens` schema: if keyed by `(user_id, token)`, `Token A` and `Token B` both exist in `push_tokens` for the same device until `Token A` is pruned. | **SERVER-DEPENDENT** |
| **Failure Handling** | Retries up to 3 times (`1.5s`, `3.0s` backoff) and logs exact HTTP/exception errors with `Log.e` (`L262–348`). | **VERIFIED** |

---

## `deleteToken()`

### Inspection of `FirebaseMessaging.getInstance().deleteToken()`

| Call Site | File & Line | Await Handling | Failure Handling | Token Regeneration & Side Effects |
| :--- | :--- | :--- | :--- | :--- |
| **Manual Re-register** (`forceReregisterPush`) | `PushNotificationManager.kt:362` | `FirebaseMessaging.getInstance().deleteToken().await()` (`L362`) | Caught & logged via `Log.e` (`L364–366`); proceeds to `token.await()` | Immediately calls `FirebaseMessaging.getInstance().token.await()` (`L369`) AND triggers `FunkyFirebaseMessagingService.onNewToken(newToken)` (`L22`), causing **two sequential `register_push_token` RPC calls** for the new token while **never calling `unregister_push_token` for the old token**. |
| **User Logout** (`unregisterCurrentToken`) | `PushNotificationManager.kt:440` | `FirebaseMessaging.getInstance().deleteToken().await()` (`L440`) | Caught & logged via `Log.e` (`L442–444`) | Destroys the device's FCM instance token on every logout. Next login must generate a brand-new FCM token via Google Play Services network round-trip. |

### Does Logout/Login Create Unnecessary Token Churn?
- **Yes**:
  1. Calling `deleteToken()` on every logout forces Google Play Services to invalidate and mint a new FCM registration token on every logout/login cycle, increasing latency on subsequent login and triggering an extra `onNewToken` broadcast.
  2. In `forceReregisterPush` (`L356–391`), calling `deleteToken().await()` without first calling `unregister_push_token(oldToken)` leaves the old token row in `public.push_tokens` on the server while inserting a new token row.

---

## Multi-Device

### Can `User A → Phone 1 Token` and `User A → Phone 2 Token` Coexist?

| Parameter / Aspect | Client Implementation | Impact on Multi-Device |
| :--- | :--- | :--- |
| **RPC Payload (`register_push_token`)** | `{"p_token": "<token>", "p_platform": "android"}` (`SupabaseApi.kt:1789–1792`) | Each physical phone generates a distinct FCM token (`Phone 1 Token != Phone 2 Token`). |
| **RPC Payload (`unregister_push_token`)** | `{"p_token": "<token>"}` (`SupabaseApi.kt:1795–1797`) | Unregisters **only** the specific `p_token` passed by the logging-out phone, leaving other devices' tokens untouched. |
| **Device ID** | None sent (`RegisterPushTokenRequest` has only `p_token` and `p_platform`). | Differentiated solely by `p_token`. |
| **Client Single-Session Logic** | `AuthViewModel.kt:1303–1304, 1876–1896` defines `active_session_id_${uid}` and `logoutDueToSessionMismatch`, though `startSessionListener` currently only checks local `SharedPreferences`. | Does not block multiple tokens in `PushNotificationManager`. |
| **Server Primary Key / Unique Constraints / Upsert** | Defined in remote Supabase database (`public.push_tokens` and `public.register_push_token` SQL are not in local repository). | **Coexistence holds if and only if** `public.push_tokens` has primary/unique key on `(token)` or `(user_id, token)` rather than `UNIQUE (user_id, platform)`. |

---

## Findings

### [CRITICAL] FINDING 1: `unregisterCurrentToken` Is Not Synchronized with `registrationMutex` (Logout / Re-Login Race)
- **File**: `app/src/main/java/com/example/service/PushNotificationManager.kt` (`L258` vs `L396–459`) & `AuthViewModel.kt` (`L2197`)
- **Problem**: `registerTokenWithRetry` acquires `registrationMutex`, whereas `unregisterCurrentToken` and `forceReregisterPush`'s `deleteToken()` do not.
- **Impact**:
  1. An in-flight `registerTokenWithRetry` for User A can complete *after* `unregisterCurrentToken`, re-registering User A's token after logout.
  2. If User B logs in immediately after User A logs out, User A's still-running `unregisterCurrentToken` can call `deleteToken().await()` (`L440`) and wipe `PushDebugState` (`L450`) *after* User B has already registered, destroying User B's token.

### [CRITICAL] FINDING 2: Expired JWT on Logout Causes `unregister_push_token` to Fail Silently on Server
- **File**: `app/src/main/java/com/example/AuthViewModel.kt` (`L2188–2209`)
- **Problem**: `AuthViewModel.logout` captures `capturedBearerToken` synchronously and immediately calls `firebaseAuth?.signOut()` (`L2205`) and `SupabaseSessionManager.clearSession(context)` (`L2209`). If User A's 1-hour Supabase access token has expired at the moment of logout, `getAuthenticatedBearerToken` returns `null`, `capturedBearerToken` falls back to the expired JWT (`L2190`), and `unregisterCurrentToken` cannot call `ensureActiveSession` because Firebase Auth was already signed out.
- **Impact**: `unregister_push_token` fails with HTTP 401, leaving User A's push token active in `public.push_tokens` after logout.

### [HIGH] FINDING 3: In-Flight `ensureActiveSession` Can Resurrect Cleared Session After Logout
- **File**: `app/src/main/java/com/example/SupabaseSessionManager.kt` (`L197–259`) & `PushNotificationManager.kt` (`L290`)
- **Problem**: If `ensureActiveSession` is awaiting `verifyFirebaseToken` (`L222`) when `logout()` calls `clearSession(context)` (`L261`), `ensureActiveSession` subsequently writes `setSession(context, accessToken, refreshToken, userId)` (`L236`) without checking if `FirebaseAuth.getInstance().currentUser` was signed out in the interim.
- **Impact**: Restores User A's Supabase session in `SupabaseSessionManager` after logout and allows the in-flight registration coroutine to register User A's token.

### [MEDIUM] FINDING 4: Old Token Not Unregistered During `onNewToken` Rotation or `forceReregisterPush`
- **File**: `app/src/main/java/com/example/service/PushNotificationManager.kt` (`L246–253`, `L356–378`)
- **Problem**: Neither `onNewToken` nor `forceReregisterPush` calls `unregister_push_token(oldToken)` before overwriting `lastToken` and registering the new token.
- **Impact**: Leaves orphaned token rows in `public.push_tokens` unless pruned by server-side logic.

---

## Release Blockers

1. **Synchronize `unregisterCurrentToken` and `forceReregisterPush` with `registrationMutex` (and cancel any in-flight registration Job on logout)** in `PushNotificationManager.kt` so logout unregistration and `deleteToken()` can never race with an in-flight registration or a subsequent user's login.
2. **Refresh Expired Session Before Signing Out on Logout (or Guard `ensureActiveSession` Against Post-Logout Write)** so `unregister_push_token` always has a valid non-expired JWT before `firebaseAuth?.signOut()` and `clearSession()` execute, and `ensureActiveSession` aborts if `FirebaseAuth.getInstance().currentUser == null` after `verifyFirebaseToken` returns.

---

## Verified

- **Unauthenticated Cold Start Guard**: `registerTokenWithRetry` (`PushNotificationManager.kt:291–301`) strictly rejects `BuildConfig.SUPABASE_ANON_KEY` and aborts if no authenticated user JWT exists.
- **Login Session Ordering**: `AuthViewModel` (`L1467, 1609, 2060, 2075`) writes `SupabaseSessionManager.setSession` before emitting `AuthState.Success` or calling `registerAndStartSession`.
- **Awaited `deleteToken()` and `token.await()`**: All FCM `Task` calls in `PushNotificationManager.kt` (`L276, 362, 369, 412, 440`) use `kotlinx.coroutines.tasks.await()` with `try/catch` and `Log.e`.
- **Token-Scoped Unregistration Payload**: `unregister_push_token` passes `{"p_token": token}`, scoping unregistration to the specific device token rather than wiping all devices for the user.

---

## Not Verified

- **Server-Side `public.push_tokens` Table Constraints & `register_push_token` SQL Definition**: Not present in local repository SQL files (`supabase_schema.sql` / `supabase/`). Specifically:
  - Whether `public.push_tokens` enforces `UNIQUE (token)` (which reassigns a token to User B on upsert) vs. `UNIQUE (user_id, token)` (which would allow both User A and User B to retain rows for the same token if User A's logout failed offline).
  - Whether `public.register_push_token` deletes prior tokens for the same `(user_id, platform)` or allows multiple Android tokens per user for multi-device coexistence.

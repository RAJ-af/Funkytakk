# PART 1 AUDIT
# FUNKYTALK PUSH NOTIFICATION AUDIT — PART 1: REPOSITORY + TOKEN ARCHITECTURE

---

## Architecture

### Complete End-to-End Push Flow
```text
[Firebase Cloud Messaging (Google Play Services)]
       │
       ├── 1. App Startup / Foreground / Login
       │      └── PushNotificationManager.ensureFirebaseInitialized(context)
       │            └── Reads GoogleServicesConfig (apiKey, appId, projectId, gcmSenderId = "1093102729820")
       │      └── FirebaseMessaging.getInstance().token.await()
       │
       ├── 2. Asynchronous Token Rotation
       │      └── FunkyFirebaseMessagingService.onNewToken(token)
       │            └── PushNotificationManager.registerToken(applicationContext, token)
       │
       ▼
[PushNotificationManager.registerTokenWithRetry(context, explicitToken)]
       │  (Mutex-serialized, max 3 attempts, 1.5s / 3.0s backoff)
       ├── Persists raw token to SharedPreferences ("push_notification_debug_prefs" -> "last_token")
       ├── Calls SupabaseSessionManager.ensureActiveSession(context)
       ├── Resolves Bearer JWT via SupabaseSessionManager.getAuthenticatedBearerToken(context)
       └── Calls Retrofit: SupabaseClient.api.registerPushToken(
               auth = "Bearer <jwt>",
               body = RegisterPushTokenRequest(p_token = token, p_platform = "android")
           )
       │
       ▼
[Supabase PostgREST RPC: POST /rest/v1/rpc/register_push_token]
       │
       ▼
[Supabase Table: public.push_tokens]
       │
       ▼
[Incoming FCM Push Message]
       │
       ├── App in Foreground / Data-or-Notification Message routed to Service:
       │      └── FunkyFirebaseMessagingService.onMessageReceived(remoteMessage)
       │            ├── PushNotificationManager.createNotificationChannels(applicationContext)  [channel: "messages", IMPORTANCE_HIGH]
       │            ├── PushNotificationManager.showPushNotification(context, title, body, data)
       │            │     ├── Checks VaultSecurityManager.isVaultLocked() (masks title/body if locked)
       │            │     └── Posts NotificationCompat on channel "messages" with PendingIntent -> MainActivity (extras: data + "from_push"=true)
       │            └── Triggers background inbox drain:
       │                  ├── EncryptedChatRepository.drainPendingDmMessages(applicationContext)
       │                  └── GroupChatRepository.drainPendingGroupMessages(applicationContext)
       │
       ├── App in Background / Killed with Notification Payload (System Tray):
       │      └── Android System uses AndroidManifest meta-data:
       │            com.google.firebase.messaging.default_notification_channel_id = "messages"
       │
       ▼
[User Logout: AuthViewModel.logout(context)]
       │
       ├── Captures active Supabase Bearer JWT BEFORE clearing session
       └── Launches PushNotificationManager.unregisterCurrentToken(appCtx, capturedBearerToken)
             ├── 1. Resolves token via FirebaseMessaging.getInstance().token.await() ?: getSavedToken(appCtx)
             ├── 2. Calls SupabaseClient.api.unregisterPushToken(
             │          auth = capturedBearerToken,
             │          body = PushTokenRequest(p_token = token)
             │      ) -> POST /rest/v1/rpc/unregister_push_token
             ├── 3. Calls FirebaseMessaging.getInstance().deleteToken().await()
             └── 4. Resets PushDebugState & SharedPreferences ("is_registered" = false, "last_token" = null)
```

---

## Files & Components

### 1. Core Push & Notification Files
| File | Class / Object | Key Functions | Role |
| :--- | :--- | :--- | :--- |
| `app/src/main/AndroidManifest.xml` | Manifest | `<uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>` (L12), `<meta-data android:name="com.google.firebase.messaging.default_notification_channel_id" android:value="messages"/>` (L91–93), `<service android:name=".service.FunkyFirebaseMessagingService">` (L95–101) | Declares runtime permission, default FCM channel `"messages"`, and binds `com.google.firebase.MESSAGING_EVENT`. |
| `app/src/main/java/com/example/service/PushNotificationManager.kt` | `PushNotificationManager` (`DefaultLifecycleObserver`), `PushDebugState` | `initialize` (L94), `onStart` (L111), `onUserLoggedIn` (L123), `ensureFirebaseInitialized` (L143), `createNotificationChannels` (L213), `registerCurrentToken` (L237), `registerToken` (L246), `registerTokenWithRetry` (L255), `forceReregisterPush` (L356), `unregisterCurrentToken` (L397), `showPushNotification` (L462) | Central singleton managing `"messages"` channel creation, FCM token acquisition, 3-try backoff registration with Supabase, logout unregistration + token deletion, foreground lifecycle observation, and debug state persistence. |
| `app/src/main/java/com/example/service/FunkyFirebaseMessagingService.kt` | `FunkyFirebaseMessagingService` (`FirebaseMessagingService`) | `onCreate` (L17), `onNewToken` (L22), `onMessageReceived` (L31) | Receives new FCM tokens and incoming FCM payloads, displays notifications on `"messages"`, and triggers DM + Group offline inbox drains. |
| `app/src/main/java/com/example/SupabaseApi.kt` | `SupabaseApi`, `RegisterPushTokenRequest`, `PushTokenRequest` | `registerPushToken` (L1066), `unregisterPushToken` (L1073), `RegisterPushTokenRequest` (L1789), `PushTokenRequest` (L1795) | Retrofit definitions for `POST rest/v1/rpc/register_push_token` (`p_token`, `p_platform = "android"`) and `POST rest/v1/rpc/unregister_push_token` (`p_token`). |
| `app/src/main/java/com/example/util/SystemNotificationHelper.kt` | `SystemNotificationHelper` | `createNotificationChannels` (L28), `hasNotificationPermission` (L66), `showSystemNotification` (L77) | Secondary local notification helper configured with `CHANNEL_MESSAGES_ID = "messages"` (`IMPORTANCE_HIGH`) and `POST_NOTIFICATIONS` permission check. |
| `app/src/main/java/com/example/FunkyTalkApplication.kt` | `FunkyTalkApplication` (`Application`) | `onCreate` (L20) | Calls `PushNotificationManager.initialize(this)` on process start. |
| `app/src/main/java/com/example/MainActivity.kt` | `MainActivity` (`FragmentActivity`) | `onCreate` (L28), `DisposableEffect(lifecycleOwner)` (L85), `LaunchedEffect(authState)` (L129) | Creates notification channels on start, requests `POST_NOTIFICATIONS` on Android 13+, and triggers token registration on `ON_RESUME`, permission grant, and `AuthState.Success`. |
| `app/src/main/java/com/example/AuthViewModel.kt` | `AuthViewModel` (`ViewModel`) | `initializeFirebaseIfNeeded` (L1246), `registerAndStartSession` (L1877), `startSessionListener` (L1885), `logout` (L2184) | Initializes `FirebaseApp` with `gcmSenderId`, triggers `PushNotificationManager.onUserLoggedIn` upon login/session restore, and triggers `unregisterCurrentToken` before clearing session on logout. |
| `app/src/main/java/com/example/GoogleServicesConfig.kt` | `GoogleServicesConfig` | `getGcmSenderId` (L23), `getApplicationId` (L74) | Supplies `project_number` (`"1093102729820"`) and `mobilesdk_app_id` from `google-services.json` so `FirebaseMessaging` has a valid sender ID. |
| `app/src/main/java/com/example/ui/screens/home/tabs/ProfileTabContent.kt` | `ProfileTabContent` | `showOptionsMenuSheet` -> Settings > About (L1618–1707) | Renders debug line (`pushDebugState.summaryLine`) and `"Re-register push"` button in `BuildConfig.DEBUG`. |
| `app/src/main/java/com/example/ui/chat/ChatAppearanceAndDebugModals.kt` | `ChatAppearanceBottomSheet` | About (Debug) section (L376–429) | Secondary Settings > Chat appearance debug section displaying `pushDebugState.summaryLine` and `"Re-register push"` button in `BuildConfig.DEBUG`. |

---

### 2. Complete Repository Search Results

| Symbol / Keyword | File & Line | Class / Function | Purpose | Caller | Lifecycle Trigger |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `FirebaseMessaging` | `PushNotificationManager.kt:26, 276, 362, 369, 412, 440` | `PushNotificationManager` | Singleton entry point for FCM token retrieval and deletion | `registerTokenWithRetry`, `forceReregisterPush`, `unregisterCurrentToken` | Cold start, foreground, login, `onNewToken`, manual re-register, logout |
| `getToken()` / `.token` | `PushNotificationManager.kt:276, 369, 412` | `registerTokenWithRetry`, `forceReregisterPush`, `unregisterCurrentToken` | Obtains `Task<String>` for the device's current FCM registration token | `PushNotificationManager` | Cold start, foreground, login, manual re-register, logout |
| `token.await()` | `PushNotificationManager.kt:276, 369, 412` | `registerTokenWithRetry`, `forceReregisterPush`, `unregisterCurrentToken` | Suspends coroutine via `kotlinx.coroutines.tasks.await()` until `FirebaseMessaging.getInstance().token` completes | `PushNotificationManager` | Inside `Dispatchers.IO` coroutines |
| `onNewToken` | `FunkyFirebaseMessagingService.kt:22` | `FunkyFirebaseMessagingService.onNewToken(token: String)` | Receives refreshed/initial FCM token from Google Play Services | Android FCM SDK (`MESSAGING_EVENT`) | App install, token rotation, or after `deleteToken()` |
| `register_push_token` | `SupabaseApi.kt:1065` | `SupabaseApi.registerPushToken` | Retrofit `@POST("rest/v1/rpc/register_push_token")` sending `RegisterPushTokenRequest(p_token, p_platform="android")` | `PushNotificationManager.registerTokenWithRetry` (`L304`) | Cold start, foreground, login, `onNewToken`, manual re-register |
| `unregister_push_token` | `SupabaseApi.kt:1072` | `SupabaseApi.unregisterPushToken` | Retrofit `@POST("rest/v1/rpc/unregister_push_token")` sending `PushTokenRequest(p_token)` | `PushNotificationManager.unregisterCurrentToken` (`L419`) | User logout (`AuthViewModel.logout`) |
| `deleteToken()` | `PushNotificationManager.kt:362, 440` | `forceReregisterPush`, `unregisterCurrentToken` | Awaits `FirebaseMessaging.getInstance().deleteToken().await()` to invalidate current FCM token | `ProfileTabContent` / `ChatAppearanceBottomSheet` ("Re-register push"), `AuthViewModel.logout` | Manual re-register button click, or user logout |
| `POST_NOTIFICATIONS` | `AndroidManifest.xml:12`, `MainActivity.kt:48, 55`, `SystemNotificationHelper.kt:70, 88` | `MainActivity.onCreate`, `SystemNotificationHelper.hasNotificationPermission` | Declares, checks, and requests Android 13+ (API 33+) runtime notification permission | `MainActivity` (`LaunchedEffect(Unit)`), `SystemNotificationHelper` | Activity creation (`MainActivity.onCreate`) |
| `NotificationChannel` | `PushNotificationManager.kt:3, 217`, `SystemNotificationHelper.kt:3, 36, 48`, `VoiceRoomOverlayService.kt:154` | `createNotificationChannels` | Creates Android O+ notification channels | `FunkyTalkApplication.onCreate`, `MainActivity.onCreate`, `FunkyFirebaseMessagingService.onCreate/onNewToken/onMessageReceived` | App process start, Activity start, FCM service start, push receipt |
| `"messages"` | `AndroidManifest.xml:93`, `PushNotificationManager.kt:71`, `SystemNotificationHelper.kt:19` | `CHANNEL_ID_MESSAGES`, `CHANNEL_MESSAGES_ID`, `default_notification_channel_id` | High-importance (`IMPORTANCE_HIGH`) notification channel ID for chat pushes | `PushNotificationManager`, `SystemNotificationHelper`, FCM system tray handler | Created before any push can arrive (`Application.onCreate`) |

---

## Token Acquisition

1. **FCM Token Acquisition & Coroutine Awaiting**:
   - Implemented in `PushNotificationManager.registerTokenWithRetry` (`PushNotificationManager.kt:272–277`).
   - Before accessing `FirebaseMessaging.getInstance()`, `ensureFirebaseInitialized(appCtx)` (`L143–160`) checks `FirebaseApp.getApps(appCtx)` and, if empty, initializes `FirebaseApp` with `apiKey`, `applicationId`, `projectId`, and `gcmSenderId` (`"1093102729820"` from `GoogleServicesConfig.getGcmSenderId`).
   - Uses `FirebaseMessaging.getInstance().token.await()?.trim()` inside `withContext(Dispatchers.IO)` and `registrationMutex.withLock`.
2. **Null / Empty Handling**:
   - Lines `279–284`: Checks `if (token.isNullOrBlank())`. If null or blank, logs `Log.e(TAG, "Attempt $attempt/$MAX_RETRY_ATTEMPTS failed: FirebaseMessaging.getInstance().token returned null or blank")`, updates `PushDebugState` with `isRegistered = false` and `lastError`, and continues to the next backoff retry.
3. **Exception Handling**:
   - Lines `335–346`: Rethrows `CancellationException` to respect structured concurrency, and catches `Exception`, logging full stack traces via `Log.e(TAG, "Attempt $attempt/$MAX_RETRY_ATTEMPTS threw exception during push token registration: $errMsg", e)` and storing `lastError` in `PushDebugState`.
   - Non-2xx HTTP responses from Supabase RPC are caught at lines `324–333`, reading `resp.errorBody()?.string()` and logging the exact status code, message, and response body with `Log.e`.
4. **Token Persistence**:
   - Stored in `SharedPreferences` (`"push_notification_debug_prefs"`) under keys `is_registered`, `last_token`, `last_registered_at`, and `last_error` (`L73–77`, `L177–203`).
   - Exposed reactively via `PushNotificationManager.debugState: StateFlow<PushDebugState>` (`L90–91`).
   - Raw token is persisted at line `287` immediately after `token.await()` succeeds—even before the Supabase RPC completes—so `getSavedToken(context)` and the debug UI always have the latest token suffix.
5. **Token Logging**:
   - Logs only the last 6 characters (`token.takeLast(6)`) in `Log.d` (`L24` in `FunkyFirebaseMessagingService.kt`, `L321` & `L426` in `PushNotificationManager.kt`) and in the Settings > About debug UI (`PushDebugState.tokenSuffix`).
6. **Authentication Dependency (Can registration occur before Supabase authentication is ready?)**:
   - **Yes, it can be invoked before Supabase auth is ready (e.g., `FunkyFirebaseMessagingService.onNewToken` on fresh install, or cold start while logged out), and it is handled safely:**
   - In `registerTokenWithRetry` (`L290–301`):
     1. The fetched FCM token is first saved locally (`updateDebugState(appCtx, lastToken = token)`).
     2. `SupabaseSessionManager.ensureActiveSession(appCtx)` is called (`L290`), which checks if the stored Supabase JWT is valid or, if a `FirebaseAuth` user is signed in, automatically exchanges the Firebase ID token via `verifyFirebaseToken` for a fresh Supabase JWT.
     3. If the user is not logged in (no valid Supabase JWT), `authToken` is `null` (`L296`), the attempt logs `Log.e` and retries up to 3 times (`1.5s`, `3.0s`).
     4. As soon as the user completes login (`AuthViewModel.registerAndStartSession` / `MainActivity` `LaunchedEffect(authState)`), `registerCurrentToken` runs again and registers the token with the newly minted Supabase session.

---

## Registration Triggers

| Trigger | Implemented | Auth Safe | Token Safe | RPC | Evidence |
| :--- | :---: | :---: | :---: | :--- | :--- |
| **Cold App Start** | Yes | Yes (`isUserLoggedIn` + `ensureActiveSession`) | Yes (`token.await()`) | `register_push_token(p_token, p_platform="android")` | `FunkyTalkApplication.onCreate` (`L24`) -> `PushNotificationManager.initialize` -> `ProcessLifecycleOwner` `onStart` (`L111–120`); plus `MainActivity.kt` `LaunchedEffect(authState)` (`L184`) and `AuthViewModel.startSessionListener` (`L1892`). |
| **Foreground (App Resume)** | Yes | Yes (`isUserLoggedIn` + `ensureActiveSession`) | Yes (`token.await()`) | `register_push_token(p_token, p_platform="android")` | `PushNotificationManager.onStart(owner)` via `ProcessLifecycleOwner` (`PushNotificationManager.kt:111–120`) AND `MainActivity.kt` `Lifecycle.Event.ON_RESUME` observer (`MainActivity.kt:93–97`). |
| **Successful Login** | Yes | Yes (runs after `SupabaseSessionManager.setSession`) | Yes (`token.await()`) | `register_push_token(p_token, p_platform="android")` | `AuthViewModel.registerAndStartSession` (`AuthViewModel.kt:1883`) and `MainActivity.kt` `LaunchedEffect(authState)` when `authState is AuthState.Success` (`MainActivity.kt:184`). |
| **Session Restoration** | Yes | Yes (`ensureActiveSession` refreshes expired JWT via Firebase ID token) | Yes (`token.await()`) | `register_push_token(p_token, p_platform="android")` | `AuthViewModel.checkCurrentUser` -> `startSessionListener` (`AuthViewModel.kt:1305, 1892`) -> `PushNotificationManager.onUserLoggedIn`, which calls `SupabaseSessionManager.ensureActiveSession(appCtx)` (`PushNotificationManager.kt:290`). |
| **`onNewToken`** | Yes | Yes (persists token locally; calls `ensureActiveSession` before RPC) | Yes (uses non-blank `token` param or awaits `.token`) | `register_push_token(p_token, p_platform="android")` | `FunkyFirebaseMessagingService.onNewToken` (`FunkyFirebaseMessagingService.kt:22–29`) -> `PushNotificationManager.registerToken(applicationContext, token)`. |
| **Manual Re-register** | Yes | Yes (`ensureActiveSession`) | Yes (`deleteToken().await()` then `token.await()`) | `register_push_token(p_token, p_platform="android")` | `ProfileTabContent.kt:1685–1694` & `ChatAppearanceAndDebugModals.kt:403–411` -> `PushNotificationManager.forceReregisterPush(context)` (`PushNotificationManager.kt:356–391`). |
| **Retry After Failure** | Yes | Yes (re-runs `ensureActiveSession` on each attempt) | Yes (re-awaits `token.await()` if needed) | `register_push_token(p_token, p_platform="android")` | `PushNotificationManager.registerTokenWithRetry` (`PushNotificationManager.kt:262–349`): `for (attempt in 1..3)` with exponential backoff (`1500ms`, `3000ms`). |

---

## Lifecycle

1. **`FunkyTalkApplication.onCreate()` (`FunkyTalkApplication.kt:20–26`)**:
   - Executes first on process creation.
   - Calls `PushNotificationManager.initialize(this)`:
     - Immediately creates the `"messages"` `NotificationChannel` (`IMPORTANCE_HIGH`).
     - Loads persisted `PushDebugState` from `SharedPreferences`.
     - Registers `PushNotificationManager` with `ProcessLifecycleOwner.get().lifecycle`.
2. **`ProcessLifecycleOwner` (`PushNotificationManager.kt:111–120`)**:
   - `onStart(owner: LifecycleOwner)` fires on cold start and every transition from background to foreground.
   - Checks `isUserLoggedIn(ctx)` and launches `registerCurrentToken(ctx)` on `Dispatchers.IO`.
3. **`MainActivity.onCreate()` & Compose Lifecycle (`MainActivity.kt:28–185`)**:
   - Calls `PushNotificationManager.initialize(applicationContext)`, `PushNotificationManager.createNotificationChannels(applicationContext)`, and `SystemNotificationHelper.createNotificationChannels(applicationContext)` synchronously before `setContent`.
   - Requests `POST_NOTIFICATIONS` on Android 13+ (`TIRAMISU`); if granted while logged in, calls `PushNotificationManager.onUserLoggedIn(applicationContext)`.
   - Observes `Lifecycle.Event.ON_RESUME` (`L93–97`): if logged in, calls `PushNotificationManager.onUserLoggedIn(applicationContext)`.
   - Observes `LaunchedEffect(authState)` (`L129–185`): when `authState is AuthState.Success`, calls `PushNotificationManager.registerCurrentToken(applicationContext)`.
4. **`AuthViewModel` (`AuthViewModel.kt:1877–1894`, `L2184–2229`)**:
   - On session start (`startSessionListener`) or new login (`registerAndStartSession`), triggers `PushNotificationManager.onUserLoggedIn(context)`.
   - On `logout(context)`: captures `capturedBearerToken` before `firebaseAuth?.signOut()` and `SupabaseSessionManager.clearSession(context)`, then launches `PushNotificationManager.unregisterCurrentToken(appCtx, capturedBearerToken)` on `Dispatchers.IO`.
5. **`FunkyFirebaseMessagingService` (`FunkyFirebaseMessagingService.kt:13–57`)**:
   - `onCreate()`: creates `"messages"` notification channel.
   - `onNewToken(token)`: creates `"messages"` channel and launches `PushNotificationManager.registerToken(applicationContext, token)`.
   - `onMessageReceived(remoteMessage)`: creates `"messages"` channel, posts notification via `PushNotificationManager.showPushNotification(...)`, and drains pending DM and Group messages.

---

## Duplicate Registration Risks

### Can registration execute multiple times on cold start or login?
- **Yes, multiple triggers fire in close succession during cold start and login**:
  1. `ProcessLifecycleOwner.onStart` -> `PushNotificationManager.onStart` -> `registerCurrentToken`
  2. `AuthViewModel.checkCurrentUser` -> `startSessionListener` -> `PushNotificationManager.onUserLoggedIn` -> `registerCurrentToken`
  3. `MainActivity` `DisposableEffect` (`ON_RESUME`) -> `PushNotificationManager.onUserLoggedIn` -> `registerCurrentToken`
  4. `MainActivity` `LaunchedEffect(authState)` (`AuthState.Success`) -> `PushNotificationManager.registerCurrentToken`
  5. Additionally, when `forceReregisterPush` calls `deleteToken().await()` and `token.await()`, Firebase also invokes `FunkyFirebaseMessagingService.onNewToken(newToken)`, which calls `registerToken`.
- **How Concurrency Is Protected**:
  - `PushNotificationManager.registerTokenWithRetry` is guarded by `private val registrationMutex = Mutex()` (`PushNotificationManager.kt:83, 258`).
  - Concurrent calls do **not** race in parallel; they queue sequentially behind `registrationMutex.withLock`.
  - The backend `register_push_token` RPC is idempotent (`ON CONFLICT` upsert on `push_tokens`), so sequential calls with the same token do not corrupt state, though they perform redundant network round-trips (3–4 sequential RPC calls on cold start).

---

## Verified

- **Exact RPC Signatures & Payloads**:
  - `register_push_token` sends `{"p_token": "<token>", "p_platform": "android"}` (`SupabaseApi.kt:1065–1070`, `1789–1792`).
  - `unregister_push_token` sends `{"p_token": "<token>"}` (`SupabaseApi.kt:1072–1077`, `1795–1797`).
- **Awaited Token Retrieval**:
  - `FirebaseMessaging.getInstance().token.await()` is used everywhere instead of unawaited `Task.getResult()`.
- **Firebase Options GCM Sender ID**:
  - `GoogleServicesConfig.getGcmSenderId(context)` supplies `"1093102729820"` to `FirebaseOptions.Builder().setGcmSenderId(...)` in both `PushNotificationManager.ensureFirebaseInitialized` and `AuthViewModel.initializeFirebaseIfNeeded`.
- **Notification Channel `"messages"` (Importance `HIGH`)**:
  - Created in `FunkyTalkApplication.onCreate`, `MainActivity.onCreate`, `FunkyFirebaseMessagingService.onCreate/onNewToken/onMessageReceived`, and declared as `com.google.firebase.messaging.default_notification_channel_id` in `AndroidManifest.xml`.
- **Error Logging & 3-Try Exponential Backoff**:
  - `registerTokenWithRetry` logs HTTP status code, message, error body, and exceptions via `Log.e` and retries up to 3 times (`1500ms`, `3000ms` delays).
- **Logout Token Capture & Cleanup**:
  - `AuthViewModel.logout` captures the Supabase Bearer token prior to `clearSession`, calls `unregister_push_token`, and calls `FirebaseMessaging.getInstance().deleteToken().await()`.
- **Debug UI in Settings > About**:
  - Gated by `BuildConfig.DEBUG` in `ProfileTabContent.kt` and `ChatAppearanceAndDebugModals.kt`, showing registration status, last 6 characters of token, last registration timestamp, and the `"Re-register push"` button.

---

## Not Verified

- **Live End-to-End FCM Delivery from Server**: Cannot be verified statically without triggering an actual push notification from the Supabase backend / Edge Function to a physical device or emulator with Google Play Services.
- **Deep-Link Navigation to Specific Chat on Push Tap**: While `PushNotificationManager.showPushNotification` copies all `data` keys (`kind`, `sender_id`, `conversation_id`, `group_id`) and `"from_push" = true` into the `MainActivity` `Intent` (`PushNotificationManager.kt:475–478`), `MainActivity.onCreate` / `onNewIntent` currently only inspects `intent.getStringExtra("navigate_to")` and `intent.data` URI (`MainActivity.kt:36–38, 488–491`) rather than routing directly based on `from_push` / `conversation_id` / `group_id` extras.

---

## Findings

1. **Redundant Sequential RPC Calls on Cold Start / Resume (Low Severity)**:
   - Because `ProcessLifecycleOwner.onStart`, `MainActivity` `ON_RESUME`, `MainActivity` `LaunchedEffect(authState)`, and `AuthViewModel.startSessionListener` all trigger `registerCurrentToken` on cold start, `register_push_token` is called 3–4 times sequentially (serialized by `registrationMutex`). While safe and idempotent, a short debounce (e.g., skipping if the same token was already registered for the same user within the last few seconds unless forced) would eliminate redundant network calls.
2. **Push Tap Intent Extras Routing (`from_push`, `conversation_id`, `group_id`) (Medium Severity)**:
   - `PushNotificationManager.showPushNotification` attaches `data` extras (`kind`, `sender_id`, `conversation_id`, `group_id`) and `"from_push" = true` to the `MainActivity` `PendingIntent`, and `FunkyFirebaseMessagingService.onMessageReceived` drains the inbox when the push arrives. However, when the user taps the system notification, `MainActivity` does not currently read `intent.getBooleanExtra("from_push", false)`, `conversation_id`, or `group_id` to automatically open the specific DM or group conversation or trigger an additional inbox drain on tap.
3. **Logout Coroutine Scope Lifecycle (Low Severity)**:
   - In `AuthViewModel.logout` (`AuthViewModel.kt:2199`), `PushNotificationManager.unregisterCurrentToken` is launched on `viewModelScope.launch(Dispatchers.IO)` and inside `PushNotificationManager.unregisterCurrentToken` it switches to `withContext(Dispatchers.IO)`. Since `AuthViewModel` remains alive across screen transitions in `MainActivity`, `viewModelScope` is not cancelled on logout, so the call completes normally. However, launching on `PushNotificationManager`'s application-level `scope` would make it resilient even if the `Activity`/`ViewModel` were destroyed immediately during logout.

# FunkyTalk QA & Pre-Launch Engineering Notes

## 1. Debug-Only Test Vault Password
- Define `QA_TEST_VAULT_PASSWORD=...` inside your local `.env` file (or AI Studio Secrets panel) to enable the **"Use QA Test Password"** quick-fill button on debug vault unlock screens (`VaultSecurityManager.getQaTestVaultPassword()`).
- Release builds hardcode `BuildConfig.QA_TEST_VAULT_PASSWORD = ""` and ignore `.env` entries so no test secret or shortcut ever ships in release APKs.

## 2. Logcat Redaction & Verification
- `SupabaseClient` only attaches `HttpLoggingInterceptor` when `BuildConfig.DEBUG == true`, capped at `Level.HEADERS` with `redactHeader` for `Authorization`, `apikey`, `Cookie`, and `Set-Cookie`. Release builds perform zero OkHttp logging.
- `app/proguard-rules.pro` strips `android.util.Log.d`, `android.util.Log.v`, and `isLoggable` calls in R8 release builds.
- Safe QA verification command on a connected device (checks only HTTP status/tag summaries):
  ```bash
  adb logcat -s OkHttp:D SupabaseSession:D PushNotificationMgr:D HomeScreen:D
  ```
- Confirm in a release build (`assembleRelease`) that `OkHttp` emits 0 lines and no `eyJ`, `Bearer`, `apikey`, or raw error bodies appear in logcat.

## 3. Session Refresh & Edge Function Deployment
- Client refreshes proactively at `exp - 5 minutes`, serializes concurrent callers under one `Mutex` (`SupabaseSessionManager`), retries once on HTTP 401 (`SupabaseClient`), and retries HTTP 500 from `/functions/v1/verify-firebase-token` with exponential backoff (`500ms`, `1000ms`, `2000ms`).
- Deploy the updated `/supabase/functions/verify-firebase-token/index.ts` before release testing:
  ```bash
  supabase functions deploy verify-firebase-token
  ```
- Required Edge Function secrets: `SUPABASE_URL`, `SUPABASE_SERVICE_ROLE_KEY`, `SUPABASE_ANON_KEY`, and optionally `FIREBASE_PROJECT_ID` (defaults to `funkytalk-f1aae`) / `FIREBASE_API_KEY`.

## 4. Firebase App Check Enforcement
- `AppCheckInstaller` installs `DebugAppCheckProviderFactory` in debug builds and `PlayIntegrityAppCheckProviderFactory` in release builds inside `FunkyTalkApplication.onCreate()`.
- For debug QA builds, copy the App Check debug secret UUID printed once at cold start in logcat (`DebugAppCheckProvider`) and register it in **Firebase Console -> App Check -> Apps -> Manage debug tokens**.
- For Play Store release builds, register the Play App Signing SHA-256 certificate in Firebase Console -> Project Settings and enable App Check enforcement on Firebase services once tested.

## 5. Package / Namespace Rename TODO
- `applicationId` is `"com.funkytalk.app"` while Kotlin sources remain under `namespace = "com.example"` to avoid massive source-tree churn before first release.
- **Post-MVP TODO**: Migrate `namespace` in `app/build.gradle.kts` and move Kotlin package declarations from `com.example.*` to `com.funkytalk.app.*` in a dedicated refactor PR, updating `AndroidManifest.xml`, ProGuard rules, and R imports together.

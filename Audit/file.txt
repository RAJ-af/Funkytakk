# PART 3 AUDIT
# FUNKYTALK PUSH NOTIFICATION AUDIT — PART 3: SUPABASE RPC + RLS + DATABASE SECURITY

---

## Schema

### 1. Local Repository SQL Inspection
A full search of all SQL and migration files in the repository (`supabase_schema.sql`, `supabase/migrations/20260917000000_secure_token_bridge_and_roles.sql`, `supabase/migrations/20260920000000_speaker_requests_and_mic_policy.sql`, `supabase/migrations/20260925000000_voice_room_lifecycle_and_speaker_cleanup.sql`) confirms that the DDL for `push_tokens` is hosted solely on the remote Supabase instance and is not checked into the repository.

| Schema Attribute | Value / Status | Source / Evidence |
| :--- | :--- | :--- |
| **Table Name** | `push_tokens` (referenced in system specification; no direct table queries in `SupabaseApi.kt`) | Client uses RPCs only (`SupabaseApi.kt:1065, 1072`) |
| **Columns & Types** | **NOT VERIFIED FROM AVAILABLE PROJECT SOURCES** (Inferred from RPC inputs: `user_id uuid`, `token text`, `platform text`, timestamps) | `RegisterPushTokenRequest` (`SupabaseApi.kt:1789–1792`) |
| **Primary Key** | **NOT VERIFIED FROM AVAILABLE PROJECT SOURCES** | No DDL in `supabase_schema.sql` or `supabase/migrations/` |
| **Foreign Keys** | **NOT VERIFIED FROM AVAILABLE PROJECT SOURCES** | No DDL in `supabase_schema.sql` or `supabase/migrations/` |
| **Unique Constraints** | **NOT VERIFIED FROM AVAILABLE PROJECT SOURCES** | No DDL in `supabase_schema.sql` or `supabase/migrations/` |
| **Indexes** | **NOT VERIFIED FROM AVAILABLE PROJECT SOURCES** | No DDL in `supabase_schema.sql` or `supabase/migrations/` |
| **Timestamps** | **NOT VERIFIED FROM AVAILABLE PROJECT SOURCES** | No DDL in `supabase_schema.sql` or `supabase/migrations/` |
| **`user_id` Column** | **NOT VERIFIED FROM AVAILABLE PROJECT SOURCES** | Not passed in client RPC body (`RegisterPushTokenRequest`) |
| **`token` Column** | Passed as `p_token: String` | `SupabaseApi.kt:1790, 1796` |
| **`platform` Column** | Passed as `p_platform: String = "android"` | `SupabaseApi.kt:1791` |
| **Device Information** | None sent by client (no `device_id`, model, or app version in RPC body) | `SupabaseApi.kt:1789–1797` |

---

## `register_push_token`

### 1. Client-Side Contract (`SupabaseApi.kt:1065–1070`, `1789–1792`)
```kotlin
@POST("rest/v1/rpc/register_push_token")
suspend fun registerPushToken(
    @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
    @Header("Authorization") auth: String,
    @Body body: RegisterPushTokenRequest
): Response<ResponseBody>

@JsonClass(generateAdapter = true)
data class RegisterPushTokenRequest(
    val p_token: String,
    val p_platform: String = "android"
)
```

### 2. Audit Verification Matrix
| Check | Status | Evidence |
| :--- | :--- | :--- |
| **Arguments** | **VERIFIED (Client)**: `(p_token: String, p_platform: String = "android")`. Does **not** accept `p_user_id`. | `SupabaseApi.kt:1789–1792`, `PushNotificationManager.kt:307–310` |
| **Client Cannot Specify Another User's `user_id`** | **VERIFIED (Client Payload)**: `RegisterPushTokenRequest` contains only `p_token` and `p_platform`. Caller identity is transmitted solely via the `Authorization: Bearer <jwt>` header (`PushNotificationManager.kt:306`). | `SupabaseApi.kt:1068, 1789–1792` |
| **Server-Side `auth.uid()` Binding** | **NOT VERIFIED FROM AVAILABLE PROJECT SOURCES** | SQL function body is not present in repository files. |
| **Token Ownership Reassignment / Upsert Behavior** | **NOT VERIFIED FROM AVAILABLE PROJECT SOURCES** | SQL `ON CONFLICT` clause is not present in repository files. |
| **`SECURITY DEFINER` / `SECURITY INVOKER`** | **NOT VERIFIED FROM AVAILABLE PROJECT SOURCES** | SQL function definition is not present in repository files. |
| **`search_path` Hardening (`SET search_path = public`)** | **NOT VERIFIED FROM AVAILABLE PROJECT SOURCES** | SQL function definition is not present in repository files. |
| **Privileges & RLS Interaction** | **NOT VERIFIED FROM AVAILABLE PROJECT SOURCES** | SQL function definition is not present in repository files. |

---

## `unregister_push_token`

### 1. Client-Side Contract (`SupabaseApi.kt:1072–1077`, `1795–1797`)
```kotlin
@POST("rest/v1/rpc/unregister_push_token")
suspend fun unregisterPushToken(
    @Header("apikey") apiKey: String = BuildConfig.SUPABASE_ANON_KEY,
    @Header("Authorization") auth: String,
    @Body body: PushTokenRequest
): Response<ResponseBody>

@JsonClass(generateAdapter = true)
data class PushTokenRequest(
    val p_token: String
)
```

### 2. Audit Verification Matrix
| Check | Status | Evidence |
| :--- | :--- | :--- |
| **Arguments** | **VERIFIED (Client)**: `(p_token: String)`. Does **not** accept `p_user_id`. | `SupabaseApi.kt:1795–1797`, `PushNotificationManager.kt:423` |
| **Anonymous Execution Blocked on Client** | **VERIFIED (Client Guard)**: `PushNotificationManager.unregisterCurrentToken` (`L405–410, 418`) explicitly filters out `BuildConfig.SUPABASE_ANON_KEY` and skips the RPC if no authenticated user JWT is available. | `PushNotificationManager.kt:405–418` |
| **Only Own Token Removed (`WHERE user_id = auth.uid() AND token = p_token`)** | **NOT VERIFIED FROM AVAILABLE PROJECT SOURCES** | SQL function body is not present in repository files. |
| **Another User's Token Cannot Be Deleted** | **NOT VERIFIED FROM AVAILABLE PROJECT SOURCES** | Depends on whether server SQL filters `DELETE FROM public.push_tokens WHERE token = p_token AND user_id = auth.uid()`. |
| **Server-Side Anonymous Execution Blocked** | **NOT VERIFIED FROM AVAILABLE PROJECT SOURCES** | Depends on `REVOKE EXECUTE ON FUNCTION public.unregister_push_token FROM anon` or `auth.uid() IS NOT NULL` check in SQL. |

---

## RLS

### Row Level Security Policies on `public.push_tokens`
**NOT VERIFIED FROM AVAILABLE PROJECT SOURCES**

Because the SQL migration creating `public.push_tokens` and its RLS policies is not checked into `supabase_schema.sql` or `supabase/migrations/`, none of the table-level RLS policies can be verified from local repository sources:

| Conceptual RLS Test | Expected | Actual Verification Status |
| :--- | :--- | :--- |
| **User A → `SELECT` User B token** (`GET /rest/v1/push_tokens`) | BLOCKED | **NOT VERIFIED FROM AVAILABLE PROJECT SOURCES** |
| **User A → `INSERT` token for User B** (`POST /rest/v1/push_tokens`) | BLOCKED | **NOT VERIFIED FROM AVAILABLE PROJECT SOURCES** |
| **User A → `UPDATE` User B token** (`PATCH /rest/v1/push_tokens`) | BLOCKED | **NOT VERIFIED FROM AVAILABLE PROJECT SOURCES** |
| **User A → `DELETE` User B token** (`DELETE /rest/v1/push_tokens`) | BLOCKED | **NOT VERIFIED FROM AVAILABLE PROJECT SOURCES** |

*(Note: The Android client never queries `/rest/v1/push_tokens` directly; all client operations go through `/rest/v1/rpc/register_push_token` and `/rest/v1/rpc/unregister_push_token`. However, PostgREST exposes all tables in the `public` schema to direct REST requests unless RLS is enabled and restrictive policies are applied.)*

---

## RPC Security

1. **Client-Side Parameter Integrity (`VERIFIED`)**:
   - Neither `RegisterPushTokenRequest` (`p_token`, `p_platform`) nor `PushTokenRequest` (`p_token`) includes a `user_id` field (`SupabaseApi.kt:1789–1797`).
   - Both RPC calls pass `Authorization: Bearer <accessToken>` where `<accessToken>` is minted by the `verify-firebase-token` Edge Function (`SupabaseSessionManager.kt:222–239`) and signed with the project's Supabase JWT secret.
   - Both `registerTokenWithRetry` (`PushNotificationManager.kt:291–301`) and `unregisterCurrentToken` (`PushNotificationManager.kt:405–418`) explicitly reject `BuildConfig.SUPABASE_ANON_KEY`.
2. **Server-Side Function Security (`NOT VERIFIED FROM AVAILABLE PROJECT SOURCES`)**:
   - Whether `register_push_token` and `unregister_push_token` run as `SECURITY DEFINER` with a pinned `SET search_path = public`, validate `auth.uid() IS NOT NULL`, and scope `DELETE` to `WHERE user_id = auth.uid() AND token = p_token` is **NOT VERIFIED FROM AVAILABLE PROJECT SOURCES**.

---

## Grants

**NOT VERIFIED FROM AVAILABLE PROJECT SOURCES**

| Role | Expected Privilege on `push_tokens` Table | Expected Privilege on `register_push_token` / `unregister_push_token` RPCs | Verification Status |
| :--- | :--- | :--- | :--- |
| `anon` | `NONE` (Revoked) | `NONE` (Revoked) | **NOT VERIFIED FROM AVAILABLE PROJECT SOURCES** |
| `authenticated` | `NONE` (if RPCs are `SECURITY DEFINER`) or RLS-restricted `SELECT/INSERT/UPDATE/DELETE` | `EXECUTE` | **NOT VERIFIED FROM AVAILABLE PROJECT SOURCES** |
| `service_role` | `ALL` (for server push dispatch & cleanup) | `EXECUTE` | **NOT VERIFIED FROM AVAILABLE PROJECT SOURCES** |

---

## Privacy

### Token Exposure Audit Across All Channels

| Channel | Exposure Status | Exact Location & Behavior | Severity |
| :--- | :--- | :--- | :---: |
| **Application Logs (`PushNotificationManager` / `FunkyFirebaseMessagingService`)** | **REDACTED (Last 6 chars only)** | `FunkyFirebaseMessagingService.kt:24`, `PushNotificationManager.kt:322, 427` log only `token.takeLast(6)` in `Log.d`. Error logs (`L281, 298, 327, 339`) do **not** print the token. | **SAFE** |
| **OkHttp Network Interceptor Logs (`SupabaseClient.kt`)** | **EXPOSED IN DEBUG BUILDS ONLY** | `SupabaseClient.kt:16–18` sets `HttpLoggingInterceptor.Level.BODY` when `BuildConfig.DEBUG == true` (and `Level.NONE` in release builds). In debug builds, the full JSON request body `{"p_token":"<full_token>","p_platform":"android"}` and `Authorization: Bearer <jwt>` header are printed to Logcat. | **LOW (Debug Only)** |
| **Analytics** | **NONE** | No analytics SDK or event tracking collects or transmits the FCM token. | **SAFE** |
| **URLs / Query Strings** | **NONE** | Token is sent exclusively inside the HTTP `POST` JSON request body (`@Body`), never in URL path or `@Query` parameters (`SupabaseApi.kt:1065–1077`). | **SAFE** |
| **Exceptions** | **NONE** | Exceptions caught in `PushNotificationManager.kt:337–345, 381–388, 432–434` log `e.message` without concatenating the full token. | **SAFE** |
| **Database Responses** | **NONE** | `registerPushToken` and `unregisterPushToken` return `Response<ResponseBody>` (`SupabaseApi.kt:1070, 1077`), which is not parsed or echoed into logs by `PushNotificationManager` on success. | **SAFE** |
| **Client UI (`ProfileTabContent.kt` / `ChatAppearanceAndDebugModals.kt`)** | **REDACTED (Last 6 chars, Debug Only)** | `PushDebugState.tokenSuffix` (`PushNotificationManager.kt:52`) takes only `.takeLast(6)` and renders `Token: …abcdef` only when `BuildConfig.DEBUG == true` (`ProfileTabContent.kt:1658, 1665`, `ChatAppearanceAndDebugModals.kt:376, 390`). | **SAFE** |
| **Local Disk Storage (`SharedPreferences`)** | **STORED IN PLAINTEXT PREFS** | `PushNotificationManager.kt:73–76, 195–200` stores the full `last_token` in standard `SharedPreferences` (`push_notification_debug_prefs.xml`) in the app's private data directory. | **LOW** |

---

## Cross-user Attack Analysis

| Attack Vector | Client-Side Mitigation | Server-Side Requirement | Audit Status |
| :--- | :--- | :--- | :--- |
| **1. Spoofing Another User's `user_id` During Registration** | `RegisterPushTokenRequest` omits `user_id`; user identity is carried only in the signed Supabase JWT (`Authorization: Bearer <jwt>`). | `register_push_token` must derive `user_id := auth.uid()` and reject `auth.uid() IS NULL`. | Client **VERIFIED**; Server SQL **NOT VERIFIED FROM AVAILABLE PROJECT SOURCES** |
| **2. Hijacking Another User's Push Notifications by Registering Their Token** | FCM tokens are random 163+ char secrets generated per app instance by Google Play Services and never exposed to other users via `profiles` or chat APIs. | `public.push_tokens` must have RLS enabled (`SELECT` blocked for other users) so User A cannot query `GET /rest/v1/push_tokens` to discover User B's token. | Client **VERIFIED**; Server RLS **NOT VERIFIED FROM AVAILABLE PROJECT SOURCES** |
| **3. Deleting Another User's Push Token via `unregister_push_token(p_token)`** | Client only passes its own FCM token (`PushNotificationManager.kt:411–423`). | `unregister_push_token` SQL must execute `DELETE FROM public.push_tokens WHERE token = p_token AND user_id = auth.uid()` so even if an attacker guessed a token, they cannot delete another user's row. | Server SQL **NOT VERIFIED FROM AVAILABLE PROJECT SOURCES** |
| **4. Shared-Device Token Hijack (User A Logout Fails → User B Logs In)** | On logout, client calls `unregister_push_token` and `deleteToken()`. However, as documented in Part 2, if User A logs out with an expired JWT or while offline, `Token A` is not deleted. | `register_push_token` must enforce `UNIQUE (token)` with `ON CONFLICT (token) DO UPDATE SET user_id = auth.uid(), updated_at = now()`, reassigning the device token to User B so User A stops receiving pushes on that device. | Server SQL **NOT VERIFIED FROM AVAILABLE PROJECT SOURCES** |

---

## Findings

### [CRITICAL] FINDING 1: Missing Version-Controlled SQL Migration for `push_tokens`, `register_push_token`, `unregister_push_token`, and RLS Policies
- **Files Inspected**: `supabase_schema.sql`, `supabase/migrations/*`
- **Problem**: The database schema for `public.push_tokens`, the RPC definitions for `public.register_push_token` and `public.unregister_push_token`, table RLS policies, and role grants (`anon`, `authenticated`, `service_role`) are **NOT VERIFIED FROM AVAILABLE PROJECT SOURCES** because no migration file for push notifications exists in the repository.
- **Impact**: Cannot statically verify that RLS is enabled on `public.push_tokens`, that `anon` cannot query or execute push RPCs, that `unregister_push_token` scopes deletion to `user_id = auth.uid()`, or that `register_push_token` upserts on `UNIQUE (token)` to prevent cross-user leakage on shared devices.

### [LOW] FINDING 2: `HttpLoggingInterceptor.Level.BODY` Logs Full `p_token` and `Authorization` JWT in Debug Builds
- **File**: `app/src/main/java/com/example/SupabaseClient.kt` (`L16–18`)
- **Problem**: In `BuildConfig.DEBUG` builds, `HttpLoggingInterceptor` logs full HTTP request headers (`Authorization: Bearer <jwt>`, `apikey`) and bodies (`{"p_token":"<full_fcm_token>","p_platform":"android"}`) to Android Logcat without `redactHeader("Authorization")` or `redactHeader("apikey")`.
- **Impact**: No exposure in production release builds (`Level.NONE`), but exposes raw JWTs and full FCM tokens in debug Logcat captures.

---

## Release Blockers

1. **Check In & Verify the Supabase Push Token SQL Migration (`supabase/migrations/`)**:
   - Must verify that `public.push_tokens` has:
     - `UNIQUE (token)` constraint so a physical device token can only belong to one active `user_id` at a time (`ON CONFLICT (token) DO UPDATE SET user_id = auth.uid(), platform = p_platform, updated_at = now()`).
     - `ALTER TABLE public.push_tokens ENABLE ROW LEVEL SECURITY;` with zero permissive policies for `anon` and strict `auth.uid() = user_id` policies for `authenticated`.
     - `unregister_push_token(p_token text)` scoped strictly to `WHERE token = p_token AND user_id = auth.uid()`.
     - `REVOKE ALL ON TABLE public.push_tokens FROM anon;` and `REVOKE EXECUTE ON FUNCTION public.register_push_token, public.unregister_push_token FROM anon;`.

---

## Verified

- **Client RPC Endpoints & Request Bodies (`SupabaseApi.kt:1065–1077, 1789–1797`)**:
  - `POST rest/v1/rpc/register_push_token` sends only `{"p_token": "<token>", "p_platform": "android"}`.
  - `POST rest/v1/rpc/unregister_push_token` sends only `{"p_token": "<token>"}`.
  - Client cannot pass an arbitrary `user_id` in either RPC request body.
- **No Direct Table Access from Client**:
  - Zero PostgREST calls to `/rest/v1/push_tokens` exist anywhere in the Android codebase.
- **Client Anonymous Key Rejection (`PushNotificationManager.kt:291–301, 405–418`)**:
  - Both registration and unregistration refuse to execute if the only available token is `BuildConfig.SUPABASE_ANON_KEY`.
- **Token Masking in App Logs & UI (`PushNotificationManager.kt:52, 322, 427`, `FunkyFirebaseMessagingService.kt:24`, `ProfileTabContent.kt:1665`)**:
  - Only the last 6 characters (`token.takeLast(6)`) are displayed in the debug UI and `PushNotificationManager` / `FunkyFirebaseMessagingService` logs.

---

## Not Verified

- **`public.push_tokens` Table DDL** (columns, types, primary key, foreign keys, unique constraints, indexes, timestamps): **NOT VERIFIED FROM AVAILABLE PROJECT SOURCES**
- **`public.register_push_token` & `public.unregister_push_token` SQL Function Bodies** (`auth.uid()` enforcement, `ON CONFLICT` upsert target, `SECURITY DEFINER` / `INVOKER`, `search_path`): **NOT VERIFIED FROM AVAILABLE PROJECT SOURCES**
- **Row Level Security (RLS) Policies on `public.push_tokens`**: **NOT VERIFIED FROM AVAILABLE PROJECT SOURCES**
- **Database Role Grants (`anon`, `authenticated`, `service_role`)**: **NOT VERIFIED FROM AVAILABLE PROJECT SOURCES**

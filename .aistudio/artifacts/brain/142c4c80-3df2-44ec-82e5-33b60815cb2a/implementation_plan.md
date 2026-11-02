# Implementation Plan - Account Deletion Flow (Android) [FINAL APPROVED]

Build the complete, privacy-compliant account deletion flow in the Funkytalk Android app (Kotlin, Jetpack Compose, Firebase Auth + Supabase backend).

---

## Approved Architectural & Security Rules

1. **Selective Cleanup & Persistent Local Erase:**
   - In `finalizeDeletionAndSignOut()`, clear only session/auth keys (`access_token`, `refresh_token`, `user_id`, cached profile). **NEVER** delete `DeviceInfoProvider.deviceId`.
   - Persist `pending_erase_local_chats` flag immediately when the request is confirmed. If the app is killed before the user taps Done, the wipe will execute on the next startup/logout.
   - Delete Tink keysets, Android Keystore entries, and local Room chat database **ONLY** if `eraseLocalChats == true`.
2. **Device Revocation Suppression:**
   - The server marks all user devices as revoked when `request-account-deletion` succeeds.
   - While `deletionReceipt != null`, suppress any "logged out from another device" listener, device revocation handlers, and auth-state redirects so the receipt screen is never interrupted or prematurely dismissed.
3. **Robust Error Body Parsing:**
   - Parse `response.errorBody()?.string()` as JSON (`{"error": "..."}`) on 4xx responses to reliably detect `reauth_required`, `too_many_attempts`, `invalid_or_expired_token`, `account_not_found`, etc.
4. **RPC Contract:**
   - Both `get_my_deletion_status` and `cancel_my_account_deletion` send an empty JSON body `{}` (`@Body body: Map<String, Any> = emptyMap()`) and require headers `@Header("apikey")` and `@Header("Authorization")`.
5. **Receipt Back Navigation:**
   - Use `BackHandler` on `AccountDeletionReceiptScreen` to treat system back like tapping **Done**, safely completing finalization and preventing navigation back into the deletion form.
6. **Dynamic Grace Period & Status Gate:**
   - Show "a few days" pre-request, and exact formatted local date/time post-request.
   - If `get_my_deletion_status()` fails on login or app start, block app entry with a **Retry / Log Out** gate.

---

## Files to Create & Update

1. `app/src/main/java/com/example/util/DeviceInfoProvider.kt` (Verify/Create device ID management)
2. `app/src/main/java/com/example/SupabaseApi.kt` (Add deletion request/response models & endpoints)
3. `app/src/main/java/com/example/AuthViewModel.kt` (Add deletion status state, RPC calls, reauth & deferred finalization)
4. `app/src/main/java/com/example/ui/screens/profile/AccountDeletionScreen.kt` (Create plain-language deletion screen with DELETE confirmation, reason chips, switch & reauth dialog)
5. `app/src/main/java/com/example/ui/screens/profile/AccountDeletionReceiptScreen.kt` (Create receipt screen with copy/share, formatted local date, and Done/Back finalization)
6. `app/src/main/java/com/example/ui/screens/profile/AccountDeletionGateScreen.kt` (Create grace-period / verification failure gate screen)
7. `app/src/main/java/com/example/ui/screens/home/tabs/ProfileTabContent.kt` (Add Delete Account entry point under Profile Settings)
8. `app/src/main/java/com/example/MainActivity.kt` (Wire deletion gate and receipt screens)
9. `docs/BACKEND.md` (Append complete account deletion specification)

---

## Verification
- Run `compile_applet` to confirm zero compilation errors.
- Test error paths, reauth, receipt display, and cancel deletion flow.

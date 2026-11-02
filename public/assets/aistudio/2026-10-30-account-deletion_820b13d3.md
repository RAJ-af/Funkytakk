# 2026-10-30 Account deletion (everything deleted, proof record kept)

Backend status: LIVE
- migration `20261030020000_account_deletion.sql` (+ the patch included in that file)
- Edge Functions: `request-account-deletion`, `process-account-deletions` (cron, every 10 min), `lookup-deletion-proof` (staff), `upload-to-appwrite` (now tracks files)

How it works (so the app does the right thing)
1. The user taps Delete account, signs in again (password / Google), and the app calls `request-account-deletion` with a FRESH Firebase ID token. The server checks that the sign-in happened in the last 5 minutes.
2. The server writes the permanent PROOF record (receipt id, time, device, IP, how the user re-authenticated, one-way hash of the email, username, no content), hides the profile, logs all devices out and emails a receipt.
3. After the grace period (default 7 days, `DELETION_GRACE_DAYS`) the cron worker deletes EVERYTHING: Appwrite files (avatar, banner, chat media), Cloudinary (Moments), Supabase Storage, the Firebase login, all database rows (chats, followers, moments, wallet, badges, devices, tokens...), the Supabase auth user, then scans all tables to prove nothing is left, and emails a "deleted" confirmation. Other people's records that must survive (gift history, reports) are kept without any link to the deleted person.
4. If the user signs in again during the grace period, the app offers "Cancel deletion".

Paste the block below into Google AI Studio.

```text
You are working on the Funkytalk Android app (Kotlin, Jetpack Compose, Firebase Auth + Supabase). Build the account deletion flow. The server is live; do not change server assumptions. No mock data.

## 1. Entry point
Settings -> Account -> "Delete account" (red). Opens a screen that explains, in plain language:
 - What will be deleted permanently: profile, photos and banner, chats and media, moments, followers, wallet/coins and badges, logged-in devices, and the login itself.
 - What we keep: only a minimal proof record of the deletion (receipt number, date, device, a one-way hash of the email; no messages, no photos). Say it exactly like that.
 - Grace period: "Your account is hidden immediately and deleted for good after N days. Sign in again during that time to cancel." N comes from the server answer (grace_days); before the call show "a few days".
 - Optional reason chips (not required) and a text field where the user must type DELETE to enable the red "Delete my account" button.
 - A switch "Also erase my chats stored on this phone now (cannot be undone, even if I cancel later)". Default OFF.

## 2. Re-authentication (required)
Before calling the server the user must sign in again:
 - Email/password users: dialog asking for the password, then FirebaseUser.reauthenticate(EmailAuthProvider.getCredential(email, password)).
 - Google users: reauthenticate with the Google credential (Credential Manager / existing Google sign-in flow).
 - Then force a fresh token: firebaseUser.getIdToken(true).await().token. (The server requires that the sign-in happened within the last 5 minutes.)
 - Wrong password / cancelled sign-in -> inline error, nothing is sent.

## 3. Call the server
POST https://<project>.supabase.co/functions/v1/request-account-deletion (use the same base URL/client style as the existing verify-firebase-token call; this function does NOT need a Supabase Authorization header) with JSON:
 {"firebase_token": "<fresh id token>", "confirm": true, "reason": "<optional, max 300 chars>", "device": {"device_id": "<DeviceInfoProvider.deviceId>", "name": "<Build.MANUFACTURER Build.MODEL>"}}
Responses:
 - 200 {"success": true, "receipt_id": "...", "scheduled_for": "<ISO time>", "grace_days": 7, "already_requested": true|absent}
 - 401 {"error": "reauth_required"} -> go back to step 2 and ask to sign in again
 - 401 invalid_or_expired_token -> refresh the token once, retry once, else show an error
 - 429 too_many_attempts -> "Too many attempts, try again later"
 - 404 account_not_found, 400 confirmation_required, 5xx -> short error message
Log the status and error code with Log.d (never the token).

## 4. After success
 - Show a receipt screen: "Deletion scheduled", the receipt number (selectable, with a Copy button and a Share button), the date from scheduled_for in the user's local time, and a note "We also emailed you this receipt. To cancel, sign in before that date."
 - Then sign out of Firebase and Supabase, delete the FCM token (FirebaseMessaging.getInstance().deleteToken()), clear the session and caches (Coil/image cache, EncryptedSharedPreferences session data). If the switch from step 1 was ON, also wipe the local encrypted chat database, the vault keys, the Tink keysets and the Keystore entries. If it was OFF, keep the local chat data and the vault untouched.
 - Navigate to the Welcome screen and clear the back stack.

## 5. Signing in during the grace period
After EVERY successful sign-in (email/password and Google), right after the Supabase session exists, call rpc get_my_deletion_status() (no parameters). It returns jsonb: {"pending": false} or {"pending": true, "receipt_id": "...", "status": "scheduled"|"purging"|"failed", "scheduled_for": "<ISO>", "can_cancel": true|false}.
 - If pending = true and can_cancel = true: show a full-screen "Your account is scheduled for deletion" with the date and the receipt number and two buttons: "Cancel deletion" -> rpc cancel_my_account_deletion() -> {"success": true} then continue into the app normally and show a toast "Deletion cancelled"; and "Log out". Do not let the user into the app before choosing.
 - If pending = true and can_cancel = false (status purging/failed): show "Your account is being deleted" with only a "Log out" button.
 - If pending = false: continue as usual.
Also call get_my_deletion_status() on app start when a session exists.

## 6. Cleanup
 - Remove any existing client-side FirebaseAuth.currentUser.delete() / account deletion code. The server deletes the Firebase login.
 - Make sure no screen still shows a deleted or hidden profile (the server hides it from Discover immediately).

## 7. Test checklist (report each result)
 a) Wrong password on re-auth -> error, nothing sent.
 b) Happy path with a throwaway account -> receipt screen, receipt email arrives, app returns to Welcome.
 c) Sign in again before the deadline -> "scheduled for deletion" screen -> Cancel -> normal app, profile visible in Discover again.
 d) Delete, wait for the deadline (the owner can set the grace period to 0 for testing) -> "account deleted" email arrives, signing in with the same email fails (no such user), the profile and photos are gone.
 e) Sending the request with a token older than 5 minutes -> reauth_required handled.

Make sure the project compiles and list every file changed.
```

## Add to docs/BACKEND.md

- Tables: `account_deletions` (permanent proof record: receipt id, supabase_user_id, firebase_uid, email_hash = HMAC-SHA256 of the lower-case email, username, account_created_at, status scheduled|cancelled|purging|completed|needs_review|failed, request time/IP/country/user agent/device, auth_provider and reauth_auth_time, was_banned, had_active_suspension, steps, deleted_counts, residue, attempts, last_error, email flags); `media_files` (inventory of Appwrite/Cloudinary files). Both: RLS on, no policies (service role only).
- Flow: `request-account-deletion` (needs a Firebase ID token with auth_time within 5 minutes) -> status `scheduled`, profile hidden (`profiles.deletion_scheduled_at`, excluded from `discoverable_profiles`), devices revoked, receipt email -> after the grace period `process-account-deletions` (pg_cron every 10 min, header `x-push-secret`, same secret as send-push) runs: media (Appwrite, Cloudinary `moments/<uid>/`, Supabase Storage) -> Firebase Auth user (Identity Toolkit `accounts:delete`) -> `purge_user_data()` -> `auth.admin.deleteUser` -> `find_user_residue()` (must be `{}`; otherwise status `needs_review`) -> completion email. Steps are repeatable; failures set status `failed` and are retried (max 10 attempts); nothing is marked completed unless Firebase and the database are done.
- Kept on purpose: gift history without sender/recipient, reports without reporter/reported identity, room end summaries without host identity, the proof record.
- Secrets used: `FCM_SERVICE_ACCOUNT_JSON` (must be the Firebase Admin SDK service account of project funkytalk-f1aae with permission to delete Auth users), `BREVO_API_KEY`, `APPWRITE_*`, `CLOUDINARY_*`, optional `DELETION_GRACE_DAYS` (default 7, 0 = delete at the next cron run), optional `DELETION_PROOF_PEPPER` (set once and never change it, otherwise old email hashes can no longer be matched; the service role key is used if it is not set).
- Staff lookup: Edge Function `lookup-deletion-proof` (admin/superadmin JWT) with {email | receipt_id | username | user_id}.
- App RPCs: `get_my_deletion_status()`, `cancel_my_account_deletion()`.
- Files in Appwrite are public by URL (`read("any")`); only encrypted chat media is acceptable there.
- Known limits: `room_reports.message_snapshot` (JSON) may still contain old chat text of other users; the residue scan only checks text/uuid columns, not JSON content.

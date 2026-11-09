# Gemini prompt: admin panel report workflow + "Account under review" screen (2026-11-09)

Attach the screenshot `45042.png` (the "Your Account is Under Review" design) in Gemini together with the prompt below.

```
You are working on the Funkytalk Android app (Kotlin, Jetpack Compose, Material 3, Firebase Auth login, Supabase backend, project dulpqochkxtamqztauea). TASK: build the admin-panel report workflow and the "Account under review" screen. The backend is already built and tested. Do NOT change or guess anything in the backend. If something is missing, tell me.

STEP 1: Find the existing Moderation Center / admin report screens and the code that calls admin_apply_moderation_action, admin_dismiss_report, admin_get_report_queue, admin_get_report_detail. List the files first. Do not edit yet.

IMPORTANT CHANGE: admin_apply_moderation_action and admin_dismiss_report now FAIL with take_charge_first unless the same admin first called admin_take_charge on that report. Update the existing calls.

A) ADMIN: ALERTS
- Create a notification channel with id "staff_reports", high importance.
- FCM data payload: kind="staff_report", event, report_id. Tapping the push opens that report's detail screen (if report_id is empty, open the queue).
- Events: new_report, report_reopened, report_open_again, reopen_challenged, owner_decision, report_freed.
- The same events are rows in the notifications table (type = "staff_report", action_type = event, report_id). Show them in the admin inbox.

B) ADMIN: QUEUE
- RPC admin_get_report_queue(p_status, p_limit, p_offset). Tabs: "Free" (p_status="pending"), "Taken" (p_status="in_review"), "Mine" (p_status="mine").
- Row fields: report_id, created_at, context_type, category, status, reported_name, reported_score, reports_against_pending, claimed_by_name, claim_expires_at, is_mine, reopen_status.
- Every admin sees every report. A report claimed by someone else shows "In charge: <claimed_by_name>" with the time left.

C) ADMIN: REPORT DETAIL (admin_get_report_detail returns JSON: report, claim, reporter, reported, reopens, events)
- If claim.active is false and the report status is pending: show a "Take charge" button -> admin_take_charge(p_report_id). The report is reserved for that admin for 48 hours. Show a countdown from claim.expires_at.
- If claim.is_mine: show the action buttons: Warning, Mute, Suspend (1-90 hours), Dismiss, Release (admin_release_report), and "Message user".
  - Actions: admin_apply_moderation_action(p_report_id, p_target_user_id, p_action_type, p_duration_hours, p_reason) and admin_dismiss_report(p_report_id, p_reason).
  - "Message user" for investigation: use the existing RPC admin_send_support_message(p_user_id, p_message, p_report_id).
- If another admin is in charge: read-only, no action buttons.
- If the report is resolved (status "resolved", outcome not null) and it is not older than 30 days: show "Reopen report". It opens a dialog with a required brief (10 to 2000 characters) -> admin_reopen_report(p_report_id, p_brief).
- If reopens[] has an item with status "open" and original_admin is me: show the reopen brief and a "Challenge" button -> dialog with required brief (10 to 2000 characters) -> admin_challenge_reopen(p_reopen_id, p_brief). Say clearly in the dialog: "Your brief goes directly to the owner by email."
- If I am a superadmin (owner) and a reopen has status "open" or "challenged": show both briefs and two buttons -> owner_decide_dispute(p_reopen_id, p_decision, p_note) with p_decision "reopen_upheld" or "original_upheld".
- Show events[] as a simple timeline.
- Map these errors to friendly messages: not_authenticated, admin_required, take_charge_first, already_claimed, conflict_of_interest, report_closed, report_not_found, target_mismatch, brief_too_short, brief_too_long, report_not_resolved, reopen_window_closed, already_reopened, not_original_admin, not_open, owner_required, already_decided, reopen_not_found.

D) USER: "ACCOUNT UNDER REVIEW" (use the attached screenshot as the visual design: bunny mascot, shield, yellow button)
- Call RPC get_my_account_review() (no arguments). It returns {"under_review": true/false, "since": timestamp}. Call it on app start, on every app resume, and every 60 seconds while the lock screen is shown.
- If under_review is true: show the full-screen dialog from the screenshot. Texts: title "Your Account is Under Review", body "You can't access your account right now. Someone has reported your account, and our team is verifying it according to our community guidelines.", note "Our team is currently checking your account. We'll notify you once the review is complete.", button "Okay, I Understand".
- After the button, the app stays locked: no home, rooms, chats, moments. Show a simple locked screen with the mascot, the same short text, and ONE button "Contact support" that opens the existing support chat, because an admin may write to the user there during the investigation (send_support_message(p_message, p_report_id) is the existing user RPC).
- Never show who reported, the report id, the category, or any evidence.
- When under_review becomes false: unlock automatically and show a short message "Your account review is complete".

RULES: show each changed file in full. At the end, list what you could NOT verify (you cannot run the app). Do not build anything for the owner email (the backend sends it).
```

## Backend facts (for docs/BACKEND.md)
- New report -> every admin gets an inbox row + push (Edge Function `send-staff-push`). The reporter and the reported user never get it. Max 3 alerting reports per reporter per 10 minutes.
- `admin_take_charge` reserves a report for 48 h (`room_reports.status = in_review`, `claimed_by`, `claim_expires_at`). Cron `release-expired-report-claims` (every 10 min) frees it and alerts all admins. A claim that expired is also takeable immediately.
- While a report against a user is `in_review` and not expired, `get_my_account_review()` is true for that user. The lock is enforced by the app only; the server does not block other calls yet.
- `admin_reopen_report` (needs a brief, report resolved within 30 days) lifts the active restriction, gives the safety score back, frees the report, alerts the original admin and all other admins.
- `admin_challenge_reopen` (only the original admin) -> Edge Function `notify-owner-dispute` emails both briefs to `OWNER_EMAIL`.
- `owner_decide_dispute` (superadmin only): `original_upheld` re-applies the original action (suspend for the original number of hours from now) and re-applies the score penalty; `reopen_upheld` changes nothing more.
- `private.is_admin()` now includes `superadmin`.
- Admin cannot take charge of or reopen a report where they are the reporter or the reported user.

## One-time setup (owner)
1. Supabase > Edge Functions > Secrets: add `OWNER_EMAIL` = your inbox. (`BREVO_API_KEY` and `FCM_SERVICE_ACCOUNT_JSON` already exist.)
2. Make yourself superadmin (so you can decide disputes):
```sql
update public.user_roles set role = 'superadmin' where user_id = '6735c9a4-797c-4f04-86b9-0c58cb7701b5';
```
3. Add a second admin account to test the claim / reopen flow with two real phones.

## NOT tested
Real push delivery (FCM), the owner email (Brevo), the app screens, server-side blocking of a user under review, the existing app's admin screens after the take_charge_first change.

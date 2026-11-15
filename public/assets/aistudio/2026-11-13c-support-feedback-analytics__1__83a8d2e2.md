# Gemini prompt: 24/7 support chat, Feedback hub (bugs + feature requests), Crashlytics + Analytics

WORK IN ONE ANSWER: list the files you will touch (max 5 lines), then IMMEDIATELY implement. Show every changed file in full (for huge files show only the changed functions in full plus exact insert locations). At the end list what you could not verify.

Backend is ready in Supabase (do not change it). Call with `supabase.rpc('<name>', params)`, parameter names EXACTLY as written. Errors come back as a message string; map them with the table at the end.

## 1) Support chat is now open 24/7 for every user
Until now only banned/muted users or users whose chat was opened by an admin could write to support. Now every logged-in user can.
- `get_my_support_session()` -> `{ open:true, reason:'always'|'support_opened'|'appeal', until, conversation_id, unread, can_appeal }`. `open` is true for every logged-in user. Remove every UI condition that hides or disables the support chat when `open == false` / "support_chat_closed". Keep the appeal wording only when `reason == 'appeal'`.
- Send: `send_support_message(p_message, p_report_id)` (message max 1000 chars; `p_report_id` stays null for normal users). Existing read/mark-read calls stay unchanged.
- Add a clearly visible entry point: Profile (or the 3-dot/settings menu) -> "Help & Support" screen with 3 rows: "Chat with support" (shows an unread badge from `unread`), "Report a bug", "Request a feature" (see 2). Also show the unread badge on the Profile tab icon if you already do that for other badges.
- Replies from the team arrive in the same conversation (the existing admin inbox keeps working).

## 2) Feedback hub (beta): bug reports and feature requests with tracking
New screen "Feedback" with 3 tabs: **Feature requests** (public board), **My reports** (my bugs + my requests), and a "+" button to create one.

Create: `create_feedback(p_kind, p_title, p_body, p_app_version, p_device_info)`
- `p_kind`: `'bug'` or `'feature'`. `p_title` 3-80 chars (required), `p_body` max 1500.
- Fill `p_app_version` automatically (BuildConfig VERSION_NAME + VERSION_CODE) and `p_device_info` (`"<MANUFACTURER> <MODEL> / Android <RELEASE>"`, max 200 chars). The user does not type them. For bugs, show hint text: "What happened? What did you expect? Steps to reproduce".
- Bugs are PRIVATE (only the reporter and the team see them). Say so in the UI. Feature requests are public; the creator's vote is added automatically.
- Returns the created item (same shape as below).

Feature board: `get_feedback_board(p_kind: 'feature', p_status, p_sort, p_limit: 20, p_offset)`
- `p_sort`: `'top'` (most votes) or `'new'`. `p_status` optional filter: `open|planned|in_progress|done|declined|duplicate`; when null the server hides declined/duplicate. Show filter chips: All, Planned, In progress, Done.
- Vote: `toggle_feedback_vote(p_id)` -> `{voted, votes_count}` (optimistic update). Error `voting_closed` = request is done/declined: disable the button.

My reports: `get_my_feedback(p_limit: 30, p_offset)`.

Item shape:
`{ id, kind, title, body, status, votes_count, replies_count, created_at, updated_at, is_mine, has_voted, has_update, app_version, device_info, author:{id,username,name,avatar_url} }`
- Status chips with colors: open (grey), planned (blue), in_progress (orange), done (green), declined (red), duplicate (grey).
- `has_update == true` (only on my own items) = the team replied or changed the status since I last looked: show a dot. Show the total of such dots as a badge on the "My reports" tab and on the Help & Support row.

Detail: `get_feedback(p_id)` -> item + `replies:[{id, body, is_staff, created_at, is_mine, author:{id,name,avatar_url}|null}]` (oldest first). Show replies as a chat-like thread; `is_staff == true` is shown as "Funkytalk Team" (author is null). Call `mark_feedback_seen(p_id)` when the detail screen opens (clears the dot).
- Reply: `add_feedback_reply(p_id, p_body)` (max 1000). Only the owner and the team can reply; other users only vote.
- Delete own item: `delete_feedback(p_id)` in a 3-dot menu (only when `is_mine`).

## 3) Admin: Feedback inbox (only for staff; reuse the existing admin panel style)
- `admin_list_feedback(p_kind, p_status, p_limit: 30, p_offset)`: filter chips (All / Bugs / Features) + status chip. Items include `app_version` and `device_info` for staff. "open" items come first.
- Detail uses `get_feedback(p_id)` (works for staff on bugs too). Buttons: change status `admin_set_feedback_status(p_id, p_status, p_note)` (the optional `p_note` is posted as a team reply, so the user sees why), and a reply box using `add_feedback_reply`.

## 4) Firebase Crashlytics + Analytics (the app already uses Firebase Auth / FCM)
- Add `firebase-crashlytics` and `firebase-analytics` via the Firebase BoM, and the Crashlytics Gradle plugin (`com.google.firebase.crashlytics`). Do not add any new Firebase project; use the existing `google-services.json`.
- After login set `FirebaseCrashlytics.setUserId(<supabase user id>)` and `FirebaseAnalytics.setUserId(<same>)`; clear both on logout. Never send emails, names, messages or any text the user wrote.
- Crashlytics custom keys: `app_version`, `current_screen` (update on navigation), `build_type`.
- Record non-fatal errors with `recordException` for: Supabase RPC failures that are NOT validation errors (HTTP 5xx, unknown error text), Cloudinary upload failures (HTTP code + Cloudinary error message), and image/video decode failures. Wrap the existing repository error handling, do not duplicate toasts.
- Analytics events (names exactly): `screen_view` (auto + current_screen), `moment_posted` (param `kind`: text|photo|video), `moment_post_failed` (param `reason`), `moment_liked`, `moment_comment_added`, `correction_added`, `support_chat_opened`, `support_message_sent`, `feedback_created` (param `kind`), `feedback_voted`, `room_joined`, `room_left` (param `duration_sec`), `signup_completed`, `login_completed`. No free-text params.
- Add a Settings switch "Share anonymous usage data" (default ON for the beta). When OFF: `FirebaseAnalytics.setAnalyticsCollectionEnabled(false)` and `FirebaseCrashlytics.setCrashlyticsCollectionEnabled(false)`; persist it.
- Debug builds: set Crashlytics collection disabled so test crashes don't pollute the dashboard (`firebase_crashlytics_collection_enabled=false` in the debug manifest placeholder or in code).

## Error messages -> user text
| error | text |
|---|---|
| `not_authenticated` | Please log in again |
| `rate_limited` | You're going too fast. Try again in a few minutes |
| `empty_message` | Write a message first |
| `message_too_long`, `body_too_long`, `title_too_long` | Text is too long |
| `title_too_short` | Give it a short title (3+ letters) |
| `invalid_kind` | Choose Bug or Feature |
| `voting_closed` | Voting is closed for this request |
| `feedback_not_found` | This item is no longer available |
| `forbidden` | You can't do that |
| anything else | Something went wrong. Try again |

## After implementing, test (two accounts + one admin)
1. Normal user (no ban, no window): open Help & Support -> Chat with support -> send a message. The admin sees it in the inbox and replies; the user sees the reply.
2. User A: create a feature request and a bug. User B: sees only the feature on the board and votes it; B cannot see A's bug.
3. Admin: opens Feedback inbox, sets the feature to "Planned" with a note. User A sees the dot, opens it, sees "Planned" and the team note.
4. Crashlytics: force a test non-fatal (e.g. a hidden debug button) in a release build and check the Firebase console after a few minutes.

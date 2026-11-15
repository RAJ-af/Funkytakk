# Gemini prompt: username + profile edit limits (Edit Profile screen)

WORK IN ONE ANSWER: list the files you will touch (max 5 lines), then IMMEDIATELY implement. Show every changed file in full (huge files: only the changed functions plus exact insert locations). At the end list what you could not verify.

The backend now limits how often a user can change profile fields. It is enforced by the database for BOTH direct profile updates and the RPCs below, so the app must show the limits clearly and handle the errors. Do not change the backend.

## Limits (can be changed later in the database without an app update, so read them from the server, never hardcode them)
username 1 change / 30 days, display name (`profile_name`) 3 / 7 days, age 1 / 30 days, gender 1 / 30 days, avatar 10 / day, cover 10 / day, bio 20 / day, country 3 / day, city 5 / day. Languages, hobbies and the rest are not limited.
First-time setup is FREE: while `onboarding_complete = false` (onboarding flow) nothing is limited and nothing is counted. Limits start counting after `onboarding_complete` becomes true. So in the onboarding flow set username, name, age, gender, languages etc. and `onboarding_complete = true` in the same final request or earlier, never later.

## 1) Load the limits when Edit Profile opens
`get_profile_edit_limits()` -> array of `{ field, max_changes, window_hours, used, remaining, next_allowed_at }` (`next_allowed_at` is an ISO timestamp, only set when `remaining == 0`).
- For a field with `remaining == 0`: disable that input and show under it: "You can change this again on <next_allowed_at in the user's local date format>".
- For username show always: "You can change your username once every 30 days." (use `max_changes` and `window_hours` from the response to build the text, convert hours to days).
- Refresh the limits after every successful save.

## 2) Username change
- Username rules shown as hint text: 3-20 characters, lowercase letters a-z, digits, dot and underscore; no dot at the start/end and no two dots in a row. Force lowercase and trim while typing.
- Live check (debounce 400 ms, only when the text is valid length): `check_username_available(p_username)` -> `{ available, reason }`, reason: `ok`, `same`, `invalid_format`, `reserved`, `taken`, `recently_used`. Show green "Available" for `ok`; otherwise: `invalid_format` "Use 3-20 letters, numbers, dots or underscores", `reserved` "This name is not allowed", `taken` "Already taken", `recently_used` "This name was used recently by someone else", `same` "This is your current username". Disable Save unless `ok`.
- Before saving show a confirm dialog: "You can change your username only once every 30 days. Change to @<name>?"
- Save: `change_username(p_username)` -> `{ username, next_change_at }`. On success update the local profile and show "Next change possible on <next_change_at>".
- Do NOT update `username` through a direct profile PATCH anymore; always use this RPC.

## 3) Other profile fields
- Keep saving them as you do now (direct profile update), but send ONLY the fields that really changed (compare with the loaded profile). Never send `id`, `auth_user_id`, `created_at`, `deletion_scheduled_at` (the server ignores them) and never send `username` here.
- Avatar/cover: after a successful upload only update the URL if the limit allows (check `remaining` first, show the limit message before uploading to avoid wasted uploads).

## 4) Error handling
Direct updates fail with an error object `{ code, message, hint }`; RPC errors come as the message text.
- `message = "profile_limit_<field>"` (for example `profile_limit_username`, `profile_limit_profile_name`, `profile_limit_age`, `profile_limit_avatar_url`) and `hint` = the time when the next change is allowed (ISO-like text, parse it). Show: "You've reached the limit for <field label>. You can change it again on <hint as local date and time>." Then reload `get_profile_edit_limits()`.
- Other messages: `invalid_username` "Use 3-20 letters, numbers, dots or underscores"; `username_reserved` "This name is not allowed"; `username_recently_used` "This name was used recently by someone else"; `username_taken` "Already taken"; `not_authenticated` "Please log in again"; anything else "Something went wrong. Try again".
- Field labels: username "username", profile_name "display name", age "age", gender "gender", avatar_url "profile photo", cover_url "cover photo", bio "bio", country_code "country", city "city".

## After implementing, test with one account
1. New account in onboarding: set username, name, age freely, finish onboarding.
2. Edit Profile: change username once (works), try again (field disabled with the date).
3. Change display name 3 times (works), 4th time (limit message with a date).
4. Type `admin`, `ab`, `has space`, someone else's name: see the right message for each.

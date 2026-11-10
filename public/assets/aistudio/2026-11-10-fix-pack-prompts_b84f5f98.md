# Gemini prompts, 2026-11-10: fixes after the phone test

Run them ONE AT A TIME, in this order (A, then B, then C). Each prompt tells Gemini to IMPLEMENT right away. The earlier prompts said "do not edit yet" and had no second step, so Gemini only reviewed. That was a mistake in those prompts.

Backend facts used here (all live and tested): `admin_get_report_detail`, `report_user`, `report_group`, `admin_send_support_message(p_user_id, p_message, p_report_id, p_open_hours)`, `send_support_message(p_message, p_report_id)`, `get_my_support_session()`, `admin_close_support_chat(p_user_id)`, `mark_support_conversation_read()`.

---

## Prompt A: admin report detail must show the proof + "Message User" opens a support chat window

```
You are working on the Funkytalk Android app (Kotlin, Jetpack Compose, Material 3, Supabase backend). The backend is finished and tested: do NOT change it and do NOT invent backend behavior.

WORK IN TWO STEPS IN THE SAME ANSWER. STEP 1: find the admin report detail screen (title "Report #xxxx, Moderation Investigation & Workflow") and the "Message User" dialog; list the files in at most 5 lines. STEP 2: IMMEDIATELY edit those files, do not wait for me. Show every changed file in full.

PROBLEM: the report detail screen shows only Incident Details and Reported User Profile. It never shows the proof. The data is already returned by the RPC admin_get_report_detail(p_report_id), a JSON object: { report, claim, reporter, reported, reopens, events }. The proof is inside "report":

1. report.evidence_status: "server_verified" | "unverified" | "none". Show it as a badge: "Verified by server", "Sent by the reporter's app (not verified)", "No proof attached".
2. report.evidence (JSON):
   - context "moment": evidence.reported_moment {id, content, created_at, image_urls[]} and evidence.recent_moments[] (same fields). Show the reported moment first (text + images, tap to enlarge), then "Their last moments" as a list. image_urls are public URLs: load them with the app's existing image loader.
   - context "room": evidence.title, evidence.room_live, evidence.captured_at.
   - context "dm" or "group": the app sends evidence = {"messages":[{"sender","type","text","sent_at"}]} (may be missing). Show the messages as a simple chat list.
3. report.message_snapshot (array, room reports): each item {id, sender, user_id, message, message_type, created_at}. Show as a chat list, and highlight the lines where user_id equals the reported user id.
4. report.speaker_log (room reports): speaker_log.reported_user = {name, user_id, summary:{mic_turns, mic_on_seconds}, events:[{at, event, role, detail}]}. Show "Mic turns" and "Mic time" (format mm:ss) and the events as a timeline. speaker_log.participants is the list of people in the room.
5. report.deletion_log (array or null): show "Messages deleted by the reporter" with the count and the list.
6. Show a section for each, but ONLY when it exists. If nothing exists, show a clear card: "No proof was attached to this report".
7. In the Audit Event Timeline, show the event time and a readable sentence per event ("Admin took charge", "Action: warning", "Report dismissed", "Auto released after 48 hours", "Report reopened"), not raw JSON.
8. Also render report.context_type "group" with the same card style as "dm".

"Message User" dialog: add a choice "Keep the chat open for: 1 hour / 2 hours" (default 2 hours). Call admin_send_support_message(p_user_id, p_message, p_report_id, p_open_hours) with p_open_hours = 1 or 2. Under the text field add one line: "The user will see a support chat for this time and can reply." After sending, show "Chat open until HH:mm". Add a small "Close chat now" text button that calls admin_close_support_chat(p_user_id). Error names: staff_required, empty_message, invalid_open_hours, target_user_not_found: show friendly messages.

At the end, list what you could NOT verify (you cannot run the app).
```

---

## Prompt B: report buttons: 1:1 chat, group dialog, proof

```
You are working on the Funkytalk Android app (Kotlin, Jetpack Compose, Material 3, Firebase Auth, Supabase backend). The backend is finished and tested: do NOT change it and do NOT invent backend behavior. Do NOT touch any encryption (E2EE) logic.

WORK IN TWO STEPS IN THE SAME ANSWER. STEP 1: find every place where reporting exists or used to exist (1:1 chat screen menu, group settings "Report Group" dialog, moments menu, profile menu, voice room). List the files in at most 5 lines. If a Report option used to exist in the 1:1 chat and is gone now, check the git history or diff and restore it. STEP 2: IMMEDIATELY implement everything below. Do not wait for me. Show every changed file in full.

1) 1:1 CHAT: the chat top bar has no menu. Add a three-dot menu with "Report". It opens a bottom sheet:
   - Reason chips (single choice, required): Spam & Scam, Hate Speech, Harassment, Inappropriate Content / NSFW, Other.
   - Optional text field (max 1000 characters).
   - A switch "Include the last 20 messages as proof", ON by default, with the line "Only messages you can see are sent, to moderators only." 
   - Submit button, disabled while sending.
   The app decrypts the last 20 messages locally. Build the proof JSON: {"messages":[{"sender":"me" or "them","type":"text|image|voice","text":"<plain text, or null for image/voice>","sent_at":"<ISO time>"}]} (never send image or voice files, max 200 KB in total). If the user deleted messages in this chat, build deletion_log = [{"at":"<ISO time>","type":"<message type>"}] (max 20 KB, can be null).
   Call RPC report_user(p_reported_user_id, p_context_type = "dm", p_category, p_reason, p_context_id = <conversation id as text>, p_screenshot_url = null, p_evidence, p_deletion_log).

2) GROUP: the "Report Group" dialog has NO reason choices. Add the same reason chips, optional text and the same "Include the last 20 messages as proof" switch (messages the user can see: sender display name, type, text, sent_at). Call RPC report_group(p_group_id, p_category, p_reason, p_evidence, p_deletion_log).
   The raw error "cannot_report_own_group" is shown to users. Map it to: "You can't report a group you created."

3) MOMENTS: reporting a moment already works (the server collects the proof). Keep it: report_user(p_reported_user_id, "moment", p_category, p_reason, p_context_id = moment id as text). Make sure the Report option exists on other people's moments and is hidden on the user's own moments.

4) PROFILE and VOICE ROOM: keep working as they are (server collects the proof, send no evidence): report_user(..., "profile", ...) and report_user(..., "room", ..., p_context_id = room id).

5) Friendly messages for these server errors: not_authenticated, cannot_report_yourself, category_required, reason_too_long, evidence_too_large (retry without the proof switch), deletion_log_too_large, rate_limited ("You can send up to 10 reports per hour"), not_in_room, reported_not_in_room, moment_mismatch, cannot_report_own_group. Unknown error: "Could not send, try again." After success: "Thanks, our team will review your report." A repeated report within 10 minutes returns the same id: treat it as success.

6) NEVER insert into the room_reports table directly.

At the end, list what you could NOT verify (you cannot run the app).
```

---

## Prompt C: support chat only while support opens a window (user side)

```
You are working on the Funkytalk Android app (Kotlin, Jetpack Compose, Material 3, Supabase backend). The backend is finished and tested: do NOT change it and do NOT invent backend behavior.

WORK IN TWO STEPS IN THE SAME ANSWER. STEP 1: find the existing support chat code (the "FunkyTalk SUPPORT" row in Conversations, the support chat screen, the Account Under Review lock screen). List the files in at most 5 lines. STEP 2: IMMEDIATELY implement everything below. Do not wait for me. Show every changed file in full.

NEW RULE: the support chat exists for the user ONLY while support (an admin) has opened a chat window for 1 or 2 hours. The user cannot start it.

RPCs and tables:
- get_my_support_session() returns {"open": true/false, "until": timestamp, "conversation_id": uuid, "unread": number}.
- Messages: read the user's own rows of table support_messages (columns: id, conversation_id, sender_type "user"|"admin", body, created_at, is_read_by_user) with Supabase Realtime on support_messages and on support_conversations (column chat_open_until).
- Send: RPC send_support_message(p_message). Error support_chat_closed means the window is over.
- Mark read: RPC mark_support_conversation_read() (no arguments).

BEHAVIOR:
1. Call get_my_support_session() on app start, on every app resume, when a push with data kind="support_chat" arrives, and when a notification row with type "support_message" and action_type "support_chat_open" arrives (Realtime on notifications).
2. If open is true and unread is greater than 0, OPEN THE FULL SUPPORT CHAT SCREEN automatically (once per new admin message, not in a loop). Tapping the push (kind="support_chat") opens it too. It must also open when the user is on the Account Under Review lock screen.
3. Conversations list: show the "FunkyTalk SUPPORT" row ONLY while open is true. When the window ends, remove the row and close the chat screen with the message "This support chat has ended."
4. Chat screen: normal chat bubbles, admin messages on the left with the FunkyTalk logo, a header line "Support chat open for 1h 52m" (live countdown from "until"), a message box. When the countdown reaches zero, disable the box and show the ended message.
5. Account Under Review lock screen: remove any permanent "Contact support" button. Show a button "Open support chat" ONLY while open is true. Otherwise show the text "Our team will message you here if they need anything."
6. Create two notification channels: "support_chat" (high importance) and keep "staff_reports" for admins.
7. Never show who reported the user, report ids, categories or proof anywhere on the user side.

At the end, list what you could NOT verify (you cannot run the app).
```

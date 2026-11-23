# P5 — Messages must feel instant (optimistic send + clear status)

WORK IN ONE ANSWER: list the files you need (max 5 lines), then IMMEDIATELY implement, show every changed file in full, and finish with a list of what you could not verify.

## Problem (real phones: Oppo A78, Galaxy M02)
- After the sender taps Send, the message appears on screen late (both for long and 2-letter messages).
- The sender does not know if it went: "did it send? why is it not shown?" Later it appears.
- Receiver also sees it late.

## Hard rules
- Do NOT change the E2EE format (Tink HPKE 1:1, group fan-out), server calls, table names or RPC names. Only change WHEN things happen and what the UI shows.
- Never log message text, keys or tokens (not even in debug).
- All app text in English.

## What to build
1. **Optimistic send.** On Send: (a) clear the input and add the message to the list in the SAME frame, with a local `clientId` (UUID) and status SENDING; (b) do encryption + network on a background dispatcher, never on the main thread; (c) the user can keep typing and sending while earlier messages are still going. Keep order by local time.
2. **Status per own message:** SENDING (small clock icon) -> SENT (one tick) -> DELIVERED/READ (only if the app already has this; do not invent new server states). FAILED = red "!" icon; tap = bottom sheet with "Retry" and "Delete". Never leave a message silently missing.
3. **Auto retry:** when the network comes back (ConnectivityManager callback) retry FAILED/SENDING messages in order, with backoff. Keep a persisted outbox (Room or the existing local store) so a message typed before the app is killed is not lost.
4. **No duplicates / no flicker:** match the server echo to the pending message by `clientId` (or the server message id returned by the send call) and replace it in place. If the server has no idempotency key, dedupe locally and tell me in the final list.
5. **Receiver speed:** check the Realtime subscription: subscribe once per conversation, reconnect when the network returns, and do a quick catch-up fetch of the mailbox on app resume. Decrypt on a background dispatcher; insert into the list as soon as decrypted; never block the list on one slow message.
6. **Measure first (debug builds only):** add timing logs for: tap -> bubble shown (ms), encrypt (ms), send request (ms), server echo / realtime receive (ms). In the final answer tell me which stage is slowest and why. Do not log content.
7. **Magic touch (small, smooth):** light haptic tick on send, bubble slides/fades in (about 150 ms), clock changes to tick with a tiny scale pop. Respect "reduce animations".

## Done when
- Bubble appears in under 100 ms after tap, even on the Galaxy M02.
- Airplane mode: message shows with clock/FAILED, and goes out by itself when the net returns.
- Two phones: no duplicate bubbles, order is correct.

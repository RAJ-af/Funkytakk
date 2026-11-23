# P7 — Chat lock: use fingerprint, ask less, user can turn it off

WORK IN ONE ANSWER: list the files you need (max 5 lines), then IMMEDIATELY implement, show every changed file in full, and finish with a list of what you could not verify.

## Problem
- On the chat screen the phone's fingerprint is not used; the app asks the password again and again.
- After the app is closed the user must enter the password again every time. Very frustrating.
- QA earlier: chat vault was locked after a cold start with no unlock path.

## Hard rules
- Do NOT weaken E2EE. Never store the vault password or keys in plain text. Keys may only be stored wrapped by an Android Keystore key (AES-GCM, hardware-backed when available). Tell me exactly what you store and where.
- Do not change the server, E2EE message format or backup format.
- All app text in English. Keep FLAG_SECURE / recents privacy as they are now.

## Design (two levels)
1. **Level 1 — screen lock (often):** BiometricPrompt (androidx.biometric) with BIOMETRIC_STRONG or DEVICE_CREDENTIAL (minSdk 30 allows this). Fingerprint/face first, device PIN as fallback. Always include a visible "Use vault password" button as a fallback.
2. **Level 2 — vault password (rarely):** asked only on first setup, new device/reinstall, after 14 days since the last password check, or if biometrics changed/are unavailable. After a successful password check store a wrapped vault key so a cold start does NOT need the password again.
3. **Settings > Privacy > Chat lock:**
   - Switch "Lock chats" (user can turn it OFF; then chats open without Level 1).
   - "Ask again" options: Every time I open the app / After 5 minutes away / After 1 hour away. Default: After 5 minutes away.
   - Text under the switch: "Your vault password is asked again every 14 days."
4. Lock timing: save the time the app went to background (ProcessLifecycleOwner, SystemClock.elapsedRealtime) and compare on return. Do not lock again while the app is only briefly in the background or when moving between screens inside the app. Rotation must not re-lock.
5. If no biometric/device credential is enrolled: skip Level 1 and use the vault password; show a one-line hint "Set a screen lock on your phone for faster unlock."
6. Cold start: if the chat is locked, show the unlock sheet immediately (no blank chat, no dead end).
7. **Magic touch:** lock icon opens with a tiny animation and a soft haptic on success.

## Done when
- Fingerprint unlocks chats in one touch on Oppo A78 and Galaxy M02.
- Closing and reopening the app within the chosen time does not ask anything.
- Switch OFF = no prompt. 14-day rule works (give me a debug way to simulate it).

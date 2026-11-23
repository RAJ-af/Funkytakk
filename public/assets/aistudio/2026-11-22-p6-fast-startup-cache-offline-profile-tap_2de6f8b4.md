# P6 — Fast startup, cache-first screens, offline banner, friendly errors, profile tap, AI client check

WORK IN ONE ANSWER: list the files you need (max 5 lines), then IMMEDIATELY implement, show every changed file in full, and finish with a list of what you could not verify. If this is too big for one answer, finish parts A+B fully first and say clearly what is left for the next answer.

## Hard rules
- Login/auth flow (Firebase -> verify-firebase-token -> Supabase session) must keep working exactly as now. Do not change what is sent to the server. Only remove waiting and fix states.
- Never show raw exception text, URLs, server JSON or stack traces to the user. All app text in English.

## A. Startup and "stuck" screens
Symptoms: sometimes only mockup/skeleton screens show; sometimes the first slide screen gets stuck (swipe, then privacy tick + Continue, then login shows loading, then Home); long loading.
1. Decide the start screen from LOCAL data (DataStore: onboarding done, privacy accepted, session exists) without waiting for the network. Splash must never wait more than about 1.5 s.
2. Audit the onboarding pager and privacy screen: Continue enabled state, tick state, and any LaunchedEffect that auto-navigates. Fix any state that can freeze (e.g. waiting for a flow that never emits).
3. Every screen gets one UiState: Loading / Content / Empty / Error(retry). No blank screen, no infinite spinner: after 10 s of loading show Error with a Retry button.
4. Cache-first (stale-while-revalidate): Home, rooms list, chats list, profile, followers use the last cached data instantly (Room or the existing cache), then refresh in the background. Show skeleton shimmer ONLY when there is no cache.
5. Run independent startup requests in parallel (async/awaitAll), not one after another. Nothing heavy on the main thread. OkHttp timeouts: connect 10 s, read 15 s. Make sure Coil has memory + disk cache and uses correct image sizes.
6. Report (debug timing): cold start to first content, and which request is the slowest.

## B. Offline and friendly errors
1. Global connectivity watcher (ConnectivityManager network callback). When offline show a slim top banner "No internet connection". When back online show "Back online" for 2 s then hide. Screens with cache keep showing cached data while offline.
2. One central `ErrorMapper` for the whole app. Map to short messages:
   - UnknownHost / ConnectException / SocketTimeout -> "Can't reach FunkyTalk. Check your internet and try again."
   - HTTP 401 -> refresh the session silently once; if it still fails -> "Please sign in again."
   - 403 with `device_revoked` -> "You were signed out on this device."
   - 429 -> "Too many tries. Please wait a moment."
   - 5xx / 503 `session_mint_failed` -> "Server is busy. Please try again."
   - anything else -> "Something went wrong. Please try again."
3. Every error UI has a Retry action. Buttons that need the internet are disabled/explained while offline instead of failing later.

## C. Profile opens on tap (and only complete profiles)
Symptoms: tapping a person's profile picture/card does nothing (looks static); sometimes a half-empty profile opens; slow.
1. Audit EVERY place where a user is shown: feeds/moments, comments, rooms (seats, participants), followers/following, search, chat list, chat header, invites, leaderboards. Each avatar, name and card must call ONE shared `openProfile(userId)`. Tapping yourself opens your own profile.
2. Open instantly: navigate immediately and show the header from data you already have (name, avatar, frame), then load the rest with skeletons. Never show a half-filled screen as final: if the load fails show Error + Retry.
3. Cache profiles (memory + disk, about 5 min fresh), prefetch on long-press/visible rows if cheap. Tap target at least 48 dp.
4. List every place you changed in the final answer.

## D. AI features (client side check)
The server function is `/functions/v1/ai-assist` (verify_jwt = true, needs the Supabase access token). Body: `{ feature: "translate"|"improve", text (max 1000 chars), target_language (letters only, translate), style: "fix"|"friendly"|"formal"|"short" (improve), language_hint }`. Success: `{ text, provider }`. Errors: 401 unauthenticated, 403 `consent_required` (+ `consent_version`), 429 `rate_limited` / `daily_limit`, 503 `ai_not_configured` or `all_providers_failed`, 400 invalid_*.
1. Find out if and where the app calls it. Tell me: URL, headers (Authorization Bearer = Supabase access token, apikey), body.
2. Handle every error above with a friendly message (use ErrorMapper). `consent_required` must open the existing AI consent screen and, after consent, retry once. If a consent screen/RPC does not exist in the app, say so.
3. Show a loading state on the AI button and a 20 s timeout. Do not log the text.

# Gemini prompt: Moments fix round 2 (upload still fails, nav bar still visible, feed blank)

WORK IN ONE ANSWER: list the files you will touch (max 5 lines), then IMMEDIATELY implement. Show every changed file in full (if a file is huge, show only the changed functions in full plus exact insert locations). At the end list what you could not verify.

Phone test after your last build (user's report + server logs):
- Photo post: loading popup, then "Upload failed". Video post: spinner for a very long time, then "Upload failed".
- Server logs: `get-cloudinary-signature` returns 200 every time, but NO `create_moment` call ever follows. So the failure is between Cloudinary and the app.
- The main bottom navigation bar (Home, Discover, Moments, Chats, Profile) is STILL drawn over the Create Post screen and hides the toolbar (Media, Background, Language, Topics, Location). Your last fix hid `HomeBottomBar`, but the bar the user sees is evidently a different composable.
- The text post I created earlier is not visible in the feed. The server returns it (`get_moments_feed('trending')` contains it), so the app drops it or shows nothing.

## 1) Show the REAL Cloudinary error (temporary, for this test phase)
In `uploadMomentMedia`, on any failure show a toast/snackbar with details in ALL builds (not only DEBUG): `Upload failed (<HTTP code>: <Cloudinary error.message>)` or, for exceptions, `Upload failed (<exception class>: <message>)`. Also Logcat tag `MomentUpload`: the full URL used (without the file), cloud_name, folder, HTTP code and response body. Log the response of `get-cloudinary-signature` too (WITHOUT the signature value). Add a clear `// TODO remove detail before release` comment.
Never swallow exceptions: wrap every step (signature call, compression, upload, parsing) so that any exception ends in this message.

## 2) The loading popup must always end
The "Publishing..." dialog must close in every case (success, error, cancel, timeout). Use `try/finally`. Use an OkHttp client for uploads with `connectTimeout 30s`, `writeTimeout 120s`, `readTimeout 120s`, `callTimeout 5 min`. Show real upload progress (percent) for videos (count bytes written in the RequestBody), with a Cancel button. If the upload takes longer than 5 minutes, fail with the message from point 1.
For video: before uploading, if the file is larger than 50 MB show "Video is too large (max 50 MB)" and do not upload. Max duration 60 s.

## 3) Use the cloud name and folder EXACTLY as returned by `get-cloudinary-signature`
Response: `{ signature, timestamp, api_key, cloud_name, folder }`. The upload URL must be `https://api.cloudinary.com/v1_1/<cloud_name from the response>/image/upload` (or `/video/upload`). Do not use any hardcoded cloud name or any value from BuildConfig. Multipart fields exactly: `file`, `api_key`, `timestamp`, `signature`, `folder`. `timestamp` must be sent as the same integer string. Nothing else.

## 4) Remove the main bottom navigation bar from the Create Post screen for real
Find where the bar with the labels Home / Discover / Moments / Chats / Profile is composed (search the whole app: grep for `"Discover"`, `"Chats"`, `HomeBottomBar`, `NavigationBar`, and the main Scaffold's `bottomBar`). Hide it while Create Post is open. The most reliable way: show Create Post inside a full-screen `Dialog(onDismissRequest = ..., properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false))` with `Modifier.fillMaxSize()`, so nothing from the main scaffold can be drawn above it. Keep `imePadding()` and navigation-bar padding so the toolbar sits above the keyboard. Handle the back button to close it. Also keep the Snackbar/toast above the toolbar.

## 5) Feed: a post must never silently disappear
- The user opened Trending: the server returns 1 post (id 25, title "today", language Hindi, bg_key "cream", topics ["daily_life","tech"], media []). Make sure that this item renders. Wrap the JSON decoding so that a decoding error is logged under tag `MomentsFeed` (exception + the raw JSON of the failing item) and the item is skipped, NOT the whole page. Use `ignoreUnknownKeys = true` and `coerceInputValues = true`, and give every nullable field a default (title, content, translation, language, bg_key, location, media, topics).
- Never show a blank screen: if the list is empty show an empty state ("No moments yet. Be the first to post!" on Trending, "No posts in your languages yet" on My language); if the request fails show an error state with a Retry button.
- Note for the empty My language tab: it lists posts whose language is one of the user's native or learning languages. This is expected to be empty if there is none.

## 6) Language chip sends the plain language name
The create screen chip shows "Marathi (Beginner)" (the profile string with the level). When calling `create_moment`, send only the language name, `p_language = "Marathi"` (strip the trailing " (...)" part). Same for the pill on cards: show only the name.

After this, test again: photo (1), then 3 photos, then a short video. If it still fails, send the exact toast text (it will now include the Cloudinary error) and the Logcat lines with tag `MomentUpload`.

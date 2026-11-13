# Gemini prompt: fix Moments media upload + 3 UI problems

WORK IN ONE ANSWER: list the files you will touch (max 5 lines), then IMMEDIATELY implement. Show every changed file in full. At the end list what you could not verify.

## 1) Photo/video post fails: toast "Upload failed, please try again"
Server logs prove the failure happens BEFORE `create_moment`: `get-cloudinary-signature` returns 200 every time, but no `create_moment` call follows. So Cloudinary rejects the upload (most likely "Invalid Signature") or the upload code throws. Text-only posts work fine.

How the server signs (do not change the server): `signature = sha1("folder=<folder>&timestamp=<timestamp><api_secret>")`. It signs ONLY `folder` and `timestamp`. The function is called with POST + `Authorization: Bearer <Supabase access token>` (empty body is fine) and returns `{ signature, timestamp, api_key, cloud_name, folder }`.

Fix `CloudinaryUploader.uploadMomentMedia`:
- The multipart form must contain EXACTLY these fields: `file`, `api_key`, `timestamp`, `signature`, `folder`. Use the returned values verbatim. NO other form fields (no `upload_preset`, `public_id`, `resource_type`, `eager`, `tags`, `context`, `transformation`, `overwrite`, `unique_filename`, `use_filename`, `eager_async`...). Any extra field makes Cloudinary check it against the signature and reject with "Invalid Signature".
- Resource type goes in the URL, not the form: `https://api.cloudinary.com/v1_1/<cloud_name>/image/upload` for photos, `.../video/upload` for videos.
- Take `url = secure_url`, `width`, `height`, and for video `duration` (-> `duration_sec`) from Cloudinary's JSON response. Do not compute them another way.
- Video thumbnail: do NOT upload a second file. Build `thumb_url` from the video's `secure_url`: replace `/video/upload/` with `/video/upload/so_0,f_jpg/` and change the file extension to `.jpg`. (The backend only accepts URLs inside the user's own `moments/<uid>/` folder; this derived URL stays inside it.)
- Photos: downscale to max 1600 px on the long edge and re-compress as JPEG quality 85 before upload. Limits: photo 10 MB, video 100 MB, video max 60 seconds. Show a clear message if exceeded (before uploading).
- Upload photos one after another (not in parallel), and reuse the same signature for all files of one post (valid for about 1 hour). Fetch a new one if it is older than 30 minutes.
- Error handling: on a non-2xx Cloudinary response, read the body (`{"error":{"message":"..."}}`), log it with Logcat tag `MomentUpload` (HTTP code + message), and in DEBUG builds show that message in the toast. In release builds show "Upload failed, please try again". Do not swallow exceptions silently.

## 2) Create post screen: bottom toolbar hidden behind the app's bottom navigation
In the screenshots the main bottom nav (Home, Discover, Moments, Chats, Profile) is drawn over the create screen's own toolbar (Media, Background, Language, Topics, Location), and the language chip ("Marathi") is covered by the toast. Make the create screen full-screen: hide the main bottom nav while it is open, and keep the toolbar above the keyboard and system bars (`imePadding()` + navigation bar padding). Show toasts/snackbars above the toolbar.

## 3) Hashtags show "#Dailylife" instead of "#DailyLife"
Do not build the hashtag from the topic slug. Use the label from `get_moment_options()` (`{slug,label}`) matched by slug, remove spaces from the label: label "Daily life" -> `#DailyLife`. If the slug is not found in the options, fall back to title-casing each `_`-separated part.

## 4) Profile -> "Feed" tab is empty although I have 1 post
The server returns my post correctly (`get_user_moments(p_user_id = null)` returns it; the profile header already shows "1 Posts"). So the problem is in the app. Check which of the three tab icons under "About Me / Feed" shows the Moments list, make sure that tab renders the `get_user_moments` result (text-card posts use `bg_key` gradient with title/content, photo posts show the first `media` image), and that the call runs again each time the tab becomes visible. Pull-to-refresh is optional.

After this: test again with a photo post (1 photo, then 3 photos, then 1 short video). If it still fails, send the Logcat `MomentUpload` line or the toast text.

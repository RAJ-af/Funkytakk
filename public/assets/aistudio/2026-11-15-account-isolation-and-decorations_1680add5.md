# Gemini prompt: (A) CRITICAL account isolation on logout/login, (B) decorations: name effects tab + asset frames

WORK IN ONE ANSWER: list the files you will touch (max 5 lines), then IMMEDIATELY implement. Show every changed file in full (huge files: only the changed functions plus exact insert locations). At the end list what you could not verify. Do the A part first and completely.

## A) CRITICAL BUG: second account sees the first account's chats and chat lock
Reproduced on a phone: log in as account A, log out, log in as account B -> B sees A's chats and A's chat lock (vault password) is applied. The server confirms it: 3 E2EE identity public keys are shared by 2, 2 and 4 different accounts, i.e. the app reuses the same local key pair for every account on the device. That means B can read A's local chats and A's keys can decrypt B's messages. This is a privacy/security bug and must be fixed before any beta APK goes out.

Required behaviour: ALL local data is scoped to the logged-in user id (Supabase user id = `profiles.auth_user_id`).
1. Introduce one `ActiveUser`/session holder with the current uid. Everything below is keyed by uid:
   - local chat database (Room): use one database file per uid, e.g. `chat_<uid>.db` (and the same for group chats, drafts, media index, reactions, read state);
   - local media files (decrypted/cached chat media): directory per uid;
   - Tink keysets and Android Keystore aliases for the E2EE identity key: `identity_<uid>` (never a shared alias);
   - Vault password / chat lock (PIN, password hash, salt, "lock enabled", auto-lock timer, biometric flag): stored per uid (EncryptedSharedPreferences/DataStore file per uid);
   - any SharedPreferences/DataStore that holds user-specific state (theme choices that come from the profile, last room, unread counters, Drive backup account/token, cached profile, onboarding flags).
2. On LOGOUT: stop Realtime channels, cancel WorkManager jobs, clear in-memory caches/ViewModels/repositories and Coil memory cache, close the database, unregister the FCM token for this user, sign out of Firebase and Supabase, and clear `ActiveUser`. Do NOT delete the user's own local chat data (chat history exists only on the device; deleting it would lose it), just make it unreachable until that same uid logs in again.
3. On LOGIN: set `ActiveUser = uid` first, then open ONLY that uid's database/keys/lock. A new uid on this device starts with an empty chat list, no lock, and goes through "set up chat lock/vault" like a fresh install.
4. E2EE identity key per uid: if no keyset exists for this uid, generate one and upload the public key. If the upload fails with the error text `identity_key_in_use` (the server now rejects a public key that already belongs to another account), delete that uid's local keyset, generate a NEW key pair, bump `key_version`, and upload again. Never copy or reuse a keyset between uids. Never show `identity_key_in_use` to the user.
5. Legacy data from older builds is not scoped and cannot be assigned safely to an account. On the first launch of the new build: delete all legacy unscoped chat databases, keysets, vault settings and media (this is a closed beta; testers will log in again and get fresh keys). Show once a dialog: "We upgraded chat security. Your chats on this device were reset." Do this only once (store a `schema_scope_version = 2` flag in a global, non-user-specific preference).
6. Search the whole app for any singleton/global holding user data (object, companion object, lazy database builders, `Room.databaseBuilder(... "chat.db")` with a fixed name, `MasterKey` with a fixed alias, global `SharedPreferences` names) and make them uid-aware.
7. Account deletion and "revoke device" flows must also wipe that uid's local database, keysets, vault settings and media.

Test list (must pass on a phone with two accounts, A and B):
- A logs in, chats, sets a vault password. Logout. B logs in: B sees NO chats from A, NO lock from A, goes through its own setup. B sets a different password. Logout. A logs in: sees A's chats, A's password works, B's password does not.
- B cannot decrypt A's messages and A's keys are not used for B (different identity public keys in `user_public_keys`).
- Force-close the app between logins: same result.

## B) Decorations (store system already exists: `store_items`, `get_store_items`, `purchase_item(s)`, `equip_item`, `unequip_slot`, `get_user_cosmetics_v2`)
1. Message bubbles are DATA + CODE, no image files: `item_type = 'bubble_theme'`, `config = { bg:[colors], bg_angle, radius, tail:'round'|'sharp', text, meta, border:{color,width}|null, glow }`. Keep drawing them in Compose from this config. New bubble styles are just new rows; nothing to bundle.
2. NAME EFFECTS are missing in the decoration/apply screen. Add a tab "Name" (item_type `name_effect`, 4 items exist: Neon Glow, Gold Shimmer, Fire Text, Rainbow Flow) next to the existing tabs, with a live preview of the user's own username, buy/equip via the same calls as the other tabs, and remove via `unequip_slot` using the same slot naming as the other tabs (slot for this type = `name_effect`). Config: `{ bold, glow:'#hex'|null, colors:[#hex...], speed_ms, animation }`. Implement animations in Compose:
   - `glow_pulse`: text in `colors[0]` with a shadow/glow in `glow` whose radius/alpha pulses (infinite transition, `speed_ms` period).
   - `shimmer`: gradient brush over `colors` with a highlight band moving left to right every `speed_ms`.
   - `gradient_flow`: `Brush.linearGradient` over `colors` whose offset animates continuously every `speed_ms` (looping, seamless); add `glow` as a soft shadow when set.
   - unknown animation: static text with the first color.
   Render the effect EVERYWHERE the username is shown (own profile header, other profiles, voice room participant tiles and room chat, chat headers, comments and Moments cards, support/admin lists). Others' cosmetics come from `get_user_cosmetics_v2(p_user_ids)`: batch the ids of everyone visible on screen and cache the result for 5 minutes. Performance: animate only items that are on screen; in long lists (comments, rooms with many users) show a static gradient for rows that are scrolling, animate in headers and tiles.
3. Asset avatar frames: three new frames come from the server with `config.kind == "asset"`:
   `{ kind:'asset', asset:{ url, format:'webp', size_px:320, fps:24, avatar_ratio:0.6 }, ring:null, glow:null, ornament:null, particles:null }`.
   Draw the animated WebP over the avatar: frame size = avatarSize / avatar_ratio, centered on the avatar (the frame overlaps outside the avatar, do not clip it), above the avatar and below badges/online dot. Use Coil with the animated image decoder (`ImageDecoderDecoder`, minSdk 30 supports animated WebP), disk cache enabled, and preload the file when the frame is equipped/previewed. If loading fails, fall back to the existing ring renderer with default colors. Items with `is_active = false` are hidden by the server until the files are uploaded, nothing to do on the app side. Store preview uses `preview_url` (same WebP, animated).
4. `entry_animation`, gifts with HTML/3D effects and keyboard themes are NOT part of this prompt.

After this prompt: test frames (Floral Moon, Sea Voyage, Waterfall Garden) on own avatar and on another account's avatar in a voice room, and the 4 name effects on profile + room.

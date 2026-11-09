# Funkytalk Design System (v1 draft, 2026-11-04)

Goal: one look, fast screens, no crashes. Priorities chosen by the owner: UI consistency, speed, stability. Size comes later.

Colors marked **(sampled)** were read from real app screenshots. Colors marked **(proposed)** are my suggestions and need a contrast check on a real phone.

## 1. Color tokens (light)

| Role | Hex | Use |
|---|---|---|
| background | #FAFAEE (sampled) | Screen background (warm cream) |
| surface | #FFFFFF (sampled) | Cards, search bar, unselected chips, icon buttons |
| surfaceNav | #FCFBF7 (sampled) | Bottom navigation bar |
| surfaceTrack | #ECE6D8 (sampled) | Tab switcher track, skeleton base |
| primary | #FFC529 (sampled) | Main chips, main buttons, selected states |
| primarySoft | #FFD43A (sampled) | Big action buttons (Say Hi) |
| primaryContainer | #FFE380 (sampled) | Selected bottom-nav pill |
| onPrimary | #523700 (sampled) | Text and icons on yellow |
| success | #10B982 (sampled) | Online ring, Following state |
| successContainer | #EBFCF2 (sampled) | Online chip, Following button fill |
| nightCard | #F6F7FC (sampled) | Greeting card at night |
| accentIndigo | #E4E5FA (sampled) | Greeting card icon bubble |
| textPrimary | #1F1D14 (proposed) | Titles, body |
| textSecondary | #6B6A5E (proposed) | Subtitles, captions |
| outline | #E8E4D6 (proposed) | 1dp borders |
| error | #E5484D (proposed) | Errors, destructive |
| errorContainer | #FDECEC (proposed) | Error banners |

Dark theme (proposed, needs review): background #14130E, surface #1E1C15, surfaceNav #191810, surfaceTrack #2A2820, primary stays #FFC529 with onPrimary #3A2700, textPrimary #F4F1E4, textSecondary #B5B2A3, success #34D39E, outline #35332A.

Rules: no hard-coded hex in screens, only tokens. Text on any colored surface must reach contrast 4.5:1.

## 2. Typography (keep the current font family)

Display 28/34 Bold, Title 20/26 SemiBold, Section 16/22 SemiBold, Body 14/20 Regular, Caption 12/16 Regular, Button 15/20 SemiBold. All text in sp, must not clip at font scale 1.3.

## 3. Spacing, shape, elevation, motion

- Spacing grid 4dp: 4, 8, 12, 16, 24, 32. Screen side padding 16dp. Gap between cards 12dp.
- Shapes: buttons and chips full pill, cards 20dp, bottom sheets 28dp top corners, avatars circle, banners 16dp.
- Elevation: flat with 1dp outline by default, soft shadow only for floating things (banner, mini player). Max 2 levels.
- Motion: 150ms (tap feedback), 250ms (enter/exit), 350ms (sheets). Ease-out. Speaking ripple loops about 1.2s and runs only for avatars that are on screen and speaking.

## 4. Components (one implementation each, in ui/components)

1. FunkyButton: Primary (yellow, onPrimary text, 48dp high, pill), Secondary (surface + yellow outline), Success (Following state), Text. Pressed, disabled and loading states.
2. FunkyChip: selected = primary, unselected = surface with outline.
3. FunkyCard: surface, 20dp, 1dp outline, 16dp padding.
4. FunkyListItem: avatar 48dp, title, subtitle, trailing slot. One row design for chats, followers, invite list, people sheet.
5. PresenceAvatar (already built): speaking ripple over online ring over offline outline moon. Flag badge bottom-left (about 28% of the avatar, white 2dp border), moon top-right, level or VIP badge bottom-right.
6. FunkyTopBar, FunkyBottomNav (selected pill primaryContainer), FunkySheet, FunkyDialog.
7. FunkyBanner: in-app top card for invites and alerts. Sender dp, title, 2 actions, auto-hide 6s, swipe up to dismiss, queue when several arrive.
8. State screens, every list/screen must use them: FunkyLoading (skeleton, not a spinner on a blank page), FunkyEmpty (icon, one sentence, one action), FunkyError (one sentence, Retry), FunkyOffline (thin top bar).
9. Room Ended screen: stays until the user presses Close, back or swipes down.

## 5. Layout rules

- Edge-to-edge with WindowInsets (system bars, IME, display cutout). Test with gesture navigation and the 3-button bar.
- Touch targets at least 48dp. Smallest width 320dp. Content width capped at 600dp on tablets.
- Light and dark must both work on every screen.
- contentDescription on every icon button and avatar.

## 6. Speed rules (Compose)

1. Every LazyColumn/LazyGrid item has a stable `key` (user_id, room_id) and `contentType`.
2. UI state classes are immutable (`@Immutable` or `@Stable`), lists as `ImmutableList` or wrapped.
3. Collect with `collectAsStateWithLifecycle()`. No work inside composition: no sorting, no parsing, no new objects in lists.
4. `remember` and `derivedStateOf` for derived values; hoist lambdas that capture changing state.
5. Images: Coil with an explicit size, memory and disk cache, placeholder, short crossfade (150ms). Never decode full size into a 48dp avatar. Prefer WebP.
6. Lists are paged (about 20 per page). Search is debounced (300ms).
7. Realtime: one channel per screen, with a filter, closed when the screen leaves. No polling where Realtime exists.
8. Release builds use R8 and resource shrinking. Add a Baseline Profile later.

## 7. Stability rules

1. Every network and database call goes through one Result wrapper (Success, Error, Loading). No unhandled exceptions in coroutines.
2. No `!!`. Models have safe defaults for null avatar, country, name.
3. Jobs, Flows, Realtime channels and the Agora engine are owned by a ViewModel or session and released in `onCleared`.
4. Survive rotation, process death and permission denial on every screen.
5. A global exception logger now, Crashlytics before the Play release.
6. Retries use backoff and a limit. Offline shows FunkyOffline, not a blank page.
7. Log tags per area (VoiceRoomExit, RoomEndedCheck, etc.). Never log tokens or message contents.

## 8. Definition of done for any screen

Uses tokens and components only, has loading, empty and error states, works in light and dark, gesture and 3-button navigation, font scale 1.3, and a 320dp-wide screen. Scrolls without dropped frames on a mid-range phone. Compiles, and the files changed are listed.

## 9. Icons (added 2026-11-08)

Style: minimal, soft and friendly. No Material icons in the app. One custom icon set, `FunkyIcons`, drawn by us.

- 24dp grid, 2dp stroke, round caps and round joins, generous corner radius, no sharp spikes.
- Two states: outline (inactive) and soft-filled (active: one pastel fill, usually primarySoft or primaryContainer, plus the outline).
- Colours only from tokens (textPrimary, textSecondary, primary, onPrimary). No gradients inside small icons.
- Delivered as vector drawables (XML) or Compose ImageVector, one file per icon family, with contentDescription.
- Needed first: home, discover, moments, chats, profile, back, close, plus, check, gift, coin, crown, flame, person, play, bubble, tag/theme, text-effect (A with sparkle), search, bell, share, more, mic, mic-off, headphones, people, send, emoji, lock, settings, report, block.
- Large illustrations (for example the 3D crown in the Decoration Store header) are generated images: transparent WebP, 512 px at most, under 60 KB.
- After the set exists, remove the Material icons dependency and check the build size.

## 10. Text language (added 2026-11-08)

Every user-facing string in the app is English, stored in `strings.xml`. No Hindi or other scripts in UI text for now (localisation can come later). Error messages are short and friendly: "Not enough coins", "Something went wrong. Please try again." Raw server error codes never reach the user. No generic "Information" dialogs: use a snackbar or an inline message.

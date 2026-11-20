WORK IN ONE ANSWER: list the files you need (max 5 lines), then IMMEDIATELY implement, show every changed file in full, and finish with a list of what you could not verify.

# FunkyTalk — Avatar frame sizing, Live Preview layout, frame calibration

App text must be English only. Follow `docs/DESIGN_SYSTEM.md` (background #FAFAEE, primary #FFC529, custom soft icons, no Material icons).

## Backend (already live, do not change)
`store_items.config` for asset frames (`kind = "asset"`) is now schema v2:

```json
{
  "kind": "asset", "schema_version": 2,
  "asset": {
    "url": "...webp", "format": "webp", "fps": 24, "size_px": 320, "source_size_px": 320,
    "animated": true, "loop": true,
    "inner_ratio": 0.6,      // avatar photo diameter / frame image width
    "offset_x": 0, "offset_y": 0,   // centre of the hole, as a fraction of frame width (-0.25..0.25)
    "overflow_ratio": 0,     // how far art sticks out beyond the frame canvas (0..0.5)
    "z": "above",            // "above" = frame drawn over avatar, "below" = behind
    "min_render_px": 32, "version": 1, "calibrated": false,
    "avatar_ratio": 0.6      // legacy copy of inner_ratio: only a fallback
  },
  "meta": { "schema_version": 2, "category": "avatar_frame", "tags": [], "rarity": null, "author": null, "license": null }
}
```
Code-drawn frames (ring/glow/ornament/particles) and other item types are unchanged except for the extra `meta` block (ignore unknown keys).

New RPC (admin or superadmin only, server enforces it):
`admin_set_frame_calibration(p_code text, p_inner_ratio numeric, p_offset_x numeric, p_offset_y numeric, p_overflow_ratio numeric)` → returns the new `asset` JSON. Use EXACTLY these parameter names (a wrong name gives 404 PGRST202). Errors: `forbidden`, `invalid_inner_ratio`, `invalid_offset`, `invalid_overflow_ratio`, `not_an_asset_frame`.

## Task A — one shared `FramedAvatar` composable (used everywhere)
Create `FramedAvatar(avatarUrl, avatarSize: Dp, frame: FrameConfig?, modifier)` and replace every ad-hoc frame drawing (Live Preview header, Live Preview card, store tiles, profile, voice room seats, chat header, discover cards).
- The avatar photo is ALWAYS exactly `avatarSize`, centred, circular. The ring/frame must be centred on it.
- Asset frame: `canvas = avatarSize / inner_ratio`; draw the WebP at `canvas x canvas`, centred on the avatar, shifted by `offset_x * canvas`, `offset_y * canvas`. Never clip the frame (`clipToBounds` off on the parent). `z = "below"` draws before the photo.
- Inner ratio fallback order: `inner_ratio` → `avatar_ratio` → 0.6.
- Layout space: the composable reserves `avatarSize` only; callers that need room for the overflow can use `FramedAvatar.outerSize(avatarSize, frame)` = `canvas * (1 + overflow_ratio)`.
- Animated WebP: use the existing image loader with animated decoding, respect `loop`; skip the frame if the rendered avatar is smaller than `min_render_px`; cache by `url + version`.
- Code-drawn frames keep their current rendering but must use the same centring rule.

## Task B — fix Live Preview screen
Screenshot problems: bottom buttons cut off at the screen edge; header avatar and card avatar use different frame ratios; ring not centred.
- Use Scaffold: scrollable content (`verticalScroll`), FIXED bottom bar with `navigationBarsPadding()`, 16 dp side padding, buttons min height 48 dp. Buttons must never be cut off on small screens (test Galaxy M02, 720x1600) or with gesture/3-button navigation (Oppo A78).
- The preview card must not use a fixed height; use `weight`/`heightIn`.
- Header avatar and card avatar both use `FramedAvatar` (only the size differs).
- Bonus "Size Strip": under the big preview show the same avatar at 3 sizes (chat 40 dp, room seat 64 dp, profile 120 dp) so the owner can see if a frame looks right at every size.

## Task C — Frame Calibration (staff only)
Visible only for users who already pass the Moderation Center role check (admin or superadmin). In Live Preview of an asset frame show a small "Calibrate" action that opens a bottom sheet:
- Sliders: Inner ratio (0.30–0.95), Offset X and Offset Y (-0.25–0.25), Overflow (0–0.50), with the numeric value shown. The big preview updates live.
- Buttons: "Reset" (back to stored values), "Save for everyone" → call the RPC, then refresh the store items so every screen uses the new numbers. Show a success or error message (map `forbidden` to "Only admins can do this").
- Show the `calibrated` flag ("Calibrated" / "Not calibrated yet") in the sheet.

## Rules
- Do not hardcode ratios anywhere. All numbers come from `config`.
- Do not touch coin store UI (flag is OFF).
- Finish with: what you could not verify (real phones, all frames, animation on the M02).

---
## Snippet for docs/BACKEND.md
### Decoration metadata v2 (2026-11-20)
- `store_items.config.meta` = `{schema_version, category, tags[], rarity, author, license}` for every item (unknown values are null).
- Asset avatar frames (`config.kind = "asset"`, `schema_version = 2`): `asset.inner_ratio` (avatar diameter / frame width), `offset_x`, `offset_y`, `overflow_ratio`, `z` (above|below), `animated`, `loop`, `fps`, `source_size_px`, `min_render_px`, `version`, `calibrated`. `avatar_ratio` is a legacy copy of `inner_ratio`.
- `admin_set_frame_calibration(p_code, p_inner_ratio, p_offset_x, p_offset_y, p_overflow_ratio)`: admin/superadmin only; validates ranges (0.30–0.95, ±0.25, 0–0.5), sets `calibrated = true` and bumps `asset.version`.
- Tested with a rolled-back SQL test (`supabase/tests/20261120_decoration_metadata_test.sql`). NOT tested: the app, real frames on a phone.

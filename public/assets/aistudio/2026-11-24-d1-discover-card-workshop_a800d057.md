# D1 — Editable people cards: Gen-Z bold cards on Discover + Card Workshop with Remix

WORK IN ONE ANSWER: list the files you need (max 5 lines), then IMMEDIATELY implement, show every changed file in full, and finish with a list of what you could not verify. If it is too big for one answer, finish A+B+C (render + online dot + stickers layer) completely first, and say clearly what is left for the next answer (D = workshop, E = remix).

Attach the screenshot of the current Discover screen (103375.jpg) and `docs/CHAT_AUDIT.md` is NOT needed. Do not touch chat, crypto, backup, block, report, auth.
Never log profile text.

## What we have today (screenshot)
Flat white cards on a cream background: avatar with frame and country flag, name + age + an "Offline" pill, "native -> learning" language line, an orange "Hi" button. It feels flat, not Gen-Z.

## Goal
Every user can decorate the card that OTHER people see on Discover, like a sticker-covered notebook or phone case. The user decides what shows, adds editable "hello" word stickers and emoji stickers, fills the empty white space himself, and can remix other people's cards.

## Backend (READY, tested, do not change it)
- Reading designs: `GET /rest/v1/user_card_designs?select=user_id,design&user_id=in.(id1,id2,...)` (max 50 ids per call, only authenticated users, blocked users are hidden automatically). `discoverable_profiles` already has `is_online` (true only if online and seen in the last 3 minutes).
- Saving: RPC `save_my_card_design(p_design jsonb, p_remixable boolean)` returns the cleaned design. RPC `reset_my_card_design()`. RPC `remix_card_design(p_source uuid)` copies another user's card into mine and returns it. RPC `list_remixable_cards(p_limit, p_offset)` returns user_id, profile_name, avatar_url, design, remix_count (max 30).
- Error codes (message text of the Postgres error): invalid_design, design_too_large (8 KB), invalid_color, invalid_bg_style, invalid_border, invalid_radius, invalid_show, invalid_hi_label, too_many_stickers (max 12), invalid_sticker_kind/position/rotation/scale/style/text/emoji/pack, sticker_pack_not_available, profile_required, not_available (remix refused), not_authenticated. Map all of them to friendly English text with the shared ErrorMapper.
- Design JSON (version 1). Everything optional; send only what the user changed:
```
{ "v":1,
  "theme": { "bg":"#FFF3C4", "bg2":"#FFD6E8", "accent":"#FFB800", "ink":"#1A1A1A",
             "style":"solid|gradient|dots|grid|paper|stripes", "border":"none|thin|bold|dashed", "radius":0-32 },
  "show":  { "age":true, "gender":true, "country":true, "city":true, "languages":true, "bio":false, "hobbies":false, "online_dot":true },
  "hi_label":"Namaste",              // max 12 characters, replaces the text on the Hi button
  "stickers":[ { "kind":"word|emoji|pack", "text":"hello!" (word, max 14), "emoji":"🔥" (emoji, max 16 chars),
                 "item":"<store sticker_pack code>", "idx":0 (pack),
                 "x":0..1, "y":0..1 (center of the sticker, as a fraction of the card width/height),
                 "rot":-45..45, "scale":0.5..2.5, "style":"sticker|plain|bubble|tape|outline|neon", "color":"#RRGGBB" } ] }  // max 12
```

## A. One shared card composable, used in Discover AND in the workshop preview
`PeopleCard(profile, design?)`. Cards without a design must still look bold (see F), so the whole app feels less flat even for users who never open the workshop. Card height is decided by the content only; the sticker layer is an overlay and never changes the layout (no jumping when a design loads).

## B. Online status
Remove the "Offline" pill completely. Show only a small green dot on the corner of the avatar when `is_online` is true and `design.show.online_dot` is not false. Offline = nothing at all. Same rule wherever the app currently shows the online/offline pill for people cards.

## C. Sticker layer (this is the "stuck on" look)
- Positions use the normalized x,y of the card; rotation and scale as given. Draw order: card background -> stickers -> avatar/name/languages text -> Hi button. So a sticker can never hide the name or the button and the button always stays tappable. The sticker layer does not take touches in Discover.
- Styles: `sticker` = die-cut look: thick white outline (about 4 dp) + soft drop shadow + very slight 3D lift (default); `plain` = no decoration; `bubble` = speech bubble; `tape` = a strip of washi tape behind the text; `outline` = outlined text; `neon` = glow. Words use a chunky rounded bold font. Emoji are rendered large.
- Words stickers presets (user can also type his own, max 14): Hello, Hi!, Namaste, Hola, Bonjour, Ciao, Salaam, Konnichiwa, Annyeong, Merhaba, Hey, Vibe, Let's talk.
- Fetch designs in ONE batch call for the visible user ids (max 50), cache in memory for 10 minutes, fade the design in. Max 12 stickers per card keeps the list smooth; no per-card heavy images; test scrolling on the Galaxy M02.

## D. Card Workshop (inside the existing Decoration Workshop)
Screen layout: live preview of MY card at the top (exactly the Discover card), a bottom sheet with 4 tabs below, a top bar with Undo, Redo, "Reset card" and a yellow Save button.
1. **Stickers tab:** sub-tabs Words (preset chips + a text box), Emoji (grid), Packs (free store sticker packs; locked ones show a lock). Tap = a sticker pops onto the card with a small "peel" animation. Drag with one finger, pinch to scale and rotate with two fingers, tap to select (selection shows a thin dashed outline and a mini toolbar: style, color, duplicate, to front, delete). Drag a sticker to a trash zone to delete. Haptic tick when it snaps near the center or the edges. Counter "7/12 stickers"; at 12 show a toast.
2. **Style tab:** background color swatches + two-color gradient, patterns (dots, grid, paper, stripes), accent color, border (none, thin, bold, dashed), corner radius slider 0-32.
3. **Show tab:** switches for age, gender, country, city, languages, bio, hobbies, green online dot, and a text field "Button text" (max 12). Text under it: "This changes only your card. Your full profile stays the same."
4. **Remix tab:** grid of other people's cards (mini previews with name and a "remixed N times" badge) from `list_remixable_cards`, paged. Tap a card = full preview + "Remix this". Remix replaces my card with a copy (stickers from packs I cannot use are dropped; show a note if some were dropped), the editor opens with it so I can tweak, and Undo restores my previous design. Switch "Let others remix my card" (default on).
Saving: call `save_my_card_design`; success = a little confetti burst and "Your card is live". Keep unsaved edits if the app goes to background; ask before leaving with unsaved changes.

## E. Safety
The server checks length and characters only, not bad words. Add a small local blocklist check for obviously abusive words in sticker text and the button text (show "Please choose another word"). Long-press on a card in Discover must offer the EXISTING report flow (report type profile) if the app already has it.

## F. Look and feel (minimal, Gen-Z, bold, modern)
Warm cream page background (#FFF8E6 light, true black in dark mode), ink black (#1A1A1A) text, one yellow accent (#FFB800). Default card (no design): rounded 28 dp, 2 dp ink border and a hard offset shadow (4 dp, ink color) = a "sticker sheet" neo-brutalist feel, avatar slightly tilted by 2 degrees, name in extra-bold rounded type, language chips as small pills (native -> learning with an arrow), Hi button as a chunky pill with the same hard shadow that "presses in" on tap. Subtle motion only: cards slide in with a tiny overshoot, stickers wiggle once when a design finishes loading, haptic on Hi tap. Respect "reduce animations". Contrast must stay readable on any user-chosen background: if the chosen `bg` is dark use light text automatically. All text English, 48 dp touch targets.

## Done when (two phones)
1. Phone A saves a card with 5 stickers, a pattern and a Hi label "Namaste"; phone B sees it on Discover within a minute (after pull-to-refresh), with no layout jump and smooth scrolling. 2. Offline users show no pill and no dot; online users show only the green dot. 3. Phone B remixes A's card, A's remix counter increases next time A opens the Remix tab. 4. A blocked user cannot see or remix the card. 5. Cards of users without a design look bold with the default style. 6. Invalid input (13th sticker, empty text) gives a friendly message, never a crash.

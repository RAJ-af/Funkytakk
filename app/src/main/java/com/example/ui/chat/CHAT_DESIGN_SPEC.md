# FunkyTalk Chat Design Spec

Source of truth for the Compose kit in this folder (ChatTheme.kt, ChatBubbles.kt, ChatChrome.kt, ChatKitGallery.kt).
Telegram and Signal were used only as a reference for patterns (grouped bubbles, inline time, swipe to reply, reactions).
No code or assets were copied from them.

Note on numbers: the radius, spacing and size values below are design choices made from accessibility rules
(WCAG contrast, 48dp touch targets) and common chat-app practice, not measurements taken from Telegram or Signal.
Tune them on a real phone with ChatKitGallery.

## 1. Color rules (when to use which)

| Role | Light (Butter) | Dark | Why |
|---|---|---|---|
| Wallpaper | #F3EFE4 | #0F1114 | Warm off-white so white bubbles look raised; near-black (not pure black) avoids smearing on OLED |
| Incoming bubble | #FFFFFF | #1E2128 | Neutral surface, high contrast with text |
| Outgoing bubble | #FFE7A0 | #54420F | Brand butter yellow; dark mode uses a muted olive so it does not glare |
| Text | #1B1B1B | #F3EFE2 | 15:1+ on bubbles |
| Meta on incoming | #6B6B66 | #A7A59C | 5:1 on white |
| Meta on outgoing | #6B5E2E | #D8C98F | Olive-brown keeps harmony with yellow (plain gray looks dirty on yellow) |
| Accent on incoming | #2F6FDE | #7FB0FF | Mentions, links, reply bar, unread dot |
| Accent on outgoing | #1F5BC4 | #8DB8FF | Darker blue: #2F6FDE only reaches ~3.9:1 on the yellow bubble |
| Voice button incoming | #F5B800 / dark icon | same | Strong on white |
| Voice button outgoing | #8A5A00 / white icon | #FFD25A / dark icon | Yellow on yellow would vanish |
| Read tick | accentOut | accentOut | Sent/Delivered use meta color, Read uses accent |
| Danger | #D92D20 | #FF7066 | Delete, recording dot |

Rules:
- Never hardcode bubble colors in screens. Always read LocalChatColors.
- One `darkTheme` flag drives both wallpaper and bubbles (ProvideChatTheme).
- Sender names use an 8-color palette chosen by hashing the user id (separate light and dark palettes, each >= 4.5:1).
- Presets (Butter, Sky, Mint, Graphite) only change outgoing bubble, accentOut, voice button, brand and wallpaper tint.

## 2. Size rules

- Message text 16sp / line 22sp. User scale 0.85x to 1.4x (LocalChatTextScale). Name 13sp medium. Quote 12/13sp. Meta 11sp. System line 12sp.
- Bubble radius 18dp; 5dp on the joining side inside a group. Padding 10dp horizontal, 6dp vertical.
- Max bubble width: min(340dp, 80% of screen).
- Spacing: 2dp inside a group, 8dp between groups, 8dp screen edge. Group = same sender within 2 minutes.
- Avatar 32dp, shown only beside the last incoming message of a group. Name only on the first message of a group.
- Media: 3dp inner padding, inner radius 15dp, aspect ratio taken from message metadata (never jumps).
- Voice: 240dp wide, 40dp play button inside a 48dp hit area, waveform 28dp tall, bars 3dp wide with 2dp gap.
- Every tappable control has a 48dp hit area.
- Emoji-only messages (1-3 emoji): 52 / 42 / 34sp, no bubble.

## 3. Motion

- New message: slide up 12dp + fade, 150-180ms.
- Highlight when jumping to a message: accent 18% background, fades over ~600ms (hold 1.2s).
- Swipe to reply: threshold 64dp, max 96dp, haptic tick on arming, arrow hint appears behind the bubble.
- Mic <-> send: 150ms crossfade. Mic scales 1.35x while recording.
- Typing dots: 1.2s loop. Respect "remove animations" (LocalReduceMotion).

## 4. States every message must handle

Sending (spinner) -> Sent (1 tick) -> Delivered (2 ticks) -> Read (2 accent ticks); failed (red retry);
edited label; forwarded header; reply quote; mention; search highlight; selected; highlighted; disappearing timer;
voice: loading / playing / paused / unread dot / speed; media: placeholder -> decrypting -> ready.

## 5. Accessibility

- messageDescription() gives TalkBack a full sentence per bubble.
- Contrast: all text pairs >= 4.5:1, graphics >= 3:1.
- Works with font scale up to 200% (no fixed heights on text containers).
- Reduce motion respected.

## 6. Features beyond Telegram/Signal (for a language-learning, E2EE app)

1. Tap any word in a message to look it up or save it to vocabulary (`onWordClick`, Devanagari-safe).
2. Inline translation strip + Listen, with a visible "Cloud AI" tag when the cloud was used (consent transparency).
3. Voice-note transcript slot (for learners).
4. Security notice at the top of each chat and a reusable "security code changed" line.
5. @-mention jump button and unread-mention badge.
6. System events collapse into one line instead of spamming the timeline.
7. Four chat color presets plus a text-size slider.
8. Gallery screen for visual QA.

## 7. Implementation order for the app

1. ChatTheme + MessageBubble/MessageText (colors fixed first).
2. Reply, mention, reactions.
3. Voice note (fix playback BEFORE styling).
4. Media + viewer (Telephoto).
5. System lines, date chips, unread divider, security notice.
6. Composer, mic hold, recording bar.
7. Long-press menu, selection mode, swipe to reply.
8. Translation, word tap, link preview, files, emoji-only.

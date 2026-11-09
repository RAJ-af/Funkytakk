# 2026-11-08 — Decoration Store fixes, English-only text, redesign, soft custom icons

## What the logs and screenshots showed (for you, not for Gemini)

- Every "Buy & Apply" request from the app (`POST /rest/v1/rpc/purchase_items`) came back 404 with PostgREST error PGRST202 (12 times). The function exists and the other new function from the same migration works (46 calls, all 200). So the app sends a JSON body whose key names do not match the function. The body lengths fit `{"p_item_ids":[13,1,29,8],"p_equip":true}`, but a different 7-letter key such as `p_apply` gives the same length, so I cannot tell which key is wrong. Prompt R1 makes Gemini print it.
- The "Information / फिर कोशिश करो" dialog is my mistake: my earlier prompts told Gemini to write Hindi error texts. The app should be English only (prompt R2).
- Name effects are missing from the store because my D1 prompt told Gemini to skip them. They exist on the server (4 items). Prompt R3 adds them.
- The bottom navigation bar and Material-style icons do not match your mockup.

Attach the two mockup images (Decoration Store and Preview) when you run R3.

## Prompt R1 — fix the purchase call (run first)

```text
Decoration Store में "Buy & Apply" हर बार "Information" dialog दिखाता है। Server logs के हिसाब से app का हर अनुरोध POST /rest/v1/rpc/purchase_items 404 (PGRST202) पाता है। मतलब भेजे गए JSON के keys फ़ंक्शन के parameters से नहीं मिलते।

सही रूप (ठीक यही नाम):
  POST /rest/v1/rpc/purchase_items
  body: {"p_item_ids":[13,1,29,8],"p_equip":true}
  p_item_ids = चुने हुए items की id (संख्याएँ, अधिकतम 20), p_equip = true यानी खरीदो और लगाओ, false यानी सिर्फ़ खरीदो।

1. बताओ कि app अभी कौन से keys भेजता है, और request का exact JSON, HTTP code और error body debug log में छापो (token कभी नहीं, सिर्फ़ debug build में)।
2. keys ठीक करो।
3. जवाब {"ok":true,"bought":[...],"already_owned":[...],"equipped":[...],"coins_spent":n,"remaining_balance":n} पढ़ो। अभी सब items मुफ़्त हैं, इसलिए already_owned में आना सामान्य है, गलती नहीं है। equipped की list से अपना लगाया हुआ हाल ताज़ा करो।
4. Server की errors: insufficient_coins, item_not_found, no_items, too_many_items, not_authenticated। हर एक के लिए अलग छोटा अंग्रेज़ी संदेश (R2 के नियम से)।
5. Remove बटन unequip_slot(p_slot) बुलाता है। p_slot के मान: avatar_frame, bubble_theme, chat_bg, name_effect, entry_animation। जाँचो कि यही मान जाते हैं।
6. Compile करके बदली files की सूची दो।
```

## Prompt R2 — English only, no generic dialogs

```text
App में हर user-facing text अंग्रेज़ी में होना चाहिए, strings.xml में।

1. पूरे project में देवनागरी (regex [\u0900-\u097F]) वाले सारे user-facing strings खोजो (dialogs, toasts, snackbars, error mapping, placeholders, GiftManager.kt की error मैपिंग, store, room, chat)। सूची दो: file, लाइन, पुराना text।
2. सबको छोटी, दोस्ताना अंग्रेज़ी में बदलो। उदाहरण: "Not enough coins", "This user is no longer in the room", "This room has ended", "Slow down a little", "Something went wrong. Please try again."
3. कच्चे server error codes (insufficient_coins आदि) user को कभी न दिखें।
4. "Information" वाला सामान्य dialog हटाओ। छोटी बातों के लिए snackbar, और कार्रवाई वाली गड़बड़ी के लिए screen के अंदर inline संदेश।
5. Code की टिप्पणियाँ और log लाइनें जैसी हैं वैसी रहें।
6. Compile करके बदली files की सूची दो।
```

## Prompt R3 — redesign the Decoration Store like the mockup

```text
Decoration Store और Preview को मेरे दोनों mockup जैसा बनाओ (images साथ लगी हैं)। minimal, सुंदर, हल्के (soft) custom icons, Material icons नहीं। docs/DESIGN_SYSTEM.md के tokens इस्तेमाल करो।

Store screen:
1. यह पूरी screen का अपना route है, नीचे का app navigation bar न दिखे (Home/Discover/Moments/Chats/Profile वाला)। सिर्फ़ नीचे चुने हुए items की ट्रे और बटन हों।
2. ऊपर: back बटन और FunkyTalk चिप, दाहिने coins की गोली "(सिक्का) 2,020 (+)"। अभी संख्या "+" बटन के पीछे कट रही है। संख्या पूरी दिखे, हज़ार पर कोमा।
3. बड़ा शीर्षक "Decoration Store", उसके नीचे "Make your profile and chat more unique with premium decorations!", और दाहिनी ओर 3D ताज (crown) का चित्र। यह चित्र image generation से बनाओ: पारदर्शी पीछे, चमकता सुनहरा 3D ताज, 512 px, WebP, 60 KB से छोटा, और drawable में डालो।
4. वर्ग टैब (गोल कार्ड जैसे, चुना हुआ पीला): All, Avatar, Entry, Name, Bubble, Theme, हर एक पर अपना soft icon। टैब की पंक्ति scroll हो सकती है।
5. हर वर्ग का अपना खंड, "View All" के साथ: Avatar Frames, Entry Animations, Name Effects (नया, पहले छूटा था), Message Bubbles, Chat Themes। Name Effects वाले कार्ड पर user का नाम उस effect के साथ दिखे। sticker packs इस screen में नहीं।
6. कार्ड: ऊपर छोटी preview (frame के लिए खाली गोला या अपना avatar frame के साथ, bubble में "Hi!", theme का रंग), नीचे नाम, और कीमत की गोली (सिक्का + संख्या, 0 हो तो "Free")। चुना हुआ कार्ड पीले घेरे और पीले गोले में ✓ के साथ। लगा हुआ item पर छोटा "Applied" निशान, और कार्ड पर Remove अलग से न भरे, उसे Preview या ट्रे में रखो।
7. Entry animations के कार्ड पर सब पर एक ही 🚀 है, वह हटाओ। हर preset (golden_beam, butterflies, portal_ring, heart_bloom) के लिए अलग छोटी thumbnail बनाओ: या तो preset का एक चमकदार frame Canvas से खींचकर, या image generation से चार अलग चित्र (सुनहरी किरण, तितली, चमकता घेरा, दिल) जो सिर्फ़ thumbnail हों। असली animation code से ही चले। ▶ दबाने पर वही छोटा preview चले।
8. नीचे की ट्रे: "N Selected" और ऊपर-नीचे खुलने वाला तीर, चुने items की छोटी thumbnails हर एक पर ✕, और दो बटन: "Preview" (आउटलाइन) और "Buy & Apply (N)" (पीला)। एक वर्ग में एक ही item चुना जा सके। नीचे का हिस्सा 3-button nav पर न कटे।

Preview screen:
9. ऊपर बड़ा कार्ड: user का अपना avatar चुनी हुई frame के साथ, चुनी हुई theme के पीछे, name effect के साथ नाम, और bubble में "Hi!"। ▶ बटन से entry animation एक बार चले।
10. बीच में टैब Avatar / Entry / Name / Bubble / Theme (केवल चुने हुए वर्ग), और उसके नीचे हर चुने item का पन्ना, ←/→ या swipe से, नीचे बिंदु।
11. नीचे दो बटन: "Buy Only" और "Apply All (N)" (पीला, ✓)।

सब text अंग्रेज़ी में (R2 के नियम)। Loading में skeleton, error पर inline संदेश और Retry। Compile करके बदली files की सूची दो।
```

## Prompt R4 — soft custom icon set (then use it app-wide)

```text
App के लिए अपना soft icon set बनाओ, FunkyIcons। docs/DESIGN_SYSTEM.md का भाग 9 (Icons) पढ़ो और उसी नियम से:

1. शैली: 24dp ग्रिड, 2dp stroke, गोल कोने और गोल सिरे, नरम आकार, कोई नुकीला हिस्सा नहीं। दो रूप: outline (सामान्य) और soft-filled (चुना हुआ, एक हल्का पेस्टल भराव)। रंग सिर्फ़ tokens से।
2. इन्हें खुद बनाओ (vector drawable XML या Compose ImageVector, हर एक की contentDescription): home, discover, moments, chats, profile, back, close, plus, check, gift, coin, crown, flame, person, play, bubble, tag, text-effect (A पर चमक), search, bell, share, more, mic, mic-off, headphones, people, send, emoji, lock, settings, report, block।
3. Decoration Store में सबसे पहले लगाओ (R3 के icons यही हों), फिर नीचे का navigation bar और top bars। बाकी screens Phase 2 के साथ बदलेंगे।
4. जहाँ Material icons इस्तेमाल हो रहे हैं उनकी पूरी सूची दो (file के हिसाब से गिनती), और कितने बदले और कितने बाकी हैं।
5. सब बदल जाने पर ही Material icons dependency हटाओ और build का आकार नापकर बताओ।
6. Compile करके बदली files की सूची दो।
```

## Order

R1, then R2, then R3 (with the two mockup images attached), then R4. After R4 you can run Phase 1 and Phase 2 from `2026-11-04-optimization-phases.md`; the icon rules are now part of `docs/DESIGN_SYSTEM.md`.

## Test (after R1)

1. Select one frame, Buy & Apply: no dialog, the frame shows on your avatar and on the other phone within a few seconds.
2. Select four items (frame, entry, bubble, theme) and Apply All: all four become "Applied".
3. Remove a frame: it disappears from your avatar and from the other phone.
4. Turn on airplane mode and press Buy & Apply: an English "Something went wrong. Please try again." message, not a dialog.

# 2026-11-04 — invites, room-ended handling, create-room kick-out, follow list, presence

Run the prompts one at a time (A to E) and test after each. A and C matter most.

## Background (for you, not for Gemini)

What the database shows (checked 2026-11-04, times in UTC):

- A private room (started 22:06:33) ended by `host_timeout` at 22:11:00, while a newer room
  by the same host had been running since 22:08:56. The newer room then ended at 22:11:15
  (`host_ended`). If the app does not filter end signals by room id, the OLD room's end
  signal can kick the host out of the NEW room. This fits "enter, then thrown out with
  a room-ended toast". It is a strong lead, not yet confirmed in the app code.
- Host-away grace is 3 minutes (cron runs every minute), so a room ends 3 to 4 minutes after
  the host leaves.
- `list_invitable_following(p_room_id)` returns only people the caller follows. With 0
  follows it returns nothing, so the "everyone is listed" behaviour comes from the app.
- `send_room_invite(p_room_id, p_user_id)` exists and writes a `notifications` row
  (`type = 'room_invite'`, with `room_id`), but there are NO `room_invite` notifications
  and NO `room_invites` rows in the database. So invites are not going through it today
  (most likely they are sent as a chat message).
- Push is sent by database triggers only for `dm_messages` and `group_message_recipients`.
  Nothing sends push for `notifications` rows. A room-invite push needs a backend change.

## Prompt A — room-ended signal must belong to the current room

```text
Voice room में दो समस्याएँ हैं: (1) room बनाते ही host बाहर फेंक दिया जाता है और नीचे toast "room ended" आता है, (2) Room Ended screen इतनी जल्दी हट जाती है कि user उसे देख या बंद नहीं कर पाता।

1. जाँच: "room ended" दिखाने वाले सारे रास्ते (voice_rooms का update/delete event, room_end_summaries का insert event, polling, room_participants का हटना, "room not found") खोजकर सूची दो। हर एक में बताओ कि वह room_id से filter होता है या नहीं।

2. हर end signal को सिर्फ़ तब मानो जब उसका room_id अभी के room के id के बराबर हो। किसी दूसरे या पुराने room का event पूरी तरह ignore करो। सारी Realtime subscriptions में room_id का filter लगाओ।

3. Room बनाने या join करने के बाद पहले 10 सेकंड में "row नहीं मिली" या "participant नहीं मिला" को end मत मानो। 2 सेकंड के अंतर से 3 बार दोबारा जाँचो, फिर भी न मिले तभी end मानो।

4. जब असली end signal मिले (ended_reason के साथ), तो हमेशा: Agora में leave() और release(), फिर Room Ended screen।

5. Room Ended screen अपने आप कभी न हटे। उसमें साफ़ "Close" बटन हो, back से और नीचे swipe से भी बंद हो, और वह तभी बंद हो जब user चाहे। उसमें room का नाम, कितनी देर चला और ended_reason का आसान संदेश (host ने बंद किया / host 3 मिनट से दूर था) दिखाओ। एक room के लिए यह screen एक ही बार दिखे।

6. हर बार room से बाहर निकलते समय log लिखो: किस कारण से (user ने leave दबाया, host ने end किया, कौन सा signal, कौन सा room_id)।

7. Compile करके बताओ कि कौन सी files बदलीं।

Test: (a) एक room बनाओ और host से 3 मिनट के लिए बाहर आ जाओ, फिर तुरंत नया room बनाओ और पुराने room के खत्म होने (host_timeout) तक नए room में रहो। नया room बंद नहीं होना चाहिए। (b) Room को खुद End करो, Room Ended screen तब तक रुके जब तक Close न दबाओ।
```

## Prompt B — invites (banner, notification tap, push tap)

```text
Room invites में तीन समस्याएँ हैं। पहले यह बताओ (कोई बदलाव मत करो): invite अभी किस रास्ते से भेजा जाता है, send_room_invite(p_room_id, p_user_id) RPC से, या chat message के रूप में? और invite का room_id कौन सा id है (UUID या छोटा code)?

फिर ये ठीक करो:
1. App खुली हो और invite आए तो ऊपर से Discord जैसा एक card (heads-up banner) दिखे: भेजने वाले का नाम और dp, room का नाम, "Join" और "Dismiss" बटन। 6 सेकंड में अपने आप हटे, ऊपर swipe से हटे, और tap करने पर invite popup खुले। एक साथ कई invites आएँ तो एक के बाद एक दिखें।

2. Notifications screen में room invite पर tap करने से कुछ नहीं होता। इसे ठीक करो: tap पर room की स्थिति जाँचो (voice_rooms में उस id की row, is_live = true और status = 'active')। अगर room चालू है तो Join popup खोलो। नहीं तो साफ़ संदेश "यह room खत्म हो चुका है" दिखाओ और invite को पढ़ा हुआ चिह्नित करो।

3. Push notification पर tap करने पर भी यही रास्ता चले (ऊपर वाला बिंदु 2)। Join popup दिखाने से पहले room की स्थिति जाँचो। room खत्म हो तो Join बटन मत दिखाओ।

4. Join दबाने पर जो id get-room-token को जाए वह room का UUID हो, छोटा join code नहीं।

5. Compile करके बताओ कि कौन सी files बदलीं।
```

## Prompt C — Invite Friend list when you follow nobody

```text
Invite Friend (follow वाली) list में अगर user किसी को follow नहीं करता, तो अभी सारे users (dp, online/offline, नाम, देश) दिख जाते हैं। इसे ठीक करो:

1. यह list सिर्फ़ RPC list_invitable_following(p_room_id) से बने। इस RPC के अलावा किसी और query या "सब users" वाले fallback से कभी नहीं।
2. 0 नतीजे आएँ तो खाली screen का संदेश दिखाओ: "आप अभी किसी को follow नहीं करते। Invite करने के लिए पहले लोगों को follow करें।" और एक "Discover" बटन।
3. null avatar, null country, लंबे नाम और 100 नतीजों पर भी app crash न हो। list lazy (LazyColumn) हो और हर item को स्थिर key (user_id) मिले।
4. Loading और error की अलग-अलग screen हों।
5. Compile करके बताओ कि कौन सी files बदलीं।
```

## Prompt D — presence indicators (online / offline / speaking)

```text
सब जगह (room seats, follow list, invite list, chat list) एक ही PresenceAvatar composable इस्तेमाल करो, जिसमें तीन स्थितियाँ हों:

1. Online: dp के चारों ओर हरी रिंग (outer outline)। कोई dot नहीं।
2. Offline: dp के ऊपर बिना रंग (सिर्फ़ outline) वाले चाँद (moon) का icon overlay, dp का कोना घेरता हुआ। रिंग नहीं।
3. Speaking: dp के ऊपर बोलते समय चलता हुआ animated voice indicator (आवाज़ की पट्टियाँ या फैलती हुई ripple)।

नियम: Speaking सबसे ऊपर, फिर Online, फिर Offline। animation सिर्फ़ उन्हीं avatars पर चले जो बोल रहे हों (infiniteTransition), ताकि अटके नहीं। light और dark दोनों theme में साफ़ दिखे। accessibility के लिए contentDescription हो ("Online", "Offline", "Speaking")। null avatar पर placeholder दिखे। Compile करके बताओ कि कौन सी files बदलीं।
```

## Prompt E — error 17 follow-up

```text
अगर कोई room "ended" दिखाया जाए तो उस समय Agora engine में leave() और release() ज़रूर हो चुका हो, ताकि अगला join error 17 (ERR_JOIN_CHANNEL_REJECTED) न दे। हर room-ended रास्ते में यह जाँचो और log करो कि leave/release हुआ। Compile करके बताओ।
```

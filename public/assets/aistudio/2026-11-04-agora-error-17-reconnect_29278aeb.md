# 2026-11-04 — Agora reconnect stability (error 17) + events card check

Run Prompt A first, test on 2 phones, then run Prompt B.

## Background (for you, not for Gemini)

Agora error 17 is `ERR_JOIN_CHANNEL_REJECTED`. As far as I know it means join was
called while the user is already in the channel (or a join is already in progress).
That fits the symptom: it appears on reconnect, then the next try works. So the app
is most likely calling `join()` a second time when it should let Agora reconnect by
itself. This is a likely cause, not yet confirmed in the code: Prompt A makes Gemini
find the real call sites.

## Prompt A — voice reconnect stability

```text
Voice room में reconnect पर कभी-कभी Agora error 17 (ERR_JOIN_CHANNEL_REJECTED) आता है और अगली कोशिश में सब ठीक हो जाता है। यानी शायद join दो बार call हो रहा है। इसे स्थिर करो।

1. जाँच: पूरे project में जहाँ भी VoiceEngine.join() / joinChannel बुलाया जाता है, उन सब जगहों की सूची दो (file + line + कौन सा trigger: network बदलना, app foreground, recomposition, ViewModel दोबारा बनना, token refresh, retry)। बताओ कि कौन सी जगह दूसरी बार join करवा सकती है।

2. AgoraVoiceEngine में join एक बार में एक ही चले:
   - state machine रखो: Idle, Joining, Joined, Leaving।
   - Joining या Joined हो तो नया join() चुपचाप ignore करो (log में लिखो)।
   - join और leave को Mutex से serialize करो।

3. Reconnect पर दोबारा join मत करो। जब connection state RECONNECTING हो तो Agora खुद reconnect करता है, बस UI पर "Reconnecting..." दिखाओ।

4. Token refresh सिर्फ़ renewToken से हो: onTokenPrivilegeWillExpire पर get-room-token से नया token लो और renewToken(newToken) बुलाओ। इसके लिए leave/join मत करो।

5. अगर join सच में फिर करना पड़े (connection FAILED या हमेशा के लिए टूटा): पहले leave() करो और leave पूरा होने (onLeaveChannel) का इंतज़ार करो, फिर join। retry में exponential backoff रखो (1s, 2s, 4s) और अधिकतम 3 कोशिशें। retry job room छोड़ने पर cancel हो।

6. Error 17 आए तो: अगर connection state CONNECTED है तो इसे सफलता मानो और कुछ मत करो। नहीं तो leave() करके एक बार दोबारा join करो।

7. Engine का एक ही instance हो, वह Composable में नहीं बल्कि ViewModel/session scope में बने, और room छोड़ते समय leave() के बाद release() हो। screen rotation या recomposition से नया engine या नया join नहीं बनना चाहिए।

8. Logging (token कभी मत छापो): हर connection state बदलाव (state + reason), हर error code, हर join/leave/renewToken call और उसका trigger, timestamp के साथ।

9. Compile करके बताओ कि कौन सी files बदलीं।

Test (मैं करूँगा): 10 सेकंड airplane mode, WiFi से mobile data बदलना, 1 मिनट screen lock, app को 30 सेकंड background में रखना, फ़ोन rotate करना। हर बार error 17 आया या नहीं।
```

## Prompt B — events section check (beta test event)

```text
Backend में "Funkytalk Beta Test Event" नाम का एक test event जोड़ा गया है (table events, category_tag "Language", join_room_id और join_url दोनों null)। Events section में जाँचो:

1. यह event सूची में दिखता है (title, description, banner, समय, timezone label IST)।
2. समय स्थानीय (local) समय में सही दिखता है। event_time UTC में आता है, उसे फ़ोन के timezone में बदलो।
3. जब join_room_id और join_url दोनों null हों तो Join button crash न करे। उस हाल में button बंद रखो या "Link soon" जैसा साफ़ संदेश दो।
4. पुराना event (Daily Chinese Practice, जिसका समय बीत चुका है) कैसे दिखता है, और past events सूची में रहते हैं या नहीं। जो अभी हो रहा है वह बताओ, बदलो मत।
5. सूची खाली होने पर, loading पर और network error पर साफ़ screen दिखे।

बदलाव किए हों तो compile करके files की सूची दो।
```

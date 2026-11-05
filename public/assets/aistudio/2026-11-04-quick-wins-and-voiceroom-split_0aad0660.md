# 2026-11-04 — Audit quick wins + splitting VoiceRoomScreen.kt (about 7,000 lines)

Run in this order: Q1, Q2, then S0 (read-only map). Test on a phone after each.
Do NOT run Phase 1 or Phase 2 of `2026-11-04-optimization-phases.md` on the voice room screen until it is split.

## What I changed from the audit (for you, not for Gemini)

- Skip 4.4 (Agora destroy hook "if the app crashes"): no code runs when a process crashes, and the native handles die with the process. The leave and destroy calls we added are enough.
- 6.2 (remove location permissions): first check that nothing uses location. Removing a used permission breaks that feature.
- 3.2 (heartbeat 60s to 120s): leave it. Online status depends on that interval on the server side.
- 5.2 (remove "overlapping" libraries): do not remove anything blind. Only after checking each one is unused.
- 5.1: ABI filter only for the debug build. Release goes out as AAB, which already sends each phone only its own CPU type.
- 5.3: the "~35 MB test APK" number is an estimate, not measured. Measure after the change.

## Prompt Q1 — safe quick wins

```text
ये 6 छोटे सुधार करो। व्यवहार मत बदलो:

1. CircleFlag.kt: हर recomposition पर नया ImageLoader.Builder(context).build() बन रहा है। इसे हटाकर app का ग्लोबल (singleton) ImageLoader इस्तेमाल करो। बताओ कि और कहाँ-कहाँ ऐसा ही नया ImageLoader बनता है।
2. app/build.gradle.kts: ABI filter (सिर्फ़ arm64-v8a) केवल debug buildType में लगाओ, release में नहीं (release AAB खुद हर फ़ोन को उसका ABI देता है)। टिप्पणी लिखो कि debug में x86 emulator नहीं चलेगा।
3. Logs: पूरे project में Log.* की हर लाइन खोजो जो bearer token, access_token, Authorization, refresh token या पूरा user id छापती है (खासकर VoiceRoomRealtimeManager.kt और SupabaseSessionManager.kt)। token कभी न छपे, debug में भी नहीं (ज़रूरत हो तो सिर्फ़ पहले 4 अक्षर)। बाकी Log.d को BuildConfig.DEBUG के अंदर रखो।
4. Backup: AndroidManifest में allowBackup के लिए backup_rules.xml और data_extraction_rules.xml (Android 12+) दोनों में funky_talk_prefs को exclude करो, और token रखने वाली बाकी prefs/DB files भी।
5. Coil cache: FunkyTalkApplication के ImageLoader में memoryCache (app की मेमोरी का 25%) और diskCache (150 MB) साफ़ तय करो।
6. Bottom sheets और dialogs (PeopleSheet, FollowersListDialog, InviteFriendSheet और बाकी सब ModalBottomSheet/Dialog): जहाँ navigationBarsPadding() नहीं है वहाँ जोड़ो।

हर बदलाव के बाद compile करो और बताओ कि कौन सी files बदलीं। जो जगहें मिलीं पर बदली नहीं उनकी भी सूची दो।
```

## Prompt Q2 — startup, resume, polling, deep links

```text
1. FunkyTalkApplication.onCreate में PushNotificationManager.initialize() और PresenceManager.initialize() को main thread से हटाओ (background या App Startup)। MainActivity के 5 initializers पहला frame बनने के बाद चलें।
2. MainActivity में ON_RESUME पर 5 network calls एक साथ चलती हैं। उन्हें एक के बाद एक (थोड़ी देरी से) चलाओ और एक ही resume में 30 सेकंड के अंदर दोबारा मत चलाओ।
3. AccountSuspendedScreen में while(true) हर 3 सेकंड polling है। इसे 10 सेकंड करो और screen छोड़ते ही बंद करो।
4. Deep link funkytalk://room/{code}: code का format जाँचो (अक्षर-अंक, तय लंबाई, जैसे FT4041)। गलत हो तो ignore करो। user logged-in न हो या session तैयार न हो तो link को सँभालकर रखो और login के बाद ही room खोलो।
5. SharedPreferences की पहली read main thread पर न हो (DataStore migration अभी नहीं)।
6. बदलाव से पहले और बाद में cold start का समय नापो (adb shell am start -W या Android Studio Profiler) और आँकड़े बताओ। Compile करके files की सूची दो।
```

## Prompt S0 — map VoiceRoomScreen.kt (read-only, no code changes)

```text
VoiceRoomScreen.kt लगभग 7000 लाइन की है। कोई code मत बदलो। इसका नक्शा बनाओ:

1. फ़ाइल के हर top-level composable, function और class की सूची: नाम, लाइन की सीमा (से-तक), कितनी लाइन।
2. हर हिस्से के बारे में: कौन सी state पढ़ता और लिखता है (remember/mutableStateOf/ViewModel), कौन से LaunchedEffect/DisposableEffect/collect चलाता है।
3. इन समूहों में बाँटो और हर समूह का कुल आकार दो: seats grid, room chat, नीचे के controls (mic, gift, आदि), top bar, sheets (People, Invite, Gifts, Settings), dialogs, overlays और animations, Realtime/RTC/polling का काम, helper और mapper functions।
4. कुल कितने `remember`/`mutableStateOf` हैं और कितने composables 300 लाइन से लंबे हैं।
5. सुझाओ कि इसे ~10-14 files (हर एक लगभग 500 लाइन या कम) में कैसे बाँटा जाए: नई file का नाम, उसमें क्या जाएगा, और कौन सी state उसे parameter के रूप में दी जाएगी। साथ में सबसे सुरक्षित क्रम बताओ (पहले वह हिस्सा जिसमें state सबसे कम है)।
6. जो हिस्से सबसे जोखिम वाले हैं (Prompt A का room-ended और grace लॉजिक, Agora की leave/release, Realtime filters) उन्हें "छूना मना" सूची में रखो।
```

## Prompt S1 — extract one group (use after I read the S0 map)

```text
VoiceRoomScreen.kt से सिर्फ़ यह समूह अलग file में ले जाओ: [समूह का नाम]।

नियम:
1. व्यवहार, नाम और navigation बिल्कुल न बदलें। यह सिर्फ़ code को हिलाना है। नई file ui/screens/voiceroom/ में, लगभग 500 लाइन तक।
2. निकाला गया हिस्सा stateless composable हो: ज़रूरी state और lambdas parameters से आएँ।
3. Prompt A का room-ended, grace और Realtime filter वाला code जैसा है वैसा रहे।
4. Compile करो। फिर बताओ: नई file, VoiceRoomScreen.kt की नई लाइन-संख्या, और कौन सी चीज़ें parameters बनीं।
5. कुछ टूटे तो वहीं रुको और बताओ। अगला समूह खुद मत छुओ।

Test (मैं करूँगा): room बनाना, join, mic चालू-बंद, चैट भेजना, invite, room end और Room Ended screen।
```

## Prompt S2 — later: state to a ViewModel (design after the split)

After the file is split, move state and the Realtime/RTC orchestration into a `VoiceRoomViewModel` with one immutable `VoiceRoomUiState`. I will write this prompt from the S0 map.

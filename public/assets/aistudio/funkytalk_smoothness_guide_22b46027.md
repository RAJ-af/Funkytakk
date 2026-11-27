# FunkyTalk: Professional Smoothness Guide (500 users target)

Ye ek **guide** hai, order nahi. Jo cheez tumhare app me fit na baithe use skip karo. Sab kuch ek saath mat karo; Part 3 ka priority order follow karo.

---

## 1. Abhi ki audit (Supabase project: Funkytalk, read-only check)

**Jo theek hai**
- Region **Mumbai (ap-south-1)**: India users ke liye sahi.
- Database chhota aur healthy: 22 auth users, cache hit **100%**, connections 18/60.
- Reads/writes me zyadatar RPC functions use ho rahe hain (`get_moments_feed`, `get_my_groups`...). Ye sahi pattern hai.
- Admin/staff functions me `private.is_staff()` jaisa check hai. Security design achha hai.
- Feed pagination (limit max 50) pehle se hai.

**Jo problem hai**
| # | Problem | Asar |
|---|---------|------|
| 1 | `cron.job_run_details` = ~300k rows, **~70 MB (DB ka ~70%)**, kabhi clean nahi hoti | DB limit (free 500 MB) jaldi bharega, background I/O badhta hai |
| 2 | 2 cron jobs **har 1 minute** (84k+ runs), spikes 3-6 sec | Chhote shared CPU pe constant churn |
| 3 | Random latency spikes **3-15 sec** (heartbeat, support_session, realtime query sab me) | Yahi "kabhi kabhi data late aata hai" ka sabse bada signal hai |
| 4 | App open pe bahut saari alag calls (support_session, account_review, deletion_status, touch_device, register_push_token, heartbeat, cosmetics...) | Ek bhi slow hui to UI ruki rehti hai |
| 5 | `heartbeat()` har call pe row UPDATE karta hai | 500 users pe bahut writes |
| 6 | Realtime me 16 tables; Realtime WAL query DB ki sabse badi load | Free tier pe CPU khata hai |
| 7 | 18 edge functions + 4 backends (Firebase, Supabase, Appwrite, Cloudinary) | Zyada hops, zyada cold starts |
| 8 | 32 FK indexes missing, 4 RLS initplan warnings | Abhi chhota asar, 500 users pe badhega |

**Honest note:** Mere paas Supabase ki CPU/Disk-IO graph nahi thi aur compute size ya plan tools se nahi dikha. Spikes ka sabse sambhavit karan free-tier shared CPU ka burst khatam hona hai (jo background jobs aur chhoti DB ke saath mil kar hota hai), par isko pakka karne ke liye Dashboard me **Reports -> Database** me CPU aur Disk IO graph dekho. Agar graph me CPU baar baar 100% ya IO budget khatam dikhe to confirm ho jayega.

DB me data sirf 97 MB aur 22 users hain. **Yaani problem "load" nahi, setup aur free-tier ki seema hai.** 500 users ke liye database kaafi hai agar neeche ka kaam kar lo.

---

## 2. Dost ka idea (cache server + WebRTC) kyun nahi

- WebRTC peer-to-peer media/data ke liye hai. Database cache sync ke liye nahi.
- Beech me ek aur server = ek aur bill, ek aur bug, ek aur jagah jahan data purana ho sakta hai.
- Asli solution **app ke andar local cache** hai (Part 4). Free, fast, aur professional apps (Instagram, WhatsApp) yahi karte hain.

---

## 3. Priority order (isi order me karo)

### P0 (aaj/kal, 1-2 ghante)
1. `funkytalk_supabase_fixes.sql` ke **Section 1 aur 2** chalao (cron history cleanup + frequency kam).
2. Dashboard -> Reports -> Database me CPU/IO graph dekho (confirm karne ke liye).
3. App open pe **cached data pehle dikhao** (Part 4.1). Ye smoothness ka sabse bada fayda deta hai.

### P1 (is hafte)
4. App open pe calls ko **ek `get_app_bootstrap()` RPC** me jodo (Part 4.2).
5. Heartbeat interval 60 sec karo, SQL ka Section 5 (check karke) chalao.
6. Realtime sirf active chat/room pe, screen chhodte hi unsubscribe (Part 4.5).
7. SQL Section 3, 4, 6 chalao.

### P2 (launch se pehle)
8. Services simplify karo (Part 6).
9. Load test (Part 8).
10. **Pro plan** le lo (Part 7).

---

## 4. App-side: "professional app" jaisa feel kaise aata hai

Professional apps fast isliye lagte hain ki wo **network ka wait user ko dikhate hi nahi**.

### 4.1 Cache-first (stale-while-revalidate): sabse zaruri
Flow:
1. Screen khulte hi **local saved data turant dikhao** (0 ms).
2. Background me fresh data lao.
3. Aaya to UI chupchap update karo. Fail hua to purana data dikhta rehne do.

Kya cache karo: profile, discover list, moments feed (pehla page), my groups, DM list, store items, cosmetics, language lists.
Kya cache **mat** karo: coin balance ka final value (server hi sach hai), chats ke naye messages (realtime se aate hain).

Tools (stack ke hisab se):
- **Flutter:** `hive` / `isar` / `drift`, ya Riverpod ke saath `cached_network_image` images ke liye.
- **React Native:** `@tanstack/react-query` + `persistQueryClient` (MMKV storage).
- **Native Android (Kotlin):** Room + Flow (DB hi single source of truth).

Rule: **UI hamesha local DB/cache se padhe, network sirf cache ko update kare.**

Cache TTL (kitne der baad fresh lana):
| Data | Fresh karo |
|------|-----------|
| Profile (apna) | app open pe + edit ke baad |
| Discover / Moments feed | app open pe + pull-to-refresh |
| Store items, gifts catalog | 24 ghante (ya app version change pe) |
| Language/topic lists | hafte me ek baar |

### 4.2 App open pe network budget
Target: **app open pe max 2-3 requests, parallel me.**
Abhi ~8 alag calls ho rahi hain. Inhe ek RPC me jodo:

```
get_app_bootstrap() -> {
  profile, wallet_balance, equipped_cosmetics,
  moderation_status, deletion_status, support_unread, unread_counts
}
```
- Ek hi Postgres function me sab data jodo (jaise `get_moments_feed` ne kiya hai).
- Ye "boot" call cache-first ke saath chalao: UI pehle cache se bane, bootstrap background me update kare.
- Jo chizein turant nahi chahiye (`register_push_token`, `touch_device`) unhe UI dikhne ke **baad** (2-3 sec delay) fire karo, ya din me ek baar.
- `heartbeat` ko app open ke blocking path me mat rakho.

### 4.3 Skeleton, optimistic UI, aur "kabhi blank nahi"
- Spinner ki jagah **skeleton** (shimmer) dikhao, aur wo bhi sirf pehli baar jab cache khaali ho.
- **Optimistic UI:** like, follow, send message turant UI me dikhao, server confirm baad me. Fail hua to chhota "retry" dikhao.
- Kabhi poori screen blank mat chhodo. Error pe bhi purana data + chhota banner "Connection slow".

### 4.4 Network discipline
- **Timeout** har request pe (10-15 sec) + **retry with backoff** (1s, 2s, 4s, max 3).
- **Dedupe:** same data ke liye ek saath 2 requests mat bhejo (tab switch, double tap).
- **Debounce:** search me 300-400 ms.
- **Pagination:** list me `limit 20` + infinite scroll. `select *` mat karo, sirf zaruri columns.
- **Prefetch:** user moments feed pe ho to agle page ka data aur next 2-3 images pehle se load karo.
- Offline: actions ek chhoti queue me rakho, net aane pe bhejo.

### 4.5 Realtime sahi tareeke se
- Subscription sirf **active chat / active voice room** pe. Screen band = `unsubscribe`.
- Home/Discover/Feed pe realtime ki zarurat nahi. Pull-to-refresh ya 60 sec polling kaafi hai.
- "Typing…", online dots jaise chhote events ke liye table-changes ki jagah **Broadcast / Presence** use karo (DB hit nahi hota).
- Ek screen = zyada se zyada 1-2 channels. Ek user ke liye total 3-4 se zyada nahi.
- App background me jaye to channels band karo; wapas aaye to ek baar refetch karo.

### 4.6 Heartbeat / presence
- Client se heartbeat **60 sec** pe (30 nahi). App foreground me ho tabhi.
- Online ka threshold server pe **3 minute** rakho (`last_seen > now() - 3 min`).
- App background/close pe `go_offline()` ek baar bhejo.

### 4.7 Images aur media (Cloudinary)
- URL me transformations lagao: `f_auto,q_auto,w_400` (thumbnail), `w_1080` (full). Original bada kabhi mat bhejo.
- List me **thumbnail** use karo, detail screen me bada.
- App me image cache (Flutter `cached_network_image`, RN `expo-image`/`fast-image`, Android Coil/Glide).
- Gift animations / banners / keyboard PNGs app ke andar **bundle** karo ya pehli baar download karke disk cache karo. Har baar network se mat lao.
- Upload ke time image compress karo (max ~1080 px, quality ~80%) pehle client pe, phir upload.

### 4.8 Startup speed
- Splash ke baad pehla screen **cache se** dikhao. Auth token local me ho to login screen skip.
- Heavy kaam (analytics, push token, remote config) UI dikhne ke baad.
- Bade assets (3D, animations) lazy load. Pehle screen ke liye sirf zaruri.

---

## 5. Backend rules (jab naye features add karo)

**Database**
- Jis column pe `where`/`order by`/join ho uspe index. Naya table banao to **har foreign key pe index**.
- Naya list-screen = **ek RPC function** jo JSON wapas de (pagination ke saath).
- `select *` nahi, `offset` bade pages pe nahi (feed 50 se aage jaye to cursor/keyset pagination: `created_at < last_seen_value`).
- Counters (likes_count, comments_count) pe triggers theek hain; par bade aggregate (`count(*)` pura table) screen pe mat chalao.

**RLS**
- Hamesha `(select auth.uid())` likho, `auth.uid()` nahi.
- Ek table + ek action pe **ek hi permissive policy**; staff/user ko OR se ek me jodo.
- Complex RLS ki jagah `security definer` RPC with explicit checks (jaisa tum already karte ho).

**Edge functions**
- Sirf tab jab **secret / third-party API** chahiye: payments, push (FCM), Cloudinary signature, email, AI, token generation (voice room).
- Simple read/write = RPC ya direct client query. Edge function me cold start (1-2 sec) lagta hai.
- Cron se call hone wale functions ko kam baar chalao (15 min+).
- Jo functions webhook/cron ke liye `verify_jwt=false` hain (send-push, verify-firebase-token, verify-email, beta-signup, etc.), unme **secret header ya signature check zaroor** ho. Warna koi bhi URL ko call kar sakta hai.
- Naye features pe pehle sochho: "ye RPC se ho sakta hai?"

**Cron (pg_cron)**
- Jitna kam, utna achha. Cleanup jobs raat me ya 10-15 min pe.
- Har cron job ki history automatic purge ho (SQL section 1 isko handle karta hai).
- Naya cron job lagane se pehle poochho: "kya ye user-action pe trigger ho sakta hai?"

**Security (launch se pehle)**
- Advisors me `discoverable_profiles` (SECURITY DEFINER view) test karke fix karo (SQL section 8).
- Jin tables me RLS on hai par policy nahi (account_deletions, push_tokens, ai_requests, etc.) wo sirf server/edge se use hote hain to theek hai. Bas app se unhe direct query mat karo.
- Wallet/gift/store functions (`send_gift`, `purchase_item`) pe **rate limit + idempotency** rakho (double-tap pe 2 baar coin na kate).
- Supabase Auth me password login use karte ho to Leaked Password Protection on karo.

---

## 6. Services simplify karo (decision guide)

Abhi: Firebase + Supabase + Appwrite + Cloudinary. Har ek alag latency, alag keys, alag failure point.

| Service | Kaam | Meri raay |
|---------|------|-----------|
| **Supabase** | Database, realtime, RPC | Core. Rakho. |
| **Firebase** | Login (verify-firebase-token) + push (FCM) | Rakho jab tak login Firebase se hai. Baad me Supabase Auth pe move sochna. |
| **Cloudinary** | Images/media | Rakho. Direct signed upload (client -> Cloudinary), edge function sirf signature de. |
| **Appwrite** | `upload-to-appwrite` edge function se files | **Ek media store chuno.** Agar Cloudinary sab handle kar sakta hai to Appwrite hatao (ek hop kam, ek edge function kam). |

Migration ek saath mat karo: naye uploads pehle Cloudinary pe bhejo, purane Appwrite files rehne do jab tak zarurat ho.

---

## 7. 500 users ke liye plan

**Free tier ki seema (approx. docs me current limits check karo):**
- Shared CPU, chhoti RAM: burst ke baad slow.
- 7 din inactivity pe project **pause** ho jata hai.
- Realtime concurrent connections aur messages/sec ki limit hai (docs: supabase.com/docs/guides/realtime/limits).
- Edge function invocations ki monthly limit hai.
- Backups nahi (ya bahut limited).

**Capacity ka andaza (500 registered users):**
- Concurrent online usually 10-20% = **50-100 users**.
- Har user ~1 realtime connection + 2-3 channels = 100-300 channels, free limits ke paas aa sakta hai agar 4.5 ke rules follow na ho.
- Heartbeat 60 sec pe 100 online users = ~100 writes/min (theek). 30 sec pe aur throttle ke bina = double.

**Sujhaav:**
- **Closed beta (abhi se 100 users tak):** free tier chalega agar P0 + P1 ho jaye.
- **Public launch se pehle: Pro plan ($25/month)** lo. Fayda: no pause, backups, zyada limits, compute upgrade ka option. Agar graph me CPU high dikhe to compute size ek step upar (Small) karo. Ye ek click ka kaam hai, naya project nahi banana padega.
- Launch se pehle **project active rakho** (7 din inactivity pause se bacho).

---

## 8. Launch se pehle load test (free, 1 ghanta)

Real test ke bina "500 users chal jayenge" bas andaza hai.

1. **k6** install karo (free). Ek simple script: 50 virtual users, app-open flow (bootstrap RPC, feed RPC) 2 min tak.
2. Dekho: **p95 latency** (target < 500 ms), error rate (target 0%).
3. Test ke time Dashboard -> Reports me CPU, connections, realtime connections dekho.
4. Test **staging project** pe karo (naya free project) ya off-peak time pe. Real users ke time pe mat.
5. Realtime test: 50 users ek hi chat room me, 1 message/sec. Delivery delay dekho.

Pass criteria:
- App open (cache ke saath) < 1 sec me UI dikhe.
- Fresh data < 2 sec me.
- Spikes (> 3 sec) test me nahi aate.

---

## 9. Hafte me ek baar monitoring (10 minute)

SQL Editor me chalao:

```sql
-- Sabse mehnge queries
select left(regexp_replace(query,'\s+',' ','g'),100) q, calls,
       round(mean_exec_time::numeric,1) mean_ms, round(max_exec_time::numeric,0) max_ms
from pg_stat_statements
where query not ilike '%pg_stat%' and query not ilike '%cron.%'
order by total_exec_time desc limit 10;

-- DB size aur sabse bade tables
select pg_size_pretty(pg_database_size(current_database())) db_size;
select relname, pg_size_pretty(pg_total_relation_size(oid)) size
from pg_class where relkind='r' and relnamespace in ('public'::regnamespace,'cron'::regnamespace)
order by pg_total_relation_size(oid) desc limit 8;

-- Connections
select count(*) from pg_stat_activity;
```

Dashboard: **Advisors** (Performance + Security) har naye feature ke baad chalao, aur **Reports -> API / Database** dekho.
Rule of thumb: koi cheez p95 > 1 sec ho to uspe kaam karo.

---

## 10. Naya feature add karne se pehle checklist

- [ ] Kya ye ek RPC se ho sakta hai (edge function ki zarurat nahi)?
- [ ] Naye table pe RLS + `(select auth.uid())` + FK indexes?
- [ ] List hai to pagination (limit 20) aur sirf zaruri columns?
- [ ] UI cache-first hai, skeleton hai, error state hai?
- [ ] Realtime chahiye? To kis screen pe, aur unsubscribe kab?
- [ ] Naya cron chahiye? Kitni baar? (kam se kam)
- [ ] Coin/wallet se judi cheez hai to idempotent + rate limited?
- [ ] 2 phones pe test kiya, slow network (3G throttle) pe bhi?

---

## 11. Jo mat karna

- Beech me alag cache server ya WebRTC sync.
- Har screen pe realtime subscription.
- App open pe 8-10 alag calls.
- Edge function har chhoti cheez ke liye.
- Cron har minute.
- Poori file ek saath SQL Editor me bina padhe chalana.

---

*Files: `funkytalk_supabase_fixes.sql` (database fixes, sections me), ye guide (app + architecture).*

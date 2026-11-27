## Screen share in voice rooms (host only)

Room audio stays on Agora. Only the screen video uses Daily.co, so the two providers' free minutes stay separate.

**Files**
- `supabase/migrations/20261127_room_screen_share.sql`
- `supabase/functions/get-screenshare-token/index.ts`
- `docs/gemini-prompts/2026-11-27-room-screen-share.md`

**Flow**
1. Host taps Share. App calls Edge Function `get-screenshare-token` with `{action:"start", room_id}`.
2. The function runs the SQL rule check `start_screen_share` as the caller (host only, room live, host present, not suspended, flag on, monthly cap not reached), creates a private Daily room `ss-<room_id>` and returns a sender token (screen only).
3. Room members see the row in `room_screen_shares` (Realtime). Watching is opt-in: tap Watch calls `{action:"watch", share_id}` and returns a viewer token that cannot send anything.
4. App sends `screen_share_heartbeat` every 30 s. Leaving calls `leave_screen_share`.
5. pg_cron job `sweep_screen_shares` (every minute) closes dead sessions: no heartbeat for 90 s, room ended, host left, 4 h limit, or monthly cap reached.

**Why our own minute counter**
Daily has no hard stop: after its 10,000 free minutes it bills. Every participant-minute (host and each viewer) is added to `rtc_provider_usage` under provider `daily` when a session closes. Shares stop at `app_flags.screen_share_daily_cap_minutes` (default 9000).

**Flags (`app_flags`)**
| key | default | use |
|---|---|---|
| `screen_share_enabled` | false | master switch |
| `screen_share_daily_cap_minutes` | 9000 | stop shares at this many Daily minutes per month |
| `screen_share_max_viewers` | 20 | max watchers per share |

**Tables:** `room_screen_shares` (readable by people in the room), `room_screen_viewers` (server only, no client access, the minute ledger).

**RPCs (signed-in users):** `start_screen_share`, `join_screen_share`, `screen_share_heartbeat`, `leave_screen_share`. `sweep_screen_shares` is cron only.

**Secrets needed:** `DAILY_API_KEY`, `DAILY_DOMAIN` (the Daily subdomain, same ones `get-room-token` already reads).

**Turn on / watch usage**
```sql
update public.app_flags set value = 'true'::jsonb where key = 'screen_share_enabled';
select * from public.get_rtc_provider_minutes_status();  -- look at the daily row
```

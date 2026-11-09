-- reporting_phase2_admin_workflow
-- Admin workflow: push alert on new report, "Take charge" (48 h claim), auto-release, account "under review",
-- reopen (revoke) with brief, challenge by the original admin (brief goes to the owner by email), owner decision.

-- 0) superadmin (owner) counts as admin everywhere private.is_admin() is used
create or replace function private.is_admin()
 returns boolean language sql stable security definer set search_path to 'pg_catalog', 'public' as $$
  select exists (select 1 from public.user_roles ur
                  where ur.user_id = auth.uid()::text and ur.role in ('admin', 'superadmin'));
$$;

create or replace function private.is_superadmin()
 returns boolean language sql stable security definer set search_path to 'pg_catalog', 'public' as $$
  select exists (select 1 from public.user_roles ur
                  where ur.user_id = auth.uid()::text and ur.role = 'superadmin');
$$;

-- 1) claim columns on room_reports (status values: pending = free, in_review = claimed, resolved)
alter table public.room_reports
  add column if not exists claimed_by uuid references auth.users(id) on delete set null,
  add column if not exists claimed_at timestamptz,
  add column if not exists claim_expires_at timestamptz;
create index if not exists room_reports_claim_idx on public.room_reports (claim_expires_at) where status = 'in_review';
create index if not exists room_reports_reported_review_idx on public.room_reports (reported_user_id) where status = 'in_review';

alter policy "Authenticated users can submit own reports" on public.room_reports
  with check (
    ((select auth.uid()) = reporter_id)
    and status = 'pending' and evidence_status = 'none'
    and evidence is null and message_snapshot is null and speaker_log is null and deletion_log is null
    and outcome is null and resolved_at is null and resolved_by is null and target_score_at_report is null
    and claimed_by is null and claimed_at is null and claim_expires_at is null
  );

-- 2) audit trail + reopen/dispute records (staff read only, written by RPCs)
create table if not exists public.report_events (
  id bigint generated always as identity primary key,
  report_id uuid not null references public.room_reports(id) on delete cascade,
  actor_id uuid references auth.users(id) on delete set null,
  event text not null,
  detail jsonb,
  created_at timestamptz not null default now()
);
create index if not exists report_events_report_idx on public.report_events (report_id, created_at);

create table if not exists public.report_reopens (
  id uuid primary key default gen_random_uuid(),
  report_id uuid not null references public.room_reports(id) on delete cascade,
  reopened_by uuid references auth.users(id) on delete set null,
  reopen_brief text not null check (length(btrim(reopen_brief)) between 10 and 2000),
  original_admin uuid references auth.users(id) on delete set null,
  original_outcome text,
  original_duration_hours integer,
  original_reason text,
  target_user_id uuid references auth.users(id) on delete cascade,
  status text not null default 'open' check (status in ('open', 'challenged', 'decided')),
  challenge_brief text check (challenge_brief is null or length(btrim(challenge_brief)) between 10 and 2000),
  challenged_at timestamptz,
  owner_emailed_at timestamptz,
  decision text check (decision is null or decision in ('reopen_upheld', 'original_upheld')),
  decision_note text,
  decided_by uuid references auth.users(id) on delete set null,
  decided_at timestamptz,
  created_at timestamptz not null default now()
);
create index if not exists report_reopens_report_idx on public.report_reopens (report_id, created_at);

alter table public.report_events enable row level security;
alter table public.report_reopens enable row level security;
revoke all on public.report_events, public.report_reopens from anon, authenticated;
grant select on public.report_events, public.report_reopens to authenticated;
drop policy if exists "Admins can view report events" on public.report_events;
create policy "Admins can view report events" on public.report_events for select to authenticated using ((select private.is_admin()));
drop policy if exists "Admins can view report reopens" on public.report_reopens;
create policy "Admins can view report reopens" on public.report_reopens for select to authenticated using ((select private.is_admin()));

-- 3) helpers
create or replace function private.admin_ids()
 returns uuid[] language sql stable security definer set search_path to 'pg_catalog', 'public' as $$
  select coalesce(array_agg(u.id), '{}'::uuid[])
    from public.user_roles ur join auth.users u on u.id::text = ur.user_id
   where ur.role in ('admin', 'superadmin');
$$;

create or replace function private.log_report_event(p_report uuid, p_actor uuid, p_event text, p_detail jsonb default null)
 returns void language sql security definer set search_path to 'pg_catalog', 'public' as $$
  insert into public.report_events (report_id, actor_id, event, detail) values (p_report, p_actor, p_event, p_detail);
$$;

-- call an Edge Function with the Vault push secret (same pattern as notify_message_push); never raises
create or replace function private.call_edge(p_fn text, p_body jsonb)
 returns void language plpgsql security definer set search_path to 'pg_catalog', 'public' as $$
declare v_secret text;
begin
  select decrypted_secret into v_secret from vault.decrypted_secrets where name = 'push_webhook_secret' limit 1;
  if v_secret is null then return; end if;
  perform net.http_post(
    url := 'https://dulpqochkxtamqztauea.supabase.co/functions/v1/' || p_fn,
    headers := jsonb_build_object('Content-Type', 'application/json', 'x-push-secret', v_secret),
    body := p_body, timeout_milliseconds := 15000);
exception when others then
  null;
end;
$$;

-- inbox notification + push for a list of staff users
create or replace function private.notify_staff(p_event text, p_title text, p_message text, p_report uuid, p_recipients uuid[])
 returns void language plpgsql security definer set search_path to 'pg_catalog', 'public' as $$
begin
  if p_recipients is null or cardinality(p_recipients) = 0 then return; end if;
  insert into public.notifications (user_id, type, title, message, report_id, action_type)
  select distinct u, 'staff_report'::text, p_title::text, p_message::text, p_report, p_event::text from unnest(p_recipients) u;
  perform private.call_edge('send-staff-push', jsonb_build_object(
    'event', p_event, 'recipient_ids', to_jsonb(p_recipients), 'title', p_title, 'body', left(p_message, 140), 'report_id', p_report));
end;
$$;

-- 4) new report -> alert every admin (not the reporter/reported; max 3 alerting reports per reporter per 10 min)
create or replace function private.trg_report_created()
 returns trigger language plpgsql security definer set search_path to 'pg_catalog', 'public' as $$
begin
  if (select count(*) from public.room_reports x
       where x.reporter_id = NEW.reporter_id and x.created_at > now() - interval '10 minutes') > 3 then
    return NEW;
  end if;
  perform private.notify_staff('new_report', 'New report',
    'A new ' || coalesce(NEW.context_type, '') || ' report needs review'
      || case when NEW.category is not null then ' (' || left(NEW.category, 40) || ')' else '' end || '.',
    NEW.id,
    array(select a from unnest(private.admin_ids()) a
           where a is distinct from NEW.reporter_id and a is distinct from NEW.reported_user_id));
  return NEW;
exception when others then
  return NEW;
end;
$$;
drop trigger if exists trg_report_created on public.room_reports;
create trigger trg_report_created after insert on public.room_reports
  for each row execute function private.trg_report_created();

-- the caller must hold a live (not expired) claim on the report
create or replace function private.require_claim(p_report uuid, p_admin uuid)
 returns public.room_reports language plpgsql security definer set search_path to 'pg_catalog', 'public' as $$
declare v public.room_reports;
begin
  select * into v from public.room_reports where id = p_report for update;
  if not found then raise exception 'report_not_found' using errcode = '22023'; end if;
  if v.status <> 'in_review' or v.claimed_by is distinct from p_admin
     or v.claim_expires_at is null or v.claim_expires_at <= now() then
    raise exception 'take_charge_first' using errcode = '42501';
  end if;
  return v;
end;
$$;

-- 5) take charge / release
create or replace function public.admin_take_charge(p_report_id uuid)
 returns jsonb language plpgsql security definer set search_path to 'pg_catalog', 'public' as $$
declare v_admin uuid := auth.uid(); v public.room_reports; v_exp timestamptz;
begin
  if v_admin is null then raise exception 'not_authenticated' using errcode = '28000'; end if;
  if not private.is_admin() then raise exception 'admin_required' using errcode = '42501'; end if;
  select * into v from public.room_reports where id = p_report_id for update;
  if not found then raise exception 'report_not_found' using errcode = '22023'; end if;
  if v.reported_user_id = v_admin or v.reporter_id = v_admin then
    raise exception 'conflict_of_interest' using errcode = '42501';
  end if;
  if v.status not in ('pending', 'in_review') then raise exception 'report_closed' using errcode = '22023'; end if;
  if v.status = 'in_review' and coalesce(v.claim_expires_at, '-infinity'::timestamptz) > now() then
    if v.claimed_by = v_admin then
      return jsonb_build_object('ok', true, 'report_id', p_report_id, 'claim_expires_at', v.claim_expires_at, 'already_mine', true);
    end if;
    raise exception 'already_claimed' using errcode = 'P0001';
  end if;
  v_exp := now() + interval '48 hours';
  update public.room_reports set status = 'in_review', claimed_by = v_admin, claimed_at = now(), claim_expires_at = v_exp
   where id = p_report_id;
  perform private.log_report_event(p_report_id, v_admin, 'take_charge', jsonb_build_object('expires_at', v_exp));
  if v.reported_user_id is not null then
    insert into public.notifications (user_id, type, title, message, action_type)
    values (v.reported_user_id, 'account_review', 'Account under review',
            'Your account is under review. We will notify you when the review is complete.', 'under_review');
  end if;
  return jsonb_build_object('ok', true, 'report_id', p_report_id, 'claim_expires_at', v_exp);
end;
$$;

create or replace function public.admin_release_report(p_report_id uuid)
 returns jsonb language plpgsql security definer set search_path to 'pg_catalog', 'public' as $$
declare v_admin uuid := auth.uid(); v public.room_reports;
begin
  if v_admin is null then raise exception 'not_authenticated' using errcode = '28000'; end if;
  if not private.is_admin() then raise exception 'admin_required' using errcode = '42501'; end if;
  v := private.require_claim(p_report_id, v_admin);
  update public.room_reports set status = 'pending', claimed_by = null, claimed_at = null, claim_expires_at = null where id = p_report_id;
  perform private.log_report_event(p_report_id, v_admin, 'release', null);
  return jsonb_build_object('ok', true, 'report_id', p_report_id);
end;
$$;

-- 6) action / dismiss now require the caller's live claim
create or replace function public.admin_apply_moderation_action(
  p_report_id uuid, p_target_user_id uuid, p_action_type text, p_duration_hours integer default null, p_reason text default null)
 returns jsonb language plpgsql security definer set search_path to 'pg_catalog', 'public' as $$
declare
  v_admin uuid := auth.uid();
  v_expires_at timestamptz := null;
  v_rr public.room_reports;
  v_room_id uuid;
  v_reporter_id uuid;
  v_notif_message text;
  v_conversation_id uuid;
begin
  if v_admin is null then raise exception 'not_authenticated' using errcode = '28000'; end if;
  if not private.is_admin() then raise exception 'admin_required' using errcode = '42501'; end if;
  if p_action_type not in ('suspend', 'warning', 'mute') then raise exception 'invalid_action_type' using errcode = '22023'; end if;
  if not exists (select 1 from auth.users u where u.id = p_target_user_id) then
    raise exception 'target_user_not_found' using errcode = '22023';
  end if;

  if p_report_id is not null then
    v_rr := private.require_claim(p_report_id, v_admin);
    if v_rr.reported_user_id is not null and v_rr.reported_user_id <> p_target_user_id then
      raise exception 'target_mismatch' using errcode = '22023';
    end if;
  end if;

  if p_action_type = 'suspend' then
    if p_duration_hours is null or p_duration_hours < 1 or p_duration_hours > 90 then
      raise exception 'suspend_duration_must_be_1_to_90_hours' using errcode = '22023';
    end if;
    v_expires_at := now() + make_interval(hours => p_duration_hours);
  elsif p_action_type = 'warning' then
    if p_duration_hours is not null and p_duration_hours <> 1 then
      raise exception 'warning_duration_must_be_1_hour' using errcode = '22023';
    end if;
    v_expires_at := now() + interval '1 hour';
    p_duration_hours := 1;
  elsif p_action_type = 'mute' then
    p_duration_hours := null;
    v_expires_at := null;
  end if;

  insert into public.user_moderation_status (user_id, action_type, started_at, expires_at, reason, report_id, issued_by, updated_at)
  values (p_target_user_id, p_action_type, now(), v_expires_at, nullif(trim(p_reason), ''), p_report_id, v_admin, now())
  on conflict (user_id) do update set
    action_type = excluded.action_type, started_at = excluded.started_at, expires_at = excluded.expires_at,
    reason = excluded.reason, report_id = excluded.report_id, issued_by = excluded.issued_by, updated_at = now();

  select rr.room_id, rr.reporter_id into v_room_id, v_reporter_id from public.room_reports rr where rr.id = p_report_id;

  insert into public.moderation_logs (caller_id, target_id, action_type, reason, room_id, created_at, report_id, duration_hours)
  values (v_admin::text, p_target_user_id::text, p_action_type, coalesce(nullif(trim(p_reason), ''), ''), v_room_id, now(), p_report_id, p_duration_hours);

  update public.room_reports
     set status = 'resolved', resolved_at = now(), resolved_by = v_admin, outcome = p_action_type,
         claimed_by = null, claimed_at = null, claim_expires_at = null
   where id = p_report_id;
  if p_report_id is not null then
    perform private.log_report_event(p_report_id, v_admin, 'action',
      jsonb_build_object('action_type', p_action_type, 'duration_hours', p_duration_hours));
  end if;

  perform private.apply_safety_penalty(p_target_user_id, p_action_type, p_report_id);

  if v_reporter_id is not null then
    v_notif_message := case p_action_type
      when 'suspend' then 'We reviewed your report and suspended the reported user for ' || p_duration_hours || ' hour(s).'
      when 'warning' then 'We reviewed your report and issued a warning to the reported user.'
      when 'mute' then 'We reviewed your report and muted the reported user.'
      else 'We reviewed your report and took action.' end;
    insert into public.notifications (user_id, type, title, message, report_id, action_type)
    values (v_reporter_id, 'report_update', 'Action taken on your report', v_notif_message, p_report_id, p_action_type);
    insert into public.support_conversations (user_id) values (v_reporter_id)
      on conflict (user_id) do update set last_message_at = now(), status = 'open'
      returning id into v_conversation_id;
    insert into public.support_messages (conversation_id, sender_type, sender_id, body, report_id, is_read_by_user, is_read_by_admin)
    values (v_conversation_id, 'admin', v_admin, v_notif_message, p_report_id, false, true);
  end if;

  return jsonb_build_object('ok', true, 'report_id', p_report_id, 'target_user_id', p_target_user_id,
    'action_type', p_action_type, 'duration_hours', p_duration_hours, 'expires_at', v_expires_at);
end;
$$;

create or replace function public.admin_dismiss_report(p_report_id uuid, p_reason text default null)
 returns jsonb language plpgsql security definer set search_path to 'pg_catalog', 'public' as $$
declare
  v_admin uuid := auth.uid(); v_rr public.room_reports; v_notif_message text; v_conversation_id uuid;
begin
  if v_admin is null then raise exception 'not_authenticated' using errcode = '28000'; end if;
  if not private.is_admin() then raise exception 'admin_required' using errcode = '42501'; end if;
  v_rr := private.require_claim(p_report_id, v_admin);
  if v_rr.reporter_id is null then raise exception 'report_not_found' using errcode = '22023'; end if;

  update public.room_reports
     set status = 'resolved', resolved_at = now(), resolved_by = v_admin, outcome = 'dismissed',
         claimed_by = null, claimed_at = null, claim_expires_at = null
   where id = p_report_id;
  perform private.log_report_event(p_report_id, v_admin, 'dismiss', null);

  v_notif_message := 'We reviewed your report and no action was needed.';
  if nullif(trim(p_reason), '') is not null then v_notif_message := v_notif_message || ' ' || trim(p_reason); end if;
  insert into public.notifications (user_id, type, title, message, report_id, action_type)
  values (v_rr.reporter_id, 'report_update', 'Update on your report', v_notif_message, p_report_id, 'no_action');
  insert into public.support_conversations (user_id) values (v_rr.reporter_id)
    on conflict (user_id) do update set last_message_at = now(), status = 'open'
    returning id into v_conversation_id;
  insert into public.support_messages (conversation_id, sender_type, sender_id, body, report_id, is_read_by_user, is_read_by_admin)
  values (v_conversation_id, 'admin', v_admin, v_notif_message, p_report_id, false, true);
  return jsonb_build_object('ok', true, 'report_id', p_report_id, 'action_type', 'no_action');
end;
$$;

-- 7) reopen (revoke) with a brief
create or replace function public.admin_reopen_report(p_report_id uuid, p_brief text)
 returns jsonb language plpgsql security definer set search_path to 'pg_catalog', 'public' as $$
declare
  v_admin uuid := auth.uid(); v public.room_reports; v_brief text := btrim(coalesce(p_brief, ''));
  v_ms public.user_moderation_status; v_hours integer; v_pen integer; v_new integer; v_id uuid;
  v_orig uuid; v_reason text;
begin
  if v_admin is null then raise exception 'not_authenticated' using errcode = '28000'; end if;
  if not private.is_admin() then raise exception 'admin_required' using errcode = '42501'; end if;
  if length(v_brief) < 10 then raise exception 'brief_too_short' using errcode = '22023'; end if;
  if length(v_brief) > 2000 then raise exception 'brief_too_long' using errcode = '22023'; end if;
  select * into v from public.room_reports where id = p_report_id for update;
  if not found then raise exception 'report_not_found' using errcode = '22023'; end if;
  if v.status <> 'resolved' or v.outcome is null then raise exception 'report_not_resolved' using errcode = '22023'; end if;
  if coalesce(v.resolved_at, v.created_at) < now() - interval '30 days' then
    raise exception 'reopen_window_closed' using errcode = '22023';
  end if;
  if exists (select 1 from public.report_reopens ro where ro.report_id = p_report_id and ro.status in ('open', 'challenged')) then
    raise exception 'already_reopened' using errcode = '22023';
  end if;
  if v.reported_user_id = v_admin or v.reporter_id = v_admin then raise exception 'conflict_of_interest' using errcode = '42501'; end if;

  v_orig := v.resolved_by;
  select ml.reason into v_reason from public.moderation_logs ml
   where ml.report_id = p_report_id and ml.action_type = v.outcome order by ml.created_at desc limit 1;

  -- lift the active action that came from this report
  select * into v_ms from public.user_moderation_status where user_id = v.reported_user_id and report_id = p_report_id;
  if found then
    v_hours := case
      when v_ms.action_type = 'suspend' and v_ms.expires_at is not null
        then greatest(1, round(extract(epoch from (v_ms.expires_at - v_ms.started_at)) / 3600)::integer)
      when v_ms.action_type = 'warning' then 1 else null end;
    delete from public.user_moderation_status where user_id = v.reported_user_id;
  else
    select ml.duration_hours into v_hours from public.moderation_logs ml
     where ml.report_id = p_report_id and ml.action_type = v.outcome order by ml.created_at desc limit 1;
  end if;

  -- give the safety-score penalty back
  if v.outcome in ('suspend', 'warning', 'mute') and v.reported_user_id is not null then
    select w.penalty into v_pen from public.safety_penalty_weights w where w.action_type = v.outcome;
    if v_pen is not null then
      update public.user_safety_scores
         set score = least(100, score + v_pen), upheld_count = greatest(0, upheld_count - 1), updated_at = now()
       where user_id = v.reported_user_id returning score into v_new;
      if found then
        insert into public.safety_score_events (user_id, report_id, delta, score_after, reason)
        values (v.reported_user_id, p_report_id, v_pen, v_new, 'reopen:' || v.outcome);
      end if;
    end if;
  end if;

  insert into public.report_reopens (report_id, reopened_by, reopen_brief, original_admin, original_outcome,
                                     original_duration_hours, original_reason, target_user_id)
  values (p_report_id, v_admin, v_brief, v_orig, v.outcome, v_hours, v_reason, v.reported_user_id)
  returning id into v_id;

  update public.room_reports
     set status = 'pending', outcome = null, resolved_at = null, resolved_by = null,
         claimed_by = null, claimed_at = null, claim_expires_at = null
   where id = p_report_id;
  insert into public.moderation_logs (caller_id, target_id, action_type, reason, report_id)
  values (v_admin::text, coalesce(v.reported_user_id::text, ''), 'reopen', v_brief, p_report_id);
  perform private.log_report_event(p_report_id, v_admin, 'reopen', jsonb_build_object('reopen_id', v_id, 'original_outcome', v.outcome));

  if v_orig is not null and v_orig <> v_admin then
    perform private.notify_staff('report_reopened', 'Your report decision was reopened',
      'Another admin reopened a report you acted on. Brief: ' || left(v_brief, 300), p_report_id, array[v_orig]);
  end if;
  perform private.notify_staff('report_open_again', 'Report reopened', 'A report was reopened and is free to take charge.', p_report_id,
    array(select a from unnest(private.admin_ids()) a where a <> v_admin and a is distinct from v_orig));
  return jsonb_build_object('ok', true, 'reopen_id', v_id, 'report_id', p_report_id);
end;
$$;

-- 8) the original admin challenges the reopen; both briefs are emailed to the owner by an Edge Function
create or replace function public.admin_challenge_reopen(p_reopen_id uuid, p_brief text)
 returns jsonb language plpgsql security definer set search_path to 'pg_catalog', 'public' as $$
declare v_admin uuid := auth.uid(); v public.report_reopens; v_brief text := btrim(coalesce(p_brief, ''));
begin
  if v_admin is null then raise exception 'not_authenticated' using errcode = '28000'; end if;
  if not private.is_admin() then raise exception 'admin_required' using errcode = '42501'; end if;
  if length(v_brief) < 10 then raise exception 'brief_too_short' using errcode = '22023'; end if;
  if length(v_brief) > 2000 then raise exception 'brief_too_long' using errcode = '22023'; end if;
  select * into v from public.report_reopens where id = p_reopen_id for update;
  if not found then raise exception 'reopen_not_found' using errcode = '22023'; end if;
  if v.original_admin is distinct from v_admin then raise exception 'not_original_admin' using errcode = '42501'; end if;
  if v.status <> 'open' then raise exception 'not_open' using errcode = '22023'; end if;
  update public.report_reopens set challenge_brief = v_brief, challenged_at = now(), status = 'challenged' where id = p_reopen_id;
  perform private.log_report_event(v.report_id, v_admin, 'challenge', jsonb_build_object('reopen_id', p_reopen_id));
  if v.reopened_by is not null then
    perform private.notify_staff('reopen_challenged', 'Your reopen was challenged',
      'The admin who handled the report challenged your reopen. The owner will decide.', v.report_id, array[v.reopened_by]);
  end if;
  perform private.call_edge('notify-owner-dispute', jsonb_build_object('reopen_id', p_reopen_id));
  return jsonb_build_object('ok', true, 'reopen_id', p_reopen_id);
end;
$$;

-- 9) owner (superadmin) decides who is right
create or replace function public.owner_decide_dispute(p_reopen_id uuid, p_decision text, p_note text default null)
 returns jsonb language plpgsql security definer set search_path to 'pg_catalog', 'public' as $$
declare v_admin uuid := auth.uid(); v public.report_reopens; v_exp timestamptz; v_msg text;
begin
  if v_admin is null then raise exception 'not_authenticated' using errcode = '28000'; end if;
  if not private.is_superadmin() then raise exception 'owner_required' using errcode = '42501'; end if;
  if p_decision not in ('reopen_upheld', 'original_upheld') then raise exception 'invalid_decision' using errcode = '22023'; end if;
  select * into v from public.report_reopens where id = p_reopen_id for update;
  if not found then raise exception 'reopen_not_found' using errcode = '22023'; end if;
  if v.status = 'decided' then raise exception 'already_decided' using errcode = '22023'; end if;

  if p_decision = 'original_upheld' then
    if v.original_outcome in ('suspend', 'warning', 'mute') and v.target_user_id is not null then
      v_exp := case v.original_outcome
                 when 'suspend' then now() + make_interval(hours => coalesce(v.original_duration_hours, 1))
                 when 'warning' then now() + interval '1 hour' else null end;
      insert into public.user_moderation_status (user_id, action_type, started_at, expires_at, reason, report_id, issued_by, updated_at)
      values (v.target_user_id, v.original_outcome, now(), v_exp, v.original_reason, v.report_id, v.original_admin, now())
      on conflict (user_id) do update set
        action_type = excluded.action_type, started_at = excluded.started_at, expires_at = excluded.expires_at,
        reason = excluded.reason, report_id = excluded.report_id, issued_by = excluded.issued_by, updated_at = now();
      perform private.apply_safety_penalty(v.target_user_id, v.original_outcome, v.report_id);
    end if;
    update public.room_reports
       set status = 'resolved', outcome = v.original_outcome, resolved_at = now(), resolved_by = v.original_admin,
           claimed_by = null, claimed_at = null, claim_expires_at = null
     where id = v.report_id;
  end if;

  update public.report_reopens
     set status = 'decided', decision = p_decision, decision_note = nullif(btrim(p_note), ''), decided_by = v_admin, decided_at = now()
   where id = p_reopen_id;
  perform private.log_report_event(v.report_id, v_admin, 'owner_decision', jsonb_build_object('decision', p_decision));
  v_msg := 'Owner decision: ' || case p_decision when 'original_upheld' then 'the original action stands.' else 'the reopen stands.' end
           || coalesce(' Note: ' || left(nullif(btrim(p_note), ''), 300), '');
  perform private.notify_staff('owner_decision', 'Owner decision on a reopened report', v_msg, v.report_id,
    array(select a from unnest(array[v.original_admin, v.reopened_by]) a where a is not null));
  return jsonb_build_object('ok', true, 'reopen_id', p_reopen_id, 'decision', p_decision);
end;
$$;

-- 10) the app asks: is my account under review?
create or replace function public.get_my_account_review()
 returns jsonb language plpgsql stable security definer set search_path to 'pg_catalog', 'public' as $$
declare v_uid uuid := auth.uid(); v_since timestamptz;
begin
  if v_uid is null then return jsonb_build_object('under_review', false, 'since', null); end if;
  select min(rr.claimed_at) into v_since from public.room_reports rr
   where rr.reported_user_id = v_uid and rr.status = 'in_review' and rr.claim_expires_at > now();
  return jsonb_build_object('under_review', v_since is not null, 'since', v_since);
end;
$$;

-- 11) a claim with no action for 48 h is freed
create or replace function private.release_expired_report_claims()
 returns integer language plpgsql security definer set search_path to 'pg_catalog', 'public' as $$
declare r record; n integer := 0;
begin
  for r in select id, claimed_by from public.room_reports where status = 'in_review' and claim_expires_at <= now() for update loop
    update public.room_reports set status = 'pending', claimed_by = null, claimed_at = null, claim_expires_at = null where id = r.id;
    perform private.log_report_event(r.id, r.claimed_by, 'auto_release', null);
    perform private.notify_staff('report_freed', 'Report is free again',
      'A report was not handled within 48 hours and is free to take charge.', r.id, private.admin_ids());
    n := n + 1;
  end loop;
  return n;
end;
$$;
select cron.schedule('release-expired-report-claims', '*/10 * * * *', $$select private.release_expired_report_claims();$$);

-- 12) queue and detail show the claim state
drop function if exists public.admin_get_report_queue(text, integer, integer);
create function public.admin_get_report_queue(p_status text default 'pending', p_limit integer default 50, p_offset integer default 0)
 returns table(report_id uuid, created_at timestamptz, context_type text, category text, reason text, status text,
               evidence_status text, reporter_id uuid, reporter_name text, reported_user_id uuid, reported_name text,
               reported_score integer, reports_against_total bigint, reports_against_pending bigint,
               claimed_by uuid, claimed_by_name text, claim_expires_at timestamptz, is_mine boolean, reopen_status text)
 language plpgsql stable security definer set search_path to 'pg_catalog', 'public' as $$
begin
  if not private.is_admin() then raise exception 'admin_required' using errcode = '42501'; end if;
  return query
  select rr.id, rr.created_at, rr.context_type, rr.category, rr.reason,
         case when rr.status = 'in_review' and rr.claim_expires_at <= now() then 'pending' else rr.status end,
         rr.evidence_status, rr.reporter_id, private.user_display_name(rr.reporter_id),
         rr.reported_user_id, private.user_display_name(rr.reported_user_id), coalesce(ss.score, 100),
         (select count(*) from public.room_reports x where x.reported_user_id = rr.reported_user_id),
         (select count(*) from public.room_reports x where x.reported_user_id = rr.reported_user_id and x.status in ('pending', 'in_review')),
         case when rr.status = 'in_review' and rr.claim_expires_at > now() then rr.claimed_by end,
         case when rr.status = 'in_review' and rr.claim_expires_at > now() then private.user_display_name(rr.claimed_by) end,
         case when rr.status = 'in_review' and rr.claim_expires_at > now() then rr.claim_expires_at end,
         (rr.status = 'in_review' and rr.claim_expires_at > now() and rr.claimed_by = auth.uid()),
         (select ro.status from public.report_reopens ro where ro.report_id = rr.id order by ro.created_at desc limit 1)
    from public.room_reports rr
    left join public.user_safety_scores ss on ss.user_id = rr.reported_user_id
   where case
           when p_status is null then true
           when p_status = 'pending' then (rr.status = 'pending' or (rr.status = 'in_review' and rr.claim_expires_at <= now()))
           when p_status = 'in_review' then (rr.status = 'in_review' and rr.claim_expires_at > now())
           when p_status = 'mine' then (rr.status = 'in_review' and rr.claim_expires_at > now() and rr.claimed_by = auth.uid())
           when p_status = 'open' then rr.status in ('pending', 'in_review')
           else rr.status = p_status end
   order by (rr.status = 'in_review' and rr.claim_expires_at > now()) asc,
            coalesce(ss.score, 100) asc,
            (select count(*) from public.room_reports x where x.reported_user_id = rr.reported_user_id and x.status in ('pending', 'in_review')) desc,
            rr.created_at asc
   limit least(greatest(coalesce(p_limit, 50), 1), 200) offset greatest(coalesce(p_offset, 0), 0);
end;
$$;

create or replace function public.admin_get_report_detail(p_report_id uuid)
 returns jsonb language plpgsql stable security definer set search_path to 'pg_catalog', 'public' as $$
declare v_rr public.room_reports%rowtype; v_active boolean;
begin
  if not private.is_admin() then raise exception 'admin_required' using errcode = '42501'; end if;
  select * into v_rr from public.room_reports where id = p_report_id;
  if not found then raise exception 'report_not_found' using errcode = '22023'; end if;
  v_active := v_rr.status = 'in_review' and v_rr.claim_expires_at > now();
  return jsonb_build_object(
    'report', to_jsonb(v_rr),
    'claim', jsonb_build_object('active', v_active,
              'claimed_by', case when v_active then v_rr.claimed_by end,
              'claimed_by_name', case when v_active then private.user_display_name(v_rr.claimed_by) end,
              'expires_at', case when v_active then v_rr.claim_expires_at end,
              'is_mine', v_active and v_rr.claimed_by = auth.uid()),
    'reporter', jsonb_build_object('user_id', v_rr.reporter_id, 'name', private.user_display_name(v_rr.reporter_id)),
    'reported', jsonb_build_object(
      'user_id', v_rr.reported_user_id,
      'name', private.user_display_name(v_rr.reported_user_id),
      'score', coalesce((select s.score from public.user_safety_scores s where s.user_id = v_rr.reported_user_id), 100),
      'upheld_count', coalesce((select s.upheld_count from public.user_safety_scores s where s.user_id = v_rr.reported_user_id), 0),
      'active_moderation', (select to_jsonb(ms) from public.user_moderation_status ms where ms.user_id = v_rr.reported_user_id),
      'recent_score_events', (select coalesce(jsonb_agg(to_jsonb(e) order by e.created_at desc), '[]'::jsonb)
                                from (select delta, score_after, reason, report_id, created_at from public.safety_score_events
                                       where user_id = v_rr.reported_user_id order by created_at desc limit 10) e),
      'other_reports', (select coalesce(jsonb_agg(to_jsonb(o) order by o.created_at desc), '[]'::jsonb)
                          from (select id, created_at, context_type, category, status, outcome from public.room_reports
                                 where reported_user_id = v_rr.reported_user_id and id <> v_rr.id
                                 order by created_at desc limit 10) o)),
    'reopens', (select coalesce(jsonb_agg(to_jsonb(ro) order by ro.created_at), '[]'::jsonb)
                  from public.report_reopens ro where ro.report_id = v_rr.id),
    'events', (select coalesce(jsonb_agg(to_jsonb(ev) order by ev.created_at), '[]'::jsonb)
                 from (select actor_id, event, detail, created_at from public.report_events
                        where report_id = v_rr.id order by created_at desc limit 50) ev));
end;
$$;

-- 13) grants: signed-in users only (each function checks the role itself)
do $$
declare f text;
begin
  for f in select unnest(array[
    'public.admin_take_charge(uuid)', 'public.admin_release_report(uuid)',
    'public.admin_apply_moderation_action(uuid,uuid,text,integer,text)', 'public.admin_dismiss_report(uuid,text)',
    'public.admin_reopen_report(uuid,text)', 'public.admin_challenge_reopen(uuid,text)',
    'public.owner_decide_dispute(uuid,text,text)', 'public.get_my_account_review()',
    'public.admin_get_report_queue(text,integer,integer)', 'public.admin_get_report_detail(uuid)'])
  loop
    execute format('revoke execute on function %s from public, anon', f);
    execute format('grant execute on function %s to authenticated', f);
  end loop;
end $$;

-- 14) keep the dispute record when the reported user's auth account is deleted (applied as reporting_phase2_reopens_fk_set_null)
alter table public.report_reopens drop constraint if exists report_reopens_target_user_id_fkey;
alter table public.report_reopens
  add constraint report_reopens_target_user_id_fkey foreign key (target_user_id) references auth.users(id) on delete set null;

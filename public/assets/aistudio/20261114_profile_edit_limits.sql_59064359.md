-- Profile edit limits (username, display name, age, gender, avatar, cover, bio, location).
-- Enforced by a trigger on public.profiles, so it applies to direct app updates AND to the RPCs below.
-- Limits live in a table (profile_field_limits): change a number there, no app update needed.

-- 1) limits + log + reserved names
create table if not exists public.profile_field_limits (
  field text primary key,
  max_changes integer not null check (max_changes >= 0),
  window_hours integer not null check (window_hours > 0),
  note text
);
insert into public.profile_field_limits (field, max_changes, window_hours, note) values
  ('username',     1, 720, '1 change per 30 days'),
  ('profile_name', 3, 168, '3 changes per 7 days'),
  ('age',          1, 720, '1 change per 30 days'),
  ('gender',       1, 720, '1 change per 30 days'),
  ('avatar_url',  10,  24, '10 per day'),
  ('cover_url',   10,  24, '10 per day'),
  ('bio',         20,  24, '20 per day'),
  ('country_code', 3,  24, '3 per day'),
  ('city',         5,  24, '5 per day')
on conflict (field) do nothing;
alter table public.profile_field_limits enable row level security;
drop policy if exists profile_field_limits_select on public.profile_field_limits;
create policy profile_field_limits_select on public.profile_field_limits for select to authenticated using (true);

create table if not exists public.profile_change_log (
  id bigint generated always as identity primary key,
  user_id uuid not null references public.profiles(auth_user_id) on delete cascade,
  field text not null,
  old_value text,
  new_value text,
  changed_at timestamptz not null default now()
);
create index if not exists idx_profile_change_log_user on public.profile_change_log (user_id, field, changed_at desc);
create index if not exists idx_profile_change_log_oldname on public.profile_change_log (old_value, changed_at) where field = 'username';
alter table public.profile_change_log enable row level security;
drop policy if exists profile_change_log_select on public.profile_change_log;
create policy profile_change_log_select on public.profile_change_log for select to authenticated using (user_id = (select auth.uid()));

create table if not exists public.reserved_usernames (pattern text primary key);
alter table public.reserved_usernames enable row level security;
insert into public.reserved_usernames (pattern) values
  ('admin%'), ('administrator%'), ('support%'), ('staff%'), ('moderator%'), ('mod'), ('official%'), ('system%'),
  ('help%'), ('team'), ('root'), ('null'), ('undefined'), ('anonymous'), ('deleted%'), ('%funkytalk%'), ('%funky_talk%')
on conflict (pattern) do nothing;

revoke insert, update, delete, truncate on public.profile_field_limits, public.profile_change_log, public.reserved_usernames from anon, authenticated;
revoke select on public.profile_field_limits, public.profile_change_log, public.reserved_usernames from anon;
revoke select on public.reserved_usernames from authenticated;

-- 2) protect columns that a client must never change directly (RPCs and Edge Functions are not affected)
create or replace function public.profiles_protect_columns() returns trigger
language plpgsql set search_path to 'pg_catalog','public' as $$
begin
  if current_user in ('authenticated', 'anon') then
    NEW.id := OLD.id;
    NEW.auth_user_id := OLD.auth_user_id;
    NEW.created_at := OLD.created_at;
    NEW.deletion_scheduled_at := OLD.deletion_scheduled_at;
    if OLD.onboarding_complete then NEW.onboarding_complete := true; end if;   -- cannot go back to "free setup"
  end if;
  return NEW;
end $$;
drop trigger if exists trg_profiles_a_protect on public.profiles;
create trigger trg_profiles_a_protect
  before update of id, auth_user_id, created_at, deletion_scheduled_at, onboarding_complete on public.profiles
  for each row execute function public.profiles_protect_columns();

-- 3) limits + username rules (runs for direct updates and RPCs; skipped for service role and for staff acting on others)
create or replace function private.guard_profile_limits() returns trigger
language plpgsql security definer set search_path to 'pg_catalog','public' as $$
declare
  v_uid uuid := auth.uid(); v_f text; v_old text; v_new text; v_lim public.profile_field_limits;
  v_used int; v_first timestamptz; v_oj jsonb; v_nj jsonb;
begin
  if v_uid is null or v_uid <> OLD.auth_user_id then return NEW; end if;

  if NEW.username is distinct from OLD.username then
    if NEW.username is null or NEW.username !~ '^[a-z0-9._]{3,20}$' or NEW.username ~ '^\.|\.$|\.\.' then
      raise exception 'invalid_username' using errcode = '22023';
    end if;
    if not private.is_staff() and exists (select 1 from public.reserved_usernames r where NEW.username like r.pattern) then
      raise exception 'username_reserved' using errcode = '22023';
    end if;
    if exists (select 1 from public.profile_change_log l
                where l.field = 'username' and l.old_value = NEW.username and l.user_id <> v_uid
                  and l.changed_at > now() - interval '30 days') then
      raise exception 'username_recently_used' using errcode = '22023';
    end if;
  end if;

  if not OLD.onboarding_complete then return NEW; end if;   -- first-time setup is free

  v_oj := to_jsonb(OLD); v_nj := to_jsonb(NEW);
  foreach v_f in array array['username','profile_name','age','gender','avatar_url','cover_url','bio','country_code','city'] loop
    v_old := v_oj ->> v_f; v_new := v_nj ->> v_f;
    if v_old is distinct from v_new then
      select * into v_lim from public.profile_field_limits where field = v_f;
      if found then
        select count(*), min(changed_at) into v_used, v_first from public.profile_change_log
         where user_id = v_uid and field = v_f and changed_at > now() - make_interval(hours => v_lim.window_hours);
        if v_used >= v_lim.max_changes then
          raise exception 'profile_limit_%', v_f using errcode = '22023',
            hint = (v_first + make_interval(hours => v_lim.window_hours))::text;
        end if;
      end if;
      insert into public.profile_change_log (user_id, field, old_value, new_value)
      values (v_uid, v_f, left(v_old, 200), left(v_new, 200));
    end if;
  end loop;
  return NEW;
end $$;
drop trigger if exists trg_profiles_b_limits on public.profiles;
create trigger trg_profiles_b_limits
  before update of username, profile_name, age, gender, avatar_url, cover_url, bio, country_code, city on public.profiles
  for each row execute function private.guard_profile_limits();

-- 4) RPCs for the UI
create or replace function public.check_username_available(p_username text default null) returns jsonb
language plpgsql stable security definer set search_path to 'pg_catalog','public' as $$
declare v_uid uuid := auth.uid(); v text := lower(btrim(coalesce(p_username, '')));
begin
  if v_uid is null then raise exception 'not_authenticated' using errcode = '28000'; end if;
  if exists (select 1 from public.profiles where auth_user_id = v_uid and username = v) then
    return jsonb_build_object('available', false, 'reason', 'same');
  end if;
  if v !~ '^[a-z0-9._]{3,20}$' or v ~ '^\.|\.$|\.\.' then return jsonb_build_object('available', false, 'reason', 'invalid_format'); end if;
  if not private.is_staff() and exists (select 1 from public.reserved_usernames r where v like r.pattern) then
    return jsonb_build_object('available', false, 'reason', 'reserved');
  end if;
  if exists (select 1 from public.profiles where username = v) then return jsonb_build_object('available', false, 'reason', 'taken'); end if;
  if exists (select 1 from public.profile_change_log l where l.field = 'username' and l.old_value = v and l.user_id <> v_uid
              and l.changed_at > now() - interval '30 days') then
    return jsonb_build_object('available', false, 'reason', 'recently_used');
  end if;
  return jsonb_build_object('available', true, 'reason', 'ok');
end $$;

create or replace function public.change_username(p_username text default null) returns jsonb
language plpgsql security definer set search_path to 'pg_catalog','public' as $$
declare v_uid uuid := auth.uid(); v text := lower(btrim(coalesce(p_username, ''))); v_next timestamptz;
begin
  if v_uid is null then raise exception 'not_authenticated' using errcode = '28000'; end if;
  if v = '' then raise exception 'invalid_username' using errcode = '22023'; end if;
  begin
    update public.profiles set username = v where auth_user_id = v_uid;
  exception when unique_violation then
    raise exception 'username_taken' using errcode = '22023';
  end;
  select min(changed_at) + interval '30 days' into v_next from public.profile_change_log
   where user_id = v_uid and field = 'username' and changed_at > now() - interval '30 days';
  return jsonb_build_object('username', v, 'next_change_at', v_next);
end $$;

create or replace function public.get_profile_edit_limits() returns jsonb
language plpgsql stable security definer set search_path to 'pg_catalog','public' as $$
declare v_uid uuid := auth.uid(); v_setup boolean; v_res jsonb;
begin
  if v_uid is null then raise exception 'not_authenticated' using errcode = '28000'; end if;
  select not onboarding_complete into v_setup from public.profiles where auth_user_id = v_uid;
  select coalesce(jsonb_agg(jsonb_build_object(
      'field', f.field, 'max_changes', f.max_changes, 'window_hours', f.window_hours,
      'used', u.used, 'remaining', greatest(f.max_changes - u.used, 0),
      'next_allowed_at', case when u.used >= f.max_changes then u.first_at + make_interval(hours => f.window_hours) end)
      order by f.field), '[]'::jsonb) into v_res
    from public.profile_field_limits f
    cross join lateral (
      select case when coalesce(v_setup, false) then 0 else count(*)::int end as used, min(l.changed_at) as first_at
        from public.profile_change_log l
       where l.user_id = v_uid and l.field = f.field and l.changed_at > now() - make_interval(hours => f.window_hours)) u;
  return v_res;
end $$;

do $$ declare r record; begin
  for r in select p.oid::regprocedure as sig from pg_proc p join pg_namespace n on n.oid = p.pronamespace
            where n.nspname = 'public' and p.proname in ('check_username_available','change_username','get_profile_edit_limits')
  loop
    execute format('revoke all on function %s from public, anon', r.sig);
    execute format('grant execute on function %s to authenticated', r.sig);
  end loop;
end $$;

-- 5) account deletion must purge the change log too
do $do$ declare d text;
begin
  d := pg_get_functiondef('public.purge_user_data(uuid,text)'::regprocedure);
  if position('profile_change_log' in d) = 0 and position('(''feedback_votes'', ''user_id = $1''),' in d) > 0 then
    d := replace(d, '(''feedback_votes'', ''user_id = $1''),',
      '(''profile_change_log'', ''user_id = $1''), (''feedback_votes'', ''user_id = $1''),');
    execute d;
  end if;
end $do$;

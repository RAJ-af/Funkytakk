-- RTC provider control: which providers are enabled, and how new rooms are spread over them.
--   strategy 'priority'  : use the first enabled provider (by priority) until its monthly limit, then the next
--   strategy 'balanced'  : give each new room to the enabled provider with the lowest used/limit ratio
--                          (all three providers are exercised every day)
-- A room keeps the provider it was given for its whole life.

create table if not exists public.rtc_provider_settings (
  provider text primary key check (provider in ('agora', 'zego', 'daily')),
  enabled boolean not null default false,
  priority integer not null default 1,
  monthly_limit integer not null default 9800,
  updated_at timestamptz not null default now()
);
insert into public.rtc_provider_settings (provider, enabled, priority, monthly_limit) values
  ('agora', true, 1, 9800),
  ('zego', false, 2, 9800),
  ('daily', false, 3, 9800)
on conflict (provider) do nothing;

create table if not exists public.rtc_settings (
  id integer primary key check (id = 1),
  strategy text not null default 'priority' check (strategy in ('priority', 'balanced')),
  updated_at timestamptz not null default now()
);
insert into public.rtc_settings (id, strategy) values (1, 'priority') on conflict (id) do nothing;

alter table public.rtc_provider_settings enable row level security;   -- no policies: service role / SQL editor only
alter table public.rtc_settings enable row level security;

create or replace function public.assign_rtc_provider_for_room(p_room_id uuid)
returns text
language plpgsql
security definer
set search_path to 'pg_catalog', 'public'
as $$
declare
  v_provider text;
  v_month text := to_char(now(), 'YYYY-MM');
  v_strategy text;
begin
  select rtc_provider into v_provider
    from public.voice_rooms
   where id = p_room_id
   for update;

  if not found then
    raise exception 'room_not_found' using errcode = '22023';
  end if;

  if v_provider is not null then
    return v_provider;
  end if;

  select strategy into v_strategy from public.rtc_settings where id = 1;

  select s.provider into v_provider
    from public.rtc_provider_settings s
    left join public.rtc_provider_usage u on u.provider = s.provider and u.month = v_month
   where s.enabled
     and coalesce(u.minutes_used, 0) < s.monthly_limit
   order by case when coalesce(v_strategy, 'priority') = 'balanced'
                 then coalesce(u.minutes_used, 0)::numeric / greatest(s.monthly_limit, 1)
                 else s.priority end,
            s.priority
   limit 1;

  if v_provider is null then
    raise exception 'all_providers_at_limit' using errcode = 'P0001';
  end if;

  update public.voice_rooms set rtc_provider = v_provider where id = p_room_id;
  return v_provider;
end;
$$;

drop function if exists public.get_rtc_provider_minutes_status();
create or replace function public.get_rtc_provider_minutes_status()
returns table (
  provider text,
  enabled boolean,
  priority integer,
  minutes_used integer,
  minutes_remaining integer,
  limit_minutes integer,
  strategy text
)
language sql
stable
security definer
set search_path to 'pg_catalog', 'public'
as $$
  select s.provider,
         s.enabled,
         s.priority,
         coalesce(u.minutes_used, 0),
         s.monthly_limit - coalesce(u.minutes_used, 0),
         s.monthly_limit,
         (select strategy from public.rtc_settings where id = 1)
    from public.rtc_provider_settings s
    left join public.rtc_provider_usage u
           on u.provider = s.provider and u.month = to_char(now(), 'YYYY-MM')
   order by s.priority;
$$;
revoke all on function public.get_rtc_provider_minutes_status() from public, anon, authenticated;
grant execute on function public.get_rtc_provider_minutes_status() to service_role;

-- Staff switches (or just run the UPDATE statements in the Supabase SQL editor)
create or replace function public.admin_set_rtc_provider(p_provider text, p_enabled boolean)
returns jsonb
language plpgsql
security definer
set search_path to 'pg_catalog', 'public'
as $$
declare
  n integer;
begin
  if not exists (select 1 from public.user_roles
                  where user_id = auth.uid()::text and role in ('admin', 'superadmin')) then
    raise exception 'forbidden' using errcode = '42501';
  end if;
  update public.rtc_provider_settings set enabled = p_enabled, updated_at = now() where provider = p_provider;
  get diagnostics n = row_count;
  return jsonb_build_object('success', n > 0, 'provider', p_provider, 'enabled', p_enabled);
end;
$$;

create or replace function public.admin_set_rtc_strategy(p_strategy text)
returns jsonb
language plpgsql
security definer
set search_path to 'pg_catalog', 'public'
as $$
begin
  if not exists (select 1 from public.user_roles
                  where user_id = auth.uid()::text and role in ('admin', 'superadmin')) then
    raise exception 'forbidden' using errcode = '42501';
  end if;
  if p_strategy not in ('priority', 'balanced') then
    return jsonb_build_object('success', false, 'error', 'invalid_strategy');
  end if;
  update public.rtc_settings set strategy = p_strategy, updated_at = now() where id = 1;
  return jsonb_build_object('success', true, 'strategy', p_strategy);
end;
$$;

revoke all on function public.admin_set_rtc_provider(text, boolean) from public, anon;
revoke all on function public.admin_set_rtc_strategy(text) from public, anon;
grant execute on function public.admin_set_rtc_provider(text, boolean) to authenticated;
grant execute on function public.admin_set_rtc_strategy(text) to authenticated;

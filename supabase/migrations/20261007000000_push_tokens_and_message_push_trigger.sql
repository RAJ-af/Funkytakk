-- FunkyTalk push notifications: tokens, RPCs, trigger, grants.
-- Mirrors what is deployed on the live project (migrations push_tokens_and_message_push_trigger,
-- verify_push_secret_rpc, push_trigger_timeout, push_tokens_hardening).
--
-- Manual steps that are NOT in SQL (secrets):
--   1) Vault secret (any long random text; never shown to the app):
--        select vault.create_secret('<long random text>', 'push_webhook_secret');
--      Rotate with:
--        select vault.update_secret((select id from vault.secrets where name = 'push_webhook_secret'),
--                                   replace(gen_random_uuid()::text || gen_random_uuid()::text, '-', ''),
--                                   'push_webhook_secret');
--   2) Edge Function secret FCM_SERVICE_ACCOUNT_JSON = full Firebase service-account JSON
--      (same Firebase project as the app's google-services.json).
--   3) Deploy the Edge Function `send-push` (verify_jwt = false; it checks the Vault secret itself).

create extension if not exists pg_net with schema extensions;

-- ---------------------------------------------------------------- tokens
create table if not exists public.push_tokens (
  token text primary key,                                   -- one physical device token = one user at a time
  user_id uuid not null references auth.users(id) on delete cascade,
  platform text not null default 'android',
  updated_at timestamptz not null default now()
);
create index if not exists idx_push_tokens_user on public.push_tokens (user_id);

alter table public.push_tokens enable row level security;   -- no policies on purpose: RPCs only
revoke all on public.push_tokens from anon, authenticated;

create or replace function public.register_push_token(p_token text, p_platform text default 'android') returns void
language plpgsql security definer set search_path = pg_catalog, public as $$
begin
  if auth.uid() is null then raise exception 'not_authenticated' using errcode = '28000'; end if;
  if p_token is null or char_length(p_token) < 20 or char_length(p_token) > 4096 then
    raise exception 'invalid_token' using errcode = '22023';
  end if;
  insert into public.push_tokens (token, user_id, platform, updated_at)
  values (p_token, auth.uid(), coalesce(p_platform, 'android'), now())
  on conflict (token) do update set user_id = excluded.user_id, platform = excluded.platform, updated_at = now();
  delete from public.push_tokens
  where user_id = auth.uid()
    and token not in (select token from public.push_tokens where user_id = auth.uid() order by updated_at desc limit 5);
end;
$$;

create or replace function public.unregister_push_token(p_token text) returns void
language plpgsql security definer set search_path = pg_catalog, public as $$
begin
  if auth.uid() is null then raise exception 'not_authenticated' using errcode = '28000'; end if;
  delete from public.push_tokens where token = p_token and user_id = auth.uid();
end;
$$;

create or replace function public.purge_stale_push_tokens() returns void
language sql security definer set search_path = pg_catalog, public as $$
  delete from public.push_tokens where updated_at < now() - interval '60 days';
$$;

revoke all on function public.register_push_token(text, text), public.unregister_push_token(text) from public, anon;
grant execute on function public.register_push_token(text, text), public.unregister_push_token(text) to authenticated;
revoke all on function public.purge_stale_push_tokens() from public, anon, authenticated;

select cron.schedule('purge-stale-push-tokens', '50 3 * * *', 'select public.purge_stale_push_tokens()');

-- ---------------------------------------------------------------- secret check (used by the Edge Function)
create or replace function public.verify_push_secret(p_secret text) returns boolean
language sql stable security definer set search_path = pg_catalog, public as $$
  select exists (select 1 from vault.decrypted_secrets where name = 'push_webhook_secret' and decrypted_secret = p_secret);
$$;
revoke all on function public.verify_push_secret(text) from public, anon, authenticated;
grant execute on function public.verify_push_secret(text) to service_role;

-- ---------------------------------------------------------------- trigger: content-free push request
create or replace function public.notify_message_push() returns trigger
language plpgsql security definer set search_path = pg_catalog, public as $$
declare
  v_secret text;
  v_payload jsonb;
begin
  if TG_TABLE_NAME = 'dm_messages' then
    if NEW.message_type = 'reaction' or NEW.recipient_id = NEW.sender_id then return NEW; end if;
    v_payload := jsonb_build_object('kind', 'dm', 'recipient_id', NEW.recipient_id, 'sender_id', NEW.sender_id,
                                    'conversation_id', NEW.conversation_id, 'message_type', NEW.message_type);
  else
    if NEW.message_type = 'reaction' or NEW.recipient_id = NEW.sender_id then return NEW; end if;
    if exists (
      select 1 from public.group_members m
      where m.group_id = NEW.group_id and m.user_id = NEW.recipient_id
        and (m.notify_level <> 'all' or (m.muted_until is not null and m.muted_until > now()))
    ) then return NEW; end if;
    v_payload := jsonb_build_object('kind', 'group', 'recipient_id', NEW.recipient_id, 'sender_id', NEW.sender_id,
                                    'group_id', NEW.group_id, 'message_type', NEW.message_type);
  end if;

  select decrypted_secret into v_secret from vault.decrypted_secrets where name = 'push_webhook_secret' limit 1;
  if v_secret is null then return NEW; end if;   -- push not configured: never block message delivery

  perform net.http_post(
    url := 'https://dulpqochkxtamqztauea.supabase.co/functions/v1/send-push',
    headers := jsonb_build_object('Content-Type', 'application/json', 'x-push-secret', v_secret),
    body := v_payload,
    timeout_milliseconds := 15000
  );
  return NEW;
exception when others then
  return NEW;
end;
$$;
revoke all on function public.notify_message_push() from public, anon, authenticated;

drop trigger if exists trg_push_dm_message on public.dm_messages;
create trigger trg_push_dm_message after insert on public.dm_messages
  for each row execute function public.notify_message_push();

drop trigger if exists trg_push_group_message on public.group_message_recipients;
create trigger trg_push_group_message after insert on public.group_message_recipients
  for each row execute function public.notify_message_push();

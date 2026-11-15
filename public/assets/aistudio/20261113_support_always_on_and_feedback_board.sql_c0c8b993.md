-- 1) Support chat is open 24/7 for every user (the old "window" stays only as an optional field)
-- 2) Feedback board: bug reports (private) + feature requests (public, votable), status tracking for users

-- =====================================================================
-- 1) 24/7 support chat
-- =====================================================================
create or replace function public.send_support_message(p_message text, p_report_id uuid default null)
returns uuid language plpgsql security definer set search_path to 'pg_catalog','public' as $$
declare
  v_uid uuid := auth.uid(); v_conv uuid; v_msg uuid; v_ms_report uuid; v_text text := btrim(coalesce(p_message, ''));
begin
  if v_uid is null then raise exception 'not_authenticated' using errcode = '28000'; end if;
  if v_text = '' then raise exception 'empty_message' using errcode = '22023'; end if;
  if char_length(v_text) > 1000 then raise exception 'message_too_long' using errcode = '22023'; end if;
  select ms.report_id into v_ms_report from public.user_moderation_status ms
   where ms.user_id = v_uid and ms.action_type in ('suspend', 'mute') and (ms.expires_at is null or ms.expires_at > now())
   limit 1;
  if (select count(*) from public.support_messages sm join public.support_conversations sc on sc.id = sm.conversation_id
       where sc.user_id = v_uid and sm.sender_type = 'user' and sm.created_at > now() - interval '10 minutes') >= 20 then
    raise exception 'rate_limited' using errcode = '22023';
  end if;
  insert into public.support_conversations (user_id) values (v_uid)
  on conflict (user_id) do update set last_message_at = now() returning id into v_conv;
  update public.support_conversations set last_message_at = now(), status = 'open' where id = v_conv;
  insert into public.support_messages (conversation_id, sender_type, sender_id, body, report_id, is_read_by_user, is_read_by_admin)
  values (v_conv, 'user', v_uid, v_text, coalesce(p_report_id, v_ms_report), true, false)
  returning id into v_msg;
  return v_msg;
end $$;

create or replace function public.get_my_support_session() returns jsonb
language plpgsql stable security definer set search_path to 'pg_catalog','public' as $$
declare
  v_uid uuid := auth.uid(); v_id uuid; v_until timestamptz; v_unread integer := 0;
  v_appeal boolean; v_exp timestamptz; v_window boolean;
begin
  if v_uid is null then return jsonb_build_object('open', false); end if;
  select sc.id, sc.chat_open_until into v_id, v_until from public.support_conversations sc where sc.user_id = v_uid;
  select true, ms.expires_at into v_appeal, v_exp from public.user_moderation_status ms
   where ms.user_id = v_uid and ms.action_type in ('suspend', 'mute') and (ms.expires_at is null or ms.expires_at > now())
   limit 1;
  v_window := v_until is not null and v_until > now();
  if v_id is not null then
    select count(*) into v_unread from public.support_messages sm
     where sm.conversation_id = v_id and sm.sender_type = 'admin' and not sm.is_read_by_user;
  end if;
  return jsonb_build_object('open', true,
    'reason', case when coalesce(v_appeal, false) then 'appeal' when v_window then 'support_opened' else 'always' end,
    'until', case when coalesce(v_appeal, false) and not v_window then v_exp when v_window then v_until end,
    'conversation_id', v_id, 'unread', v_unread, 'can_appeal', coalesce(v_appeal, false));
end $$;

-- =====================================================================
-- 2) Feedback board
-- =====================================================================
create table if not exists public.feedback_posts (
  id bigint generated always as identity primary key,
  user_id uuid not null references public.profiles(auth_user_id) on delete cascade,
  kind text not null check (kind in ('bug','feature')),
  title text not null check (char_length(title) between 3 and 80),
  body text not null default '' check (char_length(body) <= 1500),
  status text not null default 'open' check (status in ('open','planned','in_progress','done','declined','duplicate')),
  app_version text check (app_version is null or char_length(app_version) <= 40),
  device_info text check (device_info is null or char_length(device_info) <= 200),
  votes_count integer not null default 0,
  replies_count integer not null default 0,
  author_seen_at timestamptz not null default now(),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
create index if not exists idx_feedback_posts_board on public.feedback_posts (kind, status, votes_count desc, created_at desc);
create index if not exists idx_feedback_posts_user on public.feedback_posts (user_id, created_at desc);

create table if not exists public.feedback_votes (
  post_id bigint not null references public.feedback_posts(id) on delete cascade,
  user_id uuid not null references public.profiles(auth_user_id) on delete cascade,
  created_at timestamptz not null default now(),
  primary key (post_id, user_id)
);
create index if not exists idx_feedback_votes_user on public.feedback_votes (user_id);

create table if not exists public.feedback_replies (
  id bigint generated always as identity primary key,
  post_id bigint not null references public.feedback_posts(id) on delete cascade,
  user_id uuid not null references public.profiles(auth_user_id) on delete cascade,
  is_staff boolean not null default false,
  body text not null check (char_length(btrim(body)) between 1 and 1000),
  created_at timestamptz not null default now()
);
create index if not exists idx_feedback_replies_post on public.feedback_replies (post_id, created_at);

alter table public.feedback_posts   enable row level security;
alter table public.feedback_votes   enable row level security;
alter table public.feedback_replies enable row level security;
drop policy if exists feedback_posts_select on public.feedback_posts;
create policy feedback_posts_select on public.feedback_posts for select to authenticated
  using (user_id = (select auth.uid()) or kind = 'feature' or (select private.is_staff()));
drop policy if exists feedback_votes_select on public.feedback_votes;
create policy feedback_votes_select on public.feedback_votes for select to authenticated using (user_id = (select auth.uid()));
drop policy if exists feedback_replies_select on public.feedback_replies;
create policy feedback_replies_select on public.feedback_replies for select to authenticated
  using (exists (select 1 from public.feedback_posts p where p.id = post_id));
revoke insert, update, delete, truncate on public.feedback_posts, public.feedback_votes, public.feedback_replies from anon, authenticated;
revoke select on public.feedback_posts, public.feedback_votes, public.feedback_replies from anon;

create or replace function public.sync_feedback_votes_count() returns trigger
language plpgsql security definer set search_path to 'pg_catalog','public' as $$
begin
  if TG_OP = 'INSERT' then update public.feedback_posts set votes_count = votes_count + 1 where id = NEW.post_id; return NEW;
  elsif TG_OP = 'DELETE' then update public.feedback_posts set votes_count = greatest(votes_count - 1, 0) where id = OLD.post_id; return OLD;
  end if; return null;
end $$;
drop trigger if exists trg_sync_feedback_votes_count on public.feedback_votes;
create trigger trg_sync_feedback_votes_count after insert or delete on public.feedback_votes
  for each row execute function public.sync_feedback_votes_count();

create or replace function public.sync_feedback_replies_count() returns trigger
language plpgsql security definer set search_path to 'pg_catalog','public' as $$
begin
  if TG_OP = 'INSERT' then update public.feedback_posts set replies_count = replies_count + 1 where id = NEW.post_id; return NEW;
  elsif TG_OP = 'DELETE' then update public.feedback_posts set replies_count = greatest(replies_count - 1, 0) where id = OLD.post_id; return OLD;
  end if; return null;
end $$;
drop trigger if exists trg_sync_feedback_replies_count on public.feedback_replies;
create trigger trg_sync_feedback_replies_count after insert or delete on public.feedback_replies
  for each row execute function public.sync_feedback_replies_count();

create or replace function private.feedback_json(p public.feedback_posts, p_uid uuid, p_staff boolean default false) returns jsonb
language sql stable security definer set search_path to 'pg_catalog','public' as $$
  select jsonb_build_object(
    'id', p.id, 'kind', p.kind, 'title', p.title, 'body', p.body, 'status', p.status,
    'votes_count', p.votes_count, 'replies_count', p.replies_count,
    'created_at', p.created_at, 'updated_at', p.updated_at,
    'is_mine', p.user_id = p_uid,
    'has_voted', exists (select 1 from public.feedback_votes v where v.post_id = p.id and v.user_id = p_uid),
    'has_update', (p.user_id = p_uid and p.updated_at > p.author_seen_at),
    'app_version', case when p.user_id = p_uid or p_staff then p.app_version end,
    'device_info', case when p.user_id = p_uid or p_staff then p.device_info end,
    'author', (select jsonb_build_object('id', pr.auth_user_id, 'username', pr.username,
                 'name', coalesce(nullif(pr.profile_name,''), pr.username), 'avatar_url', pr.avatar_url)
                 from public.profiles pr where pr.auth_user_id = p.user_id));
$$;

create or replace function public.create_feedback(
  p_kind text default null, p_title text default null, p_body text default null,
  p_app_version text default null, p_device_info text default null) returns jsonb
language plpgsql security definer set search_path to 'pg_catalog','public' as $$
declare v_uid uuid := auth.uid(); v_title text := btrim(coalesce(p_title,'')); v_row public.feedback_posts;
begin
  if v_uid is null then raise exception 'not_authenticated' using errcode = '28000'; end if;
  if p_kind is null or p_kind not in ('bug','feature') then raise exception 'invalid_kind' using errcode = '22023'; end if;
  if char_length(v_title) < 3 then raise exception 'title_too_short' using errcode = '22023'; end if;
  if char_length(v_title) > 80 then raise exception 'title_too_long' using errcode = '22023'; end if;
  if char_length(btrim(coalesce(p_body,''))) > 1500 then raise exception 'body_too_long' using errcode = '22023'; end if;
  if (select count(*) from public.feedback_posts where user_id = v_uid and created_at > now() - interval '1 day') >= 10 then
    raise exception 'rate_limited' using errcode = '22023';
  end if;
  insert into public.feedback_posts (user_id, kind, title, body, app_version, device_info)
  values (v_uid, p_kind, v_title, btrim(coalesce(p_body,'')), left(nullif(btrim(coalesce(p_app_version,'')),''), 40),
          left(nullif(btrim(coalesce(p_device_info,'')),''), 200))
  returning * into v_row;
  if p_kind = 'feature' then insert into public.feedback_votes (post_id, user_id) values (v_row.id, v_uid); end if;
  select * into v_row from public.feedback_posts where id = v_row.id;
  return private.feedback_json(v_row, v_uid);
end $$;

create or replace function public.get_feedback_board(
  p_kind text default 'feature', p_status text default null, p_sort text default 'top',
  p_limit integer default 20, p_offset integer default 0) returns jsonb
language plpgsql stable security definer set search_path to 'pg_catalog','public' as $$
declare v_uid uuid := auth.uid(); v_res jsonb;
  v_lim int := least(greatest(coalesce(p_limit,20),1),50); v_off int := greatest(coalesce(p_offset,0),0);
begin
  if v_uid is null then raise exception 'not_authenticated' using errcode = '28000'; end if;
  if coalesce(p_kind,'feature') <> 'feature' then raise exception 'invalid_kind' using errcode = '22023'; end if;
  if coalesce(p_sort,'top') not in ('top','new') then raise exception 'invalid_sort' using errcode = '22023'; end if;
  with page as (
    select p.id, row_number() over (order by case when coalesce(p_sort,'top') = 'top' then p.votes_count else 0 end desc, p.created_at desc, p.id desc) rn
      from public.feedback_posts p
     where p.kind = 'feature'
       and ((p_status is not null and p.status = p_status) or (p_status is null and p.status not in ('declined','duplicate')))
     order by rn limit v_lim offset v_off)
  select coalesce(jsonb_agg(private.feedback_json(pp, v_uid) order by page.rn), '[]'::jsonb) into v_res
    from page join public.feedback_posts pp on pp.id = page.id;
  return v_res;
end $$;

create or replace function public.get_my_feedback(p_limit integer default 30, p_offset integer default 0) returns jsonb
language plpgsql stable security definer set search_path to 'pg_catalog','public' as $$
declare v_uid uuid := auth.uid(); v_res jsonb;
  v_lim int := least(greatest(coalesce(p_limit,30),1),100); v_off int := greatest(coalesce(p_offset,0),0);
begin
  if v_uid is null then raise exception 'not_authenticated' using errcode = '28000'; end if;
  with page as (
    select p.id, row_number() over (order by p.updated_at desc, p.id desc) rn
      from public.feedback_posts p where p.user_id = v_uid order by rn limit v_lim offset v_off)
  select coalesce(jsonb_agg(private.feedback_json(pp, v_uid) order by page.rn), '[]'::jsonb) into v_res
    from page join public.feedback_posts pp on pp.id = page.id;
  return v_res;
end $$;

create or replace function public.get_feedback(p_id bigint default null) returns jsonb
language plpgsql stable security definer set search_path to 'pg_catalog','public' as $$
declare v_uid uuid := auth.uid(); v_staff boolean := private.is_staff(); v_row public.feedback_posts; v_replies jsonb;
begin
  if v_uid is null then raise exception 'not_authenticated' using errcode = '28000'; end if;
  select * into v_row from public.feedback_posts p where p.id = p_id and (p.user_id = v_uid or p.kind = 'feature' or v_staff);
  if not found then raise exception 'feedback_not_found' using errcode = '22023'; end if;
  select coalesce(jsonb_agg(jsonb_build_object('id', r.id, 'body', r.body, 'is_staff', r.is_staff, 'created_at', r.created_at,
           'is_mine', r.user_id = v_uid,
           'author', case when r.is_staff then null else (select jsonb_build_object('id', pr.auth_user_id, 'name', coalesce(nullif(pr.profile_name,''), pr.username), 'avatar_url', pr.avatar_url)
                        from public.profiles pr where pr.auth_user_id = r.user_id) end) order by r.created_at, r.id), '[]'::jsonb)
    into v_replies from (select * from public.feedback_replies where post_id = p_id order by created_at, id limit 200) r;
  return private.feedback_json(v_row, v_uid, v_staff) || jsonb_build_object('replies', v_replies);
end $$;

create or replace function public.toggle_feedback_vote(p_id bigint default null) returns jsonb
language plpgsql security definer set search_path to 'pg_catalog','public' as $$
declare v_uid uuid := auth.uid(); v_n int; v_voted boolean; v_cnt int;
begin
  if v_uid is null then raise exception 'not_authenticated' using errcode = '28000'; end if;
  if not exists (select 1 from public.feedback_posts where id = p_id and kind = 'feature') then
    raise exception 'feedback_not_found' using errcode = '22023';
  end if;
  if not exists (select 1 from public.feedback_posts where id = p_id and status in ('open','planned','in_progress')) then
    raise exception 'voting_closed' using errcode = '22023';
  end if;
  insert into public.feedback_votes (post_id, user_id) values (p_id, v_uid) on conflict do nothing;
  get diagnostics v_n = row_count;
  if v_n = 1 then v_voted := true;
  else delete from public.feedback_votes where post_id = p_id and user_id = v_uid; v_voted := false; end if;
  select votes_count into v_cnt from public.feedback_posts where id = p_id;
  return jsonb_build_object('voted', v_voted, 'votes_count', v_cnt);
end $$;

create or replace function public.add_feedback_reply(p_id bigint default null, p_body text default null) returns jsonb
language plpgsql security definer set search_path to 'pg_catalog','public' as $$
declare v_uid uuid := auth.uid(); v_staff boolean := private.is_staff(); v_text text := btrim(coalesce(p_body,'')); v_owner uuid; v_row public.feedback_replies;
begin
  if v_uid is null then raise exception 'not_authenticated' using errcode = '28000'; end if;
  if v_text = '' then raise exception 'empty_message' using errcode = '22023'; end if;
  if char_length(v_text) > 1000 then raise exception 'message_too_long' using errcode = '22023'; end if;
  select user_id into v_owner from public.feedback_posts where id = p_id;
  if v_owner is null then raise exception 'feedback_not_found' using errcode = '22023'; end if;
  if v_owner <> v_uid and not v_staff then raise exception 'forbidden' using errcode = '42501'; end if;
  if (select count(*) from public.feedback_replies where user_id = v_uid and created_at > now() - interval '10 minutes') >= 20 then
    raise exception 'rate_limited' using errcode = '22023';
  end if;
  insert into public.feedback_replies (post_id, user_id, is_staff, body) values (p_id, v_uid, v_staff and v_owner <> v_uid, v_text) returning * into v_row;
  if v_owner = v_uid then update public.feedback_posts set updated_at = now(), author_seen_at = now() where id = p_id;
  else update public.feedback_posts set updated_at = now() where id = p_id; end if;
  return jsonb_build_object('id', v_row.id, 'body', v_row.body, 'is_staff', v_row.is_staff, 'created_at', v_row.created_at, 'is_mine', true);
end $$;

create or replace function public.mark_feedback_seen(p_id bigint default null) returns boolean
language plpgsql security definer set search_path to 'pg_catalog','public' as $$
declare v_uid uuid := auth.uid();
begin
  if v_uid is null then raise exception 'not_authenticated' using errcode = '28000'; end if;
  update public.feedback_posts set author_seen_at = now() where id = p_id and user_id = v_uid;
  return found;
end $$;

create or replace function public.delete_feedback(p_id bigint default null) returns boolean
language plpgsql security definer set search_path to 'pg_catalog','public' as $$
declare v_uid uuid := auth.uid(); v_n int;
begin
  if v_uid is null then raise exception 'not_authenticated' using errcode = '28000'; end if;
  delete from public.feedback_posts where id = p_id and user_id = v_uid;
  get diagnostics v_n = row_count;
  if v_n = 0 then raise exception 'feedback_not_found' using errcode = '22023'; end if;
  return true;
end $$;

create or replace function public.admin_list_feedback(
  p_kind text default null, p_status text default null, p_limit integer default 30, p_offset integer default 0) returns jsonb
language plpgsql stable security definer set search_path to 'pg_catalog','public' as $$
declare v_uid uuid := auth.uid(); v_res jsonb;
  v_lim int := least(greatest(coalesce(p_limit,30),1),100); v_off int := greatest(coalesce(p_offset,0),0);
begin
  if v_uid is null then raise exception 'not_authenticated' using errcode = '28000'; end if;
  if not private.is_staff() then raise exception 'forbidden' using errcode = '42501'; end if;
  with page as (
    select p.id, row_number() over (order by (p.status = 'open') desc, p.updated_at desc, p.id desc) rn
      from public.feedback_posts p
     where (p_kind is null or p.kind = p_kind) and (p_status is null or p.status = p_status)
     order by rn limit v_lim offset v_off)
  select coalesce(jsonb_agg(private.feedback_json(pp, v_uid, true) order by page.rn), '[]'::jsonb) into v_res
    from page join public.feedback_posts pp on pp.id = page.id;
  return v_res;
end $$;

create or replace function public.admin_set_feedback_status(
  p_id bigint default null, p_status text default null, p_note text default null) returns boolean
language plpgsql security definer set search_path to 'pg_catalog','public' as $$
declare v_uid uuid := auth.uid(); v_n int; v_note text := btrim(coalesce(p_note,''));
begin
  if v_uid is null then raise exception 'not_authenticated' using errcode = '28000'; end if;
  if not private.is_staff() then raise exception 'forbidden' using errcode = '42501'; end if;
  if p_status is null or p_status not in ('open','planned','in_progress','done','declined','duplicate') then
    raise exception 'invalid_status' using errcode = '22023';
  end if;
  if char_length(v_note) > 1000 then raise exception 'message_too_long' using errcode = '22023'; end if;
  update public.feedback_posts set status = p_status, updated_at = now() where id = p_id;
  get diagnostics v_n = row_count;
  if v_n = 0 then raise exception 'feedback_not_found' using errcode = '22023'; end if;
  if v_note <> '' then insert into public.feedback_replies (post_id, user_id, is_staff, body) values (p_id, v_uid, true, v_note); end if;
  return true;
end $$;

do $$ declare r record; begin
  for r in select p.oid::regprocedure as sig from pg_proc p join pg_namespace n on n.oid = p.pronamespace
            where n.nspname = 'public' and p.proname in (
              'create_feedback','get_feedback_board','get_my_feedback','get_feedback','toggle_feedback_vote',
              'add_feedback_reply','mark_feedback_seen','delete_feedback','admin_list_feedback','admin_set_feedback_status')
  loop
    execute format('revoke all on function %s from public, anon', r.sig);
    execute format('grant execute on function %s to authenticated', r.sig);
  end loop;
end $$;

-- account deletion must also purge the feedback tables
do $do$ declare d text;
begin
  d := pg_get_functiondef('public.purge_user_data(uuid,text)'::regprocedure);
  if position('feedback_posts' in d) = 0 and position('(''moment_comment_likes'', ''user_id = $1''),' in d) > 0 then
    d := replace(d, '(''moment_comment_likes'', ''user_id = $1''),',
      '(''feedback_votes'', ''user_id = $1''), (''feedback_replies'', ''user_id = $1''), (''feedback_posts'', ''user_id = $1''), (''moment_comment_likes'', ''user_id = $1''),');
    execute d;
  end if;
end $do$;

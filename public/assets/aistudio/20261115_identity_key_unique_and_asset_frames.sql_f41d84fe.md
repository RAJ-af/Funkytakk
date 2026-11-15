-- 1) The same E2EE identity key must never belong to two accounts (found: 3 keys shared by 2/2/4 accounts,
--    caused by the app reusing the local key pair after logout/login with another account).
--    Existing duplicates are NOT touched (test accounts); only NEW writes are blocked.
create index if not exists idx_user_public_keys_identity on public.user_public_keys (identity_public_key);

create or replace function public.guard_identity_key_unique() returns trigger
language plpgsql security definer set search_path to 'pg_catalog','public' as $$
begin
  -- unchanged key (e.g. a re-upload of an old duplicate) is tolerated; only NEW sharing is blocked
  if TG_OP = 'UPDATE' and NEW.identity_public_key is not distinct from OLD.identity_public_key then return NEW; end if;
  if exists (select 1 from public.user_public_keys k
              where k.identity_public_key = NEW.identity_public_key and k.user_id <> NEW.user_id) then
    raise exception 'identity_key_in_use' using errcode = '23505';
  end if;
  return NEW;
end $$;
drop trigger if exists trg_guard_identity_key_unique on public.user_public_keys;
create trigger trg_guard_identity_key_unique
  before insert or update of identity_public_key on public.user_public_keys
  for each row execute function public.guard_identity_key_unique();

-- 2) Public bucket for store assets (frames now, more later). Files are uploaded in the dashboard.
insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('decorations', 'decorations', true, 10485760, array['image/webp','image/png','image/gif','video/mp4'])
on conflict (id) do nothing;

-- 3) Three asset-based avatar frames. is_active = false until the .webp files are uploaded to decorations/frames/.
insert into public.store_items (code, item_type, name, description, preview_url, price_coins, config, sort_order, is_active) values
 ('frame_floral_moon', 'avatar_frame', 'Floral Moon', 'Golden moon, lantern and white blossoms',
  'https://dulpqochkxtamqztauea.supabase.co/storage/v1/object/public/decorations/frames/frame_floral.webp', 0,
  '{"kind":"asset","asset":{"url":"https://dulpqochkxtamqztauea.supabase.co/storage/v1/object/public/decorations/frames/frame_floral.webp","format":"webp","size_px":320,"fps":24,"avatar_ratio":0.6},"ring":null,"glow":null,"ornament":null,"particles":null}'::jsonb, 60, false),
 ('frame_sea_voyage', 'avatar_frame', 'Sea Voyage', 'A ship sailing through golden waves',
  'https://dulpqochkxtamqztauea.supabase.co/storage/v1/object/public/decorations/frames/frame_ship.webp', 0,
  '{"kind":"asset","asset":{"url":"https://dulpqochkxtamqztauea.supabase.co/storage/v1/object/public/decorations/frames/frame_ship.webp","format":"webp","size_px":320,"fps":24,"avatar_ratio":0.6},"ring":null,"glow":null,"ornament":null,"particles":null}'::jsonb, 70, false),
 ('frame_waterfall_garden', 'avatar_frame', 'Waterfall Garden', 'Misty waterfall with blossoming trees',
  'https://dulpqochkxtamqztauea.supabase.co/storage/v1/object/public/decorations/frames/frame_waterfall.webp', 0,
  '{"kind":"asset","asset":{"url":"https://dulpqochkxtamqztauea.supabase.co/storage/v1/object/public/decorations/frames/frame_waterfall.webp","format":"webp","size_px":320,"fps":24,"avatar_ratio":0.6},"ring":null,"glow":null,"ornament":null,"particles":null}'::jsonb, 80, false)
on conflict (code) do nothing;

-- ============================================================================
-- FUNKYTALK: USER BADGES & PROFILE COVERS SUPPORT
-- ============================================================================

-- 1. Ensure cover_url and city exist on profiles
ALTER TABLE IF EXISTS public.profiles
ADD COLUMN IF NOT EXISTS cover_url TEXT,
ADD COLUMN IF NOT EXISTS city TEXT;

-- 2. Badge Catalog Table
CREATE TABLE IF NOT EXISTS public.badge_catalog (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    icon_url TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Seed initial catalog badges
INSERT INTO public.badge_catalog (code, name, description, icon_url, is_active)
VALUES 
    ('founder', 'Founder', 'Distinguished early pioneer and core founder of FunkyTalk', 'https://api.dicebear.com/7.x/shapes/svg?seed=founder&backgroundColor=ffd700', true),
    ('early_adopter', 'Early Adopter', 'Joined FunkyTalk during initial launch', 'https://api.dicebear.com/7.x/shapes/svg?seed=early&backgroundColor=60a5fa', true),
    ('popular', 'Popular Voice', 'Frequently hosting active and vibrant voice rooms', 'https://api.dicebear.com/7.x/shapes/svg?seed=popular&backgroundColor=f472b6', true),
    ('generous', 'Generous Giver', 'Top supporter spreading joy through animated gifts', 'https://api.dicebear.com/7.x/shapes/svg?seed=generous&backgroundColor=34d399', true),
    ('vip', 'VIP Star', 'Special VIP community status', 'https://api.dicebear.com/7.x/shapes/svg?seed=vip&backgroundColor=a78bfa', true)
ON CONFLICT (code) DO UPDATE SET
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    is_active = EXCLUDED.is_active;

-- 3. User Badges Table
CREATE TABLE IF NOT EXISTS public.user_badges (
    id BIGSERIAL PRIMARY KEY,
    user_id TEXT NOT NULL,
    badge_id BIGINT NOT NULL REFERENCES public.badge_catalog(id) ON DELETE CASCADE,
    awarded_at TIMESTAMPTZ DEFAULT NOW(),
    awarded_by TEXT,
    CONSTRAINT unique_user_badge UNIQUE (user_id, badge_id)
);

-- Enable RLS
ALTER TABLE public.badge_catalog ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_badges ENABLE ROW LEVEL SECURITY;

-- Read policies
DROP POLICY IF EXISTS "Public can view badge catalog" ON public.badge_catalog;
CREATE POLICY "Public can view badge catalog" ON public.badge_catalog FOR SELECT USING (true);

DROP POLICY IF EXISTS "Public can view user badges" ON public.user_badges;
CREATE POLICY "Public can view user badges" ON public.user_badges FOR SELECT USING (true);

-- 4. RPC: award_badge
CREATE OR REPLACE FUNCTION public.award_badge(
    p_user_id TEXT,
    p_badge_code TEXT
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    v_badge_id BIGINT;
    v_caller_id TEXT;
BEGIN
    v_caller_id := auth.uid()::text;
    
    SELECT id INTO v_badge_id FROM public.badge_catalog WHERE code = p_badge_code AND is_active = true;
    IF v_badge_id IS NULL THEN
        RETURN jsonb_build_object('success', false, 'error', 'Badge not found or inactive');
    END IF;

    INSERT INTO public.user_badges (user_id, badge_id, awarded_at, awarded_by)
    VALUES (p_user_id, v_badge_id, NOW(), v_caller_id)
    ON CONFLICT (user_id, badge_id) DO UPDATE SET awarded_at = NOW();

    RETURN jsonb_build_object('success', true, 'user_id', p_user_id, 'badge_code', p_badge_code);
END;
$$;

-- 5. RPC: revoke_badge
CREATE OR REPLACE FUNCTION public.revoke_badge(
    p_user_id TEXT,
    p_badge_code TEXT
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    v_badge_id BIGINT;
BEGIN
    SELECT id INTO v_badge_id FROM public.badge_catalog WHERE code = p_badge_code;
    IF v_badge_id IS NULL THEN
        RETURN jsonb_build_object('success', false, 'error', 'Badge not found');
    END IF;

    DELETE FROM public.user_badges
    WHERE user_id = p_user_id AND badge_id = v_badge_id;

    RETURN jsonb_build_object('success', true, 'user_id', p_user_id, 'badge_code', p_badge_code);
END;
$$;

-- ============================================================================
-- FUNKYTALK ARCHITECTURE FIX: SECURE TOKEN BRIDGE & USER ROLES SECURITY
-- ============================================================================

-- 1. Create Dedicated user_roles Table (Decoupled from public users table)
CREATE TABLE IF NOT EXISTS public.user_roles (
    user_id TEXT PRIMARY KEY REFERENCES public.users(uid) ON DELETE CASCADE,
    role TEXT NOT NULL DEFAULT 'user' CHECK (role IN ('user', 'moderator', 'admin', 'superadmin')),
    granted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    granted_by TEXT
);

-- Enable RLS on user_roles (Read-only for self, write restricted to service_role)
ALTER TABLE public.user_roles ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view their own role"
ON public.user_roles
FOR SELECT
TO authenticated
USING (user_id = auth.uid()::text);

-- 2. Secure RPC for Moderator Actions (SECURITY DEFINER to verify role server-side)
CREATE OR REPLACE FUNCTION public.execute_moderator_action(
    target_user_id TEXT,
    action_type TEXT,
    room_id TEXT DEFAULT NULL,
    reason TEXT DEFAULT ''
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    caller_role TEXT;
    result JSONB;
BEGIN
    -- Check caller's role from user_roles table
    SELECT role INTO caller_role 
    FROM public.user_roles 
    WHERE user_id = auth.uid()::text;

    IF caller_role NOT IN ('moderator', 'admin', 'superadmin') THEN
        RAISE EXCEPTION 'Unauthorized: Caller does not possess moderator privileges.';
    END IF;

    -- Handle requested moderation actions safely
    IF action_type = 'kick_from_room' AND room_id IS NOT NULL THEN
        -- Remove target user from voice room participants
        DELETE FROM public.room_participants 
        WHERE room_id = execute_moderator_action.room_id 
          AND user_id = target_user_id;
        
        result := jsonb_build_object('success', true, 'action', 'kicked', 'target', target_user_id);

    ELSIF action_type = 'mute_mic' AND room_id IS NOT NULL THEN
        -- Update participant mic state
        UPDATE public.room_participants 
        SET is_mic_enabled = false 
        WHERE room_id = execute_moderator_action.room_id 
          AND user_id = target_user_id;

        result := jsonb_build_object('success', true, 'action', 'muted', 'target', target_user_id);

    ELSIF action_type = 'ban_user' THEN
        IF caller_role NOT IN ('admin', 'superadmin') THEN
            RAISE EXCEPTION 'Unauthorized: Only admins can ban users app-wide.';
        END IF;

        UPDATE public.users 
        SET is_banned = true 
        WHERE uid = target_user_id;

        result := jsonb_build_object('success', true, 'action', 'banned', 'target', target_user_id);

    ELSE
        RAISE EXCEPTION 'Invalid or unsupported action_type: %', action_type;
    END IF;

    -- Log action for audit
    INSERT INTO public.moderation_logs (caller_id, target_id, action_type, reason, created_at)
    VALUES (auth.uid()::text, target_user_id, action_type, reason, NOW());

    RETURN result;
END;
$$;

-- ============================================================================
-- Migration: Voice Room Lifecycle, Host 3-Minute Grace Period & Speaker Request Cleanup
-- ============================================================================

-- 1. Add host_left_at and ended_at columns to voice_rooms table if not already present
ALTER TABLE public.voice_rooms 
ADD COLUMN IF NOT EXISTS host_left_at TIMESTAMPTZ DEFAULT NULL,
ADD COLUMN IF NOT EXISTS ended_at TIMESTAMPTZ DEFAULT NULL;

-- 2. Allow users to DELETE their own pending speaker requests when leaving
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies 
        WHERE tablename = 'speaker_requests' 
        AND policyname = 'Users can delete own pending speaker request'
    ) THEN
        CREATE POLICY "Users can delete own pending speaker request"
        ON public.speaker_requests
        FOR DELETE
        TO authenticated
        USING (user_id = auth.uid()::text AND status = 'pending');
    END IF;
END $$;

-- 3. Allow Hosts to update speaker requests in their rooms
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies 
        WHERE tablename = 'speaker_requests' 
        AND policyname = 'Hosts can update speaker requests'
    ) THEN
        CREATE POLICY "Hosts can update speaker requests"
        ON public.speaker_requests
        FOR UPDATE
        TO authenticated
        USING (
            EXISTS (
                SELECT 1 FROM public.voice_rooms vr
                WHERE vr.id = speaker_requests.room_id
                  AND vr.host_id = auth.uid()::text
            )
        )
        WITH CHECK (
            EXISTS (
                SELECT 1 FROM public.voice_rooms vr
                WHERE vr.id = speaker_requests.room_id
                  AND vr.host_id = auth.uid()::text
            )
        );
    END IF;
END $$;

-- 4. RPC: Respond to speaker request (Accept/Decline)
CREATE OR REPLACE FUNCTION public.respond_to_speaker_request(
    request_id UUID,
    accept BOOLEAN
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    req_record RECORD;
    v_host_id TEXT;
    caller_id TEXT := auth.uid()::text;
    caller_role TEXT;
BEGIN
    -- Fetch speaker request
    SELECT * INTO req_record 
    FROM public.speaker_requests 
    WHERE id = request_id;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Speaker request not found.';
    END IF;

    -- Fetch room host_id
    SELECT host_id INTO v_host_id 
    FROM public.voice_rooms 
    WHERE id = req_record.room_id;

    -- Check if caller is the host or admin/mod
    SELECT role INTO caller_role FROM public.user_roles WHERE user_id = caller_id;
    IF caller_id != v_host_id AND caller_role NOT IN ('moderator', 'admin', 'superadmin') THEN
        RAISE EXCEPTION 'Unauthorized: Only the host can respond to speaker requests.';
    END IF;

    IF accept THEN
        -- Mark request accepted
        UPDATE public.speaker_requests 
        SET status = 'accepted' 
        WHERE id = request_id;

        -- Promote user to speaker in room_participants
        INSERT INTO public.room_participants (room_id, user_id, role, is_mic_enabled, joined_at)
        VALUES (req_record.room_id, req_record.user_id, 'speaker', true, NOW())
        ON CONFLICT (room_id, user_id) 
        DO UPDATE SET role = 'speaker', is_mic_enabled = true;

        RETURN jsonb_build_object(
            'success', true,
            'action', 'accepted',
            'room_id', req_record.room_id,
            'user_id', req_record.user_id
        );
    ELSE
        -- Mark request declined
        UPDATE public.speaker_requests 
        SET status = 'declined' 
        WHERE id = request_id;

        RETURN jsonb_build_object(
            'success', true,
            'action', 'declined',
            'room_id', req_record.room_id,
            'user_id', req_record.user_id
        );
    END IF;
END;
$$;

-- 5. RPC: Leave Voice Room with Speaker Request Cleanup and Host Away timer
CREATE OR REPLACE FUNCTION public.leave_voice_room(
    p_room_id UUID
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    caller_id TEXT := auth.uid()::text;
    v_host_id TEXT;
    is_caller_host BOOLEAN := false;
BEGIN
    -- Check if room exists and get host_id
    SELECT host_id INTO v_host_id 
    FROM public.voice_rooms 
    WHERE id = p_room_id;

    IF v_host_id IS NOT NULL AND v_host_id = caller_id THEN
        is_caller_host := true;
        -- Start 3-minute grace period on voice_rooms
        UPDATE public.voice_rooms 
        SET host_left_at = NOW() 
        WHERE id = p_room_id;
    END IF;

    -- Remove caller from room participants
    DELETE FROM public.room_participants 
    WHERE room_id = p_room_id 
      AND user_id = caller_id;

    -- Immediately remove pending speaker request from this user in this room
    DELETE FROM public.speaker_requests 
    WHERE room_id = p_room_id 
      AND user_id = caller_id 
      AND status = 'pending';

    RETURN jsonb_build_object(
        'success', true,
        'room_id', p_room_id,
        'user_id', caller_id,
        'is_host', is_caller_host
    );
END;
$$;

-- 6. RPC: Cancel single speaker request
CREATE OR REPLACE FUNCTION public.cancel_speaker_request(
    p_room_id UUID
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    caller_id TEXT := auth.uid()::text;
BEGIN
    DELETE FROM public.speaker_requests 
    WHERE room_id = p_room_id 
      AND user_id = caller_id 
      AND status = 'pending';

    RETURN jsonb_build_object('success', true, 'room_id', p_room_id);
END;
$$;

-- 6b. RPC: Request speaker seat (Creates or resets pending speaker request cleanly)
CREATE OR REPLACE FUNCTION public.request_speaker_seat(
    p_room_id UUID
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    caller_id TEXT := auth.uid()::text;
    req_id UUID;
BEGIN
    -- Remove any old declined/accepted/stale request for this user in this room to allow clean re-request
    DELETE FROM public.speaker_requests
    WHERE room_id = p_room_id AND user_id = caller_id;

    -- Insert fresh pending request
    INSERT INTO public.speaker_requests (room_id, user_id, status, created_at)
    VALUES (p_room_id, caller_id, 'pending', NOW())
    RETURNING id INTO req_id;

    RETURN jsonb_build_object(
        'success', true, 
        'status', 'pending', 
        'request_id', req_id,
        'room_id', p_room_id,
        'user_id', caller_id
    );
END;
$$;

-- 7. RPC: Host Rejoin Voice Room (Cancels 3-minute grace period if within time)
CREATE OR REPLACE FUNCTION public.host_rejoin_voice_room(
    p_room_id UUID
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    caller_id TEXT := auth.uid()::text;
    room_record RECORD;
BEGIN
    SELECT * INTO room_record 
    FROM public.voice_rooms 
    WHERE id = p_room_id;

    IF NOT FOUND THEN
        RETURN jsonb_build_object('success', false, 'status', 'not_found');
    END IF;

    -- If caller is not the host, simply rejoin as normal
    IF room_record.host_id != caller_id THEN
        RETURN jsonb_build_object('success', true, 'is_host', false);
    END IF;

    -- If room was already marked ended, return ended
    IF room_record.is_live = false OR room_record.status = 'ended' THEN
        RETURN jsonb_build_object('success', false, 'status', 'ended');
    END IF;

    -- Check if host left more than 3 minutes ago
    IF room_record.host_left_at IS NOT NULL AND room_record.host_left_at < (NOW() - INTERVAL '3 minutes') THEN
        -- Auto-end room due to expired grace period
        UPDATE public.voice_rooms 
        SET is_live = false, 
            status = 'ended', 
            ended_at = NOW() 
        WHERE id = p_room_id;

        RETURN jsonb_build_object('success', false, 'status', 'ended', 'reason', 'grace_period_expired');
    END IF;

    -- Host returned within 3-minute grace period: clear host_left_at & re-add as active host
    UPDATE public.voice_rooms 
    SET host_left_at = NULL 
    WHERE id = p_room_id;

    INSERT INTO public.room_participants (room_id, user_id, role, is_mic_enabled, joined_at)
    VALUES (p_room_id, caller_id, 'host', true, NOW())
    ON CONFLICT (room_id, user_id) 
    DO UPDATE SET role = 'host', is_mic_enabled = true;

    RETURN jsonb_build_object(
        'success', true, 
        'status', 'active', 
        'is_host', true, 
        'grace_period_cancelled', true
    );
END;
$$;

-- 8. RPC: End Voice Room explicitly
CREATE OR REPLACE FUNCTION public.end_voice_room(
    p_room_id UUID
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    caller_id TEXT := auth.uid()::text;
    v_host_id TEXT;
    caller_role TEXT;
BEGIN
    SELECT host_id INTO v_host_id 
    FROM public.voice_rooms 
    WHERE id = p_room_id;

    IF NOT FOUND THEN
        RETURN jsonb_build_object('success', false, 'error', 'Room not found');
    END IF;

    SELECT role INTO caller_role FROM public.user_roles WHERE user_id = caller_id;

    IF caller_id != v_host_id AND caller_role NOT IN ('moderator', 'admin', 'superadmin') THEN
        RAISE EXCEPTION 'Unauthorized: Only the host or moderator can end the room.';
    END IF;

    -- Mark room as ended
    UPDATE public.voice_rooms 
    SET is_live = false,
        status = 'ended',
        ended_at = NOW(),
        host_left_at = NULL
    WHERE id = p_room_id;

    -- Remove all active participants
    DELETE FROM public.room_participants 
    WHERE room_id = p_room_id;

    -- Cancel all pending speaker requests
    DELETE FROM public.speaker_requests 
    WHERE room_id = p_room_id AND status = 'pending';

    RETURN jsonb_build_object('success', true, 'room_id', p_room_id, 'status', 'ended');
END;
$$;

-- 9. Function to check and auto-cleanup voice rooms with expired host grace period (> 3 mins)
CREATE OR REPLACE FUNCTION public.check_and_cleanup_ended_rooms()
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
BEGIN
    -- 1. Auto-end rooms where Host has been away > 3 minutes
    UPDATE public.voice_rooms
    SET is_live = false,
        status = 'ended',
        ended_at = NOW()
    WHERE is_live = true
      AND status = 'active'
      AND host_left_at IS NOT NULL
      AND host_left_at < (NOW() - INTERVAL '3 minutes');

    -- 2. Clean up participants of ended rooms
    DELETE FROM public.room_participants
    WHERE room_id IN (
        SELECT id FROM public.voice_rooms 
        WHERE is_live = false OR status = 'ended'
    );
END;
$$;

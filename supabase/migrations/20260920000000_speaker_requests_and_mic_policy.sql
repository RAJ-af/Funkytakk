-- Migration for speaker_requests and participant self-update mic policy

-- 1. Ensure speaker_requests table exists with UNIQUE constraint on (room_id, user_id) for pending requests
CREATE TABLE IF NOT EXISTS public.speaker_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    room_id UUID NOT NULL REFERENCES public.voice_rooms(id) ON DELETE CASCADE,
    user_id TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'pending' CHECK (status IN ('pending', 'accepted', 'declined')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Ensure index and unique constraint for one pending request per room per user
CREATE UNIQUE INDEX IF NOT EXISTS idx_speaker_requests_pending_unique
ON public.speaker_requests (room_id, user_id)
WHERE status = 'pending';

-- Enable RLS
ALTER TABLE public.speaker_requests ENABLE ROW LEVEL SECURITY;

-- Allow listeners in the room to insert their own speaker request
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies 
        WHERE tablename = 'speaker_requests' 
        AND policyname = 'Listeners can create speaker request'
    ) THEN
        CREATE POLICY "Listeners can create speaker request"
        ON public.speaker_requests
        FOR INSERT
        TO authenticated
        WITH CHECK (
            user_id = auth.uid()::text
            AND EXISTS (
                SELECT 1 FROM public.room_participants rp
                WHERE rp.room_id = speaker_requests.room_id
                  AND rp.user_id = auth.uid()::text
                  AND rp.role = 'listener'
            )
        );
    END IF;
END $$;

-- Allow room participants & host to read speaker requests
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies 
        WHERE tablename = 'speaker_requests' 
        AND policyname = 'Participants can view speaker requests'
    ) THEN
        CREATE POLICY "Participants can view speaker requests"
        ON public.speaker_requests
        FOR SELECT
        TO authenticated
        USING (
            user_id = auth.uid()::text
            OR EXISTS (
                SELECT 1 FROM public.voice_rooms vr
                WHERE vr.id = speaker_requests.room_id
                  AND vr.host_id = auth.uid()::text
            )
        );
    END IF;
END $$;

-- Allow users to update their own mic status in room_participants
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies 
        WHERE tablename = 'room_participants' 
        AND policyname = 'Users can update own mic status'
    ) THEN
        CREATE POLICY "Users can update own mic status"
        ON public.room_participants
        FOR UPDATE
        TO authenticated
        USING (user_id = auth.uid()::text)
        WITH CHECK (user_id = auth.uid()::text);
    END IF;
END $$;

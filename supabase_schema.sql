-- ====================================================================
-- FUNKYTALK SUPABASE SQL SCHEMA & SECURITY ENFORCEMENT
-- Copy and paste directly into your Supabase SQL Editor
-- ====================================================================

-- 1. Table: users
CREATE TABLE IF NOT EXISTS public.users (
    uid VARCHAR(255) PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    display_name VARCHAR(255),
    username VARCHAR(100) UNIQUE,
    avatar TEXT,
    dob VARCHAR(50),
    gender VARCHAR(20),
    native_language VARCHAR(50),
    learning_language VARCHAR(50),
    country VARCHAR(100),
    country_code VARCHAR(10),
    hobbies TEXT[],
    custom_email_verified BOOLEAN DEFAULT FALSE,
    email_verification_token VARCHAR(255) DEFAULT NULL, -- Stores SHA-256 hash of token
    email_verification_expires_at TIMESTAMP WITH TIME ZONE DEFAULT NULL, -- Expiry timestamp
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Ensure columns exist if table was already created
ALTER TABLE public.users
ADD COLUMN IF NOT EXISTS custom_email_verified BOOLEAN DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS email_verification_token VARCHAR(255) DEFAULT NULL,
ADD COLUMN IF NOT EXISTS email_verification_expires_at TIMESTAMP WITH TIME ZONE DEFAULT NULL,
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;

-- 2. Table: rate_limits (for server-side Edge Function rate limiting)
CREATE TABLE IF NOT EXISTS public.rate_limits (
    id BIGSERIAL PRIMARY KEY,
    key VARCHAR(255) NOT NULL,       -- Email or user ID or IP
    action VARCHAR(100) NOT NULL,    -- e.g., 'send-verification-email', 'send-login-notification'
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_rate_limits_key_action_created 
ON public.rate_limits (key, action, created_at);

-- 3. ENABLE ROW LEVEL SECURITY (RLS) ON ALL TABLES
ALTER TABLE public.users ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.rate_limits ENABLE ROW LEVEL SECURITY;

-- 4. RLS POLICIES FOR 'users' TABLE
-- Drop existing policies if any
DROP POLICY IF EXISTS "Users can view own data" ON public.users;
DROP POLICY IF EXISTS "Users can update own data" ON public.users;
DROP POLICY IF EXISTS "Users can insert own record" ON public.users;
DROP POLICY IF EXISTS "Users can delete own record" ON public.users;
DROP POLICY IF EXISTS "Service role full access on users" ON public.users;

-- Policy 1: Users can only read their own profile row
CREATE POLICY "Users can view own data"
ON public.users
FOR SELECT
USING (auth.uid()::text = uid);

-- Policy 2: Users can only insert their own profile row
CREATE POLICY "Users can insert own record"
ON public.users
FOR INSERT
WITH CHECK (auth.uid()::text = uid);

-- Policy 3: Users can only update their own profile row
CREATE POLICY "Users can update own data"
ON public.users
FOR UPDATE
USING (auth.uid()::text = uid)
WITH CHECK (auth.uid()::text = uid);

-- Policy 4: Users can only delete their own profile row
CREATE POLICY "Users can delete own record"
ON public.users
FOR DELETE
USING (auth.uid()::text = uid);

-- Policy 5: Service Role (used by Edge Functions) has full access
CREATE POLICY "Service role full access on users"
ON public.users
FOR ALL
TO service_role
USING (true)
WITH CHECK (true);

-- 5. RLS POLICIES FOR 'rate_limits' TABLE
DROP POLICY IF EXISTS "Service role full access on rate_limits" ON public.rate_limits;
DROP POLICY IF EXISTS "Users can view own rate limits" ON public.rate_limits;

-- Service role has full read/write on rate_limits
CREATE POLICY "Service role full access on rate_limits"
ON public.rate_limits
FOR ALL
TO service_role
USING (true)
WITH CHECK (true);

-- Authenticated users can only read their own rate limit records
CREATE POLICY "Users can view own rate limits"
ON public.rate_limits
FOR SELECT
USING (auth.uid()::text = key);

-- 6. Helper function to purge old rate limits (older than 24 hours)
CREATE OR REPLACE FUNCTION purge_old_rate_limits()
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
BEGIN
    DELETE FROM public.rate_limits
    WHERE created_at < NOW() - INTERVAL '24 hours';
END;
$$;


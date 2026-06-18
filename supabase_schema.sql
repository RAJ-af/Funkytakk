-- ====================================================================
-- FUNKYTALK SUPABASE SQL SCHEMA COMPANION
-- Copy and paste this directly into your Supabase SQL Editor
-- ====================================================================

-- 1. Extend your existing 'users' table with secure verification coordinates
ALTER TABLE public.users
ADD COLUMN IF NOT EXISTS custom_email_verified BOOLEAN DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS email_verification_token VARCHAR(255) DEFAULT NULL;

-- 2. DDL for reference if you are creating the 'users' table from scratch:
/*
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
    email_verification_token VARCHAR(255) DEFAULT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
*/

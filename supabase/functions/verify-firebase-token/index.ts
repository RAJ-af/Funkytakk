// ============================================================================
// SUPABASE EDGE FUNCTION: verify-firebase-token
// Verifies Firebase JWT & issues authenticated Supabase session JWT
// ============================================================================

import { serve } from "https://deno.land/std@0.177.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.39.0";
import * as djwt from "https://deno.land/x/djwt@v2.8/mod.ts";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    const { firebaseIdToken } = await req.json();

    if (!firebaseIdToken) {
      return new Response(
        JSON.stringify({ error: "Missing firebaseIdToken in request body." }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    // 1. Verify Firebase ID Token via Google Public Certificates
    // Note: For production, we fetch Google's public x509 certs or verify with Google API
    const verifyRes = await fetch(
      `https://identitytoolkit.googleapis.com/v1/accounts:lookup?key=${Deno.env.get("FIREBASE_API_KEY")}`,
      {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ idToken: firebaseIdToken }),
      }
    );

    const verifyData = await verifyRes.json();
    if (!verifyRes.ok || !verifyData.users || verifyData.users.length === 0) {
      return new Response(
        JSON.stringify({ error: "Invalid or expired Firebase ID token." }),
        { status: 401, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    const firebaseUser = verifyData.users[0];
    const uid = firebaseUser.localId;
    const email = firebaseUser.email;

    // 2. Mint Supabase JWT with sub = Firebase UID
    const jwtSecret = Deno.env.get("SUPABASE_JWT_SECRET");
    if (!jwtSecret) {
      throw new Error("SUPABASE_JWT_SECRET is not configured in Supabase environment.");
    }

    const key = await crypto.subtle.importKey(
      "raw",
      new TextEncoder().encode(jwtSecret),
      { name: "HMAC", hash: "SHA-256" },
      false,
      ["sign"]
    );

    const now = Math.floor(Date.now() / 1000);
    const payload = {
      aud: "authenticated",
      exp: now + 3600 * 24 * 7, // 7 days validity
      sub: uid,
      email: email,
      role: "authenticated",
      app_metadata: {
        provider: "firebase",
        providers: ["firebase"],
      },
      user_metadata: {
        firebase_uid: uid,
      },
    };

    const supabaseToken = await djwt.create({ alg: "HS256", typ: "JWT" }, payload, key);

    return new Response(
      JSON.stringify({
        access_token: supabaseToken,
        token_type: "bearer",
        expires_in: 3600 * 24 * 7,
        user: {
          id: uid,
          email: email,
        },
      }),
      { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  } catch (error) {
    return new Response(
      JSON.stringify({ error: error.message || "Internal server error" }),
      { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  }
});

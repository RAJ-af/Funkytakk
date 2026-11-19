// ============================================================================
// SUPABASE EDGE FUNCTION: verify-firebase-token
// Verifies Firebase ID token and idempotently returns a Supabase session
// ============================================================================

import { serve } from "https://deno.land/std@0.177.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.39.0";
import { createRemoteJWKSet, jwtVerify } from "https://esm.sh/jose@5.2.0";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

const FIREBASE_JWKS = createRemoteJWKSet(
  new URL("https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com"),
);

const DEFAULT_FIREBASE_PROJECT_ID = "funkytalk-f1aae";

interface VerifiedFirebaseIdentity {
  firebaseUid: string;
  email: string;
  emailVerified: boolean;
}

function jsonResponse(body: Record<string, unknown>, status: number): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });
}

function isUniqueViolation(err: { code?: string; message?: string; status?: number } | null | undefined): boolean {
  if (!err) return false;
  const msg = (err.message || "").toLowerCase();
  return (
    err.code === "23505" ||
    err.code === "email_exists" ||
    err.status === 422 ||
    msg.includes("duplicate key") ||
    msg.includes("already registered") ||
    msg.includes("already exists")
  );
}

async function verifyFirebaseIdToken(idToken: string): Promise<VerifiedFirebaseIdentity> {
  const projectId = Deno.env.get("FIREBASE_PROJECT_ID") || DEFAULT_FIREBASE_PROJECT_ID;
  try {
    const { payload } = await jwtVerify(idToken, FIREBASE_JWKS, {
      issuer: `https://securetoken.google.com/${projectId}`,
      audience: projectId,
    });
    const firebaseUid = String(payload.sub || "");
    const email = typeof payload.email === "string" ? payload.email.trim().toLowerCase() : "";
    if (!firebaseUid || !email) {
      throw new Error("ERR_TOKEN_CLAIMS_MISSING");
    }
    return {
      firebaseUid,
      email,
      emailVerified: Boolean(payload.email_verified),
    };
  } catch (jwksErr) {
    // Fallback to Google Identity Toolkit lookup when FIREBASE_API_KEY is configured
    const apiKey = Deno.env.get("FIREBASE_API_KEY");
    if (!apiKey) {
      console.error("[verify-firebase-token] ERR_JWKS_VERIFY:", (jwksErr as Error)?.message || "unknown");
      throw new Error("ERR_INVALID_FIREBASE_TOKEN");
    }
    const verifyRes = await fetch(
      `https://identitytoolkit.googleapis.com/v1/accounts:lookup?key=${apiKey}`,
      {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ idToken }),
      },
    );
    const verifyData = await verifyRes.json();
    const user = verifyData?.users?.[0];
    if (!verifyRes.ok || !user?.localId || !user?.email) {
      console.error("[verify-firebase-token] ERR_IDENTITY_TOOLKIT:", verifyRes.status);
      throw new Error("ERR_INVALID_FIREBASE_TOKEN");
    }
    return {
      firebaseUid: String(user.localId),
      email: String(user.email).trim().toLowerCase(),
      emailVerified: Boolean(user.emailVerified),
    };
  }
}

serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    const rawBody = await req.json().catch(() => null);
    const idToken = rawBody?.firebase_token || rawBody?.firebaseIdToken;
    if (!idToken || typeof idToken !== "string") {
      return jsonResponse(
        { error_code: "ERR_MISSING_TOKEN", error: "Missing Firebase ID token in request body." },
        400,
      );
    }

    let identity: VerifiedFirebaseIdentity;
    try {
      identity = await verifyFirebaseIdToken(idToken);
    } catch (err) {
      const code = (err as Error)?.message || "ERR_INVALID_FIREBASE_TOKEN";
      return jsonResponse(
        { error_code: code, error: "Invalid or expired Firebase ID token." },
        401,
      );
    }

    const supabaseUrl = Deno.env.get("SUPABASE_URL");
    const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
    const anonKey = Deno.env.get("SUPABASE_ANON_KEY") || serviceRoleKey;

    if (!supabaseUrl || !serviceRoleKey || !anonKey) {
      console.error("[verify-firebase-token] ERR_MISSING_SUPABASE_ENV");
      return jsonResponse(
        { error_code: "ERR_SERVER_MISCONFIGURED", error: "Auth backend configuration error." },
        500,
      );
    }

    const supabaseAdmin = createClient(supabaseUrl, serviceRoleKey, {
      auth: { autoRefreshToken: false, persistSession: false },
    });
    const supabaseAnon = createClient(supabaseUrl, anonKey, {
      auth: { autoRefreshToken: false, persistSession: false },
    });

    // 1. Idempotently resolve or create the Supabase Auth user
    let supabaseUserId: string | null = null;

    const existingPublicUser = await supabaseAdmin
      .from("users")
      .select("uid")
      .eq("email", identity.email)
      .maybeSingle();

    if (existingPublicUser.data?.uid) {
      supabaseUserId = existingPublicUser.data.uid;
    } else {
      const created = await supabaseAdmin.auth.admin.createUser({
        email: identity.email,
        email_confirm: true,
        user_metadata: { firebase_uid: identity.firebaseUid },
        app_metadata: { provider: "firebase", providers: ["firebase"] },
      });

      if (created.data?.user?.id) {
        supabaseUserId = created.data.user.id;
      } else if (isUniqueViolation(created.error)) {
        // Concurrent request already created the user; re-read instead of failing
        console.warn("[verify-firebase-token] CONCURRENT_AUTH_CREATE_RECOVER: re-reading existing user");
        const reread = await supabaseAdmin
          .from("users")
          .select("uid")
          .eq("email", identity.email)
          .maybeSingle();
        supabaseUserId = reread.data?.uid ?? null;
      } else if (created.error) {
        console.error(
          "[verify-firebase-token] ERR_CREATE_AUTH_USER:",
          created.error.status ?? "unknown",
          created.error.message,
        );
      }
    }

    // 2. Mint a Supabase session via generateLink + verifyOtp (idempotent even when user already exists)
    let sessionData: {
      access_token: string;
      refresh_token: string;
      expires_in?: number;
      user_id: string;
    } | null = null;

    let lastSessionBranch = "ERR_MINT_SESSION";
    for (let attempt = 0; attempt < 2; attempt++) {
      const linkRes = await supabaseAdmin.auth.admin.generateLink({
        type: "magiclink",
        email: identity.email,
      });

      if (linkRes.error || !linkRes.data?.properties?.hashed_token) {
        lastSessionBranch = "ERR_GENERATE_LINK";
        console.error(
          "[verify-firebase-token] ERR_GENERATE_LINK:",
          linkRes.error?.message || "missing_hashed_token",
        );
        continue;
      }

      if (!supabaseUserId && linkRes.data.user?.id) {
        supabaseUserId = linkRes.data.user.id;
      }

      const otpRes = await supabaseAnon.auth.verifyOtp({
        token_hash: linkRes.data.properties.hashed_token,
        type: "magiclink",
      });

      if (!otpRes.error && otpRes.data?.session?.access_token) {
        const verifiedUid = otpRes.data.user?.id || supabaseUserId || "";
        sessionData = {
          access_token: otpRes.data.session.access_token,
          refresh_token: otpRes.data.session.refresh_token,
          expires_in: otpRes.data.session.expires_in,
          user_id: verifiedUid,
        };
        supabaseUserId = verifiedUid;
        break;
      }

      lastSessionBranch = "ERR_VERIFY_OTP";
      console.error(
        "[verify-firebase-token] ERR_VERIFY_OTP:",
        otpRes.error?.message || "missing_session",
      );
    }

    if (!sessionData || !supabaseUserId) {
      console.error(`[verify-firebase-token] FAILED_SESSION_MINT branch=${lastSessionBranch}`);
      return jsonResponse(
        { error_code: lastSessionBranch, error: "Could not create session" },
        500,
      );
    }

    // 3. Idempotently ensure public.users row exists (ignore unique violation 23505 on races)
    const { data: existingRow, error: readUserErr } = await supabaseAdmin
      .from("users")
      .select("uid")
      .eq("uid", supabaseUserId)
      .maybeSingle();

    if (readUserErr) {
      console.error("[verify-firebase-token] ERR_READ_PUBLIC_USER:", readUserErr.code, readUserErr.message);
      return jsonResponse(
        { error_code: "ERR_READ_PUBLIC_USER", error: "Could not create session" },
        500,
      );
    }

    if (!existingRow) {
      const { error: upsertErr } = await supabaseAdmin
        .from("users")
        .upsert(
          {
            uid: supabaseUserId,
            email: identity.email,
            custom_email_verified: identity.emailVerified,
          },
          { onConflict: "uid", ignoreDuplicates: true },
        );

      if (upsertErr && !isUniqueViolation(upsertErr)) {
        console.error("[verify-firebase-token] ERR_UPSERT_PUBLIC_USER:", upsertErr.code, upsertErr.message);
        return jsonResponse(
          { error_code: "ERR_UPSERT_PUBLIC_USER", error: "Could not create session" },
          500,
        );
      }

      if (upsertErr && isUniqueViolation(upsertErr)) {
        // Re-read on concurrent duplicate key race instead of failing
        const { error: rereadErr } = await supabaseAdmin
          .from("users")
          .select("uid")
          .eq("uid", supabaseUserId)
          .maybeSingle();
        if (rereadErr) {
          console.error("[verify-firebase-token] ERR_REREAD_PUBLIC_USER:", rereadErr.code, rereadErr.message);
          return jsonResponse(
            { error_code: "ERR_REREAD_PUBLIC_USER", error: "Could not create session" },
            500,
          );
        }
      }
    }

    return jsonResponse(
      {
        access_token: sessionData.access_token,
        refresh_token: sessionData.refresh_token,
        token_type: "bearer",
        expires_in: sessionData.expires_in ?? 3600,
        user_id: supabaseUserId,
        user: {
          id: supabaseUserId,
          email: identity.email,
        },
      },
      200,
    );
  } catch (error) {
    console.error("[verify-firebase-token] ERR_UNHANDLED:", (error as Error)?.message || "unknown");
    return jsonResponse(
      { error_code: "ERR_UNHANDLED", error: "Internal server error" },
      500,
    );
  }
});

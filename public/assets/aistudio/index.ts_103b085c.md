import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "jsr:@supabase/supabase-js@2";

// Generates a signed Cloudinary upload authorization so the Android app can upload Moments
// media directly to Cloudinary without ever holding the Cloudinary API secret.
// 2026-11-13: added a one-time credentials check (ping) per cold start that only writes to the
// function logs (never changes the response). Look for "cloudinary_ping" in the function logs.

const SUPABASE_URL = Deno.env.get("SUPABASE_URL")!;
const SUPABASE_ANON_KEY = Deno.env.get("SUPABASE_ANON_KEY")!;
const SUPABASE_SERVICE_ROLE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;

const CLOUDINARY_CLOUD_NAME = Deno.env.get("CLOUDINARY_CLOUD_NAME") ?? "";
const CLOUDINARY_API_KEY = Deno.env.get("CLOUDINARY_API_KEY") ?? "";
const CLOUDINARY_API_SECRET = Deno.env.get("CLOUDINARY_API_SECRET") ?? "";

const CORS_HEADERS: Record<string, string> = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

function json(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...CORS_HEADERS, "Content-Type": "application/json" },
  });
}

async function sha1Hex(input: string): Promise<string> {
  const bytes = new TextEncoder().encode(input);
  const digest = await crypto.subtle.digest("SHA-1", bytes);
  return Array.from(new Uint8Array(digest))
    .map((b) => b.toString(16).padStart(2, "0"))
    .join("");
}

let credsChecked = false;
async function checkCredentials(): Promise<void> {
  if (credsChecked) return;
  credsChecked = true;
  try {
    const r = await fetch(`https://api.cloudinary.com/v1_1/${CLOUDINARY_CLOUD_NAME}/ping`, {
      headers: { Authorization: "Basic " + btoa(`${CLOUDINARY_API_KEY}:${CLOUDINARY_API_SECRET}`) },
      signal: AbortSignal.timeout(5000),
    });
    const body = (await r.text()).slice(0, 200);
    console.log(
      `cloudinary_ping status=${r.status} body=${body} cloud=${CLOUDINARY_CLOUD_NAME} ` +
        `key_len=${CLOUDINARY_API_KEY.length} secret_len=${CLOUDINARY_API_SECRET.length}`,
    );
  } catch (e) {
    console.error("cloudinary_ping_failed", String(e));
  }
}

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: CORS_HEADERS });
  if (req.method !== "POST") return json({ error: "method_not_allowed" }, 405);

  const authHeader = req.headers.get("Authorization") ?? "";
  const userClient = createClient(SUPABASE_URL, SUPABASE_ANON_KEY, {
    global: { headers: { Authorization: authHeader } },
  });
  const { data: userData, error: userErr } = await userClient.auth.getUser();
  if (userErr || !userData?.user) {
    return json({ error: "unauthenticated" }, 401);
  }
  const userId = userData.user.id;

  const admin = createClient(SUPABASE_URL, SUPABASE_SERVICE_ROLE_KEY);

  // A suspended user can't post new Moments content either.
  const { data: activeSuspension } = await admin
    .from("user_moderation_status")
    .select("action_type, expires_at")
    .eq("user_id", userId)
    .eq("action_type", "suspend")
    .gt("expires_at", new Date().toISOString())
    .maybeSingle();
  if (activeSuspension) {
    return json({ error: "user_suspended", expires_at: activeSuspension.expires_at }, 403);
  }

  if (!CLOUDINARY_CLOUD_NAME || !CLOUDINARY_API_KEY || !CLOUDINARY_API_SECRET) {
    return json({ error: "cloudinary_not_configured" }, 500);
  }

  // @ts-ignore EdgeRuntime is provided by the Supabase edge runtime
  EdgeRuntime.waitUntil(checkCredentials());

  const timestamp = Math.floor(Date.now() / 1000);
  const folder = `moments/${userId}`;

  // Params to sign, alphabetically by key, joined as key=value&key=value, secret appended raw.
  const stringToSign = `folder=${folder}&timestamp=${timestamp}${CLOUDINARY_API_SECRET}`;
  const signature = await sha1Hex(stringToSign);

  return json({
    signature,
    timestamp,
    api_key: CLOUDINARY_API_KEY,
    cloud_name: CLOUDINARY_CLOUD_NAME,
    folder,
  });
});

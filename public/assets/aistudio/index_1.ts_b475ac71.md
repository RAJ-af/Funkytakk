import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "jsr:@supabase/supabase-js@2";

// FunkyTalk staff push (FCM HTTP v1) for the report workflow.
// Called only by the database (private.notify_staff -> private.call_edge) with the Vault secret in x-push-secret.
// Pushes carry no user content: only an event name, a short title/body written by the database and the report id.
// Needs the Edge secret FCM_SERVICE_ACCOUNT_JSON (same as send-push).

const SA_JSON = Deno.env.get("FCM_SERVICE_ACCOUNT_JSON");
const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
const EVENTS = new Set([
  "new_report", "report_reopened", "report_open_again", "reopen_challenged", "owner_decision", "report_freed",
]);

function b64url(data: ArrayBuffer | Uint8Array): string {
  const bytes = data instanceof Uint8Array ? data : new Uint8Array(data);
  let s = "";
  for (let i = 0; i < bytes.length; i++) s += String.fromCharCode(bytes[i]);
  return btoa(s).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

function pemToDer(pem: string): Uint8Array {
  const body = pem.replace(/-----BEGIN PRIVATE KEY-----/, "").replace(/-----END PRIVATE KEY-----/, "").replace(/\s+/g, "");
  const raw = atob(body);
  const out = new Uint8Array(raw.length);
  for (let i = 0; i < raw.length; i++) out[i] = raw.charCodeAt(i);
  return out;
}

let cached: { token: string; exp: number } | null = null;

// deno-lint-ignore no-explicit-any
async function getAccessToken(sa: any): Promise<string> {
  const now = Math.floor(Date.now() / 1000);
  if (cached && cached.exp - 60 > now) return cached.token;
  const enc = new TextEncoder();
  const header = { alg: "RS256", typ: "JWT" };
  const claim = {
    iss: sa.client_email,
    scope: "https://www.googleapis.com/auth/firebase.messaging",
    aud: "https://oauth2.googleapis.com/token",
    iat: now,
    exp: now + 3600,
  };
  const unsigned = `${b64url(enc.encode(JSON.stringify(header)))}.${b64url(enc.encode(JSON.stringify(claim)))}`;
  const key = await crypto.subtle.importKey(
    "pkcs8",
    pemToDer(sa.private_key),
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["sign"],
  );
  const sig = await crypto.subtle.sign("RSASSA-PKCS1-v1_5", key, enc.encode(unsigned));
  const res = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion: `${unsigned}.${b64url(sig)}`,
    }),
  });
  if (!res.ok) throw new Error(`oauth_${res.status}`);
  const j = await res.json();
  cached = { token: j.access_token, exp: now + (j.expires_in ?? 3600) };
  return cached.token;
}

const json = (o: unknown, status = 200) =>
  new Response(JSON.stringify(o), { status, headers: { "Content-Type": "application/json" } });

Deno.serve(async (req: Request) => {
  if (req.method !== "POST") return json({ error: "method_not_allowed" }, 405);

  const admin = createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!, {
    auth: { persistSession: false },
  });

  const provided = req.headers.get("x-push-secret") ?? "";
  if (!provided) return json({ error: "forbidden" }, 403);
  const { data: okSecret } = await admin.rpc("verify_push_secret", { p_secret: provided });
  if (okSecret !== true) return json({ error: "forbidden" }, 403);
  if (!SA_JSON) return json({ error: "not_configured_fcm_json" }, 503);

  // deno-lint-ignore no-explicit-any
  let ev: any;
  try {
    ev = await req.json();
  } catch {
    return json({ error: "bad_request" }, 400);
  }
  const ids: string[] = Array.isArray(ev.recipient_ids) ? ev.recipient_ids : [];
  if (!EVENTS.has(ev.event) || ids.length === 0 || ids.length > 50 || !ids.every((i) => UUID.test(String(i)))) {
    return json({ error: "bad_request" }, 400);
  }
  const reportId = UUID.test(String(ev.report_id ?? "")) ? String(ev.report_id) : "";
  const title = String(ev.title ?? "Report").slice(0, 80);
  const body = String(ev.body ?? "").slice(0, 160);

  const { data: tokens } = await admin
    .from("push_tokens")
    .select("token, user_id")
    .in("user_id", ids)
    .order("updated_at", { ascending: false })
    .limit(250);
  if (!tokens || tokens.length === 0) return json({ result: "no_tokens" });

  // at most 5 tokens per person
  const perUser = new Map<string, number>();
  const send = tokens.filter((t: { user_id: string }) => {
    const n = (perUser.get(t.user_id) ?? 0) + 1;
    perUser.set(t.user_id, n);
    return n <= 5;
  });

  // deno-lint-ignore no-explicit-any
  let sa: any;
  let accessToken: string;
  try {
    sa = JSON.parse(SA_JSON);
    accessToken = await getAccessToken(sa);
  } catch (_e) {
    console.error("fcm auth failed");
    return json({ error: "fcm_auth_failed" }, 502);
  }

  let sent = 0;
  const failures: string[] = [];
  for (const { token } of send) {
    const res = await fetch(`https://fcm.googleapis.com/v1/projects/${sa.project_id}/messages:send`, {
      method: "POST",
      headers: { Authorization: `Bearer ${accessToken}`, "Content-Type": "application/json" },
      body: JSON.stringify({
        message: {
          token,
          notification: { title, body },
          data: { kind: "staff_report", event: String(ev.event), report_id: reportId },
          android: { priority: "HIGH", notification: { channel_id: "staff_reports", tag: reportId || String(ev.event) } },
        },
      }),
    });
    if (res.ok) {
      sent++;
      continue;
    }
    let code = "";
    try {
      const j = await res.json();
      const details = Array.isArray(j?.error?.details) ? j.error.details : [];
      code = String(details.find((d: { errorCode?: string }) => d?.errorCode)?.errorCode ?? j?.error?.status ?? "");
    } catch (_e) { /* non-JSON error body */ }
    console.error(`fcm status ${res.status} code=${code}`);
    failures.push(`${res.status}:${code}`);
    if (code === "UNREGISTERED") await admin.from("push_tokens").delete().eq("token", token);
  }
  return json({ sent, failed: failures });
});

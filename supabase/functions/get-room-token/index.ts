import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "jsr:@supabase/supabase-js@2";
import { RtcTokenBuilder, RtcRole } from "npm:agora-token@2.0.2";

const SUPABASE_URL = Deno.env.get("SUPABASE_URL")!;
const SUPABASE_ANON_KEY = Deno.env.get("SUPABASE_ANON_KEY")!;
const SUPABASE_SERVICE_ROLE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;

const AGORA_APP_ID = Deno.env.get("AGORA_APP_ID") ?? "";
const AGORA_APP_CERTIFICATE = Deno.env.get("AGORA_APP_CERTIFICATE") ?? "";
const ZEGO_APP_ID = Deno.env.get("ZEGO_APP_ID") ?? "";
const ZEGO_SERVER_SECRET = Deno.env.get("ZEGO_SERVER_SECRET") ?? "";
const DAILY_API_KEY = Deno.env.get("DAILY_API_KEY") ?? "";
const DAILY_DOMAIN = Deno.env.get("DAILY_DOMAIN") ?? "";

const TOKEN_TTL_SECONDS = 3600;

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

async function generateZegoToken04(
  appId: number,
  userId: string,
  secret: string,
  effectiveTimeInSeconds: number,
  payload: string,
): Promise<string> {
  if (![16, 24, 32].includes(secret.length)) {
    throw new Error("ZEGO_SERVER_SECRET must be 16, 24, or 32 bytes long");
  }
  const createTime = Math.floor(Date.now() / 1000);
  const expire = createTime + effectiveTimeInSeconds;

  const tokenInfo = {
    app_id: appId,
    user_id: userId,
    nonce: Math.floor(Math.random() * 2147483647),
    ctime: createTime,
    expire,
    payload: payload ?? "",
  };
  const plaintext = new TextEncoder().encode(JSON.stringify(tokenInfo));

  const iv = crypto.getRandomValues(new Uint8Array(16));
  const cryptoKey = await crypto.subtle.importKey(
    "raw",
    new TextEncoder().encode(secret),
    { name: "AES-CBC" },
    false,
    ["encrypt"],
  );
  const cipherBuf = await crypto.subtle.encrypt({ name: "AES-CBC", iv }, cryptoKey, plaintext);
  const cipher = new Uint8Array(cipherBuf);

  const buf = new Uint8Array(8 + 2 + iv.length + 2 + cipher.length);
  const view = new DataView(buf.buffer);
  view.setBigInt64(0, BigInt(expire), false);
  view.setUint16(8, iv.length, false);
  buf.set(iv, 10);
  view.setUint16(10 + iv.length, cipher.length, false);
  buf.set(cipher, 12 + iv.length);

  let binary = "";
  for (const b of buf) binary += String.fromCharCode(b);
  return "04" + btoa(binary);
}

/**
 * Daily.co requires a room to exist (created via POST /v1/rooms) before a meeting token can be
 * issued for it, otherwise /v1/meeting-tokens returns 404 "Room Not Found". Get-or-create.
 */
async function ensureDailyRoomExists(roomName: string): Promise<{ ok: true } | { ok: false; detail: string }> {
  const getRes = await fetch(`https://api.daily.co/v1/rooms/${roomName}`, {
    headers: { Authorization: `Bearer ${DAILY_API_KEY}` },
  });
  if (getRes.ok) return { ok: true };
  if (getRes.status !== 404) {
    const detail = await getRes.text();
    return { ok: false, detail };
  }

  const nowSec = Math.floor(Date.now() / 1000);
  const createRes = await fetch("https://api.daily.co/v1/rooms", {
    method: "POST",
    headers: {
      Authorization: `Bearer ${DAILY_API_KEY}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      name: roomName,
      properties: {
        exp: nowSec + 6 * 3600,
        enable_chat: false,
      },
    }),
  });
  if (!createRes.ok) {
    const detail = await createRes.text();
    return { ok: false, detail };
  }
  return { ok: true };
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

  let body: { room_id?: string };
  try {
    body = await req.json();
  } catch {
    return json({ error: "invalid_json_body" }, 400);
  }
  const roomId = body.room_id;
  if (!roomId) return json({ error: "room_id_required" }, 400);

  const admin = createClient(SUPABASE_URL, SUPABASE_SERVICE_ROLE_KEY);

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

  // The speaking permission is decided HERE, on the server. The client-supplied "role" is ignored.
  // Host (voice_rooms.host_id) or a participant row with role host/speaker => may publish audio.
  // Everyone else => listen-only token.
  const { data: room } = await admin
    .from("voice_rooms")
    .select("host_id, is_live, status")
    .eq("id", roomId)
    .maybeSingle();
  if (!room) return json({ error: "room_not_found" }, 404);
  if (!room.is_live || room.status !== "active") return json({ error: "room_ended" }, 410);

  let canPublish = room.host_id === userId;
  if (!canPublish) {
    const { data: participant } = await admin
      .from("room_participants")
      .select("role")
      .eq("room_id", roomId)
      .eq("user_id", userId)
      .maybeSingle();
    canPublish = participant?.role === "host" || participant?.role === "speaker";
  }
  const isHost = room.host_id === userId;

  const { data: provider, error: assignErr } = await admin.rpc(
    "assign_rtc_provider_for_room",
    { p_room_id: roomId },
  );
  if (assignErr) {
    const msg = assignErr.message ?? "provider_assignment_failed";
    const status = msg.includes("room_not_found") ? 404 : msg.includes("all_providers_at_limit") ? 503 : 500;
    return json({ error: msg }, status);
  }

  const roleInfo = { role: canPublish ? "publisher" : "audience", can_publish: canPublish };

  try {
    const nowSec = Math.floor(Date.now() / 1000);

    if (provider === "agora") {
      const role = canPublish ? RtcRole.PUBLISHER : RtcRole.SUBSCRIBER;
      const token = RtcTokenBuilder.buildTokenWithUserAccount(
        AGORA_APP_ID,
        AGORA_APP_CERTIFICATE,
        roomId,
        userId,
        role,
        // agora-token v2 expects DURATIONS in seconds (not absolute timestamps). Passing "now + ttl" made
        // the token valid for decades, so a demoted speaker kept the right to publish.
        TOKEN_TTL_SECONDS,
        TOKEN_TTL_SECONDS,
      );
      return json({ provider: "AGORA", room_id: roomId, token, app_id: AGORA_APP_ID, ...roleInfo });
    }

    if (provider === "zego") {
      const payload = JSON.stringify({
        room_id: roomId,
        privilege: { 1: 1, 2: canPublish ? 1 : 0 },
        stream_id_list: null,
      });
      const token = await generateZegoToken04(
        Number(ZEGO_APP_ID),
        userId,
        ZEGO_SERVER_SECRET,
        TOKEN_TTL_SECONDS,
        payload,
      );
      return json({ provider: "ZEGO", room_id: roomId, token, app_id: ZEGO_APP_ID, ...roleInfo });
    }

    if (provider === "daily") {
      const roomCheck = await ensureDailyRoomExists(roomId);
      if (!roomCheck.ok) {
        return json({ error: "daily_room_setup_failed", detail: roomCheck.detail }, 502);
      }

      const res = await fetch("https://api.daily.co/v1/meeting-tokens", {
        method: "POST",
        headers: {
          Authorization: `Bearer ${DAILY_API_KEY}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          properties: {
            room_name: roomId,
            user_id: userId,
            is_owner: isHost,
            // UNTESTED: listen-only participants must not send audio/video.
            permissions: { canSend: canPublish ? ["audio"] : false },
            exp: nowSec + TOKEN_TTL_SECONDS,
          },
        }),
      });
      if (!res.ok) {
        const errText = await res.text();
        return json({ error: "daily_token_failed", detail: errText }, 502);
      }
      const dailyData = await res.json();
      return json({ provider: "DAILY", room_id: roomId, token: dailyData.token, app_id: DAILY_DOMAIN, ...roleInfo });
    }

    return json({ error: "unknown_provider", provider }, 500);
  } catch (e) {
    return json({ error: "token_generation_failed", detail: String(e) }, 500);
  }
});

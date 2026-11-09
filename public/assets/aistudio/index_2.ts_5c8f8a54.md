import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "jsr:@supabase/supabase-js@2";

// FunkyTalk: emails the owner when an admin challenges a reopened report (both briefs, no user content, no evidence).
// Called only by the database (admin_challenge_reopen -> private.call_edge) with the Vault secret in x-push-secret.
// Needs Edge secrets: BREVO_API_KEY (already set) and OWNER_EMAIL (the owner's inbox).

const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
const json = (o: unknown, status = 200) =>
  new Response(JSON.stringify(o), { status, headers: { "Content-Type": "application/json" } });
const esc = (s: unknown) =>
  String(s ?? "").replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");

Deno.serve(async (req: Request) => {
  if (req.method !== "POST") return json({ error: "method_not_allowed" }, 405);

  const admin = createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!, {
    auth: { persistSession: false },
  });
  const provided = req.headers.get("x-push-secret") ?? "";
  if (!provided) return json({ error: "forbidden" }, 403);
  const { data: okSecret } = await admin.rpc("verify_push_secret", { p_secret: provided });
  if (okSecret !== true) return json({ error: "forbidden" }, 403);

  const brevo = Deno.env.get("BREVO_API_KEY");
  const owner = Deno.env.get("OWNER_EMAIL");
  if (!brevo || !owner) return json({ error: "not_configured" }, 503);

  let reopenId = "";
  try {
    reopenId = String((await req.json()).reopen_id ?? "");
  } catch {
    return json({ error: "bad_request" }, 400);
  }
  if (!UUID.test(reopenId)) return json({ error: "bad_request" }, 400);

  const { data: ro } = await admin.from("report_reopens").select("*").eq("id", reopenId).maybeSingle();
  if (!ro || ro.status === "open") return json({ error: "not_found_or_not_challenged" }, 404);
  const { data: rep } = await admin
    .from("room_reports").select("id, context_type, category, created_at").eq("id", ro.report_id).maybeSingle();

  const nameOf = async (id: string | null) => {
    if (!id) return "(deleted admin)";
    const { data: p } = await admin.from("profiles").select("profile_name, username").eq("auth_user_id", id).maybeSingle();
    return `${p?.profile_name || p?.username || "admin"} (${id})`;
  };
  const reopener = await nameOf(ro.reopened_by);
  const original = await nameOf(ro.original_admin);

  const html = `
<h3>Admin dispute on a report</h3>
<p>Report <b>${esc(ro.report_id)}</b> (${esc(rep?.context_type)} / ${esc(rep?.category)})<br>
Original action: <b>${esc(ro.original_outcome)}</b>${ro.original_duration_hours ? ` for ${esc(ro.original_duration_hours)} h` : ""}</p>
<p><b>Reopened by:</b> ${esc(reopener)}</p>
<blockquote>${esc(ro.reopen_brief).replace(/\n/g, "<br>")}</blockquote>
<p><b>Challenged by (original admin):</b> ${esc(original)}</p>
<blockquote>${esc(ro.challenge_brief).replace(/\n/g, "<br>")}</blockquote>
<p>Decide in the admin panel: <code>owner_decide_dispute('${esc(ro.id)}', 'reopen_upheld' | 'original_upheld', note)</code></p>`;

  const res = await fetch("https://api.brevo.com/v3/smtp/email", {
    method: "POST",
    headers: { "api-key": brevo, "Content-Type": "application/json", accept: "application/json" },
    body: JSON.stringify({
      sender: { name: "FunkyTalk", email: "noreply@funkytalk.jo3.org" },
      to: [{ email: owner }],
      subject: `FunkyTalk: admin dispute on report ${String(ro.report_id).slice(0, 8)}`,
      htmlContent: html,
    }),
  });
  if (!res.ok) {
    console.error(`brevo status ${res.status}`);
    return json({ error: "email_failed", status: res.status }, 502);
  }
  await admin.from("report_reopens").update({ owner_emailed_at: new Date().toISOString() }).eq("id", reopenId);
  return json({ ok: true });
});

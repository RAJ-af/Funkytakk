import { serve } from "https://deno.land/std@0.177.0/http/server.ts"
import { createClient } from "https://esm.sh/@supabase/supabase-js@2"

const ALLOWED_ORIGINS = [
  "https://funkytalk.jo3.org",
  "https://ais-dev-mahnbfoa6eavbwr4735yzy-335946446072.asia-east1.run.app",
  "https://ais-pre-mahnbfoa6eavbwr4735yzy-335946446072.asia-east1.run.app",
  "https://dulpqochkxtamqztauea.supabase.co",
]

function getCorsHeaders(req: Request) {
  const origin = req.headers.get("origin") || ""
  const isAllowed =
    ALLOWED_ORIGINS.includes(origin) ||
    origin.endsWith(".run.app") ||
    origin.endsWith(".supabase.co") ||
    origin.endsWith(".jo3.org") ||
    origin.startsWith("android-app://") ||
    origin.startsWith("funkytalk://")

  return {
    "Access-Control-Allow-Origin": isAllowed ? origin : ALLOWED_ORIGINS[0],
    "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type, prefer",
    "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
    "Vary": "Origin",
  }
}

async function hashToken(token: string): Promise<string> {
  const encoder = new TextEncoder()
  const data = encoder.encode(token)
  const hashBuffer = await crypto.subtle.digest("SHA-256", data)
  return Array.from(new Uint8Array(hashBuffer))
    .map((b) => b.toString(16).padStart(2, "0"))
    .join("")
}

serve(async (req) => {
  const corsHeaders = getCorsHeaders(req)

  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders })
  }

  try {
    const url = new URL(req.url)
    const uid = url.searchParams.get("uid")
    const token = url.searchParams.get("token")

    const supabaseUrl = Deno.env.get("SUPABASE_URL")!
    const supabaseServiceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!
    const supabaseAnonKey = Deno.env.get("SUPABASE_ANON_KEY")!

    // 1. AUTHENTICATION CHECK: Authorization header or auth parameter
    const authHeader = req.headers.get("Authorization") || (url.searchParams.get("auth") ? `Bearer ${url.searchParams.get("auth")}` : null)
    if (!authHeader || !authHeader.toLowerCase().startsWith("bearer ")) {
      return renderResponse(
        req,
        401,
        "Unauthorized",
        "Missing or invalid Authorization credentials.",
        false,
        corsHeaders
      )
    }

    const rawJwt = authHeader.replace(/^bearer\s+/i, "").trim()
    const supabaseAuthClient = createClient(supabaseUrl, supabaseAnonKey)
    const { data: authData, error: authError } = await supabaseAuthClient.auth.getUser(rawJwt)

    const isServiceKey = rawJwt === supabaseServiceKey
    if (authError && !isServiceKey && !authData?.user) {
      return renderResponse(
        req,
        401,
        "Unauthorized",
        "Invalid or expired authorization token.",
        false,
        corsHeaders
      )
    }

    // 2. Validate input parameters
    if (!uid || !token) {
      return renderResponse(
        req,
        400,
        "Invalid Link",
        "We couldn't authenticate this request. Missing security parameters.",
        false,
        corsHeaders
      )
    }

    const supabaseAdmin = createClient(supabaseUrl, supabaseServiceKey)

    // Retrieve user record including token hash and expiry
    const { data: user, error: fetchError } = await supabaseAdmin
      .from("users")
      .select("email_verification_token, email_verification_expires_at, custom_email_verified")
      .eq("uid", uid)
      .maybeSingle()

    if (fetchError || !user) {
      return renderResponse(
        req,
        404,
        "Verification Failed",
        "User account not found or database sync pending.",
        false,
        corsHeaders
      )
    }

    if (user.custom_email_verified) {
      return renderResponse(
        req,
        200,
        "Already Verified ✨",
        "Your email is already verified. You are completely set to log in and use FunkyTalk!",
        true,
        corsHeaders
      )
    }

    // 3. EXPIRY CHECK: Verify email_verification_expires_at
    if (user.email_verification_expires_at) {
      const expiresAt = new Date(user.email_verification_expires_at)
      if (expiresAt.getTime() < Date.now()) {
        return renderResponse(
          req,
          410,
          "Link Expired",
          "This verification link has expired (links are valid for 24 hours). Please open FunkyTalk and request a new verification email.",
          false,
          corsHeaders
        )
      }
    }

    // 4. TOKEN HASH COMPARISON
    const incomingHashedToken = await hashToken(token)
    const tokenMatches =
      user.email_verification_token &&
      (user.email_verification_token === incomingHashedToken ||
        user.email_verification_token === token)

    if (!tokenMatches) {
      return renderResponse(
        req,
        400,
        "Verification Invalid",
        "The verification link is invalid or has already been used. Please check your inbox or click resend in the app.",
        false,
        corsHeaders
      )
    }

    // 5. ONE-TIME USE: Invalidate/clear token upon successful verification
    const { error: updateError } = await supabaseAdmin
      .from("users")
      .update({
        custom_email_verified: true,
        email_verification_token: null,
        email_verification_expires_at: null,
        updated_at: new Date().toISOString(),
      })
      .eq("uid", uid)

    if (updateError) {
      throw new Error(`Database update failed: ${updateError.message}`)
    }

    return renderResponse(
      req,
      200,
      "Verified Successfully 🎉",
      "Your FunkyTalk email has been verified. You can return directly to your application now!",
      true,
      corsHeaders
    )
  } catch (error) {
    return renderResponse(
      req,
      500,
      "Service Error",
      `An unexpected issue occurred: ${error.message}`,
      false,
      corsHeaders
    )
  }
})

function renderResponse(
  req: Request,
  status: number,
  title: string,
  message: string,
  isSuccess: boolean,
  corsHeaders: Record<string, string>
): Response {
  const acceptsHtml = req.headers.get("accept")?.includes("text/html") ?? true

  if (!acceptsHtml && !isSuccess) {
    return new Response(JSON.stringify({ error: title, message }), {
      status,
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    })
  }

  const accentColor = isSuccess ? "#FF9E00" : "#D32F2F"
  const iconHtml = isSuccess
    ? `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="80" height="80"><circle cx="12" cy="12" r="10" fill="#FFF3E0"/><path d="M10 15.17l-3.23-3.23a1 1 0 00-1.42 1.42l4 4a1 1 0 001.42 0l8-8a1 1 0 00-1.42-1.42z" fill="#FF9E00"/></svg>`
    : `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="80" height="80"><circle cx="12" cy="12" r="10" fill="#FFEBEE"/><path d="M13.41 12l4.3-4.29a1 1 0 00-1.42-1.42L12 10.59l-4.29-4.3a1 1 0 00-1.42 1.42L10.59 12l-4.3 4.29a1 1 0 001.42 1.42L12 13.41l4.29 4.3a1 1 0 001.42-1.42z" fill="#D32F2F"/></svg>`

  const html = `<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${title} | FunkyTalk</title>
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;600;700;800&display=swap" rel="stylesheet">
    <style>
        body {
            font-family: 'Plus Jakarta Sans', -apple-system, BlinkMacSystemFont, sans-serif;
            background-color: #FAFAFC;
            margin: 0;
            padding: 0;
            display: flex;
            justify-content: center;
            align-items: center;
            min-height: 100vh;
        }
        .container {
            background-color: #FFFFFF;
            border-radius: 24px;
            box-shadow: 0 10px 30px rgba(0, 0, 0, 0.03);
            padding: 48px 32px;
            text-align: center;
            max-width: 440px;
            width: 80%;
            border: 1px solid #EFEFED;
        }
        .icon {
            margin-bottom: 24px;
            animation: bounceIn 0.8s cubic-bezier(0.175, 0.885, 0.32, 1.275);
        }
        h1 {
            font-size: 24px;
            font-weight: 800;
            color: #1A1A1A;
            margin: 0 0 12px 0;
        }
        p {
            font-size: 15px;
            color: #6B6E7B;
            line-height: 1.6;
            margin: 0 0 32px 0;
        }
        .btn {
            display: inline-block;
            background-color: ${accentColor};
            color: #FFFFFF;
            padding: 16px 36px;
            border-radius: 30px;
            font-size: 15px;
            font-weight: 700;
            text-decoration: none;
            transition: all 0.2s ease;
            box-shadow: 0 6px 20px ${isSuccess ? "rgba(255, 158, 0, 0.2)" : "rgba(211, 47, 47, 0.2)"};
        }
        .btn:hover {
            transform: translateY(-2px);
            opacity: 0.95;
        }
        .footer {
            margin-top: 40px;
            font-size: 11px;
            color: #B5B7C0;
        }
        @keyframes bounceIn {
            0% { transform: scale(0.3); opacity: 0; }
            50% { transform: scale(1.1); }
            70% { transform: scale(0.9); }
            100% { transform: scale(1); opacity: 1; }
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="icon">${iconHtml}</div>
        <h1>${title}</h1>
        <p>${message}</p>
        <a href="#" class="btn" onclick="window.close(); return false;">Done</a>
        <div class="footer">FunkyTalk Authentic Verification System</div>
    </div>
</body>
</html>`

  return new Response(html, {
    status,
    headers: { ...corsHeaders, "Content-Type": "text/html" },
  })
}


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
    "Access-Control-Allow-Methods": "POST, OPTIONS",
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

async function checkRateLimit(
  supabase: any,
  key: string,
  action: string,
  maxRequests = 5,
  windowSeconds = 3600
): Promise<{ allowed: boolean; remaining: number }> {
  const windowStart = new Date(Date.now() - windowSeconds * 1000).toISOString()

  const { count, error } = await supabase
    .from("rate_limits")
    .select("*", { count: "exact", head: true })
    .eq("key", key.toLowerCase())
    .eq("action", action)
    .gte("created_at", windowStart)

  if (!error && (count ?? 0) >= maxRequests) {
    return { allowed: false, remaining: 0 }
  }

  await supabase.from("rate_limits").insert({
    key: key.toLowerCase(),
    action: action,
    created_at: new Date().toISOString(),
  })

  return { allowed: true, remaining: maxRequests - ((count ?? 0) + 1) }
}

serve(async (req) => {
  const corsHeaders = getCorsHeaders(req)

  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders })
  }

  try {
    const supabaseUrl = Deno.env.get("SUPABASE_URL")!
    const supabaseServiceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!
    const supabaseAnonKey = Deno.env.get("SUPABASE_ANON_KEY")!

    // 1. AUTHENTICATION CHECK: Verify Supabase JWT token
    const authHeader = req.headers.get("Authorization")
    if (!authHeader || !authHeader.toLowerCase().startsWith("bearer ")) {
      return new Response(
        JSON.stringify({ error: "Unauthorized: Missing or invalid Authorization header" }),
        {
          status: 401,
          headers: { ...corsHeaders, "Content-Type": "application/json" },
        }
      )
    }

    const rawJwt = authHeader.replace(/^bearer\s+/i, "").trim()
    const supabaseAuthClient = createClient(supabaseUrl, supabaseAnonKey)
    const { data: authData, error: authError } = await supabaseAuthClient.auth.getUser(rawJwt)

    // Accept valid user JWT or service key for system-level calls
    const isServiceKey = rawJwt === supabaseServiceKey
    if (authError && !isServiceKey && !authData?.user) {
      return new Response(
        JSON.stringify({ error: "Unauthorized: " + (authError?.message || "Invalid token") }),
        {
          status: 401,
          headers: { ...corsHeaders, "Content-Type": "application/json" },
        }
      )
    }

    const { uid, email, displayName, token } = await req.json()

    if (!uid || !email || !token) {
      return new Response(JSON.stringify({ error: "Missing required parameters" }), {
        status: 400,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      })
    }

    const supabaseAdmin = createClient(supabaseUrl, supabaseServiceKey)

    // 2. RATE LIMITING: Max 5 verification emails per hour per email/user
    const rateLimit = await checkRateLimit(supabaseAdmin, email, "send-verification-email", 5, 3600)
    if (!rateLimit.allowed) {
      return new Response(
        JSON.stringify({
          error: "Rate limit exceeded. You can only request up to 5 verification emails per hour. Please try again later.",
        }),
        {
          status: 429,
          headers: {
            ...corsHeaders,
            "Content-Type": "application/json",
            "Retry-After": "3600",
          },
        }
      )
    }

    const brevoApiKey = Deno.env.get("BREVO_API_KEY")
    if (!brevoApiKey) {
      return new Response(
        JSON.stringify({ error: "BREVO_API_KEY environment variable is not configured in Supabase Edge Secrets!" }),
        {
          status: 500,
          headers: { ...corsHeaders, "Content-Type": "application/json" },
        }
      )
    }

    // 3. SECURE TOKEN HASHING & EXPIRY (Expires in 24 hours)
    const hashedToken = await hashToken(token)
    const expiresAt = new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString()

    // Store HASHED token and expiry in users table
    const { error: dbUpdateError } = await supabaseAdmin
      .from("users")
      .update({
        email_verification_token: hashedToken,
        email_verification_expires_at: expiresAt,
        custom_email_verified: false,
      })
      .eq("uid", uid)

    if (dbUpdateError) {
      console.warn("Could not pre-update token in users table:", dbUpdateError.message)
    }

    // Secure verification link pointing to verify-email with raw token and auth parameter
    const verificationLink = `${supabaseUrl}/functions/v1/verify-email?uid=${encodeURIComponent(uid)}&token=${encodeURIComponent(token)}&auth=${encodeURIComponent(rawJwt)}`

    // High-alert HTML email template
    const htmlEmailBody = `
    <!DOCTYPE html>
    <html>
    <head>
      <meta charset="utf-8">
      <title>Verify Your Email | FunkyTalk</title>
      <style>
        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #f6f8fa; margin: 0; padding: 20px; }
        .wrapper { background-color: #ffffff; border-radius: 12px; max-width: 500px; margin: 0 auto; padding: 32px; border: 1px solid #e1e4e8; box-shadow: 0 4px 12px rgba(0,0,0,0.05); }
        .logo { font-size: 24px; font-weight: 800; color: #ff9e00; text-align: center; margin-bottom: 24px; letter-spacing: -0.5px; }
        h2 { font-size: 18px; color: #24292e; margin-bottom: 16px; font-weight: 700; text-align: center; }
        p { font-size: 14px; color: #586069; line-height: 1.6; margin: 0 0 16px 0; text-align: left; }
        .btn-container { text-align: center; margin: 28px 0; }
        .button-link { display: inline-block; background-color: #ff9e00; color: white !important; font-weight: bold; font-size: 15px; padding: 14px 28px; text-decoration: none; border-radius: 24px; box-shadow: 0 4px 8px rgba(255, 158, 0, 0.25); }
        .footer { font-size: 11px; color: #959da5; text-align: center; margin-top: 32px; line-height: 1.5; border-top: 1px solid #eeeeee; padding-top: 20px; }
        .warning-text { font-size: 12px; color: #888888; margin-top: 20px; }
      </style>
    </head>
    <body>
      <div class="wrapper">
        <div class="logo">FunkyTalk ✨</div>
        <h2>Verify Your Email Address</h2>
        <p>Hi <strong>${displayName || "FunkyTalk Learner"}</strong>,</p>
        <p>Thank you for signing up on FunkyTalk! To complete your registration and activate your language learning ecosystem, please verify your email address by clicking the button below (valid for 24 hours):</p>
        
        <div class="btn-container">
          <a href="${verificationLink}" class="button-link" target="_blank">Verify My Email ✨</a>
        </div>

        <p class="warning-text">If the button above does not work, copy and paste this URL into your browser:</p>
        <p style="word-break: break-all; font-size: 11px; color: #0366d6;"><a href="${verificationLink}">${verificationLink}</a></p>
        
        <div class="footer">
          If you did not sign up for a FunkyTalk account, you can safely ignore this email.<br><br>
          © 2026 FunkyTalk Ecosystem. All rights reserved.
        </div>
      </div>
    </body>
    </html>
    `

    const brevoResponse = await fetch("https://api.brevo.com/v3/smtp/email", {
      method: "POST",
      headers: {
        "api-key": brevoApiKey,
        "content-type": "application/json",
      },
      body: JSON.stringify({
        sender: {
          name: "FunkyTalk ✨",
          email: "noreply@funkytalk.jo3.org",
        },
        to: [
          {
            email: email,
            name: displayName || "FunkyTalk Learner",
          },
        ],
        subject: "✨ Confirm Your FunkyTalk Email Address!",
        htmlContent: htmlEmailBody,
      }),
    })

    if (!brevoResponse.ok) {
      const errorText = await brevoResponse.text()
      throw new Error(`Brevo API status ${brevoResponse.status}: ${errorText}`)
    }

    return new Response(
      JSON.stringify({ success: true, message: "Verification link dispatched securely!" }),
      {
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      }
    )
  } catch (error) {
    return new Response(JSON.stringify({ error: error.message }), {
      status: 500,
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    })
  }
})


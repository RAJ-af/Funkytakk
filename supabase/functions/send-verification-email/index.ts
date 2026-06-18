import { serve } from "https://deno.land/std@0.177.0/http/server.ts"

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
}

serve(async (req) => {
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders })
  }

  try {
    const { uid, email, displayName, token } = await req.json()

    if (!uid || !email || !token) {
      return new Response(JSON.stringify({ error: "Missing required parameters" }), {
        status: 400,
        headers: { ...corsHeaders, 'Content-Type': 'application/json' }
      })
    }

    const brevoApiKey = Deno.env.get("BREVO_API_KEY")
    const brevoTemplateIdStr = Deno.env.get("BREVO_TEMPLATE_ID") || "4"
    const supabaseUrl = Deno.env.get("SUPABASE_URL")

    if (!brevoApiKey) {
      return new Response(JSON.stringify({ error: "BREVO_API_KEY environment variable is not configured in Supabase Edge Secrets!" }), {
        status: 500,
        headers: { ...corsHeaders, 'Content-Type': 'application/json' }
      })
    }

    // Secure verification link pointing to our matching verify-email Edge Function
    const verificationLink = `${supabaseUrl}/functions/v1/verify-email?uid=${uid}&token=${token}`

    // Make an elegant, high-alert HTML email for verification without requiring any Brevo templates!
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
        <p>Thank you for signing up on FunkyTalk! To complete your registration and activate your language learning ecosystem, please verify your email address by clicking the button below:</p>
        
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
        "content-type": "application/json"
      },
      body: JSON.stringify({
        sender: {
          name: "FunkyTalk ✨",
          email: "noreply@funkytalk.jo3.org"
        },
        to: [
          {
            email: email,
            name: displayName || "FunkyTalk Learner"
          }
        ],
        subject: "✨ Confirm Your FunkyTalk Email Address!",
        htmlContent: htmlEmailBody
      })
    })

    if (!brevoResponse.ok) {
      const errorText = await brevoResponse.text()
      throw new Error(`Brevo API status ${brevoResponse.status}: ${errorText}`)
    }

    return new Response(JSON.stringify({ success: true, message: "Verification link dispatched successfully!" }), {
      headers: { ...corsHeaders, 'Content-Type': 'application/json' }
    })

  } catch (error) {
    return new Response(JSON.stringify({ error: error.message }), {
      status: 500,
      headers: { ...corsHeaders, 'Content-Type': 'application/json' }
    })
  }
})

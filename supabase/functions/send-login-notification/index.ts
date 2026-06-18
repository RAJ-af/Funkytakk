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
    const { email, displayName, deviceType, timestamp } = await req.json()

    if (!email) {
      return new Response(JSON.stringify({ error: "Missing required parameter: email" }), {
        status: 400,
        headers: { ...corsHeaders, 'Content-Type': 'application/json' }
      })
    }

    const brevoApiKey = Deno.env.get("BREVO_API_KEY")
    if (!brevoApiKey) {
      return new Response(JSON.stringify({ error: "BREVO_API_KEY environment variable is not configured in Supabase Edge Secrets!" }), {
        status: 500,
        headers: { ...corsHeaders, 'Content-Type': 'application/json' }
      })
    }

    const nameToUse = displayName || "FunkyTalk Learner"
    const deviceToUse = deviceType || "Android App Session"
    const timeToUse = timestamp || new Date().toLocaleString("en-US", { timeZone: "UTC" }) + " (UTC)"

    // Make an elegant, high-alert alert HTML email matching Instagram's notification card style
    const htmlEmailBody = `
    <!DOCTYPE html>
    <html>
    <head>
      <meta charset="utf-8">
      <title>New Login Alert | FunkyTalk</title>
      <style>
        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #f6f8fa; margin: 0; padding: 20px; }
        .wrapper { background-color: #ffffff; border-radius: 12px; max-width: 500px; margin: 0 auto; padding: 32px; border: 1px solid #e1e4e8; box-shadow: 0 4px 12px rgba(0,0,0,0.05); }
        .logo { font-size: 24px; font-weight: 800; color: #ff9e00; text-align: center; margin-bottom: 24px; letter-spacing: -0.5px; }
        h2 { font-size: 18px; color: #24292e; margin-bottom: 16px; font-weight: 700; }
        p { font-size: 14px; color: #586069; line-height: 1.6; margin: 0 0 16px 0; }
        .alert-box { background-color: #fffaf0; border-left: 4px solid #ff9e00; padding: 16px; border-radius: 6px; margin: 20px 0; }
        .info-row { display: flex; margin-bottom: 8px; font-size: 13px; }
        .info-label { font-weight: bold; color: #24292e; width: 100px; flex-shrink: 0; }
        .info-val { color: #586069; }
        .footer { font-size: 11px; color: #959da5; text-align: center; margin-top: 32px; line-height: 1.5; }
        .button-link { display: inline-block; background-color: #ff9e00; color: white !important; font-weight: bold; font-size: 14px; padding: 12px 24px; text-decoration: none; border-radius: 20px; margin-top: 12px; }
      </style>
    </head>
    <body>
      <div class="wrapper">
        <div class="logo">FunkyTalk ✨</div>
        <h2>New Login Detected!</h2>
        <p>Hi <strong>${nameToUse}</strong>,</p>
        <p>To keep your FunkyTalk account secure, we want to let you know that we noticed a successful login activity from a new device session.</p>
        
        <div class="alert-box">
          <div class="info-row">
            <span class="info-label">Device:</span>
            <span class="info-val">${deviceToUse}</span>
          </div>
          <div class="info-row">
            <span class="info-label">Time:</span>
            <span class="info-val">${timeToUse}</span>
          </div>
          <div class="info-row">
            <span class="info-label">Status:</span>
            <span class="info-val" style="color: #2c974b; font-weight: bold;">Successful Login</span>
          </div>
        </div>

        <p>If this was you, no action is needed. You can happily ignore this message and continue learning on FunkyTalk!</p>
        <p><strong>If this was NOT you</strong>, your account security might be compromised. Please reset your password immediately using the "Forgot Password" link on the login screen to secure your credentials.</p>
        
        <div class="footer">
          This is an automated safety alert from FunkyTalk Account Security.<br>
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
          name: "FunkyTalk Safety ✨",
          email: "security@funkytalk.jo3.org"
        },
        to: [
          {
            email: email,
            name: nameToUse
          }
        ],
        subject: "🔒 Security Alert: New Login to FunkyTalk ✨",
        htmlContent: htmlEmailBody
      })
    })

    if (!brevoResponse.ok) {
      const errorText = await brevoResponse.text()
      throw new Error(`Brevo API status ${brevoResponse.status}: ${errorText}`)
    }

    return new Response(JSON.stringify({ success: true, message: "Login alert dispatched successfully via Brevo custom HTML!" }), {
      headers: { ...corsHeaders, 'Content-Type': 'application/json' }
    })

  } catch (error) {
    return new Response(JSON.stringify({ error: error.message }), {
      status: 500,
      headers: { ...corsHeaders, 'Content-Type': 'application/json' }
    })
  }
})

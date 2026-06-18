import { serve } from "https://deno.land/std@0.177.0/http/server.ts"
import { createClient } from "https://esm.sh/@supabase/supabase-js@2"

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
}

serve(async (req) => {
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders })
  }

  try {
    const url = new URL(req.url)
    const uid = url.searchParams.get("uid")
    const token = url.searchParams.get("token")

    if (!uid || !token) {
      return renderHtmlResponse(
        "Invalid Link",
        "We couldn't authenticate this request. Missing security parameters.",
        false
      )
    }

    const supabaseUrl = Deno.env.get("SUPABASE_URL")!
    const supabaseServiceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!

    const supabase = createClient(supabaseUrl, supabaseServiceKey)

    // Retrieve token from Supabase Table
    const { data: user, error: fetchError } = await supabase
      .from('users')
      .select('email_verification_token, custom_email_verified')
      .eq('uid', uid)
      .maybeSingle()

    if (fetchError || !user) {
      return renderHtmlResponse(
        "Verification Failed",
        "User account not found or database sync pending.",
        false
      )
    }

    if (user.custom_email_verified) {
      return renderHtmlResponse(
        "Already Verified ✨",
        "Your email is already verified. You are completely set to log in and use FunkyTalk!",
        true
      )
    }

    if (user.email_verification_token !== token) {
      return renderHtmlResponse(
        "Verification Expired",
        "The verification link is invalid or has expired. Please check your inbox or click resend.",
        false
      )
    }

    // Update state to True
    const { error: updateError } = await supabase
      .from('users')
      .update({ custom_email_verified: true })
      .eq('uid', uid)

    if (updateError) {
      throw new Error(`Database update failed: ${updateError.message}`)
    }

    return renderHtmlResponse(
      "Verified Successfully 🎉",
      "Your FunkyTalk email has been verified. You can return directly to your application now!",
      true
    )

  } catch (error) {
    return renderHtmlResponse(
      "Service Error",
      `An unexpected issue occurred: ${error.message}`,
      false
    )
  }
})

function renderHtmlResponse(title: string, message: string, isSuccess: boolean): Response {
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
</html>
`
  return new Response(html, {
    headers: { ...corsHeaders, 'Content-Type': 'text/html' }
  })
}

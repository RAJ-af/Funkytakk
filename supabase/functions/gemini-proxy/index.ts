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

async function checkRateLimit(
  supabase: any,
  key: string,
  action: string,
  maxRequests = 30,
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

    const userId = authData?.user?.id || "authenticated_user"
    const supabaseAdmin = createClient(supabaseUrl, supabaseServiceKey)

    // 2. RATE LIMITING: Max 30 Gemini requests per hour per user
    const rateLimit = await checkRateLimit(supabaseAdmin, userId, "gemini-proxy", 30, 3600)
    if (!rateLimit.allowed) {
      return new Response(
        JSON.stringify({
          error: "Rate limit exceeded for AI features. Please wait before sending more requests.",
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

    // 3. RETRIEVE SERVER-SIDE GEMINI API KEY
    const geminiApiKey = Deno.env.get("GEMINI_API_KEY")
    if (!geminiApiKey) {
      return new Response(
        JSON.stringify({
          error: "GEMINI_API_KEY is not configured in Supabase Edge Secrets. Please set it in your Supabase dashboard.",
        }),
        {
          status: 500,
          headers: { ...corsHeaders, "Content-Type": "application/json" },
        }
      )
    }

    const { prompt, model = "gemini-2.5-flash", systemInstruction } = await req.json()

    if (!prompt) {
      return new Response(JSON.stringify({ error: "Missing required parameter: prompt" }), {
        status: 400,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      })
    }

    // 4. FORWARD PROXY TO GOOGLE GEMINI REST API
    const geminiUrl = `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${geminiApiKey}`

    const requestPayload: Record<string, any> = {
      contents: [
        {
          role: "user",
          parts: [{ text: prompt }],
        },
      ],
    }

    if (systemInstruction) {
      requestPayload.systemInstruction = {
        parts: [{ text: systemInstruction }],
      }
    }

    const geminiResponse = await fetch(geminiUrl, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(requestPayload),
    })

    if (!geminiResponse.ok) {
      const errorText = await geminiResponse.text()
      return new Response(
        JSON.stringify({
          error: `Gemini API error (${geminiResponse.status}): ${errorText}`,
        }),
        {
          status: geminiResponse.status,
          headers: { ...corsHeaders, "Content-Type": "application/json" },
        }
      )
    }

    const geminiData = await geminiResponse.json()
    const textOutput =
      geminiData.candidates?.[0]?.content?.parts?.[0]?.text || ""

    return new Response(
      JSON.stringify({
        success: true,
        text: textOutput,
        raw: geminiData,
      }),
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

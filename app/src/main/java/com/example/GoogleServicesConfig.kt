package com.example

import android.content.Context
import org.json.JSONObject
import java.io.InputStreamReader

/**
 * Helper to read configuration directly from google-services.json packaged in the app,
 * ensuring no external secrets or environment variables are needed for Firebase / Google Auth.
 */
object GoogleServicesConfig {

    private const val GOOGLE_SERVICES_FILE = "google-services.json"

    // Extracted directly from google-services.json:
    // client_type: 3 (Web client ID used for backend token verification & GoogleSignInOptions)
    const val FALLBACK_WEB_CLIENT_ID = "1093102729820-7dbvqkqf7uall5go8aasmsvs89keesq6.apps.googleusercontent.com"
    const val FALLBACK_API_KEY = "AIzaSyAZi997wGgWE7WJ-N5NO9kOWL2agpijq-8"
    const val FALLBACK_PROJECT_ID = "funkytalk-f1aae"
    const val FALLBACK_APP_ID = "1:1093102729820:android:6dda4c8c8c1369a984c499"
    const val FALLBACK_GCM_SENDER_ID = "1093102729820"

    fun getGcmSenderId(context: Context): String {
        return readProperty(context) { json ->
            val projectInfo = json.optJSONObject("project_info")
            val num = projectInfo?.optString("project_number")
            if (!num.isNullOrBlank()) num else null
        } ?: FALLBACK_GCM_SENDER_ID
    }

    fun getWebClientId(context: Context): String {
        return readProperty(context) { json ->
            val clients = json.optJSONArray("client")
            if (clients != null && clients.length() > 0) {
                val client = clients.getJSONObject(0)
                val oauthClients = client.optJSONArray("oauth_client")
                if (oauthClients != null) {
                    for (i in 0 until oauthClients.length()) {
                        val obj = oauthClients.getJSONObject(i)
                        // client_type 3 is Web OAuth Client
                        if (obj.optInt("client_type") == 3) {
                            val id = obj.optString("client_id")
                            if (!id.isNullOrBlank()) return@readProperty id
                        }
                    }
                }
            }
            null
        } ?: FALLBACK_WEB_CLIENT_ID
    }

    fun getApiKey(context: Context): String {
        return readProperty(context) { json ->
            val clients = json.optJSONArray("client")
            if (clients != null && clients.length() > 0) {
                val apiKeys = clients.getJSONObject(0).optJSONArray("api_key")
                if (apiKeys != null && apiKeys.length() > 0) {
                    val key = apiKeys.getJSONObject(0).optString("current_key")
                    if (!key.isNullOrBlank()) return@readProperty key
                }
            }
            null
        } ?: FALLBACK_API_KEY
    }

    fun getProjectId(context: Context): String {
        return readProperty(context) { json ->
            val projectInfo = json.optJSONObject("project_info")
            val pid = projectInfo?.optString("project_id")
            if (!pid.isNullOrBlank()) pid else null
        } ?: FALLBACK_PROJECT_ID
    }

    fun getApplicationId(context: Context): String {
        return readProperty(context) { json ->
            val clients = json.optJSONArray("client")
            if (clients != null && clients.length() > 0) {
                val clientInfo = clients.getJSONObject(0).optJSONObject("client_info")
                val appId = clientInfo?.optString("mobilesdk_app_id")
                if (!appId.isNullOrBlank()) return@readProperty appId
            }
            null
        } ?: FALLBACK_APP_ID
    }

    private fun readProperty(context: Context, extractor: (JSONObject) -> String?): String? {
        try {
            // First check assets if placed in assets/
            context.assets.open(GOOGLE_SERVICES_FILE).use { stream ->
                val content = InputStreamReader(stream).readText()
                val json = JSONObject(content)
                val res = extractor(json)
                if (res != null) return res
            }
        } catch (_: Exception) {
            // Asset reading fallback
        }
        return null
    }
}

package com.example.repository

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class JokeItem(
    val setup: String,
    val punchline: String,
    val category: String = "General"
)

object JokeRepository {
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    private val curatedJokes = listOf(
        JokeItem("Why don't scientists trust atoms?", "Because they make up everything! ⚛️😂", "Science"),
        JokeItem("Why do we tell actors to 'break a leg'?", "Because every play has a cast! 🎭🤣", "Theater"),
        JokeItem("What do you call a fake noodle?", "An impasta! 🍝😆", "Food"),
        JokeItem("Why did the scarecrow win an award?", "Because he was outstanding in his field! 🌾😄", "Pun"),
        JokeItem("Why was the math book always unhappy?", "Because it had too many problems! 📐🤣", "School"),
        JokeItem("What did one wall say to the other wall?", "I'll meet you at the corner! 🧱😄", "Clever"),
        JokeItem("Why can't bicycles stand up by themselves?", "Because they're two-tired! 🚲😂", "Pun"),
        JokeItem("How does the moon cut its hair?", "Eclipse it! 🌙✂️", "Space"),
        JokeItem("Why did the cookie go to the doctor?", "Because it felt crummy! 🍪😆", "Food"),
        JokeItem("What do you call a bear with no teeth?", "A gummy bear! 🐻🍬", "Animal"),
        JokeItem("Why do birds fly south for the winter?", "Because it's too far to walk! 🐦✈️", "Nature")
    )

    suspend fun getRandomJoke(): JokeItem = withContext(Dispatchers.IO) {
        // 1. Try Official Joke API (free, reliable, HTTPS, no API key needed)
        try {
            val req = Request.Builder()
                .url("https://official-joke-api.appspot.com/random_joke")
                .header("Accept", "application/json")
                .build()
            val resp = client.newCall(req).execute()
            if (resp.isSuccessful) {
                val bodyStr = resp.body?.string()
                if (!bodyStr.isNullOrBlank()) {
                    val json = JSONObject(bodyStr)
                    val setup = json.optString("setup")
                    val punchline = json.optString("punchline")
                    val type = json.optString("type", "general")
                    if (setup.isNotBlank() && punchline.isNotBlank()) {
                        return@withContext JokeItem(
                            setup = setup.trim(),
                            punchline = punchline.trim(),
                            category = type.replaceFirstChar { it.uppercase() }
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.d("JokeRepository", "Official Joke API note: ${e.message}")
        }

        // 2. Fallback to JokeAPI safe mode
        try {
            val req2 = Request.Builder()
                .url("https://v2.jokeapi.dev/joke/Any?safe-mode")
                .header("Accept", "application/json")
                .build()
            val resp2 = client.newCall(req2).execute()
            if (resp2.isSuccessful) {
                val bodyStr2 = resp2.body?.string()
                if (!bodyStr2.isNullOrBlank()) {
                    val json2 = JSONObject(bodyStr2)
                    if (json2.optString("type") == "twopart") {
                        val setup = json2.optString("setup")
                        val delivery = json2.optString("delivery")
                        if (setup.isNotBlank() && delivery.isNotBlank()) {
                            return@withContext JokeItem(
                                setup = setup.trim(),
                                punchline = delivery.trim(),
                                category = json2.optString("category", "General")
                            )
                        }
                    } else if (json2.has("joke")) {
                        val singleJoke = json2.optString("joke")
                        if (singleJoke.isNotBlank()) {
                            return@withContext JokeItem(
                                setup = singleJoke.trim(),
                                punchline = "😂 Hope that brought a smile!",
                                category = json2.optString("category", "General")
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.d("JokeRepository", "JokeAPI note: ${e.message}")
        }

        // 3. Fallback to curated laugh collection if device is offline or network fails
        curatedJokes.random()
    }
}

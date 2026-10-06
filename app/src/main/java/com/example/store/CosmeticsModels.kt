package com.example.store

import androidx.compose.ui.graphics.Color
import org.json.JSONArray
import org.json.JSONObject

// ----------------------------------------------------
// 1. BUBBLE THEME CONFIG
// ----------------------------------------------------
data class BubbleBorderConfig(
    val colorHex: String = "#E5A800",
    val widthDp: Float = 1.5f
)

data class BubbleThemeConfig(
    val bgColorsHex: List<String> = listOf("#FFE8A3"),
    val bgAngle: Float = 0f,
    val textColorHex: String = "#1B1B1B",
    val metaColorHex: String = "#5C522B",
    val radiusDp: Float = 18f,
    val tailStyle: String = "round", // "round" | "sharp"
    val border: BubbleBorderConfig? = null,
    val glowHex: String? = null
)

// ----------------------------------------------------
// 2. CHAT BACKGROUND CONFIG
// ----------------------------------------------------
sealed interface ChatBgStyle {
    data class Solid(val colorHex: String) : ChatBgStyle
    data class Gradient(val colorsHex: List<String>, val angle: Float) : ChatBgStyle
    data class Pattern(
        val baseColorHex: String,
        val patternType: String, // "petals" | "stars"
        val patternColorHex: String,
        val opacity: Float
    ) : ChatBgStyle
}

data class ChatBgConfig(
    val light: ChatBgStyle = ChatBgStyle.Solid("#F6F4EE"),
    val dark: ChatBgStyle = ChatBgStyle.Solid("#0F1114")
)

// ----------------------------------------------------
// 3. AVATAR FRAME CONFIG
// ----------------------------------------------------
data class FrameRingConfig(
    val shape: String = "circle", // "circle" | "hexagon" | "scalloped" | "double"
    val widthDp: Float = 3f,
    val colorsHex: List<String> = listOf("#FFD700", "#FF8C00"),
    val animation: String = "none", // "none" | "rotate" | "pulse"
    val speedMs: Long = 3000L
)

data class FrameOrnamentConfig(
    val emoji: String = "👑",
    val position: String = "top_right", // "top" | "bottom" | "top_right"
    val scale: Float = 1f
)

data class FrameParticlesConfig(
    val type: String = "sparkle", // "sparkle" | "petals" | "flames" | "snow" | "stars" | "hearts"
    val count: Int = 8,
    val colorsHex: List<String> = listOf("#FFD700")
)

data class FrameGlowConfig(
    val colorHex: String = "#FFD700",
    val pulse: Boolean = false
)

data class AvatarFrameConfig(
    val ring: FrameRingConfig = FrameRingConfig(),
    val ornament: FrameOrnamentConfig? = null,
    val particles: FrameParticlesConfig? = null,
    val glow: FrameGlowConfig? = null
)

// ----------------------------------------------------
// 4. NAME EFFECT CONFIG
// ----------------------------------------------------
data class NameEffectConfig(
    val colorsHex: List<String> = listOf("#FF007F", "#7F00FF"),
    val animation: String = "none", // "none" | "shimmer" | "gradient_flow" | "glow_pulse"
    val speedMs: Long = 2000L,
    val glowHex: String? = null,
    val bold: Boolean = true
)

// ----------------------------------------------------
// 5. STICKER PACK CONFIG
// ----------------------------------------------------
data class StickerPackConfig(
    val source: String = "noto_animated",
    val format: String = "lottie",
    val emojis: List<String> = listOf("😍", "🥰", "🔥", "🎉", "❤️", "👍")
)

// ----------------------------------------------------
// HELPER PARSERS (Safe JSON -> Type-safe Configs)
// ----------------------------------------------------
fun parseHexColor(hex: String?, defaultColor: Color = Color.Gray): Color {
    if (hex.isNullOrBlank()) return defaultColor
    return try {
        val clean = hex.trim().removePrefix("#")
        val colorInt = when (clean.length) {
            6 -> (0xFF000000 or clean.toLong(16)).toInt()
            8 -> clean.toLong(16).toInt()
            else -> return defaultColor
        }
        Color(colorInt)
    } catch (_: Exception) {
        defaultColor
    }
}

object CosmeticsConfigParser {

    fun parseBubbleTheme(rawMap: Map<String, Any?>?): BubbleThemeConfig {
        if (rawMap == null) return BubbleThemeConfig()
        return try {
            val json = JSONObject(rawMap)
            val bgList = mutableListOf<String>()
            json.optJSONArray("bg")?.let { arr ->
                for (i in 0 until arr.length()) bgList.add(arr.optString(i))
            }
            if (bgList.isEmpty()) bgList.add(json.optString("bg", "#FFE8A3"))

            val borderObj = json.optJSONObject("border")
            val border = if (borderObj != null) {
                BubbleBorderConfig(
                    colorHex = borderObj.optString("color", "#E5A800"),
                    widthDp = borderObj.optDouble("width", 1.5).toFloat()
                )
            } else null

            val glow = json.optString("glow").takeIf { it.isNotBlank() && it != "null" }

            BubbleThemeConfig(
                bgColorsHex = bgList,
                bgAngle = json.optDouble("bg_angle", 0.0).toFloat(),
                textColorHex = json.optString("text", "#1B1B1B"),
                metaColorHex = json.optString("meta", "#5C522B"),
                radiusDp = json.optDouble("radius", 18.0).toFloat(),
                tailStyle = json.optString("tail", "round"),
                border = border,
                glowHex = glow
            )
        } catch (_: Exception) {
            BubbleThemeConfig()
        }
    }

    private fun parseBgStyle(obj: JSONObject?): ChatBgStyle {
        if (obj == null) return ChatBgStyle.Solid("#F6F4EE")
        val kind = obj.optString("kind", "solid")
        return when (kind) {
            "gradient" -> {
                val colors = mutableListOf<String>()
                obj.optJSONArray("colors")?.let { arr ->
                    for (i in 0 until arr.length()) colors.add(arr.optString(i))
                }
                if (colors.isEmpty()) colors.add(obj.optString("color", "#EEF3F8"))
                ChatBgStyle.Gradient(colors, obj.optDouble("angle", 0.0).toFloat())
            }
            "pattern" -> {
                ChatBgStyle.Pattern(
                    baseColorHex = obj.optString("base", "#F6F4EE"),
                    patternType = obj.optString("pattern", "stars"),
                    patternColorHex = obj.optString("pattern_color", "#E5E0D5"),
                    opacity = obj.optDouble("opacity", 0.3).toFloat()
                )
            }
            else -> ChatBgStyle.Solid(obj.optString("color", "#F6F4EE"))
        }
    }

    fun parseChatBg(rawMap: Map<String, Any?>?): ChatBgConfig {
        if (rawMap == null) return ChatBgConfig()
        return try {
            val json = JSONObject(rawMap)
            val lightObj = json.optJSONObject("light")
            val darkObj = json.optJSONObject("dark")
            ChatBgConfig(
                light = parseBgStyle(lightObj ?: json),
                dark = parseBgStyle(darkObj)
            )
        } catch (_: Exception) {
            ChatBgConfig()
        }
    }

    fun parseAvatarFrame(rawMap: Map<String, Any?>?): AvatarFrameConfig {
        if (rawMap == null) return AvatarFrameConfig()
        return try {
            val json = JSONObject(rawMap)
            val ringObj = json.optJSONObject("ring")
            val colorsList = mutableListOf<String>()
            ringObj?.optJSONArray("colors")?.let { arr ->
                for (i in 0 until arr.length()) colorsList.add(arr.optString(i))
            }
            if (colorsList.isEmpty()) colorsList.add("#FFD700")

            val ring = FrameRingConfig(
                shape = ringObj?.optString("shape", "circle") ?: "circle",
                widthDp = ringObj?.optDouble("width", 3.0)?.toFloat() ?: 3f,
                colorsHex = colorsList,
                animation = ringObj?.optString("animation", "none") ?: "none",
                speedMs = ringObj?.optLong("speed_ms", 3000L) ?: 3000L
            )

            val ornObj = json.optJSONObject("ornament")
            val ornament = if (ornObj != null) {
                FrameOrnamentConfig(
                    emoji = ornObj.optString("emoji", "👑"),
                    position = ornObj.optString("position", "top_right"),
                    scale = ornObj.optDouble("scale", 1.0).toFloat()
                )
            } else null

            val partObj = json.optJSONObject("particles")
            val particles = if (partObj != null) {
                val pColors = mutableListOf<String>()
                partObj.optJSONArray("colors")?.let { arr ->
                    for (i in 0 until arr.length()) pColors.add(arr.optString(i))
                }
                if (pColors.isEmpty()) pColors.add("#FFD700")
                FrameParticlesConfig(
                    type = partObj.optString("type", "sparkle"),
                    count = partObj.optInt("count", 8).coerceAtMost(12),
                    colorsHex = pColors
                )
            } else null

            val glowObj = json.optJSONObject("glow")
            val glow = if (glowObj != null) {
                FrameGlowConfig(
                    colorHex = glowObj.optString("color", "#FFD700"),
                    pulse = glowObj.optBoolean("pulse", false)
                )
            } else null

            AvatarFrameConfig(
                ring = ring,
                ornament = ornament,
                particles = particles,
                glow = glow
            )
        } catch (_: Exception) {
            AvatarFrameConfig()
        }
    }

    fun parseNameEffect(rawMap: Map<String, Any?>?): NameEffectConfig {
        if (rawMap == null) return NameEffectConfig()
        return try {
            val json = JSONObject(rawMap)
            val colorsList = mutableListOf<String>()
            json.optJSONArray("colors")?.let { arr ->
                for (i in 0 until arr.length()) colorsList.add(arr.optString(i))
            }
            if (colorsList.isEmpty()) colorsList.add(json.optString("color", "#FF007F"))

            val glow = json.optString("glow").takeIf { it.isNotBlank() && it != "null" }

            NameEffectConfig(
                colorsHex = colorsList,
                animation = json.optString("animation", "none"),
                speedMs = json.optLong("speed_ms", 2000L),
                glowHex = glow,
                bold = json.optBoolean("bold", true)
            )
        } catch (_: Exception) {
            NameEffectConfig()
        }
    }

    fun parseStickerPack(rawMap: Map<String, Any?>?): StickerPackConfig {
        if (rawMap == null) return StickerPackConfig()
        return try {
            val json = JSONObject(rawMap)
            val emojisList = mutableListOf<String>()
            json.optJSONArray("emojis")?.let { arr ->
                for (i in 0 until arr.length()) emojisList.add(arr.optString(i))
            }
            StickerPackConfig(
                source = json.optString("source", "noto_animated"),
                format = json.optString("format", "lottie"),
                emojis = emojisList
            )
        } catch (_: Exception) {
            StickerPackConfig()
        }
    }
}

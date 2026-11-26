package com.example.card

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import kotlin.math.roundToInt

enum class CardBgStyle(val wire: String, val label: String) {
    SOLID("solid", "Solid"),
    GRADIENT("gradient", "Gradient"),
    DOTS("dots", "Dots"),
    GRID("grid", "Grid"),
    PAPER("paper", "Paper"),
    STRIPES("stripes", "Stripes");

    companion object {
        fun fromWire(value: String?): CardBgStyle =
            entries.firstOrNull { it.wire.equals(value?.trim(), ignoreCase = true) } ?: SOLID
    }
}

enum class CardBorderStyle(val wire: String, val label: String) {
    NONE("none", "None"),
    THIN("thin", "Thin"),
    BOLD("bold", "Bold"),
    DASHED("dashed", "Dashed");

    companion object {
        fun fromWire(value: String?): CardBorderStyle =
            entries.firstOrNull { it.wire.equals(value?.trim(), ignoreCase = true) } ?: BOLD
    }
}

enum class CardStickerKind(val wire: String) {
    WORD("word"),
    EMOJI("emoji"),
    PACK("pack");

    companion object {
        fun fromWire(value: String?): CardStickerKind =
            entries.firstOrNull { it.wire.equals(value?.trim(), ignoreCase = true) } ?: EMOJI
    }
}

enum class CardStickerStyle(val wire: String, val label: String) {
    STICKER("sticker", "Sticker"),
    PLAIN("plain", "Plain"),
    BUBBLE("bubble", "Bubble"),
    TAPE("tape", "Tape"),
    OUTLINE("outline", "Outline"),
    NEON("neon", "Neon");

    companion object {
        fun fromWire(value: String?): CardStickerStyle =
            entries.firstOrNull { it.wire.equals(value?.trim(), ignoreCase = true) } ?: STICKER
    }
}

data class CardTheme(
    val bg: String = "#FFFBF0",
    val bg2: String = "#FFE8A3",
    val bgStyle: CardBgStyle = CardBgStyle.SOLID,
    val accent: String = "#FFB627",
    val border: CardBorderStyle = CardBorderStyle.BOLD,
    val radius: Int = 28
)

data class CardShowPrefs(
    val age: Boolean = true,
    val gender: Boolean = false,
    val country: Boolean = true,
    val city: Boolean = false,
    val languages: Boolean = true,
    val bio: Boolean = true,
    val hobbies: Boolean = true,
    val onlineDot: Boolean = true
)

data class CardSticker(
    val localId: String = UUID.randomUUID().toString(),
    val kind: CardStickerKind = CardStickerKind.WORD,
    val x: Float = 0.5f,
    val y: Float = 0.5f,
    val rot: Float = 0f,
    val scale: Float = 1f,
    val style: CardStickerStyle = CardStickerStyle.STICKER,
    val text: String = "",
    val emoji: String = "",
    val color: String = "#1C1917",
    val packId: Long? = null,
    val packCode: String? = null,
    val packIdx: Int? = null
)

data class CardDesign(
    val v: Int = 1,
    val theme: CardTheme = CardTheme(),
    val show: CardShowPrefs = CardShowPrefs(),
    val hiLabel: String = "Hi",
    val stickers: List<CardSticker> = emptyList()
)

data class PeopleCardProfile(
    val userId: String,
    val name: String,
    val username: String = "",
    val age: Int? = null,
    val gender: String? = null,
    val isOnline: Boolean = false,
    val avatarUrl: String = "",
    val countryCode: String = "",
    val country: String = "",
    val city: String = "",
    val nativeLang: String = "",
    val learningLang: String = "",
    val bio: String = "",
    val hobbies: List<String> = emptyList(),
    val isWaved: Boolean = false,
    val isVerified: Boolean = false
)

data class RemixableCardItem(
    val userId: String,
    val creatorName: String,
    val creatorUsername: String = "",
    val creatorAvatarUrl: String = "",
    val countryCode: String = "",
    val remixCount: Int = 0,
    val design: CardDesign
)

object CardDesignDefaults {
    const val MAX_STICKERS = 12
    const val MAX_STICKER_TEXT_LEN = 14
    const val MAX_HI_LABEL_LEN = 12

    val wordPresets: List<String> = listOf(
        "Hello", "Hi!", "Namaste", "Hola", "Bonjour", "Ciao",
        "Salaam", "Konnichiwa", "Annyeong", "Merhaba", "Hey", "Vibe", "Let's talk"
    )

    val emojiPresets: List<String> = listOf(
        "✨", "🔥", "🫶", "😎", "🥳", "✌️",
        "☕", "🎧", "🌸", "⚡", "🌍", "💬",
        "🦋", "🌙", "🍓", "🎮", "📸", "🫶🏽",
        "💫", "🌈", "🍜", "🧸", "🚀", "👑"
    )

    val bgSwatches: List<String> = listOf(
        "#FFFBF0", "#FFF3B0", "#DCFCE7", "#DBEAFE", "#F3E8FF", "#FFE4E6",
        "#FED7AA", "#CCFBF1", "#1E1B18", "#1E1B4B", "#0F2922", "#3B0764"
    )

    val gradientPresets: List<Pair<String, String>> = listOf(
        "#FFF3B0" to "#FDE68A",
        "#FCE7F3" to "#DBEAFE",
        "#DCFCE7" to "#FEF9C3",
        "#EDE9FE" to "#FFE4E6",
        "#FFEDD5" to "#FECACA",
        "#1E1B4B" to "#4C1D95",
        "#0F172A" to "#134E4A",
        "#3B0764" to "#831843"
    )

    val accentSwatches: List<String> = listOf(
        "#FFB627", "#FF5A5F", "#10B981", "#3B82F6", "#8B5CF6", "#EC4899", "#F97316", "#14B8A6"
    )

    val stickerColorSwatches: List<String> = listOf(
        "#1C1917", "#FFFFFF", "#FFB627", "#EF4444", "#10B981", "#3B82F6", "#8B5CF6", "#EC4899", "#F97316"
    )
}

object CardTextSafetyFilter {
    private val blockedWords = setOf(
        "fuck", "fucking", "fucker", "fck", "fuk", "shit", "shitty", "bitch", "bitches",
        "asshole", "bastard", "cunt", "dick", "cock", "pussy", "whore", "slut",
        "nigger", "nigga", "faggot", "fag", "retard", "retarded", "kike", "spic",
        "chink", "tranny", "porn", "porno", "nude", "nudes", "sex", "sexy", "horny",
        "boobs", "tits", "penis", "vagina", "blowjob", "handjob", "dildo", "cum",
        "rape", "rapist", "kill", "kys", "suicide", "terrorist", "nazi",
        "madarchod", "bhenchod", "bhosdike", "chutiya", "gandu", "randi", "harami",
        "kamina", "kutta", "suar", "loda", "lund", "chut"
    )

    private val leetspeakMap = mapOf(
        '0' to 'o', '1' to 'i', '3' to 'e', '4' to 'a', '5' to 's',
        '7' to 't', '@' to 'a', '$' to 's', '!' to 'i'
    )

    fun containsBlockedWord(input: String?): Boolean {
        val raw = input?.trim().orEmpty()
        if (raw.isEmpty()) return false
        val normalized = raw.lowercase().map { ch -> leetspeakMap[ch] ?: ch }.joinToString("")
        val tokens = normalized.split(Regex("[^a-z]+")).filter { it.isNotBlank() }
        if (tokens.any { it in blockedWords }) return true
        val compact = normalized.replace(Regex("[^a-z]"), "")
        return blockedWords.any { bad -> bad.length >= 4 && compact.contains(bad) }
    }
}

fun parseCardColorOrDefault(hex: String?, fallback: Color): Color {
    if (hex.isNullOrBlank()) return fallback
    val clean = hex.trim().removePrefix("#")
    val value = clean.toLongOrNull(16) ?: return fallback
    return when (clean.length) {
        6 -> Color((0xFF000000L or value).toInt())
        8 -> Color(value.toInt())
        else -> fallback
    }
}

fun Color.isCardDarkBackground(): Boolean = luminance() < 0.42f

fun normalizeHex6(hex: String?, fallback: String): String {
    val raw = hex?.trim().orEmpty()
    val clean = raw.removePrefix("#").uppercase()
    if (clean.length == 6 && clean.all { it in '0'..'9' || it in 'A'..'F' }) {
        return "#$clean"
    }
    if (clean.length == 8 && clean.all { it in '0'..'9' || it in 'A'..'F' }) {
        return "#${clean.drop(2)}"
    }
    return fallback
}

object CardDesignCodec {
    private fun round3(v: Float): Double = (v * 1000f).roundToInt() / 1000.0
    private fun round1(v: Float): Double = (v * 10f).roundToInt() / 10.0

    fun toWireMap(design: CardDesign): Map<String, Any?> {
        val bgHex = normalizeHex6(design.theme.bg, "#FFFBF0")
        val isDarkBg = parseCardColorOrDefault(bgHex, Color(0xFFFFFBF0)).isCardDarkBackground()
        val themeMap = linkedMapOf<String, Any?>(
            "bg" to bgHex,
            "ink" to (if (isDarkBg) "#FFFBF2" else "#1A1A1A"),
            "style" to design.theme.bgStyle.wire,
            "accent" to normalizeHex6(design.theme.accent, "#FFB627"),
            "border" to design.theme.border.wire,
            "radius" to design.theme.radius.coerceIn(0, 32)
        )
        if (design.theme.bgStyle == CardBgStyle.GRADIENT) {
            themeMap["bg2"] = normalizeHex6(design.theme.bg2, "#FFE8A3")
        }

        val showMap = linkedMapOf<String, Any?>(
            "age" to design.show.age,
            "gender" to design.show.gender,
            "country" to design.show.country,
            "city" to design.show.city,
            "languages" to design.show.languages,
            "bio" to design.show.bio,
            "hobbies" to design.show.hobbies,
            "online_dot" to design.show.onlineDot
        )

        val stickersList = design.stickers
            .take(CardDesignDefaults.MAX_STICKERS)
            .map { st ->
                linkedMapOf<String, Any?>().apply {
                    put("kind", st.kind.wire)
                    put("x", round3(st.x.coerceIn(0f, 1f)))
                    put("y", round3(st.y.coerceIn(0f, 1f)))
                    put("rot", round1(st.rot.coerceIn(-45f, 45f)))
                    put("scale", round3(st.scale.coerceIn(0.5f, 2.5f)))
                    put("style", st.style.wire)
                    put("color", normalizeHex6(st.color, "#1C1917"))
                    when (st.kind) {
                        CardStickerKind.WORD -> {
                            put("text", st.text.trim().take(CardDesignDefaults.MAX_STICKER_TEXT_LEN).ifEmpty { "Hello" })
                        }
                        CardStickerKind.EMOJI -> {
                            put("emoji", st.emoji.ifBlank { "✨" })
                        }
                        CardStickerKind.PACK -> {
                            if (!st.packCode.isNullOrBlank()) put("item", st.packCode)
                            put("idx", (st.packIdx ?: 0).coerceAtLeast(0))
                        }
                    }
                }
            }

        val cleanHi = design.hiLabel.trim()
            .take(CardDesignDefaults.MAX_HI_LABEL_LEN)
            .ifBlank { "Hi" }

        return linkedMapOf(
            "v" to 1,
            "theme" to themeMap,
            "show" to showMap,
            "hi_label" to cleanHi,
            "stickers" to stickersList
        )
    }

    fun toJsonString(design: CardDesign): String = JSONObject(toWireMap(design)).toString()

    fun fromJsonObject(obj: JSONObject?): CardDesign {
        if (obj == null) return CardDesign()
        val themeObj = obj.optJSONObject("theme") ?: obj
        val bg = normalizeHex6(
            themeObj.optString("bg", "").takeIf { it.isNotBlank() }
                ?: themeObj.optJSONArray("bg")?.optString(0),
            "#FFFBF0"
        )
        val bg2 = normalizeHex6(
            themeObj.optString("bg2", "").takeIf { it.isNotBlank() }
                ?: themeObj.optString("bg_end", "").takeIf { it.isNotBlank() }
                ?: themeObj.optJSONArray("bg")?.optString(1),
            "#FFE8A3"
        )
        val rawStyle = themeObj.optString("style", "").takeIf { it.isNotBlank() && it != "null" }
            ?: themeObj.optString("bg_style", "solid")
        val theme = CardTheme(
            bg = bg,
            bg2 = bg2,
            bgStyle = CardBgStyle.fromWire(rawStyle),
            accent = normalizeHex6(themeObj.optString("accent", "#FFB627"), "#FFB627"),
            border = CardBorderStyle.fromWire(themeObj.optString("border", "bold")),
            radius = themeObj.optInt("radius", 28).coerceIn(0, 32)
        )

        val showObj = obj.optJSONObject("show")
        val show = if (showObj != null) {
            CardShowPrefs(
                age = showObj.optBoolean("age", true),
                gender = showObj.optBoolean("gender", false),
                country = showObj.optBoolean("country", true),
                city = showObj.optBoolean("city", false),
                languages = showObj.optBoolean("languages", true),
                bio = showObj.optBoolean("bio", true),
                hobbies = showObj.optBoolean("hobbies", true),
                onlineDot = showObj.optBoolean("online_dot", true)
            )
        } else {
            CardShowPrefs()
        }

        val hiLabel = obj.optString("hi_label", "Hi")
            .trim()
            .take(CardDesignDefaults.MAX_HI_LABEL_LEN)
            .ifBlank { "Hi" }

        val stickersArr: JSONArray? = obj.optJSONArray("stickers")
        val stickers = mutableListOf<CardSticker>()
        if (stickersArr != null) {
            val count = minOf(stickersArr.length(), CardDesignDefaults.MAX_STICKERS)
            for (i in 0 until count) {
                val stObj = stickersArr.optJSONObject(i) ?: continue
                val kind = CardStickerKind.fromWire(stObj.optString("kind", "emoji"))
                val x = stObj.optDouble("x", 0.5).toFloat().let { if (it.isFinite()) it.coerceIn(0f, 1f) else 0.5f }
                val y = stObj.optDouble("y", 0.5).toFloat().let { if (it.isFinite()) it.coerceIn(0f, 1f) else 0.5f }
                val rot = stObj.optDouble("rot", 0.0).toFloat().let { if (it.isFinite()) it.coerceIn(-45f, 45f) else 0f }
                val scale = stObj.optDouble("scale", 1.0).toFloat().let { if (it.isFinite()) it.coerceIn(0.5f, 2.5f) else 1f }
                val style = CardStickerStyle.fromWire(stObj.optString("style", "sticker"))
                val color = normalizeHex6(stObj.optString("color", "#1C1917"), "#1C1917")
                val text = stObj.optString("text", "").trim().take(CardDesignDefaults.MAX_STICKER_TEXT_LEN)
                val emoji = stObj.optString("emoji", "").trim()
                val rawPackId = if (stObj.has("pack_id") && !stObj.isNull("pack_id")) stObj.optLong("pack_id", -1L).takeIf { it > 0L } else null
                val rawPackCode = stObj.optString("item", "").takeIf { it.isNotBlank() && it != "null" }
                    ?: stObj.optString("pack", "").takeIf { it.isNotBlank() && it != "null" }
                    ?: stObj.optString("pack_code", "").takeIf { it.isNotBlank() && it != "null" }
                val rawPackIdx = if (stObj.has("idx") && !stObj.isNull("idx")) stObj.optInt("idx", 0).coerceAtLeast(0) else null

                if (kind == CardStickerKind.WORD && text.isBlank()) continue
                if (kind == CardStickerKind.EMOJI && emoji.isBlank()) continue
                if (kind == CardStickerKind.PACK && emoji.isBlank() && rawPackId == null && rawPackCode == null) continue

                stickers.add(
                    CardSticker(
                        kind = kind,
                        x = x,
                        y = y,
                        rot = rot,
                        scale = scale,
                        style = style,
                        text = text,
                        emoji = emoji,
                        color = color,
                        packId = rawPackId,
                        packCode = rawPackCode,
                        packIdx = rawPackIdx
                    )
                )
            }
        }

        return CardDesign(
            v = obj.optInt("v", 1),
            theme = theme,
            show = show,
            hiLabel = hiLabel,
            stickers = stickers
        )
    }

    fun fromJsonString(raw: String?): CardDesign? {
        if (raw.isNullOrBlank()) return null
        return runCatching { fromJsonObject(JSONObject(raw)) }.getOrNull()
    }
}

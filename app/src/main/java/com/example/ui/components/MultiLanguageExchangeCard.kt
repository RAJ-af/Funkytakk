package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.ProfileSwapIcon

data class LanguageItem(
    val name: String,
    val code: String,
    val flagCode: String
)

object LanguageResolver {
    fun resolveLanguage(raw: String): LanguageItem {
        val clean = raw.trim()
        val lower = clean.lowercase()
        return when {
            lower.contains("korean") || lower == "ko" || lower == "kr" -> LanguageItem("Korean", "KO", "kr")
            lower.contains("english") || lower == "en" || lower == "us" || lower == "uk" -> LanguageItem("English", "EN", "us")
            lower.contains("hindi") || lower == "hi" || lower == "in" -> LanguageItem("Hindi", "HI", "in")
            lower.contains("marathi") || lower == "mr" -> LanguageItem("Marathi", "MR", "in")
            lower.contains("chinese") || lower.contains("mandarin") || lower == "zh" || lower == "cn" -> LanguageItem("Chinese", "ZH", "cn")
            lower.contains("japanese") || lower == "ja" || lower == "jp" -> LanguageItem("Japanese", "JA", "jp")
            lower.contains("spanish") || lower == "es" -> LanguageItem("Spanish", "ES", "es")
            lower.contains("french") || lower == "fr" -> LanguageItem("French", "FR", "fr")
            lower.contains("german") || lower == "de" -> LanguageItem("German", "DE", "de")
            lower.contains("russian") || lower == "ru" -> LanguageItem("Russian", "RU", "ru")
            lower.contains("portuguese") || lower == "pt" || lower == "br" -> LanguageItem("Portuguese", "PT", "pt")
            lower.contains("italian") || lower == "it" -> LanguageItem("Italian", "IT", "it")
            lower.contains("arabic") || lower == "ar" -> LanguageItem("Arabic", "AR", "sa")
            lower.contains("bengali") || lower == "bn" -> LanguageItem("Bengali", "BN", "in")
            lower.contains("tamil") || lower == "ta" -> LanguageItem("Tamil", "TA", "in")
            lower.contains("telugu") || lower == "te" -> LanguageItem("Telugu", "TE", "in")
            lower.contains("turkish") || lower == "tr" -> LanguageItem("Turkish", "TR", "tr")
            lower.contains("vietnamese") || lower == "vi" || lower == "vn" -> LanguageItem("Vietnamese", "VI", "vn")
            lower.contains("indonesian") || lower == "id" -> LanguageItem("Indonesian", "ID", "id")
            lower.contains("thai") || lower == "th" -> LanguageItem("Thai", "TH", "th")
            else -> {
                val short = if (clean.length >= 2) clean.take(2).uppercase() else "GL"
                LanguageItem(clean.capitalize(), short, "us")
            }
        }
    }

    fun parseLanguages(raw: String?, defaultList: List<LanguageItem>): List<LanguageItem> {
        if (raw.isNullOrBlank()) return defaultList
        val split = raw.split(",", "/", "·", "|", ";")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        if (split.isEmpty()) return defaultList
        return split.take(3).map { resolveLanguage(it) }
    }

    fun parseLanguageList(list: List<String>?, defaultList: List<LanguageItem>): List<LanguageItem> {
        if (list.isNullOrEmpty()) return defaultList
        val flatList = list.flatMap { it.split(",", "/", "·", "|", ";") }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        if (flatList.isEmpty()) return defaultList
        return flatList.take(3).map { resolveLanguage(it) }
    }
}

/**
 * Overlapping Circular Avatar-style flags stack (up to 3 flags overlapping)
 */
@Composable
fun OverlappingFlagStack(
    languages: List<LanguageItem>,
    flagSize: Int = 24,
    overlapOffset: Int = -8,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        languages.take(3).forEachIndexed { index, lang ->
            Box(
                modifier = Modifier
                    .offset(x = (index * overlapOffset).dp)
                    .size(flagSize.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, Color.White, CircleShape)
            ) {
                CircleFlag(
                    countryCode = lang.flagCode,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

/**
 * Modern Multi-Language Exchange Pair Card with Overlapping Flags & Custom Swap Icon
 */
@Composable
fun MultiLanguageExchangeCard(
    nativeLanguages: List<LanguageItem>,
    learningLanguages: List<LanguageItem>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left Box: Native Languages (Soft Mint Tint)
        Box(
            modifier = Modifier
                .weight(1f)
                .background(Color(0xFFF0FDF4), RoundedCornerShape(16.dp))
                .border(1.dp, Color(0xFFDCFCE7), RoundedCornerShape(16.dp))
                .padding(12.dp)
        ) {
            Column {
                // "Native" Tag Pill
                Box(
                    modifier = Modifier
                        .background(Color(0xFFDCFCE7), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (nativeLanguages.size > 1) "Native (${nativeLanguages.size})" else "Native",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF16A34A)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Overlapping Flags Stack
                OverlappingFlagStack(
                    languages = nativeLanguages,
                    flagSize = 24,
                    overlapOffset = -8
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Language codes / names (e.g. "HI · EN · MR" or "Korean (KO)")
                val codeString = nativeLanguages.joinToString(" · ") { it.code }
                val fullNameString = nativeLanguages.joinToString(", ") { it.name }

                Text(
                    text = if (nativeLanguages.size == 1) "${nativeLanguages.first().name} (${nativeLanguages.first().code})" else codeString,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = if (nativeLanguages.size == 1) "Native / Fluent" else fullNameString,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF16A34A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Center: Custom Swap Arrow ⇄ with warm accent
        Box(
            modifier = Modifier
                .padding(horizontal = 6.dp)
                .size(32.dp)
                .background(Color(0xFFFFFBEB), CircleShape)
                .border(1.dp, Color(0xFFFDE68A), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            ProfileSwapIcon(modifier = Modifier.size(15.dp), color = Color(0xFF92400E))
        }

        // Right Box: Learning Languages (Soft Sky Tint)
        Box(
            modifier = Modifier
                .weight(1f)
                .background(Color(0xFFF0F9FF), RoundedCornerShape(16.dp))
                .border(1.dp, Color(0xFFE0F2FE), RoundedCornerShape(16.dp))
                .padding(12.dp)
        ) {
            Column {
                // "Learning" Tag Pill
                Box(
                    modifier = Modifier
                        .background(Color(0xFFE0F2FE), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (learningLanguages.size > 1) "Learning (${learningLanguages.size})" else "Learning",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0284C7)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Overlapping Flags Stack
                OverlappingFlagStack(
                    languages = learningLanguages,
                    flagSize = 24,
                    overlapOffset = -8
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Language codes / names (e.g. "ZH · KO · JA" or "English (EN)")
                val codeString = learningLanguages.joinToString(" · ") { it.code }
                val fullNameString = learningLanguages.joinToString(", ") { it.name }

                Text(
                    text = if (learningLanguages.size == 1) "${learningLanguages.first().name} (${learningLanguages.first().code})" else codeString,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = if (learningLanguages.size == 1) "Intermediate" else fullNameString,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0284C7),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

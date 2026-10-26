package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.ProfileBadge

/**
 * Beautiful small badge chip for displaying profile achievements and honors.
 * Designed with soft modern micro-interactions and dialog detail preview on click.
 */
@Composable
fun ProfileBadgeChip(
    badge: ProfileBadge,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
    showLabel: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    var showDetailDialog by remember { mutableStateOf(false) }

    val (badgeBgGradient, badgeBorderColor, iconTint) = remember(badge.code) {
        when (badge.code.lowercase()) {
            "founder" -> Triple(
                listOf(Color(0xFFFFFBEB), Color(0xFFFEF3C7)),
                Color(0xFFF59E0B),
                Color(0xFFD97706)
            )
            "early_adopter" -> Triple(
                listOf(Color(0xFFEFF6FF), Color(0xFFDBEAFE)),
                Color(0xFF3B82F6),
                Color(0xFF2563EB)
            )
            "popular" -> Triple(
                listOf(Color(0xFFFDF2F8), Color(0xFFFCE7F3)),
                Color(0xFFEC4899),
                Color(0xFFDB2777)
            )
            "generous" -> Triple(
                listOf(Color(0xFFECFDF5), Color(0xFFD1FAE5)),
                Color(0xFF10B981),
                Color(0xFF059669)
            )
            "vip" -> Triple(
                listOf(Color(0xFFF5F3FF), Color(0xFFEDE9FE)),
                Color(0xFF8B5CF6),
                Color(0xFF7C3AED)
            )
            else -> Triple(
                listOf(Color(0xFFF8FAFC), Color(0xFFF1F5F9)),
                Color(0xFF94A3B8),
                Color(0xFF64748B)
            )
        }
    }

    val shape = if (showLabel) RoundedCornerShape(100.dp) else CircleShape

    Box(
        modifier = modifier
            .clip(shape)
            .background(Brush.linearGradient(badgeBgGradient))
            .border(1.dp, badgeBorderColor.copy(alpha = 0.6f), shape)
            .clickable {
                if (onClick != null) onClick() else showDetailDialog = true
            }
            .padding(horizontal = if (showLabel) 8.dp else 2.dp, vertical = if (showLabel) 3.dp else 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (!badge.iconUrl.isNullOrBlank() && (badge.iconUrl.startsWith("http") || badge.iconUrl.startsWith("content"))) {
                AsyncImage(
                    model = badge.iconUrl,
                    contentDescription = badge.name,
                    modifier = Modifier
                        .size(size)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Soft canvas emblem based on badge code
                EmblemVectorBadge(badgeCode = badge.code, size = size, tint = iconTint)
            }

            if (showLabel) {
                Text(
                    text = badge.name,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = badgeBorderColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

    if (showDetailDialog) {
        BadgeDetailDialog(badge = badge, onDismiss = { showDetailDialog = false })
    }
}

/**
 * Renders a row of badges next to profile display name
 */
@Composable
fun ProfileBadgesRow(
    badges: List<ProfileBadge>,
    modifier: Modifier = Modifier,
    badgeSize: Dp = 20.dp,
    maxBadges: Int = 4
) {
    if (badges.isEmpty()) return

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        badges.take(maxBadges).forEach { badge ->
            ProfileBadgeChip(badge = badge, size = badgeSize)
        }
        if (badges.size > maxBadges) {
            val remaining = badges.size - maxBadges
            Box(
                modifier = Modifier
                    .size(badgeSize)
                    .clip(CircleShape)
                    .background(Color(0xFFE2E8F0)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+$remaining",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF475569)
                )
            }
        }
    }
}

/**
 * Built-in custom soft emblem graphics for standard badge codes
 */
@Composable
fun EmblemVectorBadge(
    badgeCode: String,
    size: Dp = 20.dp,
    tint: Color = Color(0xFFD97706)
) {
    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        when (badgeCode.lowercase()) {
            "founder" -> SoftStarIcon(size = size * 0.8f, color = tint)
            "early_adopter" -> SoftSparkleIcon(size = size * 0.8f, color = tint)
            "popular" -> SoftHeartIcon(size = size * 0.8f, color = tint)
            "generous" -> SoftSparkleIcon(size = size * 0.8f, color = tint)
            "vip" -> SoftStarIcon(size = size * 0.8f, color = tint)
            else -> SoftShieldIcon(size = size * 0.8f, color = tint)
        }
    }
}

/**
 * Interactive dialog showing the full description, honor details, and awarded date
 */
@Composable
fun BadgeDetailDialog(
    badge: ProfileBadge,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Badge Avatar
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFFBEB))
                        .border(2.dp, Color(0xFFFDE68A), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (!badge.iconUrl.isNullOrBlank() && badge.iconUrl.startsWith("http")) {
                        AsyncImage(
                            model = badge.iconUrl,
                            contentDescription = badge.name,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        EmblemVectorBadge(badgeCode = badge.code, size = 36.dp, tint = Color(0xFFD97706))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = badge.name,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0F172A),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = badge.description.ifBlank { "Exclusive FunkyTalk community badge awarded for notable achievement." },
                    fontSize = 13.sp,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                if (!badge.awardedAt.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFF8FAFC), RoundedCornerShape(100.dp))
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(100.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Awarded on ${badge.awardedAt.take(10)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(100.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFD43B),
                        contentColor = Color(0xFF0F172A)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Awesome!", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Tasteful pastel color generator for all language badges
 */
data class LanguageBadgeStyle(val bgColor: Color, val textColor: Color)

fun getLanguageBadgeStyle(languageName: String): LanguageBadgeStyle {
    val clean = languageName.substringBefore("(").trim().lowercase()
    return when (clean) {
        "japanese" -> LanguageBadgeStyle(Color(0xFFFFE4E6), Color(0xFFBE123C))
        "french" -> LanguageBadgeStyle(Color(0xFFE0F2FE), Color(0xFF0369A1))
        "german" -> LanguageBadgeStyle(Color(0xFFFEF3C7), Color(0xFFB45309))
        "russian" -> LanguageBadgeStyle(Color(0xFFF3E8FF), Color(0xFF7E22CE))
        "hindi" -> LanguageBadgeStyle(Color(0xFFFED7AA), Color(0xFFC2410C))
        "chinese" -> LanguageBadgeStyle(Color(0xFFFFEDD5), Color(0xFF9A3412))
        "spanish" -> LanguageBadgeStyle(Color(0xFFDCFCE7), Color(0xFF15803D))
        "english" -> LanguageBadgeStyle(Color(0xFFE0E7FF), Color(0xFF4338CA))
        "arabic" -> LanguageBadgeStyle(Color(0xFFCCFBF1), Color(0xFF0F766E))
        "korean" -> LanguageBadgeStyle(Color(0xFFFCE7F3), Color(0xFFBE185D))
        "italian" -> LanguageBadgeStyle(Color(0xFFD1FAE5), Color(0xFF047857))
        "portuguese" -> LanguageBadgeStyle(Color(0xFFFEF9C3), Color(0xFFA16207))
        else -> {
            val hash = kotlin.math.abs(clean.hashCode())
            val palettes = listOf(
                LanguageBadgeStyle(Color(0xFFE0F2FE), Color(0xFF0369A1)),
                LanguageBadgeStyle(Color(0xFFF3E8FF), Color(0xFF7E22CE)),
                LanguageBadgeStyle(Color(0xFFFED7AA), Color(0xFFC2410C)),
                LanguageBadgeStyle(Color(0xFFDCFCE7), Color(0xFF15803D)),
                LanguageBadgeStyle(Color(0xFFE0E7FF), Color(0xFF4338CA)),
                LanguageBadgeStyle(Color(0xFFCCFBF1), Color(0xFF0F766E)),
                LanguageBadgeStyle(Color(0xFFFCE7F3), Color(0xFFBE185D)),
                LanguageBadgeStyle(Color(0xFFFEF3C7), Color(0xFFB45309))
            )
            palettes[hash % palettes.size]
        }
    }
}

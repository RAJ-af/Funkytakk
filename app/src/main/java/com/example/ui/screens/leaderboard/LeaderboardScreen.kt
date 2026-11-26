package com.example.ui.screens.leaderboard

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.UserProfile
import com.example.leaderboard.LeaderboardCategory
import com.example.leaderboard.LeaderboardEntry
import com.example.leaderboard.LeaderboardRepository
import com.example.leaderboard.LiveLeaderboardSnapshot
import com.example.ui.components.PresenceAvatar
import com.example.ui.theme.LocalFunkyColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun LeaderboardScreen(
    supabaseProfiles: List<UserProfile>,
    onBack: () -> Unit,
    onOpenUserProfile: (LeaderboardEntry) -> Unit
) {
    val context = LocalContext.current
    val funkyColors = LocalFunkyColors.current
    val scope = rememberCoroutineScope()

    var selectedCategory by remember { mutableStateOf(LeaderboardCategory.VOICE_TIME) }
    var snapshot by remember { mutableStateOf<LiveLeaderboardSnapshot?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

    BackHandler(onBack = onBack)

    suspend fun reloadLeaderboard(showLoader: Boolean) {
        if (showLoader && snapshot == null) isLoading = true
        LeaderboardRepository.fetchLiveLeaderboard(
            context = context,
            cachedProfiles = supabaseProfiles
        ).onSuccess { fresh ->
            snapshot = fresh
            errorMessage = null
            isLoading = false
        }.onFailure { err ->
            errorMessage = err.message ?: "Could not load live leaderboard."
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        reloadLeaderboard(showLoader = true)
        while (isActive) {
            delay(15_000L)
            reloadLeaderboard(showLoader = false)
        }
    }

    // Live 1-second timer so Voice Room open durations tick in real time
    LaunchedEffect(selectedCategory) {
        while (isActive) {
            nowMs = System.currentTimeMillis()
            delay(1_000L)
        }
    }

    val rawEntries = remember(snapshot, selectedCategory, nowMs) {
        val snap = snapshot ?: return@remember emptyList()
        when (selectedCategory) {
            LeaderboardCategory.VOICE_TIME -> snap.voiceRoomEntries.map { entry ->
                val start = entry.roomStartedAtMs
                if (start != null) {
                    val liveSec = ((nowMs - start) / 1000L).coerceAtLeast(1L)
                    entry.copy(scoreLabel = LeaderboardRepository.formatLiveDuration(liveSec))
                } else entry
            }
            LeaderboardCategory.POPULAR -> snap.popularEntries
            LeaderboardCategory.MOST_LOGINS -> snap.mostLoginsEntries
        }
    }

    val firstEntry = rawEntries.getOrNull(0)
    val secondEntry = rawEntries.getOrNull(1)
    val thirdEntry = rawEntries.getOrNull(2)
    val restEntries = remember(rawEntries) {
        if (rawEntries.size > 3) rawEntries.drop(3) else emptyList()
    }

    val stageTopBg = if (funkyColors.isDark) Color(0xFF1E1711) else Color(0xFF2B2017)
    val stageBottomBg = if (funkyColors.isDark) Color(0xFF140F0B) else Color(0xFF1C140E)

    val livePulse by rememberInfiniteTransition(label = "livePulse").animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "liveDotAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(colors = listOf(stageTopBg, stageBottomBg))
            )
    ) {
        // Warm Mango stage spotlight behind #1 podium
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        funkyColors.mango.copy(alpha = 0.24f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.5f, size.height * 0.28f),
                    radius = size.width * 0.65f
                ),
                radius = size.width * 0.65f,
                center = Offset(size.width * 0.5f, size.height * 0.28f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top Bar: Back Button + Centered Title + Live Badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    onClick = onBack,
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("leaderboard_back_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back),
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Text(
                    text = "Leaderboard",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.4).sp
                )

                Surface(
                    onClick = { scope.launch { reloadLeaderboard(showLoader = true) } },
                    shape = RoundedCornerShape(99.dp),
                    color = Color(0xFF22C55E).copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, Color(0xFF22C55E).copy(alpha = 0.45f)),
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("leaderboard_live_refresh_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .alpha(livePulse)
                                .clip(CircleShape)
                                .background(Color(0xFF4ADE80))
                        )
                        Text(
                            text = "LIVE",
                            color = Color(0xFF86EFAC),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.6.sp
                        )
                    }
                }
            }

            // Live Category Pills (No Daily/Weekly/Monthly history — strictly live categories)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LeaderboardCategory.entries.forEach { category ->
                    val isSelected = selectedCategory == category
                    Surface(
                        onClick = { selectedCategory = category },
                        shape = RoundedCornerShape(99.dp),
                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.10f),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) Color.White else Color.White.copy(alpha = 0.20f)
                        ),
                        modifier = Modifier
                            .height(40.dp)
                            .testTag("leaderboard_tab_${category.name.lowercase()}")
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 18.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = category.label,
                                color = if (isSelected) Color(0xFF231910) else Color.White.copy(alpha = 0.90f),
                                fontSize = 13.5.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            when {
                isLoading && rawEntries.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = funkyColors.mango)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Syncing live rankings...",
                                color = Color.White.copy(alpha = 0.78f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                errorMessage != null && rawEntries.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = errorMessage ?: "Unable to load live leaderboard.",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = { scope.launch { reloadLeaderboard(showLoader = true) } },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = funkyColors.mango,
                                    contentColor = funkyColors.onMango
                                ),
                                shape = RoundedCornerShape(99.dp)
                            ) {
                                Text("Retry", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                rawEntries.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                shape = CircleShape,
                                color = funkyColors.mango.copy(alpha = 0.16f),
                                modifier = Modifier.size(64.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        painter = painterResource(
                                            id = if (selectedCategory == LeaderboardCategory.VOICE_TIME) R.drawable.ic_mic else R.drawable.ic_crown_filled
                                        ),
                                        contentDescription = null,
                                        tint = funkyColors.mango,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = if (selectedCategory == LeaderboardCategory.VOICE_TIME) {
                                    "No voice rooms are live right now"
                                } else {
                                    "No live entries yet"
                                },
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (selectedCategory == LeaderboardCategory.VOICE_TIME) {
                                    "Open a voice room to claim #1 on the live timer!"
                                } else {
                                    "Live rankings update automatically as users join."
                                },
                                color = Color.White.copy(alpha = 0.70f),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { scope.launch { reloadLeaderboard(showLoader = true) } },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = funkyColors.mango,
                                    contentColor = funkyColors.onMango
                                ),
                                shape = RoundedCornerShape(99.dp)
                            ) {
                                Text("Refresh Live", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                else -> {
                    // -----------------------------------------------------
                    // TOP 3 PODIUM STAGE (2nd left · 1st center · 3rd right)
                    // -----------------------------------------------------
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(top = 6.dp),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PodiumSlot(
                            rank = 2,
                            entry = secondEntry,
                            pedestalHeight = 86.dp,
                            ringColor = Color(0xFFD6DCE8),
                            badgeColor = Color(0xFFCBD5E1),
                            badgeTextColor = Color(0xFF1E293B),
                            isChampion = false,
                            onClick = { secondEntry?.let(onOpenUserProfile) },
                            modifier = Modifier.weight(1f)
                        )
                        PodiumSlot(
                            rank = 1,
                            entry = firstEntry,
                            pedestalHeight = 114.dp,
                            ringColor = funkyColors.mango,
                            badgeColor = funkyColors.mango,
                            badgeTextColor = funkyColors.onMango,
                            isChampion = true,
                            onClick = { firstEntry?.let(onOpenUserProfile) },
                            modifier = Modifier.weight(1.08f)
                        )
                        PodiumSlot(
                            rank = 3,
                            entry = thirdEntry,
                            pedestalHeight = 68.dp,
                            ringColor = Color(0xFFD97736),
                            badgeColor = Color(0xFFD97736),
                            badgeTextColor = Color.White,
                            isChampion = false,
                            onClick = { thirdEntry?.let(onOpenUserProfile) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // -----------------------------------------------------
                    // ROUNDED BOTTOM SHEET STARTING AT RANK 4+
                    // -----------------------------------------------------
                    Surface(
                        color = funkyColors.bg,
                        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                        shadowElevation = 10.dp,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .navigationBarsPadding()
                        ) {
                            // Drag handle bar (matches Reference 1)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp, bottom = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(42.dp)
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(99.dp))
                                        .background(funkyColors.line)
                                )
                            }

                            if (restEntries.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Top ${rawEntries.size} live leaders shown on the podium above.",
                                        color = funkyColors.mute,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(restEntries, key = { "${selectedCategory.name}_${it.rank}_${it.userId}" }) { entry ->
                                        LeaderboardListRow(
                                            entry = entry,
                                            category = selectedCategory,
                                            onClick = { onOpenUserProfile(entry) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PodiumSlot(
    rank: Int,
    entry: LeaderboardEntry?,
    pedestalHeight: Dp,
    ringColor: Color,
    badgeColor: Color,
    badgeTextColor: Color,
    isChampion: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val funkyColors = LocalFunkyColors.current
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (entry != null) {
            if (isChampion) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_crown_filled),
                    contentDescription = "First place crown",
                    tint = funkyColors.mango,
                    modifier = Modifier
                        .size(26.dp)
                        .offset(y = 4.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(14.dp))
            }

            Box(
                modifier = Modifier
                    .clickable(onClick = onClick)
                    .testTag("leaderboard_podium_rank_$rank"),
                contentAlignment = Alignment.TopEnd
            ) {
                Box(
                    modifier = Modifier
                        .size(if (isChampion) 76.dp else 64.dp)
                        .clip(CircleShape)
                        .border(2.5.dp, ringColor, CircleShape)
                        .padding(3.dp)
                ) {
                    PresenceAvatar(
                        avatarUrl = entry.avatarUrl.ifBlank { null },
                        size = if (isChampion) 70.dp else 58.dp,
                        userId = entry.userId,
                        enableFrameAnimation = true,
                        countryCode = entry.countryCode.takeIf { it.isNotBlank() },
                        isOnline = entry.isOnline
                    )
                }

                // Circular rank badge (#1, #2, #3)
                Box(
                    modifier = Modifier
                        .offset(x = 2.dp, y = (-2).dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(badgeColor)
                        .border(1.5.dp, Color(0xFF1F160F), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$rank",
                        color = badgeTextColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = entry.displayName,
                color = if (isChampion) funkyColors.mango else Color.White,
                fontSize = if (isChampion) 14.sp else 13.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(3.dp))

            Surface(
                shape = RoundedCornerShape(99.dp),
                color = Color.White.copy(alpha = 0.14f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (entry.isLiveNow) Color(0xFF4ADE80) else funkyColors.mango)
                    )
                    Text(
                        text = entry.scoreLabel,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        } else {
            Spacer(modifier = Modifier.height(106.dp))
        }

        // Architectural Podium Block with Big Rank Numeral (matches Reference 1)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(pedestalHeight)
                .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                .background(
                    Brush.verticalGradient(
                        colors = if (isChampion) {
                            listOf(
                                funkyColors.mango.copy(alpha = 0.42f),
                                Color(0xFF3A2B1D).copy(alpha = 0.70f)
                            )
                        } else {
                            listOf(
                                Color.White.copy(alpha = 0.24f),
                                Color.White.copy(alpha = 0.08f)
                            )
                        }
                    )
                )
                .border(
                    width = 1.dp,
                    color = if (isChampion) funkyColors.mango.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$rank",
                color = Color.White.copy(alpha = if (entry != null) 0.92f else 0.25f),
                fontSize = if (isChampion) 46.sp else 36.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun LeaderboardListRow(
    entry: LeaderboardEntry,
    category: LeaderboardCategory,
    onClick: () -> Unit
) {
    val funkyColors = LocalFunkyColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("leaderboard_row_${entry.rank}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Large rank numeral on left (Reference 1 signature look)
        Text(
            text = "${entry.rank}",
            color = funkyColors.mute.copy(alpha = 0.65f),
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(44.dp)
        )

        Spacer(modifier = Modifier.width(6.dp))

        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(20.dp),
            color = funkyColors.surface,
            border = BorderStroke(1.dp, funkyColors.line),
            shadowElevation = 1.dp,
            modifier = Modifier.weight(1f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PresenceAvatar(
                    avatarUrl = entry.avatarUrl.ifBlank { null },
                    size = 46.dp,
                    userId = entry.userId,
                    enableFrameAnimation = true,
                    countryCode = entry.countryCode.takeIf { it.isNotBlank() },
                    isOnline = entry.isOnline
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = entry.displayName,
                            color = funkyColors.ink,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (entry.isVerified) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_verified),
                                contentDescription = "Verified",
                                tint = funkyColors.mango,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Text(
                        text = entry.subtitle,
                        color = funkyColors.mute,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(99.dp),
                    color = if (category == LeaderboardCategory.VOICE_TIME) {
                        Color(0xFF22C55E).copy(alpha = 0.14f)
                    } else {
                        funkyColors.soft
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (entry.isLiveNow) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF22C55E))
                            )
                        } else if (category == LeaderboardCategory.POPULAR) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_star_filled),
                                contentDescription = null,
                                tint = funkyColors.mango,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        Text(
                            text = entry.scoreLabel,
                            color = if (category == LeaderboardCategory.VOICE_TIME) Color(0xFF16A34A) else funkyColors.ink,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}

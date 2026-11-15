package com.example.ui.screens.home.dialogs

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.AdminReportQueueItem
import com.example.ModerationManager
import com.example.ui.components.*
import com.example.ui.theme.LocalFunkyColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant

@Composable
fun AdminReportQueueView(
    onReportClick: (reportId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val funkyColors = LocalFunkyColors.current

    // 3 Tabs: "Free" (pending), "Taken" (in_review), "Mine" (mine)
    val tabs = remember { listOf("Free", "Taken", "Mine") }
    var selectedTabIdx by remember { mutableIntStateOf(0) }

    var queueItems by remember { mutableStateOf<List<AdminReportQueueItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var pageOffset by remember { mutableIntStateOf(0) }
    val pageLimit = 50

    val currentStatusParam = remember(selectedTabIdx) {
        when (selectedTabIdx) {
            0 -> "pending"
            1 -> "in_review"
            2 -> "mine"
            else -> "pending"
        }
    }

    fun loadQueue(resetOffset: Boolean = false) {
        if (resetOffset) pageOffset = 0
        isLoading = true
        errorMessage = null
        scope.launch(Dispatchers.IO) {
            val res = ModerationManager.fetchReportQueue(
                context = context,
                status = currentStatusParam,
                limit = pageLimit,
                offset = pageOffset
            )
            withContext(Dispatchers.Main) {
                isLoading = false
                if (res.isSuccess) {
                    val loaded = res.getOrNull() ?: emptyList()
                    queueItems = loaded
                    errorMessage = null
                    val ids = loaded.mapNotNull { it.reported_user_id }.filter { it.isNotBlank() }.distinct()
                    if (ids.isNotEmpty()) {
                        scope.launch {
                            com.example.store.CosmeticsManager.batchFetchUserCosmetics(context, ids)
                        }
                    }
                } else {
                    errorMessage = res.exceptionOrNull()?.message ?: "Failed to fetch reports."
                }
            }
        }
    }

    LaunchedEffect(selectedTabIdx, pageOffset) {
        loadQueue()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(funkyColors.bg)
    ) {
        // Tab row: Free, Taken, Mine
        Surface(
            color = funkyColors.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            TabRow(
                selectedTabIndex = selectedTabIdx,
                containerColor = funkyColors.surface,
                contentColor = funkyColors.mango
            ) {
                tabs.forEachIndexed { index, tabTitle ->
                    val isSelected = selectedTabIdx == index
                    Tab(
                        selected = isSelected,
                        onClick = {
                            if (selectedTabIdx != index) {
                                selectedTabIdx = index
                                pageOffset = 0
                            }
                        },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                when (index) {
                                    0 -> Soft3DRulesActionIcon(modifier = Modifier.size(16.dp))
                                    1 -> SoftShieldIcon(size = 16.dp, color = if (isSelected) funkyColors.mango else funkyColors.mute)
                                    2 -> SoftCheckIcon(size = 14.dp, color = if (isSelected) funkyColors.mango else funkyColors.mute)
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = tabTitle,
                                    color = if (isSelected) funkyColors.ink else funkyColors.mute,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    )
                }
            }
        }

        // Action / Refresh Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when (selectedTabIdx) {
                    0 -> "Free Reports (Unclaimed)"
                    1 -> "Taken Reports (In Review)"
                    2 -> "My Claimed Reports"
                    else -> "Reports"
                },
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = funkyColors.ink
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { loadQueue() },
                    modifier = Modifier.size(32.dp)
                ) {
                    SoftRefreshIcon(size = 15.dp, color = funkyColors.mute)
                }
            }
        }

        // Content Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (isLoading && queueItems.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = funkyColors.mango, modifier = Modifier.size(32.dp))
                }
            } else if (errorMessage != null && queueItems.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        SoftWarningIcon(size = 32.dp, color = funkyColors.danger)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = errorMessage ?: "Failed to load reports",
                            color = funkyColors.danger,
                            fontSize = 13.5.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { loadQueue() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = funkyColors.mango,
                                contentColor = funkyColors.onMango
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Retry", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else if (queueItems.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(funkyColors.ok.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            SoftCheckIcon(size = 28.dp, color = funkyColors.ok)
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = when (selectedTabIdx) {
                                0 -> "No Free Reports!"
                                1 -> "No Reports Currently Taken"
                                2 -> "You Have No Claimed Reports"
                                else -> "Queue Empty"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = funkyColors.ink
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = when (selectedTabIdx) {
                                0 -> "All reports are claimed or resolved."
                                1 -> "No other staff members are currently reviewing reports."
                                2 -> "Switch to Free tab to take charge of a report."
                                else -> "Everything is up to date."
                            },
                            fontSize = 12.5.sp,
                            color = funkyColors.mute
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(queueItems, key = { it.report_id }) { item ->
                        AdminReportQueueRow(
                            item = item,
                            onClick = { onReportClick(item.report_id) }
                        )
                    }

                    // Pagination footer
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (pageOffset > 0) {
                                TextButton(onClick = { pageOffset = maxOf(0, pageOffset - pageLimit) }) {
                                    Text("Previous", fontSize = 12.sp, color = funkyColors.mango)
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                            }
                            Text(
                                text = "Showing ${pageOffset + 1} - ${pageOffset + queueItems.size}",
                                fontSize = 11.5.sp,
                                color = funkyColors.mute
                            )
                            if (queueItems.size >= pageLimit) {
                                Spacer(modifier = Modifier.width(16.dp))
                                TextButton(onClick = { pageOffset += pageLimit }) {
                                    Text("Next", fontSize = 12.sp, color = funkyColors.mango)
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
fun AdminReportQueueRow(
    item: AdminReportQueueItem,
    onClick: () -> Unit
) {
    val funkyColors = LocalFunkyColors.current
    val contextType = item.context_type ?: "room"
    val reportedName = item.reported_name?.takeIf { it.isNotBlank() } ?: "User #${item.reported_user_id?.take(6) ?: "N/A"}"
    val category = item.category ?: "Violation"
    val score = item.reported_score ?: 100
    val isMine = item.is_mine == true
    val claimedByName = item.claimed_by_name?.takeIf { it.isNotBlank() }
    val claimExpiresAt = item.claim_expires_at

    // Countdown ticker for live claim
    var countdownText by remember(claimExpiresAt) {
        mutableStateOf(formatQueueCountdown(claimExpiresAt))
    }

    LaunchedEffect(claimExpiresAt) {
        if (!claimExpiresAt.isNullOrBlank()) {
            while (isActive) {
                countdownText = formatQueueCountdown(claimExpiresAt)
                delay(1000L)
            }
        }
    }

    val scoreBadgeColor = when {
        score >= 80 -> funkyColors.ok.copy(alpha = 0.16f)
        score >= 50 -> funkyColors.soft
        else -> funkyColors.danger.copy(alpha = 0.16f)
    }
    val scoreTextColor = when {
        score >= 80 -> funkyColors.ok
        score >= 50 -> funkyColors.mango
        else -> funkyColors.danger
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = funkyColors.surface,
        border = BorderStroke(1.dp, if (isMine) funkyColors.mango else funkyColors.line),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Row 1: Context Type Pill, Reopen Status, Safety Score Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val contextTint = when (contextType.lowercase()) {
                        "room" -> Color(0xFF3B82F6)
                        "dm" -> Color(0xFFA855F7)
                        "moment" -> Color(0xFFF97316)
                        else -> funkyColors.mute
                    }
                    Surface(
                        color = contextTint.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = contextType.uppercase(),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = contextTint,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                        )
                    }

                    if (!item.reopen_status.isNullOrBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = funkyColors.soft,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "REOPEN: ${item.reopen_status.uppercase()}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = funkyColors.mango,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Surface(
                    color = scoreBadgeColor,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Score: $score",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = scoreTextColor,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                    )
                }
            }

            // Row 2: Reported User & Category
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    com.example.store.DecoratedUserName(
                        userId = item.reported_user_id,
                        name = reportedName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        defaultColor = funkyColors.ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = category,
                        fontSize = 12.sp,
                        color = funkyColors.mute
                    )
                }

                // Pending count pill
                val pendingAgainst = item.reports_against_pending ?: 0L
                if (pendingAgainst > 1) {
                    Surface(
                        color = funkyColors.danger.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "$pendingAgainst reports against user",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = funkyColors.danger,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // Row 3: Reason snippet if available
            val reason = item.reason?.takeIf { it.isNotBlank() }
            if (reason != null) {
                Text(
                    text = reason,
                    fontSize = 12.sp,
                    color = funkyColors.mute,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )
            }

            HorizontalDivider(color = funkyColors.line)

            // Row 4: Claim status footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!claimedByName.isNullOrBlank()) {
                    Text(
                        text = if (isMine) "In your charge ($countdownText)" else "In charge: $claimedByName ($countdownText)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isMine) funkyColors.mango else funkyColors.mute
                    )
                } else {
                    Text(
                        text = "Status: Free to claim",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = funkyColors.ok
                    )
                }

                Text(
                    text = "View Details →",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = funkyColors.mango
                )
            }
        }
    }
}

private fun formatQueueCountdown(expiresAt: String?): String {
    if (expiresAt.isNullOrBlank()) return "Active"
    return try {
        val target = Instant.parse(expiresAt)
        val now = Instant.now()
        val duration = Duration.between(now, target)
        if (duration.isNegative || duration.isZero) {
            "Expired"
        } else {
            val hours = duration.toHours()
            val minutes = (duration.toMinutes() % 60)
            "${hours}h ${minutes}m"
        }
    } catch (_: Exception) {
        "Active"
    }
}

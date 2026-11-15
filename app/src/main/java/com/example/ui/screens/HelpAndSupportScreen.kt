package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.SupportChatManager
import com.example.repository.FeedbackRepository
import com.example.ui.components.FunkyChevronRightIcon
import com.example.ui.components.FunkyReportIcon
import com.example.ui.components.SoftBackIcon
import com.example.ui.components.SoftChatBubbleIcon
import com.example.ui.components.SoftHeadsetIcon
import com.example.ui.components.SoftInfoIcon
import com.example.ui.components.SoftStarIcon
import com.example.ui.theme.LocalFunkyColors

@Composable
fun HelpAndSupportScreen(
    onBack: () -> Unit,
    onOpenSupportChat: () -> Unit,
    onOpenBugReport: () -> Unit,
    onOpenFeedbackHub: () -> Unit
) {
    val context = LocalContext.current
    val funkyColors = LocalFunkyColors.current
    val isDark = funkyColors.isDark

    val supportUnread by SupportChatManager.unreadCount.collectAsState()
    val feedbackUpdates by FeedbackRepository.myUpdatesCount.collectAsState()

    BackHandler { onBack() }

    LaunchedEffect(Unit) {
        SupportChatManager.fetchSupportSession(context)
        FeedbackRepository.refreshMyUpdatesCount(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(funkyColors.bg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(funkyColors.soft)
                    .clickable { onBack() }
                    .semantics { contentDescription = "Back" }
                    .testTag("help_support_back_button"),
                contentAlignment = Alignment.Center
            ) {
                SoftBackIcon(color = funkyColors.ink, size = 20.dp)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Help & Support",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = funkyColors.ink
                )
                Text(
                    text = "24/7 support, bugs & ideas",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = funkyColors.mute
                )
            }

            Spacer(modifier = Modifier.size(48.dp))
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Hero prompt banner
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = funkyColors.soft,
                border = BorderStroke(1.dp, funkyColors.mango.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(funkyColors.mango),
                        contentAlignment = Alignment.Center
                    ) {
                        SoftHeadsetIcon(size = 26.dp, color = funkyColors.onMango)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "How can we help?",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = funkyColors.ink
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "Message the team anytime, report an issue privately, or vote on upcoming features.",
                            fontSize = 12.5.sp,
                            color = funkyColors.mute,
                            lineHeight = 17.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Row 1: Chat with support
            HelpActionCard(
                title = "Chat with support",
                subtitle = "24/7 direct conversation with the FunkyTalk team",
                badgeCount = supportUnread,
                iconBg = if (isDark) Color(0xFF0EA5E9).copy(alpha = 0.18f) else Color(0xFFE0F2FE),
                iconContent = {
                    SoftChatBubbleIcon(
                        size = 22.dp,
                        color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
                    )
                },
                onClick = onOpenSupportChat,
                modifier = Modifier.testTag("help_chat_support_row")
            )

            // Row 2: Report a bug
            HelpActionCard(
                title = "Report a bug",
                subtitle = "Private report with device details automatically attached",
                badgeCount = 0,
                iconBg = if (isDark) Color(0xFFEF4444).copy(alpha = 0.18f) else Color(0xFFFEE2E2),
                iconContent = {
                    FunkyReportIcon(
                        modifier = Modifier.size(22.dp),
                        color = if (isDark) Color(0xFFF87171) else Color(0xFFDC2626)
                    )
                },
                onClick = onOpenBugReport,
                modifier = Modifier.testTag("help_report_bug_row")
            )

            // Row 3: Request a feature
            HelpActionCard(
                title = "Request a feature",
                subtitle = "Browse community ideas, upvote, and track your reports",
                badgeCount = feedbackUpdates,
                iconBg = if (isDark) funkyColors.mango.copy(alpha = 0.2f) else Color(0xFFFEF3C7),
                iconContent = {
                    SoftStarIcon(
                        size = 22.dp,
                        color = if (isDark) funkyColors.mango else Color(0xFFD97706)
                    )
                },
                onClick = onOpenFeedbackHub,
                modifier = Modifier.testTag("help_feedback_hub_row")
            )

            Spacer(modifier = Modifier.height(6.dp))

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = funkyColors.surface,
                border = BorderStroke(1.dp, funkyColors.line),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SoftInfoIcon(size = 18.dp, color = funkyColors.mute)
                    Text(
                        text = "Bug reports stay private between you and FunkyTalk staff. Feature requests are visible on the community board so others can vote.",
                        fontSize = 12.sp,
                        color = funkyColors.mute,
                        lineHeight = 17.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun HelpActionCard(
    title: String,
    subtitle: String,
    badgeCount: Int,
    iconBg: Color,
    iconContent: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val funkyColors = LocalFunkyColors.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        color = funkyColors.surface,
        border = BorderStroke(1.dp, funkyColors.line),
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 72.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                iconContent()
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = funkyColors.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = funkyColors.mute,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )
            }

            if (badgeCount > 0) {
                Box(
                    modifier = Modifier
                        .defaultMinSize(minWidth = 22.dp, minHeight = 22.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            FunkyChevronRightIcon(
                color = funkyColors.mute.copy(alpha = 0.75f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

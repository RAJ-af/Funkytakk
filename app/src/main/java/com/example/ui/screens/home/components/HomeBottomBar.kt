package com.example.ui.screens.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.*
import com.example.ui.theme.LocalAppDarkTheme
import com.example.ui.theme.LocalFunkyColors

@Composable
fun HomeBottomBar(
    activeTab: String,
    unreadChatCount: Int = 0,
    profileBadgeCount: Int = 0,
    onTabSelected: (String) -> Unit
) {
    val isMomentsTab = activeTab == "moments"
    val isAppDark = LocalAppDarkTheme.current
    val funkyColors = LocalFunkyColors.current

    val barContainerColor = if (isAppDark) funkyColors.surface else Color(0xFFFFFDF8)
    val activeIconColor = if (isAppDark) funkyColors.mango else Color(0xFF1F1607)
    val inactiveIconColor = if (isAppDark) funkyColors.mute else Color(0xFF7B6F5B)
    val activePillColor = if (isAppDark) funkyColors.soft else Color(0xFFFFE58F)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = barContainerColor,
        tonalElevation = 0.dp,
        shadowElevation = if (isAppDark) 2.dp else 8.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(funkyColors.line)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Home
                BottomNavItem(
                    label = "Home",
                    isSelected = activeTab == "home",
                    activePillColor = activePillColor,
                    onClick = { onTabSelected("home") },
                    icon = {
                        FunkyHomeIcon(
                            color = if (activeTab == "home") activeIconColor else inactiveIconColor,
                            modifier = Modifier.size(25.dp)
                        )
                    }
                )

                // 2. Discover
                val isDiscoverSelected = activeTab == "discover" || activeTab == "rooms"
                BottomNavItem(
                    label = "Discover",
                    isSelected = isDiscoverSelected,
                    activePillColor = activePillColor,
                    onClick = { onTabSelected("discover") },
                    icon = {
                        FunkyDiscoverIcon(
                            color = if (isDiscoverSelected) activeIconColor else inactiveIconColor,
                            isFilled = isDiscoverSelected,
                            modifier = Modifier.size(25.dp)
                        )
                    }
                )

                // 3. Moments
                BottomNavItem(
                    label = "Moments",
                    isSelected = isMomentsTab,
                    activePillColor = activePillColor,
                    onClick = { onTabSelected("moments") },
                    icon = {
                        FunkyGlobeIcon(
                            color = if (isMomentsTab) activeIconColor else inactiveIconColor,
                            modifier = Modifier.size(25.dp)
                        )
                    }
                )

                // 4. Chats (real unread count; hidden when 0)
                BottomNavItem(
                    label = "Chats",
                    isSelected = activeTab == "chats",
                    activePillColor = activePillColor,
                    onClick = { onTabSelected("chats") },
                    badgeCount = unreadChatCount.coerceAtLeast(0),
                    badgeBorderColor = barContainerColor,
                    icon = {
                        FunkyChatIcon(
                            color = if (activeTab == "chats") activeIconColor else inactiveIconColor,
                            isFilled = activeTab == "chats",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                )

                // 5. Profile
                BottomNavItem(
                    label = "Profile",
                    isSelected = activeTab == "profile",
                    activePillColor = activePillColor,
                    onClick = { onTabSelected("profile") },
                    badgeCount = profileBadgeCount.coerceAtLeast(0),
                    badgeBorderColor = barContainerColor,
                    icon = {
                        FunkyProfileIcon(
                            color = if (activeTab == "profile") activeIconColor else inactiveIconColor,
                            modifier = Modifier.size(25.dp)
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun BottomNavItem(
    label: String,
    isSelected: Boolean,
    activePillColor: Color,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    badgeCount: Int = 0,
    badgeBorderColor: Color = Color.White
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .defaultMinSize(minWidth = 54.dp, minHeight = 48.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .then(
                    if (isSelected) {
                        Modifier
                            .background(activePillColor)
                            .padding(horizontal = 18.dp, vertical = 9.dp)
                    } else {
                        Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            icon()

            if (badgeCount > 0) {
                val displayBadge = if (badgeCount > 99) "99+" else badgeCount.toString()
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 10.dp, y = (-6).dp)
                        .defaultMinSize(minWidth = 17.dp, minHeight = 17.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444))
                        .border(1.5.dp, badgeBorderColor, CircleShape)
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = displayBadge,
                        color = Color.White,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

package com.example.ui.screens.home.components

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.*
import com.example.ui.theme.GoldAccent

@Composable
fun HomeBottomBar(
    activeTab: String,
    onTabSelected: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = Color(0xFFFCFBF7),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Home
            BottomNavItem(
                label = "Home",
                isSelected = activeTab == "home",
                onClick = { onTabSelected("home") },
                icon = {
                    FunkyHomeIcon(
                        color = if (activeTab == "home") Color.Black else Color(0xFF6B7280),
                        modifier = Modifier.size(24.dp)
                    )
                }
            )

            // 2. Rooms
            BottomNavItem(
                label = "Rooms",
                isSelected = activeTab == "rooms",
                onClick = { onTabSelected("rooms") },
                icon = {
                    FunkyMicIcon(
                        color = if (activeTab == "rooms") Color.Black else Color(0xFF6B7280),
                        modifier = Modifier.size(24.dp)
                    )
                }
            )

            // 3. Moments
            BottomNavItem(
                label = "Moments",
                isSelected = activeTab == "moments",
                onClick = { onTabSelected("moments") },
                icon = {
                    FunkyGlobeIcon(
                        color = if (activeTab == "moments") Color.Black else Color(0xFF6B7280),
                        modifier = Modifier.size(24.dp)
                    )
                }
            )

            // 4. Chats (Active Pill style matching the design mockup)
            BottomNavItem(
                label = "Chats",
                isSelected = activeTab == "chats",
                onClick = { onTabSelected("chats") },
                badgeCount = 3,
                usePillBackground = true,
                icon = {
                    FunkyChatIcon(
                        color = Color.Black,
                        isFilled = activeTab == "chats",
                        modifier = Modifier.size(22.dp)
                    )
                }
            )

            // 5. Profile
            BottomNavItem(
                label = "Profile",
                isSelected = activeTab == "profile",
                onClick = { onTabSelected("profile") },
                icon = {
                    FunkyProfileIcon(
                        color = if (activeTab == "profile") Color.Black else Color(0xFF6B7280),
                        modifier = Modifier.size(24.dp)
                    )
                }
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    badgeCount: Int = 0,
    usePillBackground: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = Modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .height(32.dp)
                .then(
                    if (isSelected && usePillBackground) {
                        Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFFFFE380))
                            .padding(horizontal = 18.dp, vertical = 4.dp)
                    } else {
                        Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            icon()

            if (badgeCount > 0) {
                Box(
                    modifier = Modifier
                        .offset(x = 10.dp, y = (-8).dp)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(GoldAccent),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$badgeCount",
                        color = Color.Black,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.Black else Color(0xFF6B7280)
        )
    }
}

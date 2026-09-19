package com.example.ui.screens.home.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.ChatBubble
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GoldAccent

@Composable
fun HomeBottomBar(
    activeTab: String,
    onTabSelected: (String) -> Unit
) {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            icon = { Icon(imageVector = if (activeTab == "home") Icons.Filled.Home else Icons.Outlined.Home, contentDescription = "Home", modifier = Modifier.size(24.dp)) },
            label = { Text("Home", fontWeight = if (activeTab == "home") FontWeight.Bold else FontWeight.Medium, fontSize = 11.sp) },
            selected = activeTab == "home",
            onClick = { onTabSelected("home") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Black,
                selectedTextColor = Color.Black,
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray,
                indicatorColor = GoldAccent.copy(alpha = 0.5f)
            )
        )
        NavigationBarItem(
            icon = { Icon(imageVector = if (activeTab == "rooms") Icons.Filled.Mic else Icons.Outlined.Mic, contentDescription = "Rooms", modifier = Modifier.size(24.dp)) },
            label = { Text("Rooms", fontWeight = if (activeTab == "rooms") FontWeight.Bold else FontWeight.Medium, fontSize = 11.sp) },
            selected = activeTab == "rooms",
            onClick = { onTabSelected("rooms") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Black,
                selectedTextColor = Color.Black,
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray,
                indicatorColor = GoldAccent.copy(alpha = 0.5f)
            )
        )
        NavigationBarItem(
            icon = { Icon(imageVector = if (activeTab == "moments") Icons.Filled.Language else Icons.Outlined.Language, contentDescription = "Moments", modifier = Modifier.size(24.dp)) },
            label = { Text("Moments", fontWeight = if (activeTab == "moments") FontWeight.Bold else FontWeight.Medium, fontSize = 11.sp) },
            selected = activeTab == "moments",
            onClick = { onTabSelected("moments") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Black,
                selectedTextColor = Color.Black,
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray,
                indicatorColor = GoldAccent.copy(alpha = 0.5f)
            )
        )
        NavigationBarItem(
            icon = {
                BadgedBox(
                    badge = {
                        Badge(containerColor = GoldAccent) {
                            Text("3", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        }
                    }
                ) {
                    Icon(imageVector = if (activeTab == "chats") Icons.Filled.ChatBubble else Icons.Outlined.ChatBubble, contentDescription = "Chats", modifier = Modifier.size(24.dp))
                }
            },
            label = { Text("Chats", fontWeight = if (activeTab == "chats") FontWeight.Bold else FontWeight.Medium, fontSize = 11.sp) },
            selected = activeTab == "chats",
            onClick = { onTabSelected("chats") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Black,
                selectedTextColor = Color.Black,
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray,
                indicatorColor = GoldAccent.copy(alpha = 0.5f)
            )
        )
        NavigationBarItem(
            icon = { Icon(imageVector = if (activeTab == "profile") Icons.Filled.Person else Icons.Outlined.Person, contentDescription = "Profile", modifier = Modifier.size(24.dp)) },
            label = { Text("Profile", fontWeight = if (activeTab == "profile") FontWeight.Bold else FontWeight.Medium, fontSize = 11.sp) },
            selected = activeTab == "profile",
            onClick = { onTabSelected("profile") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Black,
                selectedTextColor = Color.Black,
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray,
                indicatorColor = GoldAccent.copy(alpha = 0.5f)
            )
        )
    }
}

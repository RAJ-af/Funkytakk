package com.example.ui.screens.home.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.FunkyMicIcon
import com.example.ui.theme.LocalFunkyColors

@Composable
fun RoomsTabContent(
    onCreateRoom: () -> Unit
) {
    val funkyColors = LocalFunkyColors.current
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TabRow(
            selectedTabIndex = 0,
            containerColor = funkyColors.surface,
            contentColor = funkyColors.ink
        ) {
            Tab(selected = true, onClick = {}, text = { Text("Active Conversations", fontWeight = FontWeight.Bold, color = funkyColors.ink) })
            Tab(selected = false, onClick = {}, text = { Text("Scheduled Audio", color = funkyColors.mute) })
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                FunkyMicIcon(
                    color = funkyColors.mango,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Ready to start practicing speaking?",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = funkyColors.ink
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Browse the rooms list or click the action below to create your own instant voice lounge.",
                    textAlign = TextAlign.Center,
                    color = funkyColors.mute,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onCreateRoom,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = funkyColors.mango,
                        contentColor = funkyColors.onMango
                    )
                ) {
                    Text("Start Voice Lounge", color = funkyColors.onMango, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

package com.example.ui.screens.home.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GoldAccent

@Composable
fun RoomsTabContent(
    onCreateRoom: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TabRow(
            selectedTabIndex = 0,
            containerColor = Color.White
        ) {
            Tab(selected = true, onClick = {}, text = { Text("Active Conversations", fontWeight = FontWeight.Bold) })
            Tab(selected = false, onClick = {}, text = { Text("Scheduled Audio") })
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
                Icon(
                    imageVector = Icons.Default.Podcasts,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = GoldAccent
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Ready to start practicing speaking?", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Browse the rooms list or click the action below to create your own instant voice lounge.",
                    textAlign = TextAlign.Center,
                    color = Color.Gray,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onCreateRoom,
                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)
                ) {
                    Text("Start Voice Lounge", color = Color.Black, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

package com.example.ui.screens.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.FunkyBookIcon
import com.example.ui.components.FunkyLightbulbIcon
import com.example.ui.components.FunkyTranslateIcon

@Composable
fun ChatActionPillsBar(
    onTranslateClick: () -> Unit,
    onImproveTextClick: () -> Unit,
    onExampleReplyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Pill 1: Translate
        ActionPillItem(
            icon = { FunkyTranslateIcon(color = Color.Black, modifier = Modifier.size(16.dp)) },
            label = "Translate",
            onClick = onTranslateClick
        )

        // Pill 2: Improve my text
        ActionPillItem(
            icon = { FunkyLightbulbIcon(color = Color.Black, modifier = Modifier.size(16.dp)) },
            label = "Improve my text",
            onClick = onImproveTextClick
        )

        // Pill 3: Example reply
        ActionPillItem(
            icon = { FunkyBookIcon(color = Color.Black, modifier = Modifier.size(16.dp)) },
            label = "Example reply",
            onClick = onExampleReplyClick
        )
    }
}

@Composable
private fun ActionPillItem(
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF1F2937)
        )
    }
}

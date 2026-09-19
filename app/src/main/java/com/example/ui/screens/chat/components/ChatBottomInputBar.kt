package com.example.ui.screens.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.FunkyMicIcon
import com.example.ui.components.FunkyPlusIcon
import com.example.ui.components.FunkySendIcon
import com.example.ui.components.FunkySmileyIcon

@Composable
fun ChatBottomInputBar(
    messageText: String,
    onMessageChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onPlusClick: () -> Unit,
    onSmileyClick: () -> Unit,
    onMicClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Circular Plus Button
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(Color(0xFFF3F4F6))
                .clickable { onPlusClick() },
            contentAlignment = Alignment.Center
        ) {
            FunkyPlusIcon(color = Color.Black, modifier = Modifier.size(20.dp))
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Middle Input Pill
        Row(
            modifier = Modifier
                .weight(1f)
                .height(46.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFFF3F4F6))
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Text Input
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (messageText.isEmpty()) {
                    Text(
                        text = "Type a message...",
                        fontSize = 14.5.sp,
                        color = Color(0xFF9CA3AF)
                    )
                }

                BasicTextField(
                    value = messageText,
                    onValueChange = onMessageChange,
                    textStyle = TextStyle(
                        fontSize = 14.5.sp,
                        color = Color.Black
                    ),
                    cursorBrush = SolidColor(Color(0xFFF59E0B)),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (messageText.isNotBlank()) {
                            onSendMessage()
                        }
                    }),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Smiley Icon
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable { onSmileyClick() },
                contentAlignment = Alignment.Center
            ) {
                FunkySmileyIcon(color = Color(0xFF374151), modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Microphone Icon
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable { onMicClick() },
                contentAlignment = Alignment.Center
            ) {
                FunkyMicIcon(color = Color(0xFF374151), modifier = Modifier.size(20.dp))
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Right Circular Send Button (Yellow)
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(Color(0xFFFBBF24))
                .clickable {
                    if (messageText.isNotBlank()) {
                        onSendMessage()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            FunkySendIcon(color = Color.Black, modifier = Modifier.size(18.dp))
        }
    }
}

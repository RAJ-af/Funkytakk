package com.example.ui.screens.home.dialogs

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.repository.BlockRepository
import com.example.ui.components.*
import com.example.ui.theme.LocalFunkyColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockedUsersBottomSheet(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val funkyColors = LocalFunkyColors.current
    val blockedUsers by BlockRepository.blockedUsers.collectAsState()
    val isLoading by BlockRepository.isLoading.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var unblockingUserId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        BlockRepository.refreshBlockedUsers(context)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = funkyColors.surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = funkyColors.line) },
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(funkyColors.danger.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        SoftBlockIcon(size = 20.dp, color = funkyColors.danger)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Blocked Users",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = funkyColors.ink
                        )
                        Text(
                            text = "${blockedUsers.size} user(s) blocked",
                            fontSize = 12.sp,
                            color = funkyColors.mute
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(funkyColors.soft)
                ) {
                    SoftCloseIcon(size = 14.dp, color = funkyColors.ink)
                }
            }

            HorizontalDivider(color = funkyColors.line, thickness = 1.dp)

            Spacer(modifier = Modifier.height(12.dp))

            if (isLoading && blockedUsers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = funkyColors.mango, modifier = Modifier.size(28.dp))
                }
            } else if (blockedUsers.isEmpty()) {
                // Empty state
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(funkyColors.ok.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        SoftShieldIcon(size = 36.dp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "You haven't blocked anyone.",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = funkyColors.ink,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Blocked users will appear here and can be unblocked anytime.",
                        fontSize = 13.sp,
                        color = funkyColors.mute,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    items(blockedUsers, key = { it.user_id }) { user ->
                        val displayName = user.profile_name?.takeIf { it.isNotBlank() }
                            ?: user.username?.takeIf { it.isNotBlank() }
                            ?: "User (${user.user_id.take(8)})"
                        val handleText = user.username?.takeIf { it.isNotBlank() }?.let { "@$it" }
                            ?: "Blocked from chatting & calls"
                        val isUnblockingThis = unblockingUserId == user.user_id

                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = funkyColors.bg,
                            border = BorderStroke(1.dp, funkyColors.line),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                UserAvatarImage(
                                    avatarUrl = user.avatar_url.orEmpty(),
                                    contentDescription = displayName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = displayName,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = funkyColors.ink,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = handleText,
                                        fontSize = 11.5.sp,
                                        color = funkyColors.mute,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Button(
                                    enabled = !isUnblockingThis,
                                    onClick = {
                                        unblockingUserId = user.user_id
                                        scope.launch {
                                            val res = BlockRepository.unblockUser(context, user.user_id)
                                            unblockingUserId = null
                                            res.onSuccess {
                                                Toast.makeText(context, "$displayName unblocked", Toast.LENGTH_SHORT).show()
                                            }.onFailure { err ->
                                                Toast.makeText(context, err.message ?: "Could not unblock user.", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(50.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = funkyColors.danger.copy(alpha = 0.14f),
                                        contentColor = funkyColors.danger
                                    ),
                                    border = BorderStroke(1.dp, funkyColors.danger.copy(alpha = 0.3f)),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                                    elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp)
                                ) {
                                    Text(
                                        text = if (isUnblockingThis) "..." else "Unblock",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

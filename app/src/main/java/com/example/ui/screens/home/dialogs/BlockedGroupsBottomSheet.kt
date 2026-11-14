package com.example.ui.screens.home.dialogs

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.BlockedGroupRow
import com.example.repository.GroupChatRepository
import com.example.ui.components.GroupBlockBanIcon
import com.example.ui.components.GroupUsersIcon
import com.example.ui.theme.LocalFunkyColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockedGroupsBottomSheet(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val funkyColors = LocalFunkyColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var isLoading by remember { mutableStateOf(true) }
    var blockedGroups by remember { mutableStateOf<List<BlockedGroupRow>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var unblockingIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    fun loadBlockedGroups() {
        scope.launch {
            isLoading = true
            errorMessage = null
            val res = GroupChatRepository.getMyBlockedGroups(context)
            isLoading = false
            res.onSuccess { list ->
                blockedGroups = list
            }.onFailure { err ->
                errorMessage = err.message ?: "Failed to load blocked groups"
            }
        }
    }

    LaunchedEffect(Unit) {
        loadBlockedGroups()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = funkyColors.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(funkyColors.line)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(funkyColors.danger.copy(alpha = 0.14f))
                        .border(1.dp, funkyColors.danger.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    GroupBlockBanIcon(size = 20.dp, color = funkyColors.danger)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Blocked groups",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = funkyColors.ink
                    )
                    Text(
                        text = "Groups you have blocked cannot re-invite you until unblocked",
                        fontSize = 12.5.sp,
                        color = funkyColors.mute
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = funkyColors.mango,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                errorMessage != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = errorMessage ?: "Failed to load blocked groups",
                            fontSize = 13.5.sp,
                            color = funkyColors.danger,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        TextButton(onClick = { loadBlockedGroups() }) {
                            Text("Retry", fontWeight = FontWeight.Bold, color = funkyColors.mango)
                        }
                    }
                }
                blockedGroups.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 36.dp)
                            .testTag("blocked_groups_empty_state"),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(funkyColors.bg)
                                .border(1.dp, funkyColors.line, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            GroupUsersIcon(size = 28.dp, color = funkyColors.mute)
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "No blocked groups",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = funkyColors.ink
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Any groups you block will appear here.",
                            fontSize = 13.sp,
                            color = funkyColors.mute,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 380.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(blockedGroups, key = { it.group_id }) { item ->
                            val isUnblocking = unblockingIds.contains(item.group_id)
                            val groupTitle = item.name.ifBlank { "Group (${item.group_id.take(6)})" }
                            val blockedDateLabel = remember(item.blocked_at) {
                                item.blocked_at?.take(10)?.let { "Blocked on $it" } ?: "Blocked group"
                            }

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                color = funkyColors.bg,
                                border = BorderStroke(1.dp, funkyColors.line)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(funkyColors.soft),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (!item.avatar_url.isNullOrBlank()) {
                                                AsyncImage(
                                                    model = item.avatar_url,
                                                    contentDescription = groupTitle,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .clip(CircleShape)
                                                )
                                            } else {
                                                Text(
                                                    text = groupTitle.take(1).uppercase(),
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = funkyColors.mango
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = groupTitle,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = funkyColors.ink,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = blockedDateLabel,
                                                fontSize = 12.sp,
                                                color = funkyColors.mute
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    OutlinedButton(
                                        onClick = {
                                            scope.launch {
                                                unblockingIds = unblockingIds + item.group_id
                                                val res = GroupChatRepository.unblockGroup(context, item.group_id)
                                                unblockingIds = unblockingIds - item.group_id
                                                res.onSuccess {
                                                    blockedGroups = blockedGroups.filterNot { it.group_id == item.group_id }
                                                    Toast.makeText(
                                                        context,
                                                        "Unblocked \"$groupTitle\". An admin must invite you again to rejoin.",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                }.onFailure { err ->
                                                    Toast.makeText(
                                                        context,
                                                        err.message ?: "Failed to unblock group",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                        },
                                        enabled = !isUnblocking,
                                        shape = RoundedCornerShape(50.dp),
                                        border = BorderStroke(1.dp, funkyColors.line),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = funkyColors.surface,
                                            contentColor = funkyColors.ink
                                        ),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                        modifier = Modifier
                                            .height(36.dp)
                                            .testTag("unblock_group_button_${item.group_id}")
                                    ) {
                                        if (isUnblocking) {
                                            CircularProgressIndicator(
                                                color = funkyColors.ink,
                                                strokeWidth = 2.dp,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        } else {
                                            Text(
                                                text = "Unblock",
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

package com.example.ui.screens.chat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.db.LocalGroupConversation

enum class GroupScreenRoute {
    CHAT,
    INFO,
    MANAGE_MEMBERS,
    SETTINGS
}

/**
 * Full-screen container that manages seamless navigation between:
 * 1. GroupChatScreen (chat & 3-dots actions)
 * 2. GroupInfoScreen (group header, about, quick settings, member preview, media)
 * 3. ManageMembersScreen (filter pills, search, member actions)
 * 4. GroupSettingsScreen (permissions, notifications, danger zone)
 *
 * Rendered at the root level so NO bottom navigation bar appears!
 */
@Composable
fun GroupFlowContainer(
    initialGroup: LocalGroupConversation,
    onCloseGroup: () -> Unit
) {
    var currentRoute by remember { mutableStateOf(GroupScreenRoute.CHAT) }
    var currentGroup by remember { mutableStateOf(initialGroup) }
    val isAdmin = currentGroup.myRole.equals("admin", ignoreCase = true)

    Box(modifier = Modifier.fillMaxSize()) {
        when (currentRoute) {
            GroupScreenRoute.CHAT -> {
                GroupChatScreen(
                    group = currentGroup,
                    onBack = onCloseGroup,
                    onNavigateToInfo = { currentRoute = GroupScreenRoute.INFO }
                )
            }
            GroupScreenRoute.INFO -> {
                GroupInfoScreen(
                    group = currentGroup,
                    isAdmin = isAdmin,
                    onBack = { currentRoute = GroupScreenRoute.CHAT },
                    onNavigateToManageMembers = { currentRoute = GroupScreenRoute.MANAGE_MEMBERS },
                    onNavigateToGroupSettings = { currentRoute = GroupScreenRoute.SETTINGS },
                    onAddMemberClick = { currentRoute = GroupScreenRoute.MANAGE_MEMBERS },
                    onLeaveGroupClick = { onCloseGroup() }
                )
            }
            GroupScreenRoute.MANAGE_MEMBERS -> {
                ManageMembersScreen(
                    group = currentGroup,
                    isAdmin = isAdmin,
                    onBack = { currentRoute = GroupScreenRoute.INFO },
                    onAddMemberClick = { currentRoute = GroupScreenRoute.INFO }
                )
            }
            GroupScreenRoute.SETTINGS -> {
                GroupSettingsScreen(
                    group = currentGroup,
                    isAdmin = isAdmin,
                    onBack = { currentRoute = GroupScreenRoute.INFO },
                    onLeaveGroupConfirmed = { onCloseGroup() }
                )
            }
        }
    }
}

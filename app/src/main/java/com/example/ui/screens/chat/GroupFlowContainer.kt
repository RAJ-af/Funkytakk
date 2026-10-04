package com.example.ui.screens.chat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.db.LocalGroupConversation
import com.example.repository.GroupChatRepository
import com.example.realtime.VoiceRoomRealtimeManager

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
 * 3. ManageMembersScreen (filter pills, search, member actions, add member)
 * 4. GroupSettingsScreen (permissions, notifications, danger zone)
 *
 * Rendered at the root level so NO bottom navigation bar appears!
 */
@Composable
fun GroupFlowContainer(
    initialGroup: LocalGroupConversation,
    onCloseGroup: () -> Unit
) {
    val context = LocalContext.current
    var currentRoute by remember { mutableStateOf(GroupScreenRoute.CHAT) }
    var openAddMemberOnEnter by remember { mutableStateOf(false) }

    val liveGroupFlow = remember(initialGroup.id) {
        GroupChatRepository.getGroupConversationFlow(initialGroup.id)
    }
    val observedGroup by liveGroupFlow.collectAsState(initial = initialGroup)
    val currentGroup = observedGroup ?: initialGroup

    LaunchedEffect(initialGroup.id) {
        VoiceRoomRealtimeManager.subscribeGroupRealtime(initialGroup.id)
        GroupChatRepository.getGroupOverview(context, initialGroup.id)
    }

    LaunchedEffect(initialGroup.id) {
        GroupChatRepository.liveGroupUpdates.collect { updatedId ->
            if (updatedId == initialGroup.id) {
                GroupChatRepository.getGroupOverview(context, initialGroup.id)
            }
        }
    }

    val isAdmin = currentGroup.myRole.equals("admin", ignoreCase = true)

    Box(modifier = Modifier.fillMaxSize()) {
        when (currentRoute) {
            GroupScreenRoute.CHAT -> {
                GroupChatScreen(
                    group = currentGroup,
                    onBack = onCloseGroup,
                    onNavigateToInfo = { currentRoute = GroupScreenRoute.INFO },
                    onNavigateToManageMembers = { openAdd ->
                        openAddMemberOnEnter = openAdd
                        currentRoute = GroupScreenRoute.MANAGE_MEMBERS
                    },
                    onNavigateToSettings = { currentRoute = GroupScreenRoute.SETTINGS }
                )
            }
            GroupScreenRoute.INFO -> {
                GroupInfoScreen(
                    group = currentGroup,
                    isAdmin = isAdmin,
                    onBack = { currentRoute = GroupScreenRoute.CHAT },
                    onNavigateToManageMembers = {
                        openAddMemberOnEnter = false
                        currentRoute = GroupScreenRoute.MANAGE_MEMBERS
                    },
                    onNavigateToGroupSettings = { currentRoute = GroupScreenRoute.SETTINGS },
                    onAddMemberClick = {
                        openAddMemberOnEnter = true
                        currentRoute = GroupScreenRoute.MANAGE_MEMBERS
                    },
                    onLeaveGroupClick = { onCloseGroup() }
                )
            }
            GroupScreenRoute.MANAGE_MEMBERS -> {
                ManageMembersScreen(
                    group = currentGroup,
                    isAdmin = isAdmin,
                    initialShowAddSheet = openAddMemberOnEnter,
                    onConsumeInitialShowAddSheet = { openAddMemberOnEnter = false },
                    onBack = { currentRoute = GroupScreenRoute.INFO }
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

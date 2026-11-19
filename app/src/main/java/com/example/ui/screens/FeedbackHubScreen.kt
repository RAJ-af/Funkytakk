package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.FeedbackPostDto
import com.example.ModerationManager
import com.example.SupportChatManager
import com.example.repository.FeedbackRepository
import com.example.ui.components.FunkyDeleteIcon
import com.example.ui.components.FunkySendIcon
import com.example.ui.components.SoftBackIcon
import com.example.ui.components.SoftChatBubbleIcon
import com.example.ui.components.SoftCloseIcon
import com.example.ui.components.SoftDocumentIcon
import com.example.ui.components.SoftInfoIcon
import com.example.ui.components.SoftPlusIcon
import com.example.ui.components.SoftRefreshIcon
import com.example.ui.components.SoftShieldIcon
import com.example.ui.components.SoftStarIcon
import com.example.ui.components.SoftThreeDotsIcon
import com.example.ui.components.resolveAvatarModel
import com.example.ui.theme.LocalFunkyColors
import kotlinx.coroutines.launch

@Composable
fun FeedbackHubScreen(
    initialCreateKind: String? = null,
    onConsumeInitialCreateKind: () -> Unit = {},
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val funkyColors = LocalFunkyColors.current
    val isDark = funkyColors.isDark

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Feature requests, 1 = My reports
    var statusFilter by remember { mutableStateOf<String?>(null) } // null = All, planned, in_progress, done
    var sortOrder by remember { mutableStateOf("top") } // "top" or "new"

    var boardItems by remember { mutableStateOf<List<FeedbackPostDto>>(emptyList()) }
    var myItems by remember { mutableStateOf<List<FeedbackPostDto>>(emptyList()) }
    var isBoardLoading by remember { mutableStateOf(true) }
    var isMyLoading by remember { mutableStateOf(false) }
    var isStaff by remember { mutableStateOf(false) }

    val myUpdatesCount by FeedbackRepository.myUpdatesCount.collectAsState()

    var showCreateModal by remember { mutableStateOf(false) }
    var createKind by remember { mutableStateOf("feature") }
    var createTitle by remember { mutableStateOf("") }
    var createBody by remember { mutableStateOf("") }
    var isSubmittingCreate by remember { mutableStateOf(false) }

    var activeDetailId by remember { mutableStateOf<Long?>(null) }

    fun loadBoard() {
        scope.launch {
            isBoardLoading = true
            FeedbackRepository.getFeedbackBoard(
                context = context,
                status = statusFilter,
                sort = sortOrder,
                limit = 30,
                offset = 0
            ).onSuccess { list ->
                boardItems = list
            }.onFailure { err ->
                Toast.makeText(context, err.message ?: "Something went wrong. Try again", Toast.LENGTH_SHORT).show()
            }
            isBoardLoading = false
        }
    }

    fun loadMine() {
        scope.launch {
            isMyLoading = true
            FeedbackRepository.getMyFeedback(context = context, limit = 30, offset = 0)
                .onSuccess { list ->
                    myItems = list
                }.onFailure { err ->
                    Toast.makeText(context, err.message ?: "Something went wrong. Try again", Toast.LENGTH_SHORT).show()
                }
            isMyLoading = false
        }
    }

    LaunchedEffect(Unit) {
        isStaff = ModerationManager.checkIsAdmin(context)
        loadBoard()
        loadMine()
    }

    LaunchedEffect(initialCreateKind) {
        if (initialCreateKind == "bug" || initialCreateKind == "feature") {
            createKind = initialCreateKind
            showCreateModal = true
            onConsumeInitialCreateKind()
        }
    }

    LaunchedEffect(statusFilter, sortOrder) {
        loadBoard()
    }

    BackHandler {
        when {
            activeDetailId != null -> activeDetailId = null
            showCreateModal -> showCreateModal = false
            else -> onBack()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(funkyColors.bg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(funkyColors.soft)
                    .clickable { onBack() }
                    .semantics { contentDescription = "Back" }
                    .testTag("feedback_back_button"),
                contentAlignment = Alignment.Center
            ) {
                SoftBackIcon(color = funkyColors.ink, size = 20.dp)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Feedback",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = funkyColors.ink
                )
                Text(
                    text = "Shape FunkyTalk together",
                    fontSize = 11.5.sp,
                    color = funkyColors.mute
                )
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(funkyColors.mango)
                    .clickable {
                        createKind = if (selectedTab == 0) "feature" else "bug"
                        showCreateModal = true
                    }
                    .semantics { contentDescription = "Create feedback" }
                    .testTag("feedback_create_button"),
                contentAlignment = Alignment.Center
            ) {
                SoftPlusIcon(color = funkyColors.onMango, size = 20.dp)
            }
        }

        // Segmented Tabs ("Feature requests" | "My reports")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .height(48.dp)
                .clip(RoundedCornerShape(100.dp))
                .background(funkyColors.soft)
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(100.dp))
                    .background(if (selectedTab == 0) funkyColors.surface else Color.Transparent)
                    .clickable { selectedTab = 0 }
                    .testTag("feedback_tab_board"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Feature requests",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (selectedTab == 0) funkyColors.ink else funkyColors.mute
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(100.dp))
                    .background(if (selectedTab == 1) funkyColors.surface else Color.Transparent)
                    .clickable {
                        selectedTab = 1
                        loadMine()
                    }
                    .testTag("feedback_tab_mine"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "My reports",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (selectedTab == 1) funkyColors.ink else funkyColors.mute
                    )
                    if (myUpdatesCount > 0) {
                        Box(
                            modifier = Modifier
                                .defaultMinSize(minWidth = 18.dp, minHeight = 18.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEF4444))
                                .padding(horizontal = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$myUpdatesCount",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (selectedTab == 0) {
            // Filter + Sort bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filters = listOf(
                        null to "All",
                        "planned" to "Planned",
                        "in_progress" to "In progress",
                        "done" to "Done"
                    )
                    items(filters) { (key, label) ->
                        val active = statusFilter == key
                        Surface(
                            onClick = { statusFilter = key },
                            shape = RoundedCornerShape(99.dp),
                            color = if (active) funkyColors.mango else funkyColors.surface,
                            border = BorderStroke(1.dp, if (active) funkyColors.mango else funkyColors.line)
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (active) funkyColors.onMango else funkyColors.ink,
                                modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp)
                            )
                        }
                    }
                }

                // Sort toggle Top / New
                Surface(
                    onClick = { sortOrder = if (sortOrder == "top") "new" else "top" },
                    shape = RoundedCornerShape(99.dp),
                    color = funkyColors.soft,
                    border = BorderStroke(1.dp, funkyColors.line),
                    modifier = Modifier.testTag("feedback_sort_toggle")
                ) {
                    Text(
                        text = if (sortOrder == "top") "Sort: Top" else "Sort: New",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = funkyColors.ink,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (isBoardLoading && boardItems.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = funkyColors.mango)
                }
            } else if (boardItems.isEmpty()) {
                FeedbackEmptyState(
                    title = "No feature requests yet",
                    subtitle = "Have an idea that would make FunkyTalk better? Tap + to share it with the community.",
                    actionLabel = "Request a feature",
                    onAction = {
                        createKind = "feature"
                        showCreateModal = true
                    }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(boardItems, key = { it.id }) { item ->
                        FeedbackBoardCard(
                            item = item,
                            onCardClick = { activeDetailId = item.id },
                            onVoteClick = {
                                val votingClosed = item.status in listOf("done", "declined", "duplicate")
                                if (votingClosed) {
                                    Toast.makeText(context, "Voting is closed for this request", Toast.LENGTH_SHORT).show()
                                    return@FeedbackBoardCard
                                }
                                val oldVoted = item.has_voted
                                val oldCount = item.votes_count
                                val optimisticVoted = !oldVoted
                                val optimisticCount = (oldCount + if (optimisticVoted) 1 else -1).coerceAtLeast(0)
                                boardItems = boardItems.map {
                                    if (it.id == item.id) it.copy(has_voted = optimisticVoted, votes_count = optimisticCount) else it
                                }
                                scope.launch {
                                    FeedbackRepository.toggleFeedbackVote(context, item.id)
                                        .onSuccess { res ->
                                            boardItems = boardItems.map {
                                                if (it.id == item.id) it.copy(has_voted = res.voted, votes_count = res.votes_count) else it
                                            }
                                        }
                                        .onFailure { err ->
                                            boardItems = boardItems.map {
                                                if (it.id == item.id) it.copy(has_voted = oldVoted, votes_count = oldCount) else it
                                            }
                                            Toast.makeText(context, err.message ?: "Something went wrong. Try again", Toast.LENGTH_SHORT).show()
                                        }
                                }
                            }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        } else {
            // My Reports tab
            if (isMyLoading && myItems.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = funkyColors.mango)
                }
            } else if (myItems.isEmpty()) {
                FeedbackEmptyState(
                    title = "No reports or requests yet",
                    subtitle = "Bugs you report and feature requests you submit appear here with live team updates.",
                    actionLabel = "Submit feedback",
                    onAction = {
                        createKind = "bug"
                        showCreateModal = true
                    }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(myItems, key = { it.id }) { item ->
                        MyFeedbackCard(
                            item = item,
                            onClick = { activeDetailId = item.id }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }

    // Create Feedback Modal
    if (showCreateModal) {
        CreateFeedbackDialog(
            kind = createKind,
            onKindChange = { createKind = it },
            title = createTitle,
            onTitleChange = { if (it.length <= 80) createTitle = it },
            body = createBody,
            onBodyChange = { if (it.length <= 1500) createBody = it },
            isSubmitting = isSubmittingCreate,
            onDismiss = { if (!isSubmittingCreate) showCreateModal = false },
            onSubmit = {
                val trimmedTitle = createTitle.trim()
                if (trimmedTitle.length < 3) {
                    Toast.makeText(context, "Give it a short title (3+ letters)", Toast.LENGTH_SHORT).show()
                    return@CreateFeedbackDialog
                }
                isSubmittingCreate = true
                scope.launch {
                    FeedbackRepository.createFeedback(
                        context = context,
                        kind = createKind,
                        title = trimmedTitle,
                        body = createBody.trim()
                    ).onSuccess { created ->
                        isSubmittingCreate = false
                        showCreateModal = false
                        createTitle = ""
                        createBody = ""
                        Toast.makeText(
                            context,
                            if (created.kind == "bug") "Bug report submitted privately" else "Feature request posted",
                            Toast.LENGTH_SHORT
                        ).show()
                        loadBoard()
                        loadMine()
                        selectedTab = if (created.kind == "bug") 1 else 0
                    }.onFailure { err ->
                        isSubmittingCreate = false
                        Toast.makeText(context, err.message ?: "Something went wrong. Try again", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    // Detail Thread Modal
    activeDetailId?.let { detailId ->
        FeedbackDetailDialog(
            feedbackId = detailId,
            isStaff = isStaff,
            onDismiss = {
                activeDetailId = null
                loadBoard()
                loadMine()
            },
            onDeleted = {
                activeDetailId = null
                loadBoard()
                loadMine()
            }
        )
    }
}

@Composable
private fun FeedbackBoardCard(
    item: FeedbackPostDto,
    onCardClick: () -> Unit,
    onVoteClick: () -> Unit
) {
    val funkyColors = LocalFunkyColors.current
    val votingClosed = item.status in listOf("done", "declined", "duplicate")
    Surface(
        onClick = onCardClick,
        shape = RoundedCornerShape(20.dp),
        color = funkyColors.surface,
        border = BorderStroke(1.dp, funkyColors.line),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("feedback_board_card_${item.id}")
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Upvote Box
            Surface(
                onClick = onVoteClick,
                enabled = !votingClosed,
                shape = RoundedCornerShape(14.dp),
                color = when {
                    item.has_voted -> funkyColors.mango
                    votingClosed -> funkyColors.soft.copy(alpha = 0.6f)
                    else -> funkyColors.soft
                },
                border = BorderStroke(
                    1.dp,
                    if (item.has_voted) funkyColors.mango else funkyColors.line
                ),
                modifier = Modifier
                    .width(52.dp)
                    .defaultMinSize(minHeight = 56.dp)
                    .testTag("feedback_vote_btn_${item.id}")
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    UpvoteChevronIcon(
                        color = when {
                            item.has_voted -> funkyColors.onMango
                            votingClosed -> funkyColors.mute.copy(alpha = 0.5f)
                            else -> funkyColors.ink
                        },
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.votes_count.toString(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = when {
                            item.has_voted -> funkyColors.onMango
                            votingClosed -> funkyColors.mute.copy(alpha = 0.6f)
                            else -> funkyColors.ink
                        }
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FeedbackStatusChip(status = item.status)
                    Spacer(modifier = Modifier.weight(1f))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        SoftChatBubbleIcon(size = 13.dp, color = funkyColors.mute)
                        Text(
                            text = item.replies_count.toString(),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = funkyColors.mute
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = item.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = funkyColors.ink,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (item.body.isNotBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = item.body,
                        fontSize = 12.5.sp,
                        color = funkyColors.mute,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 17.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val authorName = item.author?.name?.takeIf { it.isNotBlank() }
                        ?: item.author?.username?.takeIf { it.isNotBlank() }
                        ?: "Member"
                    val avatarModel = resolveAvatarModel(item.author?.avatar_url)
                    if (avatarModel != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current).data(avatarModel).crossfade(true).build(),
                            contentDescription = authorName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(funkyColors.soft),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = authorName.take(1).uppercase(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = funkyColors.ink
                            )
                        }
                    }
                    Text(
                        text = authorName,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = funkyColors.mute,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(text = "·", color = funkyColors.mute, fontSize = 11.5.sp)
                    Text(
                        text = SupportChatManager.formatMessageTime(item.created_at),
                        fontSize = 11.sp,
                        color = funkyColors.mute
                    )
                }
            }
        }
    }
}

@Composable
private fun MyFeedbackCard(
    item: FeedbackPostDto,
    onClick: () -> Unit
) {
    val funkyColors = LocalFunkyColors.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = funkyColors.surface,
        border = BorderStroke(
            width = if (item.has_update) 1.5.dp else 1.dp,
            color = if (item.has_update) funkyColors.mango else funkyColors.line
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("my_feedback_card_${item.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FeedbackKindChip(kind = item.kind)
                FeedbackStatusChip(status = item.status)
                Spacer(modifier = Modifier.weight(1f))
                if (item.has_update) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEF4444))
                        )
                        Text(
                            text = "Updated",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEF4444)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = item.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = funkyColors.ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (item.body.isNotBlank()) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = item.body,
                    fontSize = 12.5.sp,
                    color = funkyColors.mute,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (item.kind == "feature") {
                    Text(
                        text = "${item.votes_count} votes",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = funkyColors.ink
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    SoftChatBubbleIcon(size = 13.dp, color = funkyColors.mute)
                    Text(
                        text = "${item.replies_count} replies",
                        fontSize = 11.5.sp,
                        color = funkyColors.mute
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = SupportChatManager.formatMessageTime(item.created_at),
                    fontSize = 11.sp,
                    color = funkyColors.mute
                )
            }
        }
    }
}

@Composable
fun FeedbackKindChip(kind: String) {
    val funkyColors = LocalFunkyColors.current
    val isDark = funkyColors.isDark
    val isBug = kind.equals("bug", ignoreCase = true)
    val bg = when {
        isBug && isDark -> Color(0xFFEF4444).copy(alpha = 0.18f)
        isBug -> Color(0xFFFEE2E2)
        isDark -> funkyColors.mango.copy(alpha = 0.2f)
        else -> Color(0xFFFEF3C7)
    }
    val fg = when {
        isBug && isDark -> Color(0xFFFCA5A5)
        isBug -> Color(0xFFB91C1C)
        isDark -> funkyColors.mango
        else -> Color(0xFFB45309)
    }
    Surface(shape = RoundedCornerShape(8.dp), color = bg) {
        Text(
            text = if (isBug) "Bug" else "Feature",
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            color = fg,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun FeedbackStatusChip(status: String) {
    val funkyColors = LocalFunkyColors.current
    val isDark = funkyColors.isDark
    val normalized = status.lowercase()
    val (label, bg, fg) = when (normalized) {
        "planned" -> Triple(
            "Planned",
            if (isDark) Color(0xFF3B82F6).copy(alpha = 0.2f) else Color(0xFFDBEAFE),
            if (isDark) Color(0xFF93C5FD) else Color(0xFF1D4ED8)
        )
        "in_progress" -> Triple(
            "In progress",
            if (isDark) Color(0xFFF97316).copy(alpha = 0.22f) else Color(0xFFFFEDD5),
            if (isDark) Color(0xFFFDBA74) else Color(0xFFC2410C)
        )
        "done" -> Triple(
            "Done",
            if (isDark) Color(0xFF22C55E).copy(alpha = 0.2f) else Color(0xFFDCFCE7),
            if (isDark) Color(0xFF86EFAC) else Color(0xFF15803D)
        )
        "declined" -> Triple(
            "Declined",
            if (isDark) Color(0xFFEF4444).copy(alpha = 0.2f) else Color(0xFFFEE2E2),
            if (isDark) Color(0xFFFCA5A5) else Color(0xFFB91C1C)
        )
        "duplicate" -> Triple(
            "Duplicate",
            funkyColors.soft,
            funkyColors.mute
        )
        else -> Triple(
            "Open",
            funkyColors.soft,
            funkyColors.mute
        )
    }
    Surface(shape = RoundedCornerShape(8.dp), color = bg) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = fg,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun CreateFeedbackDialog(
    kind: String,
    onKindChange: (String) -> Unit,
    title: String,
    onTitleChange: (String) -> Unit,
    body: String,
    onBodyChange: (String) -> Unit,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit
) {
    val funkyColors = LocalFunkyColors.current
    val isDark = funkyColors.isDark
    val isBug = kind == "bug"

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(funkyColors.bg),
            color = funkyColors.bg
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(funkyColors.soft)
                            .clickable(enabled = !isSubmitting) { onDismiss() }
                            .semantics { contentDescription = "Close" },
                        contentAlignment = Alignment.Center
                    ) {
                        SoftCloseIcon(color = funkyColors.ink, size = 18.dp)
                    }

                    Text(
                        text = if (isBug) "Report a Bug" else "Request a Feature",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = funkyColors.ink
                    )

                    Spacer(modifier = Modifier.size(48.dp))
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Kind selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(100.dp))
                            .background(funkyColors.soft)
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(100.dp))
                                .background(if (!isBug) funkyColors.mango else Color.Transparent)
                                .clickable(enabled = !isSubmitting) { onKindChange("feature") }
                                .testTag("create_feedback_kind_feature"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Feature Request",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (!isBug) funkyColors.onMango else funkyColors.mute
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(100.dp))
                                .background(if (isBug) funkyColors.mango else Color.Transparent)
                                .clickable(enabled = !isSubmitting) { onKindChange("bug") }
                                .testTag("create_feedback_kind_bug"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Bug Report",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isBug) funkyColors.onMango else funkyColors.mute
                            )
                        }
                    }

                    // Visibility Notice
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isBug) {
                            if (isDark) Color(0xFF10B981).copy(alpha = 0.14f) else Color(0xFFECFDF5)
                        } else {
                            funkyColors.soft
                        },
                        border = BorderStroke(
                            1.dp,
                            if (isBug) Color(0xFF10B981).copy(alpha = 0.4f) else funkyColors.line
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (isBug) {
                                SoftShieldIcon(size = 18.dp, color = Color(0xFF10B981))
                            } else {
                                SoftStarIcon(size = 18.dp, color = funkyColors.mango)
                            }
                            Text(
                                text = if (isBug) {
                                    "Bug reports are private — only you and the FunkyTalk team can see them."
                                } else {
                                    "Feature requests appear on the public board and automatically include your vote."
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = funkyColors.ink,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    // Title
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Title", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = funkyColors.ink)
                            Text("${title.length}/80", fontSize = 11.sp, color = funkyColors.mute)
                        }
                        OutlinedTextField(
                            value = title,
                            onValueChange = onTitleChange,
                            singleLine = true,
                            enabled = !isSubmitting,
                            placeholder = {
                                Text(
                                    text = if (isBug) "Short summary of the bug" else "Short title for your idea",
                                    color = funkyColors.mute,
                                    fontSize = 14.sp
                                )
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = funkyColors.surface,
                                unfocusedContainerColor = funkyColors.surface,
                                focusedBorderColor = funkyColors.mango,
                                unfocusedBorderColor = funkyColors.line,
                                focusedTextColor = funkyColors.ink,
                                unfocusedTextColor = funkyColors.ink,
                                cursorColor = funkyColors.mango
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("create_feedback_title_input")
                        )
                    }

                    // Body
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Details", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = funkyColors.ink)
                            Text("${body.length}/1500", fontSize = 11.sp, color = funkyColors.mute)
                        }
                        OutlinedTextField(
                            value = body,
                            onValueChange = onBodyChange,
                            enabled = !isSubmitting,
                            minLines = 5,
                            maxLines = 10,
                            placeholder = {
                                Text(
                                    text = if (isBug) {
                                        "What happened? What did you expect? Steps to reproduce"
                                    } else {
                                        "Explain how this feature works and why it helps"
                                    },
                                    color = funkyColors.mute,
                                    fontSize = 14.sp
                                )
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = funkyColors.surface,
                                unfocusedContainerColor = funkyColors.surface,
                                focusedBorderColor = funkyColors.mango,
                                unfocusedBorderColor = funkyColors.line,
                                focusedTextColor = funkyColors.ink,
                                unfocusedTextColor = funkyColors.ink,
                                cursorColor = funkyColors.mango
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("create_feedback_body_input")
                        )
                    }

                    // Auto-attached diagnostics chip
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = funkyColors.surface,
                        border = BorderStroke(1.dp, funkyColors.line),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SoftInfoIcon(size = 16.dp, color = funkyColors.mute)
                            Text(
                                text = "Attached automatically: App v${FeedbackRepository.getAppVersionString()} · ${FeedbackRepository.getDeviceInfoString()}",
                                fontSize = 11.5.sp,
                                color = funkyColors.mute,
                                lineHeight = 15.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

                Surface(
                    color = funkyColors.surface,
                    border = BorderStroke(1.dp, funkyColors.line),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onSubmit,
                        enabled = !isSubmitting && title.trim().length >= 3,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = funkyColors.mango,
                            contentColor = funkyColors.onMango
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp)
                            .height(50.dp)
                            .testTag("create_feedback_submit_button")
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = funkyColors.onMango
                            )
                        } else {
                            Text(
                                text = if (isBug) "Submit Bug Report" else "Post Feature Request",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedbackDetailDialog(
    feedbackId: Long,
    isStaff: Boolean,
    onDismiss: () -> Unit,
    onDeleted: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val funkyColors = LocalFunkyColors.current

    var post by remember { mutableStateOf<FeedbackPostDto?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var replyText by remember { mutableStateOf("") }
    var isSendingReply by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    fun loadDetail(markSeen: Boolean) {
        scope.launch {
            isLoading = true
            FeedbackRepository.getFeedback(context, feedbackId)
                .onSuccess { dto ->
                    post = dto
                    if (markSeen && dto.has_update) {
                        FeedbackRepository.markFeedbackSeen(context, feedbackId)
                    }
                }
                .onFailure { err ->
                    Toast.makeText(context, err.message ?: "Something went wrong. Try again", Toast.LENGTH_SHORT).show()
                }
            isLoading = false
        }
    }

    LaunchedEffect(feedbackId) {
        loadDetail(markSeen = true)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = funkyColors.bg
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(funkyColors.soft)
                            .clickable { onDismiss() }
                            .semantics { contentDescription = "Back" }
                            .testTag("feedback_detail_back_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        SoftBackIcon(color = funkyColors.ink, size = 20.dp)
                    }

                    Text(
                        text = if (post?.kind == "bug") "Bug Report" else "Feature Request",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = funkyColors.ink
                    )

                    if (post?.is_mine == true) {
                        Box {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(funkyColors.soft)
                                    .clickable { showMoreMenu = true }
                                    .semantics { contentDescription = "More options" }
                                    .testTag("feedback_detail_more_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                SoftThreeDotsIcon(color = funkyColors.ink, size = 18.dp)
                            }
                            DropdownMenu(
                                expanded = showMoreMenu,
                                onDismissRequest = { showMoreMenu = false },
                                shape = RoundedCornerShape(20.dp),
                                containerColor = funkyColors.surface,
                                shadowElevation = 10.dp,
                                modifier = Modifier
                                    .widthIn(min = 180.dp)
                                    .border(1.dp, funkyColors.line, RoundedCornerShape(20.dp))
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "Delete",
                                            color = funkyColors.danger,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    },
                                    leadingIcon = {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(funkyColors.danger.copy(alpha = 0.12f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            FunkyDeleteIcon(color = funkyColors.danger, modifier = Modifier.size(17.dp))
                                        }
                                    },
                                    onClick = {
                                        showMoreMenu = false
                                        showDeleteConfirm = true
                                    }
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(funkyColors.soft)
                                .clickable { loadDetail(markSeen = false) }
                                .semantics { contentDescription = "Refresh" },
                            contentAlignment = Alignment.Center
                        ) {
                            SoftRefreshIcon(color = funkyColors.ink, size = 18.dp)
                        }
                    }
                }

                HorizontalDivider(color = funkyColors.line)

                if (isLoading && post == null) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = funkyColors.mango)
                    }
                } else if (post != null) {
                    val item = post!!
                    val canReply = item.is_mine || isStaff
                    val votingClosed = item.status in listOf("done", "declined", "duplicate")

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = funkyColors.surface,
                                border = BorderStroke(1.dp, funkyColors.line),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        FeedbackKindChip(kind = item.kind)
                                        FeedbackStatusChip(status = item.status)
                                        Spacer(modifier = Modifier.weight(1f))
                                        if (item.kind == "feature") {
                                            Surface(
                                                onClick = {
                                                    if (votingClosed) {
                                                        Toast.makeText(context, "Voting is closed for this request", Toast.LENGTH_SHORT).show()
                                                        return@Surface
                                                    }
                                                    scope.launch {
                                                        FeedbackRepository.toggleFeedbackVote(context, item.id)
                                                            .onSuccess { res ->
                                                                post = item.copy(has_voted = res.voted, votes_count = res.votes_count)
                                                            }
                                                            .onFailure { err ->
                                                                Toast.makeText(context, err.message ?: "Something went wrong. Try again", Toast.LENGTH_SHORT).show()
                                                            }
                                                    }
                                                },
                                                enabled = !votingClosed,
                                                shape = RoundedCornerShape(12.dp),
                                                color = if (item.has_voted) funkyColors.mango else funkyColors.soft,
                                                border = BorderStroke(1.dp, if (item.has_voted) funkyColors.mango else funkyColors.line)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    UpvoteChevronIcon(
                                                        color = if (item.has_voted) funkyColors.onMango else funkyColors.ink,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Text(
                                                        text = "${item.votes_count}",
                                                        fontSize = 12.5.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = if (item.has_voted) funkyColors.onMango else funkyColors.ink
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Text(
                                        text = item.title,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = funkyColors.ink
                                    )

                                    if (item.body.isNotBlank()) {
                                        Text(
                                            text = item.body,
                                            fontSize = 14.sp,
                                            color = funkyColors.ink.copy(alpha = 0.88f),
                                            lineHeight = 20.sp
                                        )
                                    }

                                    val metaParts = listOfNotNull(
                                        item.app_version?.takeIf { it.isNotBlank() }?.let { "App $it" },
                                        item.device_info?.takeIf { it.isNotBlank() }
                                    )
                                    if (metaParts.isNotEmpty()) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = funkyColors.soft
                                        ) {
                                            Text(
                                                text = metaParts.joinToString(" · "),
                                                fontSize = 11.sp,
                                                color = funkyColors.mute,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = "Submitted ${SupportChatManager.formatMessageTime(item.created_at)}",
                                        fontSize = 11.sp,
                                        color = funkyColors.mute
                                    )
                                }
                            }
                        }

                        item {
                            Text(
                                text = "Replies (${item.replies.size})",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = funkyColors.ink
                            )
                        }

                        if (item.replies.isEmpty()) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = funkyColors.surface,
                                    border = BorderStroke(1.dp, funkyColors.line),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "No replies yet. Team updates and notes will appear here.",
                                        fontSize = 12.5.sp,
                                        color = funkyColors.mute,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            }
                        } else {
                            items(item.replies, key = { it.id }) { reply ->
                                val isMineReply = reply.is_mine && !reply.is_staff
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = when {
                                        reply.is_staff -> funkyColors.soft
                                        isMineReply -> funkyColors.mango.copy(alpha = 0.16f)
                                        else -> funkyColors.surface
                                    },
                                    border = BorderStroke(
                                        1.dp,
                                        if (reply.is_staff) funkyColors.mango.copy(alpha = 0.45f) else funkyColors.line
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            if (reply.is_staff) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = funkyColors.mango
                                                ) {
                                                    Text(
                                                        text = "Funkytalk Team",
                                                        fontSize = 10.5.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = funkyColors.onMango,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                    )
                                                }
                                            } else {
                                                Text(
                                                    text = reply.author?.name?.takeIf { it.isNotBlank() }
                                                        ?: reply.author?.username?.takeIf { it.isNotBlank() }
                                                        ?: "You",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = funkyColors.ink
                                                )
                                            }
                                            Text(
                                                text = SupportChatManager.formatMessageTime(reply.created_at),
                                                fontSize = 10.5.sp,
                                                color = funkyColors.mute
                                            )
                                        }
                                        Text(
                                            text = reply.body,
                                            fontSize = 13.5.sp,
                                            color = funkyColors.ink,
                                            lineHeight = 19.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (canReply) {
                        Surface(
                            color = funkyColors.surface,
                            border = BorderStroke(1.dp, funkyColors.line),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = replyText,
                                    onValueChange = { if (it.length <= 1000) replyText = it },
                                    placeholder = { Text("Write a reply...", fontSize = 14.sp, color = funkyColors.mute) },
                                    maxLines = 4,
                                    enabled = !isSendingReply,
                                    shape = RoundedCornerShape(22.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = funkyColors.bg,
                                        unfocusedContainerColor = funkyColors.bg,
                                        focusedBorderColor = funkyColors.mango,
                                        unfocusedBorderColor = funkyColors.line,
                                        focusedTextColor = funkyColors.ink,
                                        unfocusedTextColor = funkyColors.ink,
                                        cursorColor = funkyColors.mango
                                    ),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                    keyboardActions = KeyboardActions(onSend = {
                                        if (replyText.isNotBlank() && !isSendingReply) {
                                            val text = replyText.trim()
                                            isSendingReply = true
                                            scope.launch {
                                                FeedbackRepository.addFeedbackReply(context, feedbackId, text)
                                                    .onSuccess {
                                                        replyText = ""
                                                        loadDetail(markSeen = false)
                                                    }
                                                    .onFailure { err ->
                                                        Toast.makeText(context, err.message ?: "Something went wrong. Try again", Toast.LENGTH_SHORT).show()
                                                    }
                                                isSendingReply = false
                                            }
                                        }
                                    }),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("feedback_reply_input")
                                )

                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(if (replyText.isNotBlank() && !isSendingReply) funkyColors.mango else funkyColors.soft)
                                        .clickable(enabled = replyText.isNotBlank() && !isSendingReply) {
                                            val text = replyText.trim()
                                            isSendingReply = true
                                            scope.launch {
                                                FeedbackRepository.addFeedbackReply(context, feedbackId, text)
                                                    .onSuccess {
                                                        replyText = ""
                                                        loadDetail(markSeen = false)
                                                    }
                                                    .onFailure { err ->
                                                        Toast.makeText(context, err.message ?: "Something went wrong. Try again", Toast.LENGTH_SHORT).show()
                                                    }
                                                isSendingReply = false
                                            }
                                        }
                                        .semantics { contentDescription = "Send reply" }
                                        .testTag("feedback_reply_send_button"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSendingReply) {
                                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = funkyColors.onMango)
                                    } else {
                                        FunkySendIcon(
                                            color = if (replyText.isNotBlank()) funkyColors.onMango else funkyColors.mute,
                                            modifier = Modifier.size(18.dp)
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

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = funkyColors.surface,
            title = { Text("Delete submission?", fontWeight = FontWeight.ExtraBold, color = funkyColors.ink) },
            text = { Text("This will permanently remove your post and all its replies.", color = funkyColors.mute, fontSize = 13.5.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        scope.launch {
                            FeedbackRepository.deleteFeedback(context, feedbackId)
                                .onSuccess {
                                    Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show()
                                    onDeleted()
                                }
                                .onFailure { err ->
                                    Toast.makeText(context, err.message ?: "Something went wrong. Try again", Toast.LENGTH_SHORT).show()
                                }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = funkyColors.danger, contentColor = Color.White)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = funkyColors.ink, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun FeedbackEmptyState(
    title: String,
    subtitle: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    val funkyColors = LocalFunkyColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(funkyColors.soft)
                .border(1.dp, funkyColors.line, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            SoftDocumentIcon(size = 28.dp, color = funkyColors.mango)
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = title,
            fontSize = 17.sp,
            fontWeight = FontWeight.ExtraBold,
            color = funkyColors.ink
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitle,
            fontSize = 13.sp,
            color = funkyColors.mute,
            lineHeight = 18.sp
        )
        Spacer(modifier = Modifier.height(18.dp))
        Button(
            onClick = onAction,
            colors = ButtonDefaults.buttonColors(containerColor = funkyColors.mango, contentColor = funkyColors.onMango),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(text = actionLabel, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun UpvoteChevronIcon(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.18f, h * 0.68f)
            lineTo(w * 0.50f, h * 0.28f)
            lineTo(w * 0.82f, h * 0.68f)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = size.minDimension * 0.18f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}

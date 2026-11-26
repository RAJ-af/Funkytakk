package com.example.card

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FlipToFront
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import com.example.StoreItemRow
import com.example.SupabaseSessionManager
import com.example.ui.theme.LocalFunkyColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.abs
import kotlin.random.Random

@Composable
fun CardWorkshopScreen(
    myProfile: PeopleCardProfile,
    onDismiss: () -> Unit,
    onCardSaved: (CardDesign) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val funkyColors = LocalFunkyColors.current
    val reduceAnimations = rememberCardReduceAnimations()

    val myUid = remember(myProfile.userId) {
        SupabaseSessionManager.getUserId(context)?.takeIf { it.isNotBlank() } ?: myProfile.userId
    }

    var isInitialLoading by remember { mutableStateOf(true) }
    var currentDesign by remember { mutableStateOf(CardDesign()) }
    var remixable by remember { mutableStateOf(true) }
    var baselineDesign by remember { mutableStateOf(CardDesign()) }
    var baselineRemixable by remember { mutableStateOf(true) }

    var undoStack by remember { mutableStateOf<List<CardDesign>>(emptyList()) }
    var redoStack by remember { mutableStateOf<List<CardDesign>>(emptyList()) }

    var selectedStickerId by remember { mutableStateOf<String?>(null) }
    var newlyAddedStickerId by remember { mutableStateOf<String?>(null) }
    var isDraggingSticker by remember { mutableStateOf(false) }
    var isHoveringTrash by remember { mutableStateOf(false) }
    var preDragDesign by remember { mutableStateOf<CardDesign?>(null) }

    var snapGuideX by remember { mutableStateOf(false) }
    var snapGuideY by remember { mutableStateOf(false) }
    var lastSnapEdgeX by remember { mutableStateOf(0) } // 0=none, 1=center, 2=left, 3=right
    var lastSnapEdgeY by remember { mutableStateOf(0) } // 0=none, 1=center, 2=top, 3=bottom

    var activeTab by remember { mutableStateOf(WorkshopTab.STICKERS) }
    var stickerSubTab by remember { mutableStateOf(StickerSubTab.WORDS) }

    var isSaving by remember { mutableStateOf(false) }
    var isResetting by remember { mutableStateOf(false) }
    var isRemixing by remember { mutableStateOf(false) }
    var showConfetti by remember { mutableStateOf(false) }
    var liveBannerMessage by remember { mutableStateOf<String?>(null) }

    var showDiscardDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var previewRemixItem by remember { mutableStateOf<RemixableCardItem?>(null) }

    // Store sticker packs state
    var stickerPacks by remember { mutableStateOf<List<StoreItemRow>>(emptyList()) }
    var isLoadingPacks by remember { mutableStateOf(false) }
    var packsError by remember { mutableStateOf<String?>(null) }

    // Remix discovery state
    var remixCards by remember { mutableStateOf<List<RemixableCardItem>>(emptyList()) }
    var isLoadingRemix by remember { mutableStateOf(false) }
    var isLoadingMoreRemix by remember { mutableStateOf(false) }
    var remixHasMore by remember { mutableStateOf(false) }
    var remixError by remember { mutableStateOf<String?>(null) }

    val isDirty = remember(currentDesign, remixable, baselineDesign, baselineRemixable) {
        currentDesign != baselineDesign || remixable != baselineRemixable
    }

    fun pushEdit(newDesign: CardDesign, recordHistory: Boolean = true) {
        if (newDesign == currentDesign) return
        if (recordHistory) {
            undoStack = (undoStack + currentDesign).takeLast(30)
            redoStack = emptyList()
        }
        currentDesign = newDesign
    }

    fun performUndo() {
        val prev = undoStack.lastOrNull() ?: return
        undoStack = undoStack.dropLast(1)
        redoStack = (redoStack + currentDesign).takeLast(30)
        currentDesign = prev
        if (selectedStickerId != null && prev.stickers.none { it.localId == selectedStickerId }) {
            selectedStickerId = null
        }
    }

    fun performRedo() {
        val next = redoStack.lastOrNull() ?: return
        redoStack = redoStack.dropLast(1)
        undoStack = (undoStack + currentDesign).takeLast(30)
        currentDesign = next
        if (selectedStickerId != null && next.stickers.none { it.localId == selectedStickerId }) {
            selectedStickerId = null
        }
    }

    fun loadPacks(force: Boolean = false) {
        if (isLoadingPacks) return
        scope.launch {
            isLoadingPacks = true
            packsError = null
            val list = CardDesignRepository.ensureStickerPacksLoaded(context, forceRefresh = force)
            stickerPacks = list
            if (list.isEmpty()) {
                // Verify whether network error occurred by trying direct fetch only on explicit retry
                if (force) {
                    val res = com.example.store.CosmeticsManager.getStoreItems(context, type = "sticker_pack")
                    res.onSuccess { stickerPacks = it }.onFailure { err ->
                        packsError = err.message ?: "Could not load sticker packs."
                    }
                }
            }
            isLoadingPacks = false
        }
    }

    fun loadRemixCards(reset: Boolean) {
        if (reset && isLoadingRemix) return
        if (!reset && (isLoadingMoreRemix || !remixHasMore)) return
        scope.launch {
            val offset = if (reset) 0 else remixCards.size
            if (reset) {
                isLoadingRemix = true
                remixError = null
            } else {
                isLoadingMoreRemix = true
            }
            val pageSize = 18
            val res = CardDesignRepository.listRemixableCards(context, limit = pageSize, offset = offset)
            res.onSuccess { fetched ->
                val filtered = fetched.filter { it.userId != myUid }
                remixCards = if (reset) filtered else (remixCards + filtered).distinctBy { it.userId }
                remixHasMore = fetched.size >= pageSize
            }.onFailure { err ->
                if (reset) {
                    remixError = err.message ?: "Could not load remixable cards."
                } else {
                    Toast.makeText(context, err.message ?: "Could not load more cards.", Toast.LENGTH_SHORT).show()
                }
            }
            isLoadingRemix = false
            isLoadingMoreRemix = false
        }
    }

    LaunchedEffect(myUid) {
        isInitialLoading = true
        val res = CardDesignRepository.fetchMyCardDesign(context)
        val serverState = res.getOrNull() ?: MyCardDesignState()
        baselineDesign = serverState.design
        baselineRemixable = serverState.remixable

        val draft = CardDesignRepository.loadDraft(context, myUid)
        if (draft != null && (draft.first != serverState.design || draft.second != serverState.remixable)) {
            currentDesign = draft.first
            remixable = draft.second
            liveBannerMessage = "Restored your unsaved draft"
        } else {
            currentDesign = serverState.design
            remixable = serverState.remixable
        }
        isInitialLoading = false
        loadPacks(force = false)
    }

    // Persist unsaved edits locally so backgrounding never loses user work
    LaunchedEffect(currentDesign, remixable, isDirty, isInitialLoading) {
        if (isInitialLoading) return@LaunchedEffect
        if (isDirty) {
            CardDesignRepository.saveDraft(context, myUid, currentDesign, remixable)
        } else {
            CardDesignRepository.clearDraft(context, myUid)
        }
    }

    LaunchedEffect(activeTab) {
        if (activeTab == WorkshopTab.REMIX && remixCards.isEmpty() && !isLoadingRemix) {
            loadRemixCards(reset = true)
        }
        if (activeTab == WorkshopTab.STICKERS && stickerSubTab == StickerSubTab.PACKS && stickerPacks.isEmpty()) {
            loadPacks(force = false)
        }
    }

    LaunchedEffect(liveBannerMessage) {
        if (liveBannerMessage != null) {
            delay(2800)
            liveBannerMessage = null
        }
    }

    LaunchedEffect(showConfetti) {
        if (showConfetti) {
            delay(1800)
            showConfetti = false
        }
    }

    fun requestExit() {
        if (isDirty) {
            showDiscardDialog = true
        } else {
            onDismiss()
        }
    }

    BackHandler(enabled = previewRemixItem == null && !showDiscardDialog && !showResetDialog) {
        requestExit()
    }

    fun addSticker(sticker: CardSticker) {
        if (currentDesign.stickers.size >= CardDesignDefaults.MAX_STICKERS) {
            Toast.makeText(context, "Max 12 stickers reached", Toast.LENGTH_SHORT).show()
            return
        }
        val spreadIdx = currentDesign.stickers.size
        val nextX = (0.28f + (spreadIdx % 4) * 0.16f).coerceIn(0.18f, 0.84f)
        val nextY = (0.25f + ((spreadIdx / 4) % 3) * 0.25f).coerceIn(0.18f, 0.80f)
        val placed = sticker.copy(
            localId = UUID.randomUUID().toString(),
            x = nextX,
            y = nextY
        )
        pushEdit(currentDesign.copy(stickers = currentDesign.stickers + placed))
        selectedStickerId = placed.localId
        newlyAddedStickerId = placed.localId
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    fun saveCard() {
        if (isSaving) return
        if (CardTextSafetyFilter.containsBlockedWord(currentDesign.hiLabel)) {
            Toast.makeText(context, "Please choose another word", Toast.LENGTH_SHORT).show()
            activeTab = WorkshopTab.SHOW
            return
        }
        if (currentDesign.stickers.any { it.kind == CardStickerKind.WORD && CardTextSafetyFilter.containsBlockedWord(it.text) }) {
            Toast.makeText(context, "Please choose another word", Toast.LENGTH_SHORT).show()
            activeTab = WorkshopTab.STICKERS
            return
        }
        scope.launch {
            isSaving = true
            val res = CardDesignRepository.saveMyCardDesign(context, currentDesign, remixable)
            isSaving = false
            res.onSuccess { saved ->
                CardDesignRepository.clearDraft(context, myUid)
                currentDesign = saved
                baselineDesign = saved
                baselineRemixable = remixable
                undoStack = emptyList()
                redoStack = emptyList()
                selectedStickerId = null
                liveBannerMessage = "Your card is live"
                if (!reduceAnimations) {
                    showConfetti = true
                }
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onCardSaved(saved)
            }.onFailure { err ->
                Toast.makeText(context, err.message ?: "Could not save your card.", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun resetCard() {
        if (isResetting) return
        scope.launch {
            isResetting = true
            val res = CardDesignRepository.resetMyCardDesign(context)
            isResetting = false
            res.onSuccess {
                CardDesignRepository.clearDraft(context, myUid)
                val clean = CardDesign()
                currentDesign = clean
                baselineDesign = clean
                remixable = true
                baselineRemixable = true
                undoStack = emptyList()
                redoStack = emptyList()
                selectedStickerId = null
                liveBannerMessage = "Card reset to default"
            }.onFailure { err ->
                Toast.makeText(context, err.message ?: "Could not reset card.", Toast.LENGTH_LONG).show()
            }
        }
    }

    val pageBg = if (funkyColors.isDark) Color(0xFF0A0908) else Color(0xFFFFF8E6)
    val selectedSticker = remember(currentDesign.stickers, selectedStickerId) {
        currentDesign.stickers.firstOrNull { it.localId == selectedStickerId }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(pageBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Top Bar: Back | Undo | Redo | Reset card | Yellow Save Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { requestExit() },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("workshop_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = funkyColors.ink
                        )
                    }
                    IconButton(
                        onClick = { performUndo() },
                        enabled = undoStack.isNotEmpty(),
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("workshop_undo_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Undo,
                            contentDescription = "Undo",
                            tint = if (undoStack.isNotEmpty()) funkyColors.ink else funkyColors.mute.copy(alpha = 0.45f)
                        )
                    }
                    IconButton(
                        onClick = { performRedo() },
                        enabled = redoStack.isNotEmpty(),
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("workshop_redo_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Redo,
                            contentDescription = "Redo",
                            tint = if (redoStack.isNotEmpty()) funkyColors.ink else funkyColors.mute.copy(alpha = 0.45f)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TextButton(
                        onClick = { showResetDialog = true },
                        enabled = !isResetting && !isSaving,
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("workshop_reset_btn")
                    ) {
                        Text(
                            text = "Reset card",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = funkyColors.danger
                        )
                    }

                    Button(
                        onClick = { saveCard() },
                        enabled = !isSaving && !isResetting,
                        shape = RoundedCornerShape(100.dp),
                        border = BorderStroke(2.dp, Color(0xFF1A1A1A)),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFB800),
                            contentColor = Color(0xFF1A1A1A),
                            disabledContainerColor = Color(0xFFFFB800).copy(alpha = 0.55f),
                            disabledContentColor = Color(0xFF1A1A1A)
                        ),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("workshop_save_btn")
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFF1A1A1A)
                            )
                        } else {
                            Text(
                                text = "Save",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }

            // Live Card Preview Zone
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isInitialLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(funkyColors.surface.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFFFFB800))
                    }
                } else {
                    PeopleCard(
                        profile = myProfile,
                        design = currentDesign,
                        isWorkshopInteractive = true,
                        selectedStickerId = selectedStickerId,
                        newlyAddedStickerId = newlyAddedStickerId,
                        snapGuideX = snapGuideX,
                        snapGuideY = snapGuideY,
                        onSelectSticker = { id ->
                            selectedStickerId = id
                        },
                        onStickerDragStart = { id ->
                            selectedStickerId = id
                            preDragDesign = currentDesign
                            isDraggingSticker = true
                        },
                        onMoveSticker = { id, rawX, rawY, fingerInTrash ->
                            isHoveringTrash = fingerInTrash
                            var nx = rawX
                            var ny = rawY
                            var sx = false
                            var edgeX = 0
                            when {
                                abs(nx - 0.5f) < 0.028f -> { nx = 0.5f; sx = true; edgeX = 1 }
                                nx < 0.11f -> { nx = 0.09f; edgeX = 2 }
                                nx > 0.89f -> { nx = 0.91f; edgeX = 3 }
                            }
                            var sy = false
                            var edgeY = 0
                            if (!fingerInTrash) {
                                when {
                                    abs(ny - 0.5f) < 0.032f -> { ny = 0.5f; sy = true; edgeY = 1 }
                                    ny < 0.14f -> { ny = 0.12f; edgeY = 2 }
                                    ny > 0.86f -> { ny = 0.88f; edgeY = 3 }
                                }
                            }
                            if ((edgeX != 0 && edgeX != lastSnapEdgeX) || (edgeY != 0 && edgeY != lastSnapEdgeY)) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            lastSnapEdgeX = edgeX
                            lastSnapEdgeY = edgeY
                            snapGuideX = sx
                            snapGuideY = sy

                            currentDesign = currentDesign.copy(
                                stickers = currentDesign.stickers.map { st ->
                                    if (st.localId == id) st.copy(x = nx, y = ny) else st
                                }
                            )
                        },
                        onStickerDragEnd = { id, droppedInTrash ->
                            val before = preDragDesign
                            preDragDesign = null
                            isDraggingSticker = false
                            isHoveringTrash = false
                            snapGuideX = false
                            snapGuideY = false
                            lastSnapEdgeX = 0
                            lastSnapEdgeY = 0
                            if (before != null) {
                                undoStack = (undoStack + before).takeLast(30)
                                redoStack = emptyList()
                            }
                            if (droppedInTrash) {
                                currentDesign = currentDesign.copy(
                                    stickers = currentDesign.stickers.filterNot { it.localId == id }
                                )
                                if (selectedStickerId == id) selectedStickerId = null
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        },
                        onTransformSticker = { id, zoomDelta, rotDelta ->
                            currentDesign = currentDesign.copy(
                                stickers = currentDesign.stickers.map { st ->
                                    if (st.localId == id) {
                                        st.copy(
                                            scale = (st.scale * zoomDelta).coerceIn(0.5f, 2.5f),
                                            rot = (st.rot + rotDelta).coerceIn(-45f, 45f)
                                        )
                                    } else {
                                        st
                                    }
                                }
                            )
                        }
                    )
                }
            }

            // Contextual Toolbar below preview: Trash drop bar OR Selected sticker toolbar OR hint
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                when {
                    isDraggingSticker -> {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isHoveringTrash) Color(0xFFDC2626) else Color(0xFFFEE2E2),
                            border = BorderStroke(1.8.dp, Color(0xFFDC2626)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("workshop_trash_zone")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.DeleteOutline,
                                    contentDescription = "Trash",
                                    tint = if (isHoveringTrash) Color.White else Color(0xFFDC2626)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (isHoveringTrash) "Release to delete sticker" else "Drag down here to delete",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isHoveringTrash) Color.White else Color(0xFFB91C1C)
                                )
                            }
                        }
                    }

                    selectedSticker != null -> {
                        SelectedStickerMiniToolbar(
                            sticker = selectedSticker,
                            onCycleStyle = {
                                val allStyles = CardStickerStyle.entries
                                val nextStyle = allStyles[(allStyles.indexOf(selectedSticker.style) + 1) % allStyles.size]
                                pushEdit(
                                    currentDesign.copy(
                                        stickers = currentDesign.stickers.map {
                                            if (it.localId == selectedSticker.localId) it.copy(style = nextStyle) else it
                                        }
                                    )
                                )
                            },
                            onSelectColor = { hex ->
                                pushEdit(
                                    currentDesign.copy(
                                        stickers = currentDesign.stickers.map {
                                            if (it.localId == selectedSticker.localId) it.copy(color = hex) else it
                                        }
                                    )
                                )
                            },
                            onDuplicate = {
                                if (currentDesign.stickers.size >= CardDesignDefaults.MAX_STICKERS) {
                                    Toast.makeText(context, "Max 12 stickers reached", Toast.LENGTH_SHORT).show()
                                } else {
                                    val copy = selectedSticker.copy(
                                        localId = UUID.randomUUID().toString(),
                                        x = (selectedSticker.x + 0.08f).coerceIn(0.08f, 0.92f),
                                        y = (selectedSticker.y + 0.08f).coerceIn(0.08f, 0.92f)
                                    )
                                    pushEdit(currentDesign.copy(stickers = currentDesign.stickers + copy))
                                    selectedStickerId = copy.localId
                                    newlyAddedStickerId = copy.localId
                                }
                            },
                            onBringToFront = {
                                val reordered = currentDesign.stickers.filterNot { it.localId == selectedSticker.localId } + selectedSticker
                                pushEdit(currentDesign.copy(stickers = reordered))
                            },
                            onDelete = {
                                pushEdit(
                                    currentDesign.copy(
                                        stickers = currentDesign.stickers.filterNot { it.localId == selectedSticker.localId }
                                    )
                                )
                                selectedStickerId = null
                            }
                        )
                    }

                    else -> {
                        Text(
                            text = "${currentDesign.stickers.size}/12 stickers  •  Tap a sticker to edit, drag to move, pinch to scale & rotate",
                            fontSize = 11.5.sp,
                            color = funkyColors.mute,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Bottom Sheet Dock with 4 Tabs
            Surface(
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = funkyColors.surface,
                border = BorderStroke(1.5.dp, funkyColors.line),
                shadowElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // 4-tab header bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        WorkshopTab.entries.forEach { tab ->
                            val selected = activeTab == tab
                            Surface(
                                shape = RoundedCornerShape(100.dp),
                                color = if (selected) Color(0xFFFFB800) else Color.Transparent,
                                border = if (selected) BorderStroke(1.8.dp, Color(0xFF1A1A1A)) else null,
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("workshop_tab_${tab.name.lowercase()}")
                                    .clip(RoundedCornerShape(100.dp))
                                    .clickable { activeTab = tab }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = tab.label,
                                        fontSize = 13.sp,
                                        fontWeight = if (selected) FontWeight.Black else FontWeight.Bold,
                                        color = if (selected) Color(0xFF1A1A1A) else funkyColors.mute
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = funkyColors.line)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        when (activeTab) {
                            WorkshopTab.STICKERS -> WorkshopStickersTab(
                                subTab = stickerSubTab,
                                onSelectSubTab = { stickerSubTab = it },
                                stickerCount = currentDesign.stickers.size,
                                stickerPacks = stickerPacks,
                                isLoadingPacks = isLoadingPacks,
                                packsError = packsError,
                                onRetryPacks = { loadPacks(force = true) },
                                onAddWordSticker = { word ->
                                    addSticker(
                                        CardSticker(
                                            kind = CardStickerKind.WORD,
                                            text = word.take(CardDesignDefaults.MAX_STICKER_TEXT_LEN),
                                            style = CardStickerStyle.STICKER,
                                            color = currentDesign.theme.accent
                                        )
                                    )
                                },
                                onAddEmojiSticker = { emoji ->
                                    addSticker(
                                        CardSticker(
                                            kind = CardStickerKind.EMOJI,
                                            emoji = emoji,
                                            style = CardStickerStyle.STICKER,
                                            color = "#1C1917"
                                        )
                                    )
                                },
                                onAddPackSticker = { pack, idx, emoji ->
                                    addSticker(
                                        CardSticker(
                                            kind = CardStickerKind.PACK,
                                            emoji = emoji,
                                            packId = pack.id,
                                            packCode = pack.code,
                                            packIdx = idx,
                                            style = CardStickerStyle.STICKER,
                                            color = "#1C1917"
                                        )
                                    )
                                },
                                onLockedPackClick = { pack ->
                                    Toast.makeText(
                                        context,
                                        "\"${pack.name}\" costs ${pack.price_coins} coins. Unlock sticker packs in the store.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            )

                            WorkshopTab.STYLE -> WorkshopStyleTab(
                                theme = currentDesign.theme,
                                onUpdateTheme = { newTheme ->
                                    pushEdit(currentDesign.copy(theme = newTheme))
                                }
                            )

                            WorkshopTab.SHOW -> WorkshopShowTab(
                                show = currentDesign.show,
                                hiLabel = currentDesign.hiLabel,
                                onUpdateShow = { newShow ->
                                    pushEdit(currentDesign.copy(show = newShow))
                                },
                                onUpdateHiLabel = { nextHi ->
                                    pushEdit(currentDesign.copy(hiLabel = nextHi))
                                }
                            )

                            WorkshopTab.REMIX -> WorkshopRemixTab(
                                remixable = remixable,
                                onToggleRemixable = { next -> remixable = next },
                                cards = remixCards,
                                isLoading = isLoadingRemix,
                                isLoadingMore = isLoadingMoreRemix,
                                hasMore = remixHasMore,
                                errorMessage = remixError,
                                onRetry = { loadRemixCards(reset = true) },
                                onLoadMore = { loadRemixCards(reset = false) },
                                onSelectCard = { item -> previewRemixItem = item }
                            )
                        }
                    }
                }
            }
        }

        // Live Toast Banner ("Your card is live", "Restored draft", etc.)
        AnimatedVisibility(
            visible = liveBannerMessage != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 62.dp)
                .zIndex(20f)
        ) {
            Surface(
                shape = RoundedCornerShape(100.dp),
                color = Color(0xFF16A34A),
                border = BorderStroke(2.dp, Color(0xFF1A1A1A)),
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = liveBannerMessage.orEmpty(),
                        color = Color.White,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        // Celebratory Confetti Overlay
        if (showConfetti && !reduceAnimations) {
            WorkshopConfettiOverlay(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(25f)
            )
        }
    }

    // Full Remix Preview Modal
    previewRemixItem?.let { item ->
        val fullPreviewProfile = remember(item) {
            PeopleCardProfile(
                userId = item.userId,
                name = item.creatorName,
                username = item.creatorUsername,
                avatarUrl = item.creatorAvatarUrl,
                countryCode = item.countryCode,
                nativeLang = "EN",
                learningLang = "ES"
            )
        }
        Dialog(onDismissRequest = { if (!isRemixing) previewRemixItem = null }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = funkyColors.surface,
                border = BorderStroke(2.dp, funkyColors.ink),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "${item.creatorName}'s card",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        color = funkyColors.ink
                    )
                    PeopleCard(
                        profile = fullPreviewProfile,
                        design = item.design
                    )
                    Text(
                        text = "Remixed ${item.remixCount} times • Remixing replaces your current card and lets you tweak it.",
                        fontSize = 12.5.sp,
                        color = funkyColors.mute
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { previewRemixItem = null },
                            enabled = !isRemixing,
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp)
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                scope.launch {
                                    isRemixing = true
                                    val res = CardDesignRepository.remixCardDesign(
                                        context = context,
                                        sourceUserId = item.userId,
                                        availablePacks = stickerPacks
                                    )
                                    isRemixing = false
                                    res.onSuccess { result ->
                                        pushEdit(result.design, recordHistory = true)
                                        selectedStickerId = null
                                        previewRemixItem = null
                                        activeTab = WorkshopTab.STICKERS
                                        if (result.droppedLockedPackStickers > 0) {
                                            liveBannerMessage = "${result.droppedLockedPackStickers} sticker(s) from locked packs were removed."
                                        } else {
                                            liveBannerMessage = "Remixed! Tweak anything or hit Undo."
                                        }
                                        onCardSaved(result.design)
                                    }.onFailure { err ->
                                        Toast.makeText(
                                            context,
                                            err.message ?: "Could not remix this card.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            },
                            enabled = !isRemixing,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFFB800),
                                contentColor = Color(0xFF1A1A1A)
                            ),
                            border = BorderStroke(1.8.dp, Color(0xFF1A1A1A)),
                            modifier = Modifier
                                .weight(1.3f)
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("workshop_confirm_remix_btn")
                        ) {
                            if (isRemixing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFF1A1A1A)
                                )
                            } else {
                                Text("Remix this", fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }
        }
    }

    // Confirm Discard Unsaved Changes Dialog
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Leave without saving?", fontWeight = FontWeight.ExtraBold) },
            text = { Text("You have unsaved edits on your card. What would you like to do?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        CardDesignRepository.clearDraft(context, myUid)
                        onDismiss()
                    },
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                ) {
                    Text("Discard", color = funkyColors.danger, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDiscardDialog = false },
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                ) {
                    Text("Keep editing", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Confirm Reset Card Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset card?", fontWeight = FontWeight.ExtraBold) },
            text = { Text("This removes your stickers and custom theme, returning your card to default.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetDialog = false
                        resetCard()
                    },
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                ) {
                    Text("Reset", color = funkyColors.danger, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showResetDialog = false },
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SelectedStickerMiniToolbar(
    sticker: CardSticker,
    onCycleStyle: () -> Unit,
    onSelectColor: (String) -> Unit,
    onDuplicate: () -> Unit,
    onBringToFront: () -> Unit,
    onDelete: () -> Unit
) {
    val funkyColors = LocalFunkyColors.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = funkyColors.surface,
        border = BorderStroke(1.5.dp, funkyColors.ink),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("workshop_sticker_toolbar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(100.dp),
                color = funkyColors.soft,
                border = BorderStroke(1.dp, funkyColors.line),
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("workshop_sticker_style_btn")
                    .clip(RoundedCornerShape(100.dp))
                    .clickable(onClick = onCycleStyle)
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = sticker.style.label,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = funkyColors.ink
                    )
                }
            }

            // Sticker Color dots
            CardDesignDefaults.stickerColorSwatches.forEach { hex ->
                val c = parseCardColorOrDefault(hex, Color(0xFF1C1917))
                val selected = sticker.color.equals(hex, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable { onSelectColor(hex) },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(c)
                            .border(
                                width = if (selected) 2.5.dp else 1.dp,
                                color = if (selected) Color(0xFFFFB800) else funkyColors.ink.copy(alpha = 0.4f),
                                shape = CircleShape
                            )
                    )
                }
            }

            IconButton(
                onClick = onDuplicate,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("workshop_sticker_duplicate_btn")
            ) {
                Icon(
                    imageVector = Icons.Outlined.ContentCopy,
                    contentDescription = "Duplicate sticker",
                    tint = funkyColors.ink
                )
            }

            IconButton(
                onClick = onBringToFront,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("workshop_sticker_front_btn")
            ) {
                Icon(
                    imageVector = Icons.Outlined.FlipToFront,
                    contentDescription = "Bring sticker to front",
                    tint = funkyColors.ink
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("workshop_sticker_delete_btn")
            ) {
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = "Delete sticker",
                    tint = funkyColors.danger
                )
            }
        }
    }
}

private data class ConfettiParticle(
    val startX: Float,
    val vx: Float,
    val speedY: Float,
    val spin: Float,
    val sizeDp: Float,
    val color: Color
)

@Composable
private fun WorkshopConfettiOverlay(modifier: Modifier = Modifier) {
    val progress = remember { Animatable(0f) }
    val particles = remember {
        val palette = listOf(
            Color(0xFFFFB800),
            Color(0xFF10B981),
            Color(0xFFEC4899),
            Color(0xFF3B82F6),
            Color(0xFF8B5CF6),
            Color(0xFFF97316)
        )
        List(44) { idx ->
            val rnd = Random(idx * 37 + 13)
            ConfettiParticle(
                startX = rnd.nextFloat(),
                vx = (rnd.nextFloat() - 0.5f) * 0.25f,
                speedY = 0.55f + rnd.nextFloat() * 0.7f,
                spin = (rnd.nextFloat() - 0.5f) * 720f,
                sizeDp = 6f + rnd.nextFloat() * 6f,
                color = palette[idx % palette.size]
            )
        }
    }

    LaunchedEffect(Unit) {
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = tween(1600, easing = LinearEasing))
    }

    Canvas(modifier = modifier) {
        val t = progress.value
        val alpha = (1f - t).coerceIn(0f, 1f)
        particles.forEach { p ->
            val px = ((p.startX + p.vx * t) * size.width)
            val py = (t * p.speedY * size.height * 0.75f)
            val s = p.sizeDp.dp.toPx()
            rotate(degrees = p.spin * t, pivot = Offset(px, py)) {
                drawRect(
                    color = p.color.copy(alpha = alpha),
                    topLeft = Offset(px - s / 2f, py - s / 2f),
                    size = Size(s, s * 0.65f)
                )
            }
        }
    }
}

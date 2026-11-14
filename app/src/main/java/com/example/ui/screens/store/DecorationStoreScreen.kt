package com.example.ui.screens.store

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.R
import com.example.StoreItemRow
import com.example.gifting.CuteCoinIcon
import com.example.store.*
import com.example.ui.components.SoftWarningIcon
import com.example.ui.theme.AppThemeManager
import com.example.ui.theme.LocalAppDarkTheme
import com.example.ui.theme.LocalFunkyColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val ColorBg: Color
    @Composable get() = LocalFunkyColors.current.bg
private val ColorSurface: Color
    @Composable get() = LocalFunkyColors.current.surface
private val ColorSurfaceTrack: Color
    @Composable get() = LocalFunkyColors.current.soft
private val ColorPrimary: Color
    @Composable get() = LocalFunkyColors.current.mango
private val ColorPrimarySoft: Color
    @Composable get() = Color(0xFFFFD43A)
private val ColorPrimaryContainer: Color
    @Composable get() = LocalFunkyColors.current.soft
private val ColorOnPrimary: Color
    @Composable get() = LocalFunkyColors.current.onMango
private val ColorTextPrimary: Color
    @Composable get() = LocalFunkyColors.current.ink
private val ColorTextSecondary: Color
    @Composable get() = LocalFunkyColors.current.mute
private val ColorOutline: Color
    @Composable get() = LocalFunkyColors.current.line
private val ColorSuccess: Color
    @Composable get() = LocalFunkyColors.current.ok
private val ColorSuccessContainer: Color
    @Composable get() = if (LocalAppDarkTheme.current) Color(0xFF143523) else Color(0xFFEBFCF2)
private val ColorError: Color
    @Composable get() = LocalFunkyColors.current.danger
private val ColorErrorContainer: Color
    @Composable get() = if (LocalAppDarkTheme.current) Color(0xFF3A1818) else Color(0xFFFDECEC)

enum class StoreTab(val displayName: String, val categoryType: String?) {
    ALL("All", null),
    AVATAR("Avatar", "avatar_frame"),
    ENTRY("Entry", "entry_animation"),
    BUBBLE("Bubble", "bubble_theme"),
    THEME("Theme", "chat_bg")
}

private fun getCategoryDisplayName(type: String): String = when (type) {
    "avatar_frame" -> "Avatar Frames"
    "entry_animation" -> "Entry Animations"
    "bubble_theme" -> "Message Bubbles"
    "chat_bg" -> "Chat Themes"
    else -> type
}

private fun getCategoryIcon(type: String): String = when (type) {
    "avatar_frame" -> "👑"
    "entry_animation" -> "🚀"
    "bubble_theme" -> "💬"
    "chat_bg" -> "🎨"
    else -> "✨"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DecorationStoreScreen(
    onDismiss: () -> Unit,
    currentUserAvatar: String? = null,
    currentUserName: String = "You"
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    BackHandler { onDismiss() }

    val walletBalance by CosmeticsManager.userWalletBalance.collectAsState()
    val myCosmetics by CosmeticsManager.myCosmetics.collectAsState()

    var selectedTab by remember { mutableStateOf(StoreTab.ALL) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var allItems by remember { mutableStateOf<List<StoreItemRow>>(emptyList()) }

    // Selected items map: Category Type -> StoreItemRow
    // Ensuring only one item can be selected per category (selecting another replaces the previous one)
    var selectedItems by remember { mutableStateOf<Map<String, StoreItemRow>>(emptyMap()) }

    // Modals and dialog states
    var showPreviewModal by remember { mutableStateOf(false) }
    var showCoinStoreDialog by remember { mutableStateOf(false) }
    var insufficientCoinsError by remember { mutableStateOf(false) }
    var generalErrorDialogMessage by remember { mutableStateOf<String?>(null) }
    var isPurchasing by remember { mutableStateOf(false) }
    var activeMiniPreviewItem by remember { mutableStateOf<StoreItemRow?>(null) }

    val validCategories = remember { listOf("avatar_frame", "entry_animation", "bubble_theme", "chat_bg") }

    fun loadStoreData() {
        scope.launch {
            isLoading = true
            errorMessage = null
            CosmeticsManager.getOrCreateWallet(context)
            val res = CosmeticsManager.getStoreItems(context, type = null)
            res.onSuccess { items ->
                // Filter out name_effect and sticker_pack as requested
                allItems = items.filter { it.item_type in validCategories }
                isLoading = false
            }.onFailure { err ->
                errorMessage = err.message ?: "Failed to load store items"
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadStoreData()
    }

    // Purchase execution handler
    fun executePurchase(itemsToBuy: List<StoreItemRow>, apply: Boolean) {
        if (itemsToBuy.isEmpty()) return
        scope.launch {
            isPurchasing = true
            val ids = itemsToBuy.map { it.id }
            val res = CosmeticsManager.purchaseItems(context, ids, apply)
            isPurchasing = false

            res.onSuccess { result ->
                val spentMsg = if (result.coinsSpent > 0) " (Spent ${result.coinsSpent} 🪙)" else ""
                val actionMsg = if (apply) "Purchased & applied successfully!" else "Purchased successfully!"
                Toast.makeText(context, "$actionMsg$spentMsg", Toast.LENGTH_LONG).show()

                // Clear selection and refresh items
                selectedItems = emptyMap()
                showPreviewModal = false
                loadStoreData()
            }.onFailure { err ->
                val raw = err.message.orEmpty()
                when {
                    raw.contains("insufficient_coins", ignoreCase = true) -> {
                        insufficientCoinsError = true
                    }
                    raw.contains("item_not_found", ignoreCase = true) -> {
                        generalErrorDialogMessage = "This item is no longer available"
                    }
                    else -> {
                        generalErrorDialogMessage = "Please try again"
                    }
                }
            }
        }
    }

    // Unequip handler
    fun executeUnequip(item: StoreItemRow) {
        scope.launch {
            val res = CosmeticsManager.unequipSlot(context, item.item_type)
            res.onSuccess {
                Toast.makeText(context, "Decoration removed", Toast.LENGTH_SHORT).show()
                loadStoreData()
            }.onFailure {
                Toast.makeText(context, "Please try again", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        containerColor = ColorBg,
        topBar = {
            StoreTopBar(
                coinsBalance = walletBalance,
                onBack = onDismiss,
                onAddCoins = { showCoinStoreDialog = true }
            )
        },
        bottomBar = {
            // "Selected (n)" Tray (requirement 3)
            AnimatedVisibility(
                visible = selectedItems.isNotEmpty(),
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                SelectedItemsTray(
                    selectedItems = selectedItems.values.toList(),
                    onRemoveItem = { item ->
                        selectedItems = selectedItems - item.item_type
                    },
                    onOpenPreview = { showPreviewModal = true },
                    onBuyAndApply = {
                        executePurchase(selectedItems.values.toList(), apply = true)
                    },
                    isProcessing = isPurchasing
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tabs Row
            StoreTabsRow(
                selectedTab = selectedTab,
                onTabSelect = { selectedTab = it }
            )

            // Main Content Area
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when {
                    isLoading -> {
                        // Requirement 8: Skeleton Loading State
                        StoreSkeletonLoading()
                    }
                    errorMessage != null -> {
                        // Requirement 8: Error State with Retry
                        StoreErrorState(
                            message = errorMessage.orEmpty(),
                            onRetry = { loadStoreData() }
                        )
                    }
                    allItems.isEmpty() -> {
                        // Requirement 8: Empty State
                        StoreEmptyState(onRefresh = { loadStoreData() })
                    }
                    selectedTab == StoreTab.ALL -> {
                        // "All" Tab: Each category has 4 items row & "View All"
                        StoreAllTabContent(
                            allItems = allItems,
                            selectedItemIds = selectedItems.values.map { it.id }.toSet(),
                            onSelectItem = { item ->
                                selectedItems = if (selectedItems[item.item_type]?.id == item.id) {
                                    selectedItems - item.item_type
                                } else {
                                    selectedItems + (item.item_type to item)
                                }
                            },
                            onUnequipItem = { executeUnequip(it) },
                            onViewAllCategory = { categoryType ->
                                selectedTab = when (categoryType) {
                                    "avatar_frame" -> StoreTab.AVATAR
                                    "entry_animation" -> StoreTab.ENTRY
                                    "bubble_theme" -> StoreTab.BUBBLE
                                    "chat_bg" -> StoreTab.THEME
                                    else -> StoreTab.ALL
                                }
                            },
                            onPlayMiniPreview = { activeMiniPreviewItem = it }
                        )
                    }
                    else -> {
                        // Category Specific Tab: Full 2-column grid
                        val categoryType = selectedTab.categoryType.orEmpty()
                        val filteredItems = allItems.filter { it.item_type == categoryType }
                        StoreCategoryTabContent(
                            items = filteredItems,
                            categoryType = categoryType,
                            selectedItemIds = selectedItems.values.map { it.id }.toSet(),
                            onSelectItem = { item ->
                                selectedItems = if (selectedItems[item.item_type]?.id == item.id) {
                                    selectedItems - item.item_type
                                } else {
                                    selectedItems + (item.item_type to item)
                                }
                            },
                            onUnequipItem = { executeUnequip(it) },
                            onPlayMiniPreview = { activeMiniPreviewItem = it }
                        )
                    }
                }
            }
        }
    }

    // Requirement 4: Preview Screen Modal
    if (showPreviewModal) {
        DecorationPreviewDialog(
            selectedItems = selectedItems.values.toList(),
            currentUserAvatar = currentUserAvatar,
            currentUserName = currentUserName,
            isPurchasing = isPurchasing,
            onDismiss = { showPreviewModal = false },
            onBuyOnly = {
                executePurchase(selectedItems.values.toList(), apply = false)
            },
            onApplyAll = {
                executePurchase(selectedItems.values.toList(), apply = true)
            }
        )
    }

    // Entry Animation Mini Preview Modal
    activeMiniPreviewItem?.let { item ->
        EntryAnimationMiniPreviewDialog(
            item = item,
            userName = currentUserName,
            onDismiss = { activeMiniPreviewItem = null }
        )
    }

    // Full-screen Coin Store (when clicking "+" or on insufficient coins)
    if (showCoinStoreDialog || insufficientCoinsError) {
        CoinStoreScreen(
            currentBalance = walletBalance,
            insufficientCoinsAlert = insufficientCoinsError,
            onDismiss = {
                showCoinStoreDialog = false
                insufficientCoinsError = false
            },
            onPurchaseCoins = { addedCoins ->
                CosmeticsManager.updateWalletBalance(walletBalance + addedCoins)
            }
        )
    }

    // General error dialog
    genericMessageDialog(
        message = generalErrorDialogMessage,
        onDismiss = { generalErrorDialogMessage = null }
    )
}

// -----------------------------------------------------------------------------
// TOP BAR
// -----------------------------------------------------------------------------
@Composable
private fun StoreTopBar(
    coinsBalance: Int,
    onBack: () -> Unit,
    onAddCoins: () -> Unit
) {
    val context = LocalContext.current
    val isDark = LocalAppDarkTheme.current
    Surface(
        color = ColorBg,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(ColorSurface)
                        .border(1.dp, ColorOutline, CircleShape)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_back),
                        contentDescription = "Back",
                        tint = ColorTextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Text(
                        text = "Decoration Store",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Style your chat & profile",
                        fontSize = 12.sp,
                        color = ColorTextSecondary
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Theme switcher (Light / Dark)
                IconButton(
                    onClick = { AppThemeManager.toggleDarkTheme(context) },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(ColorSurfaceTrack)
                        .border(1.dp, ColorOutline, CircleShape)
                        .testTag("store_theme_toggle")
                ) {
                    Icon(
                        painter = painterResource(id = if (isDark) R.drawable.ic_sun else R.drawable.ic_moon),
                        contentDescription = if (isDark) "Switch to Light Theme" else "Switch to Dark Theme",
                        tint = if (isDark) Color(0xFFFBBF24) else Color(0xFF7C3AED),
                        modifier = Modifier.size(19.dp)
                    )
                }

                // Coins balance chip with "+" button
                Surface(
                    shape = RoundedCornerShape(100.dp),
                    color = ColorSurface,
                    border = BorderStroke(1.dp, ColorOutline),
                    shadowElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier
                            .clickable { onAddCoins() }
                            .padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CuteCoinIcon(modifier = Modifier.size(18.dp))
                        Text(
                            text = String.format("%,d", coinsBalance),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorTextPrimary
                        )

                        // "+" button
                        IconButton(
                            onClick = onAddCoins,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(ColorPrimary)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_plus),
                                contentDescription = "Add Coins",
                                tint = ColorOnPrimary,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// TABS ROW
// -----------------------------------------------------------------------------
@Composable
private fun StoreTabsRow(
    selectedTab: StoreTab,
    onTabSelect: (StoreTab) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(StoreTab.values()) { tab ->
            val isSelected = selectedTab == tab
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .clickable { onTabSelect(tab) },
                shape = RoundedCornerShape(100.dp),
                color = if (isSelected) ColorPrimary else ColorSurface,
                border = BorderStroke(1.dp, if (isSelected) ColorPrimary else ColorOutline)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (tab != StoreTab.ALL) {
                        Text(getCategoryIcon(tab.categoryType.orEmpty()), fontSize = 14.sp)
                    }
                    Text(
                        text = tab.displayName,
                        fontSize = 13.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) ColorOnPrimary else ColorTextSecondary
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// TAB "ALL" CONTENT
// -----------------------------------------------------------------------------
@Composable
private fun StoreAllTabContent(
    allItems: List<StoreItemRow>,
    selectedItemIds: Set<Long>,
    onSelectItem: (StoreItemRow) -> Unit,
    onUnequipItem: (StoreItemRow) -> Unit,
    onViewAllCategory: (String) -> Unit,
    onPlayMiniPreview: (StoreItemRow) -> Unit
) {
    val categories = remember {
        listOf("avatar_frame", "entry_animation", "bubble_theme", "chat_bg")
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        categories.forEach { catType ->
            val catItems = allItems.filter { it.item_type == catType }
            if (catItems.isNotEmpty()) {
                item(key = "header_$catType") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(getCategoryIcon(catType), fontSize = 18.sp)
                            Text(
                                text = getCategoryDisplayName(catType),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorTextPrimary
                            )
                        }

                        Text(
                            text = "View All >",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB45309),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onViewAllCategory(catType) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                item(key = "row_$catType") {
                    // Show up to 4 items in a row as requested
                    val previewItems = catItems.take(4)
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(previewItems, key = { it.id }) { item ->
                            StoreItemCard(
                                item = item,
                                isSelected = selectedItemIds.contains(item.id),
                                onSelect = { onSelectItem(item) },
                                onUnequip = { onUnequipItem(item) },
                                onPlayMiniPreview = { onPlayMiniPreview(item) },
                                modifier = Modifier.width(155.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// CATEGORY TAB CONTENT
// -----------------------------------------------------------------------------
@Composable
private fun StoreCategoryTabContent(
    items: List<StoreItemRow>,
    categoryType: String,
    selectedItemIds: Set<Long>,
    onSelectItem: (StoreItemRow) -> Unit,
    onUnequipItem: (StoreItemRow) -> Unit,
    onPlayMiniPreview: (StoreItemRow) -> Unit
) {
    if (items.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No items available in this category yet",
                color = ColorTextSecondary,
                fontSize = 14.sp
            )
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 120.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items, key = { it.id }) { item ->
            StoreItemCard(
                item = item,
                isSelected = selectedItemIds.contains(item.id),
                onSelect = { onSelectItem(item) },
                onUnequip = { onUnequipItem(item) },
                onPlayMiniPreview = { onPlayMiniPreview(item) }
            )
        }
    }
}

// -----------------------------------------------------------------------------
// STORE ITEM CARD
// -----------------------------------------------------------------------------
@Composable
private fun StoreItemCard(
    item: StoreItemRow,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onUnequip: () -> Unit,
    onPlayMiniPreview: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = when {
        isSelected -> ColorPrimary
        item.equipped -> ColorSuccess
        else -> ColorOutline
    }
    val borderWidth = if (isSelected || item.equipped) 2.dp else 1.dp

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { onSelect() },
        shape = RoundedCornerShape(20.dp),
        color = ColorSurface,
        border = BorderStroke(borderWidth, borderColor),
        shadowElevation = if (isSelected) 3.dp else 1.dp
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Thumbnail Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(ColorBg),
                contentAlignment = Alignment.Center
            ) {
                // Item Preview Renderer
                ItemThumbnailRenderer(
                    item = item,
                    onPlayClick = onPlayMiniPreview
                )

                // Selection checkmark ✓ at top-right
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(ColorPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_check),
                            contentDescription = "Selected",
                            tint = ColorOnPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                // "Applied" green badge at top-left
                if (item.equipped) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp),
                        shape = RoundedCornerShape(100.dp),
                        color = ColorSuccessContainer,
                        border = BorderStroke(0.5.dp, ColorSuccess)
                    ) {
                        Text(
                            text = "Applied",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorSuccess,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Item Name
            Text(
                text = item.name,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = ColorTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            // Price or free tag
            val priceText = if (item.price_coins == 0) "0 🪙" else "${item.price_coins} 🪙"
            Text(
                text = priceText,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (item.price_coins == 0) Color(0xFF059669) else Color(0xFFB45309)
            )

            // If equipped, show "Remove" button
            if (item.equipped) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Remove",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorError,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onUnequip() }
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// ITEM THUMBNAIL RENDERER
// -----------------------------------------------------------------------------
@Composable
private fun ItemThumbnailRenderer(
    item: StoreItemRow,
    onPlayClick: () -> Unit
) {
    if (!item.preview_url.isNullOrBlank()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            AsyncImage(
                model = item.preview_url,
                contentDescription = item.name,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp),
                contentScale = ContentScale.Fit
            )
            if (item.item_type == "entry_animation") {
                PlayButtonOverlay(onClick = onPlayClick)
            }
        }
        return
    }

    // Code-based miniature previews when preview_url is empty
    when (item.item_type) {
        "avatar_frame" -> {
            val frameConfig = remember(item.config) {
                CosmeticsConfigParser.parseAvatarFrame(item.config)
            }
            AvatarFrame(
                config = frameConfig,
                sizeDp = 64.dp
            ) {
                AsyncImage(
                    model = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200",
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
        }
        "bubble_theme" -> {
            val bubbleConfig = remember(item.config) {
                CosmeticsConfigParser.parseBubbleTheme(item.config)
            }
            ThemedBubble(
                config = bubbleConfig,
                isOutgoing = true,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Text(
                    text = "Hello! ✨",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = parseHexColor(bubbleConfig.textColorHex, Color.Black)
                )
            }
        }
        "chat_bg" -> {
            val bgConfig = remember(item.config) {
                CosmeticsConfigParser.parseChatBg(item.config)
            }
            Box(modifier = Modifier.fillMaxSize()) {
                ChatBackground(config = bgConfig, isDark = LocalAppDarkTheme.current, modifier = Modifier.fillMaxSize())
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = ColorSurface.copy(alpha = 0.88f),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(4.dp)
                ) {
                    Text(
                        text = "Theme",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorTextPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
        "entry_animation" -> {
            val entryConfig = remember(item.config) {
                CosmeticsConfigParser.parseEntryEffect(item.config)
            }
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(entryConfig.badgeIcon, fontSize = 34.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = entryConfig.effectType.capitalize(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorTextSecondary
                    )
                }
                PlayButtonOverlay(onClick = onPlayClick)
            }
        }
        else -> {
            Text(item.name, fontSize = 12.sp, color = ColorTextSecondary)
        }
    }
}

@Composable
private fun BoxScope.PlayButtonOverlay(onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .align(Alignment.Center)
            .size(36.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.55f))
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_play),
            contentDescription = "Play Preview",
            tint = Color.White,
            modifier = Modifier.size(18.dp)
        )
    }
}

// -----------------------------------------------------------------------------
// SELECTED (N) TRAY (Requirement 3)
// -----------------------------------------------------------------------------
@Composable
private fun SelectedItemsTray(
    selectedItems: List<StoreItemRow>,
    onRemoveItem: (StoreItemRow) -> Unit,
    onOpenPreview: () -> Unit,
    onBuyAndApply: () -> Unit,
    isProcessing: Boolean
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = ColorSurface,
        border = BorderStroke(1.dp, ColorOutline),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            // Header: Selected (n)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Selected (${selectedItems.size})",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorTextPrimary
                    )
                    Text(
                        text = "• 1 item per category",
                        fontSize = 11.5.sp,
                        color = ColorTextSecondary
                    )
                }

                // Clear all
                Text(
                    text = "Clear",
                    fontSize = 12.sp,
                    color = ColorTextSecondary,
                    modifier = Modifier
                        .clickable { selectedItems.forEach { onRemoveItem(it) } }
                        .padding(4.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Row of selected items thumbnails with '✕' badge
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(selectedItems, key = { it.id }) { item ->
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ColorBg)
                            .border(1.dp, ColorPrimary, RoundedCornerShape(12.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            ItemMiniThumbnail(item)
                        }

                        // '✕' button at top-right
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.65f))
                                .clickable { onRemoveItem(item) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_close),
                                contentDescription = "Remove",
                                tint = Color.White,
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons: "Preview" and "Buy & Apply (n)"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // "Preview" button
                OutlinedButton(
                    onClick = onOpenPreview,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(100.dp),
                    border = BorderStroke(1.5.dp, ColorPrimary),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = ColorSurface,
                        contentColor = ColorTextPrimary
                    )
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_sparkle),
                        contentDescription = "Preview",
                        tint = ColorPrimary,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Preview",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // "Buy & Apply (n)" button
                Button(
                    onClick = onBuyAndApply,
                    enabled = !isProcessing,
                    modifier = Modifier
                        .weight(1.3f)
                        .height(48.dp),
                    shape = RoundedCornerShape(100.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ColorPrimary,
                        contentColor = ColorOnPrimary
                    )
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            color = ColorOnPrimary,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Buy & Apply (${selectedItems.size})",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemMiniThumbnail(item: StoreItemRow) {
    when (item.item_type) {
        "avatar_frame" -> Text("👑", fontSize = 22.sp)
        "entry_animation" -> {
            val entryConfig = remember(item.config) {
                CosmeticsConfigParser.parseEntryEffect(item.config)
            }
            Text(entryConfig.badgeIcon, fontSize = 22.sp)
        }
        "bubble_theme" -> Text("💬", fontSize = 22.sp)
        "chat_bg" -> Text("🎨", fontSize = 22.sp)
        else -> Text("✨", fontSize = 22.sp)
    }
}

// -----------------------------------------------------------------------------
// PREVIEW SCREEN DIALOG (Requirement 4)
// -----------------------------------------------------------------------------
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DecorationPreviewDialog(
    selectedItems: List<StoreItemRow>,
    currentUserAvatar: String?,
    currentUserName: String,
    isPurchasing: Boolean,
    onDismiss: () -> Unit,
    onBuyOnly: () -> Unit,
    onApplyAll: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 4 })
    val previewTabs = remember { listOf("Avatar", "Entry", "Bubble", "Theme") }
    val scope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(ColorBg),
            color = ColorBg
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
            ) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(ColorSurface)
                            .border(1.dp, ColorOutline, CircleShape)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_close),
                            contentDescription = "Close",
                            tint = ColorTextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Text(
                        text = "Live Preview",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorTextPrimary
                    )

                    Spacer(modifier = Modifier.size(36.dp))
                }

                // Top Hero Section: User avatar with selected frame
                val selectedFrame = selectedItems.find { it.item_type == "avatar_frame" }
                val frameConfig = remember(selectedFrame) {
                    selectedFrame?.config?.let { CosmeticsConfigParser.parseAvatarFrame(it) }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        AvatarFrame(
                            config = frameConfig,
                            sizeDp = 92.dp
                        ) {
                            AsyncImage(
                                model = currentUserAvatar ?: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=300",
                                contentDescription = "Avatar",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = currentUserName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorTextPrimary
                        )
                        Text(
                            text = if (selectedFrame != null) "Equipped: ${selectedFrame.name}" else "Default Frame",
                            fontSize = 12.sp,
                            color = ColorTextSecondary
                        )
                    }
                }

                // Category Tabs for Preview
                TabRow(
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = ColorSurface,
                    contentColor = ColorTextPrimary,
                    divider = {},
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                            color = ColorPrimary
                        )
                    }
                ) {
                    previewTabs.forEachIndexed { index, tabTitle ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                            text = {
                                Text(
                                    text = tabTitle,
                                    fontSize = 13.5.sp,
                                    fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Medium,
                                    color = if (pagerState.currentPage == index) ColorTextPrimary else ColorTextSecondary
                                )
                            }
                        )
                    }
                }

                // Swipeable Preview Pager
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(16.dp)
                ) { page ->
                    when (page) {
                        0 -> PreviewAvatarPage(selectedFrame = selectedFrame)
                        1 -> PreviewEntryPage(
                            selectedEntry = selectedItems.find { it.item_type == "entry_animation" },
                            userName = currentUserName
                        )
                        2 -> PreviewBubblePage(selectedBubble = selectedItems.find { it.item_type == "bubble_theme" })
                        3 -> PreviewThemePage(selectedTheme = selectedItems.find { it.item_type == "chat_bg" })
                    }
                }

                // Bottom Buttons: "Buy Only" and "Apply All (n)"
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = ColorSurface,
                    border = BorderStroke(1.dp, ColorOutline),
                    shadowElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onBuyOnly,
                            enabled = !isPurchasing,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(100.dp),
                            border = BorderStroke(1.dp, ColorTextSecondary)
                        ) {
                            Text(
                                text = "Buy Only",
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ColorTextPrimary
                            )
                        }

                        Button(
                            onClick = onApplyAll,
                            enabled = !isPurchasing,
                            modifier = Modifier
                                .weight(1.3f)
                                .height(48.dp),
                            shape = RoundedCornerShape(100.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ColorPrimary,
                                contentColor = ColorOnPrimary
                            )
                        ) {
                            if (isPurchasing) {
                                CircularProgressIndicator(
                                    color = ColorOnPrimary,
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = "Apply All (${selectedItems.size})",
                                    fontSize = 14.5.sp,
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

// Preview Page 0: Avatar
@Composable
private fun PreviewAvatarPage(selectedFrame: StoreItemRow?) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(20.dp))
            .background(ColorSurface)
            .border(1.dp, ColorOutline, RoundedCornerShape(20.dp))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        if (selectedFrame != null) {
            val frameConfig = remember(selectedFrame) {
                CosmeticsConfigParser.parseAvatarFrame(selectedFrame.config)
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                AvatarFrame(config = frameConfig, sizeDp = 100.dp) {
                    AsyncImage(
                        model = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=300",
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = selectedFrame.name,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorTextPrimary
                )
                Text(
                    text = selectedFrame.description ?: "Customized animated ring & effects",
                    fontSize = 12.5.sp,
                    color = ColorTextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Text("No avatar frame selected", color = ColorTextSecondary)
        }
    }
}

// Preview Page 1: Entry Animation
@Composable
private fun PreviewEntryPage(selectedEntry: StoreItemRow?, userName: String) {
    var isPlaying by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(20.dp))
            .background(ColorSurface)
            .border(1.dp, ColorOutline, RoundedCornerShape(20.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (selectedEntry != null) {
            val entryConfig = remember(selectedEntry) {
                CosmeticsConfigParser.parseEntryEffect(selectedEntry.config)
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Interactive arrival card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFFEF3C7),
                    border = BorderStroke(1.dp, Color(0xFFFDE047)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(entryConfig.badgeIcon, fontSize = 36.sp)
                        Column {
                            Text(
                                text = "$userName entered the room! ✨",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF92400E)
                            )
                            Text(
                                text = "with ${selectedEntry.name}",
                                fontSize = 12.5.sp,
                                color = Color(0xFFB45309)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { isPlaying = !isPlaying },
                    colors = ButtonDefaults.buttonColors(containerColor = ColorPrimary, contentColor = ColorOnPrimary),
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_refresh),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Replay Entry Effect", fontWeight = FontWeight.Bold)
                }
            }
        } else {
            Text("No entry animation selected", color = ColorTextSecondary)
        }
    }
}

// Preview Page 2: Bubble Theme
@Composable
private fun PreviewBubblePage(selectedBubble: StoreItemRow?) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(20.dp))
            .background(ColorBg)
            .border(1.dp, ColorOutline, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        if (selectedBubble != null) {
            val bubbleConfig = remember(selectedBubble) {
                CosmeticsConfigParser.parseBubbleTheme(selectedBubble.config)
            }

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center
            ) {
                // Incoming message
                ThemedBubble(
                    config = null,
                    isOutgoing = false,
                    modifier = Modifier.align(Alignment.Start)
                ) {
                    Text("Hey there! How does this bubble theme look? 😊", fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Outgoing message with selected bubble
                ThemedBubble(
                    config = bubbleConfig,
                    isOutgoing = true,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        text = "It looks vibrant, super stylish and fun! ✨",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = parseHexColor(bubbleConfig.textColorHex, Color.Black)
                    )
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No bubble theme selected", color = ColorTextSecondary)
            }
        }
    }
}

// Preview Page 3: Chat Theme Background
@Composable
private fun PreviewThemePage(selectedTheme: StoreItemRow?) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, ColorOutline, RoundedCornerShape(20.dp))
    ) {
        if (selectedTheme != null) {
            val bgConfig = remember(selectedTheme) {
                CosmeticsConfigParser.parseChatBg(selectedTheme.config)
            }

            ChatBackground(config = bgConfig, isDark = LocalAppDarkTheme.current, modifier = Modifier.fillMaxSize())

            // Sample floating chat bubbles over wallpaper
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.align(Alignment.Start)
                ) {
                    Text("Sample chat wallpaper preview", fontSize = 12.sp, modifier = Modifier.padding(10.dp))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = ColorPrimary.copy(alpha = 0.9f),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Chat background is live! 🎨", fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(10.dp))
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No chat theme selected", color = ColorTextSecondary)
            }
        }
    }
}

// -----------------------------------------------------------------------------
// ENTRY ANIMATION MINI PREVIEW DIALOG
// -----------------------------------------------------------------------------
@Composable
private fun EntryAnimationMiniPreviewDialog(
    item: StoreItemRow,
    userName: String,
    onDismiss: () -> Unit
) {
    val entryConfig = remember(item.config) {
        CosmeticsConfigParser.parseEntryEffect(item.config)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = ColorSurface,
            border = BorderStroke(1.dp, ColorOutline),
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Entry Preview",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorTextPrimary
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_close),
                            contentDescription = "Close",
                            tint = ColorTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(entryConfig.badgeIcon, fontSize = 56.sp)

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = item.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorTextPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = ColorSurfaceTrack,
                    border = BorderStroke(1.dp, ColorPrimary.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "$userName entered the room!",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorTextPrimary
                        )
                        Text(
                            text = "Special ${entryConfig.effectType} entrance effect",
                            fontSize = 12.sp,
                            color = ColorTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = ColorPrimary, contentColor = ColorOnPrimary),
                    shape = RoundedCornerShape(100.dp),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Text("Got It", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// SKELETON / EMPTY / ERROR STATES (Requirement 8)
// -----------------------------------------------------------------------------
@Composable
private fun StoreSkeletonLoading() {
    val infiniteTransition = rememberInfiniteTransition(label = "skeleton")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeletonAlpha"
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(6) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = ColorSurface,
                border = BorderStroke(1.dp, ColorOutline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(ColorSurfaceTrack.copy(alpha = alpha))
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(ColorSurfaceTrack.copy(alpha = alpha))
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.4f)
                            .height(12.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(ColorSurfaceTrack.copy(alpha = alpha))
                    )
                }
            }
        }
    }
}

@Composable
private fun StoreEmptyState(onRefresh: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Text("🎨", fontSize = 42.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No decorations found",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = ColorTextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "New items will be added to the catalog soon!",
                fontSize = 13.sp,
                color = ColorTextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onRefresh,
                colors = ButtonDefaults.buttonColors(containerColor = ColorPrimary, contentColor = ColorOnPrimary),
                shape = RoundedCornerShape(100.dp)
            ) {
                Text("Refresh", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun StoreErrorState(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            SoftWarningIcon(size = 38.dp, color = ColorError)
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Could not load store items",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = ColorTextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = message,
                fontSize = 12.5.sp,
                color = ColorTextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = ColorPrimary, contentColor = ColorOnPrimary),
                shape = RoundedCornerShape(100.dp)
            ) {
                Text("Try Again", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun genericMessageDialog(message: String?, onDismiss: () -> Unit) {
    if (message == null) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Information", fontWeight = FontWeight.Bold) },
        text = { Text(message) },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = ColorPrimary, contentColor = ColorOnPrimary)
            ) {
                Text("OK", fontWeight = FontWeight.Bold)
            }
        }
    )
}

private fun String.capitalize(): String = replaceFirstChar {
    if (it.isLowerCase()) it.titlecase() else it.toString()
}

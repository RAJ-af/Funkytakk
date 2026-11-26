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
import com.example.ModerationManager
import com.example.R
import com.example.StoreItemRow
import com.example.gifting.CuteCoinIcon
import com.example.store.*
import com.example.ui.components.FramedAvatar
import com.example.ui.components.SoftCheckIcon
import com.example.ui.components.SoftFilterIcon
import com.example.ui.components.SoftWarningIcon
import com.example.ui.theme.AppThemeManager
import com.example.ui.theme.LocalAppDarkTheme
import com.example.ui.theme.LocalFunkyColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

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
    CARD("Card", "card_design"),
    AVATAR("Avatar", "avatar_frame"),
    NAME("Name", "name_effect"),
    ENTRY("Entry", "entry_animation"),
    BUBBLE("Bubble", "bubble_theme"),
    THEME("Theme", "chat_bg")
}

private fun getCategoryDisplayName(type: String): String = when (type) {
    "card_design" -> "Discover Card"
    "avatar_frame" -> "Avatar Frames"
    "name_effect" -> "Name Effects"
    "entry_animation" -> "Entry Animations"
    "bubble_theme" -> "Message Bubbles"
    "chat_bg" -> "Chat Themes"
    else -> type
}

private fun getCategoryIcon(type: String): String = when (type) {
    "card_design" -> "🪪"
    "avatar_frame" -> "👑"
    "name_effect" -> "✨"
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
    currentUserName: String = "You",
    currentUserCardProfile: com.example.card.PeopleCardProfile? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val appFlags by com.example.AppFlagsRepository.flags.collectAsState()

    var showCardWorkshop by remember { mutableStateOf(false) }

    BackHandler(enabled = !showCardWorkshop) { onDismiss() }

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
    var isStaffAdmin by remember { mutableStateOf(false) }

    val validCategories = remember {
        listOf("avatar_frame", "name_effect", "entry_animation", "bubble_theme", "chat_bg")
    }

    fun loadStoreData() {
        scope.launch {
            isLoading = true
            errorMessage = null
            CosmeticsManager.getOrCreateWallet(context)
            val res = CosmeticsManager.getStoreItems(context, type = null)
            res.onSuccess { items ->
                val filtered = items.filter { it.item_type in validCategories }
                filtered.filter { it.item_type == "avatar_frame" }.forEach { item ->
                    val parsed = runCatching { CosmeticsConfigParser.parseAvatarFrame(item.config) }.getOrNull()
                    val version = parsed?.asset?.version ?: 1
                    CosmeticsManager.preloadFrameAsset(context, item.preview_url, version)
                    CosmeticsManager.preloadFrameAsset(context, parsed?.asset?.url, version)
                }
                allItems = filtered
                // Keep currently selected items synced with refreshed store configs
                if (selectedItems.isNotEmpty()) {
                    val byId = filtered.associateBy { it.id }
                    selectedItems = selectedItems.mapValues { (_, oldItem) ->
                        byId[oldItem.id] ?: oldItem
                    }
                }
                isLoading = false
            }.onFailure { err ->
                errorMessage = err.message ?: "Failed to load store items"
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        isStaffAdmin = ModerationManager.checkIsAdmin(context)
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
                    raw.contains("insufficient_coins", ignoreCase = true) ||
                        raw.contains("not enough coins", ignoreCase = true) -> {
                        if (appFlags.coinStoreEnabled) {
                            insufficientCoinsError = true
                        } else {
                            generalErrorDialogMessage = "You don't have enough coins."
                        }
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
                showAddCoinsButton = appFlags.coinStoreEnabled,
                onBack = onDismiss,
                onAddCoins = {
                    if (appFlags.coinStoreEnabled) {
                        showCoinStoreDialog = true
                    }
                }
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
                onTabSelect = { tab ->
                    if (tab == StoreTab.CARD) {
                        showCardWorkshop = true
                    } else {
                        selectedTab = tab
                    }
                }
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
                            currentUserAvatar = currentUserAvatar,
                            currentUserName = currentUserName,
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
                                    "name_effect" -> StoreTab.NAME
                                    "entry_animation" -> StoreTab.ENTRY
                                    "bubble_theme" -> StoreTab.BUBBLE
                                    "chat_bg" -> StoreTab.THEME
                                    else -> StoreTab.ALL
                                }
                            },
                            onPlayMiniPreview = { activeMiniPreviewItem = it },
                            onOpenCardWorkshop = { showCardWorkshop = true }
                        )
                    }
                    else -> {
                        // Category Specific Tab: Full 2-column grid
                        val categoryType = selectedTab.categoryType.orEmpty()
                        val filteredItems = allItems.filter { it.item_type == categoryType }
                        StoreCategoryTabContent(
                            items = filteredItems,
                            currentUserAvatar = currentUserAvatar,
                            currentUserName = currentUserName,
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
            isStaffAdmin = isStaffAdmin,
            onDismiss = { showPreviewModal = false },
            onBuyOnly = {
                executePurchase(selectedItems.values.toList(), apply = false)
            },
            onApplyAll = {
                executePurchase(selectedItems.values.toList(), apply = true)
            },
            onRefreshStoreAfterCalibration = { updatedCode, updatedAssetMap ->
                val curFrame = selectedItems["avatar_frame"]
                if (curFrame != null && curFrame.code == updatedCode && updatedAssetMap.isNotEmpty()) {
                    val newConfig = curFrame.config.orEmpty().toMutableMap()
                    newConfig["asset"] = updatedAssetMap
                    val updatedRow = curFrame.copy(config = newConfig)
                    selectedItems = selectedItems + ("avatar_frame" to updatedRow)
                }
                loadStoreData()
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

    // Full-screen Coin Store (only when coinStoreEnabled flag is true)
    if (appFlags.coinStoreEnabled && (showCoinStoreDialog || insufficientCoinsError)) {
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

    if (showCardWorkshop) {
        val fallbackUid = remember { com.example.SupabaseSessionManager.getUserId(context).orEmpty() }
        val effectiveProfile = currentUserCardProfile ?: com.example.card.PeopleCardProfile(
            userId = fallbackUid,
            name = currentUserName,
            avatarUrl = currentUserAvatar.orEmpty(),
            isOnline = true
        )
        com.example.card.CardWorkshopScreen(
            myProfile = effectiveProfile,
            onDismiss = { showCardWorkshop = false }
        )
    }
}

// -----------------------------------------------------------------------------
// TOP BAR
// -----------------------------------------------------------------------------
@Composable
private fun StoreTopBar(
    coinsBalance: Int,
    showAddCoinsButton: Boolean,
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

                // Coins balance chip (with "+" button only when coin store is enabled)
                Surface(
                    shape = RoundedCornerShape(100.dp),
                    color = ColorSurface,
                    border = BorderStroke(1.dp, ColorOutline),
                    shadowElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier
                            .then(if (showAddCoinsButton) Modifier.clickable { onAddCoins() } else Modifier)
                            .padding(
                                start = 10.dp,
                                end = if (showAddCoinsButton) 4.dp else 12.dp,
                                top = if (showAddCoinsButton) 4.dp else 6.dp,
                                bottom = if (showAddCoinsButton) 4.dp else 6.dp
                            ),
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

                        if (showAddCoinsButton) {
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
    currentUserAvatar: String?,
    currentUserName: String,
    selectedItemIds: Set<Long>,
    onSelectItem: (StoreItemRow) -> Unit,
    onUnequipItem: (StoreItemRow) -> Unit,
    onViewAllCategory: (String) -> Unit,
    onPlayMiniPreview: (StoreItemRow) -> Unit,
    onOpenCardWorkshop: () -> Unit = {}
) {
    val categories = remember {
        listOf("avatar_frame", "name_effect", "entry_animation", "bubble_theme", "chat_bg")
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        item(key = "card_workshop_banner") {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Color(0xFFFFF3C4),
                border = BorderStroke(2.dp, Color(0xFF1A1A1A)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 4.dp)
                    .testTag("store_open_card_workshop_card")
                    .clip(RoundedCornerShape(22.dp))
                    .clickable { onOpenCardWorkshop() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = "🪪 Card Workshop",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF1A1A1A)
                        )
                        Text(
                            text = "Decorate your Discover card with stickers, patterns & remix",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF44403C)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Surface(
                        shape = RoundedCornerShape(100.dp),
                        color = Color(0xFFFFB800),
                        border = BorderStroke(1.8.dp, Color(0xFF1A1A1A))
                    ) {
                        Text(
                            text = "Customize",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF1A1A1A),
                            modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

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
                                .testTag("store_view_all_$catType")
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onViewAllCategory(catType) }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
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
                                currentUserAvatar = currentUserAvatar,
                                currentUserName = currentUserName,
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
    currentUserAvatar: String?,
    currentUserName: String,
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
                currentUserAvatar = currentUserAvatar,
                currentUserName = currentUserName,
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
    currentUserAvatar: String?,
    currentUserName: String,
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
            .testTag("store_item_card_${item.id}")
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
            // Top Thumbnail Box (background surface clipped; frame art inside is never clipped)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(14.dp))
                        .background(ColorBg)
                )
                // Item Preview Renderer
                ItemThumbnailRenderer(
                    item = item,
                    currentUserAvatar = currentUserAvatar,
                    currentUserName = currentUserName,
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
    currentUserAvatar: String?,
    currentUserName: String,
    onPlayClick: () -> Unit
) {
    val context = LocalContext.current
    var previewUrlFailed by remember(item.preview_url) { mutableStateOf(false) }

    if (item.item_type == "avatar_frame") {
        val frameConfig = remember(item.config) {
            CosmeticsConfigParser.parseAvatarFrame(item.config)
        }
        val asset = frameConfig.asset
        if (asset != null || !frameConfig.kind.equals("asset", ignoreCase = true)) {
            val tileAvatarSize = if (frameConfig.kind.equals("asset", ignoreCase = true) && asset != null) {
                val ratio = (if (asset.innerRatio > 0f) asset.innerRatio else asset.avatarRatio)
                    .coerceIn(0.25f, 0.98f)
                val overflow = asset.overflowRatio.coerceIn(0f, 0.5f)
                val maxCanvas = 96.dp / (1f + overflow)
                maxCanvas * ratio
            } else {
                54.dp
            }
            FramedAvatar(
                avatarUrl = currentUserAvatar,
                avatarSize = tileAvatarSize,
                frame = frameConfig,
                enableAnimation = true
            )
            return
        }
    }

    if (!item.preview_url.isNullOrBlank() && !previewUrlFailed && item.item_type != "name_effect") {
        val req = remember(context, item.preview_url) {
            coil.request.ImageRequest.Builder(context)
                .data(item.preview_url)
                .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                .apply {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                        decoderFactory(coil.decode.ImageDecoderDecoder.Factory())
                    }
                }
                .listener(onError = { _, _ -> previewUrlFailed = true })
                .build()
        }
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            AsyncImage(
                model = req,
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

    // Code-based miniature previews when preview_url is empty or failed
    when (item.item_type) {
        "avatar_frame" -> {
            val frameConfig = remember(item.config) {
                CosmeticsConfigParser.parseAvatarFrame(item.config)
            }
            FramedAvatar(
                avatarUrl = currentUserAvatar,
                avatarSize = 54.dp,
                frame = frameConfig,
                enableAnimation = true
            )
        }
        "name_effect" -> {
            val nameConfig = remember(item.config) {
                CosmeticsConfigParser.parseNameEffect(item.config)
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                StyledName(
                    text = currentUserName.ifBlank { item.name },
                    config = nameConfig,
                    fontSize = 17.sp,
                    defaultColor = ColorTextPrimary,
                    animate = true,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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
// PREVIEW SCREEN DIALOG (Task A + Task B + Task C)
// -----------------------------------------------------------------------------
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun DecorationPreviewDialog(
    selectedItems: List<StoreItemRow>,
    currentUserAvatar: String?,
    currentUserName: String,
    isPurchasing: Boolean,
    isStaffAdmin: Boolean,
    onDismiss: () -> Unit,
    onBuyOnly: () -> Unit,
    onApplyAll: () -> Unit,
    onRefreshStoreAfterCalibration: (code: String, updatedAsset: Map<String, Any?>) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 5 })
    val previewTabs = remember { listOf("Avatar", "Name", "Entry", "Bubble", "Theme") }

    val selectedFrame = selectedItems.find { it.item_type == "avatar_frame" }
    val storedFrameConfig = remember(selectedFrame) {
        selectedFrame?.config?.let { CosmeticsConfigParser.parseAvatarFrame(it) }
    }
    val selectedNameEffect = selectedItems.find { it.item_type == "name_effect" }
    val nameEffectConfig = remember(selectedNameEffect) {
        selectedNameEffect?.config?.let { CosmeticsConfigParser.parseNameEffect(it) }
    }

    val isAssetFrameSelected = selectedFrame != null &&
        storedFrameConfig?.kind.equals("asset", ignoreCase = true) &&
        storedFrameConfig?.asset != null

    // Live calibration draft state (synced whenever the stored asset changes)
    val storedAsset = storedFrameConfig?.asset
    var draftInnerRatio by remember(selectedFrame?.code, storedAsset?.version, storedAsset?.innerRatio) {
        mutableStateOf((storedAsset?.innerRatio ?: 0.6f).coerceIn(0.30f, 0.95f))
    }
    var draftOffsetX by remember(selectedFrame?.code, storedAsset?.version, storedAsset?.offsetX) {
        mutableStateOf((storedAsset?.offsetX ?: 0f).coerceIn(-0.25f, 0.25f))
    }
    var draftOffsetY by remember(selectedFrame?.code, storedAsset?.version, storedAsset?.offsetY) {
        mutableStateOf((storedAsset?.offsetY ?: 0f).coerceIn(-0.25f, 0.25f))
    }
    var draftOverflowRatio by remember(selectedFrame?.code, storedAsset?.version, storedAsset?.overflowRatio) {
        mutableStateOf((storedAsset?.overflowRatio ?: 0f).coerceIn(0f, 0.50f))
    }
    var showCalibrationSheet by remember { mutableStateOf(false) }

    // Live FrameConfig reflecting current slider values while calibrating
    val liveFrameConfig = remember(
        storedFrameConfig,
        showCalibrationSheet,
        draftInnerRatio,
        draftOffsetX,
        draftOffsetY,
        draftOverflowRatio
    ) {
        val base = storedFrameConfig
        val baseAsset = base?.asset
        if (base != null && baseAsset != null && base.kind.equals("asset", ignoreCase = true)) {
            base.copy(
                asset = baseAsset.copy(
                    innerRatio = draftInnerRatio,
                    avatarRatio = draftInnerRatio,
                    scale = 1f / draftInnerRatio.coerceAtLeast(0.25f),
                    offsetX = draftOffsetX,
                    offsetY = draftOffsetY,
                    overflowRatio = draftOverflowRatio
                )
            )
        } else {
            base
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val dialogWindow = (androidx.compose.ui.platform.LocalView.current.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window
        LaunchedEffect(dialogWindow) {
            dialogWindow?.let { w ->
                androidx.core.view.WindowCompat.setDecorFitsSystemWindows(w, false)
            }
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = ColorBg,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(ColorSurface)
                            .border(1.dp, ColorOutline, CircleShape)
                            .testTag("preview_close_button")
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
                        color = ColorTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    )

                    if (isStaffAdmin && isAssetFrameSelected) {
                        Surface(
                            onClick = { showCalibrationSheet = true },
                            shape = RoundedCornerShape(100.dp),
                            color = ColorSurface,
                            border = BorderStroke(1.dp, ColorPrimary),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("preview_calibrate_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                SoftFilterIcon(size = 15.dp, color = ColorTextPrimary)
                                Text(
                                    text = "Calibrate",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ColorTextPrimary,
                                    maxLines = 1
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.size(48.dp))
                    }
                }
            },
            bottomBar = {
                // Fixed bottom bar: always respects navigation bar insets and 16.dp side padding
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = ColorSurface,
                    border = BorderStroke(1.dp, ColorOutline),
                    shadowElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onBuyOnly,
                            enabled = !isPurchasing,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("preview_buy_only_button"),
                            shape = RoundedCornerShape(100.dp),
                            border = BorderStroke(1.dp, ColorTextSecondary),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = "Buy Only",
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ColorTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Button(
                            onClick = onApplyAll,
                            enabled = !isPurchasing,
                            modifier = Modifier
                                .weight(1.3f)
                                .heightIn(min = 48.dp)
                                .testTag("preview_apply_all_button"),
                            shape = RoundedCornerShape(100.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ColorPrimary,
                                contentColor = ColorOnPrimary
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
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
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
            ) {
                // Top Hero Section: Header avatar using shared FramedAvatar
                val heroOuterSize = FramedAvatar.outerSize(76.dp, liveFrameConfig)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp, bottom = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier.defaultMinSize(minWidth = heroOuterSize, minHeight = heroOuterSize),
                            contentAlignment = Alignment.Center
                        ) {
                            FramedAvatar(
                                avatarUrl = currentUserAvatar,
                                avatarSize = 76.dp,
                                frame = liveFrameConfig,
                                enableAnimation = true
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        StyledName(
                            text = currentUserName,
                            config = nameEffectConfig,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            defaultColor = ColorTextPrimary,
                            animate = true
                        )
                        Text(
                            text = buildString {
                                append(if (selectedFrame != null) selectedFrame.name else "Default Frame")
                                if (selectedNameEffect != null) {
                                    append(" • ${selectedNameEffect.name}")
                                }
                            },
                            fontSize = 12.sp,
                            color = ColorTextSecondary
                        )
                    }
                }

                // Category Tabs for Preview
                ScrollableTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = ColorSurface,
                    contentColor = ColorTextPrimary,
                    edgePadding = 12.dp,
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

                // Swipeable Preview Pager (no fixed card height; expands & scrolls cleanly on small screens)
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.Top
                ) { page ->
                    when (page) {
                        0 -> PreviewAvatarPage(
                            selectedFrame = selectedFrame,
                            frameConfig = liveFrameConfig,
                            avatarUrl = currentUserAvatar,
                            isStaffAdmin = isStaffAdmin,
                            onOpenCalibrate = { showCalibrationSheet = true }
                        )
                        1 -> PreviewNamePage(selectedName = selectedNameEffect, userName = currentUserName)
                        2 -> PreviewEntryPage(
                            selectedEntry = selectedItems.find { it.item_type == "entry_animation" },
                            userName = currentUserName
                        )
                        3 -> PreviewBubblePage(selectedBubble = selectedItems.find { it.item_type == "bubble_theme" })
                        4 -> PreviewThemePage(selectedTheme = selectedItems.find { it.item_type == "chat_bg" })
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        // Staff-Only Frame Calibration Sheet (Task C)
        if (showCalibrationSheet && isStaffAdmin && selectedFrame != null && storedAsset != null) {
            FrameCalibrationBottomSheet(
                frameCode = selectedFrame.code,
                frameName = selectedFrame.name,
                isCalibrated = storedAsset.calibrated,
                innerRatio = draftInnerRatio,
                offsetX = draftOffsetX,
                offsetY = draftOffsetY,
                overflowRatio = draftOverflowRatio,
                onInnerRatioChange = { draftInnerRatio = it.coerceIn(0.30f, 0.95f) },
                onOffsetXChange = { draftOffsetX = it.coerceIn(-0.25f, 0.25f) },
                onOffsetYChange = { draftOffsetY = it.coerceIn(-0.25f, 0.25f) },
                onOverflowRatioChange = { draftOverflowRatio = it.coerceIn(0f, 0.50f) },
                onReset = {
                    draftInnerRatio = storedAsset.innerRatio.coerceIn(0.30f, 0.95f)
                    draftOffsetX = storedAsset.offsetX.coerceIn(-0.25f, 0.25f)
                    draftOffsetY = storedAsset.offsetY.coerceIn(-0.25f, 0.25f)
                    draftOverflowRatio = storedAsset.overflowRatio.coerceIn(0f, 0.50f)
                },
                onSaved = { updatedAssetMap ->
                    onRefreshStoreAfterCalibration(selectedFrame.code, updatedAssetMap)
                },
                onDismiss = { showCalibrationSheet = false }
            )
        }
    }
}

// Preview Page 0: Avatar (uses shared FramedAvatar + Size Strip)
@Composable
private fun PreviewAvatarPage(
    selectedFrame: StoreItemRow?,
    frameConfig: FrameConfig?,
    avatarUrl: String?,
    isStaffAdmin: Boolean,
    onOpenCalibrate: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 340.dp),
        shape = RoundedCornerShape(20.dp),
        color = ColorSurface,
        border = BorderStroke(1.dp, ColorOutline)
    ) {
        if (selectedFrame != null && frameConfig != null) {
            val bigAvatarSize = 96.dp
            val bigOuterSize = FramedAvatar.outerSize(bigAvatarSize, frameConfig)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.defaultMinSize(minWidth = bigOuterSize, minHeight = bigOuterSize),
                    contentAlignment = Alignment.Center
                ) {
                    FramedAvatar(
                        avatarUrl = avatarUrl,
                        avatarSize = bigAvatarSize,
                        frame = frameConfig,
                        enableAnimation = true
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = selectedFrame.name,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorTextPrimary,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = selectedFrame.description ?: "Customized animated avatar frame",
                    fontSize = 12.5.sp,
                    color = ColorTextSecondary,
                    textAlign = TextAlign.Center
                )

                val asset = frameConfig.asset
                if (isStaffAdmin && frameConfig.kind.equals("asset", ignoreCase = true) && asset != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(100.dp),
                            color = if (asset.calibrated) ColorSuccessContainer else ColorSurfaceTrack,
                            border = BorderStroke(
                                1.dp,
                                if (asset.calibrated) ColorSuccess.copy(alpha = 0.5f) else ColorOutline
                            )
                        ) {
                            Text(
                                text = if (asset.calibrated) "Calibrated" else "Not calibrated yet",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (asset.calibrated) ColorSuccess else ColorTextSecondary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        Surface(
                            onClick = onOpenCalibrate,
                            shape = RoundedCornerShape(100.dp),
                            color = ColorPrimary.copy(alpha = 0.18f),
                            border = BorderStroke(1.dp, ColorPrimary)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                SoftFilterIcon(size = 13.dp, color = ColorTextPrimary)
                                Text(
                                    text = "Calibrate",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ColorTextPrimary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
                HorizontalDivider(color = ColorOutline)
                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Size Strip",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ColorTextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))

                val sizeSamples = remember {
                    listOf(
                        40.dp to "Chat · 40 dp",
                        64.dp to "Seat · 64 dp",
                        120.dp to "Profile · 120 dp"
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.Bottom
                ) {
                    sizeSamples.forEach { (sampleSize, label) ->
                        val outer = FramedAvatar.outerSize(sampleSize, frameConfig).coerceAtLeast(48.dp)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            Box(
                                modifier = Modifier.defaultMinSize(minWidth = outer, minHeight = outer),
                                contentAlignment = Alignment.Center
                            ) {
                                FramedAvatar(
                                    avatarUrl = avatarUrl,
                                    avatarSize = sampleSize,
                                    frame = frameConfig,
                                    enableAnimation = true
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = label,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = ColorTextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 340.dp)
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No avatar frame selected", color = ColorTextSecondary)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FrameCalibrationBottomSheet(
    frameCode: String,
    frameName: String,
    isCalibrated: Boolean,
    innerRatio: Float,
    offsetX: Float,
    offsetY: Float,
    overflowRatio: Float,
    onInnerRatioChange: (Float) -> Unit,
    onOffsetXChange: (Float) -> Unit,
    onOffsetYChange: (Float) -> Unit,
    onOverflowRatioChange: (Float) -> Unit,
    onReset: () -> Unit,
    onSaved: (Map<String, Any?>) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    var isSaving by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var statusIsError by remember { mutableStateOf(false) }
    var savedCalibratedState by remember(frameCode, isCalibrated) { mutableStateOf(isCalibrated) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ColorSurface,
        scrimColor = Color.Black.copy(alpha = 0.16f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { BottomSheetDefaults.DragHandle(color = ColorOutline) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 18.dp)
        ) {
            // Header + Calibration Flag
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Calibrate Frame",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorTextPrimary
                    )
                    Text(
                        text = "$frameName ($frameCode)",
                        fontSize = 12.sp,
                        color = ColorTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(100.dp),
                    color = if (savedCalibratedState) ColorSuccessContainer else ColorSurfaceTrack,
                    border = BorderStroke(
                        1.dp,
                        if (savedCalibratedState) ColorSuccess.copy(alpha = 0.55f) else ColorOutline
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        if (savedCalibratedState) {
                            SoftCheckIcon(size = 13.dp, color = ColorSuccess)
                        }
                        Text(
                            text = if (savedCalibratedState) "Calibrated" else "Not calibrated yet",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (savedCalibratedState) ColorSuccess else ColorTextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            CalibrationSliderRow(
                label = "Inner ratio",
                value = innerRatio,
                valueRange = 0.30f..0.95f,
                onValueChange = {
                    statusMessage = null
                    onInnerRatioChange(it)
                }
            )
            CalibrationSliderRow(
                label = "Offset X",
                value = offsetX,
                valueRange = -0.25f..0.25f,
                onValueChange = {
                    statusMessage = null
                    onOffsetXChange(it)
                }
            )
            CalibrationSliderRow(
                label = "Offset Y",
                value = offsetY,
                valueRange = -0.25f..0.25f,
                onValueChange = {
                    statusMessage = null
                    onOffsetYChange(it)
                }
            )
            CalibrationSliderRow(
                label = "Overflow",
                value = overflowRatio,
                valueRange = 0f..0.50f,
                onValueChange = {
                    statusMessage = null
                    onOverflowRatioChange(it)
                }
            )

            statusMessage?.let { msg ->
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (statusIsError) ColorErrorContainer else ColorSuccessContainer,
                    border = BorderStroke(
                        1.dp,
                        if (statusIsError) ColorError.copy(alpha = 0.45f) else ColorSuccess.copy(alpha = 0.45f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = msg,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (statusIsError) ColorError else ColorSuccess,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        statusMessage = null
                        onReset()
                    },
                    enabled = !isSaving,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("calibration_reset_button"),
                    shape = RoundedCornerShape(100.dp),
                    border = BorderStroke(1.dp, ColorTextSecondary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "Reset",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ColorTextPrimary,
                        maxLines = 1
                    )
                }

                Button(
                    onClick = {
                        if (isSaving) return@Button
                        scope.launch {
                            isSaving = true
                            statusMessage = null
                            val res = CosmeticsManager.setFrameCalibration(
                                context = context,
                                code = frameCode,
                                innerRatio = innerRatio,
                                offsetX = offsetX,
                                offsetY = offsetY,
                                overflowRatio = overflowRatio
                            )
                            isSaving = false
                            res.onSuccess { updatedAssetMap ->
                                savedCalibratedState = true
                                statusIsError = false
                                statusMessage = "Saved calibration for everyone."
                                onSaved(updatedAssetMap)
                                Toast.makeText(context, "Frame calibration saved", Toast.LENGTH_SHORT).show()
                            }.onFailure { err ->
                                statusIsError = true
                                statusMessage = err.message ?: "Failed to save calibration."
                            }
                        }
                    },
                    enabled = !isSaving,
                    modifier = Modifier
                        .weight(1.4f)
                        .heightIn(min = 48.dp)
                        .testTag("calibration_save_button"),
                    shape = RoundedCornerShape(100.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ColorPrimary,
                        contentColor = ColorOnPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            color = ColorOnPrimary,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Save for everyone",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalibrationSliderRow(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = ColorTextPrimary
            )
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = ColorBg,
                border = BorderStroke(1.dp, ColorOutline)
            ) {
                Text(
                    text = String.format(Locale.US, "%.3f", value),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorTextPrimary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
        Slider(
            value = value.coerceIn(valueRange.start, valueRange.endInclusive),
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = ColorPrimary,
                activeTrackColor = ColorPrimary,
                inactiveTrackColor = ColorSurfaceTrack
            )
        )
    }
}

// Preview Page 1: Name Effect
@Composable
private fun PreviewNamePage(selectedName: StoreItemRow?, userName: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 340.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(ColorSurface)
            .border(1.dp, ColorOutline, RoundedCornerShape(20.dp))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        if (selectedName != null) {
            val nameConfig = remember(selectedName) {
                CosmeticsConfigParser.parseNameEffect(selectedName.config)
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = ColorBg,
                    border = BorderStroke(1.dp, ColorOutline)
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        StyledName(
                            text = userName.ifBlank { selectedName.name },
                            config = nameConfig,
                            fontSize = 24.sp,
                            defaultColor = ColorTextPrimary,
                            animate = true
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = selectedName.name,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorTextPrimary
                )
                Text(
                    text = selectedName.description ?: "Animated display name style",
                    fontSize = 12.5.sp,
                    color = ColorTextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Text("No name effect selected", color = ColorTextSecondary)
        }
    }
}

// Preview Page 2: Entry Animation
@Composable
private fun PreviewEntryPage(selectedEntry: StoreItemRow?, userName: String) {
    var isPlaying by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 340.dp)
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
                    shape = RoundedCornerShape(100.dp),
                    modifier = Modifier.heightIn(min = 48.dp)
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

// Preview Page 3: Bubble Theme
@Composable
private fun PreviewBubblePage(selectedBubble: StoreItemRow?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 340.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(ColorBg)
            .border(1.dp, ColorOutline, RoundedCornerShape(20.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (selectedBubble != null) {
            val bubbleConfig = remember(selectedBubble) {
                CosmeticsConfigParser.parseBubbleTheme(selectedBubble.config)
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
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
            Text("No bubble theme selected", color = ColorTextSecondary)
        }
    }
}

// Preview Page 4: Chat Theme Background
@Composable
private fun PreviewThemePage(selectedTheme: StoreItemRow?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 340.dp)
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, ColorOutline, RoundedCornerShape(20.dp))
    ) {
        if (selectedTheme != null) {
            val bgConfig = remember(selectedTheme) {
                CosmeticsConfigParser.parseChatBg(selectedTheme.config)
            }

            ChatBackground(
                config = bgConfig,
                isDark = LocalAppDarkTheme.current,
                modifier = Modifier.matchParentSize()
            )

            // Sample floating chat bubbles over wallpaper
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 340.dp),
                contentAlignment = Alignment.Center
            ) {
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

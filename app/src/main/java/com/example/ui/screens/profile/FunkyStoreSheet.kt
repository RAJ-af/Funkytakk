package com.example.ui.screens.profile

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.StoreItemRow
import com.example.store.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FunkyStoreSheet(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val coinBalance by CosmeticsManager.userWalletBalance.collectAsState()
    val myCosmetics by CosmeticsManager.myCosmetics.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = remember { listOf("Bubbles", "Backgrounds", "Frames", "Name Effects", "Stickers") }
    val tabTypes = remember { listOf("bubble_theme", "chat_bg", "avatar_frame", "name_effect", "sticker_pack") }

    var isLoading by remember { mutableStateOf(false) }
    var storeItems by remember { mutableStateOf<List<StoreItemRow>>(emptyList()) }
    var confirmingPurchaseItem by remember { mutableStateOf<StoreItemRow?>(null) }
    var actionInProgressId by remember { mutableStateOf<Long?>(null) }

    fun loadData() {
        scope.launch {
            isLoading = true
            CosmeticsManager.getOrCreateWallet(context)
            val type = tabTypes[selectedTabIndex]
            val res = CosmeticsManager.getStoreItems(context, type = type)
            storeItems = res.getOrDefault(emptyList())
            isLoading = false
        }
    }

    LaunchedEffect(selectedTabIndex) {
        loadData()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFFFAFAF9),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            // Header with Coin Balance
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "🛍️ Funky Store",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "Customize chat bubbles, frames, effects & stickers",
                        fontSize = 12.5.sp,
                        color = Color(0xFF64748B)
                    )
                }

                // Wallet Balance Chip
                Surface(
                    shape = RoundedCornerShape(100.dp),
                    color = Color(0xFFFEF3C7),
                    border = BorderStroke(1.dp, Color(0xFFFDE047))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("🪙", fontSize = 16.sp)
                        Text(
                            text = "$coinBalance Coins",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB45309)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tab Row
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.Transparent,
                contentColor = Color(0xFF0F172A),
                divider = {}
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 13.sp,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Items Content Grid
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFFFFC629))
                }
            } else if (storeItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("✨", fontSize = 32.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No items found in this section",
                            fontSize = 14.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 440.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(storeItems, key = { it.id }) { item ->
                        StoreItemCard(
                            item = item,
                            isProcessing = actionInProgressId == item.id,
                            onPurchase = { confirmingPurchaseItem = item },
                            onEquip = {
                                scope.launch {
                                    actionInProgressId = item.id
                                    val res = CosmeticsManager.equipItem(context, item.id)
                                    res.onSuccess {
                                        Toast.makeText(context, "Equipped ${item.name}!", Toast.LENGTH_SHORT).show()
                                        loadData()
                                    }.onFailure { err ->
                                        Toast.makeText(context, err.message ?: "Equip failed", Toast.LENGTH_LONG).show()
                                    }
                                    actionInProgressId = null
                                }
                            },
                            onUnequip = {
                                scope.launch {
                                    actionInProgressId = item.id
                                    val slot = when (item.item_type) {
                                        "bubble_theme" -> "bubble_theme"
                                        "chat_bg" -> "chat_bg"
                                        "avatar_frame" -> "avatar_frame"
                                        "name_effect" -> "name_effect"
                                        else -> ""
                                    }
                                    if (slot.isNotBlank()) {
                                        val res = CosmeticsManager.unequipSlot(context, slot)
                                        res.onSuccess {
                                            Toast.makeText(context, "Unequipped slot", Toast.LENGTH_SHORT).show()
                                            loadData()
                                        }.onFailure { err ->
                                            Toast.makeText(context, err.message ?: "Unequip failed", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                    actionInProgressId = null
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFC629),
                    contentColor = Color(0xFF0F172A)
                ),
                shape = RoundedCornerShape(100.dp)
            ) {
                Text("Close Store", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    // Confirm Purchase Dialog
    confirmingPurchaseItem?.let { item ->
        AlertDialog(
            onDismissRequest = { confirmingPurchaseItem = null },
            title = {
                Text("Confirm Purchase", fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text("Do you want to buy ${item.name} for ${item.price_coins} coins?")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Current Balance: $coinBalance Coins",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val itemToBuy = item
                        confirmingPurchaseItem = null
                        scope.launch {
                            actionInProgressId = itemToBuy.id
                            val res = CosmeticsManager.purchaseItem(context, itemToBuy.id)
                            res.onSuccess { remaining ->
                                Toast.makeText(context, "Successfully purchased ${itemToBuy.name}!", Toast.LENGTH_SHORT).show()
                                loadData()
                            }.onFailure { err ->
                                val msg = when {
                                    err.message?.contains("insufficient_coins", ignoreCase = true) == true -> "Insufficient coins! Earn or buy more coins."
                                    err.message?.contains("already_owned", ignoreCase = true) == true -> "You already own this item!"
                                    else -> err.message ?: "Purchase failed"
                                }
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            }
                            actionInProgressId = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC629), contentColor = Color.Black)
                ) {
                    Text("Buy Now (${item.price_coins} 🪙)", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmingPurchaseItem = null }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
private fun StoreItemCard(
    item: StoreItemRow,
    isProcessing: Boolean,
    onPurchase: () -> Unit,
    onEquip: () -> Unit,
    onUnequip: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(
            width = if (item.equipped) 2.dp else 1.dp,
            color = if (item.equipped) Color(0xFFFFC629) else Color(0xFFE2E8F0)
        ),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Live Preview Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF8FAFC)),
                contentAlignment = Alignment.Center
            ) {
                when (item.item_type) {
                    "bubble_theme" -> {
                        val config = remember(item.config) {
                            CosmeticsConfigParser.parseBubbleTheme(item.config)
                        }
                        ThemedBubble(config = config, isOutgoing = true) {
                            Text(
                                text = "Preview message ✨",
                                fontSize = 11.5.sp,
                                color = Color(0xFF0F172A)
                            )
                        }
                    }
                    "chat_bg" -> {
                        val config = remember(item.config) {
                            CosmeticsConfigParser.parseChatBg(item.config)
                        }
                        Box(modifier = Modifier.fillMaxSize()) {
                            ChatBackground(config = config, isDark = false, modifier = Modifier.fillMaxSize())
                            Text(
                                text = "Chat BG",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .background(Color.White.copy(alpha = 0.8f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    "avatar_frame" -> {
                        val config = remember(item.config) {
                            CosmeticsConfigParser.parseAvatarFrame(item.config)
                        }
                        AvatarFrame(config = config, sizeDp = 56.dp) {
                            AsyncImage(
                                model = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200",
                                contentDescription = "Avatar",
                                modifier = Modifier.fillMaxSize().clip(CircleShape)
                            )
                        }
                    }
                    "name_effect" -> {
                        val config = remember(item.config) {
                            CosmeticsConfigParser.parseNameEffect(item.config)
                        }
                        StyledName(
                            text = item.name,
                            config = config,
                            fontSize = 16.sp
                        )
                    }
                    "sticker_pack" -> {
                        Text(
                            text = "🎁 Pack",
                            fontSize = 24.sp
                        )
                    }
                    else -> {
                        Text(item.name, fontSize = 13.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = item.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
                maxLines = 1
            )

            Text(
                text = item.description ?: "",
                fontSize = 11.sp,
                color = Color(0xFF64748B),
                maxLines = 1,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Price / Ownership Action Button
            if (isProcessing) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else if (item.equipped) {
                Button(
                    onClick = onUnequip,
                    modifier = Modifier.fillMaxWidth().height(34.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE2E8F0), contentColor = Color(0xFF0F172A)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("Equipped", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            } else if (item.owned) {
                if (item.item_type == "sticker_pack") {
                    Surface(
                        shape = RoundedCornerShape(100.dp),
                        color = Color(0xFFDCFCE7),
                        modifier = Modifier.fillMaxWidth().height(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("Owned Pack", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF166534))
                        }
                    }
                } else {
                    Button(
                        onClick = onEquip,
                        modifier = Modifier.fillMaxWidth().height(34.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A), contentColor = Color.White),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Equip", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Button(
                    onClick = onPurchase,
                    modifier = Modifier.fillMaxWidth().height(34.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC629), contentColor = Color(0xFF0F172A)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    val priceTxt = if (item.price_coins == 0) "Free" else "${item.price_coins} 🪙"
                    Text("Buy $priceTxt", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

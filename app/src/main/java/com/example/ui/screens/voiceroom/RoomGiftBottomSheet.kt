package com.example.ui.screens.voiceroom

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.GiftCatalogItem
import com.example.gifting.GiftManager
import com.example.ui.components.PresenceAvatar
import kotlinx.coroutines.launch

/**
 * Model representing a room participant candidate for receiving gifts.
 */
data class GiftReceiverUser(
    val userId: String,
    val name: String,
    val avatarUrl: String?,
    val countryCode: String? = null,
    val role: String = "listener"
)

/**
 * Fallback static gift catalog matching design reference if remote catalog is empty.
 */
val defaultGiftCatalog = listOf(
    GiftCatalogItem(id = "g_heart", name = "Heart", price_coins = 10, sort_order = 1),
    GiftCatalogItem(id = "g_rose", name = "Rose", price_coins = 50, sort_order = 2),
    GiftCatalogItem(id = "g_teddy", name = "Teddy", price_coins = 100, sort_order = 3),
    GiftCatalogItem(id = "g_crown", name = "Crown", price_coins = 300, sort_order = 4),
    GiftCatalogItem(id = "g_star", name = "Star", price_coins = 20, sort_order = 5),
    GiftCatalogItem(id = "g_firework", name = "Firework", price_coins = 200, sort_order = 6),
    GiftCatalogItem(id = "g_cake", name = "Cake", price_coins = 500, sort_order = 7),
    GiftCatalogItem(id = "g_diamond", name = "Diamond", price_coins = 1000, sort_order = 8)
)

private fun getGiftEmoji(name: String): String {
    return when (name.lowercase()) {
        "heart" -> "❤️"
        "rose" -> "🌹"
        "teddy" -> "🧸"
        "crown" -> "👑"
        "star" -> "⭐"
        "firework" -> "🎆"
        "cake" -> "🎂"
        "diamond" -> "💎"
        else -> "🎁"
    }
}

/**
 * Production-ready, pixel-polished Send Gift Bottom Sheet matching the requested design reference.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomGiftBottomSheet(
    roomId: String,
    currentUserId: String,
    initialRecipientId: String? = null,
    participants: List<GiftReceiverUser>,
    isDarkMode: Boolean = false,
    onDismiss: () -> Unit,
    onGiftSentSuccess: (giftName: String, recipientName: String, quantity: Int, coinsSpent: Int) -> Unit = { _, _, _, _ -> }
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // State from GiftManager
    val walletBalance by GiftManager.walletBalance.collectAsState()
    val rawCatalog by GiftManager.catalog.collectAsState()
    val isLoadingCatalog by GiftManager.isLoadingCatalog.collectAsState()

    // Combined catalog (prefer backend items if present, else default reference catalog)
    val displayCatalog = remember(rawCatalog) {
        if (rawCatalog.isNotEmpty()) {
            rawCatalog.sortedBy { it.sort_order ?: it.price_coins }
        } else {
            defaultGiftCatalog
        }
    }

    // Refresh wallet and catalog when sheet opens
    LaunchedEffect(Unit) {
        GiftManager.refreshWallet(context)
        if (rawCatalog.isEmpty()) {
            GiftManager.fetchCatalog(context)
        }
    }

    // Filter out current user from receivers (only real room participants)
    val eligibleReceivers = remember(participants, currentUserId) {
        participants.filter { it.userId.isNotBlank() && it.userId != currentUserId }
    }

    // Selected receiver (preselect initialRecipientId if present, else first eligible receiver)
    var selectedReceiverId by remember(initialRecipientId, eligibleReceivers) {
        val initialMatch = eligibleReceivers.find { it.userId == initialRecipientId }?.userId
        mutableStateOf(initialMatch ?: eligibleReceivers.firstOrNull()?.userId ?: "")
    }

    // Selected gift item (default to Rose or 2nd gift item if available)
    var selectedGiftId by remember(displayCatalog) {
        val defaultRose = displayCatalog.find { it.name.equals("Rose", ignoreCase = true) }?.id
        mutableStateOf(defaultRose ?: displayCatalog.firstOrNull()?.id ?: "")
    }
    val selectedGift = displayCatalog.find { it.id == selectedGiftId } ?: displayCatalog.firstOrNull()

    // Quantity selector: 1, 5, 10, 20
    val quantityOptions = remember { listOf(1, 5, 10, 20) }
    var selectedQuantity by remember { mutableStateOf(1) }

    // In-flight sending state & error message
    var isSending by remember { mutableStateOf(false) }
    var inlineError by remember { mutableStateOf<String?>(null) }

    // Warm, elegant color palette matching screenshot
    val sheetBgColor = Color(0xFFFFFDF5)
    val cardBgColor = Color(0xFFFFFFFF)
    val cardBorderColor = Color(0xFFF3F4F6)
    val textMainColor = Color(0xFF1E1B18)
    val textMutedColor = Color(0xFF78716C)
    val yellowGoldAccent = Color(0xFFFFC527)
    val yellowPillBg = Color(0xFFFEF3C7)
    val selectedBorderColor = Color(0xFFFFC527)

    val totalCoinsCost = (selectedGift?.price_coins ?: 0) * selectedQuantity

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = sheetBgColor,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = Color(0xFFD6D3D1)
            )
        },
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp)
        ) {
            // ----------------------------------------------------
            // 1. HEADER: 3D Gift Box Icon + Title + Subtitle + Coins Pill
            // ----------------------------------------------------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(yellowPillBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🎁", fontSize = 24.sp)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Send Gift",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = textMainColor
                        )
                        Text(
                            text = "Send a gift to someone in this room",
                            fontSize = 12.sp,
                            color = textMutedColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Top-Right Coin Balance Pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(yellowPillBg)
                        .border(1.dp, Color(0xFFFDE68A), RoundedCornerShape(100.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(yellowGoldAccent),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF78350F)
                        )
                    }
                    Text(
                        text = String.format("%,d", walletBalance),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = Color(0xFF1E1B18)
                    )
                }
            }

            // ----------------------------------------------------
            // 2. "SEND TO" RECEIVER ROW
            // ----------------------------------------------------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Send to",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = textMainColor
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { }
                ) {
                    Text(
                        text = "Choose a user",
                        fontSize = 12.sp,
                        color = textMutedColor,
                        fontWeight = FontWeight.Medium
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = textMutedColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            if (eligibleReceivers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp, top = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No other users in the room to send gifts to",
                        fontSize = 12.sp,
                        color = textMutedColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(eligibleReceivers, key = { it.userId }) { user ->
                        val isSelected = (user.userId == selectedReceiverId)
                        val roleLabel = when (user.role.lowercase()) {
                            "host" -> "Host"
                            "speaker" -> "Speaker"
                            else -> "Listener"
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable {
                                    selectedReceiverId = user.userId
                                    inlineError = null
                                }
                        ) {
                            Box(contentAlignment = Alignment.TopEnd) {
                                Box(
                                    modifier = Modifier
                                        .padding(4.dp)
                                        .size(62.dp)
                                        .clip(CircleShape)
                                        .border(
                                            width = if (isSelected) 3.dp else 0.dp,
                                            color = if (isSelected) yellowGoldAccent else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .padding(if (isSelected) 2.dp else 0.dp)
                                ) {
                                    PresenceAvatar(
                                        avatarUrl = user.avatarUrl,
                                        size = 56.dp,
                                        flagCountryCode = user.countryCode,
                                        isOnline = true
                                    )
                                }

                                // Yellow Checkmark Badge for Selected User
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(yellowGoldAccent)
                                            .border(1.5.dp, Color.White, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = Color(0xFF1E1B18),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = user.name.ifBlank { "User" },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = textMainColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            // Role Tag (Host gets gold crown)
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(100.dp))
                                    .background(if (user.role.equals("host", ignoreCase = true)) yellowPillBg else Color(0xFFF3F4F6))
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                if (user.role.equals("host", ignoreCase = true)) {
                                    Text(text = "👑", fontSize = 10.sp)
                                }
                                Text(
                                    text = roleLabel,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (user.role.equals("host", ignoreCase = true)) Color(0xFF92400E) else Color(0xFF6B7280)
                                )
                            }
                        }
                    }
                }
            }

            // ----------------------------------------------------
            // 3. "CHOOSE A GIFT" GRID (4 Columns)
            // ----------------------------------------------------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Choose a Gift",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = textMainColor
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { }
                ) {
                    Text(
                        text = "All Gifts",
                        fontSize = 12.sp,
                        color = textMutedColor,
                        fontWeight = FontWeight.Medium
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = textMutedColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(265.dp),
                contentPadding = PaddingValues(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(displayCatalog, key = { it.id }) { item ->
                    val isSelected = (item.id == selectedGiftId)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) Color(0xFFFFFBEB) else cardBgColor)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) selectedBorderColor else cardBorderColor,
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable {
                                selectedGiftId = item.id
                                inlineError = null
                            }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.TopEnd
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (item.icon_url.isNotBlank()) {
                                AsyncImage(
                                    model = item.icon_url,
                                    contentDescription = item.name,
                                    modifier = Modifier.size(44.dp)
                                )
                            } else {
                                Text(
                                    text = getGiftEmoji(item.name),
                                    fontSize = 36.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = item.name,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = textMainColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(yellowGoldAccent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$",
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF78350F)
                                    )
                                }
                                Text(
                                    text = String.format("%,d", item.price_coins),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = textMainColor
                                )
                            }
                        }

                        // Selected checkmark badge in top-right
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .padding(4.dp)
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(yellowGoldAccent)
                                    .border(1.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color(0xFF1E1B18),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ----------------------------------------------------
            // 4. "QUANTITY" SELECTOR PILL (1, 5, 10, 20)
            // ----------------------------------------------------
            Text(
                text = "Quantity",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = textMainColor,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFFF5F5F0))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                quantityOptions.forEach { qty ->
                    val isSelected = (selectedQuantity == qty)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isSelected) yellowGoldAccent else Color.Transparent
                            )
                            .clickable {
                                selectedQuantity = qty
                                inlineError = null
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$qty",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isSelected) Color(0xFF1E1B18) else Color(0xFF57534E)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ----------------------------------------------------
            // 5. "SEND GIFT • X COINS" BUTTON
            // ----------------------------------------------------
            val isReceiverValid = selectedReceiverId.isNotBlank()
            val isGiftValid = selectedGift != null
            val canSend = isReceiverValid && isGiftValid && !isSending

            Button(
                onClick = {
                    if (!canSend) return@Button
                    val receiver = eligibleReceivers.find { it.userId == selectedReceiverId }
                    val gift = selectedGift ?: return@Button
                    val qty = selectedQuantity

                    isSending = true
                    inlineError = null

                    scope.launch {
                        val result = GiftManager.sendRoomGift(
                            context = context,
                            roomId = roomId,
                            recipientId = receiver?.userId ?: selectedReceiverId,
                            giftId = gift.id,
                            quantity = qty
                        )

                        isSending = false
                        result.onSuccess { resp ->
                            val spent = resp.coins_spent ?: (gift.price_coins * qty)
                            Toast.makeText(
                                context,
                                "Sent $qty ${gift.name} to ${receiver?.name ?: "user"}! 🎁",
                                Toast.LENGTH_SHORT
                            ).show()
                            onGiftSentSuccess(gift.name, receiver?.name ?: "user", qty, spent)
                            onDismiss()
                        }.onFailure { err ->
                            inlineError = err.message ?: "Gift नहीं भेजा जा सका, फिर कोशिश करो"
                        }
                    }
                },
                enabled = canSend,
                colors = ButtonDefaults.buttonColors(
                    containerColor = yellowGoldAccent,
                    disabledContainerColor = Color(0xFFE7E5E4)
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .shadow(4.dp, RoundedCornerShape(24.dp), spotColor = yellowGoldAccent.copy(alpha = 0.5f))
            ) {
                if (isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFF1E1B18)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Sending...",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E1B18)
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = Color(0xFF1E1B18),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Send Gift • ${String.format("%,d", totalCoinsCost)} Coins",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1E1B18)
                        )
                    }
                }
            }

            // Inline Error Banner
            AnimatedVisibility(
                visible = inlineError != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                inlineError?.let { err ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFEF2F2))
                            .border(1.dp, Color(0xFFFCA5A5), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⚠️ $err",
                            color = Color(0xFFDC2626),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ----------------------------------------------------
            // 6. FOOTER INFO NOTE
            // ----------------------------------------------------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(100.dp))
                    .background(Color(0xFFF5F5F0))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = textMutedColor,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Gifts help you appreciate and support speakers in the room.",
                    fontSize = 11.5.sp,
                    color = textMutedColor,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

package com.example.gifting

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.GiftCatalogItem
import com.example.ui.components.SoftCloseIcon
import com.example.ui.theme.GoldAccent
import kotlinx.coroutines.launch

@Composable
fun CuteCoinIcon(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(GoldAccent),
        contentAlignment = Alignment.Center
    ) {
        Text("🪙", fontSize = 10.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendGiftBottomSheet(
    recipientId: String,
    recipientName: String,
    recipientAvatar: String? = null,
    onDismiss: () -> Unit,
    onGiftSent: (remainingBalance: Int) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val walletBalance by GiftManager.walletBalance.collectAsState()
    val catalog by GiftManager.catalog.collectAsState()
    val isLoadingCatalog by GiftManager.isLoadingCatalog.collectAsState()

    var selectedGift by remember { mutableStateOf<GiftCatalogItem?>(null) }
    var giftMessage by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isInsufficientCoins by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        GiftManager.refreshWallet(context)
        GiftManager.fetchCatalog(context)
    }

    LaunchedEffect(catalog) {
        if (selectedGift == null && catalog.isNotEmpty()) {
            selectedGift = catalog.firstOrNull()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Header: Recipient info + Wallet Balance
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(recipientAvatar.orEmpty().ifBlank { "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200" })
                            .crossfade(true)
                            .build(),
                        contentDescription = "Recipient Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, Color(0xFFFDE68A), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Send Gift to $recipientName",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F172A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Brighten their day with an animated gift! 🎁",
                            fontSize = 11.5.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF1F5F9))
                ) {
                    SoftCloseIcon(size = 14.dp, color = Color(0xFF475569))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Wallet Balance Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFFFFBEB))
                    .border(BorderStroke(1.dp, Color(0xFFFDE68A)), RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CuteCoinIcon(modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "My Coin Balance:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF92400E)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$walletBalance coins",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFB45309)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFFEF3C7)
                ) {
                    Text(
                        text = "Free starter gift",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF92400E),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Insufficient Coins Warning
            AnimatedVisibility(visible = isInsufficientCoins) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFEF2F2))
                        .border(1.dp, Color(0xFFFECACA), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "⚠️ Not Enough Coins",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFDC2626)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "You need more coins to send this gift. More ways to get coins coming soon!",
                        fontSize = 12.sp,
                        color = Color(0xFFB91C1C)
                    )
                }
            }

            if (!errorMessage.isNullOrBlank() && !isInsufficientCoins) {
                Text(
                    text = errorMessage!!,
                    color = Color(0xFFDC2626),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Catalog Grid
            Text(
                text = "Choose a Gift",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF334155)
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (isLoadingCatalog && catalog.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = GoldAccent, modifier = Modifier.size(32.dp))
                }
            } else if (catalog.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFF8FAFC)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No gifts available right now.",
                        color = Color(0xFF64748B),
                        fontSize = 13.sp
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                ) {
                    items(catalog, key = { it.id }) { gift ->
                        val isSelected = selectedGift?.id == gift.id
                        val canAfford = walletBalance >= gift.price_coins

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) Color(0xFFFFFBEB) else Color(0xFFF8FAFC),
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) GoldAccent else Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier
                                .clickable {
                                    selectedGift = gift
                                    isInsufficientCoins = walletBalance < gift.price_coins
                                    errorMessage = null
                                }
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AnimatedGiftMedia(
                                        url = gift.icon_url,
                                        contentDescription = gift.name,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = gift.name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(3.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CuteCoinIcon(modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "${gift.price_coins}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (canAfford) Color(0xFFD97706) else Color(0xFFEF4444)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Optional Message input
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF8FAFC))
                    .border(BorderStroke(1.dp, Color(0xFFE2E8F0)), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (giftMessage.isEmpty()) {
                    Text(
                        text = "Add a personal note (optional)...",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp
                    )
                }
                BasicTextField(
                    value = giftMessage,
                    onValueChange = { giftMessage = it },
                    singleLine = true,
                    textStyle = TextStyle(color = Color.Black, fontSize = 13.sp),
                    cursorBrush = SolidColor(Color.Black),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Send Button
            val selected = selectedGift
            val cost = selected?.price_coins ?: 0
            val hasEnough = walletBalance >= cost

            Button(
                onClick = {
                    if (selected == null) return@Button
                    if (!hasEnough) {
                        isInsufficientCoins = true
                        return@Button
                    }
                    isSending = true
                    errorMessage = null
                    isInsufficientCoins = false

                    scope.launch {
                        val result = GiftManager.sendGift(
                            context = context,
                            recipientId = recipientId,
                            giftId = selected.id,
                            message = giftMessage
                        )

                        isSending = false
                        result.fold(
                            onSuccess = { remaining ->
                                Toast.makeText(
                                    context,
                                    "Sent ${selected.name} to $recipientName! 🎁✨",
                                    Toast.LENGTH_SHORT
                                ).show()
                                onGiftSent(remaining)
                                onDismiss()
                            },
                            onFailure = { err ->
                                if (err is InsufficientCoinsException || err.message?.contains("insufficient", ignoreCase = true) == true) {
                                    isInsufficientCoins = true
                                } else {
                                    errorMessage = err.message ?: "Failed to send gift"
                                }
                            }
                        )
                    }
                },
                enabled = selected != null && !isSending,
                colors = ButtonDefaults.buttonColors(
                    containerColor = GoldAccent,
                    disabledContainerColor = Color(0xFFE2E8F0)
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                if (isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.Black,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sending Gift...", color = Color.Black, fontWeight = FontWeight.Bold)
                } else {
                    CuteCoinIcon(modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (selected != null) "Send ${selected.name} (${selected.price_coins} Coins)" else "Select a Gift",
                        color = Color.Black,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

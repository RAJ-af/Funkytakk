package com.example.ui.screens.store

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.R
import com.example.ui.theme.AppThemeManager
import com.example.ui.theme.LocalAppDarkTheme
import com.example.ui.theme.LocalFunkyColors
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class CoinPackOption(
    val coins: Int,
    val bonus: Int,
    val price: String,
    val stackCount: Int,
    val tag: String? = null
) {
    val totalCoins: Int get() = coins + bonus
}

private data class ConfettiPiece(
    val angleRad: Float,
    val distancePx: Float,
    val color: Color,
    val sizeDp: Float,
    val rotationDeg: Float,
    val delayFraction: Float
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CoinStoreScreen(
    currentBalance: Int,
    insufficientCoinsAlert: Boolean = false,
    onDismiss: () -> Unit,
    onPurchaseCoins: (addedCoins: Int) -> Unit
) {
    val context = LocalContext.current
    val funkyColors = LocalFunkyColors.current
    val isDark = LocalAppDarkTheme.current

    val packs = remember {
        listOf(
            CoinPackOption(coins = 100, bonus = 0, price = "$0.99", stackCount = 1, tag = null),
            CoinPackOption(coins = 500, bonus = 50, price = "$4.99", stackCount = 2, tag = "Popular"),
            CoinPackOption(coins = 1200, bonus = 200, price = "$9.99", stackCount = 3, tag = null),
            CoinPackOption(coins = 3000, bonus = 750, price = "$19.99", stackCount = 4, tag = "Best value")
        )
    }

    var selectedIndex by remember { mutableStateOf(1) }
    var displayBalance by remember(currentBalance) { mutableStateOf(currentBalance) }
    var showDoneOverlay by remember { mutableStateOf(false) }
    var showSpinOfferDialog by remember { mutableStateOf(false) }
    var lastAddedCoins by remember { mutableStateOf(0) }
    var balanceBeforePurchase by remember { mutableStateOf(currentBalance) }
    var balanceAfterPurchase by remember { mutableStateOf(currentBalance) }

    BackHandler {
        if (showSpinOfferDialog) {
            showSpinOfferDialog = false
        } else if (showDoneOverlay) {
            showDoneOverlay = false
        } else {
            onDismiss()
        }
    }

    val selectedPack = packs[selectedIndex]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(funkyColors.bg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // -------------------------------------------------------------
            // TOP BAR
            // -------------------------------------------------------------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(funkyColors.soft)
                        .clickable { onDismiss() }
                        .testTag("coin_store_back_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_back),
                        contentDescription = "Back",
                        tint = funkyColors.ink,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Text(
                    text = "Coins",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp,
                    color = funkyColors.ink
                )

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(funkyColors.soft)
                        .clickable { AppThemeManager.toggleDarkTheme(context) }
                        .testTag("coin_store_theme_toggle"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = if (isDark) R.drawable.ic_sun else R.drawable.ic_moon),
                        contentDescription = if (isDark) "Switch to Light Theme" else "Switch to Dark Theme",
                        tint = if (isDark) Color(0xFFFBBF24) else Color(0xFF7C3AED),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // -------------------------------------------------------------
            // SCROLLABLE CONTENT
            // -------------------------------------------------------------
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 110.dp)
            ) {
                if (insufficientCoinsAlert) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = funkyColors.danger.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, funkyColors.danger.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_alert),
                                contentDescription = null,
                                tint = funkyColors.danger,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Not enough coins! Pick a pack below to top up your wallet.",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = funkyColors.danger
                            )
                        }
                    }
                }

                // ---------------------------------------------------------
                // HERO BALANCE CARD
                // ---------------------------------------------------------
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(36.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(Color(0xFFFFC94D), Color(0xFFFFA51F))
                            )
                        )
                ) {
                    // Decorative circles matching HTML ::after / ::before
                    Canvas(modifier = Modifier.matchParentSize()) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.28f),
                            radius = 95.dp.toPx(),
                            center = Offset(size.width + 18.dp.toPx(), (-10).dp.toPx())
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.18f),
                            radius = 65.dp.toPx(),
                            center = Offset(size.width - 70.dp.toPx(), size.height + 12.dp.toPx())
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 22.dp, vertical = 20.dp)
                    ) {
                        Text(
                            text = "Your balance",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2B2118).copy(alpha = 0.75f)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2B2118)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_coin),
                                    contentDescription = null,
                                    tint = Color(0xFFFFC94D),
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            Text(
                                text = String.format("%,d", displayBalance),
                                fontSize = 48.sp,
                                lineHeight = 52.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-1.5).sp,
                                color = Color(0xFF2B2118),
                                modifier = Modifier.testTag("coin_store_balance_text")
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Use coins for gifts and style upgrades.",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF2B2118).copy(alpha = 0.8f)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            HeroUseChip(iconRes = R.drawable.ic_gift, iconTint = Color(0xFFE11D48), label = "Gifts")
                            HeroUseChip(iconRes = R.drawable.ic_ring, iconTint = Color(0xFFD97706), label = "Frames")
                            HeroUseChip(iconRes = R.drawable.ic_rocket, iconTint = Color(0xFF7C3AED), label = "Entries")
                            HeroUseChip(iconRes = R.drawable.ic_palette, iconTint = Color(0xFF0284C7), label = "Themes")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ---------------------------------------------------------
                // SPECIAL OFFERS SPIN WHEEL BANNER (CLOSED MOCKUP)
                // ---------------------------------------------------------
                Surface(
                    onClick = { showSpinOfferDialog = true },
                    shape = RoundedCornerShape(24.dp),
                    color = funkyColors.surface,
                    border = BorderStroke(1.2.dp, funkyColors.mango.copy(alpha = 0.55f)),
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("coin_store_spin_offers_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(funkyColors.mango.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_gift),
                                contentDescription = "Special Offers Spin",
                                tint = funkyColors.mango,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Special Offers Spin",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = funkyColors.ink
                                )
                                Surface(
                                    shape = RoundedCornerShape(99.dp),
                                    color = funkyColors.soft
                                ) {
                                    Text(
                                        text = "CLOSED",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Black,
                                        color = funkyColors.mango,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Preview coin discounts & bonus spin offers",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = funkyColors.mute
                            )
                        }

                        Icon(
                            painter = painterResource(id = R.drawable.ic_chevron_right),
                            contentDescription = null,
                            tint = funkyColors.mute,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ---------------------------------------------------------
                // PACKS SECTION HEADER
                // ---------------------------------------------------------
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "Choose a pack",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = funkyColors.ink
                    )
                    Text(
                        text = "1 coin ≈ ₹0.8",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = funkyColors.mute
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ---------------------------------------------------------
                // COIN PACK CARDS
                // ---------------------------------------------------------
                packs.forEachIndexed { index, pack ->
                    val isSelected = index == selectedIndex
                    val cardBg = if (isSelected) funkyColors.soft else funkyColors.surface
                    val borderColor = if (isSelected) funkyColors.mango else funkyColors.line

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(30.dp))
                                .background(cardBg)
                                .border(3.dp, borderColor, RoundedCornerShape(30.dp))
                                .clickable { selectedIndex = index }
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                                .testTag("coin_pack_${pack.coins}"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Overlapping Mango Coin Stack
                            Box(
                                modifier = Modifier.width(78.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                for (k in 0 until pack.stackCount) {
                                    Box(
                                        modifier = Modifier
                                            .offset(x = (k * 14).dp)
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(cardBg)
                                            .padding(3.dp)
                                            .clip(CircleShape)
                                            .background(funkyColors.mango),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_coin),
                                            contentDescription = null,
                                            tint = funkyColors.onMango,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = String.format("%,d", pack.coins),
                                    fontSize = 22.sp,
                                    lineHeight = 24.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = funkyColors.ink
                                )
                                Text(
                                    text = buildAnnotatedString {
                                        append("coins")
                                        if (pack.bonus > 0) {
                                            append(" ")
                                            withStyle(
                                                SpanStyle(
                                                    color = funkyColors.ok,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            ) {
                                                append("+ ${pack.bonus} bonus")
                                            }
                                        }
                                    },
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = funkyColors.mute
                                )
                            }

                            // Dark Price Pill
                            Surface(
                                shape = RoundedCornerShape(99.dp),
                                color = funkyColors.ink
                            ) {
                                Text(
                                    text = pack.price,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = funkyColors.bg,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
                                )
                            }
                        }

                        if (!pack.tag.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(99.dp),
                                color = funkyColors.mango,
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .offset(x = 18.dp, y = (-9).dp)
                                    .zIndex(2f)
                            ) {
                                Text(
                                    text = pack.tag,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = funkyColors.onMango,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // ---------------------------------------------------------
                // SECURITY FOOTER NOTE
                // ---------------------------------------------------------
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_lock),
                        contentDescription = null,
                        tint = funkyColors.mute,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Secure payment · Coins never expire",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = funkyColors.mute
                    )
                }
            }
        }

        // -----------------------------------------------------------------
        // STICKY BOTTOM CTA BAR
        // -----------------------------------------------------------------
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Button(
                onClick = {
                    val added = selectedPack.totalCoins
                    balanceBeforePurchase = displayBalance
                    balanceAfterPurchase = displayBalance + added
                    displayBalance = balanceAfterPurchase
                    lastAddedCoins = added
                    onPurchaseCoins(added)
                    showDoneOverlay = true
                },
                shape = RoundedCornerShape(99.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = funkyColors.mango,
                    contentColor = funkyColors.onMango
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .shadow(10.dp, RoundedCornerShape(99.dp), spotColor = Color(0x66FFB627))
                    .testTag("coin_store_buy_cta")
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_coin),
                    contentDescription = null,
                    tint = funkyColors.onMango,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Get ${String.format("%,d", selectedPack.totalCoins)} coins · ${selectedPack.price}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        // -----------------------------------------------------------------
        // ANIMATED CONFETTI COMPLETION OVERLAY
        // -----------------------------------------------------------------
        AnimatedVisibility(
            visible = showDoneOverlay,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(180))
        ) {
            CoinPurchaseSuccessOverlay(
                addedCoins = lastAddedCoins,
                oldBalance = balanceBeforePurchase,
                newBalance = balanceAfterPurchase,
                onBackToStore = { showDoneOverlay = false }
            )
        }

        if (showSpinOfferDialog) {
            SpinOfferWheelDialog(
                onDismiss = { showSpinOfferDialog = false }
            )
        }
    }
}

@Composable
private fun HeroUseChip(
    @DrawableRes iconRes: Int,
    iconTint: Color,
    label: String
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(99.dp))
            .background(Color.White.copy(alpha = 0.52f))
            .padding(start = 9.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2B2118)
        )
    }
}

@Composable
private fun CoinPurchaseSuccessOverlay(
    addedCoins: Int,
    oldBalance: Int,
    newBalance: Int,
    onBackToStore: () -> Unit
) {
    val funkyColors = LocalFunkyColors.current

    val popScale = remember { Animatable(0f) }
    val confettiProgress = remember { Animatable(0f) }
    var targetNumber by remember { mutableStateOf(oldBalance) }

    val animatedBalance by animateIntAsState(
        targetValue = targetNumber,
        animationSpec = tween(durationMillis = 900, easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)),
        label = "coinCountUp"
    )

    val confettiPieces = remember {
        val palette = listOf(
            Color(0xFFFFB627),
            Color(0xFFFF5A8A),
            Color(0xFF7C5CFF),
            Color(0xFF22B573),
            Color(0xFF2D7BFF)
        )
        List(22) { idx ->
            ConfettiPiece(
                angleRad = (Random.nextFloat() * 6.28318f),
                distancePx = 110f + Random.nextFloat() * 140f,
                color = palette[idx % palette.size],
                sizeDp = 8f + Random.nextFloat() * 4f,
                rotationDeg = 360f + Random.nextFloat() * 240f,
                delayFraction = Random.nextFloat() * 0.16f
            )
        }
    }

    LaunchedEffect(addedCoins, newBalance) {
        popScale.snapTo(0f)
        confettiProgress.snapTo(0f)
        targetNumber = newBalance
        popScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        )
    }

    LaunchedEffect(addedCoins) {
        confettiProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1300)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(funkyColors.bg)
            .navigationBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        // Confetti burst layer
        Canvas(modifier = Modifier.fillMaxSize()) {
            val origin = Offset(size.width / 2f, size.height * 0.34f)
            val progress = confettiProgress.value
            confettiPieces.forEach { piece ->
                val localP = ((progress - piece.delayFraction) / (1f - piece.delayFraction)).coerceIn(0f, 1f)
                if (localP > 0f && localP < 1f) {
                    val alpha = (1f - localP).coerceIn(0f, 1f)
                    val radius = piece.distancePx.dp.toPx() * localP
                    val x = origin.x + cos(piece.angleRad) * radius
                    val y = origin.y + sin(piece.angleRad) * radius - (16.dp.toPx() * localP)
                    val sz = piece.sizeDp.dp.toPx()
                    rotate(degrees = piece.rotationDeg * localP, pivot = Offset(x, y)) {
                        drawRoundRect(
                            color = piece.color.copy(alpha = alpha),
                            topLeft = Offset(x - sz / 2f, y - sz / 2f),
                            size = Size(sz, sz),
                            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                        )
                    }
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .scale(popScale.value)
                    .clip(CircleShape)
                    .background(funkyColors.mango),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_check),
                    contentDescription = "Success",
                    tint = funkyColors.onMango,
                    modifier = Modifier.size(64.dp)
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = "+${String.format("%,d", addedCoins)} coins",
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.8).sp,
                color = funkyColors.ink,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Added to your wallet. Go style up!",
                fontSize = 15.sp,
                color = funkyColors.mute,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(22.dp))

            Surface(
                shape = RoundedCornerShape(99.dp),
                color = funkyColors.soft
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_coin),
                        contentDescription = null,
                        tint = funkyColors.mango,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = String.format("%,d", animatedBalance),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = funkyColors.ink
                    )
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

            Button(
                onClick = onBackToStore,
                shape = RoundedCornerShape(99.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = funkyColors.mango,
                    contentColor = funkyColors.onMango
                ),
                modifier = Modifier
                    .widthIn(max = 330.dp)
                    .fillMaxWidth()
                    .height(58.dp)
            ) {
                Text(
                    text = "Back to store",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

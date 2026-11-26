package com.example.card

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.store.CosmeticsConfigParser
import com.example.store.CosmeticsManager
import com.example.store.DecoratedUserName
import com.example.ui.components.CircleFlag
import com.example.ui.components.FramedAvatar
import com.example.ui.components.SoftCheckmarkBadge
import com.example.ui.components.SoftWavingHandIcon
import com.example.ui.components.resolveAvatarModel
import com.example.ui.screens.ProfileLocationPinIcon
import com.example.ui.theme.LocalAppDarkTheme
import com.example.ui.theme.LocalFunkyColors
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun rememberCardReduceAnimations(): Boolean {
    val context = LocalContext.current
    return remember {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) == 0f
        }.getOrDefault(false)
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun PeopleCard(
    profile: PeopleCardProfile,
    design: CardDesign? = null,
    modifier: Modifier = Modifier,
    compactMini: Boolean = false,
    isWorkshopInteractive: Boolean = false,
    selectedStickerId: String? = null,
    newlyAddedStickerId: String? = null,
    snapGuideX: Boolean = false,
    snapGuideY: Boolean = false,
    onCardClick: (() -> Unit)? = null,
    onCardLongClick: (() -> Unit)? = null,
    onWaveClick: (() -> Unit)? = null,
    onSelectSticker: ((String?) -> Unit)? = null,
    onStickerDragStart: ((String) -> Unit)? = null,
    onMoveSticker: ((stickerId: String, normX: Float, normY: Float, fingerInTrashZone: Boolean) -> Unit)? = null,
    onStickerDragEnd: ((stickerId: String, droppedInTrash: Boolean) -> Unit)? = null,
    onTransformSticker: ((stickerId: String, zoomDelta: Float, rotDelta: Float) -> Unit)? = null
) {
    val funkyColors = LocalFunkyColors.current
    val isDarkAppTheme = LocalAppDarkTheme.current
    val haptics = LocalHapticFeedback.current
    val reduceAnimations = rememberCardReduceAnimations()

    val effectiveDesign = remember(design, isDarkAppTheme) {
        design ?: CardDesign(
            theme = CardTheme(
                bg = if (isDarkAppTheme) "#231D15" else "#FFFBF0",
                bg2 = if (isDarkAppTheme) "#3A2D12" else "#FFE8A3",
                bgStyle = CardBgStyle.SOLID,
                accent = "#FFB627",
                border = CardBorderStyle.BOLD,
                radius = 28
            )
        )
    }

    val theme = effectiveDesign.theme
    val show = effectiveDesign.show

    val bgColor = remember(theme.bg, isDarkAppTheme) {
        parseCardColorOrDefault(theme.bg, if (isDarkAppTheme) Color(0xFF231D15) else Color(0xFFFFFBF0))
    }
    val bg2Color = remember(theme.bg2) {
        parseCardColorOrDefault(theme.bg2, Color(0xFFFFE8A3))
    }
    val accentColor = remember(theme.accent) {
        parseCardColorOrDefault(theme.accent, Color(0xFFFFB627))
    }

    val cardIsDark = remember(bgColor, bg2Color, theme.bgStyle) {
        if (theme.bgStyle == CardBgStyle.GRADIENT) {
            (bgColor.isCardDarkBackground() && bg2Color.isCardDarkBackground()) ||
                ((bgColor.red + bg2Color.red) * 0.5f * 0.299f +
                    (bgColor.green + bg2Color.green) * 0.5f * 0.587f +
                    (bgColor.blue + bg2Color.blue) * 0.5f * 0.114f) < 0.42f
        } else {
            bgColor.isCardDarkBackground()
        }
    }

    val primaryText = if (cardIsDark) Color(0xFFFFFBF2) else Color(0xFF1C1917)
    val secondaryText = if (cardIsDark) Color(0xFFD6CEC2) else Color(0xFF57534E)
    val inkBorderColor = if (cardIsDark) Color(0xFFF5EFE6).copy(alpha = 0.85f) else Color(0xFF1C1917)
    val hardShadowColor = if (isDarkAppTheme) Color(0xFF0A0806) else Color(0xFF1C1917)
    val chipSurface = if (cardIsDark) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.78f)
    val onAccentText = if (accentColor.isCardDarkBackground()) Color.White else Color(0xFF1C1917)
    val patternInk = if (cardIsDark) Color.White else Color(0xFF1C1917)

    val cornerRadiusDp = theme.radius.coerceIn(0, 32).dp
    val cardShape = RoundedCornerShape(cornerRadiusDp)
    val shadowOffset = if (compactMini) 2.dp else 4.dp

    // Design load fade-in & single wiggle (disabled in workshop & when reduceAnimations is true)
    val cardAlpha = remember(profile.userId) { Animatable(if (design != null && !isWorkshopInteractive && !reduceAnimations) 0.85f else 1f) }
    val cardWiggleDeg = remember(profile.userId) { Animatable(0f) }
    var hasWiggledForDesign by remember(profile.userId) { mutableStateOf(false) }

    LaunchedEffect(profile.userId, design != null, reduceAnimations, isWorkshopInteractive) {
        if (design != null && !isWorkshopInteractive && !reduceAnimations && !hasWiggledForDesign) {
            hasWiggledForDesign = true
            cardAlpha.snapTo(0.6f)
            cardAlpha.animateTo(1f, animationSpec = tween(260, easing = FastOutSlowInEasing))
            cardWiggleDeg.animateTo(-1.3f, tween(85))
            cardWiggleDeg.animateTo(1.3f, tween(110))
            cardWiggleDeg.animateTo(-0.5f, tween(85))
            cardWiggleDeg.animateTo(0f, tween(85))
        } else {
            cardAlpha.snapTo(1f)
            cardWiggleDeg.snapTo(0f)
        }
    }

    val clickMod = when {
        isWorkshopInteractive -> Modifier
        onCardClick != null || onCardLongClick != null -> Modifier.combinedClickable(
            onClick = { onCardClick?.invoke() },
            onLongClick = {
                if (onCardLongClick != null) {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onCardLongClick()
                }
            }
        )
        else -> Modifier
    }

    val cardScale = if (compactMini) 0.78f else 1f

    // Outer box reserves space for hard neo-brutalist offset shadow
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(end = shadowOffset, bottom = shadowOffset)
            .graphicsLayer {
                alpha = cardAlpha.value
                rotationZ = cardWiggleDeg.value
            }
    ) {
        // Hard offset ink shadow layer (neo-brutalist signature)
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = shadowOffset, y = shadowOffset)
                .clip(cardShape)
                .background(hardShadowColor)
        )

        // Main Card Surface (height determined purely by foreground content)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(cardShape)
                .then(
                    when (theme.bgStyle) {
                        CardBgStyle.GRADIENT -> Modifier.background(
                            Brush.linearGradient(
                                colors = listOf(bgColor, bg2Color),
                                start = Offset.Zero,
                                end = Offset(900f, 600f)
                            )
                        )
                        else -> Modifier.background(bgColor)
                    }
                )
                .drawWithContent {
                    // Draw subtle vector background pattern behind stickers & foreground
                    when (theme.bgStyle) {
                        CardBgStyle.DOTS -> {
                            val step = 18.dp.toPx()
                            val dotR = 1.6.dp.toPx()
                            val patColor = patternInk.copy(alpha = if (cardIsDark) 0.12f else 0.09f)
                            var y = step * 0.5f
                            var row = 0
                            while (y < size.height) {
                                var x = if (row % 2 == 0) step * 0.5f else step
                                while (x < size.width) {
                                    drawCircle(patColor, radius = dotR, center = Offset(x, y))
                                    x += step
                                }
                                y += step
                                row++
                            }
                        }
                        CardBgStyle.GRID -> {
                            val step = 20.dp.toPx()
                            val patColor = patternInk.copy(alpha = if (cardIsDark) 0.11f else 0.08f)
                            val strokeW = 1.dp.toPx()
                            var x = step
                            while (x < size.width) {
                                drawLine(patColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = strokeW)
                                x += step
                            }
                            var y = step
                            while (y < size.height) {
                                drawLine(patColor, Offset(0f, y), Offset(size.width, y), strokeWidth = strokeW)
                                y += step
                            }
                        }
                        CardBgStyle.PAPER -> {
                            val step = 22.dp.toPx()
                            val lineColor = patternInk.copy(alpha = if (cardIsDark) 0.11f else 0.08f)
                            val marginColor = accentColor.copy(alpha = 0.25f)
                            val marginX = 40.dp.toPx()
                            drawLine(marginColor, Offset(marginX, 0f), Offset(marginX, size.height), strokeWidth = 1.2.dp.toPx())
                            var y = step
                            while (y < size.height) {
                                drawLine(lineColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
                                y += step
                            }
                        }
                        CardBgStyle.STRIPES -> {
                            val step = 24.dp.toPx()
                            val patColor = patternInk.copy(alpha = if (cardIsDark) 0.09f else 0.06f)
                            val stripeW = 8.dp.toPx()
                            var x = -size.height
                            while (x < size.width + size.height) {
                                drawLine(
                                    color = patColor,
                                    start = Offset(x, size.height),
                                    end = Offset(x + size.height, 0f),
                                    strokeWidth = stripeW
                                )
                                x += step
                            }
                        }
                        else -> Unit
                    }

                    drawContent()

                    // Card border overlay
                    val cr = cornerRadiusDp.toPx()
                    when (theme.border) {
                        CardBorderStyle.NONE -> Unit
                        CardBorderStyle.THIN -> {
                            drawRoundRect(
                                color = inkBorderColor.copy(alpha = 0.7f),
                                cornerRadius = CornerRadius(cr, cr),
                                style = Stroke(width = 1.dp.toPx())
                            )
                        }
                        CardBorderStyle.BOLD -> {
                            drawRoundRect(
                                color = inkBorderColor,
                                cornerRadius = CornerRadius(cr, cr),
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }
                        CardBorderStyle.DASHED -> {
                            drawRoundRect(
                                color = inkBorderColor,
                                cornerRadius = CornerRadius(cr, cr),
                                style = Stroke(
                                    width = 2.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)
                                )
                            )
                        }
                    }
                }
                .then(clickMod)
        ) {
            // LAYER 1 (Overlay below text & Hi button): Stickers Layer (never shifts height)
            if (effectiveDesign.stickers.isNotEmpty()) {
                val cachedPacks by CardDesignRepository.stickerPacks.collectAsState()
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .zIndex(1f)
                ) {
                    effectiveDesign.stickers.take(CardDesignDefaults.MAX_STICKERS).forEach { sticker ->
                        val isSelected = isWorkshopInteractive && selectedStickerId == sticker.localId
                        val isNewlyAdded = isWorkshopInteractive && newlyAddedStickerId == sticker.localId
                        val popScale = remember(sticker.localId) {
                            Animatable(if (isNewlyAdded && !reduceAnimations) 0.45f else 1f)
                        }
                        LaunchedEffect(sticker.localId, isNewlyAdded) {
                            if (isNewlyAdded && !reduceAnimations) {
                                popScale.snapTo(0.45f)
                                popScale.animateTo(
                                    targetValue = 1f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                )
                            } else {
                                popScale.snapTo(1f)
                            }
                        }

                        val resolvedPackEmoji = remember(sticker.emoji, sticker.packCode, sticker.packId, sticker.packIdx, cachedPacks) {
                            if (sticker.kind == CardStickerKind.PACK) {
                                sticker.emoji.ifBlank {
                                    CardDesignRepository.resolvePackEmoji(sticker.packCode, sticker.packId, sticker.packIdx)
                                        ?: "✨"
                                }
                            } else {
                                sticker.emoji
                            }
                        }

                        StickerBadgeView(
                            sticker = if (resolvedPackEmoji != sticker.emoji) sticker.copy(emoji = resolvedPackEmoji) else sticker,
                            isSelected = isSelected,
                            compactMini = compactMini,
                            popScale = popScale.value,
                            cardIsDark = cardIsDark,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .layout { measurable, constraints ->
                                    val loose = constraints.copy(minWidth = 0, minHeight = 0)
                                    val placeable = measurable.measure(loose)
                                    val cx = (constraints.maxWidth * sticker.x).roundToInt()
                                    val cy = (constraints.maxHeight * sticker.y).roundToInt()
                                    layout(0, 0) {
                                        placeable.placeRelative(
                                            x = cx - placeable.width / 2,
                                            y = cy - placeable.height / 2
                                        )
                                    }
                                }
                        )
                    }
                }
            }

            // Workshop snap alignment guides (center X / Y dashed lines)
            if (isWorkshopInteractive && (snapGuideX || snapGuideY)) {
                Canvas(
                    modifier = Modifier
                        .matchParentSize()
                        .zIndex(1.5f)
                ) {
                    val guideColor = accentColor.copy(alpha = 0.85f)
                    val dash = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                    if (snapGuideX) {
                        drawLine(
                            color = guideColor,
                            start = Offset(size.width / 2f, 0f),
                            end = Offset(size.width / 2f, size.height),
                            strokeWidth = 1.5.dp.toPx(),
                            pathEffect = dash
                        )
                    }
                    if (snapGuideY) {
                        drawLine(
                            color = guideColor,
                            start = Offset(0f, size.height / 2f),
                            end = Offset(size.width, size.height / 2f),
                            strokeWidth = 1.5.dp.toPx(),
                            pathEffect = dash
                        )
                    }
                }
            }

            // LAYER 2: Foreground Content (Avatar + Name + Meta + Languages + Bio + Hobbies + Hi Button)
            val padH = if (compactMini) 10.dp else 16.dp
            val padV = if (compactMini) 10.dp else 14.dp
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(2f)
                    .padding(horizontal = padH, vertical = padV),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tilted Neo-Brutalist Avatar Column
                val userCosmeticsMap by CosmeticsManager.userCosmeticsMap.collectAsState()
                val equippedFrame = remember(profile.userId, userCosmeticsMap) {
                    val row = userCosmeticsMap[profile.userId] ?: CosmeticsManager.getCachedCosmetics(profile.userId)
                    row?.avatar_frame?.config?.let {
                        runCatching { CosmeticsConfigParser.parseAvatarFrame(it) }.getOrNull()
                    }
                }
                val avatarSize = if (compactMini) 52.dp else 72.dp
                val flagSize = if (compactMini) 17.dp else 22.dp

                Box(
                    modifier = Modifier
                        .size(avatarSize + 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // 2-degree tilted avatar frame
                    Box(
                        modifier = Modifier
                            .size(avatarSize)
                            .graphicsLayer { rotationZ = -2f },
                        contentAlignment = Alignment.Center
                    ) {
                        if (equippedFrame != null && !compactMini) {
                            FramedAvatar(
                                avatarUrl = profile.avatarUrl,
                                avatarSize = avatarSize - 6.dp,
                                frame = equippedFrame,
                                enableAnimation = false,
                                contentDescription = "${profile.name}'s avatar"
                            )
                        } else {
                            val avatarModel = resolveAvatarModel(profile.avatarUrl)
                            val avatarShape = RoundedCornerShape(if (compactMini) 14.dp else 20.dp)
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .offset(x = 2.dp, y = 2.dp)
                                    .clip(avatarShape)
                                    .background(hardShadowColor)
                            )
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(avatarModel)
                                    .crossfade(true)
                                    .placeholder(R.drawable.male_avatar_1781324792347)
                                    .error(R.drawable.male_avatar_1781324792347)
                                    .fallback(R.drawable.male_avatar_1781324792347)
                                    .build(),
                                contentDescription = "${profile.name}'s avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(avatarShape)
                                    .border(2.dp, inkBorderColor, avatarShape)
                            )
                        }
                    }

                    // Requirement B: Small green dot ONLY when isOnline && show.onlineDot; Offline = nothing
                    if (profile.isOnline && show.onlineDot) {
                        val dotSize = if (compactMini) 10.dp else 13.dp
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = (-1).dp, y = 1.dp)
                                .zIndex(6f)
                                .size(dotSize)
                                .clip(CircleShape)
                                .background(Color(0xFF22C55E))
                                .border(2.dp, bgColor, CircleShape)
                                .semantics { contentDescription = "Online" }
                        )
                    }

                    // Country Flag Badge when show.country is enabled
                    val cleanCountry = profile.countryCode.trim()
                    if (show.country && cleanCountry.isNotBlank() && cleanCountry.lowercase() != "un") {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset(x = 2.dp, y = 2.dp)
                                .zIndex(6f)
                                .size(flagSize)
                                .clip(CircleShape)
                                .background(bgColor)
                                .border(1.8.dp, inkBorderColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            CircleFlag(
                                countryCode = cleanCountry,
                                allowProgramming = false,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(if (compactMini) 8.dp else 13.dp))

                // Info Column
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(if (compactMini) 2.dp else 4.dp)
                ) {
                    // Name + Age + Gender (NO Offline pill ever!)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        DecoratedUserName(
                            userId = profile.userId,
                            name = profile.name,
                            fontSize = (16f * cardScale).sp,
                            fontWeight = FontWeight.Black,
                            defaultColor = primaryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (show.age && profile.age != null && profile.age > 0) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(chipSurface)
                                    .border(1.dp, inkBorderColor.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 6.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "${profile.age}",
                                    fontSize = (11f * cardScale).sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = primaryText
                                )
                            }
                        }
                        if (show.gender && !profile.gender.isNullOrBlank()) {
                            val genderLabel = when (profile.gender.trim().lowercase()) {
                                "female", "f", "woman" -> "♀"
                                "male", "m", "man" -> "♂"
                                else -> profile.gender.trim().take(8)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(accentColor.copy(alpha = 0.25f))
                                    .border(1.dp, inkBorderColor.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = genderLabel,
                                    fontSize = (10.5f * cardScale).sp,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryText
                                )
                            }
                        }
                    }

                    // City / Country row if enabled
                    val locationParts = buildList {
                        if (show.city && profile.city.isNotBlank()) add(profile.city.trim())
                        if (show.country && profile.country.isNotBlank()) add(profile.country.trim())
                    }.distinct()
                    if (locationParts.isNotEmpty() && (show.city || profile.city.isNotBlank())) {
                        // Show explicit city/country text line when city is enabled (flag already shows country)
                        if (show.city) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                ProfileLocationPinIcon(
                                    modifier = Modifier.size(if (compactMini) 10.dp else 12.dp),
                                    color = secondaryText
                                )
                                Text(
                                    text = locationParts.joinToString(", "),
                                    fontSize = (11f * cardScale).sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = secondaryText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Native -> Learning Arrow Language Chips
                    if (show.languages && (profile.nativeLang.isNotBlank() || profile.learningLang.isNotBlank())) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            if (profile.nativeLang.isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(chipSurface)
                                        .border(1.2.dp, inkBorderColor.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = if (compactMini) 5.dp else 7.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = profile.nativeLang.uppercase(),
                                        fontSize = (10.5f * cardScale).sp,
                                        fontWeight = FontWeight.Black,
                                        color = primaryText,
                                        maxLines = 1
                                    )
                                }
                            }
                            Text(
                                text = "→",
                                fontSize = (12f * cardScale).sp,
                                fontWeight = FontWeight.Black,
                                color = primaryText
                            )
                            if (profile.learningLang.isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(accentColor.copy(alpha = if (cardIsDark) 0.35f else 0.32f))
                                        .border(1.2.dp, inkBorderColor.copy(alpha = 0.65f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = if (compactMini) 5.dp else 7.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = profile.learningLang.uppercase(),
                                        fontSize = (10.5f * cardScale).sp,
                                        fontWeight = FontWeight.Black,
                                        color = primaryText,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    // Bio
                    if (show.bio && profile.bio.isNotBlank()) {
                        Text(
                            text = profile.bio,
                            fontSize = (12f * cardScale).sp,
                            color = secondaryText,
                            maxLines = if (compactMini) 1 else 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = (16f * cardScale).sp
                        )
                    }

                    // Hobbies tags
                    if (show.hobbies && profile.hobbies.isNotEmpty() && !compactMini) {
                        val shownHobbies = profile.hobbies.take(3)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            shownHobbies.forEach { tag ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(chipSurface)
                                        .border(1.dp, inkBorderColor.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                        .padding(horizontal = 7.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = tag,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = primaryText,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(if (compactMini) 6.dp else 10.dp))

                // Chunky Pill Hi Button with Hard Shadow Press-In
                val waveInteraction = remember { MutableInteractionSource() }
                val isPressed by waveInteraction.collectIsPressedAsState()
                val pressShift by animateDpAsState(
                    targetValue = if (isPressed) 2.dp else 0.dp,
                    animationSpec = tween(70),
                    label = "hiBtnPress"
                )
                val hiText = effectiveDesign.hiLabel.trim()
                    .take(CardDesignDefaults.MAX_HI_LABEL_LEN)
                    .ifBlank { "Hi" }
                val btnBg = when {
                    profile.isWaved -> if (cardIsDark) Color(0xFF14532D) else Color(0xFFDCFCE7)
                    else -> accentColor
                }
                val btnFg = when {
                    profile.isWaved -> if (cardIsDark) Color(0xFF86EFAC) else Color(0xFF166534)
                    else -> onAccentText
                }

                Box(
                    modifier = Modifier
                        .padding(end = if (compactMini) 2.dp else 3.dp, bottom = if (compactMini) 2.dp else 3.dp)
                        .defaultMinSize(
                            minWidth = if (compactMini) 44.dp else 56.dp,
                            minHeight = if (compactMini) 32.dp else 48.dp
                        )
                ) {
                    // Hard button shadow
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .offset(x = if (compactMini) 2.dp else 3.dp, y = if (compactMini) 2.dp else 3.dp)
                            .clip(RoundedCornerShape(100.dp))
                            .background(inkBorderColor)
                    )
                    // Foreground pill button
                    Box(
                        modifier = Modifier
                            .offset(x = pressShift, y = pressShift)
                            .defaultMinSize(
                                minWidth = if (compactMini) 44.dp else 56.dp,
                                minHeight = if (compactMini) 32.dp else 48.dp
                            )
                            .clip(RoundedCornerShape(100.dp))
                            .background(btnBg)
                            .border(if (compactMini) 1.5.dp else 2.dp, inkBorderColor, RoundedCornerShape(100.dp))
                            .then(
                                if (!isWorkshopInteractive && onWaveClick != null) {
                                    Modifier
                                        .testTag("people_card_hi_${profile.userId}")
                                        .clickable(
                                            interactionSource = waveInteraction,
                                            indication = null
                                        ) {
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onWaveClick()
                                        }
                                } else {
                                    Modifier
                                }
                            )
                            .padding(
                                horizontal = if (compactMini) 9.dp else 13.dp,
                                vertical = if (compactMini) 6.dp else 10.dp
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (profile.isWaved) {
                                SoftCheckmarkBadge(
                                    size = if (compactMini) 12.dp else 15.dp,
                                    badgeColor = Color(0xFF15803D),
                                    checkColor = Color.White
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Sent",
                                    fontSize = (12f * cardScale).sp,
                                    fontWeight = FontWeight.Black,
                                    color = btnFg,
                                    maxLines = 1
                                )
                            } else {
                                if (hiText.equals("Hi", ignoreCase = true) || hiText.equals("Hello", ignoreCase = true)) {
                                    SoftWavingHandIcon(
                                        color = btnFg,
                                        size = if (compactMini) 12.dp else 15.dp
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = hiText,
                                    fontSize = (12.5f * cardScale).sp,
                                    fontWeight = FontWeight.Black,
                                    color = btnFg,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // LAYER 3 (Workshop only): Transparent Gesture Capture Overlay for Stickers
            if (isWorkshopInteractive) {
                StickerWorkshopGestureOverlay(
                    stickers = effectiveDesign.stickers,
                    selectedStickerId = selectedStickerId,
                    onSelectSticker = { onSelectSticker?.invoke(it) },
                    onStickerDragStart = { onStickerDragStart?.invoke(it) },
                    onMoveSticker = { id, nx, ny, inTrash -> onMoveSticker?.invoke(id, nx, ny, inTrash) },
                    onStickerDragEnd = { id, inTrash -> onStickerDragEnd?.invoke(id, inTrash) },
                    onTransformSticker = { id, zoom, rot -> onTransformSticker?.invoke(id, zoom, rot) },
                    modifier = Modifier
                        .matchParentSize()
                        .zIndex(5f)
                )
            }
        }
    }
}

@Composable
private fun StickerWorkshopGestureOverlay(
    stickers: List<CardSticker>,
    selectedStickerId: String?,
    onSelectSticker: (String?) -> Unit,
    onStickerDragStart: (String) -> Unit,
    onMoveSticker: (String, Float, Float, Boolean) -> Unit,
    onStickerDragEnd: (String, Boolean) -> Unit,
    onTransformSticker: (String, Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentStickers by rememberUpdatedState(stickers)
    val currentSelectedId by rememberUpdatedState(selectedStickerId)
    val density = LocalDensity.current

    Box(
        modifier = modifier.pointerInput(Unit) {
            val hitRadiusPx = with(density) { 46.dp.toPx() }
            val trashThresholdY = size.height * 0.86f
            awaitEachGesture {
                val firstDown = awaitFirstDown(requireUnconsumed = false)
                val w = size.width.toFloat().coerceAtLeast(1f)
                val h = size.height.toFloat().coerceAtLeast(1f)

                val hitSticker = currentStickers.asReversed().firstOrNull { st ->
                    val sx = st.x * w
                    val sy = st.y * h
                    val r = hitRadiusPx * st.scale.coerceIn(0.6f, 2.0f)
                    abs(firstDown.position.x - sx) <= r && abs(firstDown.position.y - sy) <= r
                }

                if (hitSticker != null) {
                    firstDown.consume()
                    onSelectSticker(hitSticker.localId)
                } else {
                    // Check if user wants two-finger pinch on an already-selected sticker or deselect on tap
                    val targetId = currentSelectedId
                    var totalMove = 0f
                    var startedMulti = false
                    do {
                        val event = awaitPointerEvent()
                        val pressedCount = event.changes.count { it.pressed }
                        if (pressedCount >= 2 && targetId != null) {
                            startedMulti = true
                            val zoom = event.calculateZoom()
                            val rot = event.calculateRotation()
                            if (abs(zoom - 1f) > 0.002f || abs(rot) > 0.05f) {
                                onTransformSticker(targetId, zoom, rot)
                            }
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        } else {
                            event.changes.forEach {
                                val d = (it.position - it.previousPosition).getDistance()
                                totalMove += d
                            }
                        }
                    } while (event.changes.any { it.pressed })

                    if (!startedMulti && totalMove < 16f) {
                        onSelectSticker(null)
                    }
                    return@awaitEachGesture
                }

                val activeId = hitSticker.localId
                var draggingStarted = false
                var lastNormX = hitSticker.x
                var lastNormY = hitSticker.y
                var isInTrash = false

                do {
                    val event = awaitPointerEvent()
                    val pressedChanges = event.changes.filter { it.pressed }
                    if (pressedChanges.size >= 2) {
                        val zoom = event.calculateZoom()
                        val rot = event.calculateRotation()
                        val pan = event.calculatePan()
                        if (abs(zoom - 1f) > 0.002f || abs(rot) > 0.05f) {
                            onTransformSticker(activeId, zoom, rot)
                        }
                        if (pan != Offset.Zero) {
                            if (!draggingStarted) {
                                draggingStarted = true
                                onStickerDragStart(activeId)
                            }
                            lastNormX = (lastNormX + pan.x / w).coerceIn(0.03f, 0.97f)
                            lastNormY = (lastNormY + pan.y / h).coerceIn(0.03f, 0.97f)
                            val centroid = event.calculateCentroid(useCurrent = true)
                            isInTrash = centroid.y >= trashThresholdY
                            onMoveSticker(activeId, lastNormX, lastNormY, isInTrash)
                        }
                        event.changes.forEach { if (it.positionChanged()) it.consume() }
                    } else if (pressedChanges.size == 1) {
                        val ch = pressedChanges.first()
                        val delta = ch.position - ch.previousPosition
                        if (delta.getDistance() > 0.5f) {
                            if (!draggingStarted) {
                                draggingStarted = true
                                onStickerDragStart(activeId)
                            }
                            lastNormX = (lastNormX + delta.x / w).coerceIn(0.03f, 0.97f)
                            lastNormY = (lastNormY + delta.y / h).coerceIn(0.03f, 0.97f)
                            isInTrash = ch.position.y >= trashThresholdY
                            onMoveSticker(activeId, lastNormX, lastNormY, isInTrash)
                            ch.consume()
                        }
                    }
                } while (event.changes.any { it.pressed })

                if (draggingStarted) {
                    onStickerDragEnd(activeId, isInTrash)
                }
            }
        }
    )
}

@Composable
fun StickerBadgeView(
    sticker: CardSticker,
    isSelected: Boolean,
    compactMini: Boolean,
    popScale: Float,
    cardIsDark: Boolean,
    modifier: Modifier = Modifier
) {
    val sizeMult = if (compactMini) 0.72f else 1f
    val totalScale = (sticker.scale.coerceIn(0.5f, 2.5f) * popScale * sizeMult)
    val baseColor = remember(sticker.color, cardIsDark) {
        parseCardColorOrDefault(sticker.color, if (cardIsDark) Color.White else Color(0xFF1C1917))
    }

    val selectionBorderMod = if (isSelected) {
        Modifier.drawWithContent {
            drawContent()
            drawRoundRect(
                color = Color(0xFF2563EB),
                cornerRadius = CornerRadius(12.dp.toPx()),
                style = Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 7f), 0f)
                )
            )
        }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 36.dp)
            .graphicsLayer {
                rotationZ = sticker.rot.coerceIn(-45f, 45f)
                scaleX = totalScale
                scaleY = totalScale
            }
            .then(selectionBorderMod)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        if (sticker.kind == CardStickerKind.WORD) {
            val word = sticker.text.ifBlank { "Hello" }
            WordStickerBody(word = word, style = sticker.style, color = baseColor)
        } else {
            val glyph = sticker.emoji.ifBlank { "✨" }
            EmojiStickerBody(emoji = glyph, style = sticker.style, color = baseColor)
        }
    }
}

@Composable
private fun WordStickerBody(
    word: String,
    style: CardStickerStyle,
    color: Color
) {
    when (style) {
        CardStickerStyle.STICKER -> {
            // Die-cut ~4dp thick white outline + soft drop shadow + slight 3D lift
            Surface(
                shape = RoundedCornerShape(100.dp),
                color = Color.White,
                shadowElevation = 4.dp,
                modifier = Modifier.graphicsLayer { translationY = -2f }
            ) {
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .clip(RoundedCornerShape(100.dp))
                        .background(color)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val textFg = if (color.isCardDarkBackground()) Color.White else Color(0xFF1C1917)
                    Text(
                        text = word,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif,
                        color = textFg,
                        maxLines = 1
                    )
                }
            }
        }
        CardStickerStyle.PLAIN -> {
            Text(
                text = word,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                color = color,
                style = TextStyle(
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.25f),
                        offset = Offset(1f, 2f),
                        blurRadius = 3f
                    )
                ),
                maxLines = 1
            )
        }
        CardStickerStyle.BUBBLE -> {
            val fg = if (color.isCardDarkBackground()) Color.White else Color(0xFF1C1917)
            Box(
                modifier = Modifier
                    .shadow(3.dp, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .background(color)
                    .border(2.dp, Color(0xFF1C1917), RoundedCornerShape(16.dp))
                    .padding(horizontal = 11.dp, vertical = 5.dp)
            ) {
                Text(
                    text = word,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = fg,
                    maxLines = 1
                )
            }
        }
        CardStickerStyle.TAPE -> {
            Box(
                modifier = Modifier
                    .drawWithContent {
                        val tapeColor = color.copy(alpha = 0.28f)
                        val w = size.width
                        val h = size.height
                        val p = Path().apply {
                            moveTo(0f, 2f)
                            lineTo(w, 0f)
                            lineTo(w - 5f, h * 0.5f)
                            lineTo(w, h)
                            lineTo(0f, h - 2f)
                            lineTo(5f, h * 0.5f)
                            close()
                        }
                        drawPath(p, tapeColor)
                        drawContent()
                    }
                    .padding(horizontal = 14.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = word,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Black,
                    color = if (color.luminanceCompat() > 0.85f) Color(0xFF1C1917) else color,
                    maxLines = 1
                )
            }
        }
        CardStickerStyle.OUTLINE -> {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .border(2.dp, color, RoundedCornerShape(100.dp))
                    .padding(horizontal = 11.dp, vertical = 4.dp)
            ) {
                Text(
                    text = word,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Black,
                    color = color,
                    maxLines = 1
                )
            }
        }
        CardStickerStyle.NEON -> {
            Box(
                modifier = Modifier
                    .shadow(8.dp, RoundedCornerShape(100.dp), ambientColor = color, spotColor = color)
                    .clip(RoundedCornerShape(100.dp))
                    .background(Color(0xFF0F172A).copy(alpha = 0.88f))
                    .border(1.8.dp, color, RoundedCornerShape(100.dp))
                    .padding(horizontal = 11.dp, vertical = 4.dp)
            ) {
                Text(
                    text = word,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Black,
                    color = if (color == Color(0xFF1C1917)) Color(0xFF38BDF8) else color,
                    style = TextStyle(
                        shadow = Shadow(color = color.copy(alpha = 0.85f), blurRadius = 10f)
                    ),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun EmojiStickerBody(
    emoji: String,
    style: CardStickerStyle,
    color: Color
) {
    when (style) {
        CardStickerStyle.STICKER -> {
            Surface(
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 4.dp,
                modifier = Modifier.graphicsLayer { translationY = -2f }
            ) {
                Box(
                    modifier = Modifier.padding(5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = emoji, fontSize = 24.sp)
                }
            }
        }
        CardStickerStyle.PLAIN -> {
            Text(text = emoji, fontSize = 26.sp)
        }
        CardStickerStyle.BUBBLE -> {
            Box(
                modifier = Modifier
                    .shadow(3.dp, CircleShape)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.9f))
                    .border(2.dp, Color(0xFF1C1917), CircleShape)
                    .padding(7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = emoji, fontSize = 22.sp)
            }
        }
        CardStickerStyle.TAPE -> {
            Box(
                modifier = Modifier
                    .background(color.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = emoji, fontSize = 22.sp)
            }
        }
        CardStickerStyle.OUTLINE -> {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .border(2.dp, color, CircleShape)
                    .padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = emoji, fontSize = 22.sp)
            }
        }
        CardStickerStyle.NEON -> {
            Box(
                modifier = Modifier
                    .shadow(8.dp, CircleShape, ambientColor = color, spotColor = color)
                    .clip(CircleShape)
                    .background(Color(0xFF0F172A).copy(alpha = 0.85f))
                    .border(1.5.dp, color, CircleShape)
                    .padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = emoji, fontSize = 22.sp)
            }
        }
    }
}

private fun Color.luminanceCompat(): Float =
    0.299f * red + 0.587f * green + 0.114f * blue

private fun Offset.getDistance(): Float = kotlin.math.hypot(x, y)

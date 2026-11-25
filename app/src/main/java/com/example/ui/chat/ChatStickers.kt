package com.example.ui.chat

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.ui.theme.LocalFunkyColors

data class AnimatedStickerSpec(
    val id: String,
    val label: String,
    val emojiFallback: String,
    val accentColor: Color,
    val lottieJson: String
)

data class ParsedStickerToken(
    val sticker: AnimatedStickerSpec,
    val rawToken: String
)

/**
 * Self-contained open-source Lottie animated stickers inspired by Telegram.
 * Uses pure E2EE text transport (`[sticker:<id>|<emoji>]`) — zero backend schema changes required.
 */
object ChatAnimatedStickers {
    private val TOKEN_REGEX = Regex("""^\[sticker:([a-z0-9_]+)\|([^\]]+)\]$""")

    private fun buildRichStickerLottie(
        r: Float, g: Float, b: Float,
        r2: Float, g2: Float, b2: Float,
        bounceAmp: Int = 12,
        spinDeg: Int = 10,
        ringPoints: Int = 5
    ): String {
        val midY = 100 - bounceAmp
        val lowY = 100 + (bounceAmp / 3)
        return """
        {
          "v":"5.7.4","fr":30,"ip":0,"op":60,"w":200,"h":200,"nm":"telegram_sticker","ddd":0,
          "assets":[],
          "layers":[
            {
              "ddd":0,"ind":1,"ty":4,"nm":"outer_halo","sr":1,
              "ks":{
                "o":{"a":1,"k":[{"t":0,"s":[38],"e":[72]},{"t":30,"s":[72],"e":[38]},{"t":60,"s":[38]}]},
                "r":{"a":1,"k":[{"t":0,"s":[0],"e":[$spinDeg]},{"t":30,"s":[$spinDeg],"e":[-$spinDeg]},{"t":60,"s":[0]}]},
                "p":{"a":1,"k":[{"t":0,"s":[100,100,0],"e":[100,$midY,0]},{"t":28,"s":[100,$midY,0],"e":[100,$lowY,0]},{"t":45,"s":[100,$lowY,0],"e":[100,100,0]},{"t":60,"s":[100,100,0]}]},
                "a":{"a":0,"k":[0,0,0]},
                "s":{"a":1,"k":[{"t":0,"s":[88,88,100],"e":[108,108,100]},{"t":30,"s":[108,108,100],"e":[94,94,100]},{"t":60,"s":[88,88,100]}]}
              },
              "shapes":[
                {"ty":"gr","it":[
                  {"ty":"el","p":{"a":0,"k":[0,0]},"s":{"a":0,"k":[148,148]}},
                  {"ty":"fl","c":{"a":0,"k":[$r,$g,$b,1]},"o":{"a":0,"k":22}},
                  {"ty":"st","c":{"a":0,"k":[$r,$g,$b,1]},"o":{"a":0,"k":78},"w":{"a":0,"k":4.5}},
                  {"ty":"tr","p":{"a":0,"k":[0,0]},"a":{"a":0,"k":[0,0]},"s":{"a":0,"k":[100,100]},"r":{"a":0,"k":0},"o":{"a":0,"k":100}}
                ]}
              ],
              "ip":0,"op":60,"st":0
            },
            {
              "ddd":0,"ind":2,"ty":4,"nm":"accent_orbit","sr":1,
              "ks":{
                "o":{"a":1,"k":[{"t":0,"s":[70],"e":[25]},{"t":30,"s":[25],"e":[70]},{"t":60,"s":[70]}]},
                "r":{"a":1,"k":[{"t":0,"s":[0],"e":[360]},{"t":60,"s":[360]}]},
                "p":{"a":0,"k":[100,100,0]},
                "a":{"a":0,"k":[0,0,0]},
                "s":{"a":1,"k":[{"t":0,"s":[92,92,100],"e":[104,104,100]},{"t":30,"s":[104,104,100],"e":[92,92,100]},{"t":60,"s":[92,92,100]}]}
              },
              "shapes":[
                {"ty":"gr","it":[
                  {"ty":"sr","sy":1,"pt":{"a":0,"k":$ringPoints},"p":{"a":0,"k":[0,0]},"r":{"a":0,"k":0},"ir":{"a":0,"k":64},"or":{"a":0,"k":76}},
                  {"ty":"st","c":{"a":0,"k":[$r2,$g2,$b2,1]},"o":{"a":0,"k":55},"w":{"a":0,"k":3}},
                  {"ty":"tr","p":{"a":0,"k":[0,0]},"a":{"a":0,"k":[0,0]},"s":{"a":0,"k":[100,100]},"r":{"a":0,"k":0},"o":{"a":0,"k":100}}
                ]}
              ],
              "ip":0,"op":60,"st":0
            },
            {
              "ddd":0,"ind":3,"ty":4,"nm":"spark_top_right","sr":1,
              "ks":{
                "o":{"a":1,"k":[{"t":0,"s":[95],"e":[20]},{"t":30,"s":[20],"e":[95]},{"t":60,"s":[95]}]},
                "r":{"a":1,"k":[{"t":0,"s":[0],"e":[180]},{"t":60,"s":[360]}]},
                "p":{"a":0,"k":[160,40,0]},
                "a":{"a":0,"k":[0,0,0]},
                "s":{"a":1,"k":[{"t":0,"s":[65,65,100],"e":[120,120,100]},{"t":30,"s":[120,120,100],"e":[65,65,100]},{"t":60,"s":[65,65,100]}]}
              },
              "shapes":[
                {"ty":"gr","it":[
                  {"ty":"sr","sy":1,"pt":{"a":0,"k":4},"p":{"a":0,"k":[0,0]},"r":{"a":0,"k":0},"ir":{"a":0,"k":6},"or":{"a":0,"k":20}},
                  {"ty":"fl","c":{"a":0,"k":[$r2,$g2,$b2,1]},"o":{"a":0,"k":100}},
                  {"ty":"tr","p":{"a":0,"k":[0,0]},"a":{"a":0,"k":[0,0]},"s":{"a":0,"k":[100,100]},"r":{"a":0,"k":0},"o":{"a":0,"k":100}}
                ]}
              ],
              "ip":0,"op":60,"st":0
            },
            {
              "ddd":0,"ind":4,"ty":4,"nm":"spark_bottom_left","sr":1,
              "ks":{
                "o":{"a":1,"k":[{"t":0,"s":[25],"e":[95]},{"t":30,"s":[95],"e":[25]},{"t":60,"s":[25]}]},
                "r":{"a":1,"k":[{"t":0,"s":[45],"e":[225]},{"t":60,"s":[405]}]},
                "p":{"a":0,"k":[42,158,0]},
                "a":{"a":0,"k":[0,0,0]},
                "s":{"a":1,"k":[{"t":0,"s":[110,110,100],"e":[60,60,100]},{"t":30,"s":[60,60,100],"e":[110,110,100]},{"t":60,"s":[110,110,100]}]}
              },
              "shapes":[
                {"ty":"gr","it":[
                  {"ty":"sr","sy":1,"pt":{"a":0,"k":4},"p":{"a":0,"k":[0,0]},"r":{"a":0,"k":0},"ir":{"a":0,"k":4},"or":{"a":0,"k":14}},
                  {"ty":"fl","c":{"a":0,"k":[$r,$g,$b,1]},"o":{"a":0,"k":90}},
                  {"ty":"tr","p":{"a":0,"k":[0,0]},"a":{"a":0,"k":[0,0]},"s":{"a":0,"k":[100,100]},"r":{"a":0,"k":0},"o":{"a":0,"k":100}}
                ]}
              ],
              "ip":0,"op":60,"st":0
            }
          ]
        }
        """.trimIndent()
    }

    val allStickers: List<AnimatedStickerSpec> = listOf(
        AnimatedStickerSpec("party_pop", "Party", "🎉", Color(0xFFF59E0B), buildRichStickerLottie(0.96f, 0.62f, 0.04f, 0.92f, 0.28f, 0.60f, bounceAmp = 14, spinDeg = 12, ringPoints = 6)),
        AnimatedStickerSpec("heart_pulse", "Love", "❤️", Color(0xFFEF4444), buildRichStickerLottie(0.94f, 0.27f, 0.27f, 0.98f, 0.45f, 0.60f, bounceAmp = 10, spinDeg = 6, ringPoints = 8)),
        AnimatedStickerSpec("laugh_tears", "Haha", "😂", Color(0xFFFACC15), buildRichStickerLottie(0.98f, 0.80f, 0.08f, 0.22f, 0.74f, 0.97f, bounceAmp = 16, spinDeg = 10, ringPoints = 5)),
        AnimatedStickerSpec("fire_lit", "Fire", "🔥", Color(0xFFF97316), buildRichStickerLottie(0.98f, 0.45f, 0.09f, 0.99f, 0.88f, 0.28f, bounceAmp = 14, spinDeg = -8, ringPoints = 7)),
        AnimatedStickerSpec("star_cool", "Cool", "😎", Color(0xFF3B82F6), buildRichStickerLottie(0.23f, 0.51f, 0.96f, 0.55f, 0.36f, 0.96f, bounceAmp = 8, spinDeg = 8, ringPoints = 5)),
        AnimatedStickerSpec("rocket_go", "Rocket", "🚀", Color(0xFF6366F1), buildRichStickerLottie(0.39f, 0.40f, 0.95f, 0.96f, 0.62f, 0.04f, bounceAmp = 18, spinDeg = 14, ringPoints = 4)),
        AnimatedStickerSpec("wave_hi", "Hi!", "👋", Color(0xFF10B981), buildRichStickerLottie(0.06f, 0.73f, 0.51f, 0.98f, 0.75f, 0.14f, bounceAmp = 12, spinDeg = 18, ringPoints = 6)),
        AnimatedStickerSpec("thumbs_up", "Nice", "👍", Color(0xFF06B6D4), buildRichStickerLottie(0.02f, 0.71f, 0.83f, 0.23f, 0.51f, 0.96f, bounceAmp = 12, spinDeg = -10, ringPoints = 5)),
        AnimatedStickerSpec("crown_king", "Royal", "👑", Color(0xFFEAB308), buildRichStickerLottie(0.92f, 0.70f, 0.03f, 0.94f, 0.27f, 0.27f, bounceAmp = 10, spinDeg = 6, ringPoints = 8)),
        AnimatedStickerSpec("music_vibe", "Vibe", "🎧", Color(0xFFA855F7), buildRichStickerLottie(0.66f, 0.33f, 0.97f, 0.92f, 0.28f, 0.60f, bounceAmp = 14, spinDeg = -12, ringPoints = 6)),
        AnimatedStickerSpec("hug_warm", "Hug", "🤗", Color(0xFFEC4899), buildRichStickerLottie(0.93f, 0.28f, 0.60f, 0.98f, 0.57f, 0.24f, bounceAmp = 10, spinDeg = 8, ringPoints = 6)),
        AnimatedStickerSpec("night_moon", "Sleepy", "🌙", Color(0xFF64748B), buildRichStickerLottie(0.39f, 0.45f, 0.55f, 0.98f, 0.80f, 0.08f, bounceAmp = 8, spinDeg = 6, ringPoints = 4))
    )

    fun byId(id: String): AnimatedStickerSpec? = allStickers.firstOrNull { it.id == id }

    fun encode(sticker: AnimatedStickerSpec): String = "[sticker:${sticker.id}|${sticker.emojiFallback}]"

    fun parse(text: String?): ParsedStickerToken? {
        val trimmed = text?.trim() ?: return null
        val match = TOKEN_REGEX.matchEntire(trimmed) ?: return null
        val id = match.groupValues[1]
        val emoji = match.groupValues[2]
        val spec = byId(id) ?: AnimatedStickerSpec(
            id = id,
            label = "Sticker",
            emojiFallback = emoji,
            accentColor = Color(0xFFF59E0B),
            lottieJson = allStickers.first().lottieJson
        )
        return ParsedStickerToken(sticker = spec, rawToken = trimmed)
    }

    fun readableLabel(text: String?): String {
        val parsed = parse(text) ?: return text.orEmpty()
        return "${parsed.sticker.emojiFallback} ${parsed.sticker.label} Sticker"
    }
}

@Composable
fun AnimatedStickerGraphic(
    sticker: AnimatedStickerSpec,
    size: Dp = 118.dp,
    emojiFontSizeSp: Int = 52
) {
    val spec = remember(sticker.id) {
        LottieCompositionSpec.JsonString(sticker.lottieJson)
    }
    val composition by rememberLottieComposition(spec)
    var bounceTrigger by remember { mutableStateOf(false) }
    LaunchedEffect(bounceTrigger) {
        if (bounceTrigger) {
            kotlinx.coroutines.delay(180)
            bounceTrigger = false
        }
    }
    val springScale by animateFloatAsState(
        targetValue = if (bounceTrigger) 1.18f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "stickerBounce"
    )

    Box(
        modifier = Modifier
            .size(size)
            .scale(springScale)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                bounceTrigger = true
            },
        contentAlignment = Alignment.Center
    ) {
        if (composition != null) {
            LottieAnimation(
                composition = composition,
                iterations = LottieConstants.IterateForever,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize(0.78f)
                    .clip(RoundedCornerShape(28.dp))
                    .background(sticker.accentColor.copy(alpha = 0.14f))
            )
        }
        Text(
            text = sticker.emojiFallback,
            fontSize = emojiFontSizeSp.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun AnimatedStickerPickerGrid(
    onStickerSelected: (AnimatedStickerSpec) -> Unit,
    modifier: Modifier = Modifier
) {
    val funkyColors = LocalFunkyColors.current
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Animated Stickers",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = funkyColors.ink
            )
            Text(
                text = "Telegram Pack",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = funkyColors.mango,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(funkyColors.mango.copy(alpha = 0.14f))
                    .padding(horizontal = 7.dp, vertical = 2.dp)
            )
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 220.dp)
        ) {
            items(ChatAnimatedStickers.allStickers, key = { it.id }) { sticker ->
                val interactionSource = remember { MutableInteractionSource() }
                val pressed by interactionSource.collectIsPressedAsState()
                val scale by animateFloatAsState(
                    targetValue = if (pressed) 0.90f else 1.0f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "stickerGridBounce"
                )
                Column(
                    modifier = Modifier
                        .scale(scale)
                        .clip(RoundedCornerShape(14.dp))
                        .background(funkyColors.bg.copy(alpha = 0.65f))
                        .border(1.dp, funkyColors.line.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
                        .clickable(
                            interactionSource = interactionSource,
                            indication = androidx.compose.material3.ripple()
                        ) { onStickerSelected(sticker) }
                        .padding(vertical = 6.dp, horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AnimatedStickerGraphic(
                        sticker = sticker,
                        size = 54.dp,
                        emojiFontSizeSp = 26
                    )
                    Text(
                        text = sticker.label,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = funkyColors.mute,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

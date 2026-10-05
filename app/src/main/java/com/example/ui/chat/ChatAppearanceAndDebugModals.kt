package com.example.ui.chat

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudQueue
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ChatPreferencesManager
import com.example.CloudAiManager
import com.example.db.EncryptedChatDatabase
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun rememberEffectiveChatDarkTheme(): Boolean {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        ChatPreferencesManager.init(context)
    }
    val override by ChatPreferencesManager.chatDarkThemeOverride.collectAsState()
    val systemDark = isSystemInDarkTheme()
    return override ?: systemDark
}

/**
 * Hidden debug entry screen (triggered by long-pressing the app version in Settings).
 * Wraps ChatKitGallery(darkTheme, preset, textScale) with debug toggles for darkTheme,
 * the four ChatPreset values, and textScale so everything can be inspected on the phone.
 */
@Composable
fun ChatKitGalleryDebugDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val initialDark = rememberEffectiveChatDarkTheme()
    val savedPreset by ChatPreferencesManager.chatPreset.collectAsState()
    val savedScale by ChatPreferencesManager.chatTextScale.collectAsState()

    var debugDark by remember(initialDark) { mutableStateOf(initialDark) }
    var debugPreset by remember(savedPreset) { mutableStateOf(savedPreset) }
    var debugScale by remember(savedScale) { mutableStateOf(savedScale) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        BackHandler(onBack = onDismiss)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(if (debugDark) Color(0xFF181B20) else Color(0xFFFFFFFF))
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Debug Control Strip
            Surface(
                color = if (debugDark) Color(0xFF181B20) else Color(0xFFFFFFFF),
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "ChatKitGallery Debug",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (debugDark) Color(0xFFF3EFE2) else Color(0xFF1B1B1B)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (debugDark) Color(0xFF262A32) else Color(0xFFF1EEE4))
                                    .clickable {
                                        debugDark = !debugDark
                                        ChatPreferencesManager.setChatDarkThemeOverride(context, debugDark)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.DarkMode,
                                    contentDescription = "Toggle Dark Theme",
                                    tint = if (debugDark) Color(0xFFFFC629) else Color(0xFF6B6B66),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = if (debugDark) "Dark" else "Light",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (debugDark) Color(0xFFF3EFE2) else Color(0xFF1B1B1B)
                                )
                            }
                            IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                                Icon(
                                    Icons.Rounded.Close,
                                    contentDescription = "Close Gallery",
                                    tint = if (debugDark) Color(0xFFF3EFE2) else Color(0xFF1B1B1B)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    // Four Presets Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ChatPreset.entries.forEach { p ->
                            val selected = debugPreset == p
                            val previewColors = chatColors(p, debugDark)
                            Surface(
                                onClick = {
                                    debugPreset = p
                                    ChatPreferencesManager.setChatPreset(context, p)
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) previewColors.brand else (if (debugDark) Color(0xFF262A32) else Color(0xFFF1EEE4)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = p.name,
                                        fontSize = 12.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selected) previewColors.onBrand else (if (debugDark) Color(0xFFF3EFE2) else Color(0xFF1B1B1B))
                                    )
                                }
                            }
                        }
                    }

                    // Scale Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Scale ${(debugScale * 100).roundToInt()}%",
                            fontSize = 11.sp,
                            color = if (debugDark) Color(0xFFA7A59C) else Color(0xFF6B6B66),
                            modifier = Modifier.width(72.dp)
                        )
                        Slider(
                            value = debugScale,
                            onValueChange = {
                                debugScale = it
                                ChatPreferencesManager.setChatTextScale(context, it)
                            },
                            valueRange = 0.85f..1.4f,
                            modifier = Modifier.weight(1f).height(28.dp)
                        )
                    }
                }
            }

            // Actual Stateless ChatKitGallery
            Box(modifier = Modifier.weight(1f)) {
                ChatKitGallery(
                    darkTheme = debugDark,
                    preset = debugPreset,
                    textScale = debugScale
                )
            }
        }
    }
}

/**
 * Settings > Chat appearance bottom sheet:
 * - Preset picker (Butter, Sky, Mint, Graphite)
 * - Text size slider (0.85x to 1.4x)
 * - Theme mode selector (System / Light / Dark)
 * - Cloud AI consent toggle (Settings > AI)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatAppearanceBottomSheet(
    onDismiss: () -> Unit,
    onOpenDebugGallery: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        ChatPreferencesManager.init(context)
        CloudAiManager.init(context)
    }

    val currentPreset by ChatPreferencesManager.chatPreset.collectAsState()
    val currentScale by ChatPreferencesManager.chatTextScale.collectAsState()
    val darkOverride by ChatPreferencesManager.chatDarkThemeOverride.collectAsState()
    val effectiveDark = rememberEffectiveChatDarkTheme()
    val isCloudAiOn by CloudAiManager.isCloudAiEnabled.collectAsState()
    var showAiConsentSheet by remember { mutableStateOf(false) }

    val colors = chatColors(currentPreset, effectiveDark)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        contentColor = colors.text,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Chat appearance",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = colors.text
            )

            // Live Mini Preview
            ProvideChatTheme(
                darkTheme = effectiveDark,
                preset = currentPreset,
                textScale = currentScale
            ) {
                val c = LocalChatColors.current
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(c.wallpaper)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MessageBubble(outgoing = false, position = GroupPosition.Single) {
                        SenderName("Yuki", "yuki_preview")
                        MessageText(
                            text = AnnotatedString("Konnichiwa! Ready to practice today?"),
                            time = "12:30",
                            outgoing = false,
                            status = null
                        )
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        MessageBubble(outgoing = true, position = GroupPosition.Single) {
                            MessageText(
                                text = AnnotatedString("Yes! Let's start with conversational phrases."),
                                time = "12:31",
                                outgoing = true,
                                status = MessageStatus.Read
                            )
                        }
                    }
                }
            }

            // 1. Color Preset Picker (Butter, Sky, Mint, Graphite)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Color theme",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.text
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ChatPreset.entries.forEach { preset ->
                        val isSelected = currentPreset == preset
                        val pColors = chatColors(preset, effectiveDark)
                        Surface(
                            onClick = { ChatPreferencesManager.setChatPreset(context, preset) },
                            shape = RoundedCornerShape(14.dp),
                            color = pColors.wallpaper,
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) pColors.accentIn else colors.hairline
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(pColors.outgoingBubble)
                                        .border(1.dp, pColors.brand, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            Icons.Rounded.Check,
                                            contentDescription = "Selected",
                                            tint = pColors.text,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = preset.name,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = pColors.text
                                )
                            }
                        }
                    }
                }
            }

            // 2. Text Size Slider (0.85x to 1.4x)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Message text size",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.text
                    )
                    Text(
                        text = "${String.format("%.2f", currentScale)}x (${(16 * currentScale).roundToInt()}sp)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.accentIn
                    )
                }
                Slider(
                    value = currentScale,
                    onValueChange = { ChatPreferencesManager.setChatTextScale(context, it) },
                    valueRange = 0.85f..1.4f,
                    steps = 10,
                    colors = SliderDefaults.colors(
                        thumbColor = colors.brand,
                        activeTrackColor = colors.accentIn
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("0.85x", fontSize = 11.sp, color = colors.textSecondary)
                    Text("1.0x (Default)", fontSize = 11.sp, color = colors.textSecondary)
                    Text("1.40x", fontSize = 11.sp, color = colors.textSecondary)
                }
            }

            // 3. Dark Mode Option
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Dark mode",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.text
                    )
                    Text(
                        text = when (darkOverride) {
                            null -> "Match system (${if (effectiveDark) "Dark" else "Light"})"
                            true -> "Always Dark"
                            false -> "Always Light"
                        },
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Auto" to null, "Light" to false, "Dark" to true).forEach { (label, mode) ->
                        val active = darkOverride == mode
                        Surface(
                            onClick = { ChatPreferencesManager.setChatDarkThemeOverride(context, mode) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (active) colors.brand else colors.surfaceVariant
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                color = if (active) colors.onBrand else colors.text,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = colors.hairline)

            // 4. Settings > AI: Cloud AI Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Rounded.CloudQueue,
                        contentDescription = null,
                        tint = colors.accentIn,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "Cloud AI",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.text
                        )
                        Text(
                            text = if (isCloudAiOn) "Enabled for translation & writing assist" else "Off · Only on-device translation is used",
                            fontSize = 12.sp,
                            color = colors.textSecondary
                        )
                    }
                }
                Switch(
                    checked = isCloudAiOn,
                    onCheckedChange = { wantEnable ->
                        if (wantEnable) {
                            showAiConsentSheet = true
                        } else {
                            scope.launch {
                                CloudAiManager.revokeConsent(context)
                                Toast.makeText(context, "Cloud AI consent revoked", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }
        }
    }

    if (showAiConsentSheet) {
        AiConsentBottomSheet(
            onAgree = {
                showAiConsentSheet = false
                scope.launch {
                    CloudAiManager.acceptConsent(context)
                    Toast.makeText(context, "Cloud AI enabled", Toast.LENGTH_SHORT).show()
                }
            },
            onNotNow = {
                showAiConsentSheet = false
            }
        )
    }
}

/**
 * One-time Cloud AI Consent Bottom Sheet with the exact required transparency copy.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiConsentBottomSheet(
    onAgree: () -> Unit,
    onNotNow: () -> Unit
) {
    val c = LocalChatColors.current
    ModalBottomSheet(
        onDismissRequest = onNotNow,
        containerColor = c.surface,
        contentColor = c.text,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    Icons.Rounded.CloudQueue,
                    contentDescription = null,
                    tint = c.accentIn,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Cloud AI Features",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = c.text
                )
            }

            Text(
                text = "To use this feature, the selected text is sent to an AI service (Sarvam, Cerebras or Google) through FunkyTalk's server to be processed. This text leaves end-to-end encryption, and it can include messages from other people that you choose to translate. FunkyTalk does not store it and discards it as soon as you get the result. The AI provider handles it under its own terms.",
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = c.textSecondary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onNotNow,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Not now", color = c.text)
                }
                Button(
                    onClick = onAgree,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = c.brand,
                        contentColor = c.onBrand
                    )
                ) {
                    Text("Agree", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Word tap bottom sheet (Step 3h):
 * Translates the tapped word and lets the user listen to it or save it to their vocabulary list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordLookupBottomSheet(
    word: String,
    onListen: (String) -> Unit,
    onRequestCloudConsent: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val c = LocalChatColors.current
    val targetLang by CloudAiManager.myLanguage.collectAsState()
    val savedVocab by ChatPreferencesManager.savedVocab.collectAsState()

    var translatedWord by remember(word) { mutableStateOf<String?>(null) }
    var provider by remember(word) { mutableStateOf<String?>(null) }
    var isLoading by remember(word) { mutableStateOf(true) }
    var errorMsg by remember(word) { mutableStateOf<String?>(null) }

    val isAlreadySaved = remember(word, savedVocab) {
        savedVocab.any { it.word.equals(word, ignoreCase = true) }
    }

    LaunchedEffect(word) {
        isLoading = true
        errorMsg = null
        val hasConsent = CloudAiManager.isConsentAcceptedLocally(context)
        if (!hasConsent) {
            isLoading = false
            errorMsg = "Enable Cloud AI to translate individual words"
            return@LaunchedEffect
        }
        val res = CloudAiManager.translateMessage(
            context = context,
            messageId = "word_${word.lowercase()}_${targetLang.lowercase()}",
            text = word,
            targetLanguage = targetLang
        )
        res.onSuccess { ai ->
            translatedWord = ai.text
            provider = ai.provider
            isLoading = false
        }.onFailure { err ->
            isLoading = false
            if (err is com.example.AiError.ConsentRequired && onRequestCloudConsent != null) {
                onRequestCloudConsent()
            } else {
                errorMsg = err.message ?: "Could not translate word"
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        contentColor = c.text,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = word,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = c.text
                    )
                    Text(
                        text = "Translate to $targetLang",
                        fontSize = 12.sp,
                        color = c.textSecondary
                    )
                }
                IconButton(onClick = { onListen(word) }) {
                    Icon(
                        Icons.AutoMirrored.Rounded.VolumeUp,
                        contentDescription = "Listen pronunciation",
                        tint = c.accentIn
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = c.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.padding(16.dp)) {
                    when {
                        isLoading -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = c.accentIn,
                                    strokeWidth = 2.dp
                                )
                                Text("Looking up \"$word\"...", fontSize = 14.sp, color = c.textSecondary)
                            }
                        }
                        errorMsg != null -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(errorMsg!!, fontSize = 14.sp, color = c.danger)
                                if (onRequestCloudConsent != null && !CloudAiManager.isConsentAcceptedLocally(context)) {
                                    TextButton(onClick = {
                                        onDismiss()
                                        onRequestCloudConsent()
                                    }) {
                                        Text("Review Cloud AI Consent", color = c.accentIn)
                                    }
                                }
                            }
                        }
                        else -> {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = translatedWord.orEmpty(),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = c.text
                                )
                                if (!provider.isNullOrBlank() && provider != "on_device") {
                                    Text(
                                        text = "Cloud AI · via $provider",
                                        fontSize = 11.sp,
                                        color = c.textSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        val meaning = translatedWord ?: word
                        ChatPreferencesManager.saveVocabWord(context, word, meaning, targetLang)
                        Toast.makeText(context, "Saved \"$word\" to vocabulary", Toast.LENGTH_SHORT).show()
                    },
                    enabled = !isLoading,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isAlreadySaved) c.surfaceVariant else c.brand,
                        contentColor = if (isAlreadySaved) c.text else c.onBrand
                    )
                ) {
                    Icon(
                        if (isAlreadySaved) Icons.Rounded.Check else Icons.Rounded.BookmarkAdd,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (isAlreadySaved) "Saved in Vocabulary" else "Save to Vocabulary",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (savedVocab.isNotEmpty()) {
                HorizontalDivider(color = c.hairline)
                Text(
                    text = "Recent Vocabulary (${savedVocab.size})",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = c.textSecondary
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    savedVocab.take(4).forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(c.surfaceVariant.copy(alpha = 0.6f))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.word, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = c.text)
                                Text(item.translation, fontSize = 12.sp, color = c.textSecondary)
                            }
                            IconButton(
                                onClick = { ChatPreferencesManager.removeVocabWord(context, item.word) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.DeleteOutline,
                                    contentDescription = "Remove",
                                    tint = c.textSecondary,
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

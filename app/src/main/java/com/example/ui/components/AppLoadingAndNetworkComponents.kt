package com.example.ui.components

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Global Loading State Manager
 * Allows any screen, ViewModel, or network operation to show/hide a smooth loading HUD.
 */
object AppLoadingManager {
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _loadingMessage = MutableStateFlow("Loading...")
    val loadingMessage = _loadingMessage.asStateFlow()

    private val _progressPercent = MutableStateFlow<Int?>(null)
    val progressPercent = _progressPercent.asStateFlow()

    private val _cancelAction = MutableStateFlow<(() -> Unit)?>(null)
    val cancelAction = _cancelAction.asStateFlow()

    fun show(
        message: String = "Please wait...",
        progressPercent: Int? = null,
        onCancel: (() -> Unit)? = null
    ) {
        _loadingMessage.value = message
        _progressPercent.value = progressPercent
        _cancelAction.value = onCancel
        _isLoading.value = true
    }

    fun updateMessage(message: String) {
        _loadingMessage.value = message
    }

    fun updateProgress(percent: Int?, message: String? = null) {
        _progressPercent.value = percent?.coerceIn(0, 100)
        if (message != null) {
            _loadingMessage.value = message
        }
    }

    fun hide() {
        _isLoading.value = false
        _progressPercent.value = null
        _cancelAction.value = null
    }
}

/**
 * Global Network State & Connectivity Monitor
 */
object AppNetworkManager {
    private val _isOnline = MutableStateFlow(true)
    val isOnline = _isOnline.asStateFlow()

    private val _showNoInternetDialog = MutableStateFlow(false)
    val showNoInternetDialog = _showNoInternetDialog.asStateFlow()

    private var isMonitoringStarted = false

    fun startMonitoring(context: Context) {
        if (isMonitoringStarted) return
        isMonitoringStarted = true

        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (cm != null) {
            val isCurrentlyOnline = checkConnection(context)
            _isOnline.value = isCurrentlyOnline

            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            try {
                cm.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        _isOnline.value = true
                        _showNoInternetDialog.value = false
                    }

                    override fun onLost(network: Network) {
                        _isOnline.value = false
                    }
                })
            } catch (_: Exception) {
                // Fallback gracefully if permission or callback setup encounters restriction
            }
        }
    }

    fun checkConnection(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return true
        val activeNetwork = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
        val online = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        _isOnline.value = online
        return online
    }

    fun triggerNoInternetDialog() {
        _showNoInternetDialog.value = true
    }

    fun dismissNoInternetDialog() {
        _showNoInternetDialog.value = false
    }
}

/**
 * Cute, Soft Loading Popup HUD
 * Rendered centrally over the active screen when AppLoadingManager.isLoading is true.
 */
@Composable
fun AppLoadingHUDOverlay() {
    val isLoading by AppLoadingManager.isLoading.collectAsState()
    val loadingMessage by AppLoadingManager.loadingMessage.collectAsState()
    val progressPercent by AppLoadingManager.progressPercent.collectAsState()
    val cancelAction by AppLoadingManager.cancelAction.collectAsState()

    if (isLoading) {
        Dialog(
            onDismissRequest = { /* Modal blocking overlay */ },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
                    .testTag("app_loading_hud_overlay"),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .wrapContentSize()
                        .padding(horizontal = 32.dp),
                    shape = RoundedCornerShape(28.dp),
                    color = Color(0xFFFFFBF0), // Warm soft cream card
                    tonalElevation = 10.dp,
                    shadowElevation = 20.dp,
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFEADFC4))
                ) {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 28.dp, vertical = 24.dp)
                            .widthIn(min = 210.dp)
                            .wrapContentWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Cute Animated Loading Spinner Badge
                        CuteSpinningLoaderBadge(modifier = Modifier.size(64.dp))

                        Spacer(modifier = Modifier.height(18.dp))

                        // Soft Loading Message
                        Text(
                            text = loadingMessage,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2A1E00),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.testTag("loading_message_text")
                        )

                        if (progressPercent != null) {
                            val pct = progressPercent!!.coerceIn(0, 100)
                            Spacer(modifier = Modifier.height(12.dp))
                            androidx.compose.material3.LinearProgressIndicator(
                                progress = { pct / 100f },
                                modifier = Modifier
                                    .width(180.dp)
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = Color(0xFFFFB800),
                                trackColor = Color(0xFFEADFC4)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "$pct%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF7A5A00)
                            )
                        } else {
                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Please hold on a moment",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF7A7260),
                                textAlign = TextAlign.Center
                            )
                        }

                        if (cancelAction != null) {
                            Spacer(modifier = Modifier.height(14.dp))
                            androidx.compose.material3.OutlinedButton(
                                onClick = { cancelAction?.invoke() },
                                shape = RoundedCornerShape(18.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD6C8A5)),
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("loading_cancel_button")
                            ) {
                                Text(
                                    text = "Cancel",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2A1E00)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Soft & Cute "No Internet Connection" Popup Dialog
 */
@Composable
fun AppNoInternetDialog(
    onRetry: () -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    val context = LocalContext.current
    var isCheckingConnection by remember { mutableStateOf(false) }
    var statusHintText by remember { mutableStateOf("Please check your Wi-Fi or mobile data and try again.") }
    val coroutineScope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .wrapContentHeight()
                .testTag("no_internet_dialog"),
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFFFFFBF0), // Warm soft cream card
            tonalElevation = 10.dp,
            shadowElevation = 20.dp,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFEADFC4))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Soft Icon Badge with Cute Canvas Offline Cloud
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFE8E5)),
                    contentAlignment = Alignment.Center
                ) {
                    CuteCanvasOfflineCloudIcon(modifier = Modifier.size(36.dp), tint = Color(0xFFFF5A4D))
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Title
                Text(
                    text = "No Internet Connection",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF2B2118),
                    textAlign = TextAlign.Center,
                    letterSpacing = (-0.3).sp,
                    modifier = Modifier.testTag("no_internet_title")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Description Subtitle
                Text(
                    text = statusHintText,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF7A7260),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons Row
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Retry Button (Mango Yellow Main Action)
                    Button(
                        onClick = {
                            if (!isCheckingConnection) {
                                isCheckingConnection = true
                                coroutineScope.launch {
                                    kotlinx.coroutines.delay(1000)
                                    val online = AppNetworkManager.checkConnection(context)
                                    isCheckingConnection = false
                                    if (online) {
                                        AppNetworkManager.dismissNoInternetDialog()
                                        onRetry()
                                    } else {
                                        statusHintText = "Still offline. Please enable Wi-Fi or Cellular data."
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("no_internet_retry_button"),
                        shape = RoundedCornerShape(25.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFC21A),
                            contentColor = Color(0xFF2A1E00)
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        if (isCheckingConnection) {
                            Text(
                                text = "Checking connection...",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Text(
                                text = "Try Again",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    // Dismiss Button (Soft Outline)
                    Surface(
                        onClick = {
                            AppNetworkManager.dismissNoInternetDialog()
                            onDismiss()
                        },
                        color = Color.Transparent,
                        shape = RoundedCornerShape(25.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "Dismiss",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF7A7260)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Cute Canvas-drawn Spinning Loader Badge (No raw material defaults, soft animated spark dots)
 */
@Composable
fun CuteSpinningLoaderBadge(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "cute_loader")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearOutSlowInEasing)
        ),
        label = "rotation"
    )

    val scalePulse by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = modifier
            .scale(scalePulse)
            .rotate(rotationAngle),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val strokeWidth = 4.dp.toPx()

            // Draw outer soft background circle track
            drawCircle(
                color = Color(0xFFFFECB3),
                radius = w * 0.42f,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Draw active rotating mango arc
            drawArc(
                color = Color(0xFFFFB627),
                startAngle = -90f,
                sweepAngle = 140f,
                useCenter = false,
                style = Stroke(width = strokeWidth + 1.dp.toPx(), cap = StrokeCap.Round)
            )

            // Draw inner cute spark dots
            val numDots = 6
            val radius = w * 0.24f
            for (i in 0 until numDots) {
                val angle = (i * (360f / numDots)) * (Math.PI / 180.0)
                val dotX = (w / 2f) + (radius * Math.cos(angle)).toFloat()
                val dotY = (h / 2f) + (radius * Math.sin(angle)).toFloat()
                drawCircle(
                    color = if (i % 2 == 0) Color(0xFFFF8A1F) else Color(0xFFFFC21A),
                    radius = 3.dp.toPx(),
                    center = Offset(dotX, dotY)
                )
            }
        }
    }
}

/**
 * Custom Canvas-drawn Offline Cloud Icon (No raw material icons, soft cute aesthetic)
 */
@Composable
fun CuteCanvasOfflineCloudIcon(modifier: Modifier = Modifier, tint: Color = Color(0xFFFF5A4D)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeWidth = 2.5.dp.toPx()

        // Draw cloud outline
        val cloudPath = Path().apply {
            moveTo(w * 0.25f, h * 0.65f)
            cubicTo(w * 0.1f, h * 0.65f, w * 0.1f, h * 0.45f, w * 0.28f, h * 0.42f)
            cubicTo(w * 0.32f, h * 0.22f, w * 0.62f, h * 0.22f, w * 0.68f, h * 0.40f)
            cubicTo(w * 0.85f, h * 0.40f, w * 0.88f, h * 0.65f, w * 0.75f, h * 0.65f)
            close()
        }

        drawPath(
            path = cloudPath,
            color = tint.copy(alpha = 0.2f)
        )
        drawPath(
            path = cloudPath,
            color = tint,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Diagonal soft slash line indicating "No Connection / Offline"
        drawLine(
            color = tint,
            start = Offset(w * 0.2f, h * 0.25f),
            end = Offset(w * 0.8f, h * 0.8f),
            strokeWidth = strokeWidth + 0.5.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

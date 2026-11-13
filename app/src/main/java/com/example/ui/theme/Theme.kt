package com.example.ui.theme

import android.content.Context
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val PREFS_NAME = "funky_talk_prefs"
private const val KEY_APP_DARK_THEME = "app_dark_theme"

/**
 * Global dual-theme controller (Default = Light Cream White + Mango Yellow;
 * Optional Dark = Warm Brownish + Mango Yellow). Switched by user inside
 * Decoration Store, Settings, or Coin Store.
 */
object AppThemeManager {
    private val _isDarkTheme = MutableStateFlow(false)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    @Volatile
    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _isDarkTheme.value = prefs.getBoolean(KEY_APP_DARK_THEME, false)
        initialized = true
    }

    fun setDarkTheme(context: Context, isDark: Boolean) {
        _isDarkTheme.value = isDark
        initialized = true
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_APP_DARK_THEME, isDark)
            .apply()
    }

    fun toggleDarkTheme(context: Context) {
        setDarkTheme(context, !_isDarkTheme.value)
    }
}

@Immutable
data class FunkyColors(
    val isDark: Boolean,
    val bg: Color,
    val surface: Color,
    val soft: Color,
    val ink: Color,
    val mute: Color,
    val line: Color,
    val mango: Color,
    val onMango: Color,
    val ok: Color,
    val danger: Color
)

val LightFunkyColors = FunkyColors(
    isDark = false,
    bg = Color(0xFFFFF8E7),
    surface = Color(0xFFFFFFFF),
    soft = Color(0xFFFFEFC2),
    ink = Color(0xFF2B2118),
    mute = Color(0xFF8A7B66),
    line = Color(0xFFF0E3C4),
    mango = Color(0xFFFFB627),
    onMango = Color(0xFF2B2118),
    ok = Color(0xFF22B573),
    danger = Color(0xFFEF4444)
)

val DarkFunkyColors = FunkyColors(
    isDark = true,
    bg = Color(0xFF16120D),
    surface = Color(0xFF231D15),
    soft = Color(0xFF3A2D12),
    ink = Color(0xFFFFF3DC),
    mute = Color(0xFFA89A82),
    line = Color(0xFF34291A),
    mango = Color(0xFFFFB627),
    onMango = Color(0xFF2B2118),
    ok = Color(0xFF22B573),
    danger = Color(0xFFFF5A4D)
)

val LocalAppDarkTheme = compositionLocalOf { false }
val LocalFunkyColors = compositionLocalOf { LightFunkyColors }

private val DarkColorScheme = darkColorScheme(
    primary = GoldPrimary,
    secondary = GoldSecondary,
    tertiary = Pink80,
    background = DarkBg,
    surface = DarkSurface,
    surfaceVariant = DarkSoft,
    outline = DarkLine,
    onPrimary = Color(0xFF2B2118),
    onSecondary = Color(0xFF2B2118),
    onBackground = DarkInk,
    onSurface = DarkInk,
    onSurfaceVariant = DarkMute
)

private val LightColorScheme = lightColorScheme(
    primary = GoldPrimary,
    secondary = GoldSecondary,
    tertiary = GoldContainer,
    background = Color(0xFFFFF8E7),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFFFEFC2),
    outline = Color(0xFFF0E3C4),
    onPrimary = Color(0xFF2B2118),
    onSecondary = Color(0xFF2B2118),
    onBackground = Color(0xFF2B2118),
    onSurface = Color(0xFF2B2118),
    onSurfaceVariant = Color(0xFF8A7B66)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean? = null,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        AppThemeManager.init(context)
    }
    val storedDark by AppThemeManager.isDarkTheme.collectAsState()
    val effectiveDark = darkTheme ?: storedDark

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (effectiveDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        effectiveDark -> DarkColorScheme
        else -> LightColorScheme
    }

    val funkyPalette = if (effectiveDark) DarkFunkyColors else LightFunkyColors

    CompositionLocalProvider(
        LocalAppDarkTheme provides effectiveDark,
        LocalFunkyColors provides funkyPalette
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

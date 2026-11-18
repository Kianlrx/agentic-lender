package com.example.agenticlender.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Global theme state holder for dynamic dark mode switching
object ThemeState {
    var isDarkTheme = mutableStateOf(false)
}

// Dark Mode Color Palette (Midnight Blue-Black & Electric Blue)
private val DarkColorScheme = darkColorScheme(
    primary = BrightBluePrimary,
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF003853),
    onPrimaryContainer = Color(0xFFCBE7FF),
    secondary = BrightBlueSecondary,
    onSecondary = Color(0xFF000000),
    secondaryContainer = Color(0xFF004B70),
    onSecondaryContainer = Color(0xFFD1ECFF),
    tertiary = BrightBlueTertiary,
    background = DarkBackground,
    onBackground = Color(0xFFF1F5F9),
    surface = DarkSurface,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF475569),
    outlineVariant = Color(0xFF1E293B)
)

// Light Mode Color Palette (Vibrant Royal Blue & Fresh Blue Background)
private val LightColorScheme = lightColorScheme(
    primary = RoyalBluePrimary,
    onPrimary = OnRoyalBluePrimary,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF1E3A8A),
    secondary = RoyalBlueSecondary,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF0369A1),
    tertiary = RoyalBlueTertiary,
    background = LightBlueBackground,
    onBackground = Color(0xFF0F172A),
    surface = LightBlueSurface,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = LightBlueSurfaceVariant,
    onSurfaceVariant = Color(0xFF1E293B),
    outline = Color(0xFF94A3B8),
    outlineVariant = Color(0xFFCBD5E1)
)

// Main Agentic Lender Compose Theme
@Composable
fun AgenticLenderTheme(
    darkTheme: Boolean = ThemeState.isDarkTheme.value,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

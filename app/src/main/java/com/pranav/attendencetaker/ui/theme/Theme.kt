package com.pranav.attendencetaker.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// 1. Define the Dark Color Scheme manually for better control
private val DarkColorScheme = darkColorScheme(
    primary = DuoBlue,
    background = DarkBackground,
    surface = DarkSurface,
    onPrimary = Color.White,
    onBackground = DarkTextPrimary,
    onSurface = DarkTextPrimary
)

private val LightColorScheme = lightColorScheme(
    primary = DuoBlue,
    background = Color.White,
    surface = Color.White,
    onPrimary = Color.White,
    onBackground = Color(0xFF4B4B4B),
    onSurface = Color(0xFF4B4B4B)
)

// 2. Define an Enum for App Theme
enum class AppTheme {
    SYSTEM, LIGHT, DARK
}

@Composable
fun AttendenceTakerTheme(
    appTheme: AppTheme = AppTheme.SYSTEM, // New parameter
    dynamicColor: Boolean = false, // Disable dynamic color for consistent branding
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()

    // 3. Determine actual dark mode state based on preference
    val useDarkTheme = when (appTheme) {
        AppTheme.SYSTEM -> systemDark
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
    }

    val context = LocalContext.current

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (useDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        useDarkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
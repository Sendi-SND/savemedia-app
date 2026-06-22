package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = TealPrimary,
    secondary = TealPrimaryDark,
    tertiary = CyanAccent,
    background = SlateDarkBg,
    surface = SlateDarkSurface,
    surfaceVariant = SlateDarkSurfaceVariant,
    onPrimary = SlateDarkBg,
    onSecondary = SlateDarkBg,
    onTertiary = SlateDarkBg,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    onSurfaceVariant = TextSecondaryDark
)

private val LightColorScheme = lightColorScheme(
    primary = TealPrimaryDark,
    secondary = TealPrimary,
    tertiary = CyanAccent,
    background = TextPrimaryDark,
    surface = androidx.compose.ui.graphics.Color(0xFFF0F4F8),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFFE2EBF0),
    onPrimary = TextPrimaryDark,
    onSecondary = SlateDarkBg,
    onTertiary = SlateDarkBg,
    onBackground = SlateDarkBg,
    onSurface = SlateDarkBg,
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF4A5568)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set to false to enforce our high-fashion branded styling consistently
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> DarkColorScheme // Force dark theme by default since it looks significantly premium for downolader utilities!
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

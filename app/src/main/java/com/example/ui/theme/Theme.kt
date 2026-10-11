package com.example.ui.theme

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

private val DarkColorScheme = darkColorScheme(
    primary = AmberPrimary,
    onPrimary = Color(0xFF1E1500),
    primaryContainer = Color(0xFF452B00),
    onPrimaryContainer = AmberLight,
    secondary = CyanSecondary,
    onSecondary = Color(0xFF001F26),
    secondaryContainer = Color(0xFF004E5D),
    onSecondaryContainer = CyanLight,
    tertiary = EmeraldTertiary,
    onTertiary = Color.White,
    background = SlateBackgroundDark,
    onBackground = SlateTextPrimaryDark,
    surface = SlateSurfaceDark,
    onSurface = SlateTextPrimaryDark,
    surfaceVariant = SlateSurfaceCardDark,
    onSurfaceVariant = SlateTextSecondaryDark,
    outline = Color(0xFF334155),
    error = PriorityHigh
)

private val LightColorScheme = lightColorScheme(
    primary = AmberPrimaryVariant,
    onPrimary = Color.White,
    primaryContainer = AmberLight,
    onPrimaryContainer = Color(0xFF452B00),
    secondary = CyanSecondaryDark,
    onSecondary = Color.White,
    secondaryContainer = CyanLight,
    onSecondaryContainer = Color(0xFF001F26),
    tertiary = EmeraldDark,
    onTertiary = Color.White,
    background = SlateBackgroundLight,
    onBackground = SlateTextPrimaryLight,
    surface = SlateSurfaceLight,
    onSurface = SlateTextPrimaryLight,
    surfaceVariant = SlateSurfaceCardLight,
    onSurfaceVariant = SlateTextSecondaryLight,
    outline = Color(0xFFCBD5E1),
    error = PriorityHigh
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep branded ActionFlow look by default
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
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

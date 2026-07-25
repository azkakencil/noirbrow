package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = PureWhite,
    onPrimary = PureBlack,
    primaryContainer = DarkSurfaceVariant,
    onPrimaryContainer = PureWhite,
    secondary = LightGray,
    onSecondary = PureBlack,
    background = PureBlack,
    onBackground = PureWhite,
    surface = DarkSurface,
    onSurface = PureWhite,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = LightGray,
    outline = DarkBorder,
    outlineVariant = DarkGray
)

private val LightColorScheme = lightColorScheme(
    primary = PureBlack,
    onPrimary = PureWhite,
    primaryContainer = LightSurfaceVariant,
    onPrimaryContainer = PureBlack,
    secondary = DarkGray,
    onSecondary = PureWhite,
    background = PureWhite,
    onBackground = PureBlack,
    surface = LightSurface,
    onSurface = PureBlack,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = DarkGray,
    outline = LightBorder,
    outlineVariant = MediumGray
)

@Composable
fun NoirTheme(
    darkTheme: Boolean = true, // Default to Noir Dark Mode
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val CosmicColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = DarkBackground,
    secondary = VioletGlow,
    onSecondary = WhitePrimary,
    tertiary = SoftTeal,
    onTertiary = DarkBackground,
    background = DarkBackground,
    onBackground = WhitePrimary,
    surface = DarkSurface,
    onSurface = WhitePrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = MutedText,
    error = RedAlert,
    onError = WhitePrimary
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CosmicColorScheme,
        typography = Typography,
        content = content
    )
}

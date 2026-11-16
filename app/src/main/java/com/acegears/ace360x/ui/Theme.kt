package com.acegears.ace360x.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Base dark theme (graphite + restrained silver). This is the foundation the
 * Phase 9 theme engine will extend; it is not one of the 12 finished themes.
 */
private val BaseDark = darkColorScheme(
    primary = Color(0xFFC9D1DB),
    onPrimary = Color(0xFF15181C),
    primaryContainer = Color(0xFF2C3138),
    onPrimaryContainer = Color(0xFFE3E8EE),
    secondary = Color(0xFF8FA3B8),
    onSecondary = Color(0xFF0F1215),
    background = Color(0xFF0D0E10),
    onBackground = Color(0xFFE6E8EB),
    surface = Color(0xFF141619),
    onSurface = Color(0xFFE6E8EB),
    surfaceVariant = Color(0xFF1E2125),
    onSurfaceVariant = Color(0xFFB4BAC2),
    surfaceContainer = Color(0xFF181B1E),
    outline = Color(0xFF4A5058),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

@Composable
fun Ace360XTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = BaseDark, content = content)
}

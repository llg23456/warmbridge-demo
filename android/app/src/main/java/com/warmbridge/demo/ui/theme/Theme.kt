package com.warmbridge.demo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val WbShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
)

private val LightColors = lightColorScheme(
    primary = WbPrimary,
    onPrimary = Color.White,
    primaryContainer = WbSoft,
    onPrimaryContainer = WbTextPrimary,
    secondary = WbSecondary,
    onSecondary = Color.White,
    tertiary = WbIndigo,
    background = WbPageBg,
    surface = WbSurface,
    surfaceVariant = WbSoft,
    onBackground = WbTextPrimary,
    onSurface = WbTextPrimary,
    onSurfaceVariant = WbTextSecondary,
    outline = WbDivider,
    error = Color(0xFFB3261E),
)

private val DarkColors = darkColorScheme(
    primary = WbPrimary,
    onPrimary = Color.Black,
    secondary = WbSoft,
    background = Color(0xFF1C1B19),
    surface = Color(0xFF2A2826),
    onBackground = Color(0xFFF5EDE6),
    onSurface = Color(0xFFF5EDE6),
)

@Composable
fun WarmBridgeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = WbTypography,
        shapes = WbShapes,
        content = content,
    )
}

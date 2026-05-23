package com.farah.jewelryar.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val GreenDark = Color(0xFF2D4A2D)
val GreenMid = Color(0xFF3D5C3D)
val Gold = Color(0xFFC8A84C)
val Cream = Color(0xFFF5F0E8)
val TextDark = Color(0xFF1A2E1A)
val TextMed = Color(0xFF5A6E5A)

private val LightColorScheme = lightColorScheme(
    primary = GreenDark,
    onPrimary = Color.White,
    primaryContainer = GreenMid,
    onPrimaryContainer = Color.White,
    secondary = Gold,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFAF0D0),
    onSecondaryContainer = TextDark,
    background = Cream,
    onBackground = TextDark,
    surface = Color.White,
    onSurface = TextDark,
    surfaceVariant = Color(0xFFF5F0EB),
    onSurfaceVariant = TextMed,
    outline = Color(0xFFC8B896),
)

@Composable
fun KmmJewelryARTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}

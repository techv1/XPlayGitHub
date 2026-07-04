package com.techv1.xplay.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val XPlayDarkColorScheme = darkColorScheme(
    background       = BackgroundDeep,
    surface          = BackgroundSurface,
    surfaceVariant   = BackgroundCard,
    primary          = AccentPrimary,
    primaryContainer = AccentContainer,
    onPrimary        = ContentPrimary,
    onBackground     = ContentPrimary,
    onSurface        = ContentPrimary,
    onSurfaceVariant = ContentSecondary,
    error            = Error,
    secondary        = AccentLight,
)

@Composable
fun XPlayTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = XPlayDarkColorScheme,
        typography  = XPlayTypography,
        shapes      = XPlayShapes,
        content     = content
    )
}

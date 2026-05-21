package com.timofeeva.cyclesense.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = CycleSenseCosmos,
    onPrimary = Color.White,

    secondary = CycleSenseSoftAccent,
    onSecondary = CycleSenseTextPrimary,

    tertiary = CycleSenseSoftAccent,
    onTertiary = CycleSenseTextPrimary,

    background = CycleSenseBackground,
    onBackground = CycleSenseTextPrimary,

    surface = CycleSenseSurface,
    onSurface = CycleSenseTextPrimary,

    surfaceVariant = CycleSenseSurfaceSoft,
    onSurfaceVariant = CycleSenseTextSecondary,

    error = CycleSenseError,
    onError = CycleSenseSurface,

    outline = CycleSenseLavender
)

@Composable
fun CycleSenseTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
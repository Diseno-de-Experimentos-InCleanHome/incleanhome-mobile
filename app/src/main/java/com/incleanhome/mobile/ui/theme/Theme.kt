package com.incleanhome.mobile.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val InCleanHomeColorScheme = lightColorScheme(
    primary = PrimaryGreen,
    onPrimary = Color.White,
    primaryContainer = GreenLight,
    onPrimaryContainer = Navy,
    inversePrimary = GreenLight,
    secondary = Navy,
    onSecondary = Color.White,
    secondaryContainer = Border,
    onSecondaryContainer = Navy,
    tertiary = PrimaryGreenDark,
    onTertiary = Color.White,
    tertiaryContainer = GreenLight,
    onTertiaryContainer = Navy,
    background = Background,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = Background,
    onSurfaceVariant = TextSecondary,
    surfaceTint = PrimaryGreen,
    inverseSurface = Navy,
    inverseOnSurface = Color.White,
    error = Error,
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = Border,
    outlineVariant = Border,
    scrim = Navy
)

@Composable
fun InCleanHomeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = InCleanHomeColorScheme,
        typography = InCleanHomeTypography,
        shapes = InCleanHomeShapes,
        content = content
    )
}

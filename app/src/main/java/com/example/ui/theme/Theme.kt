package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val GenDevDarkColorScheme = darkColorScheme(
    primary = ElectricViolet,
    onPrimary = ObsidianVoid,
    primaryContainer = VioletContainer,
    onPrimaryContainer = OnVioletContainer,
    secondary = NeonCyan,
    onSecondary = ObsidianVoid,
    secondaryContainer = CyanContainer,
    onSecondaryContainer = OnCyanContainer,
    tertiary = PlasmaMagenta,
    onTertiary = ObsidianVoid,
    background = ObsidianVoid,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianCard,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = ObsidianVoid,
    surfaceContainerLow = ObsidianSurface,
    surfaceContainer = ObsidianCard,
    surfaceContainerHigh = ObsidianElevated,
    surfaceContainerHighest = ObsidianBorder,
    outline = ObsidianBorder,
    outlineVariant = ObsidianElevated,
    error = CoralError,
    onError = ObsidianVoid,
    errorContainer = ErrorContainerDark,
    onErrorContainer = TextPrimary
)

val GenDevShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun GenDevTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = GenDevDarkColorScheme,
        typography = GenDevTypography,
        shapes = GenDevShapes,
        content = content
    )
}

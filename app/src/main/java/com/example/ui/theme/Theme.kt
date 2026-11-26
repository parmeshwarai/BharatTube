package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val BharatDarkScheme = darkColorScheme(
    primary = SaffronPrimary,
    onPrimary = Color.White,
    primaryContainer = SaffronContainerDark,
    onPrimaryContainer = Color(0xFFFFD8C7),
    secondary = PeacockTeal,
    onSecondary = Color(0xFF00201C),
    secondaryContainer = PeacockContainerDark,
    onSecondaryContainer = Color(0xFFA7F3E8),
    tertiary = GoldHighlight,
    onTertiary = Color(0xFF281900),
    tertiaryContainer = GoldContainerDark,
    onTertiaryContainer = Color(0xFFFFE099),
    background = ObsidianBg,
    onBackground = TextOnDark,
    surface = ObsidianSurface,
    onSurface = TextOnDark,
    surfaceVariant = ObsidianSurfaceHigh,
    onSurfaceVariant = TextMutedOnDark,
    surfaceContainerLowest = ObsidianBg,
    surfaceContainerLow = ObsidianSurface,
    surfaceContainer = ObsidianSurface,
    surfaceContainerHigh = ObsidianSurfaceHigh,
    surfaceContainerHighest = Color(0xFF252E42),
    outline = ObsidianOutline,
    error = CrimsonError,
    onError = Color.White
)

private val BharatLightScheme = lightColorScheme(
    primary = SaffronPrimary,
    onPrimary = Color.White,
    primaryContainer = SaffronContainerLight,
    onPrimaryContainer = Color(0xFF4A1800),
    secondary = Color(0xFF00897B),
    onSecondary = Color.White,
    secondaryContainer = PeacockContainerLight,
    onSecondaryContainer = Color(0xFF003730),
    tertiary = Color(0xFFD97706),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFEF3C7),
    onTertiaryContainer = Color(0xFF451A03),
    background = IvoryBg,
    onBackground = TextOnLight,
    surface = IvorySurface,
    onSurface = TextOnLight,
    surfaceVariant = IvorySurfaceHigh,
    onSurfaceVariant = TextMutedOnLight,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = IvoryBg,
    surfaceContainer = IvorySurface,
    surfaceContainerHigh = IvorySurfaceHigh,
    surfaceContainerHighest = Color(0xFFE7E4DC),
    outline = IvoryOutline,
    error = CrimsonError,
    onError = Color.White
)

private val BharatShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun BharatTubeTheme(
    themeMode: String = "DARK",
    highContrast: Boolean = false,
    fontScale: Float = 1.0f,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        "LIGHT" -> false
        "DARK" -> true
        else -> isSystemInDarkTheme()
    }
    val baseScheme = if (isDark) BharatDarkScheme else BharatLightScheme
    val colorScheme = if (highContrast) {
        if (isDark) {
            baseScheme.copy(
                background = Color.Black,
                surface = Color(0xFF0A0A0A),
                outline = Color.White.copy(alpha = 0.7f),
                onSurfaceVariant = Color.White
            )
        } else {
            baseScheme.copy(
                background = Color.White,
                surface = Color.White,
                outline = Color.Black,
                onSurfaceVariant = Color.Black
            )
        }
    } else {
        baseScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = bharatTubeTypography(fontScale),
        shapes = BharatShapes,
        content = content
    )
}

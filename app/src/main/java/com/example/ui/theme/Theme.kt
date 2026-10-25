package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val FrostRushColorScheme = darkColorScheme(
    primary = IceCyan,
    onPrimary = GlacierMidnight,
    primaryContainer = FrostedSlate,
    onPrimaryContainer = IceCyanBright,
    secondary = SolarCoinGold,
    onSecondary = GlacierMidnight,
    secondaryContainer = ColorTokens.GoldContainer,
    onSecondaryContainer = SolarCoinGold,
    tertiary = AuroraPurple,
    onTertiary = GlacierMidnight,
    background = GlacierDeepNavy,
    onBackground = SnowWhite,
    surface = FrostedSlate,
    onSurface = SnowWhite,
    surfaceVariant = GlacierMidnight,
    onSurfaceVariant = FrostMist,
    error = CrimsonPeak,
    onError = SnowWhite,
    outline = SlateMuted
)

private object ColorTokens {
    val GoldContainer = androidx.compose.ui.graphics.Color(0xFF3A2B07)
}

val FrostRushShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun FrostRushTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FrostRushColorScheme,
        typography = AppTypography,
        shapes = FrostRushShapes,
        content = content
    )
}

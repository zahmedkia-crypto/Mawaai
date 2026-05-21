package com.mawaai.love.app.core.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

private val MawaaiColorScheme = darkColorScheme(
    primary = MawaaiColors.SoftRose,
    onPrimary = MawaaiColors.PearlWhite,
    primaryContainer = MawaaiColors.DeepRose,
    onPrimaryContainer = MawaaiColors.PearlWhite,
    secondary = MawaaiColors.ChampagneGold,
    onSecondary = MawaaiColors.DeepNight,
    surface = MawaaiColors.SurfaceDark,
    onSurface = MawaaiColors.TextPrimary,
    background = MawaaiColors.DeepNight,
    onBackground = MawaaiColors.TextPrimary,
    error = MawaaiColors.CrimsonRed,
    onError = MawaaiColors.PearlWhite,
    surfaceVariant = MawaaiColors.CardDark,
    onSurfaceVariant = MawaaiColors.TextSecondary,
    outline = MawaaiColors.GlassBorder
)

val MawaaiShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun MawaaiTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = MawaaiColorScheme,
            typography = MawaaiTypography,
            shapes = MawaaiShapes,
            content = content
        )
    }
}

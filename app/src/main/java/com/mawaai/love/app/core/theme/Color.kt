package com.mawaai.love.app.core.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object MawaaiColors {
    val DeepNight      = Color(0xFF0A0510)
    val SurfaceDark    = Color(0xFF130A1C)
    val CardDark       = Color(0xFF1A0F28)
    val CardElevated   = Color(0xFF221436)

    val RoseGold       = Color(0xFFE8A7B5)
    val RoseGoldDim    = Color(0xFFC4849A)
    val ChampagneGold  = Color(0xFFD4AF37)
    val SoftRose       = Color(0xFFFF6B8A)
    val DeepRose       = Color(0xFFE0294A)
    val PearlWhite     = Color(0xFFFFF0F5)
    val LavenderPurple = Color(0xFF9B59B6)
    val CrimsonRed     = Color(0xFF8B0000)

    val TextPrimary    = Color(0xFFFFF0F5)
    val TextSecondary  = Color(0xFFE8A7B5)
    val TextHint       = Color(0xFF7B5E6B)
    val TextPoetic     = Color(0xFFD4AF37)

    val GlassRose      = Color(0x20E8A7B5)
    val GlassBorder    = Color(0x40E8A7B5)
    val GlassGold      = Color(0x20D4AF37)

    // Gradients (as Brush)
    val GradMain       = Brush.verticalGradient(listOf(Color(0xFF0A0510), Color(0xFF1A0F28)))
    val GradCard       = Brush.verticalGradient(listOf(Color(0xFF1A0F28), Color(0xFF221436)))
    val GradButton     = Brush.horizontalGradient(listOf(Color(0xFFE0294A), Color(0xFF9B59B6)))
    val GradGold       = Brush.horizontalGradient(listOf(Color(0xFFD4AF37), Color(0xFFAA8C2C)))
    val GradRose       = Brush.horizontalGradient(listOf(Color(0xFFE8A7B5), Color(0xFFD4AF37)))

    // Design-feature palette (Arabic cultural: warm gold + henna + emerald)
    val DesignGold        = Color(0xFFC8860A)
    val DesignHenna       = Color(0xFF8B2F0F)
    val DesignEmerald     = Color(0xFF1B5E20)
    val DesignBgDark      = Color(0xFF1A1209)
    val DesignSurface     = Color(0xFF2C1F0F)
    val DesignTextLight   = Color(0xFFF5E6C8)
    val DesignHennaLight  = Color(0xFFD47A5C)

    // GradDesignHero was deleted in the 2026-05-13 audit cleanup. After
    // Phase 1's DesignSurface rewrite (transparent passthrough so the
    // morning/night ThemedBackground bleeds through), zero call sites
    // referenced it. If a future surface needs a deep-night gradient,
    // re-derive it inline from DesignBgDark + DesignSurface or build a
    // dedicated brush at the call site.
    val GradDesignAccent  = Brush.horizontalGradient(listOf(Color(0xFFC8860A), Color(0xFF8B2F0F)))
}

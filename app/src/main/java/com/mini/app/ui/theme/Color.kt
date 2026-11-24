package com.mini.app.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFF9B6DFF)
val PurpleGrey80 = Color(0xFFB0B0CE)
val Pink80 = Color(0xFFFFB0CD)

val Purple40 = Color(0xFF7C4DFF)
val PurpleGrey40 = Color(0xFF625B71)
val Pink40 = Color(0xFF7D5260)

// Minimal & Aesthetic Design Tokens - Dark Mode (Obsidian Carbon & Subtle Hairline Borders)
val GlassBackgroundDark = Color(0xFF090A0E)
val GlassSurfaceDark = Color(0xF511131A)
val GlassSurfaceCardDark = Color(0xF2151821)
val GlassSurfaceHighDark = Color(0xF71C202C)
val GlassSurfaceUltraDark = Color(0xFF242938)
val GlassBorderDark = Color(0x1AFFFFFF) // 10% white hairline rim
val GlassBorderHighlightDark = Color(0x33FFFFFF) // Subtle specular top edge

// Minimal & Aesthetic Design Tokens - Light Mode (Warm Architectural Paper White)
val GlassBackgroundLight = Color(0xFFF8F9FB)
val GlassSurfaceLight = Color(0xF5FFFFFF)
val GlassSurfaceCardLight = Color(0xFFFFFFFF)
val GlassSurfaceHighLight = Color(0xFFF1F3F7)
val GlassBorderLight = Color(0x0F000000) // 6% black crisp rim
val GlassBorderHighlightLight = Color(0x40FFFFFF)

// Dock Tokens
val DockGlassDark = Color(0xEE12141C)
val DockGlassLight = Color(0xF2FFFFFF)
val DockPillActiveDark = Color(0x2E6366F1)
val DockPillActiveLight = Color(0x186366F1)

// Aesthetic Minimal Accents
val MiniPrimary = Color(0xFF6366F1) // Refined Indigo Iris
val MiniPrimaryVariant = Color(0xFF4F46E5) // Deep Iris
val MiniAccentCyan = Color(0xFF0EA5E9)
val MiniAccentPink = Color(0xFFF43F5E)
val MiniAccentAmber = Color(0xFFF59E0B) // Golden Amber for Favorites
val MiniAccentEmerald = Color(0xFF10B981)
val MiniOnPrimary = Color(0xFFFFFFFF)
val MiniOnBackground = Color(0xFFF8FAFC)
val MiniSecondaryText = Color(0xFF94A3B8)
val MiniTertiaryText = Color(0xFF64748B)
val MiniDivider = Color(0x14FFFFFF)
val MiniError = Color(0xFFEF4444)
val MiniSuccess = Color(0xFF10B981)
val MiniCardBackground = Color(0xF2151821)
val MiniIconTint = Color(0xFF94A3B8)

// Minimal Subtle Brushes
val LiquidPrimaryGradient = Brush.horizontalGradient(
    listOf(Color(0xFF6366F1), Color(0xFF818CF8))
)

val LiquidVibrantGlow = Brush.radialGradient(
    colors = listOf(
        Color(0x156366F1),
        Color.Transparent
    )
)

val LiquidCardBorderGradient = Brush.verticalGradient(
    colors = listOf(
        Color.White.copy(alpha = 0.18f),
        Color.White.copy(alpha = 0.05f)
    )
)

val LiquidCardBorderLightGradient = Brush.verticalGradient(
    colors = listOf(
        Color.Black.copy(alpha = 0.08f),
        Color.Black.copy(alpha = 0.03f)
    )
)

// Inset Squircle Icon Gradients for Settings (Refined, Muted Elegance)
val GradientAppearance = Brush.linearGradient(
    listOf(Color(0xFF4F46E5), Color(0xFF6366F1))
)
val GradientTypography = Brush.linearGradient(
    listOf(Color(0xFF6366F1), Color(0xFF818CF8))
)
val GradientStorage = Brush.linearGradient(
    listOf(Color(0xFF0284C7), Color(0xFF38BDF8))
)
val GradientExport = Brush.linearGradient(
    listOf(Color(0xFF059669), Color(0xFF10B981))
)
val GradientAbout = Brush.linearGradient(
    listOf(Color(0xFFD97706), Color(0xFFF59E0B))
)
val GradientDynamic = Brush.linearGradient(
    listOf(Color(0xFFE11D48), Color(0xFFF43F5E))
)


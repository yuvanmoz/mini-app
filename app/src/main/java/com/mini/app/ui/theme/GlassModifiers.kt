package com.mini.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Liquid Glass visual system for Mini's Apple-inspired interface.
 * Implements multi-layer optical depth, specular rim lighting, internal top sheen,
 * chromatic ambient refraction, and reactive theme propagation.
 */

@Composable
fun isAppDarkTheme(): Boolean {
    return LocalIsDarkTheme.current
}

/**
 * Minimal & Aesthetic background atmosphere: clean, calm, distraction-free neutral tones.
 */
@Composable
fun Modifier.liquidAtmosphere(isDark: Boolean = LocalIsDarkTheme.current): Modifier {
    val bg = if (isDark) GlassBackgroundDark else GlassBackgroundLight
    return this.background(bg)
}

/**
 * Minimal & Aesthetic Card Surface:
 * Clean spatial depth shadow, refined subtle surface, and delicate hairline border.
 */
@Composable
fun Modifier.liquidGlassEffect(
    shape: Shape = RoundedCornerShape(20.dp),
    isDark: Boolean = LocalIsDarkTheme.current,
    elevation: Dp = 2.dp,
    borderAlpha: Float = if (isDark) 0.12f else 0.08f
): Modifier {
    val surfaceColor = if (isDark) GlassSurfaceCardDark else GlassSurfaceCardLight

    val borderGradient = if (isDark) {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = borderAlpha),
                Color.White.copy(alpha = 0.04f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color.Black.copy(alpha = borderAlpha),
                Color.Black.copy(alpha = 0.03f)
            )
        )
    }

    return this
        .shadow(
            elevation = elevation,
            shape = shape,
            ambientColor = if (isDark) Color.Black.copy(alpha = 0.40f) else Color.Black.copy(alpha = 0.04f),
            spotColor = if (isDark) Color.Black.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.05f)
        )
        .clip(shape)
        .background(surfaceColor)
        .border(
            width = 1.dp,
            brush = borderGradient,
            shape = shape
        )
}

/**
 * Liquid Glass Card container with frosted acrylic and specular top highlight.
 */
@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    isDark: Boolean = LocalIsDarkTheme.current,
    elevation: Dp = 4.dp,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )
    } else Modifier

    Column(
        modifier = modifier
            .liquidGlassEffect(shape = shape, isDark = isDark, elevation = elevation)
            .then(clickableModifier)
            .padding(contentPadding),
        content = content
    )
}

/**
 * Frosted Pill Badge with specular border (for tags, status badges, counters, and chips).
 */
@Composable
fun LiquidGlassPill(
    modifier: Modifier = Modifier,
    isDark: Boolean = LocalIsDarkTheme.current,
    pillColor: Color = MiniPrimary,
    content: @Composable RowScope.() -> Unit
) {
    val pillBg = if (isDark) pillColor.copy(alpha = 0.18f) else pillColor.copy(alpha = 0.12f)
    val pillBorder = if (isDark) pillColor.copy(alpha = 0.38f) else pillColor.copy(alpha = 0.28f)

    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(pillBg)
            .border(0.85.dp, pillBorder, CircleShape)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

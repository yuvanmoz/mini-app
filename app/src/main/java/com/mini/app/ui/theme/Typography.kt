package com.mini.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.mini.app.R

// Apple SF Pro Font Family
val AppFontFamily = FontFamily(
    Font(R.font.sf_pro_regular, FontWeight.Normal),
    Font(R.font.sf_pro_medium, FontWeight.Medium),
    Font(R.font.sf_pro_semibold, FontWeight.SemiBold),
    Font(R.font.sf_pro_bold, FontWeight.Bold)
)

fun resolveFontScale(fontSizeSetting: String): Float {
    return when (fontSizeSetting.lowercase()) {
        "small" -> 0.88f
        "medium", "default" -> 1.0f
        "large" -> 1.15f
        "xlarge", "extra large" -> 1.30f
        else -> {
            fontSizeSetting.toFloatOrNull()?.coerceIn(0.80f, 1.40f) ?: 1.0f
        }
    }
}

fun getScaledTypography(fontSizeSetting: String): Typography {
    val scale = resolveFontScale(fontSizeSetting)

    return Typography(
        // Apple Large Title: 34sp Bold
        displayLarge = TextStyle(
            fontFamily = AppFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (34 * scale).sp,
            lineHeight = (41 * scale).sp,
            letterSpacing = (-0.4).sp
        ),
        // Apple Title 1: 28sp Bold
        headlineLarge = TextStyle(
            fontFamily = AppFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (28 * scale).sp,
            lineHeight = (34 * scale).sp,
            letterSpacing = (-0.3).sp
        ),
        // Apple Title 2: 22sp Bold
        headlineMedium = TextStyle(
            fontFamily = AppFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (22 * scale).sp,
            lineHeight = (28 * scale).sp,
            letterSpacing = (-0.2).sp
        ),
        // Apple Title 3: 20sp SemiBold
        titleLarge = TextStyle(
            fontFamily = AppFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (20 * scale).sp,
            lineHeight = (25 * scale).sp,
            letterSpacing = 0.sp
        ),
        // Apple Headline: 17sp SemiBold
        titleMedium = TextStyle(
            fontFamily = AppFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (17 * scale).sp,
            lineHeight = (22 * scale).sp,
            letterSpacing = (-0.4).sp
        ),
        // Apple Subhead: 15sp Medium
        titleSmall = TextStyle(
            fontFamily = AppFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = (15 * scale).sp,
            lineHeight = (20 * scale).sp,
            letterSpacing = (-0.2).sp
        ),
        // Apple Body Large: 16sp Regular
        bodyLarge = TextStyle(
            fontFamily = AppFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (16 * scale).sp,
            lineHeight = (23 * scale).sp,
            letterSpacing = (-0.3).sp
        ),
        // Apple Body Medium: 15sp Regular
        bodyMedium = TextStyle(
            fontFamily = AppFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (15 * scale).sp,
            lineHeight = (21 * scale).sp,
            letterSpacing = (-0.2).sp
        ),
        // Apple Callout: 14sp Regular
        bodySmall = TextStyle(
            fontFamily = AppFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (13.5 * scale).sp,
            lineHeight = (18 * scale).sp,
            letterSpacing = (-0.1).sp
        ),
        // Apple Footnote: 13sp SemiBold
        labelLarge = TextStyle(
            fontFamily = AppFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (13 * scale).sp,
            lineHeight = (18 * scale).sp,
            letterSpacing = 0.sp
        ),
        // Apple Caption 1: 12sp Medium
        labelMedium = TextStyle(
            fontFamily = AppFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = (12 * scale).sp,
            lineHeight = (16 * scale).sp,
            letterSpacing = 0.sp
        ),
        // Apple Caption 2: 11sp Medium
        labelSmall = TextStyle(
            fontFamily = AppFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = (11 * scale).sp,
            lineHeight = (14 * scale).sp,
            letterSpacing = 0.1.sp
        )
    )
}

val MiniTypography = getScaledTypography("Medium")

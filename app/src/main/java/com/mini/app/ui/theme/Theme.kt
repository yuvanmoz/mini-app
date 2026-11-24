package com.mini.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = MiniPrimary,
    secondary = MiniPrimaryVariant,
    tertiary = Purple80,
    background = GlassBackgroundDark,
    surface = GlassSurfaceDark,
    surfaceVariant = GlassSurfaceHighDark,
    onPrimary = MiniOnPrimary,
    onBackground = MiniOnBackground,
    onSurface = MiniOnBackground,
    onSurfaceVariant = MiniSecondaryText,
    error = MiniError
)

private val LightColorScheme = lightColorScheme(
    primary = MiniPrimary,
    secondary = MiniPrimaryVariant,
    tertiary = Purple40,
    background = GlassBackgroundLight,
    surface = GlassSurfaceLight,
    surfaceVariant = GlassSurfaceHighLight,
    onPrimary = MiniOnPrimary,
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A),
    onSurfaceVariant = Color(0xFF64748B),
    error = MiniError
)

enum class AppTheme { LIGHT, DARK, SYSTEM }

val LocalTweetFontSizeScale = staticCompositionLocalOf { 1.0f }
val LocalIsDarkTheme = staticCompositionLocalOf { true }

@Composable
fun MiniTheme(
    appTheme: AppTheme = AppTheme.DARK,
    fontSizeSetting: String = "Medium",
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = when (appTheme) {
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
        AppTheme.SYSTEM -> isSystemInDarkTheme()
    }

    val scale = resolveFontScale(fontSizeSetting)

    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    val scaledTypography = getScaledTypography(fontSizeSetting)

    CompositionLocalProvider(
        LocalTweetFontSizeScale provides scale,
        LocalIsDarkTheme provides darkTheme
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = scaledTypography,
            content = content
        )
    }
}

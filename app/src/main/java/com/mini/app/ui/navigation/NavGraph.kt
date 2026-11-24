package com.mini.app.ui.navigation

import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.mini.app.ui.collections.CollectionsScreen
import com.mini.app.ui.feed.FeedScreen
import com.mini.app.ui.importpost.ImportScreen
import com.mini.app.ui.onboarding.OnboardingScreen
import com.mini.app.ui.search.SearchScreen
import com.mini.app.ui.settings.SettingsScreen
import com.mini.app.ui.splash.SplashScreen
import com.mini.app.ui.theme.*

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Onboarding : Screen("onboarding")
    object Feed : Screen("feed")
    object Search : Screen("search")
    object Collections : Screen("collections")
    object Settings : Screen("settings")
    object Import : Screen("import/{url}") {
        fun createRoute(url: String) = "import/${Uri.encode(url)}"
    }
}

@Composable
fun MainNavHost(
    navController: NavHostController,
    startDestination: String = Screen.Splash.route,
    isOnboardingCompleted: Boolean = false,
    onOnboardingComplete: () -> Unit = {},
    sharedUrl: String? = null,
    onSharedUrlConsumed: () -> Unit = {}
) {
    LaunchedEffect(sharedUrl) {
        if (sharedUrl != null) {
            navController.navigate(Screen.Import.createRoute(sharedUrl)) {
                launchSingleTop = true
            }
            onSharedUrlConsumed()
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                navController = navController,
                isOnboardingCompleted = isOnboardingCompleted
            )
        }
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                navController = navController,
                onComplete = onOnboardingComplete
            )
        }
        composable(
            route = "feed?tag={tag}",
            arguments = listOf(navArgument("tag") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            })
        ) { backStackEntry ->
            val filterTag = backStackEntry.arguments?.getString("tag")
            FeedScreen(navController = navController, filterTag = filterTag)
        }
        composable(Screen.Search.route) {
            SearchScreen(navController = navController)
        }
        composable(Screen.Collections.route) {
            CollectionsScreen(navController = navController)
        }
        composable(Screen.Settings.route) {
            SettingsScreen(navController = navController)
        }
        composable(
            route = Screen.Import.route,
            arguments = listOf(navArgument("url") { type = NavType.StringType })
        ) { backStackEntry ->
            val encodedUrl = backStackEntry.arguments?.getString("url") ?: ""
            val url = Uri.decode(encodedUrl)
            ImportScreen(url = url, navController = navController)
        }
    }
}

private data class DockTabItem(
    val route: String,
    val label: String,
    val outlinedIcon: ImageVector,
    val filledIcon: ImageVector
)

@Composable
fun MiniFloatingPillDock(
    navController: NavHostController,
    currentRoute: String?,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    val haptic = LocalHapticFeedback.current

    val items = remember {
        listOf(
            DockTabItem(Screen.Feed.route, "Feed", Icons.Outlined.Home, Icons.Filled.Home),
            DockTabItem(Screen.Collections.route, "Collections", Icons.Outlined.CollectionsBookmark, Icons.Filled.CollectionsBookmark),
            DockTabItem(Screen.Settings.route, "Settings", Icons.Outlined.Settings, Icons.Filled.Settings)
        )
    }

    val dockBackground = if (isDark) DockGlassDark else DockGlassLight
    val dockBorderBrush = if (isDark) {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.14f),
                Color.White.copy(alpha = 0.04f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color.Black.copy(alpha = 0.08f),
                Color.Black.copy(alpha = 0.03f)
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .shadow(
                    elevation = 8.dp,
                    shape = CircleShape,
                    ambientColor = if (isDark) Color.Black.copy(alpha = 0.4f) else Color.Black.copy(alpha = 0.06f),
                    spotColor = if (isDark) Color.Black.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.08f)
                )
                .clip(CircleShape)
                .background(dockBackground)
                .border(1.dp, dockBorderBrush, CircleShape)
                .padding(horizontal = 6.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items.forEach { item ->
                val selected = currentRoute == item.route ||
                    (item.route == Screen.Feed.route && currentRoute?.startsWith("feed") == true)

                val scale by animateFloatAsState(
                    targetValue = if (selected) 1.08f else 1.0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "dockScale"
                )

                val activeBgColor by animateColorAsState(
                    targetValue = if (selected) {
                        if (isDark) DockPillActiveDark else DockPillActiveLight
                    } else Color.Transparent,
                    animationSpec = spring(stiffness = Spring.StiffnessLow),
                    label = "dockActiveBg"
                )

                val contentColor by animateColorAsState(
                    targetValue = if (selected) MiniPrimary else MiniSecondaryText,
                    animationSpec = spring(stiffness = Spring.StiffnessLow),
                    label = "dockIconColor"
                )

                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(activeBgColor)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (!selected) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                navController.navigate(item.route) {
                                    popUpTo(Screen.Feed.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                        .padding(horizontal = if (selected) 18.dp else 16.dp, vertical = 10.dp)
                        .scale(scale),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (selected) item.filledIcon else item.outlinedIcon,
                        contentDescription = item.label,
                        tint = contentColor,
                        modifier = Modifier.size(22.dp)
                    )

                    if (selected) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = item.label,
                            color = contentColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = AppFontFamily
                        )
                    }
                }
            }
        }
    }
}


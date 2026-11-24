package com.mini.app.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.mini.app.R
import com.mini.app.ui.navigation.Screen
import com.mini.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(
    navController: NavHostController,
    isOnboardingCompleted: Boolean
) {
    val scale = remember { Animatable(0.4f) }
    val alpha = remember { Animatable(0f) }
    val currentOnboardingState by rememberUpdatedState(isOnboardingCompleted)
    val isDark = LocalIsDarkTheme.current

    LaunchedEffect(Unit) {
        launch {
            scale.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
            )
        }
        launch {
            alpha.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(durationMillis = 700)
            )
        }
        delay(1400)
        val targetRoute = if (currentOnboardingState) Screen.Feed.route else Screen.Onboarding.route
        navController.navigate(targetRoute) {
            popUpTo(Screen.Splash.route) { inclusive = true }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .liquidAtmosphere(isDark = isDark),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .scale(scale.value)
                .alpha(alpha.value)
        ) {
            Image(
                painter = painterResource(id = R.drawable.app_icon),
                contentDescription = "Mini App Logo",
                modifier = Modifier
                    .size(108.dp)
                    .shadow(elevation = 20.dp, shape = RoundedCornerShape(26.dp))
                    .clip(RoundedCornerShape(26.dp))
                    .border(
                        1.5.dp,
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = if (isDark) 0.4f else 0.8f),
                                Color.White.copy(alpha = 0.05f)
                            )
                        ),
                        RoundedCornerShape(26.dp)
                    )
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Mini",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 38.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = AppFontFamily,
                letterSpacing = (-0.8).sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Private Bookmark Library for X",
                color = MiniSecondaryText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = AppFontFamily
            )
        }
    }
}


package com.mini.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import com.mini.app.ui.navigation.MainNavHost
import com.mini.app.ui.navigation.MiniFloatingPillDock
import com.mini.app.ui.settings.SettingsViewModel
import com.mini.app.ui.theme.AppTheme
import com.mini.app.ui.theme.MiniTheme
import com.mini.app.ui.theme.liquidAtmosphere
import com.mini.app.util.XUrlParser
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var pendingSharedUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        pendingSharedUrl = extractSharedUrl(intent)

        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val themeMode by settingsViewModel.themeMode.collectAsStateWithLifecycle()
            val dynamicColor by settingsViewModel.dynamicColor.collectAsStateWithLifecycle()
            val fontSize by settingsViewModel.fontSize.collectAsStateWithLifecycle()
            val isOnboardingCompleted by settingsViewModel.isOnboardingCompleted.collectAsStateWithLifecycle()

            val navController = rememberNavController()
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            // Hide bottom dock on Splash, Onboarding, and Import screens
            val showBottomDock = currentRoute != null &&
                currentRoute != "splash" &&
                currentRoute != "onboarding" &&
                !currentRoute.startsWith("import/")

            var sharedUrlToProcess by remember { mutableStateOf(pendingSharedUrl) }

            MiniTheme(
                appTheme = themeMode,
                fontSizeSetting = fontSize,
                dynamicColor = dynamicColor
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .liquidAtmosphere()
                ) {
                    MainNavHost(
                        navController = navController,
                        isOnboardingCompleted = isOnboardingCompleted,
                        onOnboardingComplete = {
                            settingsViewModel.setOnboardingCompleted(true)
                        },
                        sharedUrl = sharedUrlToProcess,
                        onSharedUrlConsumed = { sharedUrlToProcess = null }
                    )

                    if (showBottomDock) {
                        MiniFloatingPillDock(
                            navController = navController,
                            currentRoute = currentRoute,
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingSharedUrl = extractSharedUrl(intent)
        recreate()
    }

    private fun extractSharedUrl(intent: Intent?): String? {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return null
            return XUrlParser.extractFromText(sharedText) ?: run {
                if (sharedText.trim().startsWith("http")) sharedText.trim() else null
            }
        }
        return null
    }
}

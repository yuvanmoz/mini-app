package com.mini.app

import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mini.app.ui.importpost.ImportScreen
import com.mini.app.ui.settings.SettingsViewModel
import com.mini.app.ui.theme.MiniTheme
import com.mini.app.util.XUrlParser
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ShareActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Ensure window background is transparent so Twitter stays visible underneath
        window.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))

        val sharedUrl = extractSharedUrl(intent)

        if (sharedUrl.isNullOrBlank()) {
            Toast.makeText(this, "No valid link found in shared content", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val themeMode by settingsViewModel.themeMode.collectAsStateWithLifecycle()
            val dynamicColor by settingsViewModel.dynamicColor.collectAsStateWithLifecycle()
            val fontSize by settingsViewModel.fontSize.collectAsStateWithLifecycle()

            MiniTheme(
                appTheme = themeMode,
                fontSizeSetting = fontSize,
                dynamicColor = dynamicColor
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Transparent
                ) {
                    ImportScreen(
                        url = sharedUrl,
                        onDismiss = { finish() },
                        onViewInMini = {
                            val mainIntent = Intent(this@ShareActivity, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            }
                            startActivity(mainIntent)
                            finish()
                        }
                    )
                }
            }
        }
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

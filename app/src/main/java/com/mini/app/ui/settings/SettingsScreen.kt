package com.mini.app.ui.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.mini.app.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    navController: NavHostController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val dynamicColor by viewModel.dynamicColor.collectAsStateWithLifecycle()
    val fontSizeSetting by viewModel.fontSize.collectAsStateWithLifecycle()
    val storageBreakdown by viewModel.storageBreakdown.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val isDark = LocalIsDarkTheme.current

    var showAboutDialog by remember { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    var isExportingWord by remember { mutableStateOf(false) }
    var isExportingMd by remember { mutableStateOf(false) }
    var isExportingJson by remember { mutableStateOf(false) }

    val backupPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val json = inputStream?.bufferedReader()?.use { it.readText() }
                    if (!json.isNullOrBlank()) {
                        val result = viewModel.importBackupJson(json)
                        result.fold(
                            onSuccess = { count ->
                                Toast.makeText(context, "Restored $count posts successfully!", Toast.LENGTH_LONG).show()
                                viewModel.refreshStorageUsage()
                            },
                            onFailure = { ex ->
                                Toast.makeText(context, ex.message ?: "Failed to restore backup", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Error reading backup file", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val isSystemTheme = themeMode == AppTheme.SYSTEM
    val isDarkThemeActive = when (themeMode) {
        AppTheme.DARK -> true
        AppTheme.LIGHT -> false
        AppTheme.SYSTEM -> isDark
    }

    val currentScale = remember(fontSizeSetting) {
        resolveFontScale(fontSizeSetting)
    }

    var sliderValue by remember(currentScale) {
        mutableFloatStateOf(
            when {
                currentScale <= 0.90f -> 0f
                currentScale <= 1.05f -> 1f
                currentScale <= 1.20f -> 2f
                else -> 3f
            }
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 120.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 22.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Settings",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = AppFontFamily,
                    letterSpacing = (-0.6).sp
                )
                Text(
                    text = "Appearance, typography & local storage",
                    color = MiniSecondaryText,
                    fontSize = 14.sp,
                    fontFamily = AppFontFamily
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            SettingsSectionHeader(title = "APPEARANCE")

            LiquidGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(22.dp),
                isDark = isDark,
                elevation = 3.dp,
                contentPadding = PaddingValues(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SettingsSquircleIcon(
                        icon = if (isDarkThemeActive) Icons.Outlined.DarkMode else Icons.Outlined.LightMode,
                        gradient = GradientAppearance,
                        iconTint = Color.White
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Dark Appearance",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = AppFontFamily
                        )
                        Text(
                            text = if (isDarkThemeActive) "Luminescent obsidian glass" else "Frosted crystalline glass",
                            color = MiniSecondaryText,
                            fontSize = 12.5.sp,
                            fontFamily = AppFontFamily
                        )
                    }
                    Switch(
                        checked = isDarkThemeActive,
                        onCheckedChange = { checked ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.toggleDarkMode(checked)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = MiniPrimary,
                            uncheckedThumbColor = if (isDark) Color(0xFFCBD5E1) else Color.White,
                            uncheckedTrackColor = if (isDark) Color(0x38FFFFFF) else Color(0x28000000)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                GlassDivider(isDark = isDark)
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SettingsSquircleIcon(
                        icon = Icons.Outlined.Palette,
                        gradient = Brush.linearGradient(listOf(Color(0xFF8B5CF6), Color(0xFF38BDF8))),
                        iconTint = Color.White
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Match System Theme",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = AppFontFamily
                        )
                        Text(
                            text = "Sync automatically with device day/night schedule",
                            color = MiniSecondaryText,
                            fontSize = 12.5.sp,
                            fontFamily = AppFontFamily
                        )
                    }
                    Switch(
                        checked = isSystemTheme,
                        onCheckedChange = { checked ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.setMatchSystemTheme(checked)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = MiniPrimaryVariant,
                            uncheckedThumbColor = if (isDark) Color(0xFFCBD5E1) else Color.White,
                            uncheckedTrackColor = if (isDark) Color(0x38FFFFFF) else Color(0x28000000)
                        )
                    )
                }

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    Spacer(modifier = Modifier.height(14.dp))
                    GlassDivider(isDark = isDark)
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SettingsSquircleIcon(
                            icon = Icons.Outlined.Widgets,
                            gradient = GradientDynamic,
                            iconTint = Color.White
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Dynamic Palette",
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = AppFontFamily
                            )
                            Text(
                                text = "Extract theme tints from Android wallpaper",
                                color = MiniSecondaryText,
                                fontSize = 12.5.sp,
                                fontFamily = AppFontFamily
                            )
                        }
                        Switch(
                            checked = dynamicColor,
                            onCheckedChange = { checked ->
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.setDynamicColor(checked)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = MiniAccentPink,
                                uncheckedThumbColor = if (isDark) Color(0xFFCBD5E1) else Color.White,
                                uncheckedTrackColor = if (isDark) Color(0x38FFFFFF) else Color(0x28000000)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            SettingsSectionHeader(title = "TYPOGRAPHY & READING SIZE")

            LiquidGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(22.dp),
                isDark = isDark,
                elevation = 3.dp,
                contentPadding = PaddingValues(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SettingsSquircleIcon(
                            icon = Icons.Outlined.FormatSize,
                            gradient = GradientTypography,
                            iconTint = Color.White
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Reading Font Size",
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = AppFontFamily
                            )
                            Text(
                                text = "Scales saved post text & card layouts",
                                color = MiniSecondaryText,
                                fontSize = 12.5.sp,
                                fontFamily = AppFontFamily
                            )
                        }
                    }

                    val scaleLabel = when (sliderValue.roundToInt()) {
                        0 -> "Small (88%)"
                        1 -> "Default (100%)"
                        2 -> "Large (115%)"
                        else -> "Extra Large (130%)"
                    }

                    LiquidGlassPill(
                        isDark = isDark,
                        pillColor = MiniPrimary
                    ) {
                        Text(
                            text = scaleLabel,
                            color = MiniPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = AppFontFamily
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "A",
                        color = MiniSecondaryText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = AppFontFamily
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Slider(
                        value = sliderValue,
                        onValueChange = { newVal ->
                            val rounded = (newVal * 1.0f).roundToInt().toFloat().coerceIn(0f, 3f)
                            if (rounded != sliderValue) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                sliderValue = rounded
                                val targetScale = when (rounded.toInt()) {
                                    0 -> 0.88f
                                    1 -> 1.00f
                                    2 -> 1.15f
                                    else -> 1.30f
                                }
                                viewModel.setFontScale(targetScale)
                            }
                        },
                        valueRange = 0f..3f,
                        steps = 2,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = MiniPrimary,
                            inactiveTrackColor = if (isDark) Color(0x33FFFFFF) else Color(0x1F000000)
                        )
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "A",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = AppFontFamily
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                GlassDivider(isDark = isDark)
                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "LIVE PREVIEW",
                    color = MiniSecondaryText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = AppFontFamily,
                    letterSpacing = 0.6.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                val previewScale = when (sliderValue.roundToInt()) {
                    0 -> 0.88f
                    1 -> 1.00f
                    2 -> 1.15f
                    else -> 1.30f
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isDark) Color(0x38121524) else Color(0x18000000))
                        .border(
                            1.dp,
                            if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.05f),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(14.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(GradientAppearance),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "M",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = AppFontFamily
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Mini Reader",
                                        color = MaterialTheme.colorScheme.onBackground,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = AppFontFamily
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Outlined.Verified,
                                        contentDescription = null,
                                        tint = MiniPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Text(
                                    text = "@miniapp · just now",
                                    color = MiniSecondaryText,
                                    fontSize = 11.5.sp,
                                    fontFamily = AppFontFamily
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Enjoy crystal-clear, distraction-free reading with Apple Liquid Glass aesthetics. All saved posts remain 100% private and offline on your device.",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = (15 * previewScale).sp,
                            lineHeight = (22 * previewScale).sp,
                            fontFamily = AppFontFamily
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            SettingsSectionHeader(title = "DATA & STORAGE")

            LiquidGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(22.dp),
                isDark = isDark,
                elevation = 3.dp,
                contentPadding = PaddingValues(18.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showClearCacheDialog = true }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SettingsSquircleIcon(
                        icon = Icons.Outlined.Folder,
                        gradient = GradientStorage,
                        iconTint = Color.White
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Storage & Cache",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = AppFontFamily
                        )
                        Text(
                            text = "${formatBytes(storageBreakdown.savedMediaBytes)} saved · ${formatBytes(storageBreakdown.cacheBytes)} cache",
                            color = MiniSecondaryText,
                            fontSize = 12.5.sp,
                            fontFamily = AppFontFamily
                        )
                    }
                    Surface(
                        onClick = { showClearCacheDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        color = MiniPrimary.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, MiniPrimary.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "Clear",
                            color = MiniPrimary,
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                GlassDivider(isDark = isDark)
                Spacer(modifier = Modifier.height(14.dp))

                // Export to Markdown (.md)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isExportingMd) {
                            isExportingMd = true
                            scope.launch {
                                val result = viewModel.exportToMarkdown()
                                isExportingMd = false
                                result.fold(
                                    onSuccess = { file ->
                                        Toast.makeText(
                                            context,
                                            "Exported Markdown: ${file.name}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    },
                                    onFailure = { ex ->
                                        Toast.makeText(
                                            context,
                                            ex.message ?: "Export failed",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                )
                            }
                        }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SettingsSquircleIcon(
                        icon = Icons.Outlined.Description,
                        gradient = Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFF818CF8))),
                        iconTint = Color.White
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Export to Markdown (.md)",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = AppFontFamily
                        )
                        Text(
                            text = "Clean markdown notes for Obsidian, Notion & Bear",
                            color = MiniSecondaryText,
                            fontSize = 12.5.sp,
                            fontFamily = AppFontFamily
                        )
                    }
                    if (isExportingMd) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = MiniPrimary,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        LiquidGlassPill(isDark = isDark, pillColor = MiniPrimary) {
                            Text(
                                text = ".MD",
                                color = MiniPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = AppFontFamily
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                GlassDivider(isDark = isDark)
                Spacer(modifier = Modifier.height(14.dp))

                // Export to Word (.docx)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isExportingWord) {
                            isExportingWord = true
                            scope.launch {
                                val result = viewModel.exportToWord(context)
                                isExportingWord = false
                                result.fold(
                                    onSuccess = { file ->
                                        Toast.makeText(
                                            context,
                                            "Exported to Downloads: ${file.name}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    },
                                    onFailure = { ex ->
                                        Toast.makeText(
                                            context,
                                            ex.message ?: "Export failed",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                )
                            }
                        }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SettingsSquircleIcon(
                        icon = Icons.Outlined.Download,
                        gradient = GradientExport,
                        iconTint = Color.White
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Export to Word (.docx)",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = AppFontFamily
                        )
                        Text(
                            text = "Export saved library & photos into a document",
                            color = MiniSecondaryText,
                            fontSize = 12.5.sp,
                            fontFamily = AppFontFamily
                        )
                    }
                    if (isExportingWord) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = MiniPrimary,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        LiquidGlassPill(isDark = isDark, pillColor = MiniAccentEmerald) {
                            Text(
                                text = ".DOCX",
                                color = MiniAccentEmerald,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = AppFontFamily
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                GlassDivider(isDark = isDark)
                Spacer(modifier = Modifier.height(14.dp))

                // Export JSON Backup
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isExportingJson) {
                            isExportingJson = true
                            scope.launch {
                                val result = viewModel.exportBackupJson()
                                isExportingJson = false
                                result.fold(
                                    onSuccess = { file ->
                                        Toast.makeText(
                                            context,
                                            "Backup saved to Downloads: ${file.name}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    },
                                    onFailure = { ex ->
                                        Toast.makeText(
                                            context,
                                            ex.message ?: "Backup export failed",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                )
                            }
                        }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SettingsSquircleIcon(
                        icon = Icons.Outlined.CloudDownload,
                        gradient = GradientStorage,
                        iconTint = Color.White
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Export Backup (.json)",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = AppFontFamily
                        )
                        Text(
                            text = "Save full library data file for migration or safekeeping",
                            color = MiniSecondaryText,
                            fontSize = 12.5.sp,
                            fontFamily = AppFontFamily
                        )
                    }
                    if (isExportingJson) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = MiniPrimary,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        LiquidGlassPill(isDark = isDark, pillColor = MiniAccentCyan) {
                            Text(
                                text = ".JSON",
                                color = MiniAccentCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = AppFontFamily
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                GlassDivider(isDark = isDark)
                Spacer(modifier = Modifier.height(14.dp))

                // Restore from Backup JSON
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            try {
                                backupPicker.launch(arrayOf("application/json", "*/*"))
                            } catch (e: Exception) {
                                Toast.makeText(context, "File picker unavailable", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SettingsSquircleIcon(
                        icon = Icons.Outlined.CloudUpload,
                        gradient = GradientAbout,
                        iconTint = Color.White
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Restore from Backup",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = AppFontFamily
                        )
                        Text(
                            text = "Select a mini_backup.json to restore posts & collections",
                            color = MiniSecondaryText,
                            fontSize = 12.5.sp,
                            fontFamily = AppFontFamily
                        )
                    }
                    Surface(
                        onClick = {
                            try {
                                backupPicker.launch(arrayOf("application/json", "*/*"))
                            } catch (e: Exception) {
                                Toast.makeText(context, "File picker unavailable", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = MiniPrimary.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, MiniPrimary.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "Restore",
                            color = MiniPrimary,
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            SettingsSectionHeader(title = "ABOUT")

            LiquidGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(22.dp),
                isDark = isDark,
                elevation = 3.dp,
                contentPadding = PaddingValues(18.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAboutDialog = true }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SettingsSquircleIcon(
                        icon = Icons.Outlined.Info,
                        gradient = GradientAbout,
                        iconTint = Color.White
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "About Mini",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = AppFontFamily
                        )
                        Text(
                            text = "Version 1.0.0 · Private Offline Library",
                            color = MiniSecondaryText,
                            fontSize = 12.5.sp,
                            fontFamily = AppFontFamily
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                        contentDescription = null,
                        tint = MiniSecondaryText,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }

    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = {
                Text(
                    "Clear Image Cache",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontFamily = AppFontFamily,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "This will clear ${formatBytes(storageBreakdown.cacheBytes)} of temporary image cache and preview files.\n\nYour saved offline posts, full-resolution photos, and reading library will NOT be deleted.",
                    color = MiniSecondaryText,
                    fontSize = 14.sp,
                    fontFamily = AppFontFamily
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val bytesToFree = storageBreakdown.cacheBytes
                    viewModel.clearImageCache { freedBytes ->
                        val freed = if (freedBytes > 0) freedBytes else bytesToFree
                        android.widget.Toast.makeText(
                            context,
                            if (freed > 0) "Cleared ${formatBytes(freed)} of cache" else "Cache is already clean",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                    showClearCacheDialog = false
                }) {
                    Text("Clear", color = MiniPrimary, fontFamily = AppFontFamily, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onBackground, fontFamily = AppFontFamily)
                }
            },
            containerColor = if (isDark) GlassSurfaceHighDark else GlassSurfaceHighLight,
            shape = RoundedCornerShape(22.dp)
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = {
                Text(
                    "About Mini",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontFamily = AppFontFamily,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Mini v1.0.0",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        fontFamily = AppFontFamily
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "A private, local-first reading library for posts intentionally saved from X (Twitter). All saved content is stored 100% locally on your device with high-fidelity Apple Liquid Glass aesthetics.",
                        color = MiniSecondaryText,
                        fontSize = 14.sp,
                        fontFamily = AppFontFamily
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("OK", color = MiniPrimary, fontFamily = AppFontFamily, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = if (isDark) GlassSurfaceHighDark else GlassSurfaceHighLight,
            shape = RoundedCornerShape(22.dp)
        )
    }
}

@Composable
fun SettingsSquircleIcon(
    icon: ImageVector,
    gradient: Brush,
    iconTint: Color = Color.White
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .shadow(4.dp, RoundedCornerShape(11.dp), spotColor = MiniPrimary.copy(alpha = 0.25f))
            .clip(RoundedCornerShape(11.dp))
            .background(gradient)
            .border(0.75.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(11.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun GlassDivider(isDark: Boolean) {
    HorizontalDivider(
        color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f),
        thickness = 1.dp
    )
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        color = MiniSecondaryText,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = AppFontFamily,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(horizontal = 22.dp, vertical = 6.dp)
    )
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes <= 0 -> "0 B"
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> String.format(java.util.Locale.getDefault(), "%.1f MB", bytes.toDouble() / (1024 * 1024))
    }
}

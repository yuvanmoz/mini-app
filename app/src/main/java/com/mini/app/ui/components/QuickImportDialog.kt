package com.mini.app.ui.components

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.mini.app.data.repository.ImportStep
import com.mini.app.ui.importpost.ImportUiState
import com.mini.app.ui.theme.*
import com.mini.app.util.XUrlParser

@Composable
fun QuickImportDialog(
    importState: ImportUiState,
    onImportUrl: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var inputUrl by remember { mutableStateOf("") }
    val isDark = LocalIsDarkTheme.current

    val clipboardUrl = remember {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val text = clipboard?.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
        XUrlParser.extractFromText(text) ?: if (text.trim().contains("twitter.com") || text.trim().contains("x.com")) text.trim() else null
    }

    LaunchedEffect(clipboardUrl) {
        if (!clipboardUrl.isNullOrBlank() && inputUrl.isBlank()) {
            inputUrl = clipboardUrl
        }
    }

    Dialog(onDismissRequest = {
        if (importState !is ImportUiState.Loading) {
            onDismiss()
        }
    }) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(if (isDark) GlassSurfaceCardDark else GlassSurfaceCardLight)
                .border(
                    1.dp,
                    if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f),
                    RoundedCornerShape(22.dp)
                )
                .padding(22.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MiniPrimary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Link,
                        contentDescription = null,
                        tint = MiniPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Import Post",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = AppFontFamily
                    )
                    Text(
                        text = "Paste any X or Twitter post link",
                        color = MiniSecondaryText,
                        fontSize = 12.sp,
                        fontFamily = AppFontFamily
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (importState) {
                is ImportUiState.Idle -> {
                    OutlinedTextField(
                        value = inputUrl,
                        onValueChange = { inputUrl = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                text = "https://x.com/username/status/...",
                                color = MiniSecondaryText.copy(alpha = 0.6f),
                                fontSize = 13.5.sp,
                                fontFamily = AppFontFamily
                            )
                        },
                        trailingIcon = {
                            if (!clipboardUrl.isNullOrBlank() && inputUrl != clipboardUrl) {
                                IconButton(onClick = { inputUrl = clipboardUrl }) {
                                    Icon(
                                        imageVector = Icons.Outlined.ContentPaste,
                                        contentDescription = "Paste",
                                        tint = MiniPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MiniPrimary,
                            unfocusedBorderColor = if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.12f),
                            focusedContainerColor = if (isDark) Color(0x33000000) else Color(0x0A000000),
                            unfocusedContainerColor = if (isDark) Color(0x22000000) else Color(0x05000000),
                            focusedTextColor = MaterialTheme.colorScheme.onBackground,
                            unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                        ),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(
                                text = "Cancel",
                                color = MiniSecondaryText,
                                fontFamily = AppFontFamily,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val trimmed = inputUrl.trim()
                                if (trimmed.isNotEmpty()) {
                                    onImportUrl(trimmed)
                                }
                            },
                            enabled = inputUrl.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MiniPrimary,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "Import",
                                fontFamily = AppFontFamily,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                is ImportUiState.Loading -> {
                    val statusText = when (importState.currentStep) {
                        ImportStep.RECEIVING -> "Connecting to link…"
                        ImportStep.FETCHING -> "Fetching post metadata…"
                        ImportStep.EXTRACTING -> "Extracting content…"
                        ImportStep.DOWNLOADING_MEDIA -> "Saving high-res media…"
                        ImportStep.SAVING -> "Saving to offline database…"
                        ImportStep.COMPLETE -> "Complete!"
                        ImportStep.ERROR -> "Error"
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = MiniPrimary,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = statusText,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = AppFontFamily
                        )
                    }
                }

                is ImportUiState.Success -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = MiniSuccess,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Saved to Library!",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = AppFontFamily
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = MiniPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Done", fontFamily = AppFontFamily, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                is ImportUiState.Duplicate -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = MiniPrimary,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Already in your library!",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = AppFontFamily
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = MiniPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("View", fontFamily = AppFontFamily, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                is ImportUiState.VideoUnsupported -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ErrorOutline,
                            contentDescription = null,
                            tint = MiniAccentAmber,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Video posts are not supported yet",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 14.sp,
                            fontFamily = AppFontFamily
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = MiniPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("OK", fontFamily = AppFontFamily)
                        }
                    }
                }

                is ImportUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ErrorOutline,
                            contentDescription = null,
                            tint = MiniError,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = importState.message,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 13.5.sp,
                            fontFamily = AppFontFamily
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = MiniPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Close", fontFamily = AppFontFamily)
                        }
                    }
                }
            }
        }
    }
}

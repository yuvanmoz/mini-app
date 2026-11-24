package com.mini.app.ui.importpost

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material.icons.outlined.VideocamOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.mini.app.data.repository.ImportStep
import com.mini.app.domain.model.Post
import com.mini.app.domain.model.Tag
import com.mini.app.ui.feed.AuthorAvatar
import com.mini.app.ui.navigation.Screen
import com.mini.app.ui.theme.*

@Composable
fun ImportScreen(
    url: String,
    navController: NavHostController,
    viewModel: ImportViewModel = hiltViewModel()
) {
    ImportScreen(
        url = url,
        onDismiss = { navController.popBackStack() },
        onViewInMini = {
            navController.navigate(Screen.Feed.route) {
                popUpTo(Screen.Feed.route) { inclusive = true }
            }
        },
        viewModel = viewModel
    )
}

@Composable
fun ImportScreen(
    url: String,
    onDismiss: () -> Unit,
    onViewInMini: () -> Unit,
    viewModel: ImportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val allTags by viewModel.allTags.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val isDark = LocalIsDarkTheme.current

    LaunchedEffect(Unit) {
        if (uiState is ImportUiState.Idle) {
            viewModel.startImport(url)
        }
    }

    // Scrim overlay container
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onDismiss() }
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Floating Liquid Glass Bottom Sheet Surface
        val sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        val sheetBorderBrush = if (isDark) {
            Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = 0.35f),
                    Color.White.copy(alpha = 0.06f)
                )
            )
        } else {
            Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = 0.95f),
                    Color.Black.copy(alpha = 0.08f)
                )
            )
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .shadow(elevation = 20.dp, shape = sheetShape)
                .clip(sheetShape)
                .border(width = 1.dp, brush = sheetBorderBrush, shape = sheetShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { /* Prevent click through to scrim */ }
                ),
            shape = sheetShape,
            color = if (isDark) GlassSurfaceUltraDark else GlassSurfaceHighLight
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                // Top drag handle pill
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 14.dp)
                        .width(44.dp)
                        .height(4.dp)
                        .background(if (isDark) Color.White.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.2f), CircleShape)
                )

                // Header Bar (Status Title)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MiniPrimary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.Bookmark,
                                contentDescription = null,
                                tint = MiniPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = when (uiState) {
                            is ImportUiState.Loading -> "Saving to Mini"
                            is ImportUiState.Success -> "Saved to Mini"
                            is ImportUiState.Duplicate -> "Already in Library"
                            is ImportUiState.VideoUnsupported -> "Video Post"
                            is ImportUiState.Error -> "Save Error"
                            else -> "Save Post"
                        },
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = AppFontFamily
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(
                    color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f),
                    thickness = 1.dp
                )
                Spacer(modifier = Modifier.height(14.dp))

                // Animated State Body
                AnimatedContent(
                    targetState = uiState,
                    transitionSpec = {
                        (fadeIn(tween(250)) + slideInVertically { it / 3 })
                            .togetherWith(fadeOut(tween(150)) + slideOutVertically { -it / 3 })
                    },
                    label = "import_content"
                ) { state ->
                    when (state) {
                        is ImportUiState.Loading -> PremiumLoadingView(state.currentStep, state.url)
                        is ImportUiState.Success -> PremiumSuccessView(
                            post = state.post,
                            allTags = allTags,
                            onUpdateTags = { tagNames ->
                                viewModel.updatePostTags(state.post.tweetId, tagNames)
                            },
                            onViewInMini = onViewInMini,
                            onDone = onDismiss
                        )
                        is ImportUiState.Duplicate -> PremiumDuplicateView(
                            onViewPost = onViewInMini,
                            onDone = onDismiss
                        )
                        is ImportUiState.VideoUnsupported -> PremiumVideoUnsupportedView(
                            url = state.url,
                            onOpenOnX = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(state.url))
                                context.startActivity(intent)
                                onDismiss()
                            },
                            onDone = onDismiss
                        )
                        is ImportUiState.Error -> PremiumErrorView(
                            message = state.message,
                            onRetry = { viewModel.startImport(state.url) },
                            onCancel = onDismiss
                        )
                        else -> {
                            Box(modifier = Modifier.height(100.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PremiumLoadingView(currentStep: ImportStep, url: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val isDark = LocalIsDarkTheme.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Surface(
                color = MiniPrimary.copy(alpha = 0.12f),
                shape = CircleShape,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        color = MiniPrimary,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier
                            .size(24.dp)
                            .scale(scale)
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = getCompactStepLabel(currentStep),
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = AppFontFamily
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Extracting offline media & metadata\u2026",
                    color = MiniSecondaryText,
                    fontSize = 12.sp,
                    fontFamily = AppFontFamily
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // URL snippet pill
        Surface(
            color = if (isDark) Color(0x26FFFFFF) else Color(0x0D000000),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
        ) {
            Text(
                text = url,
                color = MiniSecondaryText,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))
    }
}

@Composable
private fun PremiumSuccessView(
    post: Post,
    allTags: List<Tag>,
    onUpdateTags: (List<String>) -> Unit,
    onViewInMini: () -> Unit,
    onDone: () -> Unit
) {
    val selectedTags = remember { mutableStateListOf<String>().apply { addAll(post.tags.map { it.name }) } }
    val isDark = LocalIsDarkTheme.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
    ) {
        // Author & Post Preview Card
        Surface(
            color = if (isDark) Color(0x2EFFFFFF) else Color(0x0A000000),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f), RoundedCornerShape(18.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AuthorAvatar(
                        avatarUrl = post.author.avatarUrl ?: post.author.avatarRemoteUrl,
                        displayName = post.author.displayName,
                        modifier = Modifier.size(38.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = post.author.displayName,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = AppFontFamily,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "@${post.author.handle}",
                            color = MiniSecondaryText,
                            fontSize = 12.sp,
                            fontFamily = AppFontFamily,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (post.text.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = post.text,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        fontFamily = AppFontFamily,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Quick Collection Tag Picker
        if (allTags.isNotEmpty()) {
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Tag,
                    contentDescription = null,
                    tint = MiniSecondaryText,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Add to collection:",
                    color = MiniSecondaryText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = AppFontFamily
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(allTags, key = { it.name }) { tag ->
                    val isSelected = selectedTags.contains(tag.name)
                    LiquidGlassPill(
                        modifier = Modifier.clickable {
                            if (isSelected) {
                                selectedTags.remove(tag.name)
                            } else {
                                selectedTags.add(tag.name)
                            }
                            onUpdateTags(selectedTags.toList())
                        },
                        isDark = isDark
                    ) {
                        Text(
                            text = "#${tag.name}",
                            color = if (isSelected) Color.White else MiniPrimary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontFamily = AppFontFamily
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Action Buttons Row (Premium Pills)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onViewInMini,
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
            ) {
                Text("View in Mini", color = MiniPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = AppFontFamily)
            }
            Button(
                onClick = onDone,
                shape = RoundedCornerShape(22.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MiniPrimary),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
            ) {
                Text("Done", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = AppFontFamily)
            }
        }
    }
}

@Composable
private fun PremiumDuplicateView(
    onViewPost: () -> Unit,
    onDone: () -> Unit
) {
    val isDark = LocalIsDarkTheme.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
    ) {
        Surface(
            color = if (isDark) Color(0x2EFFFFFF) else Color(0x0A000000),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f), RoundedCornerShape(18.dp))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(16.dp)
            ) {
                Surface(
                    color = Color(0xFFFFA000).copy(alpha = 0.15f),
                    shape = CircleShape,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Outlined.Info,
                            contentDescription = null,
                            tint = Color(0xFFFFA000),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Already in Library",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = AppFontFamily
                    )
                    Text(
                        text = "This post has already been saved offline.",
                        color = MiniSecondaryText,
                        fontSize = 12.sp,
                        fontFamily = AppFontFamily
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onViewPost,
                shape = RoundedCornerShape(22.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MiniPrimary),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
            ) {
                Text("View Saved Post", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = AppFontFamily)
            }
            OutlinedButton(
                onClick = onDone,
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
            ) {
                Text("Close", color = MiniPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = AppFontFamily)
            }
        }
    }
}

@Composable
private fun PremiumVideoUnsupportedView(
    url: String,
    onOpenOnX: () -> Unit,
    onDone: () -> Unit
) {
    val isDark = LocalIsDarkTheme.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
    ) {
        Surface(
            color = if (isDark) Color(0x2EFFFFFF) else Color(0x0A000000),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f), RoundedCornerShape(18.dp))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(16.dp)
            ) {
                Surface(
                    color = MiniPrimary.copy(alpha = 0.15f),
                    shape = CircleShape,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Outlined.VideocamOff,
                            contentDescription = null,
                            tint = MiniPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Video Posts Unsupported",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = AppFontFamily
                    )
                    Text(
                        text = "Video saving isn't supported in this version.",
                        color = MiniSecondaryText,
                        fontSize = 12.sp,
                        fontFamily = AppFontFamily
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onOpenOnX,
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
            ) {
                Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null, tint = MiniPrimary, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Open on X", color = MiniPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = AppFontFamily)
            }
            Button(
                onClick = onDone,
                shape = RoundedCornerShape(22.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MiniPrimary),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
            ) {
                Text("Close", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = AppFontFamily)
            }
        }
    }
}

@Composable
private fun PremiumErrorView(
    message: String,
    onRetry: () -> Unit,
    onCancel: () -> Unit
) {
    val isDark = LocalIsDarkTheme.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
    ) {
        Surface(
            color = if (isDark) Color(0x2EFFFFFF) else Color(0x0A000000),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f), RoundedCornerShape(18.dp))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(16.dp)
            ) {
                Surface(
                    color = MiniError.copy(alpha = 0.15f),
                    shape = CircleShape,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Outlined.ErrorOutline,
                            contentDescription = null,
                            tint = MiniError,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Unable to Save Post",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = AppFontFamily
                    )
                    Text(
                        text = message,
                        color = MiniSecondaryText,
                        fontSize = 12.sp,
                        fontFamily = AppFontFamily,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(22.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MiniPrimary),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
            ) {
                Text("Retry", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = AppFontFamily)
            }
            OutlinedButton(
                onClick = onCancel,
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
            ) {
                Text("Cancel", color = MiniPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = AppFontFamily)
            }
        }
    }
}

private fun getCompactStepLabel(step: ImportStep): String = when (step) {
    ImportStep.RECEIVING -> "Receiving post link…"
    ImportStep.FETCHING -> "Fetching post from X…"
    ImportStep.EXTRACTING -> "Extracting text & author…"
    ImportStep.DOWNLOADING_MEDIA -> "Saving offline photos…"
    ImportStep.SAVING -> "Saving to library…"
    ImportStep.COMPLETE -> "Complete!"
    ImportStep.ERROR -> "Something went wrong"
}

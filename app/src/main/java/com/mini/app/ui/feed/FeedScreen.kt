package com.mini.app.ui.feed

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mini.app.domain.model.MediaItem
import com.mini.app.domain.model.Post
import com.mini.app.ui.components.FullScreenImageViewer
import com.mini.app.ui.components.NoteEditDialog
import com.mini.app.ui.components.QuickImportDialog
import com.mini.app.ui.components.TagManagementDialog
import com.mini.app.ui.importpost.ImportUiState
import com.mini.app.ui.navigation.Screen
import com.mini.app.ui.theme.*
import com.mini.app.util.PinnedPostNotificationManager
import com.mini.app.util.XUrlParser
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

fun formatRelativeTime(ms: Long): String {
    val diff = System.currentTimeMillis() - ms
    return when {
        diff < 60_000 -> "just now"
        diff < 3_600_000 -> "${diff / 60_000}m"
        diff < 86_400_000 -> "${diff / 3_600_000}h"
        diff < 7 * 86_400_000L -> "${diff / 86_400_000}d"
        else -> {
            val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
            sdf.format(Date(ms))
        }
    }
}

fun formatExactDateTime(ms: Long): String {
    if (ms <= 0) return "Unknown"
    val sdf = SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault())
    return sdf.format(Date(ms))
}

@Composable
fun FeedScreen(
    navController: NavHostController,
    filterTag: String? = null,
    viewModel: FeedViewModel = hiltViewModel()
) {
    val posts by viewModel.posts.collectAsStateWithLifecycle()
    val postCount by viewModel.postCount.collectAsStateWithLifecycle()
    val favoriteCount by viewModel.favoriteCount.collectAsStateWithLifecycle()
    val allTags by viewModel.allTags.collectAsStateWithLifecycle()
    val selectedTagFilter by viewModel.selectedTagFilter.collectAsStateWithLifecycle()
    val filterType by viewModel.filterType.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
    val quickImportState by viewModel.quickImportState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val isDark = LocalIsDarkTheme.current

    // Dialog & Overlay States
    var showQuickImportDialog by remember { mutableStateOf(false) }
    var postForNoteEdit by remember { mutableStateOf<Post?>(null) }
    var postForTagManagement by remember { mutableStateOf<Post?>(null) }
    var selectedMediaItems by remember { mutableStateOf<List<MediaItem>?>(null) }
    var selectedMediaIndex by remember { mutableStateOf(0) }
    var showSortMenu by remember { mutableStateOf(false) }

    // Clipboard auto-detection
    var detectedClipboardUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val text = clipboard?.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
            val xUrl = XUrlParser.extractFromText(text) ?: if (text.trim().startsWith("http") && (text.contains("twitter.com") || text.contains("x.com"))) text.trim() else null
            if (!xUrl.isNullOrBlank()) {
                detectedClipboardUrl = xUrl
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    LaunchedEffect(filterTag) {
        viewModel.setTagFilter(filterTag)
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = 80.dp)
            ) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = if (isDark) GlassSurfaceHighDark else GlassSurfaceHighLight,
                    contentColor = MaterialTheme.colorScheme.onBackground,
                    actionColor = MiniPrimary,
                    shape = RoundedCornerShape(14.dp)
                )
            }
        },
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Minimal & Aesthetic Top Header
            MinimalFeedTopBar(
                selectedTagFilter = selectedTagFilter,
                postCount = postCount,
                sortOrder = sortOrder,
                onClearFilter = { viewModel.setTagFilter(null) },
                onAddClick = { showQuickImportDialog = true },
                onSortClick = { showSortMenu = true },
                onSearchClick = { navController.navigate(Screen.Search.route) }
            )

            // Sort Dropdown Menu
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentSize(Alignment.TopEnd)
                    .padding(end = 20.dp)
            ) {
                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false },
                    modifier = Modifier
                        .background(if (isDark) GlassSurfaceHighDark else GlassSurfaceHighLight)
                        .border(
                            1.dp,
                            if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.06f),
                            RoundedCornerShape(14.dp)
                        )
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                "Newest Saved",
                                fontFamily = AppFontFamily,
                                fontWeight = if (sortOrder == FeedSortOrder.NEWEST) FontWeight.Bold else FontWeight.Normal,
                                color = if (sortOrder == FeedSortOrder.NEWEST) MiniPrimary else MaterialTheme.colorScheme.onBackground
                            )
                        },
                        onClick = {
                            viewModel.setSortOrder(FeedSortOrder.NEWEST)
                            showSortMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                "Oldest Saved",
                                fontFamily = AppFontFamily,
                                fontWeight = if (sortOrder == FeedSortOrder.OLDEST) FontWeight.Bold else FontWeight.Normal,
                                color = if (sortOrder == FeedSortOrder.OLDEST) MiniPrimary else MaterialTheme.colorScheme.onBackground
                            )
                        },
                        onClick = {
                            viewModel.setSortOrder(FeedSortOrder.OLDEST)
                            showSortMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                "Original Tweet Date",
                                fontFamily = AppFontFamily,
                                fontWeight = if (sortOrder == FeedSortOrder.ORIGINAL_DATE) FontWeight.Bold else FontWeight.Normal,
                                color = if (sortOrder == FeedSortOrder.ORIGINAL_DATE) MiniPrimary else MaterialTheme.colorScheme.onBackground
                            )
                        },
                        onClick = {
                            viewModel.setSortOrder(FeedSortOrder.ORIGINAL_DATE)
                            showSortMenu = false
                        }
                    )
                }
            }

            // Minimal Filter Carousel (All, ⭐ Starred, 📷 Media, 📝 Notes)
            FeedFilterCarousel(
                activeFilter = filterType,
                allCount = postCount,
                favoriteCount = favoriteCount,
                onFilterSelect = { viewModel.setFilterType(it) },
                selectedTag = selectedTagFilter,
                onClearTag = { viewModel.setTagFilter(null) }
            )

            // Detected Link in Clipboard Pill
            AnimatedVisibility(visible = detectedClipboardUrl != null) {
                detectedClipboardUrl?.let { url ->
                    ClipboardDetectedBanner(
                        url = url,
                        onImport = {
                            viewModel.startQuickImport(url)
                            detectedClipboardUrl = null
                            showQuickImportDialog = true
                        },
                        onDismiss = { detectedClipboardUrl = null }
                    )
                }
            }

            // Timeline Feed Content
            if (posts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    MinimalEmptyPlaceholder(
                        filterType = filterType,
                        hasTagFilter = selectedTagFilter != null,
                        onAddClick = { showQuickImportDialog = true }
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(top = 10.dp, bottom = 104.dp, start = 16.dp, end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(posts, key = { it.tweetId }) { post ->
                        Box(modifier = Modifier.animateItem()) {
                            MinimalTimelineCard(
                                post = post,
                                onToggleFavorite = { viewModel.toggleFavorite(post) },
                                onNoteClick = { postForNoteEdit = post },
                                onManageTags = { postForTagManagement = post },
                                onTagClick = { tag -> viewModel.setTagFilter(tag) },
                                onImageClick = { mediaList, index ->
                                    selectedMediaItems = mediaList
                                    selectedMediaIndex = index
                                },
                                onOpenOriginal = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(post.tweetUrl))
                                    context.startActivity(intent)
                                },
                                onDelete = {
                                    viewModel.deletePost(post)
                                    scope.launch {
                                        val result = snackbarHostState.showSnackbar(
                                            message = "Post deleted",
                                            actionLabel = "UNDO",
                                            duration = SnackbarDuration.Short
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            viewModel.undoDelete()
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Quick Import Dialog
    if (showQuickImportDialog) {
        QuickImportDialog(
            importState = quickImportState,
            onImportUrl = { url -> viewModel.startQuickImport(url) },
            onDismiss = {
                showQuickImportDialog = false
                viewModel.clearQuickImportState()
            }
        )
    }

    // Note Edit Dialog
    postForNoteEdit?.let { post ->
        NoteEditDialog(
            currentNote = post.note,
            onSaveNote = { newNote ->
                viewModel.updateNote(post.tweetId, newNote)
                postForNoteEdit = null
            },
            onDismiss = { postForNoteEdit = null }
        )
    }

    // Full Screen Image Viewer Modal
    selectedMediaItems?.let { mediaList ->
        FullScreenImageViewer(
            mediaItems = mediaList,
            initialIndex = selectedMediaIndex,
            onDismiss = { selectedMediaItems = null }
        )
    }

    // Tag Management Dialog
    postForTagManagement?.let { post ->
        TagManagementDialog(
            postTags = post.tags,
            allTags = allTags,
            onSaveTags = { tagNames ->
                viewModel.updatePostTags(post.tweetId, tagNames)
            },
            onCreateTag = { newTagName ->
                viewModel.createTag(newTagName)
            },
            onDismiss = { postForTagManagement = null }
        )
    }
}

@Composable
private fun MinimalFeedTopBar(
    selectedTagFilter: String?,
    postCount: Int,
    sortOrder: FeedSortOrder,
    onClearFilter: () -> Unit,
    onAddClick: () -> Unit,
    onSortClick: () -> Unit,
    onSearchClick: () -> Unit
) {
    val isDark = LocalIsDarkTheme.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (selectedTagFilter != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                IconButton(
                    onClick = onClearFilter,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0x22FFFFFF) else Color(0x0F000000))
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "#$selectedTagFilter",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = AppFontFamily
                    )
                    Text(
                        text = "Collection filter",
                        color = MiniSecondaryText,
                        fontSize = 11.5.sp,
                        fontFamily = AppFontFamily
                    )
                }
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Mini",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = AppFontFamily,
                    letterSpacing = (-0.5).sp
                )
                if (postCount > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (isDark) Color(0x26FFFFFF) else Color(0x10000000))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "$postCount",
                            color = MiniSecondaryText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = AppFontFamily
                        )
                    }
                }
            }
        }

        // Top Action Icons (+ Add, Sort, Search)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Quick Add (+) Button
            IconButton(
                onClick = onAddClick,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MiniPrimary.copy(alpha = if (isDark) 0.22f else 0.14f))
                    .border(
                        1.dp,
                        MiniPrimary.copy(alpha = 0.35f),
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = "Import Post",
                    tint = MiniPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Sort Menu Button
            IconButton(
                onClick = onSortClick,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color(0x1AFFFFFF) else Color(0x0A000000))
                    .border(
                        1.dp,
                        if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.06f),
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Sort,
                    contentDescription = "Sort",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(19.dp)
                )
            }

            // Search Button
            IconButton(
                onClick = onSearchClick,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color(0x1AFFFFFF) else Color(0x0A000000))
                    .border(
                        1.dp,
                        if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.06f),
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(19.dp)
                )
            }
        }
    }
}

@Composable
private fun FeedFilterCarousel(
    activeFilter: FeedFilterType,
    allCount: Int,
    favoriteCount: Int,
    onFilterSelect: (FeedFilterType) -> Unit,
    selectedTag: String?,
    onClearTag: () -> Unit
) {
    val isDark = LocalIsDarkTheme.current

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selectedTag != null) {
            item {
                FilterChipPill(
                    label = "#$selectedTag",
                    isSelected = true,
                    isDark = isDark,
                    trailingIcon = Icons.Outlined.Close,
                    onClick = onClearTag
                )
            }
        }

        item {
            FilterChipPill(
                label = "All ($allCount)",
                isSelected = activeFilter == FeedFilterType.ALL,
                isDark = isDark,
                onClick = { onFilterSelect(FeedFilterType.ALL) }
            )
        }

        item {
            FilterChipPill(
                label = "⭐ Starred ($favoriteCount)",
                isSelected = activeFilter == FeedFilterType.FAVORITES,
                isDark = isDark,
                onClick = { onFilterSelect(FeedFilterType.FAVORITES) }
            )
        }

        item {
            FilterChipPill(
                label = "📷 Media",
                isSelected = activeFilter == FeedFilterType.MEDIA_ONLY,
                isDark = isDark,
                onClick = { onFilterSelect(FeedFilterType.MEDIA_ONLY) }
            )
        }

        item {
            FilterChipPill(
                label = "📝 Notes",
                isSelected = activeFilter == FeedFilterType.WITH_NOTES,
                isDark = isDark,
                onClick = { onFilterSelect(FeedFilterType.WITH_NOTES) }
            )
        }
    }
}

@Composable
private fun FilterChipPill(
    label: String,
    isSelected: Boolean,
    isDark: Boolean,
    trailingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit
) {
    val bg = if (isSelected) {
        MiniPrimary.copy(alpha = if (isDark) 0.22f else 0.14f)
    } else {
        if (isDark) Color(0x14FFFFFF) else Color(0x0A000000)
    }

    val border = if (isSelected) {
        MiniPrimary.copy(alpha = 0.40f)
    } else {
        if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)
    }

    val textColor = if (isSelected) {
        MiniPrimary
    } else {
        MiniSecondaryText
    }

    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(bg)
            .border(1.dp, border, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 12.5.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            fontFamily = AppFontFamily
        )
        if (trailingIcon != null) {
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = trailingIcon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun ClipboardDetectedBanner(
    url: String,
    onImport: () -> Unit,
    onDismiss: () -> Unit
) {
    val isDark = LocalIsDarkTheme.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isDark) Color(0x286366F1) else Color(0x146366F1))
            .border(
                1.dp,
                MiniPrimary.copy(alpha = 0.35f),
                RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Outlined.Link,
                contentDescription = null,
                tint = MiniPrimary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "X link detected in clipboard",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = AppFontFamily
                )
                Text(
                    text = url,
                    color = MiniSecondaryText,
                    fontSize = 11.5.sp,
                    fontFamily = AppFontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = onImport,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MiniPrimary,
                    contentColor = Color.White
                ),
                shape = CircleShape,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                modifier = Modifier.height(30.dp)
            ) {
                Text("Import", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = AppFontFamily)
            }
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Dismiss",
                    tint = MiniSecondaryText,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun MinimalEmptyPlaceholder(
    filterType: FeedFilterType,
    hasTagFilter: Boolean,
    onAddClick: () -> Unit
) {
    val isDark = LocalIsDarkTheme.current

    val title = when {
        hasTagFilter -> "No posts in this collection"
        filterType == FeedFilterType.FAVORITES -> "No starred posts yet"
        filterType == FeedFilterType.MEDIA_ONLY -> "No media posts saved"
        filterType == FeedFilterType.WITH_NOTES -> "No notes saved yet"
        else -> "Your library is empty"
    }

    val subtitle = when {
        hasTagFilter -> "Add tags to posts to organize them into this collection"
        filterType == FeedFilterType.FAVORITES -> "Tap the star on any post to keep your best posts here"
        filterType == FeedFilterType.MEDIA_ONLY -> "Posts with photos from X will appear here"
        filterType == FeedFilterType.WITH_NOTES -> "Tap the note button on any post to write thoughts"
        else -> "Share any post from X to Mini, or tap the button below to paste a link"
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(24.dp)
            .liquidGlassEffect(shape = RoundedCornerShape(24.dp), isDark = isDark)
            .padding(32.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MiniPrimary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when {
                    filterType == FeedFilterType.FAVORITES -> Icons.Outlined.StarBorder
                    filterType == FeedFilterType.MEDIA_ONLY -> Icons.Outlined.PhotoLibrary
                    filterType == FeedFilterType.WITH_NOTES -> Icons.Outlined.EditNote
                    else -> Icons.Outlined.BookmarkBorder
                },
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = if (filterType == FeedFilterType.FAVORITES) MiniAccentAmber else MiniPrimary
            )
        }
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = AppFontFamily
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            color = MiniSecondaryText,
            fontSize = 13.5.sp,
            fontFamily = AppFontFamily,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        if (filterType == FeedFilterType.ALL && !hasTagFilter) {
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onAddClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MiniPrimary,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Paste Post Link", fontFamily = AppFontFamily, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun MinimalTimelineCard(
    post: Post,
    searchHighlight: String? = null,
    onToggleFavorite: () -> Unit,
    onNoteClick: () -> Unit,
    onManageTags: () -> Unit,
    onTagClick: ((String) -> Unit)? = null,
    onImageClick: (List<MediaItem>, Int) -> Unit,
    onOpenOriginal: () -> Unit,
    onDelete: () -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    var menuExpanded by remember { mutableStateOf(false) }
    var showExactTimeDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    var isPinned by remember(post.tweetId) {
        mutableStateOf(PinnedPostNotificationManager.getPinnedTweetId(context) == post.tweetId)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            PinnedPostNotificationManager.pinPost(context, post)
            isPinned = true
            Toast.makeText(context, "Pinned to lock screen & notifications", Toast.LENGTH_SHORT).show()
        }
    }

    if (showExactTimeDialog) {
        AlertDialog(
            onDismissRequest = { showExactTimeDialog = false },
            title = {
                Text(
                    "Post Timestamp",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontFamily = AppFontFamily,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (post.originalDateMs > 0) {
                        Text(
                            "Original post: ${formatExactDateTime(post.originalDateMs)}",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 14.sp,
                            fontFamily = AppFontFamily
                        )
                    }
                    Text(
                        "Saved to Mini: ${formatExactDateTime(post.savedAtMs)}",
                        color = MiniSecondaryText,
                        fontSize = 14.sp,
                        fontFamily = AppFontFamily
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showExactTimeDialog = false }) {
                    Text("Close", color = MiniPrimary, fontFamily = AppFontFamily, fontWeight = FontWeight.SemiBold)
                }
            },
            containerColor = if (isDark) GlassSurfaceHighDark else GlassSurfaceHighLight,
            shape = RoundedCornerShape(20.dp)
        )
    }

    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        isDark = isDark,
        elevation = 2.dp,
        contentPadding = PaddingValues(16.dp)
    ) {
        // Author Header & Favorite Star
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AuthorAvatar(
                avatarUrl = post.author.avatarUrl ?: post.author.avatarRemoteUrl,
                displayName = post.author.displayName,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = post.author.displayName,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = AppFontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "@${post.author.handle} · ${formatRelativeTime(post.savedAtMs)}",
                    color = MiniSecondaryText,
                    fontSize = 12.5.sp,
                    fontFamily = AppFontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable { showExactTimeDialog = true }
                )
            }

            // Quick Favorite Star Toggle
            val starScale by animateFloatAsState(
                targetValue = if (post.isFavorite) 1.15f else 1.0f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "starScale"
            )

            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier
                    .size(34.dp)
                    .scale(starScale)
            ) {
                Icon(
                    imageVector = if (post.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = if (post.isFavorite) "Remove Star" else "Star Post",
                    tint = if (post.isFavorite) MiniAccentAmber else MiniSecondaryText.copy(alpha = 0.7f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Post Text Body with Rich Formatting
        Spacer(modifier = Modifier.height(10.dp))
        RichPostText(
            text = post.text,
            searchHighlight = searchHighlight,
            onHashtagClick = onTagClick
        )

        // Personal Note Display (if attached)
        if (!post.note.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isDark) Color(0x221E2338) else Color(0x0C000000))
                    .border(
                        1.dp,
                        if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f),
                        RoundedCornerShape(12.dp)
                    )
                    .clickable(onClick = onNoteClick)
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(24.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MiniPrimary)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "NOTE",
                        color = MiniPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = AppFontFamily,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = post.note,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 13.5.sp,
                        fontFamily = AppFontFamily,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = "Edit note",
                    tint = MiniSecondaryText,
                    modifier = Modifier.size(15.dp)
                )
            }
        }

        // Tag Pills
        if (post.tags.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                post.tags.forEach { tag ->
                    LiquidGlassPill(
                        modifier = Modifier.clickable { onTagClick?.invoke(tag.name) },
                        isDark = isDark
                    ) {
                        Text(
                            text = "#${tag.name}",
                            color = MiniPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = AppFontFamily
                        )
                    }
                }
            }
        }

        // Media Images Gallery
        if (post.mediaItems.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            val mediaCornerShape = RoundedCornerShape(14.dp)

            if (post.mediaItems.size == 1) {
                val media = post.mediaItems[0]
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(media.localPath?.let { File(it) } ?: media.remoteUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Post Image",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .clip(mediaCornerShape)
                        .border(
                            1.dp,
                            if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.05f),
                            mediaCornerShape
                        )
                        .clickable { onImageClick(post.mediaItems, 0) }
                )
            } else if (post.mediaItems.size == 2) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    post.mediaItems.forEachIndexed { index, media ->
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(media.localPath?.let { File(it) } ?: media.remoteUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Post Image ${index + 1}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .weight(1f)
                                .height(190.dp)
                                .clip(mediaCornerShape)
                                .border(
                                    1.dp,
                                    if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.05f),
                                    mediaCornerShape
                                )
                                .clickable { onImageClick(post.mediaItems, index) }
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val media0 = post.mediaItems[0]
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(media0.localPath?.let { File(it) } ?: media0.remoteUrl)
                        .crossfade(true)
                        .build(),
                        contentDescription = "Post Image 1",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp)
                            .clip(mediaCornerShape)
                            .border(
                                1.dp,
                                if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.05f),
                                mediaCornerShape
                            )
                            .clickable { onImageClick(post.mediaItems, 0) }
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        post.mediaItems.drop(1).take(2).forEachIndexed { subIndex, media ->
                            val index = subIndex + 1
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(media.localPath?.let { File(it) } ?: media.remoteUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Post Image ${index + 1}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(110.dp)
                                    .clip(mediaCornerShape)
                                    .border(
                                        1.dp,
                                        if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.05f),
                                        mediaCornerShape
                                    )
                                    .clickable { onImageClick(post.mediaItems, index) }
                            )
                        }
                    }
                }
            }
        }

        // Minimal Action Row: Note, Manage Tags, Share, More Menu
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            // Note Button
            IconButton(
                onClick = onNoteClick,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = if (!post.note.isNullOrBlank()) Icons.Outlined.EditNote else Icons.AutoMirrored.Outlined.Notes,
                    contentDescription = "Add/Edit Note",
                    tint = if (!post.note.isNullOrBlank()) MiniPrimary else MiniSecondaryText,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Tag/Collections Button
            IconButton(
                onClick = onManageTags,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Label,
                    contentDescription = "Manage Collections",
                    tint = MiniSecondaryText,
                    modifier = Modifier.size(19.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Share Button
            IconButton(
                onClick = {
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, post.tweetUrl)
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Share Post"))
                },
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Share,
                    contentDescription = "Share",
                    tint = MiniSecondaryText,
                    modifier = Modifier.size(19.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // 3-dot More Options Menu
            Box {
                IconButton(
                    onClick = {
                        isPinned = PinnedPostNotificationManager.getPinnedTweetId(context) == post.tweetId
                        menuExpanded = true
                    },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More options",
                        tint = MiniSecondaryText,
                        modifier = Modifier.size(19.dp)
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier
                        .background(if (isDark) GlassSurfaceHighDark else GlassSurfaceHighLight)
                        .border(
                            1.dp,
                            if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.06f),
                            RoundedCornerShape(14.dp)
                        )
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (isPinned) "Unpin from lock screen" else "Pin to lock screen",
                                color = MaterialTheme.colorScheme.onBackground,
                                fontFamily = AppFontFamily
                            )
                        },
                        leadingIcon = { Icon(Icons.Outlined.PushPin, contentDescription = null, tint = MiniPrimary) },
                        onClick = {
                            menuExpanded = false
                            if (isPinned) {
                                PinnedPostNotificationManager.unpinPost(context)
                                isPinned = false
                                Toast.makeText(context, "Unpinned from notifications", Toast.LENGTH_SHORT).show()
                            } else {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    val hasPermission = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.POST_NOTIFICATIONS
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (!hasPermission) {
                                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        return@DropdownMenuItem
                                    }
                                }
                                PinnedPostNotificationManager.pinPost(context, post)
                                isPinned = true
                                Toast.makeText(context, "Pinned to lock screen & notifications", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text("Open original in X", color = MaterialTheme.colorScheme.onBackground, fontFamily = AppFontFamily)
                        },
                        leadingIcon = { Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null, tint = MiniPrimary) },
                        onClick = {
                            menuExpanded = false
                            onOpenOriginal()
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text("Copy text", color = MaterialTheme.colorScheme.onBackground, fontFamily = AppFontFamily)
                        },
                        leadingIcon = { Icon(Icons.Outlined.ContentCopy, contentDescription = null, tint = MiniPrimary) },
                        onClick = {
                            menuExpanded = false
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Post Text", post.text))
                            Toast.makeText(context, "Copied post text", Toast.LENGTH_SHORT).show()
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text("Delete", color = MiniError, fontFamily = AppFontFamily, fontWeight = FontWeight.Medium)
                        },
                        leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = MiniError) },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

// Keep backwards compatible composable signature for any other caller
@Composable
fun LiquidGlassTimelineCard(
    post: Post,
    searchHighlight: String? = null,
    onDelete: () -> Unit,
    onOpenOriginal: () -> Unit,
    onImageClick: (List<MediaItem>, Int) -> Unit,
    onManageTags: () -> Unit,
    onTagClick: ((String) -> Unit)? = null,
    onToggleFavorite: (() -> Unit)? = null,
    onNoteClick: (() -> Unit)? = null
) {
    MinimalTimelineCard(
        post = post,
        searchHighlight = searchHighlight,
        onToggleFavorite = onToggleFavorite ?: {},
        onNoteClick = onNoteClick ?: {},
        onManageTags = onManageTags,
        onTagClick = onTagClick,
        onImageClick = onImageClick,
        onOpenOriginal = onOpenOriginal,
        onDelete = onDelete
    )
}

@Composable
fun AuthorAvatar(
    avatarUrl: String?,
    displayName: String,
    modifier: Modifier = Modifier
) {
    var hasError by remember(avatarUrl) { mutableStateOf(false) }
    val localContext = LocalContext.current
    val imageModel = remember(avatarUrl) {
        avatarUrl?.let { if (it.startsWith("/")) File(it) else it }
    }

    if (imageModel != null && !hasError) {
        AsyncImage(
            model = ImageRequest.Builder(localContext)
                .data(imageModel)
                .crossfade(true)
                .listener(onError = { _, _ -> hasError = true })
                .build(),
            contentDescription = displayName,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .clip(CircleShape)
                .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape)
        )
    } else {
        val initial = displayName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
        Surface(
            color = MiniPrimary.copy(alpha = 0.18f),
            shape = CircleShape,
            modifier = modifier.border(1.dp, MiniPrimary.copy(alpha = 0.3f), CircleShape)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = initial,
                    color = MiniPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    fontFamily = AppFontFamily
                )
            }
        }
    }
}

@Composable
fun RichPostText(
    text: String,
    searchHighlight: String? = null,
    onHashtagClick: ((String) -> Unit)? = null,
    onUrlClick: ((String) -> Unit)? = null,
    maxCollapsedLines: Int = 5,
    modifier: Modifier = Modifier
) {
    val fontScale = LocalTweetFontSizeScale.current
    val context = LocalContext.current
    var isExpanded by remember(text) { mutableStateOf(false) }
    var isTextOverflowing by remember(text) { mutableStateOf(false) }

    val isLengthy = remember(text) {
        text.length > 280 || text.count { it == '\n' } >= 4
    }

    val annotatedString = remember(text, searchHighlight) {
        buildAnnotatedString {
            append(text)

            val urlPattern = Pattern.compile("(https?://[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]+)")
            val hashtagPattern = Pattern.compile("#([a-zA-Z0-9_]+)")
            val mentionPattern = Pattern.compile("@([a-zA-Z0-9_]+)")

            val urlMatcher = urlPattern.matcher(text)
            while (urlMatcher.find()) {
                val start = urlMatcher.start()
                val end = urlMatcher.end()
                val url = urlMatcher.group(1) ?: ""
                addStyle(
                    SpanStyle(
                        color = MiniPrimary,
                        textDecoration = TextDecoration.Underline,
                        fontWeight = FontWeight.Medium
                    ),
                    start, end
                )
                addStringAnnotation(tag = "URL", annotation = url, start = start, end = end)
            }

            val hashtagMatcher = hashtagPattern.matcher(text)
            while (hashtagMatcher.find()) {
                val start = hashtagMatcher.start()
                val end = hashtagMatcher.end()
                val tag = hashtagMatcher.group(1) ?: ""
                addStyle(
                    SpanStyle(color = MiniPrimary, fontWeight = FontWeight.SemiBold),
                    start, end
                )
                addStringAnnotation(tag = "HASHTAG", annotation = tag, start = start, end = end)
            }

            val mentionMatcher = mentionPattern.matcher(text)
            while (mentionMatcher.find()) {
                val start = mentionMatcher.start()
                val end = mentionMatcher.end()
                addStyle(
                    SpanStyle(color = MiniPrimary, fontWeight = FontWeight.Medium),
                    start, end
                )
            }

            if (!searchHighlight.isNullOrBlank()) {
                var searchStart = 0
                val lowerText = text.lowercase()
                val lowerQuery = searchHighlight.lowercase().trim()
                while (searchStart < lowerText.length) {
                    val index = lowerText.indexOf(lowerQuery, searchStart)
                    if (index == -1) break
                    val endIndex = index + lowerQuery.length
                    addStyle(
                        SpanStyle(
                            background = MiniPrimary.copy(alpha = 0.30f),
                            fontWeight = FontWeight.Bold
                        ),
                        index, endIndex
                    )
                    searchStart = endIndex
                }
            }
        }
    }

    Column(modifier = modifier.animateContentSize()) {
        ClickableText(
            text = annotatedString,
            maxLines = if (isExpanded) Int.MAX_VALUE else maxCollapsedLines,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { textLayoutResult ->
                if (!isExpanded) {
                    if (textLayoutResult.hasVisualOverflow || textLayoutResult.lineCount >= maxCollapsedLines) {
                        isTextOverflowing = true
                    }
                }
            },
            style = TextStyle(
                fontFamily = AppFontFamily,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = (15 * fontScale).sp,
                lineHeight = (22 * fontScale).sp,
                letterSpacing = (-0.2).sp
            ),
            onClick = { offset ->
                annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                    .firstOrNull()?.let { annotation ->
                        if (onUrlClick != null) {
                            onUrlClick(annotation.item)
                        } else {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(annotation.item))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        return@ClickableText
                    }

                annotatedString.getStringAnnotations(tag = "HASHTAG", start = offset, end = offset)
                    .firstOrNull()?.let { annotation ->
                        onHashtagClick?.invoke(annotation.item)
                    }
            }
        )

        if (isLengthy || isTextOverflowing) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isExpanded) "Show less" else "Show more…",
                color = MiniPrimary,
                fontSize = (13 * fontScale).sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = AppFontFamily,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { isExpanded = !isExpanded }
                    .padding(vertical = 2.dp, horizontal = 2.dp)
            )
        }
    }
}

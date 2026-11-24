package com.mini.app.ui.search

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.mini.app.domain.model.MediaItem
import com.mini.app.domain.model.Post
import com.mini.app.ui.components.FullScreenImageViewer
import com.mini.app.ui.components.NoteEditDialog
import com.mini.app.ui.components.TagManagementDialog
import com.mini.app.ui.feed.MinimalTimelineCard
import com.mini.app.ui.theme.*

@Composable
fun SearchScreen(
    navController: NavHostController,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val allTags by viewModel.allTags.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val isDark = LocalIsDarkTheme.current

    var selectedMediaItems by remember { mutableStateOf<List<MediaItem>?>(null) }
    var selectedMediaIndex by remember { mutableStateOf(0) }
    var postForTagManagement by remember { mutableStateOf<Post?>(null) }
    var postForNoteEdit by remember { mutableStateOf<Post?>(null) }
    var postToDelete by remember { mutableStateOf<Post?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Search Header Bar with single safe status bar inset
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color(0x1AFFFFFF) else Color(0x0A000000))
                    .border(
                        1.dp,
                        if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.05f),
                        CircleShape
                    )
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Floating Minimal Search Capsule
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(CircleShape)
                    .background(if (isDark) GlassSurfaceCardDark else GlassSurfaceCardLight)
                    .border(
                        1.dp,
                        if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f),
                        CircleShape
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = MiniSecondaryText,
                    modifier = Modifier.size(18.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (query.isEmpty()) {
                        Text(
                            text = "Search posts, notes, authors, tags…",
                            color = MiniSecondaryText,
                            fontSize = 14.5.sp,
                            fontFamily = AppFontFamily
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = { viewModel.updateQuery(it) },
                        textStyle = TextStyle(
                            fontFamily = AppFontFamily,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        cursorBrush = SolidColor(MiniPrimary),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (query.isNotEmpty()) {
                    IconButton(
                        onClick = { viewModel.updateQuery("") },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = "Clear search",
                            tint = MiniSecondaryText,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        if (query.isBlank()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 32.dp, bottom = 100.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = MiniSecondaryText.copy(alpha = 0.4f),
                    modifier = Modifier.size(52.dp)
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Search your saved library",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = AppFontFamily
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Search by text, notes, author handle, or tags",
                    color = MiniSecondaryText,
                    fontSize = 13.sp,
                    fontFamily = AppFontFamily
                )

                if (allTags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(
                        text = "EXPLORE COLLECTIONS",
                        color = MiniSecondaryText,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = AppFontFamily,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(allTags, key = { it.name }) { tag ->
                            LiquidGlassPill(
                                modifier = Modifier.clickable { viewModel.updateQuery(tag.name) },
                                isDark = isDark
                            ) {
                                Text(
                                    text = "#${tag.name}",
                                    color = MiniPrimary,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = AppFontFamily
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "(${tag.postCount})",
                                    color = MiniSecondaryText,
                                    fontSize = 11.5.sp,
                                    fontFamily = AppFontFamily
                                )
                            }
                        }
                    }
                }
            }
        } else if (results.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 100.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No saved posts found for \"$query\"",
                    color = MiniSecondaryText,
                    fontSize = 14.5.sp,
                    fontFamily = AppFontFamily
                )
            }
        } else {
            // Minimal Timeline Feed of Search Results with Query Highlights
            LazyColumn(
                contentPadding = PaddingValues(top = 10.dp, bottom = 104.dp, start = 16.dp, end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(results, key = { it.tweetId }) { post ->
                    MinimalTimelineCard(
                        post = post,
                        searchHighlight = query,
                        onToggleFavorite = { viewModel.toggleFavorite(post) },
                        onNoteClick = { postForNoteEdit = post },
                        onDelete = { postToDelete = post },
                        onOpenOriginal = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(post.tweetUrl))
                            context.startActivity(intent)
                        },
                        onImageClick = { mediaList, index ->
                            selectedMediaItems = mediaList
                            selectedMediaIndex = index
                        },
                        onManageTags = {
                            postForTagManagement = post
                        },
                        onTagClick = { tag ->
                            viewModel.updateQuery(tag)
                        }
                    )
                }
            }
        }
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

    // Delete Confirm Dialog
    postToDelete?.let { post ->
        AlertDialog(
            onDismissRequest = { postToDelete = null },
            title = {
                Text(
                    "Delete Post",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontFamily = AppFontFamily,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "Are you sure you want to delete this post from your library?",
                    color = MiniSecondaryText,
                    fontFamily = AppFontFamily,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deletePost(post)
                    postToDelete = null
                }) {
                    Text("Delete", color = MiniError, fontFamily = AppFontFamily, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { postToDelete = null }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onBackground, fontFamily = AppFontFamily)
                }
            },
            containerColor = if (isDark) GlassSurfaceHighDark else GlassSurfaceHighLight,
            shape = RoundedCornerShape(20.dp)
        )
    }
}


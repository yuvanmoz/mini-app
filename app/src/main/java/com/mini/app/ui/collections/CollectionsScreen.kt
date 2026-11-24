package com.mini.app.ui.collections

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mini.app.domain.model.Tag
import com.mini.app.ui.theme.*
import java.io.File

@Composable
fun CollectionsScreen(
    navController: NavHostController,
    viewModel: CollectionsViewModel = hiltViewModel()
) {
    val tags by viewModel.tags.collectAsStateWithLifecycle()
    val totalPostCount by viewModel.totalPostCount.collectAsStateWithLifecycle()
    val favoriteCount by viewModel.favoriteCount.collectAsStateWithLifecycle()
    val currentSort by viewModel.sortOrder.collectAsStateWithLifecycle()
    val isDark = LocalIsDarkTheme.current

    var showCreateDialog by remember { mutableStateOf(false) }
    var tagToDelete by remember { mutableStateOf<Tag?>(null) }
    var tagToRename by remember { mutableStateOf<Tag?>(null) }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Apple-style Glass Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Collections",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = AppFontFamily,
                    letterSpacing = (-0.4).sp
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box {
                        IconButton(
                            onClick = { sortMenuExpanded = true },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0x2EFFFFFF) else Color(0x14000000))
                                .border(
                                    1.dp,
                                    if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.05f),
                                    CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Sort,
                                contentDescription = "Sort Collections",
                                tint = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest = { sortMenuExpanded = false },
                            modifier = Modifier
                                .background(if (isDark) GlassSurfaceHighDark else GlassSurfaceHighLight)
                                .border(
                                    1.dp,
                                    if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.06f),
                                    RoundedCornerShape(12.dp)
                                )
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Most Posts",
                                        fontWeight = if (currentSort == CollectionSortOrder.MOST_POSTS) FontWeight.Bold else FontWeight.Normal,
                                        color = if (currentSort == CollectionSortOrder.MOST_POSTS) MiniPrimary else MaterialTheme.colorScheme.onBackground,
                                        fontFamily = AppFontFamily
                                    )
                                },
                                onClick = {
                                    viewModel.setSortOrder(CollectionSortOrder.MOST_POSTS)
                                    sortMenuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Alphabetical (A–Z)",
                                        fontWeight = if (currentSort == CollectionSortOrder.ALPHABETICAL) FontWeight.Bold else FontWeight.Normal,
                                        color = if (currentSort == CollectionSortOrder.ALPHABETICAL) MiniPrimary else MaterialTheme.colorScheme.onBackground,
                                        fontFamily = AppFontFamily
                                    )
                                },
                                onClick = {
                                    viewModel.setSortOrder(CollectionSortOrder.ALPHABETICAL)
                                    sortMenuExpanded = false
                                }
                            )
                        }
                    }

                    IconButton(
                        onClick = { showCreateDialog = true },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0x388B5CF6) else Color(0x228B5CF6))
                            .border(
                                1.dp,
                                MiniPrimary.copy(alpha = 0.4f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Create Collection",
                            tint = MiniPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 10.dp, bottom = 104.dp, start = 16.dp, end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // System Collection: All Posts
                item {
                    CollectionGlassCard(
                        title = "All Posts",
                        postCount = totalPostCount,
                        previewImageUrl = null,
                        isSystem = true,
                        onClick = { navController.navigate("feed") },
                        onRename = {},
                        onDelete = {}
                    )
                }

                // System Collection: Starred Posts
                item {
                    CollectionGlassCard(
                        title = "⭐ Starred",
                        postCount = favoriteCount,
                        previewImageUrl = null,
                        isSystem = true,
                        isStarred = true,
                        onClick = { navController.navigate("feed") },
                        onRename = {},
                        onDelete = {}
                    )
                }

                items(tags, key = { it.name }) { tag ->
                    CollectionGlassCard(
                        title = tag.name,
                        postCount = tag.postCount,
                        previewImageUrl = tag.previewImageUrl,
                        isSystem = false,
                        onClick = { navController.navigate("feed?tag=${Uri.encode(tag.name)}") },
                        onRename = { tagToRename = tag },
                        onDelete = { tagToDelete = tag }
                    )
                }
            }
        }
    }

    // Create Tag Dialog
    if (showCreateDialog) {
        var newTagName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = {
                Text(
                    "New Collection",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontFamily = AppFontFamily,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                OutlinedTextField(
                    value = newTagName,
                    onValueChange = { newTagName = it },
                    placeholder = { Text("Collection name (e.g. Design, Quotes)", fontSize = 14.sp, fontFamily = AppFontFamily) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MiniPrimary,
                        unfocusedBorderColor = if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.15f)
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTagName.isNotBlank()) {
                            viewModel.createTag(newTagName.trim())
                            showCreateDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MiniPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Create", color = Color.White, fontFamily = AppFontFamily, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onBackground, fontFamily = AppFontFamily)
                }
            },
            containerColor = if (isDark) GlassSurfaceHighDark else GlassSurfaceHighLight,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Rename Tag Dialog
    tagToRename?.let { tag ->
        var updatedName by remember { mutableStateOf(tag.name) }
        AlertDialog(
            onDismissRequest = { tagToRename = null },
            title = {
                Text(
                    "Rename Collection",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontFamily = AppFontFamily,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                OutlinedTextField(
                    value = updatedName,
                    onValueChange = { updatedName = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MiniPrimary,
                        unfocusedBorderColor = if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.15f)
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (updatedName.isNotBlank()) {
                            viewModel.renameTag(tag.name, updatedName.trim())
                            tagToRename = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MiniPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save", color = Color.White, fontFamily = AppFontFamily, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { tagToRename = null }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onBackground, fontFamily = AppFontFamily)
                }
            },
            containerColor = if (isDark) GlassSurfaceHighDark else GlassSurfaceHighLight,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Delete Tag Confirm Dialog
    tagToDelete?.let { tag ->
        AlertDialog(
            onDismissRequest = { tagToDelete = null },
            title = {
                Text(
                    "Delete Collection",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontFamily = AppFontFamily,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete the \"${tag.name}\" collection? Saved posts will NOT be deleted.",
                    color = MiniSecondaryText,
                    fontSize = 14.sp,
                    fontFamily = AppFontFamily
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteTag(tag.name)
                    tagToDelete = null
                }) {
                    Text("Delete", color = MiniError, fontFamily = AppFontFamily, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { tagToDelete = null }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onBackground, fontFamily = AppFontFamily)
                }
            },
            containerColor = if (isDark) GlassSurfaceHighDark else GlassSurfaceHighLight,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
fun CollectionGlassCard(
    title: String,
    postCount: Int,
    previewImageUrl: String?,
    isSystem: Boolean,
    isStarred: Boolean = false,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlassEffect(shape = RoundedCornerShape(16.dp), isDark = isDark, elevation = 2.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!previewImageUrl.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(if (previewImageUrl.startsWith("/")) File(previewImageUrl) else previewImageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(
                        1.dp,
                        if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.06f),
                        RoundedCornerShape(14.dp)
                    )
            )
        } else {
            val iconTint = if (isStarred) MiniAccentAmber else MiniPrimary
            val iconBg = if (isStarred) MiniAccentAmber.copy(alpha = 0.15f) else MiniPrimary.copy(alpha = 0.15f)
            val iconBorder = if (isStarred) MiniAccentAmber.copy(alpha = 0.35f) else MiniPrimary.copy(alpha = 0.3f)

            Surface(
                color = iconBg,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .size(48.dp)
                    .border(
                        1.dp,
                        iconBorder,
                        RoundedCornerShape(14.dp)
                    )
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = when {
                            isStarred -> Icons.Filled.Star
                            isSystem -> Icons.Outlined.Folder
                            else -> Icons.Outlined.Tag
                        },
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = AppFontFamily
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "$postCount ${if (postCount == 1) "post" else "posts"}",
                color = MiniSecondaryText,
                fontSize = 13.sp,
                fontFamily = AppFontFamily
            )
        }

        if (!isSystem) {
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = MiniSecondaryText,
                        modifier = Modifier.size(20.dp)
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier
                        .background(if (isDark) GlassSurfaceHighDark else GlassSurfaceHighLight)
                        .border(
                            1.dp,
                            if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.06f),
                            RoundedCornerShape(12.dp)
                        )
                ) {
                    DropdownMenuItem(
                        text = { Text("Rename", color = MaterialTheme.colorScheme.onBackground, fontFamily = AppFontFamily) },
                        leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null, tint = MiniPrimary) },
                        onClick = {
                            menuExpanded = false
                            onRename()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = MiniError, fontFamily = AppFontFamily, fontWeight = FontWeight.Medium) },
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


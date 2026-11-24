package com.mini.app.ui.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mini.app.data.repository.PostRepository
import com.mini.app.domain.model.Post
import com.mini.app.ui.feed.AuthorAvatar
import com.mini.app.ui.feed.formatRelativeTime
import com.mini.app.ui.settings.SettingsViewModel
import com.mini.app.ui.theme.*
import com.mini.app.widget.PostWidgetProvider
import com.mini.app.widget.WidgetPreferences
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.io.File
import javax.inject.Inject

@HiltViewModel
class WidgetConfigureViewModel @Inject constructor(
    private val postRepository: PostRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val posts: StateFlow<List<Post>> = combine(
        postRepository.getAllPosts(),
        _searchQuery
    ) { allPosts, query ->
        if (query.isBlank()) {
            allPosts
        } else {
            val q = query.lowercase().trim()
            allPosts.filter { post ->
                post.text.lowercase().contains(q) ||
                post.author.displayName.lowercase().contains(q) ||
                post.author.handle.lowercase().contains(q) ||
                post.tags.any { it.name.lowercase().contains(q) }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }
}

@AndroidEntryPoint
class PostWidgetConfigureActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Set the result to CANCELED. This will cause the widget host to cancel
        // out of the widget placement if the user presses the back button.
        setResult(Activity.RESULT_CANCELED)

        // Find the widget id from the intent.
        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

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
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .liquidAtmosphere()
                    ) {
                        WidgetConfigureContent(
                            appWidgetId = appWidgetId,
                            onPostSelected = { selectedPost ->
                                selectPostAndFinish(selectedPost)
                            },
                            onPlaceEmpty = {
                                placeEmptyWidgetAndFinish()
                            },
                            onCancel = {
                                finish()
                            }
                        )
                    }
                }
            }
        }
    }

    private fun selectPostAndFinish(post: Post) {
        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            WidgetPreferences.savePostForWidget(this, appWidgetId, post.tweetId)
            val appWidgetManager = AppWidgetManager.getInstance(this)
            PostWidgetProvider.updateAppWidget(this, appWidgetManager, appWidgetId)

            val resultValue = Intent().apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            setResult(Activity.RESULT_OK, resultValue)
        }
        finish()
    }

    private fun placeEmptyWidgetAndFinish() {
        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            val appWidgetManager = AppWidgetManager.getInstance(this)
            PostWidgetProvider.updateAppWidget(this, appWidgetManager, appWidgetId)

            val resultValue = Intent().apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            setResult(Activity.RESULT_OK, resultValue)
        }
        finish()
    }
}

@Composable
fun WidgetConfigureContent(
    appWidgetId: Int,
    onPostSelected: (Post) -> Unit,
    onPlaceEmpty: () -> Unit,
    onCancel: () -> Unit,
    viewModel: WidgetConfigureViewModel = hiltViewModel()
) {
    val posts by viewModel.posts.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val isDark = LocalIsDarkTheme.current

    val currentSelectedTweetId = remember(appWidgetId) {
        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            WidgetPreferences.getPostIdForWidget(context, appWidgetId)
        } else null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MiniPrimary.copy(alpha = 0.15f))
                        .border(1.dp, MiniPrimary.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Widgets,
                        contentDescription = null,
                        tint = MiniPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Choose Post for Widget",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = AppFontFamily
                    )
                    Text(
                        text = "Select an individual post to display in 4×2 widget",
                        color = MiniSecondaryText,
                        fontSize = 12.sp,
                        fontFamily = AppFontFamily
                    )
                }
            }

            IconButton(
                onClick = onCancel,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color(0x2EFFFFFF) else Color(0x14000000))
                    .border(
                        1.dp,
                        if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.05f),
                        CircleShape
                    )
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Cancel",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Search Box
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
                .liquidGlassEffect(shape = CircleShape, isDark = isDark, elevation = 2.dp)
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
                if (searchQuery.isEmpty()) {
                    Text(
                        text = "Filter posts…",
                        color = MiniSecondaryText,
                        fontSize = 14.sp,
                        fontFamily = AppFontFamily
                    )
                }
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    textStyle = TextStyle(
                        fontFamily = AppFontFamily,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 14.sp
                    ),
                    cursorBrush = SolidColor(MiniPrimary),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (searchQuery.isNotEmpty()) {
                IconButton(
                    onClick = { viewModel.updateSearchQuery("") },
                    modifier = Modifier.size(20.dp)
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

        Spacer(modifier = Modifier.height(6.dp))

        if (posts.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (searchQuery.isBlank()) "No saved posts found in library" else "No matching posts found",
                    color = MiniSecondaryText,
                    fontSize = 15.sp,
                    fontFamily = AppFontFamily
                )
                if (searchQuery.isBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onPlaceEmpty,
                        colors = ButtonDefaults.buttonColors(containerColor = MiniPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Add Widget (Select Post Later)", fontFamily = AppFontFamily, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp, start = 16.dp, end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(posts, key = { it.tweetId }) { post ->
                    val isSelected = post.tweetId == currentSelectedTweetId
                    WidgetPostSelectionCard(
                        post = post,
                        isSelected = isSelected,
                        onClick = { onPostSelected(post) }
                    )
                }
            }
        }
    }
}

@Composable
fun WidgetPostSelectionCard(
    post: Post,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val isDark = LocalIsDarkTheme.current

    LiquidGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        isDark = isDark,
        elevation = if (isSelected) 6.dp else 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                AuthorAvatar(
                    avatarUrl = post.author.avatarUrl ?: post.author.avatarRemoteUrl,
                    displayName = post.author.displayName,
                    modifier = Modifier.size(36.dp)
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
                        text = "@${post.author.handle} · ${formatRelativeTime(post.savedAtMs)}",
                        color = MiniSecondaryText,
                        fontSize = 12.sp,
                        fontFamily = AppFontFamily,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(MiniPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = post.text,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontFamily = AppFontFamily,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            if (post.mediaItems.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                val firstMedia = post.mediaItems.first()
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(firstMedia.localPath?.let { File(it) } ?: firstMedia.remoteUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
            }

            if (post.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    post.tags.take(3).forEach { tag ->
                        LiquidGlassPill(isDark = isDark) {
                            Text(
                                text = "#${tag.name}",
                                color = MiniPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = AppFontFamily
                            )
                        }
                    }
                }
            }
        }
    }
}

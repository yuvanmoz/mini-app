package com.mini.app.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mini.app.data.repository.ImportStep
import com.mini.app.data.repository.PostRepository
import com.mini.app.data.repository.VideoNotSupportedException
import com.mini.app.domain.model.Post
import com.mini.app.domain.model.Tag
import com.mini.app.ui.importpost.ImportUiState
import com.mini.app.util.XUrlParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class FeedFilterType {
    ALL,
    FAVORITES,
    MEDIA_ONLY,
    WITH_NOTES
}

enum class FeedSortOrder {
    NEWEST,
    OLDEST,
    ORIGINAL_DATE
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FeedViewModel @Inject constructor(
    private val repository: PostRepository
) : ViewModel() {

    private val _selectedTagFilter = MutableStateFlow<String?>(null)
    val selectedTagFilter: StateFlow<String?> = _selectedTagFilter.asStateFlow()

    private val _filterType = MutableStateFlow(FeedFilterType.ALL)
    val filterType: StateFlow<FeedFilterType> = _filterType.asStateFlow()

    private val _sortOrder = MutableStateFlow(FeedSortOrder.NEWEST)
    val sortOrder: StateFlow<FeedSortOrder> = _sortOrder.asStateFlow()

    private val _quickImportState = MutableStateFlow<ImportUiState>(ImportUiState.Idle)
    val quickImportState: StateFlow<ImportUiState> = _quickImportState.asStateFlow()

    private var recentlyDeletedPost: Post? = null

    val rawPosts: Flow<List<Post>> = _selectedTagFilter
        .flatMapLatest { tag ->
            if (tag.isNullOrBlank()) {
                repository.getAllPosts()
            } else {
                repository.getPostsForTag(tag)
            }
        }

    val posts: StateFlow<List<Post>> = combine(
        rawPosts,
        _filterType,
        _sortOrder
    ) { postList, filter, sort ->
        val filtered = when (filter) {
            FeedFilterType.ALL -> postList
            FeedFilterType.FAVORITES -> postList.filter { it.isFavorite }
            FeedFilterType.MEDIA_ONLY -> postList.filter { it.mediaItems.isNotEmpty() }
            FeedFilterType.WITH_NOTES -> postList.filter { !it.note.isNullOrBlank() }
        }

        when (sort) {
            FeedSortOrder.NEWEST -> filtered.sortedByDescending { it.savedAtMs }
            FeedSortOrder.OLDEST -> filtered.sortedBy { it.savedAtMs }
            FeedSortOrder.ORIGINAL_DATE -> filtered.sortedByDescending { it.originalDateMs }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val postCount: StateFlow<Int> = repository.getPostCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val favoriteCount: StateFlow<Int> = repository.getFavoriteCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val allTags: StateFlow<List<Tag>> = repository.getAllTags()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun setTagFilter(tagName: String?) {
        _selectedTagFilter.value = tagName
    }

    fun setFilterType(type: FeedFilterType) {
        _filterType.value = type
    }

    fun setSortOrder(order: FeedSortOrder) {
        _sortOrder.value = order
    }

    fun toggleFavorite(post: Post) {
        viewModelScope.launch {
            repository.toggleFavorite(post.tweetId, !post.isFavorite)
        }
    }

    fun updateNote(tweetId: String, note: String?) {
        viewModelScope.launch {
            repository.updateNote(tweetId, note?.trim()?.ifEmpty { null })
        }
    }

    fun deletePost(post: Post) {
        recentlyDeletedPost = post
        viewModelScope.launch {
            repository.deletePost(post.tweetId)
        }
    }

    fun undoDelete() {
        val postToRestore = recentlyDeletedPost ?: return
        viewModelScope.launch {
            repository.restorePost(postToRestore)
            recentlyDeletedPost = null
        }
    }

    fun updatePostTags(tweetId: String, tagNames: List<String>) {
        viewModelScope.launch {
            repository.setPostTags(tweetId, tagNames)
        }
    }

    fun createTag(name: String) {
        viewModelScope.launch {
            repository.createTag(name)
        }
    }

    fun startQuickImport(url: String) {
        viewModelScope.launch {
            _quickImportState.value = ImportUiState.Loading(ImportStep.RECEIVING, url)

            val parsedUrl = XUrlParser.parse(url)
            if (parsedUrl != null && repository.existsById(parsedUrl.statusId)) {
                _quickImportState.value = ImportUiState.Duplicate(parsedUrl.statusId)
                return@launch
            }

            val result = repository.fetchAndSavePost(url) { step ->
                _quickImportState.value = ImportUiState.Loading(step, url)
            }

            result.fold(
                onSuccess = { post ->
                    _quickImportState.value = ImportUiState.Success(post)
                },
                onFailure = { error ->
                    if (error is VideoNotSupportedException) {
                        _quickImportState.value = ImportUiState.VideoUnsupported(url)
                    } else {
                        _quickImportState.value = ImportUiState.Error(
                            error.message ?: "Something went wrong while retrieving the post.",
                            url
                        )
                    }
                }
            )
        }
    }

    fun clearQuickImportState() {
        _quickImportState.value = ImportUiState.Idle
    }
}

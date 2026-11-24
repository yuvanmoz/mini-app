package com.mini.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mini.app.data.repository.PostRepository
import com.mini.app.domain.model.Post
import com.mini.app.domain.model.Tag
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: PostRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val results: StateFlow<List<Post>> = _query
        .debounce(250)
        .flatMapLatest { q ->
            if (q.isBlank()) {
                flowOf(emptyList())
            } else {
                repository.searchPosts(q.trim())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allTags: StateFlow<List<Tag>> = repository.getAllTags()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun updateQuery(newQuery: String) {
        _query.value = newQuery
    }

    fun deletePost(post: Post) {
        viewModelScope.launch {
            repository.deletePost(post.tweetId)
        }
    }

    fun updatePostTags(tweetId: String, tagNames: List<String>) {
        viewModelScope.launch {
            repository.setPostTags(tweetId, tagNames)
        }
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

    fun createTag(name: String) {
        viewModelScope.launch {
            repository.createTag(name)
        }
    }
}

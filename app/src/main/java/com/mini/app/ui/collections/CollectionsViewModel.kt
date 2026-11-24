package com.mini.app.ui.collections

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mini.app.data.repository.PostRepository
import com.mini.app.domain.model.Tag
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class CollectionSortOrder {
    MOST_POSTS,
    ALPHABETICAL
}

@HiltViewModel
class CollectionsViewModel @Inject constructor(
    private val repository: PostRepository
) : ViewModel() {

    private val _sortOrder = MutableStateFlow(CollectionSortOrder.MOST_POSTS)
    val sortOrder: StateFlow<CollectionSortOrder> = _sortOrder.asStateFlow()

    val tags: StateFlow<List<Tag>> = combine(
        repository.getAllTags(),
        _sortOrder
    ) { tagsList, sort ->
        when (sort) {
            CollectionSortOrder.MOST_POSTS -> tagsList.sortedWith(
                compareByDescending<Tag> { it.postCount }.thenBy { it.name.lowercase() }
            )
            CollectionSortOrder.ALPHABETICAL -> tagsList.sortedBy { it.name.lowercase() }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val totalPostCount: StateFlow<Int> = repository.getPostCount()
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

    fun setSortOrder(order: CollectionSortOrder) {
        _sortOrder.value = order
    }

    fun createTag(name: String) {
        viewModelScope.launch {
            repository.createTag(name)
        }
    }

    fun renameTag(oldName: String, newName: String) {
        viewModelScope.launch {
            repository.renameTag(oldName, newName)
        }
    }

    fun deleteTag(name: String) {
        viewModelScope.launch {
            repository.deleteTag(name)
        }
    }
}

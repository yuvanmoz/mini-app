package com.mini.app.ui.importpost

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mini.app.data.repository.ImportStep
import com.mini.app.data.repository.PostRepository
import com.mini.app.data.repository.VideoNotSupportedException
import com.mini.app.domain.model.Post
import com.mini.app.domain.model.Tag
import com.mini.app.util.XUrlParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ImportUiState {
    object Idle : ImportUiState()
    data class Loading(val currentStep: ImportStep, val url: String) : ImportUiState()
    data class Success(val post: Post) : ImportUiState()
    data class Error(val message: String, val url: String) : ImportUiState()
    data class Duplicate(val tweetId: String) : ImportUiState()
    data class VideoUnsupported(val url: String) : ImportUiState()
}

@HiltViewModel
class ImportViewModel @Inject constructor(
    private val repository: PostRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ImportUiState>(ImportUiState.Idle)
    val uiState: StateFlow<ImportUiState> = _uiState.asStateFlow()

    val allTags: StateFlow<List<Tag>> = repository.getAllTags()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun startImport(url: String) {
        viewModelScope.launch {
            _uiState.value = ImportUiState.Loading(ImportStep.RECEIVING, url)

            val parsedUrl = XUrlParser.parse(url)
            if (parsedUrl != null && repository.existsById(parsedUrl.statusId)) {
                _uiState.value = ImportUiState.Duplicate(parsedUrl.statusId)
                return@launch
            }

            val result = repository.fetchAndSavePost(url) { step ->
                _uiState.value = ImportUiState.Loading(step, url)
            }

            result.fold(
                onSuccess = { post ->
                    _uiState.value = ImportUiState.Success(post)
                },
                onFailure = { error ->
                    if (error is VideoNotSupportedException) {
                        _uiState.value = ImportUiState.VideoUnsupported(url)
                    } else {
                        _uiState.value = ImportUiState.Error(
                            error.message ?: "Something went wrong while retrieving the post. Please try again.",
                            url
                        )
                    }
                }
            )
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
}

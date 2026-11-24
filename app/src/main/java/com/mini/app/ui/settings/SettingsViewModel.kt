package com.mini.app.ui.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mini.app.data.repository.PostRepository
import com.mini.app.ui.theme.AppTheme
import com.mini.app.util.WordExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val postRepository: PostRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val sharedPrefs by lazy {
        context.getSharedPreferences("mini_user_prefs", Context.MODE_PRIVATE)
    }

    companion object {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val FONT_SIZE = stringPreferencesKey("font_size")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
    }

    val themeMode: StateFlow<AppTheme> = dataStore.data.map { prefs ->
        val saved = prefs[THEME_MODE] ?: AppTheme.DARK.name
        try {
            AppTheme.valueOf(saved)
        } catch (e: Exception) {
            AppTheme.DARK
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppTheme.DARK)

    val dynamicColor: StateFlow<Boolean> = dataStore.data.map { prefs ->
        prefs[DYNAMIC_COLOR] ?: false
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val fontSize: StateFlow<String> = dataStore.data.map { prefs ->
        prefs[FONT_SIZE] ?: "Medium"
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Medium")

    val isOnboardingCompleted: StateFlow<Boolean> = dataStore.data.map { prefs ->
        prefs[ONBOARDING_COMPLETED] ?: sharedPrefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        sharedPrefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
    )

    data class StorageBreakdown(
        val savedMediaBytes: Long = 0L,
        val cacheBytes: Long = 0L
    ) {
        val totalBytes: Long get() = savedMediaBytes + cacheBytes
    }

    private val _storageBreakdown = kotlinx.coroutines.flow.MutableStateFlow(StorageBreakdown())
    val storageBreakdown: StateFlow<StorageBreakdown> = _storageBreakdown.asStateFlow()

    val storageBytes: StateFlow<Long> = _storageBreakdown.map { it.totalBytes }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    init {
        refreshStorageUsage()
    }

    fun refreshStorageUsage() {
        viewModelScope.launch {
            val info = postRepository.getStorageInfo()
            _storageBreakdown.value = StorageBreakdown(
                savedMediaBytes = info.savedMediaBytes,
                cacheBytes = info.cacheBytes
            )
        }
    }

    fun clearImageCache(onComplete: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val freed = postRepository.clearImageCache()
            refreshStorageUsage()
            onComplete(freed)
        }
    }

    fun setThemeMode(theme: AppTheme) {
        viewModelScope.launch {
            dataStore.edit { it[THEME_MODE] = theme.name }
        }
    }

    fun toggleDarkMode(isDark: Boolean) {
        setThemeMode(if (isDark) AppTheme.DARK else AppTheme.LIGHT)
    }

    fun setMatchSystemTheme(matchSystem: Boolean) {
        if (matchSystem) {
            setThemeMode(AppTheme.SYSTEM)
        } else {
            // Default to dark when disabling system mode
            setThemeMode(AppTheme.DARK)
        }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { it[DYNAMIC_COLOR] = enabled }
        }
    }

    fun setFontSize(size: String) {
        viewModelScope.launch {
            dataStore.edit { it[FONT_SIZE] = size }
        }
    }

    fun setFontScale(scale: Float) {
        val formatted = when {
            kotlin.math.abs(scale - 0.88f) < 0.03f -> "Small"
            kotlin.math.abs(scale - 1.00f) < 0.03f -> "Medium"
            kotlin.math.abs(scale - 1.15f) < 0.03f -> "Large"
            kotlin.math.abs(scale - 1.30f) < 0.03f -> "Extra Large"
            else -> String.format(java.util.Locale.US, "%.2f", scale)
        }
        setFontSize(formatted)
    }

    fun setOnboardingCompleted(completed: Boolean) {
        sharedPrefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply()
        viewModelScope.launch {
            dataStore.edit { it[ONBOARDING_COMPLETED] = completed }
        }
    }

    suspend fun exportToWord(context: Context): Result<File> {
        val posts = postRepository.getAllPosts().first()
        if (posts.isEmpty()) {
            return Result.failure(Exception("No saved posts to export"))
        }
        return WordExporter.exportToWord(context, posts)
    }

    suspend fun exportToMarkdown(): Result<File> {
        val posts = postRepository.getAllPosts().first()
        if (posts.isEmpty()) {
            return Result.failure(Exception("No saved posts to export"))
        }
        return postRepository.exportToMarkdown()
    }

    suspend fun exportBackupJson(): Result<File> {
        return postRepository.exportBackupJson()
    }

    suspend fun importBackupJson(jsonString: String): Result<Int> {
        return postRepository.importBackupJson(jsonString)
    }
}

package com.mini.app.domain.model

data class Post(
    val tweetId: String,
    val tweetUrl: String,
    val text: String,
    val author: Author,
    val mediaItems: List<MediaItem>,
    val originalDateMs: Long,
    val savedAtMs: Long,
    val collectionId: String = "uncategorized",
    val tags: List<Tag> = emptyList(),
    val isFavorite: Boolean = false,
    val note: String? = null
)

data class Author(
    val handle: String,
    val displayName: String,
    val avatarUrl: String?, // local path if downloaded, else remote URL
    val avatarRemoteUrl: String?
)

data class MediaItem(
    val id: Long,
    val remoteUrl: String,
    val localPath: String?, // local file path if downloaded
    val mediaType: String,
    val width: Int,
    val height: Int
)

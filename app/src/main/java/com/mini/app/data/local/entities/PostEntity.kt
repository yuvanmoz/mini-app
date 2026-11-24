package com.mini.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey
    val tweetId: String,
    val authorHandle: String,
    val text: String,
    val tweetUrl: String,
    val originalDateMs: Long,
    val savedAtMs: Long = System.currentTimeMillis(),
    val collectionId: String = "uncategorized",
    val isFavorite: Boolean = false,
    val note: String? = null
)

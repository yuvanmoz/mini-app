package com.mini.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "media_items")
data class MediaItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val postTweetId: String,
    val remoteUrl: String,
    val localPath: String? = null,
    val mediaType: String = "PHOTO", // "PHOTO" or "VIDEO"
    val width: Int = 0,
    val height: Int = 0
)

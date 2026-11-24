package com.mini.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "authors")
data class AuthorEntity(
    @PrimaryKey
    val handle: String,
    val displayName: String,
    val avatarRemoteUrl: String?,
    val localAvatarPath: String? = null
)

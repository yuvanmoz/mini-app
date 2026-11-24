package com.mini.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey
    val name: String,
    val createdAtMs: Long = System.currentTimeMillis()
)

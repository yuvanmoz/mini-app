package com.mini.app.domain.model

data class Tag(
    val name: String,
    val postCount: Int = 0,
    val previewImageUrl: String? = null
)

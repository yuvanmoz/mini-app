package com.mini.app.data.local.entities

import androidx.room.Entity

@Entity(
    tableName = "post_tag_cross_ref",
    primaryKeys = ["tweetId", "tagName"]
)
data class PostTagCrossRef(
    val tweetId: String,
    val tagName: String
)

package com.mini.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class FxTweetResponse(
    val code: Int,
    val message: String?,
    val tweet: FxTweet?
)

data class FxTweet(
    val id: String,
    val url: String,
    val text: String,
    val author: FxAuthor,
    val media: FxMedia?,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("created_timestamp") val createdTimestamp: Long?
)

data class FxAuthor(
    val id: String?,
    val name: String,
    @SerializedName("screen_name") val screenName: String,
    @SerializedName("avatar_url") val avatarUrl: String?,
    @SerializedName("banner_url") val bannerUrl: String?
)

data class FxMedia(
    val photos: List<FxPhoto>?,
    val videos: List<FxVideo>?,
    val all: List<FxMediaItem>?
)

data class FxPhoto(
    val url: String,
    val width: Int,
    val height: Int
)

data class FxVideo(
    val url: String,
    val width: Int,
    val height: Int,
    @SerializedName("thumbnail_url") val thumbnailUrl: String?
)

data class FxMediaItem(
    val type: String,
    val url: String
)

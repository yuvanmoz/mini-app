package com.mini.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class OEmbedResponse(
    val url: String?,
    @SerializedName("author_name") val authorName: String?,
    @SerializedName("author_url") val authorUrl: String?,
    val html: String?,
    val type: String?,
    val version: String?
)

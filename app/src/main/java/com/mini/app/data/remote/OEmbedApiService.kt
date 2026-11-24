package com.mini.app.data.remote

import com.mini.app.data.remote.dto.OEmbedResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface OEmbedApiService {
    @GET("oembed")
    suspend fun getOEmbed(
        @Query("url") url: String,
        @Query("omit_script") omitScript: Boolean = true
    ): Response<OEmbedResponse>
}

package com.mini.app.data.remote

import com.mini.app.data.remote.dto.FxTweetResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface FxTwitterApiService {
    @GET("{username}/status/{id}")
    suspend fun getTweet(
        @Path("username") username: String,
        @Path("id") id: String
    ): Response<FxTweetResponse>

    @GET("status/{id}")
    suspend fun getTweetById(
        @Path("id") id: String
    ): Response<FxTweetResponse>
}

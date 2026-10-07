package com.zara.challenge.data.remote

import com.zara.challenge.data.remote.dto.CharacterPageDto
import retrofit2.http.GET
import retrofit2.http.Query

interface ZaraChallengeApi {
    @GET("character")
    suspend fun getCharacters(
        @Query("page") page: Int,
        @Query("name") name: String? = null,
        @Query("status") status: String? = null,
        @Query("gender") gender: String? = null,
    ): CharacterPageDto
}

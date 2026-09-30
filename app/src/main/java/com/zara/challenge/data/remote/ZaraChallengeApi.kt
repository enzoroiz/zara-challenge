package com.zara.challenge.data.remote

import com.zara.challenge.data.remote.dto.CharacterDto
import com.zara.challenge.data.remote.dto.CharacterPageDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ZaraChallengeApi {
    @GET("character")
    suspend fun getCharacters(
        @Query("page") page: Int,
        @Query("name") name: String? = null,
        @Query("status") status: String? = null,
        @Query("type") type: String? = null,
        @Query("gender") gender: String? = null,
    ): CharacterPageDto

    @GET("character/{id}")
    suspend fun getCharacter(@Path("id") id: Int): CharacterDto
}

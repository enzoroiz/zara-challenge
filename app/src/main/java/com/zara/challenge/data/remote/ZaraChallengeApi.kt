package com.zara.challenge.data.remote

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

data class CharacterPageDto(val info: PageInfoDto, val results: List<CharacterDto>)
data class PageInfoDto(val count: Int, val pages: Int, val next: String?, val prev: String?)
data class CharacterDto(
    val id: Int,
    val name: String,
    val status: String,
    val species: String,
    val type: String,
    val gender: String,
    val origin: LocationDto,
    val location: LocationDto,
    val image: String,
    val episode: List<String>,
    val url: String,
    val created: String,
)
data class LocationDto(val name: String, val url: String)

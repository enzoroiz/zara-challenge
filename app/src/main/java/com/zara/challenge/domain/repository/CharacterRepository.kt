package com.zara.challenge.domain.repository

import com.zara.challenge.domain.model.Character
import com.zara.challenge.domain.model.CharacterFilters
import kotlinx.coroutines.flow.Flow

interface CharacterRepository {
    suspend fun getCharacters(
        page: Int,
        query: String,
        filters: CharacterFilters,
    ): Result<CharacterPage>
    fun observeFavorites(): Flow<List<Character>>
    fun observeFavoriteIds(): Flow<Set<Int>>
    suspend fun getCharacter(id: Int): Result<Character>
    suspend fun toggleFavorite(character: Character): Result<Unit>
    suspend fun getSimilarCharacters(character: Character): Result<List<Character>>
}

data class CharacterPage(val characters: List<Character>, val page: Int, val totalPages: Int)

package com.zara.challenge.data.repository

import com.zara.challenge.data.local.CharacterDao
import com.zara.challenge.data.local.toEntity as characterToEntity
import com.zara.challenge.data.remote.ZaraChallengeApi
import com.zara.challenge.data.remote.dto.toDomain
import com.zara.challenge.data.remote.dto.toEntity
import com.zara.challenge.domain.model.Character
import com.zara.challenge.domain.model.CharacterFilters
import com.zara.challenge.domain.model.CharacterPage
import com.zara.challenge.domain.repository.CharacterRepository
import android.util.Log
import com.zara.challenge.data.remote.dto.CharacterPageDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import javax.inject.Inject

class CharacterRepositoryImpl @Inject constructor(
    private val api: ZaraChallengeApi,
    private val dao: CharacterDao,
) : CharacterRepository {
    override suspend fun getCharacters(
        page: Int,
        query: String,
        filters: CharacterFilters,
    ): Result<CharacterPage> = runCatching {
        val response = api.getCharacters(
            page = page,
            name = query.takeIf { it.isNotBlank() },
            status = filters.status,
            gender = filters.gender,
        )
        val characters = response.results.map { it.toDomain() }
        upsertAll(response)
        CharacterPage(characters, page, response.info.pages)
    }.recoverCatching { error ->
        if (error is HttpException && error.code() == 404) {
            return@recoverCatching CharacterPage(emptyList(), page, 0)
        }
        if (page != 1) throw error
        val cached = dao.searchCharacters(
            query = query,
            status = filters.status,
            gender = filters.gender,
        ).map { it.toDomain() }
        if (cached.isEmpty()) throw error
        CharacterPage(cached, page = 1, totalPages = 1, isFromCache = true)
    }

    override suspend fun getCharacter(id: Int): Result<Character> = runCatching {
        requireNotNull(dao.getCharacter(id)) { "Character $id is not cached" }.toDomain()
    }

    override fun observeFavorites(): Flow<List<Character>> =
        dao.observeFavorites().map { characters -> characters.map { it.toDomain() } }

    override fun observeFavoriteIds(): Flow<Set<Int>> =
        dao.observeFavoriteIds().map { ids -> ids.toSet() }

    override suspend fun toggleFavorite(character: Character): Result<Unit> = runCatching {
        dao.toggleFavorite(character.characterToEntity())
    }

    override suspend fun getSimilarCharacters(character: Character): Result<List<Character>> =
        runCatching {
            val firstName = character.name.trim().substringBefore(' ')
            if (firstName.isBlank()) {
                emptyList()
            } else {
                dao.findByFirstName(firstName, character.id).map { it.toDomain() }
            }
        }

    private suspend fun upsertAll(response: CharacterPageDto) {
        try {
            dao.upsertAll(response.results.map { it.toEntity() })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("CharacterRepository", "Cache write failed", e)
        }
    }
}

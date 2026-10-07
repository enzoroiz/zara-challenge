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
    ): Result<CharacterPage> = try {
        val response = api.getCharacters(
            page = page,
            name = query.takeIf { it.isNotBlank() },
            status = filters.status,
            gender = filters.gender,
        )
        val characters = response.results.map { it.toDomain() }
        upsertAll(response)
        Result.success(CharacterPage(characters, page, response.info.pages))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        fallback(e, page, query, filters)
    }

    private suspend fun fallback(
        error: Exception,
        page: Int,
        query: String,
        filters: CharacterFilters,
    ): Result<CharacterPage> {
        if (error is HttpException && error.code() == 404) {
            return Result.success(CharacterPage(emptyList(), page, 0))
        }
        if (page != 1) return Result.failure(error)
        val cached = runCatchingCancellable {
            dao.searchCharacters(
                query = query,
                status = filters.status,
                gender = filters.gender,
            ).map { it.toDomain() }
        }.getOrDefault(emptyList())
        return if (cached.isEmpty()) {
            Result.failure(error)
        } else {
            Result.success(CharacterPage(cached, page = 1, totalPages = 1, isFromCache = true))
        }
    }

    override suspend fun getCharacter(id: Int): Result<Character> = runCatchingCancellable {
        requireNotNull(dao.getCharacter(id)) { "Character $id is not cached" }.toDomain()
    }

    override fun observeFavorites(): Flow<List<Character>> =
        dao.observeFavorites().map { characters -> characters.map { it.toDomain() } }

    override fun observeFavoriteIds(): Flow<Set<Int>> =
        dao.observeFavoriteIds().map { ids -> ids.toSet() }

    override suspend fun toggleFavorite(character: Character): Result<Unit> = runCatchingCancellable {
        dao.toggleFavorite(character.characterToEntity())
    }

    override suspend fun getSimilarCharacters(character: Character): Result<List<Character>> =
        runCatchingCancellable {
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

    private inline fun <T> runCatchingCancellable(block: () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
}

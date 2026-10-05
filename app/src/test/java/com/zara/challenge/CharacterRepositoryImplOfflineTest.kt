package com.zara.challenge

import com.zara.challenge.data.local.CharacterDao
import com.zara.challenge.data.remote.ZaraChallengeApi
import com.zara.challenge.data.remote.dto.CharacterPageDto
import com.zara.challenge.data.remote.dto.PageInfoDto
import com.zara.challenge.data.repository.CharacterRepositoryImpl
import com.zara.challenge.domain.model.CharacterFilters
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class CharacterRepositoryImplOfflineTest {
    private val api = mockk<ZaraChallengeApi>()
    private val dao = mockk<CharacterDao>(relaxed = true)
    private val repository = CharacterRepositoryImpl(api, dao)
    private val offline = IOException("offline")

    private fun http(code: Int) = HttpException(Response.error<Any>(code, "".toResponseBody()))

    @Test
    fun `blank query is not sent to the API and page metadata is returned`() = runTest {
        coEvery { api.getCharacters(3, null, null, null, null) } returns
            CharacterPageDto(PageInfoDto(40, 7, null, null), listOf(testDto()))

        val page = repository.getCharacters(3, "   ", CharacterFilters()).getOrThrow()

        assertEquals(3, page.page)
        assertEquals(7, page.totalPages)
        assertFalse(page.isFromCache)
    }

    @Test
    fun `first page falls back to the cache when the request fails`() = runTest {
        coEvery { api.getCharacters(any(), any(), any(), any(), any()) } throws offline
        coEvery { dao.searchCharacters("rick", "alive", "male") } returns listOf(testEntity())

        val page = repository.getCharacters(
            1, "rick", CharacterFilters(status = "alive", gender = "male"),
        ).getOrThrow()

        assertTrue(page.isFromCache)
        assertEquals(1, page.page)
        assertEquals(1, page.totalPages)
        assertEquals(listOf(testCharacter()), page.characters)
    }

    @Test
    fun `first page failure with an empty cache returns the original error`() = runTest {
        coEvery { api.getCharacters(any(), any(), any(), any(), any()) } throws offline
        coEvery { dao.searchCharacters(any(), any(), any()) } returns emptyList()

        val result = repository.getCharacters(1, "", CharacterFilters())

        assertSame(offline, result.exceptionOrNull())
    }

    @Test
    fun `later pages never fall back to the cache`() = runTest {
        coEvery { api.getCharacters(any(), any(), any(), any(), any()) } throws offline

        val result = repository.getCharacters(2, "", CharacterFilters())

        assertSame(offline, result.exceptionOrNull())
        coVerify(exactly = 0) { dao.searchCharacters(any(), any(), any()) }
    }

    @Test
    fun `404 on a later page is an empty last page`() = runTest {
        coEvery { api.getCharacters(any(), any(), any(), any(), any()) } throws http(404)

        val page = repository.getCharacters(2, "zzz", CharacterFilters()).getOrThrow()

        assertTrue(page.characters.isEmpty())
        assertEquals(2, page.page)
        assertEquals(0, page.totalPages)
    }

    @Test
    fun `non 404 http errors fall back to the cache on the first page`() = runTest {
        coEvery { api.getCharacters(any(), any(), any(), any(), any()) } throws http(500)
        coEvery { dao.searchCharacters("", null, null) } returns listOf(testEntity())

        assertTrue(repository.getCharacters(1, "", CharacterFilters()).getOrThrow().isFromCache)
    }

    @Test
    fun `non 404 http errors are reported when there is no cache`() = runTest {
        val error = http(429)
        coEvery { api.getCharacters(any(), any(), any(), any(), any()) } throws error
        coEvery { dao.searchCharacters(any(), any(), any()) } returns emptyList()

        assertSame(error, repository.getCharacters(1, "", CharacterFilters()).exceptionOrNull())
    }

    @Test
    fun `successful fetch is cached`() = runTest {
        coEvery { api.getCharacters(1, null, null, null, null) } returns
            CharacterPageDto(PageInfoDto(2, 1, null, null), listOf(testDto(1), testDto(2, "Morty")))

        repository.getCharacters(1, "", CharacterFilters())

        coVerify { dao.upsertAll(match { list -> list.map { it.id } == listOf(1, 2) }) }
    }

    @Test
    fun `cache write failure falls back to cached data`() = runTest {
        coEvery { api.getCharacters(any(), any(), any(), any(), any()) } returns
            CharacterPageDto(PageInfoDto(1, 1, null, null), listOf(testDto()))
        coEvery { dao.upsertAll(any()) } throws IllegalStateException("disk full")
        coEvery { dao.searchCharacters(any(), any(), any()) } returns listOf(testEntity())

        assertTrue(repository.getCharacters(1, "", CharacterFilters()).getOrThrow().isFromCache)
    }

    @Test
    fun `observe favorites maps entities to domain`() = runTest {
        every { dao.observeFavorites() } returns flowOf(listOf(testEntity(2, "Morty")))

        assertEquals(
            listOf(listOf(testCharacter(2, "Morty"))),
            repository.observeFavorites().toList(),
        )
    }

    @Test
    fun `observe favorite ids exposes a set`() = runTest {
        every { dao.observeFavoriteIds() } returns flowOf(listOf(1, 2, 2))

        assertEquals(listOf(setOf(1, 2)), repository.observeFavoriteIds().toList())
    }

    @Test
    fun `toggle favorite stores the entity`() = runTest {
        val result = repository.toggleFavorite(testCharacter())

        assertTrue(result.isSuccess)
        coVerify { dao.toggleFavorite(testEntity()) }
    }

    @Test
    fun `toggle favorite failure is returned as a failure`() = runTest {
        coEvery { dao.toggleFavorite(any()) } throws IllegalStateException("locked")

        assertEquals("locked", repository.toggleFavorite(testCharacter()).exceptionOrNull()?.message)
    }

    @Test
    fun `similar characters use the first name of the character`() = runTest {
        coEvery { dao.findByFirstName("Rick", 1) } returns listOf(testEntity(2, "Rick Prime"))

        val result = repository.getSimilarCharacters(testCharacter(1, "  Rick Sanchez ")).getOrThrow()

        assertEquals(listOf(testCharacter(2, "Rick Prime")), result)
    }

    @Test
    fun `similar characters for a blank name is empty without querying`() = runTest {
        val result = repository.getSimilarCharacters(testCharacter(1, "   ")).getOrThrow()

        assertTrue(result.isEmpty())
        coVerify(exactly = 0) { dao.findByFirstName(any(), any()) }
    }

    @Test
    fun `similar characters failure is returned as a failure`() = runTest {
        coEvery { dao.findByFirstName(any(), any()) } throws IllegalStateException("db")

        assertTrue(repository.getSimilarCharacters(testCharacter()).isFailure)
    }
}

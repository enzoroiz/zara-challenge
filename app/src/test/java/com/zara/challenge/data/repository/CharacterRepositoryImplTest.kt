package com.zara.challenge.data.repository

import com.zara.challenge.data.local.CharacterDao
import com.zara.challenge.data.remote.ZaraChallengeApi
import com.zara.challenge.data.remote.dto.CharacterPageDto
import com.zara.challenge.data.remote.dto.PageInfoDto
import com.zara.challenge.domain.model.CharacterFilters
import com.zara.challenge.ui.common.testCharacter
import com.zara.challenge.ui.common.testDto
import com.zara.challenge.ui.common.testEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
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

class CharacterRepositoryImplTest {
    private val api = mockk<ZaraChallengeApi>()
    private val dao = mockk<CharacterDao>(relaxed = true)
    private val repository = CharacterRepositoryImpl(api, dao)

    @Test
    fun `fetches filtered characters and caches the mapped results`() = runTest {
        coEvery { api.getCharacters(2, "rick", "alive", "male") } returns
            CharacterPageDto(PageInfoDto(2, 4, null, null), listOf(testDto()))

        val page = repository.getCharacters(
            page = 2,
            query = "rick",
            filters = CharacterFilters(status = "alive", gender = "male"),
        ).getOrThrow()

        assertEquals(listOf(testCharacter()), page.characters)
        assertEquals(2, page.page)
        assertEquals(4, page.totalPages)
        assertFalse(page.isFromCache)
        coVerify { dao.upsertAll(listOf(testEntity())) }
    }

    @Test
    fun `not found response returns an empty page`() = runTest {
        coEvery {
            api.getCharacters(any(), any(), any(), any())
        } throws HttpException(Response.error<Any>(404, "".toResponseBody()))

        val page = repository.getCharacters(1, "unknown", CharacterFilters()).getOrThrow()

        assertTrue(page.characters.isEmpty())
        assertEquals(0, page.totalPages)
    }

    @Test
    fun `first page uses cached results when the request fails`() = runTest {
        val offline = IOException("offline")
        coEvery { api.getCharacters(any(), any(), any(), any()) } throws offline
        coEvery { dao.searchCharacters("rick", "alive", "male") } returns listOf(testEntity())

        val page = repository.getCharacters(
            1, "rick", CharacterFilters(status = "alive", gender = "male"),
        ).getOrThrow()

        assertEquals(listOf(testCharacter()), page.characters)
        assertTrue(page.isFromCache)
    }

    @Test
    fun `first page request failure is returned when cache is empty`() = runTest {
        val offline = IOException("offline")
        coEvery { api.getCharacters(any(), any(), any(), any()) } throws offline
        coEvery { dao.searchCharacters(any(), any(), any()) } returns emptyList()

        val result = repository.getCharacters(1, "", CharacterFilters())

        assertSame(offline, result.exceptionOrNull())
    }

    @Test
    fun `request cancellation is propagated without reading cache`() = runTest {
        val cancellation = CancellationException("request cancelled")
        coEvery { api.getCharacters(any(), any(), any(), any()) } throws cancellation

        val thrown = try {
            repository.getCharacters(1, "", CharacterFilters())
            null
        } catch (error: CancellationException) {
            error
        }

        assertSame(cancellation, thrown)
        coVerify(exactly = 0) { dao.searchCharacters(any(), any(), any()) }
    }

    @Test
    fun `detail lookup returns the cached character`() = runTest {
        coEvery { dao.getCharacter(1) } returns testEntity()

        assertEquals(testCharacter(), repository.getCharacter(1).getOrThrow())
    }

    @Test
    fun `detail lookup fails when the character is not cached`() = runTest {
        coEvery { dao.getCharacter(1) } returns null

        val result = repository.getCharacter(1)

        assertEquals("Character 1 is not cached", result.exceptionOrNull()?.message)
    }

    @Test
    fun `favorite flows map characters and expose unique ids`() = runTest {
        every { dao.observeFavorites() } returns flowOf(listOf(testEntity(2, "Morty")))
        every { dao.observeFavoriteIds() } returns flowOf(listOf(1, 2, 2))

        assertEquals(
            listOf(listOf(testCharacter(2, "Morty"))),
            repository.observeFavorites().toList(),
        )
        assertEquals(listOf(setOf(1, 2)), repository.observeFavoriteIds().toList())
    }

    @Test
    fun `toggling a favorite stores its entity`() = runTest {
        repository.toggleFavorite(testCharacter()).getOrThrow()

        coVerify { dao.toggleFavorite(testEntity()) }
    }

    @Test
    fun `favorite update failure is returned`() = runTest {
        coEvery { dao.toggleFavorite(any()) } throws IllegalStateException("locked")

        val result = repository.toggleFavorite(testCharacter())

        assertEquals("locked", result.exceptionOrNull()?.message)
    }

    @Test
    fun `similar characters use the trimmed first name`() = runTest {
        coEvery { dao.findByFirstName("Rick", 1) } returns listOf(testEntity(2, "Rick Prime"))

        val result = repository.getSimilarCharacters(testCharacter(1, "  Rick Sanchez ")).getOrThrow()

        assertEquals(listOf(testCharacter(2, "Rick Prime")), result)
    }
}

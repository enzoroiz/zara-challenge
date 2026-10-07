package com.zara.challenge

import android.util.Log
import com.zara.challenge.data.local.CharacterDao
import com.zara.challenge.data.local.CharacterEntity
import com.zara.challenge.data.remote.ZaraChallengeApi
import com.zara.challenge.data.remote.dto.CharacterDto
import com.zara.challenge.data.remote.dto.CharacterPageDto
import com.zara.challenge.data.remote.dto.LocationDto
import com.zara.challenge.data.remote.dto.PageInfoDto
import com.zara.challenge.data.repository.CharacterRepositoryImpl
import com.zara.challenge.domain.model.CharacterFilters
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import java.io.IOException
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class CharacterRepositoryImplTest {
    private val api = mockk<ZaraChallengeApi>()
    private val dao = mockk<CharacterDao>(relaxed = true)
    private val repository = CharacterRepositoryImpl(api, dao)
    private val offline = IOException("offline")

    @Before
    fun mockAndroidLog() {
        mockkStatic(Log::class)
        every { Log.w(any<String>(), any<String>(), any<Throwable>()) } returns 0
    }

    @After
    fun unmockAndroidLog() = unmockkStatic(Log::class)

    private fun http(code: Int) = HttpException(Response.error<Any>(code, "".toResponseBody()))

    @Test
    fun `list request forwards supported filters and caches results`() = runTest {
        val dto = characterDto()
        coEvery {
            api.getCharacters(
                page = 1,
                name = "rick",
                status = "alive",
                gender = "male",
            )
        } returns CharacterPageDto(PageInfoDto(1, 1, null, null), listOf(dto))

        val result = repository.getCharacters(
            page = 1,
            query = "rick",
            filters = CharacterFilters(status = "alive", gender = "male"),
        )

        assertTrue(result.isSuccess)
        assertEquals("Rick Sanchez", result.getOrThrow().characters.single().name)
        coVerify { dao.upsertAll(match { it.single().id == 1 }) }
    }

    @Test
    fun `404 from filtered list is treated as an empty result`() = runTest {
        val notFound = mockk<HttpException>()
        every { notFound.code() } returns 404
        coEvery {
            api.getCharacters(
                page = 1,
                name = null,
                status = null,
                gender = null,
            )
        } throws notFound

        val result = repository.getCharacters(1, "", CharacterFilters())

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().characters.isEmpty())
        assertEquals(0, result.getOrThrow().totalPages)
    }

    @Test
    fun `detail is retrieved from the database without making an API request`() = runTest {
        coEvery { dao.getCharacter(1) } returns characterEntity()

        val result = repository.getCharacter(1)

        assertTrue(result.isSuccess)
        assertEquals("Rick Sanchez", result.getOrThrow().name)
        coVerify { dao.getCharacter(1) }
        coVerify(exactly = 0) {
            api.getCharacters(any(), any(), any(), any())
        }
    }

    @Test
    fun `missing cached detail is a failure`() = runTest {
        coEvery { dao.getCharacter(1) } returns null

        val result = repository.getCharacter(1)

        assertTrue(result.isFailure)
        assertEquals("Character 1 is not cached", result.exceptionOrNull()?.message)
    }

    @Test
    fun `database detail read failure is returned as a failure`() = runTest {
        coEvery { dao.getCharacter(1) } throws IllegalStateException("Database unavailable")

        val result = repository.getCharacter(1)

        assertTrue(result.isFailure)
        assertEquals("Database unavailable", result.exceptionOrNull()?.message)
    }

    @Test
    fun `blank query is not sent to the API and page metadata is returned`() = runTest {
        coEvery { api.getCharacters(3, null, null, null) } returns
            CharacterPageDto(PageInfoDto(40, 7, null, null), listOf(testDto()))

        val page = repository.getCharacters(3, "   ", CharacterFilters()).getOrThrow()

        assertEquals(3, page.page)
        assertEquals(7, page.totalPages)
        assertFalse(page.isFromCache)
    }

    @Test
    fun `first page falls back to the cache when the request fails`() = runTest {
        coEvery { api.getCharacters(any(), any(), any(), any()) } throws offline
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
        coEvery { api.getCharacters(any(), any(), any(), any()) } throws offline
        coEvery { dao.searchCharacters(any(), any(), any()) } returns emptyList()

        val result = repository.getCharacters(1, "", CharacterFilters())

        assertSame(offline, result.exceptionOrNull())
    }

    @Test
    fun `later pages never fall back to the cache`() = runTest {
        coEvery { api.getCharacters(any(), any(), any(), any()) } throws offline

        val result = repository.getCharacters(2, "", CharacterFilters())

        assertSame(offline, result.exceptionOrNull())
        coVerify(exactly = 0) { dao.searchCharacters(any(), any(), any()) }
    }

    @Test
    fun `404 on a later page is an empty last page`() = runTest {
        coEvery { api.getCharacters(any(), any(), any(), any()) } throws http(404)

        val page = repository.getCharacters(2, "zzz", CharacterFilters()).getOrThrow()

        assertTrue(page.characters.isEmpty())
        assertEquals(2, page.page)
        assertEquals(0, page.totalPages)
    }

    @Test
    fun `non 404 http errors fall back to the cache on the first page`() = runTest {
        coEvery { api.getCharacters(any(), any(), any(), any()) } throws http(500)
        coEvery { dao.searchCharacters("", null, null) } returns listOf(testEntity())

        assertTrue(repository.getCharacters(1, "", CharacterFilters()).getOrThrow().isFromCache)
    }

    @Test
    fun `non 404 http errors are reported when there is no cache`() = runTest {
        val error = http(429)
        coEvery { api.getCharacters(any(), any(), any(), any()) } throws error
        coEvery { dao.searchCharacters(any(), any(), any()) } returns emptyList()

        assertSame(error, repository.getCharacters(1, "", CharacterFilters()).exceptionOrNull())
    }

    @Test
    fun `successful fetch is cached`() = runTest {
        coEvery { api.getCharacters(1, null, null, null) } returns
            CharacterPageDto(PageInfoDto(2, 1, null, null), listOf(testDto(1), testDto(2, "Morty")))

        repository.getCharacters(1, "", CharacterFilters())

        coVerify { dao.upsertAll(match { list -> list.map { it.id } == listOf(1, 2) }) }
    }

    @Test
    fun `cache write failure still returns the network result`() = runTest {
        coEvery { api.getCharacters(any(), any(), any(), any()) } returns
            CharacterPageDto(PageInfoDto(1, 3, null, null), listOf(testDto()))
        coEvery { dao.upsertAll(any()) } throws IllegalStateException("disk full")

        val page = repository.getCharacters(1, "", CharacterFilters()).getOrThrow()

        assertFalse(page.isFromCache)
        assertEquals(listOf(testCharacter()), page.characters)
        assertEquals(3, page.totalPages)
        coVerify(exactly = 0) { dao.searchCharacters(any(), any(), any()) }
    }

    @Test
    fun `cache fallback read failure returns the original error as a failure`() = runTest {
        coEvery { api.getCharacters(any(), any(), any(), any()) } throws offline
        coEvery { dao.searchCharacters(any(), any(), any()) } throws IllegalStateException("db")

        val result = repository.getCharacters(1, "", CharacterFilters())

        assertTrue(result.isFailure)
        assertSame(offline, result.exceptionOrNull())
    }

    @Test
    fun `404 on the first page does not read the cache`() = runTest {
        coEvery { api.getCharacters(any(), any(), any(), any()) } throws http(404)

        val page = repository.getCharacters(1, "zzz", CharacterFilters()).getOrThrow()

        assertTrue(page.characters.isEmpty())
        assertFalse(page.isFromCache)
        coVerify(exactly = 0) { dao.searchCharacters(any(), any(), any()) }
    }

    @Test
    fun `later page failure does not read the cache`() = runTest {
        coEvery { api.getCharacters(any(), any(), any(), any()) } throws offline

        val result = repository.getCharacters(2, "", CharacterFilters())

        assertSame(offline, result.exceptionOrNull())
        coVerify(exactly = 0) { dao.searchCharacters(any(), any(), any()) }
    }

    @Test(expected = OutOfMemoryError::class)
    fun `errors that are not exceptions are not swallowed`() = runTest {
        coEvery { api.getCharacters(any(), any(), any(), any()) } throws OutOfMemoryError()

        repository.getCharacters(1, "", CharacterFilters())
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

    private fun characterDto() = CharacterDto(
        id = 1,
        name = "Rick Sanchez",
        status = "Alive",
        species = "Human",
        type = "",
        gender = "Male",
        origin = LocationDto("Earth", ""),
        location = LocationDto("Earth", ""),
        image = "image",
        episode = emptyList(),
        url = "",
        created = "",
    )

    private fun characterEntity() = CharacterEntity(
        id = 1,
        name = "Rick Sanchez",
        status = "Alive",
        species = "Human",
        type = "",
        gender = "Male",
        originName = "Earth",
        locationName = "Earth",
        image = "image",
        episodeCount = 1,
        created = "",
    )
}

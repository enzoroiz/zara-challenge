package com.zara.challenge

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
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException

class CharacterRepositoryImplTest {
    private val api = mockk<ZaraChallengeApi>()
    private val dao = mockk<CharacterDao>(relaxed = true)
    private val repository = CharacterRepositoryImpl(api, dao)

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

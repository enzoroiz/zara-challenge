package com.zara.challenge

import com.zara.challenge.data.local.CharacterDao
import com.zara.challenge.data.remote.CharacterDto
import com.zara.challenge.data.remote.CharacterPageDto
import com.zara.challenge.data.remote.LocationDto
import com.zara.challenge.data.remote.PageInfoDto
import com.zara.challenge.data.remote.ZaraChallengeApi
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
                type = null,
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
                type = null,
                gender = null,
            )
        } throws notFound

        val result = repository.getCharacters(1, "", CharacterFilters())

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().characters.isEmpty())
        assertEquals(0, result.getOrThrow().totalPages)
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
}

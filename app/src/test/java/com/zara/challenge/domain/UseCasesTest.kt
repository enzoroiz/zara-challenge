package com.zara.challenge.domain

import com.zara.challenge.domain.model.CharacterFilters
import com.zara.challenge.domain.model.CharacterPage
import com.zara.challenge.domain.repository.CharacterRepository
import com.zara.challenge.domain.usecase.GetCharacterUseCase
import com.zara.challenge.domain.usecase.GetCharactersUseCase
import com.zara.challenge.domain.usecase.GetSimilarCharactersUseCase
import com.zara.challenge.domain.usecase.ObserveFavoriteIdsUseCase
import com.zara.challenge.domain.usecase.ObserveFavoritesUseCase
import com.zara.challenge.domain.usecase.ToggleFavoriteUseCase
import com.zara.challenge.ui.common.testCharacter
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class UseCasesTest {
    private val repository = mockk<CharacterRepository>()
    private val rick = testCharacter()

    @Test
    fun `get characters forwards page query and filters`() = runTest {
        val filters = CharacterFilters(status = "alive")
        val page = Result.success(CharacterPage(listOf(rick), 2, 5))

        coEvery { repository.getCharacters(2, "rick", filters) } returns page

        assertEquals(page, GetCharactersUseCase(repository)(2, "rick", filters))
    }

    @Test
    fun `get characters defaults to empty filters`() = runTest {
        coEvery { repository.getCharacters(1, "", CharacterFilters()) } returns
            Result.success(CharacterPage(emptyList(), 1, 1))

        GetCharactersUseCase(repository)(1, "")

        coVerify { repository.getCharacters(1, "", CharacterFilters()) }
    }

    @Test
    fun `character detail and recommendations delegate to the repository`() = runTest {
        val morty = testCharacter(2, "Rick Prime")
        coEvery { repository.getCharacter(1) } returns Result.success(rick)
        coEvery { repository.getSimilarCharacters(rick) } returns Result.success(listOf(morty))

        assertEquals(rick, GetCharacterUseCase(repository)(1).getOrThrow())
        assertEquals(listOf(morty), GetSimilarCharactersUseCase(repository)(rick).getOrThrow())
    }

    @Test
    fun `toggle favorite propagates the repository result`() = runTest {
        val failure = Result.failure<Unit>(IllegalStateException("boom"))
        coEvery { repository.toggleFavorite(rick) } returns failure

        assertEquals(failure, ToggleFavoriteUseCase(repository)(rick))
    }

    @Test
    fun `favorite use cases expose the repository flows`() = runTest {
        every { repository.observeFavorites() } returns flowOf(listOf(rick))
        every { repository.observeFavoriteIds() } returns flowOf(setOf(1, 2))

        assertEquals(listOf(listOf(rick)), ObserveFavoritesUseCase(repository)().toList())
        assertEquals(listOf(setOf(1, 2)), ObserveFavoriteIdsUseCase(repository)().toList())
    }
}

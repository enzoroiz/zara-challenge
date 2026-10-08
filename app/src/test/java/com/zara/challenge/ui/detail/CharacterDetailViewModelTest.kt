package com.zara.challenge.ui.detail

import androidx.lifecycle.SavedStateHandle
import com.zara.challenge.testCharacter
import com.zara.challenge.domain.usecase.GetCharacterUseCase
import com.zara.challenge.domain.usecase.GetSimilarCharactersUseCase
import com.zara.challenge.domain.usecase.ObserveFavoriteIdsUseCase
import com.zara.challenge.domain.usecase.ToggleFavoriteUseCase
import com.zara.challenge.ui.detail.CharacterDetailUiState
import com.zara.challenge.ui.detail.CharacterDetailViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CharacterDetailViewModelTest {
    private val getCharacter = mockk<GetCharacterUseCase>()
    private val getSimilarCharacters = mockk<GetSimilarCharactersUseCase>()
    private val observeFavoriteIds = mockk<ObserveFavoriteIdsUseCase>()
    private val toggleFavorite = mockk<ToggleFavoriteUseCase>()

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `database lookup failure makes detail information unavailable`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        every { observeFavoriteIds() } returns flowOf(emptySet())
        coEvery { getCharacter(1) } returns Result.failure(IllegalStateException("Not cached"))

        val viewModel = CharacterDetailViewModel(
            SavedStateHandle(mapOf("characterId" to 1)),
            getCharacter,
            getSimilarCharacters,
            observeFavoriteIds,
            toggleFavorite,
        )
        advanceUntilIdle()

        assertEquals(CharacterDetailUiState.Unavailable, viewModel.uiState.value)
    }
}

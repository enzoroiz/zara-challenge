package com.zara.challenge

import app.cash.turbine.test
import com.zara.challenge.domain.model.Character
import com.zara.challenge.domain.model.CharacterFilters
import com.zara.challenge.domain.repository.CharacterPage
import com.zara.challenge.domain.usecase.GetCharactersUseCase
import com.zara.challenge.domain.usecase.ObserveFavoriteIdsUseCase
import com.zara.challenge.domain.usecase.ToggleFavoriteUseCase
import com.zara.challenge.ui.list.CharacterListViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@OptIn(ExperimentalCoroutinesApi::class)
class CharacterListViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val useCase = mockk<GetCharactersUseCase>()
    private val observeFavoriteIds = mockk<ObserveFavoriteIdsUseCase>()
    private val toggleFavorite = mockk<ToggleFavoriteUseCase>()

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { observeFavoriteIds() } returns flowOf(emptySet())
    }
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `initial load exposes characters`() = runTest {
        val rick = Character(1, "Rick Sanchez", "Alive", "Human", "", "Male", "Earth", "Earth", "image", 51, "created")
        coEvery { useCase(1, "", CharacterFilters()) } returns
            Result.success(CharacterPage(listOf(rick), 1, 1))

        val vm = CharacterListViewModel(useCase, observeFavoriteIds, toggleFavorite)
        dispatcher.scheduler.advanceUntilIdle()

        vm.uiState.test {
            val state = awaitItem()
            assertEquals("Rick Sanchez", state.characters.single().name)
            assertFalse(state.isLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `changing filters reloads the first page with selected criteria`() = runTest {
        val initial = Character(1, "Rick Sanchez", "Alive", "Human", "", "Male", "Earth", "Earth", "image", 51, "created")
        val filtered = Character(2, "Morty Smith", "Alive", "Human", "", "Male", "Earth", "Earth", "image", 51, "created")
        val filters = CharacterFilters(status = "alive", species = "Human")
        coEvery { useCase(1, "", CharacterFilters()) } returns
            Result.success(CharacterPage(listOf(initial), 1, 2))
        coEvery { useCase(1, "", filters) } returns
            Result.success(CharacterPage(listOf(filtered), 1, 1))

        val vm = CharacterListViewModel(useCase, observeFavoriteIds, toggleFavorite)
        dispatcher.scheduler.advanceUntilIdle()
        vm.onFiltersChanged(filters)
        dispatcher.scheduler.advanceUntilIdle()

        vm.uiState.test {
            val state = awaitItem()
            assertEquals(filters, state.filters)
            assertEquals("Morty Smith", state.characters.single().name)
            assertFalse(state.isLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }
}

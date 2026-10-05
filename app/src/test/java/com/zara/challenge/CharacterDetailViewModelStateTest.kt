package com.zara.challenge

import androidx.lifecycle.SavedStateHandle
import com.zara.challenge.domain.usecase.GetCharacterUseCase
import com.zara.challenge.domain.usecase.GetSimilarCharactersUseCase
import com.zara.challenge.domain.usecase.ObserveFavoriteIdsUseCase
import com.zara.challenge.domain.usecase.ToggleFavoriteUseCase
import com.zara.challenge.ui.detail.CharacterDetailUiState
import com.zara.challenge.ui.detail.CharacterDetailViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CharacterDetailViewModelStateTest {
    private val getCharacter = mockk<GetCharacterUseCase>()
    private val getSimilarCharacters = mockk<GetSimilarCharactersUseCase>()
    private val observeFavoriteIds = mockk<ObserveFavoriteIdsUseCase>()
    private val toggleFavorite = mockk<ToggleFavoriteUseCase>()
    private val favoriteIds = MutableStateFlow<Set<Int>>(emptySet())
    private val rick = testCharacter()
    private val prime = testCharacter(2, "Rick Prime")

    @Before
    fun setUp() {
        every { observeFavoriteIds() } returns favoriteIds
        coEvery { getCharacter(1) } returns Result.success(rick)
        coEvery { getSimilarCharacters(rick) } returns Result.success(listOf(prime))
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.createViewModel(): CharacterDetailViewModel {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        return CharacterDetailViewModel(
            SavedStateHandle(mapOf("characterId" to 1)),
            getCharacter,
            getSimilarCharacters,
            observeFavoriteIds,
            toggleFavorite,
        )
    }

    @Test
    fun `starts in loading state`() = runTest {
        val vm = createViewModel()

        assertEquals(CharacterDetailUiState.Loading, vm.uiState.value)
    }

    @Test
    fun `loads the character with similar characters`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        val state = vm.uiState.value as CharacterDetailUiState.Success
        assertEquals(rick, state.character)
        assertEquals(listOf(prime), state.similarCharacters)
        assertFalse(state.isFavorite)
        assertNull(state.recommendationError)
    }

    @Test
    fun `reflects an already favorited character`() = runTest {
        favoriteIds.value = setOf(1)
        val vm = createViewModel()
        advanceUntilIdle()

        val state = vm.uiState.value as CharacterDetailUiState.Success
        assertTrue(state.isFavorite)
        assertEquals(setOf(1), state.favoriteIds)
    }

    @Test
    fun `favorite changes update the loaded state`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        favoriteIds.value = setOf(1, 2)
        advanceUntilIdle()

        val state = vm.uiState.value as CharacterDetailUiState.Success
        assertTrue(state.isFavorite)
        assertEquals(setOf(1, 2), state.favoriteIds)

        favoriteIds.value = setOf(2)
        advanceUntilIdle()
        assertFalse((vm.uiState.value as CharacterDetailUiState.Success).isFavorite)
    }

    @Test
    fun `favorite changes do not affect an unavailable state`() = runTest {
        coEvery { getCharacter(1) } returns Result.failure(IllegalStateException("missing"))
        val vm = createViewModel()
        advanceUntilIdle()

        favoriteIds.value = setOf(1)
        advanceUntilIdle()

        assertEquals(CharacterDetailUiState.Unavailable, vm.uiState.value)
    }

    @Test
    fun `recommendation failure keeps the character and exposes the error`() = runTest {
        coEvery { getSimilarCharacters(rick) } returns Result.failure(IllegalStateException("db down"))
        val vm = createViewModel()
        advanceUntilIdle()

        val state = vm.uiState.value as CharacterDetailUiState.Success
        assertEquals(rick, state.character)
        assertEquals("db down", state.recommendationError)
        assertTrue(state.similarCharacters.isEmpty())
    }

    @Test
    fun `recommendation failure without a message uses a default`() = runTest {
        coEvery { getSimilarCharacters(rick) } returns Result.failure(RuntimeException())
        val vm = createViewModel()
        advanceUntilIdle()

        val state = vm.uiState.value as CharacterDetailUiState.Success
        assertEquals("Unable to load recommendations", state.recommendationError)
    }

    @Test
    fun `load retries after being unavailable`() = runTest {
        coEvery { getCharacter(1) } returns Result.failure(IllegalStateException("missing"))
        val vm = createViewModel()
        advanceUntilIdle()
        assertEquals(CharacterDetailUiState.Unavailable, vm.uiState.value)

        coEvery { getCharacter(1) } returns Result.success(rick)
        vm.load()
        advanceUntilIdle()

        assertTrue(vm.uiState.value is CharacterDetailUiState.Success)
    }

    @Test
    fun `toggle favorite uses the loaded character`() = runTest {
        coEvery { toggleFavorite(rick) } returns Result.success(Unit)
        val vm = createViewModel()
        advanceUntilIdle()

        vm.toggleFavorite()
        advanceUntilIdle()

        coVerify(exactly = 1) { toggleFavorite(rick) }
    }

    @Test
    fun `toggle favorite before the character is loaded does nothing`() = runTest {
        val vm = createViewModel()

        vm.toggleFavorite()
        advanceUntilIdle()

        coVerify(exactly = 0) { toggleFavorite(any()) }
    }

    @Test
    fun `toggling a similar character favorites that character`() = runTest {
        coEvery { toggleFavorite(prime) } returns Result.success(Unit)
        val vm = createViewModel()
        advanceUntilIdle()

        vm.toggleFavorite(prime)
        advanceUntilIdle()

        coVerify { toggleFavorite(prime) }
    }

    @Test
    fun `toggle favorite failure emits one error`() = runTest {
        coEvery { toggleFavorite(rick) } returns Result.failure(IllegalStateException("x"))
        val vm = createViewModel()
        advanceUntilIdle()

        vm.toggleFavorite(rick)
        advanceUntilIdle()

        assertEquals(
            "Couldn't update your favourites. Please try again.",
            vm.errorEvents.first(),
        )
        assertNull(withTimeoutOrNull(1) { vm.errorEvents.first() })
    }
}

package com.zara.challenge.ui.favorites

import com.zara.challenge.ui.common.UiText
import com.zara.challenge.R
import com.zara.challenge.domain.usecase.ObserveFavoritesUseCase
import com.zara.challenge.domain.usecase.ToggleFavoriteUseCase
import com.zara.challenge.testCharacter
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
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
class FavoritesViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val observeFavorites = mockk<ObserveFavoritesUseCase>()
    private val toggleFavorite = mockk<ToggleFavoriteUseCase>()
    private val rick = testCharacter()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `starts loading before favorites arrive`() = runTest {
        every { observeFavorites() } returns MutableSharedFlow()

        val vm = FavoritesViewModel(observeFavorites, toggleFavorite)
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value.isLoading)
        assertTrue(vm.uiState.value.characters.isEmpty())
    }

    @Test
    fun `exposes favorites and keeps updating`() = runTest {
        val source = MutableSharedFlow<List<com.zara.challenge.domain.model.Character>>()
        every { observeFavorites() } returns source
        val vm = FavoritesViewModel(observeFavorites, toggleFavorite)
        dispatcher.scheduler.advanceUntilIdle()

        source.emit(listOf(rick))
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(listOf(rick), vm.uiState.value.characters)
        assertFalse(vm.uiState.value.isLoading)
        assertNull(vm.uiState.value.error)

        source.emit(emptyList())
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.uiState.value.characters.isEmpty())
    }

    @Test
    fun `observation failure exposes the error message`() = runTest {
        every { observeFavorites() } returns flow { throw IllegalStateException("db") }

        val vm = FavoritesViewModel(observeFavorites, toggleFavorite)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(UiText.Plain("db"), vm.uiState.value.error)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `observation failure without a message uses a default`() = runTest {
        every { observeFavorites() } returns flow { throw RuntimeException() }

        val vm = FavoritesViewModel(observeFavorites, toggleFavorite)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(UiText.Res(R.string.favorites_load_error), vm.uiState.value.error)
    }

    @Test
    fun `removing a favorite toggles it`() = runTest {
        every { observeFavorites() } returns MutableSharedFlow()
        coEvery { toggleFavorite(rick) } returns Result.success(Unit)
        val vm = FavoritesViewModel(observeFavorites, toggleFavorite)

        vm.removeFavorite(rick)
        dispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { toggleFavorite(rick) }
        assertNull(withTimeoutOrNull(1) { vm.errorEvents.first() })
    }

    @Test
    fun `removing a favorite failure emits an error`() = runTest {
        every { observeFavorites() } returns MutableSharedFlow()
        coEvery { toggleFavorite(rick) } returns Result.failure(IllegalStateException("x"))
        val vm = FavoritesViewModel(observeFavorites, toggleFavorite)

        vm.removeFavorite(rick)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            UiText.Res(R.string.favorites_update_error),
            vm.errorEvents.first(),
        )
    }
}

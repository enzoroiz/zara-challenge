package com.zara.challenge.ui.list

import com.zara.challenge.R
import com.zara.challenge.testCharacter
import com.zara.challenge.ui.common.UiText
import com.zara.challenge.domain.model.CharacterFilters
import com.zara.challenge.domain.model.CharacterPage
import com.zara.challenge.domain.usecase.GetCharactersUseCase
import com.zara.challenge.domain.usecase.ObserveFavoriteIdsUseCase
import com.zara.challenge.domain.usecase.ToggleFavoriteUseCase
import com.zara.challenge.ui.list.CharacterListViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
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
        val rick = testCharacter(locationName = "Earth", image = "image", episodeCount = 51)
        coEvery { useCase(1, "", CharacterFilters()) } returns
            Result.success(CharacterPage(listOf(rick), 1, 1))

        val vm = CharacterListViewModel(useCase, observeFavoriteIds, toggleFavorite)
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals("Rick Sanchez", state.characters.single().name)
        assertFalse(state.isLoading)
    }

    @Test
    fun `changing filters reloads the first page with selected criteria`() = runTest {
        val initial = testCharacter(locationName = "Earth", image = "image", episodeCount = 51)
        val filtered = testCharacter(
            2, "Morty Smith", locationName = "Earth", image = "image", episodeCount = 51,
        )
        val filters = CharacterFilters(status = "alive", gender = "male")
        coEvery { useCase(1, "", CharacterFilters()) } returns
            Result.success(CharacterPage(listOf(initial), 1, 2))
        coEvery { useCase(1, "", filters) } returns
            Result.success(CharacterPage(listOf(filtered), 1, 1))

        val vm = CharacterListViewModel(useCase, observeFavoriteIds, toggleFavorite)
        dispatcher.scheduler.advanceUntilIdle()
        vm.onFiltersChanged(filters)
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(filters, state.filters)
        assertEquals("Morty Smith", state.characters.single().name)
        assertFalse(state.isLoading)
    }

    @Test
    fun `initial request failure emits one error and leaves a retryable error state`() = runTest {
        coEvery { useCase(1, "", CharacterFilters()) } returns
            Result.failure(IllegalStateException("Request failed"))

        val vm = CharacterListViewModel(useCase, observeFavoriteIds, toggleFavorite)
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.uiState.value.isLoading)
        assertTrue(vm.uiState.value.hasLoadError)
        assertEquals(UiText.Res(R.string.error_generic), vm.errorEvents.first())
        assertNull(withTimeoutOrNull(1) { vm.errorEvents.first() })
    }

    @Test
    fun `initial load reports loading until the request completes`() = runTest {
        val result = CompletableDeferred<Result<CharacterPage>>()
        coEvery { useCase(1, "", CharacterFilters()) } coAnswers { result.await() }

        val vm = CharacterListViewModel(useCase, observeFavoriteIds, toggleFavorite)
        dispatcher.scheduler.runCurrent()
        assertTrue(vm.uiState.value.isLoading)

        result.complete(Result.success(CharacterPage(emptyList(), 1, 1)))
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.uiState.value.isLoading)
        assertFalse(vm.uiState.value.hasLoadError)
    }

    @Test
    fun `load more failure preserves existing characters and emits one error`() = runTest {
        val rick = testCharacter(locationName = "Earth", image = "image", episodeCount = 51)
        coEvery { useCase(1, "", CharacterFilters()) } returns
            Result.success(CharacterPage(listOf(rick), 1, 2))
        coEvery { useCase(2, "", CharacterFilters()) } returns
            Result.failure(IllegalStateException("Next page failed"))

        val vm = CharacterListViewModel(useCase, observeFavoriteIds, toggleFavorite)
        dispatcher.scheduler.advanceUntilIdle()
        vm.loadMore()
        assertTrue(vm.uiState.value.isLoadingMore)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(rick), vm.uiState.value.characters)
        assertFalse(vm.uiState.value.isLoadingMore)
        assertFalse(vm.uiState.value.hasLoadError)
        assertEquals(UiText.Res(R.string.error_generic), vm.errorEvents.first())
        assertNull(withTimeoutOrNull(1) { vm.errorEvents.first() })
    }

    private fun stubSuspendUntilCancelled(
        page: Int,
        query: String = "",
        filters: CharacterFilters = CharacterFilters(),
    ): CompletableDeferred<Unit> {
        val observed = CompletableDeferred<Unit>()
        coEvery { useCase(page, query, filters) } coAnswers {
            try {
                awaitCancellation()
            } catch (e: CancellationException) {
                observed.complete(Unit)
                throw e
            }
        }
        return observed
    }

    @Test
    fun `changing the query cancels an in-flight load more`() = runTest {
        val rick = testCharacter(locationName = "Earth", image = "image", episodeCount = 51)
        coEvery { useCase(1, "", CharacterFilters()) } returns
            Result.success(CharacterPage(listOf(rick), 1, 2))
        val cancellationObserved = stubSuspendUntilCancelled(2)
        coEvery { useCase(1, "morty", CharacterFilters()) } returns
            Result.success(CharacterPage(emptyList(), 1, 1))

        val vm = CharacterListViewModel(useCase, observeFavoriteIds, toggleFavorite)
        dispatcher.scheduler.advanceUntilIdle()
        vm.loadMore()
        dispatcher.scheduler.runCurrent()
        assertTrue(vm.uiState.value.isLoadingMore)
        assertFalse(cancellationObserved.isCompleted)

        vm.onQueryChanged("morty")
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(cancellationObserved.isCompleted)
        assertFalse(vm.uiState.value.isLoadingMore)
        assertTrue(vm.uiState.value.characters.isEmpty())
    }

    @Test
    fun `changing the query cancels an in-flight initial load`() = runTest {
        val cancellationObserved = stubSuspendUntilCancelled(1)
        coEvery { useCase(1, "morty", CharacterFilters()) } returns
            Result.success(CharacterPage(emptyList(), 1, 1))

        val vm = CharacterListViewModel(useCase, observeFavoriteIds, toggleFavorite)
        dispatcher.scheduler.runCurrent()
        assertTrue(vm.uiState.value.isLoading)
        assertFalse(cancellationObserved.isCompleted)

        vm.onQueryChanged("morty")
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(cancellationObserved.isCompleted)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `changing filters cancels an in-flight initial load`() = runTest {
        val filters = CharacterFilters(status = "alive")
        val cancellationObserved = stubSuspendUntilCancelled(1)
        coEvery { useCase(1, "", filters) } returns
            Result.success(CharacterPage(emptyList(), 1, 1))

        val vm = CharacterListViewModel(useCase, observeFavoriteIds, toggleFavorite)
        dispatcher.scheduler.runCurrent()
        assertFalse(cancellationObserved.isCompleted)

        vm.onFiltersChanged(filters)
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(cancellationObserved.isCompleted)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `changing filters cancels an in-flight load more`() = runTest {
        val rick = testCharacter(locationName = "Earth", image = "image", episodeCount = 51)
        val filters = CharacterFilters(status = "alive")
        coEvery { useCase(1, "", CharacterFilters()) } returns
            Result.success(CharacterPage(listOf(rick), 1, 2))
        val cancellationObserved = stubSuspendUntilCancelled(2)
        coEvery { useCase(1, "", filters) } returns
            Result.success(CharacterPage(emptyList(), 1, 1))

        val vm = CharacterListViewModel(useCase, observeFavoriteIds, toggleFavorite)
        dispatcher.scheduler.advanceUntilIdle()
        vm.loadMore()
        dispatcher.scheduler.runCurrent()
        assertFalse(cancellationObserved.isCompleted)

        vm.onFiltersChanged(filters)
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(cancellationObserved.isCompleted)
        assertFalse(vm.uiState.value.isLoadingMore)
    }

    @Test
    fun `cached results emit one offline notice`() = runTest {
        val rick = testCharacter(locationName = "Earth", image = "image", episodeCount = 51)
        coEvery { useCase(1, "", CharacterFilters()) } returns
            Result.success(CharacterPage(listOf(rick), 1, 1, isFromCache = true))

        val vm = CharacterListViewModel(useCase, observeFavoriteIds, toggleFavorite)
        dispatcher.scheduler.advanceUntilIdle()
        vm.loadInitial()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(UiText.Res(R.string.offline_results), vm.errorEvents.first())
        assertNull(withTimeoutOrNull(1) { vm.errorEvents.first() })
    }
}

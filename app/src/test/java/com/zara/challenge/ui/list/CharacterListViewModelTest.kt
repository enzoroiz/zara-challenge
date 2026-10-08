package com.zara.challenge.ui.list

import com.zara.challenge.R
import com.zara.challenge.domain.model.CharacterFilters
import com.zara.challenge.domain.model.CharacterPage
import com.zara.challenge.domain.usecase.GetCharactersUseCase
import com.zara.challenge.domain.usecase.ObserveFavoriteIdsUseCase
import com.zara.challenge.domain.usecase.ToggleFavoriteUseCase
import com.zara.challenge.testCharacter
import com.zara.challenge.ui.common.UiText
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CharacterListViewModelTest {
    private val getCharacters = mockk<GetCharactersUseCase>()
    private val observeFavoriteIds = mockk<ObserveFavoriteIdsUseCase>()
    private val toggleFavorite = mockk<ToggleFavoriteUseCase>()
    private val favoriteIds = MutableStateFlow<Set<Int>>(emptySet())
    private val rick = testCharacter()
    private val morty = testCharacter(2, "Morty Smith")

    @Before
    fun setUp() {
        every { observeFavoriteIds() } returns favoriteIds
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.createViewModel(): CharacterListViewModel {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        return CharacterListViewModel(getCharacters, observeFavoriteIds, toggleFavorite)
    }

    @Test
    fun `initial load exposes loading state then characters`() = runTest {
        val result = CompletableDeferred<Result<CharacterPage>>()
        coEvery { getCharacters(1, "", CharacterFilters()) } coAnswers { result.await() }

        val viewModel = createViewModel()
        runCurrent()
        assertTrue(viewModel.uiState.value.isLoading)

        result.complete(Result.success(CharacterPage(listOf(rick), 1, 1)))
        advanceUntilIdle()

        assertEquals(listOf(rick), viewModel.uiState.value.characters)
        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.endReached)
    }

    @Test
    fun `changing filters reloads the first page`() = runTest {
        val filters = CharacterFilters(status = "alive", gender = "male")
        coEvery { getCharacters(1, "", CharacterFilters()) } returns
            Result.success(CharacterPage(listOf(rick), 1, 2))
        coEvery { getCharacters(1, "", filters) } returns
            Result.success(CharacterPage(listOf(morty), 1, 1))

        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onFiltersChanged(filters)
        advanceUntilIdle()

        assertEquals(filters, viewModel.uiState.value.filters)
        assertEquals(listOf(morty), viewModel.uiState.value.characters)
    }

    @Test
    fun `load more appends the next page`() = runTest {
        coEvery { getCharacters(1, "", CharacterFilters()) } returns
            Result.success(CharacterPage(listOf(rick), 1, 2))
        coEvery { getCharacters(2, "", CharacterFilters()) } returns
            Result.success(CharacterPage(listOf(morty), 2, 2))

        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.loadMore()
        advanceUntilIdle()

        assertEquals(listOf(rick, morty), viewModel.uiState.value.characters)
        assertFalse(viewModel.uiState.value.isLoadingMore)
        assertTrue(viewModel.uiState.value.endReached)
    }

    @Test
    fun `initial request failure can be retried`() = runTest {
        coEvery { getCharacters(1, "", CharacterFilters()) } returnsMany listOf(
            Result.failure(IllegalStateException("request failed")),
            Result.success(CharacterPage(listOf(rick), 1, 1)),
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.hasLoadError)
        assertEquals(UiText.Res(R.string.error_generic), viewModel.errorEvents.first())

        viewModel.retry()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.hasLoadError)
        assertEquals(listOf(rick), viewModel.uiState.value.characters)
    }

    @Test
    fun `load more failure keeps existing characters and reports an error`() = runTest {
        coEvery { getCharacters(1, "", CharacterFilters()) } returns
            Result.success(CharacterPage(listOf(rick), 1, 2))
        coEvery { getCharacters(2, "", CharacterFilters()) } returns
            Result.failure(IllegalStateException("request failed"))

        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.loadMore()
        advanceUntilIdle()

        assertEquals(listOf(rick), viewModel.uiState.value.characters)
        assertFalse(viewModel.uiState.value.isLoadingMore)
        assertEquals(UiText.Res(R.string.error_generic), viewModel.errorEvents.first())
    }

    @Test
    fun `favorite state is observed and favorite click is delegated`() = runTest {
        coEvery { getCharacters(1, "", CharacterFilters()) } returns
            Result.success(CharacterPage(listOf(rick), 1, 1))
        coEvery { toggleFavorite(rick) } returns Result.success(Unit)

        val viewModel = createViewModel()
        advanceUntilIdle()

        favoriteIds.value = setOf(rick.id)
        advanceUntilIdle()
        viewModel.onFavoriteClick(rick)
        advanceUntilIdle()

        assertEquals(setOf(rick.id), viewModel.uiState.value.favoriteIds)
        coVerify(exactly = 1) { toggleFavorite(rick) }
    }
}

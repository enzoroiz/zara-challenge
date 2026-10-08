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
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import okhttp3.ResponseBody.Companion.toResponseBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class CharacterListViewModelPagingTest {
    private val dispatcher = StandardTestDispatcher()
    private val getCharacters = mockk<GetCharactersUseCase>()
    private val observeFavoriteIds = mockk<ObserveFavoriteIdsUseCase>()
    private val toggleFavorite = mockk<ToggleFavoriteUseCase>()
    private val favoriteIds = MutableStateFlow<Set<Int>>(emptySet())
    private val rick = testCharacter(1, "Rick Sanchez")
    private val morty = testCharacter(2, "Morty Smith")

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { observeFavoriteIds() } returns favoriteIds
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun createViewModel() =
        CharacterListViewModel(getCharacters, observeFavoriteIds, toggleFavorite).also {
            dispatcher.scheduler.advanceUntilIdle()
        }

    private fun idle() = dispatcher.scheduler.advanceUntilIdle()

    @Test
    fun `load more appends the next page and marks the end`() = runTest {
        coEvery { getCharacters(1, "", CharacterFilters()) } returns
            Result.success(CharacterPage(listOf(rick), 1, 2))
        coEvery { getCharacters(2, "", CharacterFilters()) } returns
            Result.success(CharacterPage(listOf(morty), 2, 2))
        val vm = createViewModel()
        assertFalse(vm.uiState.value.endReached)

        vm.loadMore()
        idle()

        assertEquals(listOf(rick, morty), vm.uiState.value.characters)
        assertTrue(vm.uiState.value.endReached)
        assertFalse(vm.uiState.value.isLoadingMore)
    }

    @Test
    fun `load more does nothing once the last page is reached`() = runTest {
        coEvery { getCharacters(1, "", CharacterFilters()) } returns
            Result.success(CharacterPage(listOf(rick), 1, 1))
        val vm = createViewModel()

        vm.loadMore()
        idle()

        coVerify(exactly = 0) { getCharacters(2, any(), any()) }
        assertTrue(vm.uiState.value.endReached)
    }

    @Test
    fun `load more does not start while another is in flight`() = runTest {
        coEvery { getCharacters(1, "", CharacterFilters()) } returns
            Result.success(CharacterPage(listOf(rick), 1, 3))
        coEvery { getCharacters(2, "", CharacterFilters()) } returns
            Result.success(CharacterPage(listOf(morty), 2, 3))
        val vm = createViewModel()

        vm.loadMore()
        vm.loadMore()
        idle()

        coVerify(exactly = 1) { getCharacters(2, "", CharacterFilters()) }
    }

    @Test
    fun `load more is ignored while the initial load is running`() = runTest {
        coEvery { getCharacters(1, "", CharacterFilters()) } returns
            Result.success(CharacterPage(listOf(rick), 1, 3))
        val vm = CharacterListViewModel(getCharacters, observeFavoriteIds, toggleFavorite)

        vm.loadMore()
        idle()

        coVerify(exactly = 0) { getCharacters(2, any(), any()) }
    }

    @Test
    fun `empty result marks the end`() = runTest {
        coEvery { getCharacters(1, "", CharacterFilters()) } returns
            Result.success(CharacterPage(emptyList(), 1, 0))

        val vm = createViewModel()

        assertTrue(vm.uiState.value.characters.isEmpty())
        assertTrue(vm.uiState.value.endReached)
        assertFalse(vm.uiState.value.hasLoadError)
    }

    @Test
    fun `query change is debounced and applied once`() = runTest {
        coEvery { getCharacters(1, any(), any()) } returns
            Result.success(CharacterPage(listOf(rick), 1, 1))
        val vm = createViewModel()

        vm.onQueryChanged("r")
        dispatcher.scheduler.advanceTimeBy(100)
        vm.onQueryChanged("ri")
        dispatcher.scheduler.advanceTimeBy(100)
        vm.onQueryChanged("rick")
        idle()

        assertEquals("rick", vm.uiState.value.query)
        coVerify(exactly = 1) { getCharacters(1, "rick", CharacterFilters()) }
        coVerify(exactly = 0) { getCharacters(1, "r", any()) }
        coVerify(exactly = 0) { getCharacters(1, "ri", any()) }
    }

    @Test
    fun `retry reloads after a failure`() = runTest {
        coEvery { getCharacters(1, "", CharacterFilters()) } returns Result.failure(IOException())
        val vm = createViewModel()
        assertTrue(vm.uiState.value.hasLoadError)

        coEvery { getCharacters(1, "", CharacterFilters()) } returns
            Result.success(CharacterPage(listOf(rick), 1, 1))
        vm.retry()
        idle()

        assertFalse(vm.uiState.value.hasLoadError)
        assertEquals(listOf(rick), vm.uiState.value.characters)
    }

    @Test
    fun `initial failure maps http errors to a friendly message`() = runTest {
        val error = HttpException(Response.error<Any>(429, "".toResponseBody()))
        coEvery { getCharacters(1, "", CharacterFilters()) } returns Result.failure(error)

        val vm = createViewModel()

        assertEquals(UiText.Res(R.string.error_too_many_requests), vm.errorEvents.first())
        assertTrue(vm.uiState.value.hasLoadError)
    }

    @Test
    fun `favorite ids are exposed in state`() = runTest {
        coEvery { getCharacters(1, "", CharacterFilters()) } returns
            Result.success(CharacterPage(listOf(rick), 1, 1))
        val vm = createViewModel()

        favoriteIds.value = setOf(1)
        idle()

        assertEquals(setOf(1), vm.uiState.value.favoriteIds)
    }

    @Test
    fun `favorite click toggles the character`() = runTest {
        coEvery { getCharacters(1, "", CharacterFilters()) } returns
            Result.success(CharacterPage(listOf(rick), 1, 1))
        coEvery { toggleFavorite(rick) } returns Result.success(Unit)
        val vm = createViewModel()

        vm.onFavoriteClick(rick)
        idle()

        coVerify(exactly = 1) { toggleFavorite(rick) }
    }

    @Test
    fun `favorite click failure emits an error`() = runTest {
        coEvery { getCharacters(1, "", CharacterFilters()) } returns
            Result.success(CharacterPage(listOf(rick), 1, 1))
        coEvery { toggleFavorite(rick) } returns Result.failure(IllegalStateException())
        val vm = createViewModel()

        vm.onFavoriteClick(rick)
        idle()

        assertEquals(
            UiText.Res(R.string.favorites_update_error),
            vm.errorEvents.first(),
        )
    }

    @Test
    fun `offline notice is shown again after reconnecting and going offline`() = runTest {
        val online = Result.success(CharacterPage(listOf(rick), 1, 1))
        val offline = Result.success(CharacterPage(listOf(rick), 1, 1, isFromCache = true))
        coEvery { getCharacters(1, "", CharacterFilters()) } returnsMany
            listOf(offline, online, offline)
        val vm = createViewModel()
        vm.loadInitial(); idle()
        vm.loadInitial(); idle()

        assertEquals(UiText.Res(R.string.offline_results), vm.errorEvents.first())
        assertEquals(UiText.Res(R.string.offline_results), vm.errorEvents.first())
        assertNull(withTimeoutOrNull(1) { vm.errorEvents.first() })
    }

    @Test
    fun `stale initial results are ignored when filters change`() = runTest {
        val slow = kotlinx.coroutines.CompletableDeferred<Result<CharacterPage>>()
        val filters = CharacterFilters(status = "dead")
        coEvery { getCharacters(1, "", CharacterFilters()) } coAnswers { slow.await() }
        coEvery { getCharacters(1, "", filters) } returns
            Result.success(CharacterPage(listOf(morty), 1, 1))
        val vm = CharacterListViewModel(getCharacters, observeFavoriteIds, toggleFavorite)
        dispatcher.scheduler.runCurrent()

        vm.onFiltersChanged(filters)
        idle()
        slow.complete(Result.success(CharacterPage(listOf(rick), 1, 1)))
        idle()

        assertEquals(listOf(morty), vm.uiState.value.characters)
    }

    @Test
    fun `in-flight initial load is discarded as soon as the query changes`() = runTest {
        val slow = kotlinx.coroutines.CompletableDeferred<Result<CharacterPage>>()
        coEvery { getCharacters(1, "", CharacterFilters()) } coAnswers { slow.await() }
        coEvery { getCharacters(1, "morty", CharacterFilters()) } returns
            Result.success(CharacterPage(listOf(morty), 1, 1))
        val vm = CharacterListViewModel(getCharacters, observeFavoriteIds, toggleFavorite)
        dispatcher.scheduler.runCurrent()

        vm.onQueryChanged("morty")
        slow.complete(Result.success(CharacterPage(listOf(rick), 1, 1)))
        dispatcher.scheduler.advanceTimeBy(100)

        assertTrue(vm.uiState.value.characters.isEmpty())

        idle()
        assertEquals(listOf(morty), vm.uiState.value.characters)
    }
}

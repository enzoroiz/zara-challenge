package com.zara.challenge.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zara.challenge.domain.model.Character
import com.zara.challenge.domain.model.CharacterFilters
import com.zara.challenge.domain.usecase.ObserveFavoriteIdsUseCase
import com.zara.challenge.domain.usecase.ToggleFavoriteUseCase
import com.zara.challenge.domain.usecase.GetCharactersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

data class CharacterListUiState(
    val characters: List<Character> = emptyList(),
    val query: String = "",
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val hasLoadError: Boolean = false,
    val endReached: Boolean = false,
    val filters: CharacterFilters = CharacterFilters(),
    val favoriteIds: Set<Int> = emptySet(),
)

@HiltViewModel
class CharacterListViewModel @Inject constructor(
    private val getCharacters: GetCharactersUseCase,
    private val observeFavoriteIds: ObserveFavoriteIdsUseCase,
    private val toggleFavorite: ToggleFavoriteUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CharacterListUiState())
    val uiState: StateFlow<CharacterListUiState> = _uiState.asStateFlow()
    private val _errorEvents = Channel<String>(Channel.BUFFERED)
    val errorEvents = _errorEvents.receiveAsFlow()

    private var currentPage = 0
    private var totalPages = Int.MAX_VALUE
    private var searchJob: Job? = null
    private var listLoadJob: Job? = null
    private var requestId = 0

    init {
        viewModelScope.launch {
            observeFavoriteIds().collect { ids ->
                _uiState.value = _uiState.value.copy(favoriteIds = ids)
            }
        }
        loadInitial()
    }

    fun onQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(350)
            loadInitial()
        }
    }

    fun onFiltersChanged(filters: CharacterFilters) {
        searchJob?.cancel()
        _uiState.value = _uiState.value.copy(filters = filters)
        loadInitial()
    }

    fun loadInitial() {
        listLoadJob?.cancel()
        val request = ++requestId
        val query = _uiState.value.query
        val filters = _uiState.value.filters
        currentPage = 0
        totalPages = Int.MAX_VALUE
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            isLoadingMore = false,
            hasLoadError = false,
            characters = emptyList(),
            endReached = false,
        )
        listLoadJob = viewModelScope.launch {
            getCharacters(1, query, filters).onSuccess { page ->
                if (request != requestId) return@onSuccess
                currentPage = page.page
                totalPages = page.totalPages
                _uiState.value = _uiState.value.copy(
                    characters = page.characters,
                    isLoading = false,
                    hasLoadError = false,
                    endReached = page.page >= page.totalPages,
                )
            }.onFailure { error ->
                if (request != requestId) return@onFailure
                val message = error.userMessage()
                _uiState.value = _uiState.value.copy(isLoading = false, hasLoadError = true)
                _errorEvents.trySend(message).getOrThrow()
            }
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore || currentPage >= totalPages) return
        val request = requestId
        val page = currentPage + 1
        _uiState.value = state.copy(isLoadingMore = true)
        viewModelScope.launch {
            getCharacters(page, state.query, state.filters).onSuccess { result ->
                if (request != requestId) return@onSuccess
                currentPage = result.page
                totalPages = result.totalPages
                _uiState.value = _uiState.value.copy(
                    characters = _uiState.value.characters + result.characters,
                    isLoadingMore = false,
                    endReached = result.page >= result.totalPages,
                )
            }.onFailure { error ->
                if (request != requestId) return@onFailure
                _uiState.value = _uiState.value.copy(isLoadingMore = false)
                _errorEvents.trySend(error.userMessage()).getOrThrow()
            }
        }
    }

    fun onFavoriteClick(character: Character) {
        viewModelScope.launch {
            toggleFavorite(character).onFailure {
                _errorEvents.trySend("Couldn't update your favourites. Please try again.")
            }
        }
    }

    fun retry() = loadInitial()
}

private fun Throwable.userMessage() =
    message?.takeIf(String::isNotBlank) ?: "Something went wrong. Please try again."

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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

data class CharacterListUiState(
    val characters: List<Character> = emptyList(),
    val query: String = "",
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
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
            error = null,
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
                    endReached = page.page >= page.totalPages,
                )
            }.onFailure { error ->
                if (request != requestId) return@onFailure
                _uiState.value = _uiState.value.copy(isLoading = false, error = error.userMessage())
            }
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore || currentPage >= totalPages) return
        val request = requestId
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMore = true, error = null)
            getCharacters(currentPage + 1, state.query, state.filters).onSuccess { page ->
                if (request != requestId) return@onSuccess
                currentPage = page.page
                totalPages = page.totalPages
                _uiState.value = _uiState.value.copy(
                    characters = _uiState.value.characters + page.characters,
                    isLoadingMore = false,
                    endReached = page.page >= page.totalPages,
                )
            }.onFailure { error ->
                if (request != requestId) return@onFailure
                _uiState.value = _uiState.value.copy(isLoadingMore = false, error = error.userMessage())
            }
        }
    }

    fun onFavoriteClick(character: Character) {
        viewModelScope.launch {
            toggleFavorite(character).onFailure { error ->
                _uiState.value = _uiState.value.copy(error = error.userMessage())
            }
        }
    }

    fun retry() = loadInitial()
}

private fun Throwable.userMessage() = message ?: "Something went wrong. Please try again."

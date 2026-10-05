package com.zara.challenge.ui.detail

import com.zara.challenge.ui.common.toUserMessage
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zara.challenge.domain.model.Character
import com.zara.challenge.domain.usecase.GetSimilarCharactersUseCase
import com.zara.challenge.domain.usecase.GetCharacterUseCase
import com.zara.challenge.domain.usecase.ObserveFavoriteIdsUseCase
import com.zara.challenge.domain.usecase.ToggleFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed interface CharacterDetailUiState {
    data object Loading : CharacterDetailUiState
    data class Success(
        val character: Character,
        val isFavorite: Boolean = false,
        val favoriteIds: Set<Int> = emptySet(),
        val similarCharacters: List<Character> = emptyList(),
        val recommendationError: String? = null,
    ) : CharacterDetailUiState
    data class Error(val message: String) : CharacterDetailUiState
}

@HiltViewModel
class CharacterDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getCharacter: GetCharacterUseCase,
    private val getSimilarCharacters: GetSimilarCharactersUseCase,
    private val observeFavoriteIds: ObserveFavoriteIdsUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
) : ViewModel() {
    private val id: Int = checkNotNull(savedStateHandle["characterId"])
    private val _uiState = MutableStateFlow<CharacterDetailUiState>(CharacterDetailUiState.Loading)
    val uiState: StateFlow<CharacterDetailUiState> = _uiState.asStateFlow()
    private val _errorEvents = Channel<String>(Channel.BUFFERED)
    val errorEvents = _errorEvents.receiveAsFlow()
    private var favoriteIds: Set<Int> = emptySet()

    init {
        viewModelScope.launch {
            observeFavoriteIds().collect { ids ->
                favoriteIds = ids
                val current = _uiState.value as? CharacterDetailUiState.Success
                if (current != null) {
                    _uiState.value = current.copy(isFavorite = id in ids, favoriteIds = ids)
                }
            }
        }
        load()
    }

    fun load() = viewModelScope.launch {
        _uiState.value = CharacterDetailUiState.Loading
        getCharacter(id).onSuccess { character ->
            getSimilarCharacters(character)
                .onSuccess { matches ->
                    _uiState.value = CharacterDetailUiState.Success(
                        character = character,
                        isFavorite = id in favoriteIds,
                        favoriteIds = favoriteIds,
                        similarCharacters = matches,
                    )
                }
                .onFailure { error ->
                    _uiState.value = CharacterDetailUiState.Success(
                        character = character,
                        isFavorite = id in favoriteIds,
                        favoriteIds = favoriteIds,
                        recommendationError = error.message ?: "Unable to load recommendations",
                    )
                }
        }
            .onFailure { _uiState.value = CharacterDetailUiState.Error(it.toUserMessage()) }
    }

    fun toggleFavorite() {
        val current = _uiState.value as? CharacterDetailUiState.Success ?: return
        toggleFavorite(current.character)
    }

    fun toggleFavorite(character: Character) {
        viewModelScope.launch {
            toggleFavoriteUseCase(character).onFailure {
                _errorEvents.trySend("Couldn't update your favourites. Please try again.")
            }
        }
    }
}

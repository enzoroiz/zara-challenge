package com.zara.challenge.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zara.challenge.domain.model.Character
import com.zara.challenge.domain.usecase.ObserveFavoritesUseCase
import com.zara.challenge.domain.usecase.ToggleFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

data class FavoritesUiState(
    val characters: List<Character> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    observeFavorites: ObserveFavoritesUseCase,
    private val toggleFavorite: ToggleFavoriteUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState = _uiState.asStateFlow()
    private val _errorEvents = Channel<String>(Channel.BUFFERED)
    val errorEvents = _errorEvents.receiveAsFlow()

    init {
        viewModelScope.launch {
            observeFavorites()
                .catch { error ->
                    _uiState.value = FavoritesUiState(
                        isLoading = false,
                        error = error.message ?: "Unable to load favourites",
                    )
                }
                .collect { characters ->
                    _uiState.value = FavoritesUiState(characters = characters, isLoading = false)
                }
        }
    }

    fun removeFavorite(character: Character) {
        viewModelScope.launch {
            toggleFavorite(character).onFailure {
                _errorEvents.trySend("Couldn't update your favourites. Please try again.")
            }
        }
    }
}

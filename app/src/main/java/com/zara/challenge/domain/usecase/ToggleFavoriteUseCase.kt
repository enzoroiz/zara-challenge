package com.zara.challenge.domain.usecase

import com.zara.challenge.domain.model.Character
import com.zara.challenge.domain.repository.CharacterRepository
import javax.inject.Inject

class ToggleFavoriteUseCase @Inject constructor(
    private val repository: CharacterRepository,
) {
    suspend operator fun invoke(character: Character): Result<Unit> =
        repository.toggleFavorite(character)
}

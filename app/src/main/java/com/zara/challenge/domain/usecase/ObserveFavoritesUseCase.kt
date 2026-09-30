package com.zara.challenge.domain.usecase

import com.zara.challenge.domain.model.Character
import com.zara.challenge.domain.repository.CharacterRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveFavoritesUseCase @Inject constructor(
    private val repository: CharacterRepository,
) {
    operator fun invoke(): Flow<List<Character>> = repository.observeFavorites()
}

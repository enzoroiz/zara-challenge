package com.zara.challenge.domain.usecase

import com.zara.challenge.domain.repository.CharacterRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveFavoriteIdsUseCase @Inject constructor(
    private val repository: CharacterRepository,
) {
    operator fun invoke(): Flow<Set<Int>> = repository.observeFavoriteIds()
}

package com.zara.challenge.domain.usecase

import com.zara.challenge.domain.model.Character
import com.zara.challenge.domain.repository.CharacterRepository
import javax.inject.Inject

class GetSimilarCharactersUseCase @Inject constructor(
    private val repository: CharacterRepository,
) {
    suspend operator fun invoke(character: Character): Result<List<Character>> =
        repository.getSimilarCharacters(character)
}

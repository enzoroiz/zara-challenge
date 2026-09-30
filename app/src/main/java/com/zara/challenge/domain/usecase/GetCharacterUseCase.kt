package com.zara.challenge.domain.usecase

import com.zara.challenge.domain.model.Character
import com.zara.challenge.domain.repository.CharacterRepository
import javax.inject.Inject

class GetCharacterUseCase @Inject constructor(private val repository: CharacterRepository) {
    suspend operator fun invoke(id: Int): Result<Character> = repository.getCharacter(id)
}

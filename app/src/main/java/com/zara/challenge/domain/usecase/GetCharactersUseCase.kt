package com.zara.challenge.domain.usecase

import com.zara.challenge.domain.repository.CharacterPage
import com.zara.challenge.domain.repository.CharacterRepository
import com.zara.challenge.domain.model.CharacterFilters
import javax.inject.Inject

class GetCharactersUseCase @Inject constructor(private val repository: CharacterRepository) {
    suspend operator fun invoke(
        page: Int,
        query: String,
        filters: CharacterFilters = CharacterFilters(),
    ): Result<CharacterPage> = repository.getCharacters(page, query, filters)
}

package com.zara.challenge.domain.model

data class CharacterPage(
    val characters: List<Character>,
    val page: Int,
    val totalPages: Int,
    val isFromCache: Boolean = false,
)

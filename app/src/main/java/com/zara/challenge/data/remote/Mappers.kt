package com.zara.challenge.data.remote

import com.zara.challenge.data.local.CharacterEntity
import com.zara.challenge.domain.model.Character

fun CharacterDto.toDomain() = Character(
    id = id,
    name = name,
    status = status,
    species = species,
    type = type,
    gender = gender,
    originName = origin.name,
    locationName = location.name,
    image = image,
    episodeCount = episode.size,
    created = created,
)

fun CharacterDto.toEntity() = CharacterEntity(
    id = id,
    name = name,
    status = status,
    species = species,
    type = type,
    gender = gender,
    originName = origin.name,
    locationName = location.name,
    image = image,
    episodeCount = episode.size,
    created = created,
)

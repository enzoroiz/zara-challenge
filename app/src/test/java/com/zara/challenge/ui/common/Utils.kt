package com.zara.challenge.ui.common

import com.zara.challenge.data.local.CharacterEntity
import com.zara.challenge.data.remote.dto.CharacterDto
import com.zara.challenge.data.remote.dto.LocationDto
import com.zara.challenge.domain.model.Character

fun testCharacter(
    id: Int = 1,
    name: String = "Rick Sanchez",
    status: String = "Alive",
    species: String = "Human",
    type: String = "",
    gender: String = "Male",
    originName: String = "Earth",
    locationName: String = "Citadel",
    image: String = "image-$id",
    episodeCount: Int = 3,
    created: String = "created",
) = Character(
    id = id,
    name = name,
    status = status,
    species = species,
    type = type,
    gender = gender,
    originName = originName,
    locationName = locationName,
    image = image,
    episodeCount = episodeCount,
    created = created,
)

fun testEntity(
    id: Int = 1,
    name: String = "Rick Sanchez",
    status: String = "Alive",
    species: String = "Human",
    type: String = "",
    gender: String = "Male",
    originName: String = "Earth",
    locationName: String = "Citadel",
    image: String = "image-$id",
    episodeCount: Int = 3,
    created: String = "created",
) = CharacterEntity(
    id = id,
    name = name,
    status = status,
    species = species,
    type = type,
    gender = gender,
    originName = originName,
    locationName = locationName,
    image = image,
    episodeCount = episodeCount,
    created = created,
)

fun testDto(
    id: Int = 1,
    name: String = "Rick Sanchez",
    status: String = "Alive",
    species: String = "Human",
    type: String = "",
    gender: String = "Male",
    originName: String = "Earth",
    locationName: String = "Citadel",
    image: String = "image-$id",
    episodes: List<String> = listOf("e1", "e2", "e3"),
    url: String = "url",
    created: String = "created",
) = CharacterDto(
    id = id,
    name = name,
    status = status,
    species = species,
    type = type,
    gender = gender,
    origin = LocationDto(originName, "origin-url"),
    location = LocationDto(locationName, "location-url"),
    image = image,
    episode = episodes,
    url = url,
    created = created,
)

package com.zara.challenge

import com.zara.challenge.data.local.CharacterEntity
import com.zara.challenge.data.remote.dto.CharacterDto
import com.zara.challenge.data.remote.dto.LocationDto
import com.zara.challenge.domain.model.Character

fun testCharacter(
    id: Int = 1,
    name: String = "Rick Sanchez",
    status: String = "Alive",
    gender: String = "Male",
) = Character(
    id = id,
    name = name,
    status = status,
    species = "Human",
    type = "",
    gender = gender,
    originName = "Earth",
    locationName = "Citadel",
    image = "image-$id",
    episodeCount = 3,
    created = "created",
)

fun testEntity(
    id: Int = 1,
    name: String = "Rick Sanchez",
    status: String = "Alive",
    gender: String = "Male",
) = CharacterEntity(
    id = id,
    name = name,
    status = status,
    species = "Human",
    type = "",
    gender = gender,
    originName = "Earth",
    locationName = "Citadel",
    image = "image-$id",
    episodeCount = 3,
    created = "created",
)

fun testDto(id: Int = 1, name: String = "Rick Sanchez") = CharacterDto(
    id = id,
    name = name,
    status = "Alive",
    species = "Human",
    type = "",
    gender = "Male",
    origin = LocationDto("Earth", "origin-url"),
    location = LocationDto("Citadel", "location-url"),
    image = "image-$id",
    episode = listOf("e1", "e2", "e3"),
    url = "url",
    created = "created",
)

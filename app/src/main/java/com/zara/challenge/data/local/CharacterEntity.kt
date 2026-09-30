package com.zara.challenge.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.zara.challenge.domain.model.Character

@Entity(tableName = "characters")
data class CharacterEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val status: String,
    val species: String,
    val type: String,
    val gender: String,
    val originName: String,
    val locationName: String,
    val image: String,
    val episodeCount: Int,
    val created: String,
) {
    fun toDomain() = Character(
        id, name, status, species, type, gender, originName, locationName, image, episodeCount, created
    )
}

fun Character.toEntity() = CharacterEntity(
    id, name, status, species, type, gender, originName, locationName, image, episodeCount, created
)

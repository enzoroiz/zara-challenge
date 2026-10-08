package com.zara.challenge.data.repository

import com.zara.challenge.data.remote.dto.toDomain
import com.zara.challenge.data.remote.dto.toEntity
import com.zara.challenge.testCharacter
import com.zara.challenge.testDto
import com.zara.challenge.testEntity
import org.junit.Assert
import org.junit.Test

class DtoMappersTest {
    @Test
    fun `dto maps to domain with origin location and episode count`() {
        val character = testDto(id = 4, name = "Beth Smith").toDomain()

        Assert.assertEquals(
            testCharacter(id = 4, name = "Beth Smith").copy(
                originName = "Earth",
                locationName = "Citadel",
                episodeCount = 3,
            ),
            character,
        )
    }

    @Test
    fun `dto maps to entity`() {
        Assert.assertEquals(
            testEntity(id = 4, name = "Beth Smith"),
            testDto(4, "Beth Smith").toEntity()
        )
    }

    @Test
    fun `dto without episodes has zero episode count`() {
        Assert.assertEquals(0, testDto().copy(episode = emptyList()).toDomain().episodeCount)
    }
}
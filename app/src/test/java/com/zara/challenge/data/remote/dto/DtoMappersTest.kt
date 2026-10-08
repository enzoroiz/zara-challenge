package com.zara.challenge.data.remote.dto

import com.zara.challenge.ui.common.testCharacter
import com.zara.challenge.ui.common.testDto
import com.zara.challenge.ui.common.testEntity
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
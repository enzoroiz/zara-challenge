package com.zara.challenge

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zara.challenge.data.local.CharacterEntity
import com.zara.challenge.data.local.ZaraChallengeDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import android.content.Context

@RunWith(AndroidJUnit4::class)
class CharacterDaoTest {
    @Test
    fun favoriteReferencesCachedCharacterAndCanBeRemoved() = runBlocking {
        val database = inMemoryDatabase()
        try {
            val dao = database.characterDao()
            val rick = entity(1, "Rick Sanchez")

            dao.toggleFavorite(rick)
            assertEquals(listOf(1), dao.observeFavorites().first().map { it.id })

            dao.toggleFavorite(rick)
            assertEquals(emptyList<Int>(), dao.observeFavoriteIds().first())
        } finally {
            database.close()
        }
    }

    @Test
    fun firstNameMatchingIsCaseInsensitiveAndExcludesCurrentCharacter() = runBlocking {
        val database = inMemoryDatabase()
        try {
            val dao = database.characterDao()
            dao.upsertAll(
                listOf(
                    entity(1, "Rick Sanchez"),
                    entity(2, "Rick Prime"),
                    entity(3, "Morty Smith"),
                ),
            )

            val matches = dao.findByFirstName("rIcK", characterId = 1)

            assertEquals(listOf(2), matches.map { it.id })
        } finally {
            database.close()
        }
    }

    private fun inMemoryDatabase() =
        Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            ZaraChallengeDatabase::class.java,
        ).allowMainThreadQueries().build()

    private fun entity(id: Int, name: String) = CharacterEntity(
        id = id,
        name = name,
        status = "Alive",
        species = "Human",
        type = "",
        gender = "Male",
        originName = "Earth",
        locationName = "Earth",
        image = "image",
        episodeCount = 1,
        created = "created",
    )
}

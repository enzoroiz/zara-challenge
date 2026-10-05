package com.zara.challenge.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.zara.challenge.testEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CharacterDaoTest {
    private lateinit var database: ZaraChallengeDatabase
    private lateinit var dao: CharacterDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            ZaraChallengeDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = database.characterDao()
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun `upsert inserts and updates by id`() = runTest {
        dao.upsertAll(listOf(testEntity(1, "Rick Sanchez")))
        dao.upsertAll(listOf(testEntity(1, "Rick Updated"), testEntity(2, "Morty Smith")))

        assertEquals("Rick Updated", dao.getCharacter(1)?.name)
        assertEquals(listOf(1, 2), dao.observeCharacters().first().map { it.id })
    }

    @Test
    fun `getCharacter returns null when absent`() = runTest {
        assertNull(dao.getCharacter(99))
    }

    @Test
    fun `search matches name substring case insensitively and orders by id`() = runTest {
        dao.upsertAll(
            listOf(
                testEntity(3, "Evil Morty"),
                testEntity(1, "Rick Sanchez"),
                testEntity(2, "Morty Smith"),
            ),
        )

        val result = dao.searchCharacters("morty", null, null)

        assertEquals(listOf(2, 3), result.map { it.id })
    }

    @Test
    fun `search with empty query returns everything`() = runTest {
        dao.upsertAll(listOf(testEntity(1), testEntity(2, "Morty Smith")))

        assertEquals(2, dao.searchCharacters("", null, null).size)
    }

    @Test
    fun `search filters by status and gender ignoring case`() = runTest {
        dao.upsertAll(
            listOf(
                testEntity(1, "Rick", status = "Alive", gender = "Male"),
                testEntity(2, "Summer", status = "Alive", gender = "Female"),
                testEntity(3, "Birdperson", status = "Dead", gender = "Male"),
            ),
        )

        assertEquals(listOf(1, 2), dao.searchCharacters("", "alive", null).map { it.id })
        assertEquals(listOf(1, 3), dao.searchCharacters("", null, "MALE").map { it.id })
        assertEquals(listOf(1), dao.searchCharacters("", "ALIVE", "male").map { it.id })
        assertTrue(dao.searchCharacters("", "unknown", null).isEmpty())
    }

    @Test
    fun `toggleFavorite adds then removes a favorite`() = runTest {
        val rick = testEntity(1)

        dao.toggleFavorite(rick)
        assertTrue(dao.isFavorite(1))
        assertEquals(setOf(1), dao.observeFavoriteIds().first().toSet())

        dao.toggleFavorite(rick)
        assertFalse(dao.isFavorite(1))
        assertTrue(dao.observeFavoriteIds().first().isEmpty())
    }

    @Test
    fun `toggleFavorite caches the character it favorites`() = runTest {
        dao.toggleFavorite(testEntity(7, "Birdperson"))

        assertEquals("Birdperson", dao.getCharacter(7)?.name)
    }

    @Test
    fun `insertFavorite ignores duplicates`() = runTest {
        dao.upsertAll(listOf(testEntity(1)))

        dao.insertFavorite(FavoriteCharacterEntity(1))
        dao.insertFavorite(FavoriteCharacterEntity(1))

        assertEquals(listOf(1), dao.observeFavoriteIds().first())
    }

    @Test
    fun `deleteFavorite on a non favorite is a no-op`() = runTest {
        dao.deleteFavorite(1)

        assertTrue(dao.observeFavoriteIds().first().isEmpty())
    }

    @Test
    fun `favorites are ordered by name ignoring case`() = runTest {
        listOf(testEntity(1, "rick"), testEntity(2, "Morty"), testEntity(3, "Abradolf"))
            .forEach { dao.toggleFavorite(it) }
        dao.upsertAll(listOf(testEntity(4, "Not Favorite")))

        assertEquals(listOf(3, 2, 1), dao.observeFavorites().first().map { it.id })
    }

    @Test
    fun `upserting a favorited character keeps the favorite`() = runTest {
        dao.toggleFavorite(testEntity(1, "Rick"))

        dao.upsertAll(listOf(testEntity(1, "Rick Renamed")))

        assertTrue(dao.isFavorite(1))
        assertEquals("Rick Renamed", dao.observeFavorites().first().single().name)
    }

    @Test
    fun `similar characters share the first name, exclude self, and are capped at five`() = runTest {
        dao.upsertAll(
            listOf(
                testEntity(1, "Rick Sanchez"),
                testEntity(2, "rick Prime"),
                testEntity(3, "Rick Smith"),
                testEntity(4, "Rick A"),
                testEntity(5, "Rick B"),
                testEntity(6, "Rick C"),
                testEntity(7, "Rick D"),
                testEntity(8, "Rickety Cricket"),
                testEntity(9, "Morty Smith"),
            ),
        )

        val matches = dao.findByFirstName("Rick", characterId = 1)

        assertEquals(5, matches.size)
        assertTrue(matches.none { it.id == 1 || it.id == 8 || it.id == 9 })
        assertEquals(matches.map { it.name.lowercase() }, matches.map { it.name.lowercase() }.sorted())
    }

    @Test
    fun `similar characters handles single word names`() = runTest {
        dao.upsertAll(listOf(testEntity(1, "Rick"), testEntity(2, "Rick Sanchez")))

        assertEquals(listOf(2), dao.findByFirstName("rick", 1).map { it.id })
    }

    @Test
    fun `entity and domain mapping round trip`() {
        val entity = testEntity(5, "Summer Smith")

        assertEquals(entity, entity.toDomain().toEntity())
    }
}

package com.zara.challenge.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CharacterDao {
    @Query("SELECT * FROM characters ORDER BY id")
    fun observeCharacters(): Flow<List<CharacterEntity>>

    @Query("SELECT * FROM characters WHERE id = :id LIMIT 1")
    suspend fun getCharacter(id: Int): CharacterEntity?

    @Query(
        """
        SELECT * FROM characters
        WHERE name LIKE '%' || :query || '%'
          AND (:status IS NULL OR status = :status COLLATE NOCASE)
          AND (:gender IS NULL OR gender = :gender COLLATE NOCASE)
        ORDER BY id
        """,
    )
    suspend fun searchCharacters(
        query: String,
        status: String?,
        gender: String?,
    ): List<CharacterEntity>

    @Upsert
    suspend fun upsertAll(characters: List<CharacterEntity>)

    @Query(
        """
        SELECT characters.* FROM characters
        INNER JOIN favorite_characters ON characters.id = favorite_characters.characterId
        ORDER BY characters.name COLLATE NOCASE
        """,
    )
    fun observeFavorites(): Flow<List<CharacterEntity>>

    @Query("SELECT characterId FROM favorite_characters")
    fun observeFavoriteIds(): Flow<List<Int>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_characters WHERE characterId = :id)")
    suspend fun isFavorite(id: Int): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFavorite(favorite: FavoriteCharacterEntity)

    @Query("DELETE FROM favorite_characters WHERE characterId = :id")
    suspend fun deleteFavorite(id: Int)

    @Transaction
    suspend fun toggleFavorite(character: CharacterEntity) {
        upsertAll(listOf(character))
        if (isFavorite(character.id)) {
            deleteFavorite(character.id)
        } else {
            insertFavorite(FavoriteCharacterEntity(character.id))
        }
    }

    @Query(
        """
        SELECT * FROM characters
        WHERE id != :characterId
          AND lower(substr(trim(name), 1, instr(trim(name) || ' ', ' ') - 1)) = lower(:firstName)
        ORDER BY name COLLATE NOCASE
        LIMIT 5
        """,
    )
    suspend fun findByFirstName(firstName: String, characterId: Int): List<CharacterEntity>
}

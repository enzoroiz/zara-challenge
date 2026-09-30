package com.zara.challenge.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [CharacterEntity::class, FavoriteCharacterEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class ZaraChallengeDatabase : RoomDatabase() {
    abstract fun characterDao(): CharacterDao
}

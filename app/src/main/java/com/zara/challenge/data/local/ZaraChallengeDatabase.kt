package com.zara.challenge.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [CharacterEntity::class, FavoriteCharacterEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class ZaraChallengeDatabase : RoomDatabase() {
    abstract fun characterDao(): CharacterDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `favorite_characters` (
                        `characterId` INTEGER NOT NULL,
                        PRIMARY KEY(`characterId`),
                        FOREIGN KEY(`characterId`) REFERENCES `characters`(`id`)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_favorite_characters_characterId` " +
                        "ON `favorite_characters` (`characterId`)",
                )
            }
        }
    }
}

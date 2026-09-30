package com.zara.challenge.di

import android.content.Context
import androidx.room.Room
import com.zara.challenge.data.local.CharacterDao
import com.zara.challenge.data.local.ZaraChallengeDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ZaraChallengeDatabase =
        Room.databaseBuilder(context, ZaraChallengeDatabase::class.java, "zara_challenge.db")
            .fallbackToDestructiveMigration(false)
            .build()

    @Provides
    fun provideCharacterDao(database: ZaraChallengeDatabase): CharacterDao = database.characterDao()
}

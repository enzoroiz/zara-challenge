package com.zara.challenge.di

import com.zara.challenge.data.local.CharacterDao
import com.zara.challenge.data.remote.ZaraChallengeApi
import com.zara.challenge.data.repository.CharacterRepositoryImpl
import com.zara.challenge.domain.repository.CharacterRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Provides
    @Singleton
    fun provideRepository(api: ZaraChallengeApi, dao: CharacterDao): CharacterRepository =
        CharacterRepositoryImpl(api, dao)
}

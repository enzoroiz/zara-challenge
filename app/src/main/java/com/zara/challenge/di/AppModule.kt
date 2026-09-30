package com.zara.challenge.di

import android.content.Context
import androidx.room.Room
import com.zara.challenge.data.local.CharacterDao
import com.zara.challenge.data.local.ZaraChallengeDatabase
import com.zara.challenge.data.remote.ZaraChallengeApi
import com.zara.challenge.data.repository.CharacterRepositoryImpl
import com.zara.challenge.domain.repository.CharacterRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    private const val BASE_URL = "https://rickandmortyapi.com/api/"

    @Provides @Singleton
    fun provideOkHttp(@ApplicationContext context: Context): OkHttpClient {
        val cache = Cache(File(context.cacheDir, "http-cache"), 10L * 1024 * 1024)
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        return OkHttpClient.Builder()
            .cache(cache)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("Cache-Control", "public, max-age=300")
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(logging)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    @Provides @Singleton
    fun provideApi(client: OkHttpClient): ZaraChallengeApi = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ZaraChallengeApi::class.java)

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ZaraChallengeDatabase =
        Room.databaseBuilder(context, ZaraChallengeDatabase::class.java, "zara_challenge.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideCharacterDao(database: ZaraChallengeDatabase): CharacterDao = database.characterDao()

    @Provides @Singleton
    fun provideRepository(api: ZaraChallengeApi, dao: CharacterDao): CharacterRepository =
        CharacterRepositoryImpl(api, dao)
}

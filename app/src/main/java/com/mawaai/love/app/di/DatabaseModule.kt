package com.mawaai.love.app.di

import android.content.Context
import androidx.room.Room
import com.mawaai.love.app.data.dao.*
import com.mawaai.love.app.data.database.MawaaiDatabase
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
    fun provideDatabase(@ApplicationContext context: Context): MawaaiDatabase {
        return Room.databaseBuilder(
            context,
            MawaaiDatabase::class.java,
            "mawaai_db"
        ).build()
    }

    @Provides
    fun provideMemoryDao(db: MawaaiDatabase): MemoryDao = db.memoryDao()

    @Provides
    fun provideLoveLetterDao(db: MawaaiDatabase): LoveLetterDao = db.loveLetterDao()

    @Provides
    fun provideMoodDao(db: MawaaiDatabase): MoodDao = db.moodDao()

    @Provides
    fun provideProfileDao(db: MawaaiDatabase): ProfileDao = db.profileDao()

    @Provides
    fun provideArtworkDao(db: MawaaiDatabase): ArtworkDao = db.artworkDao()

    @Provides
    fun provideCountdownDao(db: MawaaiDatabase): CountdownDao = db.countdownDao()
}

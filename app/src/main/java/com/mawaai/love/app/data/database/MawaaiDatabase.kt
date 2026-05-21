package com.mawaai.love.app.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.mawaai.love.app.data.dao.*
import com.mawaai.love.app.data.model.*

@Database(
    entities = [
        Memory::class,
        LoveLetter::class,
        MoodEntry::class,
        UserProfile::class,
        Artwork::class,
        Countdown::class
    ],
    version = 5,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class MawaaiDatabase : RoomDatabase() {
    abstract fun memoryDao(): MemoryDao
    abstract fun loveLetterDao(): LoveLetterDao
    abstract fun moodDao(): MoodDao
    abstract fun profileDao(): ProfileDao
    abstract fun artworkDao(): ArtworkDao
    abstract fun countdownDao(): CountdownDao
}

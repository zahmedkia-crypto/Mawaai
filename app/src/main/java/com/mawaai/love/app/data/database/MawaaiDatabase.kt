package com.mawaai.love.app.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.mawaai.love.app.data.dao.*
import com.mawaai.love.app.data.database.dao.ProductMockupDao
import com.mawaai.love.app.data.database.dao.ProjectDao
import com.mawaai.love.app.data.database.dao.TemplateDao
import com.mawaai.love.app.data.database.entities.ProductMockupEntity
import com.mawaai.love.app.data.database.entities.ProjectEntity
import com.mawaai.love.app.data.database.entities.TemplateEntity
import com.mawaai.love.app.data.model.*

@Database(
    entities = [
        Memory::class,
        LoveLetter::class,
        MoodEntry::class,
        UserProfile::class,
        Artwork::class,
        Countdown::class,
        TemplateEntity::class,
        ProjectEntity::class,
        ProductMockupEntity::class
    ],
    version = 6,
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
    abstract fun templateDao(): TemplateDao
    abstract fun projectDao(): ProjectDao
    abstract fun productMockupDao(): ProductMockupDao
}

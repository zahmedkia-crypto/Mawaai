package com.mawaai.love.app.data.dao

import androidx.room.*
import com.mawaai.love.app.data.model.MoodEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface MoodDao {
    @Query("SELECT * FROM mood_entries ORDER BY date DESC")
    fun getAllMoods(): Flow<List<MoodEntry>>

    @Query("SELECT * FROM mood_entries ORDER BY date DESC LIMIT 1")
    fun getLatestMood(): Flow<MoodEntry?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMood(mood: MoodEntry): Long

    @Query("DELETE FROM mood_entries WHERE date < :timestamp")
    suspend fun deleteOldMoods(timestamp: Long)
}

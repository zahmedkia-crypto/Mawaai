package com.mawaai.love.app.data.repository

import com.mawaai.love.app.data.dao.MoodDao
import com.mawaai.love.app.data.model.MoodEntry
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MoodRepository @Inject constructor(
    private val dao: MoodDao
) {
    fun getAllMoods(): Flow<List<MoodEntry>> = dao.getAllMoods()
    fun getLatestMood(): Flow<MoodEntry?> = dao.getLatestMood()

    suspend fun addMood(mood: MoodEntry): Long = dao.insertMood(mood)
    suspend fun deleteOldMoods(timestamp: Long) = dao.deleteOldMoods(timestamp)
}

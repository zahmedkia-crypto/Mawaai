package com.mawaai.love.app.data.dao

import androidx.room.*
import com.mawaai.love.app.data.model.Memory
import com.mawaai.love.app.data.model.MemoryCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memories ORDER BY date DESC")
    fun getAllMemories(): Flow<List<Memory>>

    @Query("SELECT * FROM memories WHERE category = :category ORDER BY date DESC")
    fun getMemoriesByCategory(category: MemoryCategory): Flow<List<Memory>>

    @Query("SELECT * FROM memories WHERE isFavorite = 1 ORDER BY date DESC")
    fun getFavoriteMemories(): Flow<List<Memory>>

    @Query("SELECT * FROM memories WHERE id = :id")
    suspend fun getMemoryById(id: Long): Memory?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: Memory): Long

    @Update
    suspend fun updateMemory(memory: Memory)

    @Delete
    suspend fun deleteMemory(memory: Memory)

    @Query("SELECT COUNT(*) FROM memories")
    fun getMemoryCount(): Flow<Int>
}

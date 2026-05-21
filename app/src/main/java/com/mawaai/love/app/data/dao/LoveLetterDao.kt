package com.mawaai.love.app.data.dao

import androidx.room.*
import com.mawaai.love.app.data.model.LoveLetter
import kotlinx.coroutines.flow.Flow

@Dao
interface LoveLetterDao {
    @Query("SELECT * FROM love_letters ORDER BY createdAt DESC")
    fun getAllLetters(): Flow<List<LoveLetter>>

    @Query("SELECT * FROM love_letters WHERE isFavorite = 1 ORDER BY createdAt DESC")
    fun getFavoriteLetters(): Flow<List<LoveLetter>>

    @Query("SELECT * FROM love_letters WHERE id = :id")
    suspend fun getLetterById(id: Long): LoveLetter?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLetter(letter: LoveLetter): Long

    @Update
    suspend fun updateLetter(letter: LoveLetter)

    @Delete
    suspend fun deleteLetter(letter: LoveLetter)
}

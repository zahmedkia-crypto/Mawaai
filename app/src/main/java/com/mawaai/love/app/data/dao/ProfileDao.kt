package com.mawaai.love.app.data.dao

import androidx.room.*
import com.mawaai.love.app.data.model.UserProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun getProfile(): Flow<UserProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateProfile(profile: UserProfile)
}

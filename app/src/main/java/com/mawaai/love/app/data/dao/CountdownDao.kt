package com.mawaai.love.app.data.dao

import androidx.room.*
import com.mawaai.love.app.data.model.Countdown
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO for [Countdown]. The default ordering is by ascending
 * [Countdown.targetDate] so the next-up countdown sits at the top of
 * the list — past targets fall to the bottom naturally because their
 * timestamps are smaller, BUT the home-screen card prefers only
 * upcoming targets via [getUpcoming].
 */
@Dao
interface CountdownDao {

    @Query("SELECT * FROM countdowns ORDER BY targetDate ASC")
    fun getAll(): Flow<List<Countdown>>

    /** Returns countdowns whose target is in the future, soonest first. */
    @Query("SELECT * FROM countdowns WHERE targetDate >= :nowMillis ORDER BY targetDate ASC")
    fun getUpcoming(nowMillis: Long): Flow<List<Countdown>>

    @Query("SELECT * FROM countdowns WHERE isFavorite = 1 ORDER BY targetDate ASC")
    fun getFavorites(): Flow<List<Countdown>>

    @Query("SELECT * FROM countdowns WHERE id = :id")
    suspend fun getById(id: Long): Countdown?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(countdown: Countdown): Long

    @Update
    suspend fun update(countdown: Countdown)

    @Delete
    suspend fun delete(countdown: Countdown)
}

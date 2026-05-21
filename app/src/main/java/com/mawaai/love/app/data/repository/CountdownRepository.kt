package com.mawaai.love.app.data.repository

import com.mawaai.love.app.data.dao.CountdownDao
import com.mawaai.love.app.data.model.Countdown
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin Room wrapper for [Countdown]. Mirrors [MemoryRepository]'s
 * approach of running mutation calls inside [NonCancellable] so a fast
 * back-press right after "save" doesn't drop the write on slow devices.
 */
@Singleton
class CountdownRepository @Inject constructor(
    private val dao: CountdownDao
) {
    fun getAll(): Flow<List<Countdown>> = dao.getAll()
    fun getUpcoming(nowMillis: Long = System.currentTimeMillis()): Flow<List<Countdown>> =
        dao.getUpcoming(nowMillis)

    suspend fun getById(id: Long): Countdown? = dao.getById(id)

    suspend fun add(countdown: Countdown): Long = withContext(NonCancellable) {
        dao.insert(countdown)
    }

    suspend fun update(countdown: Countdown) = withContext(NonCancellable) {
        dao.update(countdown)
    }

    suspend fun delete(countdown: Countdown) = withContext(NonCancellable) {
        dao.delete(countdown)
    }
}

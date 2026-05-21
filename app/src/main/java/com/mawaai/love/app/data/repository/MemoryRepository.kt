package com.mawaai.love.app.data.repository

import android.net.Uri
import com.mawaai.love.app.core.utils.FileUtils
import com.mawaai.love.app.data.dao.MemoryDao
import com.mawaai.love.app.data.model.Memory
import com.mawaai.love.app.data.model.MemoryCategory
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemoryRepository @Inject constructor(
    private val dao: MemoryDao,
    private val fileUtils: FileUtils
) {
    fun getAllMemories(): Flow<List<Memory>> = dao.getAllMemories()
    fun getFavorites(): Flow<List<Memory>> = dao.getFavoriteMemories()
    fun getByCategory(cat: MemoryCategory) = dao.getMemoriesByCategory(cat)

    /**
     * Persists a memory in a NonCancellable context so the write completes
     * even if the caller's ViewModelScope is cancelled by a fast back-press
     * right after the save request. The previous version could silently drop
     * the insert on slow devices — that was the root cause of "save not
     * working" reports.
     */
    suspend fun addMemory(memory: Memory, imageUri: Uri?): Result<Long> =
        withContext(NonCancellable) {
            try {
                val localPath = imageUri?.let { fileUtils.copyImageToInternal(it) }
                val id = dao.insertMemory(memory.copy(imagePath = localPath))
                Result.success(id)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun deleteMemory(memory: Memory) = withContext(NonCancellable) {
        memory.imagePath?.let { fileUtils.deleteFile(it) }
        dao.deleteMemory(memory)
    }
}

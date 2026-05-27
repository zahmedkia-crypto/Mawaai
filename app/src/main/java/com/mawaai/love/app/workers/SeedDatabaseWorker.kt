package com.mawaai.love.app.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mawaai.love.app.data.database.MockupSeed
import com.mawaai.love.app.data.database.dao.ProductMockupDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SeedDatabaseWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val productMockupDao: ProductMockupDao
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val existing = productMockupDao.getMockupCount()
            if (existing == 0) {
                productMockupDao.insertMockups(MockupSeed.ALL)
            }
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}

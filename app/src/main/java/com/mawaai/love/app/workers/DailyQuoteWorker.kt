package com.mawaai.love.app.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mawaai.love.app.core.notifications.MawaaiNotificationManager
import com.mawaai.love.app.core.utils.QuoteUtils
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class DailyQuoteWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val notificationManager: MawaaiNotificationManager
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val quote = QuoteUtils.getDailyQuote()
        notificationManager.showNotification(
            title = "💕 مأواي — لحظة حب لكِ",
            body = quote,
            channelId = MawaaiNotificationManager.CHANNEL_LOVE_QUOTES
        )
        return Result.success()
    }
}

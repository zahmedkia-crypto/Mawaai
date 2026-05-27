package com.mawaai.love.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.*
import com.mawaai.love.app.core.lifecycle.ForegroundResumeTracker
import com.mawaai.love.app.core.notifications.MawaaiNotificationManager
import com.mawaai.love.app.core.opencv.OpenCVBootstrap
import com.mawaai.love.app.workers.DailyQuoteWorker
import com.mawaai.love.app.workers.SeedDatabaseWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class MawaaiApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var notificationManager: MawaaiNotificationManager
    @Inject lateinit var foregroundResumeTracker: ForegroundResumeTracker

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // Load libopencv_java4.so eagerly so direct OpenCV consumers
        // (TemplateCompositor, GarmentColorEngine, …) never race against
        // AIEngine's lazy init and crash with UnsatisfiedLinkError on Mat().
        // Phase 17 hardening: pass `this` so the bootstrap can dump the
        // device's supported ABIs and the .so files actually present in
        // applicationInfo.nativeLibraryDir at startup. The output pinpoints
        // missing-ABI vs. broken-AAR vs. JNI-version-mismatch failures
        // without needing a debugger or device-side strings dump.
        OpenCVBootstrap.init(this)
        notificationManager.createNotificationChannels()
        scheduleDailyWorkers()
        scheduleOneTimeWorkers()
        // Process-wide observer: counts background time across Activity
        // restarts so the 30s intro-replay rule reflects real user absence.
        ProcessLifecycleOwner.get().lifecycle.addObserver(foregroundResumeTracker)
    }

    private fun scheduleDailyWorkers() {
        val workManager = WorkManager.getInstance(this)
        val initialDelayMillis = millisUntilNext(hourOfDay = 9)

        val dailyQuoteRequest = PeriodicWorkRequestBuilder<DailyQuoteWorker>(1, TimeUnit.DAYS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
            .build()

        workManager.enqueueUniquePeriodicWork(
            "daily_quote",
            ExistingPeriodicWorkPolicy.KEEP,
            dailyQuoteRequest
        )
    }

    private fun scheduleOneTimeWorkers() {
        val workManager = WorkManager.getInstance(this)
        val seedRequest = OneTimeWorkRequestBuilder<SeedDatabaseWorker>()
            .build()
        
        workManager.enqueueUniqueWork(
            "seed_database",
            ExistingWorkPolicy.KEEP,
            seedRequest
        )
    }

    private fun millisUntilNext(hourOfDay: Int): Long {
        val now = Calendar.getInstance()
        val target = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, hourOfDay)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= now.timeInMillis) add(Calendar.DAY_OF_YEAR, 1)
        }
        return target.timeInMillis - now.timeInMillis
    }
}

package com.mawaai.love.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
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
        // Notification channels MUST be registered before any worker posts
        // — DailyQuoteWorker posts on its first run, which could be within
        // a few hundred ms on a healthy device. Keep this synchronous.
        notificationManager.createNotificationChannels()
        // Process-wide observer: counts background time across Activity
        // restarts so the 30s intro-replay rule reflects real user absence.
        ProcessLifecycleOwner.get().lifecycle.addObserver(foregroundResumeTracker)

        // MT-018 (2026-05-28): defer worker scheduling to the first foregrounding.
        // WorkManager itself is on-demand initialised (see AndroidManifest's
        // `tools:node="remove"` for `WorkManagerInitializer`), so
        // `WorkManager.getInstance(this)` inside `scheduleDailyWorkers` /
        // `scheduleOneTimeWorkers` will trigger the Hilt-backed Configuration
        // boot. Moving it to `ON_START` lets cold-start return to the system
        // a few ms earlier and shifts the WorkManager init off the critical
        // first-frame path.
        ProcessLifecycleOwner.get().lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) {
                    // Run exactly once per process. Subsequent foreground
                    // events are no-ops; WorkManager's `KEEP` policies are
                    // idempotent so even re-running would be harmless.
                    owner.lifecycle.removeObserver(this)
                    scheduleDailyWorkers()
                    scheduleOneTimeWorkers()
                }
            }
        )
    }

    private fun scheduleDailyWorkers() {
        val workManager = WorkManager.getInstance(this)
        val initialDelayMillis = millisUntilNext(hourOfDay = 9)

        val dailyQuoteRequest = PeriodicWorkRequestBuilder<DailyQuoteWorker>(1, TimeUnit.DAYS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
            .build()

        workManager.enqueueUniquePeriodicWork(
            DAILY_QUOTE_WORK_NAME,
            // MT-019: KEEP, never REPLACE. KEEP preserves the original
            // schedule across cold starts so the 9 AM cadence is stable.
            // REPLACE would reset the period to "1 day from now" on every
            // process restart, which would mean the daily quote silently
            // drifts later each time the app is opened during the day.
            ExistingPeriodicWorkPolicy.KEEP,
            dailyQuoteRequest,
        )
    }

    private fun scheduleOneTimeWorkers() {
        val workManager = WorkManager.getInstance(this)
        val seedRequest = OneTimeWorkRequestBuilder<SeedDatabaseWorker>()
            .build()

        workManager.enqueueUniqueWork(
            SEED_DATABASE_WORK_NAME,
            // MT-019: KEEP for the same reason as the periodic path. If the
            // seed has already run, we don't want a fresh enqueue to undo
            // any user edits to the seeded data.
            ExistingWorkPolicy.KEEP,
            seedRequest,
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

    companion object {
        const val DAILY_QUOTE_WORK_NAME = "daily_quote"
        const val SEED_DATABASE_WORK_NAME = "seed_database"
    }
}

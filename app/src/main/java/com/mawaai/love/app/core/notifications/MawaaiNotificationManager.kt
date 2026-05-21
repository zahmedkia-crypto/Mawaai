package com.mawaai.love.app.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.mawaai.love.app.MainActivity
import com.mawaai.love.app.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MawaaiNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val CHANNEL_LOVE_QUOTES = "love_quotes"
    }

    fun createNotificationChannels() {
        // minSdk = 26 (Build.VERSION_CODES.O), so the SDK_INT guard the
        // method previously carried is always-true — Phase 6 cleanup.
        val loveQuotesChannel = NotificationChannel(
            CHANNEL_LOVE_QUOTES,
            "رسائل الحب اليومية",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannels(listOf(loveQuotesChannel))
    }

    fun showNotification(title: String, body: String, channelId: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setColor(0xFFE8A7B5.toInt()) // RoseGold
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}

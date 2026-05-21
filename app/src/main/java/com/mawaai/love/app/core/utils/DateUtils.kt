package com.mawaai.love.app.core.utils

import android.text.format.DateUtils
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

object DateUtils {
    fun formatArabicDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMMM yyyy", Locale("ar"))
        return sdf.format(Date(timestamp))
    }

    fun getTimeGreeting(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 6..11 -> "صباح الورد يا رزان ☀️"
            in 12..16 -> "وقت الغداء... فكّرت فيكِ 💕"
            in 17..20 -> "مساء الحب يا رزان 🌙"
            else -> "تصبحين على خير يا حبيبتي 🌟"
        }
    }

    fun getDaysUntil(targetTimestamp: Long): Long {
        val diff = targetTimestamp - System.currentTimeMillis()
        return if (diff > 0) TimeUnit.MILLISECONDS.toDays(diff) else 0
    }

    fun isSameDay(ts1: Long, ts2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = ts1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = ts2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
}

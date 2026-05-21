package com.mawaai.love.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A user-defined countdown target — wedding day, anniversary, trip,
 * Eid, etc. The Hijri equivalent is intentionally NOT stored; it is
 * derived at render time from [targetDate] via [com.mawaai.love.app
 * .data.remote.aladhan.AladhanClient] so the value stays fresh if the
 * conversion lookup table is ever updated upstream.
 *
 * [iconKey] is a free-form short identifier that the UI maps to a
 * Material icon (e.g. "wedding", "travel", "eid", "anniversary"). The
 * default is empty so older rows render with a generic heart icon.
 */
@Entity(tableName = "countdowns")
data class Countdown(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val targetDate: Long,           // epoch millis, midnight in user's local TZ
    val iconKey: String = "",
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

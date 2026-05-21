package com.mawaai.love.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val userName: String = "",
    val partnerName: String = "رزان",
    val engagementDate: Long? = null,
    val profileImagePath: String? = null,
    val selectedTheme: ThemeVariant = ThemeVariant.ROSE,
    val themeMode: BackgroundTheme = BackgroundTheme.AUTO,
    val morningNotifHour: Int = 8,
    val eveningNotifHour: Int = 20,
    val notificationsEnabled: Boolean = true,
    val biometricEnabled: Boolean = false
)

enum class ThemeVariant { ROSE, GOLD, PURPLE, RED }

/**
 * AUTO picks MORNING (06:00–17:59) or NIGHT (18:00–05:59) based on the
 * device clock. MORNING / NIGHT lock to that background regardless of time.
 */
enum class BackgroundTheme { AUTO, MORNING, NIGHT }

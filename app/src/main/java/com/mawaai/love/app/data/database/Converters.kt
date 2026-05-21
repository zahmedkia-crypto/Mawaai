package com.mawaai.love.app.data.database

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mawaai.love.app.data.model.*

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromMemoryCategory(value: MemoryCategory): String = value.name

    @TypeConverter
    fun toMemoryCategory(value: String): MemoryCategory = MemoryCategory.valueOf(value)

    @TypeConverter
    fun fromMoodType(value: MoodType): String = value.name

    @TypeConverter
    fun toMoodType(value: String): MoodType = MoodType.valueOf(value)

    @TypeConverter
    fun fromThemeVariant(value: ThemeVariant): String = value.name

    @TypeConverter
    fun toThemeVariant(value: String): ThemeVariant = ThemeVariant.valueOf(value)

    @TypeConverter
    fun fromBackgroundTheme(value: BackgroundTheme): String = value.name

    @TypeConverter
    fun toBackgroundTheme(value: String): BackgroundTheme =
        runCatching { BackgroundTheme.valueOf(value) }.getOrDefault(BackgroundTheme.AUTO)

    @TypeConverter
    fun fromStringList(value: List<String>): String = gson.toJson(value)

    @TypeConverter
    fun toStringList(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType)
    }
}

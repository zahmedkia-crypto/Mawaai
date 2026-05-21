package com.mawaai.love.app.data.remote.aladhan

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Domain-layer wrapper around [AladhanApi] for Hijri ↔ Gregorian
 * conversion. Used by the countdown feature to enrich event dates with
 * their Islamic equivalents and by the romantic side to surface
 * upcoming Islamic holidays as ready-made countdown targets.
 *
 * - Returns null on any failure (network, non-200 envelope, parse).
 *   Callers should fall back to a Gregorian-only display.
 * - All work hops to `Dispatchers.IO` to keep the UI thread free.
 * - Date formatting is centralised here — the API expects `DD-MM-YYYY`
 *   strictly, and getting it wrong yields a confusing 400 with a
 *   plaintext-encoded `data` field.
 */
@Singleton
class AladhanClient @Inject constructor(
    private val api: AladhanApi
) {

    /** Today's Hijri equivalent. Convenient shorthand for the most common UI need. */
    suspend fun todayHijri(): HijriDay? = gregorianToHijri(LocalDate.now())

    /**
     * Returns the Hijri equivalent of [date] — a [HijriDay] with the
     * day number, Arabic + English month name, year, weekday in both
     * languages, and any Islamic holidays falling on that day.
     */
    suspend fun gregorianToHijri(date: LocalDate): HijriDay? = withContext(Dispatchers.IO) {
        val formatted = date.format(API_DATE_FORMAT)
        runCatching {
            val response = api.gregorianToHijri(formatted)
            val envelope = response.body()
            if (response.isSuccessful && envelope?.code == OK_CODE) {
                envelope.data?.hijri?.toDomain()
            } else {
                Log.w(TAG, "gToH non-200 for $formatted: ${envelope?.status} (${response.code()})")
                null
            }
        }.onFailure { Log.w(TAG, "gToH threw for $formatted", it) }.getOrNull()
    }

    /**
     * Reverse direction: Hijri-formatted [date] (DD-MM-YYYY) → Gregorian
     * [LocalDate]. Used by countdowns whose target was specified as a
     * Hijri date (e.g. user picked "Eid al-Fitr 1447").
     */
    suspend fun hijriToGregorian(date: String): LocalDate? = withContext(Dispatchers.IO) {
        runCatching {
            val response = api.hijriToGregorian(date)
            val envelope = response.body()
            if (response.isSuccessful && envelope?.code == OK_CODE) {
                envelope.data?.gregorian?.let { greg ->
                    LocalDate.parse(greg.date, API_DATE_FORMAT)
                }
            } else {
                Log.w(TAG, "hToG non-200 for $date: ${envelope?.status} (${response.code()})")
                null
            }
        }.onFailure { Log.w(TAG, "hToG threw for $date", it) }.getOrNull()
    }

    /**
     * Full Hijri calendar for [hijriYear]. The list is ordered by
     * Gregorian date and may span ~355 entries (one per Hijri day in
     * the year). Filtering for non-empty `holidays` gives the
     * occasion-picker its candidate set.
     */
    suspend fun hijriYearHolidays(hijriYear: Int): List<HolidayEntry> = withContext(Dispatchers.IO) {
        runCatching {
            val response = api.hijriCalendar(hijriYear)
            val envelope = response.body()
            if (response.isSuccessful && envelope?.code == OK_CODE) {
                envelope.data
                    ?.mapNotNull { entry ->
                        val hijri = entry.hijri ?: return@mapNotNull null
                        val holidays = hijri.holidays.orEmpty()
                        if (holidays.isEmpty()) return@mapNotNull null
                        val gregorian = entry.gregorian?.date
                            ?.let { runCatching { LocalDate.parse(it, API_DATE_FORMAT) }.getOrNull() }
                            ?: return@mapNotNull null
                        val hijriDomain = hijri.toDomain() ?: return@mapNotNull null
                        HolidayEntry(
                            gregorianDate = gregorian,
                            hijri = hijriDomain,
                            names = holidays
                        )
                    }
                    .orEmpty()
            } else {
                Log.w(TAG, "hijriCalendar non-200 for $hijriYear: ${envelope?.status}")
                emptyList()
            }
        }.onFailure { Log.w(TAG, "hijriCalendar threw for $hijriYear", it) }
            .getOrDefault(emptyList())
    }

    private fun AladhanHijriDate.toDomain(): HijriDay? {
        val dayNum = day.toIntOrNull() ?: return null
        val yearNum = year.toIntOrNull() ?: return null
        val monthNum = month?.number ?: return null
        return HijriDay(
            day = dayNum,
            monthNumber = monthNum,
            monthArabic = month.arabicName.orEmpty(),
            monthEnglish = month.englishName.orEmpty(),
            year = yearNum,
            weekdayArabic = weekday?.arabicName.orEmpty(),
            weekdayEnglish = weekday?.englishName.orEmpty(),
            holidays = holidays.orEmpty()
        )
    }

    /**
     * Trimmed domain model for a single Hijri day. Strips the wire
     * format's nested envelopes and string-typed numerics.
     */
    data class HijriDay(
        val day: Int,
        val monthNumber: Int,
        val monthArabic: String,
        val monthEnglish: String,
        val year: Int,
        val weekdayArabic: String,
        val weekdayEnglish: String,
        val holidays: List<String>
    )

    /** A single holiday with its Gregorian + Hijri coordinates. */
    data class HolidayEntry(
        val gregorianDate: LocalDate,
        val hijri: HijriDay,
        val names: List<String>
    )

    private companion object {
        const val TAG = "AladhanClient"
        const val OK_CODE = 200
        // Aladhan's strict input format — single-digit values must be
        // zero-padded. The locale anchor avoids `dd-MM-yyyy` accidentally
        // resolving Arabic-Indic digits on devices with `ar-EG` locale.
        val API_DATE_FORMAT: DateTimeFormatter =
            DateTimeFormatter.ofPattern("dd-MM-yyyy", Locale.ROOT)
    }
}

package com.mawaai.love.app.data.remote.aladhan

import com.google.gson.annotations.SerializedName

/**
 * Aladhan envelopes every payload in `{ code, status, data }` so we
 * unify the shape with a generic carrier. Errors come back as
 * `{ code: 4xx, status: "ERROR", data: <message> }` — the client only
 * forwards `data` to the domain layer when `code == 200`.
 */
data class AladhanEnvelope<T>(
    @SerializedName("code") val code: Int,
    @SerializedName("status") val status: String,
    @SerializedName("data") val data: T?
)

/**
 * Combined response carrying both calendar systems for the same day.
 * The conversion endpoints return this object directly; the calendar
 * endpoint returns a list of these objects covering an entire month.
 */
data class AladhanDateResponse(
    @SerializedName("hijri") val hijri: AladhanHijriDate?,
    @SerializedName("gregorian") val gregorian: AladhanGregorianDate?
)

/**
 * Hijri-side breakdown. We expose [day], [month.ar], [year], and the
 * weekday Arabic name to the UI; everything else (designation,
 * romanised month name, holidays) is available if a feature needs it.
 */
data class AladhanHijriDate(
    @SerializedName("date") val date: String,         // "DD-MM-YYYY"
    @SerializedName("day") val day: String,
    @SerializedName("month") val month: AladhanMonth?,
    @SerializedName("year") val year: String,
    @SerializedName("weekday") val weekday: AladhanWeekday?,
    @SerializedName("holidays") val holidays: List<String>?
)

data class AladhanGregorianDate(
    @SerializedName("date") val date: String,
    @SerializedName("day") val day: String,
    @SerializedName("month") val month: AladhanMonth?,
    @SerializedName("year") val year: String,
    @SerializedName("weekday") val weekday: AladhanWeekday?
)

data class AladhanMonth(
    @SerializedName("number") val number: Int,
    @SerializedName("en") val englishName: String?,
    @SerializedName("ar") val arabicName: String?
)

data class AladhanWeekday(
    @SerializedName("en") val englishName: String?,
    @SerializedName("ar") val arabicName: String?
)

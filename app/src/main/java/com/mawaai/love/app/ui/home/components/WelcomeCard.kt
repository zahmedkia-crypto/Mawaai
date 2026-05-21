package com.mawaai.love.app.ui.home.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mawaai.love.app.core.theme.AmiriFamily
import com.mawaai.love.app.core.theme.CairoFamily
import com.mawaai.love.app.core.theme.MawaaiColors
import com.mawaai.love.app.data.remote.aladhan.AladhanClient

@Composable
fun WelcomeCard(
    greeting: String,
    partnerName: String,
    hijri: AladhanClient.HijriDay? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        Text(
            text = greeting,
            fontFamily = AmiriFamily,
            fontSize = 24.sp,
            color = MawaaiColors.RoseGold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = partnerName,
            fontFamily = CairoFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 32.sp,
            color = MawaaiColors.PearlWhite
        )
        if (hijri != null) {
            // Hijri date sits as a quiet third line — same column as the
            // greeting + name, slightly dimmer so it reads as ambient
            // context rather than chrome. Format: "اليوم رقم شهر سنة هـ"
            // e.g. "الجمعة • 17 شَوّال 1447 هـ".
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = hijri.formatRomanticArabic(),
                fontFamily = AmiriFamily,
                fontSize = 16.sp,
                color = MawaaiColors.TextPoetic
            )
        }
    }
}

/**
 * Renders the Hijri day in a poetic Arabic form suitable for the
 * romantic-side palette. Falls back to numeric month names when the
 * server-supplied Arabic month name is empty (rare but possible when
 * the upstream API hiccups).
 */
private fun AladhanClient.HijriDay.formatRomanticArabic(): String {
    val monthLabel = monthArabic.takeIf { it.isNotBlank() } ?: "شهر $monthNumber"
    val weekdayPart = weekdayArabic.takeIf { it.isNotBlank() }?.let { "$it • " } ?: ""
    return "$weekdayPart$day $monthLabel $year هـ"
}

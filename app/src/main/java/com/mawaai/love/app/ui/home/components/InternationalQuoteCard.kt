package com.mawaai.love.app.ui.home.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mawaai.love.app.R
import com.mawaai.love.app.core.components.RoseGlassCard
import com.mawaai.love.app.core.theme.MawaaiColors
import com.mawaai.love.app.data.remote.zenquotes.ZenQuotesClient

/**
 * Secondary inspirational-quote card driven by ZenQuotes' /api/today
 * endpoint. Sits below [DailyQuoteCard] (which carries the curated
 * Arabic voice) so the two coexist without competing — the local card
 * is the primary romantic message, this one is the global / worldly
 * supplement.
 *
 * Renders as a quieter variant of [RoseGlassCard]: smaller text, no
 * leading quote icon, italic body, gold author byline, and the
 * mandatory ZenQuotes attribution byline below. Hides itself entirely
 * when [quote] is null (offline / rate-limited / empty body) so the
 * layout doesn't show an empty card.
 */
@Composable
fun InternationalQuoteCard(quote: ZenQuotesClient.Quote?) {
    if (quote == null) return

    RoseGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "\u201C${quote.text}\u201D",
                fontStyle = FontStyle.Italic,
                fontSize = 15.sp,
                color = MawaaiColors.TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            if (!quote.author.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "\u2014 ${quote.author}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MawaaiColors.ChampagneGold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.zenquotes_attribution),
                fontSize = 10.sp,
                color = MawaaiColors.TextHint,
                textAlign = TextAlign.Center
            )
        }
    }
}

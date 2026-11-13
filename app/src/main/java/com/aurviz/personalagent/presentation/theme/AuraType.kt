package com.aurviz.personalagent.presentation.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.aurviz.personalagent.R

/**
 * Repline ships as a single Regular weight, so we never ask for Bold/Italic
 * (FontSynthesis.None stops Android from faking them, which looks smeared).
 * Hierarchy comes from size, tracking and colour instead of weight.
 *
 * Note: Repline has no "…" glyph, so use three dots "..." in strings.
 */
val Repline = FontFamily(Font(R.font.repline_regular, FontWeight.Normal))

object AuraType {
    val base = TextStyle(
        fontFamily = Repline,
        fontWeight = FontWeight.Normal,
        fontSynthesis = FontSynthesis.None,
    )

    val wordmark = base.copy(fontSize = 56.sp, lineHeight = 60.sp, letterSpacing = 14.sp)
    val greeting = base.copy(fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = 0.2.sp)
    val subtitle = base.copy(fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = 1.4.sp)
    val topBar = base.copy(fontSize = 20.sp, lineHeight = 24.sp, letterSpacing = 4.sp)
    val title = base.copy(fontSize = 20.sp, lineHeight = 26.sp, letterSpacing = 0.5.sp)
    val chip = base.copy(fontSize = 15.sp, lineHeight = 18.sp, letterSpacing = 0.3.sp)
    val input = base.copy(fontSize = 17.sp, lineHeight = 24.sp, letterSpacing = 0.2.sp)
    val body = base.copy(fontSize = 15.sp, lineHeight = 22.sp, letterSpacing = 0.1.sp)
    val bodySmall = base.copy(fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = 0.1.sp)
    val label = base.copy(fontSize = 13.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp)
    val nav = base.copy(fontSize = 13.sp, lineHeight = 16.sp, letterSpacing = 0.6.sp)
    val bubble = base.copy(fontSize = 15.sp, lineHeight = 20.sp)
}

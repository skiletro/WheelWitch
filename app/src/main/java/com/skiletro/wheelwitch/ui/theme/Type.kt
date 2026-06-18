package com.skiletro.wheelwitch.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.skiletro.wheelwitch.R

/**
 * Display font used for player names, race numbers, and other short
 * numeric/branded text. Applied per-call-site via `fontFamily =
 * CtmkfFontFamily` rather than wired into [Typography], because
 * [Typography] intentionally uses [FontFamily.Default] for body text
 * to keep long-form copy readable.
 */
val CtmkfFontFamily = FontFamily(Font(R.font.ctmkf))

/**
 * App typography. Only [Typography.bodyLarge] is overridden (to match
 * the Material 3 default explicitly); all other styles use Material 3
 * defaults.
 */
val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
)

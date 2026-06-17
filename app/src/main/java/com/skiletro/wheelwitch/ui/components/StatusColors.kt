package com.skiletro.wheelwitch.ui.components

import androidx.compose.ui.graphics.Color

/**
 * Color quadruple for an OK / Error status pair.
 *
 * - [okContainer] / [onOkContainer] are intended for filled cards (dark
 *   green background with light text).
 * - [ok] is the indicator / label color used on neutral surfaces
 *   (`surfaceVariant`).
 * - The `error*` fields are the equivalent for failure / non-OK states.
 */
data class StatusColors(
    val okContainer: Color,
    val onOkContainer: Color,
    val ok: Color,
    val errorContainer: Color,
    val onErrorContainer: Color,
    val error: Color
)

/**
 * Returns the standard OK / Error color quadruple.
 *
 * Phase 8 will replace this with a theme-aware composition local; for now
 * the colors are static.
 */
fun statusColors(): StatusColors = StatusColors(
    okContainer = Color(0xFF1B5E20),
    onOkContainer = Color.White,
    ok = Color(0xFF4CAF50),
    errorContainer = Color(0xFFB71C1C),
    onErrorContainer = Color.White,
    error = Color(0xFFEF5350)
)

/**
 * Returns the lighter indicator color matching the [isOk] state.
 * Convenience for the common `color = if (isOk) ok else error` pattern.
 */
fun StatusColors.indicator(isOk: Boolean): Color = if (isOk) ok else error

/**
 * Returns the container + onContainer pair matching the [isOk] state.
 */
fun StatusColors.container(isOk: Boolean): Pair<Color, Color> =
    if (isOk) okContainer to onOkContainer
    else errorContainer to onErrorContainer

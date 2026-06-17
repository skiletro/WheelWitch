package com.skiletro.wheelwitch.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.skiletro.wheelwitch.ui.theme.LocalStatusColors
import com.skiletro.wheelwitch.ui.theme.StatusColors

/**
 * Convenience accessor for the active [StatusColors] from [LocalStatusColors].
 * Re-exports the theme-side type so callers can `import` it from one place.
 */
@Composable
@ReadOnlyComposable
fun statusColors(): StatusColors = LocalStatusColors.current

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

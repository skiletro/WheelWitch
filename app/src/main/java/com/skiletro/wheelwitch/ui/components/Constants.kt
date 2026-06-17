package com.skiletro.wheelwitch.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Shared corner radius used by primary action buttons throughout the app.
 */
val buttonShape = RoundedCornerShape(14.dp)

/**
 * Shared corner radius used by content sections (cards, panels, etc).
 */
val sectionShape = RoundedCornerShape(20.dp)

/**
 * Modifier that draws a 3dp focus border in the given [color] (defaulting to the
 * theme's primary color) when [isFocused] is true.
 */
@Composable
fun Modifier.focusBorder(
    isFocused: Boolean,
    shape: RoundedCornerShape = buttonShape,
    color: Color = MaterialTheme.colorScheme.primary
): Modifier = this.then(
    if (isFocused) Modifier.border(width = 3.dp, color = color, shape = shape)
    else Modifier
)

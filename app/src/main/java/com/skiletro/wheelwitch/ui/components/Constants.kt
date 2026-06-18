package com.skiletro.wheelwitch.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/** Color of the dark inner edge drawn inside the focus ring so it stays visible on primary-colored buttons. */
private val InnerEdgeColor = Color.Black.copy(alpha = 0.25f)

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
 * theme's primary color) when [isFocused] is true. A thin 1.5dp dark inner edge
 * is added so the ring remains visible even on primary-colored buttons.
 */
@Composable
fun Modifier.focusBorder(
    isFocused: Boolean,
    shape: RoundedCornerShape = buttonShape,
    color: Color = MaterialTheme.colorScheme.primary
): Modifier = this.then(
    if (isFocused) {
        Modifier.drawWithContent {
            drawContent()
            val outerCr = shape.topStart
            val strokePx = 3.dp.toPx()
            val innerInsetPx = 2.dp.toPx()
            val innerCr = (outerCr.toPx(size, this) - innerInsetPx).coerceAtLeast(0f)
            drawRoundRect(
                color = color,
                style = Stroke(strokePx),
                cornerRadius = CornerRadius(outerCr.toPx(size, this))
            )
            drawRoundRect(
                color = InnerEdgeColor,
                style = Stroke(1.5.dp.toPx()),
                cornerRadius = CornerRadius(innerCr),
                topLeft = Offset(innerInsetPx, innerInsetPx),
                size = Size(size.width - 2 * innerInsetPx, size.height - 2 * innerInsetPx)
            )
        }
    } else Modifier
)

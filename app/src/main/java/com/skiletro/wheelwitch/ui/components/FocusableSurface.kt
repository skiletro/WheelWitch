package com.skiletro.wheelwitch.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape

/**
 * A focusable card that draws a focus border when focused and optionally
 * handles click events.
 *
 * Replaces the repeated `.clickable {} + .focusable() + .onFocusChanged {} +
 * .focusBorder()` + `Surface` pattern that was copy-pasted across screens.
 *
 * - When [onClick] is non-null and [enabled] is true, the surface is
 *   clickable; otherwise it is focusable only.
 * - Focus state is hoisted to a `remember` scope (NOT inside the clickable
 *   lambda) so the state survives recomposition correctly.
 */
@Composable
fun FocusableSurface(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    shape: Shape = buttonShape,
    color: Color = MaterialTheme.colorScheme.surfaceVariant,
    content: @Composable () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    val clickModifier = if (onClick != null && enabled) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }

    Surface(
        shape = shape,
        color = color,
        modifier = modifier
            .then(clickModifier)
            .focusable(enabled = enabled)
            .onFocusChanged { isFocused = it.isFocused }
            .focusBorder(
                isFocused = isFocused,
                shape = shape as? RoundedCornerShape ?: buttonShape
            )
    ) {
        content()
    }
}

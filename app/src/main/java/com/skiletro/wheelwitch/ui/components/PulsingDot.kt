package com.skiletro.wheelwitch.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Animated status indicator dot.
 *
 * - The fill color smoothly transitions to [target] over [transitionMs].
 * - When [pulse] is true, the dot's alpha is animated by an
 *   [infiniteTransition] to give a subtle "alive" heartbeat feel.
 */
@Composable
fun PulsingDot(
    target: Color,
    pulse: Boolean = false,
    sizeDp: Dp = 10.dp,
    transitionMs: Int = 400,
    modifier: Modifier = Modifier
) {
    val color by animateColorAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = transitionMs, easing = LinearEasing),
        label = "dot_color"
    )

    val transition = rememberInfiniteTransition(label = "dot_pulse")
    val pulseAlpha by transition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_alpha"
    )

    Box(
        modifier = modifier
            .size(sizeDp)
            .clip(CircleShape)
            .background(color.copy(alpha = if (pulse) pulseAlpha else 1f))
    )
}

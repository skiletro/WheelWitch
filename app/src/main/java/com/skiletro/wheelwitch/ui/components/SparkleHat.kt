package com.skiletro.wheelwitch.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.skiletro.wheelwitch.R
import kotlin.math.cos
import kotlin.math.sin

private const val SPARKLE_COUNT = 6
private const val SPARKLE_PERIOD_MS = 3000
private val SPARKLE_INDICES = listOf(0, 3, 4)

/**
 * Renders the wizard hat icon with an animated sparkle overlay.
 *
 * The [tint] controls the sparkle color (with full alpha) and is also used
 * for the hat icon. The sparkles orbit at a fixed proportion of the [hatSize].
 *
 * Apply external `Modifier.offset(y = bob.dp)` to add a vertical bob if
 * desired — the bob animation is not encapsulated here so callers can
 * combine it with other offsets (e.g. a launch animation).
 */
@Composable
fun SparkleHat(
    hatSize: androidx.compose.ui.unit.Dp,
    tint: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "sparkle")
    val sparklePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = SPARKLE_PERIOD_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sparklePhase"
    )

    Box(
        modifier = modifier
            .size(hatSize + 30.dp)
            .drawBehind {
                val radius = (hatSize.value * 0.58f).dp.toPx()
                val sparkleSize = 3.dp.toPx()
                val strokeW = 2.dp.toPx()
                val centerX = size.width / 2
                val centerY = size.height / 2

                for (i in SPARKLE_INDICES) {
                    val rawPhase = (sparklePhase + i.toFloat() / SPARKLE_COUNT) % 1f
                    val alpha = when {
                        rawPhase < 0.35f -> rawPhase / 0.35f
                        rawPhase < 0.65f -> 1f
                        else -> 1f - (rawPhase - 0.65f) / 0.35f
                    }

                    val angle = i.toFloat() / SPARKLE_COUNT * 2f * Math.PI.toFloat()
                    val x = centerX + radius * cos(angle)
                    val y = centerY + radius * sin(angle)

                    drawLine(tint.copy(alpha = alpha), Offset(x - sparkleSize, y), Offset(x + sparkleSize, y), strokeW)
                    drawLine(tint.copy(alpha = alpha), Offset(x, y - sparkleSize), Offset(x, y + sparkleSize), strokeW)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_hat_wizard),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(hatSize)
        )
    }
}

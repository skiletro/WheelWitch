package com.skiletro.wheelwitch.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.cos
import kotlin.math.sin

private const val APP_NAME = "Wheel Witch"
private const val PACK_NAME = "Retro Rewind Pack"
private const val SUBTITLE = "$PACK_NAME Manager"

@Composable
fun TopBar(
    onOpenSettings: () -> Unit,
    onLaunchMiiMaker: () -> Unit,
    miiMakerEnabled: Boolean,
    onOpenNetplay: () -> Unit,
    roomsEnabled: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "bob")
    val bobOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bobOffset"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.offset(y = bobOffset.dp),
            contentAlignment = Alignment.Center
        ) {
            SparkleOverlay()
            Icon(
                painter = painterResource(com.skiletro.wheelwitch.R.drawable.ic_hat_wizard),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(38.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = APP_NAME,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = SUBTITLE,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onOpenNetplay, enabled = roomsEnabled) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Online Rooms",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        TextButton(
            onClick = onLaunchMiiMaker,
            enabled = miiMakerEnabled
        ) {
            Icon(
                painter = painterResource(com.skiletro.wheelwitch.R.drawable.ic_tshirt),
                contentDescription = "Mii Maker",
                tint = if (miiMakerEnabled) MaterialTheme.colorScheme.onSurfaceVariant
                       else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                modifier = Modifier.size(24.dp)
            )
        }
        TextButton(onClick = onOpenSettings) {
            Text(
                text = "\u2699",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        ClockText()
    }
}

@Composable
private fun SparkleOverlay() {
    val phaseState = remember { mutableFloatStateOf(0f) }
    val tint = MaterialTheme.colorScheme.primary

    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { nanos ->
                phaseState.floatValue = (nanos / 1_000_000 % 3000) / 3000f
            }
        }
    }

    Box(
        modifier = Modifier
            .size(68.dp)
            .drawBehind {
                val sparklePhase = phaseState.floatValue
                val sparkleCount = 6
                val radius = 22.dp.toPx()
                val sparkleSize = 3.dp.toPx()
                val centerX = size.width / 2
                val centerY = size.height / 2
                val strokeW = 2.dp.toPx()

                for (i in 0 until sparkleCount) {
                    val rawPhase = (sparklePhase + i.toFloat() / sparkleCount) % 1f
                    val alpha = when {
                        rawPhase < 0.35f -> rawPhase / 0.35f
                        rawPhase < 0.65f -> 1f
                        else -> 1f - (rawPhase - 0.65f) / 0.35f
                    }

                    val angle = i.toFloat() / sparkleCount * 2f * kotlin.math.PI.toFloat()
                    val x = centerX + radius * cos(angle)
                    val y = centerY + radius * sin(angle)

                    drawLine(tint.copy(alpha = alpha), Offset(x - sparkleSize, y), Offset(x + sparkleSize, y), strokeW)
                    drawLine(tint.copy(alpha = alpha), Offset(x, y - sparkleSize), Offset(x, y + sparkleSize), strokeW)
                }
            }
    )
}

@Composable
private fun ClockText() {
    val timeText = remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            val now = LocalTime.now()
            timeText.value = now.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
            delay(60_000)
        }
    }

    Text(
        text = timeText.value,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

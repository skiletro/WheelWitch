package com.skiletro.wheelwitch.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.geometry.Offset

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.cos
import kotlin.math.sin

private const val APP_NAME = "Wheel Witch"
private const val PACK_NAME = "Retro Rewind Pack"
private const val SUBTITLE = "$PACK_NAME Manager"

private const val SPARKLE_COUNT = 6
private const val SPARKLE_PERIOD_MS = 3000
private val SPARKLE_INDICES = listOf(0, 3, 4)

@Composable
fun TopBar(
    onOpenSettings: () -> Unit,
    onLaunchMiiMaker: () -> Unit,
    miiMakerEnabled: Boolean,
    onOpenOnlineMenu: () -> Unit,
    onlineMenuEnabled: Boolean,
    onOpenSaveInfo: () -> Unit = {}
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
    IconButton(onClick = onOpenOnlineMenu, enabled = onlineMenuEnabled) {
        Icon(
            imageVector = Icons.Filled.Dns,
            contentDescription = "Online Menu",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    IconButton(onClick = onOpenSaveInfo) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = "Save Data",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    IconButton(
            onClick = onLaunchMiiMaker,
            enabled = miiMakerEnabled
        ) {
            Icon(
                imageVector = Icons.Filled.Checkroom,
                contentDescription = "Mii Maker",
                tint = if (miiMakerEnabled) MaterialTheme.colorScheme.onSurfaceVariant
                       else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                modifier = Modifier.size(24.dp)
            )
        }
        IconButton(onClick = onOpenSettings) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        ClockText()
    }
}

@Composable
private fun SparkleOverlay() {
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
    val tint = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .size(68.dp)
            .drawBehind {
                val radius = 22.dp.toPx()
                val sparkleSize = 3.dp.toPx()
                val centerX = size.width / 2
                val centerY = size.height / 2
                val strokeW = 2.dp.toPx()

                for (i in SPARKLE_INDICES) {
                    val rawPhase = (sparklePhase + i.toFloat() / SPARKLE_COUNT) % 1f
                    val alpha = when {
                        rawPhase < 0.35f -> rawPhase / 0.35f
                        rawPhase < 0.65f -> 1f
                        else -> 1f - (rawPhase - 0.65f) / 0.35f
                    }

                    val angle = i.toFloat() / SPARKLE_COUNT * 2f * kotlin.math.PI.toFloat()
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
    var timeText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (isActive) {
            val now = LocalTime.now()
            timeText = now.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
            delay(60_000)
        }
    }

    Text(
        text = timeText,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

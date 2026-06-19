package com.skiletro.wheelwitch.ui.components

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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.skiletro.wheelwitch.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private const val TopBarBobAmplitudeDp = 4
private const val TopBarBobPeriodMs = 1500
private const val ClockRefreshMs = 60_000L
private val TopBarHorizontalPadding = 20.dp
private val TopBarVerticalPadding = 16.dp
private const val DisabledIconAlpha = 0.38f

/**
 * Top app bar shown on the Home screen.
 *
 * Renders the brand name + subtitle, a clock, and four icon buttons:
 * online menu, save data (licenses), Mii Maker, and settings.
 *
 * [onLaunchMiiMaker] and [onOpenOnlineMenu] accept an `enabled` flag
 * that gates the corresponding button. The Mii Maker button also dims
 * its icon tint manually when disabled because the default Material
 * disabled styling is too subtle on this design.
 */
@Composable
fun TopBar(
    onOpenSettings: () -> Unit,
    onLaunchMiiMaker: () -> Unit,
    miiMakerEnabled: Boolean,
    onOpenOnlineMenu: () -> Unit,
    onlineMenuEnabled: Boolean,
    onOpenSaveInfo: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "bob")
    val bobOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -TopBarBobAmplitudeDp.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = TopBarBobPeriodMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bobOffset"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = TopBarHorizontalPadding, vertical = TopBarVerticalPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.offset(y = bobOffset.dp),
            contentAlignment = Alignment.Center
        ) {
            SparkleHat(hatSize = 38.dp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(R.string.topbar_subtitle),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onOpenOnlineMenu, enabled = onlineMenuEnabled) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_dns),
                contentDescription = stringResource(R.string.cd_online_menu),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onOpenSaveInfo) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_person),
                contentDescription = stringResource(R.string.cd_save_data),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(
            onClick = onLaunchMiiMaker,
            enabled = miiMakerEnabled
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_checkroom),
                contentDescription = stringResource(R.string.cd_mii_maker),
                tint = if (miiMakerEnabled) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = DisabledIconAlpha),
                modifier = Modifier.size(24.dp)
            )
        }
        IconButton(onClick = onOpenSettings) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_settings),
                contentDescription = stringResource(R.string.cd_settings),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        ClockText()
    }
}

/** Displays the current time, refreshing once per [ClockRefreshMs]. */
@Composable
private fun ClockText() {
    var timeText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (isActive) {
            val now = LocalTime.now()
            timeText = now.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
            delay(ClockRefreshMs)
        }
    }

    Text(
        text = timeText,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

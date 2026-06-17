package com.skiletro.wheelwitch.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.skiletro.wheelwitch.R
import com.skiletro.wheelwitch.model.HealthCheckItem
import com.skiletro.wheelwitch.model.MemoryInfo
import com.skiletro.wheelwitch.model.ServerHealth
import com.skiletro.wheelwitch.ui.components.ScreenHeader
import com.skiletro.wheelwitch.ui.components.container
import com.skiletro.wheelwitch.ui.components.indicator
import com.skiletro.wheelwitch.ui.components.statusColors
import com.skiletro.wheelwitch.ui.components.PrimaryActionButton


import com.skiletro.wheelwitch.viewmodel.HealthState
import com.skiletro.wheelwitch.viewmodel.OnlineViewModel

@OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)
@Composable
fun HealthScreen(
    viewModel: OnlineViewModel,
    sharedTransitionScope: androidx.compose.animation.SharedTransitionScope? = null,
    animatedContentScope: androidx.compose.animation.AnimatedVisibilityScope? = null,
) {
    val healthState by viewModel.healthState.collectAsState()
    val colors = statusColors()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ScreenHeader(
            title = stringResource(R.string.health_title),
            onBack = { viewModel.goBack() },
            onRefresh = { viewModel.fetchHealth() },
            titleModifier = com.skiletro.wheelwitch.ui.components.sharedTitleModifier(
                key = "online_title_Health",
                sharedTransitionScope = sharedTransitionScope,
                animatedContentScope = animatedContentScope,
            )
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            when (healthState) {
                is HealthState.Idle -> {}
                is HealthState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is HealthState.Error -> {
                    val error = (healthState as HealthState.Error).message
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        PrimaryActionButton(
                            text = stringResource(R.string.health_retry),
                            onClick = { viewModel.fetchHealth() }
                        )
                    }
                }
                is HealthState.Success -> {
                    HealthContent((healthState as HealthState.Success).health)
                }
            }
        }
    }
}

@Composable
private fun HealthContent(health: ServerHealth) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HealthStatusCard(
            title = stringResource(R.string.health_overall_status_title),
            status = health.status,
            isOk = health.status == "ok"
        )

        health.database?.let { HealthCheckRow(stringResource(R.string.health_database), it) }
        health.postgresql?.let { HealthCheckRow(stringResource(R.string.health_postgresql), it) }
        health.retroWfcApi?.let { HealthCheckRow(stringResource(R.string.health_retro_wfc_api), it) }
        health.memory?.let { MemoryRow(it) }
    }
}

@Composable
private fun HealthStatusCard(title: String, status: String, isOk: Boolean) {
    val colors = statusColors()
    val (container, onContainer) = colors.container(isOk)
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = container
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            com.skiletro.wheelwitch.ui.components.PulsingDot(
                target = colors.indicator(isOk),
                pulse = isOk,
                sizeDp = 16.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = onContainer
                )
                Text(
                    text = if (isOk) stringResource(R.string.health_overall_status) else stringResource(R.string.health_issues_detected),
                    style = MaterialTheme.typography.bodySmall,
                    color = onContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun HealthCheckRow(label: String, item: HealthCheckItem) {
    val isOk = item.status == "ok"
    val colors = statusColors()
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            com.skiletro.wheelwitch.ui.components.PulsingDot(
                target = colors.indicator(isOk),
                pulse = isOk
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                item.message?.let { msg ->
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
            }
            Text(
                text = if (isOk) stringResource(R.string.health_ok) else stringResource(R.string.health_fail),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = colors.indicator(isOk)
            )
        }
    }
}

@Composable
private fun MemoryRow(memory: MemoryInfo) {
    val isOk = memory.status == "ok"
    val colors = statusColors()
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            com.skiletro.wheelwitch.ui.components.PulsingDot(
                target = colors.indicator(isOk),
                pulse = isOk
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.health_memory),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                val usageText = when {
                    memory.usagePercent != null && memory.used != null && memory.total != null ->
                        stringResource(R.string.health_usage_with_total_format, memory.usagePercent, memory.used, memory.total)
                    memory.usagePercent != null && memory.used != null ->
                        stringResource(R.string.health_usage_with_used_format, memory.usagePercent, memory.used)
                    memory.usagePercent != null ->
                        stringResource(R.string.health_usage_format, memory.usagePercent)
                    memory.used != null ->
                        stringResource(R.string.health_usage_just_used_format, memory.used)
                    else ->
                        stringResource(R.string.health_usage_format, 0.0)
                }
                Text(
                    text = usageText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = if (isOk) stringResource(R.string.health_ok) else stringResource(R.string.health_fail),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = colors.indicator(isOk)
            )
        }
    }
}

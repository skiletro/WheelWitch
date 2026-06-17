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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.skiletro.wheelwitch.model.HealthCheckItem
import com.skiletro.wheelwitch.model.MemoryInfo
import com.skiletro.wheelwitch.model.ServerHealth
import com.skiletro.wheelwitch.ui.components.ScreenHeader
import com.skiletro.wheelwitch.ui.components.container
import com.skiletro.wheelwitch.ui.components.indicator
import com.skiletro.wheelwitch.ui.components.statusColors
import com.skiletro.wheelwitch.viewmodel.HealthState
import com.skiletro.wheelwitch.viewmodel.OnlineViewModel

@Composable
fun HealthScreen(viewModel: OnlineViewModel) {
    val healthState by viewModel.healthState.collectAsState()
    val colors = statusColors()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ScreenHeader(
            title = "Server Health",
            onBack = { viewModel.goBack() },
            onRefresh = { viewModel.refreshHealth() }
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
                            text = "Retry",
                            onClick = { viewModel.refreshHealth() }
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
            title = "Overall Status",
            status = health.status,
            isOk = health.status == "ok"
        )

        health.database?.let { HealthCheckRow("Database", it) }
        health.postgresql?.let { HealthCheckRow("PostgreSQL", it) }
        health.retroWfcApi?.let { HealthCheckRow("Retro WFC API", it) }
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
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(colors.indicator(isOk))
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
                    text = if (isOk) "All systems operational" else "Issues detected",
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
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(colors.indicator(isOk))
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
                text = if (isOk) "OK" else "FAIL",
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
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(colors.indicator(isOk))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Memory",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                val usageText = buildString {
                    append("Usage: ")
                    if (memory.usagePercent != null) append("${"%.1f".format(memory.usagePercent)}%")
                    if (memory.used != null && memory.total != null) {
                        append(" (${memory.used} / ${memory.total})")
                    } else if (memory.used != null) {
                        append(" (${memory.used})")
                    }
                }
                Text(
                    text = usageText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = if (isOk) "OK" else "FAIL",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = colors.indicator(isOk)
            )
        }
    }
}

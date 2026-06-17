package com.skiletro.wheelwitch.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.skiletro.wheelwitch.R
import com.skiletro.wheelwitch.ui.components.ScreenHeader
import com.skiletro.wheelwitch.ui.components.focusBorder
import com.skiletro.wheelwitch.viewmodel.OnlineMenuPage
import com.skiletro.wheelwitch.viewmodel.OnlineViewModel
import com.skiletro.wheelwitch.viewmodel.RoomsState

@Composable
fun OnlineMenuScreen(
    viewModel: OnlineViewModel,
    onClose: () -> Unit
) {
    val currentPage by viewModel.currentPage.collectAsState()

    BackHandler {
        if (currentPage != OnlineMenuPage.Hub) {
            viewModel.goBack()
        } else {
            onClose()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AnimatedContent(
            targetState = currentPage,
            transitionSpec = {
                (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it } + fadeOut())
            },
            label = "online_pages"
        ) { page ->
            when (page) {
                OnlineMenuPage.Hub -> HubPage(viewModel = viewModel, onClose = onClose)
                OnlineMenuPage.Rooms -> {
                    val roomsState by viewModel.roomsState.collectAsState()
                    RoomsSubScreen(
                        roomsState = roomsState,
                        onRefresh = { viewModel.fetchRooms() },
                        onBack = { viewModel.goBack() }
                    )
                }
                OnlineMenuPage.Leaderboard -> LeaderboardScreen(viewModel)
                OnlineMenuPage.Health -> HealthScreen(viewModel)
                OnlineMenuPage.RaceStats -> RaceStatsScreen(viewModel)
                OnlineMenuPage.TimeTrial -> TimeTrialScreen(viewModel)
            }
        }
    }
}

@Composable
private fun HubPage(
    viewModel: OnlineViewModel,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        ScreenHeader(
            title = stringResource(R.string.online_menu_title),
            onBack = onClose
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HubOption(
                icon = Icons.Default.Person,
                title = stringResource(R.string.online_rooms),
                description = stringResource(R.string.online_rooms_desc),
                onClick = { viewModel.navigateTo(OnlineMenuPage.Rooms) }
            )
            HubOption(
                icon = Icons.Default.Star,
                title = stringResource(R.string.online_leaderboard),
                description = stringResource(R.string.online_leaderboard_desc),
                onClick = { viewModel.navigateTo(OnlineMenuPage.Leaderboard) }
            )
            HubOption(
                icon = Icons.Default.Favorite,
                title = stringResource(R.string.online_server_health),
                description = stringResource(R.string.online_server_health_desc),
                onClick = { viewModel.navigateTo(OnlineMenuPage.Health) }
            )
            HubOption(
                icon = Icons.Default.PlayArrow,
                title = stringResource(R.string.online_race_stats),
                description = stringResource(R.string.online_race_stats_desc),
                onClick = { viewModel.navigateTo(OnlineMenuPage.RaceStats) }
            )
            HubOption(
                icon = Icons.Default.Star,
                title = stringResource(R.string.online_time_trials),
                description = stringResource(R.string.online_time_trials_desc),
                enabled = false
            )
        }
    }
}

@Composable
private fun HealthIndicator(
    connectivity: com.skiletro.wheelwitch.model.ServerConnectivity?
) {
    val dotColor: Color
    val label: String

    when (connectivity) {
        com.skiletro.wheelwitch.model.ServerConnectivity.Online -> {
            dotColor = Color(0xFF4CAF50)
            label = stringResource(R.string.health_indicator_online)
        }
        com.skiletro.wheelwitch.model.ServerConnectivity.Offline -> {
            dotColor = MaterialTheme.colorScheme.error
            label = stringResource(R.string.health_indicator_offline)
        }
        com.skiletro.wheelwitch.model.ServerConnectivity.NoInternet -> {
            dotColor = MaterialTheme.colorScheme.onSurfaceVariant
            label = stringResource(R.string.health_indicator_no_internet)
        }
        else -> {
            dotColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            label = stringResource(R.string.health_indicator_checking)
        }
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HubOption(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true
) {
    val shape = RoundedCornerShape(16.dp)
    val baseColor = if (enabled) MaterialTheme.colorScheme.surfaceVariant
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val effectiveClick = if (enabled) onClick else null
    var focused by remember { mutableStateOf(false) }
    Surface(
        shape = shape,
        color = baseColor,
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (effectiveClick != null) Modifier.clickable(onClick = effectiveClick)
                else Modifier.focusable()
            )
            .onFocusChanged { focused = it.isFocused }
            .focusBorder(focused, shape = shape)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                )
            }
            if (!enabled) {
                Text(
                    text = stringResource(R.string.online_soon),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun RoomsSubScreen(
    roomsState: RoomsState,
    onRefresh: () -> Unit,
    onBack: () -> Unit
) {
    RoomsScreen(
        roomsState = roomsState,
        onRefresh = onRefresh,
        onClose = onBack
    )
}

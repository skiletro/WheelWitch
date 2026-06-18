package com.skiletro.wheelwitch.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.skiletro.wheelwitch.R
import com.skiletro.wheelwitch.model.PackStatus
import com.skiletro.wheelwitch.ui.components.SparkleHat
import com.skiletro.wheelwitch.ui.components.buttonShape
import com.skiletro.wheelwitch.viewmodel.PackUpdateViewModel
import com.skiletro.wheelwitch.viewmodel.UiState
import kotlinx.coroutines.delay

@Composable
fun QuickLaunchScreen(
    viewModel: PackUpdateViewModel,
    onFinish: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var actionTaken by remember { mutableStateOf(false) }
    var showCountdown by remember { mutableStateOf(false) }

    LaunchedEffect(state) {
        if (actionTaken) return@LaunchedEffect
        val s = state
        if (s is UiState.Ready) {
            actionTaken = true
            when (s.status) {
                is PackStatus.NotInstalled -> viewModel.downloadOrUpdate(s.status)
                is PackStatus.UpdateAvailable -> viewModel.downloadOrUpdate(s.status)
                is PackStatus.UpToDate -> showCountdown = true
                else -> {}
            }
        } else if (s is UiState.NoStorage || s is UiState.Error) {
            actionTaken = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (showCountdown) {
            CountdownPhase(onLaunch = {
                viewModel.launchDolphin()
                onFinish()
            })
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 400.dp)
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                when (val s = state) {
                    is UiState.NoStorage -> {
                        StatusMessage(
                            stringResource(R.string.quick_launch_storage_error),
                            isError = true
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        ExitButton(onClick = onFinish)
                    }

                    is UiState.Error -> {
                        StatusMessage(s.message, isError = true)
                        Spacer(modifier = Modifier.height(24.dp))
                        ExitButton(onClick = onFinish)
                    }

                    is UiState.Checking -> {
                        StatusMessage(stringResource(R.string.quick_launch_checking))
                    }

                    is UiState.Downloading -> {
                        StatusMessage(s.message)
                        Spacer(modifier = Modifier.height(24.dp))
                        ProgressBar(s.progress)
                    }

                    is UiState.Extracting -> {
                        StatusMessage(stringResource(R.string.status_extracting))
                        Spacer(modifier = Modifier.height(24.dp))
                        ProgressBar(s.progress)
                    }

                    is UiState.ApplyingUpdate -> {
                        StatusMessage(s.description)
                        Spacer(modifier = Modifier.height(24.dp))
                        ProgressBar(s.progress)
                    }

                    is UiState.Ready -> {
                        when (s.status) {
                            is PackStatus.Installed -> {
                                StatusMessage(
                                    stringResource(R.string.error_cannot_reach_server),
                                    isError = true
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                ExitButton(onClick = onFinish)
                            }

                            else -> StatusMessage(stringResource(R.string.quick_launch_preparing))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusMessage(text: String, isError: Boolean = false) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 32.dp)
    )
}

@Composable
private fun ProgressBar(progress: Float) {
    LinearProgressIndicator(
        progress = { progress.coerceIn(0f, 1f) },
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 300.dp)
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp)),
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
    )
}

@Composable
private fun ExitButton(onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        shape = buttonShape,
        modifier = Modifier.height(48.dp)
    ) {
        Text(stringResource(R.string.action_exit), fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun CountdownPhase(onLaunch: () -> Unit) {
    var count by remember { mutableStateOf(3) }

    LaunchedEffect(Unit) {
        while (count > 0) {
            delay(1000)
            count--
        }
        delay(500)
        onLaunch()
    }

    val isGo = count == 0
    val hatScale = 1f + (3 - count).coerceAtMost(3) * 0.05f
    val hatScaleAnim by androidx.compose.animation.core.animateFloatAsState(
        targetValue = hatScale,
        animationSpec = tween(durationMillis = 400),
        label = "hat_scale"
    )
    val hatOffsetY = if (isGo) (-40).dp else 0.dp

    val bobTransition = rememberInfiniteTransition(label = "countdown_bob")
    val bob by bobTransition.animateFloat(
        initialValue = 0f,
        targetValue = -4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "countdown_bob_offset"
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .offset(y = bob.dp + hatOffsetY),
            contentAlignment = Alignment.Center
        ) {
            SparkleHat(
                hatSize = 80.dp,
                modifier = Modifier.scale(hatScaleAnim)
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.quick_launch_up_to_date),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            for (i in 0..2) {
                RaceLightDot(
                    index = i,
                    count = count,
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        AnimatedVisibility(
            visible = !isGo,
            enter = fadeIn(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(150)),
        ) {
            Text(
                text = stringResource(R.string.quick_launch_starting_in, count),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        AnimatedVisibility(
            visible = isGo,
            enter = scaleIn(
                initialScale = 1.4f,
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(200)),
            exit = scaleOut(
                targetScale = 0.8f,
                animationSpec = tween(200)
            ) + fadeOut(animationSpec = tween(150)),
        ) {
            Text(
                text = stringResource(R.string.quick_launch_launching),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
        }
        if (isGo) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.quick_launch_launching_dolphin),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RaceLightDot(index: Int, count: Int) {
    val lit = index < count
    val go = count == 0
    val targetColor = when {
        go -> RaceLightGoGreen
        lit -> RaceLightRed
        else -> RaceLightDimRed
    }
    val color by androidx.compose.animation.animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 350, easing = LinearEasing),
        label = "race_light_color_$index"
    )
    val targetScale = if (lit && !go) 1.1f else 1f
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(durationMillis = 350),
        label = "race_light_scale_$index"
    )
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(color)
    )
}

private val RaceLightRed = Color(0xFFE53935)
private val RaceLightDimRed = Color(0xFF4D0F0F)
private val RaceLightGoGreen = Color(0xFF43A047)

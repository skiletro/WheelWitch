package com.skiletro.wheelwitch.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.skiletro.wheelwitch.R
import com.skiletro.wheelwitch.ui.components.buttonShape
import com.skiletro.wheelwitch.ui.components.sectionShape
import com.skiletro.wheelwitch.util.DolphinLauncher
import com.skiletro.wheelwitch.util.MiiWadInstaller
import com.skiletro.wheelwitch.model.MiiWadOnboarding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun OnboardingScreen(
    storageSelected: Boolean,
    isoSelected: Boolean,
    storageConfigured: Boolean,
    isoConfigured: Boolean,
    onPickStorage: () -> Unit,
    onSkipStorage: () -> Unit,
    onPickIso: () -> Unit,
    onSkipIso: () -> Unit,
    onComplete: () -> Unit
) {
    var step by remember { mutableStateOf(0) }
    var dolphinRetry by remember { mutableStateOf(0) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var dolphinInstalled by remember { mutableStateOf<Boolean?>(null) }
    val miiInstallFailedMessage = stringResource(R.string.onboarding_mii_install_failed)

    LaunchedEffect(step, dolphinRetry) {
        if (step == 1) {
            dolphinInstalled = null
            withContext(Dispatchers.IO) {
                dolphinInstalled = DolphinLauncher.isDolphinInstalled(context)
            }
        }
    }

    LaunchedEffect(storageSelected) {
        if (storageSelected && step == 2) step = 3
    }

    LaunchedEffect(isoSelected) {
        if (isoSelected && step == 3) step = 4
    }

    var miiWadState by remember { mutableStateOf<MiiWadOnboarding?>(null) }

    LaunchedEffect(step) {
        if (step == 4) {
            miiWadState = withContext(Dispatchers.IO) {
                if (MiiWadInstaller.getCachedWadFile(context) != null) MiiWadOnboarding.Installed
                else MiiWadOnboarding.NotInstalled
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    (slideInHorizontally(
                        animationSpec = tween(350),
                        initialOffsetX = { fullWidth -> fullWidth }
                    ) + fadeIn(animationSpec = tween(250))) togetherWith
                        (slideOutHorizontally(
                            animationSpec = tween(350),
                            targetOffsetX = { fullWidth -> -fullWidth }
                        ) + fadeOut(animationSpec = tween(200)))
                },
                label = "step"
            ) { currentStep ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    when (currentStep) {
                        0 -> WelcomeStep(onNext = { step = 1 })
                        1 -> DolphinCheckStep(
                            installed = dolphinInstalled,
                            onRetry = { dolphinRetry++ },
                            onNext = { step = 2 },
                            onDownload = { DolphinLauncher.openDolphinDownload(context) }
                        )
                        2 -> StorageStep(
                            onPickStorage = onPickStorage,
                            onContinue = onSkipStorage,
                            alreadyConfigured = storageConfigured
                        )
                        3 -> IsoStep(
                            onPickIso = onPickIso,
                            onContinue = onSkipIso,
                            alreadyConfigured = isoConfigured
                        )
                        4 -> MiiStep(
                            state = miiWadState,
                            onInstall = {
                                miiWadState = MiiWadOnboarding.Installing
                                scope.launch {
                                    try {
                                        withContext(Dispatchers.IO) {
                                            MiiWadInstaller.downloadAndExtractWad(context)
                                        }
                                        miiWadState = MiiWadOnboarding.Installed
                                    } catch (e: Exception) {
                                        miiWadState = MiiWadOnboarding.Error(
                                            e.message ?: miiInstallFailedMessage
                                        )
                                    }
                                }
                            },
                            onSkip = { step = 5 },
                            onNext = { step = 5 }
                        )
                        5 -> CompleteStep(onDone = onComplete)
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
            StepDots(current = step, total = 6)
        }
    }
}

@Composable
private fun WelcomeStep(onNext: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = sectionShape,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.onboarding_welcome_to),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.onboarding_app_name),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.onboarding_welcome_body),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onNext,
                shape = buttonShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = stringResource(R.string.onboarding_get_started),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun DolphinCheckStep(
    installed: Boolean?,
    onRetry: () -> Unit,
    onNext: () -> Unit,
    onDownload: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = sectionShape,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.onboarding_dolphin_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.onboarding_dolphin_body),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(20.dp))

            when (installed) {
                null -> {
                    Text(
                        text = stringResource(R.string.onboarding_dolphin_checking),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                true -> {
                    Text(
                        text = stringResource(R.string.onboarding_dolphin_installed),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onNext,
                        shape = buttonShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.onboarding_continue),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                false -> {
                    Text(
                        text = stringResource(R.string.onboarding_dolphin_not_installed),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.onboarding_dolphin_install_body),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onRetry,
                            shape = buttonShape,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.onboarding_check_again),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Button(
                            onClick = onDownload,
                            shape = buttonShape,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text(
                                text = stringResource(R.string.onboarding_download_dolphin),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StorageStep(onPickStorage: () -> Unit, onContinue: () -> Unit, alreadyConfigured: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = sectionShape,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.onboarding_storage_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (alreadyConfigured) {
                Text(
                    text = stringResource(R.string.onboarding_storage_configured),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onContinue,
                    shape = buttonShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_continue),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                Text(
                    text = stringResource(R.string.onboarding_storage_body),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onPickStorage,
                    shape = buttonShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_select_folder),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onContinue,
                    shape = buttonShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_skip),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun IsoStep(onPickIso: () -> Unit, onContinue: () -> Unit, alreadyConfigured: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = sectionShape,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.onboarding_iso_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (alreadyConfigured) {
                Text(
                    text = stringResource(R.string.onboarding_iso_configured),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onContinue,
                    shape = buttonShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_continue),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                Text(
                    text = stringResource(R.string.onboarding_iso_body),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onPickIso,
                    shape = buttonShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_select_rom),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onContinue,
                    shape = buttonShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_skip),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun MiiStep(
    state: MiiWadOnboarding?,
    onInstall: () -> Unit,
    onSkip: () -> Unit,
    onNext: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = sectionShape,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.onboarding_mii_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.onboarding_mii_body),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(20.dp))

            when (state) {
                null -> {
                    Text(
                        text = stringResource(R.string.onboarding_mii_checking),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                is MiiWadOnboarding.Installed -> {
                    Text(
                        text = stringResource(R.string.onboarding_mii_installed),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onNext,
                        shape = buttonShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.onboarding_continue),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                is MiiWadOnboarding.Installing -> {
                    val infiniteTransition = rememberInfiniteTransition(label = "wad_rotate")
                    val rotation by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 1500, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "rotation"
                    )
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(com.skiletro.wheelwitch.R.drawable.ic_hat_wizard),
                            contentDescription = stringResource(R.string.cd_installing),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(40.dp)
                                .rotate(rotation)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.onboarding_installing),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                is MiiWadOnboarding.Error -> {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onSkip,
                            shape = buttonShape,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.onboarding_skip),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Button(
                            onClick = onInstall,
                            shape = buttonShape,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text(
                                text = stringResource(R.string.onboarding_retry),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                is MiiWadOnboarding.NotInstalled -> {
                    Text(
                        text = stringResource(R.string.onboarding_mii_not_installed),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.onboarding_mii_skip_hint),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onSkip,
                            shape = buttonShape,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.onboarding_skip),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Button(
                            onClick = onInstall,
                            shape = buttonShape,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text(
                                text = stringResource(R.string.onboarding_install),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompleteStep(onDone: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = sectionShape,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.onboarding_complete_title),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.onboarding_complete_body),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onDone,
                shape = buttonShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = stringResource(R.string.onboarding_open_app),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun StepDots(current: Int, total: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (i in 0 until total) {
            val isCompleted = i <= current
            val isCurrent = i == current
            val size by androidx.compose.animation.core.animateDpAsState(
                targetValue = if (isCurrent) 10.dp else 8.dp,
                animationSpec = androidx.compose.animation.core.tween(300),
                label = "step_dot_size_$i"
            )
            val color by androidx.compose.animation.animateColorAsState(
                targetValue = if (isCompleted) MaterialTheme.colorScheme.primary
                              else MaterialTheme.colorScheme.surfaceVariant,
                animationSpec = androidx.compose.animation.core.tween(300),
                label = "step_dot_color_$i"
            )
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

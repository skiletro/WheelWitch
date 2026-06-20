package com.skiletro.wheelwitch.ui.screens

import android.os.Environment
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.skiletro.wheelwitch.R
import com.skiletro.wheelwitch.model.MiiWadOnboarding
import com.skiletro.wheelwitch.ui.components.buttonShape
import com.skiletro.wheelwitch.ui.components.sectionShape
import com.skiletro.wheelwitch.util.DolphinLauncher
import com.skiletro.wheelwitch.util.MiiWadInstaller
import com.skiletro.wheelwitch.viewmodel.MiiMakerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun OnboardingScreen(
    storageSelected: Boolean,
    isoSelected: Boolean,
    storageConfigured: Boolean,
    isoConfigured: Boolean,
    miiMaker: MiiMakerViewModel,
    onPickStorage: () -> Unit,
    onPickIso: () -> Unit,
    onSkipIso: () -> Unit,
    onRequestStoragePermission: () -> Unit,
    onComplete: () -> Unit
) {
    var step by remember { mutableStateOf(OnboardingStep.Welcome) }
    var dolphinRetry by remember { mutableStateOf(0) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var dolphinInstalled by remember { mutableStateOf<Boolean?>(null) }
    val miiInstallFailedMessage = stringResource(R.string.vm_failed_format, "install Mii Maker WAD")

    LaunchedEffect(step, dolphinRetry) {
        if (step == OnboardingStep.Dolphin) {
            dolphinInstalled = null
            withContext(Dispatchers.IO) {
                dolphinInstalled = DolphinLauncher.isDolphinInstalled(context)
            }
        }
    }

    LaunchedEffect(storageSelected) {
        if (storageSelected && step == OnboardingStep.Storage) step = OnboardingStep.Iso
    }

    LaunchedEffect(isoSelected) {
        if (isoSelected && step == OnboardingStep.Iso) step = OnboardingStep.Mii
    }

    var miiWadState by remember { mutableStateOf<MiiWadOnboarding?>(null) }

    LaunchedEffect(step) {
        if (step == OnboardingStep.Mii) {
            miiWadState = withContext(Dispatchers.IO) {
                if (MiiWadInstaller.getCachedWadFile(context) != null) MiiWadOnboarding.Installed
                else MiiWadOnboarding.NotInstalled
            }
        }
    }

    BackHandler(enabled = step.previous() != null) {
        step = step.previous() ?: step
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 48.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = step,
                modifier = Modifier.fillMaxWidth(),
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
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    when (currentStep) {
                        OnboardingStep.Welcome -> WelcomeStep(onNext = { step = OnboardingStep.Beta })
                        OnboardingStep.Beta -> BetaStep(onNext = { step = OnboardingStep.Permission })
                        OnboardingStep.Permission -> PermissionStep(
                            onGrant = onRequestStoragePermission,
                            onNext = { step = OnboardingStep.Dolphin }
                        )

                        OnboardingStep.Dolphin -> DolphinCheckStep(
                            installed = dolphinInstalled,
                            onRetry = { dolphinRetry++ },
                            onNext = { step = OnboardingStep.Storage },
                            onDownload = { DolphinLauncher.openDolphinDownload(context) }
                        )

                        OnboardingStep.Storage -> StorageStep(
                            onPickStorage = onPickStorage,
                            onContinue = { step = OnboardingStep.Iso },
                            alreadyConfigured = storageConfigured
                        )

                        OnboardingStep.Iso -> IsoStep(
                            onPickIso = onPickIso,
                            onContinue = onSkipIso,
                            alreadyConfigured = isoConfigured
                        )

                        OnboardingStep.Mii -> MiiStep(
                            state = miiWadState,
                            onInstall = {
                                miiWadState = MiiWadOnboarding.Installing
                                scope.launch {
                                    try {
                                        withContext(Dispatchers.IO) {
                                            MiiWadInstaller.downloadAndExtractWad(context)
                                        }
                                        miiWadState = MiiWadOnboarding.Installed
                                        miiMaker.refreshHasWad()
                                    } catch (e: Exception) {
                                        miiWadState = MiiWadOnboarding.Error(
                                            e.message ?: miiInstallFailedMessage
                                        )
                                    }
                                }
                            },
                            onSkip = { step = OnboardingStep.Complete },
                            onNext = { step = OnboardingStep.Complete }
                        )

                        OnboardingStep.Complete -> CompleteStep(onDone = onComplete)
                    }
                }
            }
        }
        StepDots(
            current = step.ordinal,
            total = OnboardingStep.TOTAL,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        )
    }
}

/** Ordered list of onboarding pages. [ordinal] doubles as the [StepDots] index. */
private enum class OnboardingStep {
    Welcome,
    Beta,
    Permission,
    Dolphin,
    Storage,
    Iso,
    Mii,
    Complete;

    /** Returns the previous step, or null if this is the first. */
    fun previous(): OnboardingStep? = entries.getOrNull(ordinal - 1)

    companion object {
      val TOTAL: Int = entries.size
    }
}

@Composable
private fun StepCard(
    title: String,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    titleStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.titleLarge,
    body: String? = null,
    content: @Composable ColumnScope.() -> Unit = {}
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
                text = title,
                style = titleStyle,
                fontWeight = FontWeight.Bold,
                color = titleColor
            )
            if (body != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            content()
        }
    }
}

@Composable
private fun StepPrimaryButton(text: String, onClick: () -> Unit) {
    Spacer(modifier = Modifier.height(16.dp))
    Button(
        onClick = onClick,
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
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun StepSecondaryActions(
    secondaryText: String,
    onSecondary: () -> Unit,
    primaryText: String,
    onPrimary: () -> Unit,
) {
    Spacer(modifier = Modifier.height(16.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onSecondary,
            shape = buttonShape,
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
        ) {
            Text(
                text = secondaryText,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
        Button(
            onClick = onPrimary,
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
                text = primaryText,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun WelcomeStep(onNext: () -> Unit) {
    StepCard(
        title = "${stringResource(R.string.onboarding_welcome_to)}\n${stringResource(R.string.onboarding_app_name)}",
        titleStyle = MaterialTheme.typography.headlineLarge,
        titleColor = MaterialTheme.colorScheme.primary,
        body = stringResource(R.string.onboarding_welcome_body)
    ) {
        StepPrimaryButton(
            text = stringResource(R.string.onboarding_get_started),
            onClick = onNext
        )
    }
}

@Composable
private fun BetaStep(onNext: () -> Unit) {
  StepCard(
    title = stringResource(R.string.onboarding_beta_title),
    titleStyle = MaterialTheme.typography.headlineSmall,
    body = stringResource(R.string.onboarding_beta_body)
  ) {
    StepPrimaryButton(
      text = stringResource(R.string.onboarding_beta_continue),
      onClick = onNext
    )
  }
}

@Composable
private fun PermissionStep(onGrant: () -> Unit, onNext: () -> Unit) {
    var checkKey by remember { mutableStateOf(0) }
    val isGranted = remember(checkKey) { Environment.isExternalStorageManager() }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                checkKey++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    StepCard(
        title = stringResource(R.string.onboarding_permission_title),
        body = stringResource(R.string.onboarding_permission_body)
    ) {
        if (isGranted) {
            Text(
                text = stringResource(R.string.onboarding_permission_granted),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            StepPrimaryButton(
                text = stringResource(R.string.onboarding_continue),
                onClick = onNext
            )
        } else {
            StepPrimaryButton(
                text = stringResource(R.string.onboarding_permission_grant),
                onClick = onGrant
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = { checkKey++ },
                shape = buttonShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = stringResource(R.string.onboarding_check_again),
                    style = MaterialTheme.typography.titleSmall,
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
    StepCard(
        title = stringResource(R.string.onboarding_dolphin_title),
        body = stringResource(R.string.onboarding_dolphin_body)
    ) {
        when (installed) {
            null -> Text(
                text = stringResource(R.string.status_checking),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            true -> {
                Text(
                    text = stringResource(R.string.status_installed_format, "Dolphin"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                StepPrimaryButton(
                    text = stringResource(R.string.onboarding_continue),
                    onClick = onNext
                )
            }

            false -> {
                Text(
                    text = stringResource(R.string.status_not_installed_format, "Dolphin"),
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
                StepSecondaryActions(
                    secondaryText = stringResource(R.string.onboarding_check_again),
                    onSecondary = onRetry,
                    primaryText = stringResource(R.string.onboarding_download_dolphin),
                    onPrimary = onDownload,
                )
            }
        }
    }
}

@Composable
private fun StorageStep(
    onPickStorage: () -> Unit,
    onContinue: () -> Unit,
    alreadyConfigured: Boolean
) {
    StepCard(
        title = stringResource(R.string.onboarding_storage_title),
        body = if (alreadyConfigured) stringResource(R.string.onboarding_storage_configured)
        else stringResource(R.string.onboarding_storage_body)
    ) {
        if (alreadyConfigured) {
            StepPrimaryButton(
                text = stringResource(R.string.onboarding_continue),
                onClick = onContinue
            )
        } else {
            StepPrimaryButton(
                text = stringResource(R.string.onboarding_select_folder),
                onClick = onPickStorage
            )
        }
    }
}

@Composable
private fun IsoStep(onPickIso: () -> Unit, onContinue: () -> Unit, alreadyConfigured: Boolean) {
    StepCard(
        title = stringResource(R.string.onboarding_iso_title),
        body = if (alreadyConfigured) stringResource(R.string.onboarding_iso_configured)
        else stringResource(R.string.onboarding_iso_body)
    ) {
        if (alreadyConfigured) {
            StepPrimaryButton(
                text = stringResource(R.string.onboarding_continue),
                onClick = onContinue
            )
        } else {
            StepPrimaryButton(
                text = stringResource(R.string.onboarding_select_rom),
                onClick = onPickIso
            )
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

@Composable
private fun MiiStep(
    state: MiiWadOnboarding?,
    onInstall: () -> Unit,
    onSkip: () -> Unit,
    onNext: () -> Unit
) {
    StepCard(
        title = stringResource(R.string.onboarding_mii_title),
        body = stringResource(R.string.onboarding_mii_body)
    ) {
        when (state) {
            null -> Text(
                text = stringResource(R.string.status_checking),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            is MiiWadOnboarding.Installed -> {
                Text(
                    text = stringResource(R.string.status_installed_format, "Mii Channel"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                StepPrimaryButton(
                    text = stringResource(R.string.onboarding_continue),
                    onClick = onNext
                )
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
                        painter = painterResource(R.drawable.ic_hat_wizard),
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
                StepSecondaryActions(
                    secondaryText = stringResource(R.string.onboarding_skip),
                    onSecondary = onSkip,
                    primaryText = stringResource(R.string.action_retry),
                    onPrimary = onInstall,
                )
            }

            is MiiWadOnboarding.NotInstalled -> {
                Text(
                    text = stringResource(R.string.status_not_installed),
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
                StepSecondaryActions(
                    secondaryText = stringResource(R.string.onboarding_skip),
                    onSecondary = onSkip,
                    primaryText = stringResource(R.string.action_install),
                    onPrimary = onInstall,
                )
            }
        }
    }
}

@Composable
private fun CompleteStep(onDone: () -> Unit) {
    StepCard(
        title = stringResource(R.string.onboarding_complete_title),
        titleStyle = MaterialTheme.typography.headlineLarge,
        titleColor = MaterialTheme.colorScheme.primary,
        body = stringResource(R.string.onboarding_complete_body)
    ) {
        StepPrimaryButton(
            text = stringResource(R.string.onboarding_open_app),
            onClick = onDone
        )
    }
}

@Composable
private fun StepDots(current: Int, total: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
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

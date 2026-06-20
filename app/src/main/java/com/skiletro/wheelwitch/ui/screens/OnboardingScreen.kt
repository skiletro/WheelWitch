package com.skiletro.wheelwitch.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.skiletro.wheelwitch.R
import com.skiletro.wheelwitch.model.MiiWadOnboarding
import com.skiletro.wheelwitch.ui.screens.onboarding.BetaStep
import com.skiletro.wheelwitch.ui.screens.onboarding.CompleteStep
import com.skiletro.wheelwitch.ui.screens.onboarding.DolphinCheckStep
import com.skiletro.wheelwitch.ui.screens.onboarding.IsoStep
import com.skiletro.wheelwitch.ui.screens.onboarding.MiiStep
import com.skiletro.wheelwitch.ui.screens.onboarding.OnboardingStep
import com.skiletro.wheelwitch.ui.screens.onboarding.PermissionStep
import com.skiletro.wheelwitch.ui.screens.onboarding.StepDots
import com.skiletro.wheelwitch.ui.screens.onboarding.StorageStep
import com.skiletro.wheelwitch.ui.screens.onboarding.WelcomeStep
import com.skiletro.wheelwitch.ui.screens.onboarding.rememberStoragePermissionState
import com.skiletro.wheelwitch.util.DolphinLauncher
import com.skiletro.wheelwitch.util.MiiWadInstaller
import com.skiletro.wheelwitch.viewmodel.MiiMakerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Top of the onboarding wizard. Owns the [OnboardingStep] state machine
 * and dispatches to one composable per step. Each step file lives
 * under `onboarding/` and exposes just the data it needs.
 */
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
  onComplete: () -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var step by remember { mutableStateOf(OnboardingStep.Welcome) }
  var dolphinRetry by remember { mutableIntStateOf(0) }
  var dolphinInstalled by remember { mutableStateOf<Boolean?>(null) }
  var miiWadState by remember { mutableStateOf<MiiWadOnboarding?>(null) }
  val (isGranted, recheckPermission) = rememberStoragePermissionState()
  val miiInstallFailedMessage = stringResource(R.string.vm_failed_format, "install Mii Maker WAD")

  LaunchedEffect(step, dolphinRetry) {
    if (step == OnboardingStep.Dolphin) {
      dolphinInstalled = null
      withContext(Dispatchers.IO) { dolphinInstalled = DolphinLauncher.isDolphinInstalled(context) }
    }
  }

  LaunchedEffect(storageSelected) {
    if (storageSelected && step == OnboardingStep.Storage) step = OnboardingStep.Iso
  }

  LaunchedEffect(isoSelected) {
    if (isoSelected && step == OnboardingStep.Iso) step = OnboardingStep.Mii
  }

  LaunchedEffect(step) {
    if (step == OnboardingStep.Mii) {
      miiWadState =
        withContext(Dispatchers.IO) {
          if (MiiWadInstaller.getCachedWadFile(context) != null) MiiWadOnboarding.Installed
          else MiiWadOnboarding.NotInstalled
        }
    }
  }

  BackHandler(enabled = step.previous() != null) {
    step = step.previous() ?: step
  }

  Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    Box(
      modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 48.dp),
      contentAlignment = Alignment.Center,
    ) {
      AnimatedContent(
        targetState = step,
        modifier = Modifier.fillMaxWidth(),
        transitionSpec = {
          (slideInHorizontally(animationSpec = tween(350), initialOffsetX = { it }) +
              fadeIn(animationSpec = tween(250))) togetherWith
            (slideOutHorizontally(animationSpec = tween(350), targetOffsetX = { -it }) +
              fadeOut(animationSpec = tween(200)))
        },
        label = "step",
      ) { currentStep ->
        Column(
          modifier = Modifier.fillMaxWidth(),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          when (currentStep) {
            OnboardingStep.Welcome -> WelcomeStep(onNext = { step = OnboardingStep.Beta })
            OnboardingStep.Beta -> BetaStep(onNext = { step = OnboardingStep.Permission })
            OnboardingStep.Permission ->
              PermissionStep(
                isGranted = isGranted,
                onGrant = onRequestStoragePermission,
                onNext = { step = OnboardingStep.Dolphin },
                onRecheck = recheckPermission,
              )
            OnboardingStep.Dolphin ->
              DolphinCheckStep(
                installed = dolphinInstalled,
                onRetry = { dolphinRetry++ },
                onNext = { step = OnboardingStep.Storage },
                onDownload = { DolphinLauncher.openDolphinDownload(context) },
              )
            OnboardingStep.Storage ->
              StorageStep(
                onPickStorage = onPickStorage,
                onContinue = { step = OnboardingStep.Iso },
                alreadyConfigured = storageConfigured,
              )
            OnboardingStep.Iso ->
              IsoStep(
                onPickIso = onPickIso,
                onContinue = onSkipIso,
                alreadyConfigured = isoConfigured,
              )
            OnboardingStep.Mii ->
              MiiStep(
                state = miiWadState,
                onInstall = {
                  miiWadState = MiiWadOnboarding.Installing
                  scope.launch {
                    try {
                      withContext(Dispatchers.IO) { MiiWadInstaller.downloadAndExtractWad(context) }
                      miiWadState = MiiWadOnboarding.Installed
                      miiMaker.refreshHasWad()
                    } catch (e: Exception) {
                      miiWadState =
                        MiiWadOnboarding.Error(e.message ?: miiInstallFailedMessage)
                    }
                  }
                },
                onSkip = { step = OnboardingStep.Complete },
                onNext = { step = OnboardingStep.Complete },
              )
            OnboardingStep.Complete -> CompleteStep(onDone = onComplete)
          }
        }
      }
    }
    StepDots(
      current = step.ordinal,
      total = OnboardingStep.TOTAL,
      modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp),
    )
  }
}

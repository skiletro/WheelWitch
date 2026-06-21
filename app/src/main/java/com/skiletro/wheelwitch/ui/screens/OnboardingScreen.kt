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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.skiletro.wheelwitch.ui.screens.onboarding.BetaStep
import com.skiletro.wheelwitch.ui.screens.onboarding.CompleteStep
import com.skiletro.wheelwitch.ui.screens.onboarding.OnboardingStep
import com.skiletro.wheelwitch.ui.screens.onboarding.StepDots
import com.skiletro.wheelwitch.ui.screens.onboarding.WelcomeStep

/**
 * Reduced onboarding wizard: Welcome → Beta → Complete.
 *
 * The storage picker, ROM picker, Mii WAD install, Dolphin check, and
 * all-files permission steps have been ripped out for the planned
 * install/launch rewrite. The user just sees a welcome, the beta
 * disclaimer, and a done button.
 */
@Composable
fun OnboardingScreen(
  onComplete: () -> Unit,
) {
  var step by remember { mutableStateOf(OnboardingStep.Welcome) }

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
            OnboardingStep.Beta -> BetaStep(onNext = { step = OnboardingStep.Complete })
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

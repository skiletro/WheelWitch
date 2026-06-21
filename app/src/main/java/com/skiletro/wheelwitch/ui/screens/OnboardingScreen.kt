package com.skiletro.wheelwitch.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.skiletro.wheelwitch.R
import com.skiletro.wheelwitch.ui.components.buttonShape
import com.skiletro.wheelwitch.ui.components.sectionShape

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

/** Reduced onboarding flow. The full picker / permission / install steps have been ripped out. */
private enum class OnboardingStep {
  Welcome,
  Beta,
  Complete;

  /** Returns the previous step, or null if this is the first. */
  fun previous(): OnboardingStep? = entries.getOrNull(ordinal - 1)

  companion object {
    val TOTAL: Int = entries.size
  }
}

/** First onboarding step: greets the user. */
@Composable
private fun WelcomeStep(onNext: () -> Unit) {
  StepCard(
    title =
      "${stringResource(R.string.onboarding_welcome_to)}\n${stringResource(R.string.onboarding_app_name)}",
    titleStyle = MaterialTheme.typography.headlineLarge,
    titleColor = MaterialTheme.colorScheme.primary,
    body = stringResource(R.string.onboarding_welcome_body),
  ) {
    StepPrimaryButton(text = stringResource(R.string.onboarding_get_started), onClick = onNext)
  }
}

/** Second onboarding step: surfaces the app's beta status. */
@Composable
private fun BetaStep(onNext: () -> Unit) {
  StepCard(
    title = stringResource(R.string.onboarding_beta_title),
    titleStyle = MaterialTheme.typography.headlineSmall,
    body = stringResource(R.string.onboarding_beta_body),
  ) {
    StepPrimaryButton(text = stringResource(R.string.onboarding_beta_continue), onClick = onNext)
  }
}

/** Final onboarding step: confirms the user is ready to enter the home screen. */
@Composable
private fun CompleteStep(onDone: () -> Unit) {
  StepCard(
    title = stringResource(R.string.onboarding_complete_title),
    titleStyle = MaterialTheme.typography.headlineLarge,
    titleColor = MaterialTheme.colorScheme.primary,
    body = stringResource(R.string.onboarding_complete_body),
  ) {
    StepPrimaryButton(text = stringResource(R.string.onboarding_open_app), onClick = onDone)
  }
}

/** Card frame that hosts each onboarding step's title, body, and content. */
@Composable
private fun StepCard(
  title: String,
  titleColor: Color = MaterialTheme.colorScheme.onSurface,
  titleStyle: TextStyle = MaterialTheme.typography.titleLarge,
  body: String? = null,
  content: @Composable ColumnScope.() -> Unit = {},
) {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = sectionShape,
    color = MaterialTheme.colorScheme.surfaceVariant,
  ) {
    Column(
      modifier = Modifier.padding(32.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
        text = title,
        style = titleStyle,
        fontWeight = FontWeight.Bold,
        color = titleColor,
      )
      if (body != null) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
          text = body,
          style = MaterialTheme.typography.bodyMedium,
          textAlign = TextAlign.Center,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    modifier = Modifier.fillMaxWidth().height(56.dp),
    colors = ButtonDefaults.buttonColors(
      containerColor = MaterialTheme.colorScheme.primary,
      contentColor = MaterialTheme.colorScheme.onPrimary,
    ),
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.SemiBold,
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
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
    OutlinedButton(
      onClick = onSecondary,
      shape = buttonShape,
      modifier = Modifier.weight(1f).height(48.dp),
    ) {
      Text(
        text = secondaryText,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
      )
    }
    Button(
      onClick = onPrimary,
      shape = buttonShape,
      modifier = Modifier.weight(1f).height(48.dp),
      colors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
      ),
    ) {
      Text(
        text = primaryText,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
      )
    }
  }
}

/** Step progress dots rendered at the bottom of the onboarding flow. */
@Composable
private fun StepDots(current: Int, total: Int, modifier: Modifier = Modifier) {
  Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    for (i in 0 until total) {
      val isCompleted = i <= current
      val isCurrent = i == current
      val size by
        animateDpAsState(
          targetValue = if (isCurrent) 10.dp else 8.dp,
          animationSpec = tween(300),
          label = "step_dot_size_$i",
        )
      val color by
        animateColorAsState(
          targetValue =
            if (isCompleted) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.surfaceVariant,
          animationSpec = tween(300),
          label = "step_dot_color_$i",
        )
      Box(modifier = Modifier.size(size).clip(CircleShape).background(color))
    }
  }
}

package com.skiletro.wheelwitch.ui.screens.onboarding

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.skiletro.wheelwitch.R

/** First onboarding step: greets the user. */
@Composable
internal fun WelcomeStep(onNext: () -> Unit) {
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

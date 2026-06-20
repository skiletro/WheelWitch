package com.skiletro.wheelwitch.ui.screens.onboarding

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.skiletro.wheelwitch.R

/** Second onboarding step: surfaces the app's beta status. */
@Composable
internal fun BetaStep(onNext: () -> Unit) {
  StepCard(
    title = stringResource(R.string.onboarding_beta_title),
    titleStyle = MaterialTheme.typography.headlineSmall,
    body = stringResource(R.string.onboarding_beta_body),
  ) {
    StepPrimaryButton(text = stringResource(R.string.onboarding_beta_continue), onClick = onNext)
  }
}

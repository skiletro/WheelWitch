package com.skiletro.wheelwitch.ui.screens.onboarding

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.skiletro.wheelwitch.R

/** Final onboarding step: confirms the user is ready to enter the home screen. */
@Composable
internal fun CompleteStep(onDone: () -> Unit) {
  StepCard(
    title = stringResource(R.string.onboarding_complete_title),
    titleStyle = MaterialTheme.typography.headlineLarge,
    titleColor = MaterialTheme.colorScheme.primary,
    body = stringResource(R.string.onboarding_complete_body),
  ) {
    StepPrimaryButton(text = stringResource(R.string.onboarding_open_app), onClick = onDone)
  }
}

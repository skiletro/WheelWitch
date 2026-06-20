package com.skiletro.wheelwitch.ui.screens.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.skiletro.wheelwitch.R

/**
 * Fifth onboarding step: prompts the user to select a writable folder
 * (via the SAF tree picker) where the Retro Rewind pack will live.
 * If [alreadyConfigured] is true, the picker is bypassed and the
 * Continue button is shown directly.
 */
@Composable
internal fun StorageStep(onPickStorage: () -> Unit, onContinue: () -> Unit, alreadyConfigured: Boolean) {
  StepCard(
    title = stringResource(R.string.onboarding_storage_title),
    body =
      if (alreadyConfigured) stringResource(R.string.onboarding_storage_configured)
      else stringResource(R.string.onboarding_storage_body),
  ) {
    if (alreadyConfigured) {
      StepPrimaryButton(text = stringResource(R.string.onboarding_continue), onClick = onContinue)
    } else {
      StepPrimaryButton(text = stringResource(R.string.onboarding_select_folder), onClick = onPickStorage)
    }
  }
}

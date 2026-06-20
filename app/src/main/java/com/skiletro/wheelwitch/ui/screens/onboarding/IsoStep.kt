package com.skiletro.wheelwitch.ui.screens.onboarding

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.skiletro.wheelwitch.R
import com.skiletro.wheelwitch.ui.components.buttonShape

/**
 * Sixth onboarding step: optional Mario Kart Wii ROM picker. The user
 * can either pick an ISO/RVZ/WBFS file or skip the step (the ROM is
 * not required to use the app). If [alreadyConfigured] is true, only
 * Continue is shown.
 */
@Composable
internal fun IsoStep(onPickIso: () -> Unit, onContinue: () -> Unit, alreadyConfigured: Boolean) {
  StepCard(
    title = stringResource(R.string.onboarding_iso_title),
    body =
      if (alreadyConfigured) stringResource(R.string.onboarding_iso_configured)
      else stringResource(R.string.onboarding_iso_body),
  ) {
    if (alreadyConfigured) {
      StepPrimaryButton(text = stringResource(R.string.onboarding_continue), onClick = onContinue)
    } else {
      StepPrimaryButton(text = stringResource(R.string.onboarding_select_rom), onClick = onPickIso)
      Spacer(modifier = Modifier.height(12.dp))
      OutlinedButton(
        onClick = onContinue,
        shape = buttonShape,
        modifier = Modifier.fillMaxWidth().height(48.dp),
      ) {
        Text(
          text = stringResource(R.string.onboarding_skip),
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
        )
      }
    }
  }
}

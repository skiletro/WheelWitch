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
 * Third onboarding step: requests the all-files-access permission and
 * shows a "granted" state once the user has toggled it on in Settings.
 */
@Composable
internal fun PermissionStep(isGranted: Boolean, onGrant: () -> Unit, onNext: () -> Unit, onRecheck: () -> Unit) {
  StepCard(
    title = stringResource(R.string.onboarding_permission_title),
    body = stringResource(R.string.onboarding_permission_body),
  ) {
    if (isGranted) {
      Text(
        text = stringResource(R.string.onboarding_permission_granted),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
      )
      StepPrimaryButton(text = stringResource(R.string.onboarding_continue), onClick = onNext)
    } else {
      StepPrimaryButton(text = stringResource(R.string.onboarding_permission_grant), onClick = onGrant)
      Spacer(modifier = Modifier.height(12.dp))
      OutlinedButton(
        onClick = onRecheck,
        shape = buttonShape,
        modifier = Modifier.fillMaxWidth().height(48.dp),
      ) {
        Text(
          text = stringResource(R.string.onboarding_check_again),
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
        )
      }
    }
  }
}

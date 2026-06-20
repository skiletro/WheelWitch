package com.skiletro.wheelwitch.ui.screens.onboarding

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.skiletro.wheelwitch.R

/**
 * Fourth onboarding step: checks whether the Dolphin Emulator package
 * is installed. While [installed] is null the step shows a "checking"
 * label; once the result is known it offers either Continue or
 * Retry / Download.
 */
@Composable
internal fun DolphinCheckStep(installed: Boolean?, onRetry: () -> Unit, onNext: () -> Unit, onDownload: () -> Unit) {
  StepCard(
    title = stringResource(R.string.onboarding_dolphin_title),
    body = stringResource(R.string.onboarding_dolphin_body),
  ) {
    when (installed) {
      null -> {
        Text(
          text = stringResource(R.string.status_checking),
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      true -> {
        Text(
          text = stringResource(R.string.status_installed_format, "Dolphin"),
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary,
        )
        StepPrimaryButton(text = stringResource(R.string.onboarding_continue), onClick = onNext)
      }
      false -> {
        Text(
          text = stringResource(R.string.status_not_installed_format, "Dolphin"),
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.error,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = stringResource(R.string.onboarding_dolphin_install_body),
          style = MaterialTheme.typography.bodyMedium,
          textAlign = TextAlign.Center,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
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

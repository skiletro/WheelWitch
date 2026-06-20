package com.skiletro.wheelwitch.ui.screens.onboarding

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.skiletro.wheelwitch.R
import com.skiletro.wheelwitch.model.MiiWadOnboarding

/**
 * Seventh onboarding step: optional Mii Channel WAD install. The
 * install runs on a coroutine; the parent passes back a [MiiWadOnboarding]
 * state that drives the UI (checking / installing / installed / error /
 * not-installed). Skip moves to the next step without installing.
 */
@Composable
internal fun MiiStep(state: MiiWadOnboarding?, onInstall: () -> Unit, onSkip: () -> Unit, onNext: () -> Unit) {
  StepCard(title = stringResource(R.string.onboarding_mii_title), body = stringResource(R.string.onboarding_mii_body)) {
    when (state) {
      null -> {
        Text(
          text = stringResource(R.string.status_checking),
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      is MiiWadOnboarding.Installed -> {
        Text(
          text = stringResource(R.string.status_installed_format, "Mii Channel"),
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary,
        )
        StepPrimaryButton(text = stringResource(R.string.onboarding_continue), onClick = onNext)
      }
      is MiiWadOnboarding.Installing -> {
        val infiniteTransition = rememberInfiniteTransition(label = "wad_rotate")
        val rotation by
          infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec =
              infiniteRepeatable(
                animation = tween(durationMillis = 1500, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
              ),
            label = "rotation",
          )
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
          Icon(
            painter = painterResource(R.drawable.ic_hat_wizard),
            contentDescription = stringResource(R.string.cd_installing),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(40.dp).rotate(rotation),
          )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = stringResource(R.string.onboarding_installing),
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      is MiiWadOnboarding.Error -> {
        Text(
          text = state.message,
          style = MaterialTheme.typography.bodyMedium,
          textAlign = TextAlign.Center,
          color = MaterialTheme.colorScheme.error,
        )
        StepSecondaryActions(
          secondaryText = stringResource(R.string.onboarding_skip),
          onSecondary = onSkip,
          primaryText = stringResource(R.string.action_retry),
          onPrimary = onInstall,
        )
      }
      is MiiWadOnboarding.NotInstalled -> {
        Text(
          text = stringResource(R.string.status_not_installed),
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = stringResource(R.string.onboarding_mii_skip_hint),
          style = MaterialTheme.typography.bodySmall,
          textAlign = TextAlign.Center,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        StepSecondaryActions(
          secondaryText = stringResource(R.string.onboarding_skip),
          onSecondary = onSkip,
          primaryText = stringResource(R.string.action_install),
          onPrimary = onInstall,
        )
      }
    }
  }
}

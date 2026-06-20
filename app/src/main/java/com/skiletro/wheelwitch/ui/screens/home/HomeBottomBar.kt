package com.skiletro.wheelwitch.ui.screens.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.skiletro.wheelwitch.R
import com.skiletro.wheelwitch.model.LicenseInfo
import com.skiletro.wheelwitch.model.PackStatus
import com.skiletro.wheelwitch.model.ServerConnectivity
import com.skiletro.wheelwitch.ui.components.ActivePlayerCard
import com.skiletro.wheelwitch.ui.components.PrimaryActionButton
import com.skiletro.wheelwitch.ui.components.ProgressButton
import com.skiletro.wheelwitch.ui.components.buttonShape
import com.skiletro.wheelwitch.ui.components.focusBorder
import com.skiletro.wheelwitch.viewmodel.UiState

/**
 * Bottom bar of the home screen: optional active player card on the
 * left, animated primary action on the right. The action switches
 * across the [UiState] variants: progress buttons during download /
 * extract / apply-update, a tonal "Checking…" while checking, a
 * "Retry" tonal on error, and a check-for-updates + install/launch
 * pair when [UiState.Ready].
 */
@Composable
fun HomeBottomBar(
  state: UiState,
  activeLicense: LicenseInfo?,
  cachedLeaderboardVrs: Map<Int, Int>,
  vrMultiplier: Float?,
  playerCount: Int?,
  serverConnectivity: ServerConnectivity,
  isBusy: Boolean,
  hasIso: Boolean,
  onLaunch: () -> Unit,
  onCheck: () -> Unit,
  onRetry: () -> Unit,
  onInstallOrUpdate: (() -> Unit)?,
  onPickIso: () -> Unit,
) {
  var checkButtonFocused by remember { mutableStateOf(false) }

  Row(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    if (activeLicense != null) {
      ActivePlayerCard(
        license = activeLicense,
        cachedLeaderboardVr = cachedLeaderboardVrs[activeLicense.slotIndex],
        vrMultiplier = vrMultiplier,
      )
    }

    Spacer(modifier = Modifier.weight(1f))

    AnimatedContent(
      targetState = state,
      contentKey = { it::class },
      transitionSpec = {
        (fadeIn(animationSpec = tween(250)) +
            scaleIn(initialScale = 0.92f, animationSpec = tween(250))) togetherWith
          fadeOut(animationSpec = tween(150))
      },
      label = "primary_action",
    ) { currentState ->
      when (currentState) {
        is UiState.Downloading ->
          ProgressButton(
            currentState.progress,
            currentState.message,
            currentState.bytesPerSecond,
            currentState.bytesDownloaded,
            currentState.totalBytes,
          )
        is UiState.Extracting ->
          ProgressButton(currentState.progress, stringResource(R.string.status_extracting))
        is UiState.ApplyingUpdate ->
          ProgressButton(
            currentState.progress,
            stringResource(
              R.string.home_update_step_format,
              currentState.index,
              currentState.total,
              currentState.description,
            ),
          )
        is UiState.Checking ->
          FilledTonalButton(
            onClick = {},
            enabled = false,
            shape = buttonShape,
            modifier = Modifier.height(56.dp),
          ) {
            Text(
              text = stringResource(R.string.status_checking),
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Medium,
            )
          }
        is UiState.Error ->
          FilledTonalButton(onClick = onRetry, shape = buttonShape, modifier = Modifier.height(56.dp)) {
            Text(
              text = stringResource(R.string.action_retry),
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Medium,
            )
          }
        is UiState.Ready -> {
          val status = currentState.status
          val checkSubtitle =
            when (status) {
              is PackStatus.UpToDate ->
                stringResource(R.string.home_up_to_date, status.currentVersion)
              is PackStatus.UpdateAvailable ->
                stringResource(
                  R.string.home_update_format,
                  status.currentVersion,
                  status.latestVersion,
                )
              is PackStatus.Installed ->
                stringResource(R.string.home_version_installed, status.version)
              else -> null
            }
          Row(verticalAlignment = Alignment.CenterVertically) {
            FilledTonalButton(
              onClick = onCheck,
              enabled = !isBusy,
              shape = buttonShape,
              colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
              ),
              modifier =
                Modifier.height(56.dp)
                  .onFocusChanged { checkButtonFocused = it.isFocused }
                  .focusBorder(checkButtonFocused),
            ) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                  text = stringResource(R.string.home_check_for_updates),
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Medium,
                )
                if (checkSubtitle != null) {
                  Text(
                    text = checkSubtitle,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Normal,
                  )
                }
              }
            }
            Spacer(modifier = Modifier.width(12.dp))
            when (status) {
              is PackStatus.NotInstalled ->
                PrimaryActionButton(
                  text = stringResource(R.string.action_install),
                  onClick = onInstallOrUpdate ?: {},
                )
              is PackStatus.UpdateAvailable ->
                PrimaryActionButton(
                  text = stringResource(R.string.home_update_to, status.latestVersion),
                  onClick = onInstallOrUpdate ?: {},
                )
              else -> {
                val bullet = "\u2022 "
                val launchSubText =
                  when (serverConnectivity) {
                    ServerConnectivity.Online -> {
                      val count = playerCount
                      if (count != null) "$bullet${stringResource(R.string.home_racers_online, count)}"
                      else null
                    }
                    ServerConnectivity.Offline -> "$bullet${stringResource(R.string.home_offline)}"
                    ServerConnectivity.NoInternet ->
                      "$bullet${stringResource(R.string.status_no_internet)}"
                    ServerConnectivity.Unknown -> null
                  }
                if (hasIso) {
                  PrimaryActionButton(
                    text = stringResource(R.string.home_launch_retro_rewind),
                    onClick = onLaunch,
                    subText = launchSubText,
                  )
                } else {
                  PrimaryActionButton(
                    text = stringResource(R.string.home_select_rom),
                    onClick = onPickIso,
                  )
                }
              }
            }
          }
        }
        is UiState.NoStorage -> {
          Text(
            text = stringResource(R.string.error_storage_not_configured),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}

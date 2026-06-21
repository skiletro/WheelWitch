package com.skiletro.wheelwitch.ui.screens.home

import android.widget.Toast
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.skiletro.wheelwitch.R
import com.skiletro.wheelwitch.model.PackStatus
import com.skiletro.wheelwitch.model.ServerConnectivity
import com.skiletro.wheelwitch.ui.components.PrimaryActionButton
import com.skiletro.wheelwitch.ui.components.buttonShape
import com.skiletro.wheelwitch.ui.components.focusBorder
import com.skiletro.wheelwitch.viewmodel.UiState

/**
 * Bottom bar of the home screen: check-for-updates on the left,
 * status-dependent primary action on the right. The install / launch
 * actions are stubbed for now — they surface a Snackbar saying the
 * flow isn't implemented yet, while the check-for-updates path
 * remains fully functional.
 */
@Composable
fun HomeBottomBar(
  state: UiState,
  vrMultiplier: Float?,
  playerCount: Int?,
  serverConnectivity: ServerConnectivity,
  isBusy: Boolean,
  onCheck: () -> Unit,
  onRetry: () -> Unit,
) {
  val context = LocalContext.current
  val installNotImplementedMessage = stringResource(R.string.home_install_not_implemented)
  val launchNotImplementedMessage = stringResource(R.string.home_launch_not_implemented)
  var checkButtonFocused by remember { mutableStateOf(false) }

  Row(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
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
                  onClick = {
                    Toast.makeText(context, installNotImplementedMessage, Toast.LENGTH_SHORT).show()
                  },
                )
              is PackStatus.UpdateAvailable ->
                PrimaryActionButton(
                  text = stringResource(R.string.home_update_to, status.latestVersion),
                  onClick = {
                    Toast.makeText(context, installNotImplementedMessage, Toast.LENGTH_SHORT).show()
                  },
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
                PrimaryActionButton(
                  text = stringResource(R.string.home_launch_retro_rewind),
                  onClick = {
                    Toast.makeText(context, launchNotImplementedMessage, Toast.LENGTH_SHORT).show()
                  },
                  subText = launchSubText,
                )
              }
            }
          }
        }
        is UiState.Idle -> {
          FilledTonalButton(
            onClick = onCheck,
            enabled = !isBusy,
            shape = buttonShape,
            modifier = Modifier.height(56.dp),
          ) {
            Text(
              text = stringResource(R.string.home_check_for_updates),
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Medium,
            )
          }
        }
      }
    }
  }
}

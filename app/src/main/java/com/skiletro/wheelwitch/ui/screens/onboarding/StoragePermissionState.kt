package com.skiletro.wheelwitch.ui.screens.onboarding

import android.os.Environment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Tracks whether the app holds [Environment.isExternalStorageManager].
 *
 * Returns a pair of the current `isGranted` boolean and a `recheck()`
 * function the caller can invoke when the user returns from the
 * settings app. Re-checks automatically on `ON_RESUME` so the toggle
 * in system settings is picked up.
 */
@Composable
fun rememberStoragePermissionState(): Pair<Boolean, () -> Unit> {
  var checkKey by remember { mutableIntStateOf(0) }
  val isGranted = remember(checkKey) { Environment.isExternalStorageManager() }
  val lifecycleOwner = LocalLifecycleOwner.current
  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        checkKey++
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }
  return isGranted to { checkKey++ }
}

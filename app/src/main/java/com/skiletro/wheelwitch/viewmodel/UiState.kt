package com.skiletro.wheelwitch.viewmodel

import androidx.compose.runtime.Immutable
import com.skiletro.wheelwitch.model.PackStatus
import com.skiletro.wheelwitch.util.io.DownloadProgress

/**
 * High-level state for the pack update flow.
 *
 * Lifecycle: `Idle → Checking → Ready → Installing → Installed → Error`.
 * `Installing` and `Installed` are the new install/update states
 * added when the save/install/launch refactor reintroduced the
 * orchestration; `checkStatus` and the error path still cycle back to
 * `Ready` / `Error` respectively.
 */
@Immutable
sealed class UiState {
  /** Initial state before the first check completes. */
  data object Idle : UiState()

  /** A status check is in progress. */
  data object Checking : UiState()

  /** A status check finished; [status] is the current pack state. */
  data class Ready(val status: PackStatus) : UiState()

  /**
   * A pack install or update is in progress. [progress] is non-null
   * during the download phase; it is null during extraction (which
   * is fast and not surfaced as a fraction).
   */
  data class Installing(val progress: DownloadProgress?) : UiState()

  /**
   * An install just completed. The view-model auto-transitions to
   * [Checking] immediately after this, so the UI typically sees this
   * state for a single frame.
   */
  data object Installed : UiState()

  /** A status check or install failed; [message] is user-facing. */
  data class Error(val message: String) : UiState()
}

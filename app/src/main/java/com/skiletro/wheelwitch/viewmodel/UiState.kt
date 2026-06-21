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
   * A pack install or update is in progress. The phase is one of:
   *
   * - [Installing.Downloading] — fetching the zip from the server.
   *   Carries the full [DownloadProgress] (fraction, rate, byte
   *   counts) for a determinate UI bar.
   * - [Installing.Extracting] — unpacking the zip into the SAF tree.
   *   Carries a determinate `done / total` file count.
   *
   * Both phases live under the same `Installing` parent so the home
   * screen's `isBusy = state is UiState.Installing` check still
   * works without a per-phase guard.
   */
  @Immutable
  sealed class Installing : UiState() {
    /** Zip is being fetched from the update server. */
    data class Downloading(val progress: DownloadProgress) : Installing()

    /**
     * Zip is being unpacked into the SAF tree. [filesDone] is the
     * index of the file just written; [filesTotal] is the count of
     * non-directory entries (computed by a quick pre-scan of the zip
     * header so the bar can be determinate from the first frame).
     */
    data class Extracting(val filesDone: Int, val filesTotal: Int) : Installing()
  }

  /**
   * An install just completed. The view-model auto-transitions to
   * [Checking] immediately after this, so the UI typically sees this
   * state for a single frame.
   */
  data object Installed : UiState()

  /** A status check or install failed; [message] is user-facing. */
  data class Error(val message: String) : UiState()
}

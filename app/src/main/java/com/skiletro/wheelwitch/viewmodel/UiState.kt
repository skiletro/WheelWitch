package com.skiletro.wheelwitch.viewmodel

import androidx.compose.runtime.Immutable
import com.skiletro.wheelwitch.data.ExtractingPhase
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
   *   Carries a determinate file-count bar, the current file name,
   *   and a byte-count basis for future use.
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
     * Zip is being unpacked into the SAF tree. [phase] tells the UI
     * whether the directory pre-pass is running or the per-file
     * writes are live; [currentFile] is the entry currently being
     * written, or null during the pre-pass and between files.
     */
    data class Extracting(
      val phase: ExtractingPhase,
      val filesDone: Int,
      val filesTotal: Int,
      val currentFile: String?,
      val bytesDone: Long,
      val bytesTotal: Long,
    ) : Installing()
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

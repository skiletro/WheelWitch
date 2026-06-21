package com.skiletro.wheelwitch.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.skiletro.wheelwitch.data.DolphinTree
import com.skiletro.wheelwitch.domain.RewindPackManager
import com.skiletro.wheelwitch.model.PackStatus
import com.skiletro.wheelwitch.util.io.DownloadProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

/**
 * Orchestrates the install/update flow for the Retro Rewind Pack.
 *
 * Loads the [RewindPackManager] from the persisted [DolphinTree] in
 * [init] and runs an initial [checkStatus]. Exposes the install /
 * update actions to the UI; both are guarded by an [installMutex]
 * so a double-tap on the install button cannot trigger parallel
 * downloads.
 *
 * The state machine (`UiState`) cycles through
 * `Idle → Checking → Ready → Installing → Installed → Checking → Ready`
 * on a successful install. Failure paths land in [UiState.Error] and
 * can be retried via [clearError].
 *
 * Tests can swap the [managerFactory] to inject a mock
 * [RewindPackManager] without going through SAF.
 */
class PackUpdateViewModel(
  application: Application,
  managerFactory: (Context) -> RewindPackManager? = ::defaultManagerFactory,
) : AndroidViewModel(application) {
  private val manager: RewindPackManager? = managerFactory(application)
  private val installMutex = Mutex()

  private val _state = MutableStateFlow<UiState>(UiState.Idle)
  val state: StateFlow<UiState> = _state.asStateFlow()

  init {
    checkStatus()
  }

  /**
   * Re-checks the local version against the server manifest. Emits
   * [UiState.Checking] then [UiState.Ready] with the resolved
   * [PackStatus], or [UiState.Error] on failure. When no pack
   * storage is configured (no persisted tree URI), the resolved
   * status is [PackStatus.NotInstalled] — the user is routed to
   * onboarding by the UI.
   */
  fun checkStatus() {
    viewModelScope.launch {
      _state.value = UiState.Checking
      val result =
        runCatching {
          manager?.checkStatus() ?: PackStatus.NotInstalled
        }
      _state.value =
        result.fold(
          onSuccess = {
            Timber.tag(TAG).d("checkStatus -> %s", it::class.simpleName)
            UiState.Ready(it)
          },
          onFailure = { e ->
            Timber.tag(TAG).e(e, "checkStatus failed")
            UiState.Error(e.message ?: e::class.simpleName.orEmpty())
          },
        )
    }
  }

  /**
   * Downloads and extracts the full pack zip. Guarded by
   * [installMutex]. On success, briefly emits [UiState.Installed]
   * and then auto-calls [checkStatus] to transition to [UiState.Ready]
   * with the new status.
   */
  fun installLatest() {
    viewModelScope.launch {
      installMutex.withLock {
        val mgr = manager
        if (mgr == null) {
          _state.value = UiState.Error(STORAGE_NOT_CONFIGURED)
          return@withLock
        }
        _state.value = UiState.Installing(null)
        val result =
          mgr.installLatest { progress -> _state.value = UiState.Installing(progress) }
        handleInstallResult(result)
      }
    }
  }

  /**
   * Performs the smallest set of incremental updates needed. Falls
   * back to a full reinstall if the local version is missing or
   * predates the pack format change. Same [installMutex] guard as
   * [installLatest] — calling both in parallel is a no-op for the
   * second caller.
   */
  fun update() {
    viewModelScope.launch {
      installMutex.withLock {
        val mgr = manager
        if (mgr == null) {
          _state.value = UiState.Error(STORAGE_NOT_CONFIGURED)
          return@withLock
        }
        _state.value = UiState.Installing(null)
        val result = mgr.update { progress -> _state.value = UiState.Installing(progress) }
        handleInstallResult(result)
      }
    }
  }

  /** Clears an error state and re-checks status. No-op for non-error states. */
  fun clearError() {
    if (_state.value is UiState.Error) checkStatus()
  }

  private fun handleInstallResult(result: Result<Unit>) {
    result.fold(
      onSuccess = {
        _state.value = UiState.Installed
        // Briefly show Installed, then immediately re-check status so
        // the UI lands on the new Ready(UpToDate) state.
        viewModelScope.launch { checkStatus() }
      },
      onFailure = { e ->
        Timber.tag(TAG).e(e, "install/update failed")
        _state.value = UiState.Error(e.message ?: e::class.simpleName.orEmpty())
      },
    )
  }

  private companion object {
    const val STORAGE_NOT_CONFIGURED = "Storage not configured"
    const val TAG = "PackUpdate"

    /** Default production factory: read the persisted SAF tree and wire up the manager. */
    fun defaultManagerFactory(context: Context): RewindPackManager? {
      val tree = DolphinTree.fromPersisted(context) ?: return null
      return RewindPackManager(context, tree)
    }
  }
}
